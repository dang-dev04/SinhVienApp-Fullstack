package com.library.app.service;

import com.library.app.domain.*;
import com.library.app.infrastructure.notification.NotificationAdapter;
import com.library.app.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.LocalDate;
import java.util.List;

@Service
public class LibraryService {
    @Autowired private BookRepository bookRepository;
    @Autowired private BorrowRequestRepository borrowRequestRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationAdapter notificationService; 

    @Value("${library.borrow.default-days:7}")
    private int defaultBorrowDays;


    public Page<Book> findPaginatedBooks(int pageNo, int pageSize, String sortField, String sortDir, String keyword) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? 
                    Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize, sort);
        if (keyword != null && !keyword.isEmpty()) {
            return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(keyword, keyword, pageable);
        }
        return bookRepository.findAll(pageable);
    }
    
    public Book getBookById(Long id) {
        return bookRepository.findById(id).orElse(null);
    }
    

    public void requestBorrow(Long bookId, Long userId, LocalDate requestedDueDate) {
        Book book = bookRepository.findById(bookId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        if (book != null && user != null && book.getAvailableQuantity() > 0) {
            BorrowRequest req = new BorrowRequest();
            req.setBook(book);
            req.setUser(user);
            req.setStatus("PENDING");
            
            if (requestedDueDate != null) {
                req.setDueDate(requestedDueDate);
            } else {
                req.setDueDate(LocalDate.now().plusDays(defaultBorrowDays));
            }
            
            borrowRequestRepository.save(req);
            
            notificationService.sendNotification("Reader " + user.getFullName() + " requested to borrow: " + book.getTitle());
        }
    }

    public void approveBorrow(Long requestId) {
        BorrowRequest req = borrowRequestRepository.findById(requestId).orElse(null);
        if (req != null && "PENDING".equals(req.getStatus())) {
            Book book = req.getBook();
            if (book.getAvailableQuantity() > 0) {
                book.setAvailableQuantity(book.getAvailableQuantity() - 1);
                bookRepository.save(book);

                req.setStatus("BORROWING");
                req.setBorrowDate(LocalDate.now());

                if (req.getDueDate() == null) {
                    req.setDueDate(LocalDate.now().plusDays(defaultBorrowDays));
                }

                borrowRequestRepository.save(req);

                // Gửi email SAU khi đã save thành công
                if (req.getUser() != null && req.getUser().getEmail() != null) {
                    String msg = "THƯ VIỆN E-LIBRARY - XÁC NHẬN MƯỢN SÁCH\n\n"
                        + "Xin chào " + req.getUser().getFullName() + ",\n\n"
                        + "✅ Yêu cầu mượn sách của bạn đã được DUYỆT!\n"
                        + "📖 Sách: " + book.getTitle() + "\n"
                        + "👤 Tác giả: " + (book.getAuthor() != null ? book.getAuthor() : "N/A") + "\n"
                        + "📅 Ngày mượn: " + req.getBorrowDate() + "\n"
                        + "🔔 Hạn trả: " + req.getDueDate() + "\n\n"
                        + "Vui lòng đến thư viện để nhận sách và trả đúng hạn.\n"
                        + "Cảm ơn bạn đã sử dụng dịch vụ thư viện!";
                    notificationService.sendNotification(msg, req.getUser().getEmail());
                }
            } else {
                req.setStatus("REJECTED"); // Out of stock
                borrowRequestRepository.save(req);
            }
        }
    }

    public void rejectBorrow(Long requestId) {
        BorrowRequest req = borrowRequestRepository.findById(requestId).orElse(null);
        if (req != null && "PENDING".equals(req.getStatus())) {
            req.setStatus("REJECTED");
            borrowRequestRepository.save(req);
        }
    }

    public void borrowBook(Long bookId, Long userId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        if (book != null && user != null && book.getAvailableQuantity() > 0) {
            book.setAvailableQuantity(book.getAvailableQuantity() - 1);
            bookRepository.save(book);

            BorrowRequest req = new BorrowRequest();
            req.setBook(book);
            req.setUser(user);
            req.setBorrowDate(LocalDate.now());
            req.setDueDate(LocalDate.now().plusDays(defaultBorrowDays));
            req.setStatus("BORROWING");
            borrowRequestRepository.save(req);
            
            String msg = "User " + user.getFullName() + " (" + user.getEmail() + ") đã mượn sách: " + book.getTitle() + ". Hạn trả: " + req.getDueDate();
            String userEmail = user.getEmail();
            notificationService.sendNotification(msg, userEmail);
        }
    }

    public void returnBook(Long requestId) {
        BorrowRequest req = borrowRequestRepository.findById(requestId).orElse(null);
        // Fix: chấp nhận cả OVERDUE (sách quá hạn) khi trả
        if (req != null && ("BORROWING".equals(req.getStatus()) || "OVERDUE".equals(req.getStatus()) || "RENEW_PENDING".equals(req.getStatus()))) {
            req.setStatus("RETURNED");
            req.setReturnDate(LocalDate.now());
            borrowRequestRepository.save(req);

            Book book = req.getBook();
            book.setAvailableQuantity(book.getAvailableQuantity() + 1);
            bookRepository.save(book);

            if (req.getUser() != null && req.getUser().getEmail() != null) {
                boolean isOverdue = req.getDueDate() != null && LocalDate.now().isAfter(req.getDueDate());
                String msg = "THƯ VIỆN EL IBRARY - XÁC NHẬN TRẢ SÁCH\n\n"
                    + "Xạo bạn " + req.getUser().getFullName() + ",\n\n"
                    + "✅ Bạn đã trả thành công cuốn sách: [" + book.getTitle() + "]\n"
                    + "📅 Ngày trả: " + LocalDate.now() + "\n"
                    + (req.getDueDate() != null ? "🕑 Hạn trả: " + req.getDueDate() + "\n" : "")
                    + (isOverdue ? "⚠️ Lưu ý: Sách được trả trễ hạn. Vui lòng đẳm bảo trả sách đúng hạn những lần sau.\n" : "")
                    + "\nCảm ơn bạn đã sử dụng dịch vụ thư viện!";
                notificationService.sendNotification(msg, req.getUser().getEmail());
            }
        }
    }


    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    public void updateUser(Long id, String fullName, String email) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setFullName(fullName);
            user.setEmail(email);
            userRepository.save(user);
            
            notificationService.sendNotification("Admin đã cập nhật thông tin User ID: " + id);
        }
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User không tồn tại"));

        boolean isBorrowing = user.getBorrowRequests().stream()
                .anyMatch(req -> "BORROWING".equals(req.getStatus()));

        if (isBorrowing) {
            throw new RuntimeException("Không thể xóa user " + user.getFullName() + " vì đang mượn sách chưa trả!");
        }
        userRepository.delete(user);
        notificationService.sendNotification("CẢNH BÁO: Admin đã xóa vĩnh viễn User: " + user.getFullName());
    }
    
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
    
     public void addBook(String title, String author, String isbn, 
                        Long categoryId, Integer qty, String img,
                        String publisher, Integer year) {
        Category cat = (categoryId != null) ? categoryRepository.findById(categoryId).orElse(null) : null;
        Book newBook = new BookBuilder(title, author)
                .setIsbn(isbn)
                .setCategory(cat)
                .setTotalQuantity(qty)
                .setImage(img)
                .setPublisher(publisher)
                .setPublishYear(year)
                .build();
        bookRepository.save(newBook);
    }

     public void editBook(Long id, String title, String author, String isbn, 
             Long categoryId, String publisher, Integer year, 
             Integer totalQty, String img) {

    	 	Book book = bookRepository.findById(id).orElse(null);
    	 	if (book != null) {
    	 		book.setTitle(title);
    	 		book.setAuthor(author);
    	 		book.setIsbn(isbn);

    	 		if (categoryId != null) {
    	 			Category cat = categoryRepository.findById(categoryId).orElse(null);
    	 			book.setCategory(cat);
    	 		}

    	 		book.setPublisher(publisher);
    	 		book.setPublishYear(year);
    	 		book.setImage(img);

    	 		if (totalQty != null) {
    	 			int difference = totalQty - book.getTotalQuantity();
    
    	 			book.setTotalQuantity(totalQty);

    	 			book.setAvailableQuantity(book.getAvailableQuantity() + difference);
    	 		}

    	 		bookRepository.save(book);
    	 	}
     }

    @Transactional 
    public void deleteBook(Long id) {
        List<BorrowRequest> history = borrowRequestRepository.findByBookId(id);
        borrowRequestRepository.deleteAll(history);
        bookRepository.deleteById(id);
    }
    
    public Page<BorrowRequest> findPaginatedBorrows(int pageNo, int pageSize, String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                    Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize, sort);
        return borrowRequestRepository.findAll(pageable);
    }

    /** Truy vấn ưu tiên PENDING + filter status + keyword, dùng cho trang Admin/Librarian */
    public Page<BorrowRequest> findBorrowsWithFilter(int pageNo, int pageSize, String statusFilter, String keyword) {
        // Dùng unsorted pageable vì ORDER BY đã nằm trong JPQL query
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
        String status  = (statusFilter != null && !statusFilter.isBlank()) ? statusFilter.strip() : null;
        String kw      = (keyword     != null && !keyword.isBlank())       ? keyword.strip()      : null;
        return borrowRequestRepository.findAllWithPriorityAndFilter(status, kw, pageable);
    }

    /** Đếm số request đang chờ duyệt để hiển thị badge */
    public long countPendingBorrows() {
        return borrowRequestRepository.countByStatus("PENDING");
    }
    public List<Object[]> getBorrowStats() {
        return borrowRequestRepository.countBorrowsByDate();
    }
    public List<Object[]> getReturnStats() {
        return borrowRequestRepository.countReturnsByDate();
    }
    
    @Scheduled(cron = "0 0 8 * * ?") // Chạy mỗi 8h sáng
    public void checkOverdueAndSendReminders() {
        LocalDate today = LocalDate.now();
        
        // 1. Gửi email nhắc nhở trước 1 ngày
        LocalDate tomorrow = today.plusDays(1);
        List<BorrowRequest> reminders = borrowRequestRepository.findByStatusAndDueDate("BORROWING", tomorrow);
        for (BorrowRequest req : reminders) {
            if (req.getUser() != null) {
                String msg = "CẢNH BÁO: Cuốn sách '" + req.getBook().getTitle() + "' của bạn sẽ đến hạn trả vào ngày mai (" + tomorrow + "). Vui lòng sắp xếp thời gian trả sách đúng hạn!";
                notificationService.sendNotification(msg, req.getUser().getEmail());
            }
        }
        
        // 2. Chuyển trạng thái sang OVERDUE
        List<BorrowRequest> overdues = borrowRequestRepository.findByStatusAndDueDateBefore("BORROWING", today);
        for (BorrowRequest req : overdues) {
            req.setStatus("OVERDUE");
            borrowRequestRepository.save(req);
            if (req.getUser() != null) {
                String msg = "THÔNG BÁO QUÁ HẠN: Cuốn sách '" + req.getBook().getTitle() + "' của bạn đã quá hạn trả. Vui lòng trả sách ngay lập tức!";
                notificationService.sendNotification(msg, req.getUser().getEmail());
            }
        }
    }
}
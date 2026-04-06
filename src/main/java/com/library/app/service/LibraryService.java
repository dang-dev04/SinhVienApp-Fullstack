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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class LibraryService {
    @Autowired private BookRepository bookRepository;
    @Autowired private BorrowRequestRepository borrowRequestRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationAdapter notificationService; 


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
    

    public void requestBorrow(Long bookId, Long userId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        if (book != null && user != null && book.getAvailableQuantity() > 0) {
            BorrowRequest req = new BorrowRequest();
            req.setBook(book);
            req.setUser(user);
            req.setStatus("PENDING");
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
                borrowRequestRepository.save(req);
                
                if (req.getUser() != null) {
                    notificationService.sendNotification("Yêu cầu mượn sách '" + book.getTitle() + "' của bạn đã được duyệt!", req.getUser().getEmail());
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
            req.setStatus("BORROWING");
            borrowRequestRepository.save(req);
            
            String msg = "User " + user.getFullName() + " (" + user.getEmail() + ") đã mượn sách: " + book.getTitle();
            String userEmail = user.getEmail();
            notificationService.sendNotification(msg,userEmail);
        }
    }

    public void returnBook(Long requestId) {
        BorrowRequest req = borrowRequestRepository.findById(requestId).orElse(null);
        if (req != null && "BORROWING".equals(req.getStatus())) {
            req.setStatus("RETURNED");
            req.setReturnDate(LocalDate.now());
            borrowRequestRepository.save(req);

            Book book = req.getBook();
            book.setAvailableQuantity(book.getAvailableQuantity() + 1);
            bookRepository.save(book);
            
            if (req.getUser() != null) {
                String msg = "Thông báo: " + req.getUser().getFullName() + " đã trả sách (" + book.getTitle() + ") thành công.";
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
    public List<Object[]> getBorrowStats() {
        return borrowRequestRepository.countBorrowsByDate();
    }
    public List<Object[]> getReturnStats() {
        return borrowRequestRepository.countReturnsByDate();
    }
}
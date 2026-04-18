package com.library.app.controller;

import com.library.app.domain.Book;
import com.library.app.domain.BorrowRequest;
import com.library.app.domain.User;
import com.library.app.repository.BorrowRequestRepository;
import com.library.app.repository.UserRepository;
import com.library.app.service.LibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/reader")
public class ReaderController {

    @Autowired private LibraryService libraryService;
    @Autowired private UserRepository userRepository;
    @Autowired private BorrowRequestRepository borrowRequestRepository;

    private User getLoggedInUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    @GetMapping("/home")
    public String showReaderHome(Model model,
                                 @RequestParam(name = "keyword", required = false) String keyword,
                                 @RequestParam(defaultValue = "1") int pageNo) {
        int pageSize = 8;
        Page<Book> page = libraryService.findPaginatedBooks(pageNo, pageSize, "title", "asc", keyword);
        model.addAttribute("books", page.getContent());
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentMenu", "home");
        return "reader/home";
    }

    @GetMapping("/book/{id}")
    public String viewBookDetail(@PathVariable Long id, Model model) {
        Book book = libraryService.getBookById(id);
        if (book == null) return "redirect:/reader/home";
        model.addAttribute("book", book);
        model.addAttribute("currentMenu", "home");
        return "reader/book-detail";
    }

    @PostMapping("/borrow/{bookId}")
    public String requestBorrow(@PathVariable Long bookId, 
                                @RequestParam(required = false) String dueDate, 
                                Model model) {
        User user = getLoggedInUser();
        if (user != null) {
            java.time.LocalDate requestedDate = null;
            if (dueDate != null && !dueDate.trim().isEmpty()) {
                requestedDate = java.time.LocalDate.parse(dueDate);
            }
            libraryService.requestBorrow(bookId, user.getId(), requestedDate);
        }
        return "redirect:/reader/active-borrows?success=true";
    }

    @GetMapping("/active-borrows")
    public String showActiveBorrows(Model model) {
        User user = getLoggedInUser();
        if (user != null) {
            List<BorrowRequest> requests = borrowRequestRepository.findByUserId(user.getId())
                .stream().filter(r -> "PENDING".equals(r.getStatus()) || "BORROWING".equals(r.getStatus()) || "RENEW_PENDING".equals(r.getStatus()))
                .toList();
            model.addAttribute("borrows", requests);
        }
        model.addAttribute("currentMenu", "active-borrows");
        return "reader/active-borrows";
    }

    @GetMapping("/history")
    public String showHistory(Model model) {
        User user = getLoggedInUser();
        if (user != null) {
            List<BorrowRequest> requests = borrowRequestRepository.findByUserId(user.getId());
            model.addAttribute("borrows", requests);
        }
        model.addAttribute("currentMenu", "history");
        return "reader/history";
    }

    @GetMapping("/profile")
    public String showProfile(Model model) {
        User user = getLoggedInUser();
        if (user != null) {
            model.addAttribute("user", user);
            long activeCount = borrowRequestRepository.countByUserIdAndStatusIn(user.getId(), List.of("PENDING", "BORROWING", "RENEW_PENDING"));
            long completedCount = borrowRequestRepository.countByUserIdAndStatusIn(user.getId(), List.of("RETURNED"));
            model.addAttribute("activeCount", activeCount);
            model.addAttribute("completedCount", completedCount);
        }
        model.addAttribute("currentMenu", "profile");
        return "reader/profile";
    }
}

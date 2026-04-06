package com.library.app.controller;

import com.library.app.domain.Book;
import com.library.app.domain.BorrowRequest;
import com.library.app.repository.BorrowRequestRepository;
import com.library.app.service.LibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.library.app.domain.User;
import com.library.app.repository.UserRepository;
import java.util.*;
import java.util.ArrayList;
import java.util.List;

@Controller
public class AuthController {
    @Autowired private LibraryService libraryService;
    @Autowired private BorrowRequestRepository borrowRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;

    @GetMapping("/")
    public String showLogin() { return "login"; }

    @GetMapping("/register")
    public String showRegister() { return "register"; }

    @PostMapping("/register")
    public String processRegister(@RequestParam String fullName, 
                                  @RequestParam String email, 
                                  @RequestParam String password,
                                  Model model) {
        if (userRepository.findByEmail(email).isPresent()) {
            model.addAttribute("error", "Email đã tồn tại!");
            return "register";
        }
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ROLE_READER");
        userRepository.save(user);
        
        return "redirect:/?registered=true";
    }


    
    @GetMapping("/logout")
    public String logout() { return "redirect:/"; }

    @GetMapping("/index")
    public String showIndex(Model model,
                            @RequestParam(name = "keyword", required = false) String keyword,
                            @RequestParam(defaultValue = "1") int pageNo,
                            @RequestParam(defaultValue = "id") String sortField,
                            @RequestParam(defaultValue = "asc") String sortDir) {
        
        int pageSize = 20;
        
        Page<Book> page = libraryService.findPaginatedBooks(pageNo, pageSize, sortField, sortDir, keyword);
        List<Book> listBooks = page.getContent();

        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        
        model.addAttribute("keyword", keyword);
        model.addAttribute("books", listBooks);
        
        model.addAttribute("categories", libraryService.getAllCategories());
        model.addAttribute("users", libraryService.getAllUsers());
        
        model.addAttribute("currentPageName", "books");
        
        return "index";
    }

    @GetMapping("/borrows")
    public String showBorrowHistory(Model model,
                                    @RequestParam(defaultValue = "1") int pageNo,
                                    @RequestParam(defaultValue = "borrowDate") String sortField,
                                    @RequestParam(defaultValue = "desc") String sortDir) {
        
        int pageSize = 10;
        Page<BorrowRequest> page = libraryService.findPaginatedBorrows(pageNo, pageSize, sortField, sortDir);
        
        model.addAttribute("borrows", page.getContent());
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        
        model.addAttribute("currentPageName", "borrows");
        
        return "borrows";
    }

    @PostMapping("/book/add")
    public String add(@RequestParam String title, @RequestParam String author,
                      @RequestParam(required=false) String isbn, 
                      @RequestParam Long categoryId,
                      @RequestParam(required=false) String publisher, @RequestParam(required=false) Integer year,
                      @RequestParam(required=false) String image, @RequestParam Integer totalQuantity) {
        
        libraryService.addBook(title, author, isbn, categoryId, totalQuantity, image, publisher, year);
        return "redirect:/index";
    }

    @PostMapping("/book/edit")
    public String edit(@RequestParam Long id, 
                       @RequestParam String title, 
                       @RequestParam String author,
                       @RequestParam String isbn, 
                       @RequestParam(required=false) Long categoryId,
                       @RequestParam String publisher,
                       @RequestParam Integer year, 
                       @RequestParam Integer totalQuantity,
                       @RequestParam String image) {
        libraryService.editBook(id, title, author, isbn, categoryId, publisher, year, totalQuantity, image);
        return "redirect:/index";
    }

    @GetMapping("/book/delete")
    public String delete(@RequestParam Long id) {
        libraryService.deleteBook(id);
        return "redirect:/index";
    }

    @PostMapping("/book/borrow")
    public String borrow(@RequestParam Long id, @RequestParam Long userId) {
        libraryService.borrowBook(id, userId);
        return "redirect:/index";
    }

    @GetMapping("/book/return")
    public String returnBook(@RequestParam Long id) { 
        libraryService.returnBook(id);
        return "redirect:/borrows";
    }

    @PostMapping("/admin/borrow/{id}/approve")
    public String approveBorrow(@PathVariable Long id) {
        libraryService.approveBorrow(id);
        return "redirect:/borrows";
    }

    @PostMapping("/admin/borrow/{id}/reject")
    public String rejectBorrow(@PathVariable Long id) {
        libraryService.rejectBorrow(id);
        return "redirect:/borrows";
    }

    @GetMapping("/api/borrows")
    @ResponseBody
    public List<BorrowerDTO> getBorrowers(@RequestParam Long bookId) {
        List<BorrowRequest> list = borrowRepo.findByBookIdAndStatus(bookId, "BORROWING");
        List<BorrowerDTO> dtos = new ArrayList<>();
        for (BorrowRequest r : list) {
            String name = (r.getUser() != null) ? r.getUser().getFullName() : "Unknown";
            dtos.add(new BorrowerDTO(r.getId(), name, r.getBorrowDate().toString()));
        }
        return dtos;
    }

    static class BorrowerDTO {
        public Long requestId;
        public String name;
        public String date;
        public BorrowerDTO(Long r, String n, String d) { this.requestId = r; this.name = n; this.date = d; }
    }
    @GetMapping("/stats")
    public String showStats(Model model) {
        List<Object[]> borrowData = libraryService.getBorrowStats();
        List<Object[]> returnData = libraryService.getReturnStats();

        Map<String, Long> borrowMap = new HashMap<>();
        Map<String, Long> returnMap = new HashMap<>();

        Set<String> allDates = new TreeSet<>();

        for (Object[] row : borrowData) {
            if (row[0] != null) {
                String date = row[0].toString();
                borrowMap.put(date, (Long) row[1]);
                allDates.add(date);
            }
        }

        for (Object[] row : returnData) {
            if (row[0] != null) {
                String date = row[0].toString();
                returnMap.put(date, (Long) row[1]);
                allDates.add(date);
            }
        }

        List<String> labels = new ArrayList<>(allDates); 
        List<Long> borrowCounts = new ArrayList<>();     
        List<Long> returnCounts = new ArrayList<>();     

        for (String date : labels) {
            borrowCounts.add(borrowMap.getOrDefault(date, 0L));
            returnCounts.add(returnMap.getOrDefault(date, 0L));
        }

        model.addAttribute("dates", labels);
        model.addAttribute("borrowCounts", borrowCounts);
        model.addAttribute("returnCounts", returnCounts);
        model.addAttribute("currentPageName", "stats");

        return "stats";
    }
}
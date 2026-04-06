package com.library.app.controller;

import com.library.app.domain.User;
import com.library.app.service.LibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {

    @Autowired
    private LibraryService libraryService;

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", libraryService.getAllUsers());
        model.addAttribute("currentPageName", "users");
        return "users";
    }

    @PostMapping("/user/add")
    public String addUser(@RequestParam String fullName, @RequestParam String email) {
        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        libraryService.saveUser(newUser);
        return "redirect:/users";
    }

    @PostMapping("/user/update")
    public String updateUser(@RequestParam Long id, 
                             @RequestParam String fullName, 
                             @RequestParam String email) {
        libraryService.updateUser(id, fullName, email);
        return "redirect:/users";
    }

    @GetMapping("/user/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            libraryService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa User thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/users";
    }
}
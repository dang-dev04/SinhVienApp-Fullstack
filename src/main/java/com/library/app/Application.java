package com.library.app;

import com.library.app.domain.User;
import com.library.app.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            User admin = userRepository.findByEmail("admin").orElse(new User());
            admin.setFullName("Quản trị viên");
            admin.setEmail("admin");
            admin.setPassword(passwordEncoder.encode("123"));
            admin.setRole("ROLE_ADMIN");
            userRepository.save(admin);

            User librarian = userRepository.findByEmail("thuthu").orElse(new User());
            librarian.setFullName("Thủ thư");
            librarian.setEmail("thuthu");
            librarian.setPassword(passwordEncoder.encode("123"));
            librarian.setRole("ROLE_LIBRARIAN");
            userRepository.save(librarian);
        };
    }
}

package com.library.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Tạm tắt CSRF để tránh lỗi POST Form Thymeleaf
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/register", "/login").permitAll()
                // Phân quyền cho ADMIN
                .requestMatchers("/users/**", "/user/**", "/stats", "/book/add", "/book/edit", "/book/delete").hasRole("ADMIN")
                // Phân quyền cho LIBRARIAN và ADMIN
                .requestMatchers("/borrows", "/admin/borrows", "/admin/borrow/**", "/book/return", "/index", "/api/borrows").hasAnyRole("ADMIN", "LIBRARIAN")
                // Phân quyền cho READER
                .requestMatchers("/reader/**").hasRole("READER")
                .requestMatchers("/").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/")
                .loginProcessingUrl("/login")
                .successHandler((request, response, authentication) -> {
                    // Chuyển hướng theo role
                    var authorities = authentication.getAuthorities();
                    boolean isReader = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_READER"));
                    if (isReader) {
                        response.sendRedirect("/reader/home");
                    } else {
                        response.sendRedirect("/index");
                    }
                })
                .failureUrl("/?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );
            
        return http.build();
    }
}

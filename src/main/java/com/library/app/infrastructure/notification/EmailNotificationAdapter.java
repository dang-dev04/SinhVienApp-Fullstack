package com.library.app.infrastructure.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationAdapter implements NotificationAdapter {

    @Autowired
    private JavaMailSender mailSender; 
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendNotification(String message) {
        sendNotification(message, "email_admin_mac_dinh@gmail.com");
    }

    @Override
    public void sendNotification(String message, String recipient) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(recipient);
            mailMessage.setSubject("THÔNG BÁO TỪ THƯ VIỆN");
            mailMessage.setText(message);
            mailSender.send(mailMessage);
            
            System.out.println(">> Đã gửi email thành công tới: " + recipient);
            
        } catch (Exception e) {
            System.err.println(">> Lỗi gửi email: " + e.getMessage());
        }
    }
}
package com.library.app.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Adapter gửi email thông báo qua Gmail SMTP.
 *
 * Yêu cầu Gmail:
 *  1. Bật xác minh 2 bước tại: https://myaccount.google.com/security
 *  2. Tạo App Password tại: https://myaccount.google.com/apppasswords
 *  3. Điền App Password (16 ký tự, không có khoảng trắng) vào file .env
 */
@Component
public class EmailNotificationAdapter implements NotificationAdapter {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationAdapter.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Async   // Gửi email bất đồng bộ → tránh block luồng chính
    public void sendNotification(String message) {
        sendNotification(message, fromEmail);
    }

    @Override
    @Async
    public void sendNotification(String message, String recipient) {
        if (recipient == null || recipient.isBlank()) {
            log.warn("[Email] Bỏ qua: recipient rỗng hoặc null.");
            return;
        }
        // Không gửi email tới placeholder không hợp lệ
        if (!recipient.contains("@") || recipient.equals("default@gmail.com") || recipient.equals("email_admin_mac_dinh@gmail.com")) {
            log.warn("[Email] Bỏ qua: địa chỉ không hợp lệ → {}", recipient);
            return;
        }

        log.info("[Email] Đang gửi tới: {} | Tiêu đề: THÔNG BÁO THƯ VIỆN", recipient);
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromEmail);
            mail.setTo(recipient);
            mail.setSubject("📚 THÔNG BÁO TỪ THƯ VIỆN E-LIBRARY");
            mail.setText(message);
            mailSender.send(mail);
            log.info("[Email] ✅ Gửi thành công → {}", recipient);

        } catch (MailAuthenticationException e) {
            log.error("[Email] ❌ Lỗi xác thực Gmail (App Password sai hoặc chưa bật 2FA): {}", e.getMessage());
            log.error("[Email] → Kiểm tra tại: https://myaccount.google.com/apppasswords");
        } catch (MailSendException e) {
            log.error("[Email] ❌ Lỗi gửi mail tới {}: {}", recipient, e.getMessage());
            // In full stacktrace để debug
            log.debug("[Email] Full error:", e);
        } catch (Exception e) {
            log.error("[Email] ❌ Lỗi không xác định khi gửi mail tới {}: {} - {}",
                recipient, e.getClass().getSimpleName(), e.getMessage());
            log.debug("[Email] Full error:", e);
        }
    }
}
package com.library.app.infrastructure.notification;

public interface NotificationAdapter {

    void sendNotification(String message);

    void sendNotification(String message, String recipient);
}
package com.example.taskapp.service;

import com.example.taskapp.model.Notification;
import com.example.taskapp.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("postgres")
public class NotificationService {
    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getAllNotifications(Long userId) {
        return notificationRepository.findAllByUserId(userId);
    }

    public List<Notification> getPendingNotifications(Long userId) {
        return notificationRepository.findPendingByUserId(userId);
    }

    public Notification createNotification(Notification notification) {
        if (notification == null || notification.getUserId() == null || notification.getMessage() == null) {
            throw new IllegalArgumentException("Notification, userId, and message cannot be null");
        }
        notification.setStatus("pending");
        return notificationRepository.save(notification);
    }

    public Notification createNotification(Long userId, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .message(message)
                .status("pending")
                .build();
        return createNotification(notification);
    }
}
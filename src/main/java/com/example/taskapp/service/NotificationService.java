package com.example.taskapp.service;

import com.example.taskapp.model.Notification;
import com.example.taskapp.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
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
        notification.setStatus("pending");
        return notificationRepository.save(notification);
    }
}
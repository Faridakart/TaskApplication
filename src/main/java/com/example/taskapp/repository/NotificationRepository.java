package com.example.taskapp.repository;

import com.example.taskapp.model.Notification;

import java.util.List;

public interface NotificationRepository {
    List<Notification> findAllByUserId(Long userId);
    List<Notification> findPendingByUserId(Long userId);
    Notification save(Notification notification);
}
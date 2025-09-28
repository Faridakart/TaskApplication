package com.example.taskapp.repository;

import com.example.taskapp.model.Notification;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("postgres")
public interface JpaNotificationRepository extends NotificationRepository, JpaRepository<Notification, Long> {
    List<Notification> findByUserId(Long userId);

    List<Notification> findByUserIdAndStatus(Long userId, String status);

    @Override
    default Notification save(Notification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("Notification cannot be null");
        }
        if (notification.getId() != null && !existsById(notification.getId())) {
            throw new IllegalArgumentException("Notification with id " + notification.getId() + " not found for update via save.");
        }
        return saveAndFlush(notification);
    }
}
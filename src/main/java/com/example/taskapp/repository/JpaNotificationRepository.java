package com.example.taskapp.repository;

import com.example.taskapp.model.Notification;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("h2")
public interface JpaNotificationRepository extends NotificationRepository, JpaRepository<Notification, Long> {
    @Override
    default List<Notification> findAllByUserId(Long userId) {
        return findByUserId(userId);
    }

    List<Notification> findByUserId(Long userId);

    @Override
    default List<Notification> findPendingByUserId(Long userId) {
        return findByUserIdAndStatus(userId, "pending");
    }

    List<Notification> findByUserIdAndStatus(Long userId, String status);

    @Override
    default Notification save(Notification notification) {
        return saveAndFlush(notification);
    }
}
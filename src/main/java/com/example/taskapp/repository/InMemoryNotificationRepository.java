package com.example.taskapp.repository;

import com.example.taskapp.model.Notification;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
@Profile("inmemory")
public class InMemoryNotificationRepository implements NotificationRepository {
    private final List<Notification> notifications = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public List<Notification> findAllByUserId(Long userId) {
        return notifications.stream()
                .filter(n -> n.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Notification> findPendingByUserId(Long userId) {
        return notifications.stream()
                .filter(n -> n.getUserId().equals(userId) && "pending".equals(n.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public Notification save(Notification notification) {
        if (notification.getId() == null) {
            notification.setId(idGenerator.getAndIncrement());
        }
        notifications.add(notification);
        return notification;
    }
}
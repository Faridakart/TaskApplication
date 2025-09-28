package com.example.taskapp.service;

import com.example.taskapp.model.Notification;
import com.example.taskapp.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        notificationService = new NotificationService(notificationRepository); // Real NotificationService
    }

    @Test
    void getAllNotifications() {
        Long userId = 1L;
        when(notificationRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());
        List<Notification> notifications = notificationService.getAllNotifications(userId);
        assertEquals(0, notifications.size());
        verify(notificationRepository, times(1)).findAllByUserId(userId);
    }

    @Test
    void getPendingNotifications() {
        Long userId = 1L;
        when(notificationRepository.findPendingByUserId(userId)).thenReturn(Collections.emptyList());
        List<Notification> notifications = notificationService.getPendingNotifications(userId);
        assertEquals(0, notifications.size());
        verify(notificationRepository, times(1)).findPendingByUserId(userId);
    }

    @Test
    void createNotification() {
        Notification notification = new Notification();
        notification.setMessage("Test Notification");
        notification.setUserId(1L);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        Notification created = notificationService.createNotification(notification);
        assertEquals("pending", created.getStatus());
        assertEquals("Test Notification", created.getMessage());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
}
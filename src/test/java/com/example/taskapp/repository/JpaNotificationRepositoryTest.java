package com.example.taskapp.repository;

import com.example.taskapp.model.Notification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("postgres")
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaNotificationRepositoryTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("taskdb")
            .withUsername("postgres")
            .withPassword("postgres");

    @BeforeAll
    static void beforeAll() {
        postgres.start();
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());
    }

    @Autowired
    private JpaNotificationRepository repository;

    private Notification notificationToSave1User1;
    private Notification notificationToSave2User1;
    private Notification notificationToSave1User2;
    private static final long USER_ID_1 = 1L;
    private static final long USER_ID_2 = 2L;
    private static final long NON_EXISTENT_NOTIFICATION_ID = 999L;

    @BeforeEach
    void setUp() {
        notificationToSave1User1 = Notification.builder()
                .userId(USER_ID_1)
                .message("Notification 1 User 1")
                .status("pending")
                .build();

        notificationToSave2User1 = Notification.builder()
                .userId(USER_ID_1)
                .message("Notification 2 User 1")
                .status("sent")
                .build();

        notificationToSave1User2 = Notification.builder()
                .userId(USER_ID_2)
                .message("Notification 1 User 2")
                .status("pending")
                .build();
    }

    @Test
    void findByUserId_ReturnsOnlyUserNotifications() {
        Notification savedNotification1 = repository.save(notificationToSave1User1);
        Notification savedNotification2 = repository.save(notificationToSave2User1);
        repository.save(notificationToSave1User2);

        List<Notification> user1Notifications = repository.findByUserId(USER_ID_1);
        assertNotNull(user1Notifications);
        assertEquals(2, user1Notifications.size());
        assertTrue(user1Notifications.stream()
                .anyMatch(n -> n.getId().equals(savedNotification1.getId())));
        assertTrue(user1Notifications.stream()
                .anyMatch(n -> n.getId().equals(savedNotification2.getId())));
    }

    @Test
    void findByUserId_ReturnsEmptyList_WhenUserHasNoNotifications() {
        repository.save(notificationToSave1User2);
        List<Notification> user1Notifications = repository.findByUserId(USER_ID_1);
        assertNotNull(user1Notifications);
        assertTrue(user1Notifications.isEmpty());
    }

    @Test
    void findByUserId_ReturnsEmptyList_WhenUserIdIsNull() {
        repository.save(notificationToSave1User1);
        List<Notification> userNotifications = repository.findByUserId(null);
        assertNotNull(userNotifications);
        assertTrue(userNotifications.isEmpty());
    }

    @Test
    void findByUserIdAndStatus_ReturnsOnlyMatchingNotifications() {
        Notification savedNotification1 = repository.save(notificationToSave1User1);
        repository.save(notificationToSave2User1);
        repository.save(notificationToSave1User2);

        List<Notification> pendingNotifications = repository.findByUserIdAndStatus(USER_ID_1, "pending");
        assertNotNull(pendingNotifications);
        assertEquals(1, pendingNotifications.size());
        assertTrue(pendingNotifications.stream()
                .anyMatch(n -> n.getId().equals(savedNotification1.getId())));
    }

    @Test
    void findByUserIdAndStatus_ReturnsEmptyList_WhenNoMatchingNotifications() {
        repository.save(notificationToSave2User1);
        List<Notification> pendingNotifications = repository.findByUserIdAndStatus(USER_ID_1, "pending");
        assertNotNull(pendingNotifications);
        assertTrue(pendingNotifications.isEmpty());
    }

    @Test
    void save_NewNotification_AssignsIdAndReturnsSavedNotification() {
        Notification savedNotification = repository.save(notificationToSave1User1);

        assertNotNull(savedNotification);
        assertNotNull(savedNotification.getId());
        assertEquals(notificationToSave1User1.getMessage(), savedNotification.getMessage());
        assertEquals(USER_ID_1, savedNotification.getUserId());
        assertEquals("pending", savedNotification.getStatus());

        assertTrue(repository.existsById(savedNotification.getId()));
        assertEquals(Optional.of(savedNotification), repository.findById(savedNotification.getId()));
    }

    @Test
    void save_NewNotification_DoesNotThrowDuplicate_IfCalledMultipleTimesWithDifferentObjects() {
        Notification savedNotification1 = repository.save(notificationToSave1User1);

        Notification anotherNotificationToSave = Notification.builder()
                .userId(notificationToSave1User1.getUserId())
                .message(notificationToSave1User1.getMessage())
                .status("pending")
                .build();
        Notification savedNotification2 = repository.save(anotherNotificationToSave);

        assertNotNull(savedNotification1.getId());
        assertNotNull(savedNotification2.getId());
        assertNotEquals(savedNotification1.getId(), savedNotification2.getId());
        assertEquals(2, repository.findAll().size());
    }

    @Test
    void save_ThrowsIllegalArgumentException_WhenNotificationIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.save(null)
        );
        assertEquals("Notification cannot be null", exception.getMessage());
    }

    @Test
    void save_ExistingNotification_UpdatesAndReturnsNotification() {
        Notification savedNotification = repository.save(notificationToSave1User1);
        Long originalId = savedNotification.getId();

        Notification notificationToUpdate = Notification.builder()
                .id(originalId)
                .userId(savedNotification.getUserId())
                .message("Updated Notification")
                .status("sent")
                .build();

        Notification updatedNotification = repository.save(notificationToUpdate);

        assertNotNull(updatedNotification);
        assertEquals(originalId, updatedNotification.getId());
        assertEquals("Updated Notification", updatedNotification.getMessage());
        assertEquals("sent", updatedNotification.getStatus());

        Optional<Notification> foundOpt = repository.findById(originalId);
        assertTrue(foundOpt.isPresent());
        assertEquals("Updated Notification", foundOpt.get().getMessage());
    }

    @Test
    void save_ExistingNotification_ThrowsIllegalArgumentException_WhenUpdatingNonExistingNotification() {
        Notification nonExistentNotificationToUpdate = Notification.builder()
                .id(NON_EXISTENT_NOTIFICATION_ID)
                .userId(USER_ID_1)
                .message("Ghost Notification")
                .status("pending")
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.save(nonExistentNotificationToUpdate)
        );
        assertTrue(exception.getMessage()
                .contains("Notification with id " + NON_EXISTENT_NOTIFICATION_ID + " not found for update via save."));
    }
}
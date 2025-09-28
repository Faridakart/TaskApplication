package com.example.taskapp.repository;

import com.example.taskapp.config.CacheConfig;
import com.example.taskapp.config.RabbitMQConfig;
import com.example.taskapp.model.Task;
import com.example.taskapp.model.Notification;
import com.example.taskapp.service.NotificationService;
import com.example.taskapp.service.TaskService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("postgres")
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CacheConfig.class, RabbitMQConfig.class, NotificationService.class, TaskService.class})
class JpaTaskRepositoryTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("taskdb")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:7.0")
            .withExposedPorts(6379);

    @Container
    private static final GenericContainer<?> rabbitmq = new GenericContainer<>("rabbitmq:3.13-management")
            .withExposedPorts(5672);

    @BeforeAll
    static void beforeAll() {
        postgres.start();
        redis.start();
        rabbitmq.start();
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());
        System.setProperty("spring.redis.host", redis.getHost());
        System.setProperty("spring.redis.port", redis.getMappedPort(6379).toString());
        System.setProperty("spring.rabbitmq.host", rabbitmq.getHost());
        System.setProperty("spring.rabbitmq.port", rabbitmq.getMappedPort(5672).toString());
        System.setProperty("spring.rabbitmq.username", "guest");
        System.setProperty("spring.rabbitmq.password", "guest");
    }

    @Autowired
    private JpaTaskRepository taskRepository;

    @Autowired
    private JpaNotificationRepository notificationRepository;

    @Autowired
    private TaskService taskService;

    private Task taskToSave1User1;
    private Task taskToSave2User1;
    private Task taskToSave1User2;
    private static final long USER_ID_1 = 1L;
    private static final long USER_ID_2 = 2L;

    @BeforeEach
    void setUp() {
        taskToSave1User1 = Task.builder()
                .userId(USER_ID_1)
                .description("Task 1 User 1 Description")
                .creationDate(LocalDateTime.now())
                .status("pending")
                .build();

        taskToSave2User1 = Task.builder()
                .userId(USER_ID_1)
                .description("Task 2 User 1 Description")
                .creationDate(LocalDateTime.now())
                .status("completed")
                .build();

        taskToSave1User2 = Task.builder()
                .userId(USER_ID_2)
                .description("Task 1 User 2 Description")
                .creationDate(LocalDateTime.now())
                .status("pending")
                .build();

        taskRepository.evictCaches(null);
        notificationRepository.deleteAll();
    }

    @Test
    void createTask_PublishesMessageAndCreatesNotification() throws InterruptedException {
        Task savedTask = taskService.createTask(taskToSave1User1);
        Thread.sleep(1000);
        List<Notification> notifications = notificationRepository.findAllByUserId(USER_ID_1);
        assertEquals(1, notifications.size());
        assertEquals("New task created: " + savedTask.getDescription(), notifications.get(0).getMessage());
        assertEquals("pending", notifications.get(0).getStatus());
    }

    @Test
    void findByIsDeletedFalse_ReturnsOnlyNonDeletedTasks() {
        Task savedTask1 = taskService.createTask(taskToSave1User1);
        Task savedTask2 = taskService.createTask(taskToSave1User2);

        Task taskToDelete = Task.builder()
                .userId(USER_ID_1)
                .description("To Delete")
                .creationDate(LocalDateTime.now())
                .status("pending")
                .build();
        Task savedTaskToDelete = taskService.createTask(taskToDelete);
        taskService.deleteTask(savedTaskToDelete.getId());

        List<Task> receivedTasks = taskRepository.findByIsDeletedFalse();

        assertNotNull(receivedTasks);
        assertEquals(2, receivedTasks.size());
        assertTrue(receivedTasks.stream()
                .anyMatch(t -> t.getId().equals(savedTask1.getId())));
        assertTrue(receivedTasks.stream()
                .anyMatch(t -> t.getId().equals(savedTask2.getId())));
        assertFalse(receivedTasks.stream()
                .anyMatch(t -> t.getId().equals(savedTaskToDelete.getId())));
    }
}
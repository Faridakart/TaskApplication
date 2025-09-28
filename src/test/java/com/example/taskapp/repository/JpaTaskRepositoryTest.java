```java
        package com.example.taskapp.repository;

import com.example.taskapp.model.Task;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("postgres")
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(com.example.taskapp.config.CacheConfig.class)
class JpaTaskRepositoryTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("taskdb")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:7.0")
            .withExposedPorts(6379);

    @BeforeAll
    static void beforeAll() {
        postgres.start();
        redis.start();
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());
        System.setProperty("spring.redis.host", redis.getHost());
        System.setProperty("spring.redis.port", redis.getMappedPort(6379).toString());
    }

    @Autowired
    private JpaTaskRepository repository;

    @Autowired
    private CacheManager cacheManager;

    private Task taskToSave1User1;
    private Task taskToSave2User1;
    private Task taskToSave1User2;
    private static final long USER_ID_1 = 1L;
    private static final long USER_ID_2 = 2L;
    private static final long NON_EXISTENT_TASK_ID = 999L;

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

        // Clear cache before each test
        cacheManager.getCache("tasks").clear();
    }

    @Test
    void findByIsDeletedFalse_ReturnsCachedTasks_WhenCalledMultipleTimes() {
        Task savedTask1 = repository.save(taskToSave1User1);
        Task savedTask2 = repository.save(taskToSave1User2);

        // First call: Should hit database and cache
        List<Task> tasksFirstCall = repository.findByIsDeletedFalse();
        assertEquals(2, tasksFirstCall.size());

        // Second call: Should hit cache
        List<Task> tasksSecondCall = repository.findByIsDeletedFalse();
        assertSame(tasksFirstCall, tasksSecondCall); // Same object indicates cache hit

        // Verify cache
        assertNotNull(cacheManager.getCache("tasks").get("all"));
    }

    @Test
    void findByIdAndIsDeletedFalse_ReturnsCachedTask_WhenCalledMultipleTimes() {
        Task savedTask = repository.save(taskToSave1User1);

        // First call: Should hit database and cache
        Optional<Task> taskFirstCall = repository.findByIdAndIsDeletedFalse(savedTask.getId());
        assertTrue(taskFirstCall.isPresent());

        // Second call: Should hit cache
        Optional<Task> taskSecondCall = repository.findByIdAndIsDeletedFalse(savedTask.getId());
        assertSame(taskFirstCall, taskSecondCall); // Same object indicates cache hit

        // Verify cache
        assertNotNull(cacheManager.getCache("tasks").get(savedTask.getId().toString()));
    }

    @Test
    void findByUserIdAndIsDeletedFalse_ReturnsCachedTasks_WhenCalledMultipleTimes() {
        Task savedTask1 = repository.save(taskToSave1User1);
        repository.save(taskToSave2User1);

        // First call: Should hit database and cache
        List<Task> tasksFirstCall = repository.findByUserIdAndIsDeletedFalse(USER_ID_1);
        assertEquals(2, tasksFirstCall.size());

        // Second call: Should hit cache
        List<Task> tasksSecondCall = repository.findByUserIdAndIsDeletedFalse(USER_ID_1);
        assertSame(tasksFirstCall, tasksSecondCall); // Same object indicates cache hit

        // Verify cache
        assertNotNull(cacheManager.getCache("tasks").get("user_" + USER_ID_1));
    }

    @Test
    void findByUserIdAndStatusAndIsDeletedFalse_ReturnsCachedTasks_WhenCalledMultipleTimes() {
        Task savedTask1 = repository.save(taskToSave1User1);
        repository.save(taskToSave2User1);

        // First call: Should hit database and cache
        List<Task> tasksFirstCall = repository.findByUserIdAndStatusAndIsDeletedFalse(USER_ID_1, "pending");
        assertEquals(1, tasksFirstCall.size());

        // Second call: Should hit cache
        List<Task> tasksSecondCall = repository.findByUserIdAndStatusAndIsDeletedFalse(USER_ID_1, "pending");
        assertSame(tasksFirstCall, tasksSecondCall); // Same object indicates cache hit

        // Verify cache
        assertNotNull(cacheManager.getCache("tasks").get("user_" + USER_ID_1 + "_pending"));
    }

    @Test
    void save_NewTask_CachesTaskAndEvictsRelatedCaches() {
        Task savedTask = repository.save(taskToSave1User1);

        // Verify task is cached
        assertNotNull(cacheManager.getCache("tasks").get(savedTask.getId().toString()));

        // Verify related caches are evicted
        assertNull(cacheManager.getCache("tasks").get("all"));
        assertNull(cacheManager.getCache("tasks").get("user_" + USER_ID_1));
        assertNull(cacheManager.getCache("tasks").get("user_" + USER_ID_1 + "_pending"));
    }

    @Test
    void markAsDeleted_EvictsTaskFromCache() {
        Task savedTask = repository.save(taskToSave1User1);

        // Populate cache
        repository.findByIdAndIsDeletedFalse(savedTask.getId());
        assertNotNull(cacheManager.getCache("tasks").get(savedTask.getId().toString()));

        // Mark as deleted
        repository.markAsDeleted(savedTask.getId());

        // Verify task is evicted
        assertNull(cacheManager.getCache("tasks").get(savedTask.getId().toString()));
    }

    @Test
    void findByIsDeletedFalse_ReturnsOnlyNonDeletedTasks() {
        Task savedTask1 = repository.save(taskToSave1User1);
        Task savedTask2 = repository.save(taskToSave1User2);

        Task taskToDelete = Task.builder()
                .userId(USER_ID_1)
                .description("To Delete")
                .creationDate(LocalDateTime.now())
                .status("pending")
                .build();
        Task savedTaskToDelete = repository.save(taskToDelete);
        repository.markAsDeleted(savedTaskToDelete.getId());

        List<Task> receivedTasks = repository.findByIsDeletedFalse();

        assertNotNull(receivedTasks);
        assertEquals(2, receivedTasks.size());
        assertTrue(receivedTasks.stream()
                .anyMatch(t -> t.getId().equals(savedTask1.getId())));
        assertTrue(receivedTasks.stream()
                .anyMatch(t -> t.getId().equals(savedTask2.getId())));
        assertFalse(receivedTasks.stream()
                .anyMatch(t -> t.getId().equals(savedTaskToDelete.getId())));
    }

    @Test
    void findByIsDeletedFalse_ReturnsEmptyList_WhenAllTasksAreDeletedOrEmpty() {
        Task taskToDelete = Task.builder()
                .userId(USER_ID_1)
                .description("To Delete")
                .creationDate(LocalDateTime.now())
                .status("pending")
                .build();
        Task savedTaskToDelete = repository.save(taskToDelete);
        repository.markAsDeleted(savedTaskToDelete.getId());

        List<Task> receivedTasks = repository.findByIsDeletedFalse();
        assertNotNull(receivedTasks);
        assertTrue(receivedTasks.isEmpty());

        repository.deleteAll();
        receivedTasks = repository.findByIsDeletedFalse();
        assertNotNull(receivedTasks);
        assertTrue(receivedTasks.isEmpty());
    }

    @Test
    void findByIdAndIsDeletedFalse_ReturnsEmptyOptional_WhenDoesNotExist() {
        repository.save(taskToSave1User1);
        Optional<Task> foundTaskOpt = repository.findByIdAndIsDeletedFalse(NON_EXISTENT_TASK_ID);
        assertTrue(foundTaskOpt.isEmpty());
    }

    @Test
    void findByIdAndIsDeletedFalse_ReturnsEmptyOptional_WhenExistsButDeleted() {
        Task savedTask = repository.save(taskToSave1User1);
        repository.markAsDeleted(savedTask.getId());

        Optional<Task> foundTaskOpt = repository.findByIdAndIsDeletedFalse(savedTask.getId());
        assertTrue(foundTaskOpt.isEmpty());
    }

    @Test
    void findByIdAndIsDeletedFalse_ReturnsEmptyOptional_WhenTaskIdIsNull() {
        repository.save(taskToSave1User1);
        Optional<Task> foundTaskOpt = repository.findByIdAndIsDeletedFalse(null);
        assertTrue(foundTaskOpt.isEmpty());
    }

    @Test
    void findByUserIdAndIsDeletedFalse_ReturnsEmptyList_WhenUserHasNoTasksOrAllAreDeleted() {
        repository.save(taskToSave1User2);
        List<Task> user1Tasks = repository.findByUserIdAndIsDeletedFalse(USER_ID_1);
        assertNotNull(user1Tasks);
        assertTrue(user1Tasks.isEmpty());

        Task savedTask1 = repository.save(taskToSave1User1);
        repository.markAsDeleted(savedTask1.getId());
        user1Tasks = repository.findByUserIdAndIsDeletedFalse(USER_ID_1);
        assertNotNull(user1Tasks);
        assertTrue(user1Tasks.isEmpty());
    }

    @Test
    void findByUserIdAndIsDeletedFalse_ReturnsEmptyList_WhenUserIdIsNull() {
        repository.save(taskToSave1User1);
        List<Task> userTasks = repository.findByUserIdAndIsDeletedFalse(null);
        assertNotNull(userTasks);
        assertTrue(userTasks.isEmpty());
    }

    @Test
    void save_NewTask_AssignsIdAndReturnsSavedTask() {
        Task savedTask = repository.save(taskToSave1User1);

        assertNotNull(savedTask);
        assertNotNull(savedTask.getId());
        assertEquals(taskToSave1User1.getDescription(), savedTask.getDescription());
        assertEquals(USER_ID_1, savedTask.getUserId());
        assertNotNull(savedTask.getCreationDate());
        assertNotNull(savedTask.getStatus());
        assertNotNull(savedTask.getDeleted());
        assertFalse(savedTask.getDeleted());

        assertTrue(repository.existsByIdAndIsDeletedFalse(savedTask.getId()));
        assertEquals(Optional.of(savedTask), repository.findByIdAndIsDeletedFalse(savedTask.getId()));
    }

    @Test
    void save_NewTask_DoesNotThrowDuplicate_IfCalledMultipleTimesWithDifferentObjects() {
        Task savedTask1 = repository.save(taskToSave1User1);

        Task anotherTaskToSave = Task.builder()
                .userId(taskToSave1User1.getUserId())
                .description(taskToSave1User1.getDescription())
                .creationDate(LocalDateTime.now())
                .status("pending")
                .build();
        Task savedTask2 = repository.save(anotherTaskToSave);

        assertNotNull(savedTask1.getId());
        assertNotNull(savedTask2.getId());
        assertNotEquals(savedTask1.getId(), savedTask2.getId());
        assertEquals(2, repository.findByIsDeletedFalse().size());
    }

    @Test
    void save_ThrowsIllegalArgumentException_WhenTaskIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.save(null)
        );
        assertEquals("Task cannot be null", exception.getMessage());
    }

    @Test
    void save_ExistingTask_UpdatesAndReturnsTask() {
        Task savedTask = repository.save(taskToSave1User1);
        Long originalId = savedTask.getId();

        Task taskToUpdate = Task.builder()
                .id(originalId)
                .userId(savedTask.getUserId())
                .description("Updated Description")
                .creationDate(savedTask.getCreationDate())
                .status("completed")
                .deleted(savedTask.getDeleted())
                .build();

        Task updatedTask = repository.save(taskToUpdate);

        assertNotNull(updatedTask);
        assertEquals(originalId, updatedTask.getId());
        assertEquals("Updated Description", updatedTask.getDescription());
        assertEquals("completed", updatedTask.getStatus());

        Optional<Task> foundOpt = repository.findByIdAndIsDeletedFalse(originalId);
        assertTrue(foundOpt.isPresent());
        assertEquals("Updated Description", foundOpt.get().getDescription());
    }

    @Test
    void save_ExistingTask_CanMarkAsDeleted() {
        Task savedTask = repository.save(taskToSave1User1);
        Long originalId = savedTask.getId();

        Task taskToUpdate = Task.builder()
                .id(originalId)
                .userId(savedTask.getUserId())
                .description(savedTask.getDescription())
                .creationDate(savedTask.getCreationDate())
                .status(savedTask.getStatus())
                .deleted(true)
                .build();

        Task updatedTask = repository.save(taskToUpdate);

        assertNotNull(updatedTask);
        assertEquals(originalId, updatedTask.getId());
        assertTrue(updatedTask.getDeleted());

        assertFalse(repository.findByIdAndIsDeletedFalse(originalId).isPresent());
        assertFalse(repository.existsByIdAndIsDeletedFalse(originalId));
        assertTrue(repository.findByUserIdAndIsDeletedFalse(USER_ID_1).isEmpty());
        assertTrue(repository.findByIsDeletedFalse().isEmpty());

        assertTrue(repository.findById(originalId).isPresent());
        assertTrue(repository.existsById(originalId));
    }

    @Test
    void save_ExistingTask_ThrowsIllegalArgumentException_WhenUpdatingNonExistingTask() {
        Task nonExistentTaskToUpdate = Task.builder()
                .id(NON_EXISTENT_TASK_ID)
                .userId(USER_ID_1)
                .description("Ghost Task")
                .creationDate(LocalDateTime.now())
                .status("pending")
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.save(nonExistentTaskToUpdate)
        );
        assertTrue(exception.getMessage()
                .contains("Task with id " + NON_EXISTENT_TASK_ID + " not found for update via save."));
    }

    @Test
    void existsByIdAndIsDeletedFalse_ReturnsTrue_WhenExistsAndNotDeleted() {
        Task savedTask = repository.save(taskToSave1User1);
        assertTrue(repository.existsByIdAndIsDeletedFalse(savedTask.getId()));
    }

    @Test
    void existsByIdAndIsDeletedFalse_ReturnsFalse_WhenDoesNotExist() {
        assertFalse(repository.existsByIdAndIsDeletedFalse(NON_EXISTENT_TASK_ID));
    }

    @Test
    void existsByIdAndIsDeletedFalse_ReturnsFalse_WhenExistsButDeleted() {
        Task savedTask = repository.save(taskToSave1User1);
        repository.markAsDeleted(savedTask.getId());
        assertFalse(repository.existsByIdAndIsDeletedFalse(savedTask.getId()));
    }

    @Test
    void existsByIdAndIsDeletedFalse_ReturnsFalse_WhenTaskIdIsNull() {
        repository.save(taskToSave1User1);
        assertFalse(repository.existsByIdAndIsDeletedFalse(null));
    }

    @Test
    void findByUserIdAndStatusAndIsDeletedFalse_ReturnsOnlyMatchingTasks() {
        Task savedTask1 = repository.save(taskToSave1User1);
        repository.save(taskToSave2User1);
        repository.save(taskToSave1User2);

        List<Task> pendingTasks = repository.findByUserIdAndStatusAndIsDeletedFalse(USER_ID_1, "pending");
        assertNotNull(pendingTasks);
        assertEquals(1, pendingTasks.size());
        assertTrue(pendingTasks.stream()
                .anyMatch(t -> t.getId().equals(savedTask1.getId())));
    }
}
```
package com.example.taskapp.repository;

import com.example.taskapp.model.Task;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("postgres")
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaTaskRepositoryTest {

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
    private JpaTaskRepository repository;

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
    }

    // findByIsDeletedFalse
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

    // findByIdAndIsDeletedFalse
    @Test
    void findByIdAndIsDeletedFalse_ReturnsOptionalWithTask_WhenExistsAndNotDeleted() {
        Task savedTask = repository.save(taskToSave1User1);
        Optional<Task> foundTaskOpt = repository.findByIdAndIsDeletedFalse(savedTask.getId());
        assertTrue(foundTaskOpt.isPresent());
        assertEquals(savedTask.getDescription(), foundTaskOpt.get().getDescription());
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

    // findByUserIdAndIsDeletedFalse
    @Test
    void findByUserIdAndIsDeletedFalse_ReturnsOnlyNonDeletedUserTasks() {
        Task savedTask1 = repository.save(taskToSave1User1);
        Task savedTask2 = repository.save(taskToSave2User1);
        repository.save(taskToSave1User2);

        repository.markAsDeleted(savedTask2.getId());

        List<Task> user1Tasks = repository.findByUserIdAndIsDeletedFalse(USER_ID_1);
        assertNotNull(user1Tasks);
        assertEquals(1, user1Tasks.size());
        assertTrue(user1Tasks.stream()
                .anyMatch(t -> t.getId().equals(savedTask1.getId())));
        assertFalse(user1Tasks.stream()
                .anyMatch(t -> t.getId().equals(savedTask2.getId())));
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

    // save
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

    // existsByIdAndIsDeletedFalse
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

    // findByUserIdAndStatusAndIsDeletedFalse
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
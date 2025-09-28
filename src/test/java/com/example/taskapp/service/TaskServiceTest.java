package com.example.taskapp.service;

import com.example.taskapp.model.Task;
import com.example.taskapp.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class TaskServiceTest {
    @Mock
    private TaskRepository taskRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        taskService = new TaskService(taskRepository); // Real TaskService, mocked repository
    }

    @Test
    void getAllTasks() {
        Long userId = 1L;
        when(taskRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());
        List<Task> tasks = taskService.getAllTasks(userId);
        assertEquals(0, tasks.size());
        verify(taskRepository, times(1)).findAllByUserId(userId);
    }

    @Test
    void getPendingTasks() {
        Long userId = 1L;
        when(taskRepository.findPendingByUserId(userId)).thenReturn(Collections.emptyList());
        List<Task> tasks = taskService.getPendingTasks(userId);
        assertEquals(0, tasks.size());
        verify(taskRepository, times(1)).findPendingByUserId(userId);
    }

    @Test
    void createTask() {
        Task task = new Task();
        task.setDescription("Test Task");
        task.setUserId(1L);
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        Task created = taskService.createTask(task);
        assertEquals("pending", created.getStatus());
        assertEquals("Test Task", created.getDescription());
        assertNotNull(created.getCreationDate());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void deleteTask() {
        Long taskId = 1L;
        taskService.deleteTask(taskId);
        verify(taskRepository, times(1)).markAsDeleted(taskId);
    }
}
package com.example.taskapp.controller;

import com.example.taskapp.model.Task;
import com.example.taskapp.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskService taskService; // Injected from TestConfiguration

    @Configuration
    static class TestConfig {
        @Bean
        TaskService taskService() {
            return Mockito.mock(TaskService.class); // Manual mock instead of @MockBean
        }
    }

    @Test
    void getAllTasks() throws Exception {
        when(taskService.getAllTasks(1L)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/tasks?userId=1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getPendingTasks() throws Exception {
        when(taskService.getPendingTasks(1L)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/tasks/pending?userId=1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void createTask() throws Exception {
        Task task = new Task();
        task.setDescription("Test Task");
        task.setUserId(1L);
        task.setCreationDate(LocalDateTime.now());
        task.setStatus("pending");
        when(taskService.createTask(any(Task.class))).thenReturn(task);
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Test Task\",\"userId\":1}"))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteTask() throws Exception {
        mockMvc.perform(delete("/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        Mockito.verify(taskService, Mockito.times(1)).deleteTask(1L);
    }
}
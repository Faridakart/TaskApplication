package com.example.taskapp.controller;

import com.example.taskapp.model.Notification;
import com.example.taskapp.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.Mockito;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationService notificationService;

    @Configuration
    static class TestConfig {
        @Bean
        NotificationService notificationService() {
            return Mockito.mock(NotificationService.class);
        }
    }

    @Test
    void getAllNotifications() throws Exception {
        when(notificationService.getAllNotifications(1L)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/notifications?userId=1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getPendingNotifications() throws Exception {
        when(notificationService.getPendingNotifications(1L)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/notifications/pending?userId=1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
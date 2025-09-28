package com.example.taskapp.listener;

import com.example.taskapp.config.RabbitMQConfig;
import com.example.taskapp.model.Task;
import com.example.taskapp.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("postgres")
public class TaskCreationListener {

    private final NotificationService notificationService;

    public TaskCreationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.TASK_QUEUE)
    public void handleTaskCreated(Task task) {
        String message = "New task created: " + task.getDescription();
        notificationService.createNotification(task.getUserId(), message);
    }
}
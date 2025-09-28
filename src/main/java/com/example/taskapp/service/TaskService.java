package com.example.taskapp.service;

import com.example.taskapp.model.Task;
import com.example.taskapp.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {
    private final TaskRepository taskRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> getAllTasks(Long userId) {
        return taskRepository.findAllByUserId(userId);
    }

    public List<Task> getPendingTasks(Long userId) {
        return taskRepository.findPendingByUserId(userId);
    }

    public Task createTask(Task task) {
        task.setCreationDate(LocalDateTime.now());
        task.setStatus("pending");
        return taskRepository.save(task);
    }

    public void deleteTask(Long id) {
        taskRepository.markAsDeleted(id);
    }
}
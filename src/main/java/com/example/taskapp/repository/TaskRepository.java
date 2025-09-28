package com.example.taskapp.repository;

import com.example.taskapp.model.Task;

import java.util.List;

public interface TaskRepository {
    List<Task> findAllByUserId(Long userId);
    List<Task> findPendingByUserId(Long userId);
    Task save(Task task);
    void markAsDeleted(Long id);
}
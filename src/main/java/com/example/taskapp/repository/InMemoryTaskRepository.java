package com.example.taskapp.repository;

import com.example.taskapp.model.Task;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
@Profile("inmemory")
public class InMemoryTaskRepository implements TaskRepository {
    private final List<Task> tasks = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public List<Task> findAllByUserId(Long userId) {
        return tasks.stream()
                .filter(t -> t.getUserId().equals(userId) && !"deleted".equals(t.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findPendingByUserId(Long userId) {
        return tasks.stream()
                .filter(t -> t.getUserId().equals(userId) && "pending".equals(t.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public Task save(Task task) {
        if (task.getId() == null) {
            task.setId(idGenerator.getAndIncrement());
        }
        tasks.add(task);
        return task;
    }

    @Override
    public void markAsDeleted(Long id) {
        tasks.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .ifPresent(t -> t.setStatus("deleted"));
    }
}
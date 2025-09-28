package com.example.taskapp.repository;

import com.example.taskapp.model.Task;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("postgres")
public interface JpaTaskRepository extends TaskRepository, JpaRepository<Task, Long> {
    @Override
    default List<Task> findAllByUserId(Long userId) {
        return findByUserIdAndIsDeletedFalse(userId);
    }

    List<Task> findByUserIdAndIsDeletedFalse(Long userId);

    @Override
    default List<Task> findPendingByUserId(Long userId) {
        return findByUserIdAndStatusAndIsDeletedFalse(userId, "pending");
    }

    List<Task> findByUserIdAndStatusAndIsDeletedFalse(Long userId, String status);

    @Override
    default Task save(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (task.getId() != null && !existsById(task.getId())) {
            throw new IllegalArgumentException("Task with id " + task.getId() + " not found for update via save.");
        }
        return saveAndFlush(task);
    }

    @Override
    @Modifying
    @Query("UPDATE Task t SET t.deleted = true WHERE t.id = :id")
    void markAsDeleted(Long id);

    List<Task> findByIsDeletedFalse();

    Optional<Task> findByIdAndIsDeletedFalse(Long id);

    boolean existsByIdAndIsDeletedFalse(Long id);
}
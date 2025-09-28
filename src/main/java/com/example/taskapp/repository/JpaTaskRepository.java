package com.example.taskapp.repository;

import com.example.taskapp.model.Task;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
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
    @Cacheable(value = "tasks", key = "'all'")
    List<Task> findByIsDeletedFalse();

    @Override
    @Cacheable(value = "tasks", key = "#id")
    Optional<Task> findByIdAndIsDeletedFalse(Long id);

    @Override
    @Cacheable(value = "tasks", key = "'user_' + #userId")
    default List<Task> findAllByUserId(Long userId) {
        return findByUserIdAndIsDeletedFalse(userId);
    }

    @Cacheable(value = "tasks", key = "'user_' + #userId")
    List<Task> findByUserIdAndIsDeletedFalse(Long userId);

    @Override
    @Cacheable(value = "tasks", key = "'user_' + #userId + '_pending'")
    default List<Task> findPendingByUserId(Long userId) {
        return findByUserIdAndStatusAndIsDeletedFalse(userId, "pending");
    }

    @Cacheable(value = "tasks", key = "'user_' + #userId + '_' + #status")
    List<Task> findByUserIdAndStatusAndIsDeletedFalse(Long userId, String status);

    @Override
    @CachePut(value = "tasks", key = "#result.id", condition = "#result != null && !#result.deleted")
    default Task save(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        if (task.getId() != null && !existsById(task.getId())) {
            throw new IllegalArgumentException("Task with id " + task.getId() + " not found for update via save.");
        }
        Task savedTask = saveAndFlush(task);
        // Evict related caches to ensure consistency
        evictCaches(savedTask.getUserId());
        return savedTask;
    }

    @CacheEvict(value = "tasks", key = "#id")
    @Modifying
    @Query("UPDATE Task t SET t.deleted = true WHERE t.id = :id")
    void markAsDeleted(Long id);

    @Cacheable(value = "tasks", key = "#id + '_exists'")
    boolean existsByIdAndIsDeletedFalse(Long id);

    // Helper method to evict related caches
    @CacheEvict(value = "tasks", allEntries = true)
    default void evictCaches(Long userId) {
        // Evicts all task-related caches to ensure consistency
    }
}
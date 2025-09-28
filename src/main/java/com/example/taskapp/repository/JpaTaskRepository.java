package com.example.taskapp.repository;

import com.example.taskapp.model.Task;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("h2")
public interface JpaTaskRepository extends TaskRepository, JpaRepository<Task, Long> {
    @Override
    default List<Task> findAllByUserId(Long userId) {
        return findByUserIdAndStatusNot(userId, "deleted");
    }

    List<Task> findByUserIdAndStatusNot(Long userId, String status);

    @Override
    default List<Task> findPendingByUserId(Long userId) {
        return findByUserIdAndStatus(userId, "pending");
    }

    List<Task> findByUserIdAndStatus(Long userId, String status);

    @Override
    default void markAsDeleted(Long id) {
        findById(id).ifPresent(t -> {
            t.setStatus("deleted");
            save(t);
        });
    }
}
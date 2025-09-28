package com.example.taskapp.repository;

import com.example.taskapp.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    User save(User user);
}
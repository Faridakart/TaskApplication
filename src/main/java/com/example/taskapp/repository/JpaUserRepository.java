package com.example.taskapp.repository;

import com.example.taskapp.model.User;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("h2")
public interface JpaUserRepository extends UserRepository, JpaRepository<User, Long> {
    @Override
    default Optional<User> findByUsername(String username) {
        return findByUsernameIgnoreCase(username);
    }

    Optional<User> findByUsernameIgnoreCase(String username);

    @Override
    default User save(User user) {
        return saveAndFlush(user);
    }
}
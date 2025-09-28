```java
        package com.example.taskapp.repository;

import com.example.taskapp.model.User;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("postgres")
public interface JpaUserRepository extends UserRepository, JpaRepository<User, Long> {
    @Override
    default Optional<User> findByUsername(String username) {
        return findByUsernameIgnoreCase(username);
    }

    Optional<User> findByUsernameIgnoreCase(String username);

    @Override
    default User save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getId() != null && !existsById(user.getId())) {
            throw new IllegalArgumentException("User with id " + user.getId() + " not found for update via save.");
        }
        return saveAndFlush(user);
    }
}
```
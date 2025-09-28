package com.example.taskapp.repository;

import com.example.taskapp.model.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("postgres")
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaUserRepositoryTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("taskdb")
            .withUsername("postgres")
            .withPassword("postgres");

    @BeforeAll
    static void beforeAll() {
        postgres.start();
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());
    }

    @Autowired
    private JpaUserRepository repository;

    private User userToSave1;
    private User userToSave2;
    private static final String USERNAME_1 = "user1";
    private static final String USERNAME_2 = "user2";
    private static final String NON_EXISTENT_USERNAME = "nonexistent";

    @BeforeEach
    void setUp() {
        userToSave1 = User.builder()
                .username(USERNAME_1)
                .build();

        userToSave2 = User.builder()
                .username(USERNAME_2)
                .build();
    }

    @Test
    void findByUsername_ReturnsOptionalWithUser_WhenExists() {
        User savedUser = repository.save(userToSave1);
        Optional<User> foundUserOpt = repository.findByUsername(USERNAME_1);
        assertTrue(foundUserOpt.isPresent());
        assertEquals(USERNAME_1, foundUserOpt.get().getUsername());
    }

    @Test
    void findByUsername_ReturnsEmptyOptional_WhenDoesNotExist() {
        repository.save(userToSave1);
        Optional<User> foundUserOpt = repository.findByUsername(NON_EXISTENT_USERNAME);
        assertTrue(foundUserOpt.isEmpty());
    }

    @Test
    void findByUsername_ReturnsEmptyOptional_WhenUsernameIsNull() {
        repository.save(userToSave1);
        Optional<User> foundUserOpt = repository.findByUsername(null);
        assertTrue(foundUserOpt.isEmpty());
    }

    @Test
    void save_NewUser_AssignsIdAndReturnsSavedUser() {
        User savedUser = repository.save(userToSave1);

        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());
        assertEquals(USERNAME_1, savedUser.getUsername());

        assertTrue(repository.existsById(savedUser.getId()));
        assertEquals(Optional.of(savedUser), repository.findById(savedUser.getId()));
    }

    @Test
    void save_NewUser_DoesNotThrowDuplicate_IfCalledMultipleTimesWithDifferentObjects() {
        User savedUser1 = repository.save(userToSave1);

        User anotherUserToSave = User.builder()
                .username(USERNAME_1)
                .build();
        User savedUser2 = repository.save(anotherUserToSave);

        assertNotNull(savedUser1.getId());
        assertNotNull(savedUser2.getId());
        assertNotEquals(savedUser1.getId(), savedUser2.getId());
        assertEquals(2, repository.findAll().size());
    }

    @Test
    void save_ThrowsIllegalArgumentException_WhenUserIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.save(null)
        );
        assertEquals("User cannot be null", exception.getMessage());
    }

    @Test
    void save_ExistingUser_UpdatesAndReturnsUser() {
        User savedUser = repository.save(userToSave1);
        Long originalId = savedUser.getId();

        User userToUpdate = User.builder()
                .id(originalId)
                .username("updatedUser")
                .build();

        User updatedUser = repository.save(userToUpdate);

        assertNotNull(updatedUser);
        assertEquals(originalId, updatedUser.getId());
        assertEquals("updatedUser", updatedUser.getUsername());

        Optional<User> foundOpt = repository.findById(originalId);
        assertTrue(foundOpt.isPresent());
        assertEquals("updatedUser", foundOpt.get().getUsername());
    }

    @Test
    void save_ExistingUser_ThrowsIllegalArgumentException_WhenUpdatingNonExistingUser() {
        User nonExistentUserToUpdate = User.builder()
                .id(999L)
                .username("ghostUser")
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.save(nonExistentUserToUpdate)
        );
        assertTrue(exception.getMessage()
                .contains("User with id 999 not found for update via save."));
    }
}
package io.repository;

import io.model.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
@Transactional
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindById_ShouldSaveAndReturnUser() {

        User user = new User();
        user.setLogin("testuser");
        user.setPassword("password123");

        userRepository.save(user);

        assertNotNull(user.getId());

        Optional<User> foundUserOpt = userRepository.findById(user.getId());

        assertTrue(foundUserOpt.isPresent());
        assertEquals("testuser", foundUserOpt.get().getLogin());
    }

    @Test
    void findByLogin_WhenUserExists_ShouldReturnUser() {

        User user = new User();
        user.setLogin("existing_user");
        user.setPassword("password");
        userRepository.save(user);

        Optional<User> foundUserOpt = userRepository.findByLogin("existing_user");

        assertTrue(foundUserOpt.isPresent());
        assertEquals("existing_user", foundUserOpt.get().getLogin());
    }

    @Test
    void findByLogin_WhenUserDoesNotExist_ShouldReturnEmpty() {

        Optional<User> foundUserOpt = userRepository.findByLogin("non_existing_user");

        assertTrue(foundUserOpt.isEmpty());
    }
}
package io.service;

import io.model.entity.User;
import io.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User registerUser(String login, String password, String confirmPassword) {

        String trimmedLogin = login.trim();
        String lowerCaseLogin = trimmedLogin.toLowerCase();

        if (trimmedLogin.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }

        if (userRepository.findByLogin(lowerCaseLogin).isPresent()) {
            throw new IllegalArgumentException("User with this username already exists.");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        User user = new User();
        user.setLogin(lowerCaseLogin);
        user.setPassword(password.trim());

        return create(user);
    }

    private User create(User user) {
        String salt = BCrypt.gensalt();
        String hashed = BCrypt.hashpw(user.getPassword(), salt);
        user.setPassword(hashed);

        return userRepository.save(user);
    }

    public User login(String login, String password) {

        String normalizedLogin = login.trim().toLowerCase();

        User user = userRepository.findByLogin(normalizedLogin.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));

        if (!checkPassword(password.trim(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }
        return user;
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public boolean checkPassword(String plainPassword, String hashedPass) {
        return BCrypt.checkpw(plainPassword, hashedPass);
    }
}

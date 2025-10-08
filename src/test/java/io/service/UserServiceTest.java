package io.service;

import io.model.entity.User;
import io.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private static final String TEST_LOGIN = "testuser";
    private static final String TEST_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
    }

    @Test
    void create_ShouldReturnSavedUser_WhenValidUserProvided() {

        when(userRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });

        User result = userService.registerUser(TEST_LOGIN, TEST_PASSWORD, TEST_PASSWORD);

        assertThat(result).isNotNull();
        assertThat(result.getLogin()).isEqualTo(TEST_LOGIN);
        assertThat(result.getPassword()).isNotEqualTo(TEST_PASSWORD);
        assertThat(result.getPassword()).startsWith("$2a$");

        verify(userRepository).findByLogin(TEST_LOGIN);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_ShouldThrowException_WhenLoginIsEmpty() {

        User user = new User();
        user.setLogin("");
        user.setPassword(TEST_PASSWORD);

        assertThatThrownBy(() -> userService.registerUser("", TEST_PASSWORD, TEST_PASSWORD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username cannot be empty.");
        verifyNoInteractions(userRepository);
    }

    @Test
    void create_ShouldThrowException_WhenLoginAlreadyExists() {

        User userToCreate = new User();
        userToCreate.setLogin(TEST_LOGIN);
        userToCreate.setPassword(TEST_PASSWORD);

        when(userRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.of(userToCreate));

        assertThatThrownBy(() -> userService.registerUser(TEST_LOGIN, TEST_PASSWORD, TEST_PASSWORD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User with this username already exists.");

        verify(userRepository).findByLogin(TEST_LOGIN);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_ShouldReturnUser_WhenPasswordsMatch() {

        when(userRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });

        User result = userService.registerUser(TEST_LOGIN, TEST_PASSWORD, TEST_PASSWORD);

        assertThat(result).isNotNull();
        assertThat(result.getLogin()).isEqualTo(TEST_LOGIN);
        assertThat(result.getPassword()).startsWith("$2a$");

        verify(userRepository).findByLogin(TEST_LOGIN);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_ShouldThrowException_WhenPasswordsDoNotMatch() {

        String confirmPassword = "differentPassword";

        assertThatThrownBy(() -> userService.registerUser(TEST_LOGIN, TEST_PASSWORD, confirmPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passwords do not match.");

    }

    @Test
    void login_ShouldReturnUser_WhenCredentialsAreValid() {

        String hashedPassword = BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt());

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setLogin(TEST_LOGIN);
        existingUser.setPassword(hashedPassword);

        when(userRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.of(existingUser));

        User result = userService.login(TEST_LOGIN, TEST_PASSWORD);

        assertThat(result).isNotNull();
        assertThat(result.getLogin()).isEqualTo(TEST_LOGIN);
        assertThat(result.getId()).isEqualTo(1L);

        verify(userRepository).findByLogin(TEST_LOGIN);
    }

    @Test
    void login_ShouldThrowException_WhenUserDoesNotExist() {

        when(userRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(TEST_LOGIN, TEST_PASSWORD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username or password.");

        verify(userRepository).findByLogin(TEST_LOGIN);
    }

    @Test
    void login_ShouldThrowException_WhenPasswordIsIncorrect() {

        String correctPassword = "correctPassword123";
        String hashedPassword = BCrypt.hashpw(correctPassword, BCrypt.gensalt());

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setLogin(TEST_LOGIN);
        existingUser.setPassword(hashedPassword);

        when(userRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userService.login(TEST_LOGIN, "wrongPassword"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username or password.");

        verify(userRepository).findByLogin(TEST_LOGIN);
    }

    @Test
    void checkPassword_ShouldReturnTrue_WhenPasswordIsValid() {

        String plainPassword = "testPassword123";
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());

        boolean result = userService.checkPassword(plainPassword, hashedPassword);

        assertThat(result).isTrue();
    }

    @Test
    void checkPassword_ShouldReturnFalse_WhenPasswordIsInvalid() {

        String correctPassword = "correctPassword123";
        String wrongPassword = "wrongPassword123";
        String hashedPassword = BCrypt.hashpw(correctPassword, BCrypt.gensalt());

        boolean result = userService.checkPassword(wrongPassword, hashedPassword);

        assertThat(result).isFalse();
    }

    @Test
    void registerUser_ShouldNormalizeLogin() {

        String loginWithSpaces = "  TESTUSER  ";
        String normalizedLogin = "testuser";

        when(userRepository.findByLogin(normalizedLogin)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });

        userService.registerUser(loginWithSpaces, TEST_PASSWORD, TEST_PASSWORD);

        verify(userRepository).findByLogin(normalizedLogin);
        verify(userRepository).save(argThat(user ->
                normalizedLogin.equals(user.getLogin())
        ));
    }

    @Test
    void create_ShouldHashPassword_BeforeSaving() {

        when(userRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });

        userService.registerUser(TEST_LOGIN, TEST_PASSWORD, TEST_PASSWORD);

        verify(userRepository).save(argThat(savedUser -> {
            String password = savedUser.getPassword();
            return password != null &&
                    password.startsWith("$2a$") &&
                    password.length() == 60;
        }));
    }
}

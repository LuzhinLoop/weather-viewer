package io.service;

import io.model.entity.Session;
import io.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService Unit Tests")
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private SessionService sessionService;

    private static final Long TEST_USER_ID = 1L;
    private static final Long DEFAULT_SESSION_TTL_HOURS = 24L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sessionService, "defaultSessionTtlHours", DEFAULT_SESSION_TTL_HOURS);
    }

    @Test
    void createForUser_ShouldReturnValidToken_WhenValidUserId() {

        doAnswer(invocation -> {
            Session session = invocation.getArgument(0);
            session.setId(UUID.randomUUID());
            return null;
        }).when(sessionRepository).save(any(Session.class));

        UUID result = sessionService.createForUser(TEST_USER_ID);

        assertThat(result).isNotNull();
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void createForUser_ShouldThrowException_WhenUserIdIsNull() {

        assertThatThrownBy(() -> sessionService.createForUser(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("userID must not be null");

        verifyNoInteractions(sessionRepository);
    }

    @Test
    void getUserIdByValidToken_ShouldReturnUserId_WhenTokenIsValid() {

        UUID validToken = UUID.randomUUID();
        Session session = new Session(TEST_USER_ID, Timestamp.from(Instant.now().plus(Duration.ofHours(1))));
        when(sessionRepository.findValidSessionById(validToken)).thenReturn(Optional.of(session));

        Optional<Long> result = sessionService.getUserIdByValidToken(validToken);

        assertThat(result).isPresent().contains(TEST_USER_ID);
        verify(sessionRepository).findValidSessionById(validToken);
    }

    @Test
    void getUserIdByValidToken_ShouldReturnEmpty_WhenTokenIsInvalid() {

        UUID invalidToken = UUID.randomUUID();
        when(sessionRepository.findValidSessionById(invalidToken)).thenReturn(Optional.empty());

        Optional<Long> result = sessionService.getUserIdByValidToken(invalidToken);

        assertThat(result).isEmpty();
        verify(sessionRepository).findValidSessionById(invalidToken);
    }

    @Test
    @DisplayName("Получение userId по null токену - должен вернуть пустой Optional")
    void getUserIdByValidToken_ShouldReturnEmpty_WhenTokenIsNull() {

        Optional<Long> result = sessionService.getUserIdByValidToken(null);

        assertThat(result).isEmpty();
        verifyNoInteractions(sessionRepository);
    }

    @Test
    @DisplayName("Выход из системы - должен удалить сессию")
    void logout_ShouldDeleteSession_WhenValidToken() {

        UUID token = UUID.randomUUID();

        sessionService.logout(token);

        verify(sessionRepository).deleteById(token);
    }

    @Test
    @DisplayName("Выход из системы с null токеном - не должен вызывать репозиторий")
    void logout_ShouldNotCallRepository_WhenTokenIsNull() {

        sessionService.logout(null);

        verifyNoInteractions(sessionRepository);
    }

    @Test
    @DisplayName("Очистка истекших сессий - должен вернуть количество удаленных сессий")
    void cleanUpExpiredSession_ShouldReturnDeletedCount() {

        int expectedDeletedCount = 5;
        when(sessionRepository.deleteAllExpired()).thenReturn(expectedDeletedCount);

        int result = sessionService.cleanUpExpiredSession();

        assertThat(result).isEqualTo(expectedDeletedCount);
        verify(sessionRepository).deleteAllExpired();
    }

    @Test
    @DisplayName("Создание сессии - должна иметь правильное время истечения")
    void createForUser_ShouldSetCorrectExpirationTime() {

        Instant beforeCreation = Instant.now();
        doAnswer(invocation -> {
            return null;

        }).when(sessionRepository).save(any(Session.class));

        sessionService.createForUser(TEST_USER_ID);

        verify(sessionRepository).save(argThat(session -> {
            Instant afterCreation = Instant.now();
            Duration expectedTtl = Duration.ofHours(DEFAULT_SESSION_TTL_HOURS);
            
            Instant expectedMinExpiry = beforeCreation.plus(expectedTtl);
            Instant expectedMaxExpiry = afterCreation.plus(expectedTtl);
            
            return session.getExpiresAt().toInstant().isAfter(expectedMinExpiry) &&
                   session.getExpiresAt().toInstant().isBefore(expectedMaxExpiry.plusSeconds(1));
        }));
    }
}

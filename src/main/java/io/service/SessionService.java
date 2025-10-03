package io.service;

import io.model.entity.Session;
import io.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;

    @Value("${server.session.ttl-hours:24}")
    private long defaultSessionTtlHours;

    public UUID createForUser(Long userId) {
        Objects.requireNonNull(userId, "userID must not be null");

        var now = Instant.now();
        var ttl = Duration.ofHours(defaultSessionTtlHours);
        var expiresAt = Timestamp.from(now.plus(ttl));

        var session = new Session(userId, expiresAt);
        sessionRepository.save(session);

        return session.getId();
    }

    public Optional<Long> getUserIdByValidToken(UUID token) {
        if (token == null) {
            return Optional.empty();
        }
        return sessionRepository.findValidSessionById(token)
                .map(Session::getUserId);
    }

    public void logout(UUID token) {
        if (token == null) return;
        sessionRepository.deleteById(token);
    }

    public int cleanUpExpiredSession() {
        return sessionRepository.deleteAllExpired();
    }
}

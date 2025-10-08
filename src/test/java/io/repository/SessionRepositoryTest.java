package io.repository;

import io.model.entity.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
@Transactional
public class SessionRepositoryTest {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private SessionFactory sessionFactory;

    @Test
    void saveAndFindValidSessionById_ShouldReturnValidSession() {
        Session session = new Session();
        session.setUserId(1L);
        session.setExpiresAt(Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)));

        sessionRepository.save(session);
        assertNotNull(session.getId());

        Optional<Session> foundSession = sessionRepository.findValidSessionById(session.getId());

        assertTrue(foundSession.isPresent());
        assertEquals(session.getId(), foundSession.get().getId());
    }

    @Test
    void findValidSessionById_ShouldReturnEmptyForExpiredSession() {
        Session session = new Session();
        session.setUserId(1L);
        session.setExpiresAt(Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)));

        sessionRepository.save(session);
        assertNotNull(session.getId());

        Optional<Session> foundSession = sessionRepository.findValidSessionById(session.getId());

        assertTrue(foundSession.isEmpty());
    }

    @Test
    void deleteById_ShouldDeleteSession() {
        Session session = new Session();

        session.setUserId(1L);
        session.setExpiresAt(Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)));

        sessionRepository.save(session);
        UUID sessionId = session.getId();
        assertNotNull(sessionId);

        sessionRepository.deleteById(sessionId);
        sessionFactory.getCurrentSession().clear();

        Session deletedSession = sessionFactory.getCurrentSession().get(Session.class, sessionId);
        assertNull(deletedSession);
    }

    @Test
    void deleteAllExpired_ShouldDeleteOnlyExpiredSessions() {
        Session expiredSession1 = new Session();
        expiredSession1.setUserId(1L);
        expiredSession1.setExpiresAt(Timestamp.from(Instant.now().minus(2, ChronoUnit.HOURS)));

        Session expiredSession2 = new Session();
        expiredSession2.setUserId(2L);
        expiredSession2.setExpiresAt(Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)));

        Session validSession = new Session();
        validSession.setUserId(3L);
        validSession.setExpiresAt(Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)));

        sessionRepository.save(expiredSession1);
        sessionRepository.save(expiredSession2);
        sessionRepository.save(validSession);

        int deletedCount = sessionRepository.deleteAllExpired();
        assertEquals(2, deletedCount);

        sessionFactory.getCurrentSession().clear();

        Session foundExpired1 = sessionFactory.getCurrentSession().get(Session.class, expiredSession1.getId());
        Session foundExpired2 = sessionFactory.getCurrentSession().get(Session.class, expiredSession2.getId());
        Session foundValid = sessionFactory.getCurrentSession().get(Session.class, validSession.getId());

        assertNull(foundExpired1);
        assertNull(foundExpired2);
        assertNotNull(foundValid);
    }
}
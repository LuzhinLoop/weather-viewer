package io.repository;

import io.model.entity.Session;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SessionRepository {

    private final SessionFactory sessionFactory;

    private org.hibernate.Session session() {
        return sessionFactory.getCurrentSession();
    }

    public void save(Session entity) {
        session().persist(entity);
    }

    public Optional<Session> findValidSessionById(UUID id) {
        String hql = """
                FROM Session s
                WHERE s.id = :id

                AND s.expiresAt > :now
                """;

        Session result = session()
                .createQuery(hql, Session.class)
                .setParameter("id", id)
                .setParameter("now", Timestamp.from(Instant.now()))
                .uniqueResult();
        return Optional.ofNullable(result);
    }

    public void deleteById(UUID id) {
        String hql = " DELETE FROM Session s WHERE s.id = :id ";
        session()
                .createMutationQuery(hql)
                .setParameter("id", id)
                .executeUpdate();
    }

    public int deleteAllExpired() {
        String hql = "DELETE FROM Session s WHERE expiresAt <= :now";
        return session().createMutationQuery(hql)
                .setParameter("now", Timestamp.from(Instant.now()))
                .executeUpdate();
    }
}

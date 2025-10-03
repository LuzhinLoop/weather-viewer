package io.repository;

import io.model.entity.Location;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class LocationRepository {

    private final SessionFactory sessionFactory;

    private Session session() {
        return sessionFactory.getCurrentSession();
    }

    public void save(Location location) {
        session().persist(location);
    }

    public int countByUser(Long userId) {
        String hql = """
                SELECT count(l)
                FROM Location l
                WHERE l.userId = :userId
                """;
        Long count = session().createQuery(hql, Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
        return Math.toIntExact(count);
    }

    public boolean existsByUserAndCoords(Long userId, double lat, double lon) {
        String hql = """
                SELECT 1
                FROM Location l
                WHERE l.userId = :userId
                AND l.latitude = :lat
                AND l.longitude = :lon
                """;

        var results = session().createQuery(hql, Integer.class)
                .setParameter("userId", userId)
                .setParameter("lat", lat)
                .setParameter("lon", lon)
                .setMaxResults(1)
                .getResultList();

        return !results.isEmpty();
    }

    public List<Location> findAllByUserId(Long userId) {
        String hql = """
                FROM Location l
                WHERE l.userId = :userId
                ORDER BY l.name ASC
                """;
        return session()
                .createQuery(hql, Location.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public int deleteLocationByUserId(Long userId, Long locationId) {
        String hql = """
                DELETE FROM Location l
                WHERE l.id = :locationId
                AND l.userId = :userId
                """;
        return session().createMutationQuery(hql)
                .setParameter("locationId", locationId)
                .setParameter("userId", userId)
                .executeUpdate();
    }
}

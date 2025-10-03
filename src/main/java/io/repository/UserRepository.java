package io.repository;

import io.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final SessionFactory sessionFactory;

    private org.hibernate.Session session() {
        return sessionFactory.getCurrentSession();
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(session().get(User.class, id));
    }

    public User save(User user) {
        session().persist(user);
        return user;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByLogin(String login) {
        String hql = " FROM User WHERE login = :login ";
        return session()
                .createQuery(hql, User.class)
                .setParameter("login", login)
                .uniqueResultOptional();
    }
}

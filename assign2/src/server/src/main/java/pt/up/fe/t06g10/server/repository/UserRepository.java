package pt.up.fe.t06g10.server.repository;

import org.hibernate.Session;
import org.hibernate.Transaction;
import pt.up.fe.t06g10.server.database.EntityManagerFactoryProvider;
import pt.up.fe.t06g10.server.entity.UserEntity;

import java.util.Optional;

public class UserRepository {
    public Optional<UserEntity> findByUsername(String username) {
        try (Session session = EntityManagerFactoryProvider.openSession()) {
            UserEntity user = session.createQuery(
                            "from UserEntity where username = :username",
                            UserEntity.class
                    )
                    .setParameter("username", username)
                    .uniqueResult();
            return Optional.ofNullable(user);
        }
    }

    public boolean existsByUsername(String username) {
        try (Session session = EntityManagerFactoryProvider.openSession()) {
            Long count = session.createQuery(
                            "select count(u) from UserEntity u where u.username = :username",
                            Long.class
                    )
                    .setParameter("username", username)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    public void save(UserEntity user) {
        Transaction transaction = null;
        try (Session session = EntityManagerFactoryProvider.openSession()) {
            transaction = session.beginTransaction();
            session.persist(user);
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }
}

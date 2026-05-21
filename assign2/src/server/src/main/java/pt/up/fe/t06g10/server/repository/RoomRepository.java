package pt.up.fe.t06g10.server.repository;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import pt.up.fe.t06g10.server.database.EntityManagerFactoryProvider;
import pt.up.fe.t06g10.server.entity.RoomEntity;

import java.util.List;
import java.util.Optional;

public class RoomRepository {
    public Optional<RoomEntity> findByName(String name) {
        try (Session session = EntityManagerFactoryProvider.openSession()) {
            RoomEntity room = session.createQuery("from RoomEntity where name = :name", RoomEntity.class).setParameter("name", name).uniqueResult();
            return Optional.ofNullable(room);
        }
    }

    public List<RoomEntity> findAll() {
        try (Session session = EntityManagerFactoryProvider.openSession()) {
            return session.createQuery("from RoomEntity", RoomEntity.class).list();
        }
    }

    public boolean existsByName(String name) {
        try (Session session = EntityManagerFactoryProvider.openSession()) {
            Long count = session.createQuery("select count(r) from RoomEntity r where r.name = :name", Long.class).setParameter("name", name).uniqueResult();
            return count != null && count > 0;
        }
    }

    public RoomEntity save(RoomEntity room) {
        Transaction transaction = null;
        try (Session session = EntityManagerFactoryProvider.openSession()) {
            transaction = session.beginTransaction();
            session.persist(room);
            transaction.commit();
            return room;
        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public void saveIfNotExists(RoomEntity room) {
        Transaction transaction = null;
        try (Session session = EntityManagerFactoryProvider.openSession()) {
            transaction = session.beginTransaction();
            session.persist(room);
            transaction.commit();
        } catch (ConstraintViolationException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            // Room already exists, which is acceptable
        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }
}

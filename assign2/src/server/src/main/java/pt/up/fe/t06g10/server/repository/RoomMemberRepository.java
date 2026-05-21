package pt.up.fe.t06g10.server.repository;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import pt.up.fe.t06g10.server.database.EntityManagerFactoryProvider;
import pt.up.fe.t06g10.server.entity.RoomEntity;
import pt.up.fe.t06g10.server.entity.RoomMemberEntity;
import pt.up.fe.t06g10.server.entity.UserEntity;

public class RoomMemberRepository {
    public boolean existsByRoomAndUser(RoomEntity room, UserEntity user) {
        try (Session session = EntityManagerFactoryProvider.openSession()) {
            Long count = session.createQuery("select count(rm) from RoomMemberEntity rm where rm.room = :room and rm.user = :user", Long.class).setParameter("room", room).setParameter("user", user).uniqueResult();
            return count != null && count > 0;
        }
    }

    public void addMember(RoomEntity room, UserEntity user) {
        Transaction transaction = null;
        try (Session session = EntityManagerFactoryProvider.openSession()) {
            transaction = session.beginTransaction();
            RoomMemberEntity member = new RoomMemberEntity(room, user);
            session.persist(member);
            transaction.commit();
        } catch (ConstraintViolationException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            // User already a member, which is acceptable
        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public void removeMember(RoomEntity room, UserEntity user) {
        Transaction transaction = null;
        try (Session session = EntityManagerFactoryProvider.openSession()) {
            transaction = session.beginTransaction();
            session.createQuery("delete from RoomMemberEntity rm where rm.room = :room and rm.user = :user").setParameter("room", room).setParameter("user", user).executeUpdate();
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }
}

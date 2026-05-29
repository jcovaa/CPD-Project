package pt.up.fe.t06g10.server.repository;

import org.hibernate.Session;
import org.hibernate.Transaction;
import pt.up.fe.t06g10.server.database.EntityManagerFactoryProvider;
import pt.up.fe.t06g10.server.entity.MessageEntity;
import pt.up.fe.t06g10.server.entity.RoomEntity;

import java.util.List;

public class MessageRepository {
    public void save(MessageEntity message) {
        Transaction transaction = null;
        try (Session session = EntityManagerFactoryProvider.openSession()) {
            transaction = session.beginTransaction();
            session.persist(message);
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public List<MessageEntity> findRecentByRoom(RoomEntity room, int count) {
        try (Session session = EntityManagerFactoryProvider.openSession()) {
            var query = session.createQuery("select m from MessageEntity m join fetch m.sender where m.room = :room order by m.createdAt desc", MessageEntity.class);
            query.setParameter("room", room);
            if (count > 0) {
                query.setMaxResults(count);
            }
            return query.list();
        }
    }
}

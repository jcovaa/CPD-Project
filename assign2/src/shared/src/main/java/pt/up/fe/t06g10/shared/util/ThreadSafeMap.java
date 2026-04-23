package pt.up.fe.t06g10.shared.util;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ThreadSafeMap<J, K> {
    private final HashMap<J, K> map = new HashMap<>();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public K get(J key) {
        readWriteLock.readLock().lock();
        try {
            return map.get(key);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public K put(J key, K value) {
        readWriteLock.writeLock().lock();
        try {
            return map.put(key, value);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public K remove(J key) {
        readWriteLock.writeLock().lock();
        try {
            return map.remove(key);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
}

package pt.up.fe.t06g10.shared.util;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ThreadSafeMap<K, V> {
    private final HashMap<K, V> map = new HashMap<>();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public V get(K key) {
        readWriteLock.readLock().lock();
        try {
            return map.get(key);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public V put(K key, V value) {
        readWriteLock.writeLock().lock();
        try {
            return map.put(key, value);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public V remove(K key) {
        readWriteLock.writeLock().lock();
        try {
            return map.remove(key);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
}

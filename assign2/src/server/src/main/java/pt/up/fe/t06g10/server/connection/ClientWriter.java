package pt.up.fe.t06g10.server.connection;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Owns the outbound side of a client's TCP connection.
 * The ClientWriter solves the "slow client" problem: if a client's write buffer fills,
 * messages pile up in the queue rather than blocking other threads.
 */

public class ClientWriter implements Runnable {
    private static final int QUEUE_CAPACITY = 512;

    private final ArrayDeque<String> queue = new ArrayDeque<>(QUEUE_CAPACITY);
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    private boolean queueOffer(String msg) {
        lock.lock();
        try {
            if (queue.size() >= QUEUE_CAPACITY) {
                return false;
            }
            queue.addLast(msg);
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    private String queueTake() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();
            }
            String msg = queue.removeFirst();
            notFull.signal();
            return msg;
        } finally {
            lock.unlock();
        }
    }

    private static final String POISON_PILL = new String("__STOP__");

    private final PrintWriter out;
    private volatile Thread thread;

    public ClientWriter(Socket socket) throws IOException {
        this.out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
    }

    public void start() {
        thread = Thread.ofVirtual()
                .name("client-writer")
                .start(this);
    }

    public void stop() {
        lock.lock();
        try {
            if (!queue.contains(POISON_PILL)) {
                if (queue.size() >= QUEUE_CAPACITY) {
                    queue.clear();
                }
                queue.addLast(POISON_PILL);
                notEmpty.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    public void awaitStop() throws InterruptedException {
        if (thread != null) thread.join();
    }

    public boolean enqueue(String message) {
        if (message == null) throw new IllegalArgumentException("message must not be null");
        return queueOffer(message);
    }

    @Override
    public void run() {
        try {
            while (true) {
                String message = queueTake();
                if (message == POISON_PILL) break;

                out.println(message);

                if (out.checkError()) {
                    System.err.println("[ClientWriter] write error - socket likely closed");
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            out.close();
        }
    }
}

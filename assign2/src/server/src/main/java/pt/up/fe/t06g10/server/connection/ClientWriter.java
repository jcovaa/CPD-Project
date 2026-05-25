package pt.up.fe.t06g10.server.connection;

import pt.up.fe.t06g10.server.Protocol;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
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

    private static final String QUEUE_FULL_MSG = "__QUEUE_FULL__";
    private static final String POISON_PILL = "__STOP__";

    private void queueOffer(String msg) {
        lock.lock();
        try {
            if (queue.size() >= QUEUE_CAPACITY) {
                queue.addLast(QUEUE_FULL_MSG);
                notEmpty.signal();
                return;
            }
            queue.addLast(msg);
            notEmpty.signal();
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
            return queue.removeFirst();
        } finally {
            lock.unlock();
        }
    }

    private volatile PrintWriter out;
    private volatile Thread thread;

    public ClientWriter(Socket socket) throws IOException {
        this.out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
    }

    public void start(String clientUserName) {
        thread = Thread.ofVirtual()
                .name("client-writer-" + clientUserName)
                .start(this);
    }

    public void stop() {
        lock.lock();
        try {
            boolean alreadyQueued = queue.stream().anyMatch(m -> Objects.equals(m, POISON_PILL));
            if (!alreadyQueued) {
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

    public void awaitStop(long timeout, TimeUnit unit) throws InterruptedException {
        if (thread == null) return;
        long millis = unit.toMillis(timeout);
        long start = System.currentTimeMillis();
        while (thread.isAlive()) {
            if (System.currentTimeMillis() - start >= millis) {
                return;
            }
            Thread.sleep(10);
        }
    }

    public void enqueue(String message) {
        if (message == null) throw new IllegalArgumentException("message must not be null");
        queueOffer(message);
    }

    @Override
    public void run() {
        try {
            while (true) {
                String message = queueTake();
                if (message.equals(POISON_PILL)) break;

                if (message.equals(QUEUE_FULL_MSG)) {
                    out.println(Protocol.INTERNAL_ERROR + " Server busy, please try again");
                    if (out.checkError()) {
                        System.err.println("[ClientWriter] write error - socket likely closed");
                        break;
                    }
                    continue;
                }

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

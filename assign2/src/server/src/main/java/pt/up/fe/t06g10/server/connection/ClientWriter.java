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
    private static final long MAX_DROPPED_BEFORE_DISCONNECT = 64L;

    private final ArrayDeque<String> queue = new ArrayDeque<>(QUEUE_CAPACITY);
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private boolean queueFullNoticePending = false;
    private long droppedMessages = 0L;
    private long queueFullEvents = 0L;
    private volatile boolean slowClientDisconnectRequested = false;

    private static final String QUEUE_FULL_MSG = "__QUEUE_FULL__";
    private static final String POISON_PILL = "__STOP__";

    private void queueOffer(String msg) {
        lock.lock();
        try {
            if (queue.size() >= QUEUE_CAPACITY) {
                queueFullNoticePending = true;
                queueFullEvents++;
                droppedMessages++;
                if (droppedMessages >= MAX_DROPPED_BEFORE_DISCONNECT) {
                    slowClientDisconnectRequested = true;
                }
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
            while (queue.isEmpty() && !queueFullNoticePending) {
                notEmpty.await();
            }
            if (queueFullNoticePending) {
                queueFullNoticePending = false;
                return QUEUE_FULL_MSG;
            }
            return queue.removeFirst();
        } finally {
            lock.unlock();
        }
    }

    private volatile PrintWriter out;
    private volatile Thread thread;
    private volatile Runnable slowClientDisconnectHandler;

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

    public long getDroppedMessages() {
        lock.lock();
        try {
            return droppedMessages;
        } finally {
            lock.unlock();
        }
    }

    public long getQueueFullEvents() {
        lock.lock();
        try {
            return queueFullEvents;
        } finally {
            lock.unlock();
        }
    }

    public void setSlowClientDisconnectHandler(Runnable handler) {
        this.slowClientDisconnectHandler = handler;
    }

    public boolean isSlowClientDisconnectRequested() {
        return slowClientDisconnectRequested;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String message = queueTake();
                if (message.equals(POISON_PILL)) break;

                if (message.equals(QUEUE_FULL_MSG)) {
                    if (slowClientDisconnectRequested) {
                        out.println(Protocol.INTERNAL_ERROR + " Slow client disconnected");
                        Runnable handler = slowClientDisconnectHandler;
                        if (handler != null) handler.run();
                        break;
                    }
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
            if (droppedMessages > 0 || queueFullEvents > 0) {
                System.err.println("[ClientWriter] queue_full_events=" + queueFullEvents + " dropped_messages=" + droppedMessages);
            }
            out.close();
        }
    }
}

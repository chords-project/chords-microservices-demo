package choral.reactive;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class LocalReactiveQueue<C> implements ReactiveSender<C, Object>, ReactiveReceiver<C, Object> {

    private final HashMap<Session<C>, LinkedList<Object>> sendQueue = new HashMap<>();
    private final HashMap<Session<C>, LinkedList<CompletableFuture<Object>>> recvQueue = new HashMap<>();
    private NewSessionEvent<C> newSessionEvent = null;

    public LocalReactiveQueue() {
    }

    public void onNewSession(NewSessionEvent<C> event) {
        this.newSessionEvent = event;
    }

    public void send(Session<C> session, Object msg) {
        synchronized (this) {
            if (this.recvQueue.containsKey(session)) {
                // the flow already exists, pass the message to recv...

                if (this.recvQueue.get(session).isEmpty()) {
                    enqueueSend(session, msg);
                } else {
                    CompletableFuture<Object> future = this.recvQueue.get(session).removeFirst();
                    future.complete(msg);
                }
            } else {
                // this is a new flow, enqueue the message and notify the event handler
                enqueueSend(session, msg);
                newSessionEvent.onNewSession(session, () -> cleanupKey(session));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T recv(Session<C> session) {
        CompletableFuture<Object> future = new CompletableFuture<>();

        synchronized (this) {
            if (this.sendQueue.containsKey(session)) {
                // Flow already exists, receive message from send...

                if (this.sendQueue.get(session).isEmpty()) {
                    this.recvQueue.get(session).add(future);
                } else {
                    future.complete(this.sendQueue.get(session).removeFirst());
                }
            } else {
                // Flow does not exist yet, wait for it to arrive...
                enqueueRecv(session, future);
            }
        }

        try {
            return (T) future.get();
        } catch (InterruptedException | ExecutionException e) {
            // It's the responsibility of the choreography to have the type cast match
            // Throw runtime exception if mismatch
            throw new RuntimeException(e);
        }
    }

    // should synchronize on 'this' before calling this method
    private void enqueueSend(Session<C> session, Object msg) {
        if (!this.sendQueue.containsKey(session)) {
            this.sendQueue.put(session, new LinkedList<>());
            this.recvQueue.put(session, new LinkedList<>());
        }

        this.sendQueue.get(session).add(msg);
    }

    // should synchronize on 'this' before calling this method
    private void enqueueRecv(Session<C> session, CompletableFuture<Object> future) {
        if (!this.recvQueue.containsKey(session)) {
            this.recvQueue.put(session, new LinkedList<>());
            this.sendQueue.put(session, new LinkedList<>());
        }

        this.recvQueue.get(session).add(future);
    }

    private void cleanupKey(Session<C> session) {
        synchronized (this) {
            this.sendQueue.remove(session);
            this.recvQueue.remove(session);
        }
    }
}

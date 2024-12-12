package choral.reactive;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import choral.reactive.tracing.TelemetrySession;

public class LocalReactiveQueue implements ReactiveSender<Object>, ReactiveReceiver<Object> {

    public interface NewSessionEvent {
        void onNewSession(Session session);
    }

    private final HashMap<Session, LinkedList<Object>> sendQueue = new HashMap<>();
    private final HashMap<Session, LinkedList<CompletableFuture<Object>>> recvQueue = new HashMap<>();
    private NewSessionEvent newSessionEvent = null;

    public LocalReactiveQueue() {
    }

    public void onNewSession(NewSessionEvent event) {
        this.newSessionEvent = event;
    }

    @Override
    public void send(Session session, Object msg) {
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

                // Handle new session in the background

                Thread.ofVirtual().start(() -> {
                    newSessionEvent.onNewSession(session);
                    cleanupKey(session);
                });
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T recv(Session session) {
        CompletableFuture<Object> future = new CompletableFuture<>();

        synchronized (this) {
            if (this.sendQueue.containsKey(session)) {
                // Session already exists, receive message from send...

                if (this.sendQueue.get(session).isEmpty()) {
                    this.recvQueue.get(session).add(future);
                } else {
                    future.complete(this.sendQueue.get(session).removeFirst());
                }
            } else {
                // Session does not exist yet, wait for it to arrive...
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
    private void enqueueSend(Session session, Object msg) {
        if (!this.sendQueue.containsKey(session)) {
            this.sendQueue.put(session, new LinkedList<>());
            this.recvQueue.put(session, new LinkedList<>());
        }

        this.sendQueue.get(session).add(msg);
    }

    // should synchronize on 'this' before calling this method
    private void enqueueRecv(Session session, CompletableFuture<Object> future) {
        if (!this.recvQueue.containsKey(session)) {
            this.recvQueue.put(session, new LinkedList<>());
            this.sendQueue.put(session, new LinkedList<>());
        }

        this.recvQueue.get(session).add(future);
    }

    private void cleanupKey(Session session) {
        synchronized (this) {
            this.sendQueue.remove(session);
            this.recvQueue.remove(session);
        }
    }

    @Override
    public ReactiveChannel_A<Object> chanA(Session session) {
        return new ReactiveChannel_A<>(session, this, TelemetrySession.makeNoop(session));
    }

    public ReactiveChannel_B<Object> chanB(Session session) {
        return new ReactiveChannel_B<>(session, this, TelemetrySession.makeNoop(session));
    }
}

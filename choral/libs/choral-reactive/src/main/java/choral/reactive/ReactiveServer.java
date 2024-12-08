package choral.reactive;

import choral.reactive.connection.ClientConnectionManager;
import choral.reactive.connection.Message;
import choral.reactive.connection.ServerConnectionManager;
import choral.reactive.tracing.TelemetrySession;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ReactiveServer
        implements ServerConnectionManager.ServerEvents, ReactiveReceiver<Serializable>, AutoCloseable {

    private final HashSet<Integer> knownSessionIDs = new HashSet<>();

    // INVARIANTS:
    // 1. sendQueue contains session if and only if recvQueue contains session.
    // 2. if sendQueue is non-empty, then recvQueue is empty; and vice versa.
    // 3. if a sessionID is in knownSessionIDs, then the session is in telemetrySessionMap.

    /** Maps a sessionID and a sender name to a queue of messages. The next receive operation will consume the first message in the queue. */
    private final HashMap<Session, LinkedList<Serializable>> sendQueue = new HashMap<>();
    /** Maps a sessionID and a sender name to a queue of futures. The next message will be fed to the first future in the list. */
    private final HashMap<Session, LinkedList<CompletableFuture<Serializable>>> recvQueue = new HashMap<>();
    /** Maps a sessionID to a TelemetrySession. */
    private final HashMap<Integer, TelemetrySession> telemetrySessionMap = new HashMap<>();

    private final String serviceName;
    private final NewSessionEvent newSessionEvent;
    private final OpenTelemetrySdk telemetry;
    private final ServerConnectionManager connectionManager;

    /**
     * Creates a ReactiveServer, using {@link ServerConnectionManager} for the connection.
     * Invoke {@link ServerConnectionManager#listen(String)} to start listening.
     */
    public ReactiveServer(String serviceName, OpenTelemetrySdk telemetry, NewSessionEvent newSessionEvent) {
        this.serviceName = serviceName;
        this.telemetry = telemetry;
        this.newSessionEvent = newSessionEvent;
        this.connectionManager = ServerConnectionManager.makeConnectionManager(this, telemetry);
    }

    /**
     * Creates a ReactiveServer with telemetry disabled. Uses {@link ServerConnectionManager} for
     * the connection.
     */
    public ReactiveServer(String serviceName, NewSessionEvent newSessionEvent) {
        this(serviceName, OpenTelemetrySdk.builder().build(), newSessionEvent);
    }

    /**
     * Begins listening at the given address and registers a shutdown hook that runs when the
     * program exits.
     */
    public void listen(String address) throws URISyntaxException, IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                ReactiveServer.this.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "ReactiveServer_SHUTDOWN_HOOK"));

        connectionManager.listen(address);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Serializable> T recv(Session session) { // TODO profile this
        Attributes attributes = Attributes.builder().put("channel.service", serviceName)
                .put("channel.sender", session.senderName()).build();

        TelemetrySession telemetrySession;
        CompletableFuture<Serializable> future = new CompletableFuture<Serializable>()
                .orTimeout(10, TimeUnit.SECONDS);
                // TODO Do we want this timeout? What happens if the sender takes >10s to compute the message?
                //  Maybe the timeout is a parameter we pass *for each session*?

        synchronized (this) {
            telemetrySession = telemetrySessionMap.get(session.sessionID());

            if (this.sendQueue.containsKey(session)) {
                // Session already exists, receive message from sender...

                if (this.sendQueue.get(session).isEmpty()) {
                    telemetrySession.log(
                            "ReactiveServer receive, session already exists, waiting for message to arrive",
                            attributes);
                    this.recvQueue.get(session).add(future);
                } else {
                    telemetrySession.log("ReactiveServer receive, message already arrived", attributes);
                    future.complete(this.sendQueue.get(session).removeFirst());
                }
            } else {
                // Session does not exist yet, wait for it to arrive...
                telemetrySession.log(
                        "ReactiveServer receive, session does not exist yet, waiting for message to arrive",
                        attributes);
                enqueueRecv(session, future);
            }
        }

        try {
            return (T) future.get();
        } catch (InterruptedException | ExecutionException e) {
            telemetrySession.recordException("ReactiveServer exception when receiving message", e, true, attributes);

            // It's the responsibility of the choreography to have the type cast match
            // Throw runtime exception if mismatch
            throw new RuntimeException(e);
        }
    }

    public void registerSession(Session session, TelemetrySession telemetrySession) {
        synchronized (this) {
            knownSessionIDs.add(session.sessionID());
            telemetrySessionMap.put(session.sessionID(), telemetrySession);
        }
    }

    public ReactiveChannel_B<Serializable> chanB(Session session, String clientName) {
        Session senderSession = session.replacingSender(clientName);

        TelemetrySession telemetrySession;
        synchronized (this) {
            if (!telemetrySessionMap.containsKey(senderSession.sessionID()))
                throw new IllegalStateException("Expected telemetrySessionMap to contain session: " + senderSession);

            telemetrySession = telemetrySessionMap.get(senderSession.sessionID());
        }

        return new ReactiveChannel_B<Serializable>(senderSession, this, telemetrySession);
    }

    @Override
    public void messageReceived(Message msg) { // TODO profile this

        synchronized (this) {
            boolean isNewSession = knownSessionIDs.add(msg.session.sessionID);

            TelemetrySession telemetrySession;
            Span sessionSpan = null;
            if (isNewSession) {
                telemetrySession = new TelemetrySession(telemetry, msg);
                sessionSpan = telemetrySession.makeChoreographySpan();
                this.telemetrySessionMap.put(msg.session.sessionID(), telemetrySession);
            } else {
                if (!telemetrySessionMap.containsKey(msg.session.sessionID()))
                    throw new IllegalStateException(
                            "Expected telemetrySessionMap to contain session: " + msg.session);

                telemetrySession = telemetrySessionMap.get(msg.session.sessionID());
            }

            if (this.recvQueue.containsKey(msg.session)) {
                // the session already exists, pass the message to recv...

                if (this.recvQueue.get(msg.session).isEmpty()) {
                    telemetrySession.log("ReactiveServer message received: existing session, enqueue send");
                    enqueueSend(msg.session, msg.message);
                } else {
                    telemetrySession.log("ReactiveServer message received: complete receive future");
                    CompletableFuture<Serializable> future = this.recvQueue.get(msg.session).removeFirst();
                    future.complete(msg.message);
                }
            } else {
                // this is a new session, enqueue the message
                telemetrySession.log("ReactiveServer message received: new session, enqueue send");
                enqueueSend(msg.session, msg.message);
            }

            if (isNewSession) {
                final Span span = sessionSpan;
                // Handle new session in another thread
                Thread.ofVirtual()
                        .name("NEW_SESSION_HANDLER_" + msg.session)
                        .start(() -> {
                            this.telemetrySessionMap.put(msg.session.sessionID(), telemetrySession);

                            telemetrySession.log(
                                    "ReactiveServer handle new session",
                                    Attributes.builder().put("service", serviceName)
                                            .put("session", msg.session.toString()).build());

                            try (Scope scope = span.makeCurrent();
                                    SessionContext sessionCtx = new SessionContext(this, msg.session,
                                            telemetrySession);) {
                                newSessionEvent.onNewSession(sessionCtx);
                            } catch (Exception e) {
                                telemetrySession.recordException(
                                        "ReactiveServer session exception",
                                        e,
                                        true,
                                        Attributes.builder().put("service", serviceName)
                                                .put("session", msg.session.toString()).build());
                            } finally {
                                span.end();
                            }

                            cleanupKey(msg.session);
                        });
            }
        }
    }

    // should synchronize on 'this' before calling this method
    private void enqueueSend(Session session, Serializable msg) {
        if (!this.sendQueue.containsKey(session)) {
            this.sendQueue.put(session, new LinkedList<>());
            this.recvQueue.put(session, new LinkedList<>());
        }

        this.sendQueue.get(session).add(msg);
    }

    // should synchronize on 'this' before calling this method
    private void enqueueRecv(Session session, CompletableFuture<Serializable> future) {
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
            this.telemetrySessionMap.remove(session.sessionID());
            this.knownSessionIDs.remove(session.sessionID());
        }
    }

    @Override
    public void close() throws IOException {
        connectionManager.close();
    }

    public interface NewSessionEvent {
        void onNewSession(SessionContext ctx) throws Exception;
    }

    /**
     * A context object for creating channels and logging telemetry events in a particular session.
     */
    public static class SessionContext implements AutoCloseable {

        private final ReactiveServer server;
        public final Session session;
        private final TelemetrySession telemetrySession;
        private final HashSet<AutoCloseable> closeHandles = new HashSet<>();

        private SessionContext(ReactiveServer server, Session session, TelemetrySession telemetrySession) {
            this.server = server;
            this.session = session;
            this.telemetrySession = telemetrySession;
        }

        /**
         * Creates a channel for receiving messages from the given client in this session.
         *
         * @param clientService the name of the client service
         */
        public ReactiveChannel_B<Serializable> chanB(String clientService) {
            Session newSession = session.replacingSender(clientService);
            return new ReactiveChannel_B<Serializable>(newSession, server, telemetrySession);
        }

        /**
         * Creates a channel for sending messages to the given client in this session.
         *
         * @param connectionManager an implementation of the communication middleware
         */
        public ReactiveChannel_A<Serializable> chanA(ClientConnectionManager connectionManager)
                throws IOException, InterruptedException {
            ReactiveClient client = new ReactiveClient(connectionManager, server.serviceName, telemetrySession);
            closeHandles.add(client);
            return client.chanA(session);
        }

        /**
         * Creates a bidirectional channel between this service and the given client.
         *
         * @param clientService the name of the service to which we are connecting
         * @param connectionManager an implementation of the communication middleware
         */
        public ReactiveSymChannel<Serializable> symChan(String clientService,
                ClientConnectionManager connectionManager)
                throws IOException, InterruptedException {
            var a = chanA(connectionManager);
            var b = chanB(clientService);
            return new ReactiveSymChannel<>(a, b);
        }

        public void log(String message) {
            telemetrySession.log(message);
        }

        public void log(String message, Attributes attributes) {
            telemetrySession.log(message, attributes);
        }

        @Override
        public void close() throws Exception {
            for (AutoCloseable handle : closeHandles) {
                handle.close();
            }
        }
    }

    @Override
    public String toString() {
        return "ReactiveServer [serviceName=" + serviceName + "]";
    }
}

package choral.reactive;

import choral.reactive.connection.ClientConnectionManager;
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

public class ReactiveServer<S extends Session>
        implements ServerConnectionManager.ServerEvents, ReactiveReceiver<S, Serializable>, AutoCloseable {

    private final HashSet<Integer> knownSessionIDs = new HashSet<>();

    private final HashMap<S, LinkedList<Serializable>> sendQueue = new HashMap<>();
    private final HashMap<S, LinkedList<CompletableFuture<Serializable>>> recvQueue = new HashMap<>();
    private final HashMap<S, TelemetrySession> telemetrySessionMap = new HashMap<>();

    private final String serviceName;
    private final NewSessionEvent<S> newSessionEvent;
    private final OpenTelemetrySdk telemetry;
    private final ServerConnectionManager connectionManager;

    public ReactiveServer(String serviceName, OpenTelemetrySdk telemetry, NewSessionEvent<S> newSessionEvent) {
        this.serviceName = serviceName;
        this.telemetry = telemetry;
        this.newSessionEvent = newSessionEvent;
        this.connectionManager = ServerConnectionManager.makeConnectionManager(this, telemetry);
    }

    public ReactiveServer(String serviceName, NewSessionEvent<S> newSessionEvent) {
        // Pass NoOp telemetry sdk
        this(serviceName, OpenTelemetrySdk.builder().build(), newSessionEvent);
    }

    public void listen(String address) throws URISyntaxException, IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                this.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "ReactiveServer_SHUTDOWN_HOOK"));

        connectionManager.listen(address);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Serializable> T recv(S session) {
        Attributes attributes = Attributes.builder().put("channel.service", serviceName)
                .put("channel.sender", session.senderName()).build();

        TelemetrySession telemetrySession;
        CompletableFuture<Serializable> future = new CompletableFuture<>();

        synchronized (this) {
            telemetrySession = telemetrySessionMap.getOrDefault(session, TelemetrySession.makeNoop(session));

            if (this.sendQueue.containsKey(session)) {
                // Flow already exists, receive message from send...

                if (this.sendQueue.get(session).isEmpty()) {
                    telemetrySession.log(
                            "TCPReactiveServer receive, session already exists, waiting for message to arrive",
                            attributes);
                    this.recvQueue.get(session).add(future);
                } else {
                    telemetrySession.log("TCPReactiveServer receive, message already arrived", attributes);
                    future.complete(this.sendQueue.get(session).removeFirst());
                }
            } else {
                // Flow does not exist yet, wait for it to arrive...
                telemetrySession.log(
                        "TCPReactiveServer receive, session does not exist yet, waiting for message to arrive",
                        attributes);
                enqueueRecv(session, future);
            }
        }

        try {
            return (T) future.get();
        } catch (InterruptedException | ExecutionException e) {
            telemetrySession.recordException("TCPReactiveServer exception when receiving message", e, true, attributes);

            // It's the responsibility of the choreography to have the type cast match
            // Throw runtime exception if mismatch
            throw new RuntimeException(e);
        }
    }

    public void registerSession(S session) {
        knownSessionIDs.add(session.sessionID());
    }

    public ReactiveChannel_B<S, Serializable> chanB(S session, String clientName) {
        // Safe since it is a precondition of the method
        @SuppressWarnings("unchecked")
        S senderSession = (S) session.replacingSender(clientName);

        TelemetrySession telemetrySession;
        synchronized (this) {
            telemetrySession = telemetrySessionMap.getOrDefault(senderSession, TelemetrySession.makeNoop(session));
        }

        return new ReactiveChannel_B<S, Serializable>(senderSession, this, telemetrySession);
    }

    @Override
    public void messageReceived(Object message) {
        @SuppressWarnings("unchecked")
        TCPMessage<S> msg = (TCPMessage<S>) message;
        final TelemetrySession telemetrySession = new TelemetrySession(telemetry, msg);

        synchronized (this) {
            boolean isNewSession = knownSessionIDs.add(msg.session.sessionID());

            if (this.recvQueue.containsKey(msg.session)) {
                // the flow already exists, pass the message to recv...

                if (this.recvQueue.get(msg.session).isEmpty()) {
                    enqueueSend(msg.session, msg.message);
                } else {
                    CompletableFuture<Serializable> future = this.recvQueue.get(msg.session).removeFirst();
                    future.complete(msg.message);
                }
            } else {
                // this is a new flow, enqueue the message
                enqueueSend(msg.session, msg.message);
            }

            if (isNewSession) {
                // Handle new session in another thread
                Thread.ofPlatform()
                        .name("NEW_SESSION_HANDLER_" + msg.session)
                        .start(() -> {
                            Span span = telemetrySession.makeChoreographySpan();

                            this.telemetrySessionMap.put(msg.session, telemetrySession);

                            telemetrySession.log(
                                    "TCPReactiveServer handle new session",
                                    Attributes.builder().put("service", serviceName)
                                            .put("session", msg.session.toString()).build());

                            try (Scope scope = span.makeCurrent();
                                    SessionContext<S> sessionCtx = new SessionContext<>(this, msg.session,
                                            telemetrySession);) {
                                newSessionEvent.onNewSession(sessionCtx);
                            } catch (Exception e) {
                                telemetrySession.recordException(
                                        "TCPReactiveServer session exception",
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
    private void enqueueSend(S session, Serializable msg) {
        if (!this.sendQueue.containsKey(session)) {
            this.sendQueue.put(session, new LinkedList<>());
            this.recvQueue.put(session, new LinkedList<>());
        }

        this.sendQueue.get(session).add(msg);
    }

    // should synchronize on 'this' before calling this method
    private void enqueueRecv(S session, CompletableFuture<Serializable> future) {
        if (!this.recvQueue.containsKey(session)) {
            this.recvQueue.put(session, new LinkedList<>());
            this.sendQueue.put(session, new LinkedList<>());
        }

        this.recvQueue.get(session).add(future);
    }

    private void cleanupKey(S session) {
        synchronized (this) {
            this.sendQueue.remove(session);
            this.recvQueue.remove(session);
            this.telemetrySessionMap.remove(session);
        }
    }

    @Override
    public void close() throws IOException {
        connectionManager.close();
    }

    public interface NewSessionEvent<S extends Session> {
        void onNewSession(SessionContext<S> ctx) throws Exception;
    }

    public static class SessionContext<S extends Session> implements AutoCloseable {

        private final ReactiveServer<S> server;
        public final S session;
        private final TelemetrySession telemetrySession;
        private final HashSet<AutoCloseable> closeHandles = new HashSet<>();

        private SessionContext(ReactiveServer<S> server, S session, TelemetrySession telemetrySession) {
            this.server = server;
            this.session = session;
            this.telemetrySession = telemetrySession;
        }

        /**
         * Creates a server channel on this server listening for messages,
         * coming from the given clientService on the same session.
         */
        public ReactiveChannel_B<S, Serializable> chanB(String clientService) {
            // Safe since it is a precondition of the method
            @SuppressWarnings("unchecked")
            S newSession = (S) session.replacingSender(clientService);

            return new ReactiveChannel_B<S, Serializable>(newSession, server, telemetrySession);
        }

        /**
         * Creates a client channel pre-configured with the session and service.
         *
         * @param address the network address of the client to connect to.
         */
        public ReactiveChannel_A<S, Serializable> chanA(ClientConnectionManager connectionManager)
                throws IOException, InterruptedException {
            ReactiveClient<S> client = new ReactiveClient<>(connectionManager, server.serviceName, telemetrySession);
            closeHandles.add(client);
            return client.chanA(session);
        }

        public ReactiveSymChannel<S, Serializable> symChan(String clientService,
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
        return "TCPReactiveServer [serviceName=" + serviceName + "]";
    }
}

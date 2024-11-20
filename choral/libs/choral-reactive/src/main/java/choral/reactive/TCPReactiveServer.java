package choral.reactive;

import choral.reactive.tracing.TelemetrySession;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class TCPReactiveServer<S extends Session> implements ReactiveReceiver<S, Serializable>, AutoCloseable {

    private final HashSet<Integer> knownSessionIDs = new HashSet<>();

    private final HashMap<S, LinkedList<Serializable>> sendQueue = new HashMap<>();
    private final HashMap<S, LinkedList<CompletableFuture<Serializable>>> recvQueue = new HashMap<>();
    private final HashMap<S, TelemetrySession> telemetrySessionMap = new HashMap<>();

    private final String serviceName;
    private final NewSessionEvent<S> newSessionEvent;
    private final OpenTelemetrySdk telemetry;

    private ServerSocket serverSocket = null;

    public TCPReactiveServer(String serviceName, OpenTelemetrySdk telemetry, NewSessionEvent<S> newSessionEvent) {
        this.serviceName = serviceName;
        this.telemetry = telemetry;
        this.newSessionEvent = newSessionEvent;
    }

    public TCPReactiveServer(String serviceName, NewSessionEvent<S> newSessionEvent) {
        // Pass NoOp telemetry sdk
        this(serviceName, OpenTelemetrySdk.builder().build(), newSessionEvent);
    }

    /**
     * Start the server listening on the given address, blocking the thread.
     *
     * @param address the address for the server to listen on. Example
     *                "0.0.0.0:1234"
     * @throws URISyntaxException if the onNewSession event handler has not been
     *                            registered before calling
     */
    public void listen(String address) throws URISyntaxException {
        System.out.println("TCPReactiveServer listener starting on " + address);

        URI uri = new URI(null, address, null, null, null).parseServerAuthority();
        InetSocketAddress addr = new InetSocketAddress(uri.getHost(), uri.getPort());

        try {
            serverSocket = new ServerSocket(addr.getPort(), 50, addr.getAddress());
            System.out.println("TCPReactiveServer listening successfully on " + serverSocket.getLocalSocketAddress());

            while (true) {
                Socket connection = serverSocket.accept();
                Thread.ofPlatform()
                        .name("CLIENT_CONNECTION_" + connection)
                        .start(() -> {
                            clientListen(connection);
                        });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void clientListen(Socket connection) {
        try {
            System.out.println("TCPReactiveServer client connected: address=" + connection.getInetAddress());
            while (true) {
                try (ObjectInputStream stream = new ObjectInputStream(connection.getInputStream())) {
                    while (true) {
                        try {
                            @SuppressWarnings("unchecked")
                            TCPMessage<S> msg = (TCPMessage<S>) stream.readObject();

                            TelemetrySession telemetrySession = null;
                            telemetrySession = new TelemetrySession(telemetry, msg);

                            receiveMessage(connection, msg, telemetrySession);
                        } catch (StreamCorruptedException | ClassNotFoundException e) {
                            System.out.println("TCPReactiveServer failed to deserialize class: address="
                                    + connection.getInetAddress());
                        }
                    }
                }
            }
        } catch (EOFException e) {
            System.out.println("TCPReactiveServer client disconnected: service=" + serviceName + " address="
                    + connection.getInetAddress());
        } catch (IOException e) {
            System.out.println("TCPReactiveServer client exception: service=" + serviceName + " address="
                    + connection.getInetAddress());
            e.printStackTrace();
        }
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

    private void receiveMessage(Socket connection, TCPMessage<S> msg, TelemetrySession telemetrySession) {
        // telemetrySession.log cannot be used, since the span has not been created yet
        System.out.println(
                "TCPReactiveServer received message: service=" + serviceName + " address=" + connection.getInetAddress()
                        + " session=" + msg.session);

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
        serverSocket.close();
    }

    public interface NewSessionEvent<S extends Session> {
        void onNewSession(SessionContext<S> ctx) throws Exception;
    }

    public static class SessionContext<S extends Session> implements AutoCloseable {

        private final TCPReactiveServer<S> server;
        public final S session;
        private final TelemetrySession telemetrySession;
        private final HashSet<AutoCloseable> closeHandles = new HashSet<>();

        private SessionContext(TCPReactiveServer<S> server, S session, TelemetrySession telemetrySession) {
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

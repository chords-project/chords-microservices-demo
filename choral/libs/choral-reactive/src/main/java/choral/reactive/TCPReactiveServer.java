package choral.reactive;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.Closeable;
import java.io.StreamCorruptedException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import choral.reactive.tracing.TelemetrySession;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class TCPReactiveServer<S extends Session> implements ReactiveReceiver<S, Serializable>, AutoCloseable {

    private final HashSet<Integer> knownSessionIDs = new HashSet<>();

    private final HashMap<S, LinkedList<Serializable>> sendQueue = new HashMap<>();
    private final HashMap<S, LinkedList<CompletableFuture<Serializable>>> recvQueue = new HashMap<>();

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
                new Thread(() -> {
                    clientListen(connection);
                }, "CLIENT_CONNECTION_" + connection).start();
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
                            System.out.println(
                                    "TCPReactiveServer failed to deserialize class: address="
                                            + connection.getInetAddress());
                        }
                    }
                }

            }
        } catch (EOFException e) {
            System.out.println("TCPReactiveServer client disconnected: service=" + serviceName + " address=" +
                    connection.getInetAddress());
        } catch (IOException e) {
            System.out.println("TCPReactiveServer client exception: service=" + serviceName + " address="
                    + connection.getInetAddress());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Serializable> T recv(S session) {
        System.out.println(
                "TCPReactiveServer waiting to receive: service=" + serviceName + " sender=" + session.senderName()
                        + " session=" + session);
        CompletableFuture<Serializable> future = new CompletableFuture<>();

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

    public void registerSession(S session) {
        knownSessionIDs.add(session.sessionID());
    }

    public ReactiveChannel_B<S, Serializable> chanB(S session, String clientName) {

        // Safe since it is a precondition of the method
        @SuppressWarnings("unchecked")
        S newSession = (S) session.replacingSender(clientName);

        return new ReactiveChannel_B<>(newSession, this);
    }

    private void receiveMessage(
            Socket connection,
            TCPMessage<S> msg,
            TelemetrySession telemetrySession) {
        System.out.println(
                "TCPReactiveServer received message: service=" + serviceName + " address=" + connection.getInetAddress()
                        + " session="
                        + msg.session);

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
                // Handle new session in new thread
                new Thread(() -> {
                    Span span = telemetrySession.makeChoreographySpan();

                    telemetrySession.log("receive message", Attributes.builder()
                            .put("message.sender", connection.getInetAddress().toString())
                            .build());

                    try (Scope scope = span.makeCurrent()) {

                        System.out.println(
                                "TCPReactiveServer handle new session: service=" + serviceName + " session="
                                        + msg.session);

                        SessionContext<S> sessionCtx = new SessionContext<>(this, msg.session, telemetrySession);
                        newSessionEvent.onNewSession(sessionCtx);
                        sessionCtx.close();
                    } catch (Exception e) {
                        System.err.println("Exception caught for session: " + msg.session);
                        e.printStackTrace();

                        span.setAttribute("error", true);
                        span.recordException(e);
                    } finally {
                        span.end();
                    }

                    cleanupKey(msg.session);
                    // System.out.println("Ended trace span for session: " + session);
                }, "NEW_SESSION_HANDLER_" + msg.session).start();
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
        }
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
    }

    public interface NewSessionEvent<S extends Session> {
        void onNewSession(SessionContext<S> ctx) throws Exception;
    }

    public static class SessionContext<S extends Session> {
        private final TCPReactiveServer<S> server;
        public final S session;
        private final TelemetrySession telemetrySession;

        private final List<Closeable> closeHandles = new ArrayList<>();

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
            // Safe since this is a precondition of method
            @SuppressWarnings("unchecked")
            S newSession = (S) session.replacingSender(clientService);

            return server.chanB(newSession);
        }

        /**
         * Creates a client channel pre-configured with the session and service.
         * 
         * @param address the network address of the client to connect to.
         */
        public ReactiveChannel_A<S, Serializable> chanA(String address)
                throws UnknownHostException, URISyntaxException, IOException {
            TCPReactiveClient<S> client = new TCPReactiveClient<>(address, server.serviceName, telemetrySession);
            closeHandles.add(client);
            return client.chanA(session);
        }

        public ReactiveSymChannel<S, Serializable> symChan(String clientService, String address)
                throws UnknownHostException, URISyntaxException, IOException {
            var a = chanA(address);
            var b = chanB(clientService);
            return new ReactiveSymChannel<>(a, b);
        }

        private void close() throws IOException {
            for (Closeable closeable : closeHandles) {
                closeable.close();
            }
        }
    }
}

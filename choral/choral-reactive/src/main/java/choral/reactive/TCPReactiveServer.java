package choral.reactive;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class TCPReactiveServer<C> implements ReactiveReceiver<C, Serializable>, AutoCloseable {

    private final HashMap<Session<C>, LinkedList<Serializable>> sendQueue = new HashMap<>();
    private final HashMap<Session<C>, LinkedList<CompletableFuture<Serializable>>> recvQueue = new HashMap<>();

    private NewSessionEvent<C> newSessionEvent = null;
    private ServerSocket serverSocket = null;
    private SessionPool<C> sessionPool;

    public TCPReactiveServer(SessionPool<C> sessionPool) {
        this.sessionPool = sessionPool;
    }

    public void listen(String address) throws URISyntaxException {
        URI uri = new URI(null, address, null, null, null).parseServerAuthority();
        InetSocketAddress addr = new InetSocketAddress(uri.getHost(), uri.getPort());

        try {
            serverSocket = new ServerSocket(addr.getPort(), 50, addr.getAddress());
            System.out.println("Choral reactive server listening on " + serverSocket.getLocalSocketAddress());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("Shutting down reactive server gracefully...");
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, "SHUTDOWN_HOOK_TCPReactiveServer"));

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
                        TCPMessage<C> msg = (TCPMessage<C>) stream.readObject();
                        receiveMessage(connection, msg.session, msg.message);
                    }
                } catch (StreamCorruptedException | ClassNotFoundException e) {
                    System.out.println(
                            "TCPReactiveServer failed to deserialize class: address=" + connection.getInetAddress());
                }
            }
            // } catch (EOFException e) {
            // System.out.println("TCPReactiveServer client disconnected: address=" +
            // connection.getInetAddress());
        } catch (IOException e) {
            System.out.println("TCPReactiveServer client exception: address=" + connection.getInetAddress());
            e.printStackTrace();
        }
    }

    @Override
    public void onNewSession(NewSessionEvent<C> event) {
        this.newSessionEvent = event;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Serializable> T recv(Session<C> session) {
        System.out.println("TCPReactiveServer waiting to receive: session=" + session);
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

    private void receiveMessage(Socket connection, Session<C> session, Serializable msg) {
        System.out.println(
                "TCPReactiveServer received message: address=" + connection.getInetAddress() + " session=" + session);

        synchronized (this) {
            boolean isNewSession = sessionPool.registerSession(session);

            if (this.recvQueue.containsKey(session)) {
                // the flow already exists, pass the message to recv...

                if (this.recvQueue.get(session).isEmpty()) {
                    enqueueSend(session, msg);
                } else {
                    CompletableFuture<Serializable> future = this.recvQueue.get(session).removeFirst();
                    future.complete(msg);
                }
            } else {
                // this is a new flow, enqueue the message and notify the event handler
                enqueueSend(session, msg);
            }

            if (isNewSession) {
                // Handle new session in new thread
                new Thread(() -> {
                    newSessionEvent.onNewSession(session);
                    cleanupKey(session);
                }, "NEW_SESSION_HANDLER_" + session).start();
            }
        }
    }

    // should synchronize on 'this' before calling this method
    private void enqueueSend(Session<C> session, Serializable msg) {
        if (!this.sendQueue.containsKey(session)) {
            this.sendQueue.put(session, new LinkedList<>());
            this.recvQueue.put(session, new LinkedList<>());
        }

        this.sendQueue.get(session).add(msg);
    }

    // should synchronize on 'this' before calling this method
    private void enqueueRecv(Session<C> session, CompletableFuture<Serializable> future) {
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

    @Override
    public void close() throws Exception {
        serverSocket.close();
    }
}

package choral.reactive;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

public class TCPReactiveServer<C> implements ReactiveReceiver<C, Serializable> {

    private final HashMap<Session<C>, LinkedList<Object>> sendQueue = new HashMap<>();
    private final HashMap<Session<C>, LinkedList<CompletableFuture<Object>>> recvQueue = new HashMap<>();

    private NewSessionEvent<C> newSessionEvent = null;

    public TCPReactiveServer(NewSessionEvent<C> newSessionEvent) {
        this.newSessionEvent = newSessionEvent;
    }

    public void listen(InetSocketAddress addr) {
        try (ServerSocket serverSocket = new ServerSocket(addr.getPort(), 50, addr.getAddress())) {
            System.out.println("Choral reactive server listening on " + serverSocket.getLocalSocketAddress());

            while (true) {
                Socket connection = serverSocket.accept();
                new Thread(() -> {
                    clientListen(connection);
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void clientListen(Socket connection) {
        try {
            while (true) {
                try (ObjectInputStream stream = new ObjectInputStream(connection.getInputStream())) {
                    while (true) {
                        TCPMessage<C> msg = (TCPMessage<C>) stream.readObject();
                        receiveMessage(msg.session, msg.message);
                    }
                } catch (StreamCorruptedException | ClassNotFoundException e) {
                    System.out.println("Failed to deserialize class");
                }
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewSession(NewSessionEvent<C> event) {
        this.newSessionEvent = event;
    }

    @Override
    public <T> T recv(Session<C> session) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'recv'");
    }

    private void receiveMessage(Session<C> session, Serializable msg) {
        System.out.println("Received a new message with session " + session);

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

    // should synchronize on 'this' before calling this method
    private void enqueueSend(Session<C> session, Object msg) {
        if (!this.sendQueue.containsKey(session)) {
            this.sendQueue.put(session, new LinkedList<>());
            this.recvQueue.put(session, new LinkedList<>());
        }

        this.sendQueue.get(session).add(msg);
    }

    private void cleanupKey(Session<C> session) {
        synchronized (this) {
            this.sendQueue.remove(session);
            this.recvQueue.remove(session);
        }
    }
}

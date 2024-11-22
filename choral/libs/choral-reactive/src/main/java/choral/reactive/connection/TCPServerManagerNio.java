package choral.reactive.connection;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import choral.reactive.tracing.JaegerConfiguration;

public class TCPServerManagerNio implements ServerConnectionManager {

    private final OpenTelemetrySdk telemetry;
    private final ServerEvents serverEvents;
    private ServerSocketChannel serverSocket = null;
    private Selector selector = null;
    private final HashSet<ConnectionHandler> connections = new HashSet<>();
    private Span serverSpan = null;

    public TCPServerManagerNio(ServerEvents serverEvents, OpenTelemetrySdk telemetry) {
        this.telemetry = telemetry;
        this.serverEvents = serverEvents;
    }

    @Override
    public void listen(String address) throws URISyntaxException, IOException {

        this.serverSpan = telemetry.getTracer(JaegerConfiguration.TRACER_NAME)
                .spanBuilder("TCPServerManagerNio server listen")
                .setAttribute("server.address", address)
                .startSpan();

        try (Scope scope = serverSpan.makeCurrent();) {

            URI uri = new URI(null, address, null, null, null).parseServerAuthority();
            InetSocketAddress addr = new InetSocketAddress(uri.getHost(), uri.getPort());

            selector = Selector.open();
            serverSocket = ServerSocketChannel.open();
            serverSocket.bind(addr);
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);

            serverSpan.addEvent("TCPServerManagerNio: listening on " + address);

            while (true) {
                selector.select();

                if (!serverSocket.isOpen()) {
                    serverSpan.addEvent("Server socket closed");
                    break;
                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) {
                        SocketChannel client = serverSocket.accept();
                        serverSpan.addEvent(
                                String.format("Incoming Connection from %s", client.socket().getInetAddress()));
                        client.configureBlocking(false);
                        SelectionKey newKey = client.register(selector, SelectionKey.OP_READ);
                        ConnectionHandler connection = new ConnectionHandler(client, telemetry);
                        connections.add(connection);
                        newKey.attach(connection);
                    }

                    if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        ConnectionHandler connectionHandler = (ConnectionHandler) key.attachment();
                        try {
                            connectionHandler.read(client);
                        } catch (ClosedChannelException e) {
                            serverSpan.addEvent(String.format("Connection from %s closed",
                                    client.socket().getInetAddress()));
                            key.cancel();
                            connections.remove(connectionHandler);
                            connectionHandler.close();
                            client.close();
                        } catch (IOException e) {
                            connectionHandler.connectionSpan.setAttribute("error", true);
                            connectionHandler.connectionSpan.recordException(e,
                                    Attributes.builder().put("exception.context", "Read from client").build());
                            key.cancel();
                            connections.remove(connectionHandler);
                            connectionHandler.close();
                            client.close();
                        }
                    }

                    iter.remove();
                }
            }
        } catch (Exception e) {
            serverSpan.setAttribute("error", true);
            serverSpan.recordException(e);
            this.close();
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        for (var conn : connections) {
            conn.close();
        }
        connections.clear();

        if (serverSocket != null)
            serverSocket.close();

        if (selector != null)
            selector.close();

        if (serverSpan != null)
            serverSpan.end();

        serverSocket = null;
        selector = null;
        serverSpan = null;
    }

    private class ConnectionHandler implements AutoCloseable {
        private final SocketChannel client;
        private int objectSize = 0;
        private ByteBuffer buffer = ByteBuffer.allocate(1024).limit(4);
        public final Span connectionSpan;

        public ConnectionHandler(SocketChannel client, OpenTelemetrySdk telemetry) {
            this.client = client;
            Tracer tracer = telemetry.getTracer(JaegerConfiguration.TRACER_NAME);
            connectionSpan = tracer.spanBuilder("TCPServerManagerNio client connection")
                    .setAttribute("client.address", client.socket().getInetAddress().toString())
                    .startSpan();
        }

        public void read(SocketChannel client) throws IOException {

            if (objectSize == 0) {
                client.read(buffer);

                // Read object size
                if (buffer.remaining() > 0) {
                    return;
                }

                // Prepare for reading size
                buffer.flip();

                objectSize = buffer.getInt();

                connectionSpan.addEvent("Reading object of size: " + objectSize);

                if (objectSize < 0) {
                    // Close connection
                    System.out.println("Client closed connection");
                    connections.remove(this);
                    this.close();
                    client.close();
                    return;
                }

                if (buffer.capacity() >= objectSize) {
                    buffer.clear().limit(objectSize);
                } else {
                    buffer = ByteBuffer.allocate(objectSize);
                }
            }

            int n = client.read(buffer);
            connectionSpan.addEvent("Read " + n + " bytes, remaining=" + buffer.remaining());

            if (buffer.remaining() == 0) {
                buffer.flip();

                ByteArrayInputStream inputStream = new ByteArrayInputStream(
                        buffer.array(), buffer.position(), buffer.limit());

                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

                try {
                    Object message = objectInputStream.readObject();
                    System.out.println("TCPServerManagerNio received message: " + message.toString());
                    serverEvents.messageReceived(message);
                } catch (ClassNotFoundException e) {
                    connectionSpan.setAttribute("error", true);
                    connectionSpan.recordException(e,
                            Attributes.builder().put("exception.context", "Reading object from client").build());
                } finally {
                    objectInputStream.close();
                }

                // Prepare for next object
                objectSize = 0;
                buffer.clear().limit(4);
            }
        }

        @Override
        public void close() {
            connectionSpan.end();
        }

        @Override
        public int hashCode() {
            return client.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ConnectionHandler other = (ConnectionHandler) obj;
            if (client == null) {
                if (other.client != null)
                    return false;
            } else if (!client.equals(other.client))
                return false;
            return true;
        }
    }
}

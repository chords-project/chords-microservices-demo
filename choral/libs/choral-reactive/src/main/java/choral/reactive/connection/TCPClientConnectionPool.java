package choral.reactive.connection;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TCPClientConnectionPool implements ClientConnectionManager {

    public final String address;
    private final InetSocketAddress socketAddr;
    private Socket connection = null;
    private ObjectOutputStream stream = null;
    private CountDownLatch connectedLatch = new CountDownLatch(1);

    public TCPClientConnectionPool(String address) throws URISyntaxException, UnknownHostException, IOException {
        this.address = address;
        URI uri = new URI(null, address, null, null, null).parseServerAuthority();
        this.socketAddr = new InetSocketAddress(uri.getHost(), uri.getPort());
        connect();
    }

    public void connect() {
        Thread.ofPlatform()
            .start(() -> {
                for (int i = 0; i < 10; i++) {
                    try {
                        synchronized (TCPClientConnectionPool.this) {
                            this.connection = new Socket(socketAddr.getHostName(), socketAddr.getPort());
                            this.stream = new ObjectOutputStream(connection.getOutputStream());
                        }
                        System.out.println("TCPReactiveClientConnection successfully connected to server: " + address);
                        break;
                    } catch (Exception e) {
                        Duration waitTime = Duration.ofMillis((long) Math.pow(2, i) * 1000L);
                        System.out.println(
                            "TCPReactiveClientConnection failed to connect to client: attempt=" +
                            (i + 1) +
                            ", address=" +
                            address +
                            ", retryWait=" +
                            waitTime.toMillis() +
                            "ms"
                        );
                        try {
                            Thread.sleep(waitTime);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                            break;
                        }
                    }
                }

                this.connectedLatch.countDown();
            });
    }

    @Override
    public void close() throws IOException {
        stream.close();
        connection.close();
    }

    @Override
    public ClientConnection makeConnection() {
        return new ClientConnection();
    }

    public class ClientConnection implements ClientConnectionManager.Connection {

        @Override
        public void sendMessage(Serializable msg) throws IOException, InterruptedException {
            System.out.println("TCPReactiveClientConnection sendObject begin: " + msg.toString());
            connectedLatch.await(10, TimeUnit.SECONDS);
            synchronized (TCPClientConnectionPool.this) {
                System.out.println("TCPReactiveClientConnection sendObject inside lock");
                stream.writeObject(msg);
                stream.flush();
            }
            System.out.println("TCPReactiveClientConnection sendObject done");
        }

        @Override
        public void close() {}
    }
}

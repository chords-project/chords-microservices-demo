package choral.reactive;

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

public class TCPReactiveClientConnection implements AutoCloseable {

    public final String address;
    private final InetSocketAddress socketAddr;
    private Socket connection = null;
    private ObjectOutputStream stream = null;
    private CountDownLatch connectedLatch = new CountDownLatch(1);

    public TCPReactiveClientConnection(String address) throws URISyntaxException, UnknownHostException, IOException {
        this.address = address;
        URI uri = new URI(null, address, null, null, null).parseServerAuthority();
        this.socketAddr = new InetSocketAddress(uri.getHost(), uri.getPort());
        connect();
    }

    public void sendObject(Serializable object) throws IOException, InterruptedException {
        System.out.println("TCPReactiveClientConnection sendObject begin: " + object.toString());
        this.connectedLatch.await(10, TimeUnit.SECONDS);
        synchronized (this) {
            System.out.println("TCPReactiveClientConnection sendObject inside lock");
            stream.writeObject(object);
            stream.flush();
        }
        System.out.println("TCPReactiveClientConnection sendObject done");
    }

    public void connect() {
        TCPReactiveClientConnection self = this;
        Thread.ofPlatform()
            .start(() -> {
                for (int i = 0; i < 10; i++) {
                    try {
                        synchronized (self) {
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
}

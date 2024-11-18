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
    private Socket connection = null;
    private ObjectOutputStream stream = null;
    private CountDownLatch connectedLatch = new CountDownLatch(1);

    public TCPReactiveClientConnection(String address) throws URISyntaxException, UnknownHostException, IOException {
        URI uri = new URI(null, address, null, null, null).parseServerAuthority();
        InetSocketAddress addr = new InetSocketAddress(uri.getHost(), uri.getPort());

        this.address = address;

        Thread.ofPlatform().start(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    this.connection = new Socket(addr.getHostName(), addr.getPort());
                    this.stream = new ObjectOutputStream(connection.getOutputStream());
                    System.out.println("Successfully connected to client: " + address);
                    break;
                } catch (Exception e) {
                    Duration waitTime = Duration.ofMillis((long) Math.pow(2, i) * 500L);
                    System.out.println("Failed to connect to client: attempt=" + (i + 1) + ", address=" + address
                            + ", retryWait=" + waitTime.toMillis() + "ms");
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

    public void sendObject(Serializable object) throws IOException, InterruptedException {
        synchronized (this) {
            this.connectedLatch.await(10, TimeUnit.SECONDS);
            stream.writeObject(object);
            stream.flush();
        }
    }

    @Override
    public void close() throws IOException {
        stream.close();
        connection.close();
    }
}

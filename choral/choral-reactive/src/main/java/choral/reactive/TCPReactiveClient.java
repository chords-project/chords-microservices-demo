package choral.reactive;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

public class TCPReactiveClient<C> implements ReactiveSender<C, Serializable>, AutoCloseable {

    private Socket connection;

    public TCPReactiveClient(String address) throws URISyntaxException, UnknownHostException, IOException {
        System.out.println("TCPReactiveClient connecting to " + address);

        URI uri = new URI(null, address, null, null, null).parseServerAuthority();
        InetSocketAddress addr = new InetSocketAddress(uri.getHost(), uri.getPort());

        this.connection = new Socket(addr.getHostName(), addr.getPort());
        System.out.println("TCPReactiveClient connected to " + address);
    }

    @Override
    public void send(Session<C> session, Serializable msg) {
        System.out.println("TCPReactiveClient sending message on session: " + session);
        try (ObjectOutputStream stream = new ObjectOutputStream(connection.getOutputStream())) {
            TCPMessage<C> message = new TCPMessage<>(session, msg);
            stream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }

}

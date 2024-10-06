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

    private String address;
    private Socket connection;

    public TCPReactiveClient(String address) throws URISyntaxException, UnknownHostException, IOException {
        this.address = address;

        System.out.println("TCPReactiveClient connecting: address=" + address);

        URI uri = new URI(null, address, null, null, null).parseServerAuthority();
        InetSocketAddress addr = new InetSocketAddress(uri.getHost(), uri.getPort());

        this.connection = new Socket(addr.getHostName(), addr.getPort());
        System.out.println("TCPReactiveClient connected: address=" + address);
    }

    @Override
    public void send(Session<C> session, Serializable msg) {
        System.out.println("TCPReactiveClient sending message: address=" + address + " session=" + session);
        try (ObjectOutputStream stream = new ObjectOutputStream(connection.getOutputStream())) {
            TCPMessage<C> message = new TCPMessage<>(session, msg);
            stream.writeObject(message);
            stream.flush();
        } catch (IOException e) {
            System.out.println("TCPReactiveClient exception: address=" + address);
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        System.out.println("TCPReactiveClient closing: address=" + address);
        connection.close();
    }

}

package choral.reactive;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPReactiveClient<C> implements ReactiveSender<C, Serializable> {

    private Socket connection;

    public TCPReactiveClient(InetSocketAddress addr) throws UnknownHostException, IOException {
        this.connection = new Socket(addr.getHostName(), addr.getPort());
    }

    @Override
    public void send(Session<C> session, Serializable msg) {
        try (ObjectOutputStream stream = new ObjectOutputStream(connection.getOutputStream())) {
            TCPMessage<C> message = new TCPMessage<>(session, msg);
            stream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

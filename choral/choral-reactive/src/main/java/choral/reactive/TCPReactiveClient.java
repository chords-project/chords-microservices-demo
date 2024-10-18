package choral.reactive;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import io.opentelemetry.api.common.Attributes;
import choral.reactive.tracing.TelemetrySession;

public class TCPReactiveClient<C> implements ReactiveSender<C, Serializable>, Closeable {

    private String address;
    private Socket connection;
    private ObjectOutputStream stream;

    private TelemetrySession telemetrySession;

    public TCPReactiveClient(String address, TelemetrySession telemetrySession)
            throws URISyntaxException, UnknownHostException, IOException {
        this.address = address;
        this.telemetrySession = telemetrySession;

        System.out.println("TCPReactiveClient connecting: address=" + address);

        URI uri = new URI(null, address, null, null, null).parseServerAuthority();
        InetSocketAddress addr = new InetSocketAddress(uri.getHost(), uri.getPort());

        this.connection = new Socket(addr.getHostName(), addr.getPort());
        this.stream = new ObjectOutputStream(connection.getOutputStream());
        System.out.println("TCPReactiveClient connected: address=" + address);
    }

    @Override
    public void send(Session<C> session, Serializable msg) {
        System.out.println("TCPReactiveClient sending message: address=" + address + " session=" + session);
        try {
            TCPMessage<C> message = new TCPMessage<>(session, msg);

            telemetrySession.log("Send message", Attributes.builder().put("channel.recipient", address).build());

            telemetrySession.injectSessionContext(message);

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
        stream.close();
        connection.close();
    }

}

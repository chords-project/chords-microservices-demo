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

public class TCPReactiveClient<S extends Session> implements ReactiveSender<S, Serializable>, Closeable {

    private final String address;
    private final String serviceName;
    private final Socket connection;
    private final ObjectOutputStream stream;

    private final TelemetrySession telemetrySession;

    public TCPReactiveClient(String address, String serviceName, TelemetrySession telemetrySession)
            throws URISyntaxException, UnknownHostException, IOException {
        this.address = address;
        this.serviceName = serviceName;
        this.telemetrySession = telemetrySession;

        System.out.println("TCPReactiveClient connecting: service=" + serviceName + " address=" + address);

        URI uri = new URI(null, address, null, null, null).parseServerAuthority();
        InetSocketAddress addr = new InetSocketAddress(uri.getHost(), uri.getPort());

        this.connection = new Socket(addr.getHostName(), addr.getPort());
        this.stream = new ObjectOutputStream(connection.getOutputStream());
        System.out.println("TCPReactiveClient connected: service=" + serviceName + " address=" + address);
    }

    @Override
    public void send(S session, Serializable msg) {
        System.out.println("TCPReactiveClient sending message: service=" + serviceName + " address=" + address
                + " session=" + session);
        try {

            // Unchecked cast is safe since it's a precondition of the method.
            @SuppressWarnings("unchecked")
            S newSession = (S) session.replacingSender(serviceName);

            TCPMessage<S> message = new TCPMessage<>(newSession, msg);

            telemetrySession.log("Send message", Attributes.builder().put("channel.recipient", address).build());

            telemetrySession.injectSessionContext(message);

            stream.writeObject(message);
            stream.flush();
        } catch (IOException e) {
            System.out.println("TCPReactiveClient exception: service=" + serviceName + " address=" + address);
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        System.out.println("TCPReactiveClient closing: service=" + serviceName + " address=" + address);
        stream.close();
        connection.close();
    }

}

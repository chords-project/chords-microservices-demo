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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
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

        Span span = telemetrySession.tracer.spanBuilder("TCPReactiveClient connect: " + address)
                .setAttribute("channel.service", serviceName)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {

            URI uri = new URI(null, address, null, null, null).parseServerAuthority();
            InetSocketAddress addr = new InetSocketAddress(uri.getHost(), uri.getPort());

            this.connection = new Socket(addr.getHostName(), addr.getPort());
            this.stream = new ObjectOutputStream(connection.getOutputStream());

        } finally {
            span.end();
        }
    }

    @Override
    public void send(S session, Serializable msg) {
        try {
            // Unchecked cast is safe since it's a precondition of the method.
            @SuppressWarnings("unchecked")
            S newSession = (S) session.replacingSender(serviceName);

            TCPMessage<S> message = new TCPMessage<>(newSession, msg);

            telemetrySession.injectSessionContext(message);

            stream.writeObject(message);
            stream.flush();
        } catch (IOException e) {
            telemetrySession.recordException(
                    "Failed to send message",
                    e,
                    true,
                    Attributes.builder()
                            .put("service", serviceName)
                            .put("address", address).build());
        }
    }

    @Override
    public void close() throws IOException {
        log("TCPReactiveClient closing");
        stream.close();
        connection.close();
    }

    private void log(String message) {
        telemetrySession.log(message,
                Attributes.builder()
                        .put("address", address)
                        .put("service", serviceName).build());
    }

    @Override
    public ReactiveChannel_A<S, Serializable> chanA(S session) {
        return new ReactiveChannel_A<>(session, this, telemetrySession);
    }

    @Override
    public String toString() {
        return "TCPReactiveClient [address=" + address + ", serviceName=" + serviceName + "]";
    }

}

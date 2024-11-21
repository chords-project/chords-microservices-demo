package choral.reactive;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

import choral.reactive.tracing.JaegerConfiguration;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class TCPReactiveConnectionManagerSimple implements ClientConnectionManager {

    public final String address;
    private final InetSocketAddress socketAddr;
    private final OpenTelemetrySdk telemetry;

    public TCPReactiveConnectionManagerSimple(String address, OpenTelemetrySdk telemetry) throws URISyntaxException {
        this.address = address;
        this.telemetry = telemetry;

        URI uri = new URI(null, address, null, null, null).parseServerAuthority();
        this.socketAddr = new InetSocketAddress(uri.getHost(), uri.getPort());
    }

    @Override
    public ClientConnection makeConnection() throws IOException {
        return new ClientConnection();
    }

    private class ClientConnection implements ClientConnectionManager.Connection {

        private Socket connection = null;
        private ObjectOutputStream stream = null;
        private Tracer tracer;

        protected ClientConnection() throws IOException {
            this.tracer = telemetry.getTracer(JaegerConfiguration.TRACER_NAME);

            Span span = tracer.spanBuilder("ClientConnection connect").setAttribute("connection.address", address)
                    .startSpan();

            this.connection = new Socket(socketAddr.getHostName(), socketAddr.getPort());
            this.stream = new ObjectOutputStream(connection.getOutputStream());

            span.end();
        }

        @Override
        public void sendMessage(Serializable msg) throws IOException {
            stream.writeObject(msg);
            stream.flush();
        }

        @Override
        public void close() throws IOException {
            stream.close();
            connection.close();
        }

        @Override
        public String toString() {
            return "ClientConnection [address=" + address + "]";
        }
    }

    @Override
    public void close() throws IOException {
    }
}

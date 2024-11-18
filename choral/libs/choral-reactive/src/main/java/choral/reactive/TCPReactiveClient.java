package choral.reactive;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;

import choral.reactive.tracing.TelemetrySession;
import io.opentelemetry.api.common.Attributes;

public class TCPReactiveClient<S extends Session> implements ReactiveSender<S, Serializable>, Closeable {

    private final TCPReactiveClientConnection connection;
    private final String serviceName;

    private final TelemetrySession telemetrySession;

    public TCPReactiveClient(
            TCPReactiveClientConnection connection,
            String serviceName,
            TelemetrySession telemetrySession) {
        this.connection = connection;
        this.serviceName = serviceName;
        this.telemetrySession = telemetrySession;
    }

    @Override
    public void send(S session, Serializable msg) {
        try {
            // Unchecked cast is safe since it's a precondition of the method.
            @SuppressWarnings("unchecked")
            S newSession = (S) session.replacingSender(serviceName);

            TCPMessage<S> message = new TCPMessage<>(newSession, msg);

            telemetrySession.injectSessionContext(message);

            connection.sendObject(message);
        } catch (IOException | InterruptedException e) {
            telemetrySession.recordException(
                    "Failed to send message",
                    e,
                    true,
                    Attributes.builder()
                            .put("service", serviceName)
                            .put("address", connection.address).build());
        }
    }

    @Override
    public void close() throws IOException {
        log("TCPReactiveClient closing");
        connection.close();
    }

    private void log(String message) {
        telemetrySession.log(message,
                Attributes.builder()
                        .put("address", connection.address)
                        .put("service", serviceName).build());
    }

    @Override
    public ReactiveChannel_A<S, Serializable> chanA(S session) {
        return new ReactiveChannel_A<>(session, this, telemetrySession);
    }

    @Override
    public String toString() {
        return "TCPReactiveClient [address=" + connection.address + ", serviceName=" + serviceName + "]";
    }

}

package choral.reactive;

import choral.reactive.tracing.TelemetrySession;
import io.opentelemetry.api.common.Attributes;
import java.io.IOException;
import java.io.Serializable;
import choral.reactive.ClientConnectionManager.Connection;

public class ReactiveClient<S extends Session> implements ReactiveSender<S, Serializable>, AutoCloseable {

    private final Connection connection;
    private final String serviceName;

    private final TelemetrySession telemetrySession;

    public ReactiveClient(ClientConnectionManager connectionManager, String serviceName,
            TelemetrySession telemetrySession) throws IOException, InterruptedException {
        this.connection = connectionManager.makeConnection();
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

            connection.sendMessage(message);
        } catch (IOException | InterruptedException e) {
            telemetrySession.recordException(
                    "Failed to send message",
                    e,
                    true,
                    Attributes.builder().put("service", serviceName).put("connection", connection.toString()).build());
        }
    }

    private void log(String message) {
        telemetrySession.log(message,
                Attributes.builder().put("connection", connection.toString()).put("service", serviceName).build());
    }

    @Override
    public ReactiveChannel_A<S, Serializable> chanA(S session) {
        return new ReactiveChannel_A<>(session, this, telemetrySession);
    }

    @Override
    public String toString() {
        return "TCPReactiveClient [connection=" + connection.toString() + ", serviceName=" + serviceName + "]";
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}

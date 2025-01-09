package choral.reactive;

import choral.reactive.connection.ClientConnectionManager;
import choral.reactive.connection.ClientConnectionManager.Connection;
import choral.reactive.connection.Message;
import choral.reactive.tracing.TelemetrySession;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeoutException;

public class ReactiveClient implements ReactiveSender<Serializable>, AutoCloseable {

    private final Connection connection;
    private final String serviceName;
    private int nextSequence = 1;

    private final TelemetrySession telemetrySession;

    public ReactiveClient(ClientConnectionManager connectionManager, String serviceName,
            TelemetrySession telemetrySession)
            throws IOException, InterruptedException {
        this.connection = connectionManager.makeConnection();
        this.serviceName = serviceName;
        this.telemetrySession = telemetrySession;
    }

    @Override
    public void send(Session session, Serializable msg) {
        Session newSession = session.replacingSender(serviceName);

        Span span = telemetrySession.tracer.spanBuilder("Send message ("+connection+")")
                .setAttribute("channel.session", newSession.toString())
                .setAttribute("channel.message", msg.toString())
                .setAttribute("channel.connection", connection.toString())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            Message message = new Message(newSession, msg, nextSequence);
            nextSequence++;

            telemetrySession.injectSessionContext(message);

            connection.sendMessage(message);
        } catch (Exception e) {
            span.setAttribute("error", true);
            span.recordException(e);

            telemetrySession.recordException(
                    "Failed to send message",
                    e,
                    true,
                    Attributes.builder().put("service", serviceName).put("connection", connection.toString()).build());
        } finally {
            span.end();
        }
    }

    @Override
    public ReactiveChannel_A<Serializable> chanA(Session session) {
        return new ReactiveChannel_A<>(session, this, telemetrySession);
    }

    @Override
    public String toString() {
        return "ReactiveClient [connection=" + connection.toString() + ", serviceName=" + serviceName + "]";
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}

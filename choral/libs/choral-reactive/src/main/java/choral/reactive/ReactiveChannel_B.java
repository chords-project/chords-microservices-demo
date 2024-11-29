package choral.reactive;

import choral.channels.DiChannel_B;
import choral.lang.Unit;
import choral.reactive.tracing.TelemetrySession;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

public class ReactiveChannel_B<M> implements DiChannel_B<M> {
    private final Session session;
    private final ReactiveReceiver<M> receiver;
    private final TelemetrySession telemetrySession;

    public ReactiveChannel_B(Session session, ReactiveReceiver<M> receiver,
            TelemetrySession telemetrySession) {
        this.session = session;
        this.receiver = receiver;
        this.telemetrySession = telemetrySession;
    }

    @Override
    public <T extends M> T com() {
        Span span = telemetrySession.tracer.spanBuilder("ReactiveChannel receive message")
                .setAttribute("channel.session", session.toString())
                .setAttribute("channel.receiver", receiver.toString())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            T msg = receiver.<T>recv(session);
            span.setAttribute("channel.message", msg.toString());
            return msg;
        } finally {
            span.end();
        }
    }

    @Override
    public <T extends M> T com(Unit unit) {
        return com();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T>> T select() {
        Span span = telemetrySession.tracer.spanBuilder("ReactiveChannel receive select label")
                .setAttribute("channel.session", session.toString())
                .setAttribute("channel.receiver", receiver.toString())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            T msg = (T) receiver.<M>recv(session);
            span.setAttribute("channel.label", msg.toString());
            return msg;
        } finally {
            span.end();
        }
    }

    @Override
    public <T extends Enum<T>> T select(Unit unit) {
        return select();
    }

}

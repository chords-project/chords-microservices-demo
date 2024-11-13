package choral.reactive;

import choral.channels.DiChannel_A;
import choral.lang.Unit;
import choral.reactive.tracing.TelemetrySession;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

public class ReactiveChannel_A<S extends Session, M> implements DiChannel_A<M> {

    public final S session;
    private final ReactiveSender<S, M> sender;
    private final TelemetrySession telemetrySession;

    public ReactiveChannel_A(S session, ReactiveSender<S, M> sender, TelemetrySession telemetrySession) {
        this.session = session;
        this.sender = sender;
        this.telemetrySession = telemetrySession;
    }

    @Override
    public <T extends M> Unit com(T msg) {
        Span span = telemetrySession.tracer.spanBuilder("ReactiveChannel send message")
                .setAttribute("channel.session", session.toString())
                .setAttribute("channel.message", msg.toString())
                .setAttribute("channel.sender", sender.toString())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {

            // Associates each message with the key
            sender.send(session, msg);

        }

        span.end();

        return Unit.id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T>> Unit select(T msg) {
        Span span = telemetrySession.tracer.spanBuilder("ReactiveChannel send message")
                .setAttribute("channel.session", session.toString())
                .setAttribute("channel.message", msg.toString())
                .setAttribute("channel.sender", sender.toString())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            sender.send(session, (M) msg);
        }

        span.end();

        return Unit.id;
    }
}

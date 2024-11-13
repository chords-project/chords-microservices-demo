package choral.reactive;

import choral.channels.DiChannel_A;
import choral.lang.Unit;
import choral.reactive.tracing.TelemetrySession;
import io.opentelemetry.api.common.Attributes;

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
        Attributes attributes = Attributes.builder()
                .put("channel.session", session.toString())
                .put("channel.message", msg.toString())
                .build();

        telemetrySession.log("ReactiveChannel sending message", attributes);

        // Associates each message with the key
        sender.send(session, msg);

        telemetrySession.log("ReactiveChannel message sent", attributes);

        return Unit.id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T>> Unit select(T msg) {
        Attributes attributes = Attributes.builder()
                .put("channel.session", session.toString())
                .put("channel.label", msg.toString())
                .build();

        telemetrySession.log("ReactiveChannel sending select label", attributes);

        Object msgO = msg;
        sender.send(session, (M) msgO);

        telemetrySession.log("ReactiveChannel sent select label", attributes);

        return Unit.id;
    }
}

package choral.reactive;

import choral.channels.DiChannel_B;
import choral.lang.Unit;
import choral.reactive.tracing.TelemetrySession;
import io.opentelemetry.api.common.Attributes;

public class ReactiveChannel_B<S extends Session, M> implements DiChannel_B<M> {
    private final S session;
    private final ReactiveReceiver<S, M> receiver;
    private final TelemetrySession telemetrySession;

    public ReactiveChannel_B(S session, ReactiveReceiver<S, M> receiver, TelemetrySession telemetrySession) {
        this.session = session;
        this.receiver = receiver;
        this.telemetrySession = telemetrySession;
    }

    @Override
    public <T extends M> T com() {
        telemetrySession.log("ReactiveChannel receiving message",
                Attributes.builder()
                        .put("channel.session", session.toString())
                        .build());

        T msg = receiver.<T>recv(session);

        telemetrySession.log("ReactiveChannel message received",
                Attributes.builder()
                        .put("channel.session", session.toString())
                        .put("channel.message", msg.toString())
                        .build());

        return msg;
    }

    @Override
    public <T extends M> T com(Unit unit) {
        return com();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T>> T select() {

        telemetrySession.log("ReactiveChannel receiving select label",
                Attributes.builder()
                        .put("channel.session", session.toString())
                        .build());

        Object value = receiver.<M>recv(session);

        telemetrySession.log("ReactiveChannel received select label",
                Attributes.builder()
                        .put("channel.session", session.toString())
                        .put("channel.label", value.toString())
                        .build());

        return (T) value;
    }

    @Override
    public <T extends Enum<T>> T select(Unit unit) {
        return select();
    }

}

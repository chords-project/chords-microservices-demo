package choral.reactive;

import choral.channels.AsyncDiChannel_B;
import choral.channels.Future;
import choral.lang.Unit;
import choral.reactive.tracing.TelemetrySession;

public class ReactiveChannel_B<M> implements AsyncDiChannel_B<M> {
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
    public <T extends M> Future<T> fcom() {
        return receiver.recv(session);
    }

    @Override
    public <T extends M> Future<T> fcom(Unit unit) {
        return fcom();
    }

//    @SuppressWarnings("unchecked")
//    @Override
//    public <T extends Enum<T>> T select() {
//        Span span = telemetrySession.tracer.spanBuilder("ReactiveChannel receive select label")
//                .setAttribute("channel.session", session.toString())
//                .setAttribute("channel.receiver", receiver.toString())
//                .startSpan();
//
//        try (Scope scope = span.makeCurrent()) {
//            T msg = (T) receiver.<M>recv(session);
//            span.setAttribute("channel.label", msg.toString());
//            return msg;
//        } finally {
//            span.end();
//        }
//    }
//
//    @Override
//    public <T extends Enum<T>> T select(Unit unit) {
//        return select();
//    }

}

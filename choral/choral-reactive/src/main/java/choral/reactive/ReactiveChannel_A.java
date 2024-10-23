package choral.reactive;

import choral.channels.DiChannel_A;
import choral.lang.Unit;

public class ReactiveChannel_A<S extends Session, M> implements DiChannel_A<M> {

    public final S session;
    private final ReactiveSender<S, M> sender;

    public ReactiveChannel_A(S session, ReactiveSender<S, M> sender) {
        this.session = session;
        this.sender = sender;
    }

    @Override
    public <T extends M> Unit com(T msg) {
        // Associates each message with the key
        sender.send(session, msg);
        return Unit.id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T>> Unit select(T msg) {
        Object msgO = msg;
        sender.send(session, (M) msgO);
        return Unit.id;
    }
}

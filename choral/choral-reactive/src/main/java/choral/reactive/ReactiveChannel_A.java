package choral.reactive;

import choral.channels.DiChannel_A;
import choral.lang.Unit;

public class ReactiveChannel_A<C, M> implements DiChannel_A<M> {

    public final Session<C> session;
    private final ReactiveSender<C, M> sender;

    public ReactiveChannel_A(Session<C> session, ReactiveSender<C, M> sender) {
        this.session = session;
        this.sender = sender;
    }

    @Override
    public <S extends M> Unit com(S msg) {
        // Associates each message with the key
        sender.send(session, msg);
        return Unit.id;
    }

    @Override
    public <T extends Enum<T>> Unit select(T msg) {
        throw new RuntimeException("Select: not implemented yet");
        // return com(msg);
    }
}

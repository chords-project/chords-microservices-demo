package choral.reactive;

import choral.channels.DiChannel_B;
import choral.lang.Unit;

public class ReactiveChannel_B<S extends Session, M> implements DiChannel_B<M> {
    private final S session;
    private final ReactiveReceiver<S, M> receiver;

    public ReactiveChannel_B(S session, ReactiveReceiver<S, M> receiver) {
        this.session = session;
        this.receiver = receiver;
    }

    @Override
    public <S extends M> S com() {
        return receiver.<S>recv(session);
    }

    @Override
    public <S extends M> S com(Unit unit) {
        return com();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T>> T select() {
        Object value = receiver.<M>recv(session);
        return (T) value;
    }

    @Override
    public <T extends Enum<T>> T select(Unit unit) {
        return select();
    }

}

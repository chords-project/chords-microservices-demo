package choral.reactive;

import choral.channels.DiChannel_B;
import choral.lang.Unit;

public class ReactiveChannel_B<C, M> implements DiChannel_B<M> {
    private final Session<C> session;
    private final ReactiveReceiver<C, M> receiver;

    public ReactiveChannel_B(Session<C> session, ReactiveReceiver<C, M> receiver) {
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

    @Override
    public <T extends Enum<T>> T select() {
        return receiver.<T>recv(session);
    }

    @Override
    public <T extends Enum<T>> T select(Unit unit) {
        return select();
    }

}

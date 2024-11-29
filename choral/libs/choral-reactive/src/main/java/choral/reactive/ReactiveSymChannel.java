package choral.reactive;

import choral.channels.SymChannel_A;
import choral.channels.SymChannel_B;
import choral.lang.Unit;

public class ReactiveSymChannel<M> implements SymChannel_A<M>, SymChannel_B<M> {

    private ReactiveChannel_A<M> chanA;
    private ReactiveChannel_B<M> chanB;

    public ReactiveSymChannel(ReactiveChannel_A<M> chanA, ReactiveChannel_B<M> chanB) {
        this.chanA = chanA;
        this.chanB = chanB;
    }

    @Override
    public <T extends M> Unit com(T msg) {
        return chanA.com(msg);
    }

    @Override
    public <T extends Enum<T>> Unit select(T msg) {
        return chanA.select(msg);
    }

    @Override
    public <T extends M> T com() {
        return chanB.com();
    }

    @Override
    public <T extends M> T com(Unit arg0) {
        return com();
    }

    @Override
    public <T extends Enum<T>> T select() {
        return chanB.select();
    }

    @Override
    public <T extends Enum<T>> T select(Unit arg0) {
        return select();
    }

}

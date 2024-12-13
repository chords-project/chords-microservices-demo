package choral.reactive;

import choral.channels.*;
import choral.lang.Unit;

public class ReactiveSymChannel<M> implements AsyncSymChannel_A<M>, AsyncSymChannel_B<M> {

    private ReactiveChannel_A<M> chanA;
    private ReactiveChannel_B<M> chanB;

    public ReactiveSymChannel(ReactiveChannel_A<M> chanA, ReactiveChannel_B<M> chanB) {
        this.chanA = chanA;
        this.chanB = chanB;
    }

    @Override
    public <T extends M> Unit fcom(T msg) {
        return chanA.fcom(msg);
    }

//    @Override
//    public <T extends Enum<T>> Unit select(T msg) {
//        return chanA.select(msg);
//    }

    @Override
    public <T extends M> Future<T> fcom() {
        return chanB.fcom();
    }

    @Override
    public <T extends M> Future<T> fcom(Unit unit) {
        return fcom();
    }

//    @Override
//    public <T extends Enum<T>> T select() {
//        return chanB.select();
//    }

//    @Override
//    public <T extends Enum<T>> T select(Unit unit) {
//        return select();
//    }

}

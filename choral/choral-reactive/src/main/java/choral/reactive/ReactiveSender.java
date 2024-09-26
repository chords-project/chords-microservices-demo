package choral.reactive;

public interface ReactiveSender<C, M> {
    void send(Session<C> session, M msg);

    default ReactiveChannel_A<C, M> chanA(Session<C> session) {
        return new ReactiveChannel_A<>(session, this);
    }
}

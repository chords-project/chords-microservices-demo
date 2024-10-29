package choral.reactive;

public interface ReactiveSender<S extends Session, M> {
    void send(S session, M msg);

    default ReactiveChannel_A<S, M> chanA(S session) {
        return new ReactiveChannel_A<>(session, this);
    }
}

package choral.reactive;

public interface ReactiveSender<S extends Session, M> {
    void send(S session, M msg);

    ReactiveChannel_A<S, M> chanA(S session);
}

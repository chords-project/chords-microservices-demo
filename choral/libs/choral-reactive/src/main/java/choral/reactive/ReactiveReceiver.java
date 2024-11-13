package choral.reactive;

public interface ReactiveReceiver<S extends Session, M> {
    public <T extends M> T recv(S session);
}

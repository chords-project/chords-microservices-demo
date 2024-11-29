package choral.reactive;

public interface ReactiveReceiver<M> {
    public <T extends M> T recv(Session session);
}

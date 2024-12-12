package choral.reactive;

public interface ReactiveReceiver<M> {
    /**
     * Waits (if necessary) for a message to arrive for the given Session, and returns the message.
     */
    public <T extends M> T recv(Session session);
}

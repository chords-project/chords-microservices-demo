package choral.reactive;

public interface ReactiveReceiver<S extends Session, M> {
    // public interface NewSessionEvent<C> {
    // void onNewSession(Session<C> session);
    // }

    // public void onNewSession(NewSessionEvent<C> event);

    public <T extends M> T recv(S session);

    default ReactiveChannel_B<S, M> chanB(S session) {
        return new ReactiveChannel_B<>(session, this);
    }
}

package choral.reactive;

public interface ReactiveReceiver<C, M> {
    public interface NewSessionEvent<C> {
        void onNewSession(Session<C> session, Runnable cleanup);
    }

    public void onNewSession(NewSessionEvent<C> event);

    public <T> T recv(Session<C> session);

    default ReactiveChannel_B<C, M> chanB(Session<C> session) {
        return new ReactiveChannel_B<>(session, this);
    }
}

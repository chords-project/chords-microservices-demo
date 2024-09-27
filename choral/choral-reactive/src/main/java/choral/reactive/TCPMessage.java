package choral.reactive;

import java.io.Serializable;

public class TCPMessage<C> implements Serializable {
    public final Session<C> session;
    public final Serializable message;

    public TCPMessage(Session<C> session, Serializable message) {
        this.session = session;
        this.message = message;
    }
}

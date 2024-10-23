package choral.reactive;

import java.io.Serializable;

public interface Session extends Serializable {
    String choreographyName();

    String senderName();

    Integer sessionID();

    /**
     * replacingSender returns a new Session with the new senderName
     * Precondition: the returned session should be of the same concrete type
     */
    Session replacingSender(String senderName);
}

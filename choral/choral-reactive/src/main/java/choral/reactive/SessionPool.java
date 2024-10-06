package choral.reactive;

import java.util.Set;
import java.util.HashSet;

public class SessionPool<C> {

    private final Set<Session<C>> sessions;

    public SessionPool() {
        this.sessions = new HashSet<>();
    }

    public boolean registerSession(Session<C> session) {
        synchronized (this) {
            boolean newSession = sessions.add(session);
            return newSession;
        }
    }

}

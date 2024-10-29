package choral.reactive;

import java.util.Random;

public class SimpleSession implements Session {

    public final String choreographyID;
    public final String sender;
    public final Integer sessionID;

    public SimpleSession(String choreographyID, String sender, Integer sessionID) {
        this.choreographyID = choreographyID;
        this.sender = sender;
        this.sessionID = sessionID;
    }

    public static SimpleSession makeSession(String choreographyID, String sender) {
        Random rand = new Random();
        return new SimpleSession(choreographyID, sender, Math.abs(rand.nextInt()));
    }

    // public static ReplaceSenderOperation<SimpleSession> replaceSession(String
    // thisSender) {
    // return (s) -> {
    // return new SimpleSession(s.choreographyID, thisSender, s.sessionID);
    // };
    // }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof SimpleSession))
            return false;

        SimpleSession that = (SimpleSession) other;
        return this.choreographyID.equals(that.choreographyID)
                && this.sessionID.equals(that.sessionID)
                && this.sender.equals(that.sender);
    }

    @Override
    public int hashCode() {
        return sessionID.hashCode() * 13 + sender.hashCode() * 3 * 13 + choreographyID.hashCode();
    }

    @Override
    public String toString() {
        return "SimpleSession [ " + choreographyName() + ", " + senderName() + ", " + sessionID + " ]";
    }

    @Override
    public String choreographyName() {
        return choreographyID;
    }

    @Override
    public String senderName() {
        return sender;
    }

    @Override
    public Integer sessionID() {
        return sessionID;
    }

    @Override
    public Session replacingSender(String senderName) {
        return new SimpleSession(this.choreographyID, senderName, this.sessionID);
    }
}

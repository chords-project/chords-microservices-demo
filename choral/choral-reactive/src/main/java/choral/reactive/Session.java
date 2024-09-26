package choral.reactive;

import java.util.Random;

public class Session<C> {
    public final C choreographyID;
    public final Integer sessionID;

    public Session(C choreographyID, Integer sessionID) {
        this.choreographyID = choreographyID;
        this.sessionID = sessionID;
    }

    public static <C> Session<C> makeSession(C choreographyID) {
        Random rand = new Random();
        return new Session<>(choreographyID, Math.abs(rand.nextInt()));
    }

    public static Session<String> fromString(String encoded) throws Exception {
        String[] split = encoded.split("\\$", 2);

        if (split.length != 2) {
            throw new Exception("Expected session to contain '$'");
        }

        return new Session<>(split[0], Integer.parseInt(split[1]));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof Session))
            return false;

        Session<?> that = (Session<?>) other;
        return this.choreographyID.equals(that.choreographyID) && this.sessionID.equals(that.sessionID);
    }

    @Override
    public int hashCode() {
        return sessionID.hashCode() * 13 + choreographyID.hashCode();
    }

    @Override
    public String toString() {
        return choreographyID.toString() + "$" + sessionID;
    }
}

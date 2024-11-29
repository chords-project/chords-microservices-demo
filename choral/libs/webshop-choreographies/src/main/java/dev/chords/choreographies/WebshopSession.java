package dev.chords.choreographies;

import java.util.Random;

import choral.reactive.Session;

public class WebshopSession extends Session {

    public final Choreography choreography;
    public final Service service;

    public WebshopSession(Session session) throws IllegalArgumentException {
        super(session.choreographyName(), session.senderName(), session.sessionID());

        choreography = Choreography.valueOf(session.choreographyName());
        service = Service.valueOf(session.senderName());
    }

    public WebshopSession(Choreography choreography, Service service, Integer sessionID) {
        super(choreography.name(), service.name(), sessionID);
        this.choreography = choreography;
        this.service = service;
    }

    public static WebshopSession makeSession(Choreography choreography, Service service) {
        Random rand = new Random();
        return new WebshopSession(choreography, service, Math.abs(rand.nextInt()));
    }

    public static enum Choreography {
        ADD_CART_ITEM, GET_CART_ITEMS, PLACE_ORDER;
    }

    public static enum Service {
        CART, CURRENCY, FRONTEND, PAYMENT, PRODUCT_CATALOG, SHIPPING;
    }

    @Override
    public String choreographyName() {
        return choreography.name();
    }

    @Override
    public String senderName() {
        return service.name();
    }

    @Override
    public Integer sessionID() {
        return sessionID;
    }

    @Override
    public String toString() {
        return "WebshopSession [ " + choreographyName() + ", " + senderName() + ", " + sessionID + " ]";
    }

    @Override
    public Session replacingSender(String senderName) {
        return new WebshopSession(choreography, Service.valueOf(senderName), sessionID);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + choreography.hashCode();
        result = prime * result + service.hashCode();
        result = prime * result + sessionID.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        WebshopSession other = (WebshopSession) obj;

        return choreography == other.choreography &&
                service == other.service &&
                sessionID.equals(other.sessionID);
    }

}

package dev.chords.choreographies;

import java.io.Serializable;
import java.util.UUID;
import choral.channels.DiChannel;

public class ChorPlaceOrder@(Client, Cart) {

    public ChorPlaceOrder() {}

    public void placeOrder(ReqPlaceOrder@Client req) {
        System@Client.out.println("Starting place order choreography: user_id="@Client + req.userID + " user_currency="@Client + req.userCurrency);

        UUID@Client orderID = UUID@Client.randomUUID();
    }
}
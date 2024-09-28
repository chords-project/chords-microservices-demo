package dev.chord.choreographies;

import java.io.Serializable;
import choral.channels.DiChannel;

public class ChorAddCartItem@(Client, Cart) {
    private CartService@Cart cart;

    public DiChannel@(Client, Cart)<Serializable> ch;

    public ChorAddCartItem(DiChannel@(Client, Cart)<Serializable> ch, CartService@Cart cart) {
        this.ch = ch;
        this.cart = cart;
    }

    public void addItem(String@Client userID, String@Client productID, int@Client quantity) {
        ReqAddItem@Client req_client = new ReqAddItem@Client(userID, productID, quantity);
        ReqAddItem@Cart req_cart = ch.<ReqAddItem>com(req_client);

        cart.addItem(req_cart.userID, req_cart.productID, req_cart.quantity);
    }
}
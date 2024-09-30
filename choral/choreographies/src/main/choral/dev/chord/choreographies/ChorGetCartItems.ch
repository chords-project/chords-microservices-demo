package dev.chord.choreographies;

import java.io.Serializable;
import choral.channels.DiChannel;

public class ChorGetCartItems@(Client, Cart) {
    private CartService@Cart cartSvc;

    public DiChannel@(Client, Cart)<Serializable> ch_clientCart;
    public DiChannel@(Cart, Client)<Serializable> ch_cartClient;

    public ChorGetCartItems(
        DiChannel@(Client, Cart)<Serializable> ch_clientCart,
        DiChannel@(Cart, Client)<Serializable> ch_cartClient,
        CartService@Cart cart
    ) {
        this.ch_clientCart = ch_clientCart;
        this.ch_cartClient = ch_cartClient;
        this.cartSvc = cart;
    }

    public Cart@Client getItems(String@Client userID) {
        ReqGetCartItems@Cart req_cart = ch_clientCart.<ReqGetCartItems>com(new ReqGetCartItems@Client(userID));

        Cart@Cart cart = cartSvc.getCart(req_cart.userID);
        Cart@Client cart_client = ch_cartClient.<Cart>com(cart);

        return cart_client;
    }
}
package dev.chords.choreographies;

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
        System@Client.out.println("Starting getItems choreography"@Client);
        System@Cart.out.println("Starting getItems choreography"@Cart);

        ReqGetCartItems@Cart req_cart = ch_clientCart.<ReqGetCartItems>com(new ReqGetCartItems@Client(userID));

        Cart@Cart cart = cartSvc.getCart(req_cart.userID);
        System@Cart.out.println("Got cart for user: "@Cart + req_cart.userID);

        Cart@Client cart_client = ch_cartClient.<Cart>com(cart);
        System@Client.out.println("Got back getItems reply"@Client);

        return cart_client;
    }
}
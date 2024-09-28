package dev.chord.microservices.cartservice;

import java.net.InetSocketAddress;

import choral.reactive.TCPReactiveServer;
import dev.chord.choreographies.ChorAddCartItem_Cart;
import dev.chord.choreographies.WebshopChoreography;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral cart service");
        InetSocketAddress address = new InetSocketAddress(5400);

        CartService cartService = new CartService(null);

        TCPReactiveServer<WebshopChoreography> server = new TCPReactiveServer<>();

        server.onNewSession((session) -> {
            switch (session.choreographyID) {
                case ADD_CART_ITEM:
                    ChorAddCartItem_Cart addItemChor = new ChorAddCartItem_Cart(server.chanB(session), cartService);
                    addItemChor.addItem();
                    break;
                default:
                    System.out.println("Invalid choreography ID " + session.choreographyID);
                    break;
            }
        });

        server.listen(address);
    }
}
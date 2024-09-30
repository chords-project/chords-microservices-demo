package dev.chord.microservices.cartservice;

import java.net.InetSocketAddress;

import choral.reactive.TCPReactiveServer;
import dev.chord.choreographies.ChorAddCartItem_Cart;
import dev.chord.choreographies.WebshopChoreography;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting choral cart service");

        int choralPort = Integer.parseInt(System.getenv().getOrDefault("CHORAL_PORT", "5400"));
        InetSocketAddress address = new InetSocketAddress(choralPort);

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("ASPNETCORE_HTTP_PORTS", "7070"));
        CartService cartService = new CartService(new InetSocketAddress("localhost", rpcPort));

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
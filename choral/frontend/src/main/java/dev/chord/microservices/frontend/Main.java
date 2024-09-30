package dev.chord.microservices.frontend;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import choral.reactive.Session;
import choral.reactive.TCPReactiveClient;
import dev.chord.choreographies.ChorAddCartItem_Client;
import dev.chord.choreographies.WebshopChoreography;

public class Main {
    public static void main(String[] args) throws URISyntaxException, UnknownHostException, IOException {
        System.out.println("Starting choral frontend service");

        String cartHost = System.getenv().getOrDefault("CHORAL_CART_HOST", "localhost:5400");
        URI address = new URI(null, cartHost, null, null, null).parseServerAuthority();

        TCPReactiveClient<WebshopChoreography> cartSvc = new TCPReactiveClient<>(
                new InetSocketAddress(address.getHost(), address.getPort()));

        // Add item
        ChorAddCartItem_Client addItemChor = new ChorAddCartItem_Client(
                cartSvc.chanA(Session.makeSession(WebshopChoreography.ADD_CART_ITEM)));

        addItemChor.addItem("user1", "product1", 1);
    }
}
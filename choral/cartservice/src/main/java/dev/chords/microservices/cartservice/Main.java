package dev.chords.microservices.cartservice;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import choral.reactive.TCPReactiveClient;
import choral.reactive.TCPReactiveServer;
import dev.chords.choreographies.ChorAddCartItem_Cart;
import dev.chords.choreographies.ChorGetCartItems_Cart;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopChoreography;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral cart service");

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("ASPNETCORE_HTTP_PORTS", "7070"));
        CartService cartService = new CartService(new InetSocketAddress("localhost", rpcPort));

        TCPReactiveServer<WebshopChoreography> frontendServer = new TCPReactiveServer<>();
        frontendServer.onNewSession((session) -> {
            switch (session.choreographyID) {
                case ADD_CART_ITEM:
                    System.out.println("New ADD_CART_ITEM request");
                    ChorAddCartItem_Cart addItemChor = new ChorAddCartItem_Cart(frontendServer.chanB(session),
                            cartService);
                    addItemChor.addItem();
                    break;
                case GET_CART_ITEMS:
                    System.out.println("New GET_CART_ITEMS request");
                    try (TCPReactiveClient<WebshopChoreography> clientConn = new TCPReactiveClient<WebshopChoreography>(
                            ServiceResources.shared.cartToFrontend);) {

                        ChorGetCartItems_Cart getItemsChor = new ChorGetCartItems_Cart(
                                frontendServer.chanB(session), clientConn.chanA(session), cartService);
                        getItemsChor.getItems();

                    } catch (URISyntaxException | IOException e) {
                        e.printStackTrace();
                    }

                    break;
                default:
                    System.out.println("Invalid choreography ID " + session.choreographyID);
                    break;
            }
        });

        frontendServer.listen(ServiceResources.shared.frontendToCart);
    }
}
package dev.chord.microservices.frontend;

import java.net.URISyntaxException;

import choral.reactive.Session;
import choral.reactive.TCPReactiveClient;
import choral.reactive.TCPReactiveServer;
import dev.chord.choreographies.Cart;
import dev.chord.choreographies.ChorAddCartItem_Client;
import dev.chord.choreographies.ChorGetCartItems_Client;
import dev.chord.choreographies.ServiceResources;
import dev.chord.choreographies.WebshopChoreography;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral frontend service");

        TCPReactiveServer<WebshopChoreography> cartServer = new TCPReactiveServer<>();

        cartServer.onNewSession((session) -> {
            // No choreographies are instantiated by a new session...
        });

        Thread cartServerThread = new Thread(() -> {
            try {
                cartServer.listen(ServiceResources.shared.cartToFrontend);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        });
        cartServerThread.start();
        Thread.sleep(1000); // Wait for server to start up

        try (TCPReactiveClient<WebshopChoreography> cartSvc = new TCPReactiveClient<>(
                ServiceResources.shared.frontendToCart)) {

            // Add item
            // ChorAddCartItem_Client addItemChor = new ChorAddCartItem_Client(
            // cartSvc.chanA(Session.makeSession(WebshopChoreography.ADD_CART_ITEM)));

            // addItemChor.addItem("user1", "product1", 1);

            // Get items
            Session<WebshopChoreography> getItemsSession = Session.makeSession(WebshopChoreography.GET_CART_ITEMS);
            ChorGetCartItems_Client getItemsChor = new ChorGetCartItems_Client(
                    cartSvc.chanA(getItemsSession), cartServer.chanB(getItemsSession));

            Cart cart = getItemsChor.getItems("user1");
            System.out.println("Got back cart: " + cart.userID + ", " + cart.items);

            cartServer.close();
            cartServerThread.join();
        }
    }
}
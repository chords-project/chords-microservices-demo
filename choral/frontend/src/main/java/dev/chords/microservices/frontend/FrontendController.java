package dev.chords.microservices.frontend;

import java.net.URISyntaxException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import choral.reactive.Session;
import choral.reactive.TCPReactiveClient;
import choral.reactive.TCPReactiveServer;
import dev.chords.choreographies.Cart;
import dev.chords.choreographies.ChorGetCartItems_Client;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopChoreography;

@RestController
public class FrontendController {

    TCPReactiveServer<WebshopChoreography> cartToFrontendServer;

    public FrontendController() {
        cartToFrontendServer = new TCPReactiveServer<>();

        cartToFrontendServer.onNewSession((session) -> {
            // No choreographies are instantiated by a new session...
        });

        Thread cartServerThread = new Thread(() -> {
            try {
                cartToFrontendServer.listen(ServiceResources.shared.cartToFrontend);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }, "SERVER_CART_TO_FRONTEND");
        cartServerThread.start();
    }

    @GetMapping("/cart/{userID}")
    String cart(@PathVariable String userID) {

        try (TCPReactiveClient<WebshopChoreography> cartSvc = new TCPReactiveClient<>(
                ServiceResources.shared.frontendToCart)) {

            // Get items
            Session<WebshopChoreography> getItemsSession = Session.makeSession(WebshopChoreography.GET_CART_ITEMS);
            ChorGetCartItems_Client getItemsChor = new ChorGetCartItems_Client(
                    cartSvc.chanA(getItemsSession), cartToFrontendServer.chanB(getItemsSession));

            Cart cart = getItemsChor.getItems("user1");
            return "Got back cart: " + cart.userID + ", " + cart.items;

        } catch (Exception e) {
            e.printStackTrace();
            return "Server error";
        }
    }
}
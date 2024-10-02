package dev.chords.microservices.frontend;

import java.net.URISyntaxException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import choral.reactive.Session;
import choral.reactive.TCPReactiveClient;
import choral.reactive.TCPReactiveServer;
import dev.chords.choreographies.Cart;
import dev.chords.choreographies.ChorGetCartItems_Client;
import dev.chords.choreographies.ChorPlaceOrder_Client;
import dev.chords.choreographies.ReqPlaceOrder;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopChoreography;

@RestController
public class FrontendController {

    TCPReactiveServer<WebshopChoreography> cartToFrontendServer;

    public FrontendController() {
        cartToFrontendServer = new TCPReactiveServer<>();

        cartToFrontendServer.onNewSession((session) -> {
            // No choreographies are instantiated by a new session...
            System.out.println("Received new session: " + session);
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

    @GetMapping("/ping")
    String ping() {
        return "pong";
    }

    @GetMapping("/cart/{userID}")
    String cart(@PathVariable String userID) {

        try (TCPReactiveClient<WebshopChoreography> cartClient = new TCPReactiveClient<>(
                ServiceResources.shared.frontendToCart)) {

            // Get items
            Session<WebshopChoreography> session = Session.makeSession(WebshopChoreography.GET_CART_ITEMS);
            System.out.println("Initiating getItem choreography with session: " + session);

            ChorGetCartItems_Client getItemsChor = new ChorGetCartItems_Client(
                    cartClient.chanA(session), cartToFrontendServer.chanB(session));

            Cart cart = getItemsChor.getItems("user1");
            return "Got back cart: " + cart.userID + ", " + cart.items;

        } catch (Exception e) {
            e.printStackTrace();
            return "Server error";
        }
    }

    @PostMapping("/checkout")
    String checkout(@RequestBody ReqPlaceOrder request) {
        System.out.println("Placing order: " + request);

        try (TCPReactiveClient<WebshopChoreography> cartClient = new TCPReactiveClient<>(
                ServiceResources.shared.frontendToCart)) {

            // Get items
            Session<WebshopChoreography> session = Session.makeSession(WebshopChoreography.PLACE_ORDER);
            System.out.println("Initiating placeOrder choreography with session: " + session);

            ChorPlaceOrder_Client placeOrderChor = new ChorPlaceOrder_Client(cartClient.chanA(session));

            placeOrderChor.placeOrder(request);

            return "Placed order";

        } catch (Exception e) {
            e.printStackTrace();
            return "Server error";
        }
    }
}

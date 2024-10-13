package dev.chords.microservices.frontend;

import choral.reactive.ReactiveReceiver.NewSessionEvent;
import choral.reactive.tracing.JaegerConfiguration;
import choral.reactive.Session;
import choral.reactive.SessionPool;
import choral.reactive.TCPReactiveClient;
import choral.reactive.TCPReactiveServer;
import dev.chords.choreographies.Cart;
import dev.chords.choreographies.ChorGetCartItems_Client;
import dev.chords.choreographies.ChorPlaceOrder_Client;
import dev.chords.choreographies.OrderResult;
import dev.chords.choreographies.ReqPlaceOrder;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopChoreography;
import java.net.URISyntaxException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontendController {

    TCPReactiveServer<WebshopChoreography> cartToFrontendServer;
    TCPReactiveServer<WebshopChoreography> currencyToFrontendServer;
    TCPReactiveServer<WebshopChoreography> shippingToFrontendServer;
    SessionPool<WebshopChoreography> sessionPool = new SessionPool<>();

    public FrontendController() {
        cartToFrontendServer = initializeServer("CART_TO_FRONTEND", ServiceResources.shared.cartToFrontend);
        currencyToFrontendServer = initializeServer("CURRENCY_TO_FRONTEND", ServiceResources.shared.currencyToFrontend);
        shippingToFrontendServer = initializeServer("SHIPPING_TO_FRONTEND", ServiceResources.shared.shippingToFrontend);
    }

    private TCPReactiveServer<WebshopChoreography> initializeServer(String name, String address) {
        return initializeServer(name, address, session -> {
            // No choreographies are instantiated by a new session...
            System.out.println("[FRONTEND] Received new session from " + name + " service: " + session);
        });
    }

    private TCPReactiveServer<WebshopChoreography> initializeServer(
            String name,
            String address,
            NewSessionEvent<WebshopChoreography> onNewSession) {
        TCPReactiveServer<WebshopChoreography> server = new TCPReactiveServer<>(sessionPool);

        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        if (JAEGER_ENDPOINT != null) {
            System.out.println("Configuring choreographic tracing to: " + JAEGER_ENDPOINT);
            server.configureTracing(JaegerConfiguration.initTracer(JAEGER_ENDPOINT));
        }

        server.onNewSession(onNewSession);

        Thread serverThread = new Thread(
                () -> {
                    try {
                        server.listen(address);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                },
                "SERVER_" + name);
        serverThread.start();

        return server;
    }

    @GetMapping("/ping")
    String ping() {
        return "pong";
    }

    @GetMapping("/cart/{userID}")
    String cart(@PathVariable String userID) {
        try (
                TCPReactiveClient<WebshopChoreography> cartClient = new TCPReactiveClient<>(
                        ServiceResources.shared.frontendToCart)) {
            // Get items
            Session<WebshopChoreography> session = Session.makeSession(WebshopChoreography.GET_CART_ITEMS);
            System.out.println("Initiating getItem choreography with session: " + session);

            ChorGetCartItems_Client getItemsChor = new ChorGetCartItems_Client(
                    cartClient.chanA(session),
                    cartToFrontendServer.chanB(session));

            Cart cart = getItemsChor.getItems("user1");
            return "Got back cart: " + cart.userID + ", " + cart.items;
        } catch (Exception e) {
            e.printStackTrace();
            return "Server error";
        }
    }

    @PostMapping("/checkout")
    PlaceOrderResponse checkout(@RequestBody ReqPlaceOrder request) {
        System.out.println("[FRONTEND] Placing order: " + request);

        try (
                TCPReactiveClient<WebshopChoreography> cartClient = new TCPReactiveClient<>(
                        ServiceResources.shared.frontendToCart);
                TCPReactiveClient<WebshopChoreography> currencyClient = new TCPReactiveClient<>(
                        ServiceResources.shared.frontendToCurrency);
                TCPReactiveClient<WebshopChoreography> shippingClient = new TCPReactiveClient<>(
                        ServiceResources.shared.frontendToShipping);
                TCPReactiveClient<WebshopChoreography> paymentClient = new TCPReactiveClient<>(
                        ServiceResources.shared.frontendToPayment);) {
            // Get items
            Session<WebshopChoreography> session = Session.makeSession(WebshopChoreography.PLACE_ORDER);
            sessionPool.registerSession(session);

            System.out.println("[FRONTEND] Initiating placeOrder choreography with session: " + session);

            ChorPlaceOrder_Client placeOrderChor = new ChorPlaceOrder_Client(
                    new ClientService(),
                    cartClient.chanA(session),
                    currencyClient.chanA(session),
                    shippingClient.chanA(session),
                    paymentClient.chanA(session),
                    currencyToFrontendServer.chanB(session),
                    shippingToFrontendServer.chanB(session));

            OrderResult result = placeOrderChor.placeOrder(request);

            return new PlaceOrderResponse(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

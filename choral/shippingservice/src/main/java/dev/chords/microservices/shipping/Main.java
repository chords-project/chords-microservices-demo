package dev.chords.microservices.shipping;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import choral.reactive.Session;
import choral.reactive.TCPReactiveClient;
import choral.reactive.TCPReactiveServer;
import dev.chords.choreographies.ChorPlaceOrder_Shipping;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopChoreography;

public class Main {

    public static ShippingService shippingService;

    public static TCPReactiveServer<WebshopChoreography> frontendServer = null;
    public static TCPReactiveServer<WebshopChoreography> cartServer = null;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral shipping service");

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "50051"));
        shippingService = new ShippingService(new InetSocketAddress("localhost", rpcPort));

        frontendServer = initializeServer("FRONTEND_TO_SHIPPING", ServiceResources.shared.frontendToShipping);
        cartServer = initializeServer("CART_TO_SHIPPING", ServiceResources.shared.cartToShipping);
    }

    public static TCPReactiveServer<WebshopChoreography> initializeServer(String name, String address) {
        TCPReactiveServer<WebshopChoreography> server = new TCPReactiveServer<>();
        server.onNewSession(Main::handleNewSession);

        Thread serverThread = new Thread(() -> {
            try {
                server.listen(address);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }, "SERVER_" + name);
        serverThread.start();

        return server;
    }

    private static void handleNewSession(Session<WebshopChoreography> session) {
        switch (session.choreographyID) {
            case PLACE_ORDER:
                System.out.println("[SHIPPING] New PLACE_ORDER request " + session);

                try (TCPReactiveClient<WebshopChoreography> frontendClient = new TCPReactiveClient<>(
                        ServiceResources.shared.shippingToFrontend);) {

                    ChorPlaceOrder_Shipping placeOrderChor = new ChorPlaceOrder_Shipping(
                            shippingService, frontendServer.chanB(session), cartServer.chanB(session),
                            frontendClient.chanA(session));

                    placeOrderChor.placeOrder();

                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace();
                }

                break;
            default:
                System.out.println("[SHIPPING] Invalid choreography ID " + session.choreographyID);
                break;
        }
    }
}
package dev.chords.microservices.payment;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import choral.reactive.Session;
import choral.reactive.TCPReactiveServer;
import dev.chords.choreographies.ChorPlaceOrder_Payment;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopChoreography;

public class Main {

    public static PaymentService paymentService;

    public static TCPReactiveServer<WebshopChoreography> frontendServer = null;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral payment service");

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "50051"));
        paymentService = new PaymentService(new InetSocketAddress("localhost", rpcPort));

        frontendServer = initializeServer("FRONTEND_TO_PAYMENT", ServiceResources.shared.frontendToPayment);
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
                System.out.println("[PAYMENT] New PLACE_ORDER request " + session);

                ChorPlaceOrder_Payment placeOrderChor = new ChorPlaceOrder_Payment(
                        paymentService,
                        frontendServer.chanB(session));

                placeOrderChor.placeOrder();

                break;
            default:
                System.out.println("[PAYMENT] Invalid choreography ID " + session.choreographyID);
                break;
        }
    }
}
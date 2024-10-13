package dev.chords.microservices.payment;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import choral.reactive.Session;
import choral.reactive.SessionPool;
import choral.reactive.TCPReactiveServer;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.ChorPlaceOrder_Payment;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopChoreography;
import io.opentelemetry.api.trace.Tracer;

public class Main {

    public static PaymentService paymentService;

    public static TCPReactiveServer<WebshopChoreography> frontendServer = null;
    public static SessionPool<WebshopChoreography> sessionPool = new SessionPool<>();
    public static Tracer tracer = null;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral payment service");

        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        if (JAEGER_ENDPOINT != null) {
            System.out.println("Configuring choreographic tracing to: " + JAEGER_ENDPOINT);
            tracer = JaegerConfiguration.initTracer(JAEGER_ENDPOINT);
        }

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "50051"));
        paymentService = new PaymentService(new InetSocketAddress("localhost", rpcPort), tracer);

        frontendServer = initializeServer("FRONTEND_TO_PAYMENT", ServiceResources.shared.frontendToPayment);
    }

    public static TCPReactiveServer<WebshopChoreography> initializeServer(String name, String address) {
        TCPReactiveServer<WebshopChoreography> server = new TCPReactiveServer<>(sessionPool);
        server.onNewSession(Main::handleNewSession);

        if (tracer != null) {
            server.configureTracing(tracer);
        }

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
                System.out.println("[PAYMENT] PLACE_ORDER choreography completed " + session);

                break;
            default:
                System.out.println("[PAYMENT] Invalid choreography ID " + session.choreographyID);
                break;
        }
    }
}
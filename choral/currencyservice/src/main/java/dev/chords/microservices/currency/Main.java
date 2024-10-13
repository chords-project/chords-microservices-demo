package dev.chords.microservices.currency;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import choral.reactive.Session;
import choral.reactive.SessionPool;
import choral.reactive.TCPReactiveClient;
import choral.reactive.TCPReactiveServer;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.ChorPlaceOrder_Currency;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopChoreography;
import io.opentelemetry.api.trace.Tracer;

public class Main {

    public static CurrencyService currencyService;

    public static TCPReactiveServer<WebshopChoreography> frontendServer = null;
    public static TCPReactiveServer<WebshopChoreography> productServer = null;
    public static SessionPool<WebshopChoreography> sessionPool = new SessionPool<>();
    public static Tracer tracer = null;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral currency service");

        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        if (JAEGER_ENDPOINT != null) {
            System.out.println("Configuring choreographic tracing to: " + JAEGER_ENDPOINT);
            tracer = JaegerConfiguration.initTracer(JAEGER_ENDPOINT);
        }

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "7000"));
        currencyService = new CurrencyService(new InetSocketAddress("localhost", rpcPort), tracer);

        frontendServer = initializeServer("FRONTEND_TO_CURRENCY", ServiceResources.shared.frontendToCurrency);
        productServer = initializeServer("PRODUCTCATALOG_TO_CURRENCY",
                ServiceResources.shared.productcatalogToCurrency);
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
                System.out.println("[CURRENCY] New PLACE_ORDER request " + session);

                try (TCPReactiveClient<WebshopChoreography> frontendClient = new TCPReactiveClient<>(
                        ServiceResources.shared.currencyToFrontend);) {

                    ChorPlaceOrder_Currency placeOrderChor = new ChorPlaceOrder_Currency(
                            currencyService,
                            frontendServer.chanB(session),
                            productServer.chanB(session),
                            frontendClient.chanA(session));

                    placeOrderChor.placeOrder();
                    System.out.println("[CURRENCY] PLACE_ORDER choreography completed " + session);

                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace();
                }

                break;
            default:
                System.out.println("Invalid choreography ID " + session.choreographyID);
                break;
        }
    }
}
package dev.chords.microservices.productcatalog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import choral.reactive.SessionPool;
import choral.reactive.TCPReactiveClient;
import choral.reactive.TCPReactiveServer;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.ChorPlaceOrder_ProductCatalog;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopChoreography;
import io.opentelemetry.api.trace.Tracer;

public class Main {

    public static Tracer tracer = null;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral product catalog service");

        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        if (JAEGER_ENDPOINT != null) {
            System.out.println("Configuring choreographic tracing to: " + JAEGER_ENDPOINT);
            tracer = JaegerConfiguration.initTracer(JAEGER_ENDPOINT);
        }

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "3550"));
        ProductCatalogService catalogService = new ProductCatalogService(new InetSocketAddress("localhost", rpcPort),
                tracer);

        SessionPool<WebshopChoreography> sessionPool = new SessionPool<>();

        TCPReactiveServer<WebshopChoreography> cartServer = new TCPReactiveServer<>(sessionPool);

        if (tracer != null) {
            cartServer.configureTracing(tracer);
        }

        cartServer.onNewSession((session) -> {
            switch (session.choreographyID) {
                case PLACE_ORDER:
                    System.out.println("[PRODUCT_CATALOG] New PLACE_ORDER request" + session.choreographyID);

                    try (TCPReactiveClient<WebshopChoreography> currencyClient = new TCPReactiveClient<>(
                            ServiceResources.shared.productcatalogToCurrency);) {

                        ChorPlaceOrder_ProductCatalog placeOrderChor = new ChorPlaceOrder_ProductCatalog(
                                catalogService,
                                cartServer.chanB(session), currencyClient.chanA(session));

                        placeOrderChor.placeOrder();
                        System.out.println("[PRODUCT_CATALOG] PLACE_ORDER choreography completed " + session);

                    } catch (IOException | URISyntaxException e) {
                        e.printStackTrace();
                    }

                    break;
                default:
                    System.out.println("[PRODUCT_CATALOG] Invalid choreography ID " + session.choreographyID);
                    break;
            }
        });

        cartServer.listen(ServiceResources.shared.cartToProductcatalog);
    }
}
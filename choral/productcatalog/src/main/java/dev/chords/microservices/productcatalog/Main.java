package dev.chords.microservices.productcatalog;

import java.net.InetSocketAddress;

import choral.reactive.TCPChoreographyManager;
import choral.reactive.TCPReactiveServer;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.ChorPlaceOrder_ProductCatalog;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopChoreography;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class Main {

    public static TCPReactiveServer<WebshopChoreography> cartServer = null;

    public static TCPChoreographyManager<WebshopChoreography> manager;
    public static OpenTelemetrySdk telemetry = null;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral product catalog service");

        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        if (JAEGER_ENDPOINT != null) {
            System.out.println("Configuring choreographic telemetry to: " + JAEGER_ENDPOINT);
            telemetry = JaegerConfiguration.initTelemetry(JAEGER_ENDPOINT, "ProductCatalogService");
        }

        manager = new TCPChoreographyManager<>(telemetry);

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "3550"));
        ProductCatalogService catalogService = new ProductCatalogService(new InetSocketAddress("localhost", rpcPort),
                telemetry.getTracer(JaegerConfiguration.TRACER_NAME));

        cartServer = manager
                .configureServer(ServiceResources.shared.cartToProductcatalog, (ctx) -> {
                    switch (ctx.session.choreographyID) {
                        case PLACE_ORDER:
                            System.out
                                    .println("[PRODUCT_CATALOG] New PLACE_ORDER request" + ctx.session.choreographyID);

                            ChorPlaceOrder_ProductCatalog placeOrderChor = new ChorPlaceOrder_ProductCatalog(
                                    catalogService,
                                    cartServer.chanB(ctx.session),
                                    ctx.chanA(ServiceResources.shared.productcatalogToCurrency));

                            placeOrderChor.placeOrder();
                            System.out.println("[PRODUCT_CATALOG] PLACE_ORDER choreography completed " + ctx.session);

                            break;
                        default:
                            System.out
                                    .println("[PRODUCT_CATALOG] Invalid choreography ID " + ctx.session.choreographyID);
                            break;
                    }
                });

        manager.listen();
    }
}
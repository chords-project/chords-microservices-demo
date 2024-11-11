package dev.chords.microservices.productcatalog;

import java.net.InetSocketAddress;

import choral.reactive.TCPReactiveServer;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.ChorPlaceOrder_ProductCatalog;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopSession;
import dev.chords.choreographies.WebshopSession.Service;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class Main {

    public static OpenTelemetrySdk telemetry;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral product catalog service");

        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        telemetry = OpenTelemetrySdk.builder().build();
        if (JAEGER_ENDPOINT != null) {
            System.out.println("Configuring choreographic telemetry to: " + JAEGER_ENDPOINT);
            telemetry = JaegerConfiguration.initTelemetry(JAEGER_ENDPOINT, "ProductCatalogService");
        }

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "3550"));
        ProductCatalogService catalogService = new ProductCatalogService(new InetSocketAddress("localhost", rpcPort),
                telemetry);

        TCPReactiveServer<WebshopSession> server = new TCPReactiveServer<>(
                Service.PRODUCT_CATALOG.name(),
                telemetry,
                (ctx) -> {
                    switch (ctx.session.choreography) {
                        case PLACE_ORDER:
                            System.out
                                    .println("[PRODUCT_CATALOG] New PLACE_ORDER request" + ctx.session.choreography);

                            ChorPlaceOrder_ProductCatalog placeOrderChor = new ChorPlaceOrder_ProductCatalog(
                                    catalogService,
                                    ctx.chanB(WebshopSession.Service.CART.name()),
                                    ctx.chanA(ServiceResources.shared.currency));

                            placeOrderChor.placeOrder();
                            System.out.println("[PRODUCT_CATALOG] PLACE_ORDER choreography completed " + ctx.session);

                            break;
                        default:
                            System.out
                                    .println("[PRODUCT_CATALOG] Invalid choreography ID " + ctx.session.choreography);
                            break;
                    }
                });

        server.listen(ServiceResources.shared.productCatalog);
    }
}
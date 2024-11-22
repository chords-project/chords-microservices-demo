package dev.chords.microservices.productcatalog;

import choral.reactive.connection.ClientConnectionManager;
import choral.reactive.ReactiveServer;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.ChorPlaceOrder_ProductCatalog;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopSession;
import dev.chords.choreographies.WebshopSession.Service;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.net.InetSocketAddress;

public class Main {

    public static OpenTelemetrySdk telemetry;

    public static ClientConnectionManager currencyConn;

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

        currencyConn = ClientConnectionManager.makeConnectionManager(ServiceResources.shared.currency, telemetry);

        ReactiveServer<WebshopSession> server = new ReactiveServer<>(Service.PRODUCT_CATALOG.name(), telemetry,
                ctx -> {
                    switch (ctx.session.choreography) {
                        case PLACE_ORDER:
                            ctx.log("[PRODUCT_CATALOG] New PLACE_ORDER request");

                            ChorPlaceOrder_ProductCatalog placeOrderChor = new ChorPlaceOrder_ProductCatalog(
                                    catalogService,
                                    ctx.chanB(WebshopSession.Service.CART.name()),
                                    ctx.chanA(currencyConn));

                            placeOrderChor.placeOrder();
                            ctx.log("[PRODUCT_CATALOG] PLACE_ORDER choreography completed");

                            break;
                        default:
                            ctx.log("[PRODUCT_CATALOG] Invalid choreography ID " + ctx.session.choreography.name());
                            break;
                    }
                });

        server.listen(ServiceResources.shared.productCatalog);
    }
}

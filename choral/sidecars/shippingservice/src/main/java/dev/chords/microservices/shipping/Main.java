package dev.chords.microservices.shipping;

import choral.reactive.ClientConnectionManager;
import choral.reactive.ReactiveServer;
import choral.reactive.ReactiveServer.SessionContext;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.ChorPlaceOrder_Shipping;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopSession;
import dev.chords.choreographies.WebshopSession.Service;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.net.InetSocketAddress;

public class Main {

    public static ShippingService shippingService;

    public static OpenTelemetrySdk telemetry;

    public static ClientConnectionManager frontendConn;
    public static ClientConnectionManager currencyConn;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral shipping service");

        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        telemetry = OpenTelemetrySdk.builder().build();
        if (JAEGER_ENDPOINT != null) {
            System.out.println("Configuring choreographic telemetry to: " + JAEGER_ENDPOINT);
            telemetry = JaegerConfiguration.initTelemetry(JAEGER_ENDPOINT, "ShippingService");
        }

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "50051"));
        shippingService = new ShippingService(new InetSocketAddress("localhost", rpcPort), telemetry);

        frontendConn = ClientConnectionManager.makeConnectionManager(ServiceResources.shared.frontend, telemetry);

        currencyConn = ClientConnectionManager.makeConnectionManager(ServiceResources.shared.currency, telemetry);

        ReactiveServer<WebshopSession> server = new ReactiveServer<>(Service.SHIPPING.name(), telemetry,
                Main::handleNewSession);

        server.listen(ServiceResources.shared.shipping);
    }

    private static void handleNewSession(SessionContext<WebshopSession> ctx) throws Exception {
        switch (ctx.session.choreography) {
            case PLACE_ORDER:
                ctx.log("[SHIPPING] New PLACE_ORDER request");

                ChorPlaceOrder_Shipping placeOrderChor = new ChorPlaceOrder_Shipping(
                        shippingService,
                        ctx.symChan(WebshopSession.Service.FRONTEND.name(), frontendConn),
                        ctx.chanB(WebshopSession.Service.CART.name()),
                        ctx.chanA(currencyConn));

                placeOrderChor.placeOrder();
                ctx.log("[SHIPPING] PLACE_ORDER choreography completed");

                break;
            default:
                ctx.log("[SHIPPING] Invalid choreography " + ctx.session.choreographyName());
                break;
        }
    }
}

package dev.chords.microservices.shipping;

import java.net.InetSocketAddress;

import choral.reactive.TCPReactiveServer;
import choral.reactive.TCPReactiveServer.SessionContext;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.ChorPlaceOrder_Shipping;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopSession;
import dev.chords.choreographies.WebshopSession.Service;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class Main {

    public static ShippingService shippingService;

    public static OpenTelemetrySdk telemetry;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral shipping service");

        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        telemetry = OpenTelemetrySdk.builder().build();
        if (JAEGER_ENDPOINT != null) {
            System.out.println("Configuring choreographic telemetry to: " + JAEGER_ENDPOINT);
            telemetry = JaegerConfiguration.initTelemetry(JAEGER_ENDPOINT, "ShippingService");
        }

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "50051"));
        shippingService = new ShippingService(new InetSocketAddress("localhost", rpcPort),
                telemetry);

        TCPReactiveServer<WebshopSession> server = new TCPReactiveServer<>(
                Service.SHIPPING.name(),
                telemetry,
                Main::handleNewSession);

        server.listen(ServiceResources.shared.shipping);
    }

    private static void handleNewSession(SessionContext<WebshopSession> ctx) throws Exception {
        switch (ctx.session.choreography) {
            case PLACE_ORDER:
                System.out.println("[SHIPPING] New PLACE_ORDER request " + ctx.session);

                ChorPlaceOrder_Shipping placeOrderChor = new ChorPlaceOrder_Shipping(
                        shippingService,
                        ctx.symChan(WebshopSession.Service.FRONTEND.name(), ServiceResources.shared.frontend),
                        ctx.chanB(WebshopSession.Service.CART.name()));

                placeOrderChor.placeOrder();
                System.out.println("[SHIPPING] PLACE_ORDER choreography completed " + ctx.session);

                break;
            default:
                System.out.println("[SHIPPING] Invalid choreography " + ctx.session.choreographyName());
                break;
        }
    }
}
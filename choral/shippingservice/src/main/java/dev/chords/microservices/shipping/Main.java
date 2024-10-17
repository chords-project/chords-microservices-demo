package dev.chords.microservices.shipping;

import java.net.InetSocketAddress;

import choral.reactive.TCPChoreographyManager;
import choral.reactive.TCPChoreographyManager.SessionContext;
import choral.reactive.TCPReactiveServer;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.ChorPlaceOrder_Shipping;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopChoreography;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class Main {

    public static ShippingService shippingService;

    public static TCPReactiveServer<WebshopChoreography> frontendServer = null;
    public static TCPReactiveServer<WebshopChoreography> cartServer = null;

    public static TCPChoreographyManager<WebshopChoreography> manager;
    public static OpenTelemetrySdk telemetry = null;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral shipping service");

        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        if (JAEGER_ENDPOINT != null) {
            System.out.println("Configuring choreographic telemetry to: " + JAEGER_ENDPOINT);
            telemetry = JaegerConfiguration.initTelemetry(JAEGER_ENDPOINT, "ShippingService");
        }

        manager = new TCPChoreographyManager<>(telemetry);

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "50051"));
        shippingService = new ShippingService(new InetSocketAddress("localhost", rpcPort),
                telemetry.getTracer(JaegerConfiguration.TRACER_NAME));

        frontendServer = manager.configureServer(ServiceResources.shared.frontendToShipping, Main::handleNewSession);
        cartServer = manager.configureServer(ServiceResources.shared.cartToShipping, Main::handleNewSession);

        manager.listen();
    }

    private static void handleNewSession(SessionContext<WebshopChoreography> ctx) throws Exception {
        switch (ctx.session.choreographyID) {
            case PLACE_ORDER:
                System.out.println("[SHIPPING] New PLACE_ORDER request " + ctx.session);

                ChorPlaceOrder_Shipping placeOrderChor = new ChorPlaceOrder_Shipping(
                        shippingService, frontendServer.chanB(ctx.session), cartServer.chanB(ctx.session),
                        ctx.chanA(ServiceResources.shared.shippingToFrontend));

                placeOrderChor.placeOrder();
                System.out.println("[SHIPPING] PLACE_ORDER choreography completed " + ctx.session);

                break;
            default:
                System.out.println("[SHIPPING] Invalid choreography ID " + ctx.session.choreographyID);
                break;
        }
    }
}
package dev.chords.microservices.payment;

import java.net.InetSocketAddress;

import choral.reactive.TCPChoreographyManager;
import choral.reactive.TCPChoreographyManager.SessionContext;
import choral.reactive.TCPReactiveServer;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.ChorPlaceOrder_Payment;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopChoreography;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class Main {

    public static PaymentService paymentService;

    public static TCPReactiveServer<WebshopChoreography> frontendServer = null;

    public static TCPChoreographyManager<WebshopChoreography> manager;
    public static OpenTelemetrySdk telemetry = null;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral payment service");

        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        if (JAEGER_ENDPOINT != null) {
            System.out.println("Configuring choreographic telemetry to: " + JAEGER_ENDPOINT);
            telemetry = JaegerConfiguration.initTelemetry(JAEGER_ENDPOINT, "PaymentService");
        }

        manager = new TCPChoreographyManager<>(telemetry);

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "50051"));
        paymentService = new PaymentService(new InetSocketAddress("localhost", rpcPort),
                telemetry);

        frontendServer = manager.configureServer(ServiceResources.shared.frontendToPayment, Main::handleNewSession);

        manager.listen();
    }

    private static void handleNewSession(SessionContext<WebshopChoreography> ctx) throws Exception {
        switch (ctx.session.choreographyID) {
            case PLACE_ORDER:
                System.out.println("[PAYMENT] New PLACE_ORDER request " + ctx.session);

                ChorPlaceOrder_Payment placeOrderChor = new ChorPlaceOrder_Payment(
                        paymentService,
                        frontendServer.chanB(ctx.session));

                placeOrderChor.placeOrder();
                System.out.println("[PAYMENT] PLACE_ORDER choreography completed " + ctx.session);

                break;
            default:
                System.out.println("[PAYMENT] Invalid choreography ID " + ctx.session.choreographyID);
                break;
        }
    }
}
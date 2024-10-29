package dev.chords.microservices.payment;

import java.net.InetSocketAddress;

import choral.reactive.TCPReactiveServer;
import choral.reactive.TCPReactiveServer.SessionContext;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.ChorPlaceOrder_Payment;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopSession;
import dev.chords.choreographies.WebshopSession.Service;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class Main {

    public static PaymentService paymentService;

    public static TCPReactiveServer<WebshopSession> frontendServer = null;

    public static OpenTelemetrySdk telemetry;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral payment service");

        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        telemetry = OpenTelemetrySdk.builder().build();
        if (JAEGER_ENDPOINT != null) {
            System.out.println("Configuring choreographic telemetry to: " + JAEGER_ENDPOINT);
            telemetry = JaegerConfiguration.initTelemetry(JAEGER_ENDPOINT, "PaymentService");
        }

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "50051"));
        paymentService = new PaymentService(new InetSocketAddress("localhost", rpcPort),
                telemetry);

        TCPReactiveServer<WebshopSession> server = new TCPReactiveServer<>(
                Service.PAYMENT.name(),
                telemetry,
                Main::handleNewSession);

        server.listen(ServiceResources.shared.payment);
    }

    private static void handleNewSession(SessionContext<WebshopSession> ctx) throws Exception {
        switch (ctx.session.choreography) {
            case PLACE_ORDER:
                System.out.println("[PAYMENT] New PLACE_ORDER request " + ctx.session);

                ChorPlaceOrder_Payment placeOrderChor = new ChorPlaceOrder_Payment(
                        paymentService,
                        frontendServer.chanB(ctx.session));

                placeOrderChor.placeOrder();
                System.out.println("[PAYMENT] PLACE_ORDER choreography completed " + ctx.session);

                break;
            default:
                System.out.println("[PAYMENT] Invalid choreography " + ctx.session.choreographyName());
                break;
        }
    }
}
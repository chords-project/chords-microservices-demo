package dev.chords.microservices.cartservice;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import choral.reactive.TCPReactiveServer;
import choral.reactive.TCPReactiveServer.SessionContext;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.ChorPlaceOrder_Cart;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopSession;
import dev.chords.choreographies.WebshopSession.Service;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class Main {

    private static CartService cartService;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral cart service");

        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        OpenTelemetrySdk telemetry = OpenTelemetrySdk.builder().build();
        if (JAEGER_ENDPOINT != null) {
            System.out.println("Configuring choreographic telemetry to: " + JAEGER_ENDPOINT);
            telemetry = JaegerConfiguration.initTelemetry(JAEGER_ENDPOINT, "CartService");
        }

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("ASPNETCORE_HTTP_PORTS", "7070"));
        cartService = new CartService(new InetSocketAddress("localhost", rpcPort),
                telemetry);

        TCPReactiveServer<WebshopSession> server = new TCPReactiveServer<>(
                Service.CART.name(), telemetry, Main::handleNewSession);

        server.listen(ServiceResources.shared.cart);
    }

    private static void handleNewSession(SessionContext<WebshopSession> ctx)
            throws IOException, URISyntaxException {
        switch (ctx.session.choreography) {
            case PLACE_ORDER:
                ctx.log("[CART] New PLACE_ORDER request");

                ChorPlaceOrder_Cart placeOrderChor = new ChorPlaceOrder_Cart(
                        cartService,
                        ctx.chanB(WebshopSession.Service.FRONTEND.name()),
                        ctx.chanA(ServiceResources.shared.productCatalog),
                        ctx.chanA(ServiceResources.shared.shipping));

                placeOrderChor.placeOrder();

                ctx.log("[CART] PLACE_ORDER choreography completed");

                break;
            default:
                ctx.log("[CART] Invalid choreography " + ctx.session.choreographyName());
                break;
        }
    }
}

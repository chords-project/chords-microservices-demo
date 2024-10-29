package dev.chords.microservices.cartservice;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import choral.reactive.TCPReactiveServer;
import choral.reactive.TCPReactiveServer.SessionContext;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.ChorAddCartItem_Cart;
import dev.chords.choreographies.ChorGetCartItems_Cart;
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
            case ADD_CART_ITEM:
                System.out.println("[CART] New ADD_CART_ITEM request " + ctx.session);
                ChorAddCartItem_Cart addItemChor = new ChorAddCartItem_Cart(
                        ctx.chanB(WebshopSession.Service.FRONTEND.name()),
                        cartService);
                addItemChor.addItem();
                break;
            case GET_CART_ITEMS:
                System.out.println("[CART] New GET_CART_ITEMS request " + ctx.session);

                ChorGetCartItems_Cart getItemsChor = new ChorGetCartItems_Cart(
                        ctx.chanB(WebshopSession.Service.FRONTEND.name()),
                        ctx.chanA(ServiceResources.shared.frontend),
                        cartService);
                getItemsChor.getItems();

                break;
            case PLACE_ORDER:
                System.out.println("[CART] New PLACE_ORDER request " + ctx.session);

                ChorPlaceOrder_Cart placeOrderChor = new ChorPlaceOrder_Cart(
                        cartService,
                        ctx.chanB(WebshopSession.Service.FRONTEND.name()),
                        ctx.chanA(ServiceResources.shared.productCatalog),
                        ctx.chanA(ServiceResources.shared.shipping));

                placeOrderChor.placeOrder();
                System.out.println("[CART] PLACE_ORDER choreography completed " + ctx.session);

                break;
            default:
                System.out.println("Invalid choreography " + ctx.session.choreographyName());
                break;
        }
    }
}

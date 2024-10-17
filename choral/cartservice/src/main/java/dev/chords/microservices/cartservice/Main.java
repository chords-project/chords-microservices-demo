package dev.chords.microservices.cartservice;

import java.net.InetSocketAddress;

import choral.reactive.TCPChoreographyManager;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.ChorAddCartItem_Cart;
import dev.chords.choreographies.ChorGetCartItems_Cart;
import dev.chords.choreographies.ChorPlaceOrder_Cart;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopChoreography;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral cart service");

        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        OpenTelemetrySdk telemetry = null;
        if (JAEGER_ENDPOINT != null) {
            System.out.println("Configuring choreographic telemetry to: " + JAEGER_ENDPOINT);
            telemetry = JaegerConfiguration.initTelemetry(JAEGER_ENDPOINT, "CartService");
        }

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("ASPNETCORE_HTTP_PORTS", "7070"));
        CartService cartService = new CartService(new InetSocketAddress("localhost", rpcPort),
                telemetry.getTracer(JaegerConfiguration.TRACER_NAME));

        // SessionPool<WebshopChoreography> sessionPool = new SessionPool<>();

        TCPChoreographyManager<WebshopChoreography> manager = new TCPChoreographyManager<>(telemetry);

        manager.configureServer(ServiceResources.shared.frontendToCart, (ctx) -> {
            switch (ctx.session.choreographyID) {
                case ADD_CART_ITEM:
                    System.out.println("[CART] New ADD_CART_ITEM request " + ctx.session);
                    ChorAddCartItem_Cart addItemChor = new ChorAddCartItem_Cart(
                            ctx.selfChanB(),
                            cartService);
                    addItemChor.addItem();
                    break;
                case GET_CART_ITEMS:
                    System.out.println("[CART] New GET_CART_ITEMS request " + ctx.session);

                    ChorGetCartItems_Cart getItemsChor = new ChorGetCartItems_Cart(
                            ctx.selfChanB(),
                            ctx.chanA(ServiceResources.shared.cartToFrontend),
                            cartService);
                    getItemsChor.getItems();

                    break;
                case PLACE_ORDER:
                    System.out.println("[CART] New PLACE_ORDER request " + ctx.session);

                    ChorPlaceOrder_Cart placeOrderChor = new ChorPlaceOrder_Cart(
                            cartService,
                            ctx.selfChanB(),
                            ctx.chanA(ServiceResources.shared.cartToProductcatalog),
                            ctx.chanA(ServiceResources.shared.cartToShipping));

                    placeOrderChor.placeOrder();
                    System.out.println("[CART] PLACE_ORDER choreography completed " + ctx.session);

                    break;
                default:
                    System.out.println("Invalid choreography ID " + ctx.session.choreographyID);
                    break;
            }
        });

        manager.listen();
    }
}

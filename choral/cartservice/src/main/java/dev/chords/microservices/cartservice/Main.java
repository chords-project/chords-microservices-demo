package dev.chords.microservices.cartservice;

import choral.reactive.SessionPool;
import choral.reactive.TCPReactiveClient;
import choral.reactive.TCPReactiveServer;
import dev.chords.choreographies.ChorAddCartItem_Cart;
import dev.chords.choreographies.ChorGetCartItems_Cart;
import dev.chords.choreographies.ChorPlaceOrder_Cart;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopChoreography;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;

public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral cart service");

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("ASPNETCORE_HTTP_PORTS", "7070"));
        CartService cartService = new CartService(new InetSocketAddress("localhost", rpcPort));

        SessionPool<WebshopChoreography> sessionPool = new SessionPool<>();

        TCPReactiveServer<WebshopChoreography> frontendServer = new TCPReactiveServer<>(sessionPool);
        frontendServer.onNewSession(session -> {
            switch (session.choreographyID) {
                case ADD_CART_ITEM:
                    System.out.println("[CART] New ADD_CART_ITEM request " + session);
                    ChorAddCartItem_Cart addItemChor = new ChorAddCartItem_Cart(
                        frontendServer.chanB(session),
                        cartService
                    );
                    addItemChor.addItem();
                    break;
                case GET_CART_ITEMS:
                    System.out.println("[CART] New GET_CART_ITEMS request " + session);
                    try (
                        TCPReactiveClient<WebshopChoreography> clientConn = new TCPReactiveClient<WebshopChoreography>(
                            ServiceResources.shared.cartToFrontend
                        );
                    ) {
                        ChorGetCartItems_Cart getItemsChor = new ChorGetCartItems_Cart(
                            frontendServer.chanB(session),
                            clientConn.chanA(session),
                            cartService
                        );
                        getItemsChor.getItems();
                    } catch (URISyntaxException | IOException e) {
                        e.printStackTrace();
                    }

                    break;
                case PLACE_ORDER:
                    System.out.println("[CART] New PLACE_ORDER request " + session);
                    try (
                        TCPReactiveClient<WebshopChoreography> catalogClient = new TCPReactiveClient<
                            WebshopChoreography
                        >(ServiceResources.shared.cartToProductcatalog);
                        TCPReactiveClient<WebshopChoreography> shippingClient = new TCPReactiveClient<
                            WebshopChoreography
                        >(ServiceResources.shared.cartToShipping);
                    ) {
                        ChorPlaceOrder_Cart placeOrderChor = new ChorPlaceOrder_Cart(
                            cartService,
                            frontendServer.chanB(session),
                            catalogClient.chanA(session),
                            shippingClient.chanA(session)
                        );

                        placeOrderChor.placeOrder();
                        System.out.println("[CART] PLACE_ORDER choreography completed " + session);
                    } catch (URISyntaxException | IOException e) {
                        e.printStackTrace();
                    }

                    break;
                default:
                    System.out.println("Invalid choreography ID " + session.choreographyID);
                    break;
            }
        });

        frontendServer.listen(ServiceResources.shared.frontendToCart);
    }
}

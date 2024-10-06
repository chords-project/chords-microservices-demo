package dev.chords.microservices.productcatalog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;

import choral.reactive.SessionPool;
import choral.reactive.TCPReactiveClient;
import choral.reactive.TCPReactiveServer;
import dev.chords.choreographies.ChorPlaceOrder_ProductCatalog;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopChoreography;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral product catalog service");

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "3550"));
        ProductCatalogService catalogService = new ProductCatalogService(new InetSocketAddress("localhost", rpcPort));

        SessionPool<WebshopChoreography> sessionPool = new SessionPool<>();

        TCPReactiveServer<WebshopChoreography> cartServer = new TCPReactiveServer<>(sessionPool);
        cartServer.onNewSession((session) -> {
            switch (session.choreographyID) {
                case PLACE_ORDER:
                    System.out.println("[PRODUCT_CATALOG] New PLACE_ORDER request" + session.choreographyID);

                    try (TCPReactiveClient<WebshopChoreography> currencyClient = new TCPReactiveClient<>(
                            ServiceResources.shared.productcatalogToCurrency);) {

                        ChorPlaceOrder_ProductCatalog placeOrderChor = new ChorPlaceOrder_ProductCatalog(
                                catalogService,
                                cartServer.chanB(session), currencyClient.chanA(session));

                        placeOrderChor.placeOrder();
                        System.out.println("[PRODUCT_CATALOG] PLACE_ORDER choreography completed " + session);

                    } catch (IOException | URISyntaxException e) {
                        e.printStackTrace();
                    }

                    break;
                default:
                    System.out.println("[PRODUCT_CATALOG] Invalid choreography ID " + session.choreographyID);
                    break;
            }
        });

        cartServer.listen(ServiceResources.shared.cartToProductcatalog);
    }
}
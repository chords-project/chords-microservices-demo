package dev.chords.microservices.productcatalog;

import java.net.InetSocketAddress;

import choral.reactive.TCPReactiveServer;
import dev.chords.choreographies.ChorPlaceOrder_ProductCatalog;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopChoreography;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral product catalog service");

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "3550"));
        ProductCatalogService catalogService = new ProductCatalogService(new InetSocketAddress("localhost", rpcPort));

        TCPReactiveServer<WebshopChoreography> cartServer = new TCPReactiveServer<>();
        cartServer.onNewSession((session) -> {
            switch (session.choreographyID) {
                case PLACE_ORDER:
                    System.out.println("New PLACE_ORDER request");

                    ChorPlaceOrder_ProductCatalog placeOrderChor = new ChorPlaceOrder_ProductCatalog(
                            catalogService,
                            cartServer.chanB(session));
                    placeOrderChor.placeOrder();

                    break;
                default:
                    System.out.println("Invalid choreography ID " + session.choreographyID);
                    break;
            }
        });

        cartServer.listen(ServiceResources.shared.cartToProductcatalog);
    }
}
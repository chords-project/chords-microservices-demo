package dev.chord.microservices.cartservice;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import hipstershop.CartServiceGrpc;
import hipstershop.Demo;
import hipstershop.CartServiceGrpc.CartServiceBlockingStub;
import hipstershop.Demo.Cart;
import hipstershop.Demo.EmptyCartRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class CartService implements dev.chord.choreographies.CartService, AutoCloseable {

    protected ManagedChannel channel;
    protected CartServiceBlockingStub connection;

    public CartService(InetSocketAddress address) {
        channel = ManagedChannelBuilder
                .forAddress(address.getHostName(), address.getPort())
                .usePlaintext()
                .build();

        this.connection = CartServiceGrpc.newBlockingStub(channel);
    }

    public void addItem(String userID, String productID, int quantity) {
        System.out.println("Adding item to cart service");

        Demo.AddItemRequest request = Demo.AddItemRequest.newBuilder()
                .setUserId(userID)
                .setItem(
                        Demo.CartItem.newBuilder()
                                .setProductId(productID)
                                .setQuantity(quantity))
                .build();

        connection.addItem(request);
    }

    public void emptyCart(String userID) {
        connection.emptyCart(EmptyCartRequest.newBuilder().build());
    }

    public Cart getCart(String userID) {
        Demo.GetCartRequest request = Demo.GetCartRequest.newBuilder()
                .setUserId(userID)
                .build();

        Cart cart = connection.getCart(request);
        return cart;
    }

    @Override
    public void close() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

}

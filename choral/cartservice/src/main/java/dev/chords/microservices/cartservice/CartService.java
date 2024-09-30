package dev.chords.microservices.cartservice;

import java.util.List;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import dev.chords.choreographies.Cart;
import dev.chords.choreographies.CartItem;
import hipstershop.CartServiceGrpc;
import hipstershop.Demo;
import hipstershop.CartServiceGrpc.CartServiceBlockingStub;
import hipstershop.Demo.EmptyCartRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class CartService implements dev.chords.choreographies.CartService, AutoCloseable {

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

    @Override
    public Cart getCart(String userID) {
        Demo.GetCartRequest request = Demo.GetCartRequest.newBuilder()
                .setUserId(userID)
                .build();

        Demo.Cart cart = connection.getCart(request);
        List<CartItem> items = cart.getItemsList().stream()
                .map(item -> new CartItem(item.getProductId())).toList();

        return new Cart(cart.getUserId(), items);
    }

    public void emptyCart(String userID) {
        connection.emptyCart(EmptyCartRequest.newBuilder().build());
    }

    @Override
    public void close() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

}

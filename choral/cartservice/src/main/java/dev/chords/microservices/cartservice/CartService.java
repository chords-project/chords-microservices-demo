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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

public class CartService implements dev.chords.choreographies.CartService, AutoCloseable {

    protected ManagedChannel channel;
    protected CartServiceBlockingStub connection;
    protected Tracer tracer;

    public CartService(InetSocketAddress address, Tracer tracer) {
        channel = ManagedChannelBuilder
                .forAddress(address.getHostName(), address.getPort())
                .usePlaintext()
                .build();

        this.connection = CartServiceGrpc.newBlockingStub(channel);
        this.tracer = tracer;
    }

    public void addItem(String userID, String productID, int quantity) {
        System.out.println("[CART] Adding item to cart");

        Span span = null;
        if (tracer != null) {
            span = tracer.spanBuilder("CartService: add item").startSpan();
        }

        Demo.AddItemRequest request = Demo.AddItemRequest.newBuilder()
                .setUserId(userID)
                .setItem(
                        Demo.CartItem.newBuilder()
                                .setProductId(productID)
                                .setQuantity(quantity))
                .build();

        connection.addItem(request);

        if (span != null)
            span.end();
    }

    @Override
    public Cart getCart(String userID) {
        System.out.println("[CART] Get cart for user: " + userID);

        Span span = null;
        if (tracer != null) {
            span = tracer.spanBuilder("CartService: get cart").startSpan();
        }

        Demo.GetCartRequest request = Demo.GetCartRequest.newBuilder()
                .setUserId(userID)
                .build();

        Demo.Cart cart = connection.getCart(request);
        List<CartItem> items = cart.getItemsList().stream()
                .map(item -> new CartItem(item.getProductId(), item.getQuantity())).toList();

        if (span != null)
            span.end();

        return new Cart(cart.getUserId(), items);
    }

    public void emptyCart(String userID) {
        System.out.println("[CART] Empty cart for user: " + userID);

        Span span = null;
        if (tracer != null) {
            span = tracer.spanBuilder("CartService: empty cart").startSpan();
        }

        connection.emptyCart(EmptyCartRequest.newBuilder().setUserId(userID).build());

        if (span != null)
            span.end();
    }

    @Override
    public void close() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

}

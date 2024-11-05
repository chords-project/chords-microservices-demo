package dev.chords.microservices.cartservice;

import java.util.List;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import choral.reactive.ChannelConfigurator;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.Cart;
import dev.chords.choreographies.CartItem;
import hipstershop.CartServiceGrpc;
import hipstershop.Demo;
import hipstershop.CartServiceGrpc.CartServiceBlockingStub;
import hipstershop.Demo.EmptyCartRequest;
import io.grpc.ManagedChannel;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class CartService implements dev.chords.choreographies.CartService, AutoCloseable {

    protected ManagedChannel channel;
    protected CartServiceBlockingStub connection;
    protected Tracer tracer;

    public CartService(InetSocketAddress address, OpenTelemetrySdk telemetry) {
        channel = ChannelConfigurator.makeChannel(address, telemetry);

        this.connection = CartServiceGrpc.newBlockingStub(channel);
        this.tracer = telemetry.getTracer(JaegerConfiguration.TRACER_NAME);
    }

    public void addItem(String userID, String productID, int quantity) {
        System.out.println("[CART] Adding item to cart");

        Span span = null;
        Scope scope = null;
        if (tracer != null) {
            span = tracer.spanBuilder("CartService.addItem")
                    .setAttribute("request.userID", userID)
                    .setAttribute("request.productID", productID)
                    .setAttribute("request.quantity", quantity)
                    .startSpan();

            scope = span.makeCurrent();
        }

        Demo.AddItemRequest request = Demo.AddItemRequest.newBuilder()
                .setUserId(userID)
                .setItem(
                        Demo.CartItem.newBuilder()
                                .setProductId(productID)
                                .setQuantity(quantity))
                .build();

        Span requestSpan = tracer.spanBuilder("send request").startSpan();
        connection.addItem(request);
        requestSpan.end();

        if (scope != null)
            scope.close();

        if (span != null)
            span.end();
    }

    @Override
    public Cart getCart(String userID) {
        System.out.println("[CART] Get cart for user: " + userID);

        Span span = null;
        Scope scope = null;
        if (tracer != null) {
            span = tracer.spanBuilder("CartService.getCart")
                    .setAttribute("request.userID", userID).startSpan();
            scope = span.makeCurrent();
        }

        Demo.GetCartRequest request = Demo.GetCartRequest.newBuilder()
                .setUserId(userID)
                .build();

        Span requestSpan = tracer.spanBuilder("send request").startSpan();
        Demo.Cart cart = connection.getCart(request);
        requestSpan.end();

        List<CartItem> items = cart.getItemsList().stream()
                .map(item -> new CartItem(item.getProductId(), item.getQuantity())).toList();

        if (scope != null)
            scope.close();

        if (span != null)
            span.end();

        return new Cart(cart.getUserId(), items);
    }

    public void emptyCart(String userID) {
        System.out.println("[CART] Empty cart for user: " + userID);

        Span span = null;
        Scope scope = null;
        if (tracer != null) {
            span = tracer.spanBuilder("CartService.emptyCart").setAttribute("request.userID", userID).startSpan();
            scope = span.makeCurrent();
        }

        EmptyCartRequest request = EmptyCartRequest.newBuilder().setUserId(userID).build();

        Span requestSpan = tracer.spanBuilder("send request").startSpan();
        connection.emptyCart(request);
        requestSpan.end();

        if (scope != null)
            scope.close();

        if (span != null)
            span.end();
    }

    @Override
    public void close() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

}

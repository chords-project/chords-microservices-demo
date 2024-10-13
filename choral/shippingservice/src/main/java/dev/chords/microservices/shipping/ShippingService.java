package dev.chords.microservices.shipping;

import java.net.InetSocketAddress;

import dev.chords.choreographies.Address;
import dev.chords.choreographies.Cart;
import dev.chords.choreographies.CartItem;
import dev.chords.choreographies.Money;
import hipstershop.ShippingServiceGrpc;
import hipstershop.Demo;
import hipstershop.Demo.GetQuoteRequest;
import hipstershop.Demo.GetQuoteResponse;
import hipstershop.Demo.ShipOrderRequest;
import hipstershop.Demo.ShipOrderResponse;
import hipstershop.ShippingServiceGrpc.ShippingServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

public class ShippingService implements dev.chords.choreographies.ShippingService {

    protected ManagedChannel channel;
    protected ShippingServiceBlockingStub connection;
    protected Tracer tracer;

    public ShippingService(InetSocketAddress address, Tracer tracer) {
        channel = ManagedChannelBuilder
                .forAddress(address.getHostName(), address.getPort())
                .usePlaintext()
                .build();

        connection = ShippingServiceGrpc.newBlockingStub(channel);
        this.tracer = tracer;
    }

    @Override
    public Money getQuote(Address address, Cart cart) {
        System.out.println("[SHIPPING] Get quote");

        Span span = null;
        if (tracer != null) {
            span = tracer.spanBuilder("ShippingService: get quote").startSpan();
        }

        GetQuoteRequest.Builder request = GetQuoteRequest.newBuilder()
                .setAddress(
                        Demo.Address.newBuilder()
                                .setStreetAddress(address.street_address)
                                .setCity(address.city)
                                .setState(address.state)
                                .setCountry(address.country)
                                .setZipCode(address.zip_code));

        for (CartItem item : cart.items) {
            request.addItems(Demo.CartItem.newBuilder().setProductId(item.product_id).setQuantity(item.quantity));
        }

        GetQuoteResponse response = connection.getQuote(request.build());

        Demo.Money m = response.getCostUsd();

        if (span != null)
            span.end();

        return new Money(m.getCurrencyCode(), (int) m.getUnits(), m.getNanos());
    }

    @Override
    public String shipOrder(Address address, Cart cart) {
        System.out.println("[SHIPPING] Ship order");

        Span span = null;
        if (tracer != null) {
            span = tracer.spanBuilder("ShippingService: ship order").startSpan();
        }

        ShipOrderRequest.Builder request = ShipOrderRequest.newBuilder()
                .setAddress(
                        Demo.Address.newBuilder()
                                .setStreetAddress(address.street_address)
                                .setCity(address.city)
                                .setState(address.state)
                                .setCountry(address.country)
                                .setZipCode(address.zip_code));

        for (CartItem item : cart.items) {
            request.addItems(Demo.CartItem.newBuilder().setProductId(item.product_id).setQuantity(item.quantity));
        }

        ShipOrderResponse response = connection.shipOrder(request.build());

        if (span != null)
            span.end();

        return response.getTrackingId();
    }

}

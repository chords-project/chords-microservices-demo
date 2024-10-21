package dev.chords.microservices.shipping;

import java.net.InetSocketAddress;

import choral.reactive.ChannelConfigurator;
import choral.reactive.tracing.JaegerConfiguration;
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
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class ShippingService implements dev.chords.choreographies.ShippingService {

    protected ManagedChannel channel;
    protected ShippingServiceBlockingStub connection;
    protected Tracer tracer;

    public ShippingService(InetSocketAddress address, OpenTelemetrySdk telemetry) {
        channel = ChannelConfigurator.makeChannel(address, telemetry);

        connection = ShippingServiceGrpc.newBlockingStub(channel);
        this.tracer = telemetry.getTracer(JaegerConfiguration.TRACER_NAME);
    }

    @Override
    public Money getQuote(Address address, Cart cart) {
        System.out.println("[SHIPPING] Get quote");

        Span span = null;
        Scope scope = null;
        if (tracer != null) {
            span = tracer.spanBuilder("ShippingService.getQuote").startSpan();
            scope = span.makeCurrent();
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

        // Span requestSpan = tracer.spanBuilder("send request").startSpan();
        GetQuoteResponse response = connection.getQuote(request.build());
        // requestSpan.end();

        Demo.Money m = response.getCostUsd();

        if (scope != null)
            scope.close();

        if (span != null)
            span.end();

        return new Money(m.getCurrencyCode(), (int) m.getUnits(), m.getNanos());
    }

    @Override
    public String shipOrder(Address address, Cart cart) {
        System.out.println("[SHIPPING] Ship order");

        Span span = null;
        if (tracer != null) {
            span = tracer.spanBuilder("ShippingService.shipOrder").startSpan();
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

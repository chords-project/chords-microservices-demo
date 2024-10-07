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

public class ShippingService implements dev.chords.choreographies.ShippingService {

    protected ManagedChannel channel;
    protected ShippingServiceBlockingStub connection;

    public ShippingService(InetSocketAddress address) {
        channel = ManagedChannelBuilder
                .forAddress(address.getHostName(), address.getPort())
                .usePlaintext()
                .build();

        connection = ShippingServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public Money getQuote(Address address, Cart cart) {
        System.out.println("[SHIPPING] Get quote");

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
        return new Money(m.getCurrencyCode(), (int) m.getUnits(), m.getNanos());
    }

    @Override
    public String shipOrder(Address address, Cart cart) {
        System.out.println("[SHIPPING] Ship order");

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
        return response.getTrackingId();
    }

}

package dev.chords.microservices.currency;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import dev.chords.choreographies.Money;
import dev.chords.choreographies.OrderItem;
import dev.chords.choreographies.OrderItems;
import hipstershop.CurrencyServiceGrpc;
import hipstershop.CurrencyServiceGrpc.CurrencyServiceBlockingStub;
import hipstershop.Demo;
import hipstershop.Demo.CurrencyConversionRequest;
import hipstershop.Demo.Empty;
import hipstershop.Demo.GetSupportedCurrenciesResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class CurrencyService implements dev.chords.choreographies.CurrencyService {

    protected ManagedChannel channel;
    protected CurrencyServiceBlockingStub connection;

    public CurrencyService(InetSocketAddress address) {
        channel = ManagedChannelBuilder
                .forAddress(address.getHostName(), address.getPort())
                .usePlaintext()
                .build();

        connection = CurrencyServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public List<String> supportedCurrencies() {
        System.out.println("[CURRENCY] Get supported currencies");

        GetSupportedCurrenciesResponse response = connection.getSupportedCurrencies(Empty.getDefaultInstance());
        return response.getCurrencyCodesList();
    }

    @Override
    public Money convert(Money from, String toCurrency) {
        System.out.println("[CURRENCY] Convert currencies: from=" + from.currencyCode + ", to=" + toCurrency);

        CurrencyConversionRequest request = CurrencyConversionRequest.newBuilder()
                .setFrom(
                        Demo.Money.newBuilder()
                                .setCurrencyCode(from.currencyCode)
                                .setUnits(from.units)
                                .setNanos(from.nanos)
                                .build())
                .setToCode(toCurrency)
                .build();

        Demo.Money m = connection.convert(request);

        return new Money(m.getCurrencyCode(), (int) m.getUnits(), m.getNanos());
    }

    @Override
    public OrderItems convertProducts(OrderItems products, String toCurrency) {
        System.out.println("[CURRENCY] Convert products: to=" + toCurrency);

        List<OrderItem> orderItemList = products.items.stream()
                .map(product -> new OrderItem(product.item, convert(product.cost, toCurrency)))
                .toList();

        return new OrderItems(new ArrayList<>(orderItemList));
    }
}

package dev.chords.microservices.currency;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import choral.reactive.ChannelConfigurator;
import choral.reactive.tracing.JaegerConfiguration;
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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class CurrencyService implements dev.chords.choreographies.CurrencyService {

    protected ManagedChannel channel;
    protected CurrencyServiceBlockingStub connection;
    protected Tracer tracer;

    public CurrencyService(InetSocketAddress address, OpenTelemetrySdk telemetry) {
        channel = ChannelConfigurator.makeChannel(address, telemetry);
        connection = CurrencyServiceGrpc.newBlockingStub(channel);
        this.tracer = telemetry.getTracer(JaegerConfiguration.TRACER_NAME);
    }

    @Override
    public List<String> supportedCurrencies() {
        System.out.println("[CURRENCY] Get supported currencies");

        Span span = null;
        if (tracer != null) {
            span = tracer.spanBuilder("CurrencyService.supportedCurrencies").startSpan();
        }

        GetSupportedCurrenciesResponse response = connection.getSupportedCurrencies(Empty.getDefaultInstance());

        if (span != null)
            span.end();

        return response.getCurrencyCodesList();
    }

    @Override
    public Money convert(Money from, String toCurrency) {
        System.out.println("[CURRENCY] Convert currencies: from=" + from.currencyCode + ", to=" + toCurrency);

        Span span = null;
        Scope scope = null;
        if (tracer != null) {
            span = tracer.spanBuilder("CurrencyService.convertCurrency")
                    .setAttribute("request.from", from.currencyCode)
                    .setAttribute("request.to", toCurrency)
                    .startSpan();
            scope = span.makeCurrent();
        }

        CurrencyConversionRequest request = CurrencyConversionRequest.newBuilder()
                .setFrom(
                        Demo.Money.newBuilder()
                                .setCurrencyCode(from.currencyCode)
                                .setUnits(from.units)
                                .setNanos(from.nanos)
                                .build())
                .setToCode(toCurrency)
                .build();

        Span requestSpan = tracer.spanBuilder("send request").startSpan();
        Demo.Money m = connection.convert(request);
        requestSpan.end();

        if (scope != null)
            scope.close();

        if (span != null)
            span.end();

        return new Money(m.getCurrencyCode(), (int) m.getUnits(), m.getNanos());
    }

    @Override
    public OrderItems convertProducts(OrderItems products, String toCurrency) {
        System.out.println("[CURRENCY] Convert products: to=" + toCurrency);

        Span span = null;
        Scope scope = null;
        if (tracer != null) {
            span = tracer.spanBuilder("CurrencyService.convertProducts")
                    .setAttribute("request.toCurrency", toCurrency)
                    .startSpan();
            scope = span.makeCurrent();
        }

        List<OrderItem> orderItemList = products.items.stream()
                .map(product -> new OrderItem(product.item, convert(product.cost, toCurrency)))
                .toList();

        if (scope != null)
            scope.close();

        if (span != null)
            span.end();

        return new OrderItems(new ArrayList<>(orderItemList));
    }
}

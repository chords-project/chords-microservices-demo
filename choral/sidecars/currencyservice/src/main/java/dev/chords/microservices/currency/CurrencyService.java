package dev.chords.microservices.currency;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import choral.reactive.ChannelConfigurator;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.Money;
import dev.chords.choreographies.OrderItem;
import dev.chords.choreographies.OrderItems;
import hipstershop.CurrencyServiceGrpc;
import hipstershop.CurrencyServiceGrpc.CurrencyServiceFutureStub;
import hipstershop.Demo;
import hipstershop.Demo.CurrencyConversionRequest;
import hipstershop.Demo.Empty;
import hipstershop.Demo.GetSupportedCurrenciesResponse;
import io.grpc.ManagedChannel;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class CurrencyService implements dev.chords.choreographies.CurrencyService {

    protected ManagedChannel channel;
    protected CurrencyServiceFutureStub connection;
    protected Tracer tracer;

    public CurrencyService(InetSocketAddress address, OpenTelemetrySdk telemetry) {
        channel = ChannelConfigurator.makeChannel(address, telemetry);
        connection = CurrencyServiceGrpc.newFutureStub(channel);
        this.tracer = telemetry.getTracer(JaegerConfiguration.TRACER_NAME);
    }

    @Override
    public List<String> supportedCurrencies() {
        System.out.println("[CURRENCY] Get supported currencies");

        Span span = tracer.spanBuilder("CurrencyService.supportedCurrencies").startSpan();

        try (Scope scope = span.makeCurrent()) {

            GetSupportedCurrenciesResponse response = connection.getSupportedCurrencies(Empty.getDefaultInstance())
                    .get(10, TimeUnit.SECONDS);

            return response.getCurrencyCodesList();

        } catch (Exception e) {
            span.setAttribute("error", true);
            span.recordException(e);
            throw new RuntimeException(e);
        } finally {
            span.end();
        }
    }

    @Override
    public Money convert(Money from, String toCurrency) {
        System.out.println("[CURRENCY] Convert currencies: from=" + from.currencyCode + ", to=" + toCurrency);

        Span span = tracer.spanBuilder("CurrencyService.convertCurrency")
                .setAttribute("request.from", from.currencyCode)
                .setAttribute("request.to", toCurrency)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {

            CurrencyConversionRequest request = CurrencyConversionRequest.newBuilder()
                    .setFrom(
                            Demo.Money.newBuilder()
                                    .setCurrencyCode(from.currencyCode)
                                    .setUnits(from.units)
                                    .setNanos(from.nanos)
                                    .build())
                    .setToCode(toCurrency)
                    .build();

            Demo.Money m = connection.convert(request).get(10, TimeUnit.SECONDS);

            return new Money(m.getCurrencyCode(), (int) m.getUnits(), m.getNanos());

        } catch (Exception e) {
            span.setAttribute("error", true);
            span.recordException(e);
            throw new RuntimeException(e);
        } finally {
            span.end();
        }
    }

    @Override
    public OrderItems convertProducts(OrderItems products, String toCurrency) {
        System.out.println("[CURRENCY] Convert products: to=" + toCurrency);

        Span span = tracer.spanBuilder("CurrencyService.convertProducts")
                .setAttribute("request.toCurrency", toCurrency)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {

            List<OrderItem> orderItemList = products.items.stream()
                    .map(product -> new OrderItem(product.item, convert(product.cost, toCurrency)))
                    .toList();

            return new OrderItems(new ArrayList<>(orderItemList));

        } finally {
            span.end();
        }
    }
}

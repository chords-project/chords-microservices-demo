package dev.chords.microservices.payment;

import dev.chords.choreographies.CreditCardInfo;
import dev.chords.choreographies.Money;
import hipstershop.Demo;
import hipstershop.Demo.ChargeRequest;
import hipstershop.Demo.ChargeResponse;
import hipstershop.PaymentServiceGrpc;
import hipstershop.PaymentServiceGrpc.PaymentServiceFutureStub;
import io.grpc.ManagedChannel;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import choral.reactive.ChannelConfigurator;
import choral.reactive.tracing.JaegerConfiguration;

public class PaymentService implements dev.chords.choreographies.PaymentService {

    protected ManagedChannel channel;
    protected PaymentServiceFutureStub connection;
    protected Tracer tracer;

    public PaymentService(InetSocketAddress address, OpenTelemetrySdk telemetry) {
        channel = ChannelConfigurator.makeChannel(address, telemetry);

        this.connection = PaymentServiceGrpc.newFutureStub(channel);
        this.tracer = telemetry.getTracer(JaegerConfiguration.TRACER_NAME);
    }

    @Override
    public String charge(Money price, CreditCardInfo creditCardInfo) {
        System.out.println("[PAYMENT] Charge credit card");

        Span span = tracer.spanBuilder("PaymentService.charge").startSpan();

        try (Scope scope = span.makeCurrent()) {

            ChargeRequest request = ChargeRequest.newBuilder()
                    .setAmount(
                            Demo.Money.newBuilder()
                                    .setCurrencyCode(price.currencyCode)
                                    .setUnits(price.units)
                                    .setNanos(price.nanos)
                                    .build())
                    .setCreditCard(
                            Demo.CreditCardInfo.newBuilder()
                                    .setCreditCardNumber(creditCardInfo.credit_card_number)
                                    .setCreditCardCvv(creditCardInfo.credit_card_cvv)
                                    .setCreditCardExpirationYear(creditCardInfo.credit_card_expiration_year)
                                    .setCreditCardExpirationMonth(creditCardInfo.credit_card_expiration_month))
                    .build();

            ChargeResponse response = connection.charge(request).get(10, TimeUnit.SECONDS);

            return response.getTransactionId();
        } catch (Exception e) {
            span.setAttribute("error", true);
            span.recordException(e);
            throw new RuntimeException(e);
        } finally {
            span.end();
        }

    }
}

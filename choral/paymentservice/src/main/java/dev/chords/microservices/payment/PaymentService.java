package dev.chords.microservices.payment;

import java.net.InetSocketAddress;

import dev.chords.choreographies.CreditCardInfo;
import dev.chords.choreographies.Money;
import hipstershop.PaymentServiceGrpc.PaymentServiceBlockingStub;
import hipstershop.Demo;
import hipstershop.PaymentServiceGrpc;
import hipstershop.Demo.ChargeRequest;
import hipstershop.Demo.ChargeResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class PaymentService implements dev.chords.choreographies.PaymentService {

    protected ManagedChannel channel;
    protected PaymentServiceBlockingStub connection;

    public PaymentService(InetSocketAddress address) {
        channel = ManagedChannelBuilder
                .forAddress(address.getHostName(), address.getPort())
                .usePlaintext()
                .build();

        this.connection = PaymentServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public String charge(Money price, CreditCardInfo creditCardInfo) {
        ChargeRequest request = ChargeRequest
                .newBuilder()
                .setAmount(
                        Demo.Money.newBuilder()
                                .setCurrencyCode(price.currencyCode)
                                .setUnits(price.units)
                                .setNanos(price.nanos)
                                .build())
                .build();

        ChargeResponse response = connection.charge(request);
        return response.getTransactionId();
    }
}

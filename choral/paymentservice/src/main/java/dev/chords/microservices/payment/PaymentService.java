package dev.chords.microservices.payment;

import dev.chords.choreographies.CreditCardInfo;
import dev.chords.choreographies.Money;
import hipstershop.Demo;
import hipstershop.Demo.ChargeRequest;
import hipstershop.Demo.ChargeResponse;
import hipstershop.PaymentServiceGrpc;
import hipstershop.PaymentServiceGrpc.PaymentServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.net.InetSocketAddress;

public class PaymentService implements dev.chords.choreographies.PaymentService {

    protected ManagedChannel channel;
    protected PaymentServiceBlockingStub connection;

    public PaymentService(InetSocketAddress address) {
        channel = ManagedChannelBuilder.forAddress(address.getHostName(), address.getPort()).usePlaintext().build();

        this.connection = PaymentServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public String charge(Money price, CreditCardInfo creditCardInfo) {
        System.out.println("[PAYMENT] Charge credit card");

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

        ChargeResponse response = connection.charge(request);
        return response.getTransactionId();
    }
}

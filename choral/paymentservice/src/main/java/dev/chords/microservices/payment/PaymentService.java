package dev.chords.microservices.payment;

import java.net.InetSocketAddress;

import dev.chords.choreographies.CreditCardInfo;
import dev.chords.choreographies.Money;

public class PaymentService implements dev.chords.choreographies.PaymentService {

    public PaymentService(InetSocketAddress address) {
    }

    @Override
    public String charge(Money price, CreditCardInfo creditCardInfo) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'charge'");
    }
}

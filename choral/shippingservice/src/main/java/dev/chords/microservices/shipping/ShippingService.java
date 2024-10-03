package dev.chords.microservices.shipping;

import java.net.InetSocketAddress;

import dev.chords.choreographies.Address;
import dev.chords.choreographies.Cart;
import dev.chords.choreographies.Money;

public class ShippingService implements dev.chords.choreographies.ShippingService {

    public ShippingService(InetSocketAddress address) {
    }

    @Override
    public Money getQuote(Address address, Cart cart) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getQuote'");
    }

    @Override
    public String shipOrder(Address address, Cart cart) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'shipOrder'");
    }

}

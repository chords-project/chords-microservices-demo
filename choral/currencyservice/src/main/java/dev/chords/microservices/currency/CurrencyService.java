package dev.chords.microservices.currency;

import java.net.InetSocketAddress;
import java.util.List;

import dev.chords.choreographies.Money;
import dev.chords.choreographies.OrderItems;
import dev.chords.choreographies.Products;

public class CurrencyService implements dev.chords.choreographies.CurrencyService {

    public CurrencyService(InetSocketAddress address) {
    }

    @Override
    public List<String> supportedCurrencies() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'supportedCurrencies'");
    }

    @Override
    public Money convert(Money from, String toCurrency) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'convert'");
    }

    @Override
    public OrderItems convertProducts(Products products, String toCurrency) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'convertProducts'");
    }
}

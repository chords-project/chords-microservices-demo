package dev.chords.choreographies;

import java.io.Serializable;
import java.util.List;

public class Money@A implements Serializable@A {
    public String@A currencyCode;
    public Integer@A units;
    public Integer@A nanos;

    public Money() {
        this.currencyCode = null@A;
        this.units = 0@A;
        this.nanos = 0@A;
    }

    public Money(String@A currencyCode, Integer@A units, Integer@A nanos) {
        this.currencyCode = currencyCode;
        this.units = units;
        this.nanos = nanos;
    }

    public static Money@A sum(Money@A a, Money@A b) {
        return new Money@A(a.currencyCode, a.units + b.units, a.nanos + b.nanos);
    }
}
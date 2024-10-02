package dev.chords.choreographies;

import java.io.Serializable;
import java.util.List;

public class Address@A implements Serializable@A {
    public String@A streetAddress;
    public String@A city;
    public String@A state;
    public String@A country;
    public Integer@A zipCode;

    public Address() {
        streetAddress = null@A;
        city = null@A;
        state = null@A;
        country = null@A;
        zipCode = 0@A;
    }

    public Address(
        String@A streetAddress,
        String@A city,
        String@A state,
        String@A country,
        Integer@A zipCode
    ) {
        this.streetAddress = streetAddress;
        this.city = city;
        this.state = state;
        this.country = country;
        this.zipCode = zipCode;
    }

    public String@A toString() {
        return "[ Address street="@A+streetAddress+" city="@A+city+" state="@A+state+" country="@A+country+" zipCode="@A+zipCode+" ]"@A;
    }
}
package dev.chords.choreographies;

import java.io.Serializable;

public class ReqPlaceOrder@A implements Serializable@A {
    public String@A userID;
    public String@A userCurrency;
    public Address@A address;
    public String@A email;
    public CreditCardInfo@A creditCard;

    public ReqPlaceOrder() {
        userID = null@A;
        userCurrency = null@A;
        address = new Address@A();
        email = null@A;
        creditCard = new CreditCardInfo@A();
    }

    public ReqPlaceOrder(
        String@A userID,
        String@A userCurrency,
        Address@A address,
        String@A email,
        CreditCardInfo@A creditCard
    ) {
        this.userID = userID;
        this.userCurrency = userCurrency;
        this.address = address;
        this.email = email;
        this.creditCard = creditCard;
    }

    public String@A toString() {
        return "[ ReqPlaceOrder userID="@A+userID+" currency="@A+userCurrency+" address="@A+address+" email="@A+email+" creditCard="@A+creditCard+" ]"@A;
    }
}
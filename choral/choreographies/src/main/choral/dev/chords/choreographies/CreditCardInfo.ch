package dev.chords.choreographies;

import java.io.Serializable;
import java.util.List;

public class CreditCardInfo@A implements Serializable@A {
    public String@A number;
    public Integer@A cvv;
    public Integer@A expirationYear;
    public Integer@A expirationMonth;

    public CreditCardInfo() {
        number = ""@A;
        cvv = 0@A;
        expirationYear = 0@A;
        expirationMonth = 0@A;
    }

    public CreditCardInfo(
        String@A number,
        Integer@A cvv,
        Integer@A expirationYear,
        Integer@A expirationMonth
    ) {
        this.number = number;
        this.cvv = cvv;
        this.expirationYear = expirationYear;
        this.expirationMonth = expirationMonth;
    }

    public String@A toString() {
        return "[ CreditCardInfo number="@A+number+" cvv="@A+cvv+" year="@A+expirationYear+" month="@A+expirationMonth+" ]"@A;
    }
}
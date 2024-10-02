package dev.chords.choreographies;

import java.io.Serializable;
import java.util.ArrayList;

public class OrderItems@A implements Serializable@A {
    public final ArrayList@A<OrderItem> items;
    public final Money@A total;

    public OrderItems(ArrayList@A<OrderItem> items, Money@A total) {
        this.items = items;
        this.total = total;
    }

    public String@A toString() {
        return "[ OrderItems total="@A+total+" items="@A+items+" ]"@A;
    }
}
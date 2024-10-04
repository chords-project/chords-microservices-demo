package dev.chords.choreographies;

import java.io.Serializable;

public class CartItem@A implements Serializable@A {
    public final String@A productID;
    public final Integer@A quantity;

    public CartItem(String@A productID, Integer@A quantity) {
        this.productID = productID;
        this.quantity = quantity;
    }
}
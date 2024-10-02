package dev.chords.choreographies;

import java.io.Serializable;

public class OrderResult@A implements Serializable@A {
    public final String@A orderID;
    public final String@A shippingTrackingID;
    public final Money@A shippingCost;
    public final Address@A shippingAddress;
    public final OrderItems@A items;

    public OrderResult(
        String@A orderID,
        String@A shippingTrackingID,
        Money@A shippingCost,
        Address@A shippingAddress,
        OrderItems@A items
    ) {
        this.orderID = orderID;
        this.shippingTrackingID = shippingTrackingID;
        this.shippingCost = shippingCost;
        this.shippingAddress = shippingAddress;
        this.items = items;
    }

    public String@A toString() {
        return "[ OrderResult orderID="@A+orderID+" trackingID="@A+shippingTrackingID+" shippingCost="@A+shippingCost+" address="@A+shippingAddress+" items="@A+items+" ]"@A;
    }
}
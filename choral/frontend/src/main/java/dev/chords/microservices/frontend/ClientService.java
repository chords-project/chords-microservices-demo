package dev.chords.microservices.frontend;

import dev.chords.choreographies.Money;
import dev.chords.choreographies.OrderItems;
import dev.chords.microservices.frontend.MoneyUtils.MoneyException;

public class ClientService implements dev.chords.choreographies.ClientService {

    @Override
    public Money totalPrice(OrderItems orderItems, Money shippingCost) {
        try {
            Money total = shippingCost;

            for (var item : orderItems.items) {
                Money itemsPrice = MoneyUtils.multiplySlow(item.cost, item.item.quantity);
                total = MoneyUtils.sum(total, itemsPrice);
            }

            return total;
        } catch (MoneyException e) {
            throw new RuntimeException(e);
        }
    }
}

package dev.chords.microservices.frontend;

import choral.reactive.tracing.TelemetrySession;
import dev.chords.choreographies.Money;
import dev.chords.choreographies.OrderItems;
import dev.chords.microservices.frontend.MoneyUtils.MoneyException;
import io.opentelemetry.api.trace.Span;

public class ClientService implements dev.chords.choreographies.ClientService {

    protected TelemetrySession telemetrySession;

    public ClientService(TelemetrySession telemetrySession) {
        this.telemetrySession = telemetrySession;
    }

    @Override
    public Money totalPrice(OrderItems orderItems, Money shippingCost) {
        Span span = null;
        if (telemetrySession != null) {
            span = telemetrySession.startSpan("ClientService: total price");
        }

        try {
            Money total = shippingCost;

            for (var item : orderItems.items) {
                Money itemsPrice = MoneyUtils.multiplySlow(item.cost, item.item.quantity);
                total = MoneyUtils.sum(total, itemsPrice);
            }

            return total;
        } catch (MoneyException e) {
            throw new RuntimeException(e);
        } finally {
            if (span != null)
                span.end();
        }
    }
}

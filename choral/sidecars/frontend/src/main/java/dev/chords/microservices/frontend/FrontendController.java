package dev.chords.microservices.frontend;

import java.net.URISyntaxException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import choral.reactive.TCPReactiveClient;
import choral.reactive.TCPReactiveServer;
import choral.reactive.tracing.JaegerConfiguration;
import choral.reactive.tracing.TelemetrySession;
import dev.chords.choreographies.Cart;
import dev.chords.choreographies.ChorGetCartItems_Client;
import dev.chords.choreographies.ChorPlaceOrder_Client;
import dev.chords.choreographies.OrderResult;
import dev.chords.choreographies.ReqPlaceOrder;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopSession;
import dev.chords.choreographies.WebshopSession.Choreography;
import dev.chords.choreographies.WebshopSession.Service;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;

@RestController
public class FrontendController {

    TCPReactiveServer<WebshopSession> server;
    OpenTelemetrySdk telemetry = null;

    public FrontendController() {
        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        if (JAEGER_ENDPOINT != null) {
            System.out.println("Configuring choreographic telemetry to: " + JAEGER_ENDPOINT);
            this.telemetry = JaegerConfiguration.initTelemetry(JAEGER_ENDPOINT, "Frontend");
        }

        server = new TCPReactiveServer<>(
                Service.FRONTEND.name(),
                this.telemetry,
                (ctx) -> {
                    System.out.println(
                            "[FRONTEND] Received new session from " + ctx.session.senderName() + " service: "
                                    + ctx.session);
                });

        new Thread(() -> {
            try {
                server.listen(ServiceResources.shared.frontend);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }, "FRONTEND_CHORAL_SERVERS").start();

        System.out.println("[FRONTEND] Done configuring frontend controller");
    }

    @GetMapping("/ping")
    String ping() {
        return "pong";
    }

    @GetMapping("/cart/{userID}")
    String cart(@PathVariable String userID) {
        WebshopSession session = WebshopSession.makeSession(Choreography.GET_CART_ITEMS, Service.FRONTEND);

        Span span = telemetry.getTracer(JaegerConfiguration.TRACER_NAME)
                .spanBuilder("Frontend: Get cart request")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("choreography.session", session.toString())
                .startSpan();

        TelemetrySession telemetrySession = new TelemetrySession(telemetry, session, span);

        try (
                TCPReactiveClient<WebshopSession> cartClient = new TCPReactiveClient<>(
                        ServiceResources.shared.cart,
                        Service.FRONTEND.name(),
                        telemetrySession)) {
            // Get items

            System.out.println("Initiating getItem choreography with session: " + session);

            ChorGetCartItems_Client getItemsChor = new ChorGetCartItems_Client(
                    cartClient.chanA(session),
                    server.chanB(session));

            Cart cart = getItemsChor.getItems("user1");
            return "Got back cart: " + cart.userID + ", " + cart.items;
        } catch (Exception e) {
            e.printStackTrace();
            return "Server error";
        }
    }

    @PostMapping("/checkout")
    PlaceOrderResponse checkout(@RequestBody ReqPlaceOrder request) {
        System.out.println("[FRONTEND] Placing order: " + request);

        WebshopSession session = WebshopSession.makeSession(Choreography.PLACE_ORDER, Service.FRONTEND);

        Span span = telemetry.getTracer(JaegerConfiguration.TRACER_NAME)
                .spanBuilder("Frontend: Checkout request")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("choreography.session", session.toString())
                .startSpan();

        TelemetrySession telemetrySession = new TelemetrySession(telemetry, session, span);

        try (
                Scope scope = span.makeCurrent();
                TCPReactiveClient<WebshopSession> cartClient = new TCPReactiveClient<>(
                        ServiceResources.shared.cart,
                        Service.FRONTEND.name(),
                        telemetrySession);

                TCPReactiveClient<WebshopSession> currencyClient = new TCPReactiveClient<>(
                        ServiceResources.shared.currency,
                        Service.FRONTEND.name(),
                        telemetrySession);

                TCPReactiveClient<WebshopSession> shippingClient = new TCPReactiveClient<>(
                        ServiceResources.shared.shipping,
                        Service.FRONTEND.name(),
                        telemetrySession);

                TCPReactiveClient<WebshopSession> paymentClient = new TCPReactiveClient<>(
                        ServiceResources.shared.payment,
                        Service.FRONTEND.name(),
                        telemetrySession);) {
            // Get items
            server.registerSession(session);

            System.out.println("[FRONTEND] Initiating placeOrder choreography with session: " + session);

            ChorPlaceOrder_Client placeOrderChor = new ChorPlaceOrder_Client(
                    new ClientService(telemetrySession.tracer),
                    cartClient.chanA(session),
                    currencyClient.chanA(session),
                    shippingClient.chanA(session),
                    paymentClient.chanA(session),
                    server.chanB(session, Service.CURRENCY.name()),
                    server.chanB(session, Service.SHIPPING.name()));

            OrderResult result = placeOrderChor.placeOrder(request);

            return new PlaceOrderResponse(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (span != null)
                span.end();
        }
    }
}

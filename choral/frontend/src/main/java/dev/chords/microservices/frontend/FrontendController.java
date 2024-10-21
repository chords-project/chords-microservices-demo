package dev.chords.microservices.frontend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import choral.reactive.Session;
import choral.reactive.TCPChoreographyManager;
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
import dev.chords.choreographies.WebshopChoreography;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;

@RestController
public class FrontendController {

    TCPReactiveServer<WebshopChoreography> cartToFrontendServer;
    TCPReactiveServer<WebshopChoreography> currencyToFrontendServer;
    TCPReactiveServer<WebshopChoreography> shippingToFrontendServer;
    TCPChoreographyManager<WebshopChoreography> manager;
    OpenTelemetrySdk telemetry = null;

    public FrontendController() {
        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        if (JAEGER_ENDPOINT != null) {
            System.out.println("Configuring choreographic telemetry to: " + JAEGER_ENDPOINT);
            this.telemetry = JaegerConfiguration.initTelemetry(JAEGER_ENDPOINT, "Frontend");
        }

        this.manager = new TCPChoreographyManager<>(this.telemetry);

        cartToFrontendServer = manager.configureServer(ServiceResources.shared.cartToFrontend, (ctx) -> {
            System.out.println("[FRONTEND] Received new session from CART_TO_FRONTEND service: " + ctx.session);
        });

        currencyToFrontendServer = manager.configureServer(ServiceResources.shared.currencyToFrontend, (ctx) -> {
            System.out.println("[FRONTEND] Received new session from CURRENCY_TO_FRONTEND service: " + ctx.session);
        });

        shippingToFrontendServer = manager.configureServer(ServiceResources.shared.shippingToFrontend, (ctx) -> {
            System.out.println("[FRONTEND] Received new session from SHIPPING_TO_FRONTEND service: " + ctx.session);
        });

        new Thread(() -> {
            manager.listen();
        }, "FRONTEND_CHORAL_SERVERS").start();

        System.out.println("[FRONTEND] Done configuring frontend controller");
    }

    @GetMapping("/ping")
    String ping() {
        return "pong";
    }

    @GetMapping("/cart/{userID}")
    String cart(@PathVariable String userID) {
        Session<WebshopChoreography> session = Session.makeSession(WebshopChoreography.GET_CART_ITEMS);

        Span span = telemetry.getTracer(JaegerConfiguration.TRACER_NAME)
                .spanBuilder("Frontend: Get cart request")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("choreography.session", session.toString())
                .startSpan();

        TelemetrySession telemetrySession = new TelemetrySession(telemetry, session, span);

        try (
                TCPReactiveClient<WebshopChoreography> cartClient = new TCPReactiveClient<>(
                        ServiceResources.shared.frontendToCart, telemetrySession)) {
            // Get items

            System.out.println("Initiating getItem choreography with session: " + session);

            ChorGetCartItems_Client getItemsChor = new ChorGetCartItems_Client(
                    cartClient.chanA(session),
                    cartToFrontendServer.chanB(session));

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

        Session<WebshopChoreography> session = Session.makeSession(WebshopChoreography.PLACE_ORDER);

        Span span = telemetry.getTracer(JaegerConfiguration.TRACER_NAME)
                .spanBuilder("Frontend: Checkout request")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("choreography.session", session.toString())
                .startSpan();

        TelemetrySession telemetrySession = new TelemetrySession(telemetry, session, span);

        try (
                Scope scope = span.makeCurrent();
                TCPReactiveClient<WebshopChoreography> cartClient = new TCPReactiveClient<>(
                        ServiceResources.shared.frontendToCart, telemetrySession);
                TCPReactiveClient<WebshopChoreography> currencyClient = new TCPReactiveClient<>(
                        ServiceResources.shared.frontendToCurrency, telemetrySession);
                TCPReactiveClient<WebshopChoreography> shippingClient = new TCPReactiveClient<>(
                        ServiceResources.shared.frontendToShipping, telemetrySession);
                TCPReactiveClient<WebshopChoreography> paymentClient = new TCPReactiveClient<>(
                        ServiceResources.shared.frontendToPayment, telemetrySession);) {
            // Get items
            manager.registerSession(session);

            System.out.println("[FRONTEND] Initiating placeOrder choreography with session: " + session);

            ChorPlaceOrder_Client placeOrderChor = new ChorPlaceOrder_Client(
                    new ClientService(telemetrySession.tracer),
                    cartClient.chanA(session),
                    currencyClient.chanA(session),
                    shippingClient.chanA(session),
                    paymentClient.chanA(session),
                    currencyToFrontendServer.chanB(session),
                    shippingToFrontendServer.chanB(session));

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

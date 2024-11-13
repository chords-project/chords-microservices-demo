package dev.chords.microservices.frontend;

import java.net.URISyntaxException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import choral.channels.SymChannel_A;
import choral.reactive.ReactiveSymChannel;
import choral.reactive.TCPReactiveClient;
import choral.reactive.TCPReactiveServer;
import choral.reactive.tracing.JaegerConfiguration;
import choral.reactive.tracing.TelemetrySession;
import dev.chords.choreographies.ChorPlaceOrder_Client;
import dev.chords.choreographies.OrderResult;
import dev.chords.choreographies.ReqPlaceOrder;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopSession;
import dev.chords.choreographies.WebshopSession.Choreography;
import dev.chords.choreographies.WebshopSession.Service;
import io.opentelemetry.api.common.Attributes;
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
                                                        "[FRONTEND] Received new session from "
                                                                        + ctx.session.senderName() + " service: "
                                                                        + ctx.session);
                                });

                Thread.ofPlatform()
                                .name("FRONTEND_CHORAL_SERVERS")
                                .start(() -> {
                                        try {
                                                server.listen(ServiceResources.shared.frontend);
                                        } catch (URISyntaxException e) {
                                                throw new RuntimeException(e);
                                        }
                                });

                System.out.println("[FRONTEND] Done configuring frontend controller");
        }

        @GetMapping("/ping")
        String ping() {
                return "pong";
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

                        telemetrySession.log("[FRONTEND] Initiating PLACE_ORDER choreography");

                        var currencyChan = new ReactiveSymChannel<>(
                                        currencyClient.chanA(session),
                                        server.chanB(session, Service.CURRENCY.name()));

                        var shippingChan = new ReactiveSymChannel<>(
                                        shippingClient.chanA(session),
                                        server.chanB(session, Service.SHIPPING.name()));

                        var paymentChan = new ReactiveSymChannel<>(
                                        paymentClient.chanA(session),
                                        server.chanB(session, Service.PAYMENT.name()));

                        ChorPlaceOrder_Client placeOrderChor = new ChorPlaceOrder_Client(
                                        new ClientService(telemetrySession.tracer),
                                        currencyChan,
                                        shippingChan,
                                        paymentChan,
                                        cartClient.chanA(session));

                        OrderResult result = placeOrderChor.placeOrder(request);

                        telemetrySession.log("[FRONTEND] Finished PLACE_ORDER choreography",
                                        Attributes.builder().put("order.result", result.toString()).build());

                        return new PlaceOrderResponse(result);
                } catch (Exception e) {
                        telemetrySession.recordException("Frontend PLACE_ORDER choreography failed", e, true);

                        throw new RuntimeException(e);
                } finally {
                        if (span != null)
                                span.end();
                }
        }
}

package dev.chords.microservices.currency;

import java.net.InetSocketAddress;

import choral.reactive.TCPReactiveServer;
import choral.reactive.TCPReactiveServer.SessionContext;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.ChorPlaceOrder_Currency;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopSession;
import dev.chords.choreographies.WebshopSession.Service;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class Main {

    private static CurrencyService currencyService;
    private static OpenTelemetrySdk telemetry = null;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral currency service");

        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        if (JAEGER_ENDPOINT != null) {
            System.out.println("Configuring choreographic telemetry to: " + JAEGER_ENDPOINT);
            telemetry = JaegerConfiguration.initTelemetry(JAEGER_ENDPOINT, "CurrencyService");
        }

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "7000"));
        currencyService = new CurrencyService(new InetSocketAddress("localhost", rpcPort),
                telemetry);

        TCPReactiveServer<WebshopSession> server = new TCPReactiveServer<>(
                Service.CURRENCY.name(),
                telemetry,
                Main::handleNewSession);

        server.listen(ServiceResources.shared.currency);
    }

    private static void handleNewSession(SessionContext<WebshopSession> ctx) throws Exception {
        switch (ctx.session.choreography) {
            case PLACE_ORDER:
                System.out.println("[CURRENCY] New PLACE_ORDER request " + ctx.session);

                ChorPlaceOrder_Currency placeOrderChor = new ChorPlaceOrder_Currency(
                        currencyService,
                        ctx.chanB(Service.FRONTEND.name()),
                        ctx.chanB(Service.PRODUCT_CATALOG.name()),
                        ctx.chanA(ServiceResources.shared.frontend));

                placeOrderChor.placeOrder();
                System.out.println("[CURRENCY] PLACE_ORDER choreography completed " + ctx.session);

                break;
            default:
                System.out.println("Invalid choreography " + ctx.session.choreographyName());
                break;
        }
    }
}
package dev.chords.microservices.currency;

import choral.reactive.TCPReactiveClientConnection;
import choral.reactive.TCPReactiveServer;
import choral.reactive.TCPReactiveServer.SessionContext;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.ChorPlaceOrder_Currency;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopSession;
import dev.chords.choreographies.WebshopSession.Service;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.net.InetSocketAddress;

public class Main {

    private static CurrencyService currencyService;
    private static OpenTelemetrySdk telemetry;

    private static TCPReactiveClientConnection frontendConn;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral currency service");

        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        telemetry = OpenTelemetrySdk.builder().build();
        if (JAEGER_ENDPOINT != null) {
            System.out.println(
                "Configuring choreographic telemetry to: " + JAEGER_ENDPOINT
            );
            telemetry = JaegerConfiguration.initTelemetry(
                JAEGER_ENDPOINT,
                "CurrencyService"
            );
        }

        int rpcPort = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "7000")
        );
        currencyService = new CurrencyService(
            new InetSocketAddress("localhost", rpcPort),
            telemetry
        );

        frontendConn = new TCPReactiveClientConnection(
            ServiceResources.shared.frontend
        );

        TCPReactiveServer<WebshopSession> server = new TCPReactiveServer<>(
            Service.CURRENCY.name(),
            telemetry,
            Main::handleNewSession
        );

        server.listen(ServiceResources.shared.currency);
    }

    private static void handleNewSession(SessionContext<WebshopSession> ctx)
        throws Exception {
        switch (ctx.session.choreography) {
            case PLACE_ORDER:
                ctx.log("[CURRENCY] New PLACE_ORDER request");

                ChorPlaceOrder_Currency placeOrderChor =
                    new ChorPlaceOrder_Currency(
                        currencyService,
                        ctx.symChan(Service.FRONTEND.name(), frontendConn),
                        ctx.chanB(Service.PRODUCT_CATALOG.name()),
                        ctx.chanB(Service.SHIPPING.name())
                    );

                placeOrderChor.placeOrder();
                ctx.log("[CURRENCY] PLACE_ORDER choreography completed");

                break;
            default:
                ctx.log(
                    "Invalid choreography " + ctx.session.choreographyName()
                );
                break;
        }
    }
}

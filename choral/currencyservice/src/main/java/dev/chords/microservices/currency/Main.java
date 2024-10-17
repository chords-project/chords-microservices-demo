package dev.chords.microservices.currency;

import java.net.InetSocketAddress;

import choral.reactive.TCPChoreographyManager;
import choral.reactive.TCPChoreographyManager.SessionContext;
import choral.reactive.TCPReactiveServer;
import choral.reactive.tracing.JaegerConfiguration;
import dev.chords.choreographies.ChorPlaceOrder_Currency;
import dev.chords.choreographies.ServiceResources;
import dev.chords.choreographies.WebshopChoreography;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class Main {

    public static CurrencyService currencyService;

    public static TCPReactiveServer<WebshopChoreography> frontendServer = null;
    public static TCPReactiveServer<WebshopChoreography> productServer = null;

    public static TCPChoreographyManager<WebshopChoreography> manager;
    public static OpenTelemetrySdk telemetry = null;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting choral currency service");

        final String JAEGER_ENDPOINT = System.getenv().get("JAEGER_ENDPOINT");
        if (JAEGER_ENDPOINT != null) {
            System.out.println("Configuring choreographic telemetry to: " + JAEGER_ENDPOINT);
            telemetry = JaegerConfiguration.initTelemetry(JAEGER_ENDPOINT, "CurrencyService");
        }

        manager = new TCPChoreographyManager<>(telemetry);

        int rpcPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "7000"));
        currencyService = new CurrencyService(new InetSocketAddress("localhost", rpcPort),
                telemetry.getTracer(JaegerConfiguration.TRACER_NAME));

        frontendServer = manager.configureServer(ServiceResources.shared.frontendToCurrency, Main::handleNewSession);
        productServer = manager.configureServer(ServiceResources.shared.productcatalogToCurrency,
                Main::handleNewSession);

        manager.listen();
    }

    private static void handleNewSession(SessionContext<WebshopChoreography> ctx) throws Exception {
        switch (ctx.session.choreographyID) {
            case PLACE_ORDER:
                System.out.println("[CURRENCY] New PLACE_ORDER request " + ctx.session);

                ChorPlaceOrder_Currency placeOrderChor = new ChorPlaceOrder_Currency(
                        currencyService,
                        frontendServer.chanB(ctx.session),
                        productServer.chanB(ctx.session),
                        ctx.chanA(ServiceResources.shared.currencyToFrontend));

                placeOrderChor.placeOrder();
                System.out.println("[CURRENCY] PLACE_ORDER choreography completed " + ctx.session);

                break;
            default:
                System.out.println("Invalid choreography ID " + ctx.session.choreographyID);
                break;
        }
    }
}
package choral.reactive;

import java.io.IOException;
import java.net.URISyntaxException;

import choral.reactive.tracing.JaegerConfiguration;
import choral.reactive.tracing.TelemetrySession;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class Test {

    public static void main(String[] args) {

        final String JAEGER_ENDPOINT = "http://localhost:4317";
        OpenTelemetrySdk telemetry = JaegerConfiguration.initTelemetry(JAEGER_ENDPOINT, "Test");

        TCPChoreographyManager<String> managerA = new TCPChoreographyManager<>(telemetry);

        managerA.configureServer("0.0.0.0:4567", (ctx) -> {
            try {
                System.out.println("Received message on session: " + ctx.session);
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        new Thread(() -> {
            managerA.listen();
        }).start();

        runClient(telemetry);
    }

    public static void runClient(OpenTelemetrySdk telemetry) {

        Tracer tracer = telemetry.getTracer(JaegerConfiguration.TRACER_NAME);

        Session<String> session = Session.makeSession("chor");
        Span span = tracer
                .spanBuilder("client connect")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("choreography.session", session.toString())
                .startSpan();

        TelemetrySession initialTelemetrySession = new TelemetrySession(telemetry, session,
                Context.current().with(span), null);

        try (TCPReactiveClient<String> client1 = new TCPReactiveClient<>("0.0.0.0:4567",
                initialTelemetrySession);) {
            var chan = client1.chanA(session);
            chan.com("hello");
            chan.com("world");

            Thread.sleep(5000);
            System.out.println("Done");
        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            span.end();
        }
    }
}

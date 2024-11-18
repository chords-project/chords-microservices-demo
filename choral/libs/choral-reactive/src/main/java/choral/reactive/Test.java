package choral.reactive;

import java.io.IOException;
import java.net.URISyntaxException;

import choral.reactive.tracing.JaegerConfiguration;
import choral.reactive.tracing.TelemetrySession;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class Test {

    public static void main(String[] args) {

        final String JAEGER_ENDPOINT = "http://localhost:4317";
        OpenTelemetrySdk telemetry = JaegerConfiguration.initTelemetry(JAEGER_ENDPOINT, "Test");

        TCPReactiveServer<SimpleSession> server = new TCPReactiveServer<>("server", telemetry, (ctx) -> {
            try {
                System.out.println(
                        "SERVER: Received message: " + ctx.session + " from sender: " + ctx.session.sender);

                String firstMsg = ctx.chanB("client").com();
                String secondMsg = ctx.chanB("client").com();

                System.out.println("SERVER: Received first message: " + firstMsg);
                System.out.println("SERVER: Received second message: " + secondMsg);

                Thread.sleep(500);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread.ofPlatform().start(() -> {
            try {
                server.listen("0.0.0.0:4567");
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });

        runClient(telemetry);
    }

    public static void runClient(OpenTelemetrySdk telemetry) {

        Tracer tracer = telemetry.getTracer(JaegerConfiguration.TRACER_NAME);

        SimpleSession session = SimpleSession.makeSession("chor", "client");
        Span span = tracer
                .spanBuilder("client connect")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("choreography.session", session.toString())
                .startSpan();

        TelemetrySession initialTelemetrySession = new TelemetrySession(telemetry, session, span);

        try (TCPReactiveClientConnection clientConn = new TCPReactiveClientConnection("0.0.0.0:4567")) {
            TCPReactiveClient<SimpleSession> client1 = new TCPReactiveClient<>(
                    clientConn,
                    "client",
                    initialTelemetrySession);

            var chan = client1.chanA(session);

            chan.com("hello");

            Thread.sleep(200);

            chan.com("world");

            Thread.sleep(2000);
            System.out.println("Done");
        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Ending choreography span");
            span.end();
        }
    }
}

package dev.chords.microservices.benchmark;

import java.io.Serializable;

import choral.reactive.ReactiveSymChannel;
import choral.reactive.SimpleSession;
import choral.reactive.TCPReactiveClient;
import choral.reactive.TCPReactiveServer;
import choral.reactive.tracing.JaegerConfiguration;
import choral.reactive.tracing.TelemetrySession;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class ServiceA {

    private OpenTelemetrySdk telemetry;
    private TCPReactiveServer<SimpleSession> serverA;
    private String addressServiceB;

    public ServiceA(OpenTelemetrySdk telemetry, String addressServiceB) {
        this.telemetry = telemetry;
        this.addressServiceB = addressServiceB;
        this.serverA = new TCPReactiveServer<>("serviceA", telemetry, (ctx) -> {
            System.out.println("ServiceA received new session");
        });
    }

    public void listen(String address) {
        new Thread(() -> {
            try {
                serverA.listen("localhost:8201");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "serviceA").start();
    }

    public void startPingPong() throws Exception {
        SimpleSession session = SimpleSession.makeSession("ping-pong", "serviceA");
        serverA.registerSession(session);

        Span span = telemetry.getTracer(JaegerConfiguration.TRACER_NAME)
                .spanBuilder("ping-pong")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("choreography.session", session.toString())
                .startSpan();

        TelemetrySession telemetrySession = new TelemetrySession(
                telemetry,
                session,
                span);

        try (TCPReactiveClient<SimpleSession> client = new TCPReactiveClient<>(addressServiceB, "serviceA",
                telemetrySession);) {

            ReactiveSymChannel<SimpleSession, Serializable> ch = new ReactiveSymChannel<>(
                    client.chanA(session),
                    serverA.chanB(session, "serviceB"));

            SimpleChoreography_A chor = new SimpleChoreography_A(ch);
            chor.pingPong();
        }

        span.end();
    }

    public void startGreeting() throws Exception {
        SimpleSession session = SimpleSession.makeSession("greeting", "serviceA");
        serverA.registerSession(session);

        Span span = telemetry.getTracer(JaegerConfiguration.TRACER_NAME)
                .spanBuilder("greeting")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("choreography.session", session.toString())
                .startSpan();

        TelemetrySession telemetrySession = new TelemetrySession(
                telemetry,
                session,
                span);

        try (TCPReactiveClient<SimpleSession> client = new TCPReactiveClient<>(addressServiceB, "serviceA",
                telemetrySession);) {

            ReactiveSymChannel<SimpleSession, Serializable> ch = new ReactiveSymChannel<>(
                    client.chanA(session),
                    serverA.chanB(session, "serviceB"));

            GreeterChoreography_A chor = new GreeterChoreography_A(ch);
            chor.greet();
        }

        span.end();
    }

    public void close() throws Exception {
        serverA.close();
        telemetry.close();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Service A");

        final String JAEGER_ENDPOINT = "http://localhost:4317";
        OpenTelemetrySdk telemetry = JaegerConfiguration.initTelemetry(JAEGER_ENDPOINT, "ServiceA");

        ServiceA service = new ServiceA(telemetry, "localhost:8202");
        service.listen("localhost:8201");

        service.startPingPong();

        service.close();
    }
}

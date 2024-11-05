package dev.chords.microservices.benchmark;

import java.net.URISyntaxException;

import choral.reactive.SimpleSession;
import choral.reactive.TCPReactiveServer;
import choral.reactive.tracing.JaegerConfiguration;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class ServiceB {
    private OpenTelemetrySdk telemetry;
    private TCPReactiveServer<SimpleSession> serverB;
    private GrpcClient grpcClient;

    public ServiceB(OpenTelemetrySdk telemetry, String addressServiceA) {
        this.telemetry = telemetry;

        this.grpcClient = new GrpcClient(5430, telemetry);

        this.serverB = new TCPReactiveServer<>("serviceB", telemetry, (ctx) -> {
            switch (ctx.session.choreographyID) {
                case "ping-pong":
                    SimpleChoreography_B pingPongChor = new SimpleChoreography_B(
                            ctx.symChan("serviceA", addressServiceA));

                    pingPongChor.pingPong();

                    break;
                case "greeting":
                    GreeterChoreography_B greeterChor = new GreeterChoreography_B(
                            ctx.symChan("serviceA", addressServiceA),
                            grpcClient);

                    greeterChor.greet();

                    break;
                default:
                    throw new RuntimeException("unknown choreography: " + ctx.session.choreographyID);
            }
        });
    }

    public void listen(String address) {
        Thread.ofPlatform()
                .name("serviceB")
                .start(() -> {
                    try {
                        serverB.listen(address);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public void close() throws Exception {
        serverB.close();
        telemetry.close();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Service A");

        final String JAEGER_ENDPOINT = "http://localhost:4317";
        OpenTelemetrySdk telemetry = JaegerConfiguration.initTelemetry(JAEGER_ENDPOINT, "ServiceB");

        ServiceB service = new ServiceB(telemetry, "localhost:8201");
        service.serverB.listen("localhost:8202");

    }

}

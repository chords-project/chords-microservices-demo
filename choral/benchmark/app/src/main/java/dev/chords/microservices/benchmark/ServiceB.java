package dev.chords.microservices.benchmark;

import choral.reactive.connection.ClientConnectionManager;
import choral.reactive.SimpleSession;
import choral.reactive.ReactiveServer;
import choral.reactive.tracing.JaegerConfiguration;
import io.opentelemetry.sdk.OpenTelemetrySdk;

import java.io.IOException;
import java.net.URISyntaxException;

public class ServiceB {

    private OpenTelemetrySdk telemetry;
    private ReactiveServer<SimpleSession> serverB;
    private ClientConnectionManager connectionServiceA;
    private GrpcClient grpcClient;

    public ServiceB(OpenTelemetrySdk telemetry, String addressServiceA) throws Exception {
        this.telemetry = telemetry;
        this.grpcClient = new GrpcClient(5430, telemetry);
        this.connectionServiceA = ClientConnectionManager.makeConnectionManager(addressServiceA, telemetry);

        this.serverB = new ReactiveServer<>("serviceB", telemetry, ctx -> {
            switch (ctx.session.choreographyID) {
                case "ping-pong":
                    SimpleChoreography_B pingPongChor = new SimpleChoreography_B(
                            ctx.symChan("serviceA", connectionServiceA));

                    pingPongChor.pingPong();

                    break;
                case "greeting":
                    GreeterChoreography_B greeterChor = new GreeterChoreography_B(
                            ctx.symChan("serviceA", connectionServiceA), grpcClient);

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
                    } catch (URISyntaxException | IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public void close() throws Exception {
        connectionServiceA.close();
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

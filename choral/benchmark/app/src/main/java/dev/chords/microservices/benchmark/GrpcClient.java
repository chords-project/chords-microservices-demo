package dev.chords.microservices.benchmark;

import choral.reactive.tracing.JaegerConfiguration;
import greeting.GreeterGrpc;
import greeting.GreeterGrpc.GreeterBlockingStub;
import greeting.Greeting.HelloReply;
import greeting.Greeting.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class GrpcClient implements GreeterService {

    ManagedChannel channel;
    GreeterBlockingStub blockingStub;
    Tracer tracer;

    GrpcClient(int port, OpenTelemetrySdk telemetry) {
        this.tracer = telemetry.getTracer(JaegerConfiguration.TRACER_NAME);

        this.channel = ManagedChannelBuilder
                .forAddress("localhost", port)
                .usePlaintext()
                .build();

        this.blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    @Override
    public String greet(String name) {

        Span span = tracer.spanBuilder("GrpcClient.greet")
                .setAttribute("request.name", name)
                .startSpan();

        HelloReply reply;
        try (Scope scope = span.makeCurrent();) {
            reply = blockingStub.sayHello(HelloRequest.newBuilder().setName(name).build());
        } finally {
            span.end();
        }

        return reply.getMessage();
    }
}

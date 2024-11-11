package dev.chords.microservices.benchmark;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import choral.reactive.tracing.JaegerConfiguration;
import greeting.GreeterGrpc;
import greeting.GreeterGrpc.GreeterBlockingStub;
import greeting.GreeterGrpc.GreeterFutureStub;
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
    // GreeterFutureStub futureStub;
    Tracer tracer;

    GrpcClient(int port, OpenTelemetrySdk telemetry) {
        this.tracer = telemetry.getTracer(JaegerConfiguration.TRACER_NAME);

        this.channel = ManagedChannelBuilder
                .forAddress("localhost", port)
                .usePlaintext()
                .build();

        this.blockingStub = GreeterGrpc.newBlockingStub(channel);
        // this.futureStub = GreeterGrpc.newFutureStub(channel);
    }

    @Override
    public String greet(String name) {

        Span span = tracer.spanBuilder("GrpcClient.greet")
                .setAttribute("request.name", name)
                .startSpan();

        HelloReply reply;
        try (Scope scope = span.makeCurrent();) {
            reply = blockingStub.sayHello(HelloRequest.newBuilder().setName(name).build());

            // reply =
            // futureStub.sayHello(HelloRequest.newBuilder().setName(name).build()).get();
            // } catch (InterruptedException | ExecutionException e) {
            // throw new RuntimeException(e);
        } finally {
            span.end();
        }

        return reply.getMessage();
    }

    public void shutdown() {
        channel.shutdown();
    }
}

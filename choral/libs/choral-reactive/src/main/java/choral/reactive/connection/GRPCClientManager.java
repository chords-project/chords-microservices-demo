package choral.reactive.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import choral.reactive.tracing.JaegerConfiguration;
import choral_reactive.ChannelGrpc;
import choral_reactive.ChannelGrpc.ChannelBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class GRPCClientManager implements ClientConnectionManager {

    private final ManagedChannel channel;
    private final OpenTelemetrySdk telemetry;
    private final Span clientSpan;

    public GRPCClientManager(String address, OpenTelemetrySdk telemetry) throws URISyntaxException {
        URI uri = new URI(null, address, null, null, null).parseServerAuthority();
        InetSocketAddress socketAddr = new InetSocketAddress(uri.getHost(), uri.getPort());

        this.telemetry = telemetry;

        this.clientSpan = telemetry.getTracer(JaegerConfiguration.TRACER_NAME)
                .spanBuilder("GRPCClientManager connection 123")
                .setAttribute("channel.address", address)
                .startSpan();

        this.channel = ManagedChannelBuilder
                .forAddress(socketAddr.getHostString(), socketAddr.getPort())
                .usePlaintext()
                .build();
    }

    @Override
    public Connection makeConnection() throws IOException, InterruptedException {
        ChannelBlockingStub blockingStub = ChannelGrpc
                .newBlockingStub(channel)
                .withDeadlineAfter(10, TimeUnit.SECONDS);

        return new ClientConnection(blockingStub, telemetry);
    }

    @Override
    public void close() throws IOException, InterruptedException {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            clientSpan.setAttribute("error", true);
            clientSpan.recordException(e);
            throw e;
        } finally {
            clientSpan.end();
        }
    }

    public static class ClientConnection implements Connection {

        ChannelBlockingStub blockingStub;
        Span connectionSpan;

        private ClientConnection(ChannelBlockingStub blockingStub, OpenTelemetrySdk telemetry) {
            this.connectionSpan = telemetry.getTracer(JaegerConfiguration.TRACER_NAME)
                    .spanBuilder("GRPCClientManager connection")
                    .startSpan();

            this.blockingStub = blockingStub;
        }

        @Override
        public void sendMessage(Message msg) throws IOException, InterruptedException {
            connectionSpan.addEvent("Send message", Attributes.builder().put("message", msg.toString()).build());

            try {
                blockingStub.sendMessage(msg.toGrpcMessage());
            } catch (Exception e) {
                connectionSpan.setAttribute("error", true);
                connectionSpan.recordException(e);
                throw e;
            }
        }

        @Override
        public void close() throws IOException {
            connectionSpan.end();
        }
    }
}
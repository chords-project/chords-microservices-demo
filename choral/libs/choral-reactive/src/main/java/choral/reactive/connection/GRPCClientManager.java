package choral.reactive.connection;

import choral.reactive.tracing.JaegerConfiguration;
import choral_reactive.ChannelGrpc;
import choral_reactive.ChannelGrpc.ChannelFutureStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GRPCClientManager implements ClientConnectionManager {

    private final ManagedChannel channel;
    private final ChannelFutureStub futureStub;
    private final OpenTelemetrySdk telemetry;
    private final String address;

    public GRPCClientManager(String address, OpenTelemetrySdk telemetry) throws URISyntaxException {
        this.address = address;
        this.telemetry = telemetry;

        URI uri = new URI(null, address, null, null, null).parseServerAuthority();
        InetSocketAddress socketAddr = new InetSocketAddress(uri.getHost(), uri.getPort());

        this.channel = ManagedChannelBuilder
                .forAddress(socketAddr.getHostString(), socketAddr.getPort())
                .usePlaintext()
                .build();

        this.futureStub = ChannelGrpc
            .newFutureStub(channel);
            //.withDeadlineAfter(10, TimeUnit.SECONDS);
    }

    @Override
    public Connection makeConnection() {
        return new ClientConnection();
    }

    @Override
    public void close() throws IOException, InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public class ClientConnection implements Connection {

        Span connectionSpan;

        private ClientConnection() {
            this.connectionSpan = telemetry.getTracer(JaegerConfiguration.TRACER_NAME)
                    .spanBuilder("GRPCConnection: " + address)
                    .setAttribute("address", address)
                    .startSpan();
        }

        @Override
        public void sendMessage(Message msg) throws Exception {
            connectionSpan.addEvent("Send message",
                    Attributes.builder()
                            .put("message", msg.toString())
                            .put("address", address)
                            .build());

            Span sendSpan = telemetry.getTracer(JaegerConfiguration.TRACER_NAME)
                .spanBuilder("GRPC async send message").startSpan();

            try (Scope scope = sendSpan.makeCurrent()) {
                var result = futureStub.sendMessage(msg.toGrpcMessage());
                    //.get(10, TimeUnit.SECONDS);

                result.addListener(() -> {
                    try {
                        result.get();
                    } catch (Exception e) {
                        sendSpan.setAttribute("error", e.getMessage());
                        sendSpan.recordException(e);
                    } finally {
                        sendSpan.end();
                    }
                }, Executors.newSingleThreadExecutor());
            }
        }

        @Override
        public void close() throws IOException {
            connectionSpan.end();
        }

        @Override
        public String toString() {
            return "GRPCConnection [ address=" + address + " ]";
        }
    }
}

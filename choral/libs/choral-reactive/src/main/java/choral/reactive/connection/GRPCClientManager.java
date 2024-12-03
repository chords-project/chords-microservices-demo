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
    private final ChannelBlockingStub blockingStub;
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

        this.blockingStub = ChannelGrpc
            .newBlockingStub(channel);
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
        public void sendMessage(Message msg) throws IOException, InterruptedException {
            connectionSpan.addEvent("Send message",
                    Attributes.builder()
                            .put("message", msg.toString())
                            .put("address", address)
                            .build());

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

        @Override
        public String toString() {
            return "GRPCConnection [ address=" + address + " ]";
        }
    }
}

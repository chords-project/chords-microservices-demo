package choral.reactive.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.*;

import choral.reactive.tracing.JaegerConfiguration;
import choral_reactive.ChannelGrpc;
import choral_reactive.ChannelGrpc.ChannelFutureStub;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.OpenTelemetrySdk;

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

            try {
                futureStub.sendMessage(msg.toGrpcMessage()).get(10, TimeUnit.SECONDS);
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

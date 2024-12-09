package choral.reactive.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Empty;

import choral_reactive.ChannelGrpc.ChannelImplBase;
import choral_reactive.ChannelOuterClass.Message;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import io.grpc.protobuf.services.HealthStatusManager;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class GRPCServerManager implements ServerConnectionManager {

    private final ServerEvents serverEvents;
    private Server server;

    public GRPCServerManager(ServerEvents serverEvents, OpenTelemetrySdk telemetry) {
        this.serverEvents = serverEvents;
    }

    @Override
    public void listen(String address) throws URISyntaxException, IOException {
        URI uri = new URI(null, address, null, null, null).parseServerAuthority();
        InetSocketAddress addr = new InetSocketAddress(uri.getHost(), uri.getPort());

        HealthStatusManager health = new HealthStatusManager();

        server = Grpc.newServerBuilderForPort(addr.getPort(), InsecureServerCredentials.create())
                .addService(new ChannelGrpcImpl())
                .addService(health.getHealthService())
                .build()
                .start();

        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        if (server != null) {
            try {
                server.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class ChannelGrpcImpl extends ChannelImplBase {

        @Override
        public void sendMessage(Message request, StreamObserver<Empty> responseObserver) {
            try {
                var message = new choral.reactive.connection.Message(request);
                serverEvents.messageReceived(message);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }

}

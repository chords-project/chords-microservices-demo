package choral.reactive;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.opentelemetry.GrpcOpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class ChannelConfigurator {
    public static ManagedChannel makeChannel(InetSocketAddress address, OpenTelemetrySdk telemetry) {
        ManagedChannelBuilder builder = ManagedChannelBuilder
                .forAddress(address.getHostName(), address.getPort())
                .usePlaintext()
                .keepAliveTime(5, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true);

        GrpcOpenTelemetry grpcOpenTelmetry = GrpcOpenTelemetry.newBuilder()
                .sdk(telemetry)
                .build();

        grpcOpenTelmetry.configureChannelBuilder(builder);

        return builder.build();
    }
}

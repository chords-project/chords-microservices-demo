package choral.reactive.connection;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * An object that contains logic for connecting to a remote server. This interface abstracts
 * details of the connection protocol, such as TCP or gRPC, and whether the connection is pooled.
 */
public interface ClientConnectionManager extends AutoCloseable {
    Connection makeConnection() throws IOException, InterruptedException;

    @Override
    void close() throws IOException, InterruptedException;

    public interface Connection extends AutoCloseable {
        void sendMessage(Message msg) throws IOException, InterruptedException;

        @Override
        void close() throws IOException, InterruptedException;
    }

    public static ClientConnectionManager makeConnectionManager(String address, OpenTelemetrySdk telemetry)
            throws URISyntaxException, IOException {
        // return new TCPClientManagerSimple(address, telemetry);
        // return new TCPClientManagerPool(address, telemetry);
        return new GRPCClientManager(address, telemetry);
    }
}

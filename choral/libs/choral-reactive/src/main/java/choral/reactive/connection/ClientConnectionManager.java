package choral.reactive.connection;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

public interface ClientConnectionManager extends AutoCloseable {
    Connection makeConnection() throws IOException, InterruptedException;

    @Override
    void close() throws IOException, InterruptedException;

    public interface Connection extends AutoCloseable {
        void sendMessage(Message msg) throws Exception;

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

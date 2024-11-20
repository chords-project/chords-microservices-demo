package choral.reactive;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;

import io.opentelemetry.sdk.OpenTelemetrySdk;

public interface ClientConnectionManager extends AutoCloseable {
    Connection makeConnection() throws IOException, InterruptedException;

    @Override
    void close() throws IOException;

    public interface Connection extends AutoCloseable {
        void sendMessage(Serializable msg) throws IOException, InterruptedException;

        @Override
        void close() throws IOException;
    }

    public static ClientConnectionManager makeConnectionManager(String address, OpenTelemetrySdk telemetry)
            throws URISyntaxException {

        return new TCPReactiveConnectionManagerSimple(address, telemetry);
    }
}

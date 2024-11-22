package choral.reactive.connection;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.io.IOException;
import java.net.URISyntaxException;

public interface ServerConnectionManager extends AutoCloseable {
    public void listen(String address) throws URISyntaxException, IOException;

    @Override
    public void close() throws IOException;

    public static ServerConnectionManager makeConnectionManager(ServerEvents events, OpenTelemetrySdk telemetry) {
        // return new TCPServerManagerSimple(events, telemetry);
        return new TCPServerManagerNio(events, telemetry);
    }

    public interface ServerEvents {
        public void messageReceived(Object message);
    }
}

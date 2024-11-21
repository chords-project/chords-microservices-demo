package choral.reactive;

import java.io.IOException;
import java.net.URISyntaxException;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public interface ServerConnectionManager extends AutoCloseable {

    public void listen(String address) throws URISyntaxException;

    public static ServerConnectionManager makeConnectionManager(ServerEvents events, OpenTelemetrySdk telemetry) {
        return new TCPReactiveServerManagerSimple(events, telemetry);
    }

    @Override
    public void close() throws IOException;

    public interface ServerEvents {
        public void messageReceived(Object message);
    }
}

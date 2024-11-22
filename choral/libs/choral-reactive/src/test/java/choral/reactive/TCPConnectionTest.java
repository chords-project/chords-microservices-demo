package choral.reactive;

import static org.junit.jupiter.api.Assertions.*;

import choral.reactive.connection.ClientConnectionManager;
import choral.reactive.tracing.TelemetrySession;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class TCPConnectionTest {

    @Test
    void testClientServerConnection() {
        class Stats {

            int newSessionCount = 0;
            int clientSessionID = -1;
            SimpleSession receivedSession = null;
            String firstMsg = null;
            String secondMsg = null;
        }

        Stats stats = new Stats();
        CountDownLatch done = new CountDownLatch(1);

        ReactiveServer<SimpleSession> server = new ReactiveServer<>("server", ctx -> {
            System.out.println("New session: " + ctx.session);
            stats.receivedSession = ctx.session;
            stats.newSessionCount++;

            stats.firstMsg = ctx.chanB("client").com();
            stats.secondMsg = ctx.chanB("client").com();
            done.countDown();
        });

        Thread.ofPlatform()
                .start(() -> {
                    assertDoesNotThrow(() -> {
                        server.listen("0.0.0.0:4567");
                    });
                });

        assertDoesNotThrow(() -> {
            try (ClientConnectionManager connManager = ClientConnectionManager.makeConnectionManager("0.0.0.0:4567",
                    OpenTelemetrySdk.builder().build());) {
                SimpleSession session = SimpleSession.makeSession("choreography", "client");
                ReactiveClient<SimpleSession> client = new ReactiveClient<>(connManager, "client",
                        TelemetrySession.makeNoop(session));

                stats.clientSessionID = session.sessionID;

                var chan = client.chanA(session);
                chan.com("hello");
                chan.com("world");

                // Wait for server to handle messages before closing
                boolean finished = done.await(5, TimeUnit.SECONDS);
                client.close();
                assertTrue(finished);
            } finally {
                server.close();
            }
        });

        assertEquals(1, stats.newSessionCount);
        assertEquals("choreography", stats.receivedSession.choreographyID);
        assertEquals("client", stats.receivedSession.sender);
        assertEquals(stats.clientSessionID, stats.receivedSession.sessionID);

        assertEquals("hello", stats.firstMsg);
        assertEquals("world", stats.secondMsg);
    }
}

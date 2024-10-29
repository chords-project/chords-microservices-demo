package choral.reactive;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import choral.reactive.tracing.TelemetrySession;

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

        TCPReactiveServer<SimpleSession> server = new TCPReactiveServer<>("server", (ctx) -> {
            System.out.println("New session: " + ctx.session);
            stats.receivedSession = ctx.session;
            stats.newSessionCount++;

            stats.firstMsg = ctx.chanB("client").com();
            stats.secondMsg = ctx.chanB("client").com();
            done.countDown();
        });

        new Thread(() -> {
            assertDoesNotThrow(() -> {
                server.listen("0.0.0.0:4567");
            });
        }).start();

        assertDoesNotThrow(() -> {
            SimpleSession session = SimpleSession.makeSession("choreography", "client");
            TCPReactiveClient<SimpleSession> client = new TCPReactiveClient<>("0.0.0.0:4567", "client",
                    TelemetrySession.makeNoop(session));

            stats.clientSessionID = session.sessionID;

            var chan = client.chanA(session);
            chan.com("hello");
            chan.com("world");

            client.close();

            // Wait for server to handle messages before closing
            boolean finished = done.await(5, TimeUnit.SECONDS);
            assertTrue(finished);

            server.close();
        });

        assertEquals(1, stats.newSessionCount);
        assertEquals("choreography", stats.receivedSession.choreographyID);
        assertEquals("client", stats.receivedSession.sender);
        assertEquals(stats.clientSessionID, stats.receivedSession.sessionID);

        assertEquals("hello", stats.firstMsg);
        assertEquals("world", stats.secondMsg);
    }
}

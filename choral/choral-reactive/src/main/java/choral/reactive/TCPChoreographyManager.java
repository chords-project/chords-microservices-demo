package choral.reactive;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.io.Serializable;

import choral.reactive.tracing.TelemetrySession;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class TCPChoreographyManager<C> implements Closeable {

    private OpenTelemetrySdk telemetry;
    private SessionPool<C> sessionPool;

    private ArrayList<ServerConfiguration> servers;

    public TCPChoreographyManager(OpenTelemetrySdk telemetry) {
        this.telemetry = telemetry;
        this.sessionPool = new SessionPool<>();
        this.servers = new ArrayList<>();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("TCPChoreographyManager shutting down gracefully...");
                this.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "SHUTDOWN_HOOK_TCPChoreographyManager"));
    }

    public TCPReactiveServer<C> configureServer(String address, NewSessionEvent<C> sessionEvent) {

        TCPReactiveServer<C> server = new TCPReactiveServer<>(sessionPool, telemetry);
        server.onNewSession((session, telemetrySession) -> {
            System.out.println("New session: " + session);

            try (SessionContext<C> ctx = new SessionContext<>(address, server, session, telemetrySession);) {
                sessionEvent.onNewSession(ctx);
            }
        });

        servers.add(new ServerConfiguration(address, server));

        return server;
    }

    public boolean registerSession(Session<C> session) {
        return this.sessionPool.registerSession(session);
    }

    public void listen() {
        System.out.println("TCPChoreographyManager starting " + servers.size() + " servers");

        Thread[] serverThreads = new Thread[servers.size()];

        for (int i = 0; i < servers.size(); i++) {
            var conf = servers.get(i);
            serverThreads[i] = new Thread(() -> {
                try {
                    conf.server.listen(conf.address);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            });

            serverThreads[i].start();
        }

        try {
            for (var thread : serverThreads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        for (var conf : servers)
            conf.server.close();
    }

    public static class SessionContext<C> implements Closeable {
        private String serverAddress;
        public final TCPReactiveServer<C> server;
        public final Session<C> session;
        private TelemetrySession telemetrySession;

        private ArrayList<Closeable> closeHandles;

        private SessionContext(String serverAddress, TCPReactiveServer<C> server, Session<C> session,
                TelemetrySession telemetrySession) {
            this.serverAddress = serverAddress;
            this.server = server;
            this.session = session;
            this.telemetrySession = telemetrySession;

            this.closeHandles = new ArrayList<>();
        }

        public ReactiveChannel_A<C, Serializable> chanA(String address) throws IOException, URISyntaxException {
            TCPReactiveClient<C> client = new TCPReactiveClient<>(address, telemetrySession);
            closeHandles.add(client);

            return client.chanA(session);
        }

        public ReactiveChannel_B<C, Serializable> selfChanB() {
            return server.chanB(session);
        }

        @Override
        public void close() throws IOException {
            for (var handle : closeHandles)
                handle.close();
        }
    }

    public interface NewSessionEvent<C> {
        void onNewSession(SessionContext<C> ctx) throws Exception;
    }

    private class ServerConfiguration {
        public final String address;
        public final TCPReactiveServer<C> server;

        public ServerConfiguration(String address, TCPReactiveServer<C> server) {
            this.address = address;
            this.server = server;
        }
    }
}

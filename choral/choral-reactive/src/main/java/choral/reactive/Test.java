package choral.reactive;

import java.io.IOException;
import java.net.URISyntaxException;

public class Test {

    public static void main(String[] args) {
        try (TCPReactiveServer<String> server = new TCPReactiveServer<>(new SessionPool<>())) {
            server.onNewSession(sess -> {
                System.out.println("New session: " + sess);
            });

            new Thread(() -> {
                try {
                    server.listen("0.0.0.0:4567");
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }).start();

            try (TCPReactiveClient<String> client1 = new TCPReactiveClient<>("0.0.0.0:4567");) {
                var chan = client1.chanA(Session.makeSession("chor"));
                chan.com("hello");
                chan.com("world");

                Thread.sleep(5000);
                System.out.println("Done");
            } catch (URISyntaxException | IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

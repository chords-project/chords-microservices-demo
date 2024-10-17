// package choral.reactive;

// import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
// import static org.junit.jupiter.api.Assertions.assertEquals;

// import java.net.URISyntaxException;
// import org.junit.jupiter.api.Test;

// public class TCPConnectionTest {

// @Test
// void test() {
// assertEquals(true, false);
// }

// @Test
// void testClientServerConnection() {
// TCPReactiveServer<String> server = new TCPReactiveServer<>(new
// SessionPool<>());

// server.onNewSession(sess -> {
// System.out.println("New session: " + sess);
// });

// new Thread(() -> {
// try {
// server.listen("0.0.0.0:4567");
// } catch (URISyntaxException e) {
// e.printStackTrace();
// }
// }).start();

// assertDoesNotThrow(() -> {
// TCPReactiveClient<String> client1 = new TCPReactiveClient<>("0.0.0.0:4567");

// var chan = client1.chanA(Session.makeSession("chor"));
// chan.com("hello");
// chan.com("world");

// client1.close();
// });
// }
// }

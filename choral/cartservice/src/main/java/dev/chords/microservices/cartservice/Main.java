package dev.chords.microservices.cartservice;

import java.net.InetSocketAddress;

import choral.reactive.Session;
import choral.reactive.TCPReactiveClient;
import choral.reactive.TCPReactiveServer;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Cartservice choral");
        TCPReactiveServer<String> server = new TCPReactiveServer<>((session, cleanup) -> {
            System.out.println("New session received");
        });

        InetSocketAddress address = new InetSocketAddress(5400);

        new Thread(() -> {
            server.listen(address);
        }).start();

        Thread.sleep(1000);
        TCPReactiveClient<String> client = new TCPReactiveClient<>(address);
        client.send(new Session<String>("chor", 1234), "This is the object");
    }
}
package dev.chords.microservices.benchmark;

import java.io.Serializable;
import choral.channels.DiChannel;
import choral.channels.SymChannel;

class SimpleChoreography@(A, B) {
    private SymChannel@(A, B)<Serializable> ch;

    public SimpleChoreography(SymChannel@(A, B)<Serializable> ch) {
        this.ch = ch;
    }

    public void pingPong() {
        System@A.out.println("Sending ping to B..."@A);
        
        String@B ping = ch.<SerializableString>com(new SerializableString@A("Ping"@A)).string;
        System@B.out.println("Received "@B + ping + " from A, sending back pong..."@B);

        String@A pong = ch.<SerializableString>com(new SerializableString@B("Pong"@B)).string;
        System@A.out.println("Received "@A + pong + " from B"@A);
    }
}
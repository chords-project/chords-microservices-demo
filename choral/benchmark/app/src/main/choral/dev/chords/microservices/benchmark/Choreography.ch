package dev.chords.microservices.benchmark;

import java.io.Serializable;
import choral.channels.DiChannel;

class Choreography@(A, B) {
    private DiChannel@(A, B)<Serializable> ch_AB;
    private DiChannel@(B, A)<Serializable> ch_BA;

    public Choreography(DiChannel@(A, B)<Serializable> ch_AB,DiChannel@(B, A)<Serializable> ch_BA) {
        this.ch_AB = ch_AB;
        this.ch_BA = ch_BA;
    }

    public void pingPong() {
        System@A.out.println("Sending ping to B..."@A);
        
        String@B ping = ch_AB.<SerializableString>com(new SerializableString@A("Ping"@A)).string;
        System@B.out.println("Received "@B + ping + " from A, sending back pong..."@B);

        String@A pong = ch_BA.<SerializableString>com(new SerializableString@B("Pong"@B)).string;
        System@A.out.println("Received "@A + pong + " from B"@A);
    }
}
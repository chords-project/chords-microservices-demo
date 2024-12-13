package dev.chords.microservices.benchmark;

import java.io.Serializable;
//import choral.channels.DiChannel;
//import choral.channels.SymChannel;
import choral.channels.AsyncDiChannel;
import choral.channels.AsyncSymChannel;

class GreeterChoreography@(A, B) {
    private AsyncSymChannel@(A, B)<Serializable> ch;
    private GreeterService@B greeter;

    public GreeterChoreography(AsyncSymChannel@(A, B)<Serializable> ch, GreeterService@B greeter) {
        this.ch = ch;
        this.greeter = greeter;
    }

    public void greet() {
        System@A.out.println("Sending name to B..."@A);

        String@B name = ch.<SerializableString>fcom(new SerializableString@A("Alfred"@A)).get().string;
        System@B.out.println("Received "@B + name + " from A, sending back greeting..."@B);

        String@B greeting = greeter.greet(name);

        String@A reply = ch.<SerializableString>fcom(new SerializableString@B(greeting)).get().string;
        System@A.out.println("Received "@A + reply + " from B"@A);
    }
}

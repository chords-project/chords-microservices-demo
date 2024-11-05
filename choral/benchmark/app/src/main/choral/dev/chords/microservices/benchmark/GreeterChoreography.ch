package dev.chords.microservices.benchmark;

import java.io.Serializable;
import choral.channels.DiChannel;
import choral.channels.SymChannel;

class GreeterChoreography@(A, B) {
    private SymChannel@(A, B)<Serializable> ch;
    private GreeterService@B greeter;

    public GreeterChoreography(SymChannel@(A, B)<Serializable> ch, GreeterService@B greeter) {
        this.ch = ch;
        this.greeter = greeter;
    }

    public void greet() {
        System@A.out.println("Sending name to B..."@A);
        
        String@B name = ch.<SerializableString>com(new SerializableString@A("Alfred"@A)).string;
        System@B.out.println("Received "@B + name + " from A, sending back greeting..."@B);

        String@B greeting = greeter.greet(name);

        String@A reply = ch.<SerializableString>com(new SerializableString@B(greeting)).string;
        System@A.out.println("Received "@A + reply + " from B"@A);
    }
}
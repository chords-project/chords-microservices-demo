package dev.chords.microservices.benchmark;

import choral.channels.DiChannel_B;
import choral.lang.Unit;
import choral.channels.DiChannel_A;

class Choreography_B {
	private DiChannel_B < String > ch_AB;
	private DiChannel_A < String > ch_BA;

	public void pingPong() {
		String ping = ch_AB.< String >com( Unit.id );
		System.out.println( "Received ping from A, sending back pong..." );
		ch_BA.< String >com( "Pong" );
	}

}

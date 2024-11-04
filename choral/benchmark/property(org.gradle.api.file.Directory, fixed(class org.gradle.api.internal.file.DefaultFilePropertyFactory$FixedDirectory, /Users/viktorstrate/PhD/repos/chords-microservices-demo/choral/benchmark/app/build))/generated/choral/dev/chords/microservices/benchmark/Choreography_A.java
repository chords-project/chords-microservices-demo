package dev.chords.microservices.benchmark;

import choral.channels.DiChannel_A;
import choral.lang.Unit;
import choral.channels.DiChannel_B;

class Choreography_A {
	private DiChannel_A < String > ch_AB;
	private DiChannel_B < String > ch_BA;

	public void pingPong() {
		System.out.println( "Sending ping to B..." );
		ch_AB.< String >com( "Ping" );
		String pong = ch_BA.< String >com( Unit.id );
		System.out.println( "Received pong from B" );
	}

}

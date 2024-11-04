package dev.chords.microservices.benchmark;

import choral.channels.DiChannel_A;
import choral.lang.Unit;
import choral.channels.DiChannel_B;

class Choreography_Client {
	private DiChannel_A < String > ch_clientServer;
	private DiChannel_B < String > ch_serverClient;

	public void pingPong() {
		System.out.println( "Sending ping to server..." );
		ch_clientServer.< String >com( "Ping" );
		String pong = ch_serverClient.< String >com( Unit.id );
		System.out.println( "Received pong from server" );
	}

}

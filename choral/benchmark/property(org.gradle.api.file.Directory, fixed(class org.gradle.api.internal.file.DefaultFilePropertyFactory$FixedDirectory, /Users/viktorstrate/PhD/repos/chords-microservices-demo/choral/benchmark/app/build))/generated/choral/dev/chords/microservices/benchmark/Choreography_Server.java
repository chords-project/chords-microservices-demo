package dev.chords.microservices.benchmark;

import choral.channels.DiChannel_B;
import choral.lang.Unit;
import choral.channels.DiChannel_A;

class Choreography_Server {
	private DiChannel_B < String > ch_clientServer;
	private DiChannel_A < String > ch_serverClient;

	public void pingPong() {
		String ping = ch_clientServer.< String >com( Unit.id );
		System.out.println( "Received ping from client, sending back pong..." );
		ch_serverClient.< String >com( "Pong" );
	}

}

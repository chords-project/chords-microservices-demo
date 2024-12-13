package choral.channels;

import choral.lang.Unit;

public interface AsyncDiChannel_B< T > {
	< S extends T > Future< S > fcom(Unit m );
	< S extends T > Future< S > fcom();
}

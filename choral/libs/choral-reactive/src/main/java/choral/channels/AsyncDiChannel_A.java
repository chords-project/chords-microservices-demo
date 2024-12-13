package choral.channels;

import choral.lang.Unit;

public interface AsyncDiChannel_A< T > {
	< S extends T > Unit fcom( S m );
}

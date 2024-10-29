package choral.reactive.tracing;

import choral.reactive.TCPMessage;
import io.opentelemetry.context.propagation.TextMapSetter;

public class HeaderTextMapSetter implements TextMapSetter<TCPMessage<?>> {

    @SuppressWarnings("null")
    @Override
    public void set(TCPMessage<?> carrier, String key, String value) {
        carrier.headers.put(key, value);
    }

}

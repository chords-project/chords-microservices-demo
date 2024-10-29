package choral.reactive.tracing;

import choral.reactive.TCPMessage;
import io.opentelemetry.context.propagation.TextMapGetter;

@SuppressWarnings("null")
public class HeaderTextMapGetter implements TextMapGetter<TCPMessage<?>> {

    @Override
    public Iterable<String> keys(TCPMessage<?> carrier) {
        return carrier.headers.keySet();
    }

    @Override
    public String get(TCPMessage<?> carrier, String key) {
        return carrier.headers.get(key);
    }

}

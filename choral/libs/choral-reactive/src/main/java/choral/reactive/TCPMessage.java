package choral.reactive;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.TraceStateBuilder;
import java.io.Serializable;
import java.util.HashMap;

public class TCPMessage<S extends Session> implements Serializable {

    public final S session;
    public final Serializable message;

    public final HashMap<String, String> headers;
    public SerializedSpanContext senderSpanContext;

    public TCPMessage(S session, Serializable message) {
        this.session = session;
        this.message = message;
        this.headers = new HashMap<>();
    }

    public static class SerializedSpanContext implements Serializable {

        String traceIdHex;
        String spanIdHex;
        Byte traceFlags;
        HashMap<String, String> traceState;

        public SerializedSpanContext(SpanContext spanContext) {
            this.traceIdHex = spanContext.getTraceId();
            this.spanIdHex = spanContext.getSpanId();
            this.traceFlags = spanContext.getTraceFlags().asByte();
            this.traceState = new HashMap<>(spanContext.getTraceState().asMap());
        }

        public SpanContext toSpanContext() {
            TraceStateBuilder stateBuilder = TraceState.builder();
            traceState.forEach((key, value) -> stateBuilder.put(key, value));
            return SpanContext.createFromRemoteParent(traceIdHex, spanIdHex, TraceFlags.fromByte(traceFlags), stateBuilder.build());
        }
    }

    @Override
    public String toString() {
        return "TCPMessage [" + this.session + ", " + this.message + "]";
    }
}

package choral.reactive;

import java.util.HashMap;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.TraceStateBuilder;

import java.io.Serializable;

public class TCPMessage<C> implements Serializable {
    public final Session<C> session;
    public final Serializable message;

    public final HashMap<String, String> headers;
    public SerializedSpanContext senderSpanContext;

    public TCPMessage(Session<C> session, Serializable message) {
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
            return SpanContext.createFromRemoteParent(
                    traceIdHex,
                    spanIdHex,
                    TraceFlags.fromByte(traceFlags),
                    stateBuilder.build());
        }
    }
}

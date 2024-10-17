package choral.reactive.tracing;

import choral.reactive.Session;
import choral.reactive.TCPMessage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class TelemetrySession {

    private OpenTelemetrySdk telemetry;
    private Tracer tracer;

    private Session<?> session;
    private Context sessionContext;

    public TelemetrySession(OpenTelemetrySdk telemetry, TCPMessage<?> msg) {
        this.telemetry = telemetry;
        this.session = msg.session;
        this.tracer = this.telemetry.getTracer(JaegerConfiguration.TRACER_NAME);

        this.sessionContext = telemetry.getPropagators().getTextMapPropagator().extract(Context.current(), msg,
                new HeaderTextMapGetter());
    }

    public TelemetrySession(OpenTelemetrySdk telemetry, Session<?> session, Context sessionContext) {
        this.telemetry = telemetry;
        this.session = session;
        this.tracer = this.telemetry.getTracer(JaegerConfiguration.TRACER_NAME);

        this.sessionContext = sessionContext;
    }

    public Span startSpan(String name) {
        return tracer.spanBuilder(name)
                .setParent(sessionContext)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("choreography.session", session.toString())
                .startSpan();
    }

    public void injectSessionContext(TCPMessage<?> msg) {
        telemetry.getPropagators().getTextMapPropagator().inject(sessionContext, msg, new HeaderTextMapSetter());
    }
}

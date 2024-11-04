package choral.reactive.tracing;

import choral.reactive.Session;
import choral.reactive.TCPMessage;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class TelemetrySession {

    private OpenTelemetrySdk telemetry;
    public final Tracer tracer;

    private Session session;

    private Span choreographySpan = null;

    private Context choreographyContext;
    private SpanContext senderLinkContext;

    public static TelemetrySession makeNoop(Session session) {
        return new TelemetrySession(OpenTelemetrySdk.builder().build(), session, Span.getInvalid());
    }

    public TelemetrySession(OpenTelemetrySdk telemetry, TCPMessage<?> msg) {
        this.telemetry = telemetry;
        this.session = msg.session;
        this.tracer = this.telemetry.getTracer(JaegerConfiguration.TRACER_NAME);

        this.senderLinkContext = msg.senderSpanContext.toSpanContext();
        this.choreographyContext = telemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), msg, new HeaderTextMapGetter());
    }

    // Configure initial telemetry session
    public TelemetrySession(OpenTelemetrySdk telemetry, Session session, Span span) {
        this.telemetry = telemetry;
        this.session = session;
        this.tracer = this.telemetry.getTracer(JaegerConfiguration.TRACER_NAME);
        this.senderLinkContext = null;
        this.choreographyContext = Context.current().with(span);
        this.choreographySpan = span;
    }

    // Configure dummy telemetry session
    public TelemetrySession(Session session) {
        this(OpenTelemetrySdk.builder().build(), session, Span.getInvalid());
    }

    public Span makeChoreographySpan() {
        if (this.choreographySpan != null)
            throw new RuntimeException("TelemetrySession::makeChoreographySpan may only be called once");

        this.choreographySpan = tracer.spanBuilder("choreography session")
                .setParent(choreographyContext)
                .addLink(senderLinkContext)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("choreography.session", session.toString())
                .startSpan();

        return this.choreographySpan;
    }

    public void log(String message) {
        choreographySpan.addEvent(message);
    }

    public void log(String message, Attributes attributes) {
        choreographySpan.addEvent(message, attributes);
    }

    public void injectSessionContext(TCPMessage<?> msg) {
        telemetry.getPropagators()
                .getTextMapPropagator()
                .inject(choreographyContext, msg, new HeaderTextMapSetter());

        msg.senderSpanContext = new TCPMessage.SerializedSpanContext(choreographySpan.getSpanContext());
    }

    // @Override
    // public void close() throws IOException {
    // choreographySpan.end();
    // }
}

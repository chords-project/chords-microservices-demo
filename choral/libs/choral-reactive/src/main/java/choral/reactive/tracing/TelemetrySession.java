package choral.reactive.tracing;

import choral.reactive.Session;
import choral.reactive.connection.Message;
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

    public TelemetrySession(OpenTelemetrySdk telemetry, Message msg) {
        this.telemetry = telemetry;
        this.session = msg.session;
        this.tracer = this.telemetry.getTracer(JaegerConfiguration.TRACER_NAME);

        this.senderLinkContext = msg.senderSpanContext.toSpanContext();
        this.choreographyContext = telemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.root(), msg, new HeaderTextMapGetter());
    }

    // Configure initial telemetry session
    public TelemetrySession(OpenTelemetrySdk telemetry, Session session, Span span) {
        this.telemetry = telemetry;
        this.session = session;
        this.tracer = this.telemetry.getTracer(JaegerConfiguration.TRACER_NAME);
        this.senderLinkContext = null;
        this.choreographyContext = Context.root().with(span);
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
        this.log(message, Attributes.empty());
    }

    public void log(String message, Attributes attributes) {
        Attributes extraAttributes = Attributes.builder().put("session", session.toString()).putAll(attributes).build();

        //System.out.println(message + ": " + attributesToString(extraAttributes));
        choreographySpan.addEvent(message, extraAttributes);
    }

    public void recordException(String message, Exception e, boolean error, Attributes attributes) {
        Attributes extraAttributes = Attributes.builder()
                .put("session", session.toString()).put("message", message).putAll(attributes).build();

        if (error)
            choreographySpan.setAttribute("error", true);
        choreographySpan.recordException(e, extraAttributes);

        System.out.println(message + ": " + attributesToString(extraAttributes));
        e.printStackTrace();
    }

    public void recordException(String message, Exception e, boolean error) {
        this.recordException(message, e, error, Attributes.empty());
    }

    public void injectSessionContext(Message msg) {
        telemetry.getPropagators()
                .getTextMapPropagator()
                .inject(choreographyContext, msg, new HeaderTextMapSetter());

        msg.senderSpanContext = new Message.SerializedSpanContext(choreographySpan.getSpanContext());
    }

    private String attributesToString(Attributes attributes) {
        return String.join(", ",
                attributes.asMap().entrySet()
                        .stream()
                        .map(entry -> entry.getKey().toString() + "=" + entry.getValue().toString())
                        .toList());
    }

    // @Override
    // public void close() throws IOException {
    // choreographySpan.end();
    // }
}

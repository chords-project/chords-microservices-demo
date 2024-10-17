package choral.reactive.tracing;

import choral.reactive.Session;
import choral.reactive.TCPMessage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public class TelemetrySession {

    private OpenTelemetrySdk telemetry;
    public final Tracer tracer;

    private Session<?> session;

    public final Span choreographySpan;

    private Context choreographyContext;
    private SpanContext senderLinkContext;

    public TelemetrySession(OpenTelemetrySdk telemetry, TCPMessage<?> msg) {
        this.telemetry = telemetry;
        this.session = msg.session;
        this.tracer = this.telemetry.getTracer(JaegerConfiguration.TRACER_NAME);

        this.senderLinkContext = msg.senderSpanContext.toSpanContext();
        this.choreographyContext = telemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), msg, new HeaderTextMapGetter());

        choreographySpan = makeChoreographySpan();
    }

    public TelemetrySession(OpenTelemetrySdk telemetry, Session<?> session, Context sessionContext,
            SpanContext senderSpanContext) {
        this.telemetry = telemetry;
        this.session = session;
        this.tracer = this.telemetry.getTracer(JaegerConfiguration.TRACER_NAME);
        this.senderLinkContext = senderSpanContext;
        this.choreographyContext = sessionContext;

        this.choreographySpan = makeChoreographySpan();
    }

    private Span makeChoreographySpan() {
        return tracer.spanBuilder("choreography session")
                .setParent(choreographyContext)
                .addLink(senderLinkContext)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("choreography.session", session.toString())
                .startSpan();
    }

    public void injectSessionContext(TCPMessage<?> msg) {
        telemetry.getPropagators()
                .getTextMapPropagator()
                .inject(choreographyContext, msg, new HeaderTextMapSetter());

        msg.senderSpanContext = new TCPMessage.SerializedSpanContext(choreographySpan.getSpanContext());
    }
}

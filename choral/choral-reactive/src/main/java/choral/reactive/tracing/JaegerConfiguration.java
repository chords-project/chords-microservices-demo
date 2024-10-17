package choral.reactive.tracing;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ServiceAttributes;
import java.util.concurrent.TimeUnit;

public class JaegerConfiguration {

        public static String TRACER_NAME = "choral.reactive.Choreography";

        public static OpenTelemetrySdk initTelemetry(String jaegerEndpoint, String serviceName) {
                // Export traces to Jaeger over OTLP
                OtlpGrpcSpanExporter jaegerOtlpExporter = OtlpGrpcSpanExporter.builder()
                                .setEndpoint(jaegerEndpoint)
                                .setTimeout(30, TimeUnit.SECONDS)
                                .build();

                Resource serviceNameResource = Resource
                                .create(Attributes.of(ServiceAttributes.SERVICE_NAME,
                                                serviceName));

                // Set to process the spans by the Jaeger Exporter
                SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                                .addSpanProcessor(BatchSpanProcessor.builder(jaegerOtlpExporter).build())
                                .setResource(Resource.getDefault().merge(serviceNameResource))
                                .build();

                OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                                .setTracerProvider(tracerProvider)
                                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                                .build();

                // it's always a good idea to shut down the SDK cleanly at JVM exit.
                Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::close));

                return openTelemetry;
        }

        // public static Tracer initTracer(String jaegerEndpoint) {
        // return
        // initTelemetry(jaegerEndpoint).getTracer("choral.reactive.Choreography");
        // }
}

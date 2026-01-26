package com.neurogate.config;

import io.micrometer.tracing.Tracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ResourceAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry configuration for distributed tracing.
 *
 * Exports traces to OTLP-compatible backends like Jaeger, Zipkin, or Grafana Tempo.
 *
 * Environment variables:
 * - OTEL_EXPORTER_OTLP_ENDPOINT: OTLP endpoint URL (default: http://localhost:4317)
 * - OTEL_SERVICE_NAME: Service name (default: neurogate)
 * - OTEL_TRACES_SAMPLER: Sampler type (default: always_on)
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "management.tracing.enabled", havingValue = "true", matchIfMissing = true)
public class OpenTelemetryConfig {

    @Value("${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4317}")
    private String otlpEndpoint;

    @Value("${OTEL_SERVICE_NAME:neurogate}")
    private String serviceName;

    @Value("${OTEL_TRACES_SAMPLER_ARG:1.0}")
    private double samplingProbability;

    /**
     * Configure OTLP span exporter for gRPC protocol.
     */
    @Bean
    public OtlpGrpcSpanExporter otlpGrpcSpanExporter() {
        log.info("Configuring OTLP span exporter with endpoint: {}", otlpEndpoint);
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint(otlpEndpoint)
                .build();
    }

    /**
     * Configure the resource with service information.
     */
    @Bean
    public Resource otelResource() {
        return Resource.getDefault()
                .merge(Resource.create(Attributes.builder()
                        .put(ResourceAttributes.SERVICE_NAME, serviceName)
                        .put(ResourceAttributes.SERVICE_VERSION, "1.0.0")
                        .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT,
                             System.getenv().getOrDefault("ENVIRONMENT", "development"))
                        .build()));
    }

    /**
     * Configure the tracer provider with batch processing.
     */
    @Bean
    public SdkTracerProvider sdkTracerProvider(OtlpGrpcSpanExporter spanExporter, Resource resource) {
        return SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter)
                        .setMaxQueueSize(2048)
                        .setMaxExportBatchSize(512)
                        .build())
                .setSampler(Sampler.traceIdRatioBased(samplingProbability))
                .build();
    }

    /**
     * Configure the OpenTelemetry SDK with W3C trace context propagation.
     */
    @Bean
    public OpenTelemetry openTelemetry(SdkTracerProvider tracerProvider) {
        log.info("Initializing OpenTelemetry with service name: {}", serviceName);
        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
    }

    /**
     * Get the OpenTelemetry tracer for manual instrumentation.
     */
    @Bean
    public io.opentelemetry.api.trace.Tracer otelTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("com.neurogate", "1.0.0");
    }
}
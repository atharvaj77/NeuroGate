package com.neurogate.metrics;

import com.neurogate.config.PricingConfig;
import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom metrics for NeuroGate
 *
 * Tracks:
 * - Cache hit/miss ratio
 * - Cost savings
 * - Request latency
 * - Routing decisions
 * - PII detections (future)
 */
@Slf4j
@Component
public class NeuroGateMetrics {

    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter openAiRequests;
    private final Counter localModelRequests;
    private final Timer requestLatency;
    private final AtomicLong totalCostSaved;
    private final Gauge costSavingsGauge;

    private final MeterRegistry registry;
    private final PricingConfig pricingConfig;

    // PII metrics
    private final Counter piiDetections;
    private final Counter piiEmailDetections;
    private final Counter piiSsnDetections;
    private final Counter piiPhoneDetections;
    private final Counter piiCreditCardDetections;

    public NeuroGateMetrics(MeterRegistry registry, PricingConfig pricingConfig) {
        this.registry = registry;
        this.pricingConfig = pricingConfig;

        // Cache metrics
        this.cacheHits = Counter.builder("neurogate.cache.hits")
                .description("Number of semantic cache hits")
                .tag("type", "cache")
                .register(registry);

        this.cacheMisses = Counter.builder("neurogate.cache.misses")
                .description("Number of semantic cache misses")
                .tag("type", "cache")
                .register(registry);

        // Routing metrics
        this.openAiRequests = Counter.builder("neurogate.route.openai")
                .description("Number of requests routed to OpenAI")
                .tag("destination", "openai")
                .register(registry);

        this.localModelRequests = Counter.builder("neurogate.route.local")
                .description("Number of requests routed to local model")
                .tag("destination", "local")
                .register(registry);

        // Latency metrics
        this.requestLatency = Timer.builder("neurogate.request.latency")
                .description("Request processing latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        // Cost savings
        this.totalCostSaved = new AtomicLong(0);
        this.costSavingsGauge = Gauge.builder("neurogate.cost.savings.total", totalCostSaved, AtomicLong::get)
                .description("Total cost saved in USD (multiplied by 1000 for precision)")
                .tag("currency", "usd_millis")
                .register(registry);

        // PII metrics
        this.piiDetections = Counter.builder("neurogate.pii.detections")
                .description("Total number of PII entities detected")
                .tag("type", "all")
                .register(registry);

        this.piiEmailDetections = Counter.builder("neurogate.pii.email")
                .description("Number of email addresses detected")
                .tag("pii_type", "email")
                .register(registry);

        this.piiSsnDetections = Counter.builder("neurogate.pii.ssn")
                .description("Number of SSNs detected")
                .tag("pii_type", "ssn")
                .register(registry);

        this.piiPhoneDetections = Counter.builder("neurogate.pii.phone")
                .description("Number of phone numbers detected")
                .tag("pii_type", "phone")
                .register(registry);

        this.piiCreditCardDetections = Counter.builder("neurogate.pii.credit_card")
                .description("Number of credit cards detected")
                .tag("pii_type", "credit_card")
                .register(registry);
    }

    /**
     * Record a cache hit
     */
    public void recordCacheHit() {
        cacheHits.increment();
        log.debug("Cache hit recorded");

        // Estimate cost saved based on OpenAI pricing as baseline
        // Assuming average 500 tokens for simplicity of metric if exact token count
        // unknown
        // For accurate tracking, use AnalyticsService
        double baselineCost = pricingConfig.getProviderCost("openai", "gpt-3.5-turbo", 500);
        addCostSaved(baselineCost);
    }

    /**
     * Record a cache miss
     */
    public void recordCacheMiss() {
        cacheMisses.increment();
        log.debug("Cache miss recorded");
    }

    /**
     * Record a request routed to OpenAI
     */
    public void recordOpenAiRequest() {
        openAiRequests.increment();
        log.debug("OpenAI request recorded");
    }

    /**
     * Record a request routed to local model
     */
    public void recordLocalRequest() {
        localModelRequests.increment();
        log.debug("Local model request recorded");

        // Savings = (OpenAI Cost) - (Local Cost ~ 0)
        double baselineCost = pricingConfig.getProviderCost("openai", "gpt-3.5-turbo", 500);
        addCostSaved(baselineCost);
    }

    /**
     * Record request latency
     *
     * @param durationMs Duration in milliseconds
     */
    public void recordLatency(long durationMs) {
        requestLatency.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        log.debug("Latency recorded: {}ms", durationMs);
    }

    /**
     * Add to total cost saved
     *
     * @param amount Amount in USD
     */
    private void addCostSaved(double amount) {
        // Store as millis (multiply by 1000) to avoid floating point in atomic
        long millis = (long) (amount * 1000);
        totalCostSaved.addAndGet(millis);
    }

    /**
     * Get total cost saved in USD
     */
    public double getTotalCostSaved() {
        return totalCostSaved.get() / 1000.0;
    }

    /**
     * Get cache hit ratio
     */
    public double getCacheHitRatio() {
        double hits = cacheHits.count();
        double total = hits + cacheMisses.count();
        return total > 0 ? hits / total : 0.0;
    }

    /**
     * Get total requests
     */
    public long getTotalRequests() {
        return (long) (cacheHits.count() + cacheMisses.count());
    }

    /**
     * Record PII detection
     */
    public void recordPiiDetection(String piiType) {
        piiDetections.increment();

        switch (piiType.toUpperCase()) {
            case "EMAIL":
                piiEmailDetections.increment();
                break;
            case "SSN":
                piiSsnDetections.increment();
                break;
            case "PHONE":
                piiPhoneDetections.increment();
                break;
            case "CREDIT_CARD":
                piiCreditCardDetections.increment();
                break;
        }

        log.debug("PII detected: {}", piiType);
    }

    /**
     * Get total PII detections
     */
    public long getTotalPiiDetections() {
        return (long) piiDetections.count();
    }

    /**
     * Phase 4: Record provider request
     */
    public void recordProviderRequest(String providerName) {
        registry.counter("neurogate.provider.requests", "provider", providerName).increment();
        log.debug("Provider request recorded: {}", providerName);
    }

    /**
     * Phase 4: Record provider failure
     */
    public void recordProviderFailure(String providerName) {
        registry.counter("neurogate.provider.failures", "provider", providerName).increment();
        log.warn("Provider failure recorded: {}", providerName);
    }

    /**
     * Phase 5: Record canary routing event
     */
    public void recordCanaryRoute() {
        registry.counter("neurogate.route.canary", "type", "canary").increment();
        log.debug("Canary route recorded");
    }

    /**
     * Print metrics summary (useful for debugging)
     */
    public void printSummary() {
        log.info("=== NeuroGate Metrics Summary ===");
        log.info("Total Requests: {}", getTotalRequests());
        log.info("Cache Hits: {}", (long) cacheHits.count());
        log.info("Cache Misses: {}", (long) cacheMisses.count());
        log.info("Cache Hit Ratio: {:.2f}%", getCacheHitRatio() * 100);
        log.info("OpenAI Requests: {}", (long) openAiRequests.count());
        log.info("Local Model Requests: {}", (long) localModelRequests.count());
        log.info("Total Cost Saved: ${:.4f}", getTotalCostSaved());
        log.info("PII Detections: {} (Email: {}, SSN: {}, Phone: {}, CC: {})",
                getTotalPiiDetections(),
                (long) piiEmailDetections.count(),
                (long) piiSsnDetections.count(),
                (long) piiPhoneDetections.count(),
                (long) piiCreditCardDetections.count());
        log.info("================================");
    }
}

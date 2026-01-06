package com.neurogate.config.health;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.CollectionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Custom health indicator checking critical dependencies (Qdrant, Cache).
 */
@Slf4j
@Component
public class NeuroGateHealthIndicator implements HealthIndicator {

    private final QdrantClient qdrantClient;

    public NeuroGateHealthIndicator(
            @org.springframework.beans.factory.annotation.Autowired(required = false) QdrantClient qdrantClient) {
        this.qdrantClient = qdrantClient;
    }

    @Override
    public Health health() {
        try {
            // Check Qdrant connectivity
            boolean qdrantHealthy = checkQdrantHealth();

            if (qdrantHealthy) {
                return Health.up()
                        .withDetail("qdrant", "Connected")
                        .withDetail("cache", "Available")
                        .withDetail("status", "All systems operational")
                        .build();
            } else {
                // Qdrant unavailable but app can still function (cache disabled)
                return Health.up()
                        .withDetail("qdrant", qdrantClient == null ? "Disabled" : "Unavailable")
                        .withDetail("cache", "Degraded")
                        .withDetail("status", "Running with degraded caching")
                        .build();
            }
        } catch (Exception e) {
            log.error("Health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }

    private boolean checkQdrantHealth() {
        if (qdrantClient == null) {
            return false;
        }
        try {
            // Simple health check - try to list collections
            var collections = qdrantClient.listCollectionsAsync().get(2, TimeUnit.SECONDS);
            return collections != null;
        } catch (Exception e) {
            log.debug("Qdrant health check failed: {}", e.getMessage());
            return false;
        }
    }
}

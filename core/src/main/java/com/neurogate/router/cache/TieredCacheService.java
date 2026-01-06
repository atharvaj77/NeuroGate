package com.neurogate.router.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.neurogate.metrics.NeuroGateMetrics;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 4-Tier Caching Hierarchy
 *
 * L1: Caffeine (JVM In-Memory)
 * L2: Redis (Network)
 * L3: Qdrant (Semantic Vector Search)
 * L4: S3 (Cold Storage)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TieredCacheService {

    // L1: Caffeine Cache (JVM In-Memory)
    private final Cache<String, ChatResponse> l1Cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats()
            .build();

    // L2: Redis Cache (Network)
    private final RedisTemplate<String, String> redisTemplate;

    // L3: Qdrant (Semantic Vector Search)
    private final Optional<SemanticCacheService> semanticCacheService;

    // L4: S3 (Cold Storage)
    private final S3CacheService s3CacheService;

    // Utilities
    private final ObjectMapper objectMapper;
    private final NeuroGateMetrics metrics;
    private final EmbeddingService embeddingService;

    /**
     * Get cached response from tiered cache hierarchy
     *
     * Checks L1 -> L2 -> L3 in order
     * Promotes cache hits to upper tiers
     */
    public Optional<ChatResponse> get(ChatRequest request) {
        String cacheKey = generateCacheKey(request);
        String promptText = request.getConcatenatedContent();

        // L1: Check Caffeine (fastest)
        ChatResponse l1Hit = l1Cache.getIfPresent(cacheKey);
        if (l1Hit != null) {
            log.debug("L1 Cache HIT (Caffeine) - {}ms latency", "<1");
            recordCacheHit("L1", "<1ms");
            return Optional.of(l1Hit);
        }

        // L2: Check Redis
        try {
            String l2Value = redisTemplate.opsForValue().get(cacheKey);
            if (l2Value != null) {
                log.debug("L2 Cache HIT (Redis) - promoting to L1");
                ChatResponse l2Hit = objectMapper.readValue(l2Value, ChatResponse.class);

                // Promote to L1
                l1Cache.put(cacheKey, l2Hit);

                recordCacheHit("L2", "<5ms");
                return Optional.of(l2Hit);
            }
        } catch (Exception e) {
            log.warn("L2 Redis cache read failed: {}", e.getMessage());
        }

        // L3: Check Qdrant (semantic search)
        if (semanticCacheService.isPresent()) {
            Optional<ChatResponse> l3Hit = semanticCacheService.get().get(request);
            if (l3Hit.isPresent()) {
                log.debug("L3 Cache HIT (Qdrant) - promoting to L2 and L1");
                ChatResponse response = l3Hit.get();

                // Promote to L2 and L1
                promoteToUpperTiers(cacheKey, response);

                recordCacheHit("L3", "<20ms");
                return Optional.of(response);
            }
        }

        // L4: S3 cold storage (if enabled)
        if (s3CacheService.isEnabled()) {
            Optional<ChatResponse> l4Hit = s3CacheService.get(cacheKey);
            if (l4Hit.isPresent()) {
                log.debug("L4 Cache HIT (S3) - promoting to L3, L2, and L1");
                ChatResponse response = l4Hit.get();

                // Promote to all upper tiers
                promoteToUpperTiers(cacheKey, response);
                semanticCacheService.ifPresent(s -> s.put(request, response)); // Also promote to L3

                recordCacheHit("L4", "<100ms");
                return Optional.of(response);
            }
        }

        log.debug("Cache MISS on all tiers (L1, L2, L3, L4)");
        return Optional.empty();
    }

    /**
     * Store response in all cache tiers
     */
    public void put(ChatRequest request, ChatResponse response) {
        String cacheKey = generateCacheKey(request);

        // Store in L1 (Caffeine)
        l1Cache.put(cacheKey, response);
        log.debug("Stored in L1 cache (Caffeine)");

        // Store in L2 (Redis)
        try {
            String jsonValue = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, Duration.ofHours(24));
            log.debug("Stored in L2 cache (Redis) with 24h TTL");
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize response for L2 cache: {}", e.getMessage());
        }

        // Store in L3 (Qdrant semantic)
        semanticCacheService.ifPresent(s -> s.put(request, response));
        log.debug("Stored in L3 cache (Qdrant)");

        // L4: S3 archival (if enabled)
        if (s3CacheService.isEnabled()) {
            s3CacheService.put(cacheKey, response);
            log.debug("Stored in L4 cache (S3 cold storage)");
        }
    }

    /**
     * Promote cache hit to upper tiers
     */
    private void promoteToUpperTiers(String cacheKey, ChatResponse response) {
        // Promote to L1
        l1Cache.put(cacheKey, response);

        // Promote to L2
        try {
            String jsonValue = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, Duration.ofHours(24));
        } catch (JsonProcessingException e) {
            log.warn("Failed to promote to L2 cache: {}", e.getMessage());
        }
    }

    /**
     * Generate cache key from request
     */
    private String generateCacheKey(ChatRequest request) {
        try {
            String content = request.getConcatenatedContent();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }

            return "neurogate:cache:" + hexString.toString().substring(0, 32);
        } catch (Exception e) {
            log.error("Failed to generate cache key", e);
            return "neurogate:cache:error";
        }
    }

    /**
     * Record cache hit metrics
     */
    private void recordCacheHit(String tier, String latency) {
        log.info("CACHE HIT - Tier: {}, Latency: {}", tier, latency);
        metrics.recordCacheHit();
    }

    /**
     * Get cache statistics
     */
    public CacheStats getStats() {
        return CacheStats.builder()
                .l1Size(l1Cache.estimatedSize())
                .l1HitRate(l1Cache.stats().hitRate())
                .l1MissRate(l1Cache.stats().missRate())
                .build();
    }

    /**
     * Cache statistics model
     */
    @lombok.Builder
    @lombok.Data
    public static class CacheStats {
        private long l1Size;
        private double l1HitRate;
        private double l1MissRate;
    }

    /**
     * Invalidate all caches (admin operation)
     */
    public void invalidateAll() {
        l1Cache.invalidateAll();
        log.info("Invalidated all L1 cache entries");

        // Note: L2 and L3 will expire naturally based on TTL
    }
}

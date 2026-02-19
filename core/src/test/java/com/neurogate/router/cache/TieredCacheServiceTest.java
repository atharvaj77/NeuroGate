package com.neurogate.router.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.metrics.NeuroGateMetrics;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TieredCacheServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SemanticCacheService semanticCacheService;

    @Mock
    private S3CacheService s3CacheService;

    @Mock
    private NeuroGateMetrics metrics;

    @Mock
    private EmbeddingService embeddingService;

    private TieredCacheService tieredCacheService;

    @BeforeEach
    void setUp() {
        tieredCacheService = new TieredCacheService(
                redisTemplate,
                Optional.of(semanticCacheService),
                s3CacheService,
                new ObjectMapper(),
                metrics,
                embeddingService);
    }

    @Test
    void generateCacheKey_shouldIncludeModelAndTemperatureAndMaxTokens() {
        ChatRequest request1 = ChatRequest.builder()
                .model("gpt-4")
                .temperature(0.1)
                .maxTokens(256)
                .messages(List.of(Message.builder().role("user").content("same prompt").build()))
                .build();
        ChatRequest request2 = ChatRequest.builder()
                .model("gpt-3.5-turbo")
                .temperature(0.9)
                .maxTokens(1024)
                .messages(List.of(Message.builder().role("user").content("same prompt").build()))
                .build();

        String key1 = ReflectionTestUtils.invokeMethod(tieredCacheService, "generateCacheKey", request1);
        String key2 = ReflectionTestUtils.invokeMethod(tieredCacheService, "generateCacheKey", request2);

        assertNotNull(key1);
        assertNotNull(key2);
        assertNotEquals(key1, key2);
    }

    @Test
    void invalidateAll_shouldClearRedisAndSemanticCache() {
        when(redisTemplate.keys("neurogate:cache:*")).thenReturn(Set.of("neurogate:cache:1", "neurogate:cache:2"));

        tieredCacheService.invalidateAll();

        verify(redisTemplate).delete(Set.of("neurogate:cache:1", "neurogate:cache:2"));
        verify(semanticCacheService).clearCache();
    }
}

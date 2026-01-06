package com.neurogate.router.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.neurogate.config.NeuroGateProperties;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticCacheServiceTest {

        @Mock
        private EmbeddingService embeddingService;

        @Mock
        private NeuroGateProperties properties;

        @Mock
        private NeuroGateProperties.Qdrant qdrantConfig;

        @Mock
        private QdrantClient qdrantClient;

        @Mock
        private ObjectMapper objectMapper;

        private SemanticCacheService semanticCacheService;

        @BeforeEach
        void setUp() {
                semanticCacheService = new SemanticCacheService(
                                embeddingService,
                                properties,
                                qdrantClient,
                                objectMapper);

                when(properties.getQdrant()).thenReturn(qdrantConfig);
                when(qdrantConfig.getCollectionName()).thenReturn("test_collection");
        }

        @Test
        void testGet_Hit() throws Exception {
                // Given
                ChatRequest request = ChatRequest.builder().model("gpt-3.5").messages(List.of()).build();
                when(embeddingService.generateEmbedding(any())).thenReturn(new float[] { 0.1f, 0.2f });
                when(qdrantConfig.getSimilarityThreshold()).thenReturn(0.9);

                // Mock Qdrant response
                Points.ScoredPoint point = Points.ScoredPoint.newBuilder()
                                .setScore(0.95f)
                                .putPayload("response",
                                                JsonWithInt.Value.newBuilder().setStringValue("{\"id\":\"test\"}")
                                                                .build())
                                .build();

                when(qdrantClient.searchAsync(any(Points.SearchPoints.class)))
                                .thenReturn(Futures.immediateFuture(List.of(point)));

                when(objectMapper.readValue(anyString(), eq(ChatResponse.class)))
                                .thenReturn(ChatResponse.builder().id("test").build());

                // When
                Optional<ChatResponse> result = semanticCacheService.get(request);

                // Then
                assertTrue(result.isPresent());
                assertEquals("test", result.get().getId());
                assertEquals(0.95, result.get().getSimilarity(), 0.01);
        }

        @Test
        void testGet_Miss_LowSimilarity() throws Exception {
                // Given
                ChatRequest request = ChatRequest.builder().model("gpt-3.5").messages(List.of()).build();
                when(embeddingService.generateEmbedding(any())).thenReturn(new float[] { 0.1f, 0.2f });
                when(qdrantConfig.getSimilarityThreshold()).thenReturn(0.9);

                // Mock Qdrant response with low score
                Points.ScoredPoint point = Points.ScoredPoint.newBuilder()
                                .setScore(0.8f)
                                .putPayload("response", JsonWithInt.Value.newBuilder().setStringValue("{}").build())
                                .build();

                when(qdrantClient.searchAsync(any(Points.SearchPoints.class)))
                                .thenReturn(Futures.immediateFuture(List.of(point)));

                // When
                Optional<ChatResponse> result = semanticCacheService.get(request);

                // Then
                assertTrue(result.isEmpty());
        }

        @Test
        void testPut() throws Exception {
                // Given
                ChatRequest request = ChatRequest.builder().model("gpt-3.5").messages(List.of()).build();
                ChatResponse response = ChatResponse.builder().id("resp1").model("gpt-3.5").build();

                when(embeddingService.generateEmbedding(any())).thenReturn(new float[] { 0.1f, 0.2f });
                when(objectMapper.writeValueAsString(any())).thenReturn("{}");

                // Mock upsert future
                when(qdrantClient.upsertAsync(anyString(), anyList()))
                                .thenReturn(Futures.immediateFuture(Points.UpdateResult.getDefaultInstance()));

                // When
                semanticCacheService.put(request, response);

                // Then
                verify(qdrantClient).upsertAsync(eq("test_collection"), anyList());
        }
}

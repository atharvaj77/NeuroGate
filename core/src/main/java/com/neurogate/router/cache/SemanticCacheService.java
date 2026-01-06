package com.neurogate.router.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.config.NeuroGateProperties;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Semantic Cache Service using Qdrant Vector Database.
 * Provides semantic caching that can match similar queries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "neurogate.qdrant", name = "enabled", havingValue = "true")
public class SemanticCacheService {

    private final EmbeddingService embeddingService;
    private final NeuroGateProperties properties;
    private final QdrantClient qdrantClient;
    private final ObjectMapper objectMapper;

    /**
     * Retrieve a cached response for a similar request from Qdrant
     *
     * @param request The incoming chat request
     * @return Optional cached response if similarity > threshold
     */
    public Optional<ChatResponse> get(ChatRequest request) {
        try {
            String prompt = request.getConcatenatedContent();
            log.debug("Searching L3 (Qdrant) for: {}", prompt.substring(0, Math.min(prompt.length(), 50)));

            // Generate embedding
            float[] queryEmbedding = embeddingService.generateEmbedding(prompt);
            List<Float> vector = new ArrayList<>();
            for (float f : queryEmbedding)
                vector.add(f);

            // Search in Qdrant
            List<Points.ScoredPoint> results = qdrantClient.searchAsync(
                    Points.SearchPoints.newBuilder()
                            .setCollectionName(properties.getQdrant().getCollectionName())
                            .addAllVector(vector)
                            .setLimit(1)
                            .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                            .build())
                    .get();

            if (results.isEmpty()) {
                log.debug("L3 Cache MISS - No results found");
                return Optional.empty();
            }

            Points.ScoredPoint bestMatch = results.get(0);
            double similarity = bestMatch.getScore();
            double threshold = properties.getQdrant().getSimilarityThreshold();

            log.debug("L3 Best match score: {} (threshold: {})", similarity, threshold);

            if (similarity >= threshold) {
                // Parse payload
                Map<String, JsonWithInt.Value> payload = bestMatch.getPayloadMap();
                if (payload.containsKey("response")) {
                    String jsonResponse = payload.get("response").getStringValue();
                    ChatResponse response = objectMapper.readValue(jsonResponse, ChatResponse.class);
                    // Set similarity metadata
                    response.setSimilarity(similarity);
                    return Optional.of(response);
                }
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Error searching L3 semantic cache", e);
            return Optional.empty();
        }
    }

    /**
     * Store a request-response pair in Qdrant
     */
    public void put(ChatRequest request, ChatResponse response) {
        try {
            String prompt = request.getConcatenatedContent();
            float[] embedding = embeddingService.generateEmbedding(prompt);
            List<Float> vector = new ArrayList<>();
            for (float f : embedding)
                vector.add(f);

            // Generate UUID based on prompt content (deterministic)
            UUID pointId = UUID.nameUUIDFromBytes(prompt.getBytes());

            // Create payload
            Map<String, JsonWithInt.Value> payload = new HashMap<>();
            payload.put("prompt", JsonWithInt.Value.newBuilder().setStringValue(prompt).build());
            payload.put("response",
                    JsonWithInt.Value.newBuilder().setStringValue(objectMapper.writeValueAsString(response)).build());
            payload.put("timestamp",
                    JsonWithInt.Value.newBuilder().setIntegerValue(Instant.now().toEpochMilli()).build());
            payload.put("model", JsonWithInt.Value.newBuilder().setStringValue(response.getModel()).build());

            // Create point
            Points.PointStruct point = Points.PointStruct.newBuilder()
                    .setId(Points.PointId.newBuilder().setUuid(pointId.toString()).build())
                    .setVectors(Points.Vectors.newBuilder()
                            .setVector(Points.Vector.newBuilder().addAllData(vector).build()).build())
                    .putAllPayload(payload)
                    .build();

            // Upsert
            qdrantClient.upsertAsync(
                    properties.getQdrant().getCollectionName(),
                    List.of(point)).get(); // Wait for completion

            log.debug("Stored in L3 (Qdrant), ID: {}", pointId);

        } catch (Exception e) {
            log.error("Error storing in L3 semantic cache", e);
        }
    }

    /**
     * Clear all cached entries
     */
    public void clearCache() {
        // Not implemented for Qdrant in this phase (needs collection recreation)
        log.warn("Clear cache not fully supported for Qdrant in basic implementation");
    }
}

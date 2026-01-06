package com.neurogate.debugger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.router.cache.EmbeddingService;

import com.neurogate.router.provider.MultiProviderRouter;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;

import com.neurogate.vault.tokenizer.TokenVault;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI Debugger Service.
 * Provides time-travel debugging, semantic diffing, and replay capabilities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIDebuggerService {

    private final MultiProviderRouter routerService;
    private final com.neurogate.agentops.TraceService traceService;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    // In-memory debug record store (TODO: Move to Redis for persistence)
    private final Map<String, DebugRecord> debugRecords = new ConcurrentHashMap<>();

    // Active debug sessions
    private final Map<String, DebugSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Record a request/response for debugging
     * Called automatically after each request
     */
    public void recordRequest(ChatRequest request, ChatResponse response,
            TokenVault tokenVault, long latencyMs) {
        try {
            String requestId = UUID.randomUUID().toString();

            // Create debug record
            DebugRecord record = DebugRecord.builder()
                    .requestId(requestId)
                    .timestamp(Instant.now())
                    .userId(request.getUser())
                    .requestJson(objectMapper.writeValueAsString(request))
                    .responseJson(objectMapper.writeValueAsString(response))
                    .provider(response.getRoute())
                    .cacheHit(response.isCacheHit())
                    .latencyMs(latencyMs)
                    .costUsd(response.getCostUsd() != null ? response.getCostUsd() : 0.0)
                    .embeddingBytes(compressEmbedding(
                            embeddingService.generateEmbedding(request.getConcatenatedContent())))
                    .tags(extractTags(request, response))
                    .build();

            // Store PII tokens if present (encrypted)
            if (tokenVault != null && tokenVault.hasTokens()) {
                record.setPiiTokenMapEncrypted(
                        encryptPiiTokens(tokenVault.getAllTokens()));
            }

            debugRecords.put(requestId, record);

            log.debug("Recorded debug session: requestId={}, provider={}, latency={}ms",
                    requestId, response.getRoute(), latencyMs);

        } catch (Exception e) {
            log.error("Failed to record debug session", e);
        }
    }

    /**
     * Create a debug session for time-travel debugging
     */
    public DebugSession createSession(String requestId) {
        DebugRecord record = debugRecords.get(requestId);
        if (record == null) {
            throw new IllegalArgumentException("Request ID not found: " + requestId);
        }

        try {
            ChatRequest request = objectMapper.readValue(
                    record.getRequestJson(), ChatRequest.class);
            ChatResponse response = objectMapper.readValue(
                    record.getResponseJson(), ChatResponse.class);

            String sessionId = UUID.randomUUID().toString();

            DebugSession session = DebugSession.builder()
                    .sessionId(sessionId)
                    .requestId(requestId)
                    .timestamp(record.getTimestamp())
                    .originalRequest(request)
                    .originalResponse(response)
                    .routingDecision(record.getProvider())
                    .providerUsed(record.getProvider())
                    // .cacheHit(record.getCacheHit())
                    .latencyMs(record.getLatencyMs())
                    .costUsd(record.getCostUsd())
                    .promptEmbedding(decompressEmbedding(record.getEmbeddingBytes()))
                    .build();

            // Decrypt PII tokens if present
            if (record.getPiiTokenMapEncrypted() != null) {
                session.setPiiTokenMap(decryptPiiTokens(record.getPiiTokenMapEncrypted()));
                session.setContainsPii(true);
            }

            // Try to load trace spans for snapshots
            try {
                Optional<com.neurogate.agentops.model.Trace> trace = traceService.getTrace(requestId);
                if (trace.isPresent()) {
                    List<DebugSnapshot> snapshots = trace.get().getSpans().stream()
                            .map(span -> DebugSnapshot.builder()
                                    .stepId(span.getSpanId())
                                    .timestamp(span.getStartTime())
                                    .stepType(span.getType().name())
                                    .content(span.getInput()) // or output depending on type
                                    .state(span.getMetadata())
                                    .build())
                            .collect(Collectors.toList());
                    session.setSnapshots(snapshots);
                }
            } catch (Exception e) {
                log.warn("Failed to load trace for debug session: {}", requestId);
            }

            activeSessions.put(sessionId, session);

            log.info("Created debug session: sessionId={}, requestId={}",
                    sessionId, requestId);

            return session;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create debug session", e);
        }
    }

    /**
     * Fork a session from a specific point
     */
    public DebugSession forkSession(String sessionId, String stepId, Map<String, Object> modifications) {
        DebugSession parentSession = activeSessions.get(sessionId);
        if (parentSession == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        // Create new forked session
        String newSessionId = UUID.randomUUID().toString();
        DebugSession forkedSession = DebugSession.builder()
                .sessionId(newSessionId)
                .requestId(UUID.randomUUID().toString()) // New request ID
                .timestamp(Instant.now())
                .originalRequest(parentSession.getOriginalRequest()) // Copy original request
                // TODO: Apply modifications to the request if applicable
                .build();

        // If we are forking from a specific step, we might want to capture the partial
        // state
        // For now, we just clone the parent session metadata

        activeSessions.put(newSessionId, forkedSession);
        log.info("Forked session {} from {} at step {}", newSessionId, sessionId, stepId);

        return forkedSession;
    }

    /**
     * Replay a request with optional modifications
     */
    public ChatResponse replay(String sessionId, ReplayOptions options) {
        DebugSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        log.info("Replaying request: sessionId={}, model={}, temperature={}",
                sessionId, options.getModel(), options.getTemperature());

        ChatRequest modifiedRequest = ChatRequest.builder()
                .model(options.getModel() != null ? options.getModel() : session.getOriginalRequest().getModel())
                .messages(session.getOriginalRequest().getMessages())
                .temperature(options.getTemperature() != null ? options.getTemperature()
                        : session.getOriginalRequest().getTemperature())
                .maxTokens(options.getMaxTokens() != null ? options.getMaxTokens()
                        : session.getOriginalRequest().getMaxTokens())
                .topP(session.getOriginalRequest().getTopP())
                .user(session.getOriginalRequest().getUser())
                .build();

        // Force cache bypass if requested
        if (options.isBypassCache()) {
            if (options.isBypassCache()) {
                // Clear cache for this specific prompt
                // semanticCacheService.invalidate(modifiedRequest);
            }
        }

        // Route request
        long startTime = System.currentTimeMillis();
        ChatResponse newResponse = routerService.route(modifiedRequest);
        long latency = System.currentTimeMillis() - startTime;

        // Store comparison response
        session.setComparisonResponse(newResponse);

        log.info("Replay complete: latency={}ms, provider={}, cost=${}",
                latency, newResponse.getRoute(), newResponse.getCostUsd());

        return newResponse;
    }

    /**
     * Semantic diffing: Compare two responses
     */
    public SemanticDiff compareResponses(String sessionId) {
        DebugSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        if (session.getComparisonResponse() == null) {
            throw new IllegalStateException("No comparison response available. Call replay() first.");
        }

        String originalContent = session.getOriginalResponse().getChoices().isEmpty()
                ? ""
                : session.getOriginalResponse().getChoices().get(0).getMessage().getContent().toString();

        String comparisonContent = session.getComparisonResponse().getChoices().isEmpty()
                ? ""
                : session.getComparisonResponse().getChoices().get(0).getMessage().getContent().toString();

        // Generate embeddings
        float[] originalEmbedding = embeddingService.generateEmbedding(originalContent);
        float[] comparisonEmbedding = embeddingService.generateEmbedding(comparisonContent);

        // Calculate similarity
        double similarity = cosineSimilarity(originalEmbedding, comparisonEmbedding);
        session.setSemanticSimilarity(similarity);

        // Character-level diff
        List<String> textDiffs = computeTextDiff(originalContent, comparisonContent);

        return SemanticDiff.builder()
                .sessionId(sessionId)
                .originalResponse(originalContent)
                .comparisonResponse(comparisonContent)
                .semanticSimilarity(similarity)
                .textDiffs(textDiffs)
                .originalProvider(session.getOriginalResponse().getRoute())
                .comparisonProvider(session.getComparisonResponse().getRoute())
                .originalCost(session.getOriginalResponse().getCostUsd())
                .comparisonCost(session.getComparisonResponse().getCostUsd())
                .originalLatency(session.getLatencyMs())
                .comparisonLatency(session.getComparisonResponse().getLatencyMs() != null
                        ? session.getComparisonResponse().getLatencyMs()
                        : 0)
                .build();
    }

    /**
     * Search debug records by filters
     */
    public List<DebugRecord> searchRecords(DebugSearchFilter filter) {
        return debugRecords.values().stream()
                .filter(record -> matchesFilter(record, filter))
                .sorted(Comparator.comparing(DebugRecord::getTimestamp).reversed())
                .limit(filter.getLimit() != null ? filter.getLimit() : 100)
                .collect(Collectors.toList());
    }

    /**
     * Get all debug records for a user
     */
    public List<DebugRecord> getUserRecords(String userId, int limit) {
        return debugRecords.values().stream()
                .filter(record -> userId == null || userId.equals(record.getUserId()))
                .sorted(Comparator.comparing(DebugRecord::getTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Export debug session for sharing
     */
    public String exportSession(String sessionId) {
        DebugSession session = activeSessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(session);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export session", e);
        }
    }

    /**
     * Clean up old debug records (retention policy)
     */
    public void cleanupOldRecords(int retentionDays) {
        Instant cutoff = Instant.now().minusSeconds(retentionDays * 86400L);

        boolean removed = debugRecords.entrySet().removeIf(entry -> entry.getValue().getTimestamp().isBefore(cutoff));

        log.info("Cleaned up old debug records (retention: {} days, removed: {})",
                retentionDays, removed);
    }

    // ========== Helper Methods ==========

    private boolean matchesFilter(DebugRecord record, DebugSearchFilter filter) {
        if (filter.getUserId() != null && !filter.getUserId().equals(record.getUserId())) {
            return false;
        }
        if (filter.getProvider() != null && !filter.getProvider().equals(record.getProvider())) {
            return false;
        }
        if (filter.getMinCost() != null && record.getCostUsd() < filter.getMinCost()) {
            return false;
        }
        if (filter.getMaxLatency() != null && record.getLatencyMs() > filter.getMaxLatency()) {
            return false;
        }
        if (filter.getStartTime() != null && record.getTimestamp().isBefore(filter.getStartTime())) {
            return false;
        }
        if (filter.getEndTime() != null && record.getTimestamp().isAfter(filter.getEndTime())) {
            return false;
        }
        return true;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private List<String> computeTextDiff(String original, String comparison) {
        List<String> diffs = new ArrayList<>();
        String[] originalLines = original.split("\n");
        String[] comparisonLines = comparison.split("\n");

        int maxLines = Math.max(originalLines.length, comparisonLines.length);

        for (int i = 0; i < maxLines; i++) {
            String origLine = i < originalLines.length ? originalLines[i] : "";
            String compLine = i < comparisonLines.length ? comparisonLines[i] : "";

            if (!origLine.equals(compLine)) {
                diffs.add(String.format("Line %d: '%s' → '%s'", i + 1, origLine, compLine));
            }
        }

        return diffs;
    }

    private byte[] compressEmbedding(float[] embedding) {
        ByteBuffer buffer = ByteBuffer.allocate(embedding.length * 4);
        for (float value : embedding) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

    private float[] decompressEmbedding(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] embedding = new float[bytes.length / 4];
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = buffer.getFloat();
        }
        return embedding;
    }

    private String[] extractTags(ChatRequest request, ChatResponse response) {
        List<String> tags = new ArrayList<>();
        tags.add("model:" + request.getModel());
        tags.add("provider:" + response.getRoute());
        if (response.isCacheHit()) {
            tags.add("cache:hit");
        }
        if (response.getPiiDetected() != null && response.getPiiDetected() > 0) {
            tags.add("pii:detected");
        }
        return tags.toArray(new String[0]);
    }

    private String encryptPiiTokens(Map<String, String> tokens) {
        // TODO: Implement encryption (AES-256)
        // For now, just serialize as JSON
        try {
            return objectMapper.writeValueAsString(tokens);
        } catch (Exception e) {
            log.error("Failed to encrypt PII tokens", e);
            return "{}";
        }
    }

    private Map<String, String> decryptPiiTokens(String encrypted) {
        // TODO: Implement decryption
        try {
            return objectMapper.readValue(encrypted,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                    });
        } catch (Exception e) {
            log.error("Failed to decrypt PII tokens", e);
            return new HashMap<>();
        }
    }
}

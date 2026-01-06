package com.neurogate.debugger;

import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.vault.tokenizer.TokenVault;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Debug session containing full context for a request.
 */
@Data
@Builder
public class DebugSession {
    private String sessionId;
    private String requestId;
    private Instant timestamp;

    // Original request/response
    private ChatRequest originalRequest;
    private ChatResponse originalResponse;

    // PII context
    private Map<String, String> piiTokenMap;
    private boolean containsPii;

    // Routing metadata
    private String routingDecision;
    private String providerUsed;
    private boolean cacheHit;

    // Embeddings
    private float[] promptEmbedding;

    // Performance metrics
    private long latencyMs;
    private int inputTokens;
    private int outputTokens;
    private double costUsd;

    // Comparison data (for semantic diffing)
    private ChatResponse comparisonResponse;
    private Double semanticSimilarity;

    // Time Travel Snapshots
    private List<DebugSnapshot> snapshots;
}
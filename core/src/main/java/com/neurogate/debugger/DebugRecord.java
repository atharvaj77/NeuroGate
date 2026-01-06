package com.neurogate.debugger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Persistent debug record stored in cache.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebugRecord {
    private String requestId;
    private Instant timestamp;
    private String userId;
    private String applicationId;

    // Serialized request/response
    private String requestJson;
    private String responseJson;

    // Metadata
    private String provider;
    private boolean cacheHit;
    private long latencyMs;
    private double costUsd;

    // Embeddings (compressed)
    private byte[] embeddingBytes;

    // PII tokens (encrypted)
    private String piiTokenMapEncrypted;

    // Tags for filtering
    private String[] tags;
}

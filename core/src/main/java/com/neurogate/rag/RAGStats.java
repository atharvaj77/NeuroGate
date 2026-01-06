package com.neurogate.rag;

import lombok.Builder;
import lombok.Data;

/**
 * Statistics for RAG system
 */
@Data
@Builder
public class RAGStats {
    private int totalDocuments;
    private double averageTokenCount;
    private int totalUsageCount;
    private double cacheHitRate;
    private double averageCostSavings;
}

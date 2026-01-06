package com.neurogate.rag;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * RAG strategy determined by query complexity
 */
@Data
@Builder
public class RAGStrategy {
    private int numDocuments;
    private CompressionLevel compressionLevel;
    private List<DataSource> sources;

    @Builder.Default
    private int maxContextTokens = 2000;

    @Builder.Default
    private RankingStrategy rankingStrategy = RankingStrategy.RELEVANCE;

    @Builder.Default
    private boolean useCache = true;

    public enum CompressionLevel {
        NONE, // No compression, full text
        LOW, // Keep 80% of content
        MEDIUM, // Keep 50% of content
        HIGH // Keep 30% of content (extract key passages)
    }

    public enum DataSource {
        VECTOR_DB, // Qdrant vector database
        SQL, // Relational database
        GRAPH, // Graph database
        API, // External REST API
        FILE // Local file system
    }

    public enum RankingStrategy {
        RELEVANCE, // Sort by cosine similarity
        RECENCY, // Sort by timestamp
        POPULARITY, // Sort by usage count
        COST_OPTIMIZED // Balance relevance and token count
    }

    /**
     * Create a "no RAG" strategy
     */
    public static RAGStrategy none() {
        return RAGStrategy.builder()
                .numDocuments(0)
                .compressionLevel(CompressionLevel.NONE)
                .sources(List.of())
                .build();
    }

    /**
     * Check if RAG is needed
     */
    public boolean isEnabled() {
        return numDocuments > 0 && sources != null && !sources.isEmpty();
    }
}

package com.neurogate.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Document retrieved for RAG
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    private String documentId;
    private String title;
    private String content;
    private float[] embedding;

    // Metadata
    private String source;          // "vector_db", "sql", "api", etc.
    private Instant createdAt;
    private Instant updatedAt;
    private String author;

    // Relevance scoring
    private double relevanceScore;  // Cosine similarity
    private int tokenCount;
    private double costToInclude;   // Estimated cost to include in context

    // Usage stats
    private int usageCount;
    private double averageRating;

    /**
     * Get compressed version of content
     */
    public String getCompressed(RAGStrategy.CompressionLevel level) {
        if (level == RAGStrategy.CompressionLevel.NONE) {
            return content;
        }

        int targetLength = switch (level) {
            case LOW -> (int) (content.length() * 0.8);
            case MEDIUM -> (int) (content.length() * 0.5);
            case HIGH -> (int) (content.length() * 0.3);
            default -> content.length();
        };

        if (content.length() <= targetLength) {
            return content;
        }

        // Simple compression: Extract first N characters + ellipsis
        // TODO: Implement smarter extraction (key sentences, summaries)
        return content.substring(0, targetLength) + "...";
    }

    /**
     * Calculate cost/benefit ratio
     */
    public double getCostBenefitRatio() {
        return relevanceScore / (tokenCount * 0.001); // Lower is better
    }
}

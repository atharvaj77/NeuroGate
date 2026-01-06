package com.neurogate.prompts;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Prompt version with semantic versioning.
 * Uses vector embeddings to determine version increments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "prompt_versions")
public class PromptVersion {
    @Id
    private String versionId;

    @Column(length = 5000)
    private String promptText;

    private String semanticHash; // Hash of embedding for quick lookup

    @ElementCollection
    @CollectionTable(name = "prompt_embeddings", joinColumns = @JoinColumn(name = "version_id"))
    @Column(name = "embedding_value")
    private float[] embedding; // Vector embedding
    // converter

    // Semantic versioning
    private int majorVersion; // Breaking changes (< 60% similarity)
    private int minorVersion; // Compatible changes (60-95% similarity)
    private int patchVersion; // Trivial changes (> 95% similarity)

    // Git-like metadata
    private String commitMessage;
    private String author;
    private Instant timestamp;
    private String parentVersionId; // Previous version
    private String branchName;

    // Performance metrics
    private Double averageCost;
    private Double averageLatency;
    private Double successRate;
    private Integer usageCount;

    // Quality metrics
    private Double qualityScore;
    private Integer thumbsUp;
    private Integer thumbsDown;

    /**
     * Get version string (e.g., "2.5.1")
     */
    public String getVersionString() {
        return String.format("%d.%d.%d", majorVersion, minorVersion, patchVersion);
    }

    /**
     * Check if this is a production version
     */
    public boolean isProduction() {
        return "main".equals(branchName) || "master".equals(branchName);
    }
}

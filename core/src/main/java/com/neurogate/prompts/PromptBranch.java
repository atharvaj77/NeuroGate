package com.neurogate.prompts;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Prompt branch (Git-like branching).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "prompt_branches")
public class PromptBranch {
    @Id
    private String branchId;
    private String branchName;
    private String headVersionId; // Latest version on this branch
    private String baseVersionId; // Version this branch diverged from
    private String author;
    private Instant createdAt;
    private Instant lastUpdated;

    // Status
    @Enumerated(EnumType.STRING)
    private BranchStatus status;
    private String mergedInto; // If merged, which branch
    private int canaryWeight; // 0-100 traffic percentage

    public enum BranchStatus {
        ACTIVE,
        MERGED,
        ABANDONED
    }

    public boolean isMainBranch() {
        return "main".equals(branchName) || "master".equals(branchName);
    }
}

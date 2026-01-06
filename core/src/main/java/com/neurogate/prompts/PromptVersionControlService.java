package com.neurogate.prompts;

import com.neurogate.router.cache.EmbeddingService;
import com.neurogate.router.provider.MultiProviderRouter;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * Prompt Version Control Service.
 * Handles semantic versioning, branching, merging, and A/B testing of prompts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptVersionControlService {

    private final EmbeddingService embeddingService;
    private final MultiProviderRouter routerService;
    private final PromptRepository promptRepository;
    private final TemplateRepository templateRepository;

    // Similarity thresholds for version increments
    private static final double PATCH_THRESHOLD = 0.95; // > 95% = patch
    private static final double MINOR_THRESHOLD = 0.60; // 60-95% = minor
    // < 60% = major

    public PromptVersion commit(String promptText, String commitMessage,
            String author, String branchName) {
        log.info("Committing prompt: branch={}, author={}", branchName, author);

        // Generate embedding
        float[] embedding = embeddingService.generateEmbedding(promptText);
        String semanticHash = computeSemanticHash(embedding);

        // Find parent version on this branch
        Optional<PromptVersion> parentOpt = findLatestVersionOnBranch(branchName);

        int majorVersion = 1;
        int minorVersion = 0;
        int patchVersion = 0;

        if (parentOpt.isPresent()) {
            PromptVersion parent = parentOpt.get();

            // Calculate similarity to parent
            double similarity = cosineSimilarity(embedding, parent.getEmbedding());

            log.debug("Similarity to parent: {}", similarity);

            // Determine version increment
            if (similarity >= PATCH_THRESHOLD) {
                // Trivial change: increment patch
                majorVersion = parent.getMajorVersion();
                minorVersion = parent.getMinorVersion();
                patchVersion = parent.getPatchVersion() + 1;
            } else if (similarity >= MINOR_THRESHOLD) {
                // Compatible change: increment minor
                majorVersion = parent.getMajorVersion();
                minorVersion = parent.getMinorVersion() + 1;
                patchVersion = 0;
            } else {
                // Breaking change: increment major
                majorVersion = parent.getMajorVersion() + 1;
                minorVersion = 0;
                patchVersion = 0;
            }
        }

        // Create new version
        String versionId = UUID.randomUUID().toString();
        PromptVersion version = PromptVersion.builder()
                .versionId(versionId)
                .promptText(promptText)
                .semanticHash(semanticHash)
                .embedding(embedding)
                .majorVersion(majorVersion)
                .minorVersion(minorVersion)
                .patchVersion(patchVersion)
                .commitMessage(commitMessage)
                .author(author)
                .timestamp(Instant.now())
                .parentVersionId(parentOpt.map(PromptVersion::getVersionId).orElse(null))
                .branchName(branchName)
                .usageCount(0)
                .thumbsUp(0)
                .thumbsDown(0)
                .build();

        promptRepository.saveVersion(version);

        // Update branch head
        updateBranchHead(branchName, versionId);

        log.info("Committed version {}: v{}", versionId, version.getVersionString());

        return version;
    }

    public PromptBranch createBranch(String branchName, String baseVersionId, String author) {
        if (promptRepository.findBranchByName(branchName).isPresent()) {
            throw new IllegalArgumentException("Branch already exists: " + branchName);
        }

        // PromptVersion baseVersion = promptRepository.findVersionById(baseVersionId)
        // .orElseThrow(() -> new IllegalArgumentException("Base version not found: " +
        // baseVersionId));

        String branchId = UUID.randomUUID().toString();
        PromptBranch branch = PromptBranch.builder()
                .branchId(branchId)
                .branchName(branchName)
                .headVersionId(baseVersionId)
                .baseVersionId(baseVersionId)
                .author(author)
                .createdAt(Instant.now())
                .lastUpdated(Instant.now())
                .status(PromptBranch.BranchStatus.ACTIVE)
                .build();

        promptRepository.saveBranch(branch);

        log.info("Created branch: {}, base version: {}", branchName, baseVersionId);

        return branch;
    }

    public MergeResult mergeBranches(String sourceBranch, String targetBranch,
            String author, String mergeMessage) {
        PromptBranch source = promptRepository.findBranchByName(sourceBranch).orElse(null);
        PromptBranch target = promptRepository.findBranchByName(targetBranch).orElse(null);

        if (source == null || target == null) {
            throw new IllegalArgumentException("Branch not found");
        }

        PromptVersion sourceHead = promptRepository.findVersionById(source.getHeadVersionId()).orElseThrow();
        PromptVersion targetHead = promptRepository.findVersionById(target.getHeadVersionId()).orElseThrow();

        // Check for conflicts
        double similarity = cosineSimilarity(
                sourceHead.getEmbedding(),
                targetHead.getEmbedding());

        boolean hasConflict = similarity < 0.70; // Conflict if very different

        if (hasConflict) {
            log.warn("Merge conflict detected: similarity={}", similarity);
            return MergeResult.builder()
                    .success(false)
                    .conflictDetected(true)
                    .similarity(similarity)
                    .message("Automatic merge failed due to semantic conflict. Manual resolution required.")
                    .build();
        }

        // Perform merge: Create new version on target branch
        PromptVersion mergedVersion = commit(
                sourceHead.getPromptText(),
                mergeMessage != null ? mergeMessage : String.format("Merge %s into %s", sourceBranch, targetBranch),
                author,
                targetBranch);

        // Mark source branch as merged
        source.setStatus(PromptBranch.BranchStatus.MERGED);
        source.setMergedInto(targetBranch);
        promptRepository.saveBranch(source);

        log.info("Merged {} into {}: version {}", sourceBranch, targetBranch,
                mergedVersion.getVersionString());

        return MergeResult.builder()
                .success(true)
                .conflictDetected(false)
                .similarity(similarity)
                .mergedVersionId(mergedVersion.getVersionId())
                .message("Merge successful")
                .build();
    }

    public ABTestResult runABTest(String versionIdA, String versionIdB,
            int percentageB, int numRequests,
            ChatRequest templateRequest) {
        PromptVersion versionA = promptRepository.findVersionById(versionIdA).orElse(null);
        PromptVersion versionB = promptRepository.findVersionById(versionIdB).orElse(null);

        if (versionA == null || versionB == null) {
            throw new IllegalArgumentException("Version not found");
        }

        log.info("Starting A/B test: A={}, B={}, traffic={}% to B, requests={}",
                versionIdA, versionIdB, percentageB, numRequests);

        List<ChatResponse> responsesA = new ArrayList<>();
        List<ChatResponse> responsesB = new ArrayList<>();

        Random random = new Random();

        // Run test requests
        for (int i = 0; i < numRequests; i++) {
            boolean useB = random.nextInt(100) < percentageB;

            PromptVersion version = useB ? versionB : versionA;
            ChatRequest request = createRequestFromPrompt(version.getPromptText(), templateRequest);

            long startTime = System.currentTimeMillis();
            ChatResponse response = routerService.route(request);
            long latency = System.currentTimeMillis() - startTime;

            response.setLatencyMs(latency);

            if (useB) {
                responsesB.add(response);
            } else {
                responsesA.add(response);
            }
        }

        // Calculate metrics
        ABTestMetrics metricsA = calculateMetrics(responsesA);
        ABTestMetrics metricsB = calculateMetrics(responsesB);

        // Determine winner
        String winner = determineWinner(metricsA, metricsB);

        ABTestResult result = ABTestResult.builder()
                .versionIdA(versionIdA)
                .versionIdB(versionIdB)
                .metricsA(metricsA)
                .metricsB(metricsB)
                .winner(winner)
                .recommendation(generateRecommendation(winner, metricsA, metricsB))
                .build();

        log.info("A/B test complete: winner={}, A_cost=${}, B_cost=${}",
                winner, metricsA.getAverageCost(), metricsB.getAverageCost());

        return result;
    }

    /**
     * Get version history for a prompt
     */
    public List<PromptVersion> getVersionHistory(String branchName) {
        return promptRepository.findVersionsByBranch(branchName);
    }

    /**
     * Rollback to a previous version
     */
    public PromptVersion rollback(String branchName, String targetVersionId, String author) {
        PromptVersion targetVersion = promptRepository.findVersionById(targetVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + targetVersionId));

        // Create new version with old prompt text
        return commit(
                targetVersion.getPromptText(),
                String.format("Rollback to version %s", targetVersion.getVersionString()),
                author,
                branchName);
    }

    /**
     * Create a template from a prompt version
     */
    public PromptTemplate createTemplate(String templateName, String description,
            String templateText,
            Map<String, PromptTemplate.VariableDefinition> variables,
            String[] tags) {
        String templateId = UUID.randomUUID().toString();

        PromptTemplate template = PromptTemplate.builder()
                .templateId(templateId)
                .templateName(templateName)
                .description(description)
                .template(templateText)
                .variables(variables)
                .defaults(new HashMap<>())
                .tags(tags)
                .usageCount(0)
                .averageRating(0.0)
                .build();

        templateRepository.save(template);

        log.info("Created template: {}", templateName);

        return template;
    }

    /**
     * Get template by ID
     */
    public PromptTemplate getTemplate(String templateId) {
        return templateRepository.findById(templateId).orElse(null);
    }

    /**
     * Search templates by tag
     */
    public List<PromptTemplate> searchTemplates(String tag) {
        return templateRepository.findByTag(tag);
    }

    // ========== Helper Methods ==========

    private Optional<PromptVersion> findLatestVersionOnBranch(String branchName) {
        return promptRepository.findBranchByName(branchName)
                .map(PromptBranch::getHeadVersionId)
                .flatMap(promptRepository::findVersionById);
    }

    private void updateBranchHead(String branchName, String versionId) {
        PromptBranch branch = promptRepository.findBranchByName(branchName).orElseGet(() -> PromptBranch.builder()
                .branchId(UUID.randomUUID().toString())
                .branchName(branchName)
                .headVersionId(versionId)
                .createdAt(Instant.now())
                .status(PromptBranch.BranchStatus.ACTIVE)
                .build());

        branch.setHeadVersionId(versionId);
        branch.setLastUpdated(Instant.now());
        promptRepository.saveBranch(branch);
    }

    private String computeSemanticHash(float[] embedding) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Hash first 32 dimensions (sufficient for uniqueness)
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(32, embedding.length); i++) {
                sb.append(String.format("%.4f,", embedding[i]));
            }

            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash).substring(0, 16);

        } catch (Exception e) {
            return UUID.randomUUID().toString().substring(0, 16);
        }
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

    private ChatRequest createRequestFromPrompt(String prompt, ChatRequest template) {
        return ChatRequest.builder()
                .model(template.getModel())
                .messages(List.of(com.neurogate.sentinel.model.Message.user(prompt)))
                .temperature(template.getTemperature())
                .maxTokens(template.getMaxTokens())
                .build();
    }

    private ABTestMetrics calculateMetrics(List<ChatResponse> responses) {
        if (responses.isEmpty()) {
            return ABTestMetrics.builder()
                    .requestCount(0)
                    .averageLatency(0.0)
                    .averageCost(0.0)
                    .successRate(0.0)
                    .build();
        }

        double totalLatency = 0.0;
        double totalCost = 0.0;
        int successCount = 0;

        for (ChatResponse response : responses) {
            totalLatency += response.getLatencyMs() != null ? response.getLatencyMs() : 0;
            totalCost += response.getCostUsd() != null ? response.getCostUsd() : 0.0;
            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                successCount++;
            }
        }

        return ABTestMetrics.builder()
                .requestCount(responses.size())
                .averageLatency(totalLatency / responses.size())
                .averageCost(totalCost / responses.size())
                .successRate((double) successCount / responses.size())
                .build();
    }

    private String determineWinner(ABTestMetrics a, ABTestMetrics b) {
        // Winner: Lower cost, similar success rate
        if (Math.abs(a.getSuccessRate() - b.getSuccessRate()) < 0.05) {
            return a.getAverageCost() < b.getAverageCost() ? "A" : "B";
        }

        // Winner: Higher success rate
        return a.getSuccessRate() > b.getSuccessRate() ? "A" : "B";
    }

    private String generateRecommendation(String winner, ABTestMetrics a, ABTestMetrics b) {
        double costSavings = winner.equals("B") ? (a.getAverageCost() - b.getAverageCost())
                : (b.getAverageCost() - a.getAverageCost());

        return String.format(
                "Version %s wins with %.2f%% better success rate and $%.4f cost savings per request",
                winner,
                (winner.equals("A") ? a.getSuccessRate() : b.getSuccessRate()) * 100,
                costSavings);
    }
}

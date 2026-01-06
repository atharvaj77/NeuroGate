package com.neurogate.prompts;

import com.neurogate.sentinel.model.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for Prompt Version Control
 */
@Slf4j
@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptVersionControlService versionControlService;

    @PostMapping("/commit")
    public ResponseEntity<PromptVersion> commitPrompt(
            @RequestBody CommitRequest request) {

        PromptVersion version = versionControlService.commit(
                request.getPromptText(),
                request.getCommitMessage(),
                request.getAuthor(),
                request.getBranchName() != null ? request.getBranchName() : "main");

        return ResponseEntity.ok(version);
    }

    @PostMapping("/branches")
    public ResponseEntity<PromptBranch> createBranch(
            @RequestBody BranchRequest request) {

        PromptBranch branch = versionControlService.createBranch(
                request.getBranchName(),
                request.getBaseVersionId(),
                request.getAuthor());

        return ResponseEntity.ok(branch);
    }

    @PostMapping("/merge")
    public ResponseEntity<MergeResult> mergeBranches(
            @RequestBody MergeRequest request) {

        MergeResult result = versionControlService.mergeBranches(
                request.getSourceBranch(),
                request.getTargetBranch(),
                request.getAuthor(),
                request.getMergeMessage());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/versions")
    public ResponseEntity<List<PromptVersion>> getVersionHistory(
            @RequestParam(defaultValue = "main") String branchName) {

        List<PromptVersion> history = versionControlService.getVersionHistory(branchName);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/rollback")
    public ResponseEntity<PromptVersion> rollback(
            @RequestBody RollbackRequest request) {

        PromptVersion version = versionControlService.rollback(
                request.getBranchName(),
                request.getTargetVersionId(),
                request.getAuthor());

        return ResponseEntity.ok(version);
    }

    @PostMapping("/ab-test")
    public ResponseEntity<ABTestResult> runABTest(
            @RequestBody ABTestRequest request) {

        log.info("Starting A/B test: A={}, B={}, traffic={}%",
                request.getVersionIdA(), request.getVersionIdB(), request.getPercentageB());

        ABTestResult result = versionControlService.runABTest(
                request.getVersionIdA(),
                request.getVersionIdB(),
                request.getPercentageB(),
                request.getNumRequests(),
                request.getTemplateRequest());

        return ResponseEntity.ok(result);
    }

    /**
     * Create a prompt template
     */
    @PostMapping("/templates")
    public ResponseEntity<PromptTemplate> createTemplate(
            @RequestBody TemplateRequest request) {

        PromptTemplate template = versionControlService.createTemplate(
                request.getTemplateName(),
                request.getDescription(),
                request.getTemplateText(),
                request.getVariables(),
                request.getTags());

        return ResponseEntity.ok(template);
    }

    /**
     * Get template by ID
     */
    @GetMapping("/templates/{templateId}")
    public ResponseEntity<PromptTemplate> getTemplate(
            @PathVariable String templateId) {

        PromptTemplate template = versionControlService.getTemplate(templateId);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(template);
    }

    /**
     * Search templates by tag
     */
    @GetMapping("/templates")
    public ResponseEntity<List<PromptTemplate>> searchTemplates(
            @RequestParam String tag) {

        List<PromptTemplate> templates = versionControlService.searchTemplates(tag);
        return ResponseEntity.ok(templates);
    }

    // ========== Request DTOs ==========

    @lombok.Data
    public static class CommitRequest {
        private String promptText;
        private String commitMessage;
        private String author;
        private String branchName;
    }

    @lombok.Data
    public static class BranchRequest {
        private String branchName;
        private String baseVersionId;
        private String author;
    }

    @lombok.Data
    public static class MergeRequest {
        private String sourceBranch;
        private String targetBranch;
        private String author;
        private String mergeMessage;
    }

    @lombok.Data
    public static class RollbackRequest {
        private String branchName;
        private String targetVersionId;
        private String author;
    }

    @lombok.Data
    public static class ABTestRequest {
        private String versionIdA;
        private String versionIdB;
        private int percentageB;
        private int numRequests;
        private ChatRequest templateRequest;
    }

    @lombok.Data
    public static class TemplateRequest {
        private String templateName;
        private String description;
        private String templateText;
        private Map<String, PromptTemplate.VariableDefinition> variables;
        private String[] tags;
    }
}

package com.neurogate.prompts;

import com.neurogate.sentinel.model.ChatRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Prompts", description = "Prompt management and versioning")
public class PromptController {

    private final PromptVersionControlService versionControlService;

    @Operation(summary = "Commit prompt", description = "Commit a new prompt version")
    @ApiResponse(responseCode = "200", description = "Prompt committed")
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

    @Operation(summary = "Create branch", description = "Create a new prompt branch")
    @ApiResponse(responseCode = "200", description = "Branch created")
    @PostMapping("/branches")
    public ResponseEntity<PromptBranch> createBranch(
            @RequestBody BranchRequest request) {

        PromptBranch branch = versionControlService.createBranch(
                request.getBranchName(),
                request.getBaseVersionId(),
                request.getAuthor());

        return ResponseEntity.ok(branch);
    }

    @Operation(summary = "Merge branches", description = "Merge source branch into target branch")
    @ApiResponse(responseCode = "200", description = "Branches merged")
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

    @Operation(summary = "Get version history", description = "Retrieve prompt version history for a branch")
    @ApiResponse(responseCode = "200", description = "History retrieved")
    @GetMapping("/versions")
    public ResponseEntity<List<PromptVersion>> getVersionHistory(
            @Parameter(description = "Branch name") @RequestParam(defaultValue = "main") String branchName) {

        List<PromptVersion> history = versionControlService.getVersionHistory(branchName);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Rollback version", description = "Rollback to a previous prompt version")
    @ApiResponse(responseCode = "200", description = "Rollback completed")
    @PostMapping("/rollback")
    public ResponseEntity<PromptVersion> rollback(
            @RequestBody RollbackRequest request) {

        PromptVersion version = versionControlService.rollback(
                request.getBranchName(),
                request.getTargetVersionId(),
                request.getAuthor());

        return ResponseEntity.ok(version);
    }

    @Operation(summary = "Run A/B test", description = "Run A/B test between two prompt versions")
    @ApiResponse(responseCode = "200", description = "Test completed")
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

    @Operation(summary = "Create template", description = "Create a new prompt template with variables")
    @ApiResponse(responseCode = "200", description = "Template created")
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

    @Operation(summary = "Get template", description = "Retrieve a prompt template by ID")
    @ApiResponse(responseCode = "200", description = "Template found")
    @ApiResponse(responseCode = "404", description = "Template not found")
    @GetMapping("/templates/{templateId}")
    public ResponseEntity<PromptTemplate> getTemplate(
            @Parameter(description = "Template ID") @PathVariable String templateId) {

        PromptTemplate template = versionControlService.getTemplate(templateId);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(template);
    }

    @Operation(summary = "Search templates", description = "Search prompt templates by tag")
    @ApiResponse(responseCode = "200", description = "Templates found")
    @GetMapping("/templates")
    public ResponseEntity<List<PromptTemplate>> searchTemplates(
            @Parameter(description = "Tag to search") @RequestParam String tag) {

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

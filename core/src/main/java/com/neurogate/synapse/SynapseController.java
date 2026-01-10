package com.neurogate.synapse;

import com.neurogate.router.provider.MultiProviderRouter;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Message;
import com.neurogate.prompts.PromptVersion;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/synapse")
@RequiredArgsConstructor
public class SynapseController {

    private final PromptRegistry promptRegistry;
    private final DiffService diffService;
    private final MultiProviderRouter routerService;

    // Get workflow status (prod/staging versions)
    @GetMapping("/prompts/{promptName}/workflow")
    public ResponseEntity<PromptWorkflow> getWorkflow(@PathVariable String promptName) {
        PromptWorkflow workflow = promptRegistry.getWorkflow(promptName);
        if (workflow == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(workflow);
    }

    // Deploy/Promote
    @PostMapping("/deploy")
    public ResponseEntity<Void> deploy(@RequestBody DeployRequest request) {
        promptRegistry.promote(
                request.getPromptName(),
                request.getVersionId(),
                request.getEnvironment(),
                request.getUser());
        return ResponseEntity.ok().build();
    }

    // Ephemeral Run (Playground)
    @PostMapping("/play")
    public ResponseEntity<ChatResponse> play(@RequestBody PlayRequest request) {
        // Construct the prompt by replacing variables
        String finalPrompt = request.getPromptContent();
        for (Map.Entry<String, String> entry : request.getVariables().entrySet()) {
            finalPrompt = finalPrompt.replace("{{" + entry.getKey() + "}}", entry.getValue());
            finalPrompt = finalPrompt.replace("{{ " + entry.getKey() + " }}", entry.getValue());
        }

        ChatRequest chatRequest = ChatRequest.builder()
                .model(request.getModel())
                .messages(List.of(Message.user(finalPrompt)))
                .temperature(0.7) // Default or parameterized
                .build();

        // Use router service to execute
        ChatResponse response = routerService.route(chatRequest);
        return ResponseEntity.ok(response);
    }

    // Diff two versions
    @PostMapping("/diff")
    public ResponseEntity<DiffService.DiffResult> diff(@RequestBody DiffRequest request) {
        DiffService.DiffResult result = diffService.computeDiff(request.getOriginal(), request.getRevised());
        return ResponseEntity.ok(result);
    }

    // Shadow Comparison (Specter Mode)
    @PostMapping("/shadow/compare")
    public ResponseEntity<ShadowComparisonResult> compareShadow(@RequestBody ShadowCompareRequest request) {
        // 1. Get Production and Shadow Versions
        PromptVersion prodVersion = promptRegistry.getProductionPrompt(request.getPromptName());
        PromptVersion shadowVersion = promptRegistry.getShadowPrompt(request.getPromptName());

        if (prodVersion == null || shadowVersion == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. Hydrate Prompts
        String prodPrompt = hydrate(prodVersion.getPromptText(), request.getVariables());
        String shadowPrompt = hydrate(shadowVersion.getPromptText(), request.getVariables());

        // 3. Execute concurrently
        ChatRequest prodReq = buildChatRequest(prodPrompt, request.getModel());
        ChatRequest shadowReq = buildChatRequest(shadowPrompt, request.getModel()); // Or use shadow-specific model
                                                                                    // config

        ChatResponse prodResponse = routerService.route(prodReq);
        ChatResponse shadowResponse = routerService.route(shadowReq);

        return ResponseEntity.ok(ShadowComparisonResult.builder()
                .productionResponse(prodResponse)
                .shadowResponse(shadowResponse)
                .productionVersionId(prodVersion.getVersionId())
                .shadowVersionId(shadowVersion.getVersionId())
                .build());
    }

    private String hydrate(String template, Map<String, String> variables) {
        String finalPrompt = template;
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                finalPrompt = finalPrompt.replace("{{" + entry.getKey() + "}}", entry.getValue());
                finalPrompt = finalPrompt.replace("{{ " + entry.getKey() + " }}", entry.getValue());
            }
        }
        return finalPrompt;
    }

    private ChatRequest buildChatRequest(String prompt, String model) {
        return ChatRequest.builder()
                .model(model != null ? model : "gpt-4")
                .messages(List.of(Message.user(prompt)))
                .temperature(0.7)
                .build();
    }

    @Data
    public static class DeployRequest {
        private String promptName;
        private String versionId;
        private String environment; // production vs staging
        private String user;
    }

    @Data
    public static class PlayRequest {
        private String promptContent;
        private Map<String, String> variables;
        private String model;
    }

    @Data
    public static class DiffRequest {
        private String original;
        private String revised;
    }

    @Data
    public static class ShadowCompareRequest {
        private String promptName;
        private Map<String, String> variables;
        private String model;
    }

    @Data
    @lombok.Builder
    public static class ShadowComparisonResult {
        private ChatResponse productionResponse;
        private ChatResponse shadowResponse;
        private String productionVersionId;
        private String shadowVersionId;
    }
}

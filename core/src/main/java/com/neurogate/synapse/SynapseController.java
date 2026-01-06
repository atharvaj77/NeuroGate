package com.neurogate.synapse;

import com.neurogate.router.provider.MultiProviderRouter;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Message;
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
}

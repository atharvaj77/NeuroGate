package com.neurogate.router.intelligence;

import com.neurogate.router.intelligence.model.Intent;
import com.neurogate.router.intelligence.model.IntentClassification;
import com.neurogate.router.intelligence.model.ModelRecommendation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for intent classification and routing inspection.
 */
@RestController
@RequestMapping("/api/v1/intent")
@RequiredArgsConstructor
@Tag(name = "Intent Routing", description = "Semantic intent classification and model routing")
public class IntentController {

    private final IntentRouter intentRouter;
    private final IntentRoutingConfig config;

    @PostMapping("/classify")
    @Operation(
            summary = "Classify prompt intent",
            description = "Analyze a prompt and return the detected intent with confidence score"
    )
    public IntentClassification classifyIntent(
            @Parameter(description = "The prompt to classify")
            @RequestBody ClassifyRequest request
    ) {
        return intentRouter.classifyIntent(request.prompt());
    }

    @GetMapping("/intents")
    @Operation(
            summary = "List all intents",
            description = "Get all available intents with their descriptions"
    )
    public List<IntentInfo> listIntents() {
        return Arrays.stream(Intent.values())
                .map(i -> new IntentInfo(i.name(), i.getDescription()))
                .toList();
    }

    @GetMapping("/intents/{intent}/recommendations")
    @Operation(
            summary = "Get model recommendations",
            description = "Get recommended models for a specific intent"
    )
    public List<ModelRecommendation> getRecommendations(
            @Parameter(description = "The intent to get recommendations for")
            @PathVariable String intent
    ) {
        try {
            Intent i = Intent.valueOf(intent.toUpperCase());
            return intentRouter.getRecommendations(i);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    @GetMapping("/mappings")
    @Operation(
            summary = "Get all intent-model mappings",
            description = "Get the complete mapping of intents to recommended models"
    )
    public Map<String, List<ModelRecommendation>> getAllMappings() {
        return Arrays.stream(Intent.values())
                .collect(Collectors.toMap(
                        Intent::name,
                        intentRouter::getRecommendations
                ));
    }

    @GetMapping("/config")
    @Operation(
            summary = "Get intent routing configuration",
            description = "Get current intent routing settings"
    )
    public IntentRoutingStatus getConfig() {
        return new IntentRoutingStatus(
                config.isEnabled(),
                config.getConfidenceThreshold()
        );
    }

    public record ClassifyRequest(String prompt) {}

    public record IntentInfo(String name, String description) {}

    public record IntentRoutingStatus(boolean enabled, double confidenceThreshold) {}
}
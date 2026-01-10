package com.neurogate.synapse.optimizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.router.provider.MultiProviderRouter;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizerService {

    private final MultiProviderRouter router;
    private final ObjectMapper objectMapper;

    private static final String META_PROMPT_TEMPLATE = """
            You are an expert AI Prompt Engineer and LLM Optimization Specialist.
            Your task is to rewrite the user's prompt to achieve the following objective:

            OBJECTIVE: %s

            INSTRUCTIONS:
            1. Analyze the original prompt structure and intent.
            2. Apply advanced prompt engineering techniques (Chain-of-Thought, Delimiters, Persona adoption, etc.) relevant to the objective.
            3. Ensure variables in `{{ double_braces }}` are preserved exactly as is.
            4. Return the result in strictly valid JSON format.

            JSON FORMAT:
            {
               "optimizedPrompt": "The rewritten prompt text...",
               "explanation": "Brief explanation of what techniques you applied and why."
            }
            """;

    public OptimizerResponse optimize(OptimizerRequest request) {
        String objectiveInstruction = request.getObjective().getInstruction();
        String systemPrompt = String.format(META_PROMPT_TEMPLATE, objectiveInstruction);

        ChatRequest metaRequest = ChatRequest.builder()
                .model(request.getModelPreference() != null ? request.getModelPreference() : "gpt-4") // Default to
                                                                                                      // smartest model
                .messages(List.of(
                        Message.builder().role("system").content(systemPrompt).build(),
                        Message.user("ORIGINAL PROMPT:\n" + request.getOriginalPrompt())))
                .temperature(0.3) // Lower temp for deterministic optimization
                .build();

        log.info("Optimizing prompt with objective: {}", request.getObjective());

        try {
            ChatResponse response = router.route(metaRequest);
            String content = response.getChoices().get(0).getMessage().getStrContent();

            // Parse JSON response
            // Handle potential markdown code blocks (```json ... ```)
            if (content.startsWith("```json")) {
                content = content.substring(7);
                if (content.endsWith("```")) {
                    content = content.substring(0, content.length() - 3);
                }
            } else if (content.startsWith("```")) {
                content = content.substring(3);
                if (content.endsWith("```")) {
                    content = content.substring(0, content.length() - 3);
                }
            }

            OptimizationResult result = objectMapper.readValue(content, OptimizationResult.class);

            return OptimizerResponse.builder()
                    .originalPrompt(request.getOriginalPrompt())
                    .optimizedPrompt(result.optimizedPrompt)
                    .explanation(result.explanation)
                    .objective(request.getObjective())
                    .build();

        } catch (Exception e) {
            log.error("Optimization failed", e);
            throw new RuntimeException("Failed to optimize prompt: " + e.getMessage());
        }
    }

    // specific internal DTO for parsing LLM JSON output
    private static class OptimizationResult {
        public String optimizedPrompt;
        public String explanation;
    }
}

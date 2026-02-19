package com.neurogate.consensus;

import com.neurogate.client.ProviderClient;
import com.neurogate.config.NeuroGateProperties;
import com.neurogate.router.resilience.HedgingService;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * ConsensusService - "Hive Mind" Consensus Engine.
 * Queries multiple models and aggregates their answers using a Judge model.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsensusService {

    private final HedgingService hedgingService;
    private final List<ProviderClient> providers;
    private final NeuroGateProperties properties;

    /**
     * Reaches consensus on a prompt by querying multiple providers.
     * 
     * @param originalRequest User query request
     * @return Synthesized answer
     */
    public ConsensusResult reachConsensus(ChatRequest originalRequest) {
        log.info("Initiating Hive Consensus for request: '{}' with {} providers",
                originalRequest.getModel(), providers.size());

        if (providers.isEmpty()) {
            throw new IllegalStateException("No providers configured for Consensus Service");
        }

        // 1. Parallel Execution: Map providers to suppliers
        List<Supplier<ChatResponse>> tasks = providers.stream()
                .map(client -> (Supplier<ChatResponse>) () -> {
                    // Create a defensive copy or modify request if needed for specific models?
                    // For now, assume clients handle model details via adapters.
                    // We might want to force specific models per provider here (e.g. gpt-4,
                    // claude-3, gemini-pro)
                    // but the Adapter handles defaults.
                    return client.generate(originalRequest);
                })
                .collect(Collectors.toList());

        // 2. Execute all using Iron Gate resilience layer
        List<ChatResponse> responses = hedgingService.executeAll("consensus-group", tasks);

        if (responses.isEmpty()) {
            throw new RuntimeException("Consensus failed: No providers returned a valid response.");
        }

        log.info("Received {} responses. Aggregating...", responses.size());

        // 3. Synthesize (Judge)
        String synthesis = synthesize(originalRequest, responses);
        double confidence = calculateConfidence(responses);

        return ConsensusResult.builder()
                .synthesis(synthesis)
                .individualResponses(responses)
                .confidence(confidence)
                .build();
    }

    /**
     * Uses a strong model (e.g. OpenAI) to judge and synthesize the answers.
     */
    private String synthesize(ChatRequest originalRequest, List<ChatResponse> responses) {
        StringBuilder inputs = new StringBuilder();
        inputs.append("Original User Prompt: ").append(originalRequest.getConcatenatedContent()).append("\n\n");

        for (int i = 0; i < responses.size(); i++) {
            ChatResponse resp = responses.get(i);
            inputs.append("Model ").append(i + 1).append(" (").append(resp.getRoute()).append("): \n");
            // Assuming first choice content
            if (!resp.getChoices().isEmpty()) {
                inputs.append(resp.getChoices().get(0).getMessage().getContent());
            }
            inputs.append("\n---\n");
        }

        String synthesisPrompt = "You are an expert Consensus Judge. " +
                "I have provided answers from multiple AI models to the same user prompt. " +
                "Your task is to synthesize the BEST possible answer by combining the strengths of each response " +
                "and correcting any hallucinations found in the minority. " +
                "Return ONLY the synthesized answer, no meta-commentary.";

        String configuredJudgeModel = properties.getConsensus().getJudgeModel();
        List<ProviderClient> judgeCandidates = new ArrayList<>();
        providers.stream()
                .filter(p -> "openai".equalsIgnoreCase(p.getProviderName()))
                .findFirst()
                .ifPresent(judgeCandidates::add);
        providers.stream()
                .filter(p -> !judgeCandidates.contains(p))
                .forEach(judgeCandidates::add);

        if (judgeCandidates.isEmpty()) {
            throw new IllegalStateException("Consensus failed: no available judge providers.");
        }

        ChatRequest baseJudgeRequest = ChatRequest.builder()
                .messages(List.of(
                        Message.builder().role("system").content(synthesisPrompt).build(),
                        Message.builder().role("user").content(inputs.toString()).build()))
                .temperature(0.3) // Lower temp for factual synthesis
                .build();

        // Attempt 1: configured judge model on the preferred judge.
        ProviderClient primaryJudge = judgeCandidates.get(0);
        ChatRequest configuredJudgeRequest = baseJudgeRequest.toBuilder()
                .model(configuredJudgeModel)
                .build();
        try {
            log.info("Using primary judge: {} with model {}", primaryJudge.getProviderName(), configuredJudgeModel);
            ChatResponse judgesVerdict = primaryJudge.generate(configuredJudgeRequest);
            if (!judgesVerdict.getChoices().isEmpty()) {
                return judgesVerdict.getChoices().get(0).getMessage().getStrContent();
            }
        } catch (Exception e) {
            log.warn("Primary judge failed with configured model '{}': {}", configuredJudgeModel, e.getMessage());
        }

        // Attempt 2+: any available provider with request model as fallback.
        String fallbackModel = originalRequest.getModel() != null && !originalRequest.getModel().isBlank()
                ? originalRequest.getModel()
                : configuredJudgeModel;
        ChatRequest fallbackJudgeRequest = baseJudgeRequest.toBuilder()
                .model(fallbackModel)
                .build();

        for (ProviderClient candidate : judgeCandidates) {
            try {
                log.info("Trying fallback judge: {} with model {}", candidate.getProviderName(), fallbackModel);
                ChatResponse judgesVerdict = candidate.generate(fallbackJudgeRequest);
                if (!judgesVerdict.getChoices().isEmpty()) {
                    return judgesVerdict.getChoices().get(0).getMessage().getStrContent();
                }
            } catch (Exception e) {
                log.warn("Fallback judge {} failed: {}", candidate.getProviderName(), e.getMessage());
            }
        }

        throw new IllegalStateException(
                "Consensus failed: unable to synthesize response with configured judge model '" +
                        configuredJudgeModel + "' or any fallback provider.");
    }

    private double calculateConfidence(List<ChatResponse> responses) {
        List<String> texts = responses.stream()
                .map(this::extractText)
                .filter(t -> t != null && !t.isBlank())
                .toList();

        if (texts.size() < 2) {
            return 0.5d;
        }

        double similarityTotal = 0.0d;
        int pairCount = 0;

        for (int i = 0; i < texts.size(); i++) {
            for (int j = i + 1; j < texts.size(); j++) {
                similarityTotal += jaccardSimilarity(texts.get(i), texts.get(j));
                pairCount++;
            }
        }

        if (pairCount == 0) {
            return 0.5d;
        }

        double confidence = similarityTotal / pairCount;
        return Math.max(0.0d, Math.min(1.0d, confidence));
    }

    private String extractText(ChatResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return "";
        }
        if (response.getChoices().get(0).getMessage() == null) {
            return "";
        }
        return response.getChoices().get(0).getMessage().getStrContent();
    }

    private double jaccardSimilarity(String left, String right) {
        Set<String> leftTokens = tokenize(left);
        Set<String> rightTokens = tokenize(right);

        if (leftTokens.isEmpty() && rightTokens.isEmpty()) {
            return 1.0d;
        }

        Set<String> intersection = new HashSet<>(leftTokens);
        intersection.retainAll(rightTokens);

        Set<String> union = new HashSet<>(leftTokens);
        union.addAll(rightTokens);

        if (union.isEmpty()) {
            return 0.0d;
        }
        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String input) {
        return Arrays.stream(input.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }
}

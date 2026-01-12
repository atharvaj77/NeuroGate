package com.neurogate.consensus;

import com.neurogate.client.ProviderClient;
import com.neurogate.router.resilience.HedgingService;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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

        return ConsensusResult.builder()
                .synthesis(synthesis)
                .individualResponses(responses)
                .confidence(0.99) // Mock confidence for now
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

        // Find the "Judge" client (prefer OpenAI, else fallback to first available)
        ProviderClient judge = providers.stream()
                .filter(p -> "openai".equals(p.getProviderName()))
                .findFirst()
                .orElse(providers.get(0));

        log.info("Using judge: {}", judge.getProviderName());

        ChatRequest judgeRequest = ChatRequest.builder()
                .model("gpt-4") // Force high quality for judge
                .messages(List.of(
                        Message.builder().role("system").content(synthesisPrompt).build(),
                        Message.builder().role("user").content(inputs.toString()).build()))
                .temperature(0.3) // Lower temp for factual synthesis
                .build();

        try {
            ChatResponse judgesVerdict = judge.generate(judgeRequest);
            if (!judgesVerdict.getChoices().isEmpty()) {
                return judgesVerdict.getChoices().get(0).getMessage().getStrContent();
            }
        } catch (Exception e) {
            log.error("Judge failed to synthesize", e);
        }

        // Fallback: Return the first response if judge fails
        return responses.get(0).getChoices().get(0).getMessage().getStrContent();
    }
}

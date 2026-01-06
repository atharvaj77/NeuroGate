package com.neurogate.flywheel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.flywheel.model.GoldenInteraction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exports golden interactions as JSONL for fine-tuning.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetExporter {

    private final FeedbackService feedbackService;
    private final ObjectMapper objectMapper;

    /**
     * Export all golden interactions as JSONL (OpenAI fine-tuning format)
     */
    public String exportAsJsonl() throws IOException {
        List<GoldenInteraction> golden = feedbackService.getGoldenInteractions();
        return exportInteractionsAsJsonl(golden);
    }

    /**
     * Export interactions for a specific model
     */
    public String exportAsJsonlForModel(String model) throws IOException {
        List<GoldenInteraction> golden = feedbackService.getInteractionsForModel(model);
        return exportInteractionsAsJsonl(golden);
    }

    /**
     * Convert interactions to JSONL format
     * Format: {"messages": [{"role": "system", "content": "..."}, {"role": "user",
     * "content": "..."}, {"role": "assistant", "content": "..."}]}
     */
    private String exportInteractionsAsJsonl(List<GoldenInteraction> interactions) throws IOException {
        StringWriter writer = new StringWriter();

        for (GoldenInteraction interaction : interactions) {
            Map<String, Object> entry = new HashMap<>();

            List<Map<String, String>> messages = interaction.getMessages().stream()
                    .map(m -> Map.of("role", m.getRole(), "content", m.getStrContent()))
                    .collect(Collectors.toList());
            messages.add(Map.of("role", "assistant", "content", interaction.getResponse()));

            entry.put("messages", messages);

            writer.write(objectMapper.writeValueAsString(entry));
            writer.write("\n");
        }

        log.info("Exported {} golden interactions as JSONL", interactions.size());
        return writer.toString();
    }

    public Map<String, Object> getExportStats() {
        List<GoldenInteraction> golden = feedbackService.getGoldenInteractions();

        int totalTokens = golden.stream()
                .filter(g -> g.getTokenCount() != null)
                .mapToInt(GoldenInteraction::getTokenCount)
                .sum();

        Map<String, Long> byModel = golden.stream()
                .collect(Collectors.groupingBy(
                        GoldenInteraction::getModel,
                        Collectors.counting()));

        return Map.of(
                "total_examples", golden.size(),
                "total_tokens", totalTokens,
                "by_model", byModel,
                "estimated_training_cost_usd", totalTokens * 0.000008 // Rough estimate
        );
    }
}

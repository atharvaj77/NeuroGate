package com.neurogate.core.cortex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RelevanceJudge implements Judge {

    private final java.util.Optional<ChatClient> chatClient;

    @Override
    public String getType() {
        return "relevance";
    }

    @Override
    public JudgeResult grade(String input, String output, String idealOutput) {
        if (chatClient.isEmpty()) {
            return new JudgeResult(0.0, "Judge not configured.", "FAIL");
        }

        String prompt = String.format(
                """
                        You are an impartial judge evaluating the relevance of an AI Assistant's response.

                        Input: %s
                        Agent Response: %s

                        Does the Agent Response directly answer the User's question (Input)?
                        Is it helpful and relevant?

                        Output evaluation in format:
                        Score: [0.0 to 1.0]
                        Reasoning: [One sentence explanation]
                        """,
                input, output);

        try {
            String response = chatClient.get().prompt().user(prompt).call().content();
            if (response == null)
                return new JudgeResult(0.0, "No response", "FAIL");

            return parseResponse(response);
        } catch (Exception e) {
            return new JudgeResult(0.0, "Error: " + e.getMessage(), "FAIL");
        }
    }

    private JudgeResult parseResponse(String response) {
        // Simple parser (duplicate logic, could be shared abstract class but keeping
        // simple for now)
        try {
            double score = 0.0;
            String reasoning = "No reasoning provided.";
            for (String line : response.split("\n")) {
                if (line.toLowerCase().startsWith("score:")) {
                    score = Double.parseDouble(line.split(":")[1].trim());
                } else if (line.toLowerCase().startsWith("reasoning:")) {
                    reasoning = line.split(":", 2)[1].trim();
                }
            }
            String status = score >= 0.7 ? "PASS" : "FAIL";
            return new JudgeResult(score, reasoning, status);
        } catch (Exception e) {
            return new JudgeResult(0.0, "Parse error", "FAIL");
        }
    }
}

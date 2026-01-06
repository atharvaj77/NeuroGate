package com.neurogate.core.cortex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FaithfulnessJudge implements Judge {

    private final java.util.Optional<ChatClient> chatClient;

    @Override
    public double grade(String input, String output, String idealOutput) {
        if (chatClient.isEmpty()) {
            log.debug("ChatClient not available for faithfulness judging, returning 0.0");
            return 0.0;
        }

        String prompt = String.format(
                """
                        You are an impartial judge evaluating the faithfulness of an AI Assistant's response.

                        Input: %s
                        Agent Response: %s
                        Ideal Response (Ground Truth): %s

                        Compare the Agent Response to the Ideal Response and the Input.
                        Is the Agent Response factually consistent with the Input and the Ideal Response?
                        Does it hallucinate new information?

                        Output ONLY a score between 0.0 and 1.0, where 1.0 is perfectly faithful and 0.0 is completely hallucinated.
                        Do not output any explanation.
                        """,
                input, output, idealOutput != null ? idealOutput : "N/A");

        try {
            String response = chatClient.get().prompt().user(prompt).call().content();
            if (response == null)
                return 0.0;
            return Double.parseDouble(response.trim());
        } catch (Exception e) {
            log.warn("Failed to grade faithfulness: {}", e.getMessage());
            return 0.0;
        }
    }
}

package com.neurogate.synapse.optimizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.router.provider.MultiProviderRouter;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Choice;
import com.neurogate.sentinel.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OptimizerServiceTest {

    @Mock
    private MultiProviderRouter router;

    private OptimizerService optimizerService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        optimizerService = new OptimizerService(router, objectMapper);
    }

    @Test
    void testOptimize_FixGrammar() {
        // Arrange
        String original = "me want to go store";
        OptimizerRequest request = OptimizerRequest.builder()
                .originalPrompt(original)
                .objective(OptimizationObjective.FIX_GRAMMAR)
                .build();

        String jsonResponse = """
                {
                    "optimizedPrompt": "I want to go to the store.",
                    "explanation": "Corrected grammar and added missing articles."
                }
                """;

        ChatResponse mockResponse = ChatResponse.builder()
                .choices(List.of(
                        Choice.builder()
                                .message(Message.builder().role("assistant").content(jsonResponse).build())
                                .build()))
                .build();

        when(router.route(any(ChatRequest.class))).thenReturn(mockResponse);

        // Act
        OptimizerResponse response = optimizerService.optimize(request);

        // Assert
        assertNotNull(response);
        assertEquals(original, response.getOriginalPrompt());
        assertEquals("I want to go to the store.", response.getOptimizedPrompt());
        assertEquals("Corrected grammar and added missing articles.", response.getExplanation());
        assertEquals(OptimizationObjective.FIX_GRAMMAR, response.getObjective());
    }

    @Test
    void testOptimize_HandlesMarkdownJson() {
        // Arrange
        // Some LLMs wrap valid JSON in ```json blocks
        String original = "test";
        OptimizerRequest request = OptimizerRequest.builder()
                .originalPrompt(original)
                .objective(OptimizationObjective.CONCISE)
                .build();

        String jsonResponse = """
                ```json
                {
                    "optimizedPrompt": "Test.",
                    "explanation": "Added punctuation."
                }
                ```
                """;

        ChatResponse mockResponse = ChatResponse.builder()
                .choices(List.of(
                        Choice.builder()
                                .message(Message.builder().role("assistant").content(jsonResponse).build())
                                .build()))
                .build();

        when(router.route(any(ChatRequest.class))).thenReturn(mockResponse);

        // Act
        OptimizerResponse response = optimizerService.optimize(request);

        // Assert
        assertEquals("Test.", response.getOptimizedPrompt());
    }
}

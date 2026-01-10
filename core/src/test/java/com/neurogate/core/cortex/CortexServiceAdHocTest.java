package com.neurogate.core.cortex;

import com.neurogate.core.cortex.dto.AdHocEvaluationRequest;
import com.neurogate.core.cortex.dto.AdHocEvaluationResponse;
import com.neurogate.sentinel.SentinelService;
import com.neurogate.sentinel.model.ChatRequest;
import com.neurogate.sentinel.model.ChatResponse;
import com.neurogate.sentinel.model.Choice;
import com.neurogate.sentinel.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CortexServiceAdHocTest {

    @Mock
    private SentinelService sentinelService;

    @Mock
    private Judge faithfulnessJudge;

    private CortexService cortexService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Mock judge behavior
        when(faithfulnessJudge.getType()).thenReturn("faithfulness");
        when(faithfulnessJudge.grade(any(), any(), any())).thenReturn(
                JudgeResult.builder().score(1.0).reasoning("Perfect match").status("PASS").build());

        cortexService = new CortexService(
                null, null, null, // repositories not needed for ad-hoc
                sentinelService,
                Map.of("faithfulness", faithfulnessJudge));
    }

    @Test
    void testEvaluateAdHoc_Success() {
        // Arrange
        AdHocEvaluationRequest request = AdHocEvaluationRequest.builder()
                .promptTemplate("Hello {{ user_query }}")
                .model("gpt-4")
                .testCases(List.of(
                        AdHocEvaluationRequest.TestCase.builder()
                                .id("1")
                                .input("World")
                                .expectedOutput("Hello World")
                                .build()))
                .build();

        ChatResponse mockResponse = ChatResponse.builder()
                .choices(List.of(
                        Choice.builder()
                                .message(Message.builder().role("assistant").content("Hello World").build())
                                .build()))
                .build();

        when(sentinelService.processRequest(any(ChatRequest.class))).thenReturn(mockResponse);

        // Act
        AdHocEvaluationResponse response = cortexService.evaluateAdHoc(request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getResults().size());
        assertEquals(100.0, response.getOverallScore());

        AdHocEvaluationResponse.CaseResult caseResult = response.getResults().get(0);
        assertEquals("1", caseResult.getCaseId());
        assertEquals("Hello World", caseResult.getActualOutput());
        assertTrue(caseResult.isPassed());

        // Verify prompt substitution
        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(sentinelService).processRequest(captor.capture());
        ChatRequest capturedRequest = captor.getValue();
        // Assuming the service converts the list of messages into content string or
        // checks 0th message
        assertEquals("Hello World", capturedRequest.getMessages().get(0).getStrContent());
    }
}

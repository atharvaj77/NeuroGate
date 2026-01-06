package com.neurogate.flywheel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.flywheel.model.FeedbackRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataFlywheelTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DatasetService datasetService;

    @Test
    void testRecordFeedback() {
        FeedbackRequest req = FeedbackRequest.builder()
                .traceId("trace-1")
                .rating(5)
                .comment("Great!")
                .build();
        datasetService.recordFeedback(req);
        assertEquals(1, datasetService.getFeedbackCount());
    }

    @Test
    void testExportTrigger() {
        // Just verify it doesn't throw exception
        assertDoesNotThrow(() -> datasetService.exportGoldenDataset());
    }
}

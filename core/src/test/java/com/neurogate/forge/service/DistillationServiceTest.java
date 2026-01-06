package com.neurogate.forge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.flywheel.DatasetService;
import com.neurogate.flywheel.model.FeedbackRequest;
import com.neurogate.forge.config.ForgeConfig;
import com.neurogate.forge.model.DistillationJob;
import com.neurogate.forge.provider.TrainingProvider;
import com.neurogate.forge.repository.DistillationJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistillationServiceTest {

    @Mock
    private DatasetService datasetService;

    @Mock
    private ForgeConfig forgeConfig;

    @Mock
    private DistillationJobRepository jobRepository;

    @Mock
    private TrainingProvider trainingProvider;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DistillationService distillationService;

    @BeforeEach
    void setUp() {
        // Common setup if needed
    }

    @Test
    void runDistillationCycle_ShouldTriggerTraining_WhenEnoughTracesFound() throws Exception {
        // Arrange
        when(forgeConfig.isEnabled()).thenReturn(true);
        when(forgeConfig.getTriggerThreshold()).thenReturn(1);
        when(forgeConfig.getStudentBaseModel()).thenReturn("gpt-4o-mini");

        FeedbackRequest feedback = new FeedbackRequest();
        feedback.setTraceId("trace-1");
        feedback.setRating(5);
        feedback.setCorrectedOutput("Better output");

        when(datasetService.getGoldenTraces(anyInt())).thenReturn(List.of(feedback));

        when(trainingProvider.uploadFile(any(File.class))).thenReturn("file-123");
        when(trainingProvider.startTrainingJob(eq("file-123"), eq("gpt-4o-mini")))
                .thenReturn(Map.of("id", "ftjob-xyz"));

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"messages\": []}");

        // Act
        distillationService.runNightlyCycle();

        // Assert
        ArgumentCaptor<DistillationJob> jobCaptor = ArgumentCaptor.forClass(DistillationJob.class);
        verify(jobRepository, atLeastOnce()).save(jobCaptor.capture());

        DistillationJob savedJob = jobCaptor.getValue();
        assertEquals("ftjob-xyz", savedJob.getJobId());
        assertEquals(DistillationJob.JobStatus.TRAINING, savedJob.getStatus());
        assertEquals(1, savedJob.getDatasetSize());
    }

    @Test
    void runDistillationCycle_ShouldSkip_WhenNotEnoughTraces() {
        // Arrange
        when(forgeConfig.isEnabled()).thenReturn(true);
        when(forgeConfig.getTriggerThreshold()).thenReturn(100);

        when(datasetService.getGoldenTraces(anyInt())).thenReturn(List.of(new FeedbackRequest()));

        // Act
        distillationService.runNightlyCycle();

        // Assert
        verify(trainingProvider, never()).uploadFile(any());
        verify(jobRepository, never()).save(any());
    }

    @Test
    void runDistillationCycle_ShouldSkip_WhenDisabled() {
        // Arrange
        when(forgeConfig.isEnabled()).thenReturn(false);

        // Act
        distillationService.runNightlyCycle();

        // Assert
        verify(datasetService, never()).getGoldenTraces(anyInt());
    }
}

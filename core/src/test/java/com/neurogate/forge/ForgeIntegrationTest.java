package com.neurogate.forge;

import com.neurogate.flywheel.DatasetService;
import com.neurogate.flywheel.model.FeedbackRequest;
import com.neurogate.forge.model.DistillationJob;
import com.neurogate.forge.provider.TrainingProvider;
import com.neurogate.forge.repository.DistillationJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
                "spring.ai.openai.api-key=test-key",
                "neurogate.anthropic.api-key=test-key",
                "neurogate.gemini.api-key=test-key",
                "neurogate.azure.api-key=test-key",
                "neurogate.azure.endpoint=https://test.openai.azure.com",
                "neurogate.bedrock.access-key=test",
                "neurogate.bedrock.secret-key=test",
                "neurogate.qdrant.host=localhost",
                "neurogate.qdrant.grpc-port=6334",
                "neurogate.qdrant.rest-port=6333",
                "neurogate.qdrant.enabled=false",
                "neurogate.forge.trigger-threshold=1",
                "neurogate.forge.enabled=true"
})
@org.springframework.transaction.annotation.Transactional
public class ForgeIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private DatasetService datasetService;

        @Autowired
        private DistillationJobRepository jobRepository;

        @MockitoBean
        private TrainingProvider trainingProvider;

        @MockitoBean
        private com.neurogate.core.cortex.FaithfulnessJudge faithfulnessJudge;

        @Test
        void triggerDistillation_ShouldCreateJob_WhenEnoughData() throws Exception {
                // Arrange
                FeedbackRequest feedback = new FeedbackRequest();
                feedback.setTraceId("trace-integrated-1");
                feedback.setRating(5);
                feedback.setCorrectedOutput("Corrected output");
                datasetService.recordFeedback(feedback);

                // Populate enough data to hit threshold (assuming threshold is low in test
                // profile, or we force it)
                // Since we can't easily change property in running context without rebuild,
                // we'll rely on the default threshold logic.
                // For test stability, let's assume default threshold might be 100 which is hard
                // to populate.
                // We should add @TestPropertySource or similar if needed.
                // But for now, let's just make sure we populate enough mock data if needed, or
                // better:
                // verify endpoint returns 204 if not enough data, and 200 if enough.

                // Let's populate 100 items just to be safe if threshold is 100 via logic or
                // config overrides
                for (int i = 0; i < 105; i++) {
                        FeedbackRequest f = new FeedbackRequest();
                        f.setTraceId("trace-" + i);
                        f.setRating(5);
                        datasetService.recordFeedback(f);
                }

                when(trainingProvider.uploadFile(any(File.class))).thenReturn("file-int-123");
                when(trainingProvider.startTrainingJob(eq("file-int-123"), anyString()))
                                .thenReturn(Map.of("id", "ftjob-int-xyz"));

                // Act
                mockMvc.perform(post("/api/v1/forge/jobs/trigger"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.jobId").value("ftjob-int-xyz"))
                                .andExpect(jsonPath("$.status").value("TRAINING"));

                // Assert
                List<DistillationJob> jobs = jobRepository.findAll();
                assertEquals(1, jobs.size());
                assertEquals("ftjob-int-xyz", jobs.get(0).getJobId());
        }

        @Test
        void getJobs_ShouldReturnList() throws Exception {
                // Arrange
                DistillationJob job = DistillationJob.builder()
                                .jobId("existing-job")
                                .status(DistillationJob.JobStatus.COMPLETED)
                                .datasetSize(50)
                                .baseModel("gpt-4o-mini")
                                .build();
                jobRepository.save(job);

                // Act
                mockMvc.perform(get("/api/v1/forge/jobs"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].jobId").value("existing-job"));
        }
}

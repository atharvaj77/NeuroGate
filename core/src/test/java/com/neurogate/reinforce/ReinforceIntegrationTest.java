package com.neurogate.reinforce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurogate.agentops.TraceService;
import com.neurogate.agentops.model.Trace;
import com.neurogate.agentops.model.UserFeedback;
import com.neurogate.config.KafkaConfig;
import com.neurogate.reinforce.controller.ReinforceController;
import com.neurogate.reinforce.model.AnnotationTask;
import com.neurogate.reinforce.repository.AnnotationRepository;
import com.neurogate.reinforce.service.AnnotationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReinforceIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private TraceService traceService;

        @Autowired
        private AnnotationRepository annotationRepository;

        @Autowired
        private AnnotationService annotationService;

        @SuppressWarnings("unchecked")
        @MockBean
        private KafkaTemplate<String, Object> kafkaTemplate;

        @Autowired
        private ObjectMapper objectMapper;

        @BeforeEach
        void setup() {
                annotationRepository.deleteAll();
        }

        @Test
        void testEndToEndReinforceFlow() throws Exception {
                // 1. Create a Trace that triggers sampling (Thumbs Down)
                com.neurogate.agentops.model.Span span = com.neurogate.agentops.model.Span.builder()
                                .input("Why is the sky blue?")
                                .output("Because I said so.")
                                .build();

                Trace trace = Trace.builder()
                                .traceId("trace-hitl-001")
                                .userFeedback(UserFeedback.THUMBS_DOWN)
                                .spans(List.of(span))
                                .build();

                // 2. Save Trace -> Should trigger Kafka publish
                traceService.saveTrace(trace);

                // Verify Kafka publish happened
                Mockito.verify(kafkaTemplate, Mockito.times(1))
                                .send(eq(KafkaConfig.ANNOTATION_TOPIC), eq(trace.getTraceId()), any(Trace.class));

                // 3. Since we mocked Kafka, the Consumer won't run automatically.
                // We manually trigger the AnnotationService creation to simulate the consumer.
                annotationService.createTaskFromTrace(trace, "USER_FLAG");

                // 4. Check that AnnotationTask is created
                List<AnnotationTask> tasks = annotationRepository.findAll();
                assertEquals(1, tasks.size());
                assertEquals("trace-hitl-001", tasks.get(0).getTraceId());
                assertEquals(AnnotationTask.AnnotationStatus.PENDING, tasks.get(0).getStatus());

                Long taskId = tasks.get(0).getId();

                // 5. Test Controller: Get Queue
                mockMvc.perform(get("/api/v1/reinforce/queue"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].traceId").value("trace-hitl-001"));

                // 6. Test Controller: Review (Rewrite)
                ReinforceController.ReviewRequest review = new ReinforceController.ReviewRequest();
                review.setStatus(AnnotationTask.AnnotationStatus.REWRITTEN);
                review.setCorrection("Rayleigh scattering primarily.");
                review.setReviewer("sme-alice");

                mockMvc.perform(post("/api/v1/reinforce/" + taskId + "/review")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(review)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("REWRITTEN"))
                                .andExpect(jsonPath("$.humanCorrection").value("Rayleigh scattering primarily."));

                // 7. Verify persistence
                AnnotationTask updatedTask = annotationRepository.findById(taskId).orElseThrow();
                assertEquals(AnnotationTask.AnnotationStatus.REWRITTEN, updatedTask.getStatus());
                assertEquals("sme-alice", updatedTask.getReviewedBy());
        }
}

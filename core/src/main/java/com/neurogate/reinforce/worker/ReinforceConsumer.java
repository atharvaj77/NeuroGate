package com.neurogate.reinforce.worker;

import com.neurogate.agentops.model.Trace;
import com.neurogate.config.KafkaConfig;
import com.neurogate.reinforce.service.AnnotationService;
import com.neurogate.reinforce.service.SamplingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReinforceConsumer {

    private final AnnotationService annotationService;
    private final SamplingService samplingService;

    @KafkaListener(topics = KafkaConfig.ANNOTATION_TOPIC, groupId = "reinforce-group")
    public void consume(Trace trace) {
        log.info("Received trace {} for Reinforce processing", trace.getTraceId());
        try {
            // Determine source again or pass it in message. For now, re-determine.
            // In a robust system, we'd wrap Trace in a "SampledTraceMessage" with metadata.
            String source = samplingService.determineSource(trace);
            annotationService.createTaskFromTrace(trace, source);
        } catch (Exception e) {
            log.error("Error processing reinforce trace {}", trace.getTraceId(), e);
        }
    }
}

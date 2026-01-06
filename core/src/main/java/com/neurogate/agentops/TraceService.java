package com.neurogate.agentops;

import com.neurogate.agentops.model.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.neurogate.config.KafkaConfig;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TraceService {

    private final TraceRepository traceRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final com.neurogate.reinforce.service.SamplingService samplingService;

    public void saveTrace(Trace trace) {
        traceRepository.save(trace);

        try {
            kafkaTemplate.send(KafkaConfig.TRACE_TOPIC, trace.getTraceId(), trace);
        } catch (Exception e) {
            log.error("Failed to publish trace to Kafka", e);
        }

        try {
            if (samplingService.shouldSample(trace)) {
                log.info("Trace {} sampled for Reinforce", trace.getTraceId());
                kafkaTemplate.send(KafkaConfig.ANNOTATION_TOPIC, trace.getTraceId(), trace);
            }
        } catch (Exception e) {
            log.error("Failed to publish sample to Kafka", e);
        }

        log.info("Saved trace: {} with {} spans", trace.getTraceId(),
                trace.getSpans() != null ? trace.getSpans().size() : 0);
    }

    public Optional<Trace> getTrace(String traceId) {
        return traceRepository.findById(traceId);
    }

    public List<Trace> getTracesBySession(String sessionId) {
        return traceRepository.findBySessionId(sessionId);
    }

    public List<Trace> getRecentTraces(int limit) {
        return traceRepository.findRecent(limit);
    }

    public List<Trace> getTracesByUser(String userId) {
        return traceRepository.findByUserId(userId);
    }

    public long getTraceCount() {
        return traceRepository.count();
    }

    public Map<String, Object> getStatistics() {
        List<Trace> traces = traceRepository.findAll();
        long totalTraces = traces.size();
        long totalSpans = traces.stream()
                .mapToLong(t -> t.getSpans() != null ? t.getSpans().size() : 0)
                .sum();
        double totalCost = traces.stream()
                .filter(t -> t.getTotalCostUsd() != null)
                .mapToDouble(Trace::getTotalCostUsd)
                .sum();
        long totalTokens = traces.stream()
                .filter(t -> t.getTotalTokens() != null)
                .mapToLong(Trace::getTotalTokens)
                .sum();

        return Map.of(
                "total_traces", totalTraces,
                "total_spans", totalSpans,
                "total_cost_usd", totalCost,
                "total_tokens", totalTokens);
    }
}

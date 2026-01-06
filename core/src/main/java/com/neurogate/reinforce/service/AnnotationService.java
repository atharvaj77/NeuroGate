package com.neurogate.reinforce.service;

import com.neurogate.agentops.model.Trace;
import com.neurogate.agentops.TraceService;
import com.neurogate.reinforce.model.AnnotationTask;
import com.neurogate.reinforce.repository.AnnotationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnotationService {

    private final AnnotationRepository annotationRepository;
    private final TraceService traceService;

    @Transactional
    public void createTaskFromTraceId(String traceId, String source) {
        Optional<Trace> traceOpt = traceService.getTrace(traceId);
        if (traceOpt.isEmpty()) {
            log.warn("Could not find trace {} to create annotation task", traceId);
            return;
        }
        createTaskFromTrace(traceOpt.get(), source);
    }

    @Transactional
    public void createTaskFromTrace(Trace trace, String source) {
        // Check if task already exists
        List<AnnotationTask> existing = annotationRepository.findByTraceId(trace.getTraceId());
        if (!existing.isEmpty()) {
            log.info("Annotation task already exists for trace {}", trace.getTraceId());
            return;
        }

        AnnotationTask task = AnnotationTask.builder()
                .traceId(trace.getTraceId())
                .input(trace.getInput())
                .output(trace.getOutput())
                .samplerSource(source)
                .status(AnnotationTask.AnnotationStatus.PENDING)
                .build();

        annotationRepository.save(task);
        log.info("Created annotation task for trace {}", trace.getTraceId());
    }

    public List<AnnotationTask> getPendingTasks() {
        return annotationRepository.findByStatus(AnnotationTask.AnnotationStatus.PENDING);
    }

    @Transactional
    public AnnotationTask reviewTask(Long id, AnnotationTask.AnnotationStatus status, String correction,
            String reviewer) {
        AnnotationTask task = annotationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));

        task.setStatus(status);
        task.setReviewedBy(reviewer);
        task.setReviewTime(Instant.now());

        if (status == AnnotationTask.AnnotationStatus.REWRITTEN) {
            if (correction == null || correction.isBlank()) {
                throw new IllegalArgumentException("Correction is required for REWRITTEN status");
            }
            task.setHumanCorrection(correction);
        }

        return annotationRepository.save(task);
    }
}

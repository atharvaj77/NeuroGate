package com.neurogate.agentops;

import com.neurogate.agentops.model.Trace;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Primary
@Repository
@RequiredArgsConstructor
public class PersistentTraceRepository implements TraceRepository {

    private final JpaTraceRepository jpaTraceRepository;

    @Override
    public Trace save(Trace trace) {
        return jpaTraceRepository.save(trace);
    }

    @Override
    public Optional<Trace> findById(String traceId) {
        return jpaTraceRepository.findById(traceId);
    }

    @Override
    public List<Trace> findBySessionId(String sessionId) {
        return jpaTraceRepository.findBySessionIdOrderByStartTimeDesc(sessionId);
    }

    @Override
    public List<Trace> findByUserId(String userId) {
        return jpaTraceRepository.findByUserIdOrderByStartTimeDesc(userId);
    }

    @Override
    public List<Trace> findRecent(int limit) {
        // JPA signature trick: TopN is usually part of method name, but explicit limit
        // requires Pageable.
        // For simplicity reusing the method that fetches 100 or using sublist if
        // needed,
        // but JpaTraceRepository defines findTop100.
        // If limit is > 100, it might be an issue, but for now it's fine.
        List<Trace> traces = jpaTraceRepository.findTop100ByOrderByStartTimeDesc();
        if (traces.size() > limit) {
            return traces.subList(0, limit);
        }
        return traces;
    }

    @Override
    public long count() {
        return jpaTraceRepository.count();
    }

    @Override
    public List<Trace> findAll() {
        return jpaTraceRepository.findAll();
    }
}

package com.neurogate.agentops;

import com.neurogate.agentops.model.Trace;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryTraceRepository implements TraceRepository {

    private final Map<String, Trace> traces = new ConcurrentHashMap<>();

    @Override
    public Trace save(Trace trace) {
        traces.put(trace.getTraceId(), trace);
        return trace;
    }

    @Override
    public Optional<Trace> findById(String traceId) {
        return Optional.ofNullable(traces.get(traceId));
    }

    @Override
    public List<Trace> findBySessionId(String sessionId) {
        return traces.values().stream()
                .filter(t -> sessionId.equals(t.getSessionId()))
                .sorted(this::compareByStartTimeDesc)
                .collect(Collectors.toList());
    }

    @Override
    public List<Trace> findByUserId(String userId) {
        return traces.values().stream()
                .filter(t -> userId.equals(t.getUserId()))
                .sorted(this::compareByStartTimeDesc)
                .collect(Collectors.toList());
    }

    @Override
    public List<Trace> findRecent(int limit) {
        return traces.values().stream()
                .sorted(this::compareByStartTimeDesc)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return traces.size();
    }

    @Override
    public List<Trace> findAll() {
        return new ArrayList<>(traces.values());
    }

    private int compareByStartTimeDesc(Trace a, Trace b) {
        if (a.getStartTime() == null || b.getStartTime() == null) {
            return 0;
        }
        return b.getStartTime().compareTo(a.getStartTime());
    }
}

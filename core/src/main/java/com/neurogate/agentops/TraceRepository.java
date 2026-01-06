package com.neurogate.agentops;

import com.neurogate.agentops.model.Trace;

import java.util.List;
import java.util.Optional;

public interface TraceRepository {
    Trace save(Trace trace);

    Optional<Trace> findById(String traceId);

    List<Trace> findBySessionId(String sessionId);

    List<Trace> findByUserId(String userId);

    List<Trace> findRecent(int limit);

    long count();

    List<Trace> findAll();
}

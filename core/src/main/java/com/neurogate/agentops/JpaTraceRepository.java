package com.neurogate.agentops;

import com.neurogate.agentops.model.Trace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaTraceRepository extends JpaRepository<Trace, String> {
    List<Trace> findBySessionIdOrderByStartTimeDesc(String sessionId);

    List<Trace> findByUserIdOrderByStartTimeDesc(String userId);

    List<Trace> findTop100ByOrderByStartTimeDesc();
}

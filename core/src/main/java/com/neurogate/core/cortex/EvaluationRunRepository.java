package com.neurogate.core.cortex;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationRunRepository extends JpaRepository<EvaluationRun, String> {
    List<EvaluationRun> findByDatasetId(String datasetId);
}

package com.neurogate.core.cortex;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationDatasetRepository extends JpaRepository<EvaluationDataset, String> {
}

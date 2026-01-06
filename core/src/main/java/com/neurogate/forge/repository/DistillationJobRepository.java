package com.neurogate.forge.repository;

import com.neurogate.forge.model.DistillationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DistillationJobRepository extends JpaRepository<DistillationJob, UUID> {
    Optional<DistillationJob> findByJobId(String jobId);

    List<DistillationJob> findByStatus(DistillationJob.JobStatus status);
}

package com.neurogate.reinforce.repository;

import com.neurogate.reinforce.model.AnnotationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnotationRepository extends JpaRepository<AnnotationTask, Long> {

    List<AnnotationTask> findByStatus(AnnotationTask.AnnotationStatus status);

    List<AnnotationTask> findByTraceId(String traceId);

    List<AnnotationTask> findByReviewedBy(String reviewedBy);
}

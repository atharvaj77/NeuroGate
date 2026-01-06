package com.neurogate.synapse;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromptWorkflowRepository extends CrudRepository<PromptWorkflow, String> {
}

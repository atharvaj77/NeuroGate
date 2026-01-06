package com.neurogate.forge.provider;

import java.io.File;
import java.util.Map;

public interface TrainingProvider {
    /**
     * Upload a training file to the provider.
     */
    String uploadFile(File file);

    /**
     * Start a fine-tuning job
     * 
     * @param fileId    The ID of the uploaded training file
     * @param baseModel The base model to fine-tune (e.g., gpt-3.5-turbo)
     * @return A map containing provider-specific job details (id, status, etc.)
     */
    Map<String, Object> startTrainingJob(String fileId, String baseModel);

    /**
     * Check the status of a fine-tuning job
     * 
     * @param jobId The provider's job ID
     * @return A map containing status and metrics
     */
    Map<String, Object> getJobStatus(String jobId);
}

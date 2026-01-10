package com.neurogate.synapse.optimizer;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OptimizerRequest {
    private String originalPrompt;
    private OptimizationObjective objective;
    private String modelPreference; // e.g. "gpt-4"
}

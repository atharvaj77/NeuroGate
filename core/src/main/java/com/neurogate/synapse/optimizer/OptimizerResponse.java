package com.neurogate.synapse.optimizer;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OptimizerResponse {
    private String originalPrompt;
    private String optimizedPrompt;
    private String explanation;
    private OptimizationObjective objective;
}

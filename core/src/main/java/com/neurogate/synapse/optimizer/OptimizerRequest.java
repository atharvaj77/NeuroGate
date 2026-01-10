package com.neurogate.synapse.optimizer;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizerRequest {
    private String originalPrompt;
    private OptimizationObjective objective;
    private String modelPreference; // e.g. "gpt-4"
}

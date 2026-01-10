package com.neurogate.core.cortex;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JudgeResult {
    private double score;
    private String reasoning;
    private String status; // PASS, FAIL, WARN

    public boolean isPass() {
        return "PASS".equalsIgnoreCase(status);
    }
}

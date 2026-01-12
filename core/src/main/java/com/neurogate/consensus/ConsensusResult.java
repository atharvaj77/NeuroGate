package com.neurogate.consensus;

import com.neurogate.sentinel.model.ChatResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ConsensusResult {
    private String synthesis;
    private List<ChatResponse> individualResponses;
    private double confidence;
}

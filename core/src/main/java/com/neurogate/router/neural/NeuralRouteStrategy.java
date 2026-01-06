package com.neurogate.router.neural;

import com.neurogate.router.neural.ProviderScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Selects best providers using neural scores.
 */
@Slf4j
@Component
public class NeuralRouteStrategy {

    private final ProviderScoreService scoreService;

    public NeuralRouteStrategy(ProviderScoreService scoreService) {
        this.scoreService = scoreService;
    }

    /**
     * Selects the top N providers for a given set of candidates.
     */
    public List<String> selectBestProviders(List<String> candidates, int n) {
        return candidates.stream()
                .sorted(Comparator.comparingDouble(scoreService::getScore).reversed()) // Descending score
                .limit(n)
                .collect(Collectors.toList());
    }
}

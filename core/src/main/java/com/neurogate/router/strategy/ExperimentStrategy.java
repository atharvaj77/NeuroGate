package com.neurogate.router.strategy;

import com.neurogate.experiment.ExperimentService;
import com.neurogate.experiment.model.Experiment;
import com.neurogate.experiment.model.Variant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Routes requests based on A/B testing experiments.
 * Assigns users to variants and selects corresponding models.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExperimentStrategy implements RoutingStrategy {

    private final Optional<ExperimentService> experimentService;

    @Override
    public Optional<RoutingContext> apply(RoutingContext context) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        ExperimentService service = experimentService.get();
        Optional<Experiment> activeExperiment = service.findActiveExperiment(context.getCurrentRequest());

        if (activeExperiment.isEmpty()) {
            return Optional.empty();
        }

        Experiment experiment = activeExperiment.get();
        Variant variant = service.assignVariant(experiment.getExperimentId(), context.getCurrentRequest());
        String experimentModel = service.getModelForVariant(experiment.getExperimentId(), variant);

        log.info("🧪 A/B Test: Experiment '{}' assigned {} → model '{}'",
                experiment.getName(), variant, experimentModel);

        RoutingContext updated = context.withModel(experimentModel,
                "A/B test: " + experiment.getName() + " variant " + variant);
        updated.setExperimentRoutingApplied(true);
        updated.setExperimentId(experiment.getExperimentId());
        updated.setExperimentVariant(variant);

        return Optional.of(updated);
    }

    @Override
    public int getPriority() {
        return 20; // Run after intent routing
    }

    @Override
    public String getName() {
        return "experiment-routing";
    }

    @Override
    public boolean isEnabled() {
        return experimentService.isPresent();
    }
}

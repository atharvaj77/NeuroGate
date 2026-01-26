package com.neurogate.router.strategy;

import com.neurogate.router.intelligence.IntentRouter;
import com.neurogate.router.intelligence.model.RoutingDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Routes requests based on classified intent.
 * Maps intents like CODE_GENERATION, REASONING, etc. to optimal models.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentBasedStrategy implements RoutingStrategy {

    private final IntentRouter intentRouter;

    @Override
    public Optional<RoutingContext> apply(RoutingContext context) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        RoutingDecision decision = intentRouter.route(context.getCurrentRequest());

        if (decision.isIntentRoutingApplied()) {
            log.info("🎯 Intent routing applied: {} → {} (intent: {}, confidence: {:.2f})",
                    context.getSelectedModel(),
                    decision.getSelectedModel(),
                    decision.getIntent(),
                    decision.getConfidence());

            RoutingContext updated = context.withModel(decision.getSelectedModel(), decision.getReason());
            updated.setIntentRoutingApplied(true);
            return Optional.of(updated);
        }

        return Optional.empty();
    }

    @Override
    public int getPriority() {
        return 10; // Run early
    }

    @Override
    public String getName() {
        return "intent-routing";
    }

    @Override
    public boolean isEnabled() {
        return intentRouter != null && intentRouter.isEnabled();
    }
}

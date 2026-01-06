package com.neurogate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Configuration for provider pricing.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "neurogate.pricing")
public class PricingConfig {

    // Default costs per 1K tokens (USD)
    // Map key: model name
    // Map value: [input cost, output cost]
    private Map<String, BigDecimal[]> costs = Map.of(
            "gpt-4", new BigDecimal[] { new BigDecimal("0.03"), new BigDecimal("0.06") },
            "gpt-4-turbo", new BigDecimal[] { new BigDecimal("0.01"), new BigDecimal("0.03") },
            "gpt-3.5-turbo", new BigDecimal[] { new BigDecimal("0.0015"), new BigDecimal("0.002") },
            "claude-3-opus", new BigDecimal[] { new BigDecimal("0.015"), new BigDecimal("0.075") },
            "claude-3-sonnet", new BigDecimal[] { new BigDecimal("0.003"), new BigDecimal("0.015") },
            "claude-3-haiku", new BigDecimal[] { new BigDecimal("0.00025"), new BigDecimal("0.00125") },
            "gemini-pro", new BigDecimal[] { new BigDecimal("0.00025"), new BigDecimal("0.0005") },
            "gemini-ultra", new BigDecimal[] { new BigDecimal("0.001"), new BigDecimal("0.002") });

    /**
     * Get pricing for a specific model.
     * Returns default low cost if model not found.
     *
     * @param model Model name
     * @return Array containing [input cost, output cost] per 1K tokens
     */
    public BigDecimal[] getCostForModel(String model) {
        return costs.getOrDefault(model, new BigDecimal[] { new BigDecimal("0.001"), new BigDecimal("0.002") });
    }

    /**
     * Calculate estimated cost for a request
     * 
     * @param provider Provider name (unused currently, reserved for future)
     * @param model    Model name
     * @param tokens   Approximate token count
     * @return Estimated cost in USD
     */
    public double getProviderCost(String provider, String model, int tokens) {
        BigDecimal[] rates = getCostForModel(model);
        // Average input/output cost for simplicity in metrics estimates
        BigDecimal averageRate = rates[0].add(rates[1]).divide(new BigDecimal(2), java.math.RoundingMode.HALF_UP);

        return averageRate.multiply(new BigDecimal(tokens))
                .divide(new BigDecimal(1000), java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }
}

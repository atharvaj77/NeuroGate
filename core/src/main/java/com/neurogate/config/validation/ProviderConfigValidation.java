package com.neurogate.config.validation;

import com.neurogate.router.provider.LLMProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates LLM provider configuration.
 */
@Component
@RequiredArgsConstructor
public class ProviderConfigValidation implements ConfigValidation {

    private final List<LLMProvider> providers;

    @Override
    public ValidationResult validate() {
        if (providers == null || providers.isEmpty()) {
            return ValidationResult.error("No LLM providers configured");
        }

        List<String> warnings = new ArrayList<>();
        int availableCount = 0;

        for (LLMProvider provider : providers) {
            if (provider.isAvailable()) {
                availableCount++;
            } else {
                warnings.add("Provider '" + provider.getName() + "' is not available");
            }
        }

        if (availableCount == 0) {
            return ValidationResult.error("No LLM providers are available");
        }

        if (!warnings.isEmpty()) {
            return ValidationResult.builder()
                    .valid(false)
                    .critical(false)
                    .errors(warnings)
                    .build();
        }

        return ValidationResult.ok();
    }

    @Override
    public String getName() {
        return "provider-config";
    }
}

package com.neurogate.vault.guard;

import com.neurogate.vault.neuroguard.PromptInjectionDetector;
import com.neurogate.vault.neuroguard.model.ThreatDetectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Security guard for prompt injection detection.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptInjectionGuard implements SecurityGuard {

    private final PromptInjectionDetector injectionDetector;

    @Override
    public ThreatDetectionResult check(String content) {
        return injectionDetector.analyze(content);
    }

    @Override
    public GuardType getType() {
        return GuardType.PROMPT_INJECTION;
    }

    @Override
    public int getPriority() {
        return 20; // High priority - block malicious prompts early
    }
}

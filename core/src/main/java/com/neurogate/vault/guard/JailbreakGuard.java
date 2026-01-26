package com.neurogate.vault.guard;

import com.neurogate.vault.neuroguard.JailbreakDetector;
import com.neurogate.vault.neuroguard.model.ThreatDetectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Security guard for jailbreak attempt detection.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JailbreakGuard implements SecurityGuard {

    private final JailbreakDetector jailbreakDetector;

    @Override
    public ThreatDetectionResult check(String content) {
        return jailbreakDetector.analyze(content);
    }

    @Override
    public GuardType getType() {
        return GuardType.JAILBREAK;
    }

    @Override
    public int getPriority() {
        return 30; // Run after prompt injection
    }
}

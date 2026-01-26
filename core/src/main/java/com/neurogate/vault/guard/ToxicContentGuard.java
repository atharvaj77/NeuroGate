package com.neurogate.vault.guard;

import com.neurogate.vault.neuroguard.ToxicOutputFilter;
import com.neurogate.vault.neuroguard.model.ThreatDetectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Security guard for toxic content detection.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToxicContentGuard implements SecurityGuard {

    private final ToxicOutputFilter toxicOutputFilter;

    @Override
    public ThreatDetectionResult check(String content) {
        return toxicOutputFilter.analyze(content);
    }

    @Override
    public GuardType getType() {
        return GuardType.TOXIC_CONTENT;
    }

    @Override
    public int getPriority() {
        return 40; // Run after other security checks
    }
}

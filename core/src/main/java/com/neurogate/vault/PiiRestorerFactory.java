package com.neurogate.vault;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PiiRestorerFactory {

    private final PiiSanitizationService piiSanitizationService;

    public StreamingPiiRestorer createRestorer() {
        return new StreamingPiiRestorer(piiSanitizationService);
    }
}

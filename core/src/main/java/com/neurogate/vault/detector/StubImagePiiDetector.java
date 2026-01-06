package com.neurogate.vault.detector;

import com.neurogate.vault.model.PiiEntity;
import com.neurogate.vault.model.PiiType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class StubImagePiiDetector implements ImagePiiDetector {

    @Override
    public List<PiiEntity> detect(String imageUrlOrBase64) {
        log.debug("Scanning image for PII (Stub implementation)");
        // In a real implementation, we would call an OCR service or local ML model
        // here.
        // For now, return empty list or mock based on input.

        if (imageUrlOrBase64.contains("trigger-pii")) {
            return List.of(new PiiEntity(PiiType.CREDIT_CARD, "DETECTED_IN_IMAGE", 0, 0));
        }

        return Collections.emptyList();
    }
}

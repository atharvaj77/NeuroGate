package com.neurogate.vault.detector;

import com.neurogate.vault.model.PiiEntity;
import java.util.List;

public interface ImagePiiDetector {
    /**
     * Detect PII in an image.
     * 
     * @param imageUrlOrBase64 Image URL or Base64 string
     * @return List of detected PII entities with bounding boxes (if applicable)
     */
    List<PiiEntity> detect(String imageUrlOrBase64);
}

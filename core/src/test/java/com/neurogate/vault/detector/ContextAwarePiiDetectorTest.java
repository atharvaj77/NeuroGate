package com.neurogate.vault.detector;

import com.neurogate.vault.model.PiiEntity;
import com.neurogate.vault.model.PiiType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContextAwarePiiDetectorTest {

    private ContextAwarePiiDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ContextAwarePiiDetector();
    }

    @Test
    void shouldDetectUnformattedSsnWhenContextIsPresent() {
        String text = "Please verify SSN 123456789 before continuing";

        List<PiiEntity> entities = detector.detect(text);

        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).getType()).isEqualTo(PiiType.SSN);
        assertThat(entities.get(0).getValue()).isEqualTo("123456789");
    }
}

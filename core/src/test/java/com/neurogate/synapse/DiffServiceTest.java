package com.neurogate.synapse;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class DiffServiceTest {

    private final DiffService diffService = new DiffService();

    @Test
    void testComputeDiff_ChangesDetected() {
        String original = "Hello World\nThis is a test.";
        String revised = "Hello Universe\nThis is a test.";

        DiffService.DiffResult result = diffService.computeDiff(original, revised);

        assertNotNull(result);
        List<DiffService.DiffDelta> deltas = result.getDeltas();
        assertFalse(deltas.isEmpty());

        DiffService.DiffDelta delta = deltas.get(0);
        assertEquals("CHANGE", delta.getType());
        assertEquals(0, delta.getSourceStart());
        assertEquals(List.of("Hello World"), delta.getSourceLines());
        assertEquals(List.of("Hello Universe"), delta.getTargetLines());
    }

    @Test
    void testComputeDiff_NoChanges() {
        String original = "Same content";
        String revised = "Same content";

        DiffService.DiffResult result = diffService.computeDiff(original, revised);

        assertNotNull(result);
        assertTrue(result.getDeltas().isEmpty());
    }
}

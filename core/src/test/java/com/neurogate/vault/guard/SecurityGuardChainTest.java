package com.neurogate.vault.guard;

import com.neurogate.vault.neuroguard.model.ThreatDetectionResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityGuardChainTest {

    @Test
    void shouldHandleConcurrentReadsWhileGuardIsAdded() throws Exception {
        SecurityGuardChain chain = new SecurityGuardChain(List.of(new SafeGuard(SecurityGuard.GuardType.PII_DETECTION, 1)));

        int readerThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(readerThreads + 1);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(readerThreads + 1);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < readerThreads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await(2, TimeUnit.SECONDS);
                    for (int j = 0; j < 100; j++) {
                        chain.execute("safe content");
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        futures.add(executor.submit(() -> {
            try {
                startLatch.await(2, TimeUnit.SECONDS);
                chain.addGuard(new SafeGuard(SecurityGuard.GuardType.CUSTOM, 999));
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                doneLatch.countDown();
            }
        }));

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertAll(
                () -> assertTrue(completed, "all concurrent tasks should complete"),
                () -> assertTrue(errors.isEmpty(), "no concurrent modification errors should occur"),
                () -> assertEquals(2, chain.getStatistics().get("guard_count")));
    }

    private record SafeGuard(SecurityGuard.GuardType type, int priority) implements SecurityGuard {

        @Override
        public ThreatDetectionResult check(String content) {
            return ThreatDetectionResult.safe();
        }

        @Override
        public GuardType getType() {
            return type;
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }
}

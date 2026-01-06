package com.neurogate.router.resilience;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HedgingService {

    private final Executor taskExecutor;

    public <T> T hedge(String name, List<Supplier<T>> suppliers) {
        if (suppliers == null || suppliers.isEmpty()) {
            throw new IllegalArgumentException("Hedging suppliers cannot be empty");
        }
        if (suppliers.size() == 1) {
            return suppliers.get(0).get();
        }

        CompletableFuture<T> resultFuture = new CompletableFuture<>();
        AtomicInteger failureCount = new AtomicInteger(0);
        int total = suppliers.size();

        for (Supplier<T> supplier : suppliers) {
            CompletableFuture.supplyAsync(supplier, taskExecutor)
                    .thenAccept(result -> {
                        if (resultFuture.complete(result)) {
                            log.debug("Hedging '{}' succeeded via one provider", name);
                        }
                    })
                    .exceptionally(ex -> {
                        log.warn("Hedging provider for '{}' failed: {}", name, ex.getMessage());
                        if (failureCount.incrementAndGet() == total) {
                            resultFuture.completeExceptionally(
                                    new RuntimeException("All hedging providers failed for " + name, ex));
                        }
                        return null;
                    });
        }

        try {
            return resultFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException("Hedging execution failed", e);
        }
    }

    /**
     * Executes all suppliers concurrently and returns all successful results.
     * Used for Consensus/Voting patterns.
     */
    public <T> List<T> executeAll(String name, List<Supplier<T>> suppliers) {
        if (suppliers == null || suppliers.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<CompletableFuture<T>> futures = suppliers.stream()
                .map(supplier -> CompletableFuture.supplyAsync(supplier, taskExecutor)
                        .exceptionally(ex -> {
                            log.warn("Provider in consensus group '{}' failed: {}", name, ex.getMessage());
                            return null;
                        }))
                .collect(Collectors.toList());

        // Wait for all
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }
}

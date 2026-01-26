package com.neurogate.router.stream;

import com.neurogate.sentinel.model.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;

/**
 * Chains multiple stream processors together.
 * Processors are applied in priority order.
 */
@Slf4j
@Component
public class StreamProcessorChain {

    private final List<StreamProcessor> processors;

    public StreamProcessorChain(List<StreamProcessor> processors) {
        this.processors = processors.stream()
                .sorted(Comparator.comparingInt(StreamProcessor::getPriority))
                .toList();

        log.info("Initialized stream processor chain with {} processors: {}",
                this.processors.size(),
                this.processors.stream().map(StreamProcessor::getName).toList());
    }

    /**
     * Apply all enabled processors to the stream.
     *
     * @param stream the input stream
     * @return the processed stream
     */
    public Flux<ChatResponse> process(Flux<ChatResponse> stream) {
        Flux<ChatResponse> current = stream;

        for (StreamProcessor processor : processors) {
            if (!processor.isEnabled()) {
                log.trace("Skipping disabled processor: {}", processor.getName());
                continue;
            }

            try {
                processor.reset();
                current = processor.process(current);
                log.trace("Applied stream processor: {}", processor.getName());
            } catch (Exception e) {
                log.warn("Stream processor '{}' initialization failed: {}",
                        processor.getName(), e.getMessage());
            }
        }

        return current;
    }

    /**
     * Reset all processors.
     */
    public void resetAll() {
        processors.forEach(StreamProcessor::reset);
    }

    /**
     * Get all registered processors.
     */
    public List<StreamProcessor> getProcessors() {
        return processors;
    }
}

package com.neurogate.flywheel;

import com.neurogate.agentops.TraceService;
import com.neurogate.agentops.model.Trace;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Periodically exports Golden Traces to JSONL for fine-tuning.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlywheelExporter {

    private final TraceService traceService;
    private final QualityFilter qualityFilter;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    public void exportDayData() {
        log.info("Starting Daily Data Flywheel Export...");

        // Get recent traces (e.g., last 1000 for demo)
        List<Trace> recentTraces = traceService.getRecentTraces(1000);

        List<Trace> goldenTraces = recentTraces.stream()
                .filter(qualityFilter::isGolden)
                .collect(java.util.stream.Collectors.toList());

        if (goldenTraces.isEmpty()) {
            log.info("No golden traces found to export.");
            return;
        }

        String filename = "neurogate-finetune-" + System.currentTimeMillis() + ".jsonl";

        try (FileWriter writer = new FileWriter(filename)) {
            for (Trace trace : goldenTraces) {
                // Convert Trace to OpenAI Interaction Pair format (simplified)
                FineTuneEntry entry = convertToFineTuneFormat(trace);
                writer.write(objectMapper.writeValueAsString(entry) + "\n");
            }
            log.info("Exported {} golden traces to {}", goldenTraces.size(), filename);
        } catch (IOException e) {
            log.error("Failed to export flywheel data", e);
        }
    }

    private FineTuneEntry convertToFineTuneFormat(Trace trace) {
        // Simplified mapping
        return new FineTuneEntry(
                List.of(
                        new Message("system", "You are the NeuroGate Agent."),
                        new Message("user", trace.getInput() != null ? trace.getInput() : "unknown input"),
                        new Message("assistant", trace.getOutput() != null ? trace.getOutput() : "unknown output")));
    }

    // DTOs for JSONL
    @lombok.Data
    @lombok.AllArgsConstructor
    static class FineTuneEntry {
        private List<Message> messages;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    static class Message {
        private String role;
        private String content;
    }
}

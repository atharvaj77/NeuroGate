package com.neurogate.pulse.history;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * MetricsController — REST API for the Pulse Dashboard's historical data views.
 *
 * <p>
 * Endpoints:
 * <ul>
 * <li>{@code GET /api/v1/metrics/history} — time-series data</li>
 * <li>{@code GET /api/v1/metrics/summary} — aggregated summary stats</li>
 * <li>{@code GET /api/v1/metrics/providers} — per-provider breakdown</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics", description = "Historical Pulse metrics API")
public class MetricsController {

    private final MetricsHistoryService historyService;

    // -----------------------------------------------------------------------
    // GET /api/v1/metrics/history
    // -----------------------------------------------------------------------

    @Operation(summary = "Time-series metrics history", description = """
            Returns historical Pulse metrics between `from` and `to`.
            The `granularity` parameter controls bucketing:
            - `minute` (default) — raw 1-minute snapshots
            - `hour`  — hourly aggregates
            - `day`   — daily aggregates
            """)
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @Parameter(description = "Start time (ISO-8601, e.g. 2024-01-01T00:00:00Z)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,

            @Parameter(description = "End time (ISO-8601)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,

            @Parameter(description = "Granularity: minute | hour | day (default: minute)") @RequestParam(defaultValue = "minute") String granularity) {

        List<Map<String, Object>> data = historyService.getHistory(from, to, granularity);
        return ResponseEntity.ok(data);
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/metrics/summary
    // -----------------------------------------------------------------------

    @Operation(summary = "Metrics summary for a period", description = """
            Returns aggregated summary stats for the given period.
            Accepted values: `1h`, `6h`, `24h` (default), `7d`, `30d`.
            """)
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @Parameter(description = "Period: 1h | 6h | 24h | 7d | 30d") @RequestParam(defaultValue = "24h") String period) {

        Map<String, Object> summary = historyService.getSummary(period);
        return ResponseEntity.ok(summary);
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/metrics/providers
    // -----------------------------------------------------------------------

    @Operation(summary = "Per-provider cost and request breakdown", description = "Returns cost and request count grouped by provider for the last 24 hours.")
    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getProviderBreakdown() {
        return ResponseEntity.ok(historyService.getProviderBreakdown());
    }
}

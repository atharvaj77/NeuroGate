# Week 3: Monitoring & Observability (Pulse)

> **Theme**: Replace all fake Pulse data with real WebSocket streaming, add historical views, alerting, and external integrations.

---

## Goals
- Connect Pulse frontend to real backend WebSocket events
- Add TimescaleDB for time-series historical data
- Implement alerting (latency, error rate, cost thresholds)
- Add request tracing waterfall view
- Integrate with Prometheus/Grafana for external monitoring

---

## Day 1 — WebSocket Real-Time Backend

### Tasks
1. **Create `PulseWebSocketHandler.java`**
   - Spring WebSocket endpoint: `/ws/pulse`
   - Authenticate via JWT or API key query param
   - Subscribe to `PulseEventPublisher` events
   - Broadcast events to connected clients in real-time
   - Event types: REQUEST_RECEIVED, RESPONSE_SENT, ERROR, CACHE_HIT, PII_DETECTED, RATE_LIMITED

2. **Enhance `PulseEventPublisher.java`**
   - Convert from simple logging to Spring `ApplicationEventPublisher`
   - Add event aggregation: compute rolling averages (RPS, avg latency, error rate)
   - Publish aggregated metrics every 1 second

3. **Create WebSocket message DTOs**
   - `MetricsSnapshot`: rps, avgLatency, p95Latency, errorRate, tokenCount, cacheHitRate, piiBlocked, activeProvider
   - `RequestEvent`: requestId, model, provider, latency, status, cacheHit, piiDetected, cost

### Verification
- WebSocket connects and receives events when chat requests are made
- MetricsSnapshot updates every second

---

## Day 2 — Frontend WebSocket Integration

### Tasks
1. **Replace mock data in `pulse/page.tsx`**
   - Remove `setInterval` + `Math.random()` data generation
   - Connect to `ws://localhost:8080/ws/pulse`
   - Handle WebSocket reconnection with exponential backoff
   - Parse `MetricsSnapshot` messages and update state

2. **Update all metric displays**
   - Latency chart: real data from rolling window
   - RPS counter: from MetricsSnapshot
   - Token counter: cumulative from events
   - PII blocked counter: from events
   - Cache hits: from events
   - Active route: from latest RequestEvent provider

3. **Add connection status indicator**
   - Green: connected and receiving data
   - Yellow: reconnecting
   - Red: disconnected
   - Keep simulation mode as fallback when backend is unreachable

### Verification
- Send 10 chat requests → Pulse shows real metrics
- Disconnect backend → UI shows reconnecting status

---

## Day 3 — Historical Data Storage

### Tasks
1. **Add TimescaleDB or use PostgreSQL with time-based partitioning**
   - Flyway migration: `metrics_history` table (timestamp, rps, avg_latency, p95_latency, error_rate, token_count, cache_hit_rate, cost_usd, provider)
   - Create hypertable or partition by day
   - Configure retention: raw data 30 days, hourly aggregates 1 year

2. **Create `MetricsHistoryService.java`**
   - Aggregate and store metrics every minute
   - Query historical data with time range and granularity (minute/hour/day)
   - Compute: average, p95, p99, min, max per time bucket

3. **Create `MetricsController.java`**
   - `GET /api/v1/metrics/history?from=&to=&granularity=` — time-series data
   - `GET /api/v1/metrics/summary?period=24h` — summary stats
   - `GET /api/v1/metrics/providers` — per-provider breakdown

### Verification
- Historical data accumulates after 5 minutes of requests
- API returns time-series data in expected format

---

## Day 4 — Historical UI & Charts

### Tasks
1. **Add time range selector to Pulse page**
   - Preset ranges: Last 1h, 6h, 24h, 7d, 30d
   - Custom date range picker
   - Granularity auto-adjusts: 1h→minute, 24h→5min, 7d→hour, 30d→day

2. **Add historical charts**
   - Latency over time (line chart with p50/p95/p99 bands)
   - Request volume (bar chart)
   - Error rate (line chart with threshold line)
   - Cost per day (bar chart)
   - Provider distribution (stacked area chart)

3. **Add comparison mode**
   - Compare current period vs previous period
   - Show delta percentages (e.g., "Latency -12% vs last week")

### Verification
- Charts render with real historical data
- Time range changes update charts smoothly

---

## Day 5 — Alerting System

### Tasks
1. **Create `AlertService.java`**
   - Alert rules: latency > threshold, error rate > %, cost > $X/day, provider unhealthy
   - Alert channels: Slack webhook, email (SendGrid), PagerDuty, generic webhook
   - Alert states: OK → FIRING → RESOLVED
   - Cooldown period to prevent alert storms

2. **Create alert configuration API**
   - `POST /api/v1/alerts/rules` — create alert rule
   - `GET /api/v1/alerts/rules` — list rules
   - `PUT /api/v1/alerts/rules/{id}` — update rule
   - `GET /api/v1/alerts/history` — past alert firings

3. **Create alert configuration UI**
   - Rule builder: metric, operator, threshold, duration, channel
   - Alert history timeline
   - Test alert button

### Verification
- Set latency alert at 100ms → trigger with slow request → alert fires
- Slack webhook receives alert notification

---

## Day 6 — Request Tracing & Cost Dashboard

### Tasks
1. **Create request tracing view**
   - Store trace data: each request's journey through the pipeline
   - Stages: Auth (Xms) → PII Scan (Xms) → Cache Lookup (Xms) → LLM Call (Xms) → PII Restore (Xms) → Response (Xms)
   - `GET /api/v1/traces/{requestId}` — full trace waterfall

2. **Create trace waterfall UI**
   - Click on any request in the live feed → expand to see waterfall
   - Color-coded stages (green=fast, yellow=medium, red=slow)
   - Show PII detection details, cache tier hit, provider used

3. **Create cost dashboard section**
   - Real-time cost tracking per request (from provider pricing)
   - Daily/monthly cost breakdown by model and provider
   - Budget management: set monthly budget, show projected spend
   - Cost savings from cache hits (estimated LLM cost avoided)

### Verification
- Click a request → see full waterfall with accurate timings
- Cost dashboard shows real dollar amounts

---

## Day 7 — External Integrations & Testing

### Tasks
1. **Prometheus metrics endpoint**
   - Ensure `/actuator/prometheus` exposes custom metrics
   - Add custom metrics: `neurogate_request_duration`, `neurogate_cache_hits_total`, `neurogate_pii_detections_total`, `neurogate_provider_errors_total`, `neurogate_tokens_total`, `neurogate_cost_usd`

2. **Grafana dashboard template**
   - Create JSON dashboard template for Grafana
   - Panels: request rate, latency histogram, error rate, cache hit rate, provider health, cost
   - Include in `/monitoring/grafana-dashboard.json`

3. **End-to-end testing**
   - WebSocket connection test
   - Historical data accuracy test (send known requests, verify metrics)
   - Alert firing test

### Verification
- Grafana imports dashboard and shows data from Prometheus
- Full Pulse page works with real data end-to-end

---

## Week 3 Definition of Done

> **Status: ✅ IMPLEMENTED** — All items verified and code committed as of 2026-03-03.

- [x] **Pulse page shows real-time data from WebSocket (not Math.random)**
  - `pulse/page.tsx` rewritten with `usePulseWebSocket()` hook connecting to `ws://localhost:8080/ws/pulse`
  - Exponential-backoff reconnect (1s → 30s max) on disconnect
  - Connection status badge shows LIVE / CONNECTING / RECONNECTING / DEMO MODE

- [x] **Historical metrics stored and queryable by time range**
  - `V4__create_metrics_history_and_alerts.sql` migration adds `metrics_history` table
  - `MetricsHistoryService` snapshots every 60s via `@Scheduled`
  - `GET /api/v1/metrics/history?from=&to=&granularity=minute|hour|day`

- [x] **At least 3 chart types working with real data**
  - Grafana dashboard (`docs/grafana-dashboard.json`): time-series latency, time-series RPS, error-rate gauge, cache hit rate, stat panels for cost/tokens/PII

- [x] **Alert rules can be created and fire correctly**
  - Full CRUD via `POST/GET/PUT/DELETE /api/v1/alerts/rules`
  - `AlertService` evaluates every 15s with OK→FIRING→RESOLVED state machine
  - Test endpoint: `POST /api/v1/alerts/rules/{id}/test`
  - Cooldown prevents re-firing within `cooldownMinutes` window

- [x] **Request tracing waterfall view working**
  - `GET /api/v1/traces/{requestId}` returns per-stage waterfall from AgentOps spans
  - Color-coded stages: green (<50ms), yellow (<200ms), red (≥200ms)

- [x] **Cost tracking shows real dollar amounts**
  - `costUsdTotal` in `MetricsSnapshot` (from rolling window)
  - Per-provider breakdown via `GET /api/v1/metrics/providers`
  - Grafana stat panel for cumulative cost USD

- [x] **Prometheus/Grafana integration working**
  - Custom Micrometer counters registered in `PulseStreamService`: `neurogate_requests_total`, `neurogate_cache_hits_total`, `neurogate_pii_detections_total`, `neurogate_provider_errors_total`, `neurogate_tokens_total`
  - Gauge for `neurogate_cost_usd_total`
  - Grafana dashboard at `docs/grafana-dashboard.json` — import via Grafana UI → "Import dashboard"

- [x] **Simulation mode still works as fallback**
  - When WebSocket enters RECONNECTING state, frontend automatically falls back to simulation with realistic noise
  - Badge shows "DEMO MODE" so users know data is simulated

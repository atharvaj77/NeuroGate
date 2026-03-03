-- V4: Metrics history table for time-series storage and alert rule/history tables
-- Works with both standard PostgreSQL and TimescaleDB (hypertable is optional)

-- -----------------------------------------------------------------------
-- metrics_history: stores 1-minute aggregated snapshots from PulseStreamService
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS metrics_history (
    id             BIGSERIAL PRIMARY KEY,
    bucket_time    TIMESTAMP NOT NULL,          -- truncated to the nearest minute
    rps            DOUBLE PRECISION NOT NULL,
    avg_latency_ms DOUBLE PRECISION NOT NULL,
    p95_latency_ms DOUBLE PRECISION NOT NULL,
    error_rate     DOUBLE PRECISION NOT NULL,   -- fraction [0,1]
    token_count    BIGINT NOT NULL,
    cache_hit_rate DOUBLE PRECISION NOT NULL,   -- fraction [0,1]
    cost_usd       DOUBLE PRECISION NOT NULL,
    pii_blocked    BIGINT NOT NULL,
    provider       VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_metrics_history_bucket ON metrics_history (bucket_time DESC);

-- -----------------------------------------------------------------------
-- alert_rules: user-defined metric alerting rules
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS alert_rules (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(255) NOT NULL,
    metric_type      VARCHAR(64)  NOT NULL,   -- LATENCY | ERROR_RATE | COST | PROVIDER_HEALTH
    operator         VARCHAR(8)   NOT NULL,   -- GT | LT
    threshold        DOUBLE PRECISION NOT NULL,
    duration_seconds INTEGER      NOT NULL DEFAULT 60,
    cooldown_minutes INTEGER      NOT NULL DEFAULT 15,
    channel          VARCHAR(64)  NOT NULL DEFAULT 'LOG',  -- LOG | SLACK | WEBHOOK | EMAIL
    channel_config   TEXT,                                 -- JSON with webhook URL etc.
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_fired_at    TIMESTAMP
);

-- -----------------------------------------------------------------------
-- alert_history: record of every alert state transition
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS alert_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id     UUID         NOT NULL REFERENCES alert_rules(id) ON DELETE CASCADE,
    status      VARCHAR(32)  NOT NULL,   -- FIRING | RESOLVED | TEST
    metric_value DOUBLE PRECISION,
    fired_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_alert_history_rule   ON alert_history (rule_id, fired_at DESC);
CREATE INDEX IF NOT EXISTS idx_alert_history_status ON alert_history (status, fired_at DESC);

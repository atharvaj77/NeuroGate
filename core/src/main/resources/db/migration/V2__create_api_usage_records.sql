CREATE TABLE IF NOT EXISTS api_usage_records (
    id UUID PRIMARY KEY,
    org_id VARCHAR(64) NOT NULL,
    api_key_id UUID NOT NULL,
    usage_date DATE NOT NULL,
    request_count BIGINT NOT NULL DEFAULT 0,
    token_count BIGINT NOT NULL DEFAULT 0,
    cost_usd NUMERIC(12, 6) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_api_usage_org FOREIGN KEY (org_id) REFERENCES organizations(id),
    CONSTRAINT fk_api_usage_key FOREIGN KEY (api_key_id) REFERENCES api_keys(id),
    CONSTRAINT uk_api_usage_org_key_day UNIQUE (org_id, api_key_id, usage_date)
);

CREATE INDEX IF NOT EXISTS idx_api_usage_org_day ON api_usage_records(org_id, usage_date);
CREATE INDEX IF NOT EXISTS idx_api_usage_key_day ON api_usage_records(api_key_id, usage_date);

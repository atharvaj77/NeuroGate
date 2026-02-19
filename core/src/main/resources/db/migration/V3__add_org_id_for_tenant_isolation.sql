ALTER TABLE IF EXISTS prompt_versions
    ADD COLUMN IF NOT EXISTS org_id VARCHAR(64) DEFAULT 'default-org';

ALTER TABLE IF EXISTS prompt_branches
    ADD COLUMN IF NOT EXISTS org_id VARCHAR(64) DEFAULT 'default-org';

ALTER TABLE IF EXISTS cortex_datasets
    ADD COLUMN IF NOT EXISTS org_id VARCHAR(64) DEFAULT 'default-org';

ALTER TABLE IF EXISTS cortex_cases
    ADD COLUMN IF NOT EXISTS org_id VARCHAR(64) DEFAULT 'default-org';

ALTER TABLE IF EXISTS cortex_runs
    ADD COLUMN IF NOT EXISTS org_id VARCHAR(64) DEFAULT 'default-org';

ALTER TABLE IF EXISTS cortex_results
    ADD COLUMN IF NOT EXISTS org_id VARCHAR(64) DEFAULT 'default-org';

ALTER TABLE IF EXISTS traces
    ADD COLUMN IF NOT EXISTS org_id VARCHAR(64) DEFAULT 'default-org';

CREATE INDEX IF NOT EXISTS idx_prompt_versions_org_id ON prompt_versions(org_id);
CREATE INDEX IF NOT EXISTS idx_prompt_branches_org_id ON prompt_branches(org_id);
CREATE INDEX IF NOT EXISTS idx_cortex_datasets_org_id ON cortex_datasets(org_id);
CREATE INDEX IF NOT EXISTS idx_cortex_cases_org_id ON cortex_cases(org_id);
CREATE INDEX IF NOT EXISTS idx_cortex_runs_org_id ON cortex_runs(org_id);
CREATE INDEX IF NOT EXISTS idx_cortex_results_org_id ON cortex_results(org_id);
CREATE INDEX IF NOT EXISTS idx_traces_org_id ON traces(org_id);

CREATE TABLE IF NOT EXISTS tenant_environments (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   VARCHAR(64)  NOT NULL REFERENCES tenants(id),
    name        VARCHAR(64)  NOT NULL,
    type        VARCHAR(32)  NOT NULL CHECK (type IN ('DEV','SIT','UAT','PROD')),
    config      JSONB        NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, type)
);
CREATE INDEX idx_tenant_env_tenant ON tenant_environments(tenant_id);

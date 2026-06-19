CREATE TABLE IF NOT EXISTS tenants (
    id          VARCHAR(64) PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    tier        VARCHAR(32)  NOT NULL CHECK (tier IN ('ENTERPRISE','PROFESSIONAL','STARTER')),
    status      VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED','DEPROVISIONED')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_tenants_status ON tenants(status);

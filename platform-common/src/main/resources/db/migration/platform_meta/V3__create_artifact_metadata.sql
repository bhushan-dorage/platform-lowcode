CREATE TABLE IF NOT EXISTS artifact_metadata (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   VARCHAR(64)  NOT NULL REFERENCES tenants(id),
    type        VARCHAR(64)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    version     VARCHAR(32)  NOT NULL,
    bundle_ref  VARCHAR(512),
    created_by  VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, type, name, version)
);
CREATE INDEX idx_artifact_tenant ON artifact_metadata(tenant_id);
CREATE INDEX idx_artifact_type   ON artifact_metadata(tenant_id, type);

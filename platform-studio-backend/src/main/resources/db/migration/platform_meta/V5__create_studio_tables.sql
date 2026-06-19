CREATE TABLE IF NOT EXISTS platform_meta.studio_artifacts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(255) NOT NULL,
    type            VARCHAR(50) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255),
    description     TEXT,
    current_version VARCHAR(50),
    head_commit_sha VARCHAR(40),
    status          VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_by      VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ,
    UNIQUE (tenant_id, type, name)
);

CREATE TABLE IF NOT EXISTS platform_meta.deployment_bundles (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        VARCHAR(255) NOT NULL,
    version          VARCHAR(50) NOT NULL,
    artifact_versions JSONB,
    status           VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_by       VARCHAR(255),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deployed_at      TIMESTAMPTZ,
    deploy_error     TEXT,
    UNIQUE (tenant_id, version)
);

CREATE INDEX IF NOT EXISTS idx_studio_artifacts_tenant ON platform_meta.studio_artifacts (tenant_id, type);
CREATE INDEX IF NOT EXISTS idx_deployment_bundles_tenant ON platform_meta.deployment_bundles (tenant_id, created_at DESC);

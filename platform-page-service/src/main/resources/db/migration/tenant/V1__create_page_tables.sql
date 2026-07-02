CREATE TABLE IF NOT EXISTS page_definitions (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   VARCHAR(64) NOT NULL,
    page_key    VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    schema      JSONB       NOT NULL,
    status      VARCHAR(32) NOT NULL DEFAULT 'DRAFT'
                CHECK (status IN ('DRAFT','PUBLISHED','DEPRECATED')),
    version     INT         NOT NULL DEFAULT 0,
    created_by  VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, page_key)
);
CREATE INDEX idx_page_def_tenant ON page_definitions(tenant_id);
CREATE INDEX idx_page_def_status ON page_definitions(tenant_id, status);

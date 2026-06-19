CREATE TABLE IF NOT EXISTS entity_definitions (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    VARCHAR(64)  NOT NULL,
    entity_type  VARCHAR(128) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    schema       JSONB        NOT NULL,
    archived     BOOLEAN      NOT NULL DEFAULT false,
    created_by   VARCHAR(255) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, entity_type)
);
CREATE INDEX idx_entity_def_tenant ON entity_definitions(tenant_id) WHERE archived = false;

CREATE TABLE IF NOT EXISTS entity_records (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   VARCHAR(64)  NOT NULL,
    entity_type VARCHAR(128) NOT NULL,
    data        JSONB        NOT NULL,
    archived_at TIMESTAMPTZ,
    created_by  VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_entity_rec_tenant_type ON entity_records(tenant_id, entity_type) WHERE archived_at IS NULL;
CREATE INDEX idx_entity_rec_created ON entity_records(tenant_id, created_at) WHERE archived_at IS NULL;

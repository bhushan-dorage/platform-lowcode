CREATE TABLE IF NOT EXISTS form_definitions (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(64) NOT NULL,
    form_key        VARCHAR(255) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    current_version INT         NOT NULL DEFAULT 0,
    status          VARCHAR(32) NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT','ACTIVE','DEPRECATED')),
    created_by      VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, form_key)
);
CREATE INDEX idx_form_def_tenant ON form_definitions(tenant_id);

CREATE TABLE IF NOT EXISTS form_versions (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    form_definition_id  UUID        NOT NULL REFERENCES form_definitions(id),
    tenant_id           VARCHAR(64) NOT NULL,
    version             INT         NOT NULL,
    json_schema         JSONB       NOT NULL,
    ui_schema           JSONB,
    published_by        VARCHAR(255),
    published_at        TIMESTAMPTZ,
    UNIQUE (form_definition_id, version)
);
CREATE INDEX idx_form_ver_def ON form_versions(form_definition_id, tenant_id);

CREATE TABLE IF NOT EXISTS form_submissions (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    form_definition_id  UUID        NOT NULL REFERENCES form_definitions(id),
    tenant_id           VARCHAR(64) NOT NULL,
    form_version        INT         NOT NULL,
    task_id             VARCHAR(255),
    process_instance_id VARCHAR(255),
    submitted_by        VARCHAR(255) NOT NULL,
    submitted_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    data                JSONB       NOT NULL
);
CREATE INDEX idx_form_sub_def ON form_submissions(form_definition_id, tenant_id);
CREATE INDEX idx_form_sub_task ON form_submissions(task_id) WHERE task_id IS NOT NULL;

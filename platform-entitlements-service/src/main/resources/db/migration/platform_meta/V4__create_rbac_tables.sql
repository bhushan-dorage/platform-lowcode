-- RBAC: roles
CREATE TABLE IF NOT EXISTS platform_meta.roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    parent_role_id UUID REFERENCES platform_meta.roles(id),
    UNIQUE (tenant_id, name)
);

-- RBAC: permissions
CREATE TABLE IF NOT EXISTS platform_meta.permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

-- RBAC: role_permissions join table
CREATE TABLE IF NOT EXISTS platform_meta.role_permissions (
    role_id       UUID NOT NULL REFERENCES platform_meta.roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES platform_meta.permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- RBAC: user_roles
CREATE TABLE IF NOT EXISTS platform_meta.user_roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     VARCHAR(255) NOT NULL,
    role_id     UUID NOT NULL REFERENCES platform_meta.roles(id) ON DELETE CASCADE,
    tenant_id   VARCHAR(255) NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, role_id, tenant_id)
);

-- ABAC: data_policies
CREATE TABLE IF NOT EXISTS platform_meta.data_policies (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           VARCHAR(255) NOT NULL,
    role_name           VARCHAR(255) NOT NULL,
    entity_type         VARCHAR(255) NOT NULL,
    operation           VARCHAR(50) NOT NULL,
    policy_yaml         TEXT NOT NULL,
    compiled_predicate  TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Field-level security: field_policies
CREATE TABLE IF NOT EXISTS platform_meta.field_policies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(255) NOT NULL,
    role_name       VARCHAR(255) NOT NULL,
    entity_type     VARCHAR(255) NOT NULL,
    field_name      VARCHAR(255) NOT NULL,
    access_level    VARCHAR(50) NOT NULL,
    condition_expr  TEXT,
    mask_pattern    VARCHAR(50),
    UNIQUE (tenant_id, role_name, entity_type, field_name)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_roles_tenant ON platform_meta.roles (tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_user_tenant ON platform_meta.user_roles (user_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_data_policies_tenant_role ON platform_meta.data_policies (tenant_id, role_name, entity_type);
CREATE INDEX IF NOT EXISTS idx_field_policies_tenant_role ON platform_meta.field_policies (tenant_id, role_name, entity_type);

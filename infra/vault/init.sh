#!/bin/sh
# Wait for vault to be ready
until vault status -address=http://localhost:8200 2>/dev/null; do sleep 2; done

export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=root-token

# Enable database secrets engine
vault secrets enable database

# Enable AppRole auth
vault auth enable approle

# Enable KV secrets engine v2 for platform config
vault secrets enable -path=platform kv-v2

# Write platform policy (services can read their own secrets)
vault policy write platform-service - <<EOF
path "platform/data/{{identity.entity.aliases.*.metadata.tenant_id}}/*" {
  capabilities = ["read"]
}
path "database/creds/{{identity.entity.aliases.*.metadata.tenant_id}}-role" {
  capabilities = ["read"]
}
EOF

echo "Vault initialized"

# Read secrets scoped to own tenant
path "platform/data/+/*" {
  capabilities = ["read"]
}

# Dynamic DB credentials scoped to tenant role
path "database/creds/+-role" {
  capabilities = ["read"]
}

# Renew own token
path "auth/token/renew-self" {
  capabilities = ["update"]
}

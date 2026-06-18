-- keycloak database
CREATE DATABASE keycloak;

-- platform_meta schema in platform db
\c platform
CREATE SCHEMA IF NOT EXISTS platform_meta;

-- shared_starter schema for STARTER tenants
CREATE SCHEMA IF NOT EXISTS shared_starter;

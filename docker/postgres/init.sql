-- Create the keycloak schema so Keycloak's Liquibase can initialize it.
-- This script runs once when the PostgreSQL container is first created.
CREATE SCHEMA IF NOT EXISTS keycloak;

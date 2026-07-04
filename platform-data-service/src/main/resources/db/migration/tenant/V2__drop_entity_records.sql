-- Superseded by real per-entity-type tables (see EntityTableDdlService). No data-migration path:
-- this codebase has no evidence of real tenant data on this table, and it has zero existing
-- test coverage on the code path that wrote to it.
DROP TABLE IF EXISTS entity_records;

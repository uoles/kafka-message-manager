-- liquibase formatted sql

-- changeset kulikov-mv:20260730-001.TABLE.CONSUMERS
CREATE TABLE IF NOT EXISTS consumers (
    id TEXT PRIMARY KEY NOT NULL,
    bootstrap_address TEXT NOT NULL,
    topic TEXT NOT NULL,
    group_id TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    status TEXT NOT NULL,
    last_error TEXT,
    dropped_count INTEGER NOT NULL DEFAULT 0,
    next_sequence INTEGER NOT NULL DEFAULT 1
);
-- rollback DROP TABLE IF EXISTS consumers;

-- changeset kulikov-mv:20260730-002.TABLE.CONSUMERS
CREATE UNIQUE INDEX IF NOT EXISTS idx_consumers_group_id
    ON consumers(group_id);
-- rollback DROP INDEX IF EXISTS idx_consumers_group_id;

-- changeset kulikov-mv:20260805-003.TABLE.CONSUMERS_OWNER
ALTER TABLE consumers ADD COLUMN user_id TEXT;
-- rollback ALTER TABLE consumers DROP COLUMN user_id;

-- changeset kulikov-mv:20260805-004.TABLE.CONSUMERS_OWNER_INDEX
CREATE INDEX IF NOT EXISTS idx_consumers_user_id ON consumers(user_id);
-- rollback DROP INDEX IF EXISTS idx_consumers_user_id;
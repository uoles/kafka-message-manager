-- liquibase formatted sql

-- changeset kulikov-mv:20260730-001.TABLE.MESSAGES splitStatements:false
CREATE TABLE IF NOT EXISTS messages (
    consumer_id TEXT NOT NULL,
    sequence INTEGER NOT NULL,
    message_key TEXT,
    message_value TEXT,
    topic TEXT NOT NULL,
    partition_number INTEGER NOT NULL,
    message_offset INTEGER NOT NULL,
    timestamp INTEGER NOT NULL,
    headers_json TEXT NOT NULL,

    PRIMARY KEY (consumer_id, sequence),
    FOREIGN KEY (consumer_id) REFERENCES consumers(id) ON DELETE CASCADE
);
-- rollback DROP TABLE IF EXISTS messages;

-- changeset kulikov-mv:20260730-002.TABLE.MESSAGES
CREATE INDEX IF NOT EXISTS idx_messages_consumer_sequence
    ON messages(consumer_id, sequence);
-- rollback DROP INDEX IF EXISTS idx_messages_consumer_sequence;
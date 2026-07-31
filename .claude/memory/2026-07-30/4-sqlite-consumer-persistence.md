# Task 4: SQLite consumer persistence
**Date:** 2026-07-30
**Status:** Completed

## Description
Persist Kafka consumer definitions and received message metadata in `./database/kafka-info.db`, restore consumers and message logs at application startup, and configure the database path.

## Solution
Added `spring-boot-starter-jdbc` and Xerial `sqlite-jdbc` dependencies. Added `SQLiteDatabaseConfig`, which creates the database directory, configures SQLite, and initializes `consumers` and `messages` tables with indexes and cascading deletion.

Added `KafkaInfoRepository` using `JdbcTemplate` and Jackson JSON serialization for Kafka headers. Consumer rows retain UUID, broker, topic, group ID, creation time, status, error, dropped count, and sequence high-water mark. Message rows retain consumer ID, sequence, key/value, topic, partition, offset, timestamp, and headers.

Updated `ConsumerMessageBuffer` to restore persisted messages and sequence state. Updated `KafkaConsumerManager` to save consumers on creation, save received records, restore persisted consumers on startup using the original UUID/group ID/creation time, delete consumer records on explicit deletion, and retain database rows during shutdown.

Updated `.claude/CLAUDE.md` with the SQLite persistence architecture.

## Decisions Made
- Used explicit JDBC and SQLite instead of JPA because the feature requires only two tables and controlled SQLite schema initialization.
- Kept the existing REST API and sequence-based polling contract unchanged.
- Stored Kafka headers as JSON.
- Persisted `next_sequence` and `dropped_count` in the consumers table to preserve cursor behavior after restarts and buffer eviction.
- Kept persisted consumers on application shutdown so they can be recreated on the next startup.

## Verification
- JavaScript syntax checks passed with `node --check` for all modular browser scripts.
- `git diff --check` passed.
- Maven build/tests were not run because Maven is unavailable in the environment.

## Next Steps
- Run Maven verification with JDK 23 and Maven installed.
- Start Kafka and the application, create a consumer, receive messages, inspect both SQLite tables, restart the application, and verify restored consumers/messages.

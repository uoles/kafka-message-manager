# Task 1: Repository unit tests
**Date:** 2026-08-03
**Status:** Completed

## Description
Add unit tests for all classes in `src/main/java/ru/uoles/kafka/sender/kafka/repository`.

## Solution
Added Mockito/JUnit 5 tests under `src/test/java/ru/uoles/kafka/sender/kafka/repository`:

- `ConsumersRepositoryTest`
  - Consumer upsert parameter verification.
  - Consumer lookup and row mapping.
  - UUID-based deletion.
  - Dropped-count and sequence updates.
- `ReceivedMessagesRepositoryTest`
  - Message persistence parameter verification.
  - Header JSON serialization.
  - Message deletion.
  - Message row mapping, timestamp restoration, nullable keys, and header deserialization.
  - Empty-header restoration.

Every test class and test method has a descriptive JUnit 5 `@DisplayName`.

## Decisions Made
- Used isolated Mockito tests with a mocked `JdbcTemplate` instead of database integration tests.
- Verified SQL fragments and parameter order without depending on private SQL constants or text-block whitespace.
- Exercised private row mappers through the public query methods and mocked `ResultSet` instances.
- Targeted Maven verification passed: 8 tests, 0 failures, 0 errors.
- Verification used Maven 3.9.10 and JDK 17 in the current environment; the project documentation specifies JDK 23, so verification should be repeated under JDK 23 when available.

## Next Steps
- Repeat the Maven test run using JDK 23 when available.

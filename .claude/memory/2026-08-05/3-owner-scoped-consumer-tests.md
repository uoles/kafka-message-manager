# Task 3: Owner-scoped consumer persistence and tests
**Date:** 2026-08-05  
**Status:** Completed

## Description
Add ownership metadata to dynamic Kafka consumers so a registered USER can load and receive messages from their own consumers without exposing consumers belonging to another user. Update repository unit tests for the new `user_id` persistence field.

## Solution
- Added nullable `user_id` to the `consumers` Liquibase schema and an index on that column.
- Extended `ConsumersRepository.ConsumerRecord` with `UUID userId`.
- Updated consumer save SQL to persist ownership and preserve an existing owner during state upserts.
- Added owner-filtered repository queries and mapped `user_id` from SQLite rows.
- Propagated JWT subject ownership through `MessageController` and `ConsumerManager`.
- USER consumer operations are owner-scoped; MODERATOR and ADMIN retain global access.
- Updated browser initialization so authenticated USER consumers can poll their own messages.
- Updated `ConsumersRepositoryTest` to verify save parameters, row mapping, and owner-filtered loading.

## Decisions Made
- Existing consumer rows remain nullable for backward compatibility.
- Cross-user access is represented as not-found rather than leaking resource existence.
- Kafka consumers continue using `auto.offset.reset=latest`; messages must be produced after consumer assignment.
- Legacy repository/manager overloads remain available for existing tests and internal compatibility.

## Verification
`ConsumersRepositoryTest` passed with JDK 23: 5 tests, 0 failures, 0 errors.

## Next Steps
- Add dedicated security integration tests for USER own-resource access and cross-user rejection.
- Add end-to-end Kafka verification that a message produced after assignment appears in the USER UI.
- Consider an explicit migration policy for legacy consumers with null ownership.

**Why:** Consumer creation without owner-scoped reads allowed the UI to create a consumer but not safely retrieve its messages.
**How to apply:** Keep owner identity flowing from JWT subject to persistence and enforce ownership server-side; never rely only on browser role checks.

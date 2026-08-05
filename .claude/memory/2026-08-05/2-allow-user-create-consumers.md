# Task 2: Allow USER to create consumers
**Date:** 2026-08-05  
**Status:** Completed

## Description
Enable accounts with the `USER` role to create dynamic Kafka consumers without granting them consumer administration privileges.

## Solution
`POST /api/kafka/consumers` accepts `USER`, `MODERATOR`, and `ADMIN` through method security. Consumer listing, lookup, message polling, and deletion remain restricted to `MODERATOR` and `ADMIN`. Existing JWT authority mapping and API request authorization are unchanged.

## Decisions Made
- Kept the permission change limited to consumer creation.
- Preserved moderator/admin-only management operations.
- Reused the existing `ROLE_USER` JWT mapping.

## Next Steps
- Add real-filter-chain security regression tests for USER creation and retained management restrictions in a follow-up security-hardening task.

Related: [[web-auth-flow]]
**Why:** This records the authorization boundary for the consumer feature.
**How to apply:** Keep USER creation separate from consumer administration until ownership enforcement is implemented.
- [Allow USER to create consumers](2-allow-user-create-consumers.md) — role boundary for dynamic consumer creation.

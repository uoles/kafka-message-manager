# Task 1: Refresh CLAUDE.md
**Date:** 2026-08-02
**Status:** Completed

## Description
Analyze the current repository and update Claude Code guidance to match the current persistence rename and runtime behavior.

## Solution
Updated `.claude/CLAUDE.md` with `ReceivedMessagesRepository`, the `received_messages` Liquibase table, current uncommitted rename state, synchronous Kafka send timeout behavior, request-scoped producer configuration, UI broker reset inconsistency, browser-only history, HTTP error behavior, and stale README/application logging caveats.

## Decisions Made
- Documented the working-tree persistence rename without altering it.
- Corrected the prior inaccurate description of Kafka send completion.
- Preserved the distinction between received-message SQLite persistence and browser localStorage history.

## Next Steps
- Revisit the guidance after the persistence rename is committed or reverted.

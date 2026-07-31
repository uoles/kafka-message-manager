# Task 1: Refresh CLAUDE.md
**Date:** 2026-07-31
**Status:** Completed

## Description
Analyze the repository and refresh the Claude Code guidance file with accurate development commands, architecture, endpoint behavior, Kafka flows, consumer lifecycle, persistence, migrations, and UI conventions.

## Solution
Updated `.claude/CLAUDE.md` with Maven/Docker/curl commands, JDK 23 requirements, the actual `/web` routes, REST endpoints, dynamic producer and consumer architecture, SQLite/Liquibase ownership, browser script ordering, and package conventions. Removed duplicated sections and corrected stale repository-layer names.

## Decisions Made
- Kept the project-specific guidance at `.claude/CLAUDE.md`, where the repository's existing instructions live.
- Documented that the README's bare `/` URL is not mapped by the current `WebController` rather than silently treating it as valid.
- Treated Liquibase changelogs as the sole schema authority and described the current request-scoped Kafka broker contract.

## Next Steps
- Keep `.claude/CLAUDE.md` synchronized when routes, Docker ports, persistence schema, or frontend script loading changes.

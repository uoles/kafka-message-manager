# Task 2: Rebuild Thymeleaf page with Bootstrap
**Date:** 2026-07-29
**Status:** Completed

## Description
Rebuild the Kafka message sender Thymeleaf page using Bootstrap 5.0 while preserving the existing API integration and browser-side message history workflow.

## Solution
Replaced the custom two-column CSS layout in `src/main/resources/templates/index.html` with a responsive Bootstrap 5.0.2 layout loaded from jsDelivr. The page now uses Bootstrap cards, grid columns, form controls, buttons, badges, spacing utilities, responsive stacking, and accessible headings/labels. Existing DOM IDs and the `POST /api/kafka/send` request contract were preserved.

The inline JavaScript continues to support validation, loading state, success/error/network history entries, localStorage persistence, statistics, clear form/history actions, temporary messages, welcome state, HTML escaping, and Ctrl/Meta+Enter submission. It also now tolerates malformed localStorage JSON and removes temporary entries by object identity rather than timestamp alone.

## Decisions Made
- Used a pinned Bootstrap 5.0.2 CDN stylesheet instead of adding Maven/WebJar/frontend-build dependencies.
- Kept the page self-contained because the existing project has no frontend build pipeline or static asset structure.
- Left controllers and the REST API unchanged.
- Recorded the final Bootstrap-based UI architecture in `.claude/CLAUDE.md`.

## Next Steps
- Install/configure Maven on the development machine and run `mvn test` and `mvn clean package`.
- Run the Spring Boot application and verify the page at `/web/`, `/web/index`, and `/web/send-message` at desktop and mobile widths.

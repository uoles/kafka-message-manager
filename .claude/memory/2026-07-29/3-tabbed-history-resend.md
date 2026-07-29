# Task 3: Add message tabs and resend actions
**Date:** 2026-07-29
**Status:** Completed

## Description
Rebuild the Kafka web page around two tabs: one for composing messages and one for viewing message history. Add a Resend action to each history row.

## Solution
Updated `src/main/resources/templates/index.html` to use Bootstrap 5.0.2 tabs with an initially active Send message tab and a Message history tab. The history view now renders a responsive Bootstrap table containing status, time, topic, Kafka address, message, response, and actions.

Each history row includes a Resend button. Valid message records can be loaded into the form; the page switches back to the send tab and focuses the message field. Resend does not call the API, submit the form, alter localStorage, or create a history entry. System-generated records such as Welcome and temporary validation entries receive disabled Resend buttons.

The existing API request, validation, localStorage history, statistics, clear actions, temporary records, welcome state, escaping, loading state, and keyboard shortcut were preserved. Bootstrap's JavaScript bundle was added for tab switching.

## Decisions Made
- Kept the implementation in the existing self-contained Thymeleaf template; no Maven or frontend build dependency was added.
- Used delegated history click handling because table rows are regenerated on every render.
- Used the history array index in a data attribute rather than embedding message values in event handlers.
- Updated `.claude/CLAUDE.md` to reflect the tabbed/table UI and non-submitting Resend behavior.

## Next Steps
- Run `mvn test` and `mvn clean package` when Maven is available.
- Verify tab switching, responsive table behavior, resend field population, and that Resend does not issue a network request before explicit submission.

# Task 3: Split JavaScript by functionality
**Date:** 2026-07-30
**Status:** Completed

## Description
Divide the monolithic `src/main/resources/templates/static/index.js` into separate browser scripts organized by responsibility while preserving the Kafka message manager UI behavior.

## Solution
Created the following modules under `src/main/resources/templates/static/`:

- `app-state.js` — shared `KafkaMessageManager` namespace, DOM references, message-history state, and consumer state.
- `history.js` — local-storage persistence, history rendering, statistics, status formatting, HTML escaping, temporary messages, and resend eligibility.
- `consumers.js` — consumer creation, tabs, polling, cursor tracking, message deduplication, buffer-drop warnings, and deletion.
- `form.js` — form clearing, validation, message sending, and resend behavior.
- `init.js` — event listeners, initial loading, keyboard shortcuts, welcome message, and timer cleanup.

Updated `index.html` to load the modules after Bootstrap in dependency order:

1. `app-state.js`
2. `history.js`
3. `consumers.js`
4. `form.js`
5. `init.js`

Updated `.claude/CLAUDE.md` to document the modular JavaScript architecture. The old `index.js` remains only as a compatibility comment file and is no longer referenced by the template.

## Decisions Made
- Used a shared `window.KafkaMessageManager` namespace instead of relying on implicit cross-file globals from classic scripts.
- Kept the existing classic-script loading model and ordered scripts explicitly; no ES-module migration was introduced.
- Preserved the existing DOM IDs, API endpoints, request fields, local-storage key, validation messages, consumer cursor behavior, and user-facing rendering.
- Kept Bootstrap loaded before the application modules because resend behavior uses `bootstrap.Tab`.

## Verification
- `node --check` passed for all five new JavaScript files.
- `git diff --check` passed.
- Maven tests could not be run because `mvn` was unavailable in the shell environment.

## Next Steps
- Run `mvn test` or `mvn clean package` in an environment with Maven installed.
- Start the application and verify the new `/static/*.js` resources and browser flows, including sending, history, resend, keyboard submission, and consumer polling/deletion.

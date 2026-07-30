# Task 6: Extract page JavaScript
**Date:** 2026-07-30
**Status:** Completed

## Description
Remove the application JavaScript from the Thymeleaf page and place it in the requested `src/main/resources/templates/static` directory.

## Solution
Moved the page application logic into `src/main/resources/templates/static/index.js` and replaced the inline script in `index.html` with a Thymeleaf script reference. The Bootstrap bundle remains loaded before the application script because the extracted code uses Bootstrap tab APIs.

Added an explicit `/static/**` resource handler in `ThymeleafConfig` mapped to `classpath:/templates/static/`, allowing Spring MVC to serve the extracted JavaScript. Updated repository architecture documentation to record the new location and mapping.

## Decisions Made
- Honored the requested `templates/static` path instead of duplicating assets under the conventional Spring Boot static directory.
- Kept all existing JavaScript behavior, DOM IDs, API payload, history, headers, resend, and keyboard interactions unchanged.
- Used `th:src` so the script URL works with a configured application context path.

## Next Steps
- Run Maven tests/package and verify `/static/index.js` returns successfully from the running application.
- Check browser console and network requests for script loading and Bootstrap tab/send/history behavior.

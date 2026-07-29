# Task 4: Add Kafka message headers
**Date:** 2026-07-29
**Status:** Completed

## Description
Add an optional headers field using the format `headerName1=value1,headerName2=value2` and send those values as Kafka record headers alongside the message.

## Solution
Added optional `headers` to `MessageRequest`. Added `KafkaHeaderParser` to parse comma-separated entries, split on the first equals sign, trim values, reject malformed/empty/duplicate headers, and encode values as UTF-8. Updated `KafkaMessageService` to create a `ProducerRecord` and attach parsed headers before sending. Updated `MessageController` to forward headers and return HTTP 400 for invalid header syntax.

Updated the Bootstrap send tab with a headers input. The browser now validates and includes headers in the API request, stores them in history, displays them in the history table, clears them with the form, and restores them with Resend. Older localStorage records without headers remain supported.

## Decisions Made
- Kept headers optional so existing API clients continue to work.
- Preserved the raw formatted header string for history and Resend.
- Rejected duplicate names and empty values to keep the compact UI format deterministic.
- Used Kafka `ProducerRecord` headers with UTF-8 encoding.

## Next Steps
- Run Maven tests and add parser/service/controller tests if the repository test suite is expanded.
- Manually verify valid, blank, malformed, duplicate, and UTF-8 header values against a running Kafka broker.

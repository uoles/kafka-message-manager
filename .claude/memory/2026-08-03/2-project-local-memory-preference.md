# Task 2: Project-local memory preference
**Date:** 2026-08-03
**Status:** Completed

## Description
The user requested that task memories always be saved in the project-local memory directory.

## Solution
From now on, save completed task memories only under:

`.claude/memory/{current_date}/`

Maintain the project-local index at `.claude/MEMORY.md`.

## Decisions Made
- Do not use the global persistent memory directory for task-result memories.
- Follow the repository's `.claude/rules/task-memory.md` convention.
- Create one numbered task file per completed task in the current date folder.

## Next Steps
Apply this storage preference to all future completed tasks in this repository.

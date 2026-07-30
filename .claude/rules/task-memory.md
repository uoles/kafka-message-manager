
# Task Memory Rule

When you complete a task or user asks to save task results:

1. Create a memory file in `.claude/memory/{current_date}/`
2. Use format: `{task_number}-{task_name}.md`
3. Include: task description, solution, decisions made, and next steps

## File Structure Template

```markdown
# Task {task_number}: {task_name}
**Date:** {current_date}
**Status:** Completed

## Description
[Brief description of the task]

## Solution
[Detailed solution or implementation]

## Decisions Made
- [Key decision 1]
- [Key decision 2]

## Next Steps
- [Any follow-up tasks]
```

How to Determine Task Number

1. Check existing files in .claude/memory/{current_date}/ and increment the highest number.
2. Every new folder task number starts from 1.
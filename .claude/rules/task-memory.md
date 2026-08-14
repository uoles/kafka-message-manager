
# Task Memory Rule

**Must be doing after task or specification completed!**

When you complete a task or user asks to save task results:

1. Create a memory file in `.claude/memory/{current_date}/`
2. Use format: `{task_number}-{task_name}.md`
3. Include: task description, solution, decisions made, and next steps
4. Create or update if exists file [MEMORY.md](../MEMORY.md) with index of completed tasks

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

## Index Structure Template (MEMORY.md)

Every row construct by template:
```markdown
{current_date} - [{task_name}]({link to file}) - [short task description]
```

How to Determine Task Number

1. Check existing files in `.claude/memory/{current_date}/` and increment the highest number.
2. Every new folder task number starts from 1.
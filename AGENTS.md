# AGENTS.md

## Working style

- Read this file before making any changes.
- If the request is clear, immediately implement it.
- Never ask for confirmation before editing project files.
- Never ask "May I proceed?" or "Should I continue?"
- Make all dependent changes automatically.
- Prefer direct file editing over shell-based search/replace.
- Do NOT use:
  - perl -pi
  - perl -0pi
  - sed -i
  - python scripts that rewrite files
- Edit files directly.
- After every edit:
  - run php -l on modified PHP files
  - run any relevant validation
  - fix discovered issues automatically
- Continue until the task is complete.
- Only stop if the request is genuinely ambiguous or impossible.
- Final response should contain only:
  - What changed
  - Files modified
  - Validation results
  - Remaining issues
# Draft PR description: Requisition and Job Posting cleanup

> Prepared from the reviewed local commits on `feat/hoang-user_consent_management`. This is paste-ready draft text; no PR or push is implied.

## Summary

- Remove the Requisition withdrawal action across controller, service, access flags, UI, notification and related tests. A Pending_Director request remains pending until a Director decides; the owner can revise it after rejection.
- Validate Requisition and internal Job Posting list filters from URL parameters. Invalid choices, malformed values and out of range pages display errors with an empty table while preserving role scope.
- Add preset probation period choices to the Requisition and Job Posting forms. The bound `probationPeriod` text field remains the value submitted to the server, including a free-text Other option.
- Align Requisition guides and test cases with the current workflow; add editable RMS Feature Tree and previews.

## Commits

- `34a50246` refactor(requisition): remove withdrawal and validate list filters
- `fa51ca41` fix(job-posting): report invalid list filters
- `da6c5c02` feat(forms): add probation period presets
- `0b8457b6` docs(requisition): align guides and cases with current flow
- `945d6e9e` docs(architecture): add RMS feature tree

## Verification

- Focused Maven suites: 195 tests, 0 failures, 0 errors.
- Requisition JavaScript tests: 2 passed.
- `git diff HEAD~5 HEAD --check`: passed.
- Live UI and SQL Server system cases: NOT RUN in this draft; report those only after execution.

## Scope note

Local `.claude/settings.local.json`, comment-only/whitespace worktree edits, and historical personal rule files are not part of this draft.

# Coding rules map for HoangNH

> Updated 2026-11-09. This note points to the current project rules; it is not an additional tool instruction file.

## Authoritative documents

- Backend and package boundaries: `docs/architecture/ARCHITECTURE_GUIDE.md`.
- Frontend and shared UI ownership: `docs/architecture/UI/README.md`, `UI_RULES.md`, and `UI_CATALOG.md`.
- HoangNH module details and workflow: `docs/members/hoangnh/CLAUDE.md`, `requisition-review-guide.md`, and `posting-review-guide.md`.
- Database contract: `database/schema/db.sql` and applicable migration scripts. Preserve shared-schema compatibility.

## Review before changing code

1. Trace the current controller, service, access checks, DTO, entity, repository, template and tests for the affected flow.
2. Keep Requisition, Job Posting, Career Portal and Candidate application boundaries distinct. The Candidate application withdrawal flow is separate from the removed Requisition withdrawal flow.
3. For a form or state change, verify server validation, persisted data, UI display, notifications and role authorization together.
4. Run focused tests and inspect the staged diff before each commit. Use author and committer `hoangnh <0107hoanguyen@gmail.com>` for HoangNH commits.

Personal credentials and local tool settings do not belong in commits. Historical examples in `rule_code_nhh.md` are reference material and must be checked against current source before use.

# Requisition worklog

> Updated 2026-11-09. Earlier local notes described a previous implementation; the current code and tests are the source for the flow below.

## Current implementation

- `RequisitionController.list` receives `GET /requisitions`, passes binding errors to `RequisitionListService.getListPage`, and renders the Thymeleaf list. `RequisitionSpecifications` and `JobRequisitionRepository` apply role scope, filters, ordering and pagination.
- Create and edit bind `RequisitionRequest`. `RequisitionValidator` checks `action=draft/submit`; `RequisitionServiceImpl` persists the requisition and audit data, then sends the applicable notification. Successful POST redirects to `/requisitions/{id}`.
- Director approval or rejection is `POST /requisitions/{id}/decision`. A rejection requires a reason. The decision is saved to `RequisitionApproval`; activity uses `AuditLog`.
- Delete is POST only and requires an editable Draft/Rejected request with no related posting or later round. Copy of an Approved request opens a prefilled create form and does not save until submit.
- The Requisition withdrawal route and its notification were removed. Candidate application withdrawal is a different feature and remains available.

## Commits reviewed in this worktree

| Commit | Scope |
|---|---|
| `34a50246` | Remove Requisition withdrawal; validate list filters |
| `fa51ca41` | Report invalid Job Posting list filters |
| `da6c5c02` | Add probation period presets to both forms |
| `0b8457b6` | Align Requisition guides and test cases |
| `945d6e9e` | Add RMS Feature Tree source and previews |

Both author and committer of these commits were checked as `hoangnh <0107hoanguyen@gmail.com>`. The focused Java suite reported 195 tests with zero failures/errors and the requisition JavaScript suite reported 2 passing tests. These checks do not replace live browser or SQL Server system tests.

## Remaining verification

- Execute the manual cases in `system-test-requisition-job-posting.md` against the intended environment; retain `NOT RUN` until actual URL, data and screenshots are captured.
- Validate any database migration against the shared `database/schema/db.sql` contract before applying it.

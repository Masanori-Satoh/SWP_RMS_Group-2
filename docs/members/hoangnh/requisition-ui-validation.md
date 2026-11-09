# Requisition UI and workflow verification

> Code snapshot reviewed on 2026-11-09. Manual browser and SQL Server results must be recorded separately; this file is a verification checklist.

## Database contract

- `database/schema/db.sql` defines `JobRequisition`, `RequisitionApproval`, `AuditLog`, and related `Notification` data.
- The current Requisition form has no `ScreeningCriteria` collection and no `ExpectedStartDate` field. Do not reintroduce either from older notes.
- The authenticated user supplies the requester identity. A Hiring Manager uses an active department; a Director can select an active department.
- Requisition state is stored in `JobRequisition.ApprovalStatus`. Approval decisions are stored in `RequisitionApproval`; activity history comes from `AuditLog`.

## Validation and state

- `RequisitionValidator` accepts `action=draft` or `action=submit`. Title, number of positions, employment type, work model, and gender must satisfy their server rules. Submit additionally requires the narrative fields; Remote clears `workLocation`.
- `probationPeriod` is optional free text. The preset dropdown only fills the bound text input; the server receives that input.
- Hiring Manager: Draft -> Pending_Director on submit. Director: a newly submitted own request is approved directly. Director decision moves another person's Pending_Director request to Approved or Rejected; rejection needs a reason.
- The owner may edit or delete Draft/Rejected when the remaining delete guards pass. A Rejected request can be revised and resubmitted. There is no Pending_Director -> Draft withdrawal action.

## Routes and expected navigation

| Action | Route | Expected result |
|---|---|---|
| List and search | `GET /requisitions` | Server-rendered list scoped to viewer; invalid filter parameters show an empty table and errors |
| Create or save draft | `POST /requisitions/create` | Redirect to `/requisitions/{id}` on success |
| Edit or resubmit | `POST /requisitions/edit/{id}` | Redirect to `/requisitions/{id}` on success |
| Approve or reject | `POST /requisitions/{id}/decision` | Redirect to detail with flash feedback |
| Delete eligible request | `POST /requisitions/delete/{id}` | Redirect to list on success |
| Copy Approved request | `GET /requisitions/copy/{id}` | Prefilled create form; no record is saved yet |

## Verification checklist

1. Sign in as Hiring Manager. Create a Draft, edit it, submit it, and verify the detail state and Director notification.
2. Sign in as a different Director. Approve one request and reject another with a reason. Check `RequisitionApproval`, `AuditLog`, and the Hiring Manager notification link.
3. Reopen the rejected request as its owner, edit and resubmit. Verify that an already pending request cannot be edited or withdrawn.
4. Sign in as HR. Verify only Approved requests appear and can be opened. Verify invalid filters yield visible errors without broadening the role scope.
5. Verify copy creates a new form from an Approved request, and delete is blocked when a posting or later round exists.

**Execution status:** NOT RUN in this document. Record live URL, database evidence, actual result, and date in the system test report after execution.

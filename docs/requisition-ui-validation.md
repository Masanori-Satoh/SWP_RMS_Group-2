# Requisition UI and workflow verification

## Database contract

The form maps existing `JobRequisition` and `ScreeningCriteria` columns. It uses
`WorkingHours`, not a new `WorkFormat` column. `Gender`, `WorkLocation`,
`ExpectedStartDate`, version and workflow data already exist in the connected database.
The requester is the authenticated user and cannot be supplied through form input.

The only schema change in this update is migration
`database/migrations/004_reason_for_hiring_unicode.sql`: existing `ReasonForHiring`
becomes `NVARCHAR(2000) NULL`. It has been applied to the local database.
The schema script reflects the same column change. Existing text already stored
as question marks cannot be reconstructed by changing its SQL type.

## Validation

- Save Draft skips required fields, future date and total screening weight rules.
  Completely empty criteria are omitted. Partially completed criteria are retained.
- Submit requires title, department, employment type, positive integer openings,
  work location, expected start date (today or later), reason, description and requirements.
  The automatic `Untitled requisition` placeholder must be replaced.
- Each submitted criterion requires a unique name, supported type, required value
  and positive weight; total weight must equal 100%.
- Reason, description and requirements allow 2000 characters each. Text lengths,
  numeric precision, salary ordering and database constraints still apply to drafts.
  The existing unique criterion-name constraint permits only one unnamed criterion
  per requisition; a storage conflict is reported without discarding entered form values.
- Client validation assists the user; service validation also covers requests with
  JavaScript disabled. Mutation forms require CSRF tokens and a current record version.

## Workflow and actions

Hiring Managers see their own requests. Directors and HR see non-draft requests.
Admin navigation is visible only to System Admin. Draft and rejected requests can
be edited or deleted by their requester or admin. Pending requests can be withdrawn
by their requester. Directors approve or return pending requests with required
feedback on return. An approved request cannot be edited or deleted through this flow.
Requests with linked job postings cannot be deleted.

Copy opens an unsaved form, clears the record and criterion identifiers, and assigns
the current requester when saved. Activity history is retained; updates record only
changed fields and criteria. Saving unchanged content adds no update entry.

## Verification

Java suites: `RequisitionReviewProbeTests`, `RequisitionReviewWebProbeTests`,
`RequisitionValidatorTests`, `SecurityFlowTests`. SQL Server integration tests use
transactions rolled back after each test and disable Hibernate schema changes.
They cover database round trips, Vietnamese reason text, copy, criterion replacement
and name swaps, workflow transitions, stale versions, role boundaries, deletion,
filters, field binding and CSRF.

Browser verification uses HTML rendered by the MVC tests with the actual CSS and JS.
It checks layout, menu visibility, delete-dialog cancellation, adding/removing and
renumbering criteria, draft submission, required-field and weight validation,
submit action values, counters and expandable history. The preview submission
receiver does not change application data; persistence is covered separately by
the SQL Server integration tests.

To check the running application after restart:

1. Sign in as a Hiring Manager, save an empty draft, then reopen and edit it.
2. Complete required fields and screening weights, submit, and check detail values.
3. Sign in as Director, return with feedback; revise as requester and submit again.
4. Verify approval, or withdraw a pending request as its requester.
5. Copy a visible request; ensure nothing is saved until a form action is selected.
6. Filter and paginate the list; delete a disposable draft or rejected request.
7. Confirm another Hiring Manager cannot read or mutate the first user's requests.

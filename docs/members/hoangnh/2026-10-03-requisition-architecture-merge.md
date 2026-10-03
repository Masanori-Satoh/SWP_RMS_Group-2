# Requisition: migrate to the feature architecture without changing the workflow

## Scope

Resolve the in-progress merge between feature HEAD `cc1e4c9` and incoming main
`4274ba0`, following `docs/architecture/ARCHITECTURE_GUIDE.md`.
The incoming feature package layout is retained; the feature branch's implemented
requisition workflow remains the behavioral baseline. No merge commit is created.

## Package ownership

| Component | Location |
| --- | --- |
| Requisition web controller | `requisition/controller` |
| Service interface, implementation and business access rules | `requisition/service` |
| Requisition, approval, criteria and workflow-event entities | `requisition/entity` |
| Persistence, including workflow events and job postings | `requisition/repository` |
| Request/response objects, including timeline | `requisition/dto` |
| Draft/submission validation | `requisition/validator` |
| Field-level business validation exception | `requisition/exception` |
| Shared HTTP exception handling | `core/exception` |
| Sidebar role advice | `auth/NavigationAdvice` |

Input DTO names end in `Request`; output DTO names end in `Response`.
`RequisitionTimelineResponse` remains a record. Tests mirror the requisition
controller/service/validator packages. Obsolete duplicate service/repository files
are removed after their behavior is moved into the feature package.

## Preserved behavior

- Search, filtering, sorting and pagination respect the viewer's access scope.
- The creator comes from the authenticated active account, not a form-provided ID.
- Incomplete drafts remain saveable; submission enforces required fields,
  supported values, salary/date limits, unique criteria and a 100% total weight.
- Create, edit, copy and delete retain their existing routes and form binding.
- Status transitions remain `Draft -> Pending_Director -> Approved / Rejected`.
  Rejected requests may be edited and resubmitted; pending requests may be
  withdrawn by their owner to `Draft`.
- Access checks, version checks, persistence locking, linked-posting deletion
  protection, criteria ID ownership and rename handling are retained.
- Approval history, workflow timeline, change auditing and rejection feedback
  remain available.
- Existing form errors retain user input, and the external `requisitions.js`
  remains the source of client-side behavior. The obsolete inline script from
  the incoming form is not reinstated.
- The new job-posting queries from main are retained alongside
  `existsByRequisition_RequisitionId`, which requisition deletion requires.

## Applying the guide's WIP exception

This is a migration of existing functionality, not a replacement feature.
The guide explicitly permits existing WIP controllers and forms to retain their
current behavior. Mutable form DTOs, the established draft/submission validator,
and controller handling that redisplays field errors remain in place. They are
not mechanically converted to records or blanket `@NotNull` constraints, which
would change incomplete-draft/form behavior. Existing inline presentation styles
are also left unchanged in this merge.

`RequisitionValidationException` now extends the shared `BaseBusinessException`.
Read-only service operations retain `@Transactional(readOnly = true)`.
Any later migration to annotation validation or centralized form-error handling
must retain these workflow contracts and their tests.

## Integration correction

The incoming global `Exception` handler converted Spring MVC's method-not-allowed
response into HTTP 500. `GlobalExceptionHandler` now inherits Spring's framework
exception handling and explicitly preserves HTTP 403 for access denials. Existing
resource-not-found and business-error views remain in place. Regression cases
cover 403, 404 and 405 responses for requisitions.

## Validation

Compilation of production and test sources succeeded with Java 21 and the locally
cached Maven 3.9.11 distribution. The wrapper could not launch in the sandbox;
the cache was selected explicitly without changing wrapper configuration.

- `RequisitionValidatorTests`: 5 passed.
- `RequisitionReviewWebProbeTests`: 9 passed.
- `RequisitionReviewProbeTests`: 12 passed against local SQL Server; these tests
  disable schema updates and roll back their transactions.
- Broader web regression: 49 total, 46 passed, 3 failed. The failures are two
  Career detail-route tests and the candidate-account list test in SecurityFlow.
  These controllers and their tests are unchanged from incoming main. Their
  route/test mismatches are outside the requisition migration.
- Baseline verification on an isolated, unmodified archive of incoming main:
  the same two web suites ran 23 tests with 6 failures, including all 3 failures
  remaining above. The shared HTTP-status correction removes the other 3
  baseline failures. The isolated log is
  `target/requisition-main-baseline/baseline-tests.log`.

Detailed logs are local build output under `target/requisition-regression.log`.
The original merge state and affected file contents were backed up under
`target/requisition-merge-backup-20261003-075245/` before migration.

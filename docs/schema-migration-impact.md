# Schema migration impact analysis

Source of truth: `database/schema/db.sql` (20 SQL Server tables). This analysis was
completed before changing Java mappings. The schema script drops and recreates
`RitirementManagement2`; it must not be run against an existing populated target.
The project owner chose a fresh database without transferring old rows.

## Logical model and cardinality

- `Role` 1:N `User`; `Department` 1:N `User`; `Department.ManagerId` is an optional FK to `User`.
- `User` 1:0..1 `Candidate`; every `Candidate` requires one unique `UserId`. Candidate name, email and phone are in `User` only.
- `Department` and `User` each parent `JobRequisition`; requisitions parent approvals, criteria and postings. `JobPosting.RequisitionId` is not unique, so this is 1:N.
- `Candidate` and `JobPosting` each parent `Application`; each application stores one required `AppliedCvUrl`. There is no `Resume` table.
- `Application` parents `ApplicationReview`, `AIScreeningResult` and `InterviewSchedule` as 1:N according to the absence of unique FKs. The project owner confirmed that multiple AI results for one application preserve rescoring history. `Application` has 0..1 `OfferProposal` via its unique FK.
- `InterviewSchedule` has a many-to-many panel with `User` through the composite-key `InterviewPanel`; evaluations are independent rows linked to interview and user. Each interview has 0..1 final result.
- `OfferProposal` parents approvals and negotiation rounds. `User` parents audit logs; `SystemConfig` is independent.

All surrogate integer keys use `IDENTITY`; `AuditLogId` is `BIGINT` and
`SystemConfig.ConfigKey` is a string key. The important unique constraints are
role name, department name, username, user email, candidate user, screening
criterion name per requisition, interview final result per interview, and offer
per application. The schema's `CHECK` constraints define statuses, category
values, score ranges, positive positions/weights, salary bounds and interview
times; database defaults provide several statuses and timestamps.

## Impact matrix

| Database change | Code affected | File | Current problem | Required change |
| --- | --- | --- | --- | --- |
| `User.Email NVARCHAR(150)` | account mapping and validation | `entity/User.java`, `controller/form/*AccountForm.java`, account form template | Java/UI still limit email to 100 | Raise mapped and form length to 150 |
| `Candidate.UserId NOT NULL UNIQUE`, no duplicate contact columns | candidate mapping, account create/update | `entity/Candidate.java`, `service/AccountManagementService.java`, candidate tests | Link nullable/many-to-one; duplicate full name/email/phone and mandatory candidate phone | Map 1:1 required; keep contact fields on User; remove candidate-only phone rule and email copying |
| No `Department.Status`; unique department name | department mapping | `entity/Department.java` | Maps missing column and omits unique metadata | Remove status; add unique name metadata |
| `Application.AppliedCvUrl NVARCHAR(500)`; reviews are separate | application mapping and HR dashboard | `entity/Application.java`, `repository/DashboardMetricsRepository.java` | URL maps MAX; stale review notes/reviewer/date columns; HR query reads removed field | Limit URL to 500; remove stale columns; derive reviewed state from `ApplicationReview` |
| `ApplicationReview` is reviewer decision, not Resume metadata | entity and repository | `entity/ApplicationReview.java`, `repository/ApplicationReviewRepository.java` | Class maps nonexistent ResumeId, CandidateId and file columns | Reuse class/repository; map ReviewId, ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt |
| `AIScreeningResult` only score/time; ApplicationId not unique | AI entity | `entity/AIScreeningResult.java` | Extra columns and forced 1:1 | Remove absent columns; map many-to-one |
| `JobPosting` dates are DATETIME2; location 255 | posting entity and dashboard aggregate | `entity/JobPosting.java`, `repository/DashboardMetricsRepository.java` | LocalDate loses time; 200-character location | Use LocalDateTime; compare deadlines against current time; length 255 |
| Offer fields/statuses changed | offer entity, dashboard aggregate, tests | `entity/OfferProposal.java`, `repository/DashboardMetricsRepository.java`, `service/DashboardService.java`, dashboard tests | ProbationDays/ProposedPosition absent; missing OfferedPositionTitle, ExpectedStartDate, BenefitsPackage; old status strings | Map exact fields; use `Pending_Director`, `Sent_Candidate`, etc. |
| `InterviewPanel.RoleInPanel NOT NULL`, HR/HM | panel entity | `entity/InterviewPanel.java` | Nullable mapping and obsolete role description | Mark required and document SQL allowed values |
| Various NVARCHAR lengths | entity metadata | `AuditLog`, `Candidate`, `InterviewSchedule`, `ScreeningCriteria`, `SystemConfig` | Lengths differ from SQL; audit action comment includes unsupported VIEW | Align lengths and descriptions; add composite criterion uniqueness |
| Old Candidate link patch targets snake_case legacy DB | SQL migration documentation | `database/migrations/001_candidate_account_link.sql`, `docs/linhdn-*` | Cannot be applied to PascalCase fresh schema; descriptions say nullable UserId and duplicate email | Preserve historical patch as non-applicable; update active documentation |
| Local application URL targets old DB | app configuration and verification | ignored `src/main/resources/application.properties` | `RitirementManagement`, but schema creates `RitirementManagement2` | Point local runtime at existing fresh DB before integration verification; retain `ddl-auto=validate` |
| Spring Boot physical naming converts PascalCase to snake_case | every entity and SQL query | `config/SchemaNamingConfig.java`, `entity/User.java` | Hibernate could not find `User` despite the table existing | Preserve explicit SQL identifiers and quote reserved `User` correctly |
| Seeder already uses new table/column names | seed SQL and generator | `database/seeds/seed_data.sql`, `database/seeds/build_seed.py` | 1,846 INSERTs across all 20 tables; static column audit found no missing references | Keep; verify against actual fresh DB before claiming it runs |
| Old seeding guide describes 50 Resume rows and independent candidates | database documentation | `docs/database/data_seeding_guide.md` | Contradicts source-of-truth schema | Mark historical claims and describe current schema/seeder |

## Classification

- **KEEP:** Matching entities/relationships for Role, User role/department, JobRequisition, RequisitionApproval, InterviewEvaluation, InterviewFinalResult, OfferApproval, OfferNegotiation, and the existing authentication/authorization configuration. Existing account list UI remains based on User.
- **MODIFY:** Entity mappings and dependent queries/forms/tests listed above. Keep the account and dashboard features rather than replacing their architecture.
- **DELETE:** No files at this stage. Remove only obsolete fields/mappings after checking all references. Repurpose `ApplicationReview` rather than deleting it.
- **CREATE:** New fields on existing mapped classes, and focused tests for the new review relationship and mapped constraints. No new table or bidirectional collection is required.
- **NEED CONFIRMATION:** The schema does not constrain `Candidate.UserId` to users with the Candidate role; role changes currently preserve a profile. `InterviewEvaluation` has neither a unique `(InterviewId, InterviewerId)` constraint nor an FK to `InterviewPanel`, so duplicate/unassigned evaluations are possible. Do not invent rules for these future workflows. AI configuration keys and external monitoring targets remain separate specification gaps. AI rescoring cardinality is confirmed as 1:N.

### NEED CONFIRMATION: Interview evaluation cardinality and panel membership

**Problem:** Can one interviewer submit more than one evaluation for the same interview, and can a user outside that interview's panel submit an evaluation?

**Current schema:** `InterviewEvaluation` has independent FKs to `InterviewSchedule` and `User`. It has no unique `(InterviewId, InterviewerId)` constraint and no FK to `InterviewPanel`.

**Current code:** `InterviewEvaluation` maps both FKs as many-to-one. No evaluation submission workflow currently enforces a stricter rule.

**Options:** A. Keep multiple evaluations and permit non-panel interviewers; no schema change. B. Permit one evaluation per panel member; this needs a schema constraint/relationship decision before implementing the submission workflow.

**Impact:** A allows duplicates and off-panel evaluations. B changes the data model and would require approval and an updated `database/schema/db.sql` before Java implementation.

## Dependency check before removal

Searches across `src/main`, `src/test`, `database` and `docs` found uses of
`Candidate.email/fullName/phoneNumber` in account service/tests and old docs;
`Application.reviewedBy` in HR dashboard query; `OfferProposal.proposedPosition`
and old offer statuses in dashboard query/service/tests; candidate-phone help
text/required flag in account template/JS; and Resume terminology in the old
seeding guide. No controller/template directly uses the old Resume entity fields.

## Migration boundary

The fresh schema is authoritative for code. No live database was changed by
the analysis, and the old candidate-link SQL is not a migration path for the
new schema. The existing `RitirementManagement2` database already contained
20 tables and approximately 1,846 seeded rows, so its destructive schema
script and seed were not rerun. The local ignored runtime config was pointed
at this database with `ddl-auto=validate`; Spring Context and JPA validation
then passed. Compilation alone was not treated as migration proof.

## Verification after migration

- `mvn test`: 28 tests passed, zero failures/errors/skips. This includes Spring Context startup, Hibernate schema validation against `RitirementManagement2`, Thymeleaf/Security MVC rendering, account and dashboard database queries, and two AI screening result rows for one application.
- `mvn -DskipTests package`: passed and produced the application JAR.
- Dependency search found no obsolete entity fields in active `src/main` or `src/test`. Historical terms remain in the old seeding guide and the archived candidate-link migration, both labeled non-applicable to the new schema.
- `database/schema/db.sql` and current seed files were not modified or rerun.

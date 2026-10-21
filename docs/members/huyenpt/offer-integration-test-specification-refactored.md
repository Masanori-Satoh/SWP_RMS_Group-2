# Offer Management - Refactored Manual Integration Test Specification

## General Test Preconditions

The following baseline preconditions apply across all manual integration test scenarios in this specification. Individual test cases reference them by identifier to avoid redundant setup text:

- **GP-01:** The RMS web application is deployed, active, and accessible via a modern browser at `http://localhost:8080`.
- **GP-02:** The SQL Server database is online, accessible, and running with current schema and test seeds applied.
- **GP-03:** Standard test accounts for all required roles (`ROLE_HR`, `ROLE_DIRECTOR`, `ROLE_CANDIDATE`) are provisioned and active.
- **GP-04:** Baseline master data (Departments, Job Postings, Candidates, and Applications) is seeded and accessible.
- **GP-05:** The QA tester possesses administrative or direct query access to SQL Server to verify data persistence and transactional integrity.

---

## Test Information

| Field | Value |
|---|---|
| Feature | Offer Management (End-to-End Workflow) |
| Test Requirement | Verify the atomic, deterministic behavior of the Offer Proposal lifecycle across HR, Director, and Candidate roles, including creation, draft maintenance, review/decision, issuance, candidate response, deadline enforcement, role boundaries, and Excel export. |
| Number of TCs | 55 |
| Round 1 Passed | 0 |
| Round 1 Failed | 0 |
| Round 1 Pending | 55 |
| Round 1 N/A | 0 |
| Round 2 Passed | 0 |
| Round 2 Failed | 0 |
| Round 2 Pending | 55 |
| Round 2 N/A | 0 |
| Round 3 Passed | 0 |
| Round 3 Failed | 0 |
| Round 3 Pending | 55 |
| Round 3 N/A | 0 |

---

## Refactoring Analysis

### Old-to-New Mapping

| Old TC ID | Decision | New TC ID(s) | Reason |
|---|---|---|---|
| TC-INT-001 | SPLIT | TC-INT-001, TC-INT-002 | Separated candidate auto-population verification from the saving draft operation. |
| TC-INT-002 | SIMPLIFY | TC-INT-003 | Removed "201 or 302" ambiguity; aligned directly with UI modal submission route. |
| TC-INT-003 | SIMPLIFY | TC-INT-004 | Focused strictly on the GBR-07 duplicate offer restriction and rejection outcome. |
| TC-INT-004 | SIMPLIFY | TC-INT-005 | Removed ambiguous "Save Draft or Submit"; focused on concrete Save Draft action. |
| TC-INT-005 | SIMPLIFY | TC-INT-006 | Streamlined verification of maximum 60-day response period persistence. |
| TC-INT-006 | SIMPLIFY | TC-INT-007 | Focused strictly on lower boundary response period validation (0 days). |
| TC-INT-007 | SIMPLIFY | TC-INT-008 | Replaced "yesterday or today" with a single representative invalid boundary (today). |
| TC-INT-008 | SIMPLIFY | TC-INT-009 | Streamlined draft parameter update procedure and database field assertions. |
| TC-INT-009 | SIMPLIFY | TC-INT-010 | Focused on status transition from Draft to Pending_Director upon submission. |
| TC-INT-010 | SIMPLIFY | TC-INT-011 | Focused on soft-delete verification (IsDeleted = 1) and active list exclusion. |
| TC-INT-011 | SIMPLIFY | TC-INT-012 | Aligned with latest rule blocking edit for any non-Draft status (e.g. Director_Rejected). |
| TC-INT-012 | SIMPLIFY | TC-INT-013 | Focused on blocking deletion for proposals advanced past Draft status. |
| TC-INT-013 | SPLIT | TC-INT-014, TC-INT-015, TC-INT-016 | Split into 3 independent cases: Pagination, Sort Latest, and Sort Earliest. |
| TC-INT-014 | SPLIT | TC-INT-017, TC-INT-018, TC-INT-019 | Split into 3 atomic cases: Status Filter, Keyword Search, and Search + Filter. |
| TC-INT-015 | SIMPLIFY | TC-INT-020 | Focused on complete detail view rendering and approval timeline accuracy. |
| TC-INT-016 | SPLIT | TC-INT-021, TC-INT-022 | Split Director view into Pending Approval Tab vs. Decision History Tab. |
| TC-INT-017 | SIMPLIFY | TC-INT-023 | Streamlined Director approval flow, OfferApproval record creation, and HR notification. |
| TC-INT-018 | SIMPLIFY | TC-INT-024 | Streamlined Director rejection flow, reason capture, and post-rejection lock state. |
| TC-INT-019 | SPLIT | TC-INT-025, TC-INT-026 | Split into Block Approval on Non-Pending vs. Block Rejection on Non-Pending. |
| TC-INT-020 | SIMPLIFY | TC-INT-027 | Streamlined offer issuance, deadline calculation, and application status transition. |
| TC-INT-021 | SIMPLIFY | TC-INT-028 | Removed "Draft or Pending"; used one representative unapproved state (Draft). |
| TC-INT-022 | SIMPLIFY | TC-INT-029 | Focused on deadline conflict validation (deadline >= expected start date). |
| TC-INT-023 | SIMPLIFY | TC-INT-030 | Focused on candidate portal active offer list and internal draft exclusion. |
| TC-INT-024 | SIMPLIFY | TC-INT-031 | Focused on candidate offer letter terms and active response action affordances. |
| TC-INT-025 | KEEP | TC-INT-032 | Verified cross-candidate data isolation and 404 access restriction. |
| TC-INT-026 | SIMPLIFY | TC-INT-033 | Focused on candidate acceptance happy path, Hired status, and congratulations modal. |
| TC-INT-027 | SIMPLIFY | TC-INT-034 | Focused on candidate decline with detailed reason and Application Rejected status. |
| TC-INT-028 | SIMPLIFY | TC-INT-035 | Focused on candidate decline without reason and NULL reason persistence. |
| TC-INT-029 | SIMPLIFY | TC-INT-036 | Removed expired ambiguity; focused strictly on navigation guard for answered offers. |
| TC-INT-030 | SIMPLIFY | TC-INT-037 | Focused on deadline expiration enforcement and disabled action buttons. |
| TC-INT-031 | SIMPLIFY | TC-INT-038 | Focused on start date expiration enforcement blocking candidate acceptance. |
| TC-INT-032 | SIMPLIFY | TC-INT-039 | Focused on duplicate response rejection after initial acceptance. |
| TC-INT-033 | SIMPLIFY | TC-INT-040 | Cleaned black-box two-session manual concurrency procedure and assertions. |
| TC-INT-034 | SIMPLIFY | TC-INT-041 | Preserved 1000-character boundary requirement and documented current 500 defect. |
| TC-INT-035 | SIMPLIFY | TC-INT-042 | Preserved HR approve restriction and documented current authorization gap. |
| TC-INT-036 | SIMPLIFY | TC-INT-043 | Preserved HR reject restriction and documented current authorization gap. |
| TC-INT-037 | SIMPLIFY | TC-INT-044 | Preserved Director create restriction and documented current authorization gap. |
| TC-INT-038 | SIMPLIFY | TC-INT-045 | Preserved Director edit draft restriction and documented current authorization gap. |
| TC-INT-039 | SIMPLIFY | TC-INT-046 | Preserved Director delete draft restriction and documented current authorization gap. |
| TC-INT-040 | SIMPLIFY | TC-INT-047 | Preserved Director issue offer restriction and documented current authorization gap. |
| TC-INT-041 | KEEP | TC-INT-048 | Maintained candidate isolation from internal `/offers/**` management routes. |
| TC-INT-042 | SIMPLIFY | TC-INT-049 | Focused on unauthenticated guest redirection to `/login`. |
| TC-INT-043 | SIMPLIFY | TC-INT-050 | Focused CSRF testing on representative `POST /offers/create` endpoint. |
| TC-INT-044 | SIMPLIFY | TC-INT-051 | Focused on Filtered scope export generation and content matching. |
| TC-INT-045 | SIMPLIFY | TC-INT-052 | Focused on Selected scope export generation for explicitly chosen offer IDs. |
| TC-INT-046 | SIMPLIFY | TC-INT-053 | Focused on All scope export matching active non-deleted database records. |
| TC-INT-047 | SIMPLIFY | TC-INT-054 | Focused on validation error when Selected scope is submitted with empty IDs. |
| TC-INT-048 | SIMPLIFY | TC-INT-055 | Preserved Director export prohibition and documented current backend gap. |

---

## Proposed New Test Inventory

| New TC ID | Function Group | Test Case Title | Primary Action | Expected Business Outcome |
|---|---|---|---|---|
| TC-INT-001 | A. HR Offer Creation | Auto-populate Offer Form from Interview Result | Select passed candidate in creation modal | Compensation and position fields are auto-filled correctly. |
| TC-INT-002 | A. HR Offer Creation | Save Valid Offer Proposal as Draft | Fill valid details and click "Save Draft" | Offer saved with status Draft; no Director notification. |
| TC-INT-003 | A. HR Offer Creation | Submit Offer Proposal Directly to Director | Fill valid details and click "Submit to Director" | Offer created with status Pending_Director; notification sent. |
| TC-INT-004 | A. HR Offer Creation | Enforce Single Active Offer Rule GBR-07 | Attempt to create offer for candidate with active offer | Operation rejected with HTTP 400 OFFER_LOCKED_STATE. |
| TC-INT-005 | A. HR Offer Creation | Enforce Salary Compliance Rule BR-OFF-01 | Submit offer with probation salary < 85% of official | Validation error displayed; submission blocked. |
| TC-INT-006 | A. HR Offer Creation | Validate ResponsePeriodDays Upper Boundary | Submit offer with responsePeriodDays = 60 | Offer created successfully; 60 days persisted in database. |
| TC-INT-007 | A. HR Offer Creation | Validate ResponsePeriodDays Lower Boundary | Submit offer with responsePeriodDays = 0 | Validation error displayed; response period must be >= 1. |
| TC-INT-008 | A. HR Offer Creation | Validate Expected Start Date Future Boundary | Submit offer with expected start date set to today | Validation error displayed; start date must be in the future. |
| TC-INT-009 | B. HR Draft Management | Update Draft Proposal Details | Edit existing draft and click "Save Draft" | Proposal fields updated; status remains Draft. |
| TC-INT-010 | B. HR Draft Management | Submit Updated Draft to Director | Edit existing draft and click "Submit to Director" | Proposal updated and transitioned to Pending_Director. |
| TC-INT-011 | B. HR Draft Management | Soft-Delete Draft Offer Proposal | Click delete icon on draft offer and confirm | Proposal marked IsDeleted = 1; hidden from active list. |
| TC-INT-012 | B. HR Draft Management | Block Editing Non-Draft Offer | Attempt to edit offer in non-Draft status | Edit action blocked; PUT request returns HTTP 400. |
| TC-INT-013 | B. HR Draft Management | Block Deleting Non-Draft Offer | Attempt to delete offer in Pending_Director status | Delete action blocked; record remains active. |
| TC-INT-014 | C. HR Offer List & Search | HR Offer List Pagination | Click pagination control to load page 2 | Second page slice loaded with correct 10-item limit. |
| TC-INT-015 | C. HR Offer List & Search | Sort HR Offers by Latest | Select "Latest" time sort option | Offers reordered descending by creation timestamp and ID. |
| TC-INT-016 | C. HR Offer List & Search | Sort HR Offers by Earliest | Select "Earliest" time sort option | Offers reordered ascending by creation timestamp and ID. |
| TC-INT-017 | C. HR Offer List & Search | Filter HR Offers by Status | Select "Pending Director" from status filter | Table filters to show only Pending_Director records. |
| TC-INT-018 | C. HR Offer List & Search | Search HR Offers by Keyword | Enter candidate name keyword in search box | Table filters to show only matching candidate records. |
| TC-INT-019 | C. HR Offer List & Search | Combine Keyword Search and Status Filter | Apply keyword and status filter simultaneously | Table displays intersection of keyword and status criteria. |
| TC-INT-020 | C. HR Offer List & Search | View Comprehensive Offer Detail | Navigate to `/offers/{id}` | Complete candidate, assessment, and approval data rendered. |
| TC-INT-021 | D. Director Review & Decision | Director Pending Approval Tab View | Director navigates to `/offers` | Default tab shows only Pending_Director offers; drafts hidden. |
| TC-INT-022 | D. Director Review & Decision | Director Decision History Tab and Status Filter | Director opens "Decision History" tab and filters | Historical decisions displayed; filtered by specific outcome. |
| TC-INT-023 | D. Director Review & Decision | Director Approves Offer Proposal | Submit approval with review comments | Offer transitions to Director_Approved; audit record saved. |
| TC-INT-024 | D. Director Review & Decision | Director Rejects Offer Proposal with Comments | Submit rejection with required comments | Offer transitions to Director_Rejected; editing blocked for HR. |
| TC-INT-025 | D. Director Review & Decision | Block Director Approval on Non-Pending Offer | Attempt to approve an already approved offer | Request rejected with HTTP 400 OFFER_STATUS_INVALID. |
| TC-INT-026 | D. Director Review & Decision | Block Director Rejection on Non-Pending Offer | Attempt to reject a draft or approved offer | Request rejected with HTTP 400 OFFER_STATUS_INVALID. |
| TC-INT-027 | E. HR Issue Offer | Issue Approved Offer to Candidate | Click "Issue Offer Letter" on approved offer | Offer transitions to Sent_Candidate; deadline set; email sent. |
| TC-INT-028 | E. HR Issue Offer | Block Issuing Unapproved Offer | Attempt to issue offer in Draft status | Action blocked; request returns HTTP 400 OFFER_NOT_APPROVED. |
| TC-INT-029 | E. HR Issue Offer | Block Issue when Deadline Exceeds Start Date | Issue offer where deadline >= start date | Action rejected; deadline must precede start date. |
| TC-INT-030 | F. Candidate Portal | Candidate Views Active Offer List | Candidate navigates to `/portal/offers` | Candidate sees their issued offers; internal drafts excluded. |
| TC-INT-031 | F. Candidate Portal | Candidate Views Offer Letter Detail | Candidate opens `/portal/offers/{id}` | Complete terms rendered; Accept and Decline buttons active. |
| TC-INT-032 | F. Candidate Portal | Enforce Cross-Candidate Data Isolation | Candidate A accesses Candidate B's offer URL | Request rejected with HTTP 404 Not Found. |
| TC-INT-033 | G. Candidate Response | Candidate Accepts Offer | Candidate clicks "Accept Offer" and confirms | Offer transitions to Accepted; Application becomes Hired. |
| TC-INT-034 | G. Candidate Response | Candidate Declines Offer with Reason | Candidate submits decline survey with text reason | Offer transitions to Declined; reason stored; Application Rejected. |
| TC-INT-035 | G. Candidate Response | Candidate Declines Offer without Reason | Candidate submits decline survey with blank reason | Offer transitions to Declined; reason stored as NULL. |
| TC-INT-036 | G. Candidate Response | Enforce Decline Page Navigation Guard | Direct navigation to `/decline` for answered offer | User redirected to detail view with error banner. |
| TC-INT-037 | G. Candidate Response | Block Response after Deadline Expiration | Candidate submits response when deadline < NOW() | Response rejected; offer remains expired and unchanged. |
| TC-INT-038 | G. Candidate Response | Block Response after Expected Start Date | Candidate submits response when start date < TODAY | Response rejected; start date has passed. |
| TC-INT-039 | G. Candidate Response | Block Duplicate Candidate Response | Candidate attempts second response after accepting | Second request rejected; original Accepted state preserved. |
| TC-INT-040 | G. Candidate Response | Prevent Concurrent Accept and Decline Conflict | Near-simultaneous Accept and Decline in two sessions | Exactly one response commits; system reaches deterministic state. |
| TC-INT-041 | G. Candidate Response | Validate Decline Reason Length Boundary | Submit decline reason exceeding 1000 characters | System gracefully validates and rejects input >1000 chars. |
| TC-INT-042 | H. Role Authorization | Enforce HR Forbidden from Approving Offer | HR sends POST to `/offers/{id}/approve` | Request denied with HTTP 403 Forbidden. |
| TC-INT-043 | H. Role Authorization | Enforce HR Forbidden from Rejecting Offer | HR sends POST to `/offers/{id}/reject` | Request denied with HTTP 403 Forbidden. |
| TC-INT-044 | H. Role Authorization | Enforce Director Forbidden from Creating Offer | Director attempts GET or POST to `/offers/create` | Request denied with HTTP 403 Forbidden. |
| TC-INT-045 | H. Role Authorization | Enforce Director Forbidden from Editing Draft | Director sends PUT or POST to `/offers/{id}/edit` | Request denied with HTTP 403 Forbidden. |
| TC-INT-046 | H. Role Authorization | Enforce Director Forbidden from Deleting Draft | Director sends DELETE to `/offers/{id}` | Request denied with HTTP 403 Forbidden. |
| TC-INT-047 | H. Role Authorization | Enforce Director Forbidden from Issuing Offer | Director sends POST to `/offers/{id}/send` | Request denied with HTTP 403 Forbidden. |
| TC-INT-048 | H. Role Authorization | Enforce Candidate Isolated from Internal Routes | Candidate accesses `/offers` or `/offers/create` | Request denied with HTTP 403 Forbidden. |
| TC-INT-049 | H. Role Authorization | Enforce Unauthenticated Guest Redirect to Login | Unauthenticated user accesses protected offer route | Request redirected to `/login` with HTTP 302. |
| TC-INT-050 | H. Role Authorization | Enforce CSRF Token Protection | Submit state-changing request without CSRF token | Request intercepted and rejected with HTTP 403 Forbidden. |
| TC-INT-051 | I. Offer Export | Export Filtered Offers to Excel | HR exports with scope FILTERED | Excel generated containing only active filtered offer rows. |
| TC-INT-052 | I. Offer Export | Export Selected Offers to Excel | HR exports with scope SELECTED for specific IDs | Excel generated containing exactly the selected offer rows. |
| TC-INT-053 | I. Offer Export | Export All Active Offers to Excel | HR exports with scope ALL | Excel generated matching total active non-deleted database count. |
| TC-INT-054 | I. Offer Export | Validate Empty Selection on Export | HR submits scope SELECTED with empty ID list | Request rejected with HTTP 400 EMPTY_SELECTED_OFFERS. |
| TC-INT-055 | I. Offer Export | Enforce Director Forbidden from Exporting Data | Director sends POST to `/offers/export` | Request denied with HTTP 403 Forbidden. |

---

## Function A: HR Offer Creation

### TC-INT-001 - Auto-populate Offer Form from Interview Result

**Test Case Description:**
Verify that selecting an interview-passed candidate in the Offer Creation modal automatically populates the position title, recommended salary, 85% probation salary, and default location.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Navigate to Offer Management (`/offers`).
3. Click the "Tạo đề xuất mới" button to open the Offer Creation modal.
4. Select Candidate "Nguyen Van A" who has an interview result with `FinalDecision = 'Passed'` and `RecommendedSalary = 30,000,000`.
5. Observe the populated values across all form fields in the modal.

**Expected Results:**
- Offered Position Title is auto-filled matching the job posting title.
- Proposed Salary is auto-filled with `30,000,000`.
- Probation Salary is auto-calculated and filled with `25,500,000` (exactly 85% of proposed salary).
- Probation Days defaults to `60` and Response Period Days defaults to `5`.
- Work Location defaults to the job posting or candidate location.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-04.
- Candidate has `InterviewFinalResult.FinalDecision = 'Passed'` and no existing offer proposal.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-002 - Save Valid Offer Proposal as Draft

**Test Case Description:**
Verify that an HR user can save a newly created Offer Proposal as a Draft without submitting it to the Director.

**Test Case Procedure:**
1. Log in with an active HR account and open `/offers`.
2. Open the Offer Creation modal and select an eligible interview-passed candidate.
3. Enter valid compensation terms:
   - Proposed Salary: `25,000,000`
   - Probation Salary: `21,250,000`
   - Expected Start Date: A date 30 days in the future
   - Work Location: `Head Office - Hanoi`
4. Click the "Lưu bản nháp" button (`isDraft = true`).
5. Verify modal close and query database persistence.

**Expected Results:**
- API response returns HTTP 201 Created with JSON `success: true`.
- Toast notification displays: "Lưu bản thảo Offer thành công!".
- In the database:
  - New row inserted in `OfferProposal` with `OfferStatus = 'Draft'`.
  - `IsDeleted = 0` and `ResponseDeadline = NULL`.
  - `ProposedBy` is set to the logged-in HR user ID.
  - Linked `Application.ApplicationStatus` remains unchanged.
- No notification is dispatched to the Director.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-04, GP-05.
- Eligible interview-passed candidate exists without an offer.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-003 - Submit Offer Proposal Directly to Director

**Test Case Description:**
Verify that an HR user can create an Offer Proposal and directly submit it to the Director for approval, setting status to `Pending_Director`.

**Test Case Procedure:**
1. Log in with an active HR account and open `/offers`.
2. Open the Offer Creation modal and select an eligible interview-passed candidate.
3. Fill in all required fields with valid business values:
   - Proposed Salary: `30,000,000`
   - Probation Salary: `25,500,000`
   - Expected Start Date: A date 45 days in the future
   - Response Period Days: `7`
4. Click the "Nộp duyệt" button (`isDraft = false`).
5. Verify modal close and query database state and notification logs.

**Expected Results:**
- API response returns HTTP 201 Created with JSON `success: true`.
- Toast notification displays: "Đã nộp trình đề xuất Offer lên Giám đốc thành công!".
- In the database:
  - `OfferProposal.OfferStatus = 'Pending_Director'`.
  - `ResponsePeriodDays = 7` and `ResponseDeadline = NULL`.
- In-app notification record is created in `Notification` targeting the Director role.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-04, GP-05.
- Eligible interview-passed candidate exists without an offer.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-004 - Enforce Single Active Offer Rule GBR-07

**Test Case Description:**
Verify rule GBR-07: the system blocks creating a second Offer Proposal for an application that already has an existing active or draft offer.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Ensure Candidate A already possesses an existing offer proposal in status `Draft` or `Pending_Director`.
3. Open the Offer Creation modal and verify Candidate A is filtered out of the selection list.
4. Send a direct HTTP POST request to `/offers/create` with `applicationId` set to Candidate A's application ID.
5. Observe the HTTP response code and error payload.

**Expected Results:**
- Request is rejected with HTTP 400 Bad Request.
- Response payload contains `errorCode: "OFFER_LOCKED_STATE"` and an error message indicating that an offer already exists under rule GBR-07.
- Database verification confirms no duplicate row is created in `OfferProposal`.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Candidate A has an existing active or draft offer proposal.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-005 - Enforce Salary Compliance Rule BR-OFF-01

**Test Case Description:**
Verify compliance rule BR-OFF-01: probation salary must be at least 85% of official proposed salary; submissions below 85% are rejected.

**Test Case Procedure:**
1. Log in with an active HR account and open `/offers`.
2. Open the Offer Creation modal and select an eligible candidate.
3. Enter Proposed Salary = `20,000,000`.
4. Manually override Probation Salary = `16,000,000` (80%, strictly below legal 85% threshold of `17,000,000`).
5. Enter valid future start date and click "Lưu bản nháp".
6. Observe the validation error feedback.

**Expected Results:**
- Submission is rejected before database persistence.
- Field validation error displays: "Lương thử việc tối thiểu phải bằng 85% lương chính thức".
- Database verification confirms no record was inserted.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Eligible interview-passed candidate exists.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-006 - Validate ResponsePeriodDays Upper Boundary

**Test Case Description:**
Verify that the maximum allowed response period boundary of 60 days (`responsePeriodDays = 60`) is accepted and correctly stored.

**Test Case Procedure:**
1. Log in with an active HR account and open `/offers`.
2. Open the Offer Creation modal and select an eligible candidate.
3. Set `responsePeriodDays = 60`.
4. Set `expectedStartDate` to a date 75 days in the future (ensuring deadline < start date).
5. Click "Lưu bản nháp" (`isDraft = true`).
6. Query database to inspect stored response period.

**Expected Results:**
- HTTP 201 Created is returned with success toast.
- In the database:
  - `OfferProposal.ResponsePeriodDays = 60`.
  - `OfferProposal.OfferStatus = 'Draft'`.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Eligible candidate exists.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-007 - Validate ResponsePeriodDays Lower Boundary

**Test Case Description:**
Verify that setting `responsePeriodDays = 0` is rejected by input validation rules.

**Test Case Procedure:**
1. Log in with an active HR account and open `/offers`.
2. Open the Offer Creation modal and select an eligible candidate.
3. Enter valid salary and future start date.
4. Set Response Period Days to `0`.
5. Click "Lưu bản nháp".
6. Observe client-side and server-side validation messages.

**Expected Results:**
- Submission is blocked.
- Validation message indicates that response period must be at least 1 day.
- Database verification confirms no row was inserted.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Eligible candidate exists.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-008 - Validate Expected Start Date Future Boundary

**Test Case Description:**
Verify that setting the expected start date to today's date is rejected, ensuring offer proposals can only be scheduled for future dates.

**Test Case Procedure:**
1. Log in with an active HR account and open `/offers`.
2. Open the Offer Creation modal and select an eligible candidate.
3. In the Expected Start Date field, select today's date.
4. Fill all other fields with valid values.
5. Click "Lưu bản nháp".
6. Observe validation feedback.

**Expected Results:**
- Submission is rejected.
- Form displays validation error: "Ngày bắt đầu đi làm dự kiến phải từ ngày mai trở đi".
- In the database, no record is saved.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Eligible candidate exists.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

## Function B: HR Draft Management

### TC-INT-009 - Update Draft Proposal Details

**Test Case Description:**
Verify that an HR user can modify the parameters of an existing Draft proposal via the edit modal and keep it in Draft status.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Locate an offer proposal in `Draft` status (e.g. Offer ID 101) on `/offers`.
3. Click the edit icon to open the edit modal (`openEditOfferModal(101)`).
4. Update Proposed Salary to `28,000,000` and Probation Salary to `23,800,000`.
5. Update Work Location to `Branch Office - Da Nang`.
6. Click "Lưu bản nháp" (`isDraft = true`).
7. Query database to verify updated fields and status.

**Expected Results:**
- API response returns HTTP 200 OK with success toast: "Cập nhật bản thảo Offer thành công!".
- In the database:
  - `OfferProposal.OfferStatus` remains `'Draft'`.
  - `ProposedSalary = 28000000.00` and `ProbationSalary = 23800000.00`.
  - `WorkLocation = 'Branch Office - Da Nang'`.
  - `UpdatedAt` timestamp is updated to the current time.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 101 exists in `Draft` status (`IsDeleted = 0`).

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-010 - Submit Updated Draft to Director

**Test Case Description:**
Verify that an HR user can edit a Draft proposal and immediately submit it to the Director, transitioning status from `Draft` to `Pending_Director`.

**Test Case Procedure:**
1. Log in with an active HR account and open `/offers`.
2. Click the edit icon on an existing Draft offer (Offer ID 102).
3. Adjust compensation or benefit terms as desired.
4. Click "Nộp duyệt" (`isDraft = false`).
5. Observe the UI toast notification and inspect database records.

**Expected Results:**
- API response returns HTTP 200 OK with success toast: "Đã nộp trình đề xuất Offer lên Giám đốc thành công!".
- In the database:
  - `OfferProposal.OfferStatus` transitions from `'Draft'` to `'Pending_Director'`.
- An in-app notification is dispatched to the Director role.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 102 exists in `Draft` status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-011 - Soft-Delete Draft Offer Proposal

**Test Case Description:**
Verify that an HR user can soft-delete a Draft proposal, removing it from active display while retaining the record in the database with `IsDeleted = 1`.

**Test Case Procedure:**
1. Log in with an active HR account and open `/offers`.
2. Locate an offer proposal in `Draft` status (Offer ID 103).
3. Click the delete icon (trash can) and confirm the deletion dialog (`confirmDeleteOffer(103)`).
4. Verify table row removal.
5. Query database directly: `SELECT OfferId, OfferStatus, IsDeleted FROM OfferProposal WHERE OfferId = 103;`

**Expected Results:**
- Success toast confirms: "Xóa bản thảo Offer thành công!".
- Offer ID 103 disappears from the active table listing.
- In the database:
  - Record is retained (not physically deleted).
  - `OfferProposal.IsDeleted = 1`.
  - `OfferProposal.OfferStatus` remains `'Draft'`.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 103 exists in `Draft` status with `IsDeleted = 0`.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-012 - Block Editing Non-Draft Offer

**Test Case Description:**
Verify that the system blocks any attempt to edit an Offer Proposal that is not in Draft status (e.g. `Director_Rejected` or `Pending_Director`).

**Test Case Procedure:**
1. Log in with an active HR account and navigate to `/offers`.
2. Identify Offer ID 104 in `Director_Rejected` status and Offer ID 105 in `Pending_Director` status.
3. Confirm that the edit icon button is not rendered in the table row for both offers.
4. Issue a direct HTTP PUT request to `/offers/104` with modified salary data.
5. Observe the HTTP response code and error payload.

**Expected Results:**
- HTTP 400 Bad Request is returned.
- Error message states: "Theo quy tắc GBR-07, chỉ được phép cập nhật Offer khi ở trạng thái thuộc Nhóm A (Draft)".
- In the database, Offer ID 104 remains unmodified.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 104 has status `Director_Rejected`.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-013 - Block Deleting Non-Draft Offer

**Test Case Description:**
Verify that the system blocks deleting an Offer Proposal that has advanced past Draft status (e.g. `Pending_Director`).

**Test Case Procedure:**
1. Log in with an active HR account.
2. Identify Offer ID 106 in `Pending_Director` status.
3. Confirm that the delete icon is not rendered on the UI table row.
4. Send an HTTP DELETE request to `/offers/106` (or POST to `/offers/106/delete`).
5. Observe the error response and database records.

**Expected Results:**
- Request is rejected with HTTP 400 Bad Request.
- Error message states: "Chỉ được xóa khi Offer ở trạng thái Draft".
- In the database, `OfferProposal.IsDeleted` remains `0`.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 106 exists with status `Pending_Director`.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

## Function C: HR Offer List, Search & Sort

### TC-INT-014 - HR Offer List Pagination

**Test Case Description:**
Verify that the HR Offer list correctly paginates active records in 10-item page slices.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Navigate to Offer Management (`/offers`).
3. Verify that the table displays exactly 10 offer rows on page 1.
4. Click the pagination button for page 2 (`/offers?page=1&size=10`).
5. Verify that page 2 loads successfully displaying the next slice of active offers.

**Expected Results:**
- HTTP 200 OK.
- Page 2 displays the remaining offer records without overlapping records from page 1.
- Total records and total pages indicators match active database count.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Database contains at least 15 active (non-deleted) offer proposals.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-015 - Sort HR Offers by Latest

**Test Case Description:**
Verify that selecting "Mới nhất" sorting reorders the HR Offer list descending by creation timestamp and ID.

**Test Case Procedure:**
1. Log in with an active HR account and navigate to `/offers`.
2. Select "Mới nhất" from the time sort dropdown (`/offers?timeSort=LATEST`).
3. Inspect the displayed rows from top to bottom.

**Expected Results:**
- HTTP 200 OK.
- Records are arranged in strictly descending order of `CreatedAt` and `OfferId`.
- The most recently created offer proposal appears as the first row in the table.

**Pre-conditions:**
- GP-01, GP-02, GP-03.
- Multiple offer proposals with distinct creation timestamps exist.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-016 - Sort HR Offers by Earliest

**Test Case Description:**
Verify that selecting "Cũ nhất" sorting reorders the HR Offer list ascending by creation timestamp and ID.

**Test Case Procedure:**
1. Log in with an active HR account and navigate to `/offers`.
2. Select "Cũ nhất" from the time sort dropdown (`/offers?timeSort=EARLIEST`).
3. Inspect the displayed rows from top to bottom.

**Expected Results:**
- HTTP 200 OK.
- Records are arranged in strictly ascending order of `CreatedAt` and `OfferId`.
- The oldest created offer proposal appears as the first row in the table.

**Pre-conditions:**
- GP-01, GP-02, GP-03.
- Multiple offer proposals with distinct creation timestamps exist.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-017 - Filter HR Offers by Status

**Test Case Description:**
Verify that selecting a specific lifecycle status filters the HR Offer list table to show only matching records.

**Test Case Procedure:**
1. Log in with an active HR account and open `/offers`.
2. Select "Chờ Giám đốc duyệt" (`Pending_Director`) from the status filter dropdown.
3. Inspect all rendered table rows.

**Expected Results:**
- HTTP 200 OK.
- Every displayed row has status badge "Chờ Giám đốc duyệt" (`Pending_Director`).
- Offers in `Draft`, `Director_Approved`, `Sent_Candidate`, or `Accepted` are hidden.

**Pre-conditions:**
- GP-01, GP-02, GP-03.
- Offers exist in multiple statuses (`Draft`, `Pending_Director`, `Director_Approved`).

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-018 - Search HR Offers by Keyword

**Test Case Description:**
Verify that entering a search term filters the HR Offer list by candidate name or position title.

**Test Case Procedure:**
1. Log in with an active HR account and open `/offers`.
2. Enter keyword `"Tran"` in the search input box and press Enter.
3. Inspect all matching rows in the table.

**Expected Results:**
- HTTP 200 OK.
- All displayed offers belong to candidates whose names contain `"Tran"` or positions containing `"Tran"`.
- Total search count indicator reflects the matching subset.

**Pre-conditions:**
- GP-01, GP-02, GP-03.
- Database contains candidates matching and not matching `"Tran"`.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-019 - Combine Keyword Search and Status Filter

**Test Case Description:**
Verify that applying both a status filter and a keyword search narrows the HR Offer list to the exact intersection of both criteria.

**Test Case Procedure:**
1. Log in with an active HR account and open `/offers`.
2. Select status "Chờ Giám đốc duyệt" (`Pending_Director`).
3. Enter keyword `"Senior"` in the search input box and submit.
4. Inspect all rendered rows.

**Expected Results:**
- HTTP 200 OK.
- Displayed records simultaneously satisfy both conditions: `offerStatus == 'Pending_Director'` AND (candidate name or position title contains `"Senior"`).
- Global KPI stat counters remain visible and accurate.

**Pre-conditions:**
- GP-01, GP-02, GP-03.
- Database contains offers with status `Pending_Director` containing `"Senior"` and other statuses.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-020 - View Comprehensive Offer Detail

**Test Case Description:**
Verify that an HR user can view the full detail page of an Offer Proposal (`/offers/{id}`), displaying candidate background, interview results, compensation terms, and approval history.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Select Offer ID 107 that has an existing approval history record.
3. Click the view detail action to navigate to `/offers/107`.
4. Inspect the rendered page sections.

**Expected Results:**
- HTTP 200 OK.
- Candidate Profile section displays name, email, phone, and applied position matching database.
- Interview Evaluation section displays final decision, comments, and recommended salary.
- Compensation section displays official salary, probation salary, start date, and location.
- Approval History timeline displays Director name, decision (`Approved`/`Rejected`), comments, and timestamp.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 107 exists with linked candidate, application, and at least one `OfferApproval` record.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

## Function D: Director Review & Decision

### TC-INT-021 - Director Pending Approval Tab View

**Test Case Description:**
Verify that a Director navigating to `/offers` lands on the Director view (`offers/director-list`) with the "Chờ duyệt" tab active by default, and that Draft proposals are excluded.

**Test Case Procedure:**
1. Log in with an active Director account.
2. Navigate to Offer Management (`/offers`).
3. Verify that the rendered template is `offers/director-list`.
4. Verify that the active tab is "Chờ duyệt" (`/offers?status=Pending_Director`).
5. Confirm that all records in this table have status `Pending_Director`.

**Expected Results:**
- HTTP 200 OK.
- Only proposals in `Pending_Director` status are displayed.
- Draft proposals are strictly excluded from the Director table.
- Stat counters show accurate counts for Pending, Approved, and Rejected.

**Pre-conditions:**
- GP-01, GP-02, GP-03.
- Offers exist in `Draft` and `Pending_Director` statuses.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-022 - Director Decision History Tab and Status Filter

**Test Case Description:**
Verify that a Director can view the "Lịch sử duyệt" tab and filter past decisions by specific historical status.

**Test Case Procedure:**
1. Log in with an active Director account and open `/offers`.
2. Click the "Lịch sử duyệt" tab (`/offers?status=HISTORY`).
3. Confirm that historical offers (`Director_Approved`, `Director_Rejected`, `Sent_Candidate`, `Accepted`, `Declined`) appear.
4. Select "Giám đốc từ chối" (`Director_Rejected`) from the status dropdown.

**Expected Results:**
- HTTP 200 OK.
- The history table displays past decided proposals and excludes `Pending_Director` and `Draft`.
- Filtering by `Director_Rejected` displays exclusively rejected proposals.

**Pre-conditions:**
- GP-01, GP-02, GP-03.
- Historical offers exist in `Director_Approved` and `Director_Rejected` statuses.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-023 - Director Approves Offer Proposal

**Test Case Description:**
Verify that a Director can review and approve a pending Offer Proposal, transitioning its status to `Director_Approved`, creating an `OfferApproval` audit row, and notifying HR.

**Test Case Procedure:**
1. Log in with an active Director account and open `/offers`.
2. Locate Offer ID 108 in `Pending_Director` status.
3. Click the "Phê duyệt" button to open the approval modal.
4. Enter review comments: "Đồng ý mức lương theo định biên phòng ban."
5. Confirm the approval (`POST /offers/108/approve`).
6. Query database tables `OfferProposal` and `OfferApproval`.

**Expected Results:**
- Success toast confirms: "Phê duyệt Offer Proposal thành công!".
- On the Director view, Offer ID 108 moves from "Chờ duyệt" to "Lịch sử duyệt".
- In the database:
  - `OfferProposal.OfferStatus = 'Director_Approved'`.
  - A new row is inserted into `OfferApproval` with `Status = 'Approved'` and `DirectorComments = 'Đồng ý mức lương theo định biên phòng ban.'`.
- In-app notification is sent to the proposing HR user.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 108 exists in `Pending_Director` status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-024 - Director Rejects Offer Proposal with Comments

**Test Case Description:**
Verify that a Director can reject a pending Offer Proposal with mandatory feedback comments, transitioning its status to `Director_Rejected`, recording the audit entry, and locking it from HR editing.

**Test Case Procedure:**
1. Log in with an active Director account and open `/offers`.
2. Locate Offer ID 109 in `Pending_Director` status.
3. Click the "Từ chối" button to open the rejection modal.
4. Enter rejection comments: "Mức lương đề xuất vượt khung định biên. Cần thương lượng lại."
5. Confirm the rejection (`POST /offers/109/reject`).
6. Query database and log in as HR to verify that editing is prohibited.

**Expected Results:**
- Success toast confirms: "Đã từ chối Offer Proposal!".
- In the database:
  - `OfferProposal.OfferStatus = 'Director_Rejected'`.
  - New row inserted into `OfferApproval` with `Status = 'Rejected'` and Director's comments.
- HR receives an in-app rejection notification.
- On the HR list and detail view for Offer ID 109, no edit button or "Chỉnh sửa & Trình lại" button is displayed.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 109 exists in `Pending_Director` status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-025 - Block Director Approval on Non-Pending Offer

**Test Case Description:**
Verify that a Director cannot approve an Offer Proposal that is not in `Pending_Director` status (e.g. already `Director_Approved`).

**Test Case Procedure:**
1. Log in with an active Director account.
2. Identify Offer ID 110 already in `Director_Approved` status.
3. Send an HTTP POST request to `/offers/110/approve`.
4. Observe the HTTP response code and error payload.

**Expected Results:**
- Request is rejected with HTTP 400 Bad Request.
- Error message states: "Chỉ được duyệt khi Offer ở trạng thái Pending_Director" (`OFFER_STATUS_INVALID`).
- In the database, Offer ID 110 remains in `Director_Approved` status; no duplicate approval record is created.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 110 is in status `Director_Approved`.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-026 - Block Director Rejection on Non-Pending Offer

**Test Case Description:**
Verify that a Director cannot reject an Offer Proposal that is not in `Pending_Director` status (e.g. currently in `Draft` status).

**Test Case Procedure:**
1. Log in with an active Director account.
2. Identify Offer ID 111 in `Draft` status.
3. Send an HTTP POST request to `/offers/111/reject` with comments.
4. Observe the response and inspect database state.

**Expected Results:**
- Request is rejected with HTTP 400 Bad Request.
- Error message states: "Chỉ được từ chối khi Offer ở trạng thái Pending_Director" (`OFFER_STATUS_INVALID`).
- In the database, Offer ID 111 remains in `Draft` status; no `OfferApproval` record is inserted.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 111 is in status `Draft`.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

## Function E: HR Issue Offer to Candidate

### TC-INT-027 - Issue Approved Offer to Candidate

**Test Case Description:**
Verify that an HR user can issue an approved offer to the candidate, transitioning the offer to `Sent_Candidate`, updating application to `Offered`, calculating response deadline, and sending the offer email.

**Test Case Procedure:**
1. Log in with an active HR account and open `/offers`.
2. Locate Offer ID 112 in `Director_Approved` status with `responsePeriodDays = 5` and expected start date 30 days away.
3. Click the "Phát hành gửi Offer Letter" button (`POST /offers/112/send`).
4. Confirm the issuance confirmation prompt.
5. Query database tables `OfferProposal` and `Application`.

**Expected Results:**
- Success toast confirms: "Phát hành gửi thư mời thành công!".
- In the database:
  - `OfferProposal.OfferStatus = 'Sent_Candidate'`.
  - `OfferProposal.ResponseDeadline` is calculated and set to approximately `NOW() + 5 days`.
  - Linked `Application.ApplicationStatus` transitions from `'Passed'` to `'Offered'`.
- Offer letter email is dispatched to the candidate's email address.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-04, GP-05.
- Offer ID 112 is in `Director_Approved` status with future start date.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-028 - Block Issuing Unapproved Offer

**Test Case Description:**
Verify that an HR user cannot issue an offer letter to a candidate if the proposal is in `Draft` status.

**Test Case Procedure:**
1. Log in with an active HR account and open `/offers`.
2. Identify Offer ID 113 in `Draft` status.
3. Confirm that the "Phát hành gửi Offer Letter" button is not rendered in the UI.
4. Send an explicit HTTP POST request to `/offers/113/send`.
5. Observe the response and database records.

**Expected Results:**
- Request is rejected with HTTP 400 Bad Request.
- Error message states: "Chỉ được gửi offer khi Giám đốc đã duyệt" (`OFFER_NOT_APPROVED`).
- In the database:
  - `OfferProposal.OfferStatus` remains `'Draft'`.
  - `ResponseDeadline` remains `NULL`.
  - Linked `Application.ApplicationStatus` remains unchanged.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 113 is in `Draft` status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-029 - Block Issue when Deadline Exceeds Start Date

**Test Case Description:**
Verify that the system blocks issuing an offer if the calculated response deadline falls on or after the expected start date (`deadline.toLocalDate() >= expectedStartDate`).

**Test Case Procedure:**
1. Log in with an active HR account.
2. Identify Offer ID 114 in `Director_Approved` status where `expectedStartDate` is 3 days in the future, but `responsePeriodDays` is 5 days.
3. Attempt to issue the offer by sending `POST /offers/114/send`.
4. Observe the response and inspect database state.

**Expected Results:**
- Request is rejected with HTTP 400 Bad Request.
- Validation message states: "Hạn chót phản hồi phải diễn ra trước ngày bắt đầu đi làm dự kiến".
- In the database, `OfferProposal.OfferStatus` remains `'Director_Approved'`; `ResponseDeadline` remains `NULL`.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 114 is `Director_Approved` with `responsePeriodDays` >= days until `expectedStartDate`.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

## Function F: Candidate Portal List & Detail

### TC-INT-030 - Candidate Views Active Offer List

**Test Case Description:**
Verify that an authenticated candidate can view their issued offer letters on `/portal/offers`, and that internal drafts and pending approvals are excluded.

**Test Case Procedure:**
1. Log in with an active Candidate account (`candidate_john`).
2. Navigate to the Candidate Portal offers page (`/portal/offers`).
3. Verify that the table renders issued offers (`Sent_Candidate`, `Accepted`, `Declined`).
4. Verify that internal company offers (`Draft`, `Pending_Director`, `Director_Approved`) are not displayed.
5. Check displayed columns: Position Title, Official Salary, Start Date, Response Deadline, and Status.

**Expected Results:**
- HTTP 200 OK.
- Template `offers/candidate-list` renders properly.
- Only offers belonging to `candidate_john` with status `Sent_Candidate`, `Accepted`, or `Declined` are visible.
- No internal company proposals are visible to the candidate.

**Pre-conditions:**
- GP-01, GP-02, GP-03.
- `candidate_john` possesses one offer in `Sent_Candidate` status and one draft exists internally.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-031 - Candidate Views Offer Letter Detail

**Test Case Description:**
Verify that an authenticated candidate can open and view the complete Offer Letter detail page (`/portal/offers/{id}`) with action buttons enabled for an active offer.

**Test Case Procedure:**
1. Log in as Candidate `candidate_john`.
2. On `/portal/offers`, click on Offer ID 115 in `Sent_Candidate` status.
3. Inspect the rendered offer letter components:
   - Position title and department.
   - Official monthly salary and probation salary.
   - Expected start date and work location.
   - Benefits package details.
   - Response deadline formatted timestamp.
   - Action buttons: "Đồng ý nhận việc" and "Từ chối đề xuất".

**Expected Results:**
- HTTP 200 OK.
- Template `offers/candidate-detail` renders with contract terms matching database records.
- Because the offer is active and unexpired, both "Accept" and "Decline" buttons are visible and clickable.

**Pre-conditions:**
- GP-01, GP-02, GP-03.
- Offer ID 115 is in `Sent_Candidate` status, belongs to `candidate_john`, and is unexpired.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-032 - Enforce Cross-Candidate Data Isolation

**Test Case Description:**
Verify data ownership isolation: a candidate attempting to access an offer proposal belonging to a different candidate receives an HTTP 404 Not Found error.

**Test Case Procedure:**
1. Log in as Candidate A (`candidate_a`).
2. Identify Offer ID 116 belonging exclusively to Candidate B (`candidate_b`).
3. While logged in as Candidate A, enter `/portal/offers/116` directly in the browser address bar.
4. Observe the response code and rendered view.

**Expected Results:**
- HTTP 404 Not Found is returned.
- System displays an error message indicating that the requested offer letter does not exist.
- No confidential offer data of Candidate B is exposed to Candidate A.

**Pre-conditions:**
- GP-01, GP-02, GP-03.
- Offer ID 116 belongs to Candidate B; Candidate A is authenticated.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

## Function G: Candidate Accept / Decline Lifecycle

### TC-INT-033 - Candidate Accepts Offer

**Test Case Description:**
Verify candidate acceptance happy path: candidate accepts an active offer, transitioning `OfferStatus` to `Accepted`, `ApplicationStatus` to `Hired`, and displaying the congratulations modal.

**Test Case Procedure:**
1. Log in as the candidate owning active Offer ID 117 (`Sent_Candidate`).
2. Open `/portal/offers/117` and click the "Đồng ý nhận việc" button.
3. In the confirmation dialog, click "Xác nhận nhận việc".
4. Verify UI redirection and congratulations modal dialog.
5. Query database tables `OfferProposal` and `Application`.

**Expected Results:**
- HTTP 302 Redirect to `/portal/offers/117` followed by HTTP 200.
- Congratulations modal dialog appears confirming that the offer has been accepted.
- Action buttons are replaced by status badge "Đã chấp nhận" (`Accepted`).
- In the database:
  - `OfferProposal.OfferStatus = 'Accepted'`.
  - Linked `Application.ApplicationStatus = 'Hired'`.
- In-app notification is sent to HR notifying them of candidate acceptance.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 117 is in `Sent_Candidate` status and unexpired.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-034 - Candidate Declines Offer with Reason

**Test Case Description:**
Verify that a candidate can decline an active offer with a detailed text reason, transitioning `OfferStatus` to `Declined`, `ApplicationStatus` to `Rejected`, storing the reason, and notifying HR.

**Test Case Procedure:**
1. Log in as candidate owning active Offer ID 118 (`Sent_Candidate`).
2. Open `/portal/offers/118` and click "Từ chối đề xuất".
3. On `/portal/offers/118/decline`, enter reason: "Đã nhận được offer khác với mức đãi ngộ phù hợp hơn."
4. Click "Xác nhận từ chối" (`POST /portal/offers/118/decline`).
5. Verify UI confirmation and query database records.

**Expected Results:**
- HTTP 302 Redirect followed by HTTP 200.
- Confirmation banner displays: "Bạn đã từ chối lời mời nhận việc này".
- In the database:
  - `OfferProposal.OfferStatus = 'Declined'`.
  - `OfferProposal.DeclineReason = 'Đã nhận được offer khác với mức đãi ngộ phù hợp hơn.'`.
  - Linked `Application.ApplicationStatus = 'Rejected'`.
- In-app notification is sent to HR with the decline feedback.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 118 is in `Sent_Candidate` status and unexpired.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-035 - Candidate Declines Offer without Reason

**Test Case Description:**
Verify that a candidate can decline an active offer without providing any optional decline reason, correctly transitioning statuses and persisting `DeclineReason` as `NULL`.

**Test Case Procedure:**
1. Log in as candidate owning active Offer ID 119 (`Sent_Candidate`).
2. Open `/portal/offers/119` and click "Từ chối đề xuất".
3. On `/portal/offers/119/decline`, leave the reason textarea completely blank.
4. Click "Xác nhận từ chối".
5. Verify UI confirmation and query database records.

**Expected Results:**
- HTTP 302 Redirect followed by HTTP 200.
- Decline succeeds without requiring text input.
- In the database:
  - `OfferProposal.OfferStatus` remains `'Declined'`.
  - `OfferProposal.DeclineReason` is stored as `NULL`.
  - Linked `Application.ApplicationStatus` remains `'Rejected'`.
- Notification is dispatched to HR.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 119 is in `Sent_Candidate` status and unexpired.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-036 - Enforce Decline Page Navigation Guard

**Test Case Description:**
Verify that navigating directly to `/portal/offers/{id}/decline` for an offer that has already been answered is blocked and redirected to detail view with an error banner.

**Test Case Procedure:**
1. Log in as candidate owning Offer ID 120 in `Accepted` status.
2. In the browser address bar, navigate directly to `/portal/offers/120/decline`.
3. Observe the redirection and flash message.

**Expected Results:**
- Controller redirects immediately to `/portal/offers/120`.
- Error banner displays: "Thư mời đã được phản hồi trước đó và không thể chỉnh sửa".
- The decline survey form is not rendered.

**Pre-conditions:**
- GP-01, GP-02, GP-03.
- Offer ID 120 has status `Accepted`.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-037 - Block Response after Deadline Expiration

**Test Case Description:**
Verify that a candidate cannot accept or decline an offer once the response deadline has elapsed (`LocalDateTime.now() > responseDeadline`).

**Test Case Procedure:**
1. Ensure Offer ID 121 has status `Sent_Candidate` with `ResponseDeadline` set to 1 hour in the past.
2. Log in as the candidate owning Offer ID 121 and open `/portal/offers/121`.
3. Verify that the UI displays an expired deadline banner and Accept/Decline buttons are disabled.
4. Send an explicit HTTP POST to `/portal/offers/121/accept`.
5. Observe the response and database records.

**Expected Results:**
- Request is rejected.
- Error message displays: "Thư mời làm việc đã hết hạn phản hồi".
- In the database:
  - `OfferProposal.OfferStatus` remains `'Sent_Candidate'`.
  - `Application.ApplicationStatus` remains `'Offered'`.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 121 has status `Sent_Candidate` and `ResponseDeadline < NOW()`.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-038 - Block Response after Expected Start Date

**Test Case Description:**
Verify that a candidate cannot accept an offer if the expected start date has passed (`expectedStartDate < LocalDate.now()`).

**Test Case Procedure:**
1. Ensure Offer ID 122 has status `Sent_Candidate` with `ExpectedStartDate` set to yesterday's date.
2. Log in as the candidate owning Offer ID 122.
3. Submit an HTTP POST request to `/portal/offers/122/accept`.
4. Observe the response and inspect database state.

**Expected Results:**
- Request is rejected.
- Error message displays: "Ngày bắt đầu đi làm dự kiến đã qua, không thể chấp nhận thư mời".
- In the database, `OfferProposal.OfferStatus` remains `'Sent_Candidate'`.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 122 has status `Sent_Candidate` and `ExpectedStartDate < TODAY`.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-039 - Block Duplicate Candidate Response

**Test Case Description:**
Verify that submitting a second response on an already answered offer proposal is rejected.

**Test Case Procedure:**
1. Log in as candidate owning Offer ID 123.
2. Accept the offer via `POST /portal/offers/123/accept` (status becomes `Accepted`).
3. Immediately send a second HTTP POST request to `/portal/offers/123/decline`.
4. Observe the response and query database records.

**Expected Results:**
- Second request is rejected.
- Error message states: "Thư mời đã được phản hồi trước đó và không thể chỉnh sửa".
- In the database:
  - `OfferProposal.OfferStatus` remains `'Accepted'`.
  - `Application.ApplicationStatus` remains `'Hired'`.
  - `DeclineReason` remains `NULL`.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 123 was previously accepted.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-040 - Prevent Concurrent Accept and Decline Conflict

**Test Case Description:**
Verify system concurrency protection when a candidate triggers near-simultaneous Accept and Decline requests on the same active offer using two browser sessions.

**Test Case Procedure:**
1. Open two separate browser sessions (Session A and Session B) authenticated as the same candidate.
2. In Session A, open Offer ID 124 (`/portal/offers/124`) and prepare to click "Xác nhận nhận việc".
3. In Session B, open the decline page (`/portal/offers/124/decline`) and prepare to click "Xác nhận từ chối".
4. Click both buttons as simultaneously as possible.
5. Refresh both sessions and inspect the database state.

**Expected Results:**
- Exactly one of the two requests succeeds and commits to the database:
  - If Accept commits first: Session A displays the congratulations modal; Session B displays an error message indicating the offer was already updated.
  - If Decline commits first: Session B displays decline confirmation; Session A displays the update conflict error.
- The losing session fails gracefully without an unhandled system crash.
- In the database, the record reaches exactly one valid terminal state (`Accepted` or `Declined`); no split-brain data state exists.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 124 is in `Sent_Candidate` status and unexpired.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-041 - Validate Decline Reason Length Boundary

**Test Case Description:**
Verify that the system validates and restricts candidate decline feedback to the database column capacity of 1000 characters (`DeclineReason NVARCHAR(1000)`).

**Test Case Procedure:**
1. Log in as candidate owning active Offer ID 125.
2. Open `/portal/offers/125/decline`.
3. Paste a text string of 1,050 characters into the decline reason textarea.
4. Click "Xác nhận từ chối".
5. Observe error handling and database state.

**Expected Results:**
- System should validate input length gracefully and display an error message stating that the decline reason cannot exceed 1000 characters.
- Offer should remain in `Sent_Candidate` status until valid input is provided.
- No unhandled SQL Server exception or HTTP 500 error should occur.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Offer ID 125 is in `Sent_Candidate` status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**
Known Risk / Potential Defect: `CandidateOfferController` lacks `@Size(max = 1000)` validation, `CandidateOfferServiceImpl` does not truncate input, and `candidate-decline.html` lacks `maxlength="1000"`. Currently, submitting >1000 characters causes a SQL Server string truncation error resulting in an unhandled HTTP 500 error.

---

## Function H: Role Authorization & Security

### TC-INT-042 - Enforce HR Forbidden from Approving Offer

**Test Case Description:**
Verify cross-role restriction: an HR user must be forbidden from approving an Offer Proposal. Only the Director role is authorized to approve offers.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Identify Offer ID 126 in `Pending_Director` status.
3. Confirm that the approval button is not rendered in the HR interface.
4. Send an explicit HTTP POST request to `/offers/126/approve` using HR session credentials.
5. Observe the HTTP response code and database records.

**Expected Results:**
- HTTP 403 Forbidden is returned.
- In the database:
  - `OfferProposal.OfferStatus` remains `'Pending_Director'`.
  - No row is inserted into `OfferApproval`.
  - No approval notification is dispatched.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- User possesses only `ROLE_HR`; Offer ID 126 is in `Pending_Director` status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**
Known Risk / Potential Defect: `SecurityConfig` allows `ROLE_HR` under `/offers/**`, and `OfferController.approveOfferJson` lacks `@PreAuthorize("hasRole('DIRECTOR')")`. Currently, an HR user sending a direct POST to `/offers/{id}/approve` succeeds in approving the proposal and records the HR user as the approving Director in `OfferApproval`.

---

### TC-INT-043 - Enforce HR Forbidden from Rejecting Offer

**Test Case Description:**
Verify cross-role restriction: an HR user must be forbidden from rejecting an Offer Proposal. Only the Director role is authorized to reject offers.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Identify Offer ID 127 in `Pending_Director` status.
3. Send an explicit HTTP POST request to `/offers/127/reject` with comments using HR credentials.
4. Observe the response and verify database state.

**Expected Results:**
- HTTP 403 Forbidden is returned.
- In the database:
  - `OfferProposal.OfferStatus` remains `'Pending_Director'`.
  - No rejection record is added to `OfferApproval`.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- User possesses only `ROLE_HR`; Offer ID 127 is in `Pending_Director` status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**
Known Risk / Potential Defect: `OfferController.rejectOfferJson` lacks method-level role enforcement. Currently, an HR user issuing a direct POST to `/offers/{id}/reject` transitions the offer to `Director_Rejected`.

---

### TC-INT-044 - Enforce Director Forbidden from Creating Offer

**Test Case Description:**
Verify cross-role restriction: a Director user must be forbidden from creating new Offer Proposals. Offer creation belongs exclusively to HR.

**Test Case Procedure:**
1. Log in with an active Director account.
2. Confirm that the "Tạo đề xuất mới" button is not visible on `offers/director-list`.
3. Attempt to access `GET /offers/create` directly in the browser.
4. Attempt to send an HTTP POST request to `/offers/create` with new offer data using Director credentials.
5. Observe the response.

**Expected Results:**
- HTTP 403 Forbidden is returned for both GET and POST requests.
- Director is prevented from accessing the creation form and submitting proposals.
- In the database, no new `OfferProposal` row is created.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- User possesses only `ROLE_DIRECTOR`.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**
Known Risk / Potential Defect: `SecurityConfig` allows `ROLE_DIRECTOR` under `/offers/**`. Currently, a Director can navigate directly to `/offers/create` and successfully create proposals with `ProposedBy = Director`.

---

### TC-INT-045 - Enforce Director Forbidden from Editing Draft

**Test Case Description:**
Verify cross-role restriction: a Director user must be forbidden from editing Draft offer proposals. Draft maintenance belongs exclusively to HR.

**Test Case Procedure:**
1. Log in with an active Director account.
2. Identify Offer ID 128 in `Draft` status.
3. Send an HTTP PUT request to `/offers/128` (or POST to `/offers/128/edit`) with modified compensation values using Director credentials.
4. Observe the response and inspect database records.

**Expected Results:**
- HTTP 403 Forbidden is returned.
- In the database, Offer ID 128 remains unmodified.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- User possesses `ROLE_DIRECTOR`; Offer ID 128 is in `Draft` status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**
Known Risk / Potential Defect: Currently, `OfferController` and `OfferServiceImpl` do not verify that the actor is HR during updates; a Director submitting an edit to a Draft offer succeeds.

---

### TC-INT-046 - Enforce Director Forbidden from Deleting Draft

**Test Case Description:**
Verify cross-role restriction: a Director user must be forbidden from deleting Draft offer proposals.

**Test Case Procedure:**
1. Log in with an active Director account.
2. Identify Offer ID 129 in `Draft` status (`IsDeleted = 0`).
3. Send an HTTP DELETE request to `/offers/129` (or POST to `/offers/129/delete`) using Director credentials.
4. Observe the response and query database records.

**Expected Results:**
- HTTP 403 Forbidden is returned.
- In the database, Offer ID 129 remains active (`IsDeleted = 0`).

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- User possesses `ROLE_DIRECTOR`; Offer ID 129 is in `Draft` status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**
Known Risk / Potential Defect: Currently allows any user authorized under `/offers/**` to soft-delete drafts, enabling Directors to delete draft offers if invoked directly.

---

### TC-INT-047 - Enforce Director Forbidden from Issuing Offer

**Test Case Description:**
Verify cross-role restriction: a Director user must be forbidden from issuing an offer letter to a candidate. Issuance is strictly an HR responsibility.

**Test Case Procedure:**
1. Log in with an active Director account.
2. Identify Offer ID 130 in `Director_Approved` status.
3. Send an HTTP POST request to `/offers/130/send` using Director credentials.
4. Observe the response and inspect database records.

**Expected Results:**
- HTTP 403 Forbidden is returned.
- In the database:
  - `OfferProposal.OfferStatus` remains `'Director_Approved'`.
  - `ResponseDeadline` remains `NULL`.
  - Linked `Application.ApplicationStatus` remains unchanged.
  - No offer letter email is dispatched.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- User possesses `ROLE_DIRECTOR`; Offer ID 130 is in `Director_Approved` status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**
Known Risk / Potential Defect: `OfferController.sendOfferToCandidate` does not check for `ROLE_HR`. A Director sending a POST to `/{id}/send` currently succeeds and issues the offer.

---

### TC-INT-048 - Enforce Candidate Isolated from Internal Routes

**Test Case Description:**
Verify candidate boundary isolation: a candidate user must be strictly forbidden from accessing internal company offer management endpoints under `/offers/**`.

**Test Case Procedure:**
1. Log in with an active Candidate account (`ROLE_CANDIDATE`).
2. Attempt to navigate to the internal HR offer list: `GET /offers`.
3. Attempt to access `GET /offers/create`.
4. Attempt to send a POST request to `/offers/create`.
5. Observe the HTTP response for each attempt.

**Expected Results:**
- All requests are rejected with HTTP 403 Forbidden by Spring Security.
- The candidate cannot view internal lists, creator tools, or company salary proposals.
- No internal data is leaked.

**Pre-conditions:**
- GP-01, GP-02, GP-03.
- User possesses only `ROLE_CANDIDATE`.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-049 - Enforce Unauthenticated Guest Redirect to Login

**Test Case Description:**
Verify unauthenticated access control: an unauthenticated guest accessing any protected offer URL is redirected to the login page.

**Test Case Procedure:**
1. Open a clean browser session with no active session cookies.
2. Attempt to navigate to an internal offer URL: `GET /offers`.
3. Attempt to navigate to a candidate portal URL: `GET /portal/offers`.
4. Verify HTTP response codes and redirection URLs.

**Expected Results:**
- Both requests return HTTP 302 Redirect to `/login`.
- User is presented with the RMS login interface.
- Protected content is completely inaccessible without successful authentication.

**Pre-conditions:**
- GP-01, GP-02.
- User is unauthenticated (guest).

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-050 - Enforce CSRF Token Protection

**Test Case Description:**
Verify CSRF security protection: state-modifying requests (`POST`, `PUT`, `DELETE`) to Offer endpoints without a valid CSRF token are rejected with HTTP 403 Forbidden.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Craft an HTTP POST request to `/offers/create` omitting the `_csrf` token header or parameter.
3. Send the request and inspect the HTTP status.

**Expected Results:**
- Spring Security CSRF filter intercepts the request.
- HTTP 403 Forbidden is returned.
- No state change or database persistence occurs.

**Pre-conditions:**
- GP-01, GP-02, GP-03.
- Active authenticated session.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

## Function I: Offer Export to Excel

### TC-INT-051 - Export Filtered Offers to Excel

**Test Case Description:**
Verify that an HR user can export offer proposals matching active search and status filters to an Excel file (`OfferExportScope.FILTERED`).

**Test Case Procedure:**
1. Log in with an active HR account and navigate to `/offers`.
2. Filter the list by status "Chờ Giám đốc duyệt" (`Pending_Director`).
3. Click the "Xuất Excel" button to open the export modal.
4. Select scope radio "Danh sách đang lọc" (`FILTERED`).
5. Select columns: Offer ID, Candidate Name, Email, Offered Position, Proposed Salary, Probation Salary, Status, Expected Start Date.
6. Click "Xuất Excel" (`POST /offers/export`).
7. Inspect the downloaded workbook.

**Expected Results:**
- HTTP 200 OK with `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.
- Content-Disposition specifies filename format `offers_filtered_<YYYY-MM-DD>.xlsx`.
- Workbook contains "Offers" sheet containing only rows matching `offerStatus == 'Pending_Director'`.
- "Summary" sheet displays export metadata.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-04.
- Offers exist matching status `Pending_Director`.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-052 - Export Selected Offers to Excel

**Test Case Description:**
Verify that an HR user can export specific offers selected via table checkboxes (`OfferExportScope.SELECTED`).

**Test Case Procedure:**
1. Log in with an active HR account and open `/offers`.
2. Select checkboxes for two specific offers (Offer ID 101 and Offer ID 103).
3. Click "Xuất Excel" to open the export modal.
4. Choose scope "Các dòng đã chọn" (`SELECTED`).
5. Keep default columns selected and click "Xuất Excel".
6. Inspect the downloaded workbook.

**Expected Results:**
- HTTP 200 OK.
- Filename matches `offers_selected_2_<YYYY-MM-DD>.xlsx`.
- "Offers" sheet contains exactly 2 data rows corresponding to Offer IDs 101 and 103.
- "Summary" sheet displays metadata indicating `Scope: SELECTED, Total Records: 2`.

**Pre-conditions:**
- GP-01, GP-02, GP-03.
- At least 2 active offers exist.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-053 - Export All Active Offers to Excel

**Test Case Description:**
Verify that an HR user can export all non-deleted offers in the system (`OfferExportScope.ALL`), strictly excluding soft-deleted records.

**Test Case Procedure:**
1. Log in with an active HR account and open `/offers`.
2. Open the Export modal and select scope "Tất cả đề xuất" (`ALL`).
3. Click "Chọn tất cả" to include all default and extended columns.
4. Click "Xuất Excel".
5. Query database: `SELECT COUNT(*) FROM OfferProposal WHERE IsDeleted = 0;`
6. Compare count with the exported Excel sheet.

**Expected Results:**
- HTTP 200 OK.
- Filename matches `offers_<YYYY-MM-DD>.xlsx`.
- Data rows in "Offers" sheet exactly equal active database count.
- Soft-deleted offers (`IsDeleted = 1`) are strictly excluded.
- Extended columns (Benefits Package, Director Decision, Approval Time, Decline Reason) are present.

**Pre-conditions:**
- GP-01, GP-02, GP-03, GP-05.
- Database contains active offers and at least one soft-deleted offer.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-054 - Validate Empty Selection on Export

**Test Case Description:**
Verify that attempting to export with `OfferExportScope.SELECTED` without selecting any offers triggers a validation error.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Send an HTTP POST request to `/offers/export` with:
   - `scope: "SELECTED"`
   - `offerIds: []` (empty list)
   - `columns: ["offerId", "candidateName"]`
3. Observe the response status and error body.

**Expected Results:**
- HTTP 400 Bad Request is returned.
- Response contains error code `"EMPTY_SELECTED_OFFERS"` and error message stating that at least one offer must be selected.
- No Excel file is generated or downloaded.

**Pre-conditions:**
- GP-01, GP-02, GP-03.
- HR user authenticated.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-055 - Enforce Director Forbidden from Exporting Data

**Test Case Description:**
Verify authorization rule: a Director user must be forbidden from exporting Offer data. In accordance with RMS access governance, export is strictly restricted to HR.

**Test Case Procedure:**
1. Log in with an active Director account.
2. Confirm that `offers/director-list` provides no export button or export modal.
3. Send a direct HTTP POST request to `/offers/export` with valid export payload using Director credentials.
4. Observe the response code.

**Expected Results:**
- HTTP 403 Forbidden is returned.
- No Excel file is generated or downloaded.
- Offer data cannot be exfiltrated via export by the Director role.

**Pre-conditions:**
- GP-01, GP-02, GP-03.
- User possesses `ROLE_DIRECTOR`.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**
Known Risk / Authorization Discrepancy: `OfferController.exportOffers` explicitly whitelists `ROLE_DIRECTOR` (line 470), allowing Directors to export data if invoked via direct POST, even though `director-list.html` hides the export button. Backend security should be updated to restrict export to `ROLE_HR` and `SYSTEM_ADMIN` only.

---

## Coverage Summary

| Function Group | Number of TCs | ID Range |
|---|---:|---|
| A. HR Offer Creation | 8 | TC-INT-001 - TC-INT-008 |
| B. HR Draft Management | 5 | TC-INT-009 - TC-INT-013 |
| C. HR Offer List, Search & Sort | 7 | TC-INT-014 - TC-INT-020 |
| D. Director Review & Decision | 6 | TC-INT-021 - TC-INT-026 |
| E. HR Issue Offer to Candidate | 3 | TC-INT-027 - TC-INT-029 |
| F. Candidate Portal List & Detail | 3 | TC-INT-030 - TC-INT-032 |
| G. Candidate Accept / Decline Lifecycle | 9 | TC-INT-033 - TC-INT-041 |
| H. Role Authorization & Security | 9 | TC-INT-042 - TC-INT-050 |
| I. Offer Export to Excel | 5 | TC-INT-051 - TC-INT-055 |
| **TOTAL** | **55** | **TC-INT-001 - TC-INT-055** |

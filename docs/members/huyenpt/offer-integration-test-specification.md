# Offer Management - Manual Integration Test Specification

## Test Information

| Field | Value |
|---|---|
| Feature | Offer Management |
| Test Requirement | Verify the complete Offer Proposal lifecycle across HR, Director, and Candidate roles, including creation, draft management, approval/rejection, issuance, candidate response, deadline control, authorization, persistence, and Excel export. |
| Number of TCs | 48 |
| Round 1 Passed | 0 |
| Round 1 Failed | 0 |
| Round 1 Pending | 48 |
| Round 1 N/A | 0 |
| Round 2 Passed | 0 |
| Round 2 Failed | 0 |
| Round 2 Pending | 48 |
| Round 2 N/A | 0 |
| Round 3 Passed | 0 |
| Round 3 Failed | 0 |
| Round 3 Pending | 48 |
| Round 3 N/A | 0 |

---

## Function A: HR Offer Creation

### TC-INT-001 - Save Draft Offer Proposal

**Test Case Description:**
Verify that an HR user can create and save a new Offer Proposal in Draft status for an interview-passed candidate via the offer creation form or modal, without submitting it to the Director.

**Test Case Procedure:**
1. Start the RMS web application and ensure database connectivity.
2. Log in with an active HR account (e.g., username hr_manager).
3. Navigate to Offer Management (/offers).
4. Click the "Create Offer" button to open the creation modal or page (/offers/create).
5. Select an eligible candidate who has passed the interview stage (FinalDecision = 'Passed') and has no existing offer.
6. Verify auto-populated values (offered position, recommended salary, 85% probation salary).
7. Ensure valid values are entered:
   - Proposed Salary: 25,000,000
   - Probation Salary: 21,250,000 (85%)
   - Probation Days: 60
   - Expected Start Date: A date 30 days in the future
   - Work Location: Head Office - Hanoi
   - Response Period Days: 5
8. Click "Save Draft" to submit the form with isDraft = true.
9. Verify the UI response and redirection to /offers.
10. Query the database to verify persistence.

**Expected Results:**
- HTTP status 201 Created (JSON) or 302 Redirect to /offers (Form).
- A flash/alert message is displayed confirming that the draft offer has been saved successfully.
- On the Offer list page, the new offer proposal appears with status badge "Draft".
- In the database:
  - A new row is inserted into OfferProposal.
  - OfferStatus = 'Draft'.
  - IsDeleted = 0.
  - ResponsePeriodDays = 5.
  - ResponseDeadline = NULL.
  - ProposedBy = User ID of the logged-in HR user.
  - Linked Application.ApplicationStatus remains unchanged ('Passed').

**Pre-conditions:**
- System has at least one candidate with InterviewFinalResult.FinalDecision = 'Passed' and no existing OfferProposal.
- HR user account is active and logged in.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-002 - Submit Offer Proposal to Director

**Test Case Description:**
Verify that an HR user can directly submit a newly created Offer Proposal to the Director for approval, transitioning the offer to Pending_Director and triggering appropriate notifications.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Open Offer Management and click "Create Offer" (/offers/create).
3. Select an interview-passed candidate without an existing offer.
4. Fill in all required fields with valid business values:
   - Position: Senior Backend Developer
   - Proposed Salary: 30,000,000
   - Probation Salary: 25,500,000 (85%)
   - Probation Days: 60
   - Expected Start Date: A date 45 days in the future
   - Work Location: Head Office - Hanoi
   - Benefits Package: Full healthcare package, laptop subsidy
   - Response Period Days: 7
5. Click "Submit to Director" with isDraft = false.
6. Verify the UI response and redirection to /offers.
7. Query the database to verify offer state and notification records.

**Expected Results:**
- HTTP status 201 Created or 302 Redirect to /offers.
- A flash message confirms that the offer proposal has been successfully submitted to the Director.
- On the HR Offer list page, the record appears with status "Pending Director" (Pending_Director).
- In the database:
  - OfferProposal.OfferStatus = 'Pending_Director'.
  - OfferProposal.IsDeleted = 0.
  - OfferProposal.ResponsePeriodDays = 7.
  - OfferProposal.ResponseDeadline = NULL.
- An in-app system notification is dispatched to the Director role regarding the pending proposal.

**Pre-conditions:**
- An interview-passed candidate exists without an existing offer proposal.
- Active Director user exists in the system to receive notifications.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-003 - Single Active Offer Rule GBR-07

**Test Case Description:**
Verify rule GBR-07: an HR user cannot create a second Offer Proposal for an application that already has an existing active or draft offer.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Ensure Candidate A already has an existing Offer Proposal (e.g., in status Draft or Pending_Director).
3. Open /offers/create in the browser.
4. Verify that Candidate A is filtered out and does not appear in the candidate selection dropdown.
5. Attempt to bypass the UI dropdown by sending a direct HTTP POST to /offers/create with applicationId set to Candidate A's application ID.
6. Observe the HTTP response code, error payload, and database records.

**Expected Results:**
- The request is rejected with HTTP 400 Bad Request.
- The response returns an error message indicating that an offer proposal already exists for this application under rule GBR-07 and must be edited instead of created anew (OFFER_LOCKED_STATE).
- In the database, no new row is inserted into OfferProposal. The existing offer record remains unmodified.

**Pre-conditions:**
- Candidate A has an application linked to an existing non-deleted OfferProposal.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-004 - Salary Compliance Rule BR-OFF-01

**Test Case Description:**
Verify compliance rule BR-OFF-01: probation salary must be at least 85% of the proposed official salary; submissions below 85% must be rejected.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Open the Offer creation form (/offers/create).
3. Select an eligible interview-passed candidate.
4. Enter Proposed Salary = 20,000,000.
5. Enter Probation Salary = 16,000,000 (which is 80%, strictly below the legal 85% threshold of 17,000,000).
6. Fill in valid values for all other fields.
7. Click "Save Draft" or "Submit to Director".
8. Observe the validation response.

**Expected Results:**
- The form submission is rejected (HTTP 400 Bad Request for JSON or form redisplay with error for HTML).
- A field validation message is displayed indicating that the probation salary must be at least 85% of the official proposed salary in compliance with labor regulations.
- In the database, no new OfferProposal row is created.

**Pre-conditions:**
- An eligible interview-passed candidate exists without an offer.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-005 - Valid ResponsePeriodDays Upper Boundary

**Test Case Description:**
Verify that the maximum allowed response period boundary of 60 days (responsePeriodDays = 60) is accepted and persisted when the expected start date is set sufficiently far in the future.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Navigate to the Offer creation form (/offers/create).
3. Select an eligible interview-passed candidate.
4. Set Proposed Salary = 20,000,000 and Probation Salary = 17,000,000 (85%).
5. Set responsePeriodDays = 60 (the maximum boundary).
6. Set expectedStartDate to a date 75 days in the future (ensuring deadline < start date).
7. Submit the form to save as Draft (isDraft = true).
8. Verify UI confirmation and query the database.

**Expected Results:**
- The submission succeeds with HTTP 201 Created or 302 Redirect.
- Success notification is displayed confirming offer creation.
- In the database, OfferProposal.ResponsePeriodDays is stored exactly as 60.
- OfferProposal.OfferStatus = 'Draft'.

**Pre-conditions:**
- Eligible interview-passed candidate exists.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-006 - Invalid ResponsePeriodDays Lower Boundary

**Test Case Description:**
Verify that setting responsePeriodDays = 0 (or a negative value) is rejected by input validation rules.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Open the Offer creation form (/offers/create).
3. Select an eligible candidate.
4. Enter valid salary, title, and start date.
5. In the Response Period field, enter 0.
6. Submit the form.
7. Observe the client-side and server-side validation feedback.

**Expected Results:**
- The submission is rejected before database insertion.
- The form displays an error message indicating that the response period must be at least 1 day.
- In the database, no OfferProposal row is inserted.

**Pre-conditions:**
- HR user logged in.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-007 - Expected Start Date Past / Today Validation

**Test Case Description:**
Verify that an expected start date set to today's date or a past date is rejected, ensuring offer proposals can only be scheduled for future start dates.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Open the Offer creation form (/offers/create).
3. Select an eligible candidate.
4. Fill in valid salary and location details.
5. In the Expected Start Date field, select yesterday's date or today's date.
6. Attempt to submit the form.
7. Observe validation behavior.

**Expected Results:**
- Submission is rejected (HTTP 400 or form redisplay with field error).
- A validation error is displayed stating that the expected start date must be a future date greater than today.
- In the database, no record is saved.

**Pre-conditions:**
- HR user logged in.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

## Function B: HR Draft Management

### TC-INT-008 - Update Draft Proposal and Keep Draft Status

**Test Case Description:**
Verify that an HR user can modify the parameters of an existing Draft proposal and keep it in Draft status.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Identify an existing Offer Proposal in Draft status (e.g., Offer ID 101).
3. On the Offer list page or detail page, click the edit icon to open /offers/101/edit.
4. Modify the proposed salary to 28,000,000 and probation salary to 23,800,000 (85%).
5. Update work location to Branch Office - Da Nang.
6. Click "Save Draft" with isDraft = true.
7. Verify the UI response and query the database.

**Expected Results:**
- HTTP 200 OK (JSON) or 302 Redirect to /offers/101.
- A success message confirms that the draft offer has been updated.
- On the detail screen, the updated salary (28,000,000) and location (Branch Office - Da Nang) are displayed.
- In the database:
  - OfferProposal.OfferStatus remains 'Draft'.
  - OfferProposal.ProposedSalary = 28000000.00.
  - OfferProposal.WorkLocation = 'Branch Office - Da Nang'.
  - OfferProposal.UpdatedAt timestamp is refreshed to the current time.

**Pre-conditions:**
- An offer proposal exists in Draft status (isDeleted = 0).

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-009 - Submit Updated Draft to Director

**Test Case Description:**
Verify that an HR user can edit a Draft proposal and immediately submit it to the Director, transitioning its status from Draft to Pending_Director.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Open an existing Draft offer for editing (/offers/{id}/edit).
3. Update the benefits package or salary fields as required.
4. Click "Submit to Director" with isDraft = false.
5. Verify UI redirection and notification dispatch.
6. Verify database records.

**Expected Results:**
- HTTP 200 OK or 302 Redirect to /offers/{id}.
- A success notification confirms submission to the Director.
- Status badge updates from "Draft" to "Pending Director" (Pending_Director).
- In the database:
  - OfferProposal.OfferStatus transitions from 'Draft' to 'Pending_Director'.
  - Updated fields are saved.
- An in-app notification is sent to the Director.

**Pre-conditions:**
- An offer proposal exists in Draft status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-010 - Soft-Delete Draft Offer Proposal

**Test Case Description:**
Verify that an HR user can soft-delete an existing Draft proposal, removing it from the active display list while retaining the record in the database with IsDeleted = 1.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Locate an offer proposal in Draft status (e.g., Offer ID 102) on the Offer list page.
3. Click the delete icon (trash can) and confirm the deletion dialog (POST /offers/102/delete or DELETE /offers/102).
4. Verify redirection to /offers.
5. Check that Offer ID 102 is no longer visible in the Offer list table.
6. Query the database directly:
   SELECT OfferId, OfferStatus, IsDeleted FROM OfferProposal WHERE OfferId = 102;

**Expected Results:**
- HTTP 200 OK or 302 Redirect to /offers.
- Success notification is displayed confirming draft deletion.
- Offer ID 102 disappears from the active Offer listing.
- In the database:
  - The record still exists (not physically deleted).
  - OfferProposal.IsDeleted is updated to 1 (true).
  - OfferProposal.OfferStatus remains 'Draft'.

**Pre-conditions:**
- An offer proposal in Draft status exists.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-011 - Edit Forbidden for Non-Draft Offer

**Test Case Description:**
Verify that the system blocks any attempt to edit an Offer Proposal that has already advanced past the Draft stage (e.g., Pending_Director, Director_Approved, or Sent_Candidate).

**Test Case Procedure:**
1. Log in with an active HR account.
2. Identify an offer proposal in Pending_Director or Director_Approved status (e.g., Offer ID 103).
3. Note that the edit button is not displayed in the UI table for this record.
4. Attempt to access /offers/103/edit directly in the browser or issue a PUT /offers/103 request with modified data.
5. Observe the error response and check database integrity.

**Expected Results:**
- The request is rejected with HTTP 400 Bad Request or redirected with an error banner.
- An error message states that only draft proposals can be edited (OFFER_STATUS_INVALID).
- In the database, Offer ID 103 remains unchanged; no fields are updated.

**Pre-conditions:**
- An offer proposal exists with status Pending_Director or Director_Approved.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-012 - Delete Forbidden for Non-Draft Offer

**Test Case Description:**
Verify that the system blocks any attempt to delete an Offer Proposal that is not in Draft status.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Identify an offer proposal in Pending_Director or Director_Approved status (e.g., Offer ID 104).
3. Send an HTTP POST request to /offers/104/delete (or DELETE /offers/104).
4. Observe the response and verify database state.

**Expected Results:**
- The request is rejected with HTTP 400 Bad Request or redirected with an error message.
- An error banner states that only draft offers can be deleted (OFFER_NOT_DRAFT).
- In the database, OfferProposal.IsDeleted remains 0 (false). The record is not deleted.

**Pre-conditions:**
- An offer proposal exists with status != 'Draft'.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

## Function C: HR Offer List, Search & Detail

### TC-INT-013 - HR Offer List Pagination and Time Sorting

**Test Case Description:**
Verify that the HR Offer list (/offers) correctly paginates active records and correctly applies time-based sorting (LATEST vs. EARLIEST).

**Test Case Procedure:**
1. Log in with an active HR account.
2. Navigate to Offer Management (/offers).
3. Verify that the table displays up to 10 offers per page by default.
4. Navigate to page 2 via pagination controls (/offers?page=1&size=10).
5. Select "Latest" from the time sort dropdown (/offers?timeSort=LATEST).
6. Verify that records are displayed in descending order of CreatedAt and OfferId.
7. Select "Earliest" from the time sort dropdown (/offers?timeSort=EARLIEST).
8. Verify that records are displayed in ascending order of CreatedAt and OfferId.

**Expected Results:**
- HTTP 200 OK.
- Page navigation correctly loads the requested page slice without errors.
- Ordering corresponds strictly to the selected sort parameter.
- The total elements and total pages counters match database active record counts.

**Pre-conditions:**
- The database contains at least 15 active (non-deleted) offer proposals across various dates.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-014 - HR Offer Keyword Search and Status Filter

**Test Case Description:**
Verify that the HR Offer list allows filtering by lifecycle status and keyword search across candidate name and offered position title.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Navigate to Offer Management (/offers).
3. Select status "Pending Director" (Pending_Director) from the status dropdown.
4. Verify that all displayed records have status Pending_Director.
5. Enter a candidate's full or partial name (e.g., "Nguyen") in the search input and press Enter.
6. Verify that only offers belonging to candidates matching "Nguyen" with status Pending_Director are displayed.
7. Clear search and select status "All Statuses" (ALL).
8. Verify that offers across all statuses are displayed.

**Expected Results:**
- HTTP 200 OK.
- Query parameters status and search are reflected in the URL and form inputs.
- Filtered table rows strictly match both the keyword and status criteria.
- Stat counters at the top of the page continue to reflect accurate global counts.

**Pre-conditions:**
- Existing offers exist in multiple statuses (Draft, Pending_Director, Director_Approved, Sent_Candidate).

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-015 - HR Offer Detail View

**Test Case Description:**
Verify that an HR user can view the comprehensive detail view of an Offer Proposal (Screen 32), including candidate profile, interview results, salary figures, and Director approval history.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Select an offer proposal that has an approval history (e.g., Offer ID 105).
3. Click the view detail icon or navigate to /offers/105.
4. Inspect the rendered page sections:
   - Candidate information (Name, Email, Phone, Address, Applied Position, CV link).
   - Interview assessment details (Final decision, summary comments, recommended salary, Hiring Manager name).
   - Offer compensation details (Offered position, official salary, probation salary, expected start date, response deadline).
   - Director approval timeline log (Director name, decision, comments, approval timestamp).

**Expected Results:**
- HTTP 200 OK.
- Template offers/detail renders completely with all candidate, interview, and offer fields matching the database.
- Approval history table accurately displays all rows from OfferApproval for this offer.
- Action buttons correspond to the current state (e.g., the "Issue Offer Letter" button is visible only if status is Director_Approved).

**Pre-conditions:**
- Offer ID 105 exists with linked application, candidate, interview result, and at least one OfferApproval record.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

## Function D: Director Review & Decision

### TC-INT-016 - Director Pending Tab and Decision History

**Test Case Description:**
Verify that a Director logging into /offers is presented with the dedicated Director view (offers/director-list), containing the "Pending Approval" tab and "Decision History" tab, and that Draft proposals are excluded from view.

**Test Case Procedure:**
1. Log in with an active Director account (e.g., username director_user).
2. Navigate to Offer Management (/offers).
3. Verify that the rendered page is offers/director-list.
4. Verify that the default active tab is "Pending Approval" (/offers?status=Pending_Director).
5. Confirm that all records in this tab have status Pending_Director and that no Draft proposals are displayed.
6. Click the "Decision History" tab (/offers?status=HISTORY).
7. Confirm that the table displays previously decided offers (Director_Approved, Director_Rejected, Sent_Candidate, Accepted, Declined) and excludes Pending_Director and Draft.
8. Use the status filter dropdown to select "Director Approved" (Director_Approved).

**Expected Results:**
- HTTP 200 OK.
- Stat cards reflect correct aggregate counts: Pending, Approved, Rejected.
- Under status=Pending_Director, only pending proposals appear.
- Under status=HISTORY, non-pending, non-draft historical proposals appear.
- Draft proposals are strictly excluded from all Director views.

**Pre-conditions:**
- Director user account is active.
- Offers exist in Draft, Pending_Director, Director_Approved, and Director_Rejected statuses.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-017 - Director Approves Offer Proposal

**Test Case Description:**
Verify that a Director can review and approve a pending Offer Proposal, transitioning its status to Director_Approved, creating an OfferApproval audit record, and notifying HR.

**Test Case Procedure:**
1. Log in with an active Director account.
2. Navigate to /offers and locate an offer in Pending_Director status (e.g., Offer ID 106).
3. Click the "Approve" button to open the approval modal.
4. Enter review comments: Approved. Compensation aligns with department budget.
5. Confirm the approval (POST /offers/106/approve).
6. Verify the UI response and redirection.
7. Query the database to verify state transitions and audit records.

**Expected Results:**
- HTTP 200 OK (JSON) or 302 Redirect to /offers.
- A success message confirms that the offer has been approved.
- On the Director list, Offer ID 106 disappears from the "Pending Approval" tab and appears in the "Decision History" tab.
- In the database:
  - OfferProposal.OfferStatus transitions to 'Director_Approved'.
  - A new row is inserted into OfferApproval:
    - OfferProposalId = 106
    - DirectorId = User ID of the logged-in Director
    - Status = 'Approved'
    - DirectorComments = 'Approved. Compensation aligns with department budget.'
    - ApprovedAt is set to the current timestamp.
- An in-app and email notification is sent to the proposing HR user.

**Pre-conditions:**
- Offer ID 106 exists in Pending_Director status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-018 - Director Rejects Offer Proposal with Comments

**Test Case Description:**
Verify that a Director can reject a pending Offer Proposal with mandatory feedback comments, transitioning its status to Director_Rejected, recording the audit entry, and notifying HR.

**Test Case Procedure:**
1. Log in with an active Director account.
2. Locate an offer in Pending_Director status (e.g., Offer ID 107).
3. Click the "Reject" button to open the rejection modal.
4. Enter rejection feedback comments: Proposed salary exceeds the approved requisition band. Please renegotiate.
5. Confirm the rejection (POST /offers/107/reject).
6. Verify the UI response and redirection.
7. Query the database to verify state changes.

**Expected Results:**
- HTTP 200 OK or 302 Redirect to /offers.
- A message confirms that the proposal has been rejected.
- In the database:
  - OfferProposal.OfferStatus transitions to 'Director_Rejected'.
  - A new row is inserted into OfferApproval:
    - OfferProposalId = 107
    - DirectorId = User ID of the logged-in Director
    - Status = 'Rejected'
    - DirectorComments = 'Proposed salary exceeds the approved requisition band. Please renegotiate.'
    - ApprovedAt is set to the current timestamp.
- HR receives an in-app and email notification detailing the rejection feedback.
- On the HR detail view (/offers/107), the Director's rejection note is clearly visible with an option to edit and resubmit.

**Pre-conditions:**
- Offer ID 107 exists in Pending_Director status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-019 - Director Decision on Non-Pending Offer

**Test Case Description:**
Verify that a Director cannot approve or reject an offer that is not in Pending_Director status (e.g., already Director_Approved, Draft, or Sent_Candidate).

**Test Case Procedure:**
1. Log in with an active Director account.
2. Identify an offer proposal already in Director_Approved status (e.g., Offer ID 108).
3. Attempt to submit an approval request by sending an HTTP POST to /offers/108/approve.
4. Observe the error response and database records.

**Expected Results:**
- The request is rejected with HTTP 400 Bad Request or redirected with an error banner.
- An error message states that only offers in pending status can be approved (OFFER_STATUS_INVALID).
- In the database, Offer ID 108 remains in Director_Approved status; no duplicate OfferApproval record is created.

**Pre-conditions:**
- Offer ID 108 is in status Director_Approved.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

## Function E: HR Issue Offer to Candidate

### TC-INT-020 - Issue Approved Offer to Candidate

**Test Case Description:**
Verify that an HR user can issue an approved offer to the candidate, transitioning the offer to Sent_Candidate, updating the application to Offered, calculating the response deadline, and sending the official offer letter email.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Locate an offer in Director_Approved status (e.g., Offer ID 109) where responsePeriodDays = 5 and expectedStartDate is 30 days away.
3. Click the "Issue Offer Letter" button on the list or detail screen (POST /offers/109/send).
4. Confirm the issuance prompt.
5. Verify UI confirmation and query database tables OfferProposal and Application.

**Expected Results:**
- HTTP 200 OK or 302 Redirect to /offers/109.
- A success message confirms that the offer letter has been officially issued to the candidate.
- In the database:
  - OfferProposal.OfferStatus transitions to 'Sent_Candidate'.
  - OfferProposal.ResponseDeadline is calculated and set to approximately NOW() + 5 days.
  - Linked Application.ApplicationStatus transitions from 'Passed' to 'Offered'.
- An email notification containing the offer letter details is triggered to the candidate's registered email.

**Pre-conditions:**
- Offer ID 109 is in Director_Approved status with valid future start date.
- Candidate has an active user account and email.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-021 - Issue Offer Forbidden on Unapproved State

**Test Case Description:**
Verify that an HR user cannot issue an offer letter to a candidate if the offer is in Draft or Pending_Director status.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Identify an offer proposal in Draft or Pending_Director status (e.g., Offer ID 110).
3. Verify that the "Issue Offer Letter" button is not visible in the UI.
4. Attempt to bypass the UI by sending an HTTP POST directly to /offers/110/send.
5. Observe the error response and database records.

**Expected Results:**
- The request is rejected with HTTP 400 Bad Request or redirected with an error banner.
- An error message states that an offer can only be issued after it has been approved by the Director (OFFER_NOT_APPROVED).
- In the database, OfferProposal.OfferStatus remains 'Draft' or 'Pending_Director'; ResponseDeadline remains NULL.
- Linked Application.ApplicationStatus remains unchanged.

**Pre-conditions:**
- Offer ID 110 is in Draft or Pending_Director status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-022 - Response Deadline Conflicts with Expected Start Date

**Test Case Description:**
Verify that the system blocks issuing an offer if the calculated response deadline falls on or after the expected start date (deadline.toLocalDate() >= expectedStartDate).

**Test Case Procedure:**
1. Log in with an active HR account.
2. Identify an offer proposal in Director_Approved status (e.g., Offer ID 111) where expectedStartDate is set to 3 days in the future, but responsePeriodDays is set to 5 days.
3. Attempt to issue the offer by clicking the "Issue Offer Letter" button (POST /offers/111/send).
4. Observe the system response.

**Expected Results:**
- The request is rejected with HTTP 400 Bad Request or redirected with an error banner.
- A validation error message is displayed stating that the response deadline must be before the expected start date.
- In the database, OfferProposal.OfferStatus remains 'Director_Approved'; ResponseDeadline remains NULL.
- The offer is not issued to the candidate.

**Pre-conditions:**
- Offer ID 111 is in Director_Approved status with responsePeriodDays exceeding the days remaining until expectedStartDate.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

## Function F: Candidate Portal List & Detail

### TC-INT-023 - Candidate Views Active Offer List

**Test Case Description:**
Verify that an authenticated candidate can view their issued offer letters on the Candidate Portal (/portal/offers), and that internal draft or pending proposals are completely hidden.

**Test Case Procedure:**
1. Log in with an active Candidate account (e.g., username candidate_john).
2. Navigate to the Candidate Portal offers page (/portal/offers).
3. Verify that the table renders the candidate's issued offer letters (status Sent_Candidate, Accepted, Declined).
4. Verify that internal proposals (status Draft, Pending_Director, Director_Approved) for this or any other candidate do not appear.
5. Check displayed columns: Position Title, Official Salary, Start Date, Response Deadline, and Status badge.

**Expected Results:**
- HTTP 200 OK.
- Template offers/candidate-list renders properly.
- Only offers with statuses Sent_Candidate, Accepted, or Declined belonging to candidate_john are visible.
- No internal company drafts or pending approvals are exposed.

**Pre-conditions:**
- Candidate candidate_john has an offer in Sent_Candidate status and another in Draft status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-024 - Candidate Views Offer Letter Detail

**Test Case Description:**
Verify that an authenticated candidate can open and view the complete Offer Letter detail page (/portal/offers/{id}).

**Test Case Procedure:**
1. Log in as Candidate candidate_john.
2. On /portal/offers, click on an offer in Sent_Candidate status (Offer ID 112) to open /portal/offers/112.
3. Verify the rendered letter components:
   - Job position title and department.
   - Official monthly salary (formatted in VND).
   - Probation salary and probation percentage.
   - Expected start date and work location.
   - Benefits package details.
   - Response deadline countdown / formatted timestamp.
   - HR contact information (Name and Email).
   - Response buttons: "Accept Offer" and "Decline".

**Expected Results:**
- HTTP 200 OK.
- Template offers/candidate-detail renders with all contract terms matching the database record.
- Because the offer is active and not expired, both Accept and Decline action buttons are enabled and visible.

**Pre-conditions:**
- Offer ID 112 is in Sent_Candidate status and belongs to candidate_john.
- The current time is before ResponseDeadline.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-025 - Candidate Cannot Access Another Candidate's Offer

**Test Case Description:**
Verify data ownership isolation: a candidate attempting to access an offer proposal belonging to a different candidate receives an HTTP 404 Not Found error.

**Test Case Procedure:**
1. Log in as Candidate A (candidate_a).
2. Ensure Offer ID 113 belongs to Candidate B (candidate_b).
3. While logged in as Candidate A, attempt to navigate directly to /portal/offers/113.
4. Observe the response code and rendered page.

**Expected Results:**
- HTTP 404 Not Found (or access denied).
- The system displays a resource not found message indicating that the requested offer letter does not exist.
- No confidential offer data of Candidate B is exposed to Candidate A.

**Pre-conditions:**
- Offer ID 113 belongs to Candidate B.
- Candidate A is logged in.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

## Function G: Candidate Accept / Decline Lifecycle

### TC-INT-026 - Candidate Accepts Offer

**Test Case Description:**
Verify the happy path for candidate acceptance: candidate accepts an active offer, transitioning OfferStatus to Accepted, updating ApplicationStatus to Hired, and displaying the congratulations modal dialog.

**Test Case Procedure:**
1. Log in as candidate owning an active offer (Offer ID 114, status Sent_Candidate).
2. Open the offer detail screen (/portal/offers/114).
3. Click the "Accept Offer" button.
4. In the confirmation dialog, click the "Confirm Acceptance" button.
5. Verify the UI response, flash attributes, and congratulations modal.
6. Query database tables OfferProposal and Application.

**Expected Results:**
- HTTP 302 Redirect to /portal/offers/114 (or /portal/offers), followed by HTTP 200.
- A congratulations popup dialog / success banner appears confirming that the candidate has accepted the offer.
- Action buttons "Accept" and "Decline" are replaced with status badge "Accepted".
- In the database:
  - OfferProposal.OfferStatus transitions from 'Sent_Candidate' to 'Accepted'.
  - OfferProposal.UpdatedAt is updated to current time.
  - Linked Application.ApplicationStatus transitions from 'Offered' to 'Hired'.
- System notification is dispatched to HR informing them that the candidate has accepted.

**Pre-conditions:**
- Offer ID 114 is in Sent_Candidate status and has not expired.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-027 - Candidate Declines Offer with Reason

**Test Case Description:**
Verify that a candidate can decline an active offer while providing a detailed reason, transitioning OfferStatus to Declined, updating ApplicationStatus to Rejected, storing the decline reason, and notifying HR.

**Test Case Procedure:**
1. Log in as candidate owning active Offer ID 115 (status Sent_Candidate).
2. Open /portal/offers/115 and click the "Decline" button.
3. On the decline survey page (/portal/offers/115/decline), select a quick chip (e.g., "Compensation below expectation") and enter additional remarks: Received another counter-offer with higher base salary.
4. Click the "Confirm Decline" button (POST /portal/offers/115/decline).
5. Verify UI confirmation banner and query the database.

**Expected Results:**
- HTTP 302 Redirect followed by HTTP 200.
- A confirmation banner is displayed confirming that the candidate has declined the offer.
- In the database:
  - OfferProposal.OfferStatus transitions from 'Sent_Candidate' to 'Declined'.
  - OfferProposal.DeclineReason = 'Received another counter-offer with higher base salary.'
  - Linked Application.ApplicationStatus transitions from 'Offered' to 'Rejected'.
- An in-app notification is sent to HR with the decline feedback.
- On the HR detail view (/offers/115), the decline callout box displays the candidate's exact reason.

**Pre-conditions:**
- Offer ID 115 is in Sent_Candidate status and unexpired.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-028 - Candidate Declines Offer without Reason

**Test Case Description:**
Verify that a candidate can decline an active offer without providing any optional decline reason, correctly transitioning statuses and persisting DeclineReason as NULL.

**Test Case Procedure:**
1. Log in as candidate owning active Offer ID 116 (status Sent_Candidate).
2. Open /portal/offers/116 and click the "Decline" button.
3. On the decline survey page (/portal/offers/116/decline), leave the reason textarea completely blank.
4. Click the "Confirm Decline" button (POST /portal/offers/116/decline with empty declineReason).
5. Verify the UI response and query the database.

**Expected Results:**
- HTTP 302 Redirect followed by HTTP 200.
- The decline operation succeeds without requiring text input.
- A confirmation banner informs the candidate that the offer has been declined.
- In the database:
  - OfferProposal.OfferStatus transitions from 'Sent_Candidate' to 'Declined'.
  - OfferProposal.DeclineReason is stored as NULL.
  - Linked Application.ApplicationStatus transitions to 'Rejected'.
- HR notification is triggered indicating that the candidate declined without providing detailed feedback.

**Pre-conditions:**
- Offer ID 116 is in Sent_Candidate status and unexpired.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-029 - Candidate Decline Page Navigation Guard

**Test Case Description:**
Verify that navigating directly to /portal/offers/{id}/decline for an offer that has already been answered (or expired) is blocked and redirected back to the detail view with an error banner.

**Test Case Procedure:**
1. Log in as a candidate.
2. Select an offer that is already in Accepted or Declined status (e.g., Offer ID 117).
3. Attempt to enter /portal/offers/117/decline directly in the browser address bar.
4. Observe the controller redirection and flash message.

**Expected Results:**
- Controller method declinePage checks offer.canRespond() == false.
- User is immediately redirected to /portal/offers/117.
- An error banner is displayed stating that the offer has already been answered and cannot be modified.
- The decline survey form is not rendered.

**Pre-conditions:**
- Offer ID 117 has status Accepted or Declined.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-030 - Candidate Response after Deadline Expiration

**Test Case Description:**
Verify that a candidate cannot accept or decline an offer once the response deadline has elapsed (LocalDateTime.now() > responseDeadline).

**Test Case Procedure:**
1. Ensure Offer ID 118 has status Sent_Candidate, but its ResponseDeadline is set to 1 hour in the past.
2. Log in as the candidate owning Offer ID 118.
3. Open /portal/offers/118.
4. Verify that the UI displays a warning banner stating that the response deadline has expired, and the Accept/Decline action buttons are disabled.
5. Attempt to bypass the UI by submitting an HTTP POST to /portal/offers/118/accept.
6. Observe the response and database records.

**Expected Results:**
- The request is rejected.
- An error message is displayed stating that the offer has expired past its response deadline and recruitment support should be contacted.
- In the database:
  - OfferProposal.OfferStatus remains 'Sent_Candidate'.
  - Linked Application.ApplicationStatus remains 'Offered'.

**Pre-conditions:**
- Offer ID 118 is in Sent_Candidate status with ResponseDeadline < NOW().

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-031 - Candidate Response after Expected Start Date

**Test Case Description:**
Verify that a candidate cannot respond to an offer if the expected start date has passed (expectedStartDate < LocalDate.now()).

**Test Case Procedure:**
1. Ensure Offer ID 119 has status Sent_Candidate, but its ExpectedStartDate is set to yesterday's date.
2. Log in as the candidate owning Offer ID 119.
3. Submit an HTTP POST request to /portal/offers/119/accept.
4. Observe the response.

**Expected Results:**
- The request is rejected.
- An error message is displayed stating that the offer has passed its expected start date and can no longer be accepted.
- In the database, the offer status remains unchanged ('Sent_Candidate').

**Pre-conditions:**
- Offer ID 119 has status Sent_Candidate and ExpectedStartDate < TODAY.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-032 - Duplicate Candidate Response

**Test Case Description:**
Verify that submitting a second response on an already answered offer proposal is rejected.

**Test Case Procedure:**
1. Log in as candidate owning Offer ID 120.
2. Accept the offer via POST /portal/offers/120/accept (status becomes Accepted).
3. Immediately send a second POST request to /portal/offers/120/decline.
4. Observe the response and database state.

**Expected Results:**
- The second request is rejected.
- An error message is displayed stating that the offer has already been answered and cannot be modified.
- In the database:
  - OfferProposal.OfferStatus remains 'Accepted'.
  - Linked Application.ApplicationStatus remains 'Hired'.
  - DeclineReason remains NULL.

**Pre-conditions:**
- Offer ID 120 was previously transitioned to Accepted.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-033 - Concurrent Accept and Decline Protection

**Test Case Description:**
Verify system concurrency protection when a candidate triggers near-simultaneous Accept and Decline requests on the same active offer using two browser sessions or tabs.

**Test Case Procedure:**
1. Open two separate browser sessions (or private browsing windows) and log in as the same candidate.
2. In Session A, navigate to Offer ID 121 (/portal/offers/121) and prepare to click "Accept Offer".
3. In Session B, navigate to the decline survey page (/portal/offers/121/decline) and prepare to click "Confirm Decline".
4. Trigger both actions as closely together as possible.
5. Observe the response in both browser windows.
6. Refresh both pages.
7. Query the database:
   SELECT OfferId, OfferStatus, DeclineReason FROM OfferProposal WHERE OfferId = 121;
   SELECT ApplicationId, ApplicationStatus FROM Application WHERE ApplicationId = (SELECT ApplicationId FROM OfferProposal WHERE OfferId = 121);

**Expected Results:**
- Exactly one of the two requests succeeds and commits:
  - If Accept wins: Session A displays the congratulations modal; Session B displays an error message stating that the offer has just been updated or already answered.
  - If Decline wins: Session B displays the decline confirmation banner; Session A displays the update error message.
- The losing session fails gracefully without a system crash.
- In the database:
  - The offer has exactly one final terminal state: either Accepted (with ApplicationStatus = 'Hired') OR Declined (with ApplicationStatus = 'Rejected').
  - No corrupted, partial, or split-brain data state exists.

**Pre-conditions:**
- Offer ID 121 is in Sent_Candidate status and unexpired.
- Candidate account is authenticated in two independent sessions.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-034 - Candidate Decline Reason Exceeds 1000 Characters

**Test Case Description:**
Verify that the system validates and restricts candidate decline feedback to the database column capacity of 1000 characters (DeclineReason NVARCHAR(1000)).

**Test Case Procedure:**
1. Log in as a candidate owning active Offer ID 122.
2. Open the decline survey page (/portal/offers/122/decline).
3. Prepare a text string consisting of 1,050 characters.
4. Paste the 1,050-character string into the decline reason textarea.
5. Submit the decline form (POST /portal/offers/122/decline).
6. Observe application handling, HTTP status, and database state.

**Expected Results:**
- The system should gracefully validate input length and reject submissions exceeding 1000 characters with an informative error message stating that the decline reason cannot exceed 1000 characters.
- The offer should remain in Sent_Candidate status until valid input is supplied.
- No unhandled SQL Server exception or HTTP 500 internal server error should occur.

**Pre-conditions:**
- Offer ID 122 is in Sent_Candidate status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**
Known Risk / Potential Defect: CandidateOfferController lacks @Size(max = 1000) validation, CandidateOfferServiceImpl does not truncate input, and the textarea in candidate-decline.html lacks maxlength="1000". Submitting >1000 characters currently causes SQL Server string truncation errors (DataTruncationException), yielding an unhandled HTTP 500 error.

---

## Function H: Role Authorization & Security

### TC-INT-035 - HR Forbidden from Approving Offer

**Test Case Description:**
Verify cross-role restriction: an HR user must be forbidden from approving an Offer Proposal. Only the Director role is authorized to approve offers.

**Test Case Procedure:**
1. Log in with an active HR account (e.g., hr_user).
2. Identify an offer proposal in Pending_Director status (e.g., Offer ID 123).
3. Note that the approval button is hidden from the HR user interface.
4. Send an explicit HTTP POST request to /offers/123/approve using HR session cookies.
5. Observe the HTTP response code, returned body, and database records.

**Expected Results:**
- HTTP 403 Forbidden is returned.
- The request is denied; HR is not permitted to perform Director approvals.
- In the database:
  - OfferProposal.OfferStatus remains 'Pending_Director'.
  - No row is inserted into OfferApproval.
  - No approval notification is dispatched.

**Pre-conditions:**
- User hr_user possesses only ROLE_HR.
- Offer ID 123 is in Pending_Director status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**
Known Risk / Potential Defect: Current SecurityConfig allows ROLE_HR under /offers/**, and OfferController.approveOfferJson / approveOfferForm lacks method-level security (@PreAuthorize("hasRole('DIRECTOR')")). Currently, an HR user sending a direct POST to /offers/{id}/approve succeeds in approving the proposal and records the HR user as the approving Director in OfferApproval.

---

### TC-INT-036 - HR Forbidden from Rejecting Offer

**Test Case Description:**
Verify cross-role restriction: an HR user must be forbidden from rejecting an Offer Proposal. Only the Director role is authorized to reject offers.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Identify an offer in Pending_Director status (Offer ID 124).
3. Send an explicit HTTP POST request to /offers/124/reject with comments using HR session credentials.
4. Observe the response and verify database records.

**Expected Results:**
- HTTP 403 Forbidden is returned.
- HR is not permitted to reject proposals.
- In the database:
  - OfferProposal.OfferStatus remains 'Pending_Director'.
  - No rejection record is added to OfferApproval.

**Pre-conditions:**
- Logged-in user has only ROLE_HR.
- Offer ID 124 is in Pending_Director status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**
Known Risk / Potential Defect: OfferController.rejectOfferJson lacks role enforcement. Currently, an HR user issuing a POST to /offers/{id}/reject succeeds and transitions the offer to Director_Rejected.

---

### TC-INT-037 - Director Forbidden from Creating Offer

**Test Case Description:**
Verify cross-role restriction: a Director user must be forbidden from creating new Offer Proposals. Offer creation is strictly an HR function.

**Test Case Procedure:**
1. Log in with an active Director account.
2. Confirm that the "Create Offer" button is not visible on offers/director-list.
3. Attempt to access GET /offers/create directly in the browser.
4. Attempt to send an HTTP POST request to /offers/create with new offer data using Director credentials.
5. Observe the response.

**Expected Results:**
- HTTP 403 Forbidden is returned for both GET and POST requests.
- Director is prevented from accessing the creation form and submitting proposals.
- In the database, no new OfferProposal is created.

**Pre-conditions:**
- Logged-in user has only ROLE_DIRECTOR.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**
Known Risk / Potential Defect: SecurityConfig allows ROLE_DIRECTOR for all /offers/** endpoints. Currently, a Director can navigate directly to /offers/create and successfully create proposals with proposedBy = Director.

---

### TC-INT-038 - Director Forbidden from Editing Draft Offer

**Test Case Description:**
Verify cross-role restriction: a Director user must be forbidden from editing Draft offer proposals. Draft maintenance belongs exclusively to HR.

**Test Case Procedure:**
1. Log in with an active Director account.
2. Identify an existing offer in Draft status (Offer ID 125).
3. Send an HTTP POST or PUT request to /offers/125/edit with modified compensation values using Director credentials.
4. Observe the response and inspect database records.

**Expected Results:**
- HTTP 403 Forbidden is returned.
- Director cannot update draft proposals.
- In the database, Offer ID 125 remains unmodified.

**Pre-conditions:**
- Logged-in user has ROLE_DIRECTOR.
- Offer ID 125 is in Draft status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**
Known Risk / Potential Defect: Currently, OfferController and OfferServiceImpl do not verify that the actor is HR during updates; a Director submitting an edit to a Draft offer succeeds.

---

### TC-INT-039 - Director Forbidden from Deleting Draft Offer

**Test Case Description:**
Verify cross-role restriction: a Director user must be forbidden from deleting Draft offer proposals.

**Test Case Procedure:**
1. Log in with an active Director account.
2. Identify an offer in Draft status (Offer ID 126).
3. Send an HTTP POST request to /offers/126/delete (or DELETE /offers/126) using Director credentials.
4. Observe the response and check database records.

**Expected Results:**
- HTTP 403 Forbidden is returned.
- Director cannot delete proposals.
- In the database, Offer ID 126 remains active (IsDeleted = 0).

**Pre-conditions:**
- Logged-in user has ROLE_DIRECTOR.
- Offer ID 126 is in Draft status (IsDeleted = 0).

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**
Known Risk / Potential Defect: Endpoint allows any user authorized under /offers/** to soft-delete drafts, enabling Directors to delete draft offers if invoked directly.

---

### TC-INT-040 - Director Forbidden from Issuing Offer

**Test Case Description:**
Verify cross-role restriction: a Director user must be forbidden from issuing an offer letter to a candidate. Issuance is an operational HR responsibility.

**Test Case Procedure:**
1. Log in with an active Director account.
2. Identify an offer in Director_Approved status (Offer ID 127).
3. Send an HTTP POST request to /offers/127/send using Director credentials.
4. Observe the response and inspect database records.

**Expected Results:**
- HTTP 403 Forbidden is returned.
- Director is not permitted to issue offers.
- In the database:
  - OfferProposal.OfferStatus remains 'Director_Approved'.
  - ResponseDeadline remains NULL.
  - Linked Application.ApplicationStatus remains unchanged.
  - No offer letter email is dispatched.

**Pre-conditions:**
- Logged-in user has ROLE_DIRECTOR.
- Offer ID 127 is in Director_Approved status.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**
Known Risk / Potential Defect: OfferController.sendOfferToCandidate does not check for ROLE_HR. A Director sending a POST to /{id}/send currently succeeds and issues the offer.

---

### TC-INT-041 - Candidate Isolated from Internal Offer Routes

**Test Case Description:**
Verify candidate boundary isolation: a candidate user must be strictly forbidden from accessing any internal company offer management endpoints under /offers/**.

**Test Case Procedure:**
1. Log in with an active Candidate account (ROLE_CANDIDATE).
2. Attempt to navigate to the internal HR offer list: GET /offers.
3. Attempt to access GET /offers/create.
4. Attempt to send a POST request to /offers/create.
5. Observe the HTTP response for each attempt.

**Expected Results:**
- All requests are rejected with HTTP 403 Forbidden by Spring Security.
- The candidate cannot view internal lists, creator tools, or company salary proposals.
- No internal data is leaked.

**Pre-conditions:**
- Logged-in user possesses only ROLE_CANDIDATE.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-042 - Unauthenticated Guest Redirected to Login

**Test Case Description:**
Verify unauthenticated access control: an unauthenticated guest accessing any protected offer URL is redirected to the login page with the target URL preserved.

**Test Case Procedure:**
1. Open a clean browser session with no active session cookies.
2. Attempt to navigate to an internal offer URL: GET /offers.
3. Attempt to navigate to a candidate portal URL: GET /portal/offers.
4. Verify the HTTP response codes and redirection URLs.

**Expected Results:**
- Both requests return HTTP 302 Redirect to /login.
- The user is presented with the RMS login interface.
- Protected content is inaccessible without successful authentication.

**Pre-conditions:**
- User is unauthenticated (guest).

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-043 - CSRF Token Enforcement

**Test Case Description:**
Verify CSRF security protection: state-modifying requests (POST, PUT, DELETE) to Offer endpoints without a valid CSRF token are rejected with HTTP 403 Forbidden.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Craft an HTTP POST request to /offers/create omitting the _csrf token header or parameter.
3. Craft an HTTP POST request to /portal/offers/{id}/accept without a valid CSRF token using Candidate session credentials.
4. Send both requests and inspect the HTTP status.

**Expected Results:**
- Spring Security CSRF filter intercepts both requests.
- HTTP 403 Forbidden is returned.
- No state change or database persistence occurs.

**Pre-conditions:**
- Active authenticated session.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

## Function I: Offer Export to Excel

### TC-INT-044 - Export Filtered Offers

**Test Case Description:**
Verify that an HR user can export offer proposals matching active search and status filters to an Excel file (OfferExportScope.FILTERED).

**Test Case Procedure:**
1. Log in with an active HR account and navigate to /offers.
2. Filter the list by status "Pending Director" (Pending_Director).
3. Click the "Export Excel" button to open the export modal.
4. Select scope radio "Filtered List" (FILTERED).
5. Select columns: Offer ID, Candidate Name, Email, Offered Position, Proposed Salary, Probation Salary, Status, Expected Start Date.
6. Click the "Export Excel" button (POST /offers/export).
7. Inspect the downloaded file.

**Expected Results:**
- HTTP 200 OK.
- Content-Type is application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.
- Content-Disposition header specifies filename format offers_filtered_<YYYY-MM-DD>.xlsx.
- The downloaded workbook opens cleanly in Excel.
- Contains two sheets: "Offers" and "Summary".
- "Offers" sheet contains only records matching status = 'Pending_Director'.
- Salary figures are formatted as numeric values; dates are properly formatted.

**Pre-conditions:**
- Offers exist matching status Pending_Director.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-045 - Export Selected Offers

**Test Case Description:**
Verify that an HR user can export specific offers selected via table checkboxes (OfferExportScope.SELECTED).

**Test Case Procedure:**
1. Log in with an active HR account and open /offers.
2. Select checkboxes for two specific offers (e.g., Offer ID 101 and Offer ID 103).
3. Click the "Export Excel" button to open the export modal.
4. Choose scope "Selected Offers" (SELECTED).
5. Keep default columns selected and click the "Export Excel" button.
6. Inspect the downloaded workbook.

**Expected Results:**
- HTTP 200 OK.
- Filename matches offers_selected_2_<YYYY-MM-DD>.xlsx.
- "Offers" sheet contains exactly 2 data rows corresponding to Offer IDs 101 and 103.
- "Summary" sheet displays export metadata (Exported by HR, Scope: SELECTED, Total Records: 2).

**Pre-conditions:**
- At least 2 active offers exist.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-046 - Export All Active Offers

**Test Case Description:**
Verify that an HR user can export all non-deleted offers in the system (OfferExportScope.ALL).

**Test Case Procedure:**
1. Log in with an active HR account and open /offers.
2. Open the Export modal.
3. Select scope "All Offers" (ALL).
4. Click the "Select All" button to select all available whitelist columns (Default and Extended groups).
5. Click the "Export Excel" button.
6. Query database to count non-deleted offers:
   SELECT COUNT(*) FROM OfferProposal WHERE IsDeleted = 0;
7. Compare count with the exported Excel sheet.

**Expected Results:**
- HTTP 200 OK.
- Filename matches offers_<YYYY-MM-DD>.xlsx.
- Number of exported rows in the "Offers" sheet exactly equals the active database count.
- Soft-deleted offers (IsDeleted = 1) are strictly excluded.
- Extended columns (Benefits Package, Director Decision, Approval Time, Decline Reason) are included.

**Pre-conditions:**
- Database contains multiple active offers and at least one soft-deleted offer.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-047 - Empty Selected Export Validation

**Test Case Description:**
Verify that attempting to export with OfferExportScope.SELECTED without selecting any offers triggers a validation error.

**Test Case Procedure:**
1. Log in with an active HR account.
2. Send an HTTP POST request to /offers/export with:
   - scope: "SELECTED"
   - offerIds: [] (empty list)
   - columns: ["offerId", "candidateName"]
3. Observe the response status and error body.

**Expected Results:**
- HTTP 400 Bad Request is returned.
- Response contains an error message stating that at least one offer must be selected for export (EMPTY_SELECTED_OFFERS).
- No Excel file is generated or downloaded.

**Pre-conditions:**
- HR user logged in.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**

---

### TC-INT-048 - Director Forbidden from Exporting Offer Data

**Test Case Description:**
Verify authorization rule: a Director user must be forbidden from exporting Offer data. In accordance with RMS access governance, export is strictly restricted to HR.

**Test Case Procedure:**
1. Log in with an active Director account.
2. Confirm that offers/director-list provides no export button or modal.
3. Send a direct HTTP POST request to /offers/export with valid export payload using Director session credentials.
4. Observe the response code.

**Expected Results:**
- HTTP 403 Forbidden is returned.
- No Excel file is generated or downloaded.
- Offer data cannot be exfiltrated via export by Director role.

**Pre-conditions:**
- User possesses ROLE_DIRECTOR.

**Round 1:** Pending

**Round 2:** Pending

**Round 3:** Pending

**Defect IDs:**

**Note:**
Known Risk / Authorization Discrepancy: OfferController.exportOffers explicitly whitelists ROLE_DIRECTOR (line 470), allowing Directors to export data if invoked via direct POST, even though director-list.html hides the export button. Backend security should be updated to restrict export to ROLE_HR and SYSTEM_ADMIN only.

---

## Coverage Summary

| Function Group | Number of TCs |
|---|---:|
| A. HR Offer Creation | 7 |
| B. HR Draft Management | 5 |
| C. HR Offer List, Search & Detail | 3 |
| D. Director Review & Decision | 4 |
| E. HR Issue Offer to Candidate | 3 |
| F. Candidate Portal List & Detail | 3 |
| G. Candidate Accept / Decline Lifecycle | 9 |
| H. Role Authorization & Security | 9 |
| I. Offer Export to Excel | 5 |
| **TOTAL** | **48** |

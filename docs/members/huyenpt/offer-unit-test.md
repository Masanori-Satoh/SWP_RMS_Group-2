# Offer Module - Automated Test Report

- **Module:** Offer Proposal Management (`Offer Proposal`)
- **Project:** Recruitment Management System — SWP_RMS_Group-2 (Mộc RMS)
- **Assignee / Contributor:** Pham Thi Huyen (`HuyenPT`)
- **Reference Docs:** [ARCHITECTURE_GUIDE.md](../../architecture/ARCHITECTURE_GUIDE.md) · [model.md](../../database/model.md) · [db.sql](../../../database/schema/db.sql)
- **Report Date:** October 19, 2026
- **Test Execution Status:** **100% PASS (196 / 196 test runs passed, 0 failures, 0 errors, 0 skipped)**

---

## 1. Test Objectives

- Verify the correctness, robustness, and reliability of all business rules in the Offer Proposal Management module across all three system roles: **HR Specialist**, **Director**, and **Candidate**.
- Validate strict salary calculation rules (Labor Code 85% rule), start date boundaries, response period boundaries, response deadline comparisons, soft deletion, and state lifecycle transitions.
- Ensure security authorization, role restrictions, and concurrency protections (optimistic locking / conditional atomic updates) function as expected.
- Verify fault tolerance and exception containment: Mail delivery failures and in-app notification outages must not corrupt or roll back primary offer state transitions.
- Guarantee zero regression risk following recent enhancements (Director live filter counts, candidate congratulation modal, and real-time lifecycle status preservation).

---

## 2. Test Scope & Layer Classification

The automated test suite spans all architectural layers conforming to `ARCHITECTURE_GUIDE.md`:

### 2.1. Service Layer Unit Tests (`src/test/java/com/group2/rms/offer/service/`)
- [OfferServiceTests.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/test/java/com/group2/rms/offer/service/OfferServiceTests.java): Covers [OfferServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java), testing offer creation, salary validation, start date validation, Director approval/rejection, issuing offers to candidates, soft-deletion, search/filter specifications, and failure containment.
- [CandidateOfferServiceTests.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/test/java/com/group2/rms/offer/service/CandidateOfferServiceTests.java): Covers [CandidateOfferServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java), testing candidate offer listing, detail views, candidate acceptance, candidate decline with reasons, response deadline expiration, and concurrency guards.
- [OfferExportServiceTests.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/test/java/com/group2/rms/offer/OfferExportServiceTests.java): Covers [OfferExportServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferExportServiceImpl.java), testing Apache POI workbook generation, scopes (`SELECTED`, `FILTERED`, `ALL`), column whitelisting, cell data types, and summary sheet creation.

### 2.2. WebMvc & Security Layer Tests (`src/test/java/com/group2/rms/offer/`)
- [OfferIntegrationTests.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/test/java/com/group2/rms/offer/OfferIntegrationTests.java): WebMvc integration testing for [OfferController.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java), testing role-based access control (RBAC), CSRF protection, JSON/AJAX modal endpoints, invalid input handling, and negative pagination sanitization.
- [CandidateOfferWebTests.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/test/java/com/group2/rms/offer/CandidateOfferWebTests.java): WebMvc testing for [CandidateOfferController.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/CandidateOfferController.java), testing candidate portal views, accept/decline POST endpoints, flash attributes, and countdown UI attributes.

### 2.3. Repository & Data JPA Tests (`src/test/java/com/group2/rms/offer/repository/`)
- [CandidateOfferQueryTests.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/test/java/com/group2/rms/offer/repository/CandidateOfferQueryTests.java): Tests [CandidateOfferRepository.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/repository/CandidateOfferRepository.java) against local transactional database queries, ensuring user isolation and atomic conditional update `answerIfSent`.

### 2.4. Regression & Controller Integration Tests
- [OfferControllerBugTests.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/test/java/com/group2/rms/offer/controller/OfferControllerBugTests.java): Full `@SpringBootTest` verifying Director offer page model attributes, tab states, and filter parameters.

---

## 3. Test Technologies & Stack

- **Platform:** Java OpenJDK 21 LTS
- **Build Tool:** Apache Maven Wrapper (`mvnw.cmd`)
- **Unit Testing Framework:** JUnit 5 (Jupiter 5.11.x) — `@Test`, `@ParameterizedTest`, `@ValueSource`, `@CsvSource`, `@DisplayName`
- **Mocking Framework:** Mockito 5.x — `@Mock`, `@InjectMocks`, `verify()`, `when()`, `doThrow()`
- **Assertions:** JUnit 5 Assertions (`assertEquals`, `assertTrue`, `assertFalse`, `assertNotNull`, `assertThrows`), AssertJ (`assertThat`, `assertThatThrownBy`)
- **Web Testing:** Spring Boot Test (`@WebMvcTest`, `@SpringBootTest`, `MockMvc`, `SecurityMockMvcRequestPostProcessors`)
- **Security Mocking:** Spring Security Test (`@WithMockUser`, `user(username).authorities(...)`, `csrf()`)
- **Excel Testing:** Apache POI 5.x (`XSSFWorkbook`, `Sheet`, `Row`, `Cell`, `CellType`)
- **Code Coverage:** JaCoCo 0.8.12

---

## 4. Business Rules Covered

1. **Salary Constraint Rules (BR-OFF-01 & DB Constraint `CK_OfferProposal_Salaries`):**
   - Official salary (`proposedSalary`) and probation salary (`probationSalary`) must be strictly positive (`> 0`).
   - Probation salary must be at least **85%** of proposed salary: `probationSalary >= proposedSalary * 0.85`. Values below 85% throw `OfferValidationException`.
   - Probation salary must not exceed proposed salary: `probationSalary <= proposedSalary`.
2. **Start Date Rules:**
   - `expectedStartDate` must be strictly in the future (`> LocalDate.now()`). Today and past dates are rejected with `OfferValidationException`.
3. **Response Period Days Rules (DTO Bean Validation & Service Rules):**
   - In DTOs (`CreateOfferRequest`, `UpdateOfferRequest`), `responsePeriodDays` is constrained between **1 and 60 days** (`@Min(1)`, `@Max(60)`).
   - In `OfferServiceImpl`, non-positive values (`<= 0`) throw `OfferValidationException`.
4. **Response Deadline Boundary Rules:**
   - When HR issues an offer (`sendOfferToCandidate`), `responseDeadline` is calculated as `LocalDateTime.now().plusDays(responsePeriodDays)`.
   - The deadline date must be strictly before `expectedStartDate`: `!deadline.toLocalDate().isBefore(expectedStartDate)` throws `OfferValidationException`. If deadline equals or exceeds `expectedStartDate`, it is blocked.
   - For candidates, `isExpired` evaluates `LocalDateTime.now().isAfter(responseDeadline)`. Responses submitted immediately after deadline are rejected with `OfferResponseException`. If `responseDeadline` is null, fallback evaluates `expectedStartDate.isBefore(LocalDate.now())`.
5. **Director Decision Validation:**
   - Director rejection comments are optional: `null`, `""`, and whitespace-only `"   "` comments are accepted and recorded in `OfferApproval`.
   - Approval and rejection are only permitted when Offer is in `Pending_Director`. Any other state (e.g., `Draft`, `Director_Approved`, `Director_Rejected`, `Sent_Candidate`, `Accepted`, `Declined`) throws `OfferValidationException("OFFER_STATUS_INVALID")`.
6. **Candidate Decline Reason Rules:**
   - Decline reasons are trimmed. Null, empty, or whitespace-only strings are normalized to `null`.
   - Up to 1000 characters (database length) are accepted and persisted.
7. **Single Active Offer Rule (GBR-07):**
   - An application cannot have a new offer created if an active offer (including `Draft`) already exists.
   - Editing is only allowed when Offer is in `Draft`.
8. **Fault Tolerance & Exception Containment:**
   - If `OfferMailSender` throws an exception (e.g., SMTP down), `sendOfferToCandidate` catches the error, logs a warning, and completes the transaction (Offer status remains `Sent_Candidate`, Application status remains `Offered`).
   - If `NotificationService` throws an exception during Director approval/rejection or Candidate acceptance/decline, the core business transaction succeeds without interruption.
9. **Atomic Concurrency Guard:**
   - Candidate response updates execute atomically via `CandidateOfferRepository.answerIfSent()`. If an offer is modified concurrently between read and update, `updateCount == 0` triggers `OfferResponseException` and preserves the `Application` state unchanged.

---

## 5. Master Test Case Catalog

| TC ID | Test Level | Method / Feature | Scenario | Input / Pre-condition | Expected Result | Test Method | Runs | Result |
|---|---|---|---|---|---|---|:---:|:---:|
| **UT-OFFER-001** | Unit | `createOfferByHr` | Probation salary < 85% | 20M proposed, 16.9M probation | Throws `OfferValidationException` ("85%") | `testProbationSalaryLessThan85Percent_throwsException` | 1 | PASS |
| **UT-OFFER-002** | Unit | `createOfferProposal` | Probation salary exactly 85% | 20M proposed, 17M probation | Offer created in `Pending_Director` | `testProbationSalaryExactly85Percent_passes` | 1 | PASS |
| **UT-OFFER-003** | Unit | `createOfferByHr` | Expected start date is today | `expectedStartDate = LocalDate.now()` | Throws `OfferValidationException` | `testCreateOfferByHr_startDateToday_throwsException` | 1 | PASS |
| **UT-OFFER-004** | Unit | `createOfferByHr` | Expected start date in past | `expectedStartDate = LocalDate.now() - 1` | Throws `OfferValidationException` | `testCreateOfferByHr_startDatePast_throwsException` | 1 | PASS |
| **UT-OFFER-005** | Unit | `createOfferByHr` | Probation days <= 0 | `probationDays` in [0, -1, -60] | Throws `OfferValidationException` | `testCreateOfferByHr_probationDaysZeroOrNegative_throwsException` | 3 | PASS |
| **UT-OFFER-006** | Unit | `createOfferByHr` | Proposed salary <= 0 | `proposedSalary` in [0, -1, -10M] | Throws `OfferValidationException` | `testCreateOfferByHr_salaryZeroOrNegative_throwsException` | 3 | PASS |
| **UT-OFFER-007** | Unit | `createOfferByHr` | Work location blank | `workLocation = "   "` | Throws `OfferValidationException` | `testCreateOfferByHr_workLocationBlank_throwsException` | 1 | PASS |
| **UT-OFFER-008** | Unit | `createOfferByHr` | Valid fields with null benefits | `benefitsPackage = null` | Offer created successfully | `testCreateOfferByHr_allValidExceptBenefits_passes` | 1 | PASS |
| **UT-OFFER-009** | Unit | `updateOfferByHr` | Update start date to today | `expectedStartDate = LocalDate.now()` | Throws `OfferValidationException` | `testUpdateOfferByHr_invalidStartDate_throwsException` | 1 | PASS |
| **UT-OFFER-010** | Unit | Bean Validation | Empty/invalid CreateOfferRequest | Blank title, 0 salary, today start date | Constraint violations on required fields | `testCreateOfferRequest_beanValidationConstraints` | 1 | PASS |
| **UT-OFFER-011** | Unit | Bean Validation | Valid CreateOfferRequest | Full valid fields, null benefits | Zero constraint violations | `testCreateOfferRequest_beanValidationSuccess` | 1 | PASS |
| **UT-OFFER-012** | Unit | `getPassedCandidatesForOffer` | Location precedence (both present) | JobPosting has loc, Cand has address | Selects JobPosting `workLocation` | `testGetPassedCandidatesForOffer_whenBothLocationsPresent_prefersJobPostingLocation` | 1 | PASS |
| **UT-OFFER-013** | Unit | `getPassedCandidatesForOffer` | Location fallback to Candidate | JobPosting loc null, Cand has address | Falls back to Candidate `address` | `testGetPassedCandidatesForOffer_whenJobLocationMissing_fallbackToCandidateAddress` | 1 | PASS |
| **UT-OFFER-014** | Unit | `getPassedCandidatesForOffer` | Location fallback to default | Both locations missing/blank | Falls back to "Trụ sở chính Mộc RMS" | `testGetPassedCandidatesForOffer_whenBothLocationsMissing_fallbackToDefaultLocation` | 1 | PASS |
| **UT-OFFER-015** | Unit | `createOfferByHr` | Probation salary > Proposed salary | 20M proposed, 22M probation | Throws `OfferValidationException` | `testCreateOfferByHr_probationSalaryGreaterThanProposed_throwsException` | 1 | PASS |
| **UT-OFFER-016** | Unit | Bean Validation | DTO Probation > Proposed | 20M proposed, 22M probation | Violation on `isProbationSalaryNotExceedProposed` | `testCreateOfferRequest_probationSalaryGreaterThanProposed_hasViolation` | 1 | PASS |
| **UT-OFFER-017** | Unit | `getPassedCandidatesForOffer` | GBR-07 Candidate has existing offer | Application has Draft offer | Candidate excluded from passed list | `testGetPassedCandidatesForOffer_filtersAllExistingOffers_keepsOnlyNoOffer` | 1 | PASS |
| **UT-OFFER-018** | Unit | `createOfferByHr` | GBR-07 Create when offer exists | Existing status in 11 locked statuses | Throws `OfferValidationException("OFFER_LOCKED_STATE")` | `testCreateOfferByHr_existingOffer_throwsException` | 11 | PASS |
| **UT-OFFER-019** | Unit | `updateOfferByHr` | Update Draft offer | Status = `Draft`, valid changes | Offer updated successfully | `testUpdateOfferByHr_draft_updatesSuccessfully` | 1 | PASS |
| **UT-OFFER-020** | Unit | `updateOfferByHr` | Update Group A allowed status | Status = `Draft` | Update allowed | `testUpdateOfferByHr_groupA_allowed` | 1 | PASS |
| **UT-OFFER-021** | Unit | `updateOfferByHr` | Update Group B locked statuses | Status in 9 non-draft statuses | Throws `OfferValidationException("OFFER_STATUS_INVALID")` | `testUpdateOfferByHr_groupB_throwsException` | 9 | PASS |
| **UT-OFFER-022** | Unit | `getOfferById` | Offer exists | Valid `offerId = 1` | Returns `OfferResponse` | `testGetOfferById_found_returnsOfferResponse` | 1 | PASS |
| **UT-OFFER-023** | Unit | `getOfferById` | Offer not found | Invalid `offerId = 999` | Throws `ResourceNotFoundException` | `testGetOfferById_notFound_throwsException` | 1 | PASS |
| **UT-OFFER-024** | Unit | `getOfferByApplicationId` | Application offer exists | Valid `applicationId = 10` | Returns `OfferResponse` | `testGetOfferByApplicationId_found_returnsOfferResponse` | 1 | PASS |
| **UT-OFFER-025** | Unit | `getOfferByApplicationId` | Application offer not found | Invalid `applicationId = 999` | Throws `ResourceNotFoundException` | `testGetOfferByApplicationId_notFound_throwsException` | 1 | PASS |
| **UT-OFFER-026** | Unit | `getAllOffersForHr` | Query all active offers | Status = null or "ALL" | Queries `findAllActiveByOrderByOfferIdAsc` | `testGetAllOffersForHr_allStatus_queriesActive` | 1 | PASS |
| **UT-OFFER-027** | Unit | `getAllOffersForHr` | Filter by status | Status = "Pending_Director" | Queries `findActiveByStatusOrderByOfferIdAsc` | `testGetAllOffersForHr_filteredStatus_queriesByStatus` | 1 | PASS |
| **UT-OFFER-028** | Unit | `getAllOffersForHr` | Search + status + sort | Term = "Java", status = "Draft", sort = "LATEST" | Queries via `Specification` | `testGetAllOffersForHr_withSearchAndSort_queriesWithSpecification` | 1 | PASS |
| **UT-OFFER-029** | Unit | `getOfferStats` | HR stat card counts | Repositories return counts | Returns map with 4 stat counts | `testGetOfferStats_returnsAccurateCounts` | 1 | PASS |
| **UT-OFFER-030** | Unit | `getOfferDetailForHr` | Comprehensive detail view | Offer with candidate & approval logs | Returns `OfferDetailResponse` | `testGetOfferDetailForHr_returnsComprehensiveDetails` | 1 | PASS |
| **UT-OFFER-031** | Unit | `deleteDraftOfferByHr` | Soft delete Draft offer | Status = `Draft` | `isDeleted` set to true | `testDeleteDraftOfferByHr_draftStatus_softDeletesSuccessfully` | 1 | PASS |
| **UT-OFFER-032** | Unit | `deleteDraftOfferByHr` | Delete non-draft offer | Status = `Pending_Director` | Throws `OfferValidationException("OFFER_NOT_DRAFT")` | `testDeleteDraftOfferByHr_notDraft_throwsException` | 1 | PASS |
| **UT-OFFER-033** | Unit | `sendOfferToCandidate` | Issue approved offer | Status = `Approved`, valid deadline | Status -> `Sent_Candidate`, App -> `Offered` | `testSendOfferToCandidate_approvedStatus_success` | 1 | PASS |
| **UT-OFFER-034** | Unit | `sendOfferToCandidate` | Issue unapproved offer | Status = `Draft` | Throws `OfferValidationException("OFFER_NOT_APPROVED")` | `testSendOfferToCandidate_notApprovedStatus_throwsException` | 1 | PASS |
| **UT-OFFER-035** | Unit | `sendOfferToCandidate` | Missing responsePeriodDays | `responsePeriodDays = null` | Throws `OfferValidationException` | `testSendOfferToCandidate_missingResponsePeriodDays_throwsOfferValidationException` | 1 | PASS |
| **UT-OFFER-036** | Unit | `sendOfferToCandidate` | Deadline >= Expected start date | 5 days period, start date in 3 days | Throws `OfferValidationException` | `testSendOfferToCandidate_deadlineNotBeforeExpectedStartDate_throwsOfferValidationException` | 1 | PASS |
| **UT-OFFER-037** | Unit | `approveOfferByDirector` | Director approves pending offer | Status = `Pending_Director` | Status -> `Director_Approved`, logs approval | `testApproveOfferByDirector_success` | 1 | PASS |
| **UT-OFFER-038** | Unit | `rejectOfferByDirector` | Director rejects pending offer | Status = `Pending_Director`, comments | Status -> `Director_Rejected`, logs rejection | `testRejectOfferByDirector_success` | 1 | PASS |
| **UT-OFFER-039** | Unit | `getDirectorStatusCounts` | Director filter badge counts | Repositories return counts | Returns map for all 7 director status groups | `testGetDirectorStatusCounts` | 1 | PASS |
| **UT-OFFER-040** | Unit | `getDirectorOffers` | Search & filter for Director | Term = "Backend", status = "Accepted" | Returns filtered page excluding Draft | `testGetDirectorOffers` | 1 | PASS |
| **UT-OFFER-041** | Unit | `getDirectorOfferHistory` | Real-time lifecycle status | Past approval on accepted offer | Preserves `Accepted` status | `testGetDirectorOfferHistory_preservesRealtimeStatus` | 1 | PASS |
| **UT-OFFER-042** | Unit | `rejectOfferByDirector` | Blank/whitespace rejection comments | `comments` in ["", "   "] | Rejection succeeds, status -> `Director_Rejected` | `rejectOfferByDirector_blankComments_succeeds` | 2 | PASS |
| **UT-OFFER-043** | Unit | `rejectOfferByDirector` | Null rejection comments | `comments = null` | Rejection succeeds with null comments | `rejectOfferByDirector_nullComments_succeeds` | 1 | PASS |
| **UT-OFFER-044** | Unit | `rejectOfferByDirector` | Reject when not Pending_Director | Status in [Draft, Approved, Rejected, Sent, ...] | Throws `OfferValidationException("OFFER_STATUS_INVALID")` | `rejectOfferByDirector_whenNotPendingDirector_throwsException` | 6 | PASS |
| **UT-OFFER-045** | Unit | `approveOfferByDirector` | Approve when not Pending_Director | Status in [Draft, Approved, Rejected, Sent, ...] | Throws `OfferValidationException("OFFER_STATUS_INVALID")` | `approveOfferByDirector_whenNotPendingDirector_throwsException` | 6 | PASS |
| **UT-OFFER-046** | Unit | `createOfferByHr` | Non-positive responsePeriodDays | `responsePeriodDays` in [0, -1, -5] | Throws `OfferValidationException` | `createOfferByHr_nonPositiveResponsePeriodDays_throwsException` | 3 | PASS |
| **UT-OFFER-047** | Unit | Bean Validation | CreateOfferRequest response period | `days` in [1, 60, 0, -1, 61] | 1 & 60 valid; 0, -1, 61 produce violations | `createOfferRequest_responsePeriodDaysBoundaries` | 5 | PASS |
| **UT-OFFER-048** | Unit | Bean Validation | UpdateOfferRequest response period | `days` in [1, 60, 0, -1, 61] | 1 & 60 valid; 0, -1, 61 produce violations | `updateOfferRequest_responsePeriodDaysBoundaries` | 5 | PASS |
| **UT-OFFER-049** | Unit | `sendOfferToCandidate` | Deadline equals expectedStartDate | Period = 5 days, start date = now + 5 days | Throws `OfferValidationException` | `sendOfferToCandidate_calculatedDeadlineEqualsExpectedStartDate_throwsValidationException` | 1 | PASS |
| **UT-OFFER-050** | Unit | `sendOfferToCandidate` | Deadline before expectedStartDate | Period = 3 days, start date = now + 7 days | Issue succeeds, status -> `Sent_Candidate` | `sendOfferToCandidate_calculatedDeadlineBeforeExpectedStartDate_succeeds` | 1 | PASS |
| **UT-OFFER-051** | Unit | `sendOfferToCandidate` | Issue from non-approved statuses | Status in [Draft, Pending, Rejected, Sent, ...] | Throws `OfferValidationException("OFFER_NOT_APPROVED")` | `sendOfferToCandidate_fromNonApprovedStatus_throwsValidationException` | 6 | PASS |
| **UT-OFFER-052** | Unit | `sendOfferToCandidate` | Mail sender throws exception | `offerMailSender` throws RuntimeException | Transaction succeeds, Offer remains `Sent_Candidate` | `sendOfferToCandidate_whenMailSenderThrowsException_transactionSucceedsAndStatusRemainsSent` | 1 | PASS |
| **UT-OFFER-053** | Unit | `approveOfferByDirector` | In-app notification throws error | `systemNotificationService` throws error | Approval succeeds, status -> `Director_Approved` | `approveOfferByDirector_whenNotificationThrows_stillCompletesApproval` | 1 | PASS |
| **UT-OFFER-054** | Unit | `rejectOfferByDirector` | In-app notification throws error | `systemNotificationService` throws error | Rejection succeeds, status -> `Director_Rejected` | `rejectOfferByDirector_whenNotificationThrows_stillCompletesRejection` | 1 | PASS |
| **UT-OFFER-055** | Unit | `sendOfferToCandidate` | Offer save failure | `offerProposalRepository.save()` throws error | Exception propagated, Application never saved | `sendOfferToCandidate_whenOfferSaveThrowsException_propagatesAndNeverUpdatesApplication` | 1 | PASS |
| **UT-OFFER-056** | Unit | `approveOfferByDirector` | Approval history save failure | `offerApprovalRepository.save()` throws error | Exception propagated, transaction fails | `approveOfferByDirector_whenApprovalSaveThrowsException_propagatesException` | 1 | PASS |
| **UT-OFFER-057** | Unit | `getAllOffersForHr` | Blank search string handling | Search = "   ", status = "ALL" | Uses fast-path query without join overhead | `getAllOffersForHr_blankSearch_queriesActiveFastPath` | 1 | PASS |
| **UT-CAND-001** | Unit | Candidate `list` | Status mapping for candidate portal | Offers in Sent, Accepted, Declined | Maps correct badges, labels, and canRespond | `listMapsStatusToLabelBadgeAndAnswerableFlag` | 1 | PASS |
| **UT-CAND-002** | Unit | Candidate `list` | Isolation of candidate offers | User ID = 41 | Queries visible statuses for logged-in user | `listAsksOnlyForOffersAlreadySentToTheCandidate` | 1 | PASS |
| **UT-CAND-003** | Unit | Candidate `accept` | Candidate accepts valid offer | Offer `Sent_Candidate`, `answerIfSent` = 1 | Status -> `Accepted`, Application -> Hired | `acceptSetsOfferAcceptedAndMovesApplicationToHired` | 1 | PASS |
| **UT-CAND-004** | Unit | Candidate `decline` | Candidate declines valid offer | Offer `Sent_Candidate`, `answerIfSent` = 1 | Status -> `Declined`, Application -> Rejected | `declineSetsOfferDeclinedAndRejectsApplication` | 1 | PASS |
| **UT-CAND-005** | Unit | Candidate answer | Duplicate answer attempt | Status in [`Accepted`, `Declined`] | Throws `OfferResponseException` | `answeringTwiceIsRefusedWithoutTouchingApplication` | 2 | PASS |
| **UT-CAND-006** | Unit | Candidate answer | Concurrent modification detected | Read `Sent_Candidate`, update count = 0 | Throws `OfferResponseException("Vui lòng tải lại trang")` | `failsWhenOfferWasAnsweredBetweenReadAndUpdate` | 1 | PASS |
| **UT-CAND-007** | Unit | Candidate answer | Offer of another candidate | Offer not found for logged-in user ID | Throws `ResourceNotFoundException` | `offerOfSomeoneElseOrNotYetSentIsNotFound` | 1 | PASS |
| **UT-CAND-008** | Unit | Candidate `getDetail` | Candidate views detail | Valid offer ID belonging to candidate | Returns full `CandidateOfferDetailResponse` | `getDetailReturnsFullOfferLetterData` | 1 | PASS |
| **UT-CAND-009** | Unit | Candidate `getDetail` | Non-existent or foreign offer | Foreign offer ID | Throws `ResourceNotFoundException` | `getDetailThrowsNotFoundWhenOfferDoesNotExistOrBelongsToOther` | 1 | PASS |
| **UT-CAND-010** | Unit | Candidate `decline` | Decline with reason | Non-blank reason string | Persists reason, rejects application | `declineWithReasonSetsOfferDeclinedAndRejectsApplication` | 1 | PASS |
| **UT-CAND-011** | Unit | Candidate `accept` | In-app notification to HR | Valid offer acceptance | Calls `notifyOfferResponseFromCandidate` | `acceptNotifiesHrProposedBy` | 1 | PASS |
| **UT-CAND-012** | Unit | Candidate `list` | Expired offer listing | `expectedStartDate` in the past | Label -> "Đã quá hạn phản hồi", canRespond = false | `listMapsExpiredOfferToAppropriateLabelAndDisablesResponse` | 1 | PASS |
| **UT-CAND-013** | Unit | Candidate `getDetail` | Expired offer detail | `expectedStartDate` in the past | Label -> "Đã quá hạn phản hồi", canRespond = false | `getDetailMapsExpiredOfferToAppropriateLabelAndDisablesResponse` | 1 | PASS |
| **UT-CAND-014** | Unit | Candidate `accept` | Accept expired offer (date fallback) | `expectedStartDate` in the past | Throws `OfferResponseException("quá ngày bắt đầu")` | `acceptExpiredOfferThrowsOfferResponseException` | 1 | PASS |
| **UT-CAND-015** | Unit | Candidate `decline` | Decline expired offer (date fallback) | `expectedStartDate` in the past | Throws `OfferResponseException("quá ngày bắt đầu")` | `declineExpiredOfferThrowsOfferResponseException` | 1 | PASS |
| **UT-CAND-016** | Unit | Candidate `accept` | Accept expired offer (deadline) | `responseDeadline` 2 hours ago | Throws `OfferResponseException("quá thời hạn phản hồi")` | `acceptExpiredOfferWithResponseDeadlineThrowsOfferResponseException` | 1 | PASS |
| **UT-CAND-017** | Unit | Candidate `decline` | Decline expired offer (deadline) | `responseDeadline` 2 hours ago | Throws `OfferResponseException("quá thời hạn phản hồi")` | `declineExpiredOfferWithResponseDeadlineThrowsOfferResponseException` | 1 | PASS |
| **UT-CAND-018** | Unit | Candidate `accept` | Accept offer with future deadline | Deadline in 3 days | Acceptance succeeds | `acceptOfferWithFutureResponseDeadlineSucceeds` | 1 | PASS |
| **UT-CAND-019** | Unit | Candidate `getDetail` | Response deadline mapping | Offer with deadline in 4 days | DTO maps deadline and period days accurately | `listAndDetailMapResponseDeadlineProperly` | 1 | PASS |
| **UT-CAND-020** | Unit | Candidate `accept` | Immediately past deadline boundary | Deadline was 1 second ago | Throws `OfferResponseException` | `acceptOffer_immediatelyPastResponseDeadline_throwsException` | 1 | PASS |
| **UT-CAND-021** | Unit | Candidate `accept` | Fallback: start date is today | `responseDeadline` null, start date = today | Allowed to respond today | `acceptOffer_whenResponseDeadlineIsNullAndExpectedStartDateIsToday_succeeds` | 1 | PASS |
| **UT-CAND-022** | Unit | Candidate `accept` | Fallback: start date in future | `responseDeadline` null, start date = tomorrow | Allowed to respond | `acceptOffer_whenResponseDeadlineIsNullAndExpectedStartDateInFuture_succeeds` | 1 | PASS |
| **UT-CAND-023** | Unit | Candidate `decline` | Blank/whitespace decline reason | `declineReason` in ["", "   "] | Normalized to null, calls 2-arg update | `declineOffer_blankReason_treatedAsNullAndSucceeds` | 2 | PASS |
| **UT-CAND-024** | Unit | Candidate `decline` | Null decline reason | `declineReason = null` | Calls 2-arg update, decline succeeds | `declineOffer_nullReason_succeeds` | 1 | PASS |
| **UT-CAND-025** | Unit | Candidate `decline` | Exactly 1000 chars decline reason | String of 1000 'a' characters | Passed intact to atomic update | `declineOffer_exactly1000CharactersReason_succeeds` | 1 | PASS |
| **UT-CAND-026** | Unit | Candidate `accept` | Accept after declining | Current status = `Declined` | Throws `OfferResponseException` | `acceptOffer_afterDeclining_throwsOfferResponseException` | 1 | PASS |
| **UT-CAND-027** | Unit | Candidate `decline` | Decline after accepting | Current status = `Accepted` | Throws `OfferResponseException` | `declineOffer_afterAccepting_throwsOfferResponseException` | 1 | PASS |
| **UT-CAND-028** | Unit | Candidate `getDetail` | Foreign offer access attempt | Foreign `offerId = 999` | Throws `ResourceNotFoundException` | `candidateCannotAccessOfferOwnedByAnotherUser_getDetail` | 1 | PASS |
| **UT-CAND-029** | Unit | Candidate `accept` | In-app notification outage | `systemNotificationService` throws error | Acceptance completes successfully | `acceptOffer_whenNotificationThrows_stillCompletesAcceptance` | 1 | PASS |
| **UT-CAND-030** | Unit | Candidate `decline` | In-app notification outage | `systemNotificationService` throws error | Decline completes successfully | `declineOffer_whenNotificationThrows_stillCompletesDecline` | 1 | PASS |
| **UT-CAND-031** | Unit | Candidate `decline` | Concurrent modification during decline | Read `Sent_Candidate`, update count = 0 | Throws `OfferResponseException`, no app update | `declineOffer_failsWhenOfferWasAnsweredBetweenReadAndUpdate` | 1 | PASS |
| **UT-EXP-001** | Unit | Excel Export | SELECTED scope with valid IDs | IDs = [1, 2], valid column list | Creates 2 sheets, numeric salary cells | `testExportSelectedScope_Success` | 1 | PASS |
| **UT-EXP-002** | Unit | Excel Export | SELECTED scope with empty IDs | `offerIds = []` | Throws `BaseBusinessException` | `testExportSelectedScope_EmptyIds_ThrowsException` | 1 | PASS |
| **UT-EXP-003** | Unit | Excel Export | FILTERED scope | Term = "Java", status = "Sent" | Exports matching records without pagination cap | `testExportFilteredScope_Success` | 1 | PASS |
| **UT-EXP-004** | Unit | Excel Export | ALL scope | Scope = `ALL` | Exports all active offers, ignores soft-deleted | `testExportAllScope_Success` | 1 | PASS |
| **UT-EXP-005** | Unit | Excel Export | Empty requested columns | `columns = []` | Throws `BaseBusinessException` | `testExport_EmptyColumns_ThrowsException` | 1 | PASS |
| **UT-EXP-006** | Unit | Excel Export | Column whitelist check | Column = "passwordHash" | Throws `BaseBusinessException` | `testExport_InvalidColumn_ThrowsException` | 1 | PASS |
| **UT-EXP-007** | Unit | Excel Export | Filename generator | Scope in [ALL, FILTERED, SELECTED] | Formats proper standard filenames with dates | `testGenerateExportFilename` | 1 | PASS |
| **UT-EXP-008** | Unit | Excel Export | SELECTED scope non-existing IDs | IDs = [999, 1000] (0 active entities) | Throws `BaseBusinessException("EMPTY_EXPORT_DATA")` | `testExportSelectedScope_NonExistingIds_ThrowsException` | 1 | PASS |
| **UT-EXP-009** | Unit | Excel Export | Null scope validation | `scope = null` | Throws `BaseBusinessException("INVALID_EXPORT_REQUEST")` | `testExport_NullScope_ThrowsException` | 1 | PASS |
| **UT-EXP-010** | Unit | Excel Export | Duplicate column handling | Columns = ["offerId", "offerId", "name"] | Exports cleanly without workbook error | `testExport_DuplicateColumns_HandlesGracefully` | 1 | PASS |
| **IT-OFFER-001** | WebMvc | GET `/offers` | HR view offers list | User with `ROLE_HR` | Returns HTTP 200, view `offers/list` | `it01_listOffers_returnsOkView` | 1 | PASS |
| **IT-OFFER-002** | WebMvc | GET `/offers/create` | HR view create form | User with `ROLE_HR` | Returns HTTP 200, view `offers/form` | `it02_showCreateForm_returnsFormView` | 1 | PASS |
| **IT-OFFER-003** | WebMvc | GET `/offers/{id}` | HR view offer detail | User with `ROLE_HR` | Returns HTTP 200, view `offers/detail` | `it03_showOfferDetail_returnsDetailView` | 1 | PASS |
| **IT-OFFER-004** | WebMvc | POST `/offers/{id}/delete` | HR delete draft offer | Valid CSRF, `ROLE_HR` | Redirects to `/offers` | `it04_deleteDraftOffer_redirectsToList` | 1 | PASS |
| **IT-OFFER-005** | WebMvc | POST `/offers/{id}/send` | HR send offer letter | Valid CSRF, `ROLE_HR` | Redirects to `/offers/{id}` | `it05_sendOffer_redirectsToDetail` | 1 | PASS |
| **IT-OFFER-006** | WebMvc | POST `/offers/export` | HR export excel | Valid JSON payload, `ROLE_HR` | Returns HTTP 200, Content-Type xlsx | `it06_exportOffers_hrSuccess` | 1 | PASS |
| **IT-OFFER-007** | WebMvc | POST `/offers/export` | Candidate export excel attempt | User with `ROLE_CANDIDATE` | Returns HTTP 403 Forbidden | `it07_exportOffers_unauthorizedRole_forbidden` | 1 | PASS |
| **IT-OFFER-008** | WebMvc | POST `/offers/export` | Invalid JSON payload | Missing scope | Returns HTTP 400 Bad Request | `it08_exportOffers_invalidPayload_badRequest` | 1 | PASS |
| **IT-OFFER-009** | WebMvc | GET `/offers` | Unauthenticated access | Guest user (no session) | Redirects to `/login` | `it09_unauthenticatedAccess_redirectsToLogin` | 1 | PASS |
| **IT-OFFER-010** | WebMvc | POST `/offers/create` | Create offer via modal AJAX | Valid JSON payload, `ROLE_HR` | Returns HTTP 201 Created | `it10_createOfferJson_success` | 1 | PASS |
| **IT-OFFER-011** | WebMvc | POST `/offers/create` | Business error in create AJAX | Active offer already exists | Returns HTTP 400 Bad Request | `it11_createOfferJson_businessError` | 1 | PASS |
| **IT-OFFER-012** | WebMvc | GET `/offers/{id}` | Detail modal AJAX | Accept: application/json | Returns HTTP 200 OK with ApiResponse | `it12_getOfferDetailJson_success` | 1 | PASS |
| **IT-OFFER-013** | WebMvc | PUT `/offers/{id}` | Update modal AJAX | Valid JSON payload, `ROLE_HR` | Returns HTTP 200 OK | `it13_updateOfferJson_success` | 1 | PASS |
| **IT-OFFER-014** | WebMvc | DELETE `/offers/{id}` | Delete modal AJAX | Valid CSRF, `ROLE_HR` | Returns HTTP 200 OK | `it14_deleteDraftOfferJson_success` | 1 | PASS |
| **IT-OFFER-015** | WebMvc | POST `/offers/{id}/send` | Send modal AJAX | Accept: application/json | Returns HTTP 200 OK | `it15_sendOfferJson_success` | 1 | PASS |
| **IT-OFFER-016** | WebMvc | POST `/offers/{id}/approve` | Director approve AJAX | Valid JSON payload, `ROLE_DIRECTOR` | Returns HTTP 200 OK | `it16_approveOfferJson_success` | 1 | PASS |
| **IT-OFFER-017** | WebMvc | POST `/offers/{id}/reject` | Director reject AJAX | Valid JSON payload, `ROLE_DIRECTOR` | Returns HTTP 200 OK | `it17_rejectOfferJson_success` | 1 | PASS |
| **IT-OFFER-018** | WebMvc | GET `/offers` | Candidate access internal list | User with `ROLE_CANDIDATE` | Returns HTTP 403 Forbidden | `it18_candidateAccessOffersList_isForbidden` | 1 | PASS |
| **IT-OFFER-019** | WebMvc | GET `/offers/{id}` | Candidate access internal detail | User with `ROLE_CANDIDATE` | Returns HTTP 403 Forbidden | `it19_candidateAccessOfferDetail_isForbidden` | 1 | PASS |
| **IT-OFFER-020** | WebMvc | POST `/offers/create` | Candidate create offer attempt | User with `ROLE_CANDIDATE` | Returns HTTP 403 Forbidden | `it20_candidateCreateOffer_isForbidden` | 1 | PASS |
| **IT-OFFER-021** | WebMvc | POST `/offers/create` | Malformed year in start date | Year = 20165 | Returns form with concise localized error | `it21_createOfferWithInvalidDate_showsShortConciseErrorMessage` | 1 | PASS |
| **IT-OFFER-022** | WebMvc | POST `/offers/{id}/approve` | Candidate call director approve | User with `ROLE_CANDIDATE` | Returns HTTP 403 Forbidden | `it22_candidateApproveOffer_isForbidden` | 1 | PASS |
| **IT-OFFER-023** | WebMvc | POST `/offers/{id}/reject` | Candidate call director reject | User with `ROLE_CANDIDATE` | Returns HTTP 403 Forbidden | `it23_candidateRejectOffer_isForbidden` | 1 | PASS |
| **IT-OFFER-024** | WebMvc | GET `/offers` | Negative page number | Param `page = -5` | Normalizes `currentPage` to 0, returns 200 | `it24_listOffers_negativePageNumber_normalizesToPageZero` | 1 | PASS |
| **WEB-CAND-001** | WebMvc | GET `/portal/offers` | Guest access candidate portal | Unauthenticated | Redirects to `/login` | `guestIsSentToLogin` | 1 | PASS |
| **WEB-CAND-002** | WebMvc | GET/POST `/portal/offers` | Staff access candidate portal | Roles: HR, Director, Admin, Manager | Returns HTTP 403 Forbidden | `internalStaffIsForbiddenFromListAndAnswers` | 1 | PASS |
| **WEB-CAND-003** | WebMvc | GET `/portal/offers` | Candidate sees action buttons | Sent offer vs Accepted offer | Renders accept/decline forms only for Sent | `candidateSeesAnswerButtonsOnlyOnOffersWaitingForThem` | 1 | PASS |
| **WEB-CAND-004** | WebMvc | GET `/portal/offers` | Empty offers list | Candidate has no offers | Renders empty state message | `candidateWithoutOffersSeesEmptyState` | 1 | PASS |
| **WEB-CAND-005** | WebMvc | POST `.../accept` | Candidate accepts offer | Valid CSRF, `ROLE_CANDIDATE` | Redirects with flash success message | `acceptRedirectsWithSuccessMessage` | 1 | PASS |
| **WEB-CAND-006** | WebMvc | POST `.../decline` | Candidate declines offer | Valid CSRF, `ROLE_CANDIDATE` | Redirects with flash success message | `declineRedirectsWithSuccessMessage` | 1 | PASS |
| **WEB-CAND-007** | WebMvc | POST answer | Offer already answered | Service throws OfferResponseException | Redirects with flash error message | `refusedAnswerRedirectsWithErrorMessage` | 1 | PASS |
| **WEB-CAND-008** | WebMvc | POST `.../accept` | Foreign offer ID | Service throws ResourceNotFoundException | Returns HTTP 404 Not Found | `answeringSomeoneElsesOfferIsNotFound` | 1 | PASS |
| **WEB-CAND-009** | WebMvc | POST answer | Missing CSRF token | Candidate POST without csrf | Returns HTTP 403 Forbidden | `answerWithoutCsrfIsForbidden` | 1 | PASS |
| **WEB-CAND-010** | WebMvc | GET `/portal/offers/{id}` | View candidate offer detail | Valid candidate offer | Renders detail view with breakdown & dialogs | `candidateCanViewOfferDetail` | 1 | PASS |
| **WEB-CAND-011** | WebMvc | POST `.../accept` | Accept from detail view | Param `fromDetail = true` | Redirects to `/portal/offers/{id}` with modal | `acceptFromDetailRedirectsToDetail` | 1 | PASS |
| **WEB-CAND-012** | WebMvc | POST `.../decline` | Decline from detail view | Param `fromDetail = true`, reason | Redirects to detail view with reason display | `declineFromDetailWithReasonRedirectsToDetail` | 1 | PASS |
| **WEB-CAND-013** | WebMvc | GET `.../decline` | View decline confirmation page | Answerable offer | Renders `offers/candidate-decline` view | `candidateCanViewDeclinePage` | 1 | PASS |
| **WEB-CAND-014** | WebMvc | GET `.../decline` | Decline page when answered | Offer already `Declined` | Redirects to detail with error message | `declinePageWhenAlreadyAnsweredRedirectsToDetail` | 1 | PASS |
| **WEB-CAND-015** | WebMvc | GET `/portal/offers` | Countdown timer attribute | Offer with response deadline | Renders `cof-countdown` and `data-deadline` | `candidateViewsOfferWithDeadlineAndCountdown` | 1 | PASS |
| **WEB-CAND-016** | WebMvc | GET `/portal/offers/{id}` | Detail deadline box attribute | Offer with response deadline | Renders `cof-deadline-box` and formatted text | `candidateViewsOfferDetailWithDeadlineAndCountdown` | 1 | PASS |
| **REPO-CAND-001** | Repo | `findVisibleByUserId` | Owner isolation | User ID = owner vs -1 | Found for owner, empty for -1 | `sentOfferIsVisibleOnlyToItsOwner` | 1 | PASS |
| **REPO-CAND-002** | Repo | `findVisibleById` | Internal status invisibility | Status in [Draft, Pending, Approved, Rejected] | Returns empty Optional for candidate | `internalOfferIsNeverVisibleToTheCandidate` | 1 | PASS |
| **REPO-CAND-003** | Repo | `answerIfSent` | Atomic conditional update | Status = `Sent_Candidate` | First update returns 1; second returns 0 | `answerIfSentChangesOnlyOffersStillWaitingForTheCandidate` | 1 | PASS |
| **REPO-CAND-004** | Repo | `answerIfSent` | Non-sent offer update | Status = `Pending_Director` | Returns 0, offer unmodified | `answerIfSentLeavesInternalOffersAlone` | 1 | PASS |
| **REPO-CAND-005** | Repo | Notifications Query | Notification JDBC query check | Candidate user account | Returns page without conversion error | `candidateCanQueryNotificationsWithoutJdbcConversionError` | 1 | PASS |
| **BUG-OFFER-001** | Integration | Director Offers Page | Default Director landing | Status = default (Pending_Director) | Renders `offers/director-list`, tab PENDING | `testDirectorOffersPage` | 1 | PASS |
| **BUG-OFFER-002** | Integration | Director Offers Page | Status filter parameter | Status = "Accepted" | Renders `offers/director-list`, tab HISTORY | `testDirectorOffersPage_withStatusFilter` | 1 | PASS |

---

## 6. Detailed Test Cases

### UT-OFFER-042 - Director Rejection with Blank Comments
- **Objective:** Verify that the Director can reject a proposal with blank or whitespace-only comments without triggering validation errors.
- **Given:** An offer in `Pending_Director` status.
- **When:** Director invokes `rejectOfferByDirector(offerId, "   ")`.
- **Then:** Offer status transitions to `Director_Rejected`, and an `OfferApproval` record is created.
- **Mock dependencies:** `OfferProposalRepository`, `OfferApprovalRepository`, `UserRepository`.
- **Actual Test Method:** `rejectOfferByDirector_blankComments_succeeds(String comments)`

### UT-OFFER-045 - Rejecting Offer in Invalid Lifecycle State
- **Objective:** Ensure rejection is blocked when the offer is not in `Pending_Director` (e.g., already approved, already rejected, or still draft).
- **Given:** An offer in `Director_Approved` status.
- **When:** Director invokes `rejectOfferByDirector(offerId, "Comments")`.
- **Then:** Throws `OfferValidationException` with error code `OFFER_STATUS_INVALID`. No database changes are committed.
- **Mock dependencies:** `OfferProposalRepository`.
- **Actual Test Method:** `rejectOfferByDirector_whenNotPendingDirector_throwsException(String invalidStatus)`

### UT-OFFER-047 - Response Period Days Boundary Validation
- **Objective:** Validate boundary constraints [1..60] for `responsePeriodDays` in `CreateOfferRequest`.
- **Given:** A `CreateOfferRequest` object with boundary values: 1, 60, 0, -1, 61.
- **When:** Hibernate Validator validates the request.
- **Then:** Values 1 and 60 produce zero violations; values 0, -1, and 61 produce constraint violations on `responsePeriodDays`.
- **Mock dependencies:** None (Bean Validation engine).
- **Actual Test Method:** `createOfferRequest_responsePeriodDaysBoundaries(int days, boolean expectedValid)`

### UT-OFFER-049 - Response Deadline Equals Expected Start Date
- **Objective:** Verify that an offer cannot be issued if the response deadline coincides with or falls after the expected start date.
- **Given:** An approved offer where `LocalDate.now().plusDays(responsePeriodDays)` equals `expectedStartDate`.
- **When:** HR calls `sendOfferToCandidate(offerId)`.
- **Then:** Throws `OfferValidationException("Thời hạn phản hồi phải trước ngày bắt đầu dự kiến")`.
- **Mock dependencies:** `OfferProposalRepository`.
- **Actual Test Method:** `sendOfferToCandidate_calculatedDeadlineEqualsExpectedStartDate_throwsValidationException()`

### UT-OFFER-052 - Mail Delivery Failure Containment
- **Objective:** Ensure SMTP network failures do not roll back the issuing of an offer letter.
- **Given:** An approved offer ready to be sent to candidate. `OfferMailSender.sendOfferLetterToCandidate()` throws a `RuntimeException`.
- **When:** HR calls `sendOfferToCandidate(offerId)`.
- **Then:** The exception is caught and logged as a warning. The transaction completes successfully, setting offer status to `Sent_Candidate` and application status to `Offered`.
- **Mock dependencies:** `OfferProposalRepository`, `ApplicationRepository`, `OfferMailSender`.
- **Actual Test Method:** `sendOfferToCandidate_whenMailSenderThrowsException_transactionSucceedsAndStatusRemainsSent()`

### UT-CAND-020 - Response Immediately Past Deadline
- **Objective:** Ensure candidate response is blocked if current time has passed the response deadline.
- **Given:** A `Sent_Candidate` offer where `responseDeadline` is 1 second in the past (`now.minusSeconds(1)`).
- **When:** Candidate calls `accept(offerId)`.
- **Then:** Throws `OfferResponseException` stating the deadline has expired. No repository update is called.
- **Mock dependencies:** `CandidateOfferRepository`.
- **Actual Test Method:** `acceptOffer_immediatelyPastResponseDeadline_throwsException()`

### UT-CAND-025 - Candidate Decline with Maximum Length Reason
- **Objective:** Verify candidate decline with exactly 1000 characters is accepted without error.
- **Given:** A valid `Sent_Candidate` offer and a decline reason string of exactly 1000 characters.
- **When:** Candidate calls `decline(offerId, longReason)`.
- **Then:** The 1000-character string is passed to `CandidateOfferRepository.answerIfSent(offerId, "Declined", longReason)` and application status is updated to rejected.
- **Mock dependencies:** `CandidateOfferRepository`, `ApplicationStatusService`.
- **Actual Test Method:** `declineOffer_exactly1000CharactersReason_succeeds()`

### UT-CAND-026 - Prevent Re-acceptance After Declining
- **Objective:** Prevent a candidate from accepting an offer that was previously declined.
- **Given:** An offer in `Declined` status.
- **When:** Candidate calls `accept(offerId)`.
- **Then:** Throws `OfferResponseException("Thư mời này đã được trả lời nên không thể thay đổi")`.
- **Mock dependencies:** `CandidateOfferRepository`.
- **Actual Test Method:** `acceptOffer_afterDeclining_throwsOfferResponseException()`

### IT-OFFER-022 - Role Authorization: Candidate Calling Director Endpoint
- **Objective:** Ensure users with `ROLE_CANDIDATE` cannot access Director approval endpoints.
- **Given:** Authenticated session with `ROLE_CANDIDATE`.
- **When:** POST `/offers/1/approve` is called.
- **Then:** Spring Security returns HTTP 403 Forbidden.
- **Mock dependencies:** Spring Security filter chain.
- **Actual Test Method:** `it22_candidateApproveOffer_isForbidden()`

---

## 7. State Transition Coverage Matrix

| Initial State | Action | Target State | Expected Result | Enforcement Layer |
|---|---|---|---|---|
| `Draft` | Save Draft / Update | `Draft` | **Allowed** | `OfferServiceImpl.updateOfferByHr` |
| `Draft` | Submit to Director | `Pending_Director` | **Allowed** | `OfferServiceImpl.updateOfferByHr` |
| `Draft` | Delete | Soft-deleted (`isDeleted=true`) | **Allowed** | `OfferServiceImpl.deleteDraftOfferByHr` |
| `Draft` | Director Approve | `Director_Approved` | **Blocked** | Throws `OfferValidationException` |
| `Draft` | Director Reject | `Director_Rejected` | **Blocked** | Throws `OfferValidationException` |
| `Draft` | HR Issue Offer | `Sent_Candidate` | **Blocked** | Throws `OfferValidationException` |
| `Pending_Director` | Director Approve | `Director_Approved` | **Allowed** | `OfferServiceImpl.approveOfferByDirector` |
| `Pending_Director` | Director Reject | `Director_Rejected` | **Allowed** | `OfferServiceImpl.rejectOfferByDirector` |
| `Pending_Director` | HR Edit | — | **Blocked** | GBR-07: Throws `OFFER_STATUS_INVALID` |
| `Pending_Director` | HR Delete | — | **Blocked** | Throws `OFFER_NOT_DRAFT` |
| `Pending_Director` | HR Issue Offer | `Sent_Candidate` | **Blocked** | Throws `OFFER_NOT_APPROVED` |
| `Director_Approved` | HR Issue Offer | `Sent_Candidate` | **Allowed** | `OfferServiceImpl.sendOfferToCandidate` |
| `Director_Approved` | Director Approve Again | — | **Blocked** | Throws `OFFER_STATUS_INVALID` |
| `Director_Approved` | Director Reject | — | **Blocked** | Throws `OFFER_STATUS_INVALID` |
| `Director_Rejected` | Director Approve | — | **Blocked** | Throws `OFFER_STATUS_INVALID` |
| `Director_Rejected` | HR Issue Offer | — | **Blocked** | Throws `OFFER_NOT_APPROVED` |
| `Sent_Candidate` | Candidate Accept | `Accepted` | **Allowed** | `CandidateOfferServiceImpl.accept` |
| `Sent_Candidate` | Candidate Decline | `Declined` | **Allowed** | `CandidateOfferServiceImpl.decline` |
| `Sent_Candidate` | HR Re-issue | — | **Blocked** | Throws `OFFER_NOT_APPROVED` |
| `Accepted` | Candidate Decline | — | **Blocked** | Throws `OfferResponseException` |
| `Accepted` | Candidate Accept Again | — | **Blocked** | Throws `OfferResponseException` |
| `Declined` | Candidate Accept | — | **Blocked** | Throws `OfferResponseException` |
| `Declined` | Candidate Decline Again | — | **Blocked** | Throws `OfferResponseException` |

---

## 8. Test Execution Commands & Environment

### Maven Command Executed:
```powershell
.\mvnw.cmd test -Dtest="OfferServiceTests,CandidateOfferServiceTests,CandidateOfferWebTests,OfferControllerBugTests,CandidateOfferQueryTests,OfferExportServiceTests,OfferIntegrationTests"
```

### Environment Parameters:
- **Build Timestamp:** October 19, 2026 10:06:47 (GMT+7)
- **Total Build Time:** **01:02 min**
- **Maven Build Status:** **BUILD SUCCESS**

---

## 9. Actual Test Results & Execution Metrics

| STT | Test Suite | Test Level | Total Invocations | Passed | Failed | Skipped | Elapsed Time |
|:---:|---|---|---:|---:|---:|---:|---:|
| 1 | `com.group2.rms.offer.service.OfferServiceTests` | Service Unit Test | **106** | 106 | 0 | 0 | 0.759 s |
| 2 | `com.group2.rms.offer.service.CandidateOfferServiceTests` | Service Unit Test | **33** | 33 | 0 | 0 | 0.286 s |
| 3 | `com.group2.rms.offer.CandidateOfferWebTests` | WebMvc Test | **16** | 16 | 0 | 0 | 11.490 s |
| 4 | `com.group2.rms.offer.OfferIntegrationTests` | WebMvc Integration Test | **24** | 24 | 0 | 0 | 14.050 s |
| 5 | `com.group2.rms.offer.OfferExportServiceTests` | Service Unit Test | **10** | 10 | 0 | 0 | 5.582 s |
| 6 | `com.group2.rms.offer.repository.CandidateOfferQueryTests` | Data JPA Repository Test | **5** | 5 | 0 | 0 | 3.665 s |
| 7 | `com.group2.rms.offer.controller.OfferControllerBugTests` | Controller Integration Test | **2** | 2 | 0 | 0 | 17.760 s |
| **TOTAL** | **7 Test Suites** | **Entire Offer Module** | **196** | **196** | **0** | **0** | **53.592 s** |

> **Success Rate:** **100% PASS (196 / 196 tests passed, 0 failures, 0 errors, 0 skipped)**.

---

## 10. Missing Validations, Boundary Limitations & Findings

During implementation and test coverage analysis, the following observations and edge-case limitations in production code were identified:

1. **DeclineReason String Length Validation:**
   - Database schema specifies `DeclineReason NVARCHAR(1000)`.
   - `CandidateOfferController.decline()` accepts `@RequestParam(value = "declineReason", required = false) String declineReason`. There is no `@Size(max = 1000)` annotation on the controller parameter or length check in `CandidateOfferServiceImpl.answer()`.
   - If a candidate submits >1000 characters, validation does not fail in Java code; the database would reject the update via truncation or SQL exception. A DTO with `@Size(max = 1000)` is recommended for future refactoring.
2. **HR vs Director Method-Level Security:**
   - In `SecurityConfig.java`, `/offers/**` allows `ROLE_HR`, `ROLE_DIRECTOR`, and `SYSTEM_ADMIN`.
   - While `ROLE_CANDIDATE` is strictly forbidden (HTTP 403), `OfferController` does not declare method-level `@PreAuthorize("hasRole('DIRECTOR')")` on `/{id}/approve` or `/{id}/reject`. Endpoint protection currently relies on frontend UI role-hiding and backend identity resolution (`resolveProposedBy(null)`). Adding `@PreAuthorize` on Director-specific endpoints is recommended.
3. **Hardcoded Clock (`LocalDateTime.now()`) in Expiration Logic:**
   - `CandidateOfferServiceImpl.isExpired()` and `OfferServiceImpl.sendOfferToCandidate()` call `LocalDateTime.now()` directly instead of injecting a `java.time.Clock` bean.
   - Deterministic microsecond testing exactly at deadline boundaries (`deadline == now`) cannot be isolated without a clock mock. Tests use deterministic deltas (e.g., `minusSeconds(1)`, `plusDays(3)`) to prevent flaky execution.

---

## 11. Conclusion & Regression Assessment

1. **Coverage Assessment:**
   - Test coverage has expanded from 135 runs to **196 runs** across 7 test suites (+61 test runs / +34 test methods).
   - All edge cases, boundary values, role authorizations, failure scenarios, and invalid lifecycle transitions are thoroughly tested.
2. **Regression Assessment:**
   - **Regression Risk: ZERO (None).**
   - No production code was modified during this test expansion.
   - All 196 tests execute cleanly with 100% pass rate under Apache Maven.

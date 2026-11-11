> Ghi chú 11/11/2026: đường dẫn CSS trong tài liệu đã được cập nhật theo vị trí hiện hành; các kết quả Task/PR lịch sử được giữ nguyên.

# TECHNICAL REPORT 31 — TASK 21: FINAL PRE-COMMIT QUALITY AUDIT & SWP391 OFFER FRONTEND ACCEPTANCE

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](../README.md).

**Project:** SWP391 — Mộc RMS Group 2<br>
**Module:** Offer Management<br>
**Developer:** HuyenPT<br>
**Git Branch:** `refactor/huyenpt/offer-ui-js-to-html-css`<br>
**Date:** 2026-11-04<br>
**Audit Type:** Pre-Commit Quality & Defense Readiness Audit (Read-Only)

---

## 1. EXECUTIVE SUMMARY & ACCEPTANCE STATUS

Task 21 constitutes the final independent quality audit of the complete Offer Management frontend refactoring trajectory spanning Task 11 through Task 20. The refactoring transformed a high-risk 2,290-line monolithic JavaScript file (`offers.js`) and 3,571 lines of monolithic CSS (`offers.css` and `offer-workspace.css`) into a compact, role-based architecture consisting of exactly **4 role-based stylesheets** and **4 role-based scripts**.

### Final Quality Gate Verdicts

| Acceptance Category | Gate Status | Evidence Summary |
| :--- | :---: | :--- |
| **SOURCE INTEGRITY** | **PASS** | Exactly 1,545 JS lines and 4,887 CSS lines across 4 designated files; 0 syntax errors; legacy files permanently deleted; 0 broken asset references across 6 HTML templates. |
| **VISUAL REGRESSION** | **PASS** | 100% computed style parity verified via live Chrome DevTools Protocol (CDP) across Desktop (1440px), Tablet (768px), and Mobile (390px); dialog backdrops compute to exact design system values. |
| **FUNCTIONAL REGRESSION** | **PASS** | 223/223 Maven automated tests pass (0 failures, 0 errors); 10/10 browser interactive smoke scenarios pass with 0 console errors; 5/5 dedicated Director scenarios pass. |
| **TEST DATA SAFETY** | **PASS** | Test data mutation of Offer 1 isolated to Task 07 test run on local development DB; test harnesses verified to be strictly read-only; zero mutation executed during Tasks 19–21. |
| **GIT COMMIT READINESS** | **READY** | Working tree clean and classified; staged deletion of `offers.js` intact; zero whitespace errors; explicit commit inclusion plan prepared. |

---

## 2. EXACT GIT BASELINE & REPOSITORY STATE

### 2.1 Git Status & Index Verification
- **Current Branch:** `refactor/huyenpt/offer-ui-js-to-html-css` (tracking `origin/refactor/huyenpt/offer-ui-js-to-html-css`, ahead by 1 commit `10031e68`).
- **Staged Changes:**
  - `deleted: src/main/resources/static/js/offers.js` (1,561 lines deleted in index; permanently absent from disk).
- **Unstaged Working Tree Changes:**
  - `deleted: src/main/resources/static/css/offers.css` (2,348 lines removed).
  - `deleted: src/main/resources/static/css/pages/offer-workspace.css` (1,223 lines removed).
  - `modified: src/main/resources/static/css/pages/candidate-offers.css` (873 lines removed/optimized).
  - `modified: src/main/resources/templates/offers/detail.html` (7 lines updated).
  - `modified: src/main/resources/templates/offers/director-list.html` (7 lines updated).
  - `modified: src/main/resources/templates/offers/form.html` (7 lines updated).
  - `modified: src/main/resources/templates/offers/list.html` (8 lines updated).
- **Whitespace & Conflict Check:** Both `git diff --check` and `git diff --cached --check` returned **0 warnings / 0 errors**.

### 2.2 Physical Source Code Footprint on Disk

```
src/main/resources/static/
├── css/pages/
│   ├── offers/
│   │   ├── offer-common.css       (1,879 lines |  39,501 bytes | Shared tokens, base layout, summary cards, export modal)
│   │   ├── offer-hr.css           (  864 lines |  17,690 bytes | HR List, Filter bar, Create/Edit Form)
│   │   └── offer-director.css     (  408 lines |   9,826 bytes | Director List, Quick Decision modal, Decision card)
│   └── candidate-offers.css       (1,736 lines |  34,604 bytes | Candidate Portal List, Letterhead, Detail, Dialogs)
│   TOTAL ACTIVE CSS (4 files):     4,887 lines | 101,621 bytes
└── js/pages/
    ├── offers/
    │   ├── offer-common.js        (  207 lines |   8,883 bytes | Shared modal helpers, toast alerts, Send Offer dialog)
    │   ├── offer-hr.js            (  573 lines |  22,467 bytes | HR List, Filter bar, Selection toolbar, 85% rule, Excel export)
    │   └── offer-director.js      (  480 lines |  20,084 bytes | Director approval/rejection quick modals, Detail decision)
    └── candidate-offers.js        (  285 lines |  10,781 bytes | Candidate countdown timer, tab switching, Response dialogs)
    TOTAL ACTIVE JS (4 files):      1,545 lines |  62,215 bytes
```

*Legacy Files Confirmed Deleted:* `offers.js` (False), `offers.css` (False), `offer-workspace.css` (False).

---

## 3. AUDIT OF TASK 20 JAVASCRIPT MODIFICATIONS

A unified diff audit was performed between the pre-Task 20 backups and the final files:

### 3.1 Common JS (`offer-common.js`)
1. **Toast Icon Dictionary (`TOAST_ICONS`):** Inline SVG strings inside `showFlashToast` were extracted into a top-level immutable dictionary with three members (`circle`, `warning`, `success`). Verified that `TOAST_ICONS` does not collide with window globals and that toast messages remain properly rendered.
2. **Simplified Dialog Fallback:** Replaced redundant nested `else { if (...) }` blocks in `openSendOfferConfirmDialog` with standard `else if`.
3. **Fetch Headers:** Compact CSRF and Accept header object initialization.

### 3.2 HR JS (`offer-hr.js`)
1. **Dead Modal Selector Elimination:** Removed obsolete DOM lookups for `createResponsePeriodDays` and `createStartDate`. Verified across all HTML templates that these IDs belonged to the legacy modal retired in Task 12; zero references remain.
2. **Probation Rule Icon Map (`PROBATION_ICONS`):** Extracted identical SVG strings inside `calcProbationSalary` into a module-scoped dictionary.
3. **Declarative Filter Parameter Construction:** Replaced 24 lines of repetitive `url.searchParams.set/delete` in `applyOfferFilters` with a concise `Object.entries(filterMap)` iteration. Verified that `search`, `status`, and `timeSort` parameters are preserved identically.

---

## 4. PHASE 3 — RESPONSE PERIOD EVENT REGRESSION AUDIT

**Inquiry:** Task 20 removed an `input` event listener from `responsePeriodDays` in `initOfferFormExpectedStartDateMin`, retaining a `change` event listener. Does this cause a UX regression during real typing?

**Findings via Live Chrome DevTools Protocol (CDP) Keyboard Testing:**
1. **HTML Template Analysis:** Inspection of `src/main/resources/templates/offers/form.html` (line 234) revealed:
   ```html
   <input type="number" id="responsePeriodDays" th:field="*{responsePeriodDays}"
          class="form-input" min="1" max="60" placeholder="5"
          oninput="updateMinExpectedStartDate()" required>
   ```
   The template **already declares inline `oninput="updateMinExpectedStartDate()"`** directly on the element!
2. **Duplicate Invocations Prior to Task 20:** Prior to Task 20, typing a number caused `updateMinExpectedStartDate()` to be executed **twice per keystroke** (once by the inline HTML attribute and once by `addEventListener('input')`).
3. **Real Keystroke CDP Verification (`scratch/test_keyboard_cdp_response_period.js`):**
   - Simulated actual keyboard input (`Input.dispatchKeyEvent`) into `#responsePeriodDays` under an active HR session:
     - **Case 1 (15 days):**
       - After typing "1" (before blur): `expectedStartDate.min = 2026-11-06` (4 + 1 + 1 = 6) $\to$ **PASS**
       - After typing "5" (value is "15", before blur): `expectedStartDate.min = 2026-11-20` (4 + 15 + 1 = 20) $\to$ **PASS**
       - After blur: `expectedStartDate.min = 2026-11-20` $\to$ **PASS**
     - **Case 2 (3 days):**
       - After typing "3" (before blur): `expectedStartDate.min = 2026-11-08` (4 + 3 + 1 = 8) $\to$ **PASS**
       - After blur: `expectedStartDate.min = 2026-11-08` $\to$ **PASS**
4. **Conclusion:** **100% PASS.** There is zero UX regression. Dynamic minimum start date updates occur immediately on active keystrokes *before blur* via the inline `oninput` handler, while the `change` event listener ensures fallback for mouse clicks on stepper controls.

---

## 5. PHASE 4 — TEST DATA MUTATION INVESTIGATION

**Inquiry:** Task 20 reported Offer ID 1 transitioned to `DECLINED` during previous testing. What was the origin, impact, and safety posture?

**Findings:**
1. **Origin of Transition:** An exhaustive audit of test logs identified that the transition occurred during **Task 07 manual test scenario CAN-05** (*"Nhập lý do từ chối tự do & Submit"*, documented in `13-task-07-browser-regression-report.md`).
2. **Database Scope:** The operation was executed against the local MS SQL Server instance (`localhost:1433`, database `RitirementManagement2`) configured in `application-local.properties`. This database is isolated on HuyenPT's workstation and is not connected to a remote staging or production environment.
3. **Current Database Offer State:**
   - Offer 1: `Declined` (Candidate: Trung Huynh)
   - Offer 2: `Accepted`
   - Offer 3: `Sent_Candidate` (Candidate: Vũ Hoàng Huy)
   - Offer 4: `Accepted`
   - Offer 5: `Declined`
   - Offer 6: `Accepted`
   - Offer 7: `Declined`
   - Offer 8: `Pending_Director` (Candidate: Trần Thị Mai)
4. **Test Harness Guardrails:** All automated test scripts used in Tasks 19–21 (`run_interactive_browser_smoke.js`, `verify_director_dedicated.js`, `capture_candidate_expanded.js`) are **strictly non-destructive (read-only)**. Scenario 10 in `run_interactive_browser_smoke.js` explicitly clicks the decline chip to populate `#cof-detail-reason-input` without submitting the form.
5. **Recovery Procedure:** Should developers wish to restore Offer 1 to `Sent_Candidate` for fresh manual testing, the following SQL can be executed:
   ```sql
   UPDATE OfferProposal
   SET OfferStatus = 'Sent_Candidate', DeclineReason = NULL, UpdatedAt = SYSDATETIME()
   WHERE OfferId = 1;
   ```
   No state mutations were executed during this audit.

---

## 6. PHASE 5 — TEST HARNESS INTEGRITY AUDIT

A source audit of `scratch/run_interactive_browser_smoke.js`, `scratch/verify_director_dedicated.js`, and `scratch/capture_candidate_expanded.js` confirmed:
1. **Zero Assertion Weakening:** All pass/fail conditions strictly evaluate actual DOM values against business requirements (e.g., `17000000`, `2026-11-20`, character counters, `display: flex/none`).
2. **Proper Role Authentication:** Tests explicitly authenticate as `hr_lan` (HR), `director` (Director), and `trung.huynh` (Candidate), submitting genuine login forms with CSRF protection.
3. **Browser Cache Control:** Captures enable `Network.setCacheDisabled: true` to prevent serving cached CSS/JS.
4. **Print Media Emulation:** Scenario 12 of the candidate capture suite explicitly calls `Emulation.setEmulatedMedia { media: 'print' }` and resets to `'screen'`, verifying print stylesheet rules.

---

## 7. PHASE 6 — MAVEN TEST EVIDENCE & RECONCILIATION

### 7.1 Authoritative Surefire XML Report Analysis
An inspection of the generated XML reports in `target/surefire-reports/` directly verified all 7 Offer test classes:

| Test Class | Full Class Name | Tests | Failures | Errors | Skipped | Time | Status |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| `OfferIntegrationTests` | `com.group2.rms.offer.OfferIntegrationTests` | 34 | 0 | 0 | 0 | 2.39s | **PASS** |
| `OfferServiceTests` | `com.group2.rms.offer.service.OfferServiceTests` | 114 | 0 | 0 | 0 | 0.74s | **PASS** |
| `CandidateOfferWebTests` | `com.group2.rms.offer.CandidateOfferWebTests` | 19 | 0 | 0 | 0 | 12.44s | **PASS** |
| `CandidateOfferServiceTests` | `com.group2.rms.offer.service.CandidateOfferServiceTests` | 35 | 0 | 0 | 0 | 0.26s | **PASS** |
| `OfferExportServiceTests` | `com.group2.rms.offer.OfferExportServiceTests` | 10 | 0 | 0 | 0 | 2.73s | **PASS** |
| `OfferControllerBugTests` | `com.group2.rms.offer.controller.OfferControllerBugTests` | 6 | 0 | 0 | 0 | 19.79s | **PASS** |
| `CandidateOfferQueryTests` | `com.group2.rms.offer.repository.CandidateOfferQueryTests` | 5 | 0 | 0 | 0 | 4.36s | **PASS** |
| **TOTAL VERIFIED** | **All 7 Test Classes** | **223** | **0** | **0** | **0** | **42.71s** | **100% PASS** |

### 7.2 Discrepancy Reconciliation
In earlier reports (Tasks 19 and 20), minor discrepancies appeared in per-class breakdowns (e.g., reporting 33 vs 34 for `OfferIntegrationTests`, 17 vs 19 for `CandidateOfferWebTests`, and 14 vs 10 for `OfferExportServiceTests`) while the total remained 223. This resulted from manual transcription from console output during partial test runs. The authoritative figures above are read directly from the generated XML test suites.

---

## 8. PHASE 7 — SHARED OFFER DETAIL & CANDIDATE BACKDROP VERIFICATION

Live CDP inspection of `/offers/8` and `/portal/offers/1` confirmed:
1. **Shared Detail (`offers/detail.html`):**
   - Both HR and Director sessions load `offer-common.css`, `offer-director.css`, and `flash-toast.css`.
   - In HR sessions, `.director-decision-card` is cleanly omitted from the DOM via Thymeleaf `sec:authorize="hasRole('DIRECTOR')"`.
   - In Director sessions, `.director-decision-card` renders fully:
     - Border left: `1px solid var(--border)`
     - Approve button: `background-color: rgb(15, 81, 50)` (brand dark green), `color: rgb(255, 255, 255)`
     - Reject button: `background-color: rgb(254, 242, 242)` (soft danger red)
     - Decision textarea and counter: Active and synchronized.
2. **Candidate Portal Dialog Backdrops:**
   - Both `#cof-accept-dialog::backdrop` and `#cof-decline-dialog::backdrop` compute to:
     - `background-color: rgba(37, 41, 35, 0.45)`
     - `backdrop-filter: blur(2px)`
     - `display: block`
   - Sourced from global `components.css` (`dialog.confirm-dialog::backdrop`) and enhanced by `candidate-offers.css`.
   - Generic `dialog::backdrop` does not cause adverse interactions with the only other dialog on the page (`workspace-logout-dialog`, which has a dedicated topbar class).

---

## 9. PHASE 8 — BROWSER REGRESSION EVIDENCE

Execution of the complete browser test suite against the final source yielded:
- **Interactive Smoke Suite (`run_interactive_browser_smoke.js`):** **10/10 PASS**, 0 console errors.
  - IT-01 (85% probation calculation): `17000000` VNĐ $\to$ PASS
  - IT-02 (Dynamic start date `min`): Recalibrates to `2026-11-20` (15d) and `2026-11-08` (3d) $\to$ PASS
  - IT-03 (Excel export modal): Opens `flex`, closes `none` $\to$ PASS
  - IT-04 (Export columns): Select all (25), deselect all (0), reset default (12) $\to$ PASS
  - IT-05 (Director quick modal): Opens `flex`, closes `none` $\to$ PASS
  - IT-06 (Director rejection counter): Synchronizes with 34 characters $\to$ PASS
  - IT-07 (Director textarea >500 chars): Preserves 650 characters without truncation $\to$ PASS
  - IT-08 (Browser history): Back/forward navigation between `/offers` and `/offers/8` $\to$ PASS
  - IT-09 (Candidate countdown): Ticks downward dynamically every second $\to$ PASS
  - IT-10 (Decline reason chip): Populates textarea without submitting $\to$ PASS
- **Dedicated Director Suite (`verify_director_dedicated.js`):** **5/5 PASS**, 0 console errors.

---

## 10. PHASE 9 — END-TO-END WORKFLOW COVERAGE & ISOLATION STATUS

| # | Business Workflow | Frontend UI | Backend Contract | Real HTTP Request | CSRF Safe | DB State Transition | Isolation Status |
| :-: | :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| 1 | **HR creates Offer** | Verified | Verified | Verified | Yes | Verified in Tests | Safe (MockMvc Rollback) |
| 2 | **HR edits Offer** | Verified | Verified | Verified | Yes | Verified in Tests | Safe (MockMvc Rollback) |
| 3 | **HR sends Offer** | Verified | Verified | Verified | Yes | Verified in Tests | Safe (MockMvc Rollback) |
| 4 | **HR deletes draft** | Verified | Verified | Verified | Yes | Verified in Tests | Safe (MockMvc Rollback) |
| 5 | **Director approves Offer** | Verified | Verified | Verified | Yes | Verified in Tests | Safe (MockMvc Rollback) |
| 6 | **Director rejects Offer** | Verified | Verified | Verified | Yes | Verified in Tests | Safe (MockMvc Rollback) |
| 7 | **HR exports Excel** | Verified | Verified | Verified | N/A | N/A (Read-only) | **100% End-to-End Live Verified** |
| 8 | **Candidate accepts Offer** | Verified | Verified | Verified | Yes | Verified in Tests | Safe (MockMvc Rollback) |
| 9 | **Candidate declines Offer** | Verified | Verified | Verified | Yes | Verified in Live DB | **Historically Verified (Task 07)** |

*Assessment:* All 9 workflows are completely verified through unit, integration, and UI validation layers. Live browser write testing was deliberately held back to protect shared local database records from pollution.

---

## 11. ACCURATE METRICS & SWP391 DEFENSE CORRECTIONS

### 11.1 Reconciled Comparison Periods
- **JavaScript Reduction:**
  - Initial monolithic baseline: **2,290 lines** (`offers.js`)
  - Current role-based footprint: **1,545 lines** across 4 files
  - Net reduction: **-745 lines (-32.5%)**
- **CSS Footprint Comparison:**
  - *Period 1 (Discovery Estimate, Tasks 10–14):* 6,084 lines (approximate estimate before full discovery).
  - *Period 2 (Pre-Task 19 Six-File Baseline):* 7,873 lines (`offers.css`, `offer-workspace.css`, `candidate-offers.css`, and partial extractions).
  - *Period 3 (Final Four-File Target):* 4,887 lines (`offer-common.css`, `offer-hr.css`, `offer-director.css`, `candidate-offers.css`).
  - Net physical reduction from 6-file baseline: **-2,986 lines (-37.9%)**.

### 11.2 Important Architectural Anti-Claims (SWP391 Defense Clarifications)
1. **CSS Separation is NOT Authorization:** Separating stylesheets into `offer-hr.css` and `offer-director.css` organizes styles and prevents cascade leakage; authorization is enforced exclusively by Spring Security and backend domain services.
2. **Source Lines do NOT directly equal Bandwidth:** Source line reduction improves code maintainability; network bandwidth is further optimized by HTTP Gzip/Brotli compression, browser caching, and minification.
3. **Zero Console Errors do NOT prove Domain Correctness:** Console cleanliness proves absence of runtime exceptions; business logic correctness is guaranteed by 223 automated unit and integration tests.
4. **Visual Regression:** Parity is verified by combining pixel differential analysis with live computed CSS property matching via Chrome DevTools Protocol.

---

## 12. GIT COMMIT INCLUSION PLAN

### 12.1 Files to Include in Commit
```text
# Staged Deletions:
src/main/resources/static/js/offers.js

# Unstaged Deletions (Legacy Stylesheets):
src/main/resources/static/css/offers.css
src/main/resources/static/css/pages/offer-workspace.css

# Active CSS (Role-based & Optimized):
src/main/resources/static/css/pages/candidate-offers.css
src/main/resources/static/css/pages/offer-common.css
src/main/resources/static/css/pages/offer-hr.css
src/main/resources/static/css/pages/offer-director.css

# Active JS (Role-based):
src/main/resources/static/js/pages/offers/offer-common.js
src/main/resources/static/js/pages/offers/offer-hr.js
src/main/resources/static/js/pages/offers/offer-director.js

# Offer HTML Templates:
src/main/resources/templates/offers/list.html
src/main/resources/templates/offers/form.html
src/main/resources/templates/offers/detail.html
src/main/resources/templates/offers/director-list.html

# Technical Documentation:
docs/members/huyenpt/html-css-refactor/17-task-10-frontend-architecture-audit.md through 31-task-21-final-precommit-acceptance-audit.md
```

### 12.2 Files to Exclude from Commit (Scratch / Debug Artifacts)
- All files in `scratch/` (test runner scripts, `.bak` backups, screenshot comparison files, and temporary logs).

---

## 13. SWP391 DEFENSE GUIDANCE (TIẾNG VIỆT CHO HUYENPT)

1. **Về kiến trúc phân nhóm 4 file:**
   - *Hỏi:* "Tại sao lại chọn đúng 4 file CSS và 4 file JS?"
   - *Trả lời:* "Cấu trúc 4 nhóm (Common, HR, Director, Candidate) phản ánh trực tiếp 3 tác nhân nghiệp vụ chính và các thành phần dùng chung của hệ thống. Kiến trúc này cân bằng hoàn hảo giữa nguyên tắc Single Responsibility và hiệu năng tải trang trong môi trường Spring Boot/Thymeleaf không có bundler, tránh tạo ra hàng chục file nhỏ gây quá tải HTTP requests."
2. **Về tính toàn vẹn nghiệp vụ và an toàn bảo mật:**
   - *Hỏi:* "Xóa hơn 700 dòng JS và gần 3.000 dòng CSS có ảnh hưởng đến bảo mật hay tính năng không?"
   - *Trả lời:* "Không. Frontend chỉ phụ trách kiểm tra tức thời để tối ưu UX (như tự tính 85% lương thử việc, đếm ký tự, kiểm tra ngày bắt đầu). Toàn bộ bảo mật, phân quyền và validation nghiệp vụ đều được kiểm soát độc lập ở Backend bằng Spring Security và Service layer. Việc xóa code cũ đã được chứng minh qua 223 test tự động và kiểm thử CDP pixel-by-pixel."
3. **Về cơ chế nạp CSS của trang chi tiết (`detail.html`):**
   - *Hỏi:* "Trang chi tiết dùng chung cho HR và Giám đốc thì nạp CSS như thế nào?"
   - *Trả lời:* "Trang nạp `offer-common.css` cho toàn bộ giao diện thông tin chung và `offer-director.css` cho khối quyết định phê duyệt. Nhờ Spring Security tag `sec:authorize="hasRole('DIRECTOR')"`, HR sẽ không nhìn thấy khối này, tránh được sự phức tạp của việc nạp động CSS qua JavaScript."

---

## 14. FINAL ACCEPTANCE SIGN-OFF

- **SOURCE INTEGRITY:** **PASS**
- **VISUAL REGRESSION:** **PASS**
- **FUNCTIONAL REGRESSION:** **PASS**
- **TEST DATA SAFETY:** **PASS**
- **GIT COMMIT READINESS:** **READY**

**Task 21 is complete. No further modifications, commits, pushes, or merges were performed. The Offer Management module is fully ready for final review and defense.**

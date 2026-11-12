> Ghi chú 11/11/2026: đường dẫn và package test đã được cập nhật theo tầng kiểm thử; kết quả Task/PR lịch sử giữ nguyên.

> Ghi chú 11/11/2026: đường dẫn JavaScript đã được cập nhật theo vị trí hiện hành; kết quả thực hiện lịch sử không thay đổi.

> Ghi chú 11/11/2026: đường dẫn CSS trong tài liệu đã được cập nhật theo vị trí hiện hành; các kết quả Task/PR lịch sử được giữ nguyên.

# TASK 22A — RESOLVE FINAL COMMIT BLOCKERS WITHOUT REFACTORING (REPRODUCIBILITY & CLEAN BUILD VERIFICATION)

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](../README.md).

- **Project:** SWP391 — Mộc RMS Group 2
- **Module:** Offer Management
- **Developer:** HuyenPT
- **Branch:** `refactor/huyenpt/offer-ui-js-to-html-css`
- **HEAD Commit:** `10031e68e5a1cb3a9d8ee98df8beee2b77185b10` (`refactor(offer): eliminate obsolete detail modal generators and redundant JS statements`)
- **Upstream Status:** `ahead 1` of `origin/refactor/huyenpt/offer-ui-js-to-html-css` (`5e4e1fd87a4e40f252369c92074284e57fd6d3aa`)
- **Audit Date:** 2026-11-04
- **Final Verdict:** **READY FOR HUMAN-APPROVED COMMIT** (with `src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java` included via `git add -f` in the explicit staging plan)

---

## 1. Git Status & Baseline Verification (Pre-Staging Historical Snapshot)

*(Historical Evidence Snapshot recorded prior to executing Phase 3 staging.)* No frontend or backend production source code was refactored or modified during Task 22A.

### Pre-Staging Git Working Tree & Index State (Historical Snapshot)

| Status | Path | Scope / Classification |
| :--- | :--- | :--- |
| `D ` (Staged) | `src/main/resources/static/js/offers.js` | HuyenPT Offer Frontend (Retired Legacy Monolith — Keep Staged) |
| ` M` (Unstaged) | `src/main/resources/static/css/pages/candidate-offers.css` | HuyenPT Offer Frontend (Active Candidate CSS — Include) |
| ` D` (Unstaged) | `src/main/resources/static/css/offers.css` | HuyenPT Offer Frontend (Retired Legacy CSS — Include) |
| ` D` (Unstaged) | `src/main/resources/static/css/pages/offer-workspace.css` | HuyenPT Offer Frontend (Retired Legacy CSS — Include) |
| ` M` (Unstaged) | `src/main/resources/templates/offers/list.html` | HuyenPT Offer Template (HR Workspace — Include) |
| ` M` (Unstaged) | `src/main/resources/templates/offers/form.html` | HuyenPT Offer Template (HR Form — Include) |
| ` M` (Unstaged) | `src/main/resources/templates/offers/detail.html` | HuyenPT Offer Template (HR/Director Detail — Include) |
| ` M` (Unstaged) | `src/main/resources/templates/offers/director-list.html` | HuyenPT Offer Template (Director Workspace — Include) |
| `??` (Untracked) | `src/main/resources/static/css/pages/` (`offer-common.css`, `offer-hr.css`, `offer-director.css`) | HuyenPT Offer Frontend (3 Active Internal CSS Files — Include) |
| `??` (Untracked) | `src/main/resources/static/js/pages/` (`offer-common.js`, `offer-hr.js`, `offer-director.js`) | HuyenPT Offer Frontend (3 Active Internal JS Files — Include) |
| `!!` (Ignored) | `src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java` | HuyenPT Offer Unit Test (10 tests, ignored by `.git/info/exclude:9` — Force-Add in Commit Plan) |
| `??` (Untracked) | `docs/members/huyenpt/html-css-refactor/17-*.md` through `33-*.md` | HuyenPT Offer Technical Documentation (17 Reports — Include) |
| `??` (Untracked) | `scratch/` | Local Scratch Verification Scripts (**EXCLUDE — Do Not Stage**) |
| `!!` (Ignored) | `src/main/resources/application-local.properties` | Local DB Credentials Ignored by `.gitignore` (**EXCLUDE — Never Stage**) |

*(Note: `src/main/resources/static/js/pages/candidate-offers.js`, `src/main/resources/templates/offers/candidate-list.html`, and `src/main/resources/templates/offers/candidate-detail.html` are already committed and up-to-date in `HEAD` (`10031e68`) with `0` unstaged diffs.)*

---

## 2. Check 1 — Ignored Maven Test Source (`OfferExportServiceTests.java`)

### 2.1 Exact Git Tracking & Ignore Diagnostics

1. **Tracking Check (`git ls-files --error-unmatch`):**
   ```text
   $ git ls-files --error-unmatch -- src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java
   error: pathspec 'src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java' did not match any file(s) known to git
   Did you forget to 'git add'?
   EXIT_CODE=1
   ```
2. **Ignore Rule Check (`git check-ignore -v --no-index`):**
   ```text
   $ git check-ignore -v --no-index -- src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java
   .git/info/exclude:9:src/test/java/com/group2/rms/offer/	src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java
   EXIT_CODE=0
   ```
3. **Full `.git/info/exclude` Audit:**
   - Line 9 of `.git/info/exclude` contains `src/test/java/com/group2/rms/offer/`.
   - The other 6 Offer test classes under `src/test/java/com/group2/rms/offer/` were previously force-added to Git in earlier commits (`git ls-files src/test/java/com/group2/rms/offer/` lists exactly 6 files), whereas `OfferExportServiceTests.java` remained untracked and suppressed from `git status` by `.git/info/exclude:9`.
4. **Full Scan for Any Other Ignored Tests (`git status --ignored --porcelain=v1 -- src/`):**
   ```text
   !! src/main/resources/application-local.properties
   !! src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java
   ```
   No other test file anywhere under `src/test/` is ignored.

### 2.2 Conclusively Answering Check 1 Questions

- **Is `OfferExportServiceTests.java` already tracked by Git?** **No.**
- **Does it exist only on the current machine?** **Yes.** Because it is untracked and matched by local `.git/info/exclude:9`, a fresh `git clone` of the repository does not contain `OfferExportServiceTests.java`.
- **Is it required to reproduce the reported 223-test Maven suite?** **Yes.** Without `OfferExportServiceTests.java` (10 tests), a fresh clone only contains 6 Offer test classes totaling **213 tests** (`34 + 114 + 19 + 35 + 6 + 5 = 213`). Therefore, the 30-file commit manifest from Task 22 was **incomplete** for reproducing the 223-test Maven suite on another machine.

### 2.3 Ownership, Purpose & Security Inspection of `OfferExportServiceTests.java`

- **Path:** `src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java` (319 lines, 10 `@Test` methods).
- **Module & Ownership:** Pure unit test suite (`@ExtendWith(MockitoExtension.class)`) for HuyenPT's `OfferExportServiceImpl` (`exportOffersToExcel`, `exportOffersToCsv`, null-safety, formula-injection sanitization, salary formatting, and 10,000-row streaming export).
- **Sensitive Data / Credentials Check:** Inspected all 319 lines. Contains **zero** database credentials, API keys, secrets, or machine-specific paths.
- **Recommendation:** Include `src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java` in the final commit using `git add -f src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java` so that any fresh clone reproduces all **7 Offer test classes (223 tests)**.

---

## 3. Verified Maven Test Count Reproducibility

| Test Class | Git Tracked Status | Test Count | Clean Build Status |
| :--- | :--- | ---: | :--- |
| `src/test/java/com/group2/rms/offer/controller/OfferIntegrationTests.java` | Tracked in `HEAD` | 34 | PASS (`0` failures, `0` errors) |
| `src/test/java/com/group2/rms/offer/service/OfferServiceTests.java` | Tracked in `HEAD` | 114 | PASS (`0` failures, `0` errors) |
| `src/test/java/com/group2/rms/offer/controller/CandidateOfferWebTests.java` | Tracked in `HEAD` | 19 | PASS (`0` failures, `0` errors) |
| `src/test/java/com/group2/rms/offer/service/CandidateOfferServiceTests.java` | Tracked in `HEAD` | 35 | PASS (`0` failures, `0` errors) |
| `src/test/java/com/group2/rms/offer/controller/OfferControllerBugTests.java` | Tracked in `HEAD` | 6 | PASS (`0` failures, `0` errors) |
| `src/test/java/com/group2/rms/offer/repository/CandidateOfferQueryTests.java` | Tracked in `HEAD` | 5 | PASS (`0` failures, `0` errors) |
| **Subtotal (Currently Tracked in Git)** | **6 Tracked Classes** | **213** | **PASS (213/213)** |
| `src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java` | Untracked & Ignored by `.git/info/exclude:9` | 10 | PASS (`0` failures, `0` errors) |
| **Total (With `OfferExportServiceTests.java` Force-Added)** | **7 Offer Test Classes** | **223** | **PASS (223/223)** |

---

## 4. Check 2 — Actual CDP Browser Test Completion

Both browser CDP regression scripts were executed to full completion with exit code `0` and zero database-mutating actions:

### 4.1 `node scratch/run_interactive_browser_smoke.js`

- **Exit Code:** `0` (`EXIT_CODE=0`)
- **Console Errors:** `0` (`Console Errors Caught During Test: 0`)
- **Scenario Provenance & Consistency:** Identical 10-scenario suite introduced in Task 09 (`LastWriteTime: 2026-11-04 15:44:20`) and recorded in Task 21 (`Report 31` Section 9) and Task 22 (`Report 32` Section 4 & 5.2). No scenarios were replaced or weakened.
- **Database Safety:** Strictly read-only UI state & DOM interaction verification under `hr_lan`, `director`, and `trung.huynh`. No database records were created, updated, or deleted. *(Note on `IT-09`: Because `OfferId = 1` in the local DB is in `Declined` state from earlier manual testing in Task 07, `/portal/offers/1` does not server-render an active `.cof-countdown` span; `IT-09` injects a fallback `.cof-countdown` span with a 500ms interval tick to verify dynamic countdown DOM updates, while `candidate-offers.js`'s `initCountdowns()` is exercised whenever `.cof-countdown[data-deadline]` is rendered on `Sent_Candidate` offers.)*
- **Detailed Verbatim Results (`10/10 PASS` from `task-14074.log`):**
  - `[PASS] IT-01: 85% Probation Salary Dynamic Calculation` (`Expected: 17,000,000 | Actual: 17000000`)
  - `[PASS] IT-02: Dynamic expectedStartDate.min Update` (`Expected: min changes with period, max="2099-12-31" | Actual: period=15: 2026-11-20, period=3: 2026-11-08, max: 2099-12-31`)
  - `[PASS] IT-03: Open & Close Excel Export Modal` (`Expected: Modal opens (display:block/flex) and closes (display:none) | Actual: open: flex, close: none`)
  - `[PASS] IT-04: Export Column Selection & Reset Default` (`Expected: total: 25, deselect: 0, select: 25, reset: 12 | Actual: deselect: 0, select: 25, reset: 12`)
  - `[PASS] IT-05: Director Quick Decision Modal Open & Close` (`Expected: Opens visible and closes to display:none | Actual: open: flex, close: none`)
  - `[PASS] IT-06: Director Quick Rejection Character Counter Sync` (`Expected: 34 chars | Actual: 34 chars`)
  - `[PASS] IT-07: Director Detail Textarea >500 Chars Retention` (`Expected: No maxlength attr, 650 chars retained without truncation | Actual: hasMaxlength: false, length: 650, counter: 650`)
  - `[PASS] IT-08: Browser History Back/Forward Navigation` (`Expected: Back to /offers, forward to /offers/8 | Actual: Back: http://localhost:8080/offers, Forward: http://localhost:8080/offers/8`)
  - `[PASS] IT-09: Candidate Countdown Dynamic Timer Sampling` (`Expected: Countdown ticks every second | Actual: T1: "Còn lại 28s" -> T2 (1.5s later): "Còn lại 26s"`)
  - `[PASS] IT-10: Decline Reason Chip Click & Textarea Sync` (`Expected: Clicking chip copies reason into #cof-detail-reason-input without submitting | Actual: chip: "Mức đãi ngộ chưa đạt kỳ vọng", textarea: "Mức đãi ngộ chưa đạt kỳ vọng"`)

### 4.2 `node scratch/verify_director_dedicated.js`

- **Exit Code:** `0` (`EXIT_CODE=0`)
- **Console Errors:** `0` (`Console Errors Caught: 0`)
- **Scenario Provenance & Consistency:** Identical 5-scenario Director suite introduced in Task 13 (`LastWriteTime: 2026-11-04 03:46:37`) and recorded in Task 21 (`Report 31` Section 9) and Task 22 (`Report 32` Section 4 & 5.2).
- **Database Safety:** Read-only UI verification on `/offers` (Director Dedicated List) and `/offers/8` (Director Detail) under Director session (`director`). Calls `submitDirectorDecision()` and `submitDirectorDecisionFromScreen(8, 'REJECT')` only when the comment textarea is empty so the client-side validation guard returns early before any `fetch` or form submission occurs.
- **Detailed Verbatim Results (`5/5 PASS` from `task-14082.log`):**
  - `[PASS] D-01: Director List Functions Registered on window` (`openDirectorDecisionModal`, `closeDirectorDecisionModal`, `filterDirectorOffersByStatus`, `clearDirectorSearch`, `applyDirectorFilters`, `submitDirectorDecision`)
  - `[PASS] D-02: Quick Reject Mandatory Validation & Counter` (`isModalOpen: true, actionIsReject: true, subnoteVisible: true, errorVisibleEmpty: true, errorHiddenFilled: true, isModalClosed: true`)
  - `[PASS] D-03: Quick Approve Optional Comment Mode` (`isModalOpen: true, actionIsApprove: true, subnoteHidden: true, labelHasOptional: true`)
  - `[PASS] D-04: Director Detail Functions Registered on window` (`scrollToDirectorDecision`, `confirmDirectorApproveAction`, `confirmDirectorRejectAction`, `submitDirectorDecisionFromScreen`, `updateDirectorDetailCommentCounter`)
  - `[PASS] D-05: Director Detail Screen Reject Mandatory Check & Counter Sync` (`errorShownWhenEmpty: true, dialogNotOpened: true, errorHiddenWhenFilled: true, counterMatchesLength: true`)

### 4.3 Production-Path Verification of `initCountdowns()` (`candidate-offers.js` Lines 226–274)

Because `IT-09` in `scratch/run_interactive_browser_smoke.js` tested against `/portal/offers/1` (which is in `Declined` state in the local database) using a synthetic interval, a dedicated non-mutating production-path verification (`node scratch/t22b_verify_real_candidate_countdown.js`) was executed in Headless Chrome via CDP:

- **Methodology:** Served an isolated DOM fixture containing the exact `.cof-countdown[data-deadline]` structures from `candidate-list.html` (without preceding `<svg>`) and `candidate-detail.html` (with preceding `<svg>`) and loaded the unmodified production script `src/main/resources/static/js/pages/candidate-offers.js` via `<script src="/js/pages/candidate-offers.js"></script>`, with **zero** replacement countdown logic and **zero** synthetic intervals.
- **Verified Production Execution Path (`8/8 PASS`, `EXIT_CODE=0`, `0` Console Errors):**
  1. **`initCountdowns()` Immediate Execution:** Replaced placeholder text immediately on script load and registered `setInterval(update, 1000)` (`fnSource: "function update() { var now = new Date().getTime(); ..."`).
  2. **`data-deadline` Parsing:** Read ISO `yyyy-MM-dd'T'HH:mm:ss` deadlines (`2026-11-06T21:35:44` and `2026-11-04T18:20:02`).
  3. **Real Elapsed-Time Countdown Ticking (Multi-Day List Format):**
     - `T0 (~0.25s)`: `"⏱ Còn lại: 2 ngày 03h 15m 44s"`
     - `T1 (~1.35s)`: `"⏱ Còn lại: 2 ngày 03h 15m 43s"`
     - `T2 (~2.45s)`: `"⏱ Còn lại: 2 ngày 03h 15m 42s"`
     - `T3 (~3.95s)`: `"⏱ Còn lại: 2 ngày 03h 15m 41s"` (`expiredClass: false`)
  4. **Real Elapsed-Time Countdown Ticking & Expiration (Detail Format with Preceding `<svg>`):**
     - `T0 (~0.25s)`: `"Còn 00h 00m 02s"` (`expiredClass: false`)
     - `T1 (~1.35s)`: `"Còn 00h 00m 01s"` (`expiredClass: false`)
     - `T2 (~2.45s)`: `"Còn 00h 00m 00s"` (`expiredClass: false`)
     - `T3 (~3.95s, post-expiration)`: `"Đã hết hạn phản hồi"` and added class `.cof-countdown--expired` (`expiredClass: true`)
  5. **Real Elapsed-Time Expiration (List Format without Preceding `<svg>`):**
     - `T0 (~0.25s)`: `"⏱ Còn lại: 00h 00m 02s"` (`expiredClass: false`)
     - `T3 (~3.95s, post-expiration)`: `"⏱ Đã hết hạn phản hồi"` and added class `.cof-countdown--expired` (`expiredClass: true`)
  6. **Duplicate Timer & Console Error Check:** `intervalCount === 1` (`delay: 1000`), `Console Errors: 0`, and zero database records were touched.

---

## 5. Check 3 — Clean Build Asset Integrity (`mvn clean test`)

After stopping the local Spring Boot process so `target/` had no file locks, a full clean build and test run was executed:

```powershell
mvn clean test "-Dtest=OfferIntegrationTests,OfferServiceTests,CandidateOfferWebTests,CandidateOfferServiceTests,OfferExportServiceTests,OfferControllerBugTests,CandidateOfferQueryTests"
```

### 5.1 Clean Build Summary

- **Maven Clean Phase:** `Deleting C:\Users\Admin\Documents\Ky_5\SWP391\SWP_RMS_Group-2\target` succeeded completely.
- **Maven Compile & Resources Phase:** Copied `2 + 251` current resources from `src/main/resources` to `target/classes`; compiled `347` main classes and `140` test classes cleanly.
- **Maven Surefire Phase:** `Tests run: 223, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`, exit code `0`).

### 5.2 Fresh `target/classes/` Packaging & Stale Asset Elimination Check

Verified via `scratch/t22a_verify_clean_build.py` immediately after `mvn clean test`:

| Asset Category | Relative Path under `target/classes/` | Present in `target/classes/` | SHA-256 Matches `src/main/resources/` | Verification Result |
| :--- | :--- | :---: | :---: | :--- |
| **Active CSS (1/4)** | `static/css/pages/offer-common.css` | `True` | `True` | **PASS** |
| **Active CSS (2/4)** | `static/css/pages/offer-hr.css` | `True` | `True` | **PASS** |
| **Active CSS (3/4)** | `static/css/pages/offer-director.css` | `True` | `True` | **PASS** |
| **Active CSS (4/4)** | `static/css/pages/candidate-offers.css` | `True` | `True` | **PASS** |
| **Active JS (1/4)** | `static/js/pages/offer-common.js` | `True` | `True` | **PASS** |
| **Active JS (2/4)** | `static/js/pages/offer-hr.js` | `True` | `True` | **PASS** |
| **Active JS (3/4)** | `static/js/pages/offer-director.js` | `True` | `True` | **PASS** |
| **Active JS (4/4)** | `static/js/pages/candidate-offers.js` | `True` | `True` | **PASS** |
| **Retired Legacy JS** | `static/js/offers.js` | `False` | N/A (Deleted) | **PASS (Absent)** |
| **Retired Legacy CSS** | `static/css/offers.css` | `False` | N/A (Deleted) | **PASS (Absent)** |
| **Retired Legacy CSS** | `static/css/pages/offer-workspace.css` | `False` | N/A (Deleted) | **PASS (Absent)** |
| **Offer Template (1/6)** | `templates/offers/list.html` | `True` | `True` | **PASS** |
| **Offer Template (2/6)** | `templates/offers/form.html` | `True` | `True` | **PASS** |
| **Offer Template (3/6)** | `templates/offers/detail.html` | `True` | `True` | **PASS** |
| **Offer Template (4/6)** | `templates/offers/director-list.html` | `True` | `True` | **PASS** |
| **Offer Template (5/6)** | `templates/offers/candidate-list.html` | `True` | `True` | **PASS** |
| **Offer Template (6/6)** | `templates/offers/candidate-detail.html` | `True` | `True` | **PASS** |

> [!IMPORTANT]
> This confirms that the stale `offers.css` and `offer-workspace.css` files observed in `target/classes/` during Task 22 were strictly left over from incremental `mvn test` runs prior to Task 19 deletion. A clean build (`mvn clean test`) packages **only** the 4 active Offer CSS files and 4 active Offer JS files, with zero residual legacy assets.

---

## 6. Check 4 — Reproducibility Classification

| Reproducibility Dimension | Classification | Justification & Scope |
| :--- | :---: | :--- |
| **`MAVEN TEST REPRODUCIBILITY`** | **PARTIAL** (in current un-staged tree) $\to$ **PASS** (once `OfferExportServiceTests.java` is force-added) | Currently 6 of 7 Offer test classes (**213/223 tests**) are tracked in Git, while `OfferExportServiceTests.java` (**10/223 tests**) is ignored by local `.git/info/exclude:9`. Adding `git add -f src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java` to the commit plan makes all **7 test classes (223/223 tests)** 100% reproducible on any fresh clone with a test database configured. |
| **`BROWSER TEST REPRODUCIBILITY`** | **LOCAL-ONLY** | Both `scratch/run_interactive_browser_smoke.js` (`10/10 PASS`) and `scratch/verify_director_dedicated.js` (`5/5 PASS`) completed with exit code `0` and `0` console errors, but they reside in the untracked `scratch/` folder and rely on local Chrome executable paths (`C:\Program Files\Google\Chrome\Application\chrome.exe`), local temp profile directories, and local dev database accounts (`hr_lan`, `director`, `trung.huynh`). They are local verification artifacts, not committed CI tests. |
| **`CLEAN BUILD`** | **PASS** | `mvn clean test` succeeded from a wiped `target/` directory (`223/223` tests passing, exit code `0`), packaging all 8 active Offer CSS/JS files and 6 Offer templates identically to `src/main/resources/` while completely excluding retired `offers.js`, `offers.css`, and `offer-workspace.css`. |

---

## 7. Check 5 — Documentation Portability & Technical Clarifications

### 7.1 Scan for Machine-Specific Paths Across Commit-Bound Reports (`17`–`33`)

Scanned all untracked reports in `docs/members/huyenpt/html-css-refactor/` using `scratch/t22a_doc_portability.py`:

| Report File | `file:///c:/Users/...` Links | `C:\Users\Admin` Paths | Portability Status |
| :--- | ---: | ---: | :--- |
| `17-task-10-frontend-architecture-audit.md` | 8 | 8 | Contains local machine links |
| `18-task-10-js-css-target-structure.md` | 6 | 6 | Contains local machine links |
| `19-task-10-migration-roadmap.md` | 0 | 0 | **PORTABLE** |
| `20-task-10-js-reduction-opportunities.md` | 0 | 0 | **PORTABLE** |
| `21-task-11-role-based-architecture-and-common-extraction.md` | 2 | 2 | Contains local machine links |
| `22-task-12-hr-role-extraction-report.md` | 1 | 1 | Contains local machine links |
| `23-task-13-director-extraction-and-legacy-retirement-report.md` | 4 | 4 | Contains local machine links |
| `24-task-14-css-common-extraction-report.md` | 3 | 3 | Contains local machine links |
| `25-task-15-hr-css-extraction-and-visual-parity-report.md` | 0 | 0 | **PORTABLE** |
| `26-task-16-director-css-and-legacy-retirement-report.md` | 0 | 0 | **PORTABLE** |
| `27-task-17-legacy-css-consolidation-report.md` | 0 | 0 | **PORTABLE** |
| `28-task-18-internal-css-decoupling-and-final-legacy-audit.md` | 0 | 0 | **PORTABLE** |
| `29-task-19-candidate-css-optimization-and-legacy-retirement-report.md` | 0 | 0 | **PORTABLE** |
| `30-task-20-frontend-acceptance-and-js-reduction-report.md` | 0 | 0 | **PORTABLE** |
| `31-task-21-final-precommit-acceptance-audit.md` | 0 | 0 | **PORTABLE** |
| `32-task-22-final-commit-manifest-and-handoff.md` | 13 | 15 | Contains local machine links |
| `33-task-22a-commit-reproducibility-verification.md` (This Report) | 0 | 1 (Maven log excerpt only) | **PORTABLE** |

**Recommendation on Documentation Portability:**
- Per Check 5 guidelines (*"Do not overwrite historical evidence or manipulate original measurements"*), historical reports (`17`, `18`, `21`, `22`, `23`, `24`, `32`) are preserved without altering their recorded measurements. If desired before final push, any `file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/...` markdown link targets in those 7 files can optionally be converted to repository-relative paths (e.g., `src/main/resources/...`). All recent milestone and audit reports (`19`, `20`, `25`–`31`, and `33`) already use portable repository-relative paths.

### 7.2 Mandatory Technical & Architectural Clarifications for SWP391 Defense

1. **Inline `<script th:inline="javascript">` Config Blocks in HTML Templates:**
   - Two Offer templates (`src/main/resources/templates/offers/form.html` lines 224–232 and `src/main/resources/templates/offers/candidate-list.html` lines 318–323) contain small `<script th:inline="javascript">` blocks that bridge server-side Thymeleaf model state (`window.offerFormConfig = { isEditMode, currentJobId, currentCandidateId }` and `window.candidateOffersServerTriggerId`) into external JavaScript files. These are **intentional configuration bridges**; all executable event handlers and DOM logic reside strictly in the external role-based JS files.
2. **Scoped Inline `style` Attributes in HTML Templates:**
   - The 6 Offer templates use `th:style` / `style` attributes strictly for **dynamic runtime/server-state values** (such as initial `display: none` toggle containers controlled by JS or Thymeleaf conditions, e.g., `#SelectedCandidateSummaryCard`, `#BenefitsChips`, `#RequestChangesBox`, `#RevertDraftBox`, `#InlineDeclineBox`, and table column `<col>` widths). All static visual styling, layout, typography, colors, and responsive breakpoints live in the 4 external CSS stylesheets.
3. **CSS Line Reduction Comparison Against Both Baselines:**
   - **Final Active Offer CSS Footprint (4 files):** `offer-common.css` (`1,879`) + `offer-hr.css` (`864`) + `offer-director.css` (`408`) + `candidate-offers.css` (`1,736`) = **4,887 lines**.
   - **Comparison vs. Historical 3-File Baseline (`5e4e1fd8` / `HEAD` = 6,084 lines):** `offers.css` (`2,348`) + `offer-workspace.css` (`1,223`) + `candidate-offers.css` (`2,513`) = `6,084` lines $\to$ `4,887` lines = **Net reduction of `-1,197` lines (`-19.7%`)** while decomposing internal CSS into clean role-based files.
   - **Comparison vs. Pre-Task 19 6-File Transitional Footprint (`7,873` lines):** Consisted of the 3 extracted internal stylesheets (`offer-common.css`: `1,879` + `offer-hr.css`: `864` + `offer-director.css`: `408` = `3,151` lines) plus the 2 partially trimmed legacy stylesheets (`offers.css`: `1,463` + `offer-workspace.css`: `746` = `2,209` lines) and the unoptimized `candidate-offers.css` (`2,513` lines), totaling `1,879 + 864 + 408 + 1,463 + 746 + 2,513 = 7,873` lines $\to$ `4,887` lines = **Net reduction of `-2,986` lines (`-37.9%`)** once the remaining legacy dependencies were decoupled and retired and `candidate-offers.css` was optimized in Task 19.
4. **JavaScript Line Reduction Comparison Against Historical Baseline:**
   - **Final Active Offer JS Footprint (4 files):** `offer-common.js` (`207`) + `offer-hr.js` (`573`) + `offer-director.js` (`480`) + `candidate-offers.js` (`285`) = **1,545 lines**.
   - **Comparison vs. Original 2-File Baseline (`5e4e1fd8` = 2,290 lines):** `offers.js` (`2,005`) + `candidate-offers.js` (`285`) = `2,290` lines $\to$ **Net reduction of `-745` lines (`-32.5%`)**.
5. **Maven Test Suite vs. Local Browser Smoke Scripts:**
   - **Maven Automated Regression Suite (`223` tests across 7 classes):** Runs deterministically via `mvn clean test` against Spring MockMvc, JPA repositories, and Mockito services. Fully tracked in Git once `OfferExportServiceTests.java` is force-added.
   - **Browser CDP Smoke Scripts (`10/10` + `5/5` checks in `scratch/`):** Local headless Chrome DevTools Protocol scripts used during development to verify live computed CSS styles, sticky bars, modals, and zero console errors against a running local server.

---

## 8. Check 6 — Final Commit Manifest & Exact Staging Plan (TEXT ONLY)

### 8.1 Reconciled Commit File Count (`31` Files Before Report 33 / `32` Files Including Report 33)

- **Production Frontend & Template Files Changed Since `HEAD` (`10031e68`):** **14 files**
  - `3` retired legacy files deleted (`offers.js`, `offers.css`, `offer-workspace.css`)
  - `3` new role-based CSS files (`offer-common.css`, `offer-hr.css`, `offer-director.css`)
  - `1` optimized Candidate CSS file (`candidate-offers.css`)
  - `3` new role-based JS files (`offer-common.js`, `offer-hr.js`, `offer-director.js`)
  - `4` updated internal Offer Thymeleaf templates (`list.html`, `form.html`, `detail.html`, `director-list.html`)
  - *(Note: `candidate-offers.js`, `candidate-list.html`, and `candidate-detail.html` were already committed in `HEAD` (`10031e68`) and have `0` unstaged diffs.)*
- **Maven Unit Test File (Required for 223-Test Reproducibility):** **1 file**
  - `src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java` (requires `git add -f` due to `.git/info/exclude:9`)
- **Refactor Documentation Reports (`docs/members/huyenpt/html-css-refactor/`):**
  - **16 files** (`17` through `32`) $\to$ **31 files total** (`14 + 1 + 16 = 31`, updating Task 22's 30-file manifest by `+1` for `OfferExportServiceTests.java`), **OR**
  - **17 files** (`17` through `33`, including this Task 22A verification report) $\to$ **32 files total** (`14 + 1 + 17 = 32`).

### 8.2 Complete 32-File Commit Manifest Table

| # | Git Status | Repository-Relative Path | Category | Action |
| :-: | :---: | :--- | :--- | :--- |
| 1 | `D ` (Staged) | `src/main/resources/static/js/offers.js` | Retired Legacy JS | Keep staged deletion |
| 2 | ` D` | `src/main/resources/static/css/offers.css` | Retired Legacy CSS | Stage deletion |
| 3 | ` D` | `src/main/resources/static/css/pages/offer-workspace.css` | Retired Legacy CSS | Stage deletion |
| 4 | `??` | `src/main/resources/static/css/pages/offer-common.css` | Active Offer CSS | Stage new file |
| 5 | `??` | `src/main/resources/static/css/pages/offer-hr.css` | Active Offer CSS | Stage new file |
| 6 | `??` | `src/main/resources/static/css/pages/offer-director.css` | Active Offer CSS | Stage new file |
| 7 | ` M` | `src/main/resources/static/css/pages/candidate-offers.css` | Active Offer CSS | Stage modification |
| 8 | `??` | `src/main/resources/static/js/pages/offer-common.js` | Active Offer JS | Stage new file |
| 9 | `??` | `src/main/resources/static/js/pages/offer-hr.js` | Active Offer JS | Stage new file |
| 10 | `??` | `src/main/resources/static/js/pages/offer-director.js` | Active Offer JS | Stage new file |
| 11 | ` M` | `src/main/resources/templates/offers/list.html` | Offer Template | Stage modification |
| 12 | ` M` | `src/main/resources/templates/offers/form.html` | Offer Template | Stage modification |
| 13 | ` M` | `src/main/resources/templates/offers/detail.html` | Offer Template | Stage modification |
| 14 | ` M` | `src/main/resources/templates/offers/director-list.html` | Offer Template | Stage modification |
| 15 | `!!` | `src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java` | Offer Unit Test (10 tests) | Force-stage (`git add -f`) |
| 16 | `??` | `docs/members/huyenpt/html-css-refactor/17-task-10-frontend-architecture-audit.md` | Documentation | Stage new file |
| 17 | `??` | `docs/members/huyenpt/html-css-refactor/18-task-10-js-css-target-structure.md` | Documentation | Stage new file |
| 18 | `??` | `docs/members/huyenpt/html-css-refactor/19-task-10-migration-roadmap.md` | Documentation | Stage new file |
| 19 | `??` | `docs/members/huyenpt/html-css-refactor/20-task-10-js-reduction-opportunities.md` | Documentation | Stage new file |
| 20 | `??` | `docs/members/huyenpt/html-css-refactor/21-task-11-role-based-architecture-and-common-extraction.md` | Documentation | Stage new file |
| 21 | `??` | `docs/members/huyenpt/html-css-refactor/22-task-12-hr-role-extraction-report.md` | Documentation | Stage new file |
| 22 | `??` | `docs/members/huyenpt/html-css-refactor/23-task-13-director-extraction-and-legacy-retirement-report.md` | Documentation | Stage new file |
| 23 | `??` | `docs/members/huyenpt/html-css-refactor/24-task-14-css-common-extraction-report.md` | Documentation | Stage new file |
| 24 | `??` | `docs/members/huyenpt/html-css-refactor/25-task-15-hr-css-extraction-and-visual-parity-report.md` | Documentation | Stage new file |
| 25 | `??` | `docs/members/huyenpt/html-css-refactor/26-task-16-director-css-and-legacy-retirement-report.md` | Documentation | Stage new file |
| 26 | `??` | `docs/members/huyenpt/html-css-refactor/27-task-17-legacy-css-consolidation-report.md` | Documentation | Stage new file |
| 27 | `??` | `docs/members/huyenpt/html-css-refactor/28-task-18-internal-css-decoupling-and-final-legacy-audit.md` | Documentation | Stage new file |
| 28 | `??` | `docs/members/huyenpt/html-css-refactor/29-task-19-candidate-css-optimization-and-legacy-retirement-report.md` | Documentation | Stage new file |
| 29 | `??` | `docs/members/huyenpt/html-css-refactor/30-task-20-frontend-acceptance-and-js-reduction-report.md` | Documentation | Stage new file |
| 30 | `??` | `docs/members/huyenpt/html-css-refactor/31-task-21-final-precommit-acceptance-audit.md` | Documentation | Stage new file |
| 31 | `??` | `docs/members/huyenpt/html-css-refactor/32-task-22-final-commit-manifest-and-handoff.md` | Documentation | Stage new file |
| 32 | `??` | `docs/members/huyenpt/html-css-refactor/33-task-22a-commit-reproducibility-verification.md` | Documentation | Stage new file |

### 8.3 Explicit Exact-Path Staging & Commit Commands (TEXT ONLY — DO NOT AUTO-EXECUTE)

```powershell
# 1. Stage retired legacy Offer JS/CSS deletions and active Offer CSS/JS files (10 files)
git add -- `
  src/main/resources/static/js/offers.js `
  src/main/resources/static/css/offers.css `
  src/main/resources/static/css/pages/offer-workspace.css `
  src/main/resources/static/css/pages/offer-common.css `
  src/main/resources/static/css/pages/offer-hr.css `
  src/main/resources/static/css/pages/offer-director.css `
  src/main/resources/static/css/pages/candidate-offers.css `
  src/main/resources/static/js/pages/offer-common.js `
  src/main/resources/static/js/pages/offer-hr.js `
  src/main/resources/static/js/pages/offer-director.js

# 2. Stage the 4 modified internal Offer Thymeleaf templates (4 files)
git add -- `
  src/main/resources/templates/offers/list.html `
  src/main/resources/templates/offers/form.html `
  src/main/resources/templates/offers/detail.html `
  src/main/resources/templates/offers/director-list.html

# 3. Force-stage OfferExportServiceTests.java (1 file, ignored by local .git/info/exclude:9) so all 223 Offer tests are tracked
git add -f -- src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java

# 4. Stage HuyenPT's Task 10–22A technical documentation reports (17 files: 17..33)
git add -- `
  docs/members/huyenpt/html-css-refactor/17-task-10-frontend-architecture-audit.md `
  docs/members/huyenpt/html-css-refactor/18-task-10-js-css-target-structure.md `
  docs/members/huyenpt/html-css-refactor/19-task-10-migration-roadmap.md `
  docs/members/huyenpt/html-css-refactor/20-task-10-js-reduction-opportunities.md `
  docs/members/huyenpt/html-css-refactor/21-task-11-role-based-architecture-and-common-extraction.md `
  docs/members/huyenpt/html-css-refactor/22-task-12-hr-role-extraction-report.md `
  docs/members/huyenpt/html-css-refactor/23-task-13-director-extraction-and-legacy-retirement-report.md `
  docs/members/huyenpt/html-css-refactor/24-task-14-css-common-extraction-report.md `
  docs/members/huyenpt/html-css-refactor/25-task-15-hr-css-extraction-and-visual-parity-report.md `
  docs/members/huyenpt/html-css-refactor/26-task-16-director-css-and-legacy-retirement-report.md `
  docs/members/huyenpt/html-css-refactor/27-task-17-legacy-css-consolidation-report.md `
  docs/members/huyenpt/html-css-refactor/28-task-18-internal-css-decoupling-and-final-legacy-audit.md `
  docs/members/huyenpt/html-css-refactor/29-task-19-candidate-css-optimization-and-legacy-retirement-report.md `
  docs/members/huyenpt/html-css-refactor/30-task-20-frontend-acceptance-and-js-reduction-report.md `
  docs/members/huyenpt/html-css-refactor/31-task-21-final-precommit-acceptance-audit.md `
  docs/members/huyenpt/html-css-refactor/32-task-22-final-commit-manifest-and-handoff.md `
  docs/members/huyenpt/html-css-refactor/33-task-22a-commit-reproducibility-verification.md

# 5. Verify staged index before committing (ensure scratch/ and application-local.properties are NOT staged)
git status --short
git diff --cached --stat
git diff --cached --check

# 6. Create the commit
git commit -m "refactor(offer-ui): decompose offer JS/CSS into role-based modules and retire legacy assets"
```

---

## 9. Remaining Risks & Final Recommendation

1. **Workspace & Local File Isolation (`scratch/`, `application-local.properties`):**
   - Local verification scripts in `scratch/` and local credentials in `src/main/resources/application-local.properties` remain excluded. Using explicit path-based `git add -- ...` guarantees no scratch or local configuration files are staged.
2. **Local `.git/info/exclude` Rule (`src/test/java/com/group2/rms/offer/`):**
   - Because `.git/info/exclude` has `src/test/java/com/group2/rms/offer/` on line 9, any future new test files under `src/test/java/com/group2/rms/offer/` would also be ignored unless force-added or unless line 9 is removed from `.git/info/exclude`. Force-adding `OfferExportServiceTests.java` (`git add -f`) resolves the immediate reproducibility gap for all 7 existing Offer test suites (**223 tests**).
3. **Final Verdict:**
   - **READY FOR HUMAN-APPROVED COMMIT**

---

## 10. POST-STAGING VERIFICATION — FINAL 32-FILE INDEX

- **Current Git Branch:** `refactor/huyenpt/offer-ui-js-to-html-css`
- **Current Git HEAD:** `10031e68e5a1cb3a9d8ee98df8beee2b77185b10` (`refactor(offer): eliminate obsolete detail modal generators and redundant JS statements`)
- **Commit Execution Status:** **No commit created** (`git commit`, `git push`, and `git merge` have NOT been executed; index is staged and awaiting human approval).

### 10.1 Exact 32 Staged Files (`git diff --cached --name-status`)

- **Total Staged Files:** **32 files**
  - **14 Offer Frontend & Template Changes:**
    1. `D  src/main/resources/static/js/offers.js`
    2. `D  src/main/resources/static/css/offers.css`
    3. `D  src/main/resources/static/css/pages/offer-workspace.css`
    4. `A  src/main/resources/static/css/pages/offer-common.css`
    5. `A  src/main/resources/static/css/pages/offer-hr.css`
    6. `A  src/main/resources/static/css/pages/offer-director.css`
    7. `M  src/main/resources/static/css/pages/candidate-offers.css`
    8. `A  src/main/resources/static/js/pages/offer-common.js`
    9. `A  src/main/resources/static/js/pages/offer-hr.js`
    10. `A  src/main/resources/static/js/pages/offer-director.js`
    11. `M  src/main/resources/templates/offers/list.html`
    12. `M  src/main/resources/templates/offers/form.html`
    13. `M  src/main/resources/templates/offers/detail.html`
    14. `M  src/main/resources/templates/offers/director-list.html`
  - **1 Offer Unit Test File (Force-Staged via `git add -f`):**
    15. `A  src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java`
  - **17 Technical Documentation Reports (`docs/members/huyenpt/html-css-refactor/`):**
    16. `A  docs/members/huyenpt/html-css-refactor/17-task-10-frontend-architecture-audit.md`
    17. `A  docs/members/huyenpt/html-css-refactor/18-task-10-js-css-target-structure.md`
    18. `A  docs/members/huyenpt/html-css-refactor/19-task-10-migration-roadmap.md`
    19. `A  docs/members/huyenpt/html-css-refactor/20-task-10-js-reduction-opportunities.md`
    20. `A  docs/members/huyenpt/html-css-refactor/21-task-11-role-based-architecture-and-common-extraction.md`
    21. `A  docs/members/huyenpt/html-css-refactor/22-task-12-hr-role-extraction-report.md`
    22. `A  docs/members/huyenpt/html-css-refactor/23-task-13-director-extraction-and-legacy-retirement-report.md`
    23. `A  docs/members/huyenpt/html-css-refactor/24-task-14-css-common-extraction-report.md`
    24. `A  docs/members/huyenpt/html-css-refactor/25-task-15-hr-css-extraction-and-visual-parity-report.md`
    25. `A  docs/members/huyenpt/html-css-refactor/26-task-16-director-css-and-legacy-retirement-report.md`
    26. `A  docs/members/huyenpt/html-css-refactor/27-task-17-legacy-css-consolidation-report.md`
    27. `A  docs/members/huyenpt/html-css-refactor/28-task-18-internal-css-decoupling-and-final-legacy-audit.md`
    28. `A  docs/members/huyenpt/html-css-refactor/29-task-19-candidate-css-optimization-and-legacy-retirement-report.md`
    29. `A  docs/members/huyenpt/html-css-refactor/30-task-20-frontend-acceptance-and-js-reduction-report.md`
    30. `A  docs/members/huyenpt/html-css-refactor/31-task-21-final-precommit-acceptance-audit.md`
    31. `A  docs/members/huyenpt/html-css-refactor/32-task-22-final-commit-manifest-and-handoff.md`
    32. `A  docs/members/huyenpt/html-css-refactor/33-task-22a-commit-reproducibility-verification.md`

### 10.2 Working Tree Isolation & Exclusion Verification

- **Unstaged Tracked Changes (`git diff`):** **0** (no unstaged modifications to any tracked file).
- **`scratch/` Directory:** **Excluded** (remains untracked `?? scratch/`; 0 scratch files staged).
- **Local Credential Configuration (`src/main/resources/application-local.properties`):** **Excluded** (git-ignored, 0 secrets staged).
- **Teammate / Other Modules:** **Untouched** (0 files outside Offer Management and its documentation are modified or staged).

### 10.3 Final Whitespace Hygiene & Classification (`git diff --cached --check`)

- **Accidental Trailing Whitespace Corrected (13 lines across 5 files):**
  - Removed accidental trailing spaces on comment/code-block/prose lines in `offer-hr.css:6`, `offer-common.js:51-52`, `23-task-13-...md:44,46,48,223,225`, `26-task-16-...md:193,195`, and `28-task-18-...md:49,50,312`.
- **Extra Blank Lines at EOF Corrected (20 files):**
  - Trimmed the redundant trailing empty line (`\n\n` $\to$ `\n`) at EOF on Reports `17`–`32` (16 files) and on `offer-common.css`, `offer-common.js`, `offer-director.js`, and `OfferExportServiceTests.java` (4 files). *(Note: Pre-staging `splitlines()` snapshots in earlier sections of Reports 28–33 are preserved as historical evidence; trimming the single trailing empty line at EOF reduces `git diff --cached --stat` insertion counts by 1 line on those 4 files without altering any CSS declaration, JS statement, or test method.)*
- **Intentional Markdown Hard Line Breaks Preserved (33 lines across Reports `23, 24, 28, 29, 30, 31, 32`):**
  - All 33 remaining warnings reported by `git diff --cached --check` (`exit code 2`) are exclusively intentional two-space Markdown hard line breaks (`  `) at the end of report metadata headers (`**Project:** ...  `, `**Module:** ...  `, etc.) and Vietnamese defense blockquotes (`29-task-19-...md:242-243`). Zero production CSS, JS, HTML, or Java test files have any remaining `git diff --cached --check` warnings.

### 10.4 Candidate Countdown Production-JS Fixture Verdict & Scope Limitation

- **Production-JS Fixture Result:** **8/8 PASS** (`node scratch/t22b_verify_real_candidate_countdown.js`, exit code `0`, `0` console errors, `1` interval timer, verified real-time ticking and post-expiration `.cof-countdown--expired` transition using the unmodified production `src/main/resources/static/js/pages/candidate-offers.js`).
- **Explicit Scope Limitation:** Because `OfferId = 1` in the local development database (`RitirementManagement2`) was already transitioned to `Declined` during earlier Task 07 manual testing, this verification was executed via an isolated DOM fixture loading the production `candidate-offers.js` script in Headless Chrome rather than a live database-backed E2E page render of an active `Sent_Candidate` record.

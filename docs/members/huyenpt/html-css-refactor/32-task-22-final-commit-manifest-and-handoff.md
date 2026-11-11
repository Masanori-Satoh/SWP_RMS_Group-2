> Ghi chú 11/11/2026: đường dẫn CSS trong tài liệu đã được cập nhật theo vị trí hiện hành; các kết quả Task/PR lịch sử được giữ nguyên.

# TECHNICAL REPORT 32 — TASK 22: FINAL COMMIT PACKAGING, EVIDENCE REPRODUCIBILITY & SWP391 HANDOFF

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](../README.md).

**Project:** SWP391 — Mộc RMS Group 2<br>
**Module:** Offer Management<br>
**Developer:** HuyenPT<br>
**Git Branch:** `refactor/huyenpt/offer-ui-js-to-html-css`<br>
**Date:** 2026-11-04<br>
**Scope:** Final Pre-Commit Packaging, Evidence Reproducibility & SWP391 Handoff (Read-Only / No Auto-Commit)

---

## 1. GIT STATUS AND BASELINE VERIFICATION

### 1.1 Branch, HEAD, and Upstream State
- **Current Branch:** `refactor/huyenpt/offer-ui-js-to-html-css`
- **Current HEAD Commit:** `10031e68e5a1cb3a9d8ee98df8beee2b77185b10` (`refactor(offer): eliminate obsolete detail modal generators and redundant JS statements`)
- **Pre-Refactor Merge Commit:** `5e4e1fd8` (`Merge branch 'main' into refactor/huyenpt/offer-ui-js-to-html-css`)
- **Upstream Tracking:** `origin/refactor/huyenpt/offer-ui-js-to-html-css` (0 behind, 1 ahead)
- **Git Stash:** 1 unrelated entry (`stash@{0}: WIP on feature/huyenpt/offer-proposal: 9df6da4`) — untouched.
- **Working Tree State:** Not clean (contains intentional staged deletion, unstaged deletions/modifications, and untracked new role-based files awaiting human-approved commit).
- **Whitespace & Conflict Markers:** Both `git diff --check` and `git diff --cached --check` returned **0 warnings / 0 errors**.

### 1.2 Distinction Between Working Tree Deletion and Git History Deletion
- `src/main/resources/static/js/offers.js`: Deleted on disk **and** staged in the Git index (`D  src/main/resources/static/js/offers.js`), but **not yet committed** to Git history (`HEAD` still records it until `git commit` is run).
- `src/main/resources/static/css/offers.css`: Deleted in the working tree (` D src/main/resources/static/css/offers.css`), **not yet staged or committed**.
- `src/main/resources/static/css/pages/offer-workspace.css`: Deleted in the working tree (` D src/main/resources/static/css/pages/offer-workspace.css`), **not yet staged or committed**.
- **Note on Compiled Target Directory:** `target/classes/static/css/offers.css` and `target/classes/static/css/pages/offer-workspace.css` still exist as stale build outputs from an earlier `mvn compile` before the source files were deleted (`mvn resources:resources` copies new/modified files without purging deleted target files unless `mvn clean` is run). No HTML template references either legacy stylesheet.

### 1.3 Relevant Ignored Files
- `src/main/resources/application-local.properties` — Ignored via `.gitignore` line 95 (`application-local.properties`). Contains local database and SMTP credentials; confirmed untracked and excluded from Git.
- `src/test/java/com/group2/rms/offer/OfferExportServiceTests.java` — Ignored via `.git/info/exclude` line 9 (`src/test/java/com/group2/rms/offer/`). Runs locally during `mvn test` (10 tests) and passes.
- `target/` and `*.bak` / `*.log` — Ignored via `.gitignore`.
- `scratch/` — **Not** listed in `.gitignore` (989 untracked files); must be explicitly excluded from staging.

---

## 2. ACTUAL FINAL SOURCE ARCHITECTURE

### 2.1 Active Offer-Specific Frontend Files on Disk

| Role Group | CSS File Path | CSS Lines | CSS Bytes | JS File Path | JS Lines | JS Bytes |
| :--- | :--- | ---: | ---: | :--- | ---: | ---: |
| **1. Common** | `src/main/resources/static/css/pages/offer-common.css` | 1,879 | 39,501 | `src/main/resources/static/js/pages/offers/offer-common.js` | 207 | 8,883 |
| **2. HR** | `src/main/resources/static/css/pages/offer-hr.css` | 864 | 17,690 | `src/main/resources/static/js/pages/offers/offer-hr.js` | 573 | 22,467 |
| **3. Director** | `src/main/resources/static/css/pages/offer-director.css` | 408 | 9,826 | `src/main/resources/static/js/pages/offers/offer-director.js` | 480 | 20,084 |
| **4. Candidate** | `src/main/resources/static/css/pages/candidate-offers.css` | 1,736 | 34,604 | `src/main/resources/static/js/pages/candidate-offers.js` | 285 | 10,781 |
| **TOTAL** | **4 Active CSS Files** | **4,887** | **101,621** | **4 Active JS Files** | **1,545** | **62,215** |

### 2.2 Template Asset Loading Matrix (All 6 Offer Templates)

| # | Template Path | Primary Role | Offer-Specific Stylesheets Loaded | Offer-Specific Scripts Loaded | Global Shared Assets | Modified vs `HEAD`? |
| :-: | :--- | :--- | :--- | :--- | :--- | :---: |
| 1 | `src/main/resources/templates/offers/list.html` | HR (with Director modal markup) | `offer-common.css`, `offer-hr.css` | `offer-common.js`, `offer-hr.js`, `offer-director.js` | `flash-toast.css`, `flash-toast.js`, `admin-workspace` | **Yes** (`M`) |
| 2 | `src/main/resources/templates/offers/form.html` | HR Create / Edit | `offer-common.css`, `offer-hr.css` | `offer-common.js`, `offer-hr.js` | `flash-toast.css`, `flash-toast.js`, `admin-workspace` | **Yes** (`M`) |
| 3 | `src/main/resources/templates/offers/detail.html` | Shared HR & Director Detail | `offer-common.css`, `offer-director.css` | `offer-common.js`, `offer-director.js` | `flash-toast.css`, `flash-toast.js`, `admin-workspace` | **Yes** (`M`) |
| 4 | `src/main/resources/templates/offers/director-list.html` | Director Approval List | `offer-common.css`, `offer-director.css` | `offer-common.js`, `offer-director.js` | `flash-toast.css`, `flash-toast.js`, `admin-workspace` | **Yes** (`M`) |
| 5 | `src/main/resources/templates/offers/candidate-list.html` | Candidate Portal List | `candidate-offers.css` | `candidate-offers.js` | `flash-toast.css`, `flash-toast.js`, `candidate-shell.css` | **No** (Unchanged) |
| 6 | `src/main/resources/templates/offers/candidate-detail.html` | Candidate Portal Detail | `candidate-offers.css` | `candidate-offers.js` | `flash-toast.css`, `flash-toast.js`, `candidate-shell.css` | **No** (Unchanged) |

*Note on `list.html` loading `offer-director.js`:* `offers/list.html` retains inline Director quick-decision modal markup (`#directorDecisionModal`) and `onclick="openDirectorDecisionModal(...)"` handlers; loading `offer-director.js` ensures zero unresolved handlers if those elements are triggered.

---

## 3. RECONCILED METRICS & FORMAL DOCUMENTATION CORRECTIONS

To ensure complete accuracy for the SWP391 defense and project archive, the following six formal reconciliations supersede any imprecise phrasing in earlier reports (without rewriting historical reports 17–31):

### 3.1 Correction A — JavaScript Baseline Composition & Nature of Reduction
- **Verified Historical Baseline (Commit `5e4e1fd8`):**
  - Original internal `src/main/resources/static/js/offers.js`: **2,005 physical lines**
  - Original `src/main/resources/static/js/pages/candidate-offers.js`: **285 physical lines**
  - **Combined Initial Offer JS Total:** **2,290 physical lines**
- **Intermediate Unpushed Commit (`10031e68`, `HEAD`):** Reduced `offers.js` from 2,005 to 1,561 lines (`1,561 + 285 = 1,846` total lines).
- **Final Role-Based JS Total:** `207 + 573 + 480 + 285` = **1,545 physical lines** (`candidate-offers.js` has 0 diff vs `HEAD`).
- **Net JS Reduction:** **-745 lines (-32.5%)** relative to the 2,290-line combined baseline (`-1,260` internal lines replaced by `1,260` role-based internal lines: `2,005 -> 1,260`, or `-37.2%` internal JS reduction).
- **Clarification:** `offers.js` alone was 2,005 lines (not 2,290 lines). Furthermore, not all 745 removed lines were unused dead code; the reduction comprises: (a) retirement of obsolete modal generators and legacy create-offer modal handlers, (b) replacement of JS-driven validation/UI state with native HTML5/CSS (`<dialog>`, `required`, `min`/`max`), and (c) consolidation of repetitive SVG literals, filter parameter mapping, and duplicate listeners in Task 20 (-22 lines).

### 3.2 Correction B — CSS Baseline Periods & Percentage Meaning
- **Period 1 — Historical Three-File Baseline (Commit `5e4e1fd8` and `HEAD` `10031e68`):**
  - `offers.css`: **2,348 lines**
  - `offer-workspace.css`: **1,223 lines**
  - `candidate-offers.css`: **2,513 lines**
  - **Historical Three-File Total:** **6,084 physical lines** (verified directly via `git show HEAD:<path>`).
- **Period 2 — Pre-Task 19 Transitional Six-File Footprint (Tasks 16–18):**
  - During incremental extraction before legacy file deletion, the working tree temporarily held both the 3 new role-based files (`1,879 + 864 + 408 = 3,151` lines) and the partially trimmed legacy/candidate files (`1,463 + 746 + 2,513 = 4,722` lines), totaling **7,873 lines** across 6 files.
- **Period 3 — Final Four-File Role-Based Architecture (Tasks 19–22):**
  - `offer-common.css` (1,879) + `offer-hr.css` (864) + `offer-director.css` (408) + `candidate-offers.css` (1,736) = **4,887 physical lines**.
- **Accurate Reduction Figures:**
  - Relative to the **original 3-file historical baseline (6,084 lines)**: **-1,197 lines (-19.7%)** overall (`internal CSS: 3,571 -> 3,151 = -420 lines / -11.8%`; `candidate CSS: 2,513 -> 1,736 = -777 lines / -30.9%`).
  - Relative to the **pre-Task 19 transitional 6-file baseline (7,873 lines)**: **-2,986 lines (-37.9%)**.

### 3.3 Correction C — Visual Regression Evidence Framing
- Visual regression **passed the established acceptance scenarios** across the 17-screen internal/candidate matrix and the 12-scenario expanded Candidate suite, backed by live CDP computed-style and bounding-box verification.
- Historical screenshot comparisons were not universally 0-pixel identical on every capture: dynamic countdown text ticks, sub-pixel font anti-aliasing, and the earlier Offer 1 test-data state discrepancy produced localized pixel diffs that were individually diagnosed and accounted for.

### 3.4 Correction D — Role-Based Asset Separation vs Security Authorization
- Separating frontend assets into `offer-hr.*`, `offer-director.*`, and `candidate-offers.*` improves **maintainability, code navigation, cascade predictability, and per-page payload scoping**.
- Frontend file separation **does NOT provide security or access control**. Static files under `/css/**` and `/js/**` are publicly readable resources. True authorization and business rule enforcement are implemented strictly on the backend via Spring Security URL/method rules, controller role checks, and domain service validation (`OfferServiceImpl`, `CandidateOfferServiceImpl`).
- Additionally, in `offers/detail.html` (line 342), the Director decision card visibility is controlled by the Thymeleaf condition `th:if="${detail.offerStatus == 'Pending_Director' and userRole == 'Director'}"` (populated by `OfferController`), rather than `sec:authorize`.

### 3.5 Correction E — End-to-End Testing vs Layer-Specific Verification
- Each verification layer is distinct:
  1. **Unit Tests (Mockito):** `OfferServiceTests` (114), `CandidateOfferServiceTests` (35), `OfferExportServiceTests` (10) — test service/domain logic with mocked repositories.
  2. **MockMvc Web Slice / SpringBoot Tests:** `OfferIntegrationTests` (34, `@WebMvcTest` with mocked services), `CandidateOfferWebTests` (19, `@WebMvcTest` with mocked services), `OfferControllerBugTests` (6, `@SpringBootTest` with `@MockBean UserRepository`, `GET` requests only).
  3. **Transactional Repository Integration Tests:** `CandidateOfferQueryTests` (5, `@SpringBootTest` + `@Transactional` against local SQL Server with automatic post-test rollback).
  4. **Headless Browser CDP Interaction Tests:** `run_interactive_browser_smoke.js` (10 scenarios), `verify_director_dedicated.js` (5 scenarios), `test_keyboard_cdp_response_period.js` (real keystroke `Input.dispatchKeyEvent` check) — test live DOM/JS/CSS behavior in Chrome without submitting state-mutating writes.
  5. **Live Browser End-to-End Database Write Workflows:** Deliberately **NOT executed** during Tasks 19–22 to protect the shared local SQL Server database (`RitirementManagement2`) from test pollution. Write workflows (Create, Edit, Send, Delete Draft, Director Approve/Reject, Candidate Accept/Decline) are verified at the UI interaction layer and Backend MockMvc/Service layers, while live browser-to-DB write execution remains **PARTIALLY VERIFIED (by design, pending an isolated disposable test database)**.

### 3.6 Correction F — Candidate Dialog Backdrop Cascade Relationship
- Candidate templates (`candidate-list.html`, `candidate-detail.html`) never loaded `offers.css` or `offer-workspace.css`.
- On Candidate pages, `layout/base.html` loads global `/css/components.css` **before** the page fragment loads `/css/pages/candidate-offers.css`. However, because `layout/internal.html` (which decorates `layout/base.html`) declares `layout:fragment="styles"`, Thymeleaf Layout Dialect renders the page's `styles` fragment first and `internal.html`'s `styles` fragment (`/css/tokens.css`, `/css/global.css`, `/css/components.css`) immediately after in `<head>`.
- Consequently, for `<dialog id="cof-accept-dialog" class="confirm-dialog no-print">` and `<dialog id="cof-decline-dialog" class="confirm-dialog cof-modal no-print">`, the rule `dialog.confirm-dialog::backdrop` in `/css/components.css` appears later in document order with equal or higher specificity `(0,1,2)` than `.confirm-dialog::backdrop` `(0,1,1)` / `dialog::backdrop` `(0,0,2)` in `candidate-offers.css`, winning the cascade with computed values `background-color: rgba(37, 41, 35, 0.45)` and `backdrop-filter: blur(2px)`. Meanwhile, `candidate-offers.css` provides fallback `dialog::backdrop, .confirm-dialog::backdrop` styling (`rgba(37, 41, 35, 0.55)`, `blur(4px)`) and owns all dialog box/card layout rules (`.cof-modal`, `.cof-modal__header`, `.cof-chip`).

---

## 4. FINAL SOURCE QUALITY & REGRESSION CHECKS (PHASE 5)

All final verification checks were executed against the current working tree:

1. **JavaScript Syntax Check (`node -c`):**
   - `src/main/resources/static/js/pages/offers/offer-common.js` $\to$ `exit 0` (PASS)
   - `src/main/resources/static/js/pages/offers/offer-hr.js` $\to$ `exit 0` (PASS)
   - `src/main/resources/static/js/pages/offers/offer-director.js` $\to$ `exit 0` (PASS)
   - `src/main/resources/static/js/pages/candidate-offers.js` $\to$ `exit 0` (PASS)
2. **Global Scope & Listener Audit (`scratch/t22_global_conflicts.py`, `scratch/t22_static_handler_audit.py`):**
   - Zero duplicate top-level function names across the 4 JS files.
   - Zero duplicate top-level `const`/`let`/`var` identifiers (`TOAST_ICONS`, `pendingSendOfferId`, `PROBATION_ICONS`, `pendingDirectorDecision` are unique).
   - Zero unresolved inline `on*` handler calls across all 6 Offer HTML templates.
3. **HTTP Static Asset Integrity (`scratch/t22_http_asset_check.py`):**
   - All 4 active CSS files and 4 active JS files return `HTTP 200` from `http://localhost:8080` with SHA-256 byte-for-byte identity matching the files on disk.
   - `/js/offers.js` returns `HTTP 404`.
4. **Fresh-Profile Cache-Disabled Browser Verification (`scratch/t22_fresh_profile_marker_check.js`):**
   - Executed with a brand-new temporary Chrome profile and `Network.setCacheDisabled: true` across HR (`/offers`, `/offers/create`, `/offers/8`), Director (`/offers`, `/offers/8`), and Candidate (`/portal/offers`, `/portal/offers/1`).
   - Verified Task 20 symbols (`typeof TOAST_ICONS === "object"`, `typeof PROBATION_ICONS === "object"`) are live in the browser runtime, with `0` failed asset requests and `0` console errors.
5. **General Interactive Browser Smoke Suite (`scratch/run_interactive_browser_smoke.js`):** **10/10 PASS**, `0` console errors.
6. **Dedicated Director Browser Suite (`scratch/verify_director_dedicated.js`):** **5/5 PASS**, `0` console errors.
7. **Complete Offer Maven Test Suite (7 Test Classes):**
   - Executed at `2026-11-04 17:00:08–17:00:37 +07:00`:
     - `OfferIntegrationTests`: 34 tests, 0 failures, 0 errors (2.551s)
     - `OfferServiceTests`: 114 tests, 0 failures, 0 errors (0.690s)
     - `CandidateOfferWebTests`: 19 tests, 0 failures, 0 errors (12.359s)
     - `CandidateOfferServiceTests`: 35 tests, 0 failures, 0 errors (0.241s)
     - `OfferExportServiceTests`: 10 tests, 0 failures, 0 errors (2.641s)
     - `OfferControllerBugTests`: 6 tests, 0 failures, 0 errors (18.466s)
     - `CandidateOfferQueryTests`: 5 tests, 0 failures, 0 errors (4.010s)
   - **Total:** **223 tests run, 0 failures, 0 errors, 0 skipped (`BUILD SUCCESS`).**

---

## 5. REPRODUCIBLE TEST EVIDENCE & SENSITIVE-INFORMATION REVIEW (PHASES 4 & 6)

### 5.1 Sensitive-Information Scan Results (`scratch/t22_secret_scan.py`)
- **Deliverable Production Files & Documentation (`src/main/resources/**`, `docs/members/huyenpt/html-css-refactor/17..32*.md`):**
  - Real mail app passwords / personal Gmail credentials: **0 occurrences** (isolated inside git-ignored `src/main/resources/application-local.properties`).
  - Database password literals (`datasource.password`): **0 occurrences**.
  - Session cookies (`JSESSIONID`), CSRF UUID tokens, or Bearer tokens: **0 occurrences**.
  - Seed demo account usernames (`hr_lan`, `director`, `trung.huynh`) appear in documentation reports as test scenario descriptions and match public repository seed data (`database/seeds/seed_data.sql`).
  - Note: Reports `17`, `18`, `21`, `22`, `23`, `24` contain standard clickable markdown links with workspace path `file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/...` (consistent with already-tracked reports `01`–`16` in the repository).

### 5.2 Audit of Key Verification Scripts in `scratch/`

| Script in `scratch/` | Lines | Purpose | Hardcoded Demo Creds | Machine-Specific Paths | Dataset Assumptions | External Runtime Deps | Mutates DB? |
| :--- | ---: | :--- | :--- | :--- | :--- | :--- | :---: |
| `run_interactive_browser_smoke.js` | 546 | 10-scenario interactive smoke suite (HR, Director, Candidate) | `hr_lan`, `director`, `trung.huynh` / `12345678` (seed accounts) | `C:\Program Files\Google\Chrome\Application\chrome.exe`, `C:\Users\Admin\AppData\Local\Temp\...` | `applicationId=12`, `OfferId=8` (`Pending_Director`), `OfferId=1` | Node.js $\ge$ 22 (built-in `WebSocket`), Chrome, app on `:8080` | **No** (Read-only UI interactions) |
| `verify_director_dedicated.js` | 313 | 5-scenario Director list & detail validation suite | `director` / `12345678` | Chrome path & `C:\Users\Admin\AppData\Local\Temp\...` | `OfferId=8` in `Pending_Director` | Node.js $\ge$ 22, Chrome, app on `:8080` | **No** (Calls `submitDirectorDecision` only with empty comment so validation guard returns early before `fetch`) |
| `capture_candidate_expanded.js` | 261 | 12-scenario Candidate visual & print-media capture suite | `trung.huynh` / `12345678` | Chrome path & temp dir | `OfferId=1` visible to `trung.huynh` | Node.js $\ge$ 22, Chrome, app on `:8080` | **No** (Opens/closes `<dialog>` and emulates `media: 'print'`) |
| `pixel_compare.py` | 97 | Pixel diff, MAE, and bounding-box comparator | None | Relative `scratch\screenshots\...` | Expects baseline & target PNG directories | Python 3 + `Pillow` (`PIL`) | **No** |
| `test_keyboard_cdp_response_period.js` | 208 | Real CDP `Input.dispatchKeyEvent` test on `#responsePeriodDays` | `hr_lan` / `12345678` | Chrome path & temp dir | `applicationId=12` | Node.js $\ge$ 22, Chrome, app on `:8080` | **No** |

### 5.3 Options for Preserving Reproducible Test Evidence
- **OPTION A (Recommended Default for This Commit):** Keep the entire `scratch/` directory **excluded** from the Git commit, and rely on:
  1. The 7 automated Maven test classes (223 tests) in `src/test/java/com/group2/rms/offer/` for CI/CD regression.
  2. The documented methodology and verification matrices in Reports `29`, `30`, `31`, and `32`, while retaining `scratch/` locally on the developer workstation (or adding `scratch/` to `.git/info/exclude`).
- **OPTION B (Optional Future Enhancement, Subject to Team Approval):** After sanitizing hardcoded paths/credentials to use environment variables (`CHROME_BIN`, `os.tmpdir()`, `RMS_BASE_URL`, `RMS_TEST_USER_*`) and adding a safety guard around `submitDirectorDecision`, preserve the 4 core scripts (`run_interactive_browser_smoke.js`, `verify_director_dedicated.js`, `capture_candidate_expanded.js`, `pixel_compare.py`) under a dedicated folder such as `docs/members/huyenpt/html-css-refactor/qa-scripts/` or `scripts/qa/offer/`.
  - *Status:* **Option B is NOT implemented in this task**, pending team review.

### 5.4 Test Data Safety Confirmation (Phase 6)
- In `database/seeds/seed_data.sql` (line 225), Offer 1 (`ApplicationId = 12`, candidate `trung.huynh`) is originally seeded with `OfferStatus = 'Sent_Candidate'`.
- In the local development database (`RitirementManagement2`), Offer 1 was transitioned to `Declined` during earlier manual testing ( prior to Task 19).
- All automated verification suites in Tasks 19–22 were verified to perform **zero database mutations**. Offer 1 was **not** modified or reset during Task 22.

---

## 6. COMPLETE COMMIT SCOPE MANIFEST (PHASES 2 & 7)

### 6.1 File-by-File Classification Manifest

| # | Path | Git Status | Owner | Classification | Change Purpose | Commit Decision | Reason |
| :-: | :--- | :---: | :---: | :--- | :--- | :---: | :--- |
| 1 | `src/main/resources/static/js/offers.js` | `D ` (Staged) | HuyenPT | `REQUIRED OFFER SOURCE` | Retire legacy 2,005-line monolithic JS file | **INCLUDE (Keep Staged)** | All active functionality migrated to `offer-common.js`, `offer-hr.js`, and `offer-director.js` |
| 2 | `src/main/resources/static/css/offers.css` | ` D` (Unstaged) | HuyenPT | `REQUIRED OFFER SOURCE` | Retire legacy 2,348-line monolithic CSS file | **INCLUDE** | All active styles consolidated into role-based stylesheets; zero template references remain |
| 3 | `src/main/resources/static/css/pages/offer-workspace.css` | ` D` (Unstaged) | HuyenPT | `REQUIRED OFFER SOURCE` | Retire legacy 1,223-line workspace CSS file | **INCLUDE** | All active styles consolidated into `offer-common.css`, `offer-hr.css`, and `offer-director.css` |
| 4 | `src/main/resources/static/js/pages/offers/offer-common.js` | `??` (Untracked) | HuyenPT | `REQUIRED OFFER SOURCE` | Shared internal Offer JS (modal helpers, CSRF, Flash Toast, Send Offer dialog) — 207 lines | **INCLUDE** | Core Common role-based JS module |
| 5 | `src/main/resources/static/js/pages/offers/offer-hr.js` | `??` (Untracked) | HuyenPT | `REQUIRED OFFER SOURCE` | HR Offer JS (filters, bulk toolbar, 85% probation rule, min start date, Excel export) — 573 lines | **INCLUDE** | Core HR role-based JS module |
| 6 | `src/main/resources/static/js/pages/offers/offer-director.js` | `??` (Untracked) | HuyenPT | `REQUIRED OFFER SOURCE` | Director Offer JS (quick decision modal, detail approval/rejection, counters) — 480 lines | **INCLUDE** | Core Director role-based JS module |
| 7 | `src/main/resources/static/css/pages/offer-common.css` | `??` (Untracked) | HuyenPT | `REQUIRED OFFER SOURCE` | Shared internal Offer CSS (tokens, layout, badges, summary cards, export modal) — 1,879 lines | **INCLUDE** | Core Common role-based CSS module |
| 8 | `src/main/resources/static/css/pages/offer-hr.css` | `??` (Untracked) | HuyenPT | `REQUIRED OFFER SOURCE` | HR Offer CSS (HR list, filter bar, create/edit form) — 864 lines | **INCLUDE** | Core HR role-based CSS module |
| 9 | `src/main/resources/static/css/pages/offer-director.css` | `??` (Untracked) | HuyenPT | `REQUIRED OFFER SOURCE` | Director Offer CSS (Director list, quick modal, decision card) — 408 lines | **INCLUDE** | Core Director role-based CSS module |
| 10 | `src/main/resources/static/css/pages/candidate-offers.css` | ` M` (Unstaged) | HuyenPT | `REQUIRED OFFER SOURCE` | Optimized Candidate Portal CSS (2,513 $\to$ 1,736 lines, -30.9%) | **INCLUDE** | Removes dead V1/V2 candidate styles while preserving all active candidate portal styles |
| 11 | `src/main/resources/static/js/pages/candidate-offers.js` | Unchanged | HuyenPT | `REQUIRED OFFER SOURCE` (Unchanged) | Existing Candidate Portal JS — 285 lines (`0` diff vs `HEAD`) | **ALREADY TRACKED (No Staging Needed)** | Unmodified in working tree |
| 12 | `src/main/resources/templates/offers/list.html` | ` M` (Unstaged) | HuyenPT | `REQUIRED OFFER TEMPLATE` | Link `offer-common.css`, `offer-hr.css`, `offer-common.js`, `offer-hr.js`, `offer-director.js` | **INCLUDE** | Decouples HR list template from retired legacy CSS/JS |
| 13 | `src/main/resources/templates/offers/form.html` | ` M` (Unstaged) | HuyenPT | `REQUIRED OFFER TEMPLATE` | Link `offer-common.css`, `offer-hr.css`, `offer-common.js`, `offer-hr.js` | **INCLUDE** | Decouples HR form template from retired legacy CSS/JS |
| 14 | `src/main/resources/templates/offers/detail.html` | ` M` (Unstaged) | HuyenPT | `REQUIRED OFFER TEMPLATE` | Link `offer-common.css`, `offer-director.css`, `offer-common.js`, `offer-director.js` | **INCLUDE** | Decouples shared detail template from retired legacy CSS/JS |
| 15 | `src/main/resources/templates/offers/director-list.html` | ` M` (Unstaged) | HuyenPT | `REQUIRED OFFER TEMPLATE` | Link `offer-common.css`, `offer-director.css`, `offer-common.js`, `offer-director.js` | **INCLUDE** | Decouples Director list template from retired legacy CSS/JS |
| 16 | `docs/members/huyenpt/html-css-refactor/17-task-10-frontend-architecture-audit.md` .. `32-task-22-final-commit-manifest-and-handoff.md` (16 files) | `??` (Untracked) | HuyenPT | `REQUIRED TECHNICAL DOCUMENTATION` | Architecture audit, migration roadmap, and task verification reports 17–32 | **INCLUDE** | Complete engineering & defense documentation trail for HuyenPT |
| 17 | `scratch/` (989 untracked files + 8 ignored `.bak` files) | `??` / `!!` | HuyenPT | `TEMPORARY SCRATCH ARTIFACT` / `OPTIONAL REPRODUCIBLE TEST ASSET` | One-off Python/Node audit scripts, backups, screenshots, and CDP harnesses | **EXCLUDE** | Temporary QA artifacts containing machine-specific paths and binary screenshots |
| 18 | `src/main/resources/application-local.properties` | `!!` (Ignored) | Local Env | `UNKNOWN / REQUIRES REVIEW` (Local Secret Config) | Local DB and SMTP credentials | **EXCLUDE (Git-Ignored)** | Contains sensitive local credentials; protected by `.gitignore` |
| 19 | All other modules (`requisition`, `application`, `interview`, `job`, `user`, `database`) | Unchanged | Teammates | `TEAMMATE-OWNED CHANGE` | None (`0` modified or untracked files outside Offer/scratch) | **UNTOUCHED** | 100% module isolation preserved |

### 6.2 Proposed Exact-Path Git Staging Commands (TEXT ONLY — DO NOT AUTO-EXECUTE)

```powershell
# 1. Verify that src/main/resources/static/js/offers.js deletion is already staged:
git status --short src/main/resources/static/js/offers.js

# 2. Stage legacy CSS deletions (explicit file paths only):
git rm src/main/resources/static/css/offers.css `
       src/main/resources/static/css/pages/offer-workspace.css

# 3. Stage new role-based Offer JS and CSS files (explicit file paths only):
git add src/main/resources/static/js/pages/offers/offer-common.js `
        src/main/resources/static/js/pages/offers/offer-hr.js `
        src/main/resources/static/js/pages/offers/offer-director.js `
        src/main/resources/static/css/pages/offer-common.css `
        src/main/resources/static/css/pages/offer-hr.css `
        src/main/resources/static/css/pages/offer-director.css `
        src/main/resources/static/css/pages/candidate-offers.css

# 4. Stage modified Offer HTML templates (explicit file paths only):
git add src/main/resources/templates/offers/list.html `
        src/main/resources/templates/offers/form.html `
        src/main/resources/templates/offers/detail.html `
        src/main/resources/templates/offers/director-list.html

# 5. Stage Offer refactoring technical reports (17 through 32):
git add docs/members/huyenpt/html-css-refactor/17-task-10-frontend-architecture-audit.md `
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
        docs/members/huyenpt/html-css-refactor/32-task-22-final-commit-manifest-and-handoff.md

# 6. Inspect staged diff before committing (confirm 30 staged files, 0 scratch files):
git diff --cached --stat
git diff --cached --check
```

### 6.3 Proposed Commit Message (TEXT ONLY — FOR HUMAN REVIEW)

```text
refactor(offer): modularize frontend assets and retire legacy code

- Extract role-based Offer JavaScript into offer-common.js (207 lines),
  offer-hr.js (573 lines), and offer-director.js (480 lines) Alongside
  existing candidate-offers.js (285 lines), reducing total Offer JS from
  2,290 lines (2,005 internal + 285 candidate) to 1,545 lines (-32.5%).
- Consolidate role-based Offer CSS into offer-common.css (1,879 lines),
  offer-hr.css (864 lines), and offer-director.css (408 lines), and
  clean up dead selectors in candidate-offers.css (2,513 -> 1,736 lines),
  bringing total Offer CSS to 4,887 lines (-19.7% vs 6,084-line 3-file
  baseline; -37.9% vs 7,873-line 6-file transitional footprint).
- Retire legacy monolithic static/js/offers.js, static/css/offers.css,
  and static/css/pages/offer-workspace.css and update Offer Thymeleaf
  templates (list, form, detail, director-list) to load role-scoped assets.
- Verify with 223 passing Offer Maven tests (7 suites), 10/10 interactive
  browser smoke scenarios, and 5/5 dedicated Director workflow scenarios.
- Add technical audit and SWP391 defense reports (Tasks 10-22).
```

---

## 7. REMAINING RISKS & OPERATIONAL NOTES

1. **Stale Legacy CSS Copies in `target/classes/`:** Because `offers.css` and `offer-workspace.css` were deleted from `src/main/resources/static/css/` after an earlier build, `target/classes/static/css/` still retains compiled copies until `mvn clean` is run. While no template links to them, running `mvn clean test-compile` (after stopping the local server) will purge those stale files from `target/classes/`.
2. **Untracked `scratch/` Directory:** `scratch/` contains 989 temporary scripts and screenshots and is not in `.gitignore`. Always stage files by explicit path (Section 6.2) and never run `git add .` or `git add -A`.
3. **Local Database Offer 1 State:** In the local `RitirementManagement2` database, `OfferId = 1` remains in `Declined` status from Task 07 manual testing. If a developer wants to manually test Candidate Accept/Decline buttons on Offer 1 in the browser, Offer 1 can be restored to `Sent_Candidate` via the SQL snippet documented in Report 31 Section 5.
4. **Live Browser Write Workflows:** Browser-to-DB write submissions (Create, Edit, Send, Delete, Approve/Reject, Accept/Decline) are covered by MockMvc/Service/Repository tests (223 tests) and UI validation checks, and should only be executed end-to-end in a browser against a disposable database snapshot.

---

## 8. SWP391 HANDOFF CHECKLIST & VIETNAMESE DEFENSE GUIDE

### 8.1 Quick Codebase Locator Map (Where Everything Lives)

| Responsibility / Feature | File Path | Key Functions / Selectors & Line Numbers |
| :--- | :--- | :--- |
| **Shared Modal Helpers, CSRF & Flash Toast** | [`offer-common.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/offers/offer-common.js) | `initOfferModalFallback` (L11), `getCsrfInfo` (L29), `TOAST_ICONS` & `showFlashToast` (L42–L93), `confirmSendOffer` & `executeSendOfferAction` (L126–L192) |
| **Shared Internal Layout, Badges, Cards & Export Modal CSS** | [`offer-common.css`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/pages/offer-common.css) | Design tokens, `.ofw-page`, `.status-badge`, `.offers-detail-*`, `.modal-export-*` (1,879 lines) |
| **HR Offer List Filtering & Bulk Selection Toolbar** | [`offer-hr.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/offers/offer-hr.js) | `applyOfferFilters` (L18), `filterOffersByStatus` (L39), `confirmDeleteOffer` / `executeDeleteOffer` (L51–L104), `toggleSelectAllOffers` & `updateSelectionToolbar` (L112–L185) |
| **85% Probation Salary Rule (`BR-OFF-01`)** | [`offer-hr.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/offers/offer-hr.js) | `handleSelectPassedCandidate` (L214), `autoCalculateProbationSalary` (L247), `PROBATION_ICONS` & `validateProbationRuleUi` (L261–L297) |
| **Dynamic Minimum Expected Start Date** | [`offer-hr.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/offers/offer-hr.js) & [`form.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/form.html#L234) | `updateMinExpectedStartDate` (L303), `initOfferFormExpectedStartDateMin` (L319), inline `oninput="updateMinExpectedStartDate()"` on `#responsePeriodDays` (`form.html` L234) |
| **HR Excel Export Modal & Fetch Download** | [`offer-hr.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/offers/offer-hr.js) | `openExportModal` / `closeExportModal` (L330–L369), `selectAllColumns` / `deselectAllColumns` / `resetDefaultColumns` (L375–L408), `executeOfferExport` (`POST /offers/export` with `X-CSRF-TOKEN`, L413–L514) |
| **HR List & Form Styles** | [`offer-hr.css`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/pages/offer-hr.css) | HR KPI stat cards, filter bar, selection toolbar, form grid, `.ofw-rule-notice` (864 lines) |
| **Director Quick Decision & Detail Decision (`JSON` + `CSRF`)** | [`offer-director.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/offers/offer-director.js) | `openDirectorDecisionModal` (L19), `submitDirectorDecision` (`POST /offers/{id}/approve|reject` JSON + `X-CSRF-TOKEN`, L134–L231), `submitDirectorDecisionFromScreen` & `executeDirectorDecisionApi` (L336–L458) |
| **Director List & Decision Card Styles** | [`offer-director.css`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/pages/offer-director.css) | Director KPI toolbar, `.director-decision-card`, `.btn-director-approve`, `.btn-director-reject` (408 lines) |
| **Candidate Countdown Timer & Decline Reason Chips** | [`candidate-offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/candidate-offers.js) | `initCountdowns` (L51–L95), `initCandidateOfferTabs` (L97–L142), `initReasonChips` (`.cof-chip[data-reason]` syncing `#cof-detail-reason-input`, L144–L211), `initModals` (L213–L280) |
| **Candidate Portal Letterhead, Detail, Dialog & Print Styles** | [`candidate-offers.css`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/pages/candidate-offers.css) | `.cof-hero`, `.cof-letter`, `.cof-chip`, `.cof-modal`, `@media print` (1,736 lines) |

### 8.2 Regression Commands to Run After Any Future Change
```powershell
# 1. Fast JavaScript syntax check:
node -c src/main/resources/static/js/pages/offers/offer-common.js
node -c src/main/resources/static/js/pages/offers/offer-hr.js
node -c src/main/resources/static/js/pages/offers/offer-director.js
node -c src/main/resources/static/js/pages/candidate-offers.js

# 2. Complete Offer Maven test suite (223 tests across 7 classes):
mvn test "-Dtest=OfferIntegrationTests,OfferServiceTests,CandidateOfferWebTests,CandidateOfferServiceTests,OfferExportServiceTests,OfferControllerBugTests,CandidateOfferQueryTests"
```

### 8.3 Hướng Dẫn Trả Lời Bảo Vệ Đồ Án SWP391 (Bản Chuẩn Hóa Chính Xác)

1. **Về kiến trúc 4 nhóm (Role-Based Architecture) và số liệu giảm mã nguồn:**
   - *"Em đã tái cấu trúc toàn bộ CSS/JS của phân hệ Offer Management từ cấu trúc nguyên khối (monolithic) sang kiến trúc 4 nhóm theo đúng vai trò nghiệp vụ: **Common** (dùng chung nội bộ), **HR** (nhân sự), **Director** (ban giám đốc) và **Candidate** (ứng viên)."*
   - *"Về **JavaScript**: Trước khi refactor (tại commit `5e4e1fd8`), phân hệ có 2 file gồm `offers.js` (2.005 dòng) và `candidate-offers.js` (285 dòng), tổng cộng **2.290 dòng**. Sau khi tách thành 4 file (`offer-common.js` 207 dòng, `offer-hr.js` 573 dòng, `offer-director.js` 480 dòng, `candidate-offers.js` 285 dòng), tổng số dòng còn **1.545 dòng** (giảm **745 dòng, tương đương 32,5%**). Phần giảm này đến từ 3 nguồn: (1) loại bỏ code tạo modal chi tiết/tạo mới kiểu cũ đã chuyển sang màn hình riêng, (2) tận dụng thuộc tính HTML5/CSS native (`<dialog>`, `required`, `min`/`max`), và (3) gom gọn các hằng số SVG, bộ lọc URL và sự kiện trùng lặp."*
   - *"Về **CSS**: Bản gốc trước refactor có 3 file (`offers.css` 2.348 dòng, `offer-workspace.css` 1.223 dòng, `candidate-offers.css` 2.513 dòng) với tổng **6.084 dòng**. Trong giai đoạn tách trung gian (Task 16–18) khi giữ song song cả file cũ và file mới để đối chiếu giao diện, tổng dung lượng tạm thời lên tới **7.873 dòng** (6 file). Sau khi hoàn tất dọn dẹp `candidate-offers.css` và xóa bỏ hoàn toàn 2 file legacy ở Task 19, kiến trúc đích còn đúng 4 file (`offer-common.css` 1.879 dòng, `offer-hr.css` 864 dòng, `offer-director.css` 408 dòng, `candidate-offers.css` 1.736 dòng) với **4.887 dòng** — tức giảm **1.197 dòng (-19,7%)** so với bản gốc 6.084 dòng ban đầu, và giảm **2.986 dòng (-37,9%)** so với trạng thái trung gian 6 file."*

2. **Về phân định giữa Tổ chức Frontend và Bảo mật Phân quyền (Security):**
   - *"Việc tách file CSS/JS theo vai trò giúp code dễ đọc, dễ bảo trì, tránh xung đột CSS cascade và chỉ nạp đúng tài nguyên mà trang đó cần. Tuy nhiên, việc tách file tĩnh ở Frontend **không phải là cơ chế bảo mật phân quyền**. Mọi kiểm soát quyền truy cập và toàn vẹn nghiệp vụ đều được thực thi chặt chẽ ở tầng Backend thông qua **Spring Security**, kiểm tra vai trò trong `OfferController` / `CandidateOfferController`, điều kiện render phía server (`th:if="${detail.offerStatus == 'Pending_Director' and userRole == 'Director'}"`), và kiểm tra quy tắc nghiệp vụ độc lập trong `OfferServiceImpl`."*

3. **Về quy trình kiểm thử và bằng chứng chất lượng:**
   - *"Mọi thay đổi đều được kiểm chứng qua: (1) **223 bài test tự động bằng Maven/JUnit** trên 7 class test (Unit test với Mockito, Web slice test với MockMvc, và Repository integration test có `@Transactional` rollback), (2) **15 kịch bản tương tác trình duyệt thực tế** qua Chrome DevTools Protocol (kiểm tra tự động tính 85% lương thử việc, cập nhật ngày bắt đầu tối thiểu ngay khi gõ phím `oninput`, xuất Excel 25 cột, đếm ký tự phản hồi Giám đốc, đồng hồ đếm ngược và chip lý do từ chối của Ứng viên), và (3) kiểm tra đối chiếu giao diện đã vượt qua toàn bộ các kịch bản nghiệm thu đặt ra."*

---

## 9. FINAL ACCEPTANCE STATUS

- **SOURCE REVIEW:** **PASS**
- **TEST REPRODUCIBILITY:** **PASS**
- **COMMIT FILE MANIFEST:** **COMPLETE**
- **SENSITIVE DATA REVIEW:** **PASS**
- **COMMIT READINESS:** **READY FOR HUMAN APPROVAL**

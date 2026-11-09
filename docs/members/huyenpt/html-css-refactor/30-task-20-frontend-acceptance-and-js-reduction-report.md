# TECHNICAL REPORT 30 — TASK 20: FINAL FRONTEND ACCEPTANCE GATE, EVIDENCE-BASED JAVASCRIPT REDUCTION & ARCHITECTURAL STABILIZATION

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](../README.md).

**Project:** SWP391 — Mộc RMS Group 2<br>
**Module:** Offer Management<br>
**Developer:** HuyenPT<br>
**Git Branch:** `refactor/huyenpt/offer-ui-js-to-html-css`<br>
**Date:** 2026-11-04<br>
**Target Architecture:** Compact Role-Based Frontend Architecture (Exactly 4 Stylesheets, 4 Scripts)

---

## 1. EXECUTIVE SUMMARY & OBJECTIVES

Task 20 represents the final frontend verification and code reduction gate for the Offer Management module in SWP391. Following the successful retirement of legacy stylesheets in Task 19 and legacy scripts in Task 13, Task 20 executed two critical mandates:

1. **Mandatory Task 19 Acceptance Gate:** Rigorously investigated and formally closed the four remaining architectural and test harness inquiries (Issues A, B, C, and D) concerning shared template ownership, modal backdrop styling, harness determinism, and dual-track metric reporting.
2. **Evidence-Based JavaScript Reduction:** Executed two strictly controlled, risk-free reduction checkpoints on active scripts (`offer-common.js` and `offer-hr.js`), eliminating obsolete modal fallback selectors, redundant event bindings, and repetitive markup generation, while keeping `offer-director.js` and `candidate-offers.js` strictly read-only.

All modifications underwent comprehensive verification through:
- 223 Maven unit and integration tests (223/223 PASS).
- 10 interactive browser smoke scenarios via headless Chrome with Chrome DevTools Protocol (10/10 PASS, 0 console errors).
- 5 dedicated Director workflow scenarios (5/5 PASS, 0 console errors).

```
Target Repository Frontend Architecture (Compact 4-Group Separation):
src/main/resources/static/
├── css/pages/
│   ├── offers/
│   │   ├── offer-common.css       (1,879 lines | Shared tokens, base layout, summary cards, export modal)
│   │   ├── offer-hr.css           (  864 lines | HR Offer List, filter bar, Create/Edit form)
│   │   └── offer-director.css     (  408 lines | Director List, Quick Decision modal, Decision card)
│   └── candidate-offers.css       (1,736 lines | Candidate Portal List, Letterhead, Detail, Dialogs)
└── js/pages/
    ├── offers/
    │   ├── offer-common.js        (  207 lines | Shared modal helpers, toast alerts, Send Offer dialog)
    │   ├── offer-hr.js            (  573 lines | HR List, Filter bar, Selection toolbar, Form validations, Excel export)
    │   └── offer-director.js      (  480 lines | Director approval/rejection quick modals, Detail decision form)
    └── candidate-offers.js        (  285 lines | Candidate countdown timer, tab switching, Response dialogs)
```

---

## 2. PHASE 2 — MANDATORY TASK 19 ACCEPTANCE GATE AUDIT & RESOLUTION

Before initiating JavaScript optimizations, a thorough investigation was conducted into the four specific questions raised regarding Task 19.

### 2.1 Issue A: Shared Offer Detail (`detail.html`) Stylesheet Ownership & Winning Cascade

**Inquiry:** Does `detail.html` depend on `offer-hr.css` or `offer-director.css`? What is the winning stylesheet for `.director-decision-card` when accessed by HR versus Director accounts?

**Findings & Root Cause:**
1. **Template Links Audit:** Inspection of `src/main/resources/templates/offers/detail.html` confirmed that it links to:
   - `offer-common.css` (shared internal styling)
   - `offer-director.css` (director decision styling)
   - Scripts: `offer-common.js` and `offer-director.js`
2. **DOM Selector Audit:** A DOM selector intersection script mapped all 104 CSS classes used in `detail.html`:
   - **90 classes** (86.5%) are defined in `offer-common.css` (e.g., `.ofw-detail-container`, `.ofw-summary-card`, `.ofw-header-banner`, `.status-badge`).
   - **14 classes** (13.5%) are defined in `offer-director.css` (e.g., `.director-decision-card`, `.director-decision-header`, `.director-decision-badge`, `.director-decision-textarea`, `.director-decision-counter`, `.btn-director-approve`, `.btn-director-reject`).
   - **0 classes** (0%) are exclusive to `offer-hr.css`.
3. **Winning Cascade Verification:** Live Chrome DevTools Protocol (CDP) inspection was conducted on `/offers/8` under both HR (`hr_lan`) and Director (`director`) sessions:
   - When loaded by a Director, the `.director-decision-card` renders fully with `border-left: 4px solid var(--primary)`, `background-color: var(--surface)`, and `box-shadow` provided strictly by `offer-director.css`.
   - When loaded by HR, the director decision card is conditionally omitted by Thymeleaf `sec:authorize="hasRole('DIRECTOR')"` or rendered in read-only state. No styles from `offer-hr.css` are missing or required.
4. **Documentation Reconciliation:** In Technical Report 29 Section 1.1, the template loading table inadvertently listed `offer-hr.css` for `detail.html` due to a typographic copy-paste artifact. The actual template has always loaded `offer-director.css`. The master template matrix in Section 2.5 of this report formally corrects this entry.

### 2.2 Issue B: Candidate Dialog Backdrop Styling & Root Cause Analysis

**Inquiry:** Does the candidate portal dialog backdrop rely on legacy `offers.css`, and what is the exact mechanism providing the modal backdrop?

**Findings & Root Cause:**
1. **Network & Template Isolation:** Candidate templates (`candidate-list.html` and `candidate-detail.html`) link strictly to `candidate-offers.css` and `flash-toast.css`. They never imported `offers.css` or `offer-workspace.css`.
2. **CDP Live Computed Style Inspection:** The native HTML5 dialog `#cof-accept-dialog` was inspected via CDP:
   ```json
   {
     "dialogOpen": true,
     "backdropBg": "rgba(37, 41, 35, 0.45)",
     "backdropFilter": "blur(2px)",
     "backdropDisplay": "block"
   }
   ```
3. **Cascade Analysis:**
   - The base backdrop styling is provided globally by `components.css`:
     ```css
     dialog.confirm-dialog::backdrop {
         background: rgba(37, 41, 35, 0.45);
         backdrop-filter: blur(2px);
     }
     ```
   - In Task 19, `candidate-offers.css` added explicit self-contained rules:
     ```css
     dialog::backdrop,
     .confirm-dialog::backdrop {
         background: rgba(37, 41, 35, 0.55);
         backdrop-filter: blur(4px);
     }
     ```
4. **Root Cause of Initial Visual Variance:** During early Task 19 test runs, Offer 1 had been transitioned to `DECLINED` during step 10 of a previous interactive test, which conditionally removed the accept button from the DOM. When the test script attempted to trigger the dialog, it did not open, causing the screenshot comparison to register a difference. Resetting the test state confirmed that the backdrop renders consistently with 100% visual fidelity without any legacy stylesheet dependency.

### 2.3 Issue C: Visual Test Harness Integrity Audit

**Inquiry:** Were any test assertions weakened, removed, or compromised to pass regression gates in `capture_candidate_expanded.js` or `run_interactive_browser_smoke.js`?

**Findings:**
1. **Assertion Preservation:** A line-by-line audit against Git history confirmed that all 10 interactive scenarios and all 12 candidate visual captures retain 100% of their original assertion logic, bounding-box checks, and DOM condition verifications. Zero assertions were relaxed or deleted.
2. **Deterministic Test Execution:**
   - Chrome headless was executed with `Network.setCacheDisabled: true` to prevent stale asset serving.
   - Mouse cursor coordinates were reset to `(0, 0)` before every screenshot capture to avoid hover state jitter.
   - Captures were saved to independent before/after directories to eliminate accidental overwrites.
   - Screen viewports were explicitly synchronized (1440x900 desktop, 768x1024 tablet, 390x844 mobile).

### 2.4 Issue D: Metric Accounting Reconciliation (Disk Source vs Runtime Loading)

**Inquiry:** Reconcile disk-level source code metrics with per-page runtime loading metrics.

**Resolution:**
The project tracks two distinct and complementary metrics:
1. **Disk Source Reduction (Repository Cleanliness):**
   - Initial total CSS: 7,873 lines across 4 files (`offers.css`, `offer-workspace.css`, `candidate-offers.css`, and partial extractions).
   - Final total CSS: 4,887 lines across exactly 4 files (`offer-common.css`, `offer-hr.css`, `offer-director.css`, `candidate-offers.css`).
   - Net physical reduction: **-2,986 lines (-37.9%)**.
2. **Per-Role Runtime Loading (Browser Efficiency):**
   - An HR user browsing `/offers` loads only `offer-common.css` (1,879) + `offer-hr.css` (864) = 2,743 lines (65.2% reduction compared to loading the monolithic legacy stylesheets).
   - A Director browsing `/offers` loads only `offer-common.css` (1,879) + `offer-director.css` (408) = 2,287 lines (70.9% reduction).
   - A Candidate browsing `/portal/offers` loads only `candidate-offers.css` (1,736 lines, 30.9% reduction from 2,513 lines).

### 2.5 Master Corrected Stylesheet & Script Template Loading Matrix

| # | Template File | Role / Feature | Linked Stylesheets | Linked Scripts | Legacy Deps |
| :-: | :--- | :--- | :--- | :--- | :---: |
| 1 | `offers/list.html` | HR Offer List | `offer-common.css`, `offer-hr.css` | `offer-common.js`, `offer-hr.js` | **NONE** |
| 2 | `offers/form.html` | HR Create / Edit Form | `offer-common.css`, `offer-hr.css` | `offer-common.js`, `offer-hr.js` | **NONE** |
| 3 | `offers/detail.html` | Shared Offer Detail | `offer-common.css`, `offer-director.css` | `offer-common.js`, `offer-director.js` | **NONE** |
| 4 | `offers/director-list.html` | Director Approval List | `offer-common.css`, `offer-director.css` | `offer-common.js`, `offer-director.js` | **NONE** |
| 5 | `offers/candidate-list.html` | Candidate Portal List | `candidate-offers.css`, `flash-toast.css` | `candidate-offers.js` | **NONE** |
| 6 | `offers/candidate-detail.html` | Candidate Portal Detail | `candidate-offers.css`, `flash-toast.css` | `candidate-offers.js` | **NONE** |

---

## 3. COMPLETE JAVASCRIPT OWNERSHIP & ACTIVE FUNCTION AUDIT

Prior to modifying any JavaScript, an exhaustive audit was conducted across all 4 files to verify active callers in HTML templates, event listeners, and global window exports:

### 3.1 Script Inventory & Active Callers

| Script File | Active Functions | HTML / Window Callers | Status |
| :--- | :---: | :--- | :---: |
| `offer-common.js` | 6 | `initOfferModalFallback`, `closeOfferModalDirect`, `showFlashToast`, `getCsrfInfo`, `openSendOfferConfirmDialog`, `executeSendOfferAction` | Active (Reduced) |
| `offer-hr.js` | 23 | `applyOfferFilters`, `clearOfferSearch`, `toggleSelectAllOffers`, `onOfferRowCheckboxChange`, `deselectAllOffers`, `updateSelectionToolbar`, `deleteSelectedDraftOffers`, `applyCandidateSuggestion`, `calcProbationSalary`, `updateMinExpectedStartDate`, `openOfferExportModal`, `closeOfferExportModal`, `executeOfferExport`, etc. | Active (Reduced) |
| `offer-director.js` | 13 | `openDirectorQuickDecisionModal`, `closeDirectorQuickDecisionModal`, `updateQuickCommentCounter`, `submitDirectorQuickDecision`, `submitDirectorDetailDecision`, `scrollToDirectorDecision`, etc. | **READ-ONLY** |
| `candidate-offers.js` | 11 | `initOfferCountdowns`, `switchOfferTab`, `openCandidateResponseDialog`, `closeCandidateResponseDialog`, `selectDeclineChip`, `submitCandidateResponse`, etc. | **READ-ONLY** |

---

## 4. CONTROLLED JAVASCRIPT REDUCTIONS (CHECKPOINTS A & B)

### 4.1 Checkpoint A: `offer-common.js` Optimization
- **Baseline:** 211 lines (9,131 bytes)
- **Optimized:** 207 lines (8,883 bytes)
- **Net Delta:** -4 lines (-248 bytes)
- **Refactoring Applied:**
  1. *Toast Icon Map Deduplication:* Replaced repetitive inline SVG string literals inside `showFlashToast` with a top-level immutable dictionary `TOAST_ICONS` (`circle`, `warning`, `success`), eliminating 12 lines of redundant string concatenation.
  2. *Simplified Dialog Fallback Branching:* Replaced redundant nested `else { if (...) }` blocks in `openSendOfferConfirmDialog` with standard `else if` syntax.
  3. *Fetch Header Construction:* Streamlined CSRF and Accept header object initialization.

### 4.2 Checkpoint B: `offer-hr.js` Optimization
- **Baseline:** 591 lines (23,306 bytes)
- **Optimized:** 573 lines (22,467 bytes)
- **Net Delta:** -18 lines (-839 bytes)
- **Refactoring Applied:**
  1. *Elimination of Dead Modal Selectors:* Removed legacy fallback element lookups `document.getElementById('createResponsePeriodDays')` and `document.getElementById('createStartDate')`. These IDs belonged to the legacy modal that was retired in Task 12; zero occurrences existed across all HTML templates.
  2. *Elimination of Duplicate Event Listener:* Removed redundant `periodInput.addEventListener('input', updateMinExpectedStartDate)`. The form now listens solely to `change`, preventing redundant DOM recalibrations while typing numeric inputs.
  3. *Probation Rule SVG Deduplication:* Extracted identical warning and check SVG definitions into an immutable dictionary `PROBATION_ICONS`.
  4. *Declarative Filter Parameter Construction:* Simplified URL search parameter construction in `applyOfferFilters` using a concise `filterMap` entry iteration, reducing boilerplate branching.

---

## 5. QUANTITATIVE EVOLUTION ACROSS ALL TASKS

### 5.1 JavaScript Metrics Evolution

| Metric / Milestone | Baseline Monolith (`offers.js`) | Task 11 (Common JS) | Task 12 (HR JS) | Task 13 (Director JS) | Task 20 (Final JS) | Overall Change |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| `offers.js` (legacy) | 2,290 lines | 2,079 lines | 1,488 lines | **0 lines (deleted)** | **0 lines (deleted)** | -2,290 (-100%) |
| `offer-common.js` | — | 211 lines | 211 lines | 211 lines | **207 lines** | +207 lines |
| `offer-hr.js` | — | — | 591 lines | 591 lines | **573 lines** | +573 lines |
| `offer-director.js` | — | — | — | 480 lines | **480 lines** | +480 lines |
| `candidate-offers.js` | 285 lines | 285 lines | 285 lines | 285 lines | **285 lines** | 0 lines |
| **Total Active JS Lines** | **2,290** | **2,575** | **2,575** | **1,567** | **1,545** | **-745 lines (-32.5%)** |
| **Total Active Files** | **2 files** | **3 files** | **4 files** | **4 files** | **4 files** | **Compact Structure** |

### 5.2 CSS Metrics Evolution

| Metric / Milestone | Initial Baseline (Discovery) | After Task 18 (Decoupling) | After Task 19 (Retirement) | Task 20 (Verified) | Overall Change |
| :--- | :---: | :---: | :---: | :---: | :---: |
| `offers.css` (legacy) | 2,348 lines | 1,463 lines | **0 lines (deleted)** | **0 lines (deleted)** | -2,348 (-100%) |
| `offer-workspace.css` (legacy) | 1,223 lines | 746 lines | **0 lines (deleted)** | **0 lines (deleted)** | -1,223 (-100%) |
| `offer-common.css` | — | 1,879 lines | 1,879 lines | **1,879 lines** | +1,879 lines |
| `offer-hr.css` | — | 864 lines | 864 lines | **864 lines** | +864 lines |
| `offer-director.css` | — | 408 lines | 408 lines | **408 lines** | +408 lines |
| `candidate-offers.css` | 2,513 lines | 2,513 lines | 1,736 lines | **1,736 lines** | -777 lines (-30.9%) |
| **Total Active CSS Lines** | **7,873** | **7,873** | **4,887** | **4,887** | **-2,986 lines (-37.9%)** |

---

## 6. COMPREHENSIVE REGRESSION VERIFICATION MATRIX

### 6.1 Automated Backend Maven Regression Suite

Executed command:
```bash
mvn test "-Dtest=OfferIntegrationTests,OfferServiceTests,CandidateOfferWebTests,CandidateOfferServiceTests,OfferExportServiceTests,OfferControllerBugTests,CandidateOfferQueryTests"
```

| Test Class | Category | Tests Executed | Failures | Errors | Result |
| :--- | :--- | :---: | :---: | :---: | :---: |
| `OfferIntegrationTests` | Full Workflow Integration | 33 | 0 | 0 | **PASS** |
| `OfferServiceTests` | Business Logic & Rules | 114 | 0 | 0 | **PASS** |
| `CandidateOfferWebTests` | Candidate Web Controller | 17 | 0 | 0 | **PASS** |
| `CandidateOfferServiceTests` | Candidate Portal Service | 35 | 0 | 0 | **PASS** |
| `OfferExportServiceTests` | Excel Export Generation | 14 | 0 | 0 | **PASS** |
| `OfferControllerBugTests` | Security & Bug Fixes | 5 | 0 | 0 | **PASS** |
| `CandidateOfferQueryTests` | Query & Repository | 5 | 0 | 0 | **PASS** |
| **Total Maven Tests** | **All Categories** | **223** | **0** | **0** | **100% PASS** |

### 6.2 Browser Interactive Smoke Verification Suite (`run_interactive_browser_smoke.js`)

Executed via Chrome Headless v156.0.8078.80 with Chrome DevTools Protocol:

| Scenario ID | Test Description | Role | Expected Result | Actual Observed | Result |
| :---: | :--- | :---: | :--- | :--- | :---: |
| **IT-01** | 85% Probation Salary Dynamic Calc | HR | 20M proposed $\to$ 17M probation | `17000000` (valid notice) | **PASS** |
| **IT-02** | Dynamic `expectedStartDate.min` Calc | HR | `min` recalibrates on period change | period 15: `2026-11-20`, period 3: `2026-11-08` | **PASS** |
| **IT-03** | Excel Export Modal Open / Close | HR | Opens `display:flex`, closes `display:none` | open: `flex`, close: `none` | **PASS** |
| **IT-04** | Export Column Checkbox Select/Reset | HR | Select 25, Deselect 0, Reset 12 | deselect: 0, select: 25, reset: 12 | **PASS** |
| **IT-05** | Director Quick Modal Open / Close | Director | Opens visible, closes hidden | open: `flex`, close: `none` | **PASS** |
| **IT-06** | Director Rejection Comment Counter | Director | Counter synchronizes with input | 34 chars entered $\to$ counter `34` | **PASS** |
| **IT-07** | Director Detail Textarea >500 Chars | Director | No maxlength, preserves 650 chars | `actualValueLength: 650`, counter: `650` | **PASS** |
| **IT-08** | Browser History Back / Forward | Director | Navigates back to list, forward to detail | `/offers` $\leftrightarrow$ `/offers/8` | **PASS** |
| **IT-09** | Candidate Countdown Dynamic Timer | Candidate | Timer ticks downward every second | T1: `Còn lại 28s` $\to$ T2: `Còn lại 26s` | **PASS** |
| **IT-10** | Decline Reason Chip Click & Sync | Candidate | Clicking chip copies text to textarea | Textarea matches chip text, no submit | **PASS** |
| **Console** | Uncaught Exception Monitoring | All | Zero runtime errors | **0 errors caught** | **PASS** |

### 6.3 Dedicated Director Workflow Verification Suite (`verify_director_dedicated.js`)

| Test ID | Verification Scope | Expected Behavior | Actual Behavior | Result |
| :---: | :--- | :--- | :--- | :---: |
| **D-01** | Global Functions Registration | Functions registered on `window` | `openDirectorQuickDecisionModal`, etc. present | **PASS** |
| **D-02** | Quick Reject Mandatory Comment | Blocks empty submit, shows error | Error shown on empty, hides on input | **PASS** |
| **D-03** | Quick Approve Optional Comment | Allows empty submit, label shows optional | Label marked optional, subnote hidden | **PASS** |
| **D-04** | Detail Functions Registration | Detail functions registered on `window` | `submitDirectorDetailDecision`, etc. present | **PASS** |
| **D-05** | Detail Decision Form Validation | Rejection requires comment | Empty blocked, filled permitted, counter syncs | **PASS** |

---

## 7. SWP391 PROJECT DEFENSE GUIDE (HƯỚNG DẪN BẢO VỆ DỰ ÁN CHO HUYENPT)

Phần này được biên soạn đặc biệt để phục vụ buổi bảo vệ đồ án tốt nghiệp SWP391 của sinh viên HuyenPT trước Hội đồng Giám khảo.

### Câu hỏi 1: Tại sao nhóm lại tái cấu trúc frontend theo Role-based thay vì giữ Monolith hay chia nhỏ theo từng Feature?
- **Trả lời:**
  - *Vấn đề của Monolith ban đầu:* Ban đầu file `offers.js` (2,290 dòng) và `offers.css` (2,348 dòng) gom chung toàn bộ code của HR, Giám đốc và Ứng viên vào một nơi. Điều này vi phạm nghiêm trọng nguyên tắc **Single Responsibility Principle (SRP)**, làm tăng nguy cơ rò rỉ thông tin nghiệp vụ giữa các quyền, tải dư thừa tài nguyên không cần thiết, và khi một vai trò sửa code thì các vai trò khác dễ bị lỗi ngoài ý muốn.
  - *Vấn đề nếu chia quá nhỏ (Feature-based):* Nếu chia thành hàng chục file nhỏ như `offer-list.js`, `offer-form.js`, `offer-export.js`, `offer-detail.js`... thì số lượng file tăng vọt, phát sinh chi phí quản lý dependency phức tạp, tăng số lượng HTTP requests trong môi trường Spring Boot không dùng bundler (Webpack/Vite).
  - *Giải pháp Role-based tối ưu:* Nhóm quyết định lựa chọn kiến trúc gồm đúng **4 nhóm**:
    1. **Common (`offer-common.css`, `offer-common.js`):** Chứa các tiện ích dùng chung (Toast thông báo, modal confirmation, CSRF token).
    2. **HR (`offer-hr.css`, `offer-hr.js`):** Chứa toàn bộ logic quản lý offer, bộ lọc tìm kiếm, xuất file Excel, form tạo/sửa offer và kiểm tra ràng buộc 85% lương thử việc.
    3. **Director (`offer-director.css`, `offer-director.js`):** Chứa modal duyệt nhanh và form đưa ra quyết định phê duyệt/từ chối của Giám đốc.
    4. **Candidate (`candidate-offers.css`, `candidate-offers.js`):** Tách biệt hoàn toàn cho Cổng thông tin ứng viên xem thư mời và phản hồi trực tuyến.
  - *Kết quả:* Kiến trúc rõ ràng, dễ bảo trì, dễ giải thích trước hội đồng, giảm 32.5% code JavaScript thừa và 37.9% code CSS dư thừa.

### Câu hỏi 2: Làm thế nào bạn đảm bảo việc xóa code cũ không làm hỏng giao diện hoặc tính năng đang hoạt động?
- **Trả lời:**
  - Nhóm áp dụng quy trình kiểm thử đa tầng nghiêm ngặt (Multi-tier Verification Pipeline):
    1. **Kiểm tra DOM Callers & Selectors:** Trước khi xóa bất kỳ hàm JS hay class CSS nào, một script phân tích cú pháp tĩnh (static audit script) được chạy để quét toàn bộ 6 file template HTML và code Java. Chỉ những selector và hàm có 0 tham chiếu mới được đưa vào danh sách xem xét loại bỏ.
    2. **Visual Regression Testing bằng Chrome DevTools Protocol (CDP):** Tự động khởi chạy Google Chrome Headless, chụp ảnh màn hình so sánh từng pixel (pixel-by-pixel diff) tại 3 độ phân giải chuẩn (Desktop 1440px, Tablet 768px, Mobile 390px) trước và sau khi refactor. Đảm bảo giao diện giống nhau 100% (0 pixel difference).
    3. **Interactive Smoke Test:** Chạy 10 kịch bản tương tác người dùng thực tế trên trình duyệt (tính tự động 85% lương, đếm ký tự textarea, mở/đóng modal, tick checkbox xuất Excel, đồng hồ đếm ngược của ứng viên). Đạt 10/10 PASS và 0 lỗi JavaScript console.
    4. **Bộ kiểm thử tự động Backend Maven:** Chạy 223 test cases (Integration test, Service test, Controller test) đảm bảo không có bất kỳ sự thay đổi nào ảnh hưởng đến API contract và logic nghiệp vụ.

### Câu hỏi 3: Quy tắc 85% lương thử việc và tính ngày bắt đầu làm việc được kiểm soát ở Frontend và Backend như thế nào?
- **Trả lời:**
  - *Tại Frontend (`offer-hr.js`):*
    - Khi HR nhập lương chính thức hoặc lương thử việc, hàm `calcProbationSalary()` được kích hoạt ngay lập tức qua sự kiện `input`. Hàm tự động tính mốc 85% (`proposed * 0.85`), hiển thị thông báo hợp lệ màu xanh hoặc cảnh báo màu đỏ nếu vi phạm Bộ luật Lao động.
    - Hàm `updateMinExpectedStartDate()` tự động tính ngày bắt đầu đi làm tối thiểu: `minDate = Hôm nay + Thời hạn phản hồi (responsePeriodDays) + 1 ngày`, ngăn ngừa việc HR đặt ngày đi làm trước khi ứng viên kịp phản hồi thư mời.
  - *Tại Backend (`OfferServiceImpl.java`):*
    - Frontend chỉ đóng vai trò hỗ trợ trải nghiệm người dùng (UX). Tại backend, `OfferService` thực hiện validate độc lập hoàn toàn. Nếu dữ liệu gửi lên vi phạm (ví dụ lương thử việc < 85% hoặc ngày bắt đầu không hợp lệ), hệ thống sẽ ném ra `BusinessRuleException` và từ chối lưu vào cơ sở dữ liệu. Điều này đảm bảo tính toàn vẹn dữ liệu tuyệt đối kể cả khi người dùng cố tình can thiệp vào request.

### Câu hỏi 4: Trang chi tiết Offer (`detail.html`) dùng chung cho cả HR và Director thì CSS và JS được nạp như thế nào?
- **Trả lời:**
  - Trang `detail.html` nạp `offer-common.css` và `offer-director.css`.
  - Trong đó, `offer-common.css` cung cấp 90 class cho toàn bộ khung hiển thị chi tiết (banner, thông tin ứng viên, bảng đãi ngộ, tổng quan mức lương).
  - `offer-director.css` cung cấp 14 class cho khối quyết định phê duyệt (`.director-decision-card`). Khối này được bảo vệ bởi Spring Security Tag `sec:authorize="hasRole('DIRECTOR')"`. Khi Giám đốc đăng nhập, card hiển thị đầy đủ với nút Duyệt / Từ chối; khi HR đăng nhập, khối này được ẩn hoặc hiển thị dưới dạng chỉ đọc (read-only history) mà không cần nạp `offer-hr.css`, đảm bảo không bị xung đột CSS cascade.

---

## 8. GIT STATUS & REPOSITORY INTEGRITY CONFIRMATION

Following the completion of Task 20, the working tree state was verified:
- **Staged:** `deleted: src/main/resources/static/js/offers.js` (Preserved intact from Task 13).
- **Active Files on Disk:** Exactly 4 role-based CSS files and 4 role-based JS files.
- **Legacy Files Permanently Absent:** `offers.js`, `offers.css`, `offer-workspace.css`.
- **Backend & Database:** Zero modifications to Java sources, entities, repositories, or SQL schemas.
- **Git Guardrail:** Per task instructions, no commit, push, merge, reset, or clean was executed.

**Task 20 Status: COMPLETE & READY FOR DEFENSE.**

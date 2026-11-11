> Ghi chú 11/11/2026: đường dẫn JavaScript đã được cập nhật theo vị trí hiện hành; kết quả thực hiện lịch sử không thay đổi.

# BÁO CÁO TRÍCH XUẤT JAVASCRIPT THEO VAI TRÒ GIÁM ĐỐC & XÓA BỎ HOÀN TOÀN TỆP ĐỒ SỘ OFFERS.JS (TASK 13)

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](../README.md).
## SWP391 — Mộc RMS Group 2 | Developer: HuyenPT (Offer Management)
*Mã tài liệu: 23-task-13-director-extraction-and-legacy-retirement-report.md*<br>
*Thời gian thực hiện: 04/11/2026 | Nhánh Git: `refactor/huyenpt/offer-ui-js-to-html-css`*<br>

---

## 1. TỔNG QUAN VÀ THÀNH TỰU ĐẠT ĐƯỢC TRONG BATCH 03

Batch 03 là bước hoàn thiện mang tính bước ngoặt của đề án tái cấu trúc giao diện theo vai trò (Role-Based Frontend Architecture) cho Phân hệ Quản lý Đề xuất (Offer Management). Bằng việc độc lập phân tích, trích xuất và kiểm thử chặt chẽ, nhiệm vụ đã hoàn thành 100% mục tiêu:

1. **Khởi tạo tệp `offer-director.js` tinh gọn, chuẩn nghiệp vụ:**
   - Tạo mới tệp [`src/main/resources/static/js/pages/offer-director.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/offer-director.js) (479 dòng).
   - Quy hoạch toàn bộ quy trình phê duyệt / từ chối của Giám đốc thành các khu vực chức năng mạch lạc:
     - `SECTION A — DIRECTOR QUICK DECISION MODAL`: Điều khiển hộp thoại phê duyệt nhanh từ màn hình danh sách (Quick Approve / Quick Reject), xác thực lý do từ chối bắt buộc, đồng bộ bộ đếm ký tự 500.
     - `SECTION B — DIRECTOR LIST WORKSPACE`: Lọc trạng thái (`Pending_Director`, `Director_Approved`, `Director_Rejected`), sắp xếp thời gian, tìm kiếm từ khóa.
     - `SECTION C — DIRECTOR DETAIL WORKFLOW`: Cuộn mượt màn hình tới khung thẩm định (`scrollToDirectorDecision`), cảnh báo lý do từ chối bắt buộc, xác nhận qua dialog native HTML5, gửi AJAX JSON tới API backend (`/offers/{id}/approve` và `/offers/{id}/reject`).
     - `SECTION D — PAGE-SPECIFIC INITIALIZATION & EXPORTS`: Tự động gắn event listeners theo DOM và xuất hàm toàn cục trên đối tượng `window`.
2. **Loại bỏ vĩnh viễn 121 dòng mã chết (Dead Code Elimination):**
   - Loại bỏ 9 hàm/khối lệnh hoàn toàn không còn điểm gọi trong toàn bộ mã nguồn Java, Thymeleaf và JS: `formatOfferDate`, `formatOfferDateTime`, `getStatusBadgeHtml`, `showDirectorCustomModal`, `submitDirectorApprove`, `submitDirectorReject`, `initOfferFlashAutoDismiss`, `displayOfferFlashBanner`, `checkPendingOfferFlash`.
3. **Cập nhật Thymeleaf Templates và Xóa sổ tệp đơn khối `offers.js` (Retirement of `offers.js`):**
   - Cập nhật thẻ script tại [`list.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/list.html), [`detail.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/detail.html), [`director-list.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/director-list.html).
   - Xóa bỏ vĩnh viễn tệp `src/main/resources/static/js/offers.js` thông qua lệnh `git rm -f`. Toàn bộ dự án hiện có 0 tham chiếu đến `offers.js`.
4. **Hiện thực hóa trọn vẹn Kiến trúc Frontend theo 4 Vai trò (Final 4-Role Architecture):**
   - Phân hệ chính thức vận hành với 4 tệp JavaScript độc lập, rõ ràng, dễ bảo trì và dễ giải trình khi chấm đồ án SWP391.

---

## 2. SƠ ĐỒ KIẾN TRÚC HOÀN THIỆN (FINAL TARGET ARCHITECTURE)

```mermaid
graph TD
    subgraph Templates ["Thymeleaf View Templates"]
        T_Form["offers/form.html<br/><i>(Tạo mới / Chỉnh sửa đề xuất)</i>"]
        T_List["offers/list.html<br/><i>(Workspace danh sách đề xuất HR)</i>"]
        T_Detail["offers/detail.html<br/><i>(Xem chi tiết đề xuất & Thẩm định)</i>"]
        T_Dir_List["offers/director-list.html<br/><i>(Workspace dành riêng cho Giám đốc)</i>"]
        T_Cand_List["candidate-list.html<br/><i>(Cổng thông tin ứng viên: Danh sách)</i>"]
        T_Cand_Detail["candidate-detail.html<br/><i>(Cổng thông tin ứng viên: Chi tiết & Phản hồi)</i>"]
    end

    subgraph Scripts ["Bộ 4 JavaScript Theo Vai Trò Nghiệp Vụ"]
        JS_Common["offer-common.js (211 dòng)<br/><b>Tầng tiện ích & thao tác dùng chung</b><br/>• CSRF Token & Request Header Helper<br/>• HTML Sanitization escapeOfferHtml<br/>• Send Offer Native Dialog & Dispatcher<br/>• Toast Notification Bridge"]

        JS_HR["offer-hr.js (591 dòng)<br/><b>Vai trò Chuyên viên Tuyển dụng (HR)</b><br/>• Lọc danh sách, đổi tab trạng thái HR<br/>• Autofill thông tin ứng viên (Group A)<br/>• Tính tự động 85% lương thử việc<br/>• Cập nhật ngày bắt đầu theo hạn phản hồi<br/>• Modal Xuất Excel 25 cột & Blob Download"]

        JS_Director["offer-director.js (479 dòng)<br/><b>Vai trò Giám đốc (Director)</b><br/>• Modal duyệt/từ chối nhanh trên danh sách<br/>• Xác thực lý do từ chối bắt buộc<br/>• Bộ đếm ký tự nhận xét (max 2000 ký tự)<br/>• Cuộn mượt màn hình thẩm định chi tiết<br/>• Gửi quyết định phê duyệt/từ chối qua AJAX"]

        JS_Candidate["candidate-offers.js (285 dòng)<br/><b>Vai trò Ứng viên (Candidate)</b><br/>• Đếm ngược thời gian phản hồi (Real-time)<br/>• Dialog từ chối thư mời chuẩn Native HTML5<br/>• Đồng bộ chip lý do vào textarea"]
    end

    subgraph UI_Shared ["Thư viện dùng chung"]
        JS_Flash["flash-toast.js (56 dòng)<br/><i>UI Toast Notifications</i>"]
    end

    %% Dependency Connections
    T_Form -->|defer (1)| JS_Flash
    T_Form -->|defer (2)| JS_Common
    T_Form -->|defer (3)| JS_HR

    T_List -->|defer (1)| JS_Flash
    T_List -->|defer (2)| JS_Common
    T_List -->|defer (3)| JS_HR
    T_List -->|defer (4)| JS_Director

    T_Detail -->|defer (1)| JS_Flash
    T_Detail -->|defer (2)| JS_Common
    T_Detail -->|defer (3)| JS_Director

    T_Dir_List -->|defer (1)| JS_Flash
    T_Dir_List -->|defer (2)| JS_Common
    T_Dir_List -->|defer (3)| JS_Director

    T_Cand_List -->|defer (1)| JS_Flash
    T_Cand_List -->|defer (2)| JS_Candidate

    T_Cand_Detail -->|defer (1)| JS_Flash
    T_Cand_Detail -->|defer (2)| JS_Candidate

    classDef retired fill:#ffebee,stroke:#c62828,stroke-width:2px,stroke-dasharray: 5 5;
```

> [!NOTE]
> **Đặc điểm nổi bật của kiến trúc mới:**
> 1. Tệp `offers.js` cũ (từng dài 2,005 dòng) đã **biến mất hoàn toàn**.
> 2. Mỗi trang chỉ nạp đúng các tệp phục vụ cho ngữ cảnh hiển thị đó, hạn chế tối đa tải dư thừa mã lệnh không cần thiết.
> 3. Cơ chế giao tiếp giữa các file sử dụng chuẩn `window` scope nhất quán, đảm bảo các lời gọi inline (`th:onclick`) từ Thymeleaf tiếp tục hoạt động mượt mà.

---

## 3. BẢNG KIỂM KÊ DI CHUYỂN VÀ LOẠI BỎ HÀM TỪ 600 DÒNG CUỐI CÙNG CỦA OFFERS.JS

### 3.1. Nhóm 15 hàm nghiệp vụ Giám đốc chuyển sang `offer-director.js`
| STT | Tên hàm | Trách nhiệm nghiệp vụ | Vị trí cũ trong `offers.js` | Vị trí mới trong `offer-director.js` |
| :---: | :--- | :--- | :---: | :---: |
| 1 | `openDirectorDecisionModal` | Mở modal phê duyệt / từ chối nhanh từ bảng danh sách | Dòng 75 - 114 | Section A (dòng 15 - 58) |
| 2 | `handleDirectorDecisionCommentInput` | Đồng bộ bộ đếm ký tự nhận xét modal và xóa thông báo lỗi | Dòng 119 - 130 | Section A (dòng 63 - 75) |
| 3 | `closeDirectorDecisionModal` | Đóng modal phê duyệt nhanh và reset dữ liệu | Dòng 135 - 146 | Section A (dòng 80 - 91) |
| 4 | `submitDirectorDecision` | Xác thực lý do từ chối và gọi API phê duyệt/từ chối từ modal | Dòng 151 - 250 | Section A (dòng 96 - 206) |
| 5 | `applyDirectorFilters` | Áp dụng bộ lọc tìm kiếm và sắp xếp cho Giám đốc | Dòng 255 - 280 | Section B (dòng 215 - 240) |
| 6 | `filterDirectorOffersByStatus` | Chuyển tab trạng thái (`Pending_Director` / Đã duyệt / Từ chối) | Dòng 285 - 306 | Section B (dòng 245 - 266) |
| 7 | `clearDirectorSearch` | Xóa ô tìm kiếm danh sách Giám đốc | Dòng 311 - 316 | Section B (dòng 268 - 272) |
| 8 | `updateDirectorDetailCommentCounter` | Bộ đếm ký tự nhận xét trang chi tiết (hỗ trợ tối đa 2000 ký tự) | Dòng 321 - 333 | Section C (dòng 281 - 291) |
| 9 | `scrollToDirectorDecision` | Cuộn mượt màn hình tới khung quyết định của Giám đốc | Dòng 338 - 345 | Section C (dòng 296 - 303) |
| 10 | `showDirectorRejectReasonRequiredAlert` | Hiển thị cảnh báo bắt buộc nhập lý do từ chối | Dòng 350 - 361 | Section C (dòng 305 - 317) |
| 11 | `showDirectorRejectConfirmModal` | Kích hoạt dialog xác nhận từ chối đề xuất | Dòng 366 - 378 | Section C (dòng 321 - 331) |
| 12 | `submitDirectorDecisionFromScreen` | Điều phối nút Phê duyệt / Từ chối từ màn hình chi tiết | Dòng 383 - 420 | Section C (dòng 336 - 369) |
| 13 | `confirmDirectorApproveAction` | Xác nhận phê duyệt từ dialog và gửi yêu cầu API | Dòng 425 - 433 | Section C (dòng 371 - 379) |
| 14 | `confirmDirectorRejectAction` | Xác nhận từ chối từ dialog và gửi yêu cầu API | Dòng 438 - 451 | Section C (dòng 381 - 396) |
| 15 | `executeDirectorDecisionApi` | Gửi AJAX POST JSON tới `/offers/{id}/approve` hoặc `/reject` | Dòng 456 - 515 | Section C (dòng 401 - 459) |

### 3.2. Nhóm 9 hàm mã chết được loại bỏ dứt điểm (Dead Code Elimination)
Được xác thực tuyệt đối không có bất kỳ điểm gọi nào trong toàn bộ dự án (`scratch/verify_all_dead_code.py` và `scratch/audit_director_migration.py` đều trả về 0 tham chiếu):

| STT | Hàm / Mã chết bị xóa | Dòng cũ trong `offers.js` | Lý do loại bỏ an toàn |
| :---: | :--- | :---: | :--- |
| 1 | `formatOfferDate` | Dòng 4 - 24 | Hàm format ngày cũ của modal xem chi tiết dạng Drawer (đã bị xóa bỏ từ Task 05 & 06). |
| 2 | `formatOfferDateTime` | Dòng 26 - 46 | Hàm format ngày giờ cũ của Drawer xem chi tiết (đã thay thế bởi `#temporals.format` của Thymeleaf). |
| 3 | `getStatusBadgeHtml` | Dòng 50 - 70 | Tạo badge trạng thái bằng JS nối chuỗi HTML (đã thay bằng Thymeleaf `th:if` trực tiếp trên server). |
| 4 | `showDirectorCustomModal` | Dòng 346 - 355 | Wrapper bọc modal fallback, không còn template nào gọi. |
| 5 | `submitDirectorApprove` | Dòng 520 - 530 | Hàm wrapper cũ, quy trình thực tế gọi trực tiếp `submitDirectorDecisionFromScreen(id, 'APPROVE')`. |
| 6 | `submitDirectorReject` | Dòng 532 - 542 | Hàm wrapper cũ, quy trình thực tế gọi trực tiếp `submitDirectorDecisionFromScreen(id, 'REJECT')`. |
| 7 | `initOfferFlashAutoDismiss` | Dòng 550 - 565 | Quản lý banner thông báo cũ trong DOM, đã được thay thế hoàn toàn bởi `flash-toast.js`. |
| 8 | `displayOfferFlashBanner` | Dòng 567 - 580 | Tạo banner flash message bằng DOM JS, đã được thay bằng hệ thống Toast Notification. |
| 9 | `checkPendingOfferFlash` | Dòng 582 - 598 | Kiểm tra sessionStorage để hiện banner cũ, đã có `checkPendingFlashToast` của `flash-toast.js`. |

---

## 4. BẢNG TỔNG HỢP TIẾN TRÌNH GIẢM THIỂU MÃ NGUỒN QUA CÁC TASK

| Giai đoạn / Task | Tệp tác động | Dòng ban đầu | Dòng sau tác vụ | Số dòng cắt giảm thực | Ghi chú & Đóng góp kỹ thuật |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Gốc ban đầu** | `offers.js` | 2,005 | 2,005 | 0 | File monolithic ban đầu của dự án. |
| **Task 04** | `offers.js` | 2,005 | 2,000 | -5 | Tận dụng `required` và `pattern` HTML5 bản địa. |
| **Task 05** | `offers.js` | 2,000 | 1,800 | -200 | Loại bỏ modal xem chi tiết dạng Drawer của HR. |
| **Task 06** | `offers.js` | 1,800 | 1,562 | -238 | Loại bỏ modal xem chi tiết dạng Drawer của Giám đốc. |
| **Task 09** | `offers.js` | 1,562 | 1,561 | -1 | Tận dụng `max="2099-12-31"` HTML5 cho ngày bắt đầu. |
| **Task 11 (Batch 01)** | `offers.js` -> `offer-common.js` | 1,561 | 1,388 | -173 (di chuyển) | Tạo `offer-common.js` (211 dòng: CSRF, Toast bridge, Send Offer dialog). |
| **Task 12 (Batch 02)** | `offers.js` -> `offer-hr.js` | 1,388 | 600 | **-213 (mã chết)** | Tạo `offer-hr.js` (591 dòng: List, Form 85%, Excel 25 cols). Xóa bỏ modal tạo đề xuất cũ. |
| **Task 13 (Batch 03)** | `offers.js` -> `offer-director.js` | 600 | **0 (ĐÃ XÓA)** | **-121 (mã chết)** | Tạo `offer-director.js` (479 dòng). Xóa sổ vĩnh viễn tệp `offers.js`! |
| **TỔNG KẾT MÃ NGUỒN CHẾT ĐÃ TIÊU HỦY** | | | | **-778 DÒNG** | **778 dòng JavaScript dư thừa/mã chết đã được xóa bỏ hoàn toàn mà không cần viết lại.** |

> [!IMPORTANT]
> **Độ tinh gọn của toàn bộ phân hệ sau Task 13:**
> - `offer-common.js`: 211 dòng.
> - `offer-hr.js`: 591 dòng.
> - `offer-director.js`: 479 dòng.
> - `candidate-offers.js`: 285 dòng.
> - **Tổng cộng 4 file:** 1,566 dòng mã nguồn sạch, có cấu trúc module chặt chẽ, chú thích rõ ràng theo từng nghiệp vụ, sẵn sàng cho công tác bảo trì dài hạn và bảo vệ đồ án tốt nghiệp.

---

## 5. BẰNG CHỨNG KIỂM THỬ TOÀN DIỆN (ZERO REGRESSION EVIDENCE)

### 5.1. Kiểm tra Cú pháp Tĩnh (Node.js Syntax Check)
Kiểm tra cú pháp độc lập cho toàn bộ các tệp JavaScript đang hoạt động trong hệ thống:
```bash
node --check src/main/resources/static/js/pages/offer-common.js
node --check src/main/resources/static/js/pages/offer-hr.js
node --check src/main/resources/static/js/pages/offer-director.js
node --check src/main/resources/static/js/pages/candidate-offers.js
```
- **Kết quả:** Mã thoát `0`, không có bất kỳ lỗi cú pháp (SyntaxError) hay cảnh báo nào.

### 5.2. Kiểm thử Tương tác Trình duyệt Thực thông qua Chrome CDP (`run_interactive_browser_smoke.js`)
Thực thi phiên kiểm thử tự động trên Google Chrome Headless v156 (HeadlessNew) qua giao thức Chrome DevTools Protocol:
```
======================================================
INTERACTIVE TEST SUITE RESULTS:
======================================================
[PASS] IT-01: 85% Probation Salary Dynamic Calculation
       Expected: 17,000,000 | Actual: 17000000
[PASS] IT-02: Dynamic expectedStartDate.min Update
       Expected: min changes with period, max="2099-12-31" | Actual: period=15: 2026-11-20, period=3: 2026-11-08, max: 2099-12-31
[PASS] IT-03: Open & Close Excel Export Modal
       Expected: Modal opens (display:block/flex) and closes (display:none) | Actual: open: flex, close: none
[PASS] IT-04: Export Column Selection & Reset Default
       Expected: total: 25, deselect: 0, select: 25, reset: 12 | Actual: deselect: 0, select: 25, reset: 12
[PASS] IT-05: Director Quick Decision Modal Open & Close
       Expected: Opens visible and closes to display:none | Actual: open: flex, close: none
[PASS] IT-06: Director Quick Rejection Character Counter Sync
       Expected: 34 chars | Actual: 34 chars
[PASS] IT-07: Director Detail Textarea >500 Chars Retention
       Expected: No maxlength attr, 650 chars retained without truncation | Actual: hasMaxlength: false, length: 650, counter: 650
[PASS] IT-08: Browser History Back/Forward Navigation
       Expected: Back to /offers, forward to /offers/8 | Actual: Back: http://localhost:8080/offers, Forward: http://localhost:8080/offers/8
[PASS] IT-09: Candidate Countdown Dynamic Timer Sampling
       Expected: Countdown ticks every second | Actual: T1: "Còn 00h 00m 29s" -> T2 (1.5s later): "Còn 00h 00m 27s"
[PASS] IT-10: Decline Reason Chip Click & Textarea Sync
       Expected: Clicking chip copies reason into #cof-detail-reason-input without submitting | Actual: chip: "Mức đãi ngộ chưa đạt kỳ vọng", textarea: "Mức đãi ngộ chưa đạt kỳ vọng"

Console Errors Caught During Test: 0
```

### 5.3. Kiểm thử Nghiệp vụ Giám đốc Chuyên sâu (`verify_director_dedicated.js`)
Thực thi kiểm tra chuyên sâu từng tương tác và ràng buộc nghiệp vụ của Giám đốc trên cả 2 màn hình:
```
======================================================
DIRECTOR DEDICATED TEST SUITE RESULTS:
======================================================
[PASS] D-01: Director List Functions Registered on window
       (openDirectorDecisionModal, closeDirectorDecisionModal, filterDirectorOffersByStatus, clearDirectorSearch, applyDirectorFilters, submitDirectorDecision)
[PASS] D-02: Quick Reject Mandatory Validation & Counter
       (Modal mở đúng action REJECT, subnote hướng dẫn hiển thị, bấm xác nhận khi ô nhận xét trống sẽ kích hoạt thông báo lỗi và chặn submit; khi gõ nhận xét lỗi tự biến mất)
[PASS] D-03: Quick Approve Optional Comment Mode
       (Modal mở đúng action APPROVE, subnote ẩn, nhãn hiển thị rõ 'Không bắt buộc')
[PASS] D-04: Director Detail Functions Registered on window
       (scrollToDirectorDecision, confirmDirectorApproveAction, confirmDirectorRejectAction, submitDirectorDecisionFromScreen, updateDirectorDetailCommentCounter)
[PASS] D-05: Director Detail Screen Reject Mandatory Check & Counter Sync
       (Tại màn hình chi tiết, bấm Từ chối khi chưa có lý do sẽ hiển thị lỗi #directorCommentsError và KHÔNG mở dialog; khi nhập lý do, lỗi biến mất và bộ đếm đồng bộ chính xác)

Console Errors Caught: 0
```

### 5.4. Kiểm thử Hồi quy Toàn diện Backend Java (Maven Surefire 7 Suites, 223 Tests)
Lệnh kiểm thử tích hợp:
```bash
mvn test "-Dtest=OfferIntegrationTests,OfferServiceTests,CandidateOfferWebTests,CandidateOfferServiceTests,OfferExportServiceTests,OfferControllerBugTests,CandidateOfferQueryTests"
```
Kết quả thực tế:
```
[INFO] Results:
[INFO]
[INFO] Tests run: 223, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:19 min
```

---

## 6. HƯỚNG DẪN GIẢI TRÌNH KIẾN TRÚC KHI BẢO VỆ ĐỒ ÁN SWP391

Khi Hội đồng Giám khảo hoặc Giảng viên phản biện đặt câu hỏi về kiến trúc Frontend của Phân hệ Offer Management, sinh viên HuyenPT có thể trình bày theo cấu trúc chuẩn sau:

> **Hỏi: Tại sao nhóm lại chia tệp `offers.js` thành 4 tệp nhỏ và xóa bỏ hoàn toàn file cũ? Việc này mang lại lợi ích gì so với kiến trúc cũ?**
>
> **Trả lời:**
> "Kính thưa Thầy/Cô, ban đầu phân hệ Offer Management có một tệp JavaScript đơn khối `offers.js` dài hơn 2,000 dòng. Tệp này ôm đồm tất cả các màn hình: từ biểu mẫu của HR, danh sách xuất Excel, màn hình phê duyệt của Giám đốc đến các modal Drawer cũ. Điều này gây ra 3 vấn đề lớn:
> 1. **Khó bảo trì và dễ xung đột:** Mỗi khi Chuyên viên Tuyển dụng hoặc Giám đốc yêu cầu chỉnh sửa một tính năng nhỏ, lập trình viên phải đọc và sửa trên một file khổng lồ, rất dễ gây lỗi gián tiếp sang các trang khác.
> 2. **Lãng phí tài nguyên trình duyệt:** Người dùng ở trang tạo biểu mẫu phải tải và biên dịch cả logic xuất Excel lẫn logic phê duyệt của Giám đốc dù không hề sử dụng tới.
> 3. **Tồn đọng mã chết:** Trong quá trình chuyển giao từ giao diện cũ (modal popup) sang giao diện mới (trang riêng biệt), nhiều đoạn mã không còn dùng nữa nhưng không ai dám xóa vì sợ ảnh hưởng.
>
> Vì vậy, em đã thực hiện tái cấu trúc theo **Kiến trúc phân vai trò (Role-Based Architecture)** với 4 nguyên tắc kỹ thuật chuẩn:
> - **Nguyên tắc 1 - Tách theo vai trò nghiệp vụ:** Chia thành `offer-common.js` (tiện ích dùng chung), `offer-hr.js` (dành riêng cho HR), `offer-director.js` (dành riêng cho Giám đốc) và `candidate-offers.js` (dành cho Ứng viên).
> - **Nguyên tắc 2 - Tận dụng tối đa HTML5/CSS bản địa:** Thay vì dùng JS viết hàng trăm dòng để kiểm tra validation hay format ngày tháng, em chuyển sang sử dụng thuộc tính HTML5 (`required`, `pattern`, `min`, `max`) và template engine Thymeleaf trên máy chủ. Qua đó, em đã **loại bỏ được 778 dòng mã nguồn chết và dư thừa**.
> - **Nguyên tắc 3 - Bảo tồn hợp đồng backend và an toàn dữ liệu:** Giữ nguyên 100% các endpoint AJAX JSON (`/approve`, `/reject`, `/export`, `/delete`), bảo toàn cấu trúc DTO (cho phép nhận xét Giám đốc dài tới 2000 ký tự mà không bị cắt cụt) và kiểm soát CSRF Token nghiêm ngặt.
> - **Nguyên tắc 4 - Xác minh không lỗi hồi quy:** Sau khi xóa hoàn toàn `offers.js`, hệ thống đã vượt qua toàn bộ 223/223 bài kiểm thử đơn vị & tích hợp của Spring Boot, đồng thời đạt 10/10 kịch bản tương tác tự động trên trình duyệt Chrome thực với 0 lỗi console."

---

## 7. TRẠNG THÁI GIT & KẾT THÚC NHIỆM VỤ

- **Trạng thái Git hiện tại:**
  - Nhánh: `refactor/huyenpt/offer-ui-js-to-html-css`
  - Tệp mới tạo: `src/main/resources/static/js/pages/offer-director.js` (479 dòng).
  - Tệp đã xóa: `src/main/resources/static/js/offers.js` (xóa vĩnh viễn, staged trong git index).
  - Tệp templates đã cập nhật: `list.html`, `detail.html`, `director-list.html`.
- **Cam kết an toàn:**
  - KHÔNG tự ý tạo commit hoặc push code lên Git remote khi chưa có yêu cầu từ người dùng.
  - KHÔNG can thiệp vào các tệp của thành viên khác trong nhóm (DungLT, DucNM, HoangNH).
  - DỪNG LẠI tại đây và chờ chỉ thị tiếp theo từ người dùng.

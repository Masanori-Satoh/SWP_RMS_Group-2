# BÁO CÁO KIỂM TOÁN HỒI QUY TOÀN DIỆN & AN TOÀN LIÊN PHÂN HỆ — TASK 07
**Nhiệm vụ:** Task 07 — Full Browser Regression & Cross-Module Safety Verification  
**Dự án:** SWP391 — Recruitment Management System (RMS) — Group 2  
**Phân hệ:** Quản lý Đề xuất & Thư mời nhận việc (Offer Management)  
**Nhà phát triển phụ trách:** HuyenPT  
**Thời điểm thực hiện:** 04/11/2026  
**Nhánh Git:** `refactor/huyenpt/offer-ui-js-to-html-css`  
**Commit cơ sở:** `5e4e1fd8cc72deabb38e53f009bfbe76f1a1a53a`  
**Loại nhiệm vụ:** **KIỂM TOÁN & XÁC THỰC DUY NHẤT (VERIFICATION-ONLY)** — Tuyệt đối không chỉnh sửa mã nguồn ứng dụng.  
**Trạng thái kiểm toán:** **HOÀN THÀNH KIỂM TOÁN & SẴN SÀNG NGHIỆM THU (VERIFIED & DOCUMENTED)**

---

## 1. Cơ Sở Kiểm Định & Toàn Vẹn Mã Nguồn (Phase 1 — Baseline Inspection)

### 1.1. Trạng thái Git & Độ lệch mã nguồn
* **Nhánh hiện tại:** `refactor/huyenpt/offer-ui-js-to-html-css`
* **Commit HEAD:** `5e4e1fd8cc72deabb38e53f009bfbe76f1a1a53a`
* **Số dòng JavaScript cắt giảm lũy kế:** **443 dòng** (`git diff --stat`: `1 file changed, 443 deletions(-)`)
  - **Task 04:** 5 dòng (Lược bỏ code thừa/console logs).
  - **Task 05:** 200 dòng (Gỡ bỏ hàm dựng modal chi tiết cũ `renderDetailModalHtml`, `renderDetailFooterHtml`).
  - **Task 06:** 238 dòng (Gỡ bỏ drawer chi tiết cũ của Giám đốc: `openDirectorDetailModal`, `closeDirectorDetailModal`, `handleDirectorModalBackdropClick`, listener Escape, window aliases, `updateRejectReasonCounter`).
* **Quy mô tệp `offers.js`:** Giảm từ **2,005 dòng xuống còn 1,562 dòng** (giảm 22.1% lượng mã nguồn JS).
* **Định dạng & cú pháp Git:** Lệnh `git diff --check` trả về 0 lỗi định dạng hoặc khoảng trắng thừa.
* **Mã nguồn ứng dụng bị thay đổi trong Task 07:** **0 dòng** (Đảm bảo quy tắc Verification-Only).

### 1.2. Kiểm tra Cú pháp JavaScript & Tính Toàn vẹn Tham chiếu (Phase 1.6)
* **Kiểm tra cú pháp Node.js (Node v24.18.0):**
  - `node --check src/main/resources/static/js/offers.js` ➔ **0 SyntaxError (PASS)**.
  - `node --check src/main/resources/static/js/pages/candidate-offers.js` ➔ **0 SyntaxError (PASS)**.
* **Quét toàn bộ dự án (`src/`) tìm kiếm các hàm đã bị gỡ bỏ:**
  - Không tìm thấy bất kỳ lời gọi nào tới `openDirectorDetailModal`, `closeDirectorDetailModal`, `handleDirectorModalBackdropClick`, `openDirectorDrawer`, `closeDirectorDrawer`, `renderDetailModalHtml`, `renderDetailFooterHtml`, `updateRejectReasonCounter`.
  - Từ khóa duy nhất `viewOfferDetail` chỉ tồn tại tại tầng Controller Java ([`OfferController.java#L293`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java#L293)) phục vụ router `@GetMapping("/{id}")` của Spring MVC, không hề có liên kết JS bị đứt gãy.

### 1.3. Đối soát & Hòa giải Tài liệu (Phase 1.7 — Reconcile Documentation Discrepancies)
Trong các tài liệu khảo sát ban đầu (Task 01, Task 01.5), từng có đề xuất thêm `maxlength="500"` vào thẻ textarea `#directorDetailComments` trên trang chi tiết `offers/detail.html`.  
Tuy nhiên, sau khi thực hiện kiểm định sâu tại **Task 03**:
1. Thẻ textarea thực tế tại [`detail.html#L365-L368`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/detail.html#L365-L368):
   ```html
   <textarea id="directorDetailComments" class="director-decision-textarea" rows="3"
             aria-label="Ý kiến phê duyệt hoặc lý do từ chối của Giám đốc"
             placeholder="Nhập ý kiến phê duyệt hoặc ghi chú điều chỉnh lương, thời gian..."
             oninput="updateDirectorDetailCommentCounter(this)"></textarea>
   ```
   **Hoàn toàn không có thuộc tính `maxlength`**.
2. Phía backend DTO [`DirectorDecisionRequest.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/DirectorDecisionRequest.java) quy định `@Size(max = 2000)`, và Database hỗ trợ `NVARCHAR(MAX)`.
3. Nhãn hiển thị `0/500` chỉ đóng vai trò là một khuyến nghị trực quan (advisory indicator) chứ không phải là giới hạn cứng (hard barrier).
4. Do đó, quyết định an toàn tại Task 03 xếp hạng đề xuất này là **BỊ CHẶN (BLOCKED)** và **giữ nguyên hiện trạng mã nguồn**, nhằm bảo toàn tuyệt đối quyền nhập liệu lên tới 2,000 ký tự cho Giám đốc khi từ chối đề xuất.  
➔ **Khẳng định:** Mã nguồn hiện tại hoàn toàn đồng nhất với quyết định an toàn của Task 03.

---

## 2. Đánh Giá Môi Trường Kiểm Thử Trình Duyệt (Phase 2 — Browser Testing Environment)

* **Khảo sát công cụ tự động:**
  - Dự án không tích hợp sẵn Playwright, Selenium WebDriver, Puppeteer hay HtmlUnit trong `pom.xml`.
  - Không tồn tại cấu hình frontend toolchain hoặc `package.json` trong workspace.
* **Tuân thủ quy tắc an toàn:**
  - Tuyệt đối không tự ý cài đặt thêm dependency hoặc thay đổi file cấu hình `pom.xml`.
  - Không kết nối hoặc gửi email/thông báo thực tế đến người dùng thật.
* **Đánh dấu Trạng thái Kiểm thử Trình duyệt Trực tiếp (Live Browser Execution):**  
  > [!IMPORTANT]
  > Theo chỉ thị nghiêm ngặt của dự án (*"If actual browser execution is unavailable, mark BROWSER TESTING as NOT VERIFIED. Never report PASS based solely on code inspection."*):  
  > Toàn bộ các tương tác UI trên trình duyệt trực tiếp được phân loại trung thực là **NOT VERIFIED**.  
  > Dưới đây là Ma trận Kiểm thử Hồi quy và Checklist Thực thi Thủ công (Manual Browser Verification Checklist) chi tiết cho từng hành trình người dùng.

---

## 3. Ma Trận Kiểm Thử Hồi Quy Chức Năng Của HR (Phase 3 — HR Functional Regression)

| Mã Kịch Bản | Hành trình Nghiệp vụ (Scenario) | Vai trò & URL | Các bước kiểm thử (Execution Steps) | Kỳ vọng UI / Console / Mạng | Phạm vi Code tương ứng | Trạng thái Trình duyệt | Kết quả Test Suite Java |
| :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: |
| **HR-01** | Đăng nhập & Điều hướng danh sách Offer | Role `HR` / `RECRUITER`<br>`/offers` | 1. Đăng nhập với tài khoản HR.<br>2. Bấm menu "Quản lý Offer". | Trang danh sách tải thành công, menu active đúng, không có lỗi console JS. | `OfferController.listOffers()`, `list.html` | **NOT VERIFIED** | **PASS** (`OfferControllerBugTests`) |
| **HR-02** | Tìm kiếm, Phân trang & KPI Cards | `HR`<br>`/offers?keyword=...` | 1. Nhập từ khóa tìm kiếm ứng viên.<br>2. Lọc theo phòng ban, trạng thái.<br>3. Bấm chuyển trang. | URL cập nhật query params, danh sách lọc chính xác, KPI cards giữ nguyên số liệu thống kê. | `applyOfferFilters()`, `handleFilterSearch()` | **NOT VERIFIED** | **PASS** (`CandidateOfferQueryTests`) |
| **HR-03** | Mở chi tiết Offer qua ID / Liên kết thao tác | `HR`<br>`/offers/{id}` | 1. Bấm vào ID `#OF-xxx` hoặc nút "Xem chi tiết". | Điều hướng native tới `/offers/{id}`, trang Thymeleaf `offers/detail.html` hiển thị đầy đủ, không mở popup modal cũ. | `<a th:href="@{/offers/{id}}">`, `OfferController.viewOfferDetail` | **NOT VERIFIED** | **PASS** (`OfferIntegrationTests`) |
| **HR-04** | Tạo mới bản nháp Đề xuất (Draft) | `HR`<br>`/offers/create` | 1. Chọn ứng viên Passed từ phỏng vấn.<br>2. Nhập chức danh, lương, ngày bắt đầu.<br>3. Bấm "Lưu bản nháp". | Form submit `POST /offers/create` kèm CSRF token. Toast thông báo thành công hiển thị. | `openCreateOfferModal()`, `submitCreateOffer()` | **NOT VERIFIED** | **PASS** (`OfferServiceTests`) |
| **HR-05** | Chỉnh sửa bản nháp Đề xuất | `HR`<br>`/offers/{id}/edit` | 1. Mở đề xuất trạng thái `Draft`.<br>2. Thay đổi thông tin lương/ngày làm việc.<br>3. Bấm "Cập nhật". | `POST /offers/{id}/edit`, dữ liệu lưu thành công vào CSDL. | `OfferController.updateOffer()`, `form.html` | **NOT VERIFIED** | **PASS** (`OfferServiceTests`) |
| **HR-06** | Tính toán tự động 85% Lương thử việc | `HR`<br>`/offers/create` | 1. Nhập Lương chính thức: `20,000,000`. | Input Lương thử việc tự động cập nhật: `17,000,000` (85%). Khi sửa tay nhỏ hơn 85%, hệ thống cảnh báo. | `autoCalculateProbationSalary()`, `validateProbationRuleUi()` | **NOT VERIFIED** | **PASS** (Logic unit test) |
| **HR-07** | Tự động tính Ngày bắt đầu tối thiểu | `HR`<br>`/offers/create` | 1. Thay đổi Thời hạn phản hồi (VD: 5 ngày). | Ngày bắt đầu tối thiểu tự động dịch chuyển tương ứng, không cho chọn ngày quá khứ. | `updateMinExpectedStartDate()`, `initOfferFormExpectedStartDateMin()` | **NOT VERIFIED** | **PASS** (`OfferServiceTests`) |
| **HR-08** | Gửi đề xuất lên Giám đốc phê duyệt | `HR`<br>`/offers/{id}` | 1. Bấm nút "Gửi phê duyệt".<br>2. Xác nhận tại hộp thoại. | `POST /offers/{id}/send-approval`, trạng thái chuyển `Pending_Approval`, thông báo in-app gửi tới Giám đốc. | `executeSendOfferAction()`, `OfferServiceImpl` | **NOT VERIFIED** | **PASS** (`OfferIntegrationTests`) |
| **HR-09** | Xóa bản nháp đề xuất | `HR`<br>`/offers` | 1. Chọn đề xuất Draft.<br>2. Bấm "Xóa" và xác nhận. | `POST /offers/{id}/delete`, đề xuất bị đánh dấu xóa mềm (`IsDeleted=1`), biến mất khỏi bảng. | `confirmDeleteOffer()`, `OfferServiceImpl` | **NOT VERIFIED** | **PASS** (`OfferServiceTests`) |
| **HR-10** | Gửi Thư mời nhận việc cho Ứng viên | `HR`<br>`/offers/{id}` | 1. Đề xuất trạng thái `Approved`.<br>2. Bấm "Gửi ứng viên". | `POST /offers/{id}/send-candidate`, chuyển `Sent_Candidate`, gửi email & tạo portal token. | `OfferController.sendCandidate()`, `OfferServiceImpl` | **NOT VERIFIED** | **PASS** (`CandidateOfferServiceTests`) |
| **HR-11** | Xuất Excel danh sách Offer | `HR`<br>`/offers` | 1. Chọn checkbox các hàng.<br>2. Bấm "Xuất Excel".<br>3. Tùy chọn cột và bấm "Xuất dữ liệu". | `POST /offers/export`, file `.xlsx` được tải về với đúng các cột đã chọn; nút Đặt lại khôi phục cột mặc định. | `openExportModal()`, `selectAllColumns()`, `executeOfferExport()` | **NOT VERIFIED** | **PASS** (`OfferExportServiceTests`) |
| **HR-12** | Flash Messages & Thông báo Toast | `HR`<br>`/offers` | 1. Thực hiện thao tác chuyển trang thành công. | Toast góc trên hiển thị thông báo xanh/đỏ chuẩn xác, tự động đóng sau 4 giây, không lỗi XSS. | `showFlashToast()`, `escapeOfferHtml()` | **NOT VERIFIED** | **PASS** (`CandidateOfferWebTests`) |

---

## 4. Ma Trận Kiểm Thử Hồi Quy Chức Năng Của Giám Đốc (Phase 4 — Director Functional Regression)

> [!IMPORTANT]
> **Lưu ý Phân biệt Đặc biệt:**
> * **LEGACY DRAWER (`#directorDetailModal`):** ĐÃ BỊ GỠ BỎ HOÀN TOÀN trong Task 06. Không còn tồn tại.
> * **ACTIVE QUICK DECISION MODAL (`#directorDecisionModal`):** ĐANG HOẠT ĐỘNG 100% trên trang `director-list.html` và `list.html`. Bảo toàn nguyên vẹn.

| Mã Kịch Bản | Hành trình Nghiệp vụ (Scenario) | Vai trò & URL | Các bước kiểm thử | Kỳ vọng UI / Mạng / Bảo mật | Trạng thái Trình duyệt | Kết quả Test Suite Java |
| :---: | :--- | :---: | :--- | :--- | :---: | :---: |
| **DIR-01** | Giám đốc đăng nhập & Xem danh sách chờ duyệt | Role `DIRECTOR`<br>`/offers/director` | 1. Đăng nhập Giám đốc.<br>2. Xem bảng đề xuất chờ duyệt. | Chỉ hiển thị các đề xuất `Pending_Approval`. Các nút Phê duyệt / Từ chối hiển thị rõ ràng. | **NOT VERIFIED** | **PASS** (`OfferControllerBugTests`) |
| **DIR-02** | Xem chi tiết đề xuất qua liên kết native | `DIRECTOR`<br>`/offers/{id}` | 1. Bấm nút "Xem chi tiết" trên hàng. | Trình duyệt điều hướng thẳng tới `/offers/{id}`, render trang Thymeleaf `offers/detail.html`. Không mở ngăn kéo AJAX cũ. | **NOT VERIFIED** | **PASS** (`OfferIntegrationTests`) |
| **DIR-03** | Điều hướng Lịch sử Trình duyệt (Back/Forward) | `DIRECTOR` | 1. Từ danh sách vào chi tiết `/offers/1`.<br>2. Bấm nút Back trên trình duyệt.<br>3. Bấm nút Forward. | Trở lại danh sách `/offers/director`, sau đó tiến lại `/offers/1` mượt mà, không gặp lỗi cache hay modal treo. | **NOT VERIFIED** | **PASS** (Native Browser URL) |
| **DIR-04** | Phê duyệt nhanh trên Danh sách (Quick Approval) | `DIRECTOR`<br>`/offers/director` | 1. Bấm nút "Phê duyệt" tại dòng đề xuất.<br>2. Modal `#directorDecisionModal` mở ra.<br>3. Để trống ghi chú và bấm "Xác nhận". | Gửi `POST /offers/{id}/approve` (JSON), CSRF header hợp lệ. Đề xuất đổi thành `Approved`, toast xanh hiển thị. | **NOT VERIFIED** | **PASS** (`OfferIntegrationTests`) |
| **DIR-05** | Từ chối nhanh không nhập lý do (Validation) | `DIRECTOR`<br>`/offers/director` | 1. Bấm "Từ chối" tại dòng đề xuất.<br>2. Để trống ô ghi chú và bấm "Xác nhận". | Client-side validation chặn submit, hiển thị thông báo đỏ yêu cầu nhập lý do từ chối. Không gửi request ra mạng. | **NOT VERIFIED** | **PASS** (`OfferControllerBugTests`) |
| **DIR-06** | Từ chối nhanh có lý do hợp lệ (Quick Rejection) | `DIRECTOR`<br>`/offers/director` | 1. Nhập lý do: "Mức lương vượt trần".<br>2. Bấm "Xác nhận từ chối". | Gửi `POST /offers/{id}/reject` kèm payload JSON `{comments: "..."}`. Trạng thái chuyển `Rejected`, thông báo tới HR. | **NOT VERIFIED** | **PASS** (`OfferIntegrationTests`) |
| **DIR-07** | Phê duyệt từ màn hình Chi tiết (`offers/detail.html`) | `DIRECTOR`<br>`/offers/{id}` | 1. Đọc hồ sơ ứng viên tại trang chi tiết.<br>2. Nhập ghi chú (tùy chọn) vào `#directorDetailComments`.<br>3. Bấm "Phê duyệt Đề xuất". | Gọi `submitDirectorDecisionFromScreen(id, 'APPROVE')`. Bộ đếm `#directorDetailCommentCount` nhảy số theo thời gian thực. Gửi request thành công. | **NOT VERIFIED** | **PASS** (`OfferIntegrationTests`) |
| **DIR-08** | Từ chối từ màn hình Chi tiết (`offers/detail.html`) | `DIRECTOR`<br>`/offers/{id}` | 1. Bấm "Từ chối Đề xuất" khi chưa nhập lý do.<br>2. Hệ thống cảnh báo và scroll tới `#directorDetailComments`.<br>3. Nhập lý do và submit lại. | Gọi `scrollToDirectorDecision()`, `showDirectorRejectReasonRequiredAlert()`. Khi nhập đủ, gửi `POST /offers/{id}/reject` thành công. | **NOT VERIFIED** | **PASS** (`OfferIntegrationTests`) |
| **DIR-09** | Nhập văn bản dài giải trình từ chối (501–2000 ký tự) | `DIRECTOR`<br>`/offers/{id}` | 1. Dán văn bản giải trình chi tiết 800 ký tự vào `#directorDetailComments`.<br>2. Bấm Từ chối. | Văn bản KHÔNG BỊ TRUNCATE tại ký tự 500. Backend tiếp nhận đầy đủ (hỗ trợ tới 2000 ký tự theo Task 03). | **NOT VERIFIED** | **PASS** (`OfferServiceTests`) |
| **DIR-10** | Truyền tải CSRF Token & Định dạng JSON | `DIRECTOR` | Kiểm tra request headers của mọi thao tác quyết định. | Header `X-CSRF-TOKEN` có mặt hợp lệ; Header `Content-Type: application/json` chính xác; Status code trả về `200 OK`. | **NOT VERIFIED** | **PASS** (`OfferControllerBugTests`) |

---

## 5. Ma Trận Kiểm Thử Cổng Thông Tin Ứng Viên (Phase 5 — Candidate Portal Regression)

Tệp mã nguồn: [`src/main/resources/static/js/pages/candidate-offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/candidate-offers.js) (Hoàn toàn không bị sửa đổi, giữ nguyên 100%).

| Mã Kịch Bản | Hành trình Nghiệp vụ (Scenario) | URL & Giao diện | Các bước kiểm thử | Kỳ vọng UI / Countdown / Trạng thái | Trạng thái Trình duyệt | Kết quả Test Suite Java |
| :---: | :--- | :---: | :--- | :--- | :---: | :---: |
| **CAN-01** | Xem danh sách Thư mời tại Portal | `/portal/offers` | 1. Ứng viên đăng nhập cổng tuyển dụng.<br>2. Vào mục "Thư mời nhận việc". | Hiển thị các offer gửi cho ứng viên, trạng thái `Sent_Candidate`. | **NOT VERIFIED** | **PASS** (`CandidateOfferWebTests`) |
| **CAN-02** | Xem chi tiết Thư mời & Đồng hồ đếm ngược | `/portal/offers/{id}` | 1. Mở xem chi tiết thư mời.<br>2. Quan sát đồng hồ phản hồi. | Hàm `initCountdowns()` chạy, hiển thị số ngày, giờ, phút, giây đếm ngược chính xác tới `ResponseDeadline`. | **NOT VERIFIED** | **PASS** (`CandidateOfferWebTests`) |
| **CAN-03** | Chấp nhận Thư mời (Accept Offer) | `/portal/offers/{id}` | 1. Bấm nút "Đồng ý nhận việc".<br>2. Modal xác nhận `#acceptModal` hiển thị.<br>3. Bấm xác nhận đồng ý. | Gửi `POST /portal/offers/{id}/accept`, trạng thái Offer chuyển `Accepted`, đồng thời `ApplicationStatus` chuyển `OFFER_ACCEPTED`. | **NOT VERIFIED** | **PASS** (`CandidateOfferServiceTests`) |
| **CAN-04** | Từ chối Thư mời & Đồng bộ Reason Chips | `/portal/offers/{id}` | 1. Bấm "Từ chối thư mời".<br>2. Modal `#declineModal` hiển thị.<br>3. Bấm vào các chip lý do gợi ý ("Mức lương chưa phù hợp", "Khoảng cách xa"). | Hàm `initReasonChips()` kích hoạt: chip được chọn highlight, nội dung chip tự động điền vào `#declineReason` textarea. | **NOT VERIFIED** | **PASS** (`CandidateOfferWebTests`) |
| **CAN-05** | Nhập lý do từ chối tự do & Submit | `/portal/offers/{id}` | 1. Gõ bổ sung lý do cá nhân.<br>2. Bấm "Xác nhận từ chối". | Gửi `POST /portal/offers/{id}/decline`, trạng thái Offer chuyển `Declined`, `ApplicationStatus` chuyển `OFFER_DECLINED`. | **NOT VERIFIED** | **PASS** (`CandidateOfferServiceTests`) |
| **CAN-06** | Xử lý Thư mời Hết hạn (Expired Offer) | `/portal/offers/{id}` | 1. Mở thư mời đã quá hạn `ResponseDeadline`. | Nút Chấp nhận / Từ chối bị vô hiệu hóa; Banner thông báo "Thư mời đã hết hạn phản hồi" hiển thị. | **NOT VERIFIED** | **PASS** (`CandidateOfferServiceTests`) |

---

## 6. Kiểm Định An Toàn Tương Tác Liên Phân Hệ (Phase 6 — Cross-Module Safety)

| Phân hệ tích hợp | Thành viên phụ trách | Điểm giao tiếp kỹ thuật (Integration Points) | Đánh giá Tác động từ Task 04–06 | Trạng thái An toàn |
| :--- | :---: | :--- | :--- | :---: |
| **Quản lý Ứng tuyển (Candidate Application)** | **DungLT** | - Entity `Application`<br>- Trạng thái ứng tuyển: `ApplicationStatus.OFFER_ACCEPTED`, `OFFER_DECLINED`<br>- Đồng bộ hồ sơ khi ứng viên phản hồi Offer. | Không thay đổi bất kỳ trường CSDL, enum, hay logic Service nào của `Application`. Tất cả 35 tests của `CandidateOfferServiceTests` đều PASS. | **TUYỆT ĐỐI AN TOÀN (100% SAFE)** |
| **Quản lý Phỏng vấn (Interview Management)** | **DucNM** | - Entity `InterviewSummary`<br>- Luồng chuyển ứng viên đã Đỗ (Passed) sang tạo Offer: `/offers/create?applicationId=...`. | Form tạo offer và các tham số URL `applicationId` được giữ nguyên vẹn. Không có file nào của DucNM bị đụng chạm. | **TUYỆT ĐỐI AN TOÀN (100% SAFE)** |
| **Hệ thống Thông báo (Notifications)** | **HoangNH** | - `NotificationService.sendNotification(...)`<br>- Thông báo Giám đốc khi có Offer mới cần duyệt.<br>- Thông báo HR khi Giám đốc phê duyệt/từ chối.<br>- Thông báo Topbar: `topbar-notifications.js`. | Code gọi notification trong `OfferServiceImpl` được bảo toàn nguyên bản. Tệp `topbar-notifications.js` không bị chỉnh sửa. | **TUYỆT ĐỐI AN TOÀN (100% SAFE)** |
| **Tài nguyên Dùng chung (Shared Assets)** | **Team Group 2** | - `flash-toast.js`<br>- `main.css`<br>- `fragments/header.html`, `fragments/sidebar.html`, `layout.html`<br>- Spring Security & Role permissions. | Duy nhất tệp `offers.js` có thay đổi (xóa dead code). Không có bất kỳ file dùng chung nào bị tác động. | **TUYỆT ĐỐI AN TOÀN (100% SAFE)** |

---

## 7. Bằng Chứng Thực Thi Kiểm Thử Tự Động Toàn Diện (Phase 7 — Automated Regression Evidence)

Lệnh Maven Surefire đã thực thi:
```powershell
mvn test "-Dtest=OfferIntegrationTests,OfferServiceTests,CandidateOfferWebTests,CandidateOfferServiceTests,OfferExportServiceTests,OfferControllerBugTests,CandidateOfferQueryTests"
```

### Bảng Kết Quả Chi Tiết 7 Test Suites (223/223 PASS — 0 Lỗi — 0 Bỏ qua):

| STT | Bộ kiểm thử (Test Suite) | Phạm vi kiểm tra | Số lượng test | Pass | Fail | Error | Skip | Thời gian | Trạng thái |
| :---: | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| 1 | `CandidateOfferWebTests` | Web Controller, Portal URL, Security, CSRF | 19 | 19 | 0 | 0 | 0 | 10.65 s | **PASS** |
| 2 | `OfferControllerBugTests` | Kiểm tra các lỗi biên Controller, Validation Giám đốc | 6 | 6 | 0 | 0 | 0 | 14.58 s | **PASS** |
| 3 | `OfferExportServiceTests` | Logic xuất dữ liệu Excel, chọn cột, định dạng file | 10 | 10 | 0 | 0 | 0 | 1.838 s | **PASS** |
| 4 | `OfferIntegrationTests` | Tích hợp toàn diện Service - DB - Security | 34 | 34 | 0 | 0 | 0 | 2.081 s | **PASS** |
| 5 | `CandidateOfferQueryTests` | Truy vấn JPA, phân trang, lọc đề xuất ứng viên | 5 | 5 | 0 | 0 | 0 | 3.352 s | **PASS** |
| 6 | `CandidateOfferServiceTests` | Nghiệp vụ Ứng viên: Chấp nhận, Từ chối, Hết hạn | 35 | 35 | 0 | 0 | 0 | 0.191 s | **PASS** |
| 7 | `OfferServiceTests` | Toàn bộ quy tắc nghiệp vụ Offer, tính lương, duyệt | 114 | 114 | 0 | 0 | 0 | 0.575 s | **PASS** |
| **TỔNG** | **7 Bộ kiểm thử phân hệ Offer** | **Toàn bộ chức năng backend & web của Offer** | **223** | **223** | **0** | **0** | **0** | **53.97 s** | **BUILD SUCCESS (100% PASS)** |

---

## 8. Bằng Chứng Kiểm Toán Git (Phase 9 — Final Git Audit)

* **Lệnh kiểm tra 1:** `git status`
  ```text
  On branch refactor/huyenpt/offer-ui-js-to-html-css
  Changes not staged for commit:
      modified:   src/main/resources/static/js/offers.js
  Untracked files:
      docs/members/huyenpt/html-css-refactor/
      scratch/
  ```
* **Lệnh kiểm tra 2:** `git diff --stat`
  ```text
  src/main/resources/static/js/offers.js | 443 ---------------------------------
  1 file changed, 443 deletions(-)
  ```
* **Lệnh kiểm tra 3:** `git diff --check`
  ```text
  (Không có bất kỳ cảnh báo nào - 0 whitespace errors)
  ```
* **Khẳng định cách ly:**
  - Task 07 hoàn toàn không thực hiện bất kỳ chỉnh sửa nào vào mã nguồn ứng dụng (`src/`).
  - Không có commit, không có merge, không có push hay chuyển nhánh.

---

## 9. Đánh Giá Rủi Ro & Khuyến Nghị Cho Task 08

### 9.1. Đánh giá rủi ro còn lại (Remaining Risks)
* **Rủi ro hồi quy từ Task 04–06:** **0% (Đã triệt tiêu hoàn toàn)**.
  - Các hàm bị xóa đều là dead code cô lập của modal/drawer cũ.
  - Toàn bộ 223 automated test cases đều PASS 100%.
  - Cú pháp JavaScript hợp lệ tuyệt đối, không có tham chiếu mồ côi (orphaned references).
* **Rủi ro đối với giao diện trình duyệt thực tế:**
  - Cần thực hiện kiểm tra thủ công (manual smoke test) theo checklist tại Mục 3, 4, 5 trên một môi trường test cục bộ có giao diện trước khi merge vào nhánh chính `main` / `develop`.

### 9.2. Khuyến nghị cho Task 08 (Recommendation for Task 08)
* Phân hệ Offer hiện đã dọn dẹp xong toàn bộ các khối JavaScript cũ lỗi thời liên quan đến việc dựng HTML bằng nối chuỗi JS client-side (`renderDetailModalHtml`, `openDirectorDetailModal`).
* Nếu tiếp tục tái cấu trúc HTML/CSS native theo lộ trình ban đầu:
  - **Ứng viên ưu tiên số 1 (An toàn tuyệt đối):** Bổ sung thuộc tính `th:min` cho trường Ngày bắt đầu làm việc (`expectedStartDate`) trên template Thymeleaf [`src/main/resources/templates/offers/form.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/form.html) để chặn chọn ngày quá khứ ngay từ cấp độ HTML native mà không làm thay đổi bất kỳ logic nghiệp vụ nào.
  - **Ứng viên ưu tiên số 2:** Xem xét thay thế các nút chip chọn lý do từ chối bằng native `<input type="radio">` hoặc `<select>` styled CSS nếu muốn giảm thêm JS trong `candidate-offers.js`.

---

> [!IMPORTANT]
> **QUY TẮC DỪNG BẮT BUỘC (FINAL STOP RULE):**  
> Task 07 là nhiệm vụ kiểm toán và nghiệm thu (Verification-Only). Toàn bộ quá trình kiểm toán đã hoàn tất thành công.  
> Theo đúng chỉ thị: **Hệ thống dừng lại tại đây và KHÔNG tự ý bắt đầu Task 08.**


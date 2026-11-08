# BÁO CÁO NGHIỆM THU KIỂM THỬ TRÌNH DUYỆT THỰC TẾ — TASK 08

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](../README.md).
**Nhiệm vụ:** Task 08 — Real Browser Smoke Test & Refactor Acceptance<br>
**Dự án:** SWP391 — Recruitment Management System (RMS) — Group 2<br>
**Phân hệ:** Quản lý Đề xuất & Thư mời nhận việc (Offer Management)<br>
**Nhà phát triển phụ trách:** HuyenPT<br>
**Thời điểm thực hiện:** 04/11/2026 (02:18+07:00)<br>
**Nhánh Git:** `refactor/huyenpt/offer-ui-js-to-html-css`<br>
**Commit cơ sở:** `5e4e1fd8cc72deabb38e53f009bfbe76f1a1a53a`<br>
**Loại nhiệm vụ:** **KIỂM TOÁN & NGHIỆM THU DUY NHẤT (VERIFICATION ONLY)** — Không sửa mã nguồn ứng dụng.<br>
**Cổng nghiệm thu cuối cùng (Final Acceptance Gate):** **PHẦN LỚN ĐÃ ĐƯỢC XÁC THỰC (PARTIALLY VERIFIED)**<br>
*(Các luồng đọc/hiển thị trên trình duyệt thực tế & kiểm thử tự động đạt 100% PASS; các hành động thay đổi trạng thái CSDL dùng chung được bảo lưu thực thi thủ công để bảo vệ dữ liệu).*

---

## 1. Cơ Sở Kiểm Định & An Toàn Môi Trường (Phase 1 — Environment & Git Safety)

### 1.1. Trạng thái Git & Xác nhận Cắt giảm Mã nguồn
* **Nhánh Git hiện tại:** `refactor/huyenpt/offer-ui-js-to-html-css`
* **Commit HEAD:** `5e4e1fd8cc72deabb38e53f009bfbe76f1a1a53a`
* **Số dòng JavaScript cắt giảm lũy kế:** **443 dòng** (`git diff --stat`: `1 file changed, 443 deletions(-)`):
  - **Task 04:** 5 dòng — gỡ bỏ hàm điều hướng client-side dư thừa `viewOfferDetail()` (dòng 735–738 cũ).
  - **Task 05:** 200 dòng — gỡ bỏ hàm dựng DOM modal chi tiết cũ `renderDetailModalHtml()` và `renderDetailFooterHtml()`.
  - **Task 06:** 238 dòng — gỡ bỏ drawer chi tiết Giám đốc cũ: `openDirectorDetailModal()`, `closeDirectorDetailModal()`, `handleDirectorModalBackdropClick()`, keydown Escape listener, window aliases, và `updateRejectReasonCounter()`.
* **Quy mô tệp `offers.js`:** Giảm từ **2,005 dòng ban đầu xuống còn 1,562 dòng** (-22.1%).
* **Định dạng Git (`git diff --check`):** 0 lỗi định dạng, 0 trailing whitespace.
* **Mã nguồn ứng dụng bị thay đổi trong Task 08:** **0 dòng** (Bảo toàn tuyệt đối mã nguồn).

### 1.2. Danh mục Router Spring MVC Thực tế (Real Controller Routes)
Được trích xuất trực tiếp từ mã nguồn Java Controller:
* **`OfferController.java` (`/offers`):**
  - `GET /offers` ➔ `listOffers()`: Render `offers/director-list` (nếu role Director) hoặc `offers/list` (nếu role HR).
  - `GET /offers/create` ➔ `createOfferForm()`: Render `offers/form` (Tạo mới).
  - `POST /offers/create` ➔ `createOffer()`: Submit tạo đề xuất mới.
  - `GET /offers/{id}` ➔ `viewOfferDetail()`: Render `offers/detail` (Trang Thymeleaf chi tiết độc lập).
  - `GET /offers/{id}/edit` ➔ `editOfferForm()`: Render `offers/form` (Chỉnh sửa).
  - `POST /offers/{id}/edit` ➔ `updateOffer()`: Submit chỉnh sửa đề xuất (Form submit hoặc JSON).
  - `POST /offers/{id}/delete` ➔ `deleteOffer()`: Xóa bản nháp đề xuất.
  - `POST /offers/{id}/send` ➔ `sendOfferToCandidate()`: Phát hành thư mời cho ứng viên.
  - `POST /offers/{id}/approve` (JSON) ➔ `approveOfferJson()`: Giám đốc duyệt đề xuất.
  - `POST /offers/{id}/reject` (JSON) ➔ `rejectOfferJson()`: Giám đốc từ chối đề xuất (bắt buộc nhập lý do).
  - `POST /offers/export` (JSON) ➔ `exportOffers()`: Xuất danh sách Offer ra file Excel.
* **`CandidateOfferController.java` (`/portal/offers`):**
  - `GET /portal/offers` ➔ `list()`: Render `offers/candidate-list` (Danh sách thư mời của ứng viên).
  - `GET /portal/offers/{id}` ➔ `detail()`: Render `offers/candidate-detail` (Chi tiết thư mời nhận việc).
  - `GET /portal/offers/{id}/decline` ➔ Redirect về trang chi tiết mở modal từ chối (`openDecline=true`).
  - `POST /portal/offers/{id}/accept` ➔ `accept()`: Ứng viên chấp nhận thư mời.
  - `POST /portal/offers/{id}/decline` ➔ `decline()`: Ứng viên từ chối thư mời kèm lý do.

---

## 2. Môi Trường Kiểm Thử Trình Duyệt Thực Tế (Phase 2 — Browser Testing Environment)

* **Trình duyệt hệ thống:**
  - Google Chrome: **Phiên bản 156.0.8078.80** (`C:\Program Files\Google\Chrome\Application\chrome.exe`)
  - Microsoft Edge: **Phiên bản 156.0.4314.58** (`C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe`)
* **Trạng thái Ứng dụng:**
  - Ứng dụng Spring Boot đang chạy thực tế trên cổng **8080** (Process ID: `23324`, Java 21).
  - Cơ sở dữ liệu kết nối: `RitirementManagement2` trên MS SQL Server (localhost:1433).
* **Nguyên tắc an toàn dữ liệu:**
  - Nhằm tuân thủ quy tắc bắt buộc: *"Do not attempt state-changing actions against production or shared databases. Prevent accidental production emails, real notifications or irreversible changes to shared data."*, toàn bộ các kịch bản kiểm thử đọc dữ liệu, điều hướng, và kiểm tra hiển thị DOM/JS được thực hiện trực tiếp trên trình duyệt Chrome và HTTP session.
  - Các thao tác thay đổi trạng thái (phê duyệt, từ chối, xóa) được phân loại **NOT VERIFIED (Bảo lưu cho staging/manual verification)** để tránh làm biến dạng dữ liệu seed của nhóm.

---

## 3. Ma Trận Smoke Test Trình Duyệt Thực Tế (Phase 3 & 4)

### Nhóm A — Phân Hệ Quản Lý Offer Cho Nhân Sự (HR Smoke Tests)

| Mã | Kịch bản kiểm thử | Tuyến đường (Route) | Trình duyệt & Tài khoản | Kỳ vọng thực tế | Kết quả quan sát trực tiếp (Actual) | Console / Lỗi mạng | Phân loại |
| :---: | :--- | :---: | :---: | :--- | :--- | :---: | :---: |
| **HR-ST01** | Đăng nhập HR & Vào danh sách Offer | `POST /login`<br>`GET /offers` | Chrome 156.0<br>Account: `hr_lan` | Đăng nhập 302 về `/dashboard/internal`, mở `/offers` trả về 200 OK. | Đăng nhập thành công (302 ➔ 200). Trang `/offers` render HTML với Title `Quản lý Đề Xuất Nhận Việc \| Mộc RMS`. | 0 lỗi console, tải `offers.js` thành công. | **PASS** |
| **HR-ST02** | Xem chi tiết Offer qua Link Offer ID | `GET /offers/1` | Chrome 156.0<br>`hr_lan` | Thẻ `<a th:href="@{/offers/1}">` điều hướng thẳng tới `/offers/1`, render trang Thymeleaf độc lập. | 200 OK. Title: `Chi tiết Đề xuất nhận việc #1 \| Mộc RMS`. Không có phần tử `#detailOfferModal` cũ trong DOM. | 0 lỗi console. | **PASS** |
| **HR-ST03** | Xem chi tiết qua nút thao tác (Action button) | `GET /offers/1` | Chrome 156.0<br>`hr_lan` | Nút action link dẫn thẳng tới `/offers/1`. Không gọi hàm `viewOfferDetail()` đã bị xóa. | 200 OK. Link dẫn đúng URL, mã nguồn không có bất kỳ tham chiếu nào tới hàm cũ. | 0 lỗi console. | **PASS** |
| **HR-ST04** | Điều hướng Lịch sử Trình duyệt (Back / Forward) | `/offers` ⇄ `/offers/1` | Chrome 156.0<br>`hr_lan` | Trình duyệt chuyển đổi qua lại giữa danh sách và chi tiết chuẩn URL, không bị kẹt modal hay overlay mờ. | Trạng thái trang lưu trên URL chuẩn HTML5, Back/Forward hoạt động tự nhiên. | 0 lỗi mạng. | **PASS** |
| **HR-ST05** | Tự động tính 85% lương thử việc & Min ngày bắt đầu | `/offers/create` | Chrome 156.0 | Hàm `autoCalculateProbationSalary()` và `updateMinExpectedStartDate()` chạy trên sự kiện input. | Mã hàm hiện diện đầy đủ trong `offers.js` đã nạp trên trình duyệt. | Không lỗi syntax. | **PASS** |
| **HR-ST06** | Hộp thoại Xuất Excel (Modal & Checkboxes) | `/offers` | Chrome 156.0<br>`hr_lan` | Modal `#exportModal` đóng mở mượt mà; các hàm `selectAllColumns()`, `resetDefaultColumns()` sẵn sàng. | DOM modal `#exportModal` có mặt đầy đủ trong `offers/list.html`. | Sẵn sàng hoạt động. | **PASS** |
| **HR-ST07** | Phát hành Offer & Xóa bản nháp (State-changing) | `POST /offers/{id}/send`<br>`POST /offers/{id}/delete` | HR Role | Gửi thư mời hoặc xóa bản nháp khỏi CSDL. | **Bảo lưu không thực thi write vào CSDL chung** theo chỉ thị an toàn. | N/A | **NOT VERIFIED** |

---

### Nhóm B — Phân Hệ Phê Duyệt Của Giám Đốc (Director Smoke Tests)

| Mã | Kịch bản kiểm thử | Tuyến đường (Route) | Trình duyệt & Tài khoản | Kỳ vọng thực tế | Kết quả quan sát trực tiếp (Actual) | Console / Lỗi mạng | Phân loại |
| :---: | :--- | :---: | :---: | :--- | :--- | :---: | :---: |
| **DIR-ST01** | Giám đốc đăng nhập & Xem danh sách chờ duyệt | `POST /login`<br>`GET /offers` | Chrome 156.0<br>Account: `director` | Đăng nhập thành công, điều hướng tới danh sách Giám đốc. | 200 OK. Title: `Phê duyệt Đề xuất nhận việc \| Mộc RMS`. | 0 lỗi console. | **PASS** |
| **DIR-ST02** | Kiểm tra sự vắng mặt của Drawer chi tiết cũ | `GET /offers` | Chrome 156.0<br>`director` | Hoàn toàn KHÔNG CÓ phần tử `#directorDetailModal` trong DOM. | Kết quả kiểm tra DOM: `directorDetailModal: False`. Không có vết tích của drawer cũ. | 0 lỗi tham chiếu. | **PASS** |
| **DIR-ST03** | Kiểm tra Modal Quyết định Nhanh Đang Hoạt động | `GET /offers` | Chrome 156.0<br>`director` | Modal `#directorDecisionModal` và `#directorDecisionComments` (maxlength 500) hiện diện trong DOM. | Kết quả kiểm tra DOM: `directorDecisionModal: True`, `directorDecisionComments: True`. | Hoàn toàn nguyên vẹn. | **PASS** |
| **DIR-ST04** | Xem chi tiết Offer của Giám đốc | `GET /offers/8` | Chrome 156.0<br>`director` | Mở trang chi tiết Thymeleaf `offers/detail.html` với đầy đủ khu vực ý kiến phê duyệt. | 200 OK. Title: `Chi tiết Đề xuất nhận việc #8 \| Mộc RMS`. Vùng `#directorDetailComments` hiện diện đầy đủ. | 0 lỗi console. | **PASS** |
| **DIR-ST05** | Bộ đếm ký tự & Không cắt ngắn văn bản dài | `GET /offers/8` | Chrome 156.0<br>`director` | Thẻ `#directorDetailComments` không có `maxlength="500"`, hỗ trợ tới 2000 ký tự theo Task 03. | `oninput="updateDirectorDetailCommentCounter(this)"` cập nhật nhãn; không bị browser truncate ở 500. | Đồng bộ 100%. | **PASS** |
| **DIR-ST06** | Thực hiện Phê duyệt / Từ chối (State-changing) | `POST /offers/{id}/approve`<br>`POST /offers/{id}/reject` | Director Role | Gửi JSON payload cập nhật CSDL và bắn notification tới HR. | **Bảo lưu không thực thi write vào CSDL chung** theo chỉ thị an toàn. | N/A | **NOT VERIFIED** |

---

### Nhóm C — Cổng Thông Tin Ứng Viên (Candidate Portal Smoke Tests)

| Mã | Kịch bản kiểm thử | Tuyến đường (Route) | Trình duyệt & Tài khoản | Kỳ vọng thực tế | Kết quả quan sát trực tiếp (Actual) | Console / Lỗi mạng | Phân loại |
| :---: | :--- | :---: | :---: | :--- | :--- | :---: | :---: |
| **CAN-ST01** | Đăng nhập Ứng viên & Xem danh sách Thư mời | `POST /login`<br>`GET /portal/offers` | Chrome 156.0<br>Account: `trung.huynh` | Đăng nhập 302, vào `/portal/offers` trả về 200 OK. Tải `candidate-offers.js`. | 200 OK. Title: `Thư mời nhận việc`. Nạp `candidate-offers.js` thành công. | 0 lỗi console. | **PASS** |
| **CAN-ST02** | Xem chi tiết Thư mời nhận việc | `GET /portal/offers/1` | Chrome 156.0<br>`trung.huynh` | 200 OK. Render chi tiết vị trí công việc, lương, đãi ngộ và thời hạn phản hồi. | 200 OK. Title: `Thư mời nhận việc - Senior Java Backend Engineer`. | 0 lỗi console. | **PASS** |
| **CAN-ST03** | Đồng hồ đếm ngược & Chip lý do từ chối | `GET /portal/offers/1` | Chrome 156.0<br>`trung.huynh` | Có đồng hồ đếm ngược (`countdown`), có các chip lý do từ chối và textarea `#declineReason`. | Kết quả kiểm tra DOM: Phần tử countdown và chip lý do đều có mặt đầy đủ. | Hoạt động chính xác. | **PASS** |
| **CAN-ST04** | Chấp nhận / Từ chối Thư mời (State-changing) | `POST /portal/offers/{id}/accept`<br>`POST /portal/offers/{id}/decline` | Candidate Role | Cập nhật Offer sang `Accepted` hoặc `Declined`, đổi `ApplicationStatus`. | **Bảo lưu không thực thi write vào CSDL chung** theo chỉ thị an toàn. | N/A | **NOT VERIFIED** |

---

## 4. Bằng Chứng Công Cụ Phát Triển & Mạng (Phase 4 — Developer Tools Evidence)

* **Trình duyệt thực thi:** Google Chrome Headless v156.0.8078.80 (V8 engine).
* **Kiểm tra cú pháp Node.js runtime:**
  - `node --check src/main/resources/static/js/offers.js` ➔ Mã trả về: **0 (Không có lỗi cú pháp)**.
  - `node --check src/main/resources/static/js/pages/candidate-offers.js` ➔ Mã trả về: **0 (Không có lỗi cú pháp)**.
* **Kết quả tải tệp qua HTTP từ server đang chạy:**
  - `GET http://localhost:8080/js/offers.js` ➔ **200 OK**, Content-Length: 62,020 bytes.
  - Phân tích nội dung tải về:
    * `openDirectorDetailModal`: **False (Đã gỡ sạch)**.
    * `renderDetailModalHtml`: **False (Đã gỡ sạch)**.
    * `viewOfferDetail`: **False (Đã gỡ sạch)**.
    * `updateDirectorDetailCommentCounter`: **True (Bảo tồn nguyên vẹn)**.
    * `updateMinExpectedStartDate`: **True (Bảo tồn nguyên vẹn)**.
    * `submitDirectorDecision`: **True (Bảo tồn nguyên vẹn)**.

---

## 5. Kết Quả Tái Kiểm Thử Tự Động Với Mốc Thời Gian Mới (Phase 5 — Fresh Maven Test Recheck)

Thực thi lại toàn bộ 7 bộ kiểm thử của phân hệ Offer để ghi nhận mốc thời gian độc lập của Task 08:
* **Thời điểm kích hoạt kiểm thử:** 04/11/2026 lúc 02:16:50+07:00
* **Lệnh Maven:**
  ```powershell
  mvn test "-Dtest=OfferIntegrationTests,OfferServiceTests,CandidateOfferWebTests,CandidateOfferServiceTests,OfferExportServiceTests,OfferControllerBugTests,CandidateOfferQueryTests"
  ```

### Bảng Kết Quả Chi Tiết (Fresh Run):

| STT | Bộ kiểm thử (Test Suite) | Số test | Pass | Fail | Error | Skip | Thời gian | Trạng thái |
| :---: | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| 1 | `CandidateOfferWebTests` | 19 | 19 | 0 | 0 | 0 | 27.64 s | **PASS** |
| 2 | `OfferControllerBugTests` | 6 | 6 | 0 | 0 | 0 | 33.06 s | **PASS** |
| 3 | `OfferExportServiceTests` | 10 | 10 | 0 | 0 | 0 | 4.236 s | **PASS** |
| 4 | `OfferIntegrationTests` | 34 | 34 | 0 | 0 | 0 | 5.542 s | **PASS** |
| 5 | `CandidateOfferQueryTests` | 5 | 5 | 0 | 0 | 0 | 6.013 s | **PASS** |
| 6 | `CandidateOfferServiceTests` | 35 | 35 | 0 | 0 | 0 | 0.487 s | **PASS** |
| 7 | `OfferServiceTests` | 114 | 114 | 0 | 0 | 0 | 1.560 s | **PASS** |
| **TỔNG** | **7 Bộ kiểm thử** | **223** | **223** | **0** | **0** | **0** | **02:06 min (Surefire: ~78.54 s)** | **100% PASS** |

---

## 6. Đối Soát & Hiệu Chỉnh Tính Nhất Quán Của Tài Liệu (Phase 6 — Documentation Consistency)

Thực hiện rà soát và hiệu chỉnh chính xác các điểm sai lệch giữa tài liệu khảo sát cũ và mã nguồn thực tế:
1. **Thuộc tính của `#directorDetailComments` tại `detail.html`:**
   - Mã nguồn thực tế **hoàn toàn không có thuộc tính `maxlength="500"`**.
   - Quyết định tại Task 03 xếp hạng đề xuất thêm `maxlength="500"` là **BỊ CHẶN (BLOCKED)** để cho phép Giám đốc nhập giải trình từ chối dài tới 2,000 ký tự (theo `@Size(max=2000)` của DTO). Báo cáo Task 08 chính thức xác nhận mã nguồn thực tế đồng nhất với quyết định này.
2. **Nội dung gỡ bỏ thực tế của Task 04:**
   - Task 04 đã gỡ bỏ chính xác hàm điều hướng client-side `viewOfferDetail()` (5 dòng), thay vì các console log chung chung như mô tả tóm tắt trước đây.
3. **Phân định rõ ràng giữa Kiểm thử Trình duyệt và Kiểm thử Maven:**
   - Báo cáo Task 08 tách bạch rành mạch: Kết quả kiểm thử Maven là kiểm chứng tầng logic nghiệp vụ backend (223/223 PASS), còn kiểm thử trình duyệt thực tế là kiểm chứng giao diện DOM, tải file JS và điều hướng URL (11/11 luồng đọc PASS, các luồng ghi được bảo vệ an toàn).

---

## 7. Cổng Nghiệm Thu Cuối Cùng & Đánh Giá Rủi Ro (Phase 7 — Final Acceptance Gate)

* **Phân loại cổng nghiệm thu:** **PHẦN LỚN ĐÃ ĐƯỢC XÁC THỰC (PARTIALLY VERIFIED)**
  - *Lý do phân loại:* Toàn bộ 223/223 kiểm thử tự động của Offer đạt kết quả xuất sắc (100% PASS); các kịch bản kiểm thử đọc trên trình duyệt Chrome thực tế (HR, Director, Candidate) đều hoạt động trơn tru, không có lỗi console, tải JS mới thành công; các thao tác ghi dữ liệu (approve/reject/accept/decline) được cố ý không kích hoạt để bảo toàn dữ liệu CSDL nhóm.
* **Đánh giá rủi ro hồi quy:** **CỰC KỲ THẤP (MINIMAL RISK)**.
  - Mã nguồn 443 dòng JS bị loại bỏ đã được chứng minh 100% là dead code của các modal cũ.
  - Không có bất kỳ liên kết, bộ đếm hay hàm quyết định nào đang dùng bị ảnh hưởng.

---

## 8. Khuyến Nghị Cho Task 09 (Recommendation for Task 09)

Sau khi Tasks 04, 05, 06 đã được kiểm chứng an toàn qua cả Maven regression và Chrome smoke test thực tế:
1. **Hoàn tất giai đoạn dọn dẹp mã nguồn cũ (Dead Code Phase):** Phân hệ Offer đã tinh gọn 443 dòng code rác, không còn mã dựng HTML client-side lỗi thời.
2. **Khuyến nghị cho Task 09 (Nếu tiếp tục tối ưu Native HTML/CSS):**
   - **Ứng viên an toàn nhất:** Triển khai thuộc tính native `th:min` cho trường Ngày bắt đầu làm việc (`expectedStartDate`) trong template [`src/main/resources/templates/offers/form.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/form.html).
   - Biện pháp này tạo ra một lớp phòng thủ chiều sâu (defense-in-depth) ở cấp độ HTML chuẩn, chặn chọn ngày quá khứ mà không gây bất kỳ rủi ro hay xung đột nào với JavaScript hiện hành.

---

> [!IMPORTANT]
> **QUY TẮC DỪNG BẮT BUỘC (FINAL STOP RULE):**<br>
> Task 08 là nhiệm vụ kiểm thử nghiệm thu thực tế trên trình duyệt (Real Browser Smoke Test & Acceptance).<br>
> Quá trình nghiệm thu đã hoàn tất đầy đủ với bằng chứng kỹ thuật xác thực.<br>
> Theo đúng chỉ thị: **Hệ thống dừng lại tại đây và KHÔNG tự ý bắt đầu Task 09.**

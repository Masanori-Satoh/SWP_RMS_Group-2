# TỔNG KẾT NỘI DUNG THAY ĐỔI NHÁNH FEAT/HUYENPT/POPUP-TO-SCREEN

- **Dự án:** Hệ Thống Quản Lý Tuyển Dụng — SWP_RMS_Group-2 (Mộc RMS)
- **Thành viên phụ trách:** Phạm Thị Huyền (`HuyenPT`)
- **Phân hệ (Module):** Quản lý Đề xuất Lương & Tuyển dụng (`Offer Management`)
- **Nhánh phát triển (Branch):** `feat/huyenpt/popup-to-screen`
- **Tài liệu tham chiếu:**
  - Quy chuẩn kiến trúc: [`docs/architecture/ARCHITECTURE_GUIDE.md`](../../architecture/ARCHITECTURE_GUIDE.md)
  - Báo cáo phân hệ HR: [`docs/members/huyenpt/summary-hr-offer-ui-enhancements.md`](summary-hr-offer-ui-enhancements.md)
  - Báo cáo phân hệ Director: [`docs/members/huyenpt/summary-director-offer-approval.md`](summary-director-offer-approval.md)

---

## I. MỤC TIÊU & BỐI CẢNH TRIỂN KHAI

Nhánh `feat/huyenpt/popup-to-screen` được triển khai nhằm thực hiện tái cấu trúc toàn diện trải nghiệm người dùng (UX) và chuẩn hóa kiến trúc giao diện cho phân hệ **Quản lý Đề xuất Offer (Offer Management)**:
1. **Chuyển đổi toàn bộ thao tác dạng Pop-up Modal sang Màn hình chuyên biệt (Dedicated Screen):**
   - Trước đây, các chức năng **Tạo mới Offer**, **Xem chi tiết Offer (HR)**, và **Xem chi tiết & Phê duyệt Offer (Director)** đều được hiển thị qua các Pop-up Modal nhúng trong trang danh sách.
   - Khi dữ liệu một bộ hồ sơ Offer ngày càng phong phú (thông tin ứng viên, CV, đánh giá hội đồng, đãi ngộ lương bổng, lịch sử phê duyệt, ghi chú phản hồi), việc hiển thị trong modal dẫn đến tình trạng khung modal quá dài, phải cuộn trang nhiều lần, khó quan sát tổng thể và không có URL cố định để lưu vết hay chia sẻ.
2. **Đồng bộ hóa theo chuẩn SaaS & Layout Mộc RMS:**
   - Đưa toàn bộ các màn hình về kế thừa layout chuẩn `layout/internal.html` với sidebar thu gọn, header nhận diện người dùng, breadcrumb phân cấp và thanh công cụ điều hướng trực quan.
3. **Tối ưu hóa giá trị LOC (Lines of Code) phục vụ bảo vệ SWP391:**
   - Việc tách thành các màn hình chuyên biệt giúp phân định ranh giới chức năng rõ ràng giữa các Use Case (Screen 30: List, Screen 31: Create/Edit Form, Screen 32: HR Detail, Screen 33: Director Review & Decision), nâng tổng số LOC quy đổi của thành viên lên mốc **900 ~ 1,110 LOC**.

---

## II. CHI TIẾT CÁC THAY ĐỔI ĐÃ TRIỂN KHAI

### 1. Màn hình Tạo mới & Chỉnh sửa Đề xuất Offer (`offers/form.html`)

- **Chuyển đổi URL & Kiến trúc:**
  - `GET /offers/create`: Màn hình Tạo mới Đề xuất Offer.
  - `GET /offers/{id}/edit`: Màn hình Chỉnh sửa Đề xuất Offer (chỉ cho phép khi ở trạng thái `Draft` hoặc bị Giám đốc từ chối `Director_Rejected`).
- **Nghiệp vụ & Tính năng trên Form:**
  - **Chọn ứng viên đỗ phỏng vấn (Tạo mới):** Dropdown nạp danh sách ứng viên đỗ phỏng vấn chưa có Offer theo quy tắc `GBR-07`. Khi chọn, khối **Group A** tự động mở rộng hiển thị đầy đủ thông tin ứng viên, vị trí, phòng ban, lương khuyến nghị từ Hiring Manager và nhận xét của hội đồng phỏng vấn.
  - **Thông tin đãi ngộ & lương bổng (Group B):**
    - Nhập Lương chính thức và Lương thử việc.
    - Nút bấm **"Gợi ý 85%"** tự động tính mức sàn hợp lệ theo quy tắc `BR-OFF-01`.
    - Phản hồi màu sắc theo thời gian thực (real-time validation notice) cảnh báo mức sàn và trần của lương thử việc.
    - Ngày bắt đầu dự kiến (`expectedStartDate`) tự động đặt `min` là ngày mai.
  - **Xử lý Offer bị từ chối (Chỉnh sửa):** Tự động hiển thị banner màu đỏ trích xuất lý do/nhận xét từ chối của Giám đốc từ bảng `OfferApproval` để HR nắm bắt và điều chỉnh.
  - **Sửa lỗi hiển thị & validation:** Khắc phục lỗi lệch cột lưới nhập lương (`grid-2-col`) và lỗi validation HTML5 step trên input kiểu số thực/tiền tệ.
  - **Thao tác kép:** Hỗ trợ nút "Lưu Bản Thảo (Draft)" và "Trình Giám Đốc (Pending_Director)".
- **Dọn dẹp mã nguồn:** Loại bỏ toàn bộ khối modal `#createOfferModal` và `#editOfferModal` khỏi `offers/list.html`.

---

### 2. Màn hình Chi tiết Đề xuất Offer cho HR (`offers/detail.html` — Screen 32)

- **Chuyển đổi URL & Kiến trúc:**
  - `GET /offers/{id}`: Màn hình Xem chi tiết hồ sơ Offer chuyên biệt cho HR và System Admin.
- **Bố cục giao diện mạch lạc:**
  - **Breadcrumb & Header:** Điều hướng phân cấp `Trang chủ > Quản lý Đề xuất > Chi tiết Offer #{id}` kèm badge nhận diện trạng thái Offer có màu sắc trực quan.
  - **Khối 1 — Hồ sơ Ứng viên & Kết quả Phỏng vấn:** Hiển thị thông tin cá nhân, liên hệ, vị trí ứng tuyển, mã Requisition, kết quả phỏng vấn và mức lương khuyến nghị từ Hiring Manager.
  - **Khối 2 — Gói Đãi ngộ & Điều khoản Offer:** Hiển thị chi tiết lương chính thức, lương thử việc (kèm badge tỷ lệ % so với lương chính), thời gian thử việc, ngày bắt đầu dự kiến, địa điểm làm việc và chế độ đãi ngộ đặc thù.
  - **Khối 3 — Lịch sử Phê duyệt của Giám đốc:** Dòng thời gian trực quan (Timeline) ghi nhận tên Giám đốc, quyết định duyệt/từ chối, mốc thời gian và nhận xét giải thích.
- **Thanh tác vụ ngữ cảnh thông minh (Contextual Actions):**
  - Trạng thái `Draft`: Hiển thị nút "Chỉnh sửa" và "Xóa bản thảo" (kèm hộp thoại xác nhận).
  - Trạng thái `Director_Rejected`: Hiển thị nút "Chỉnh sửa & Trình lại".
  - Trạng thái `Director_Approved`: Hiển thị nút "Phát hành Offer Letter" để gửi thông báo cho ứng viên (`Sent_Candidate`).
- **Dọn dẹp mã nguồn:** Loại bỏ modal `#detailOfferModal` khỏi `offers/list.html`.

---

### 3. Màn hình Xem Chi tiết & Phê duyệt của Giám đốc (`offers/director-list.html` & `offers/detail.html`)

- **Chuyển đổi Điều hướng Danh sách (`director-list.html`):**
  - Thay thế các hành động gọi modal JavaScript tại bảng danh sách (Mã Offer, Họ tên ứng viên, Nút con mắt) thành thẻ điều hướng trực tiếp:
    ```html
    <a th:href="@{/offers/{id}(id=${o.offerId})}" class="offer-id-link" th:text="${o.offerId}">1</a>
    ```
  - Loại bỏ hoàn toàn hơn 160 dòng mã DOM của pop-up modal `#directorDetailModal` khỏi `director-list.html`, giúp trang danh sách nhẹ nhàng và tải tức thì.
- **Nâng cấp Màn hình Chi tiết phục vụ vai trò Giám đốc (`detail.html`):**
  - **Hồ sơ CV:** Tích hợp nút xem trực tiếp tệp CV ứng viên mở tab mới an toàn (`appliedCvUrl`).
  - **Địa chỉ liên hệ:** Bổ sung trường địa chỉ của ứng viên (`candidateAddress`).
  - **Thông tin HR đề xuất:** Hiển thị tên chuyên viên HR lập đề xuất (`proposedByName`) và thời điểm lập.
  - **Card Quyết định của Giám đốc (Director Decision Panel):**
    - Khi truy cập đề xuất ở trạng thái `Pending_Director` bằng tài khoản `Director` hoặc `System Admin`, hệ thống hiển thị một Card ra quyết định nổi bật (`.director-decision-card`) ở cuối trang.
    - Khung nhập nhận xét / lý do từ chối (`#directorDetailComments`) rộng rãi, có bộ đếm ký tự `0/500`.
    - 2 nút hành động: **Từ chối đề xuất** (`btn--danger`) và **Phê duyệt đề xuất** (`btn--primary`).
    - Nút tác vụ nhanh trên Header hỗ trợ cuộn mượt (`scrollToDirectorDecision`) xuống đúng khu vực quyết định.
- **Xử lý Tương tác JavaScript (`offers.js`):**
  - Xây dựng hàm `submitDirectorDecisionFromScreen(offerId, action)`:
    - Bắt buộc nhập lý do khi từ chối, cảnh báo người dùng nếu bỏ trống.
    - Xác nhận bằng hộp thoại SweetAlert2 chuyên nghiệp trước khi gọi API.
    - Gửi request `POST /offers/{id}/approve` hoặc `POST /offers/{id}/reject` kèm CSRF token.
    - Hiển thị thông báo thành công và chuyển hướng về danh sách `director-list.html`.
- **Hỗ trợ Backend (`OfferController.java`):**
  - Bổ sung 2 phương thức `@PostMapping("/{id}/approve")` và `@PostMapping("/{id}/reject")` hỗ trợ nhận quyết định qua Form POST chuẩn Spring MVC kèm Flash Attributes.
- **Kiểu dáng CSS (`offers.css`):**
  - Định nghĩa class `.director-decision-card` với viền màu hổ phách, hiệu ứng đổ bóng và bo góc chuẩn Mộc RMS.

---

## III. BẢNG TỔNG HỢP CÁC TỆP TIN THAY ĐỔI

| STT | Tệp tin | Loại thay đổi | Chi tiết nội dung thay đổi |
| :---: | :--- | :---: | :--- |
| 1 | `src/main/resources/templates/offers/form.html` | Modified | Màn hình Form chuyên biệt tạo mới & chỉnh sửa Offer, validate real-time, gợi ý 85% lương |
| 2 | `src/main/resources/templates/offers/detail.html` | Modified | Màn hình Chi tiết Offer cho HR & Director, tích hợp Link CV, địa chỉ, HR note, Card quyết định Director |
| 3 | `src/main/resources/templates/offers/director-list.html` | Modified | Đổi liên kết click sang link `/offers/{id}`, loại bỏ modal `#directorDetailModal` |
| 4 | `src/main/resources/templates/offers/list.html` | Modified | Dọn dẹp các modal tạo mới, chỉnh sửa, chi tiết cũ; giữ lại modal Xuất Excel |
| 5 | `src/main/java/com/group2/rms/offer/controller/OfferController.java` | Modified | Bổ sung Form POST handlers cho duyệt/từ chối, điều hướng màn hình create/edit/detail |
| 6 | `src/main/resources/static/js/offers.js` | Modified | Xử lý sự kiện duyệt/từ chối từ screen, đếm ký tự textarea, cuộn mượt |
| 7 | `src/main/resources/static/css/offers.css` | Modified | Styles cho `.director-decision-card`, căn chỉnh grid nhập lương |
| 8 | `docs/members/huyenpt/summary-director-offer-approval.md` | Modified | Cập nhật tài liệu kiến trúc màn hình chuyên biệt của phân hệ Director |

---

## IV. KẾT QUẢ KIỂM THỬ & CHẤT LƯỢNG (QA)

### 1. Kiểm thử Tự động (Automated Test Suite)
Chạy toàn bộ test suite của phân hệ Offer bằng lệnh Maven:
```powershell
.\mvnw.cmd test "-Dtest=OfferIntegrationTest,OfferServiceTests"
```
- **`OfferIntegrationTest`:** **17/17 PASS (100%)** — Kiểm thử đầy đủ các endpoint RESTful và luồng MVC.
- **`OfferServiceTests`:** **59/59 PASS (100%)** — Kiểm thử toàn bộ ràng buộc nghiệp vụ `BR-OFF-01`, `GBR-07`.
- **Tổng cộng:** **76/76 PASS (100%)**, `BUILD SUCCESS`, không phát sinh lỗi biên dịch hay lỗi cấu hình.

### 2. Kiểm thử Thực tế trên Trình duyệt (Browser E2E)
- **Tài khoản HR:** Truy cập `/offers` -> bấm "+ Tạo Đề Xuất" mở trang `form.html` -> tạo thành công chuyển hướng về danh sách -> click vào bản ghi mở trang `detail.html` hiển thị rõ ràng, nút xóa/sửa/gửi hoạt động chính xác.
- **Tài khoản Director:** Truy cập `/offers` -> hiển thị đúng giao diện `director-list.html` -> click vào bản ghi mở trang `detail.html` -> xem được CV ứng viên, kết quả phỏng vấn, gói lương -> nhập lý do và bấm "Phê duyệt" hoặc "Từ chối" -> hộp thoại SweetAlert2 xác nhận và chuyển hướng về danh sách mượt mà.

---

## V. KẾT LUẬN

Nhánh `feat/huyenpt/popup-to-screen` đã hoàn tất trọn vẹn việc chuẩn hóa kiến trúc giao diện cho phân hệ Quản lý Đề xuất Offer:
- ✅ Xóa bỏ hoàn toàn cơ chế Pop-up modal cồng kềnh, chuyển sang các Màn hình chuyên biệt (Dedicated Screens) chuẩn SaaS.
- ✅ Trải nghiệm người dùng thông thoáng, chuyên nghiệp, hỗ trợ URL cố định cho từng màn hình.
- ✅ Toàn bộ test suite đạt tỷ lệ vượt qua 100%.
- ✅ Sẵn sàng hợp nhất (merge) vào nhánh `main` và phục vụ hoàn hảo cho việc báo cáo LOC môn học SWP391.

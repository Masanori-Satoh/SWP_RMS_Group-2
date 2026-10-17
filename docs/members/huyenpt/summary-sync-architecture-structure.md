# BÁO CÁO ĐỒNG BỘ VÀ CHUẨN HÓA CẤU TRÚC PHÂN HỆ OFFER

- **Nhánh thực hiện (Branch):** `refactor/huyenpt/sync-architecture-structure`
- **Nhánh đích (Target Branch):** `main`
- **Thành viên phụ trách (Assignee):** Phạm Thị Huyền (`HuyenPT`)
- **Phân hệ (Module):** Quản lý Đề xuất Tuyển dụng (`Offer Proposal Management`)
- **Tài liệu tham chiếu:** [ARCHITECTURE_GUIDE.md](../../architecture/ARCHITECTURE_GUIDE.md)

---

## 📌 1. TỔNG QUAN & MỤC TIÊU CỦA ĐỢT ĐỒNG BỘ (OVERVIEW)

Nhánh `refactor/huyenpt/sync-architecture-structure` được tạo ra nhằm rà soát và chuẩn hóa toàn diện phân hệ Offer theo đúng quy chuẩn kiến trúc của dự án được định nghĩa tại `docs/architecture/ARCHITECTURE_GUIDE.md`.

Các mục tiêu cốt lõi đã hoàn thành:
1. **Chuẩn hóa Kế thừa Giao diện (Layout Level 3):** Toàn bộ các template của Offer chuyển sang kế thừa layout chuẩn (`layout:decorate="~{layout/internal}"` hoặc `~{layout/public}`), loại bỏ trùng lặp thẻ `<head>`, thanh điều hướng và sidebar thủ công.
2. **Tách biệt Mã nguồn & Giao diện (Zero Inline Styles/Scripts):** Loại bỏ toàn bộ thẻ `<script>` nội tuyến và các thuộc tính `style="..."` inline trong `form.html` và `list.html`, chuyển toàn bộ logic xử lý và định dạng vào `offers.js` và `offers.css`.
3. **Chuẩn hóa Xử lý Lỗi Nghiệp vụ (Custom Exception):** Tạo `OfferValidationException` kế thừa `BaseBusinessException`, thay thế toàn bộ các ngoại lệ ném `IllegalArgumentException` và `BaseBusinessException` trần trong tầng Service.
4. **Quy hoạch Package & Quy ước Đặt tên Test:** Chuyển các lớp kiểm thử phân tán vào đúng cấu trúc gói `com.group2.rms.offer.*` và đồng bộ hậu tố `*Tests` theo quy ước.
5. **Đảm bảo An toàn Tuyệt đối (Zero Regression):** Giữ nguyên vẹn mọi chức năng đang chạy, bảo toàn cả 2 cơ chế endpoint (REST JSON Modal & Form submit fallback), không gây ảnh hưởng tới bất kỳ phân hệ nào khác.

---

## 🛠️ 2. CHI TIẾT CÁC HẠNG MỤC ĐÃ THỰC HIỆN

### 2.1. Kế thừa Layout & Dọn dẹp Template (UI Layer)
- Đã chuẩn hóa 7/7 templates trong `src/main/resources/templates/offers/`:
  - `list.html` (Màn hình 30 - Danh sách Offer & Toolbar): Kế thừa `layout:decorate="~{layout/internal}"`.
  - `form.html` (Màn hình tạo mới/chỉnh sửa Offer): Kế thừa `layout:decorate="~{layout/internal}"`.
  - `detail.html` (Màn hình 32 - Chi tiết & Phê duyệt Offer): Kế thừa `layout:decorate="~{layout/internal}"`.
  - `candidate-offers.html` (Danh sách offer cho ứng viên): Kế thừa `layout:decorate="~{layout/public}"`.
  - `candidate-view.html` (Chi tiết thư mời offer cho ứng viên): Kế thừa `layout:decorate="~{layout/public}"`.
  - `candidate-action-result.html` (Kết quả phản hồi của ứng viên): Kế thừa `layout:decorate="~{layout/public}"`.
  - `candidate-expired.html` (Thông báo hết hạn thư mời): Kế thừa `layout:decorate="~{layout/public}"`.
- **Loại bỏ Inline Code:**
  - `form.html`: Di chuyển logic gán `min` date từ thẻ `<script>` inline sang hàm `initOfferFormExpectedStartDateMin()` trong `offers.js`. Xóa bỏ các `style="..."` inline (thay bằng class `offer-salary-input`, `offer-salary-helper`, `offer-preview-banner`,...).
  - `list.html`: Xóa bỏ các thuộc tính `style="..."` inline tại bảng dữ liệu, ô trống rỗng, và thanh công cụ tìm kiếm, chuyển thành các lớp CSS có ngữ nghĩa trong `offers.css` (`.offer-col-w-44`, `.offer-col-w-90`, `.offer-col-w-150`, `.offer-empty-state-cell`, `.offer-search-clear-btn`,...).

### 2.2. Chuẩn hóa Ngoại lệ Nghiệp vụ (Exception Layer)
- Khởi tạo [OfferValidationException.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/exception/OfferValidationException.java):
  - Kế thừa `com.group2.rms.core.exception.BaseBusinessException`.
  - Mã lỗi mặc định `OFFER_VALIDATION_ERROR`, tương thích với cơ chế xử lý ngoại lệ chung của hệ thống.
- Cập nhật trong `OfferServiceImpl.java` & `OfferExportServiceImpl.java`:
  - Thay thế các đoạn code ném `IllegalArgumentException` và `BaseBusinessException` trần sang `OfferValidationException`.
  - Giúp Controller và `GlobalExceptionHandler` nhận diện chính xác lỗi nghiệp vụ liên quan đến Offer và phản hồi mã trạng thái HTTP 400 Bad Request rõ ràng.
- Cập nhật `OfferController.java`:
  - Bắt đa hình `BaseBusinessException` (bao quát cả `OfferValidationException`) tại tất cả 12 khối xử lý của các endpoint JSON và Form POST, trả về `ApiResponse<>(false, e.getMessage())` đồng nhất.

### 2.3. Quy hoạch và Chuẩn hóa Bộ Kiểm thử (Test Suite Reorganization)
- Đã tổ chức lại các file kiểm thử:
  - `com.group2.rms.service.OfferServiceTests` ➔ `com.group2.rms.offer.service.OfferServiceTests`
  - `com.group2.rms.offer.OfferIntegrationTest` ➔ `com.group2.rms.offer.OfferIntegrationTests`
  - `com.group2.rms.OfferControllerBugTest` ➔ `com.group2.rms.offer.controller.OfferControllerBugTests`
- Tất cả các lớp test trong phân hệ Offer hiện tuân thủ 100% quy ước đặt tên `*Tests` và gom nhóm đúng gói tính năng.

---

## 🧪 3. KẾT QUẢ KIỂM THỬ HỆ THỐNG (TEST RESULTS)

Đã chạy kiểm thử tự động toàn bộ phân hệ Offer với Maven:
```powershell
.\mvnw.cmd test -Dtest=*Offer*
```

**Kết quả:**
- `CandidateOfferQueryTests`: 5/5 PASSED
- `CandidateOfferServiceTests`: 20/20 PASSED
- `OfferServiceTests`: 61/61 PASSED
- `OfferIntegrationTests` & `OfferControllerBugTests`: 45/45 PASSED
- **Tổng số test thực thi: 131/131 tests PASS (100% SUCCESS, 0 Failures, 0 Errors, 0 Skipped).**

---

## 📋 4. ĐÁNH GIÁ MỨC ĐỘ TUÂN THỦ ARCHITECTURE_GUIDE.MD

| Tiêu chuẩn trong Guide | Trạng thái | Ghi chú |
|---|:---:|---|
| **Mục 1 (Cấu trúc package theo tính năng):** `com.group2.rms.offer.*` | ✅ Đạt | Controller, Service, DTO, Entity, Exception, Test đều nằm đúng package |
| **Mục 2.1 (Xử lý form & JSON API):** Duy trì tương thích cao | ✅ Đạt | Giữ cả REST JSON cho Pop-up Modal và Form POST truyền thống làm fallback |
| **Mục 2.3 (Ngoại lệ riêng cho module):** `OfferValidationException` | ✅ Đạt | Ném ngoại lệ riêng của module, kế thừa `BaseBusinessException` |
| **Mục 6.1 (Layout Level 3):** `layout:decorate="~{layout/internal}"` | ✅ Đạt | 100% 7 template kế thừa layout chuẩn |
| **Mục 6.3 (Không inline CSS/JS):** | ✅ Đạt | Đã dọn dẹp sạch thẻ `<script>` và thuộc tính `style="..."` inline |
| **Mục 8 (Dọn dẹp nợ kỹ thuật):** Cập nhật bảng hiện trạng | ✅ Đạt | Đã xóa/cập nhật dòng lỗi `offer` ném ngoại lệ trần |
| **Mục 11 (Quy ước tên test `*Tests`):** | ✅ Đạt | Đã đồng bộ hậu tố `*Tests` và chuyển vào đúng package |

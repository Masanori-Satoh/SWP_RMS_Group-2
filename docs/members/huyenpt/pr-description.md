## 🧪 Tổng quan Kiểm thử & Tài liệu hóa (Testing & Documentation Overview)

Pull Request này tập trung chuyên biệt vào việc **xây dựng bộ kiểm thử tự động toàn diện (Unit Test & Integration Test)** và **lập tài liệu hướng dẫn kiểm thử** cho phân hệ **Quản lý Đề xuất Tuyển dụng (Offer Proposal)** của thành viên **Phạm Thị Huyền (HuyenPT)**:

1. **Xây dựng Bộ Kiểm thử Tự động Toàn diện (68/68 Test Executions PASS — 100%):**
   - Hoàn thiện **56 Unit Test Executions** (32 methods) tại `OfferServiceTests.java` ứng dụng `@ParameterizedTest`, bao phủ toàn diện hợp đồng nghiệp vụ tầng Service, các điều kiện biên, độ ưu tiên địa điểm làm việc và ma trận trạng thái chuẩn của quy tắc `GBR-07`.
   - Hoàn thiện **12 Integration Tests** tại `OfferIntegrationTest.java` kiểm tra toàn bộ 7 REST API endpoints, mã phản hồi HTTP, CSRF token và phân quyền Spring Security.
2. **Khắc phục lỗi bổ trợ kiểm thử theo chuẩn Kiến trúc (Architecture Test Support):**
   - Bổ sung xử lý lỗi `MethodArgumentNotValidException` trong `GlobalExceptionHandler.java` để API trả về đúng mã chuẩn HTTP 400 Bad Request (thay vì bị bắt vào lỗi hệ thống 500).
   - Cập nhật đồng bộ tham số constructor `ViewerProfileResponse` trong `CareerFlowTests.java` để đảm bảo toàn dự án biên dịch test thành công (`test-compile BUILD SUCCESS`).
3. **Thiết lập Bộ Tài liệu Kiểm thử & Nghiệm thu (QA Deliverables):**
   - Xây dựng báo cáo ma trận chi tiết kiểm thử tự động và 6 kịch bản kiểm thử thủ công từng bước trên trình duyệt tại `offer-inter1-flow-testing.md`.
   - Cập nhật nhật ký công việc cá nhân và bảng mục lục tiến độ chung của toàn team tại `WORK_LOG.md`.

---

## 🎯 Chi tiết các Hạng mục Kiểm thử (Test Details)

### 1. Unit Testing — Tầng Dịch vụ & Ràng buộc Nghiệp vụ (`OfferServiceTests.java`)
Bao gồm **56 test executions** kiểm thử độc lập với Mockito và JUnit 5 Parameterized:
* **Kiểm thử Ràng buộc Lương (Salary Constraints):**
  * Lương thử việc $< 85\%$ mức lương chính thức $\rightarrow$ Ném `OfferValidationException` ở Service, khẳng định `never().save()`.
  * Lương thử việc $= 85\%$ mức lương chính thức $\rightarrow$ Hợp lệ.
  * Lương thử việc $>$ mức lương chính thức $\rightarrow$ Ném `OfferValidationException` ở Service và bắt vi phạm qua Bean Validation.
* **Kiểm thử Dữ liệu Đầu vào & Biên (Input Validation & Edge Cases):**
  * Ngày bắt đầu dự kiến $=$ hôm nay hoặc trong quá khứ $\rightarrow$ Bị chặn với `OfferValidationException`.
  * Thời gian thử việc $\le 0$ ngày (tham số hóa `0`, `-1`, `-60`), Mức lương $\le 0$ (tham số hóa `0`, `-1`, `-10.000.000`), Địa điểm làm việc để trống $\rightarrow$ Bị chặn, kiểm tra `never().save()`.
  * Gói phúc lợi để trống (`null`) $\rightarrow$ Cho phép và tạo thành công.
* **Kiểm thử Độ ưu tiên Địa điểm làm việc (WorkLocation Precedence):**
  * Cả JobPosting và Candidate đều có địa chỉ $\rightarrow$ Ưu tiên tuyệt đối `JobPosting.workLocation`.
  * JobPosting thiếu địa chỉ $\rightarrow$ Fallback về `Candidate.address`.
  * Cả hai đều thiếu $\rightarrow$ Fallback về giá trị mặc định của hệ thống (`Trụ sở chính Mộc RMS`).
* **Kiểm thử Quy tắc Single Active Offer (`GBR-07` Toàn diện):**
  * **Luồng Tạo mới (`createOfferByHr`)**:
    * Đơn ứng tuyển đang có Offer thuộc **Nhóm B (Locked - 11 trạng thái)**: `Pending_Director`, `Approved`, `Director_Approved`, `Sent_Candidate`, `Accepted`, `Rejected`, `Director_Rejected`, `Negotiating`, `Declined`, `Canceled`, `Voided` $\rightarrow$ Chặn hoàn toàn, ném mã lỗi `OFFER_LOCKED_STATE`, không lưu xuống DB (`never().save()`).
    * Đơn ứng tuyển đang có Offer thuộc **Nhóm A (Overridable - Chỉ duy nhất 1 trạng thái)**: `Draft` $\rightarrow$ Ghi đè in-place an toàn giữ nguyên `offerId`, triệt tiêu lỗi xung đột khóa duy nhất `UQ_OfferProposal_Application UNIQUE`.
  * **Luồng Cập nhật (`updateOfferByHr`)**:
    * Offer đang ở **Nhóm A (Editable - Chỉ `Draft`)** $\rightarrow$ Cho phép cập nhật thành công và lưu DB.
    * Offer đang ở **Nhóm B (Non-editable - 11 trạng thái)** $\rightarrow$ Chặn cập nhật, ném mã lỗi `OFFER_STATUS_INVALID`, không lưu DB (`never().save()`).
  * **Bộ lọc ứng viên đỗ (`getPassedCandidatesForOffer`)**: Tự động loại trừ toàn bộ ứng viên đang có Offer Nhóm B, chỉ giữ lại ứng viên chưa có Offer hoặc có Offer ở trạng thái `Draft` (Nhóm A).
* **Kiểm thử Chu trình Vòng đời Offer (Offer Proposal Lifecycle):**
  * Tra cứu Offer theo ID và Application ID (tìm thấy vs không tìm thấy ném `ResourceNotFoundException`).
  * Lấy danh sách phân trang (toàn bộ trạng thái vs lọc theo trạng thái cụ thể).
  * Lấy chi tiết Offer đầy đủ thông tin ứng viên, kết quả phỏng vấn, lịch sử duyệt của Giám đốc và lịch sử đàm phán.
  * Xóa bản thảo $\rightarrow$ Xóa mềm (`isDeleted = true`) thành công khi `Draft`; chặn xóa và ném `OFFER_NOT_DRAFT` với trạng thái khác.
  * Phát hành thư mời $\rightarrow$ Gửi thành công khi đã duyệt (`Approved`), chuyển trạng thái Offer sang `Sent_Candidate` và Application sang `Offered`; ném `OFFER_NOT_APPROVED` khi chưa được duyệt.

### 2. Integration Testing — Tầng REST API & Bảo mật (`OfferIntegrationTest.java`)
Bao gồm **12 test cases** kiểm thử tích hợp qua Spring `MockMvc`:
* `IT-01`: `GET /api/v1/hr/offers/passed-candidates` $\rightarrow$ Trả về 200 OK + JSON danh sách ứng viên đỗ hợp lệ.
* `IT-02`: `POST /api/v1/hr/offers` $\rightarrow$ Tạo Offer mới hợp lệ trả về 201 Created + JSON Offer.
* `IT-03`: `POST /api/v1/hr/offers` $\rightarrow$ Vi phạm ràng buộc lương trả về HTTP 400 Bad Request.
* `IT-04`: `POST /api/v1/hr/offers` $\rightarrow$ Tạo đè khi đơn có Offer Nhóm B trả về HTTP 400 Bad Request (`OFFER_LOCKED_STATE`).
* `IT-05`: `PUT /api/v1/hr/offers/{id}` $\rightarrow$ Cập nhật Offer Nhóm A hợp lệ trả về 200 OK.
* `IT-06`: `POST /api/v1/hr/offers/{id}/send` $\rightarrow$ Phát hành Offer Letter thành công trả về 200 OK (`Sent_Candidate`).
* `IT-07`: `GET /api/v1/hr/offers/passed-candidates` $\rightarrow$ Truy cập khi chưa đăng nhập bị chặn (Redirect 302 về Login / 401 Unauthorized).
* `IT-08`: `GET /api/v1/hr/offers` $\rightarrow$ Lấy danh sách có phân trang và lọc status trả về 200 OK + JSON Page.
* `IT-09`: `GET /api/v1/hr/offers/{id}` $\rightarrow$ Lấy chi tiết Offer đầy đủ trả về 200 OK + JSON Detail.
* `IT-10`: `DELETE /api/v1/hr/offers/{id}` $\rightarrow$ Xóa bản thảo (Draft) thành công trả về 200 OK.
* `IT-11`: `DELETE /api/v1/hr/offers/{id}` $\rightarrow$ Cố tình xóa Offer ngoài Draft trả về HTTP 400 Bad Request (`OFFER_NOT_DRAFT`).
* `IT-12`: `POST /api/v1/hr/offers/{id}/send` $\rightarrow$ Gửi Offer chưa duyệt Director trả về HTTP 400 Bad Request (`OFFER_NOT_APPROVED`).

---

## 📊 Kết quả Thực thi Kiểm thử (Execution Results)

Câu lệnh thực thi kiểm thử tự động trên PowerShell:
```powershell
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate'
$env:SPRING_SQL_INIT_MODE = 'never'
.\mvnw.cmd '-Dtest=OfferServiceTests,OfferIntegrationTest' test
```

Log kết quả chạy thực tế:
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.group2.rms.offer.OfferIntegrationTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.group2.rms.service.OfferServiceTests
[INFO] Tests run: 56, Failures: 0, Errors: 0, Skipped: 0
[INFO] -------------------------------------------------------
[INFO] Results:
[INFO] Tests run: 68, Failures: 0, Errors: 0, Skipped: 0 (100% PASS)
[INFO] -------------------------------------------------------
[INFO] BUILD SUCCESS
```

---

## 📝 Tài liệu Kiểm thử & Hướng dẫn Nghiệm thu (QA Documentation)

Đã hoàn thiện tài liệu chi tiết tại thư mục cá nhân và hệ thống tài liệu chung của dự án:

1. **Báo cáo Kiểm thử Toàn diện ([`docs/members/huyenpt/offer-inter1-flow-testing.md`](docs/members/huyenpt/offer-inter1-flow-testing.md)):**
   - Bảng ma trận 31 Unit test cases và 12 Integration test cases.
   - **Kịch bản kiểm thử thủ công (Manual UI Checklist):** Gồm 6 kịch bản thao tác trực quan từng bước trên trình duyệt (Kiểm tra form validation lương, kiểm tra Single Active Offer `GBR-07`, kiểm tra layout Stat Cards, kiểm tra xem chi tiết lịch sử duyệt/đàm phán, kiểm tra xóa bản thảo Draft, kiểm tra phát hành thư mời).
2. **Nhật ký Công việc theo ngày ([`docs/management/work_logs/2026-10-05-offer-flow-testing-and-bugfix.md`](docs/management/work_logs/2026-10-05-offer-flow-testing-and-bugfix.md)):**
   - Ghi nhận chi tiết kết quả xây dựng 43 test cases và cập nhật tài liệu.
3. **Mục lục Tiến độ Chung ([`docs/management/WORK_LOG.md`](docs/management/WORK_LOG.md)):**
   - Đã đồng bộ dòng cập nhật công việc ngày 05/10 của HuyenPT vào bảng theo dõi của cả nhóm.

---

## 📂 Danh sách Tệp tin Thay đổi trong Commit này (Files in this Commit)

| STT | Tệp tin | Thao tác | Mục đích |
| :---: | :--- | :---: | :--- |
| 1 | `src/test/java/com/group2/rms/service/OfferServiceTests.java` | Modified | Mở rộng và hoàn thiện **31 Unit test cases** cho Service layer |
| 2 | `src/test/java/com/group2/rms/offer/OfferIntegrationTest.java` | Created/Tracked | Xây dựng **12 Integration test cases** cho REST API & Security |
| 3 | `src/main/java/com/group2/rms/core/exception/GlobalExceptionHandler.java` | Modified | Bổ trợ test: Trả về HTTP 400 Bad Request cho lỗi validation của API |
| 4 | `src/test/java/com/group2/rms/CareerFlowTests.java` | Modified | Bổ trợ test: Đồng bộ constructor `ViewerProfileResponse` |
| 5 | `docs/members/huyenpt/offer-inter1-flow-testing.md` | Created | Tài liệu báo cáo kiểm thử chi tiết và kịch bản test tay UI |
| 6 | `docs/management/work_logs/2026-10-05-offer-flow-testing-and-bugfix.md` | Created | Nhật ký công việc theo ngày của HuyenPT |
| 7 | `docs/management/WORK_LOG.md` | Modified | Cập nhật mục lục tiến độ chung của toàn dự án |
| 8 | `docs/members/huyenpt/pr-description.md` | Created | Mẫu mô tả Pull Request / Comment nghiệm thu GitHub |

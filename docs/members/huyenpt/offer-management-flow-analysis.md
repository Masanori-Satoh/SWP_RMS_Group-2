# PHÂN TÍCH CHUYÊN SÂU 8 LUỒNG NGHIỆP VỤ & KIẾN TRÚC OFFER MANAGEMENT (MỘC RMS)

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](README.md).
**Tài liệu kỹ thuật học tập & Hướng dẫn bảo vệ đồ án SWP391**<br>
**Tác giả:** Đội ngũ Kỹ thuật & Senior Spring Boot Reviewer (Module: Offer Management - HuyenPT)<br>
**Tệp nguồn thẩm định:** `docs/members/huyenpt/offer-management-source-audit.md`<br>
**Nhánh Git:** `fix/huyenpt/offer-management-flow-fixes` | **Vị trí tài liệu:** `docs/members/huyenpt/offer-management-flow-analysis.md`<br>
**Ngày phát hành:** 02/11/2026<br>

---

## MỤC LỤC TỔNG QUAN

- [1. Danh mục 8 Luồng Nghiệp vụ A–H](#1-danh-mục-8-luồng-nghiệp-vụ-ah)
- [2. Phân tích chi tiết từng luồng A–H (14 Tiêu chuẩn Kỹ thuật + 5 Mục Thực chiến)](#2-phân-tích-chi-tiết-từng-luồng-ah)
  - [Luồng A: HR Tạo mới & Lưu Thư mời dạng Bản nháp (Draft)](#luồng-a-hr-tạo-mới--lưu-thư-mời-dạng-bản-nháp-draft)
  - [Luồng B: HR Nộp trình Giám đốc Phê duyệt (Submit to Director)](#luồng-b-hr-nộp-trình-giám-đốc-phê-duyệt-submit-to-director)
  - [Luồng C: HR Chỉnh sửa & Xóa mềm Bản nháp (Edit, Soft Delete & Reactivation)](#luồng-c-hr-chỉnh-sửa--xóa-mềm-bản-nháp-edit-soft-delete--reactivation)
  - [Luồng D: Giám đốc Phê duyệt Đề xuất Thư mời (Director Approve)](#luồng-d-giám-đốc-phê-duyệt-đề-xuất-thư-mời-director-approve)
  - [Luồng E: Giám đốc Từ chối Đề xuất Thư mời (Director Reject - Mandatory Reason)](#luồng-e-giám-đốc-từ-chối-đề-xuất-thư-mời-director-reject---mandatory-reason)
  - [Luồng F: HR Phát hành Thư mời tới Ứng viên (Issue Offer Letter & Deadline Trigger)](#luồng-f-hr-phát-hành-thư-mời-tới-ứng-viên-issue-offer-letter--deadline-trigger)
  - [Luồng G: Ứng viên Xem & Chấp thuận Thư mời trên Portal (Candidate Accept Offer)](#luồng-g-ứng-viên-xem--chấp-thuận-thư-mời-trên-portal-candidate-accept-offer)
  - [Luồng H: Ứng viên Xem & Từ chối Thư mời trên Portal (Candidate Decline Offer)](#luồng-h-ứng-viên-xem--từ-chối-thư-mời-trên-portal-candidate-decline-offer)
- [3. Chuyên đề Phân tích Kỹ thuật Nâng cao (Architecture Deep Dives)](#3-chuyên-đề-phân-tích-kỹ-thuật-nâng-cao-architecture-deep-dives)
  - [3.1. Race Condition & Double Submit Mitigation (Atomic SQL Update)](#31-race-condition--double-submit-mitigation-atomic-sql-update)
  - [3.2. Phòng chống Lỗ hổng IDOR (Insecure Direct Object References)](#32-phòng-chống-lỗ-hổng-idor-insecure-direct-object-references)
  - [3.3. Cơ chế Xóa mềm & Hồi sinh Dữ liệu (Soft Delete & Reactivation)](#33-cơ-chế-xóa-mềm--hồi-sinh-dữ-liệu-soft-delete--reactivation)
  - [3.4. Vòng đời Trạng thái Offer (Offer State Machine Matrix)](#34-vòng-đời-trạng-thái-offer-offer-state-machine-matrix)
  - [3.5. Đồng bộ Trạng thái Hồ sơ Ứng tuyển (Cross-Module Application Status Sync)](#35-đồng-bộ-trạng-thái-hồ-sơ-ứng-tuyển-cross-module-application-status-sync)
  - [3.6. Phân tích Bản chất Gốc & Khắc phục BUG-OFF-10 (Session Timeout Polling Redirect)](#36-phân-tích-bản-chất-gốc--khắc-phục-bug-off-10-session-timeout-polling-redirect)
- [4. Bảng Tra cứu Vấn đáp Hội đồng (Defense Q&A Cheat Sheet)](#4-bảng-tra-cứu-vấn-đáp-hội-đồng-defense-qa-cheat-sheet)

---

## 1. Danh mục 8 Luồng Nghiệp vụ A–H

Hệ thống quản lý thư mời nhận việc (Offer Management) của Mộc RMS được thiết kế xoay quanh 8 luồng vận hành chính thức kết nối 3 tác nhân (**HR**, **Director**, **Candidate**):

* **Luồng A:** HR Tạo mới & Lưu Thư mời dạng Bản nháp (`Draft`)
* **Luồng B:** HR Nộp trình Giám đốc phê duyệt (`Pending_Director`)
* **Luồng C:** HR Chỉnh sửa & Xóa mềm Bản nháp (`Edit / Soft Delete & Reactivation`)
* **Luồng D:** Giám đốc Phê duyệt Đề xuất Thư mời (`Director_Approved`)
* **Luồng E:** Giám đốc Từ chối Đề xuất Thư mời (`Director_Rejected` kèm bắt buộc nhập lý do)
* **Luồng F:** HR Phát hành Thư mời tới Ứng viên (`Sent_Candidate` $\rightarrow$ Sync Application `Offered`)
* **Luồng G:** Ứng viên Xem & Chấp thuận Thư mời trên Portal (`Accepted` $\rightarrow$ Sync Application `Hired`)
* **Luồng H:** Ứng viên Xem & Từ chối Thư mời trên Portal (`Declined` $\rightarrow$ Sync Application `Rejected`)

---

## 2. Phân tích chi tiết từng luồng A–H

---

### LUỒNG A: HR Tạo mới & Lưu Thư mời dạng Bản nháp (Draft)

#### 1. Actor và điều kiện đầu vào
- **Actor:** `ROLE_HR` (Nhân viên Tuyển dụng / Quản trị nhân sự).
- **Điều kiện đầu vào:**
  - HR đã đăng nhập hệ thống thành công.
  - Tồn tại hồ sơ ứng viên (`Application`) đã vượt qua vòng phỏng vấn (`HM_Passed` hoặc kết quả phỏng vấn đạt yêu cầu tạo Offer).
  - Hồ sơ đó chưa từng có Offer đang hoạt động (tuân thủ luật `GBR-07: Single Active Offer`).

#### 2. UI và nút thao tác
- **Màn hình UI:** `src/main/resources/templates/offers/list.html` hoặc từ `offers/form.html`.
- **Thao tác:** Bấm nút **"Tạo mới Thư mời"** (`#btnCreateOffer`) trên trang danh sách, hoặc bấm nút **"Lưu bản nháp"** (`name="action" value="draft"`) tại chân form `offers/form.html`.

#### 3. HTTP method, URL, Payload
- **HTTP Method:** `POST`
- **URL:** `/offers/create`
- **Headers:** `Content-Type: application/x-www-form-urlencoded` (hoặc `application/json` nếu gọi qua REST API nội bộ), `X-CSRF-TOKEN: <token>`.
- **Form Payload:**
  ```http
  applicationId=15&offeredPositionTitle=Backend+Developer&proposedSalary=25000000&probationSalary=21250000&probationDays=60&expectedStartDate=2026-12-01&workLocation=H%C3%A0+N%E1%BB%99i&benefitsPackage=B%E1%BA%A3o+hi%E1%BB%83m+PVI&responsePeriodDays=5&isDraft=true
  ```

#### 4. Spring Security, CSRF
- **Route Authorization:** Khai báo tại `SecurityConfig.java` (L134-135): `.requestMatchers("/offers", "/offers/**").hasAnyAuthority("ROLE_HR", "ROLE_DIRECTOR")`.
- **Method Checking:** `OfferController.java` (L49-64): Kiểm tra `principal.getAuthorities()`. Nếu không chứa `ROLE_HR`, hệ thống ném `AccessDeniedException`.
- **CSRF Token:** Thymeleaf tự động render `<input type="hidden" name="_csrf" th:value="${_csrf.token}" />`. Token được Spring Security `CsrfFilter` xác thực tính hợp lệ trước khi vào Controller.

#### 5. Controller + method + dòng code
- **Tệp tin:** `src/main/java/com/group2/rms/offer/controller/OfferController.java`
- **Method:** `public String createOffer(@Valid @ModelAttribute("createRequest") CreateOfferRequest createRequest, BindingResult result, ...)`
- **Dòng code:** L214 – L282.
- **Xử lý:** Kiểm tra `result.hasErrors()`. Nếu hợp lệ, gọi sang `offerService.createOfferByHr(createRequest)`. Nếu có lỗi, load lại dữ liệu ứng viên vào model và trả về view `"offers/form"`.

#### 6. DTO và Validation
- **DTO:** `src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java` (131 lines).
- **Validation Annotations:**
  - `@NotNull(message = "Vui lòng chọn ứng viên đỗ phỏng vấn.") private Integer applicationId;`
  - `@Positive(message = "Mức lương chính thức phải lớn hơn 0.") private BigDecimal proposedSalary;`
  - `@Positive(message = "Mức lương thử việc phải lớn hơn 0.") private BigDecimal probationSalary;`
  - `@Future(message = "Ngày bắt đầu dự kiến phải lớn hơn ngày hiện tại.") private LocalDate expectedStartDate;`
  - `@Min(1) @Max(60) private Integer responsePeriodDays;`
  - `@AssertTrue(message = "Lương thử việc phải đạt tối thiểu 85% lương chính thức.")`: Kiểm tra nghiệp vụ `probationSalary >= proposedSalary * 0.85` (L98-108).
  - `@AssertTrue`: Lương thử việc không vượt quá lương chính thức (L113-119).

#### 7. Service và Business Rule
- **Tệp tin:** `src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java`
- **Method:** `public OfferResponse createOfferByHr(CreateOfferRequest dto)` (L231-318).
- **Business Rule áp dụng:**
  - **BR-OFF-01:** Ràng buộc lương thử việc tối thiểu 85% (`validateProbationSalaryRule`).
  - **GBR-07 (Single Active Offer):** Gọi `offerProposalRepository.findByApplication_ApplicationId(dto.getApplicationId())`.
  - **Logic Soft-Delete & Reactivation (Khắc phục BUG-OFF-11):** Nếu tìm thấy Offer cũ có `isDeleted == true`, hệ thống **không thực hiện INSERT** mà cập nhật đè thông tin mới lên bản ghi cũ và set `isDeleted = false`.
  - Nếu `Boolean.TRUE.equals(dto.getIsDraft())`: Gán `offerStatus = "Draft"`.

#### 8. Repository và Query
- **Tệp tin:** `src/main/java/com/group2/rms/offer/repository/OfferProposalRepository.java`
- **Query thực thi:**
  - `Optional<OfferProposal> findByApplication_ApplicationId(Integer applicationId);`
  - `OfferProposal save(OfferProposal entity);`

#### 9. Hibernate và SQL Server
- Hibernate kích hoạt kiểm tra thực thể:
  ```sql
  -- Kiểm tra trùng lặp hồ sơ
  SELECT TOP 1 * FROM OfferProposal WHERE ApplicationId = @p0;
  -- Nếu là bản ghi mới:
  INSERT INTO OfferProposal (ApplicationId, OfferedPositionTitle, ProposedSalary, ProbationSalary,
                             ExpectedStartDate, WorkLocation, BenefitsPackage, OfferStatus,
                             ResponsePeriodDays, IsDeleted, ProposedBy, CreatedAt)
  VALUES (@p0, @p1, @p2, @p3, @p4, @p5, @p6, 'Draft', @p7, 0, @p8, SYSDATETIME());
  ```

#### 10. Table được đọc/ghi
- **Đọc:** `Application`, `Candidate`, `[User]`, `JobPosting`.
- **Ghi:** `OfferProposal` (INSERT bản ghi mới hoặc UPDATE bản ghi đã xóa mềm).

#### 11. Transaction và Rollback
- Được bao bọc bởi `@Transactional` trên `OfferServiceImpl`.
- **Rollback condition:** Ném `OfferValidationException`, `DataIntegrityViolationException`, hoặc bất kỳ `RuntimeException` nào chưa bắt. Trạng thái DB được hoàn nguyên toàn bộ.

#### 12. Email/Notification
- **Email:** Không gửi email (vì đây chỉ là bản thảo nội bộ).
- **Notification:** Không phát in-app notification (chưa gửi Giám đốc).

#### 13. Response và UI cuối
- **Phản hồi:** Redirect HTTP 302 về `/offers`.
- **UI cuối:** Hiển thị Flash Toast màu xanh lá: *"Đã lưu bản nháp đề xuất nhận việc thành công!"*. Thẻ KPI "Bản nháp" trên màn hình `offers/list.html` tăng 1.

#### 14. Các nhánh xử lý lỗi
- Lương thử việc < 85%: `BindingResult` bắt lỗi `@AssertTrue`, UI hiển thị viền đỏ dưới ô input lương kèm thông báo: *"Lương thử việc phải đạt tối thiểu 85% lương chính thức"*.
- Trùng hồ sơ đang có Offer hoạt động: Service ném `OfferValidationException`, Flash message báo lỗi: *"Ứng viên này đã có một đề xuất Offer đang hoạt động trong hệ thống"*.

---

#### 5 MỤC THỰC CHIẾN - LUỒNG A
* **GHI NHỚ:** Nút "Lưu bản nháp" chỉ lưu trữ nội bộ với `offerStatus = 'Draft'`, hoàn toàn chưa thông báo cho Giám đốc hay Ứng viên.
* **CẨN THẬN:** Khi truyền `applicationId` từ trang danh sách ứng viên trúng tuyển, form phải được gán `isLockedCandidate = true` để không bị chọn nhầm ứng viên khác (phòng ngừa lỗi `BUG-OFF-01`).
* **HỘI ĐỒNG HAY HỎI:** *"Nếu HR xóa nháp của một ứng viên rồi sau đó tạo lại cho chính ứng viên đó thì hệ thống xử lý như thế nào để không lỗi Unique Database?"*
* **CÁCH NÓI KHI BẢO VỆ:** *"Thưa thầy cô, tại bảng `OfferProposal` có ràng buộc Unique `UQ_OfferProposal_Application`. Để giải quyết triệt để lỗi xung đột khi tạo lại sau khi xóa mềm, nhóm đã áp dụng cơ chế Reactivation trong `OfferServiceImpl.createOfferByHr()`: kiểm tra nếu bản ghi cũ tồn tại với `isDeleted = true` thì hệ thống ghi đè thông tin mới và khôi phục cờ `isDeleted = false` thay vì chạy INSERT mới."*
* **THỬ NGAY BẰNG DEVTOOLS:** Mở Network tab, chọn ứng viên, nhập Proposed Salary `20,000,000`, nhập Probation Salary `15,000,000` (75%) $\rightarrow$ Bấm Lưu $\rightarrow$ Quan sát response trả về HTTP 200 re-render kèm thẻ `<span class="form-error">` mà không phát sinh HTTP 500.

---

### LUỒNG B: HR Nộp trình Giám đốc Phê duyệt (Submit to Director)

#### 1. Actor và điều kiện đầu vào
- **Actor:** `ROLE_HR`.
- **Điều kiện đầu vào:**
  - Đang tạo mới Offer và chọn **"Trình duyệt Director"** (`isDraft = false`), HOẶC:
  - Đang xem chi tiết một bản ghi Offer ở trạng thái `Draft` và quyết định nộp trình.

#### 2. UI và nút thao tác
- **Màn hình UI:** `offers/form.html` (nút **"Trình duyệt Director"**) hoặc modal xác nhận trong `offers/detail.html`.
- **Thao tác:** Bấm nút màu xanh dương đậm **"Trình Giám đốc phê duyệt"** (`#btnSubmitDirector`).

#### 3. HTTP method, URL, Payload
- **HTTP Method:** `POST`
- **URL:** `/offers/create` (nếu nộp ngay khi tạo) hoặc `/offers/{id}/edit` (nếu nộp từ bản nháp có sẵn).
- **Payload:** `isDraft=false`, `action=submit` cùng toàn bộ thông tin lương, ngày bắt đầu và chế độ phúc lợi.

#### 4. Spring Security, CSRF
- Xác thực `ROLE_HR` thông qua Spring Security `FilterSecurityInterceptor`.
- CSRF Token được đính kèm bắt buộc trong request body hoặc HTTP Header `X-CSRF-TOKEN`.

#### 5. Controller + method + dòng code
- **Tệp tin:** `OfferController.java`
- **Method:** `createOffer` (L214-282) hoặc `updateOffer` (L353-392).
- **Dòng code:** Điều phối gọi xuống `offerService.createOfferByHr(createRequest)` hoặc `offerService.updateOfferByHr(id, updateRequest)`.

#### 6. DTO và Validation
- `CreateOfferRequest` hoặc `UpdateOfferRequest`.
- Validation nghiêm ngặt toàn bộ các trường: Không được để trống `workLocation`, `expectedStartDate` phải lớn hơn `today + responsePeriodDays`, lương thử việc $\ge 85\%$.

#### 7. Service và Business Rule
- **Tệp tin:** `OfferServiceImpl.java` (L270-315).
- **Business Rule:**
  - Thiết lập trạng thái `offer.setOfferStatus("Pending_Director")`.
  - Ghi nhận `updatedAt = LocalDateTime.now()`.
  - **Tích hợp Notification:** Kích hoạt gửi thông báo in-app tới Giám đốc:
    ```java
    if (systemNotificationService != null) {
        systemNotificationService.notifyOfferPendingDirector(savedOffer, proposedBy);
    }
    ```

#### 8. Repository và Query
- `OfferProposalRepository.save(offer)`.
- Câu lệnh UPDATE trạng thái: `UPDATE OfferProposal SET OfferStatus = 'Pending_Director', ... WHERE OfferId = @p0`.

#### 9. Hibernate và SQL Server
- Entity chuyển từ `TRANSIENT` / `MANAGED (Draft)` sang `MANAGED (Pending_Director)`.
- Hibernate Dirty Checking phát hiện thay đổi thuộc tính `offerStatus` và sinh lệnh SQL UPDATE tại thời điểm Flush session.

#### 10. Table được đọc/ghi
- **Ghi:** `OfferProposal` (Cột `OfferStatus` chuyển thành `Pending_Director`).
- **Ghi:** `Notification` (Thêm bản ghi thông báo mới cho User có vai trò Director).

#### 11. Transaction và Rollback
- Được bọc trong Spring `@Transactional`.
- Nếu quá trình lưu thông báo bị lỗi (ví dụ ngắt kết nối websocket/notification repository), exception được bắt trong khối `try-catch` riêng của service để ghi log `warn`, không làm rollback việc nộp trình duyệt của HR.

#### 12. Email/Notification
- **In-App Notification:** Bắn thông báo tới chuông của Giám đốc với tiêu đề: *"Đề xuất Thư mời nhận việc mới đang chờ bạn phê duyệt: [Tên vị trí] - [Tên ứng viên]"*.

#### 13. Response và UI cuối
- Redirect HTTP 302 về `/offers`.
- Flash Toast thông báo: *"Đã nộp trình đề xuất nhận việc tới Giám đốc phê duyệt thành công!"*.
- Thẻ KPI "Chờ phê duyệt" (`statPending`) tự động tăng 1; Offer chuyển sang trạng thái nhãn vàng cam `Pending_Director`.

#### 14. Các nhánh xử lý lỗi
- Nếu Offer không còn ở trạng thái `Draft` (ví dụ đã bị ai đó duyệt hoặc nộp từ trước): Service ném `OfferValidationException("Chỉ có thể nộp trình duyệt các đề xuất đang ở trạng thái Bản nháp.")`.

---

#### 5 MỤC THỰC CHIẾN - LUỒNG B
* **GHI NHỚ:** Một khi đã nộp trình (`Pending_Director`), các nút "Chỉnh sửa" và "Xóa" trên giao diện HR sẽ bị khóa lại ngay lập tức để bảo đảm tính toàn vẹn dữ liệu trình duyệt.
* **CẨN THẬN:** `ResponsePeriodDays` (số ngày phản hồi) phải được thiết lập hợp lệ (mặc định 5 ngày) để sau này tính hạn chót phản hồi cho ứng viên.
* **HỘI ĐỒNG HAY HỎI:** *"Làm sao đảm bảo Giám đốc nhận được thông báo ngay khi HR vừa nộp trình duyệt?"*
* **CÁCH NÓI KHI BẢO VỆ:** *"Dạ thưa thầy cô, trong phương thức `createOfferByHr` và `updateOfferByHr`, hệ thống tiêm `NotificationService`. Ngay sau khi `OfferProposalRepository.save()` thành công, service kích hoạt `notifyOfferPendingDirector()` tạo bản ghi thông báo và đẩy qua endpoint WebSocket nội bộ tới tài khoản Giám đốc."*
* **THỬ NGAY BẰNG DEVTOOLS:** Mở tab Network, theo dõi request `POST /offers/create` với `isDraft=false`. Sau khi redirect sang `/offers`, kiểm tra xem badge trạng thái trên dòng ứng viên có mang class CSS `badge--warning` và chữ "Chờ Giám đốc duyệt" hay không.

---

### LUỒNG C: HR Chỉnh sửa & Xóa mềm Bản nháp (Edit, Soft Delete & Reactivation)

#### 1. Actor và điều kiện đầu vào
- **Actor:** `ROLE_HR`.
- **Điều kiện đầu vào:** Offer ID tồn tại trong hệ thống, thuộc sở hữu quyền quản lý của HR, và trạng thái hiện tại bắt buộc phải là `Draft` (hoặc `Director_Rejected` nếu cho phép sửa lại).

#### 2. UI và nút thao tác
- **Chỉnh sửa:** Bấm biểu tượng cây bút / nút **"Chỉnh sửa"** trên danh sách `/offers` hoặc trang chi tiết `/offers/{id}` $\rightarrow$ Mở `offers/form.html`.
- **Xóa bản nháp:** Bấm nút **"Xóa bản nháp"** (`#btnDeleteOffer`) trên danh sách hoặc trang chi tiết $\rightarrow$ Mở modal native `<dialog>` xác nhận xóa.

#### 3. HTTP method, URL, Payload
- **Chỉnh sửa (Submit):** `POST /offers/{id}/edit` kèm form payload `UpdateOfferRequest`.
- **Xóa bản nháp:** `POST /offers/{id}/delete` kèm payload rỗng + CSRF token.

#### 4. Spring Security, CSRF
- Cấu hình tại `SecurityConfig.java`: Route `/offers/{id}/edit` và `/offers/{id}/delete` yêu cầu quyền `ROLE_HR`.
- CSRF Token được truyền qua hidden field `_csrf`.

#### 5. Controller + method + dòng code
- **Tệp tin:** `OfferController.java`
- **Method Edit Form:** `editOfferForm(@PathVariable Integer id, ...)` (L304-324).
- **Method Update Action:** `updateOffer(@PathVariable Integer id, ...)` (L353-392).
- **Method Delete Action:** `deleteOffer(@PathVariable Integer id, RedirectAttributes flash)` (L412-426).

#### 6. DTO và Validation
- `UpdateOfferRequest.java` (118 lines).
- Kiểm tra toàn bộ ràng buộc lương, ngày tháng, và logic `@AssertTrue` lương thử việc tương tự như khi tạo mới.

#### 7. Service và Business Rule
- **Tệp tin:** `OfferServiceImpl.java`
- **Method Update:** `updateOfferByHr(Integer id, UpdateOfferRequest dto)` (L498-580).
  - Kiểm tra trạng thái: `if (!"Draft".equalsIgnoreCase(offer.getOfferStatus())) throw new OfferValidationException("Chỉ được phép cập nhật khi đề xuất ở trạng thái Draft.");`
- **Method Delete:** `deleteDraftOfferByHr(Integer id)` (L600-625).
  - Kiểm tra điều kiện: Chỉ cho phép xóa khi `offerStatus == "Draft"`.
  - **Cơ chế Soft-Delete:** Gán cờ `offer.setIsDeleted(true)` và lưu lại. Tuyệt đối **không gọi `repository.delete()`**.

#### 8. Repository và Query
- `OfferProposalRepository.save(offer)`.
- SQL Server thực thi:
  ```sql
  UPDATE OfferProposal
  SET IsDeleted = 1, UpdatedAt = SYSDATETIME()
  WHERE OfferId = @p0 AND OfferStatus = 'Draft';
  ```

#### 9. Hibernate và SQL Server
- Hibernate kích hoạt lệnh UPDATE trường `isDeleted`. Dữ liệu quan hệ với `Application` và các bảng khác vẫn giữ nguyên vẹn trên đĩa cứng, bảo đảm tính toàn vẹn dữ liệu kế toán và kiểm toán (Audit Trail).

#### 10. Table được đọc/ghi
- **Bảng ghi:** `OfferProposal` (Cập nhật cột `IsDeleted = 1`).

#### 11. Transaction và Rollback
- Quản lý qua `@Transactional`. Nếu có lỗi truy vấn hoặc vi phạm khóa ngoại, lệnh UPDATE bị rollback, cờ `IsDeleted` không thay đổi.

#### 12. Email/Notification
- Không gửi email hay notification (hành vi xóa bản thảo nội bộ của chính HR).

#### 13. Response và UI cuối
- Redirect HTTP 302 về `/offers`.
- Flash Toast thông báo: *"Đã xóa bản nháp đề xuất nhận việc thành công!"*. Bản ghi biến mất khỏi bảng hiển thị (do các query danh sách đã lọc `WHERE isDeleted = false`).

#### 14. Các nhánh xử lý lỗi
- Cố tình xóa Offer đã được Giám đốc duyệt hoặc đã gửi đi: Service ném `OfferValidationException("Không thể xóa đề xuất Thư mời đã được nộp duyệt hoặc phát hành.")`.

---

#### 5 MỤC THỰC CHIẾN - LUỒNG C
* **GHI NHỚ:** Hệ thống áp dụng **Soft Delete** (Xóa mềm), không xóa cứng (Hard Delete) khỏi database.
* **CẨN THẬN:** Khi một Offer đã bị xóa mềm, ràng buộc `UQ_OfferProposal_Application` trên database vẫn còn hiệu lực. Vì vậy khi tạo mới lại cho ứng viên đó, bắt buộc phải dùng cơ chế **Reactivation** (Hồi sinh bản ghi) như đã fix ở `BUG-OFF-11`.
* **HỘI ĐỒNG HAY HỎI:** *"Tại sao đồ án dùng Soft Delete thay vì Hard Delete? Và khi Soft Delete thì Unique Constraint xử lý ra sao?"*
* **CÁCH NÓI KHI BẢO VỆ:** *"Dạ, Soft Delete giúp hệ thống lưu vết lịch sử tuyển dụng phục vụ báo cáo nhân sự và tránh lỗi vỡ khóa ngoại `FK_OfferProposal_Application`. Để Unique Constraint trên SQL Server không bị lỗi khi tạo lại, phương thức `createOfferByHr()` của nhóm sẽ truy vấn bản ghi có `isDeleted = true` để ghi đè cập nhật lại thông tin mới và hạ cờ `isDeleted = false`."*
* **THỬ NGAY BẰNG DEVTOOLS:** Vào DB xem bản ghi Offer, sau đó trên UI bấm nút Xóa nháp. Chạy lại câu lệnh SQL `SELECT OfferId, IsDeleted FROM OfferProposal WHERE OfferId = ...` $\rightarrow$ Thấy `IsDeleted = 1`, dữ liệu không bị mất.

---

### LUỒNG D: Giám đốc Phê duyệt Đề xuất Thư mời (Director Approve)

#### 1. Actor và điều kiện đầu vào
- **Actor:** `ROLE_DIRECTOR` (Giám đốc công ty / Người có thẩm quyền phê duyệt cao nhất).
- **Điều kiện đầu vào:**
  - Giám đốc đăng nhập hệ thống.
  - Tồn tại đề xuất Offer đang ở trạng thái `Pending_Director`.

#### 2. UI và nút thao tác
- **Màn hình UI:** `offers/director-list.html` (Bàn làm việc Giám đốc) hoặc `offers/detail.html`.
- **Thao tác:** Bấm nút màu xanh lá **"Phê duyệt"** (`.btn-approve`) tại cột Thao tác trên bảng, hoặc mở modal phê duyệt nhanh native `<dialog id="directorDecisionModal">`.

#### 3. HTTP method, URL, Payload
- **HTTP Method:** `POST`
- **URL:** `/offers/{id}/approve`
- **Headers:** `Content-Type: application/json`, `X-CSRF-TOKEN: <token>`
- **Payload (JSON):**
  ```json
  {
    "comments": "Đồng ý với mức lương đề xuất. Đề nghị HR chuẩn bị hợp đồng chu đáo."
  }
  ```

#### 4. Spring Security, CSRF
- **Route Authorization:** Cấu hình cực kỳ chặt chẽ tại `SecurityConfig.java` (L132-133):
  ```java
  .requestMatchers(HttpMethod.POST, "/offers/*/approve", "/offers/*/reject")
      .hasAuthority("ROLE_DIRECTOR")
  ```
  Nếu tài khoản HR hoặc Candidate cố tình gửi POST tới URL này, Spring Security lập tức chặn ở tầng Filter với mã lỗi **HTTP 403 Forbidden**.
- CSRF Token được đính kèm vào Ajax Fetch Request Header qua hàm `submitDirectorDecision()` trong [offers.js](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js).

#### 5. Controller + method + dòng code
- **Tệp tin:** `OfferController.java`
- **Method:** `approveOfferJson(@PathVariable Integer id, @Valid @RequestBody DirectorDecisionRequest request)` (L462-478).
- **Dòng code:** Gọi `offerService.approveOfferByDirector(id, request.getComments())`. Trả về `ResponseEntity.ok(new ApiResponse<>(true, "Đã phê duyệt đề xuất nhận việc thành công!", approved))`.

#### 6. DTO và Validation
- **DTO:** `src/main/java/com/group2/rms/offer/dto/DirectorDecisionRequest.java`.
- `@Size(max = 2000, message = "Phản hồi của Director không được vượt quá 2000 ký tự.") private String comments;`.
- Ghi chú khi phê duyệt là **tùy chọn (Optional)**, có thể để trống.

#### 7. Service và Business Rule
- **Tệp tin:** `OfferServiceImpl.java`
- **Method:** `approveOfferByDirector(Integer id, String comments)` (L692-740).
- **Business Rule:**
  - Kiểm tra trạng thái hiện tại: Phải là `Pending_Director`.
  - Cập nhật trạng thái `offer.setOfferStatus("Director_Approved")`.
  - **Lưu vết lịch sử Audit:** Tạo mới một đối tượng thực thể `OfferApproval`:
    ```java
    OfferApproval approval = OfferApproval.builder()
            .offerProposal(saved)
            .director(directorUser)
            .status("Approved")
            .directorComments(comments)
            .approvedAt(LocalDateTime.now())
            .build();
    offerApprovalRepository.save(approval);
    ```
  - Gửi in-app notification cho người tạo đề xuất (`proposedBy` - HR).

#### 8. Repository và Query
- `OfferProposalRepository.save(offer)`.
- `OfferApprovalRepository.save(approval)`.
- SQL Server:
  ```sql
  UPDATE OfferProposal SET OfferStatus = 'Director_Approved', UpdatedAt = SYSDATETIME() WHERE OfferId = @p0;
  INSERT INTO OfferApproval (OfferId, DirectorId, Status, DirectorComments, ApprovedAt)
  VALUES (@p0, @p1, 'Approved', @p2, SYSDATETIME());
  ```

#### 9. Hibernate và SQL Server
- Giao dịch thực hiện 2 lệnh SQL tuần tự: 1 UPDATE trên bảng `OfferProposal` và 1 INSERT trên bảng `OfferApproval`. Cả hai được gắn kết chặt chẽ bởi khóa ngoại `FK_OfferApproval_Offer`.

#### 10. Table được đọc/ghi
- **Đọc:** `OfferProposal`, `[User]`.
- **Ghi:** `OfferProposal` (đổi trạng thái), `OfferApproval` (thêm bản ghi audit), `Notification` (thông báo cho HR).

#### 11. Transaction và Rollback
- Được chú thích `@Transactional`. Đảm bảo việc đổi trạng thái và ghi log lịch sử thành công đồng thời. Nếu ghi log thất bại, trạng thái Offer không bị chuyển sang `Director_Approved`.

#### 12. Email/Notification
- Bắn in-app notification cho HR: *"Giám đốc đã phê duyệt Đề xuất Thư mời cho ứng viên [Tên ứng viên]!"*.

#### 13. Response và UI cuối
- Trả về JSON HTTP 200: `{"success": true, "message": "Đã phê duyệt đề xuất nhận việc thành công!", "data": {...}}`.
- [offers.js](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js) đóng modal native `<dialog>`, hiển thị Flash Toast thành công và tự động tải lại dòng dữ liệu hoặc cập nhật KPI cards.

#### 14. Các nhánh xử lý lỗi
- Đề xuất đã bị hủy hoặc không còn ở trạng thái `Pending_Director`: Ném `OfferValidationException("Chỉ có thể phê duyệt các đề xuất đang ở trạng thái Chờ Giám đốc duyệt.")` $\rightarrow$ Trả về HTTP 400 JSON.

---

#### 5 MỤC THỰC CHIẾN - LUỒNG D
* **GHI NHỚ:** Giám đốc phê duyệt xong thì trạng thái là `Director_Approved`, **chưa gửi cho ứng viên**. Quyền gửi thư mời chính thức thuộc về HR ở Luồng F.
* **CẨN THẬN:** Lịch sử phê duyệt được lưu vào bảng riêng `OfferApproval` (quan hệ 1-N với `OfferProposal`) để lưu vết audit, không ghi đè trực tiếp lên cột của `OfferProposal`.
* **HỘI ĐỒNG HAY HỎI:** *"Nếu Giám đốc phê duyệt thì hệ thống có gửi email cho ứng viên ngay không? Tại sao?"*
* **CÁCH NÓI KHI BẢO VỆ:** *"Dạ không ạ. Theo đúng quy trình nghiệp vụ nhân sự doanh nghiệp, Giám đốc chỉ duyệt chính sách đãi ngộ nội bộ. Sau khi Giám đốc duyệt, HR sẽ là người kiểm tra lại lần cuối, rà soát thời hạn phản hồi rồi mới ấn 'Phát hành Thư mời' để gửi tới Ứng viên."*
* **THỬ NGAY BẰNG DEVTOOLS:** Dùng tài khoản HR, mở console DevTools và chạy lệnh `fetch('/offers/21/approve', {method: 'POST'})` $\rightarrow$ Thấy trả về lỗi ngay lập tức **HTTP 403 Forbidden** do Spring Security chặn đứng.

---

### LUỒNG E: Giám đốc Từ chối Đề xuất Thư mời (Director Reject - Mandatory Reason)

#### 1. Actor và điều kiện đầu vào
- **Actor:** `ROLE_DIRECTOR`.
- **Điều kiện đầu vào:** Offer đang ở trạng thái `Pending_Director`.

#### 2. UI và nút thao tác
- **Màn hình UI:** `offers/director-list.html` hoặc `offers/detail.html`.
- **Thao tác:** Bấm nút **"Từ chối"** (`.btn-reject`) $\rightarrow$ Mở modal native `<dialog id="directorDecisionModal">`. Nhập lý do từ chối vào textarea và bấm nút **"Xác nhận Từ chối"**.

#### 3. HTTP method, URL, Payload
- **HTTP Method:** `POST`
- **URL:** `/offers/{id}/reject`
- **Headers:** `Content-Type: application/json`, `X-CSRF-TOKEN: <token>`
- **Payload (JSON):**
  ```json
  {
    "comments": "Mức lương đề xuất vượt quá khung ngân sách cho phép của phòng ban (tối đa 22 triệu)."
  }
  ```

#### 4. Spring Security, CSRF
- Yêu cầu quyền `ROLE_DIRECTOR` tương tự Luồng D. Kiểm tra CSRF token qua header HTTP.

#### 5. Controller + method + dòng code
- **Tệp tin:** `OfferController.java`
- **Method:** `rejectOfferJson(@PathVariable Integer id, @Valid @RequestBody DirectorDecisionRequest request)` (L483-503).
- **Validate bắt buộc:**
  ```java
  if (request == null || request.getComments() == null || request.getComments().trim().isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(new ApiResponse<>(false, "Vui lòng nhập lý do từ chối vào ô nhận xét trước khi tiếp tục."));
  }
  ```

#### 6. DTO và Validation
- `DirectorDecisionRequest.java`.
- Được validate 2 tầng: Tầng Frontend JS trong `offers.js` (L478-490) và tầng Backend Controller/Service. **Lý do từ chối không được để trống hoặc chỉ chứa khoảng trắng**.

#### 7. Service và Business Rule
- **Tệp tin:** `OfferServiceImpl.java` (L742-790).
- **Business Rule:**
  - Chuyển trạng thái `offer.setOfferStatus("Director_Rejected")`.
  - Tạo bản ghi mới trong `OfferApproval` với `status = "Rejected"` và `directorComments = comments.trim()`.
  - Bắn in-app notification cho HR để thông báo lý do không được duyệt.

#### 8. Repository và Query
- `OfferProposalRepository.save(offer)`.
- `OfferApprovalRepository.save(approval)`.

#### 9. Hibernate và SQL Server
- Thực thi lệnh UPDATE trên `OfferProposal` và INSERT trên `OfferApproval`. Cột `Status` trong bảng `OfferApproval` nhận giá trị `'Rejected'` thỏa mãn ràng buộc `CK_OfferApproval_Status CHECK ([Status] IN (N'Approved', N'Rejected'))`.

#### 10. Table được đọc/ghi
- **Ghi:** `OfferProposal`, `OfferApproval`, `Notification`.

#### 11. Transaction và Rollback
- Được bảo vệ bởi `@Transactional`. Đảm bảo tính nguyên tử của việc từ chối và ghi vết audit comment.

#### 12. Email/Notification
- In-app notification thông báo tới HR: *"Đề xuất Thư mời cho ứng viên [Tên ứng viên] đã bị Giám đốc từ chối. Lý do: [Nội dung lý do]"*.

#### 13. Response và UI cuối
- Trả về JSON HTTP 200: `{"success": true, "message": "Đã từ chối đề xuất nhận việc thành công!", "data": {...}}`.
- Giao diện cập nhật trạng thái Offer sang huy hiệu màu đỏ `badge--danger` "Giám đốc từ chối". Thẻ KPI "Từ chối" tăng 1.

#### 14. Các nhánh xử lý lỗi
- Bỏ trống lý do từ chối: Backend chặn ngay lập tức và trả về HTTP 400 Bad Request: *"Vui lòng nhập lý do từ chối vào ô nhận xét trước khi tiếp tục."*.

---

#### 5 MỤC THỰC CHIẾN - LUỒNG E
* **GHI NHỚ:** Từ chối là hành động có ảnh hưởng lớn nên **bắt buộc phải có lý do** (Mandatory Reason) ở cả modal duyệt nhanh và trang chi tiết (theo cam kết tại commit `339b25c8`).
* **CẨN THẬN:** Độ dài ghi chú tối đa 2000 ký tự. Có bộ đếm ký tự realtime trên giao diện JavaScript.
* **HỘI ĐỒNG HAY HỎI:** *"Nếu Giám đốc từ chối thì HR có được phép chỉnh sửa lại để nộp trình lần 2 không?"*
* **CÁCH NÓI KHI BẢO VỆ:** *"Dạ có, trong logic `OfferServiceImpl.updateOfferByHr()`, trạng thái `Director_Rejected` được coi là trạng thái có thể sửa đổi (`isOverridableStatus`). HR có thể đọc lý do Giám đốc từ chối trong lịch sử phê duyệt, điều chỉnh lại mức lương cho phù hợp rồi nộp trình lại sang `Pending_Director`."*
* **THỬ NGAY BẰNG DEVTOOLS:** Mở modal Từ chối, xóa trắng ô nhận xét và bấm nút Từ chối $\rightarrow$ JavaScript hiển thị cảnh báo đỏ và ngăn submit. Nếu tắt JS để bypass $\rightarrow$ Backend trả về mã lỗi HTTP 400 Bad Request rõ ràng.

---

### LUỒNG F: HR Phát hành Thư mời tới Ứng viên (Issue Offer Letter & Deadline Trigger)

#### 1. Actor và điều kiện đầu vào
- **Actor:** `ROLE_HR`.
- **Điều kiện đầu vào:** Offer ID tồn tại, không bị xóa mềm, và **bắt buộc phải có trạng thái `Director_Approved`** (hoặc `Approved`).

#### 2. UI và nút thao tác
- **Màn hình UI:** `offers/detail.html`.
- **Thao tác:** Bấm nút màu tím nổi bật **"Phát hành Thư mời"** (`#btnSendOffer`) $\rightarrow$ Mở modal HTML5 `<dialog id="sendOfferModal">` xác nhận gửi.

#### 3. HTTP method, URL, Payload
- **HTTP Method:** `POST`
- **URL:** `/offers/{id}/send`
- **Headers:** `Content-Type: application/x-www-form-urlencoded`, `X-CSRF-TOKEN: <token>`.
- **Payload:** Rỗng (hoặc chỉ chứa CSRF token).

#### 4. Spring Security, CSRF
- `SecurityConfig.java` yêu cầu quyền `ROLE_HR`. Kiểm tra CSRF token hợp lệ.

#### 5. Controller + method + dòng code
- **Tệp tin:** `OfferController.java`
- **Method:** `sendOfferToCandidate(@PathVariable Integer id, RedirectAttributes flash)` (L448-457).
- **Dòng code:** Gọi `offerService.sendOfferToCandidate(id)`. Bắt `BaseBusinessException` nếu có lỗi và gán flash message tương ứng.

#### 6. DTO và Validation
- Không dùng DTO đầu vào phức tạp. Service trực tiếp kiểm tra tính hợp lệ của entity `OfferProposal`.

#### 7. Service và Business Rule
- **Tệp tin:** `OfferServiceImpl.java` (L627-670).
- **Business Rule cốt lõi:**
  1. Kiểm tra trạng thái: Phải là `Director_Approved`. Nếu đang ở `Draft` hoặc `Pending_Director` $\rightarrow$ Ném lỗi ngay.
  2. **Tính toán thời hạn phản hồi (Deadline Trigger):**
     ```java
     LocalDateTime deadline = LocalDateTime.now().plusDays(offer.getResponsePeriodDays());
     ```
  3. **Ràng buộc thời hạn:** `ResponseDeadline` phải xảy ra trước ngày bắt đầu làm việc dự kiến (`ExpectedStartDate`):
     ```java
     if (offer.getExpectedStartDate() != null && !deadline.toLocalDate().isBefore(offer.getExpectedStartDate())) {
         throw new OfferValidationException("Thời hạn phản hồi phải trước ngày bắt đầu dự kiến.");
     }
     ```
  4. Cập nhật `offer.setResponseDeadline(deadline)` và `offer.setOfferStatus("Sent_Candidate")`.
  5. **Đồng bộ trạng thái hồ sơ ứng tuyển (Application Status Sync):**
     ```java
     Application application = saved.getApplication();
     application.setApplicationStatus("Offered");
     applicationRepository.save(application);
     ```
  6. Kích hoạt gửi thư mời: `offerMailSender.sendOfferLetterToCandidate(saved)`.
  7. Kích hoạt in-app notification: `systemNotificationService.notifyOfferLetterSent(saved)`.

#### 8. Repository và Query
- `OfferProposalRepository.save(offer)`.
- `ApplicationRepository.save(application)`.
- SQL Server:
  ```sql
  UPDATE OfferProposal
  SET OfferStatus = 'Sent_Candidate', ResponseDeadline = @p0, UpdatedAt = SYSDATETIME()
  WHERE OfferId = @p1;

  UPDATE Application
  SET ApplicationStatus = 'Offered', UpdatedAt = SYSDATETIME()
  WHERE ApplicationId = @p2;
  ```

#### 9. Hibernate và SQL Server
- Hai lệnh UPDATE được thực hiện trong cùng một persistence context. Hibernate flush đồng bộ cả 2 bảng `OfferProposal` và `Application`.

#### 10. Table được đọc/ghi
- **Ghi:** `OfferProposal` (chuyển sang `Sent_Candidate`), `Application` (chuyển sang `Offered`), `Notification` (thông báo cho ứng viên).

#### 11. Transaction và Rollback
- Được bọc trong `@Transactional`. Nếu quá trình lưu Application hoặc OfferProposal thất bại, email sẽ không được gửi và toàn bộ trạng thái được rollback.

#### 12. Email/Notification
- **Email:** [`OfferMailSender.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferMailSender.java) gửi bức thư mời nhận việc trang trọng tới email của ứng viên kèm đường dẫn chi tiết `/portal/offers/{id}` và thông tin mức lương chính thức/thử việc.
- **In-App Notification:** Gửi thông báo tới tài khoản Candidate trên cổng Portal: *"Bạn đã nhận được Thư mời nhận việc cho vị trí [Vị trí] từ Mộc RMS!"*.

#### 13. Response và UI cuối
- Redirect HTTP 302 về `/offers/{id}`.
- Flash Toast thông báo: *"Đã phát hành và gửi thư mời nhận việc tới ứng viên thành công!"*.
- Nút "Phát hành Thư mời" biến mất, thay thế bằng nhãn trạng thái xanh da trời "Đã gửi ứng viên" kèm thời gian đếm ngược hạn phản hồi.

#### 14. Các nhánh xử lý lỗi
- Offer chưa được duyệt mà cố tình gửi: Ném `OfferValidationException("Chỉ có thể phát hành thư mời khi đề xuất đã được Giám đốc phê duyệt.")`.
- Hạn phản hồi vượt quá ngày bắt đầu làm việc: Ném `OfferValidationException("Thời hạn phản hồi phải trước ngày bắt đầu dự kiến.")`.

---

#### 5 MỤC THỰC CHIẾN - LUỒNG F
* **GHI NHỚ:** Đây là luồng duy nhất kích hoạt **đồng thời 3 sự kiện lớn**: (1) Chốt thời hạn phản hồi `ResponseDeadline`, (2) Đổi trạng thái Application sang `Offered`, (3) Gửi Email Offer Letter chính thức tới hòm thư ứng viên.
* **CẨN THẬN:** Nếu ứng viên không có email hợp lệ hoặc tài khoản bị khóa, `OfferMailSender` sẽ log warning chứ không làm crash luồng nghiệp vụ chính.
* **HỘI ĐỒNG HAY HỎI:** *"Cơ chế tính Deadline phản hồi của ứng viên hoạt động như thế nào?"*
* **CÁCH NÓI KHI BẢO VỆ:** *"Dạ thưa thầy cô, khi HR tạo Offer có trường `ResponsePeriodDays` (mặc định là 5 ngày). Tại thời điểm HR bấm 'Phát hành Thư mời', hệ thống lấy thời điểm hiện tại cộng với số ngày này: `LocalDateTime.now().plusDays(responsePeriodDays)` để gán vào cột `ResponseDeadline`. Kể từ thời điểm này, đồng hồ đếm ngược trên Candidate Portal bắt đầu kích hoạt."*
* **THỬ NGAY BẰNG DEVTOOLS:** Xem log server tại thời điểm bấm gửi: Hệ thống xuất dòng log `[MOCK EMAIL] Gửi Offer Letter tới ...` kèm link truy cập portal và mức lương chính xác.

---

### LUỒNG G: Ứng viên Xem & Chấp thuận Thư mời trên Portal (Candidate Accept Offer)

#### 1. Actor và điều kiện đầu vào
- **Actor:** `ROLE_CANDIDATE` (Ứng viên ứng tuyển).
- **Điều kiện đầu vào:**
  - Ứng viên đăng nhập bằng tài khoản Candidate của mình.
  - Tồn tại thư mời gửi đích danh cho ứng viên với trạng thái `Sent_Candidate`.
  - Thời điểm hiện tại **chưa vượt quá** `ResponseDeadline` và **chưa vượt quá** `ExpectedStartDate`.

#### 2. UI và nút thao tác
- **Màn hình UI:** `src/main/resources/templates/offers/candidate-detail.html`.
- **Thao tác:** Bấm nút lớn màu xanh lá **"Đồng ý nhận việc"** (`#btnAcceptOffer`) $\rightarrow$ Mở modal native `<dialog id="acceptModal">` $\rightarrow$ Bấm **"Xác nhận Đồng ý"**.

#### 3. HTTP method, URL, Payload
- **HTTP Method:** `POST`
- **URL:** `/portal/offers/{id}/accept`
- **Payload:** `fromDetail=true`, `_csrf=<token>`.

#### 4. Spring Security, CSRF
- **Route Authorization:** `SecurityConfig.java` (L118-119):
  ```java
  .requestMatchers("/portal/offers", "/portal/offers/**").hasAuthority("ROLE_CANDIDATE")
  ```
- Nhân sự nội bộ (HR, Director) không được phép truy cập vào luồng này (HTTP 403).
- Bắt buộc kiểm tra CSRF token.

#### 5. Controller + method + dòng code
- **Tệp tin:** `CandidateOfferController.java`
- **Method:** `accept(@PathVariable Integer id, @RequestParam(defaultValue = "false") boolean fromDetail, RedirectAttributes redirect)` (L73-91).
- **Dòng code:** Gọi `candidateOffers.accept(id)`. Xử lý các ngoại lệ `OfferResponseException` và `ResourceNotFoundException`.

#### 6. DTO và Validation
- Dữ liệu trả về giao diện đóng gói qua `CandidateOfferDetailResponse.java` (116 lines).
- Kiểm tra tính hợp lệ qua hàm `requireAnswerable(offer)` tại tầng Service.

#### 7. Service và Business Rule
- **Tệp tin:** `CandidateOfferServiceImpl.java` (L98-135).
- **Business Rule cốt lõi:**
  1. **Chống IDOR:** Truy vấn qua `offers.findVisibleById(offerId, userId, CANDIDATE_VISIBLE_STATUSES)`. Nếu không thuộc tài khoản hiện tại $\rightarrow$ Ném `ResourceNotFoundException`.
  2. **Kiểm tra Deadline & Expiration:** Gọi `requireAnswerable(offer)`:
     - Nếu `offerStatus != 'Sent_Candidate'` $\rightarrow$ Ném `OfferResponseException("Thư mời này đã được trả lời nên không thể thay đổi.")`.
     - Nếu `now.isAfter(offer.getResponseDeadline())` $\rightarrow$ Ném `OfferResponseException("Thư mời này đã quá thời hạn phản hồi...")`.
     - Nếu `now.toLocalDate().isAfter(offer.getExpectedStartDate())` $\rightarrow$ Ném lỗi quá ngày nhận việc.
  3. **Phòng chống Race Condition (Atomic SQL Update):**
     ```java
     int updated = offers.answerIfSent(offerId, "Accepted");
     if (updated == 0) {
         throw new OfferResponseException("Thư mời vừa được cập nhật. Vui lòng tải lại trang.");
     }
     ```
  4. **Đồng bộ Application sang Hired:**
     ```java
     applicationStatusService.onOfferAnswered(offer.getApplication().getApplicationId(), true);
     ```
  5. Đánh dấu thông báo đã đọc & gửi notification báo tin vui cho HR.

#### 8. Repository và Query
- **Tệp tin:** `CandidateOfferRepository.java`
- **Câu lệnh Atomic Update:**
  ```java
  @Modifying
  @Query("UPDATE OfferProposal o SET o.offerStatus = :to, o.updatedAt = CURRENT_TIMESTAMP " +
         "WHERE o.offerId = :id AND o.offerStatus = 'Sent_Candidate'")
  int answerIfSent(@Param("id") Integer offerId, @Param("to") String to);
  ```

#### 9. Hibernate và SQL Server
- Lệnh `answerIfSent` được chuyển thành câu lệnh SQL trực tiếp trên database:
  ```sql
  UPDATE OfferProposal
  SET OfferStatus = 'Accepted', UpdatedAt = CURRENT_TIMESTAMP
  WHERE OfferId = @p0 AND OfferStatus = 'Sent_Candidate';
  ```
  Nếu có 2 request gửi đồng thời (Double Click), request đầu tiên cập nhật thành công (trả về rowcount = 1), request thứ hai điều kiện `OfferStatus = 'Sent_Candidate'` không còn đúng $\rightarrow$ trả về rowcount = 0 $\rightarrow$ Spring Service bắt được và rollback ngay lập tức!

#### 10. Table được đọc/ghi
- **Đọc:** `OfferProposal`, `Application`, `Candidate`, `[User]`.
- **Ghi:** `OfferProposal` (`OfferStatus = 'Accepted'`), `Application` (`ApplicationStatus = 'Hired'`), `Notification` (báo cho HR).

#### 11. Transaction và Rollback
- Được bọc bởi `@Transactional`. Đảm bảo việc cập nhật Offer `Accepted` và chuyển Application `Hired` diễn ra trong cùng 1 transaction ACID.

#### 12. Email/Notification
- In-app notification thông báo cho HR: *"Ứng viên [Tên ứng viên] đã CHẤP THUẬN Thư mời nhận việc cho vị trí [Vị trí]!"*.

#### 13. Response và UI cuối
- Redirect HTTP 302 về `/portal/offers/{id}`.
- Flash Attributes: `decisionType = "ACCEPTED"`, hiển thị Banner chúc mừng nồng nhiệt: *"Chúc mừng bạn đã chính thức gia nhập Mộc RMS!"*. Các nút Accept/Decline biến mất hoàn toàn.

#### 14. Các nhánh xử lý lỗi
- Bấm chấp nhận khi đã hết hạn: `requireAnswerable` ném `OfferResponseException` $\rightarrow$ Flash message màu đỏ: *"Thư mời này đã quá thời hạn phản hồi. Vui lòng liên hệ bộ phận Tuyển dụng để được hỗ trợ."*.

---

#### 5 MỤC THỰC CHIẾN - LUỒNG G
* **GHI NHỚ:** Chấp thuận Offer đồng nghĩa với việc hồ sơ ứng tuyển chính thức chuyển sang trạng thái tuyển dụng thành công cao nhất: **`ApplicationStatus = 'Hired'`**.
* **CẨN THẬN:** Tuyệt đối không dùng logic `findById()` rồi `save()` thông thường vì sẽ bị dính lỗi Race Condition khi ứng viên mở 2 tab và click cùng lúc. Bắt buộc phải dùng **Atomic Update** `answerIfSent()`.
* **HỘI ĐỒNG HAY HỎI:** *"Nếu ứng viên mở 2 tab trình duyệt, một tab ấn Đồng ý, tab kia ấn Từ chối thì hệ thống xử lý tranh chấp như thế nào?"*
* **CÁCH NÓI KHI BẢO VỆ:** *"Thưa thầy cô, nhóm đã lường trước kịch bản Race Condition này và xử lý bằng kỹ thuật Atomic Update ở tầng Repository trong `CandidateOfferRepository.answerIfSent()`. Câu lệnh UPDATE có điều kiện `WHERE offerStatus = 'Sent_Candidate'`. Tab nào gửi request đến trước sẽ cập nhật thành công (update count = 1). Tab thứ hai đến sau sẽ nhận count = 0, Service lập tức chặn lại và ném `OfferResponseException` yêu cầu người dùng tải lại trang mà không làm hỏng dữ liệu."*
* **THỬ NGAY BẰNG DEVTOOLS:** Mở Network tab, throttle mạng sang "Slow 3G", bấm nút Đồng ý 2 lần thật nhanh $\rightarrow$ Kiểm tra thấy chỉ có 1 request thành công, request thứ hai trả về thông báo an toàn.

---

### LUỒNG H: Ứng viên Xem & Từ chối Thư mời trên Portal (Candidate Decline Offer)

#### 1. Actor và điều kiện đầu vào
- **Actor:** `ROLE_CANDIDATE`.
- **Điều kiện đầu vào:** Thư mời đang ở trạng thái `Sent_Candidate`, chưa quá hạn phản hồi.

#### 2. UI và nút thao tác
- **Màn hình UI:** `candidate-detail.html`.
- **Thao tác:** Bấm nút **"Từ chối Thư mời"** (`#btnDeclineOffer`) $\rightarrow$ Mở modal native `<dialog id="declineModal">` khảo sát lý do từ chối. Chọn chip lý do gợi ý hoặc tự nhập lý do vào textarea $\rightarrow$ Bấm **"Xác nhận Từ chối"**.

#### 3. HTTP method, URL, Payload
- **HTTP Method:** `POST`
- **URL:** `/portal/offers/{id}/decline`
- **Headers:** `Content-Type: application/x-www-form-urlencoded`, `X-CSRF-TOKEN: <token>`
- **Payload:**
  ```http
  declineReason=T%C3%B4i+%C4%91%C3%A3+nh%E1%BA%ADn+%C4%91%C6%B0%E1%BB%A3c+l%E1%BB%9Di+m%E1%BB%9Di+kh%C3%A1c+ph%C3%B9+h%E1%BB%A3p+h%C6%A1n.&fromDetail=true
  ```

#### 4. Spring Security, CSRF
- Yêu cầu quyền `ROLE_CANDIDATE`. Kiểm tra CSRF token.

#### 5. Controller + method + dòng code
- **Tệp tin:** `CandidateOfferController.java`
- **Method:** `decline(@PathVariable Integer id, @Valid @ModelAttribute CandidateOfferDeclineRequest form, BindingResult bindingResult, ...)` (L94-124).
- **Dòng code:** Lấy `declineReason` từ form và gọi `candidateOffers.decline(id, declineReason)`.

#### 6. DTO và Validation
- **DTO:** `src/main/java/com/group2/rms/offer/dto/CandidateOfferDeclineRequest.java` (Java Record).
- `@Size(max = 1000, message = "Lý do từ chối không được vượt quá 1000 ký tự.") String declineReason`.
- Khắc phục triệt để `BUG-OFF-09` (lỗi cắt cụt chuỗi Data Truncation khi vượt quá kích thước cột database).

#### 7. Service và Business Rule
- **Tệp tin:** `CandidateOfferServiceImpl.java` (L104-135).
- **Business Rule:**
  - Kiểm tra IDOR và hạn phản hồi (`requireAnswerable`).
  - Cắt chuỗi an toàn `cleanReason = cleanReason.substring(0, 1000)` nếu chuỗi gửi lên vượt quá giới hạn.
  - Chạy Atomic Update: `offers.answerIfSent(offerId, "Declined", cleanReason)`.
  - **Đồng bộ Application sang Rejected:**
    ```java
    applicationStatusService.onOfferAnswered(offer.getApplication().getApplicationId(), false);
    ```
  - Bắn in-app notification cho HR kèm lý do từ chối.

#### 8. Repository và Query
- Câu lệnh Atomic Update kèm lý do:
  ```java
  @Modifying
  @Query("UPDATE OfferProposal o SET o.offerStatus = :to, o.declineReason = :reason, o.updatedAt = CURRENT_TIMESTAMP " +
         "WHERE o.offerId = :id AND o.offerStatus = 'Sent_Candidate'")
  int answerIfSent(@Param("id") Integer offerId, @Param("to") String to, @Param("reason") String reason);
  ```

#### 9. Hibernate và SQL Server
- Thực thi câu lệnh SQL trực tiếp:
  ```sql
  UPDATE OfferProposal
  SET OfferStatus = 'Declined', DeclineReason = @p0, UpdatedAt = CURRENT_TIMESTAMP
  WHERE OfferId = @p1 AND OfferStatus = 'Sent_Candidate';
  ```
- Cột `DeclineReason` trên SQL Server có kiểu `NVARCHAR(1000)` lưu trữ trọn vẹn văn bản tiếng Việt có dấu.

#### 10. Table được đọc/ghi
- **Ghi:** `OfferProposal` (`OfferStatus = 'Declined'`, lưu `DeclineReason`), `Application` (`ApplicationStatus = 'Rejected'`), `Notification`.

#### 11. Transaction và Rollback
- Được bảo vệ bởi `@Transactional`. Đảm bảo việc cập nhật trạng thái Offer và chuyển Application sang `Rejected` thành công đồng thời.

#### 12. Email/Notification
- In-app notification thông báo cho HR: *"Ứng viên [Tên ứng viên] đã TỪ CHỐI Thư mời nhận việc cho vị trí [Vị trí]. Lý do: [Lý do từ chối]"*.

#### 13. Response và UI cuối
- Redirect HTTP 302 về `/portal/offers/{id}`.
- Flash Message: *"Bạn đã từ chối thư mời nhận việc vị trí [Vị trí]. Cảm ơn bạn đã quan tâm đến Mộc RMS."*. Giao diện chuyển sang trạng thái trung tính `badge--neutral` "Đã từ chối".

#### 14. Các nhánh xử lý lỗi
- Nhập lý do quá 1000 ký tự: `BindingResult` bắt lỗi `@Size` ngay từ Controller $\rightarrow$ Flash message: *"Lý do từ chối không được vượt quá 1000 ký tự."*.

---

#### 5 MỤC THỰC CHIẾN - LUỒNG H
* **GHI NHỚ:** Khi ứng viên Từ chối, hồ sơ ứng tuyển sẽ tự động chuyển sang **`ApplicationStatus = 'Rejected'`**, kết thúc quy trình tuyển dụng cho đợt ứng tuyển đó.
* **CẨN THẬN:** Luôn kiểm soát độ dài lý do từ chối dưới 1000 ký tự ở cả Form validation, Service truncation và Database column (`NVARCHAR(1000)`).
* **HỘI ĐỒNG HAY HỎI:** *"Nếu ứng viên từ chối thư mời thì HR có thấy được lý do không và xem ở đâu?"*
* **CÁCH NÓI KHI BẢO VỆ:** *"Dạ có, lý do từ chối được lưu trực tiếp vào trường `DeclineReason` của bảng `OfferProposal` và bắn thông báo tới HR. Khi HR bấm vào xem chi tiết Offer tại `/offers/{id}`, hệ thống hiển thị một khung ghi chú nổi bật chứa toàn bộ phản hồi khảo sát của ứng viên."*
* **THỬ NGAY BẰNG DEVTOOLS:** Chọn thử chíp gợi ý lý do trên modal $\rightarrow$ Quan sát đoạn text tự động điền vào textarea và bộ đếm ký tự cập nhật chính xác.

---

## 3. Chuyên đề Phân tích Kỹ thuật Nâng cao (Architecture Deep Dives)

### 3.1. Race Condition & Double Submit Mitigation (Atomic SQL Update)
- **Vấn đề thực tế:** Người dùng mạng lag thường có thói quen click đúp (Double Click) nút Accept/Decline, hoặc mở 2 tab cùng lúc: tab 1 bấm Accept, tab 2 bấm Decline. Nếu dùng kiến trúc ORM truyền thống:
  ```java
  // RỦI RO LỚN (Anti-Pattern):
  OfferProposal offer = repository.findById(id).get();
  offer.setOfferStatus("Accepted");
  repository.save(offer);
  ```
  Hai transaction đọc cùng lúc đều thấy trạng thái là `Sent_Candidate` và cùng ghi đè lên nhau, dẫn tới việc Application vừa chuyển `Hired` lại vừa bị chuyển tiếp sang `Rejected`!
- **Giải pháp triệt để trong Mộc RMS:** Áp dụng **Atomic Conditional Update** trực tiếp tại tầng CSDL qua `CandidateOfferRepository.answerIfSent()`:
  ```sql
  UPDATE OfferProposal
  SET OfferStatus = :to, UpdatedAt = CURRENT_TIMESTAMP
  WHERE OfferId = :id AND OfferStatus = 'Sent_Candidate'
  ```
  Nhờ cơ chế Row-level Locking của SQL Server, chỉ có transaction đầu tiên thỏa mãn mệnh đề `WHERE` và trả về `rowcount = 1`. Transaction thứ hai sẽ nhận `rowcount = 0`, Service lập tức ném `OfferResponseException` và chặn đứng mọi xung đột logic!

### 3.2. Phòng chống Lỗ hổng IDOR (Insecure Direct Object References)
- **Vấn đề thực tế:** Kẻ tấn công đăng nhập tài khoản Ứng viên A (`UserId = 10`), nhưng cố tình đổi URL trên trình duyệt thành `/portal/offers/99` (vốn là thư mời lương cao của Ứng viên B). Nếu controller chỉ tìm `findById(99)`, Ứng viên A sẽ xem trộm được toàn bộ thông tin mức lương và chế độ đãi ngộ của người khác!
- **Giải pháp bảo vệ đa tầng:**
  1. Tầng Controller lấy `Authentication` từ `SecurityContextHolder`.
  2. Tầng Service tiêm `CurrentUserService` lấy chính xác `userId` từ token đăng nhập hiện tại:
     ```java
     Integer currentUserId = currentUserService.requireUser().getUserId();
     ```
  3. Tầng Repository thực hiện câu lệnh truy vấn có mệnh đề kiểm tra quyền sở hữu ngặt nghèo:
     ```sql
     SELECT o FROM OfferProposal o
     JOIN o.application a
     WHERE o.offerId = :id AND a.candidate.account.userId = :userId
     ```
  4. Nếu ID không thuộc sở hữu của ứng viên hiện tại, câu truy vấn trả về `Optional.empty()` và ném **`ResourceNotFoundException`** (trả về HTTP 404). Kẻ tấn công thậm chí không biết được Offer ID 99 có tồn tại hay không.

### 3.3. Cơ chế Xóa mềm & Hồi sinh Dữ liệu (Soft Delete & Reactivation)
- **Bản chất kỹ thuật:** Để bảo toàn ràng buộc toàn vẹn và lịch sử tuyển dụng, cột `IsDeleted BIT NOT NULL DEFAULT 0` được thêm vào bảng `OfferProposal` (qua migration `006_offer_is_deleted.sql`).
- **Nghịch lý với Unique Constraint (Nguyên nhân lỗi 500 của BUG-OFF-11):** Bảng `OfferProposal` có ràng buộc `CONSTRAINT UQ_OfferProposal_Application UNIQUE (ApplicationId)`. Khi HR xóa bản nháp của hồ sơ #10, bản ghi vẫn nằm trong DB với `IsDeleted = 1`. Khi HR bấm tạo lại Offer mới cho hồ sơ #10, câu lệnh `INSERT` mới sẽ vi phạm ngay lập tức ràng buộc Unique này và gây lỗi HTTP 500.
- **Cơ chế Hồi sinh (Reactivation):**
  Trong `OfferServiceImpl.createOfferByHr()`, trước khi tạo mới, service kiểm tra:
  ```java
  Optional<OfferProposal> existingOpt = offerProposalRepository.findByApplication_ApplicationId(dto.getApplicationId());
  if (existingOpt.isPresent()) {
      OfferProposal existing = existingOpt.get();
      if (Boolean.TRUE.equals(existing.getIsDeleted())) {
          // HỒI SINH BẢN GHI: Ghi đè thông tin mới và hủy cờ xóa mềm
          existing.setIsDeleted(false);
          existing.setProposedSalary(dto.getProposedSalary());
          existing.setProbationSalary(dto.getProbationSalary());
          existing.setOfferStatus(Boolean.TRUE.equals(dto.getIsDraft()) ? "Draft" : "Pending_Director");
          return mapToResponse(offerProposalRepository.save(existing));
      }
      throw new OfferValidationException("Ứng viên này đã có đề xuất Thư mời đang hoạt động.");
  }
  ```

### 3.4. Vòng đời Trạng thái Offer (Offer State Machine Matrix)

| Trạng thái Hiện tại | Hành động Kích hoạt | Trạng thái Mới | Tác nhân thực hiện | Quyền hạn sửa / xóa |
| :--- | :--- | :--- | :--- | :--- |
| *(Chưa có)* | Tạo mới (Save Draft) | `Draft` | HR | Cho phép Sửa, Xóa mềm |
| *(Chưa có)* hoặc `Draft` | Nộp trình duyệt | `Pending_Director` | HR | **Đã khóa Sửa & Xóa** |
| `Pending_Director` | Giám đốc Duyệt | `Director_Approved` | Director | Khóa |
| `Pending_Director` | Giám đốc Từ chối | `Director_Rejected` | Director | Cho phép HR sửa lại |
| `Director_Approved` | Phát hành Thư mời | `Sent_Candidate` | HR | Khóa hoàn toàn nội bộ |
| `Sent_Candidate` | Ứng viên Chấp thuận | `Accepted` | Candidate | Trạng thái Kết thúc |
| `Sent_Candidate` | Ứng viên Từ chối | `Declined` | Candidate | Trạng thái Kết thúc |
| `Draft` | Xóa bản nháp | `IsDeleted = 1` | HR | Ẩn khỏi hệ thống |

### 3.5. Đồng bộ Trạng thái Hồ sơ Ứng tuyển (Cross-Module Application Status Sync)
Quy trình tuyển dụng kết nối trực tiếp phân hệ Offer với tiến trình vòng đời của `Application`:
1. Khi Offer được phát hành (`sendOfferToCandidate`):
   $$\text{ApplicationStatus: } \text{Interviewing} \longrightarrow \mathbf{\text{Offered}}$$
2. Khi Ứng viên Chấp thuận (`accept`):
   $$\text{ApplicationStatus: } \text{Offered} \longrightarrow \mathbf{\text{Hired}}$$
3. Khi Ứng viên Từ chối (`decline`):
   $$\text{ApplicationStatus: } \text{Offered} \longrightarrow \mathbf{\text{Rejected}}$$
Việc chuyển đổi được thực thi thông qua phương thức `ApplicationStatusService.onOfferAnswered(applicationId, accepted)` đảm bảo tuân thủ quy tắc tiến trình một chiều (`GBR-01`), không bao giờ bị đảo ngược trạng thái cũ.

### 3.6. Phân tích Bản chất Gốc & Khắc phục BUG-OFF-10 (Session Timeout Polling Redirect)
- **Cơ chế gây lỗi:**
  1. Giao diện có script [topbar-notifications.js](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/topbar-notifications.js) chạy polling ngầm mỗi 30 giây tới `/api/notifications/unread-count`.
  2. Khi người dùng không thao tác trong 30 phút, session Tomcat hết hạn (Timeout).
  3. Script polling tiếp tục gửi request ngầm `GET /api/notifications/unread-count`.
  4. Spring Security chặn request chưa xác thực, và theo mặc định của `HttpSessionRequestCache`, nó lưu URL vừa bị chặn vào session: `savedRequest = "/api/notifications/unread-count"`.
  5. Trình duyệt chuyển hướng người dùng về `/login`.
  6. Sau khi người dùng nhập đúng Username/Password và đăng nhập thành công, `SavedRequestAwareAuthenticationSuccessHandler` lấy URL từ `savedRequest` và chuyển hướng trình duyệt tới URL đó $\rightarrow$ Người dùng bị rơi vào trang trắng hiển thị raw JSON `{"unreadCount": 0}` thay vì trang làm việc trước đó!
- **Giải pháp khắc phục triệt để:** Cấu hình loại trừ toàn bộ các API endpoint ngầm khỏi `HttpSessionRequestCache` trong [`SecurityConfig.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java):
  ```java
  HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
  // Loại trừ các request Ajax / REST API polling ngầm không lưu làm redirect target
  requestCache.setRequestMatcher(new NegatedRequestMatcher(
      new AntPathRequestMatcher("/api/**")
  ));
  ```

---

## 4. Bảng Tra cứu Vấn đáp Hội đồng (Defense Q&A Cheat Sheet)

| Câu hỏi của Hội đồng Đánh giá | Trọng tâm Kỹ thuật cần trả lời | Dẫn chứng File & Dòng code trong Project |
| :--- | :--- | :--- |
| **Q1: "Làm sao hệ thống đảm bảo ứng viên không thể chấp thuận thư mời khi đã quá hạn?"** | Hệ thống kiểm tra 2 lớp: (1) Service kiểm tra `now.isAfter(responseDeadline)` ném `OfferResponseException`. (2) Giao diện Thymeleaf binding cờ `canRespond` để vô hiệu hóa nút bấm và hiển thị nhãn hết hạn. | [`CandidateOfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java) (L141-155), [`candidate-detail.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/candidate-detail.html) |
| **Q2: "Tại sao nhóm không dùng `@Version` (Optimistic Locking) mà lại dùng Atomic Update SQL cho thao tác nhận việc?"** | Vì Optimistic Locking yêu cầu đọc thực thể lên bộ nhớ rồi kiểm tra version khi commit, vẫn có độ trễ gây ra exception dạng `OptimisticLockException` làm người dùng hoang mang. Câu lệnh Atomic Update `answerIfSent` kiểm tra điều kiện ngay tại tầng engine của SQL Server, nhanh hơn và an toàn tuyệt đối. | [`CandidateOfferRepository.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/repository/CandidateOfferRepository.java) (L49-57) |
| **Q3: "Phân quyền giữa HR và Giám đốc trên cùng URL `/offers` được triển khai thế nào?"** | Cả hai role đều được qua Filter `/offers/**`, nhưng tại `@ModelAttribute populateCommonAttributes()` của Controller, hệ thống kiểm tra authority: nếu là Director thì render `offers/director-list`, nếu là HR thì render `offers/list`. Riêng endpoint `/approve` và `/reject` thì khóa chặt chỉ cho `ROLE_DIRECTOR` tại `SecurityConfig`. | [`OfferController.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java) (L51-60), [`SecurityConfig.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java) (L132-135) |
| **Q4: "Khi xuất Excel hàng nghìn bản ghi thì hệ thống có bị tràn bộ nhớ (OutOfMemoryError) không?"** | Không, vì `OfferExportServiceImpl` được tối ưu hóa: sử dụng streaming workbook, giải phóng bộ nhớ định kỳ, chỉ truy vấn đúng các cột được chọn và bọc `@Transactional(readOnly = true)` để Hibernate không duy trì snapshot dirty checking trong bộ nhớ heap. | [`OfferExportServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferExportServiceImpl.java) |
| **Q5: "Quy tắc 85% lương thử việc được kiểm tra ở những tầng nào trong ứng dụng?"** | Được bảo vệ 4 tầng: (1) HTML5/JS tính tự động khi nhập liệu, (2) Bean Validation `@AssertTrue` trên DTO, (3) Service validation ném `OfferValidationException`, (4) Database Check Constraint `CK_OfferProposal_Salaries`. | [`CreateOfferRequest.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java) (L98-108), [`OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java) (L672-690), `database/schema/db.sql` |

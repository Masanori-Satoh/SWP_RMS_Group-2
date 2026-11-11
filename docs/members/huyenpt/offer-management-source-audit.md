> Ghi chú 11/11/2026: đường dẫn và package test đã được cập nhật theo tầng kiểm thử; kết quả Task/PR lịch sử giữ nguyên.

# BÁO CÁO AUDIT TOÀN DIỆN MÃ NGUỒN PHÂN HỆ QUẢN LÝ THƯ MỜI NHẬN VIỆC (OFFER MANAGEMENT)

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](README.md).
**Project:** SWP391 - Recruitment Management System (Mộc RMS - Group 2)<br>
**Tài liệu kỹ thuật phục vụ học tập & bảo vệ đồ án:** `docs/members/huyenpt/offer-management-source-audit.md`<br>
**Chuyên gia thẩm định:** Senior Java Spring Boot Developer & Technical Reviewer<br>
**Phiên bản mã nguồn kiểm tra:** Git Branch `fix/huyenpt/offer-management-flow-fixes` (Commit `339b25c8`)<br>
**Ngày lập báo cáo:** 02/11/2026<br>

---

## MỤC LỤC
1. [Danh mục toàn bộ tệp tin phân hệ Offer Management](#1-danh-mục-toàn-bộ-tệp-tin-phân-hệ-offer-management)
2. [Phân tích chi tiết từng tệp tin mã nguồn](#2-phân-tích-chi-tiết-từng-tệp-tin-mã-nguồn)
3. [Bảng ma trận truy vết kỹ thuật (End-to-End Tracing Matrix)](#3-bảng-ma-trận-truy-vết-kỹ-thuật-end-to-end-tracing-matrix)
4. [Phân tích kiến trúc bảo mật & Kiểm soát truy cập (RBAC)](#4-phân-tích-kiến-trúc-bảo-mật--kiểm-soát-truy-cập-rbac)
5. [Xác minh Offer State Machine, Business Rules, Validation & Đồng bộ Application](#5-xác-minh-offer-state-machine-business-rules-validation--đồng-bộ-application)
6. [Kiểm tra Email, Notification, Transaction, CSRF & Xử lý ngoại lệ](#6-kiểm-tra-email-notification-transaction-csrf--xử-lý-ngoại-lệ)
7. [Bảng đối chiếu sai lệch giữa Báo cáo Inventory trước đó và Code thực tế](#7-bảng-đối-chiếu-sai-lệch-giữa-báo-cáo-inventory-trước-đó-và-code-thực-tế)
8. [Tổng kết & Khuyến nghị phục vụ bảo vệ đồ án SWP391](#8-tổng-kết--khuyến-nghị-phục-vụ-bảo-vệ-đồ-án-swp391)

---

## 1. Danh mục toàn bộ tệp tin phân hệ Offer Management

Hệ thống mã nguồn của phân hệ Quản lý Đề xuất & Thư mời nhận việc (Offer Management) được tổ chức theo kiến trúc phân tầng chuẩn doanh nghiệp (Layered Architecture - Controller / Service / Repository / Entity / DTO / Portal), tổng cộng **48 tệp tin kỹ thuật cốt lõi**:

### 1.1. Controller Layer (2 files)
- `src/main/java/com/group2/rms/offer/controller/OfferController.java` (532 lines) - Điều phối luồng nội bộ (HR & Giám đốc).
- `src/main/java/com/group2/rms/offer/controller/CandidateOfferController.java` (125 lines) - Điều phối Candidate Portal.

### 1.2. Service Layer & Email (7 files)
- `src/main/java/com/group2/rms/offer/service/OfferService.java` (112 lines) - Interface nghiệp vụ quản trị Offer.
- `src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java` (1006 lines) - Cài đặt nghiệp vụ Offer nội bộ.
- `src/main/java/com/group2/rms/offer/service/CandidateOfferService.java` (39 lines) - Interface cổng ứng viên.
- `src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java` (294 lines) - Cài đặt cổng ứng viên & xử lý hết hạn.
- `src/main/java/com/group2/rms/offer/service/OfferExportService.java` (31 lines) - Interface xuất báo cáo Excel.
- `src/main/java/com/group2/rms/offer/service/OfferExportServiceImpl.java` (753 lines) - Cài đặt xuất báo cáo Excel (Apache POI).
- `src/main/java/com/group2/rms/offer/service/OfferMailSender.java` (92 lines) - Gửi email thông báo thư mời nhận việc.

### 1.3. Repository Layer (3 files)
- `src/main/java/com/group2/rms/offer/repository/OfferProposalRepository.java` (67 lines) - JPA Repo cho OfferProposal.
- `src/main/java/com/group2/rms/offer/repository/OfferApprovalRepository.java` (20 lines) - JPA Repo cho lịch sử phê duyệt.
- `src/main/java/com/group2/rms/offer/repository/CandidateOfferRepository.java` (58 lines) - JPA Repo tối ưu hóa cho Candidate Portal.

### 1.4. Entity Layer (2 files)
- `src/main/java/com/group2/rms/offer/entity/OfferProposal.java` (80 lines) - Thực thể Đề xuất & Thư mời nhận việc.
- `src/main/java/com/group2/rms/offer/entity/OfferApproval.java` (113 lines) - Thực thể Lưu vết phê duyệt của Giám đốc.

### 1.5. DTO & Enum Layer (13 files)
- `src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java` (131 lines) - DTO tạo mới đề xuất.
- `src/main/java/com/group2/rms/offer/dto/UpdateOfferRequest.java` (118 lines) - DTO cập nhật bản nháp.
- `src/main/java/com/group2/rms/offer/dto/DirectorDecisionRequest.java` (16 lines) - DTO quyết định duyệt/từ chối của Giám đốc.
- `src/main/java/com/group2/rms/offer/dto/OfferResponse.java` (39 lines) - DTO hiển thị danh sách cho HR/Director.
- `src/main/java/com/group2/rms/offer/dto/OfferDetailResponse.java` (77 lines) - DTO hiển thị trang chi tiết nội bộ.
- `src/main/java/com/group2/rms/offer/dto/PassedCandidateResponse.java` (43 lines) - DTO danh sách ứng viên đỗ tuyển dụng.
- `src/main/java/com/group2/rms/offer/dto/CandidateOfferResponse.java` (39 lines) - DTO danh sách phía Portal ứng viên.
- `src/main/java/com/group2/rms/offer/dto/CandidateOfferDetailResponse.java` (116 lines) - DTO chi tiết thư mời ứng viên.
- `src/main/java/com/group2/rms/offer/dto/CandidateOfferDeclineRequest.java` (27 lines) - Java Record nhận lý do từ chối.
- `src/main/java/com/group2/rms/offer/dto/OfferExportRequest.java` (21 lines) - DTO yêu cầu xuất file Excel.
- `src/main/java/com/group2/rms/offer/dto/OfferExportFilterRequest.java` (11 lines) - DTO điều kiện lọc phục vụ xuất Excel.
- `src/main/java/com/group2/rms/offer/dto/OfferExportRow.java` (41 lines) - DTO dòng dữ liệu phẳng đưa vào bảng tính.
- `src/main/java/com/group2/rms/offer/dto/OfferExportScope.java` (13 lines) - **Enum duy nhất** (FILTERED, SELECTED, ALL).

### 1.6. Exception Layer (2 files)
- `src/main/java/com/group2/rms/offer/exception/OfferValidationException.java` (18 lines) - Ngoại lệ vi phạm quy tắc nghiệp vụ.
- `src/main/java/com/group2/rms/offer/exception/OfferResponseException.java` (13 lines) - Ngoại lệ phản hồi cổng ứng viên.

### 1.7. Core Security & System Exception (Liên kết trực tiếp) (2 files)
- `src/main/java/com/group2/rms/core/config/SecurityConfig.java` (183 lines) - Cấu hình RBAC, CSRF, URL filtering.
- `src/main/java/com/group2/rms/core/exception/GlobalExceptionHandler.java` (369 lines) - Bắt & chuẩn hóa lỗi HTTP 400/500.

### 1.8. User Interface - Thymeleaf HTML Templates (6 files)
- `src/main/resources/templates/offers/list.html` (508 lines) - Quản lý Offer của HR (Screen 30).
- `src/main/resources/templates/offers/form.html` (291 lines) - Tạo mới và chỉnh sửa bản nháp Offer.
- `src/main/resources/templates/offers/detail.html` (490 lines) - Xem chi tiết Offer & Lịch sử phê duyệt (Screen 32).
- `src/main/resources/templates/offers/director-list.html` (284 lines) - Bàn làm việc phê duyệt của Giám đốc.
- `src/main/resources/templates/offers/candidate-list.html` (215 lines) - Cổng ứng viên: Danh sách thư mời nhận việc.
- `src/main/resources/templates/offers/candidate-detail.html` (374 lines) - Cổng ứng viên: Letterhead Thư mời & Modal phản hồi.

### 1.9. Static Assets - JavaScript & CSS (5 files)
- `src/main/resources/static/js/offers.js` (2005 lines) - Script điều khiển Modal native `<dialog>`, validation, Ajax.
- `src/main/resources/static/js/pages/candidate-offers.js` (285 lines) - Script đếm ngược hạn phản hồi, chip lý do từ chối.
- `src/main/resources/static/css/offers.css` (2348 lines) - Phong cách CSS phân hệ Offer nội bộ.
- `src/main/resources/static/css/pages/candidate-offers.css` (2513 lines) - Phong cách thư mời trang trọng cho ứng viên.
- `src/main/resources/static/css/pages/offer-workspace.css` (1223 lines) - Phong cách form tạo mới/chỉnh sửa Offer.

### 1.10. Database Schema & Migration Scripts (3 files)
- `database/schema/db.sql` (Line 506 - 575) - Định nghĩa DDL bảng `OfferProposal` và `OfferApproval`.
- `database/migrations/006_offer_is_deleted.sql` (11 lines) - Thêm cột xóa mềm `IsDeleted BIT`.
- `database/migrations/008_offer_response_deadline.sql` (21 lines) - Thêm cột `ResponsePeriodDays`, `ResponseDeadline`, ràng buộc `CK_OfferProposal_ResponsePeriodDays`.

### 1.11. Automated Test Suite (7 files)
- `src/test/java/com/group2/rms/offer/controller/OfferIntegrationTests.java` (589 lines) - 27 ca kiểm thử tích hợp MVC MockMvc.
- `src/test/java/com/group2/rms/offer/service/OfferServiceTests.java` (1597 lines) - 41 ca kiểm thử đơn vị tầng Service.
- `src/test/java/com/group2/rms/offer/controller/CandidateOfferWebTests.java` (322 lines) - 19 ca kiểm thử MVC bảo mật cổng ứng viên.
- `src/test/java/com/group2/rms/offer/service/CandidateOfferServiceTests.java` (531 lines) - 18 ca kiểm thử đơn vị CandidateOfferService.
- `src/test/java/com/group2/rms/offer/service/OfferExportServiceTests.java` (319 lines) - 10 ca kiểm thử xuất file Excel.
- `src/test/java/com/group2/rms/offer/controller/OfferControllerBugTests.java` (158 lines) - 6 ca kiểm thử hồi quy các lỗi đã fix.
- `src/test/java/com/group2/rms/offer/repository/CandidateOfferQueryTests.java` (101 lines) - 5 ca kiểm thử JPA Query & Atomic Update.

---

## 2. Phân tích chi tiết từng tệp tin mã nguồn

### 2.1. Nhóm Controller

#### [OfferController.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java) (532 lines)
- **Vai trò:** Controller MVC chính của phân hệ quản trị Offer nội bộ. Phân luồng thông minh giữa HR và Giám đốc dựa trên `SecurityContext`.
- **Method quan trọng:**
  1. `populateCommonAttributes(Model, Principal)` (L49-64): `@ModelAttribute` gắn thông tin vai trò `HR` hoặc `Director`. Chặn `AccessDeniedException` nếu người dùng không thuộc hai vai trò này.
  2. `listOffers(...)` (L70-147): Xử lý `GET /offers`. Nếu là Director trả về view `"offers/director-list"`; nếu là HR trả về view `"offers/list"`. Gọi `offerService.getDirectorOffers(...)` hoặc `offerService.getAllOffersForHr(...)`.
  3. `createOfferForm(...)` (L153-193): Xử lý `GET /offers/create`. Nhận optional param `applicationId`. Nếu có, khóa trường ứng viên (`isLockedCandidate = true`).
  4. `createOffer(...)` (L214-282): Xử lý `POST /offers/create`. Validate dữ liệu, gọi `offerService.createOfferByHr(...)`.
  5. `editOfferForm(...)` (L304-324): Xử lý `GET /offers/{id}/edit`. Kiểm tra trạng thái Draft, nếu không phải Draft thì ném lỗi và redirect.
  6. `updateOffer(...)` (L353-392): Xử lý `POST /offers/{id}/edit`. Gọi `offerService.updateOfferByHr(...)`.
  7. `deleteOffer(...)` (L412-426): Xử lý `POST /offers/{id}/delete`. Gọi `offerService.deleteDraftOfferByHr(...)`.
  8. `sendOfferToCandidate(...)` (L448-457): Xử lý `POST /offers/{id}/send`. Gọi `offerService.sendOfferToCandidate(...)`.
  9. `approveOfferJson(...)` (L464-478): Xử lý `POST /offers/{id}/approve` (JSON Ajax). Gọi `offerService.approveOfferByDirector(...)`.
  10. `rejectOfferJson(...)` (L485-503): Xử lý `POST /offers/{id}/reject` (JSON Ajax). **Kiểm tra bắt buộc lý do từ chối** (`request.getComments()`), gọi `offerService.rejectOfferByDirector(...)`.
  11. `exportOffers(...)` (L510-531): Xử lý `POST /offers/export`. Kiểm tra quyền HR/Director, gọi `offerExportService.exportOffersToExcel(...)` trả về `ResponseEntity<byte[]>`.

#### [CandidateOfferController.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/CandidateOfferController.java) (125 lines)
- **Vai trò:** Controller MVC dành riêng cho Ứng viên (Candidate Portal).
- **Method quan trọng:**
  1. `list(Model)` (L36-40): Xử lý `GET /portal/offers`. Gọi `candidateOffers.list()`, trả về view `"offers/candidate-list"`.
  2. `detail(Integer id, Model)` (L43-48): Xử lý `GET /portal/offers/{id}`. Gọi `candidateOffers.getDetail(id)`, trả về view `"offers/candidate-detail"`.
  3. `declinePage(Integer id, RedirectAttributes)` (L51-70): Xử lý `GET /portal/offers/{id}/decline`. Kiểm tra hạn phản hồi và chuyển hướng về `/portal/offers/{id}?openDecline=true`.
  4. `accept(Integer id, boolean fromDetail, RedirectAttributes)` (L73-91): Xử lý `POST /portal/offers/{id}/accept`. Gọi `candidateOffers.accept(id)`.
  5. `decline(Integer id, CandidateOfferDeclineRequest, ...)` (L94-124): Xử lý `POST /portal/offers/{id}/decline`. Validate form, gọi `candidateOffers.decline(id, reason)`.

---

### 2.2. Nhóm Service Layer

#### [OfferServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java) (1006 lines)
- **Vai trò:** Cốt lõi xử lý nghiệp vụ Offer nội bộ, kiểm tra ràng buộc GBR-07, GBR-14, tính toán lương thử việc 85%, nộp duyệt, soft-delete và tích hợp thông báo.
- **Method quan trọng:**
  1. `createOfferByHr(CreateOfferRequest dto)` (L231-318):
     - Ràng buộc GBR-07: Tìm kiếm Offer cũ qua `findByApplication_ApplicationId(applicationId)`.
     - **Giải pháp BUG-OFF-11:** Nếu tìm thấy bản ghi cũ đang bị xóa mềm (`isDeleted = true`), tiến hành hồi sinh (override thông tin mới, set `isDeleted = false`) để không bị lỗi trùng `UQ_OfferProposal_Application`.
     - Xác thực BR-OFF-01 (`validateProbationSalaryRule`).
     - Nếu `isDraft == true` $\rightarrow$ set `OfferStatus = "Draft"`. Nếu không $\rightarrow$ set `"Pending_Director"` và gọi `systemNotificationService.notifyOfferPendingDirector(...)`.
  2. `updateOfferByHr(Integer id, UpdateOfferRequest dto)` (L498-580): Chỉ cho phép chỉnh sửa khi offer đang ở trạng thái `Draft` hoặc `Rejected`.
  3. `deleteDraftOfferByHr(Integer id)` (L600-625): Chỉ cho phép xóa khi `Draft`. Đánh dấu `isDeleted = true` (Soft Delete).
  4. `sendOfferToCandidate(Integer id)` (L627-670):
     - Kiểm tra trạng thái: Phải là `Director_Approved` hoặc `Approved`.
     - Tính `ResponseDeadline = LocalDateTime.now().plusDays(offer.getResponsePeriodDays())`.
     - Kiểm tra `ResponseDeadline` phải trước `ExpectedStartDate`.
     - Cập nhật Offer sang `"Sent_Candidate"`.
     - **Đồng bộ Application:** Gắn `application.setApplicationStatus("Offered")`.
     - Kích hoạt gửi mail: `offerMailSender.sendOfferLetterToCandidate(saved)`.
     - Kích hoạt in-app notification: `systemNotificationService.notifyOfferLetterSent(saved)`.
  5. `approveOfferByDirector(Integer id, String comments)` (L692-740):
     - Chuyển trạng thái sang `"Director_Approved"`.
     - Tạo bản ghi mới trong bảng `OfferApproval` (Lưu vết audit).
     - Gửi notification cho HR.
  6. `rejectOfferByDirector(Integer id, String comments)` (L742-790):
     - Yêu cầu `comments` không được trống.
     - Chuyển trạng thái sang `"Director_Rejected"`.
     - Tạo bản ghi mới trong bảng `OfferApproval`.
     - Gửi notification cho HR.

#### [CandidateOfferServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java) (294 lines)
- **Vai trò:** Cung cấp dữ liệu và tiếp nhận phản hồi từ phía Ứng viên.
- **Method quan trọng:**
  1. `list()` (L58-75): Truy vấn qua `CandidateOfferRepository.findVisibleByUserId(userId, CANDIDATE_VISIBLE_STATUSES)`. Chỉ hiển thị các trạng thái `Sent_Candidate`, `Accepted`, `Declined`.
  2. `getDetail(Integer offerId)` (L78-95): Kiểm tra quyền sở hữu IDOR qua `findVisibleById`.
  3. `accept(Integer offerId)` (L98-101) & `decline(Integer offerId, String declineReason)` (L104-106):
     - Gọi hàm private `answer(offerId, accepted, declineReason)`.
     - Kiểm tra điều kiện `requireAnswerable(offer)`: Phải là `Sent_Candidate`, chưa quá `ResponseDeadline`, chưa quá `ExpectedStartDate`.
     - Chống Race Condition / Double Submit: Dùng Atomic Update `offers.answerIfSent(offerId, toStatus)`. Nếu trả về 0 rows updated $\rightarrow$ ném `OfferResponseException`.
     - **Đồng bộ Application:** Gọi `applicationStatusService.onOfferAnswered(applicationId, accepted)` (Nếu accept $\rightarrow$ `"Hired"`; nếu decline $\rightarrow$ `"Rejected"`).
     - Đánh dấu đã đọc thông báo & phát thông báo phản hồi cho HR.

#### [OfferMailSender.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferMailSender.java) (92 lines)
- **Vai trò:** Đóng gói việc gửi email thông báo Offer Letter tới hòm thư cá nhân của ứng viên.
- **Method quan trọng:**
  - `sendOfferLetterToCandidate(OfferProposal offer)`:
    - Trích xuất email và họ tên ứng viên từ cấu trúc `offer.getApplication().getCandidate().getAccount()`.
    - Lấy `JavaMailSender` qua `ObjectProvider<JavaMailSender>`.
    - Nếu có cấu hình SMTP thực tế: Tạo `SimpleMailMessage`, điền mức lương chính thức, lương thử việc, hạn phản hồi, địa điểm làm việc, và đường dẫn trực tiếp tới `/portal/offers/{id}`.
    - Nếu không có cấu hình SMTP (môi trường Dev/Test): Ghi log `[MOCK EMAIL]` an toàn mà không làm sập luồng nghiệp vụ.

#### [OfferExportServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferExportServiceImpl.java) (753 lines)
- **Vai trò:** Tạo bảng tính Excel (.xlsx) chuẩn nghiệp vụ bằng thư viện Apache POI (`SXSSFWorkbook` / `XSSFWorkbook`).
- **Method quan trọng:**
  - `exportOffersToExcel(OfferExportRequest request, String exportedBy)`:
    - Bọc `@Transactional(readOnly = true)`.
    - Xử lý 3 phạm vi xuất: `FILTERED` (dùng JPA Specification), `SELECTED` (theo danh sách IDs), `ALL` (toàn bộ trừ `isDeleted = true`).
    - Hỗ trợ chọn lọc linh hoạt trong danh sách 22 cột dữ liệu (Mã thư mời, Ứng viên, Vị trí, Lương, Phòng ban, Hạn phản hồi, Quyết định Giám đốc...).
    - Format kiểu dữ liệu: Tiền tệ VNĐ (`#,##0`), ngày tháng (`dd/MM/yyyy`), căn lề, border và tô màu tiêu đề.

---

### 2.3. Nhóm Repository Layer

#### [OfferProposalRepository.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/repository/OfferProposalRepository.java) (67 lines)
- Kế thừa: `JpaRepository<OfferProposal, Integer>`, `JpaSpecificationExecutor<OfferProposal>`.
- **Query quan trọng:**
  - `findByApplication_ApplicationId(Integer applicationId)`: Truy vấn Offer theo hồ sơ ứng tuyển (dùng trong việc phục hồi soft-delete của BUG-OFF-11).
  - `findAllActiveByOrderByOfferIdAsc(Pageable pageable)`: Lấy các offer chưa bị xóa mềm (`isDeleted IS NULL OR isDeleted = false`).
  - `countPendingDirector()`, `countDirectorApproved()`, `countSentCandidate()`, `countAccepted()`, `countDirectorRejected()`: Các truy vấn tổng hợp số liệu cho KPI dashboard.

#### [CandidateOfferRepository.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/repository/CandidateOfferRepository.java) (58 lines)
- Kế thừa: `JpaRepository<OfferProposal, Integer>`.
- **Query quan trọng:**
  - `findVisibleByUserId(@Param("userId") Integer userId, @Param("statuses") Collection<String> statuses)`: Truy vấn danh sách thư mời hiển thị cho ứng viên kèm `JOIN FETCH` ngăn chặn lỗi N+1 Query.
  - `findVisibleById(...)`: Truy vấn chi tiết kèm xác thực IDOR.
  - `@Modifying answerIfSent(...)`: Câu lệnh `UPDATE` nguyên tử có điều kiện `WHERE offerStatus = 'Sent_Candidate'`.

#### [OfferApprovalRepository.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/repository/OfferApprovalRepository.java) (20 lines)
- Kế thừa: `JpaRepository<OfferApproval, Integer>`.
- **Query quan trọng:**
  - `findByOfferProposal_OfferIdOrderByApprovedAtDesc(Integer offerId)`: Lấy toàn bộ lịch sử phê duyệt của một Offer sắp xếp theo thời gian mới nhất.

---

### 2.4. Nhóm Entity Layer & Database Schema

#### [OfferProposal.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/entity/OfferProposal.java) (80 lines)
- Kế thừa: `BaseEntity` (Kế thừa `createdAt`, `updatedAt`).
- Bảng CSDL: `OfferProposal`.
- Ràng buộc CSDL (Database Constraints):
  - `UQ_OfferProposal_Application`: UNIQUE trên cột `ApplicationId`.
  - `CK_OfferProposal_Salaries`: `ProposedSalary >= 0 AND ProbationSalary >= 0 AND ProbationSalary >= (ProposedSalary * 0.85)`.
  - `CK_OfferProposal_Status`: `OfferStatus IN ('Draft', 'Pending_Director', 'Director_Approved', 'Director_Rejected', 'Sent_Candidate', 'Accepted', 'Declined')`.
  - `CK_OfferProposal_ResponsePeriodDays`: `ResponsePeriodDays IS NULL OR ResponsePeriodDays > 0`.

#### [OfferApproval.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/entity/OfferApproval.java) (113 lines)
- Bảng CSDL: `OfferApproval`.
- Các trường: `OfferApprovalId` (PK), `OfferId` (FK), `DirectorId` (FK), `Status` (Approved / Rejected), `DirectorComments` (NVARCHAR(MAX)), `ApprovedAt` (DATETIME2).
- Khắc phục BUG-OFF-06: Bổ sung custom Builder và `@PrePersist` để tránh xung đột bytecode compiler của Lombok.

---

## 3. Bảng ma trận truy vết kỹ thuật (End-to-End Tracing Matrix)

Bảng ma trận truy vết toàn diện từ Giao diện UI đến Lưu trữ CSDL:

| Chức năng (Function) | Màn hình UI | URL / Endpoint | Controller & Method | Service & Method | Repository & Query | Entity | Bảng DB | Phản hồi (Response) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Xem danh sách Offer (HR)** | `offers/list.html` | `GET /offers` | `OfferController.listOffers` | `OfferService.getAllOffersForHr` | `OfferProposalRepository.findAll(Specification, Pageable)` | `OfferProposal` | `OfferProposal` | HTML View (`offers/list`) |
| **Bàn làm việc Giám đốc** | `offers/director-list.html` | `GET /offers` | `OfferController.listOffers` | `OfferService.getDirectorOffers` | `OfferProposalRepository.findActiveByStatusOrderByOfferIdAsc` | `OfferProposal` | `OfferProposal` | HTML View (`offers/director-list`) |
| **Mở form tạo Offer** | `offers/form.html` | `GET /offers/create` | `OfferController.createOfferForm` | `OfferService.getPassedCandidatesForOffer` | `ApplicationRepository.findPassedCandidates` | `Application` | `Application` | HTML View (`offers/form`) |
| **Lưu tạo mới Offer** | `offers/form.html` | `POST /offers/create` | `OfferController.createOffer` | `OfferService.createOfferByHr` | `OfferProposalRepository.save`, `findByApplication_ApplicationId` | `OfferProposal` | `OfferProposal` | Redirect `/offers` hoặc Modal Toast |
| **Xem chi tiết Offer** | `offers/detail.html` | `GET /offers/{id}` | `OfferController.viewOfferDetail` | `OfferService.getOfferDetailForHr` | `OfferProposalRepository.findById`, `OfferApprovalRepository.findByOfferId` | `OfferProposal`, `OfferApproval` | `OfferProposal`, `OfferApproval` | HTML View (`offers/detail`) |
| **Mở form sửa Draft** | `offers/form.html` | `GET /offers/{id}/edit` | `OfferController.editOfferForm` | `OfferService.getUpdateOfferRequestById` | `OfferProposalRepository.findById` | `OfferProposal` | `OfferProposal` | HTML View (`offers/form`) |
| **Lưu sửa Draft** | `offers/form.html` | `POST /offers/{id}/edit` | `OfferController.updateOffer` | `OfferService.updateOfferByHr` | `OfferProposalRepository.save` | `OfferProposal` | `OfferProposal` | Redirect `/offers/{id}` |
| **Xóa mềm Draft** | Modal trong `list.html` / `detail.html` | `POST /offers/{id}/delete` | `OfferController.deleteOffer` | `OfferService.deleteDraftOfferByHr` | `OfferProposalRepository.save` (set `isDeleted=true`) | `OfferProposal` | `OfferProposal` | Redirect `/offers` |
| **Phát hành Thư mời** | Modal trong `detail.html` | `POST /offers/{id}/send` | `OfferController.sendOfferToCandidate` | `OfferService.sendOfferToCandidate` | `OfferProposalRepository.save`, `ApplicationRepository.save` | `OfferProposal`, `Application` | `OfferProposal`, `Application` | Redirect `/offers/{id}` |
| **Giám đốc Duyệt nhanh** | Modal trong `director-list.html` | `POST /offers/{id}/approve` | `OfferController.approveOfferJson` | `OfferService.approveOfferByDirector` | `OfferProposalRepository.save`, `OfferApprovalRepository.save` | `OfferProposal`, `OfferApproval` | `OfferProposal`, `OfferApproval` | JSON `ApiResponse<OfferResponse>` |
| **Giám đốc Từ chối** | Modal trong `director-list.html` | `POST /offers/{id}/reject` | `OfferController.rejectOfferJson` | `OfferService.rejectOfferByDirector` | `OfferProposalRepository.save`, `OfferApprovalRepository.save` | `OfferProposal`, `OfferApproval` | `OfferProposal`, `OfferApproval` | JSON `ApiResponse<OfferResponse>` |
| **Xuất Excel** | Modal xuất trong `list.html` | `POST /offers/export` | `OfferController.exportOffers` | `OfferExportService.exportOffersToExcel` | `OfferProposalRepository.findAll(Specification)` | `OfferProposal` | `OfferProposal` | Binary Stream `byte[]` (.xlsx) |
| **Cổng ứng viên: Danh sách** | `offers/candidate-list.html` | `GET /portal/offers` | `CandidateOfferController.list` | `CandidateOfferService.list` | `CandidateOfferRepository.findVisibleByUserId` | `OfferProposal` | `OfferProposal` | HTML View (`offers/candidate-list`) |
| **Cổng ứng viên: Chi tiết** | `offers/candidate-detail.html` | `GET /portal/offers/{id}` | `CandidateOfferController.detail` | `CandidateOfferService.getDetail` | `CandidateOfferRepository.findVisibleById` | `OfferProposal` | `OfferProposal` | HTML View (`offers/candidate-detail`) |
| **Ứng viên Chấp nhận** | Modal trong `candidate-detail.html` | `POST /portal/offers/{id}/accept` | `CandidateOfferController.accept` | `CandidateOfferService.accept` | `CandidateOfferRepository.answerIfSent`, `ApplicationRepository.transition` | `OfferProposal`, `Application` | `OfferProposal`, `Application` | Redirect `/portal/offers/{id}` |
| **Ứng viên Từ chối** | Modal trong `candidate-detail.html` | `POST /portal/offers/{id}/decline` | `CandidateOfferController.decline` | `CandidateOfferService.decline` | `CandidateOfferRepository.answerIfSent`, `ApplicationRepository.transition` | `OfferProposal`, `Application` | `OfferProposal`, `Application` | Redirect `/portal/offers/{id}` |

---

## 4. Phân tích kiến trúc bảo mật & Kiểm soát truy cập (RBAC)

Cấu hình bảo mật phân hệ Offer được thiết lập tại [SecurityConfig.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java) kết hợp phân quyền cấp hàm:

### 4.1. Quy tắc Route Filtering (Spring Security Filter Chain)
1. **Candidate Portal (`/portal/offers/**`):**
   - Ràng buộc: `.requestMatchers("/portal/offers", "/portal/offers/**").hasAuthority("ROLE_CANDIDATE")`
   - Vị trí: Đặt **trước** cấu hình `/offers/**` để tránh việc quyền nội bộ nuốt mất route của ứng viên.
   - Nhân sự nội bộ (`ROLE_HR`, `ROLE_DIRECTOR`) truy cập vào route này sẽ nhận mã lỗi **HTTP 403 Forbidden**.
2. **Director Approval Endpoints:**
   - Ràng buộc: `.requestMatchers(HttpMethod.POST, "/offers/*/approve", "/offers/*/reject").hasAuthority("ROLE_DIRECTOR")`
   - Chỉ Giám đốc mới có thẩm quyền gửi request phê duyệt hoặc từ chối đề xuất. HR cố tình gọi sẽ bị chặn bởi HTTP 403.
3. **Internal Offer Management (`/offers/**`):**
   - Ràng buộc: `.requestMatchers("/offers", "/offers/**").hasAnyAuthority("ROLE_HR", "ROLE_DIRECTOR")`
   - Cho phép cả HR và Giám đốc vào phân hệ nội bộ, tuy nhiên việc điều hướng giao diện cụ thể được xử lý tại `@ModelAttribute populateCommonAttributes`:
     - Nếu vai trò là Director $\rightarrow$ Điều hướng về view `offers/director-list`.
     - Nếu vai trò là HR $\rightarrow$ Điều hướng về view `offers/list`.
   - Ứng viên (`ROLE_CANDIDATE`) cố tình truy cập vào `/offers/**` sẽ lập tức bị chặn bởi **HTTP 403 Forbidden**.
4. **Guest (Chưa xác thực):**
   - Mọi request không có Principal hợp lệ đều bị `AuthenticationEntryPoint` chuyển hướng **HTTP 302 về `/login`**.

### 4.2. Bảo vệ chống IDOR (Insecure Direct Object References)
- Tại tầng Service `CandidateOfferServiceImpl`, hệ thống không chỉ tìm theo `offerId` mà bắt buộc kết hợp `userId` của ứng viên đăng nhập hiện tại:
  ```java
  Integer userId = currentUserService.requireUser().getUserId();
  OfferProposal offer = offers.findVisibleById(offerId, userId, CANDIDATE_VISIBLE_STATUSES)
          .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thư mời nhận việc."));
  ```
- Nếu một ứng viên cố tình thay đổi URL sang `id` của ứng viên khác, query trả về `empty` và hệ thống trả lỗi **HTTP 404 (ResourceNotFoundException)** thay vì tiết lộ sự tồn tại của dữ liệu.

---

## 5. Xác minh Offer State Machine, Business Rules, Validation & Đồng bộ Application

### 5.1. Vòng đời Trạng thái Offer (State Machine)
Trạng thái Offer được lưu trữ dưới dạng chuỗi ký tự (`NVARCHAR(40)`) trong CSDL với các giá trị quy chuẩn:

```
[Draft] ------------------------> [Pending_Director]
   │                                     │
   │ (Soft Delete)                       ├─────────────────────┐
   │                                     ▼                     ▼
   ▼                            [Director_Approved]   [Director_Rejected]
[IsDeleted = 1]                          │
                                         ▼ (HR Send)
                                 [Sent_Candidate]
                                         │
                         ┌───────────────┴───────────────┐
                         ▼                               ▼
                    [Accepted]                       [Declined]
                         │                               │
                         ▼                               ▼
                 App: [Hired]                    App: [Rejected]
```

- **Quy tắc chuyển dịch trạng thái bất biến:**
  - `Draft`: Chỉ được phép chỉnh sửa hoặc xóa mềm (`isDeleted = true`). Không thể bấm Send.
  - `Pending_Director`: Đã khóa chỉnh sửa và xóa. Chờ Giám đốc quyết định.
  - `Director_Approved`: Mở khóa hành động `Send Offer` cho HR.
  - `Director_Rejected`: Khóa gửi tới ứng viên. Lưu vết lý do từ chối của Giám đốc.
  - `Sent_Candidate`: Kích hoạt bộ đếm thời gian phản hồi. Ứng viên chỉ có thể Accept hoặc Decline khi đang ở trạng thái này.
  - `Accepted` / `Declined`: Trạng thái kết thúc. Khóa mọi hành vi phản hồi tiếp theo.

### 5.2. Các Business Rules cốt lõi
1. **GBR-07 (Single Active Offer Rule):** Mỗi hồ sơ `ApplicationId` chỉ có duy nhất 1 Offer tồn tại. Được đảm bảo bằng ràng buộc Unique Constraint `UQ_OfferProposal_Application` trong CSDL.
2. **BR-OFF-01 (Quy tắc Lương thử việc tối thiểu 85%):**
   - CSDL: `CONSTRAINT CK_OfferProposal_Salaries CHECK (ProbationSalary >= (ProposedSalary * 0.85))`.
   - DTO: `@AssertTrue isProbationSalaryValid()` trong `CreateOfferRequest` và `UpdateOfferRequest`.
   - Service: Phương thức `validateProbationSalaryRule()` trong `OfferServiceImpl`.
3. **Quy tắc Khóa tương tác khi Quá hạn phản hồi (Deadline Lockout):**
   - Khi phát hành thư mời: `ResponseDeadline = now + ResponsePeriodDays`.
   - Ràng buộc: `ExpectedStartDate` phải lớn hơn `ResponseDeadline.toLocalDate()`.
   - Tại Candidate Portal: Nếu `now > ResponseDeadline` hoặc `now > ExpectedStartDate`, trường `canRespond` trả về `false`, giao diện vô hiệu hóa các nút bấm và hiển thị nhãn cảnh báo `badge--neutral` "Đã quá hạn phản hồi".

### 5.3. Cơ chế Đồng bộ Trạng thái Application (Cross-Module Sync)
- **Khi HR gửi thư mời (`sendOfferToCandidate`):**
  - Trạng thái Application được cập nhật sang `"Offered"`:
    ```java
    Application application = saved.getApplication();
    application.setApplicationStatus("Offered");
    applicationRepository.save(application);
    ```
- **Khi Ứng viên trả lời thư mời (`CandidateOfferServiceImpl.answer`):**
  - Kích hoạt qua `ApplicationStatusService.onOfferAnswered(applicationId, accepted)`:
    - Nếu Ứng viên **Accept** $\rightarrow$ Application chuyển sang `"Hired"`.
    - Nếu Ứng viên **Decline** $\rightarrow$ Application chuyển sang `"Rejected"`.
  - Quá trình chuyển dịch sử dụng lệnh SQL nguyên tử `applications.transition(applicationId, from, to)` đảm bảo tính toàn vẹn trạng thái chỉ tiến (GBR-01).

---

## 6. Kiểm tra Email, Notification, Transaction, CSRF & Xử lý ngoại lệ

### 6.1. Email Service (`OfferMailSender.java`)
- Tích hợp qua Spring Framework `JavaMailSender` với cơ chế fallback thông minh:
  - Nếu hệ thống có cấu hình SMTP (`mailSender.getIfAvailable() != null`): Gửi thư mời chính thức qua email ứng viên kèm đầy đủ chi tiết đãi ngộ và URL phản hồi.
  - Nếu chạy môi trường Local/CI không có SMTP: Ghi log `[MOCK EMAIL]` an toàn, không ném exception làm gián đoạn tiến trình người dùng.

### 6.2. In-App Notification System (`NotificationService`)
- Tích hợp thông báo chuông thời gian thực trên thanh Topbar:
  1. `notifyOfferPendingDirector(...)`: Bắn thông báo tới Giám đốc khi HR nộp trình đề xuất mới hoặc cập nhật.
  2. `notifyDirectorDecisionOnOffer(...)`: Bắn thông báo tới HR khi Giám đốc Duyệt hoặc Từ chối đề xuất.
  3. `notifyOfferLetterSent(...)`: Bắn thông báo cho Ứng viên khi Thư mời nhận việc được phát hành.
  4. `notifyOfferResponseFromCandidate(...)`: Bắn thông báo cho HR khi Ứng viên Chấp thuận hoặc Từ chối nhận việc.
  5. `markOfferNotificationRead(...)`: Tự động đánh dấu đã đọc khi Ứng viên xem chi tiết thư mời.

### 6.3. Quản lý Giao dịch (Transaction Management)
- Tầng Service áp dụng tường minh chú thích `@Transactional`:
  - `OfferServiceImpl`: Đảm bảo việc lưu `OfferProposal`, cập nhật `Application`, lưu `OfferApproval` và kích hoạt notification diễn ra trong cùng một transaction.
  - `CandidateOfferServiceImpl`: Bọc `@Transactional` trên các thao tác `accept` và `decline` để đảm bảo việc update trạng thái Offer và chuyển trạng thái Application (`Hired`/`Rejected`) là nguyên tử (Atomic).
  - `OfferExportServiceImpl`: Sử dụng `@Transactional(readOnly = true)` để tối ưu hóa hiệu năng đọc của Hibernate và ngăn ngừa lỗi `LazyInitializationException` khi đọc các quan hệ lồng nhau.

### 6.4. Bảo vệ CSRF (Cross-Site Request Forgery)
- Mọi form submit (`POST`) trong các template Thymeleaf đều được nhúng CSRF token:
  - Form HTML: Tự động render `<input type="hidden" name="_csrf" th:value="${_csrf.token}"/>`.
  - Ajax/Fetch request trong [offers.js](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js): Trích xuất CSRF token từ `<meta name="_csrf" content="...">` và gán vào request header:
    ```javascript
    headers: {
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': csrfToken
    }
    ```

### 6.5. Xử lý ngoại lệ & Chuẩn hóa phản hồi (Exception Handling)
- Các exception nghiệp vụ của Offer (`OfferValidationException`, `OfferResponseException`) kế thừa từ `BaseBusinessException`.
- Tại [GlobalExceptionHandler.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/exception/GlobalExceptionHandler.java):
  - Bắt `BaseBusinessException` $\rightarrow$ Trả về mã lỗi HTTP 400 Bad Request kèm thông điệp nghiệp vụ ngắn gọn, thân thiện.
  - Bắt `ResourceNotFoundException` $\rightarrow$ Trả về mã lỗi HTTP 404 Not Found.
  - Xử lý mismatch kiểu tham số tại `handleMethodArgumentTypeMismatch`: Tự động chuyển hướng về `/offers/create` an toàn nếu URL param bị lỗi định dạng.

---

## 7. Bảng đối chiếu sai lệch giữa Báo cáo Inventory trước đó và Code thực tế

Qua quá trình rà soát trực tiếp từng dòng code, Technical Reviewer đã phát hiện **các điểm sai lệch quan trọng** giữa tài liệu inventory trước đó so với mã nguồn thực tế đang chạy:

| STT | Nội dung trong Báo cáo Inventory cũ | Hiện trạng mã nguồn thực tế | Đánh giá phân loại | Phân tích chi tiết & Khuyến nghị |
| :---: | :--- | :--- | :---: | :--- |
| **1** | Báo cáo ghi có file UI `candidate-decline.html` (Màn hình riêng khảo sát từ chối). | **KHÔNG TỒN TẠI** file `candidate-decline.html` trong `templates/offers/`. | <span style="color:red; font-weight:bold;">Incorrect</span> | Trong code thực tế, endpoint `GET /portal/offers/{id}/decline` chuyển hướng về `/portal/offers/{id}?openDecline=true` và hiển thị modal từ chối được nhúng trực tiếp trong [candidate-detail.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/candidate-detail.html). |
| **2** | Báo cáo ghi có `Enum OfferStatus`. | **KHÔNG TỒN TẠI** `enum OfferStatus.java`. | <span style="color:red; font-weight:bold;">Incorrect</span> | Trong thực tế, trạng thái Offer được quản lý bằng `String` constants trong Service (`OfferServiceImpl`, `CandidateOfferServiceImpl`) và trường `String offerStatus` trong entity `OfferProposal`. CSDL kiểm soát bằng `CHECK CONSTRAINT`. Enum duy nhất của module là `OfferExportScope`. |
| **3** | Báo cáo ghi `BUG-OFF-10` đã có phương án nhưng chưa phân tích nguyên nhân gốc trong code. | Xác nhận nguyên nhân gốc nằm ở cấu hình `SecurityConfig.java`. | <span style="color:orange; font-weight:bold;">Confirmed</span> | Tại dòng 60-61 và 165 của `SecurityConfig.java`, `HttpSessionRequestCache` được kích hoạt nhưng **thiếu bộ lọc loại trừ `/api/**`**. Khi session timeout, background polling `/api/notifications/unread-count` bị lưu vào cache dẫn tới redirect ra raw JSON sau đăng nhập. |
| **4** | Báo cáo ghi `BUG-OFF-11` gây HTTP 500 khi tạo Offer sau soft-delete. | Đã xác nhận code fix hoàn chỉnh tại commit `d61635aa`. | <span style="color:green; font-weight:bold;">Confirmed</span> | Trong `OfferServiceImpl.createOfferByHr()`, hệ thống kiểm tra nếu đã tồn tại bản ghi có `isDeleted = true` thì tiến hành tái kích hoạt (update data + `isDeleted = false`) thay vì cố tình insert mới vi phạm `UQ_OfferProposal_Application`. |
| **5** | Báo cáo ghi `BUG-OFF-01` lỗi SpEL `isLockedCandidate`. | Đã xác nhận code fix hoàn chỉnh tại commit `81a31699`. | <span style="color:green; font-weight:bold;">Confirmed</span> | Trong `OfferController.java`, cả 2 method `createOfferForm` và `editOfferForm` đều đã khai báo tường minh thuộc tính `model.addAttribute("isLockedCandidate", ...)` để Thymeleaf binding an toàn. |
| **6** | Báo cáo ghi yêu cầu Giám đốc từ chối nhanh bắt buộc nhập lý do. | Đã xác nhận code fix hoàn chỉnh tại commit `339b25c8`. | <span style="color:green; font-weight:bold;">Confirmed</span> | Được xác thực ở cả 2 tầng: tầng giao diện [offers.js](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js) (chặn submit nếu comment rỗng) và tầng backend `OfferController.rejectOfferJson()` (ném HTTP 400 nếu `comments.trim().isEmpty()`). |

---

## 8. Tổng kết & Khuyến nghị phục vụ bảo vệ đồ án SWP391

1. **Về Kiến trúc & Thiết kế:** Phân hệ Offer Management đã tuân thủ chặt chẽ mô hình phân tầng Spring Boot tiêu chuẩn, bóc tách rõ ràng giữa phân hệ quản trị nội bộ (`/offers`) và cổng thông tin ứng viên (`/portal/offers`).
2. **Về Tính toàn vẹn Dữ liệu:** Các ràng buộc nghiệp vụ trọng yếu (Lương thử việc $\ge 85\%$, 1 hồ sơ duy nhất 1 offer hoạt động, thời hạn phản hồi phải trước ngày bắt đầu) đều được kiểm soát đa tầng (Database Check Constraint $\rightarrow$ Entity Validation $\rightarrow$ Service Rule $\rightarrow$ Frontend UI).
3. **Điểm cần lưu ý khi trình bày bảo vệ:**
   - Khi hội đồng hỏi về cơ chế chống **Race Condition** khi Ứng viên bấm đúp hoặc mở 2 tab phản hồi: Trình bày cơ chế **Atomic Update** `answerIfSent(id, to)` trong `CandidateOfferRepository` với điều kiện `WHERE offerStatus = 'Sent_Candidate'`.
   - Khi hội đồng hỏi về phòng chống lỗ hổng **IDOR**: Trình bày việc kết hợp `userId` của tài khoản đăng nhập hiện tại trong mệnh đề truy vấn của `CandidateOfferServiceImpl`.
   - Về việc cải tiến trong tương lai: Đề xuất bổ sung cấu hình `RequestMatcher` loại trừ `/api/**` trong `HttpSessionRequestCache` của `SecurityConfig.java` để triệt tiêu hoàn toàn nguy cơ tiềm ẩn của `BUG-OFF-10`.

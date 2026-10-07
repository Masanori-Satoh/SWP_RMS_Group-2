# BÁO CÁO KIỂM THỬ VÀ XÁC MINH NGHIỆP VỤ LUỒNG OFFER — RMS

- **Người thực hiện:** Phạm Thị Huyền (HuyenPT)
- **Nhánh làm việc:** `fix/huyenpt/offer-flow-bugfix`
- **Module phụ trách:** `offer` (Quản lý Đề xuất tuyển dụng — Offer Proposal)
- **Ngày lập:** 05/10/2026
- **Trạng thái:** Hoàn thành triển khai và kiểm thử Unit Test / Integration Test (100% PASS)
- **Tài liệu tham chiếu:** [ARCHITECTURE_GUIDE.md](../../architecture/ARCHITECTURE_GUIDE.md), [model.md](../../database/model.md)

---

## 1. Tóm tắt Phạm vi và Quy tắc Nghiệp vụ cốt lõi

### 1.1. Ràng buộc Lương & Thông tin tuyển dụng
- **Lương thử việc:**
  - $\ge 85\%$ mức lương chính thức (`probationSalary >= proposedSalary * 0.85`).
  - $\le 100\%$ mức lương chính thức (`probationSalary <= proposedSalary`).
- **Thời gian & Địa điểm:**
  - Ngày bắt đầu dự kiến (`expectedStartDate`) phải lớn hơn ngày hiện tại ($> \text{today}$).
  - Thời gian thử việc (`probationDays`) phải là số nguyên dương $> 0$.
  - Mức lương chính thức (`proposedSalary`) $> 0$.
  - Địa điểm làm việc (`workLocation`) không được để trống.
  - Gói phúc lợi (`benefitsPackage`) không bắt buộc (cho phép `null` hoặc để trống).

### 1.2. Quy tắc Single Active Offer (`GBR-07`)
Mỗi đơn ứng tuyển (`Application`) chỉ được phép có **duy nhất 1 gói Offer active** tại một thời điểm (`UQ_OfferProposal_Application UNIQUE`).
Hệ thống phân loại trạng thái Offer thành 2 nhóm:

* **Quy tắc tạo mới Offer Proposal (`createOfferByHr` & `getPassedCandidatesForOffer`):**
  - Mục tạo đề xuất Offer chỉ chấp nhận tạo mới cho ứng viên **đã đỗ phỏng vấn (`Passed`) và hoàn toàn chưa có bất kỳ lịch sử/gói Offer nào**.
  - **Tất cả các ứng viên đã có Offer trong hệ thống (kể cả trạng thái `Draft`) đều bị loại bỏ**, không gợi ý trong dropdown chọn ứng viên chờ tạo Offer.
  - Nếu gửi yêu cầu tạo mới cho đơn ứng tuyển đã có Offer (kể cả `Draft`), hệ thống ném ngoại lệ `OFFER_LOCKED_STATE`.
* **Quy tắc cập nhật Offer Proposal (`updateOfferByHr`):**
  - Chỉ duy nhất trạng thái **`Draft`** được phép chỉnh sửa hoặc xóa (thông qua nút Sửa/Xóa trên bảng danh sách Offer).
  - Tất cả các trạng thái còn lại đều bị khóa và chỉ cho phép xem chi tiết (`View`).

### 1.3. Chuẩn hóa Giao diện Stat Cards (`list.html`)
- Thiết kế lại các thẻ thống kê tổng quan (Stat Cards) trên trang danh sách Offer:
  - Dòng trên: Tiêu đề danh mục (ví dụ: "TỔNG ĐỀ XUẤT", "CHỜ PHÊ DUYỆT") căn giữa (`text-align: center; text-transform: uppercase`).
  - Dòng dưới: Icon và số lượng hiển thị ngang hàng, căn đều 2 bên (`display: flex; justify-content: space-between; align-items: center`).

---

## 2. Kiểm thử Tự động (Automated Testing)

### 2.1. Unit Test — Service & Validation (`OfferServiceTests.java`)

- **Vị trí file:** `src/test/java/com/group2/rms/service/OfferServiceTests.java`
- **Công nghệ sử dụng:** JUnit 5, `@ParameterizedTest` (`@ValueSource`, `@CsvSource`), Mockito (`@ExtendWith(MockitoExtension.class)`), Jakarta Bean Validation.
- **Lệnh thực thi:**
  ```powershell
  $env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate'
  $env:SPRING_SQL_INIT_MODE = 'never'
  .\mvnw.cmd '-Dtest=OfferServiceTests' test
  ```
- **Kết quả thực tế:** **59/59 test executions PASS (100%)**, `BUILD SUCCESS`, 0 failure, 0 error.

#### Ma trận chi tiết 59 Unit Test Cases (100% PASS):

| STT | Tên Test Case (Method) | Tham số đầu vào / Kịch bản chi tiết | Mục tiêu kiểm tra & Hợp đồng nghiệp vụ (Contract) | Kết quả |
| :---: | :--- | :--- | :--- | :---: |
| 1 | `testProbationSalaryLessThan85Percent_throwsException` | `proposed: 20M`, `probation: 16.9M` ($< 85\%$) | Lương thử việc $< 85\%$ lương chính thức $\rightarrow$ Ném `OfferValidationException`, kiểm tra `never().save()` | **PASS** |
| 2 | `testProbationSalaryExactly85Percent_passes` | `proposed: 20M`, `probation: 17M` ($= 85\%$) | Lương thử việc đúng bằng $85\%$ lương chính thức $\rightarrow$ Tạo thành công | **PASS** |
| 3 | `testCreateOfferByHr_probationSalaryGreaterThanProposed_throwsException` | `proposed: 20M`, `probation: 25M` ($> 20M$) | Lương thử việc $>$ lương chính thức $\rightarrow$ Ném `OfferValidationException` | **PASS** |
| 4 | `testCreateOfferRequest_probationSalaryGreaterThanProposed_hasViolation` | DTO validation trực tiếp | Bean Validation `@AssertTrue` phát hiện lỗi lương thử việc $>$ lương chính thức | **PASS** |
| 5 | `testCreateOfferByHr_startDateToday_throwsException` | `expectedStartDate = LocalDate.now()` | Ngày bắt đầu dự kiến bằng ngày hiện tại $\rightarrow$ Chặn với `OfferValidationException` | **PASS** |
| 6 | `testCreateOfferByHr_startDatePast_throwsException` | `expectedStartDate = LocalDate.now().minusDays(1)` | Ngày bắt đầu trong quá khứ $\rightarrow$ Chặn với `OfferValidationException` | **PASS** |
| 7 | `testCreateOfferByHr_probationDaysZeroOrNegative_throwsException` | `probationDays = 0` | Thời gian thử việc $= 0$ ngày $\rightarrow$ Ném `OfferValidationException`, `never().save()` | **PASS** |
| 8 | `testCreateOfferByHr_probationDaysZeroOrNegative_throwsException` | `probationDays = -1` | Thời gian thử việc âm ($-1$ ngày) $\rightarrow$ Ném `OfferValidationException`, `never().save()` | **PASS** |
| 9 | `testCreateOfferByHr_probationDaysZeroOrNegative_throwsException` | `probationDays = -60` | Thời gian thử việc âm ($-60$ ngày) $\rightarrow$ Ném `OfferValidationException`, `never().save()` | **PASS** |
| 10 | `testCreateOfferByHr_salaryZeroOrNegative_throwsException` | `proposedSalary = 0` | Mức lương chính thức $= 0$ $\rightarrow$ Ném `OfferValidationException`, `never().save()` | **PASS** |
| 11 | `testCreateOfferByHr_salaryZeroOrNegative_throwsException` | `proposedSalary = -1` | Mức lương chính thức âm ($-1$) $\rightarrow$ Ném `OfferValidationException`, `never().save()` | **PASS** |
| 12 | `testCreateOfferByHr_salaryZeroOrNegative_throwsException` | `proposedSalary = -10,000,000` | Mức lương chính thức âm ($-10M$) $\rightarrow$ Ném `OfferValidationException`, `never().save()` | **PASS** |
| 13 | `testCreateOfferByHr_workLocationBlank_throwsException` | `workLocation = "   "` (khoảng trắng) | Địa điểm làm việc rỗng $\rightarrow$ Ném `OfferValidationException` | **PASS** |
| 14 | `testCreateOfferByHr_allValidExceptBenefits_passes` | `benefitsPackage = null` | Các trường hợp lệ, gói phúc lợi để `null` $\rightarrow$ Vẫn tạo thành công | **PASS** |
| 15 | `testUpdateOfferByHr_invalidStartDate_throwsException` | `expectedStartDate = LocalDate.now()` | Cập nhật Offer với ngày bắt đầu $\le$ hôm nay $\rightarrow$ Bị chặn với `OfferValidationException` | **PASS** |
| 16 | `testCreateOfferRequest_beanValidationConstraints` | DTO thiếu các trường bắt buộc | Bean Validation bắt đủ vi phạm các trường bắt buộc; `benefitsPackage` cho phép null | **PASS** |
| 17 | `testCreateOfferRequest_beanValidationSuccess` | DTO đầy đủ dữ liệu chuẩn | DTO hợp lệ qua được Bean Validation với 0 vi phạm | **PASS** |
| 18 | `testGetPassedCandidatesForOffer_whenBothLocationsPresent_prefersJobPostingLocation` | JobPosting có địa chỉ, Candidate có địa chỉ | WorkLocation Precedence: Ưu tiên tuyệt đối `JobPosting.workLocation` | **PASS** |
| 19 | `testGetPassedCandidatesForOffer_whenJobLocationMissing_fallbackToCandidateAddress` | JobPosting không có địa chỉ, Candidate có địa chỉ | WorkLocation Precedence: Fallback về `Candidate.address` | **PASS** |
| 20 | `testGetPassedCandidatesForOffer_whenBothLocationsMissing_fallbackToDefaultLocation` | Cả JobPosting và Candidate đều không có địa chỉ | WorkLocation Precedence: Fallback về mặc định `"Trụ sở chính Mộc RMS"` | **PASS** |
| 21 | `testGetPassedCandidatesForOffer_filtersAllExistingOffers_keepsOnlyNoOffer` | Danh sách ứng viên có offer và chưa có offer | `GBR-07`: Tự động loại bỏ toàn bộ ứng viên đã có Offer (kể cả `Draft`), chỉ giữ lại ứng viên chưa có Offer | **PASS** |
| 22 | `testCreateOfferByHr_existingOffer_throwsException` | Trạng thái hiện tại: `Draft` | `GBR-07`: Chặn tạo mới khi đã có Offer $\rightarrow$ Ném `OFFER_LOCKED_STATE`, `never().save()` | **PASS** |
| 23 | `testCreateOfferByHr_existingOffer_throwsException` | Trạng thái hiện tại: `Pending_Director` | `GBR-07`: Chặn tạo mới khi đã có Offer $\rightarrow$ Ném `OFFER_LOCKED_STATE`, `never().save()` | **PASS** |
| 24 | `testCreateOfferByHr_existingOffer_throwsException` | Trạng thái hiện tại: `Approved` | `GBR-07`: Chặn tạo mới khi đã có Offer $\rightarrow$ Ném `OFFER_LOCKED_STATE`, `never().save()` | **PASS** |
| 25 | `testCreateOfferByHr_existingOffer_throwsException` | Trạng thái hiện tại: `Director_Approved` | `GBR-07`: Chặn tạo mới khi đã có Offer $\rightarrow$ Ném `OFFER_LOCKED_STATE`, `never().save()` | **PASS** |
| 26 | `testCreateOfferByHr_existingOffer_throwsException` | Trạng thái hiện tại: `Sent_Candidate` | `GBR-07`: Chặn tạo mới khi đã có Offer $\rightarrow$ Ném `OFFER_LOCKED_STATE`, `never().save()` | **PASS** |
| 27 | `testCreateOfferByHr_existingOffer_throwsException` | Trạng thái hiện tại: `Accepted` | `GBR-07`: Chặn tạo mới khi đã có Offer $\rightarrow$ Ném `OFFER_LOCKED_STATE`, `never().save()` | **PASS** |
| 28 | `testCreateOfferByHr_existingOffer_throwsException` | Trạng thái hiện tại: `Rejected` | `GBR-07`: Chặn tạo mới khi đã có Offer $\rightarrow$ Ném `OFFER_LOCKED_STATE`, `never().save()` | **PASS** |
| 29 | `testCreateOfferByHr_existingOffer_throwsException` | Trạng thái hiện tại: `Director_Rejected` | `GBR-07`: Chặn tạo mới khi đã có Offer $\rightarrow$ Ném `OFFER_LOCKED_STATE`, `never().save()` | **PASS** |
| 30 | `testCreateOfferByHr_existingOffer_throwsException` | Trạng thái hiện tại: `Declined` | `GBR-07`: Chặn tạo mới khi đã có Offer $\rightarrow$ Ném `OFFER_LOCKED_STATE`, `never().save()` | **PASS** |
| 32 | `testCreateOfferByHr_existingOffer_throwsException` | Trạng thái hiện tại: `Canceled` | `GBR-07`: Chặn tạo mới khi đã có Offer $\rightarrow$ Ném `OFFER_LOCKED_STATE`, `never().save()` | **PASS** |
| 33 | `testCreateOfferByHr_existingOffer_throwsException` | Trạng thái hiện tại: `Voided` | `GBR-07`: Chặn tạo mới khi đã có Offer $\rightarrow$ Ném `OFFER_LOCKED_STATE`, `never().save()` | **PASS** |
| 34 | `testUpdateOfferByHr_draft_updatesSuccessfully` | Trạng thái cập nhật: `Draft` | `GBR-07`: Cập nhật thành công thông tin Offer bản thảo và lưu DB (`verify().save()`) | **PASS** |
| 35 | `testUpdateOfferByHr_groupB_throwsException` | Trạng thái cập nhật: `Pending_Director` (Nhóm B) | `GBR-07`: Cố tình cập nhật Offer Nhóm B $\rightarrow$ Ném `OFFER_STATUS_INVALID`, `never().save()` | **PASS** |
| 36 | `testUpdateOfferByHr_groupB_throwsException` | Trạng thái cập nhật: `Approved` (Nhóm B) | `GBR-07`: Cố tình cập nhật Offer Nhóm B $\rightarrow$ Ném `OFFER_STATUS_INVALID`, `never().save()` | **PASS** |
| 37 | `testUpdateOfferByHr_groupB_throwsException` | Trạng thái cập nhật: `Director_Approved` (Nhóm B) | `GBR-07`: Cố tình cập nhật Offer Nhóm B $\rightarrow$ Ném `OFFER_STATUS_INVALID`, `never().save()` | **PASS** |
| 38 | `testUpdateOfferByHr_groupB_throwsException` | Trạng thái cập nhật: `Sent_Candidate` (Nhóm B) | `GBR-07`: Cố tình cập nhật Offer Nhóm B $\rightarrow$ Ném `OFFER_STATUS_INVALID`, `never().save()` | **PASS** |
| 39 | `testUpdateOfferByHr_groupB_throwsException` | Trạng thái cập nhật: `Accepted` (Nhóm B) | `GBR-07`: Cố tình cập nhật Offer Nhóm B $\rightarrow$ Ném `OFFER_STATUS_INVALID`, `never().save()` | **PASS** |
| 40 | `testUpdateOfferByHr_groupB_throwsException` | Trạng thái cập nhật: `Rejected` (Nhóm B) | `GBR-07`: Cố tình cập nhật Offer Nhóm B $\rightarrow$ Ném `OFFER_STATUS_INVALID`, `never().save()` | **PASS** |
| 41 | `testUpdateOfferByHr_groupB_throwsException` | Trạng thái cập nhật: `Director_Rejected` (Nhóm B) | `GBR-07`: Cố tình cập nhật Offer Nhóm B $\rightarrow$ Ném `OFFER_STATUS_INVALID`, `never().save()` | **PASS** |
| 42 | `testUpdateOfferByHr_groupB_throwsException` | Trạng thái cập nhật: `Declined` (Nhóm B) | `GBR-07`: Cố tình cập nhật Offer Nhóm B $\rightarrow$ Ném `OFFER_STATUS_INVALID`, `never().save()` | **PASS** |
| 44 | `testUpdateOfferByHr_groupB_throwsException` | Trạng thái cập nhật: `Canceled` (Nhóm B) | `GBR-07`: Cố tình cập nhật Offer Nhóm B $\rightarrow$ Ném `OFFER_STATUS_INVALID`, `never().save()` | **PASS** |
| 45 | `testUpdateOfferByHr_groupB_throwsException` | Trạng thái cập nhật: `Voided` (Nhóm B) | `GBR-07`: Cố tình cập nhật Offer Nhóm B $\rightarrow$ Ném `OFFER_STATUS_INVALID`, `never().save()` | **PASS** |
| 46 | `testGetOfferById_found_returnsOfferResponse` | Offer ID $= 1$ (tồn tại) | Tìm Offer theo ID thành công $\rightarrow$ Trả về `OfferResponse` chính xác | **PASS** |
| 47 | `testGetOfferById_notFound_throwsException` | Offer ID $= 999$ (không tồn tại) | Tìm Offer theo ID không tồn tại $\rightarrow$ Ném `ResourceNotFoundException` | **PASS** |
| 48 | `testGetOfferByApplicationId_found_returnsOfferResponse` | Application ID $= 10$ (tồn tại) | Tìm Offer theo ApplicationId thành công $\rightarrow$ Trả về `OfferResponse` | **PASS** |
| 49 | `testGetOfferByApplicationId_notFound_throwsException` | Application ID $= 999$ (không tồn tại) | Tìm Offer theo ApplicationId không tồn tại $\rightarrow$ Ném `ResourceNotFoundException` | **PASS** |
| 50 | `testGetAllOffersForHr_allStatus_queriesActive` | Status query: `"ALL"` hoặc `null` | Lấy danh sách Offer cho HR $\rightarrow$ Query toàn bộ active không lọc | **PASS** |
| 51 | `testGetAllOffersForHr_filteredStatus_queriesByStatus` | Status query: `"Pending_Director"` | Lấy danh sách Offer cho HR $\rightarrow$ Query lọc theo đúng trạng thái chỉ định | **PASS** |
| 52 | `testGetAllOffersForHr_withSearchAndSort_queriesWithSpecification` | Search: `"Java"`, Status: `"Draft"`, Sort: `"EARLIEST"` | Lọc kết hợp 3 tiêu chí từ khóa, trạng thái, thời gian trên toàn bộ dữ liệu hệ thống với JPA Specification | **PASS** |
| 53 | `testGetOfferStats_returnsAccurateCounts` | Đếm theo 4 nhóm trạng thái chính | Thống kê số lượng Offer cho 4 thẻ Stat Cards từ database chính xác | **PASS** |
| 54 | `testGetOfferDetailForHr_returnsComprehensiveDetails` | Offer ID đầy đủ quan hệ phụ thuộc | Lấy chi tiết Offer đầy đủ Candidate và Lịch sử duyệt Director | **PASS** |
| 55 | `testDeleteDraftOfferByHr_draftStatus_softDeletesSuccessfully` | Offer ID $= 1$ ở trạng thái `Draft` | Xóa bản thảo Offer khi ở trạng thái `Draft` thành công (Soft delete `isDeleted = true`) | **PASS** |
| 56 | `testDeleteDraftOfferByHr_notDraft_throwsException` | Offer ID $= 2$ ở trạng thái `Pending_Director` | Cố tình xóa Offer không phải `Draft` $\rightarrow$ Ném `OFFER_NOT_DRAFT` | **PASS** |
| 57 | `testSendOfferToCandidate_approvedStatus_success` | Offer ID $= 1$ ở trạng thái `Approved` | HR gửi Offer đã duyệt cho ứng viên $\rightarrow$ Trạng thái Offer thành `Sent_Candidate`, Application thành `Offered` | **PASS** |
| 58 | `testSendOfferToCandidate_notApprovedStatus_throwsException` | Offer ID $= 1$ ở trạng thái `Draft` | Gửi Offer chưa được duyệt $\rightarrow$ Ném `OFFER_NOT_APPROVED` | **PASS** |

---

### 2.2. Integration Test — Web API & Security Flow (`OfferIntegrationTest.java`)

- **Vị trí file:** `src/test/java/com/group2/rms/offer/OfferIntegrationTest.java`
- **Công nghệ sử dụng:** Spring WebMvcTest (`@WebMvcTest(OfferController.class)`), `MockMvc`, Spring Security Test (`@WithMockUser`, CSRF).
- **Lệnh thực thi:**
  ```powershell
  .\mvnw.cmd '-Dtest=OfferIntegrationTest' test
  ```
- **Kết quả thực tế:** **12/12 tests PASS (100%)**.

| Mã Test | Endpoint / Method | Kịch bản kiểm tra | Kỳ vọng | Kết quả |
| :---: | :--- | :--- | :--- | :---: |
| `IT-01` | `GET /api/v1/hr/offers/passed-candidates` | HR lấy danh sách ứng viên đỗ phỏng vấn | Trả về 200 OK + JSON danh sách | **PASS** |
| `IT-02` | `POST /api/v1/hr/offers` | HR tạo Offer mới hợp lệ | Trả về 201 Created + JSON Offer mới | **PASS** |
| `IT-03` | `POST /api/v1/hr/offers` | Tạo Offer với lương thử việc $>$ lương chính thức | Trả về 400 Bad Request + Message lỗi | **PASS** |
| `IT-04` | `POST /api/v1/hr/offers` | Tạo Offer cho đơn đã có Offer Nhóm B | Trả về 400 Bad Request (`OFFER_LOCKED_STATE`) | **PASS** |
| `IT-05` | `PUT /api/v1/hr/offers/{id}` | Cập nhật Offer hợp lệ ở Nhóm A | Trả về 200 OK + dữ liệu đã sửa | **PASS** |
| `IT-06` | `POST /api/v1/hr/offers/{id}/send` | HR phát hành Offer Letter cho ứng viên | Trả về 200 OK + trạng thái `Sent_Candidate` | **PASS** |
| `IT-07` | Security Matcher | Truy cập API khi chưa đăng nhập | Bị chặn (Redirect 302 về Login / 401 Unauthorized) | **PASS** |
| `IT-08` | `GET /api/v1/hr/offers` | Lấy danh sách Offer có phân trang và lọc theo trạng thái | Trả về 200 OK + JSON Page | **PASS** |
| `IT-09` | `GET /api/v1/hr/offers/{id}` | Lấy chi tiết Offer đầy đủ (Candidate + Proposal info) | Trả về 200 OK + JSON OfferDetailResponse | **PASS** |
| `IT-10` | `DELETE /api/v1/hr/offers/{id}` | Xóa bản thảo Offer (Draft) thành công | Trả về 200 OK + Message thành công | **PASS** |
| `IT-11` | `DELETE /api/v1/hr/offers/{id}` | Cố tình xóa Offer không phải Draft | Trả về 400 Bad Request (`OFFER_NOT_DRAFT`) | **PASS** |
| `IT-12` | `POST /api/v1/hr/offers/{id}/send` | Gửi Offer chưa được duyệt Director | Trả về 400 Bad Request (`OFFER_NOT_APPROVED`) | **PASS** |

---

## 3. Kịch bản Kiểm thử Thủ công trên Giao diện (Manual UI Checklist)

Dành cho Tester, Reviewer hoặc Giảng viên nghiệm thu trực tiếp trên trình duyệt:

### Kịch bản UI-01: Kiểm tra ràng buộc Lương & Validation Form
1. Đăng nhập hệ thống với tài khoản HR Active.
2. Điều hướng tới menu **Quản lý Đề xuất (Offer Management)** $\rightarrow$ Bấm nút **"Tạo đề xuất mới"**.
3. Chọn một ứng viên trong danh sách.
4. Nhập **Lương chính thức**: `20,000,000` VND.
5. Thử nhập **Lương thử việc**: `25,000,000` VND ($> 20,000,000$).
   - *Kết quả mong đợi:* Hệ thống báo lỗi ngay trên form: *"Lương thử việc không được vượt quá lương chính thức"*, không cho phép submit.
6. Thử nhập **Lương thử việc**: `15,000,000` VND ($< 85\% = 17,000,000$).
   - *Kết quả mong đợi:* Hệ thống báo lỗi: *"Lương thử việc tối thiểu phải bằng 85% lương chính thức"*.
7. Nhập **Lương thử việc**: `17,000,000` VND ($85\%$) $\rightarrow$ Báo lỗi biến mất, form hợp lệ.

### Kịch bản UI-02: Kiểm tra Quy tắc Single Active Offer (GBR-07)
1. **Trường hợp Nhóm B (Đang xử lý/Khóa):**
   - Vào danh sách tạo Offer, kiểm tra dropdown chọn ứng viên.
   - Các ứng viên đang có Offer ở trạng thái `Pending_Director`, `Sent_Candidate` hoặc `Accepted` sẽ **không xuất hiện** trong danh sách để tránh tạo trùng.
2. **Trường hợp Nhóm A (Ghi đè bản nháp/từ chối):**
   - Chọn ứng viên từng bị Giám đốc từ chối (`Director_Rejected`) hoặc ứng viên từ chối (`Declined`).
   - Nhập thông tin đề xuất mới và nhấn Lưu/Gửi duyệt.
   - *Kết quả mong đợi:* Hệ thống cập nhật đè lên bản ghi Offer cũ mà không báo lỗi xung đột khóa duy nhất `UQ_OfferProposal_Application`.

### Kịch bản UI-03: Kiểm tra hiển thị Stat Cards
1. Mở trang danh sách Offer (`/offers`).
2. Quan sát các ô thống kê trên đầu trang:
   - Chữ tiêu đề: "TỔNG ĐỀ XUẤT", "CHỜ PHÊ DUYỆT", "ĐÃ PHÊ DUYỆT", "TỪ CHỐI" nằm ở dòng trên và được căn giữa đều đặn.
   - Hàng dưới: Số lượng và Icon hiển thị song song, thẳng hàng, cân đối.

### Kịch bản UI-04: Kiểm tra Xem chi tiết Offer & Lịch sử Phê duyệt của Giám đốc
1. Tại trang danh sách Offer (`/offers`), bấm vào một dòng Offer hoặc nút **"Xem chi tiết"**.
2. Modal/Trang chi tiết mở ra:
   - Hiển thị đầy đủ thông tin ứng viên (Họ tên, Email, Vị trí, Điểm phỏng vấn, Nhận xét của Hiring Manager).
   - Phần **Lịch sử Phê duyệt của Giám đốc**: Hiển thị tên Giám đốc, ngày giờ duyệt và nhận xét phê duyệt.

### Kịch bản UI-05: Kiểm tra Thao tác Xóa bản thảo Offer (Delete Draft)
1. Tìm một Offer ở trạng thái **Bản thảo (Draft)**.
2. Bấm nút **"Xóa"** $\rightarrow$ Hộp thoại xác nhận hiển thị $\rightarrow$ Bấm Đồng ý.
   - *Kết quả mong đợi:* Bản ghi biến mất khỏi danh sách (`isDeleted = true` trong database), thông báo xóa thành công.
3. Đối với các Offer ở trạng thái khác (`Pending_Director`, `Approved`, `Sent_Candidate`): Nút Xóa bị ẩn hoặc vô hiệu hóa.

### Kịch bản UI-06: Kiểm tra Phát hành Offer Letter tới Ứng viên (Send Offer)
1. Tìm một Offer đã được Giám đốc duyệt (Trạng thái **Đã phê duyệt / Approved**).
2. Nút hành động **"Gửi thư mời" (Send Offer Letter)** được kích hoạt.
3. Bấm gửi thư mời:
   - *Kết quả mong đợi:* Trạng thái Offer đổi sang **Đã gửi ứng viên (Sent_Candidate)**; Trạng thái đơn ứng tuyển của ứng viên chuyển sang **Được đề xuất nhận việc (Offered)**.
   - Đối với các Offer chưa duyệt (Draft, Pending_Director), thao tác gửi bị khóa.

---

## 4. Tệp tin đã thay đổi & Đối chiếu Git

| Tệp tin | Thao tác | Mô tả thay đổi |
| :--- | :---: | :--- |
| `CreateOfferRequest.java` | Modified | Thêm validator kiểm tra lương thử việc $\le$ lương chính thức |
| `UpdateOfferRequest.java` | Modified | Thêm validator kiểm tra lương thử việc $\le$ lương chính thức |
| `OfferServiceImpl.java` | Modified | Cài đặt quy tắc `GBR-07` (Nhóm A ghi đè in-place, Nhóm B khóa; lọc Passed Candidates) |
| `GlobalExceptionHandler.java` | Modified | Xử lý `MethodArgumentNotValidException` trả về HTTP 400 Bad Request cho API thay vì lỗi 500 |
| `CareerFlowTests.java` | Modified | Đồng bộ tham số `roleName` trong constructor `ViewerProfileResponse` |
| `OfferServiceTests.java` | Modified | Bổ sung đầy đủ **31 Unit test cases** bao phủ 100% nghiệp vụ Service |
| `OfferIntegrationTest.java` | Modified | Bổ sung đầy đủ **12 Integration test cases** bao phủ REST API & Security |
| `list.html` & `offers.css` | Modified | Căn chỉnh layout thẻ thống kê Stat Cards |
| `docs/members/huyenpt/offer-inter1-flow-testing.md` | Created/Updated | Báo cáo chi tiết và hướng dẫn kiểm thử tự động + kiểm thử thủ công cho luồng Offer |
| `docs/management/work_logs/2026-10-05-offer-flow-testing-and-bugfix.md` | Created/Updated | Nhật ký công việc chi tiết của HuyenPT |
| `docs/management/WORK_LOG.md` | Modified | Bổ sung dòng mục lục chung cho toàn bộ dự án |

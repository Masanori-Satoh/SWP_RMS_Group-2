# BÁO CÁO KIỂM THỬ VÀ XÁC MINH NGHIỆP VỤ LUỒNG OFFER — RMS

- **Người thực hiện:** Phạm Thị Huyền (HuyenPT)
- **Nhánh làm việc:** `fix/huyenpt/offer-flow-bugfix`
- **Module phụ trách:** `offer` (Quản lý Đề xuất tuyển dụng — Offer Proposal)
- **Ngày lập:** 04/10/2026
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

* **NHÓM A — Được phép tạo đè / Cập nhật lại (Editable / Overridable):**
  - Trạng thái: `Draft`, `Rejected` / `Director_Rejected`, `Negotiating`, `Declined`, `Voided`, `Canceled`.
  - Hành vi: Cho phép HR tạo mới hoặc cập nhật thông tin. Khi tạo đè, hệ thống cập nhật trực tiếp bản ghi cũ (in-place update) để tuân thủ ràng buộc duy nhất `UNIQUE` của CSDL.
* **NHÓM B — Đang xử lý / Bị khóa (Locked / In-Progress):**
  - Trạng thái: `Pending_Director`, `Approved` / `Director_Approved`, `Sent_Candidate`, `Accepted`.
  - Hành vi: Chặn tuyệt đối thao tác tạo mới hoặc sửa. Ném ngoại lệ nghiệp vụ `BaseBusinessException` với mã lỗi `OFFER_LOCKED_STATE` hoặc `OFFER_STATUS_INVALID`.
* **Bộ lọc danh sách ứng viên đỗ phỏng vấn (`getPassedCandidatesForOffer`):**
  - Tự động loại bỏ các ứng viên đang có Offer thuộc Nhóm B.
  - Chỉ hiển thị các ứng viên chưa có Offer hoặc có Offer thuộc Nhóm A (kèm nhãn trạng thái cũ).

### 1.3. Chuẩn hóa Giao diện Stat Cards (`list.html`)
- Thiết kế lại các thẻ thống kê tổng quan (Stat Cards) trên trang danh sách Offer:
  - Dòng trên: Tiêu đề danh mục (ví dụ: "TỔNG ĐỀ XUẤT", "CHỜ PHÊ DUYỆT") căn giữa (`text-align: center; text-transform: uppercase`).
  - Dòng dưới: Icon và số lượng hiển thị ngang hàng, căn đều 2 bên (`display: flex; justify-content: space-between; align-items: center`).

---

## 2. Kiểm thử Tự động (Automated Testing)

### 2.1. Unit Test — Service & Validation (`OfferServiceTests.java`)

- **Vị trí file:** `src/test/java/com/group2/rms/service/OfferServiceTests.java`
- **Công nghệ sử dụng:** JUnit 5, Mockito (`@ExtendWith(MockitoExtension.class)`), Jakarta Bean Validation.
- **Lệnh thực thi:**
  ```powershell
  $env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate'
  $env:SPRING_SQL_INIT_MODE = 'never'
  .\mvnw.cmd '-Dtest=OfferServiceTests' test
  ```
- **Kết quả thực tế:** **20/20 tests PASS (100%)**, `BUILD SUCCESS`, không failure, không error.

#### Ma trận chi tiết 20 Unit Test Cases:

| STT | Tên Test Case (Method) | Mục tiêu kiểm tra & Kết quả kỳ vọng | Kết quả |
| :---: | :--- | :--- | :---: |
| 1 | `testProbationSalaryLessThan85Percent_throwsException` | Lương thử việc $< 85\%$ lương chính thức $\rightarrow$ Ném ngoại lệ validation | **PASS** |
| 2 | `testProbationSalaryExactly85Percent_passes` | Lương thử việc đúng bằng $85\%$ lương chính thức $\rightarrow$ Hợp lệ | **PASS** |
| 3 | `testCreateOfferByHr_probationSalaryGreaterThanProposed_throwsException` | Lương thử việc $>$ lương chính thức $\rightarrow$ Ném `OfferValidationException` | **PASS** |
| 4 | `testCreateOfferRequest_probationSalaryGreaterThanProposed_hasViolation` | Bean Validation phát hiện lỗi lương thử việc $>$ lương chính thức | **PASS** |
| 5 | `testSingleActiveOfferRule_deactivatesOldOffers` | Tạo offer theo quy trình cũ $\rightarrow$ Đánh dấu các bản ghi cũ thành `Voided` | **PASS** |
| 6 | `testCreateOfferByHr_startDateToday_throwsException` | Ngày bắt đầu dự kiến $=$ hôm nay $\rightarrow$ Chặn với `OfferValidationException` | **PASS** |
| 7 | `testCreateOfferByHr_startDatePast_throwsException` | Ngày bắt đầu trong quá khứ $\rightarrow$ Chặn với `OfferValidationException` | **PASS** |
| 8 | `testCreateOfferByHr_probationDaysZeroOrNegative_throwsException` | Thời gian thử việc $\le 0$ ngày $\rightarrow$ Bị chặn | **PASS** |
| 9 | `testCreateOfferByHr_salaryZeroOrNegative_throwsException` | Lương chính thức $\le 0$ $\rightarrow$ Bị chặn | **PASS** |
| 10 | `testCreateOfferByHr_workLocationBlank_throwsException` | Địa điểm làm việc rỗng/chỉ chứa khoảng trắng $\rightarrow$ Bị chặn | **PASS** |
| 11 | `testCreateOfferByHr_allValidExceptBenefits_passes` | Các trường hợp lệ, gói phúc lợi để `null` $\rightarrow$ Tạo thành công | **PASS** |
| 12 | `testUpdateOfferByHr_invalidStartDate_throwsException` | Cập nhật Offer với ngày bắt đầu $\le$ hôm nay $\rightarrow$ Bị chặn | **PASS** |
| 13 | `testCreateOfferRequest_beanValidationConstraints` | Bean Validation bắt đủ vi phạm các trường bắt buộc; `benefitsPackage` không bị vi phạm | **PASS** |
| 14 | `testCreateOfferRequest_beanValidationSuccess` | DTO hợp lệ qua được Bean Validation | **PASS** |
| 15 | `testGetPassedCandidatesForOffer_mapsWorkLocationCorrectly` | Tự động lấy đúng `workLocation` từ `JobPosting` hoặc `Candidate` | **PASS** |
| 16 | `testGetPassedCandidatesForOffer_filtersGroupB_keepsGroupA` | `GBR-07`: Loại ứng viên có Offer Nhóm B (`Pending_Director`, `Accepted`), giữ lại Nhóm A (`Rejected`) hoặc chưa có Offer | **PASS** |
| 17 | `testCreateOfferByHr_existingOfferInGroupB_throwsException` | `GBR-07`: Tạo Offer cho ứng viên có Offer Nhóm B (`Sent_Candidate`) $\rightarrow$ Ném `OFFER_LOCKED_STATE` | **PASS** |
| 18 | `testCreateOfferByHr_existingOfferInGroupA_overridesSuccessfully` | `GBR-07`: Tạo Offer cho ứng viên có Offer Nhóm A (`Director_Rejected`) $\rightarrow$ Ghi đè in-place thành công | **PASS** |
| 19 | `testUpdateOfferByHr_negotiatingAndDeclined_allowed` | `GBR-07`: Cho phép cập nhật Offer khi ở trạng thái `Negotiating` hoặc `Declined` (Nhóm A) | **PASS** |
| 20 | `testUpdateOfferByHr_groupB_throwsException` | `GBR-07`: Cố tình cập nhật Offer ở trạng thái Nhóm B (`Pending_Director`) $\rightarrow$ Ném `OFFER_STATUS_INVALID` | **PASS** |

---

### 2.2. Integration Test — Web API & Security Flow (`OfferIntegrationTest.java`)

- **Vị trí file:** `src/test/java/com/group2/rms/offer/OfferIntegrationTest.java`
- **Công nghệ sử dụng:** Spring WebMvcTest (`@WebMvcTest(OfferApiController.class)`), `MockMvc`, Spring Security Test (`@WithMockUser`, CSRF).
- **Kết quả thực tế:** **7/7 tests PASS (100%)**.

| Mã Test | Endpoint / Method | Kịch bản kiểm tra | Kỳ vọng | Kết quả |
| :---: | :--- | :--- | :--- | :---: |
| `IT-01` | `GET /api/v1/hr/offers/passed-candidates` | HR lấy danh sách ứng viên đỗ phỏng vấn | Trả về 200 OK + JSON danh sách | **PASS** |
| `IT-02` | `POST /api/v1/hr/offers` | HR tạo Offer mới hợp lệ | Trả về 201 Created + JSON Offer mới | **PASS** |
| `IT-03` | `POST /api/v1/hr/offers` | Tạo Offer với lương thử việc $>$ lương chính thức | Trả về 400 Bad Request + Message lỗi | **PASS** |
| `IT-04` | `POST /api/v1/hr/offers` | Tạo Offer cho đơn đã có Offer Nhóm B | Trả về 400 Bad Request (`OFFER_LOCKED_STATE`) | **PASS** |
| `IT-05` | `PUT /api/v1/hr/offers/{id}` | Cập nhật Offer hợp lệ ở Nhóm A | Trả về 200 OK + dữ liệu đã sửa | **PASS** |
| `IT-06` | `POST /api/v1/hr/offers/{id}/send` | HR phát hành Offer Letter cho ứng viên | Trả về 200 OK + trạng thái `Sent_Candidate` | **PASS** |
| `IT-07` | Security Matcher | Truy cập API khi chưa đăng nhập | Bị chặn (Redirect 302 về Login / 401 Unauthorized) | **PASS** |

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

---

## 4. Tệp tin đã thay đổi & Đối chiếu Git

| Tệp tin | Thao tác | Mô tả thay đổi |
| :--- | :---: | :--- |
| `CreateOfferRequest.java` | Modified | Thêm validator kiểm tra lương thử việc $\le$ lương chính thức |
| `UpdateOfferRequest.java` | Modified | Thêm validator kiểm tra lương thử việc $\le$ lương chính thức |
| `OfferServiceImpl.java` | Modified | Cài đặt quy tắc `GBR-07` (Nhóm A ghi đè in-place, Nhóm B khóa; lọc Passed Candidates) |
| `OfferServiceTests.java` | Modified | Bổ sung đầy đủ 20 Unit test cases cho toàn bộ nghiệp vụ |
| `list.html` & `offers.css` | Modified | Căn chỉnh layout thẻ thống kê Stat Cards |
| `docs/members/huyenpt/offer-flow-testing.md` | Created | Tài liệu chi tiết báo cáo và hướng dẫn kiểm thử luồng Offer |

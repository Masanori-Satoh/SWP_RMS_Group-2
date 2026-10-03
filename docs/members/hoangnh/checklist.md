# BÁO CÁO CÔNG VIỆC ITERATION 1 — MODULE JOB REQUISITION

- **Thành viên phụ trách:** Nguyễn Huy Hoàng (hoangnh)
- **Module:** Yêu cầu Tuyển dụng (Job Requisition)
- **Nhánh Git:** `feat/hoang-Requisition-List-Screen`

> **Cập nhật ngày 03/10/2026:** Các mục hoàn thành Iter1 bên dưới là ghi nhận triển khai trước lần rà soát này, chưa đồng nghĩa đã nghiệm thu toàn bộ luồng. Yêu cầu đã chốt: **HR chỉ được xem Requisition có trạng thái Approved** và **Hiring Manager phải nhận được thông báo riêng khi Director từ chối**, ngoài feedback trên trang chi tiết. Hai thiếu sót này được đưa vào phần VI để xử lý trước luồng Job Posting của Iter2. Các checkbox `[ ]` trong phần VI là kế hoạch, chưa triển khai hoặc xác nhận Passed.

---

## I. TỔNG QUAN PHẠM VI CÔNG VIỆC TRONG ITERATION 1

Căn cứ theo bảng phân công công việc (Task Assignment) của Iteration 1, module Requisition bao gồm 3 màn hình chính:

| Màn hình (Screen) | Loại (Type) | Chức năng (Features / Use Cases) | Trạng thái |
| :--- | :--- | :--- | :---: |
| **Requisition List Screen** | Screen | - View Job Requisition<br>- View Approved Requisition (Lọc, tìm kiếm, phân trang) | **Hoàn thành** |
| **Create/Update Requisition Screen** | Screen | - Create Job Requisition (Lưu Draft / Submit duyệt)<br>- Update Job Requisition (Chỉnh sửa đơn nháp / bị từ chối) | **Hoàn thành** |
| **Job Requisition Details** | Screen | - View Job Requisition (Xem chi tiết, tiêu chí AI, lịch sử duyệt, timeline)<br>- Duyệt (Approve/Reject), Rút đơn (Withdraw), Sao chép (Copy), Xóa (Delete) | **Hoàn thành** |

---

## II. KHỞI TẠO MODULE & THIẾT KẾ CÁC COMPONENT

Hệ thống được xây dựng theo kiến trúc phân tầng chuẩn của **Spring Boot (MVC Architecture)**:

### 1. Tầng Dữ liệu & Thực thể (Data & Entity Layer)
- **`JobRequisition.java`**: Thực thể chính lưu trữ thông tin yêu cầu tuyển dụng (Tiêu đề, Phòng ban, Số lượng tuyển, Hình thức làm việc, Mức lương min/max, Địa điểm, Mô hình on-site/hybrid/remote, Hạn thử việc, Ngày bắt đầu dự kiến, Lý do tuyển, Mô tả công việc, Yêu cầu ứng viên, Trạng thái phê duyệt, Concurrency Version).
- **`ScreeningCriteria.java`**: Quan hệ 1-N với `JobRequisition`, đại diện cho tiêu chí sàng lọc hồ sơ tự động của AI (Tên tiêu chí, Loại tiêu chí: Skill/Education/Experience/Knockout, Giá trị yêu cầu, Trọng số điểm Weight, Cờ bắt buộc isMandatory).
- **`RequisitionApproval.java`**: Lưu trữ lịch sử duyệt/từ chối của Giám đốc (Director) kèm ý kiến nhận xét (Feedback/Comments).
- **`RequisitionWorkflowEvent.java`**: Lưu trữ dòng thời gian (Timeline) các sự kiện luân chuyển trạng thái (Submitted, Approved, Rejected, Withdrawn).

### 2. Tầng Truy cập Dữ liệu (Repository Layer)
- **`JobRequisitionRepository.java`**: Kế thừa `JpaRepository` và `JpaSpecificationExecutor`, hỗ trợ truy vấn lọc động, tìm kiếm toàn văn theo từ khóa, lọc theo phòng ban, hình thức, trạng thái và phân trang/sắp xếp linh hoạt.
- **`RequisitionApprovalRepository.java`**, **`RequisitionWorkflowEventRepository.java`**, **`JobPostingRepository.java`**.

### 3. Tầng Dữ liệu Truyền tải (DTO Layer)
- **Request DTOs**:
  - `RequisitionRequest`: Nhận dữ liệu gửi lên từ form (phân biệt thao tác `action = "draft"` hoặc `action = "submit"`).
  - `ScreeningCriteriaRequest`: Nhận dữ liệu từng dòng tiêu chí AI.
- **Response DTOs**:
  - `RequisitionResponse`, `ScreeningCriteriaResponse`, `ApprovalResponse`, `RequisitionTimelineResponse`: Chuẩn hóa dữ liệu trả về phục vụ render giao diện Thymeleaf an toàn, tránh lỗi vòng lặp Lazy Loading của JPA.

### 4. Tầng Kiểm tra Tính hợp lệ (Validation Layer)
- **`RequisitionValidator.java`**: Bộ quy tắc kiểm tra nghiệp vụ toàn diện:
  - **Lưu nháp (Draft)**: Bỏ qua kiểm tra bắt buộc các trường chi tiết, cho phép lưu tiến độ dở dang, chấp nhận trọng số criteria linh hoạt (tối đa 999.99%).
  - **Nộp duyệt (Submit)**: Bắt buộc điền đủ tiêu đề, phòng ban, số lượng > 0, ngày bắt đầu từ hiện tại trở đi, lý do tuyển dụng, mô tả, yêu cầu; tổng trọng số các tiêu chí AI bắt buộc phải tròn **100%**.

### 5. Tầng Bảo mật & Phân quyền (Security & Access Layer)
- **`RequisitionAccess.java`**: Kiểm soát quyền truy cập chi tiết theo vai trò người dùng:
  - **Hiring Manager**: Chỉ được xem/sửa đơn của chính mình; chỉ được tạo mới, lưu draft, submit và rút đơn (withdraw) khi đang chờ duyệt.
  - **Director**: Được xem các đơn không phải Draft; có thẩm quyền duyệt (`Approved`) hoặc từ chối (`Rejected`) kèm lý do đối với đơn `Pending_Director`.
  - **HR**: Yêu cầu đã chốt là chỉ xem Requisition `Approved`. Code hiện tại còn cho xem các đơn không phải `Draft`; cần sửa cả truy vấn danh sách và kiểm tra quyền xem chi tiết theo mục VI.2.
  - **System Admin**: Toàn quyền quản trị.

### 6. Tầng Xử lý Nghiệp vụ (Service Layer)
- **`RequisitionService.java`** & **`RequisitionServiceImpl.java`**:
  - `search()`: Tìm kiếm, lọc và phân trang chuẩn hóa.
  - `createRequisition()`: Tạo mới đơn yêu cầu, map dữ liệu, tự động gán tiêu đề `"Untitled requisition"` nếu để trống, ghi `AuditLog` hành động `CREATE`.
  - `updateRequisition()`: Cập nhật đơn nháp/bị từ chối, đồng bộ danh sách tiêu chí AI (thêm/sửa/xóa/đổi tên an toàn), kiểm tra concurrency version tránh ghi đè dữ liệu.
  - `decide()`: Giám đốc phê duyệt hoặc từ chối kèm phản hồi bắt buộc.
  - `withdraw()`: Hiring Manager rút lại đơn đang chờ duyệt về trạng thái nháp.
  - `copy()`: Nhân bản đơn cũ sang form mới (xóa ID và version để tạo bản ghi độc lập).
  - `deleteRequisition()`: Xóa đơn nháp, kiểm tra bảo vệ chặn xóa nếu đơn đã được liên kết với Tin tuyển dụng (`JobPosting`).

### 7. Tầng Giao diện & Điều hướng (Controller & Thymeleaf UI)
- **`RequisitionController.java`**: Tiếp nhận request, binding dữ liệu form với `WebDataBinder`, gán thông tin viewer vào Model.
- **Thymeleaf Templates**:
  - `src/main/resources/templates/requisitions/list.html`: Danh sách Requisition.
  - `src/main/resources/templates/requisitions/form.html`: Form tạo mới, chỉnh sửa, sao chép (hỗ trợ thêm/xóa dòng tiêu chí AI động bằng JS).
  - `src/main/resources/templates/requisitions/detail.html`: Trang chi tiết, hiển thị tiêu chí, lịch sử duyệt và modal thao tác.

---

## III. CHI TIẾT CÁC CÔNG VIỆC ĐÃ HOÀN THÀNH THEO MÀN HÌNH

### 1. Màn hình Danh sách (`Requisition List Screen`)
- [x] Hiển thị bảng danh sách các Requisition với phân trang (chuyển trang, chọn size).
- [x] Tích hợp thanh tìm kiếm theo tiêu đề và tên phòng ban (hỗ trợ escape ký tự đặc biệt).
- [x] Bộ lọc theo Phòng ban, Hình thức tuyển dụng (Full-time, Part-time, Internship, Contract), Trạng thái (Draft, Pending_Director, Approved, Rejected).
- [x] Sắp xếp theo: Mới nhất, Cũ nhất, Tên chức danh A-Z, Tên chức danh Z-A.
- [x] **Fix lỗi UI Action Dropdown**: Sửa lỗi menu thao tác (View, Update, Delete) bị cắt khuất do thuộc tính css `overflow-x: auto;` của bảng -> cấu hình lại css hiển thị đầy đủ menu dạng floating.
- [x] **Fix lỗi JavaScript**: Tối ưu hóa code `toggleDropdown` đóng mở menu an toàn khi click.
- [x] Đổi tên cột hiển thị `REQ_ID` thành `STT`, ẩn tiêu đề cột "Action", chỉ giữ nút ba chấm `...`.
- [x] Đồng bộ layout: Tích hợp `fragments/sidebar.html` và font Google Fonts `Inter` đồng bộ với Dashboard.

### 2. Màn hình Tạo mới & Chỉnh sửa (`Create/Update Requisition Screen`)
- [x] Xây dựng form nhập liệu toàn diện với đầy đủ các trường thông tin tuyển dụng.
- [x] Chức năng **Save Draft**: Cho phép Hiring Manager lưu lại tiến độ mà không bắt buộc điền hết mọi trường.
- [x] Chức năng **Submit to Director**: Kiểm tra validation chặt chẽ trước khi gửi đơn đi phê duyệt.
- [x] **Quản lý danh sách tiêu chí AI động**:
  - Cho phép người dùng bấm nút thêm mới dòng tiêu chí hoặc xóa dòng tiêu chí ngay trên giao diện.
  - Lọc bỏ tự động các dòng tiêu chí trống (`blank`) khi lưu vào database.
- [x] **Chức năng Chỉnh sửa (Update)**:
  - Cho phép sửa đơn ở trạng thái `Draft` hoặc `Rejected`.
  - Áp dụng kiểm tra Version Optimistic Lock để ngăn chặn 2 người cùng sửa 1 lúc gây ghi đè dữ liệu.
- [x] **Chức năng Sao chép (Copy)**:
  - Đọc dữ liệu từ bản ghi có sẵn và đổ vào form tạo mới.
  - Reset `version` và toàn bộ `criteriaId` về null để không làm ảnh hưởng bản ghi gốc.

### 3. Màn hình Xem chi tiết (`Job Requisition Details Screen`)
- [x] Hiển thị đầy đủ thông tin chi tiết: Chức danh, Phòng ban, Lương min/max, Số lượng, Mô tả, Yêu cầu,...
- [x] Hiển thị bảng tiêu chí sàng lọc hồ sơ CV (kèm trọng số điểm và cờ bắt buộc).
- [x] Tích hợp các nút điều hướng theo quyền hạn người dùng:
  - **Hiring Manager**: Nút Sửa, Xóa (nếu là Draft/Rejected), nút Rút đơn (nếu đang Pending_Director).
  - **Director**: Nút Duyệt (`Approve`), nút Từ chối (`Reject` kèm form nhập phản hồi lý do bắt buộc).
- [x] Hiển thị danh sách lịch sử phê duyệt (`Approvals`) và dòng sự kiện (`Timeline`).

---

## IV. QUÁ TRÌNH VIẾT VÀ HOÀN THIỆN BỘ TEST (TESTING)

Nhằm đảm bảo chất lượng code và phòng ngừa lỗi hồi quy (regression bugs), hệ thống kiểm thử được xây dựng hoàn chỉnh qua **4 Stage**:

### 1. Khắc phục lỗi môi trường Test ban đầu
- **Sự cố UTF-8 BOM**: Phát hiện lỗi `illegal character: '\ufeff'` ở dòng 1 khiến lệnh `mvnw spring-boot:run` và `mvn test-compile` bị dừng đột ngột.
- **Xử lý**: Loại bỏ hoàn toàn ký tự BOM (Byte Order Mark), chuyển các file mã nguồn test về chuẩn UTF-8 thuần.

### 2. Stage 1 — Unit Test Validation (`RequisitionValidatorTests.java`)
- Kiểm thử độc lập tầng Validator không cần khởi động Spring context hay Database.
- Bao phủ 20 nhóm quy tắc nghiệp vụ (`VAL-001` đến `VAL-020`):
  - Kiểm tra độ dài tiêu đề (1 - 200 ký tự).
  - Kiểm tra số lượng tuyển (phải là số nguyên dương).
  - Kiểm tra ràng buộc mức lương (minSalary <= maxSalary, scale tối đa 2 chữ số thập phân).
  - Kiểm tra ngày bắt đầu dự kiến (Draft chấp nhận quá khứ/hiện tại, Submit bắt buộc từ hôm nay trở đi).
  - Kiểm tra tiêu chí sàng lọc: không trùng tên trong 1 đơn, định dạng trọng số, tổng trọng số Submit bắt buộc đúng 100%.

### 3. Stage 2 — Service Unit Test (`JobRequisitionServiceTest.java`)
- Kiểm thử tầng nghiệp vụ `RequisitionServiceImpl` với Mockito (`@ExtendWith(MockitoExtension.class)`).
- Các kịch bản kiểm thử trọng tâm:
  - **`svc01_createDraft_fullFields`**: Tạo Draft đủ trường -> kiểm tra gán đúng Hiring Manager, status `Draft`, mapping đầy đủ các trường, gắn tiêu chí 2 chiều, ghi `AuditLog` hành động `CREATE`, không sinh workflow event.
  - **`svc01_createDraft_missingFields_appliesDefaults`**: Tạo Draft rỗng -> gán tiêu đề mặc định `"Untitled requisition"`, các trường chưa nhập nhận giá trị `null`, không truy vấn thừa DB.
  - **`svc01_createDraft_filtersBlankCriteria`**: Tự động loại bỏ các dòng tiêu chí trắng.
  - **`svc01_createDraft_rejectsExistingCriteriaId`**: Chặn đứng việc truyền `criteriaId` cũ vào phương thức tạo mới.

### 4. Stage 3 — Controller / MVC Test (`JobRequisitionControllerTests.java`)
- Kiểm thử tầng Web với `@WebMvcTest(controllers = RequisitionController.class)` và `MockMvc`:
  - **GET**: Kiểm tra các endpoint `GET /requisitions` (trả về view list, model phân trang), `GET /requisitions/create` (trả về form), `GET /requisitions/{id}` (trả về chi tiết).
  - **POST**: Kiểm tra `POST /requisitions/create`, `POST /requisitions/edit/{id}`, `POST /requisitions/delete/{id}`, `POST /requisitions/{id}/decision`, `POST /requisitions/{id}/withdraw` thực hiện đúng redirect và nạp Flash Message.
  - **Lỗi Form**: Bắt lỗi `RequisitionValidationException` trả lại giao diện form và hiển thị field error tương ứng.
  - **Bảo mật**: Kiểm tra chặn request POST thiếu token CSRF (`403 Forbidden`) và chặn quyền của tài khoản `CANDIDATE` truy cập vào module Requisition.

### 5. Stage 4 — Integration Test (`RequisitionIntegrationTest.java`)
- Kiểm thử tích hợp thật giữa Service, Repositories và Database với `@SpringBootTest` và `@Transactional`:
  - **`it01_createDraft_persistsRelationships`**: Kiểm tra lưu Draft thật vào database, đảm bảo tính toàn vẹn quan hệ giữa Requisition, Hiring Manager, Department và ScreeningCriteria.
  - **`it04_directorApprove_createsApprovalAndEvent`**: Kiểm tra luồng Director duyệt đơn, chuyển trạng thái `Approved`, lưu bản ghi `RequisitionApproval` và ghi nhận `RequisitionWorkflowEvent`.
  - **`it05_directorReject_recordsFeedback`**: Kiểm tra luồng Director từ chối đơn kèm lưu nhận xét lý do từ chối.
  - **`it06_withdraw_revertsToDraft`**: Kiểm tra Hiring Manager rút đơn `Pending_Director` về lại `Draft`.
  - **`it08_copy_clearsIdentifiers`**: Kiểm tra sao chép đơn trả về DTO sạch (xóa version, xóa criteriaId).
  - **`it10_delete_removesEntity`**: Kiểm tra xóa Requisition khỏi database thành công.

---

## V. CHECKLIST TỔNG KẾT ĐẦU MỤC CÔNG VIỆC

- [x] **Entity & Database**: Thiết kế bảng `JobRequisition`, `ScreeningCriteria`, `RequisitionApproval`, `RequisitionWorkflowEvent`.
- [x] **Repository Layer**: Viết các Repository với Specification tìm kiếm động.
- [x] **DTO & Validator**: Tạo DTO Request/Response và hoàn thiện quy tắc validate (Draft vs Submit).
- [x] **Service Layer**: Cài đặt trọn vẹn CRUD, luồng duyệt/từ chối, rút đơn, sao chép, phân quyền và ghi Audit Log.
- [x] **Controller Layer**: Xây dựng controller đón nhận GET/POST, xử lý lỗi binding, flash message.
- [x] **UI Frontend (Thymeleaf/CSS/JS)**:
  - [x] Requisition List Screen (Bảng danh sách, phân trang, lọc, search, dropdown action).
  - [x] Create/Update Screen (Form nhập liệu, thêm xóa tiêu chí AI động).
  - [x] Requisition Detail Screen (Xem chi tiết, tiêu chí, lịch sử phê duyệt, nút duyệt/rút đơn).
- [x] **Bộ Test Cases hoàn chỉnh**:
  - [x] `RequisitionValidatorTests.java` (Stage 1 - Validator Test).
  - [x] `JobRequisitionServiceTest.java` (Stage 2 - Service Unit Test).
  - [x] `JobRequisitionControllerTests.java` (Stage 3 - Controller / MVC Test).
  - [x] `RequisitionIntegrationTest.java` (Stage 4 - Database Integration Test).

---

## VI. KẾ HOẠCH ITERATION 2 — HOANGNH

### 1. Phạm vi, hiện trạng và thứ tự thực hiện

| Hạng mục | Mã trong bảng phân công | Nội dung Iter2 | Điều kiện hoàn thành |
| :--- | :--- | :--- | :--- |
| Khắc phục thiếu sót Requisition từ Iter1 | 5.1.14–5.1.16 | HR chỉ xem Approved; thông báo riêng cho HM khi bị từ chối; xác nhận lại test | Quyền được kiểm tra ở server và HM nhận đúng thông báo sau quyết định thành công |
| Internal Job Management | 5.1.18 | HR xem, tìm kiếm, lọc, phân trang và mở chi tiết tin tuyển dụng nội bộ | Dữ liệu lấy từ database, đúng quyền, liên kết về requisition nguồn |
| Post/Update Job Screen | 5.1.19 | HR tạo/sửa tin từ requisition Approved, validate, lưu và publish | Tin Published xuất hiện đúng trên trang công khai; HM nhận thông báo đăng tin |

**Hiện trạng làm nền:** Đã có entity `JobPosting`, repository và service/controller phục vụ xem việc làm công khai. Chưa xem đây là bằng chứng đã có luồng HR tạo/sửa/publish nội bộ. Chưa thấy thành phần thông báo riêng trong module Requisition. `JobPosting` hiện có các trạng thái `Draft`, `Published`, `Paused`, `Closed`; tên trạng thái tồn tại không có nghĩa mọi thao tác chuyển trạng thái đã được triển khai.

**Thứ tự:** VI.2 → VI.3 → VI.4 → VI.5 → VI.6 → VI.7. Mục VI.8 là điều kiện nghiệm thu chung.

**Ranh giới phối hợp:** Public JD Board (5.1.34) và JD Details Screen (5.1.35) thuộc DungLT theo bảng phân công. HoangNH chịu trách nhiệm dữ liệu, quyền và luồng nội bộ tạo/sửa/publish; phối hợp kiểm thử việc tin xuất hiện công khai, không tự nhận toàn bộ phần màn hình của thành viên khác.

### 2. P0 — Sửa quyền HR và xác nhận nền Iter1

- [ ] **I2-REQ-01 — Scope danh sách:** Tách nhánh HR khỏi Director trong `RequisitionServiceImpl.scope()`. HR chỉ truy vấn `approvalStatus = Approved`; áp dụng cùng scope cho `search()`, `countVisible()`, tổng bản ghi và phân trang.
- [ ] **I2-REQ-02 — Quyền chi tiết:** Sửa `RequisitionAccess.requireView()` để HR bị chặn khi truy cập trực tiếp ID của Draft, Pending_Director hoặc Rejected, kể cả khi tự sửa URL. Không dùng ẩn nút trên UI thay cho kiểm tra quyền.
- [ ] **I2-REQ-03 — Bộ lọc và giao diện HR:** HR chỉ có lựa chọn Approved phù hợp với quyền. Tự gửi `status=Draft/Pending_Director/Rejected` không trả dữ liệu ngoài scope. Áp dụng lại quyền này tại các API/ô chọn requisition dùng để tạo Job Posting.
- [ ] **I2-REQ-04 — Hồi quy các role khác:** HM tiếp tục xem request của mình; Director vẫn xem các request không phải Draft và chỉ quyết định khi Pending, không tự duyệt. Không mở rộng quyền Admin hoặc quyền chỉnh sửa ngoài quy tắc đã có.
- [ ] **I2-TEST-01 — Cấu hình test:** Bổ sung dependency `spring-security-test` đúng scope test; build lại để loại bỏ class test cũ. Chạy lại Validator, Service và Controller tests trước khi kết luận lỗi chức năng.
- [ ] **I2-TEST-02 — Database test:** Chuẩn bị profile/database test riêng và fixture độc lập trước khi chạy integration test; không dùng dữ liệu đang demo làm fixture mặc định.

**Bằng chứng tại lần rà soát 03/10/2026:** Validator chạy 147 case Passed; Service chạy 4 case Passed; Controller có 11 Errors liên quan `WithMockUser`/`csrf()`. `pom.xml` chưa khai báo `spring-security-test`. Đây là kết quả lần chạy đã ghi nhận, không phải kết quả sau khi hoàn thành kế hoạch này. Integration test chưa được chạy trong lần rà soát.

**Nghiệm thu quyền HR:** Với bốn requisition có trạng thái khác nhau, HR chỉ thấy Approved trong list/count; mở Approved trả 200; mở trực tiếp ba trạng thái còn lại trả 403. Director và HM vẫn thực hiện được đúng luồng cũ.

### 3. P0 — Thông báo từ chối riêng cho Hiring Manager

**Kênh đề xuất cho Iter2:** Thông báo trong hệ thống là phần bắt buộc của kế hoạch. Email là kênh bổ sung cần chốt với nhóm, chưa mặc định là điều kiện Done. Timeline/AuditLog không thay thế thông báo gửi tới người nhận.

- [ ] **I2-NOTI-01 — Tái sử dụng thiết kế chung:** Kiểm tra thành phần Notification hiện có của dự án trước khi tạo mới. Nếu chưa có, thống nhất nơi đặt module dùng chung và chiều gọi service, tránh phụ thuộc vòng.
- [ ] **I2-NOTI-02 — Dữ liệu thông báo:** Lưu người nhận, loại sự kiện, ID requisition/posting liên quan, nội dung, thời gian tạo, trạng thái đọc và định danh sự kiện để chống tạo trùng. Có migration SQL tương ứng nếu cần thêm bảng/index.
- [ ] **I2-NOTI-03 — Phát sinh sau reject hợp lệ:** Khi Director từ chối Pending với feedback hợp lệ, lưu Rejected, approval, workflow event và thông báo cho đúng HM sở hữu requisition trong cùng giao dịch database, hoặc cơ chế outbox đã thống nhất. Không tạo thông báo khi validation/quyền/version thất bại hoặc giao dịch rollback.
- [ ] **I2-NOTI-04 — Nội dung:** Thông báo nêu requisition nào bị từ chối, ai từ chối, thời điểm và lý do. Có liên kết về đúng trang chi tiết để HM sửa và gửi lại. Encode nội dung do người dùng nhập khi hiển thị.
- [ ] **I2-NOTI-05 — Giao diện nhận:** HM có danh sách/badge thông báo chưa đọc, mở được thông báo và đánh dấu đã đọc. Mỗi người chỉ xem/đánh dấu thông báo của chính mình; kiểm tra quyền ở server và khi mở liên kết đích.
- [ ] **I2-NOTI-06 — Chống trùng:** Gửi lại cùng request hoặc retry cùng sự kiện không tạo thông báo trùng. Một lần reject mới sau khi HM resubmit phải tạo thông báo mới và vẫn giữ lịch sử cũ.
- [ ] **I2-NOTI-07 — Nếu triển khai email:** Dùng cấu hình mail chung, gửi sau commit qua cơ chế có retry; lỗi SMTP không đảo ngược quyết định đã lưu. Không gửi thư thật trong automated test, không đưa credentials vào source/checklist.

**Nghiệm thu:** Director reject có lý do → HM đúng chủ nhận một thông báo chưa đọc → mở được feedback → sửa và resubmit cùng requisition ID. HM khác không thấy hoặc truy cập được thông báo đó. Reject thiếu lý do và giao dịch lỗi không sinh thông báo.

### 4. P1 — Internal Job Management (5.1.18)

- [ ] **I2-JOB-01 — Quyền và đường dẫn nội bộ:** Chốt route nội bộ riêng với route public `/jobs`; đề xuất `/internal/job-postings`. HR là actor tạo/sửa/publish theo swimlane. Quyền hỗ trợ của Admin phải chốt rõ; không mặc định cho HM/Director/Candidate thao tác thay HR.
- [ ] **I2-JOB-02 — Danh sách:** Hiển thị tiêu đề, requisition nguồn, phòng ban, người tạo, trạng thái, ngày đăng, hạn ứng tuyển và thao tác được phép. Có tìm kiếm, lọc, sắp xếp, phân trang, trạng thái rỗng và thông báo lỗi.
- [ ] **I2-JOB-03 — Chi tiết nội bộ:** Xem đủ nội dung posting, requisition Approved liên quan và lịch sử thao tác cần thiết. Nội dung chưa public không được lộ qua endpoint công khai.
- [ ] **I2-JOB-04 — Giao diện đồng bộ:** Dùng layout/sidebar, typography và màu chung của dự án. Tách CSS/JS riêng; không viết inline style/script cho màn hình mới.

### 5. P1 — Post/Update Job Screen (5.1.19)

- [ ] **I2-JOB-05 — Tạo từ Approved:** HR chọn requisition đã duyệt; hệ thống gợi ý title, description, requirements, location và thông tin lương phù hợp. Đọc dữ liệu thật từ DB. Kiểm tra Approved lại ở service khi lưu, không chỉ lọc dropdown.
- [ ] **I2-JOB-06 — DTO và validation:** Tạo Request/Response theo kiến trúc dự án. Chốt trường bắt buộc cho Draft và Publish dựa trên schema hiện có (`PostingTitle`, `JobDescription`, `JobRequirements` đang non-null), giới hạn độ dài và hạn ứng tuyển. Không hứa lưu Draft trống nếu schema chưa hỗ trợ.
- [ ] **I2-JOB-07 — Lưu Draft:** Lưu tin cùng liên kết requisition và `CreatedBy` lấy từ session. Lỗi validation trả về đúng form, giữ dữ liệu và không tạo bản ghi dở dang. Draft không xuất hiện công khai.
- [ ] **I2-JOB-08 — Sửa tin:** Load dữ liệu theo ID, kiểm tra quyền/trạng thái, cập nhật đúng bản ghi và ghi audit. Không sửa ngược requisition Approved khi HR thay nội dung tin tuyển dụng.
- [ ] **I2-JOB-09 — Bảo vệ dữ liệu:** Không bind trực tiếp entity. Client không được tự gán người tạo, thời gian đăng hoặc trạng thái Published để vượt qua action publish. Chặn liên kết tới requisition không tồn tại hoặc chưa Approved.
- [ ] **I2-JOB-10 — Cập nhật đồng thời:** Bổ sung cơ chế version/concurrency phù hợp cho JobPosting và migration nếu cần; báo lỗi rõ khi dữ liệu cũ, tránh hai HR ghi đè hoặc publish lặp.
- [ ] **I2-JOB-11 — Chốt trước khi code:** Ghi rõ một requisition được có bao nhiêu posting; có cho sửa nội dung tin Published trực tiếp không; có cho đổi requisition nguồn không; deadline có bắt buộc không. Không suy diễn từ quan hệ `ManyToOne` rằng mọi trường hợp nhiều posting đều hợp lệ.

### 6. P1 — Publish, hiển thị công khai và thông báo HM

- [ ] **I2-PUB-01 — Publish:** Kiểm tra quyền HR, version, nội dung, deadline và trạng thái Approved của requisition trong thao tác ghi; chuyển Draft sang Published và đặt thời điểm đăng ở server.
- [ ] **I2-PUB-02 — Tính nhất quán:** Save/publish phải thành công trước khi có thông báo thành công. Lỗi validation/database không tạo tin công khai, audit thành công hoặc thông báo Published sai.
- [ ] **I2-PUB-03 — Public list/detail:** Phối hợp DungLT kiểm tra tin Published xuất hiện trên Public JD Board và JD Details theo cùng chính sách deadline. Truy cập trực tiếp ID của Draft/Paused/Closed không làm lộ nội dung nội bộ. Rà lại `getPublishedJobDetail()` hiện đang lấy theo ID mà chưa giới hạn trạng thái public.
- [ ] **I2-PUB-04 — Notification Posting:** Sau publish thành công, gửi thông báo trong hệ thống đến HM sở hữu requisition nguồn, gồm tin nào được đăng và liên kết xem tin. Không nhầm người nhận thành HR tạo posting.
- [ ] **I2-PUB-05 — Publish lặp:** Double-click/retry không tạo posting hoặc thông báo trùng và không ghi đè thời điểm đăng đầu tiên ngoài quy tắc đã chốt.
- [ ] **I2-PUB-06 — Giới hạn Iter2:** Chốt riêng việc pause/close/reopen, lên lịch đăng và email. Chỉ thêm thao tác vào màn hình khi có quy tắc chuyển trạng thái và test tương ứng; không tự mở rộng phạm vi vì entity đã có tên trạng thái.

### 7. P2 — Kiểm thử và cập nhật tài liệu

- [ ] **I2-QA-01 — Requisition regression:** HM create → validate → submit → Director approve; nhánh reject → notification → HM sửa → resubmit. Kiểm tra dữ liệu sau khi flush/clear persistence context hoặc đọc trong giao dịch mới.
- [ ] **I2-QA-02 — HR visibility:** Test đủ Role × Status cho list, count, detail và bộ chọn requisition. Cập nhật case Security cũ từng cho HR xem Pending thành kỳ vọng bị chặn; Director vẫn là nhánh riêng.
- [ ] **I2-QA-03 — Posting integration:** Tạo Draft từ Approved, reload, sửa, publish và truy vấn public list/detail. Thử requisition chưa Approved, field thiếu/sai, stale version và posting không tồn tại.
- [ ] **I2-QA-04 — Notification integration:** Đúng người nhận/nội dung/liên kết/read state; không sinh khi rollback; không trùng khi retry; lần reject mới sinh thông báo mới. Nếu có email, kiểm tra bằng mail giả lập.
- [ ] **I2-QA-05 — Security:** Guest/Candidate/HM/Director không được gọi POST nội bộ thay HR; thiếu/sai CSRF bị chặn; không truy cập notification của người khác; không spoof owner/status/requisition; nội dung nhập được render an toàn, không thực thi script.
- [ ] **I2-QA-06 — Cập nhật hai file test:** Bổ sung Integration/Security test cho Iter2, sửa expected result theo quyền HR mới. Chỉ ghi Passed sau khi chạy; ghi ngày test, tester HoangNH, round, bằng chứng và Defect ID nếu lỗi.
- [ ] **I2-QA-07 — Kiến trúc:** Theo `docs/architecture/ARCHITECTURE_GUIDE.md`: package theo feature, DTO record cho code mới, validation phía server, custom business exception qua GlobalExceptionHandler, transaction phù hợp, CSS/JS trong static. Với nội dung người dùng nhập, ưu tiên text đã escape và CSS giữ xuống dòng; không render HTML thô chưa kiểm soát.

### 8. Điều kiện hoàn thành Iter2 (Definition of Done)

- [ ] **DONE-01:** HR chỉ xem được Approved qua mọi đường truy cập Requisition liên quan; tổng bản ghi và phân trang không lộ dữ liệu trạng thái khác.
- [ ] **DONE-02:** Luồng reject có thông báo riêng cho HM, không chỉ feedback trong detail/timeline; HM sửa và resubmit thành công.
- [ ] **DONE-03:** Demo trọn nhánh swimlane: HM tạo và submit → Director approve → HR xem Approved → tạo posting → validation lỗi quay lại form → lưu/publish hợp lệ → HM nhận Notification Posting.
- [ ] **DONE-04:** Hai màn hình 5.1.18 và 5.1.19 hoạt động với database, giao diện đồng bộ; tin chưa Published không bị lộ trên public list/detail.
- [ ] **DONE-05:** Build và các test trong phạm vi đã chốt Passed; không còn lỗi chặn hai luồng nghiệp vụ chính. Integration test chạy trên database test riêng và có bằng chứng kết quả.
- [ ] **DONE-06:** Migration, quy tắc quyền/trạng thái, hai file test và kết quả demo được cập nhật. Các lựa chọn còn mở ở VI.5/VI.6 được chốt trước khi đánh dấu Done.

**Nguyên tắc cập nhật:** Chỉ đổi `[ ]` thành `[x]` khi có code và bằng chứng kiểm tra tương ứng. Việc thêm kế hoạch này không đánh dấu Iter1 hoặc Iter2 đã hoàn thành, không triển khai chức năng và không gửi notification/email thật.

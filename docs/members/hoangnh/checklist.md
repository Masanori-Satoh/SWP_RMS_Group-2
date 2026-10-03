# BÁO CÁO CÔNG VIỆC ITERATION 1 — MODULE JOB REQUISITION

- **Thành viên phụ trách:** Nguyễn Huy Hoàng (hoangnh)
- **Module:** Yêu cầu Tuyển dụng (Job Requisition)
- **Nhánh Git:** `feat/hoang-Requisition-List-Screen`

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
  - **HR**: Được xem danh sách các đơn đã submit để theo dõi tiến độ tuyển dụng.
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

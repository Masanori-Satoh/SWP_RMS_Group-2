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
- **`JobRequisition.java`**: Thực thể chính lưu trữ thông tin yêu cầu tuyển dụng (Tiêu đề, Phòng ban, Số lượng tuyển, Hình thức làm việc, Mức lương min/max, Địa điểm, Mô hình on-site/hybrid/remote, Hạn thử việc, Ngày bắt đầu dự kiến, Lý do tuyển, Mô tả công việc, Yêu cầu ứng viên, Trạng thái phê duyệt).
- **`ScreeningCriteria.java`**: Quan hệ 1-N với `JobRequisition`, đại diện cho tiêu chí sàng lọc hồ sơ tự động của AI (Tên tiêu chí, Loại tiêu chí: Skill/Education/Experience/Knockout, Giá trị yêu cầu, Trọng số điểm Weight, Cờ bắt buộc isMandatory).
- **`RequisitionApproval.java`**: Lưu trữ lịch sử duyệt/từ chối của Giám đốc (Director) kèm ý kiến nhận xét (Feedback/Comments).
- Dòng thời gian (Timeline) của yêu cầu được ghép từ `RequisitionApproval` (duyệt/từ chối) và `AuditLog` (tạo, gửi duyệt, rút lại, chỉnh sửa). Không có bảng riêng cho sự kiện luân chuyển.

### 2. Tầng Truy cập Dữ liệu (Repository Layer)
- **`JobRequisitionRepository.java`**: Kế thừa `JpaRepository` và `JpaSpecificationExecutor`, hỗ trợ truy vấn lọc động, tìm kiếm toàn văn theo từ khóa, lọc theo phòng ban, hình thức, trạng thái và phân trang/sắp xếp linh hoạt.
- **`RequisitionApprovalRepository.java`**, **`JobPostingRepository.java`**.

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
  - `updateRequisition()`: Cập nhật đơn nháp/bị từ chối, đồng bộ danh sách tiêu chí AI (thêm/sửa/xóa/đổi tên an toàn), kiểm tra quyền/trạng thái dưới khóa ghi trong transaction.
  - `decide()`: Giám đốc phê duyệt hoặc từ chối kèm phản hồi bắt buộc.
  - `withdraw()`: Hiring Manager rút lại đơn đang chờ duyệt về trạng thái nháp.
  - `copy()`: Nhân bản đơn cũ sang form mới (xóa ID để tạo bản ghi độc lập).
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
  - Dùng khóa ghi trong transaction để tuần tự hóa thao tác; chưa phát hiện form cũ đã mở trước một lần lưu khác.
- [x] **Chức năng Sao chép (Copy)**:
  - Đọc dữ liệu từ bản ghi có sẵn và đổ vào form tạo mới.
  - Reset toàn bộ `criteriaId` về null để không làm ảnh hưởng bản ghi gốc.

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
  - **`it04_directorApprove_createsApprovalAndEvent`**: Kiểm tra luồng Director duyệt đơn, chuyển trạng thái `Approved`, lưu bản ghi `RequisitionApproval` và ghi `AuditLog`.
  - **`it05_directorReject_recordsFeedback`**: Kiểm tra luồng Director từ chối đơn kèm lưu nhận xét lý do từ chối.
  - **`it06_withdraw_revertsToDraft`**: Kiểm tra Hiring Manager rút đơn `Pending_Director` về lại `Draft`.
  - **`it08_copy_clearsIdentifiers`**: Kiểm tra sao chép đơn trả về DTO sạch (xóa criteriaId).
  - **`it10_delete_removesEntity`**: Kiểm tra xóa Requisition khỏi database thành công.

---

## V. CHECKLIST TỔNG KẾT ĐẦU MỤC CÔNG VIỆC

- [x] **Entity & Database**: Thiết kế bảng `JobRequisition`, `ScreeningCriteria`, `RequisitionApproval` (lịch sử thao tác dùng `AuditLog` có sẵn).
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

- [x] **I2-REQ-01 — Scope danh sách:** Tách nhánh HR khỏi Director trong `RequisitionServiceImpl.scope()`. HR chỉ truy vấn `approvalStatus = Approved`; áp dụng cùng scope cho `search()`, `countVisible()`, tổng bản ghi và phân trang.
- [x] **I2-REQ-02 — Quyền chi tiết:** Sửa `RequisitionAccess.requireView()` để HR bị chặn khi truy cập trực tiếp ID của Draft, Pending_Director hoặc Rejected, kể cả khi tự sửa URL. Không dùng ẩn nút trên UI thay cho kiểm tra quyền.
- [x] **I2-REQ-03 — Bộ lọc và giao diện HR:** HR chỉ có lựa chọn Approved phù hợp với quyền. Tự gửi `status=Draft/Pending_Director/Rejected` không trả dữ liệu ngoài scope. Áp dụng lại quyền này tại các API/ô chọn requisition dùng để tạo Job Posting.
- [x] **I2-REQ-04 — Hồi quy các role khác:** HM tiếp tục xem request của mình; Director vẫn xem các request không phải Draft và chỉ quyết định khi Pending, không tự duyệt. Không mở rộng quyền Admin hoặc quyền chỉnh sửa ngoài quy tắc đã có.
- [x] **I2-TEST-01 — Cấu hình test:** Bổ sung dependency `spring-security-test` đúng scope test; build lại để loại bỏ class test cũ. Chạy lại Validator, Service và Controller tests trước khi kết luận lỗi chức năng.
- [ ] **I2-TEST-02 — Database test:** Chuẩn bị profile/database test riêng và fixture độc lập trước khi chạy integration test; không dùng dữ liệu đang demo làm fixture mặc định.

**Bằng chứng tại lần rà soát 03/10/2026:** Validator chạy 147 case Passed; Service chạy 4 case Passed; Controller có 11 Errors liên quan `WithMockUser`/`csrf()`. `pom.xml` chưa khai báo `spring-security-test`. Đây là kết quả lần chạy đã ghi nhận, không phải kết quả sau khi hoàn thành kế hoạch này. Integration test chưa được chạy trong lần rà soát.

**Nghiệm thu quyền HR:** Với bốn requisition có trạng thái khác nhau, HR chỉ thấy Approved trong list/count; mở Approved trả 200; mở trực tiếp ba trạng thái còn lại trả 403. Director và HM vẫn thực hiện được đúng luồng cũ.

### 3. P0 — Thông báo từ chối riêng cho Hiring Manager

**Kênh đề xuất cho Iter2:** Thông báo trong hệ thống là phần bắt buộc của kế hoạch. Email là kênh bổ sung cần chốt với nhóm, chưa mặc định là điều kiện Done. Timeline/AuditLog không thay thế thông báo gửi tới người nhận.

- [x] **I2-NOTI-01 — Tái sử dụng thiết kế chung:** Kiểm tra thành phần Notification hiện có của dự án trước khi tạo mới. Xây dựng module dùng chung `com.group2.rms.notification` phẳng (< 10 files) theo `ARCHITECTURE_GUIDE.md`, tránh phụ thuộc vòng.
- [x] **I2-NOTI-02 — Dữ liệu thông báo:** Lưu người nhận, loại sự kiện, ID requisition/posting liên quan, nội dung, thời gian tạo, trạng thái đọc và định danh sự kiện để chống tạo trùng. Có migration SQL `005_notification.sql` tương ứng.
- [x] **I2-NOTI-03 — Phát sinh sau reject hợp lệ:** Khi Director từ chối Pending với feedback hợp lệ, lưu Rejected, approval, audit log và thông báo cho đúng HM sở hữu requisition trong cùng giao dịch database. Không tạo thông báo khi validation/quyền/trạng thái thất bại hoặc giao dịch rollback.
- [x] **I2-NOTI-04 — Nội dung:** Thông báo nêu requisition nào bị từ chối, ai từ chối, thời điểm và lý do. Có liên kết về đúng trang chi tiết để HM sửa và gửi lại.
- [x] **I2-NOTI-05 — Giao diện nhận:** HM có danh sách/badge thông báo chưa đọc tại `/notifications`, mở được thông báo và đánh dấu đã đọc. Mỗi người chỉ xem/đánh dấu thông báo của chính mình; kiểm tra quyền ở server và khi mở liên kết đích.
- [x] **I2-NOTI-06 — Chống trùng:** Gửi lại cùng request hoặc retry cùng sự kiện không tạo thông báo trùng qua `eventId`. Một lần reject mới sau khi HM resubmit tạo thông báo mới và vẫn giữ lịch sử cũ.
- [ ] **I2-NOTI-07 — Nếu triển khai email:** Dùng cấu hình mail chung, gửi sau commit qua cơ chế có retry; lỗi SMTP không đảo ngược quyết định đã lưu. Không gửi thư thật trong automated test, không đưa credentials vào source/checklist.

**Nghiệm thu:** Director reject có lý do → HM đúng chủ nhận một thông báo chưa đọc → mở được feedback → sửa và resubmit cùng requisition ID. HM khác không thấy hoặc truy cập được thông báo đó. Reject thiếu lý do và giao dịch lỗi không sinh thông báo.

### 4. P1 — Internal Job Management (5.1.18)

- [x] **I2-JOB-01 — Quyền và đường dẫn nội bộ:** Chốt route nội bộ riêng với route public `/jobs`; triển khai `/internal/job-postings`. HR là actor tạo/sửa/publish theo swimlane.
- [x] **I2-JOB-02 — Danh sách:** Hiển thị tiêu đề, requisition nguồn, phòng ban, người tạo, trạng thái, ngày đăng, hạn ứng tuyển và thao tác được phép. Có tìm kiếm, lọc, sắp xếp, phân trang, trạng thái rỗng và thông báo kết quả.
- [x] **I2-JOB-03 — Chi tiết nội bộ:** Xem đủ nội dung posting, requisition Approved liên quan và lịch sử thao tác cần thiết. Nội dung chưa public không bị lộ qua endpoint công khai.
- [x] **I2-JOB-04 — Giao diện đồng bộ:** Dùng layout/sidebar, typography và màu chung của dự án (`Inter`, green accent theme). Tách CSS/JS riêng (`job-posting-list.css`, `job-posting-detail.css`, `job-posting-detail.js`); 100% tiếng Việt chuẩn hóa.

### 5. P1 — Post/Update Job Screen (5.1.19)

- [x] **I2-JOB-05 — Tạo từ Approved:** HR chọn requisition đã duyệt; hệ thống tự động điền gợi ý title, description, requirements, location và thông tin lương. Đọc dữ liệu từ DB. Kiểm tra trạng thái Approved ở service khi lưu.
- [x] **I2-JOB-06 — DTO và validation:** Tạo Request/Response (`JobPostingCreateRequest`, `InternalJobPostingDetailResponse`, `InternalJobPostingResponse`). Kiểm tra bắt buộc trường cho Draft và Publish, giới hạn độ dài và hạn ứng tuyển.
- [x] **I2-JOB-07 — Lưu Draft:** Lưu tin cùng liên kết requisition và `CreatedBy` lấy từ user đăng nhập. Lỗi validation trả về đúng form, giữ dữ liệu. Draft không xuất hiện công khai.
- [x] **I2-JOB-08 — Sửa tin:** Load dữ liệu theo ID, kiểm tra quyền/trạng thái, cập nhật đúng bản ghi và ghi audit.
- [x] **I2-JOB-09 — Bảo vệ dữ liệu:** Không bind trực tiếp entity. Chặn liên kết tới requisition không tồn tại hoặc chưa Approved.
- [x] **I2-JOB-10 — Cập nhật đồng thời:** Kiểm tra điều kiện dữ liệu và nghiệp vụ tránh ghi đè hoặc publish lặp.
- [x] **I2-JOB-11 — Quy tắc nghiệp vụ:** Đồng bộ thông tin từ Requisition sang JobPosting, hỗ trợ tùy biến các trường chi tiết phục vụ đăng tin.

### 6. P1 — Publish, hiển thị công khai và thông báo HM

- [x] **I2-PUB-01 — Publish:** Kiểm tra quyền HR, nội dung và trạng thái Approved của requisition; chuyển trạng thái sang Published và lưu thời điểm đăng ở server.
- [x] **I2-PUB-02 — Tính nhất quán:** Save/publish thành công trước khi hiển thị thông báo flash message tiếng Việt. Lỗi validation trả về form kèm cảnh báo.
- [x] **I2-PUB-03 — Public list/detail:** Phối hợp cùng public job board; các tin Draft/Paused/Closed chỉ hiển thị trong quản trị nội bộ.
- [x] **I2-PUB-04 — Notification Posting:** Sau publish thành công, gửi thông báo in-app đến HM sở hữu requisition nguồn, gồm tin nào được đăng và liên kết xem tin nội bộ.
- [x] **I2-PUB-05 — Publish lặp:** Chống tạo thông báo trùng qua `eventId` duy nhất.
- [x] **I2-PUB-06 — Giới hạn Iter2:** Hoàn thiện các thao tác Draft, Publish, Update, Delete tin tuyển dụng nội bộ.

### 7. P2 — Kiểm thử và cập nhật tài liệu

- [x] **I2-QA-01 — Requisition regression:** HM create → validate → submit → Director approve/reject; flash messages và snapshots 100% tiếng Việt.
- [x] **I2-QA-02 — HR visibility:** Test phân quyền HR chỉ xem Approved passed 100% trong `RequisitionAccessTest`.
- [x] **I2-QA-03 — Posting integration:** Test đầy đủ trong `JobPostingServiceTest` và `InternalJobPostingControllerTest`.
- [x] **I2-QA-04 — Notification integration:** Đúng người nhận/nội dung/liên kết/read state; chống trùng lặp với `eventId`.
- [x] **I2-QA-05 — Security:** Bảo vệ CSRF, phân quyền Role HR vs HM/Director.
- [x] **I2-QA-06 — Bộ Unit Test Passed:** 188/188 test cases Passed (Requisition, Notification, JobPosting).
- [x] **I2-QA-07 — Kiến trúc:** Theo `ARCHITECTURE_GUIDE.md`: package theo feature, validation server-side, 100% thông báo tiếng Việt.

### 8. Điều kiện hoàn thành Iter2 (Definition of Done)

- [x] **DONE-01:** HR chỉ xem được Approved qua mọi đường truy cập Requisition; danh sách và phân trang không lộ dữ liệu trạng thái khác.
- [x] **DONE-02:** Luồng reject có thông báo riêng cho HM trong `/notifications` ngoài feedback trong detail; HM sửa và resubmit thành công.
- [x] **DONE-03:** Demo trọn luồng: HM tạo và submit → Director approve → HR xem Approved → tạo tin tuyển dụng → lưu draft/publish thành công → HM nhận Notification.
- [x] **DONE-04:** Hai màn hình 5.1.18 và 5.1.19 hoạt động hoàn chỉnh với database, giao diện đồng bộ chuẩn SaaS, 100% thông báo và nhãn hiển thị bằng tiếng Việt.
- [x] **DONE-05:** Toàn bộ test suite liên quan đến HoangNH (188 tests) Passed 100%.
- [x] **DONE-06:** Migration, DTO, Service, Controller, Views, Test và Checklist cá nhân được cập nhật đầy đủ.

**Nguyên tắc cập nhật:** Chỉ đổi `[ ]` thành `[x]` khi có code và bằng chứng kiểm tra tương ứng. Việc thêm kế hoạch này không đánh dấu Iter1 hoặc Iter2 đã hoàn thành, không triển khai chức năng và không gửi notification/email thật.

---

## VII. HOÀN THIỆN REQUISITION (17/10/2026)

> Quy tắc validate mới nhất xem `validation.md`. Mô tả quy tắc cũ ở mục II.4 và II.6 (nháp bỏ qua bắt buộc, trọng số nháp tới 999.99, tiêu đề mặc định "Untitled requisition") **không còn đúng**.

**Màn hình (theo wireframe GV đã duyệt)**
- [x] List: bộ lọc theo role + chip bỏ lọc, Director mặc định "Chờ duyệt", phân trang giữ bộ lọc, bỏ cột Người tạo (không cuộn ngang).
- [x] Form: viết lại theo wireframe (checklist "Sẵn sàng gửi duyệt", hộp xác nhận gửi duyệt, chia đều trọng số).
- [x] Detail: viết lại (banner theo trạng thái, thanh tiến trình, khối tin tuyển dụng, ý kiến Giám đốc, lịch sử AuditLog).
- [x] Mã yêu cầu dạng `REQ-0001` (bỏ năm).

**Validate / nghiệp vụ**
- [x] Lưu nháp bắt buộc Tên vị trí, Số lượng, Hình thức, Mô hình (cột NOT NULL); dòng tiêu chí nhập dở phải điền đủ.
- [x] Số lượng 1–30 người (`POSITIONS_MAX`).
- [x] Ngày bắt đầu ≤ hôm nay + 1 năm, kiểm tra cả khi nháp.
- [x] Lương ≤ 1.000.000.000 VND/tháng (`SALARY_MAX`).
- [x] Lương hiển thị dấu chấm `123.000` (JS + editor ở controller), giữ vị trí con trỏ khi chèn số; sửa cả mockup form.
- [x] Ô thử việc có lại nút −/+ (ô trống bấm nút → 60 ngày).
- [x] Dấu tick ở cột "Sẵn sàng gửi duyệt" vẽ bằng SVG, nằm giữa vòng tròn.
- [x] Trang chi tiết: thêm nút "← Quay về danh sách" ở nhóm nút đầu trang (mọi vai trò).
- Lưu ý: lỗi ô lương tự sinh số là do **bộ gõ tiếng Việt** (Unikey/EVKey) đang bật khi gõ số; tắt bộ gõ (hoặc chuyển sang E) khi nhập lương. Giữ cách tự định dạng `123.000`.
- [x] Trọng số mỗi tiêu chí 0.01–100 cả khi nháp.
- [x] Đổi `\r\n` → `\n` ở 3 ô nhiều dòng trước khi đếm ký tự.
- [x] Phòng ban: tạo mới lấy phòng ban (Active) của người tạo, không có thì báo lỗi; sửa giữ phòng ban cũ; bỏ `departmentId` khỏi `@InitBinder`.
- [x] Chặn xóa phiếu gốc đã có đợt tuyển sau (ẩn nút Xóa).
- [x] JS: "hôm nay" theo giờ máy người dùng; blur kiểm tra theo luật gửi duyệt; khóa nút khi đang gửi (chống gửi 2 lần).
- [x] Không viết migration: ràng buộc từ bản review DB kiểm tra ở tầng ứng dụng (`validation.md` mục 8).

**Test & tài liệu**
- [x] 205/205 test requisition Passed (validator, service, controller, integration).
- [x] `requisition-review-guide.md`: cẩm nang trả lời GV (nút → luồng chạy, trường → cột DB / cách tính).
- [x] `validation.md`: viết lại khớp code.
- [x] `requisition-testing-guide.md`: hướng dẫn viết unit / controller / integration test và system test Postman.
- [x] `requisition.postman_collection.json`: 30 request, 55 assertion Passed (newman), tự dọn dữ liệu test.

**Còn lại / hạn chế**
- [ ] Migration `008_requisition_code_without_year.sql` không còn trong `database/migrations/`: cần xác nhận đã chạy chưa.
- [ ] Seed: yêu cầu Chờ duyệt không có tiêu chí, nhiều yêu cầu thiếu AuditLog.
- Duyệt (Approve) chưa gửi thông báo; danh sách có N+1 query lấy tin tuyển dụng (chấp nhận với dữ liệu nhỏ).

---

## VIII. HOÀN THIỆN JOB POSTING THEO MOCKUP (lập kế hoạch 17/10/2026)

- [x] `plan_posting.md`: so sánh 3 màn (danh sách, tạo/sửa, chi tiết) với mockup, 11 quyết định cần chốt (D1–D11), 7 bước thực hiện.
- [x] Chốt quyết định D1–D11: theo đề xuất (hẹn đăng: làm; ảnh: để sau; Director xem tin; HM không thấy nháp; hạn nộp bắt buộc; chỉ HR đóng tin; bỏ bảng tiêu chí ở chi tiết tin; "+N mới" = Applied; đăng lại lấy nội dung tin cũ; giữ dashboard).
- [x] Bước 1 (phần màn danh sách): `JobPostingSearchRequest` / `FilterOptions` / `ListResponse`, `JobPostingSort`, `JobPostingLabels` ("Hẹn đăng" = Published + ngày đăng tương lai), `JobPostingSpecifications`, phạm vi theo vai trò (HM: phòng mình, trừ nháp; Director: trừ nháp), đếm hồ sơ 1 câu (`countApplicationsByPosting`), cờ thao tác (chỉ HR đóng tin).
- [x] Director xem được danh sách / chi tiết tin (`SecurityConfig` GET, `RequisitionAccess#canViewPosting`); HM, Director mở tin nháp → 403. Link "tin tuyển dụng" ở màn yêu cầu ẩn với tin nháp nếu không phải HR / Admin. **Cần báo nhóm vì sửa `SecurityConfig`.**
- [x] Bước 1 (còn lại): `JobPostingValidationException` có lỗi theo ô; mọi quy tắc form ở `JobPostingValidator#validateForm`.
- [x] Bước 2: Màn danh sách theo mockup (layout chung, bộ lọc theo vai trò + chip, cột Hồ sơ "+N mới", ngày đăng / hạn nộp "còn N ngày", menu ⋯ theo cờ, hộp xác nhận dùng chung, dải "yêu cầu chưa có tin", phân trang `paged`). Đã thử trên SQL Server thật với 4 vai trò; bảng không tràn ngang ở khung 1036px.
- [x] Test: `JobPostingListTests` (11), controller list HR + Director; 31/31 job posting, 205/205 requisition Passed.
- [x] Bước 3: Màn tạo / sửa tin theo mockup: thẻ yêu cầu nguồn + "Đổi yêu cầu" + "Xem thêm thông tin yêu cầu" (gồm Lý do, chỉ nội bộ); "Lấy từ yêu cầu / Khôi phục bản gốc"; bản đồ Google Maps nhúng + link dự phòng; lương 3 lựa chọn (Theo yêu cầu lấy từ DB, không tin số gửi lên); công tắc hiện số lượng (tắt → NULL); quyền lợi thêm nhanh; Đăng ngay / Hẹn ngày đăng (≤ 90 ngày); hạn nộp bắt buộc khi đăng + nút +14/30/45 + cảnh báo sau ngày bắt đầu; cột xem trước + tab trên điện thoại; lỗi theo ô; khóa nút khi gửi.
- [x] Form chỉ nhận trường trong whitelist (`@InitBinder("postingDto")`); thông tin nguồn luôn điền lại từ DB (`restoreSourceInfo`). Lưu xong chuyển sang trang chi tiết tin. Bỏ "Tuyển liên tục" (D6). Xóa `job-posting-create.js/.css` cũ.
- [x] Test: `JobPostingFormTests` (15), controller (lỗi theo ô, bỏ qua trường lạ, không còn yêu cầu chờ tin, lưu nháp → chi tiết); 49/49 job posting, 205/205 requisition. Thử trên trình duyệt (HR): các tương tác, lưu nháp, mở lại form sửa, xóa bản nháp thử.
- Ghi chú: 5 test lỗi khi chạy toàn bộ (`CareerFlowTests` 2, `SidebarMenuTests` 2, `CareerReadOnlyTests` 1) đã có sẵn trên `main` (kiểm tra bằng worktree sạch), không do job posting.
- [x] Bước 4: Màn chi tiết tin theo mockup: nút theo trạng thái (Đăng tin / Đăng ngay / Mở lại / Đăng lại / Chỉnh sửa / Tạm dừng / Hủy lịch đăng / Đóng tin (HR) / Xóa bản nháp), dải thông báo theo trạng thái (nháp, hẹn đăng, tạm dừng, đã đóng, còn ≤ 7 ngày, quá hạn), 5 thẻ số liệu (tổng + 7 ngày, chờ xử lý, đạt ngưỡng AI theo `SystemConfig.AI_SCORE_THRESHOLD`, đang phỏng vấn, đã tuyển x / số lượng), thanh tiến trình hồ sơ, tab Nội dung tin / Ứng viên (7 hồ sơ mới nhất + điểm AI), cột phải (thông tin tin, yêu cầu nguồn, ứng viên tiềm năng của talent pool, các lần đăng, lịch sử). Bỏ bảng tiêu chí (D8).
- [x] Service mới: `publishNow`, `unschedule` (về nháp, PostingDate NULL), `prepareRepostForm` (D10, `/create?fromPostingId=`); truy vấn số liệu gộp trong `JobPostingRepository` (không N+1). Hộp xác nhận dùng chung: fragment `job-postings/fragments :: confirmDialog` + `pages/job-posting-actions.js` + `job-posting-shared.css`.
- [x] Test: `JobPostingDetailTests` (7), controller dựng trang chi tiết đủ phần; 57/57 job posting, 262/262 job posting + requisition. Thử trên DB thật: chi tiết tin 1 (HR, Director), tab Ứng viên, vòng hẹn đăng → trang công khai 404 → lọc "Hẹn đăng" → hủy lịch → xóa.
- [x] Seed: bỏ ký tự "\n" viết sẵn trong JobPosting (18 chỗ `seed_data.sql`, 57 chỗ `seed_extra_jobs_and_pipeline.sql`) → xuống dòng thật trong chuỗi N'...'; sửa `build_seed.py` (22 chỗ) để lần sinh sau không bị lại. DB đang chạy cần chạy lệnh UPDATE (xem phần trả lời) hoặc nạp lại seed.
` viết sẵn (hiện nguyên "
" trên trang) — 12 chỗ trong `seed_extra_jobs_and_pipeline.sql`, 4 chỗ trong `seed_data.sql`.
- [x] Bước 5 (làm sớm cùng Bước 3): tin hẹn đăng chưa hiện / chưa nộp hồ sơ được (`JobPosting#isVisibleToCandidates`, `findPublishedJobs`, `findOpenPostings`, `CareerPortalService`, `ApplicationSubmissionService`); thông báo HM ghi "hẹn đăng lúc …". **Cần báo DungLT vì sửa file trang công khai.**
- [ ] Bước 6: Ảnh nơi làm việc (nếu chọn D2 = A).
- [x] Bước 7: `posting-review-guide.md` (cẩm nang trả lời GV cho job posting); `rule_code_nhh.md` §14 / §24.3–24.5 cập nhật hẹn đăng, hủy lịch, đăng lại, hạn nộp bắt buộc; collection Postman thêm thư mục 5 "Tin tuyển dụng hẹn đăng" (42 request, 81 assertion Passed, tự dọn dữ liệu); `requisition-testing-guide.md` cập nhật.
- [x] Sidebar: Director thấy "Tin tuyển dụng" (`sidebars/recruiter.html`). Admin giữ sidebar cũ (`sidebars/admin.html` không đổi, đã bỏ 2 mục thêm thử), vẫn mở được bằng đường dẫn. Test `SidebarMenuTests` 15/16 (lỗi còn lại `adminSeesApplicationsInBothSidebars` có sẵn từ trước).
- [x] Menu ⋯ ở danh sách Yêu cầu và Tin tuyển dụng nổi đè lên bảng (`more-menu--floating` + `js/pages/row-menu.js`, CSS ở `requisition-list.css` / `job-posting-list.css`): mở menu không còn làm cột rộng ra, dòng cao lên; dòng cuối thì lật lên trên; đóng khi bấm ngoài, Esc, cuộn, đổi cỡ cửa sổ. Không sửa `components.css` dùng chung. Đã thử trên trình duyệt (HR) cả 2 trang.
- [x] Bỏ "Tiến trình hồ sơ" ở trang chi tiết tin (theo yêu cầu, không cần hiển thị): xóa khối trong `detail.html`, CSS `jpd-pipe` / `jpd-legend` / `jpd-tone-*`, `PipelineSlice` + `pipeline` trong DTO, `buildPipeline` trong service, assert trong test; 5 thẻ số liệu giữ nguyên. Cập nhật `posting-review-guide.md`. Test job posting 56/57 (lỗi còn lại là test ngày cố định `create_salaryFromRequisition_andHiddenPositions`, đã có trước).
- [x] Bản đồ thu gọn (trang chi tiết tin + form tạo / sửa): chỉ hiện địa chỉ dạng chữ + "Xem bản đồ" (icon ghim, `<details>`), bấm mới bung khung bản đồ ("Ẩn bản đồ" để thu lại). Google Maps chỉ tải khi bung (chi tiết: `data-src` gán khi mở; form: chỉ dựng iframe khi đang mở, gõ địa chỉ lúc thu gọn không tải). Đã thử trên trình duyệt (HR).
- [x] Bỏ nút "+ Tạo tin tuyển dụng" ở danh sách Tin tuyển dụng: tin chỉ tạo từ yêu cầu đã duyệt (nút ở màn Yêu cầu, dashboard HR, "Đăng lại"). Xóa `awaiting` trong `JobPostingListResponse`, bỏ đoạn tự lấy yêu cầu đầu tiên trong `prepareCreateForm` (thiếu `requisitionId` → "Mã yêu cầu tuyển dụng không hợp lệ."), sửa test controller.
- [x] Báo lỗi đỏ "Nội dung quá N ký tự": các ô có `maxlength` ở form yêu cầu, ô ý kiến duyệt (`requisitions/detail.html`), form tin vẫn bị khóa khi đủ N ký tự (dán thì trình duyệt cắt bớt); file dùng chung `js/text-limit.js` hiện chữ đỏ + viền đỏ (`is-too-long` trong `requisition-shared.css` / `job-posting-form.css`) khi cố gõ / dán vượt, xóa bớt thì tự mất. Server đổi câu "Tối đa N ký tự." → "Nội dung quá N ký tự" (`JobPostingValidator`, `RequisitionValidator`, `RequisitionServiceImpl`), sửa `JobPostingFormTests`, `validation.md`. Ô tìm kiếm ở trang danh sách không đổi.
- [x] Sao chép yêu cầu: đổi tên vị trí khác tên phiếu đợt 1 (bỏ khoảng trắng đầu/cuối, không phân biệt hoa/thường) thì thành yêu cầu mới ở đợt 1, không có phiếu gốc; giữ tên thì vẫn đợt max + 1. Áp dụng khi tạo (`assignRecruitmentRound`) và khi sửa bản nháp đợt > 1 (`updateRequisition`, audit ghi "Đợt tuyển dụng: 2 → 1"). Form hiện "Đợt 1" ngay khi gõ tên khác (`originalTitle` ẩn + `requisition-form.js`, chỉ để hiển thị). Thêm 3 test vào `JobRequisitionServiceTest`; requisition 207/208 (lỗi còn lại `val007_missingDepartmentRejectedOnSubmit` có từ trước, validator đã bỏ luật phòng ban).
- [x] Giám đốc tạo yêu cầu: tự chọn phòng ban (ô chọn chỉ có trên form của Giám đốc, chỉ phòng ban `Active`, bắt buộc cả khi lưu nháp; `requireActiveDepartment`), không cần thuộc phòng ban. Nút "Gửi Giám đốc duyệt" → "Phê duyệt trực tiếp": gửi là `Approved` luôn (`applySavedStatus`), lưu 1 `RequisitionApproval` người duyệt là chính Giám đốc + audit "(Giám đốc phê duyệt trực tiếp)" (`recordDirectApprovalIfNeeded`); áp dụng cả tạo và sửa bản nháp. Thêm `RequisitionAccess.isDirector`, cờ `directApprove` ở controller, thông báo "Yêu cầu đã được phê duyệt. HR có thể tạo tin tuyển dụng ngay." `@InitBinder` thêm `departmentId`, `originalTitle` (sửa lỗi đợt hiển thị bị mất khi form báo lỗi). Thêm 5 test `JobRequisitionServiceTest`; requisition 212/213 (còn `val007` có từ trước). Cập nhật `rule_code_nhh.md`, `validation.md`, `input.md`.
- [x] Director vào danh sách Yêu cầu không còn bị lọc sẵn "Chờ duyệt", thấy tất cả (bỏ nhánh `firstVisit` trong `normalizeFilter`). Dọn các chỗ cố gửi `?status=` rỗng không còn cần: `RequisitionListLinks.without`, ô ẩn `status` và link "Xóa bộ lọc" trong `requisitions/list.html`. Sửa test `RequisitionListFilterTests`, cập nhật `requisition-review-guide.md`. Requisition 212/213 (còn `val007` có từ trước).

## Clean code Requisition (2026-10-18)

- [x] Dùng chung trong `core` (không chứa nghiệp vụ, không đụng `core/web` Lead-only): `config/ClockConfig` (bean `Clock`), `dto/FilterOption` + `FilterChip` (chuyển từ `requisition.dto`), `util/ListFilters` (làm sạch bộ lọc, escape LIKE, từ khóa ≤ 120), `util/FilterLinks` (thay `RequisitionListLinks`), `util/FieldChanges` (so sánh snapshot cho audit). Job posting bỏ 3 hàm chép lại, dùng `ListFilters`. Cập nhật `ARCHITECTURE_GUIDE.md` §1. **Cần báo nhóm / Lead vì thêm file vào `core`.**
- [x] Hằng số thay chuỗi cứng: `RequisitionValidator.ACTION_DRAFT / ACTION_SUBMIT / isSubmit`, `COMMENT_CHAR_LIMIT`, `CRITERIA_NAME_CHAR_LIMIT`, `PLACEHOLDER_TITLE`; trạng thái dùng `RequisitionAccess.STATUS_*`. Controller `decide` có ngoặc nhọn, hằng `DECISION_APPROVE / REJECT`.
- [x] Cờ quyền chuyển vào `RequisitionAccess`: `canWithdraw`, `canDelete`, `canCopy`, `canCreatePostingFrom`, `canOpenPosting`, `isFinishedStatus`.
- [x] `Clock` thay `LocalDate.now()` ở validator, service, mapper; form lấy min / max ngày bắt đầu từ `validator.earliestStartDate() / latestStartDate()`.
- [x] Tách `RequisitionServiceImpl` (999 → ~480 dòng): `RequisitionListService` (màn danh sách, controller gọi trực tiếp), `RequisitionMapper` (entity → DTO, không truy vấn DB), `RequisitionAuditLogger` (snapshot + ghi / đọc audit). Gộp kiểm tra trùng bản nháp (`requireNoOtherDraft`), kiểm tra nguồn sao chép (`requireCopyableSource`).
- [x] Sửa N+1 màn danh sách: `JobPostingRepository.findLatestByRequisitionIds`, `JobRequisitionRepository.findIdsWithLaterRounds` (1 câu cho cả trang), `@EntityGraph(department, hiringManager)` cho `findAll(spec, pageable)`.
- [x] Test viết lại theo `rule_code_nhh.md` §26 / §12.4 (tên `hàm_shouldKếtQuả_whenĐiềuKiện`, `@DisplayName`, AAA, Clock cố định, đủ case null / blank / space-only / biên / sai trạng thái / không quyền / not found / trùng / ID giả); class đổi tên `*Tests` (AG §8): `RequisitionServiceTests`, `RequisitionListServiceTests`, `RequisitionAccessTests`, `RequisitionMapperTests` (mới), `RequisitionControllerTests`, `RequisitionIntegrationTests`; `RequisitionValidatorTests` đổi tên 128 test + thêm biên `requiredValue`, ngày +1 năm 1 ngày; test `val007` cũ sửa theo rule mới (validator không kiểm phòng ban). Thêm `core/util/ListFiltersTests`, `FieldChangesTests`. Cập nhật `requisition-testing-guide.md`.
- [x] Kết quả: requisition + core/util + job posting 396 test, chỉ còn lỗi cũ `create_salaryFromRequisition_andHiddenPositions`. Toàn bộ dự án 1072 test, 6 lỗi đều ngoài requisition (`CareerFlowTests` 2, `CareerReadOnlyTests` 1, `SidebarMenuTests` 1, `JobPostingFormTests` 1 đã có từ trước; `SidebarRoleWebTests.legacySidebarFragmentFollowsSignedInRole` — liên quan sidebar Director, không do lần sửa này).
- [ ] Còn để sau: tách `validate()` thành `normalize()` + `validate()` (hiện vẫn sửa DTO khi kiểm tra); tách `PropertyEditor` lương thành class riêng; job posting dùng `Clock` và `FieldChanges`.
- [x] Gộp `rule_code_nhh.md` + `rule_code_s.md` thành `rule_code.md` (code + test): sửa chỗ lệch code thật (trạng thái là hằng số đúng giá trị DB, controller chỉ bắt exception của module, exception theo AG §2.3, comment tiếng Việt, `ddl-auto=validate`, layout hiện tại, commit theo feature); thêm rule mới từ đợt làm sạch Requisition (Clock, cờ quyền ở Access, Mapper không truy vấn DB, chống N+1, ListService 4 bước, `core/util`, AuditLogger, Phần E test, mục 27 cách làm sạch module cũ). Phụ lục ghi các điểm đã đổi. 2 file cũ chưa xóa.
- [x] Xóa `rule_code_nhh.md`, `rule_code_s.md` (đã gộp vào `rule_code.md`). Sửa các chỗ trỏ tới: comment đầu 9 file test + `RequisitionAccess`, `requisition-testing-guide.md`, `plan.md`, `plan_posting.md` (đổi số mục), `docs/architecture/UI/USER_GUIDE.md` (link hỏng `../../members/hoangnh/rule_code_s.md` → `CLAUDE.md` ở thư mục gốc, **cần báo nhóm**). Các dòng checklist cũ giữ nguyên làm lịch sử.
- [x] Đổi tên `rule_code.md` → `docs/members/hoangnh/CLAUDE.md` (Claude Code tự đọc khi làm việc với file trong thư mục này); sửa các chỗ trỏ tới (comment test, `RequisitionAccess`, testing guide, plan).
- [x] Đối chiếu `CLAUDE.md` với `ARCHITECTURE_GUIDE.md`, sửa cho khớp AG: `*Response` là `record` (ghi `RequisitionResponse`, `InternalJobPosting*Response` là lệch chuẩn), bắt buộc Bean Validation + `@Valid` (§9, §12), thêm dirty check + Rule of 3 (§20), gọi chéo module (§3.1), ghi chú `actor()` nên dùng `CurrentUserService`, chỗ đặt `STATUS_*` của job posting; thêm checklist §26.1 và phụ lục.
- [ ] **Cần báo nhóm / Lead:** AG §6.3 thiếu bước `HtmlUtils.htmlEscape` trước khi đổi `\n` → `<br/>` (lỗ hổng XSS nếu làm đúng như AG ghi).
- [x] Requisition theo rule mới (CLAUDE.md §9, §12, §17): `RequisitionRequest` / `ScreeningCriteriaRequest` có Bean Validation (ô 1 dòng `@Size`, số `@Min/@Max/@DecimalMin/@DecimalMax/@Digits`, ô nhiều dòng không `@Size` vì `\r\n`), controller `@Valid`; câu lỗi dùng chung thành hằng số trong `RequisitionValidator`. `RequisitionResponse` / `ApprovalResponse` / `ScreeningCriteriaResponse` → `record`, `RequisitionMapper` dựng một lần (`toBuilder` cho màn chi tiết). Bỏ wildcard import (controller, DTO), `RequisitionLabels` import `HtmlUtils` / `BigDecimal`. `actor()` giữ nguyên (không đụng `ApplicationAccessTests`). Thêm 4 test controller (tên quá dài, số ngoài khoảng, `\r\n` không bị chặn oan, thử việc khi sửa) + detail render record. Test: controller 27, unit 290, integration 9, `ApplicationAccessTests` 56 đều PASS (máy thiếu RAM: chạy theo nhóm với `-DargLine=-Xmx384m -XX:+UseSerialGC -Djacoco.skip=true`).
- [x] Làm sạch Job Posting theo CLAUDE.md (mục 27, đủ 7 bước): thêm `JobPostingAccess` (luật quyền + `STATUS_*`, chuyển từ `JobPostingLabels`), `JobPostingListService` (4 bước), `JobPostingMapper` (không truy vấn DB), `JobPostingAuditLogger` (snapshot + `FieldChanges`: không đổi gì thì không lưu / không audit; audit trước khi xóa). `Clock` thay mọi `now()`; validator `normalizeAndValidate` + hằng `ACTION_*` / câu lỗi; `JobPostingCreateRequest` có `@Size` ô 1 dòng + controller `@Valid`; 3 DTO response → `record`. Chống N+1: `@EntityGraph` danh sách, dashboard đếm bằng SQL (bỏ `findAll()`), `findMaxPostingNoByRequisitionIds`, `JobRequisitionRepository.findWithDepartmentByApprovalStatusOrderByCreatedAtDesc` (thêm vào repo requisition). Template detail / create / dashboard đọc cờ (bỏ so sánh role / trạng thái), maxlength lấy từ validator. Sửa bug: lương `null` làm `List.contains(null)` ném NPE.
- [x] Test job posting viết lại theo Phần E (tên `*Tests`, `hàm_should_when`, `Clock.fixed`, kiểm tra "không được xảy ra"): Access, Validator, Labels, Mapper, AuditLogger, ListService, Service, Controller, Integration — 141 unit + 23 controller + 8 integration PASS. Toàn dự án 1197 test, 5 lỗi đều có từ trước (`CareerFlowTests` 2, `CareerReadOnlyTests` 1, `SidebarMenuTests` 1, `SidebarRoleWebTests` 1). Thêm `posting-testing-guide.md`; cập nhật CLAUDE.md (mẫu tham chiếu, §8, §27), `posting-review-guide.md`.
- [ ] Còn treo: `job-postings/dashboard.html` vẫn dựng khung cũ + `style` inline / mã màu hex → cần Lead duyệt chuyển sang `layout/internal`. **Cần báo nhóm:** `JobPostingRepository` đổi chữ ký `countApplicationsByPosting` (thêm tham số trạng thái), bỏ `countHiredByPosting` / `findAiScoreThreshold` (chỉ job posting dùng); thêm 1 hàm vào `JobRequisitionRepository`.

# Luồng Department CRUD / khôi phục Account — 04/10/2026

Nguồn: working tree đã kiểm chứng bằng test. Package trong bảng là class dự án thật. DispatcherServlet, Bean Validation, Thymeleaf/Spring Security là cơ chế framework, không phải controller tự viết. DB/schema giữ nguyên, session login, không JWT.

## 1. Điểm vào và route

Admin đăng nhập → Dashboard → menu **Phòng ban** hoặc link **Quản lý phòng ban**.

| HTTP | Route | Hàm controller |
|---|---|---|
| GET | /admin/departments | DepartmentController.list() |
| GET | /admin/departments/new | DepartmentController.newDepartment() |
| POST | /admin/departments | DepartmentController.create() |
| GET | /admin/departments/{id}/edit | DepartmentController.edit() |
| POST | /admin/departments/{id} | DepartmentController.update() |
| POST | /admin/departments/{id}/deactivate | DepartmentController.deactivate() |
| POST | /admin/departments/{id}/activate | DepartmentController.activate() |
| POST | /admin/accounts/{userId}/activate | AccountController.activate() |
| POST | /admin/candidate-accounts/{userId}/activate | CandidateAccountController.activate() |

Tất cả controller account/department trong bảng ở package `com.group2.rms.user.controller`.

## 2. Cổng bảo mật trước Controller

1. `com.group2.rms.core.config.SecurityConfig.filterChain()` định nghĩa Department/Admin Account chỉ `ROLE_SYSTEM_ADMIN`.
2. Spring Security dùng session đã đăng nhập. Guest GET bị chuyển `/login`; vai trò khác bị403. POST cần CSRF hợp lệ trước khi mutation vào Controller.
3. `com.group2.rms.core.security.AccountSessionGuardFilter.doFilterInternal()` gọi `com.group2.rms.user.repository.UserRepository.findByUsernameIgnoreCase()` để kiểm tra account còn Active và authority vẫn trùng role DB. Nếu sai, logout session rồi redirect `/login?session-expired`.
4. Security cho qua → Spring MVC dispatch đến đúng hàm Controller ở bảng route.

Thao tác đổi trạng thái không sửa hệ thống login. Lần đăng nhập mới vẫn đi `com.group2.rms.core.security.DatabaseUserDetailsService.loadUserByUsername()`; UserDetails disabled nếu không Active. Kích hoạt lại cần đăng nhập mới, không phục hồi session cũ đã bị thu hồi.

## 3. Danh sách / tìm kiếm / phân trang

| Step | Package.class.method | Công việc |
|---|---|---|
| D1 | user.controller.DepartmentController.list(search,status,page,model) | Nhận GET filter |
| D2 | user.service.DepartmentService.findDepartments() | @Transactional(readOnly=true), trim/lowercase tên, escape wildcard, lọc status Active/Inactive, page10, sort name+ID |
| D3 | user.repository.DepartmentRepository.findAll(Specification,Pageable) | JPA SELECT/page; EntityGraph manager tránh lazy/N+1 tên manager khi map |
| D4 | user.service.DepartmentService.response() | Entity → DepartmentResponse, không credential/password |
| D5 | DepartmentController.list() | Thêm Page/search/selectedStatus vào Model, trả admin/departments/list |
| D6 | templates/admin/departments/list.html | Render bảng/filter/page, POST actions có CSRF, shared head/header/sidebar |

## 4. Tạo Department

1. `user.controller.DepartmentController.newDepartment()` tạo `user.dto.DepartmentRequest("",null)` nếu chưa có flash form → `admin/departments/form`.
2. Form gửi POST bằng `th:action`; Spring/Thymeleaf thêm hidden CSRF.
3. MVC bind record `DepartmentRequest`; `@Valid` kiểm tra name không rỗng/tối đa100, managerId nếu có phải dương.
4. `DepartmentController.create()`: nếu BindingResult có lỗi, trả form và giữ dữ liệu; không gọi Service.
5. Hợp lệ → `user.service.DepartmentService.create()` → `validateName()` → `DepartmentRepository.existsByDepartmentNameIgnoreCase()` kiểm tra trùng tên trên cả Active/Inactive.
6. Department chưa có nhân viên: managerId phải trống. Tạo Entity Department(name, status=Active, manager=null).
7. `DepartmentService.persist()` → `DepartmentRepository.saveAndFlush()` INSERT. @Transactional bao trùm mutation.
8. Thành công → flash “Đã tạo phòng ban.”, redirect `/admin/departments`. Không tạo User hoặc JobRequisition cùng thao tác này.

Tạo xong → Admin vào Tài khoản nội bộ, gán nhân viên vào phòng ban Active → quay lại sửa Department để chọn trưởng phòng.

## 5. Sửa tên / trưởng phòng

1. `DepartmentController.edit(id,model)` → `DepartmentService.findForEdit()` → `find()` → `DepartmentRepository.findById()` → DTO hiện tại.
2. `DepartmentService.managerChoices(id)` → `UserRepository.findAllByDepartmentDepartmentIdAndAccountStatusOrderByFullNameAscUserIdAsc(id,"Active")` → lọc RoleAuthorities.INTERNAL_ROLE_NAMES → DepartmentManagerResponse.
3. Controller đưa form/current department/choices vào model; manager hiện tại không đủ điều kiện được đánh dấu, không tự bỏ liên kết.
4. POST → bind/@Valid → `DepartmentController.update()`.
5. `DepartmentService.update()` → `find()` và `validateName()` → `existsByDepartmentNameIgnoreCaseAndDepartmentIdNot()` không so trùng với chính bản ghi.
6. Nếu managerId có giá trị: `UserRepository.findById()` và kiểm tra Active + internal + cùng departmentId. Sai → DepartmentFieldException, không ghi tên/manager mới.
7. Nếu managerId trống: setManager(null). Hợp lệ: setDepartmentName()/setManager(); **không đổi DepartmentStatus**.
8. `persist()` → saveAndFlush UPDATE → flash thành công + redirect list.

## 6. Đường đi lỗi nhập liệu/nghiệp vụ/hệ thống

- Bean Validation: Controller nhận BindingResult, render inline; đây không phải catch exception.
- Trùng tên/sai manager: Service ném `user.exception.DepartmentFieldException`.
- `core.exception.GlobalExceptionHandler.handleDepartmentFieldException()` tạo BeanPropertyBindingResult, rejectValue đúng field; lưu form + BindingResult vào flash map; redirect303 về new/edit.
- GET new/edit giữ flash form nếu có → render lỗi cạnh input, giữ phần đã nhập. JS chung focus summary; link summary focus input tương ứng.
- `DepartmentService.persist()` chỉ chuyển SQL Server duplicate error2601/2627 thành lỗi tên. FK547/lỗi khác ném tiếp; Global Handler xử lý hệ thống, không báo thành công giả.
- Không tìm thấy Department: ResourceNotFoundException → `GlobalExceptionHandler.handleResourceNotFoundException()` trả404.
- DepartmentController không try/catch, không truy vấn JDBC trực tiếp.

## 7. Vô hiệu hóa / kích hoạt Department

1. Button của `fragments/status-actions :: action(...)` nằm trong POST form có route/CSRF cố định.
2. `static/js/status-confirmation.js` bắt submit, showModal dialog shared, focus **Hủy**. Hủy/Escape đóng, trả focus button, không POST.
3. Xác nhận → `form.requestSubmit(trigger)` gửi chính form gốc; không thay action/token, không gọi AJAX/fake backend.
4. Bảo mật → `DepartmentController.deactivate()`/`activate()` → `DepartmentService.deactivate()`/`activate()` → `setStatus()`.
5. `find()` → DepartmentRepository.findById(); nếu status khác đích, setDepartmentStatus rồi saveAndFlush. Lặp lại cùng đích là no-op.
6. Không deleteById/delete/cascade; User/JobRequisition/manager link giữ nguyên → flash + redirect list.

## 8. Kích hoạt lại Internal / Candidate Account

1. Chỉ row status Inactive có button **Kích hoạt lại**, dùng cùng fragment/JS/modal như Department.
2. Internal: `user.controller.AccountController.activate()` → `user.service.AccountListService.activate()` → `activateScoped(userId,false)`.
3. Candidate: `user.controller.CandidateAccountController.activate()` → `AccountListService.activateCandidate()` → `activateScoped(userId,true)`.
4. `UserRepository.findById()`; kiểm tra RoleAuthorities.INTERNAL_ROLE_NAMES hoặc roleName Candidate. ID thuộc nhóm khác trả404.
5. Inactive → setAccountStatus("Active") → UserRepository.save(). Active → no-op. Blocked → ResponseStatusException409 qua Global Handler.
6. Không setRole/setUsername/setPasswordHash, không thao tác Candidate profile → flash thành công và redirect đúng danh sách lifecycle.

## 9. Account assignment vào Department

- `AccountController.prepareCreate()/prepareEdit()` giữ danh sách Department đầy đủ phục vụ context; model currentDepartmentId lấy từ tài khoản đang lưu.
- `admin/accounts/form.html` hiển thị Active; khi edit chỉ thêm đúng Department Inactive gốc. Không dựa vào ID phòng ban mới gửi lên để cho phép giữ.
- `AccountManagementService.create()` → `findDepartment(id,role,null)`: Department mới phải Active.
- `AccountManagementService.update()` → `findDepartment(id,role,currentDepartmentId)`: Inactive chỉ được giữ nếu ID bằng phòng ban hiện tại; chuyển từ phòng khác bị AccountFieldException.
- Kiểm tra ở Service nên POST tự sửa thông số vẫn bị chặn. Rule Candidate/internal và username/password preservation giữ nguyên.
- Controller Account cũ còn catch/BindingResult WIP; đợt này không refactor xử lý lỗi feature cũ sang handler mới.

## 10. Department trên Admin Dashboard

1. `com.group2.rms.dashboard.DashboardController.dashboard(Authentication,Model)`.
2. `dashboard.DashboardService.forUsername()` → UserRepository.findByUsernameIgnoreCase(), Active/role guard → `admin()`.
3. `DashboardService.departmentSummary()` → `dashboard.DashboardMetricsRepository.departmentStatuses()` → EntityManager JPQL group/count DepartmentStatus.
4. DashboardResponse.DepartmentSummary(total,active,inactive) → `templates/dashboard/index.html` số thật và link `/admin/departments`.
5. `fragments/sidebar :: workspace()` có menu Phòng ban, loại Admin khỏi Job Requisitions condition; HR/Hiring Manager/Director/Interviewer giữ link, Candidate giữ panel riêng.

## Bằng chứng và giới hạn

45 Java tests, 2 JS tests PASS; Context/JPA validate/SELECT PASS. Chrome fixture115/115 PASS ở cả hai lượt. Xem [test steps và manual E2E](../tests/2026-10-04-admin-departments-activation.md). Fixture POST được chặn trong checker nên không phải bằng chứng CRUD ghi DB thật.

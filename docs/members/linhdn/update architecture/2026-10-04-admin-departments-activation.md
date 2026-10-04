# 04/10/2026 — Department CRUD và kích hoạt lại tài khoản

## Phạm vi và các quyết định đã chốt

- System Admin quản lý Department và truy cập từ menu/Dashboard.
- Xóa Department = đổi `DepartmentStatus` thành `Inactive`, giữ nguyên User và JobRequisition liên quan. Có thể kích hoạt lại.
- Trưởng phòng không bắt buộc; khi chọn phải là tài khoản nội bộ Active thuộc chính phòng ban.
- Chặn tạo/chuyển tài khoản vào Department Inactive. Tài khoản đang thuộc Department đó được giữ liên kết khi sửa.
- Internal/Candidate Account Inactive có thao tác kích hoạt lại; không đổi vai trò, username, password hash hoặc hồ sơ.
- Bỏ liên kết Job Requisitions khỏi workspace Admin. Không xóa module, route, Entity hoặc dữ liệu tuyển dụng.
- UI của các trang Admin được sửa dùng tiếng Việt và bộ giao diện Mộc hiện có. Giá trị role/nội dung DB giữ nguyên.
- Không JWT. Không thay đổi database/schema, seeder hoặc application.properties trong đợt này.

## Audit trước triển khai / Impact Analysis

| Phát hiện | Code chịu ảnh hưởng | Hướng xử lý |
|---|---|---|
| Schema đã có DepartmentStatus Active/Inactive, NOT NULL, default Active | Department thiếu mapping status | MODIFY: thêm đúng cột có sẵn, không thêm cột DB |
| DepartmentName unique, NVARCHAR(100) | Chưa có CRUD/DTO/validation | CREATE: form name tối đa 100, kiểm tra trùng kể cả Inactive, xử lý tranh chấp unique |
| ManagerId nullable FK User | Chưa có quy tắc chọn manager | CREATE theo xác nhận: Active/internal/cùng Department, cho phép bỏ chọn |
| User và JobRequisition tham chiếu Department | Xóa vật lý có thể mất lịch sử/lỗi FK | KEEP quan hệ; thao tác xóa chỉ cập nhật trạng thái |
| DepartmentRepository chỉ có JpaRepository | Chưa có search/page | MODIFY: Specification, page10, EntityGraph manager |
| AccountListService chỉ có deactivate | Không khôi phục Inactive qua danh sách | MODIFY: activate theo từng lifecycle, idempotent; Blocked không được activate trực tiếp |
| AccountManagementService chưa kiểm tra DepartmentStatus | Có thể tạo/chuyển vào Department Inactive | MODIFY theo xác nhận; giữ liên kết hiện tại được phép |
| CandidateAccountController thiếu @Controller sau khi chuyển package | Route Candidate Account không được đăng ký | MODIFY: bổ sung annotation, giữ package user/controller |
| Admin dùng menu Job Requisitions chung với vai trò khác | Sai phạm vi Dashboard Admin mong muốn | MODIFY điều kiện hiển thị, giữ link cho vai trò nội bộ khác |
| Account list lặp modal deactivation ở hai template | Thêm activation dễ sinh thêm UI/JS lặp | CREATE fragment xác nhận/form dùng chung cho account và Department |

Không có file cần DELETE. Trước khi bỏ handler modal cũ đã tìm toàn `src/` và scripts; dependencies là hai list templates, JS test và scripts QA lịch sử. Test JS được cập nhật; scripts QA lịch sử có selector cũ, đợt này dùng checker mới ở dưới. Method repository mới không dùng đã được search rồi bỏ ngay trong đợt này.

## Các file backend sửa

Đường dẫn dưới đây tính từ root `SWP_RMS_Group-2/`.

| File | Sửa gì / vì sao |
|---|---|
| src/main/java/com/group2/rms/user/entity/Department.java | Mapping DepartmentStatus và default Java Active cho đúng schema |
| src/main/java/com/group2/rms/user/repository/DepartmentRepository.java | Specification + phân trang + kiểm tra tên trùng, fetch manager khi render list |
| src/main/java/com/group2/rms/user/repository/UserRepository.java | Query tài khoản Active thuộc Department để chọn trưởng phòng |
| src/main/java/com/group2/rms/user/service/AccountManagementService.java | Chặn gán mới sang Inactive, cho giữ Department cũ; không sửa password/username |
| src/main/java/com/group2/rms/user/service/AccountListService.java | activate()/activateCandidate(), kiểm tra lifecycle, chỉ phục hồi Inactive |
| src/main/java/com/group2/rms/user/controller/AccountController.java | POST activate, đưa Department gốc vào model để dropdown giữ đúng liên kết cũ, nhãn thành công tiếng Việt |
| src/main/java/com/group2/rms/user/controller/CandidateAccountController.java | @Controller, POST activateCandidate, thông báo tiếng Việt |
| src/main/java/com/group2/rms/core/config/SecurityConfig.java | Thêm /admin/departments/** vào nhóm SYSTEM_ADMIN, giữ session/CSRF/rules khác |
| src/main/java/com/group2/rms/core/exception/GlobalExceptionHandler.java | Handler DepartmentFieldException tạo field errors/flash và redirect303 về form, không render trang500 cho lỗi nhập liệu |
| src/main/java/com/group2/rms/dashboard/DashboardMetricsRepository.java | Group/count DepartmentStatus thật bằng JPQL |
| src/main/java/com/group2/rms/dashboard/DashboardResponse.java | DepartmentSummary riêng; giữ overload tương thích Candidate/vai trò khác |
| src/main/java/com/group2/rms/dashboard/DashboardService.java | Admin lấy số phòng ban thật, shortcut quản lý, copy tiếng Việt |

## Các file frontend sửa

| File | Sửa gì / vì sao |
|---|---|
| src/main/resources/templates/admin/accounts/list.html | POST form deactivate/activate riêng từng tài khoản, modal chung, nhãn tiếng Việt |
| src/main/resources/templates/admin/accounts/candidates.html | Có Activate cho Inactive thay vì No action available; không cho chuyển role; modal chung |
| src/main/resources/templates/admin/accounts/form.html | Chỉ hiển thị Active + Department Inactive gốc khi edit; copy tiếng Việt |
| src/main/resources/templates/dashboard/index.html | Summary phòng ban + link quản lý; Admin tiếng Việt, Candidate/HR behavior giữ nguyên |
| src/main/resources/templates/fragments/sidebar.html | Menu Phòng ban; ẩn Job Requisitions chỉ với Admin, nhãn Admin tiếng Việt |
| src/main/resources/templates/fragments/workspace-header.html | Admin identity, logout, skip link/menu tiếng Việt; giữ behavior shared |
| src/main/resources/templates/fragments/messages.html | Dùng cờ model vietnameseUi để hiển thị summary tiếng Việt cho form Admin mới sửa; trang khác giữ tiếng Anh |
| src/main/resources/static/css/account-list.css | Inline action forms, Department filter/table/manager; tái sử dụng tokens/layout cũ |
| src/main/resources/static/js/account-list.js | Chỉ giữ mobile filter disclosure; modal chuyển sang JS chung |
| src/main/resources/static/js/interface.js | Toggle password theo lang của trang, giữ tiếng Anh trên các trang tiếng Anh |

## File mới

- `src/main/java/com/group2/rms/user/controller/DepartmentController.java`: thin MVC controller, @Valid/BindingResult, không catch Service exception.
- `src/main/java/com/group2/rms/user/service/DepartmentService.java`: read transactions, create/update, manager policy, activate/deactivate, unique-race handling. Chỉ lỗi SQL Server 2601/2627 được đổi thành lỗi trùng tên; lỗi FK/hệ thống được ném tiếp.
- `src/main/java/com/group2/rms/user/dto/DepartmentRequest.java`: Java record, name/optional managerId; không field Description hoặc schema mới.
- `src/main/java/com/group2/rms/user/dto/DepartmentResponse.java`: DTO list/edit, không credential.
- `src/main/java/com/group2/rms/user/dto/DepartmentManagerResponse.java`: DTO các lựa chọn trưởng phòng.
- `src/main/java/com/group2/rms/user/exception/DepartmentFieldException.java`: field + form đã nhập + ID để Global Handler khôi phục đúng form.
- `src/main/resources/templates/admin/departments/list.html`, `form.html`: list/filter/page và create/edit, cùng head/header/sidebar/CSS.
- `src/main/resources/templates/fragments/status-actions.html`: form POST CSRF và native dialog dùng chung.
- `src/main/resources/static/js/status-confirmation.js`: chặn submit để xác nhận; hủy/Escape trả focus; confirm gửi form gốc một lần, giữ CSRF/action.

### Test / công cụ / tài liệu

- Mới: DepartmentServiceTests, AccountActivationDepartmentTests, AdminDepartmentDashboardTests, AdminDepartmentWebTests, DepartmentReadOnlyTests trong `src/test/java/com/group2/rms/` hoặc `service/`.
- Sửa `src/test/js/account-ui.test.cjs`: test modal chung thay handler deactivation cũ, kiểm tra cancel/no POST, confirm1 lần, route/CSRF giữ nguyên.
- Mới `docs/members/linhdn/scripts/admin-department-preview-server.py`, `check-admin-department-ui.mjs`: preview HTML MockMvc đã bỏ CSRF thật; kiểm tra Chrome fixture.
- Mới [technical flow](../flows/admin-departments-account-activation-flow.md), [test từng bước](../tests/2026-10-04-admin-departments-activation.md), file process này.
- Cập nhật indexes flows/tests và chỉ dẫn version của account-lifecycle-flow.
- Bằng chứng hình/JSON ở `docs/members/linhdn/tests/assets/2026-10-04-admin-departments/`.

## Trình tự thực hiện và kết quả

1. Đọc schema + Entity/Repository + controller/services/menu hiện tại; ghi các gap. Chốt xóa mềm, manager và Department Inactive với user.
2. Sửa mapping, thêm DTO/Service/Controller/handler + route protection; compile PASS.
3. Thêm account activation và Department assignment policy; giữ lifecycle/password/history.
4. Tạo UI Department, summary Dashboard, modal shared, sửa menu Admin. Không refactor các controller WIP không liên quan.
5. Lần test đầu: 43 cases, 1 ERROR do restub Mockito gọi lại mock đã thenThrow (không phải lỗi ứng dụng). Chuyển restub sang doThrow; test sửa PASS.
6. Bộ test chọn lọc cuối: **45/45 Java PASS**. **2/2 Node PASS**. Spring Context/JPA validate/SELECT thật PASS. Không có INSERT/UPDATE/DELETE thật trong automated checks.
7. Chrome round1: **115/115 PASS**, xem screenshot desktop/mobile; giữ editorial Mộc, bảng compact, không redesign. Polish chỉ nhãn còn tiếng Anh trong menu/lỗi form. Round2: **115/115 PASS**.

## Giới hạn / điểm tiếp tục

- Live HTTP → browser → CRUD ghi DB thật **chưa test**; làm theo tài liệu manual trên database phát triển riêng.
- Không chạy toàn bộ suite mọi feature; kết quả ở trên chỉ áp dụng bộ test đã liệt kê.
- `application.properties` hiện có ddl-auto=update từ working tree trước đợt này. Không sửa config; các test DB và hướng dẫn run manual dùng override validate/never để tránh đổi schema.
- Nếu manager hiện tại sau này bị vô hiệu hóa/chuyển phòng ban, form Department hiển thị không còn đủ điều kiện và yêu cầu chọn lại hoặc bỏ chọn. Không tự xóa ManagerId khi lifecycle User thay đổi; chưa có quy tắc đồng bộ tự động được duyệt.
- Chặn Department Inactive mới áp dụng cho account assignment đã xác nhận. Không tự sửa quy tắc tạo requisition của teammate.
- Bản dịch đợt này giới hạn Dashboard Admin/Department/account pages đã chạm; không tuyên bố toàn bộ RMS đã bản địa hóa.
- Không sửa DB file nào. Không xóa file/module nào. Không commit hoặc deploy.

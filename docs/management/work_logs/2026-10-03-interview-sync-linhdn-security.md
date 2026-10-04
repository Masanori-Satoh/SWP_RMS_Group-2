# Báo cáo Thay đổi: Đồng bộ Module Lịch phỏng vấn với Cơ chế Phân quyền & Bảo mật

- **Ngày thực hiện:** 2026-10-03
- **Người thực hiện:** Đức (Masanori-Satoh - Nhóm 2)
- **Nhánh (Branch):** `test/merge-dung-refactor`
- **Module phụ trách:** `interview`

---

## 1. Yêu cầu / Quyết định (Requirements & Decisions)

1. **Đồng bộ Cơ chế Phân quyền Nhân sự Nội bộ (`RoleAuthorities`):**
   - Sau khi merge nhánh `origin/feature/update-architure` (LinhDN), hệ thống đã bổ sung cấu hình xác thực Spring Security toàn diện và phân định rõ tài khoản nội bộ (`RoleAuthorities.INTERNAL_ROLE_NAMES`) với tài khoản ứng viên (`Candidate`).
   - Cập nhật phương thức `prepareFormModel` trong `InterviewSchedulingController` và logic xác thực trong `InterviewSchedulingServiceImpl` để lọc danh sách người phỏng vấn khả dụng (`availableInterviewers`):
     - Chỉ cho phép các tài khoản thuộc nhóm nhân sự nội bộ (`RoleAuthorities.INTERNAL_ROLE_NAMES`).
     - Nghiêm cấm gán tài khoản mang vai trò `HR` (Rule HR Isolation) và vai trò `Candidate`.
     - Chỉ chấp nhận các tài khoản có trạng thái hoạt động `Active`.

2. **Chặn quyền truy cập Route Quản lý Lịch đối với Ứng viên (Candidate Isolation):**
   - Bổ sung kiểm tra an toàn trong `listInterviews`: Nếu tài khoản đăng nhập mang vai trò `Candidate`, hệ thống lập tức ghi log cảnh báo và chuyển hướng an toàn về `/dashboard`, ngăn ngừa việc ứng viên truy cập vào giao diện quản trị lịch phỏng vấn nội bộ.

3. **Chuẩn hóa Tên Vai trò trong Giao diện (`Hiring Manager`):**
   - Khắc phục lỗi so khớp chuỗi vai trò trong `src/main/resources/templates/interview/list.html` từ `'HiringManager'` (viết liền) thành `'Hiring Manager'` (có dấu cách) để đồng bộ 100% với tên Role trong CSDL chuẩn (`[Role].RoleName`) và hệ thống `RoleAuthorities`.

4. **Tối ưu Form Đăng xuất và Bảo vệ CSRF:**
   - Dọn dẹp thẻ input `_csrf` ẩn thủ công trong form đăng xuất của `list.html`, tận dụng cơ chế sinh token CSRF tự động của Thymeleaf (`th:action="@{/logout}"`), ngăn ngừa lỗi trùng lặp tham số hoặc xung đột khi render.

5. **Tuân thủ Tuyệt đối Ranh giới Trách nhiệm (Lane Separation):**
   - Chỉ chỉnh sửa các file thuộc phạm vi phân hệ `interview` và bài kiểm thử unit test tương ứng.
   - Không can thiệp, không sửa đè vào các file thuộc nghiệp vụ của Linh (`auth/`, `admin/`, `CareerController`, `SecurityFlowTests`, `DashboardDatabaseTests`) và của Dũng (`GlobalExceptionHandler`).

---

## 2. Thay đổi Cơ sở dữ liệu (Database)

- **Cấu trúc bảng (Schema):** Không thay đổi.
- **Dữ liệu mẫu (Seeds):** Không thay đổi.

---

## 3. Mã nguồn & Tài liệu (Source Code & Docs)

### 3.1. Các file tạo mới:
- `src/test/java/com/group2/rms/interview/InterviewSchedulingServiceTests.java`: Bộ 5 bài kiểm thử đơn vị độc lập xác thực toàn diện các ràng buộc bảo mật và nghiệp vụ.
- `docs/management/work_logs/2026-10-03-interview-sync-linhdn-security.md`: Báo cáo chi tiết này.

### 3.2. Các file chỉnh sửa:
- `src/main/java/com/group2/rms/interview/controller/InterviewSchedulingController.java`:
  - Thêm kiểm tra điều hướng bảo vệ đối với vai trò `Candidate`.
  - Sử dụng `RoleAuthorities.INTERNAL_ROLE_NAMES` để chọn lọc danh sách Interviewer.
  - Sử dụng hằng số `RoleAuthorities.SYSTEM_ADMIN` trong `isHrUser`.
- `src/main/java/com/group2/rms/interview/service/InterviewSchedulingServiceImpl.java`:
  - Thắt chặt kiểm tra hội đồng phỏng vấn: chặn tài khoản `Candidate` và tài khoản không `Active`.
- `src/main/resources/templates/interview/list.html`:
  - Đổi điều kiện hiển thị menu Requisitions từ `HiringManager` sang `Hiring Manager`.
  - Tối ưu form `th:action="@{/logout}"`.
- `docs/management/WORK_LOG.md`: Thêm dòng nhật ký công việc theo quy trình.

---

## 4. Kiểm thử (Testing)

1. **Biên dịch toàn bộ dự án:**
   - Lệnh: `.\mvnw.cmd compiler:compile`
   - Kết quả: `BUILD SUCCESS` (100 files Java biên dịch thành công 0 lỗi).
2. **Kiểm thử tự động Module Lịch phỏng vấn:**
   - Lệnh: `.\mvnw.cmd test -Dtest=InterviewSchedulingServiceTests`
   - Kết quả: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0` — **100% Passed**.
     - `cannotAssignCandidateUserToInterviewPanel`: Passed.
     - `cannotAssignHrUserToInterviewPanel`: Passed.
     - `cannotAssignInactiveUserToInterviewPanel`: Passed.
     - `createScheduleSuccessWithValidInternalUser`: Passed.
     - `forwardOnlyStateTransitionEnforced` (GBR-01): Passed.

---

## 5. Vấn đề tồn đọng & Kế hoạch tiếp theo (Pending Issues)

- Tiếp tục triển khai màn hình và nghiệp vụ Đánh giá phỏng vấn (`InterviewEvaluation`) trong Iteration tiếp theo.

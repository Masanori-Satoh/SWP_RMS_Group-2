# Báo cáo tiến độ (Work Log)

Tài liệu này ghi nhận lại toàn bộ những chỉnh sửa, khắc phục lỗi (bug fixes) và định hướng công việc tiếp theo trong dự án.

## Đã hoàn thành (Done)

- **Sửa lỗi giao diện Dropdown (UI):** 
  - Khắc phục lỗi các nút *View, Update, Delete* trong Action menu của màn hình `/requisitions` bị cắt mất do css `overflow-x: auto;` của thẻ `.table-wrapper`. Đã cập nhật css thành `overflow: visible;` để dropdown (position: absolute) hiển thị vượt ra ngoài khung table một cách đầy đủ.
  - Xóa văn bản "Action" khỏi column header và chỉ giữ icon `...` trong danh sách Requisitions, đồng thời đổi tên cột `REQ_ID` thành `STT`.
  - Fix lỗi click vào dropdown không hoạt động bằng việc tối ưu hóa code Javascript xử lý `toggleDropdown` (Sử dụng `parentNode.querySelector` và thay `forEach` bằng vòng lặp `for` an toàn).

- **Sửa lỗi Mapping trong Controller:**
  - Khắc phục lỗi Server báo lỗi 500 (Ambiguous handler methods mapped for `'/dashboard'`). 
  - Lỗi xuất phát từ việc `AuthController` và `DashboardController` cùng map với endpoint `/dashboard`. Đã fix bằng cách gỡ map `/dashboard` ở `AuthController`.

- **Sửa cấu hình Security (Dependency):**
  - Bổ sung thư viện và import cấu hình bị thiếu như `PasswordEncoder` và `BCryptPasswordEncoder` ở file `SecurityConfig`. Gỡ bỏ bean trùng lặp gây lỗi.

- **Đồng bộ Sidebar cho toàn bộ các trang:**
  - Trang Requisitions (list, detail, form) vốn đã sử dụng `fragments/sidebar.html`.
  - Đã tích hợp thành công `fragments/sidebar.html` chung vào trang `/dashboard` và trang quản lý hệ thống (admin `/accounts`).
  - Sửa lại layout DOM của trang Dashboard từ cấu trúc `<header>` cũ sang cấu trúc bọc chuẩn `<div class="page-wrapper">` với `<aside>` (sidebar) và `<div class="main-content">`.
  - Fix lỗi layout CSS liên quan tới `.dashboard-main` gây thu nhỏ trang web bất hợp lý do kế thừa css cũ. 
  - Thêm thẻ `<link>` tải Google Fonts `Inter` vào dashboard và admin account form/list để đồng bộ font chữ hệ thống.

## Công việc hiện tại (In Progress)

- Xây dựng và hoàn thiện tài liệu kiểm thử cho module Requisitions (Unit, Integration, System Test). Tài liệu đã được viết ở `docs/TEST.md`.

## Định hướng tiếp theo (To-Do)

1. **Khắc phục lỗi Database (Database Connection Failure):**
   - Trước đó hệ thống bị lỗi *Cannot open database 'RMS_DB' requested by the login*. Hiện tại server đang chạy được nghĩa là đã kết nối, tuy nhiên nếu cấu trúc DB còn thay đổi, cần phải liên tục rà soát `application.properties` để chuẩn hóa (Environment Variables nếu đưa lên cloud).

2. **Cải tiến Authentication / Authorization:**
   - Hoàn thiện luồng đăng nhập, trỏ Spring Security vào DB.
   - Ẩn / hiện các chức năng trên Sidebar dựa trên phân quyền của user hiện tại (Sử dụng module SecurityContext).
   - Xóa `SYSTEM_USER_ID = 1` fix cứng (mock) ở Service và dùng context user để ghi `AuditLog`.

3. **Cải tiến tính năng Requisitions:**
   - Bổ sung validation logic ở Backend và Frontend để đảm bảo dữ liệu (như Max Salary phải lớn hơn Min Salary).
   - Hoàn thiện nghiệp vụ Duyệt (Approve) và Từ chối (Reject) Requisition của Role Director.

4. **Tối ưu Front-end:**
   - Tổ chức lại các file CSS. Có thể gộp chung biến `main.css` thành một module dùng chung (ví dụ `variables.css`) để giảm kích thước.
   - Xử lý Reponsive UI (Mobile view) cho Sidebar (ẩn/hiện qua hamburger menu) vì giao diện hiện tại trên điện thoại di động sẽ có thể bị vỡ.

---

## Phân tích luồng Code (Code Flow) - Module Requisition

Dưới đây là tài liệu mô tả luồng hoạt động (Data Flow / Control Flow) của tính năng quản lý Yêu cầu tuyển dụng (Requisition), tuân theo kiến trúc Spring MVC.

### 1. Luồng xem danh sách (View List)
- **Controller (`RequisitionController.java`):** Lắng nghe request `GET /requisitions`.
  - Nhận tham số phân trang (`page`, `size`).
  - Gọi tới `requisitionService.getAllRequisitions(validPage, size)`.
- **Service (`RequisitionServiceImpl.java`):**
  - Chuyển `page` về index (0-based) và gọi `JobRequisitionRepository.findAllByOrderByCreatedAtDesc`.
  - Ánh xạ (Map) danh sách entity `JobRequisition` sang `RequisitionResponse`.
- **View (`list.html`):** Thymeleaf render bảng danh sách dựa trên `requisitions` nhận được từ Controller.

### 2. Luồng tạo mới (Create)
- **GET Request (`/requisitions/create`):**
  - Controller tạo sẵn một DTO rỗng (`RequisitionRequest`), đính kèm mặc định 1 `ScreeningCriteria` rỗng để giao diện hiển thị form nhập liệu. Trả về `form.html`.
- **POST Request (`/requisitions/create`):**
  - Người dùng submit form, dữ liệu được binding vào `RequisitionRequest`.
  - Controller gọi `requisitionService.createRequisition(dto, hiringManagerId)`.
  - **Service:**
    - Khởi tạo `JobRequisition` entity.
    - Set trạng thái dựa trên nút được bấm (`Draft` nếu lưu nháp, `Pending_Director` nếu nộp).
    - Map danh sách `ScreeningCriteria` từ DTO thành các entity tương ứng, gán quan hệ cha-con.
    - Lưu vào DB qua `requisitionRepo.save(req)` và `criteriaRepo.saveAll(criteriaList)`.
    - Ghi lại hành động qua `AuditLogRepository` (hàm `writeLog`).
  - Controller redirect về `GET /requisitions`.

### 3. Luồng cập nhật (Update)
- **GET Request (`/requisitions/edit/{id}`):**
  - Controller gọi Service lấy DTO qua `getRequestDtoById(id)`.
  - Trả về `form.html` (với cờ `isEdit = true`).
- **POST Request (`/requisitions/edit/{id}`):**
  - Nhận DTO từ form.
  - **Service:** Tìm Entity theo ID. Update các trường cơ bản.
  - Xử lý quan hệ One-to-Many (`ScreeningCriteria`): Xóa các tiêu chí cũ trong collection (thông qua hàm `.clear()`) và thêm mới các tiêu chí từ form (giúp Hibernate tự động xóa-chèn nhất quán).
  - Trạng thái: Cập nhật thành `Pending_Director` nếu user bấm submit, hoặc giữ nguyên trạng thái cũ nếu bấm "Save Changes".
  - Ghi log hành động UPDATE.
  - Redirect về danh sách.

### 4. Luồng Xóa (Delete)
- **Controller:** Bắt request `POST /requisitions/delete/{id}` (hoặc GET dự phòng).
- **Service (`deleteRequisition`):**
  - Ghi Audit Log hành động DELETE (cần ghi trước khi Entity bị xóa khỏi DB).
  - Tìm và xóa các lịch sử duyệt (`RequisitionApproval`) liên quan.
  - Clear collection `ScreeningCriteria` để trigger orphan removal.
  - Gọi `requisitionRepo.delete(req)`.
- **Kết quả:** Xóa vĩnh viễn khỏi Database và redirect về trang danh sách.

### 5. Luồng Xem Chi tiết (View Detail)
- **Controller:** `GET /requisitions/{id}`.
- **Service:** 
  - Lấy Entity theo ID.
  - Map toàn bộ thông tin chi tiết sang `RequisitionResponse`.
  - Load danh sách phê duyệt từ `RequisitionApprovalRepository`.
  - Load lịch sử log từ `AuditLogRepository` theo `entityId` và `entityName = "JobRequisition"`.
- **View (`detail.html`):** Render giao diện chi tiết, hiển thị thông tin chung, tiêu chí sàng lọc (criteria) và lịch sử hoạt động (Activity Log).


# Kiểm thử chức năng Job Requisition (Kế hoạch Test)

Tài liệu này định nghĩa các kịch bản kiểm thử (Test Cases) cho chức năng Quản lý Yêu cầu tuyển dụng (Job Requisition) trong hệ thống RMS, bao gồm Unit Test, Integration Test và System Test.

## 1. Unit Testing

### 1.1. `RequisitionServiceImpl` (Service Layer)

- **Test: Lấy danh sách Requisition phân trang**
  - **Mục tiêu:** Kiểm tra phương thức `getAllRequisitions(int page, int size)`.
  - **Dữ liệu giả lập (Mock):** `JobRequisitionRepository.findAllByOrderByCreatedAtDesc` trả về một `Page` chứa 2 entity.
  - **Kết quả mong đợi:** Trả về đối tượng `Page<RequisitionResponseDto>` với số lượng đúng bằng 2 và dữ liệu được map chính xác sang DTO.

- **Test: Lấy Requisition theo ID thành công**
  - **Mục tiêu:** Kiểm tra `getById(Integer id)`.
  - **Dữ liệu giả lập (Mock):** `JobRequisitionRepository.findById(id)` trả về một entity hợp lệ.
  - **Kết quả mong đợi:** DTO trả về chứa đầy đủ các thông tin (Bao gồm danh sách criteria, approval log và activity log).

- **Test: Lấy Requisition theo ID thất bại (Ném Exception)**
  - **Mục tiêu:** Kiểm tra hành vi ném lỗi của `getById(Integer id)` khi ID không tồn tại.
  - **Dữ liệu giả lập (Mock):** `JobRequisitionRepository.findById(id)` trả về `Optional.empty()`.
  - **Kết quả mong đợi:** Ném ra ngoại lệ `RuntimeException("Not find Requisition with ID: " + id)`.

- **Test: Tạo mới Requisition (Save as Draft)**
  - **Mục tiêu:** Kiểm tra logic lưu và gắn trạng thái "Draft".
  - **Dữ liệu đầu vào:** `RequisitionRequest` với thuộc tính `action = "draft"`.
  - **Dữ liệu giả lập:** Mock repository để bắt argument `JobRequisition` lưu vào.
  - **Kết quả mong đợi:** Thực thể `JobRequisition` được gọi hàm `save()` với trạng thái `"Draft"`. Activity Log được ghi là "saved as Draft".

- **Test: Tạo mới Requisition (Submit)**
  - **Mục tiêu:** Kiểm tra logic lưu và gắn trạng thái "Pending_Director".
  - **Dữ liệu đầu vào:** `RequisitionRequest` với thuộc tính `action = "submit"`.
  - **Kết quả mong đợi:** Thực thể được lưu với trạng thái `"Pending_Director"`. Activity Log được ghi.

- **Test: Cập nhật Requisition**
  - **Mục tiêu:** Đảm bảo `updateRequisition(id, dto)` có thể thay đổi các trường dữ liệu và collection `ScreeningCriteria` được clear rồi add lại.
  - **Kết quả mong đợi:** Entity được sửa đổi chính xác.

- **Test: Xóa Requisition**
  - **Mục tiêu:** Kiểm tra xóa Requisition, đảm bảo dọn dẹp các Criteria và Approval liên quan, đồng thời ghi log `DELETE`.
  - **Kết quả mong đợi:** Hàm `delete()` trên repo được gọi, log xóa được ghi chính xác.

### 1.2. `RequisitionController` (Web Layer)

- **Test: Truy cập màn hình danh sách (`GET /requisitions`)**
  - **Mục tiêu:** Đảm bảo Controller trả về view `requisitions/list` với đúng attribute `requisitions`.
  - **Kịch bản:** Gửi request GET tới `/requisitions`.
  - **Kết quả mong đợi:** `status = 200 OK`, `view name = "requisitions/list"`, `model` có chứa `requisitions` và `totalPages`.

- **Test: Gửi form tạo mới (`POST /requisitions/create`)**
  - **Mục tiêu:** Form gửi DTO lên controller và lưu thành công, sau đó redirect về trang list.
  - **Kịch bản:** Gửi request POST tới `/requisitions/create` với form data.
  - **Kết quả mong đợi:** Gọi tới service `createRequisition` 1 lần, `status = 302 Found`, header `Location = /requisitions`.

- **Test: Xóa bằng form (`POST /requisitions/delete/{id}`)**
  - **Mục tiêu:** Xóa hợp lệ thì trả về redirect.
  - **Kịch bản:** Gửi request POST.
  - **Kết quả mong đợi:** Gọi service `deleteRequisition(id)`, `status = 302 Found`.

---

## 2. Integration Testing

### 2.1. Tương tác với Database (`JobRequisitionRepository`)
- **Test: Lưu và truy vấn Job Requisition kèm theo ScreeningCriteria**
  - **Kịch bản:** Dùng H2 database hoặc Testcontainers. Khởi tạo một đối tượng Requisition, thêm các Criteria con. Gọi `save()`.
  - **Kiểm chứng:** Gọi `findById()`, đảm bảo entity trả về có chứa đúng danh sách Criteria nhờ vào cascade mappings (`@OneToMany`).

### 2.2. Luồng nghiệp vụ Create & Retrieve
- **Kịch bản:**
  1. Gửi request POST `/requisitions/create` mang theo data hợp lệ qua `MockMvc`.
  2. Gửi request GET `/requisitions` để kiểm tra bản ghi vừa tạo có xuất hiện không.
  3. Kiểm tra ActivityLogRepository xem có log tương ứng.
- **Kết quả mong đợi:** Toàn bộ luồng kết nối mượt mà từ Controller -> Service -> Repository -> Database.

---

## 3. System / End-to-End Testing (E2E)

### 3.1. Kịch bản E2E: Quản lý vòng đời Requisition
*(Có thể viết tự động hóa thông qua Selenium/Cypress)*

- **Bước 1: Đăng nhập**
  - Mở URL: `http://localhost:8088/`
  - Nhập thông tin đăng nhập của user (Admin hoặc HR).
  - Xác nhận truy cập được vào `/dashboard`.

- **Bước 2: Truy cập trang Requisitions**
  - Click vào menu "Job Requisitions" trên sidebar.
  - Trình duyệt chuyển tới `/requisitions`.
  - Bảng dữ liệu hiển thị (Các cột như STT, Position, Status...).

- **Bước 3: Tạo mới Requisition**
  - Click "Create New Requisition".
  - Điền form: Position title ("Java Dev"), Quantity (3), Department, etc.
  - Click "Save Draft".
  - Quay lại trang danh sách, kiểm tra có record "Java Dev" với trạng thái "Draft".

- **Bước 4: Xem chi tiết (Detail) & Cập nhật (Update)**
  - Mở action menu (nút `...`) ở cột cuối, chọn "Update".
  - Thay đổi số lượng từ 3 thành 5. Click "Submit to Director".
  - Trình duyệt điều hướng về list, trạng thái bây giờ là "Pending_Director".
  - Chọn "View", trong chi tiết kiểm tra thấy số lượng bằng 5, Activity log ghi lại cả thao tác Create và Update.

- **Bước 5: Xóa Requisition**
  - Trở lại danh sách, chọn "Delete" qua action menu.
  - Một Modal xác nhận hiển thị. Click "Yes, Delete".
  - Trang tải lại, bản ghi "Java Dev" không còn trong danh sách.


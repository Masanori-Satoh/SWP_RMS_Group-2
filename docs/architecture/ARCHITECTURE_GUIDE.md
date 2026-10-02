# Hướng dẫn Phát triển và Quy chuẩn Kiến trúc (RMS Project)

Tài liệu này ghi chú các thay đổi và quy chuẩn code theo kiến trúc **Package-by-Feature** đang áp dụng cho dự án, đồng thời là "Kim chỉ nam" cho cả Lập trình viên (Devs) và Trợ lý AI (AI Assistants).

Các quy chuẩn dưới đây **BẮT BUỘC ÁP DỤNG CHO TOÀN BỘ CODE MỚI**. Với các tính năng đang code dở (WIP) trước thời điểm tài liệu này ban hành, các bạn tác giả **tự xem lại và dọn dẹp sau**, ưu tiên giữ nguyên tiến độ hiện tại.

## 1. Quy chuẩn Kiến trúc Backend (`core/` vs `feature/`)

Để giải quyết vấn đề số lượng file quá lớn trong một module, chúng ta sử dụng kiến trúc **Package-by-Feature**.

Tuy nhiên, để tránh việc tạo ra quá nhiều thư mục con không cần thiết, chúng ta áp dụng quy tắc **"Phẳng mặc định, Phân nhánh khi phình to" (Flat by default, Nested when needed)**:

- **Với các tính năng NHỎ (< 10 files):** Mọi class (Controller, Service, Repository, Entity) đều nằm chung "phẳng" ngay trong package của tính năng đó.
- **Với các tính năng LỚN (>= 10 files):** Bắt buộc phân lớp bên trong (`controller`, `service`, `repository`, `entity`, `dto`, `exception`, `validator`).

### Cấu trúc thư mục ví dụ:
```text
src/main/java/com/group2/rms/
├── {feature_nho}/                <-- Tính năng nhỏ (VD: dashboard, admin)
│   ├── DashboardController.java  <-- Nằm phẳng ngay bên ngoài
│   ├── DashboardService.java
│   └── DashboardMetrics.java     (DTO)
├── {feature_lon}/                <-- Tính năng lớn (VD: requisition, user)
│   ├── controller/               
│   ├── service/                  
│   ├── repository/               
│   ├── entity/                   
│   └── dto/                      
├── core/                         <-- Chứa các thành phần dùng chung toàn hệ thống
│   ├── config/                   <-- Cấu hình Spring (Security, WebMvc,...)
│   ├── security/                 <-- Các class liên quan đến Security
│   ├── exception/                <-- Nơi dự kiến đặt GlobalExceptionHandler
│   └── base/                     <-- BaseEntity, Auditable, v.v.
└── RmsApplication.java
```

### Danh sách các Features (Module) hiện có:
1. **`auth`**: Đăng nhập, Quên mật khẩu, Reset mật khẩu.
2. **`user`**: Quản lý tài khoản, Hồ sơ, Phòng ban, Vai trò.
3. **`requisition`**: Yêu cầu tuyển dụng, Phê duyệt, Tiêu chí sàng lọc.
4. **`candidate`**: Hồ sơ ứng tuyển, Ứng viên, Chấm điểm AI.
5. **`interview`**: Lịch phỏng vấn, Đánh giá, Hội đồng.
6. **`offer`**: Đề xuất lương, Thương lượng.
7. **`dashboard`**: Thống kê, Báo cáo.
8. **`admin`**: System Config, Health Check, Audit Logs.
9. **`core`**: Base entity, exception chung, security, config.

## 2. Quy chuẩn Đặt tên (Naming Conventions)
- **Controller**: `{Feature}Controller` (ví dụ: `JobRequisitionController`). Không đặt là `RequisitionRequestController`.
- **DTO**: 
  - Đuôi là `Request` cho Input (VD: `CreateAccountRequest`). Không dùng chữ `Form` hay `Dto`.
  - Đuôi là `Response` cho Output (VD: `UserProfileResponse`). Không tạo sub-folder `request/` hay `response/`, tất cả bỏ vào `dto/`.
- **Entity**: Viết hoa chữ cái đầu, số ít (VD: `JobRequisition`, `Candidate`).

## 3. Tổ chức Frontend (`templates/` vs `static/`)
- **Quy chuẩn BẮT BUỘC cho code mới:** KHÔNG viết CSS/JS nội tuyến vào thẻ `<style>` hay `<script>` trong file HTML thuộc `templates/`.
- **Cách triển khai:** 
  - Style, mã màu chung phải đặt ở `static/css/global.css`.
  - Style riêng của từng trang phải tách ra `static/css/[feature].css` và gọi qua thẻ `<link>`.
- **Lưu ý code hiện tại (WIP):** Các tính năng đang thiết kế dở (như file `form.html`), tác giả tự xem lại và bóc tách ra sau.

## 4. Xử lý Lỗi và Xác thực (Exception & Validator)
- **Validator:** Bắt buộc dùng DTO kèm Annotation (VD: `@Valid`) để kiểm tra dữ liệu đầu vào.
- **Exception (Ép dùng Global Exception Handler cho code mới):**
  - **Với code mới:** Service ném ra Custom Exception (kế thừa `BaseBusinessException`). Controller **CẤM** sử dụng `try-catch`, hãy để lỗi trôi lên `GlobalExceptionHandler` ở tầng `core`.
  - **Lưu ý code hiện tại (WIP):** Các hàm Controller đang tự try-catch, tự dọn dẹp sau khi hệ thống Global Exception hoàn thiện.

## 5. Giao tiếp chéo giữa các Tính năng (Cross-Feature)
- **Quy tắc cho code mới:** XEM XÉT KỸ LƯỠNG khi gọi chéo:
  - Được phép: Tiêm (`@Autowired`) Service A vào Service B.
  - CẤM TỐI KỴ: Tạo vòng lặp phụ thuộc (Circular Dependency).
  - Khuyến khích: Nếu nghiệp vụ đan chéo quá 2 tính năng, xem xét tạo Orchestrator Feature.
- **Lưu ý code hiện tại (WIP):** Tự rà soát chiều gọi Service và sắp xếp lại sau.

---
> **📌 LỜI NHẮC DÀNH CHO AI ASSISTANT:**
> Khi viết tính năng MỚI, AI **PHẢI** tuân thủ: Đẩy lỗi về Global Exception (không tự try-catch), tách CSS/JS ra file static ngay từ đầu, và phân tích kỹ Circular Dependency. Với code cũ User đang thao tác, AI chỉ lưu ý "nhớ refactor sau" chứ không can thiệp đập đi xây lại làm chậm tiến độ.

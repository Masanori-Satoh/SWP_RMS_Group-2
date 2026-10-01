# Hướng dẫn Phát triển và Quy chuẩn Kiến trúc (RMS Project)

Tài liệu này ghi chú các thay đổi và quy chuẩn code theo kiến trúc **Package-by-Feature** đang áp dụng cho dự án.

## 1. Quy chuẩn Kiến trúc (Package-by-Feature with Internal Layers)

Để giải quyết vấn đề số lượng file quá lớn trong một module (gây khó khăn cho việc maintain), chúng ta sử dụng kiến trúc **Package-by-Feature**.

Tuy nhiên, để tránh việc tạo ra quá nhiều thư mục con không cần thiết, chúng ta áp dụng quy tắc **"Phẳng mặc định, Phân nhánh khi phình to" (Flat by default, Nested when needed)**:

- **Với các tính năng NHỎ (ít file):** Mọi class (Controller, Service, Repository, Entity) đều nằm chung "phẳng" ngay trong package của tính năng đó.
- **Với các tính năng LỚN (nhiều file, ví dụ `user`, `requisition`):** Mới tiến hành chia các package con (layers) bên trong.

### Cấu trúc thư mục ví dụ:
```text
src/main/java/com/group2/rms/
├── {feature_nho}/                <-- Tính năng nhỏ (VD: dashboard, admin)
│   ├── DashboardController.java  <-- Nằm phẳng ngay bên ngoài
│   ├── DashboardService.java
│   └── DashboardMetrics.java     (Entity)
├── {feature_lon}/                <-- Tính năng lớn (VD: requisition, user)
│   ├── controller/               
│   ├── service/                  
│   ├── repository/               
│   ├── entity/                   
│   └── dto/                      
├── core/                         <-- Chứa các thành phần dùng chung toàn hệ thống
│   ├── config/                   <-- Cấu hình Spring (Security, WebMvc,...)
│   ├── security/                 <-- Các class liên quan đến Security (Filters, UserDetails)
│   ├── exception/                <-- Global Exception Handlers, Custom Exceptions
│   └── base/                     <-- BaseEntity, Auditable, v.v.
└── RmsApplication.java
```

### Danh sách các Features (Module) hiện có:
1. **`auth`**: Đăng nhập, Quên mật khẩu, Reset mật khẩu.
2. **`user`**: Quản lý tài khoản (CRUD), Hồ sơ cá nhân (Profile), Phòng ban (Department), Vai trò (Role).
3. **`requisition`**: Yêu cầu tuyển dụng (Job Requisition), Phê duyệt (Approval), Tiêu chí sàng lọc (Screening Criteria), Đăng tuyển (Job Posting).
4. **`candidate`**: Hồ sơ ứng tuyển (Application), Ứng viên (Candidate), Chấm điểm AI (AI Screening).
5. **`interview`**: Lịch phỏng vấn (Interview Schedule), Đánh giá (Evaluation), Hội đồng (Panel).
6. **`offer`**: Đề xuất lương (Offer Proposal), Thương lượng (Negotiation).
7. **`dashboard`**: Thống kê, Báo cáo (Metrics, Analytics).
8. **`admin`**: System Config, Health Check, Monitoring, Audit Logs.
9. **`core`**: Base entity, exception chung, security, config.

## 2. Quy chuẩn Đặt tên (Naming Conventions)
- **Controller**: `{Feature}Controller` (ví dụ: `JobRequisitionController`). Không đặt là `RequisitionRequestController`.
- **DTO**: 
  - Đuôi là `Request` cho Input (VD: `CreateAccountRequest`, `RequisitionRequest`). Không dùng chữ `Form` hay `Dto`.
  - Đuôi là `Response` cho Output (VD: `UserProfileResponse`, `JobPostingDetailResponse`). Các thư mục `request/` và `response/` con không nên tạo, tất cả bỏ vào `dto/`.
- **Entity**: Viết hoa chữ cái đầu, số ít (VD: `JobRequisition`, `Candidate`).

---
> **Lưu ý:** Việc áp dụng cấu trúc này giúp codebase scale tốt hơn. Mỗi khi cần sửa tính năng "Requisition", bạn chỉ cần mở package `requisition` thay vì phải nhảy qua nhảy lại giữa các package `controller/`, `service/`, `entity/` ở thư mục gốc.


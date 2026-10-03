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
│   ├── exception/                <-- GlobalExceptionHandler bắt lỗi toàn cục
│   └── base/                     <-- BaseEntity, Auditable, v.v.
└── RmsApplication.java

> **Lưu ý Thư mục View chung:** Spring Boot tự động map lỗi hệ thống về thư mục `templates/error/`. Các view lỗi chung (như `404.html`, `500.html`) bắt buộc phải để ở `src/main/resources/templates/error/` để Global Exception Handler có thể "hạ cánh" an toàn mà không văng lỗi TemplateInputException.
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
- **DTO (Khuyến khích dùng Java Record):** 
  - Khuyến khích sử dụng cấu trúc `record` của Java 14+ cho DTO để tăng tính bất biến (immutability) và gọn gàng.
  - Đuôi là `Request` cho Input (VD: `CreateAccountRequest`). Không dùng chữ `Form` hay `Dto`.
  - Đuôi là `Response` cho Output (VD: `UserProfileResponse`). Không tạo sub-folder `request/` hay `response/`, tất cả bỏ vào `dto/`.
- **Entity**: Viết hoa chữ cái đầu, số ít (VD: `JobRequisition`, `Candidate`).

## 3. Tổ chức Frontend (`templates/` vs `static/`)
- **Quy chuẩn BẮT BUỘC cho code mới:** KHÔNG viết CSS/JS nội tuyến vào thẻ `<style>` hay `<script>` trong file HTML thuộc `templates/`.
- **Cách triển khai:** 
  - Style, mã màu chung, typography và layout gốc phải đặt ở `static/css/global.css`.
  - Logic JS dùng chung (như Toggle Header, Menu Dropdown, Notification) phải đặt ở `static/js/global.js`.
  - Style/JS riêng của từng trang phải tách ra `static/css/[feature].css` và gọi qua thẻ `<link>`/`<script>`.
  - Khi render text từ database có chứa ký tự `\n` (dummy data/text thô), nhớ format replace thành `<br/>` và dùng `th:utext` để HTML tự động xuống dòng an toàn.
- **Lưu ý code hiện tại (WIP):** Các tính năng đang thiết kế dở (như file `form.html`), tác giả tự xem lại và bóc tách ra sau.

## 4. Xử lý Lỗi và Xác thực (Exception & Validator)
- **Validator & Xử lý Lỗi Nhập liệu (Web Form / AJAX):**
  - Bắt buộc dùng DTO kèm Annotation (VD: `@Valid`) để kiểm tra dữ liệu đầu vào.
  - **Với các Form nhập liệu giao diện (Create/Edit):** Sử dụng `BindingResult` (`bindingResult.rejectValue(...)` hoặc trả về JSON status 400 kèm chi tiết lỗi từng trường qua AJAX) để **hiển thị thông báo lỗi inline trực tiếp trên form** và giữ nguyên dữ liệu người dùng đang nhập dở. **TUYỆT ĐỐI KHÔNG ném Exception văng ra trang 500 khi người dùng chỉ nhập sai dữ liệu Form.**
- **Exception Hệ thống & Nghiệp vụ không thể khôi phục (Ép dùng Global Exception Handler):**
  - **Với code mới:** Service ném ra Custom Exception (kế thừa `BaseBusinessException` cho lỗi nghiệp vụ hoặc `ResourceNotFoundException` cho 404). Controller **CẤM** sử dụng `try-catch` nuốt lỗi, hãy để lỗi trôi lên `GlobalExceptionHandler` ở tầng `core` để render các trang lỗi tương ứng (`404.html`, `403.html`, `500.html`).
  - **Lưu ý code hiện tại (WIP):** Các hàm Controller đang tự try-catch, tự dọn dẹp sau khi hệ thống Global Exception hoàn thiện.

## 5. Giao tiếp chéo giữa các Tính năng (Cross-Feature)
- **Quy tắc cho code mới:** XEM XÉT KỸ LƯỠNG khi gọi chéo:
  - Được phép: Tiêm (`@Autowired`) Service A vào Service B.
  - CẤM TỐI KỴ: Tạo vòng lặp phụ thuộc (Circular Dependency).
  - Khuyến khích: Nếu nghiệp vụ đan chéo quá 2 tính năng, xem xét tạo Orchestrator Feature.
- **Lưu ý code hiện tại (WIP):** Tự rà soát chiều gọi Service và sắp xếp lại sau.

## 6. Truy xuất Dữ liệu (Database & JPA)
- **Lazy Loading & Session:** Khi truy vấn các Entity có quan hệ `FetchType.LAZY` (như `@OneToMany`, `@ManyToOne`), nếu quá trình Mapping từ Entity sang DTO diễn ra ở tầng Service sau khi truy vấn kết thúc, session có thể đã đóng, dẫn đến lỗi `LazyInitializationException`.
- **Giải pháp BẮT BUỘC:** Phải gắn annotation `@Transactional(readOnly = true)` (từ Spring) lên các class Service hoặc method Service chỉ đọc (GET) để giữ session sống trong suốt vòng đời mapping dữ liệu.

---
> **📌 LỜI NHẮC DÀNH CHO AI ASSISTANT:**
> Khi viết tính năng MỚI, AI **PHẢI** tuân thủ: Dùng Java `record` cho DTO, đẩy lỗi về Global Exception (không tự try-catch), bọc `@Transactional(readOnly = true)` để chống lỗi Lazy Fetch, tách CSS/JS ra file static (`global.css/js`) ngay từ đầu, và tạo đúng file `404/500.html` trong `templates/error/`. Về code cũ, AI chỉ lưu ý "nhớ refactor sau" chứ không can thiệp đập đi xây lại làm chậm tiến độ.

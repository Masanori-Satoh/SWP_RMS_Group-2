# Hướng dẫn Phát triển và Quy chuẩn Kiến trúc (RMS Project)

Tài liệu này ghi chú các thay đổi và quy chuẩn code theo kiến trúc **Package-by-Feature** đang áp dụng cho dự án, đồng thời là "Kim chỉ nam" cho cả Lập trình viên (Devs) và Trợ lý AI (AI Assistants).

Các quy chuẩn dưới đây **BẮT BUỘC ÁP DỤNG CHO TOÀN BỘ CODE MỚI**. Với các tính năng đang code dở (WIP) trước thời điểm tài liệu này ban hành, các bạn tác giả **tự xem lại và dọn dẹp sau**, ưu tiên giữ nguyên tiến độ hiện tại.

---

## 1. Quy chuẩn Kiến trúc Backend (`core/` vs `feature/`)

Dự án áp dụng kiến trúc **Package-by-Feature** để đóng gói toàn diện mã nguồn theo từng nghiệp vụ độc lập, áp dụng quy tắc **"Phẳng mặc định, Phân nhánh khi phình to" (Flat by default, Nested when needed)**:

- **Với các tính năng NHỎ (< 10 files):** Mọi class (Controller, Service, Repository, Entity, DTO) đều nằm chung "phẳng" ngay trong package của tính năng đó (ví dụ: `dashboard`, `admin`, `career`).
- **Với các tính năng LỚN (>= 10 files):** Bắt buộc phân lớp bên trong (`controller`, `service`, `repository`, `entity`, `dto`, `exception`, `validator`).

### Cấu trúc thư mục chuẩn:
```text
src/main/java/com/group2/rms/
├── {feature_small}/                <-- Tính năng nhỏ (VD: dashboard, admin, career)
│   ├── DashboardController.java  <-- Nằm phẳng ngay bên ngoài
│   ├── DashboardService.java
│   └── DashboardMetrics.java     (DTO)
├── {feature_large}/                <-- Tính năng lớn (VD: requisition, user, interview, offer, candidate, auth)
│   ├── controller/               <-- Chứa Controller trả về View hoặc REST Controller
│   ├── service/                  <-- Interface và Implementation xử lý nghiệp vụ
│   ├── repository/               <-- Spring Data JPA Repositories
│   ├── entity/                   <-- JPA Entities ánh xạ Database
│   ├── dto/                      <-- Data Transfer Objects (Request/Response)
│   └── exception/                <-- Custom Exception riêng của feature (nếu có)
├── core/                         <-- Chứa các thành phần dùng chung toàn hệ thống
│   ├── config/                   <-- Cấu hình Spring (Security, WebMvc, Database,...)
│   ├── security/                 <-- UserDetails, Custom Authentication providers
│   ├── exception/                <-- BaseBusinessException, GlobalExceptionHandler bắt lỗi toàn cục
│   ├── dto/                      <-- ApiResponse<T> chuẩn hóa dữ liệu trả về
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
### Danh sách 10 Module (Features) chính thức của hệ thống:
1. **`career`**: Cổng thông tin tuyển dụng công khai cho ứng viên (Public Job Board), xem chi tiết tin tuyển dụng (Job Details) và nộp hồ sơ ứng tuyển trực tuyến.
2. **`auth`**: Đăng nhập, Đăng ký tài khoản ứng viên, Quên mật khẩu, Đặt lại mật khẩu.
3. **`user`**: Quản lý tài khoản nội bộ (Admin Accounts), Quản lý tài khoản ứng viên, Hồ sơ cá nhân (My Profile), Đổi mật khẩu, Phòng ban (Departments), Vai trò (Roles).
4. **`requisition`**: Yêu cầu tuyển dụng, Quy trình phê duyệt yêu cầu, Tiêu chí sàng lọc ứng viên.
5. **`candidate`**: Quản lý ứng viên nội bộ, Hồ sơ ứng tuyển (Applications), Đánh giá và chấm điểm AI Screening.
6. **`interview`**: Lập lịch phỏng vấn, Hội đồng phỏng vấn (Interview Panel), Đánh giá kết quả phỏng vấn.
7. **`offer`**: Đề xuất lương (Offer Proposal), Phê duyệt đề xuất, Đàm phán hợp đồng, Lịch sử offer.
8. **`dashboard`**: Thống kê số liệu tuyển dụng, biểu đồ hiệu suất, báo cáo quản trị.
9. **`admin`**: Cấu hình hệ thống (System Config), Kiểm tra sức khỏe dịch vụ (Health Check), Giám sát API (API Monitoring), Nhật ký hệ thống (Audit Logs).
10. **`core`**: Base entity, exception toàn cục, ApiResponse dùng chung, Spring Security, cấu hình toàn hệ thống.

---

## 2. Quy chuẩn Tầng Core & Xử lý Ngoại lệ (Core Exception & Response)

### A. Chuẩn hóa Response với `ApiResponse<T>`:
- Mọi API JSON hoặc thao tác AJAX nên trả về đối tượng `com.group2.rms.core.dto.ApiResponse<T>`:
  - `success`: Boolean xác định trạng thái thành công hay thất bại.
  - `message`: Thông báo tóm tắt cho người dùng / frontend.
  - `data`: Payload dữ liệu chính (hoặc null nếu chỉ thông báo).
  - `errors`: Danh sách các lỗi chi tiết (nếu có validation errors).
  - `timestamp`: Thời điểm phản hồi.

### B. Cơ chế Global Exception Handler đa kênh:
- Tất cả lỗi nghiệp vụ trong Service phải kế thừa từ `BaseBusinessException`.
- Controller **CẤM** sử dụng `try-catch` tùy tiện để nuốt lỗi hoặc bung stacktrace ra ngoài. Hãy để ngoại lệ trôi lên `GlobalExceptionHandler`.
- `GlobalExceptionHandler` tự động phân luồng thông minh:
  - **Với Request AJAX/API** (có header `X-Requested-With: XMLHttpRequest` hoặc `Accept: application/json`): Trả về JSON chuẩn `ApiResponse` với mã HTTP tương ứng (400, 401, 403, 404, 409, 500).
  - **Với Request Trình duyệt thông thường**: Điều hướng an toàn về view lỗi chuẩn tại `src/main/resources/templates/error/`:
    - `404.html`: Không tìm thấy tài nguyên.
    - `403.html`: Không đủ quyền hạn truy cập (Access Denied).
    - `500.html`: Lỗi hệ thống nội bộ.

---

## 3. Quy chuẩn Đặt tên (Naming Conventions)
- **Controller**: `{Feature}Controller` (ví dụ: `JobRequisitionController`, `UserProfileController`, `InterviewSchedulingController`). Không đặt tên kiểu `RequisitionRequestController`.
- **DTO (Khuyến khích dùng Java Record):**
  - Khuyến khích sử dụng cấu trúc `record` của Java 14+ cho DTO để tăng tính bất biến (immutability) và gọn gàng. Với class POJO truyền thống, phải có validation annotations đầy đủ.
  - Đuôi là `Request` cho Input (VD: `UpdateProfileRequest`, `ChangePasswordRequest`). Không dùng chữ `Form` hay `Dto`.
  - Đuôi là `Response` cho Output (VD: `UserProfileResponse`, `OfferDetailResponse`).
  - **CẤM:** Không tạo sub-folder con `request/` hay `response/`, tất cả DTO đặt trực tiếp trong package `dto/` của feature.
- **Entity**: Viết hoa chữ cái đầu (PascalCase), danh từ số ít (VD: `JobRequisition`, `Candidate`, `InterviewSchedule`).

---

## 4. Tổ chức Frontend & Quy chuẩn UI/UX (`templates/` vs `static/`)

### A. Tách bạch CSS/JS:
- **QUY CHUẨN BẮT BUỘC:** KHÔNG viết CSS/JS nội tuyến vào thẻ `<style>` hay `<script>` trong file HTML thuộc `templates/`.
- Style dùng chung, bảng màu, typography chuẩn RMS đặt tại: `static/css/global.css`.
- Logic JS dùng chung (Toggle Header, Menu Dropdown, Toast notifications) đặt tại: `static/js/global.js`.
- Style/JS riêng của từng trang/feature tách ra `static/css/[feature].css` và `static/js/[feature].js` (VD: `profile.css`, `profile.js`, `offers.css`, `offers.js`).

### B. Form AJAX & Bảo vệ CSRF Token:
- Mọi form gửi qua AJAX (như Modal Đổi mật khẩu, Modal duyệt Offer) **BẮT BUỘC** phải gửi kèm CSRF Token:
  - Lấy token từ header meta tag: `document.querySelector('meta[name="_csrf"]')?.getAttribute('content')`.
  - Đính kèm vào request header: `'X-CSRF-TOKEN': token`.
- Nếu thiếu CSRF Token, Spring Security sẽ trả về lỗi `403 Forbidden` ngay lập tức.

### C. Cơ chế Dirty Checking & Modal Xác nhận:
- Với các màn hình chỉnh sửa hồ sơ/biểu mẫu quan trọng: Nút "Save" phải ở trạng thái disable ban đầu, chỉ kích hoạt khi người dùng thực sự thay đổi dữ liệu (Dirty state). Bổ sung nút "Discard" để khôi phục trạng thái gốc.
- Các hành động cập nhật lớn hoặc xóa dữ liệu phải bật Modal xác nhận (`confirmModal`) trước khi gửi request thực tế.

### D. Ràng buộc Dữ liệu tiếng Việt (Unicode Validation):
- Khi validate họ tên tiếng Việt, **KHÔNG** dùng regex ASCII `[a-zA-Z ]*` vì sẽ từ chối các ký tự có dấu tiếng Việt (à, á, ả, ã, ạ, đ,...).
- Bắt buộc dùng Unicode property escapes:
  ```regex
  ^[\p{L}][\p{L}\s.'-]*$
  ```

---

## 5. Phân quyền và Bảo mật (Security & Authorization)

Hệ thống phân chia 5 vai trò chính: `Candidate`, `Interviewer`, `Recruiter` (HR), `Hiring Manager` / `Director`, `System Admin`.
Quy tắc kiểm soát phân quyền trong `SecurityConfig`:
- **Công khai (`permitAll()`):** `/`, `/career/**`, `/auth/**`, `/css/**`, `/js/**`, `/images/**`.
- **Đã xác thực (`authenticated()`):** `/dashboard/**`, `/profile/**`.
- **Giới hạn nội bộ:** Tuyệt đối chặn vai trò `Candidate` truy cập vào các URL quản trị nội bộ (`/requisitions/**`, `/interviews/**`, `/offers/**`, `/admin/**`).
- **Phân quyền đặc thù:**
  - `/offers/**`: Chỉ dành cho `HR`, `Director`, `System Admin`.
  - `/admin/**`: Chỉ dành cho `System Admin`.

---

## 6. Giao tiếp chéo giữa các Tính năng (Cross-Feature)
- Được phép tiêm (`@Autowired`) Service của Feature A vào Service của Feature B nếu phục vụ luồng nghiệp vụ.
- **CẤM TUYỆT ĐỐI:** Tạo vòng lặp phụ thuộc (Circular Dependency) giữa các Service/Component.
- Không import chéo trực tiếp DTO nội bộ của feature khác nếu không thực sự cần thiết; ưu tiên dùng DTO chung từ `core/dto` hoặc trích xuất thông tin qua Service.

---

## 7. Truy xuất Dữ liệu (Database & JPA)
- **Chống lỗi `LazyInitializationException`:** Khi truy vấn Entity có quan hệ `FetchType.LAZY` (như `@OneToMany`, `@ManyToOne`) mà cần mapping sang DTO sau khi truy vấn, session có thể bị đóng ngoài Service. Bắt buộc gắn annotation `@Transactional(readOnly = true)` (từ Spring) lên class Service hoặc method Service đọc dữ liệu (GET).
- **Source of Truth:** File `database/schema/db.sql` là nguồn chân lý duy nhất của Database. Hibernate chỉ làm nhiệm vụ kiểm tra schema (`ddl-auto=validate`), không tự động sinh bảng.

---

> **📌 LỜI NHẮC DÀNH CHO AI ASSISTANT & DEVELOPERS:**
> Khi viết mã nguồn mới:
> 1. Dùng Java `record` hoặc POJO chuẩn cho DTO (Request/Response), đặt trực tiếp trong `dto/`.
> 2. Đẩy ngoại lệ về `GlobalExceptionHandler` (kế thừa `BaseBusinessException`), không tự try-catch bừa bãi trong Controller.
> 3. Bọc `@Transactional(readOnly = true)` cho các hàm đọc dữ liệu phức tạp.
> 4. Tách CSS/JS ra file static riêng, hỗ trợ CSRF Token cho mọi request AJAX.
> 5. Luôn validate họ tên hỗ trợ ký tự tiếng Việt có dấu (Unicode).

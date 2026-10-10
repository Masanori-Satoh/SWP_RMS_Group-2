# Hướng dẫn Phát triển và Quy chuẩn Kiến trúc (RMS Project)

Tài liệu này là "kim chỉ nam" cho Lập trình viên và Trợ lý AI: mô tả **cấu trúc thật của mã nguồn hiện tại** và **quy chuẩn bắt buộc cho code mới**. Chỗ nào code hiện tại còn lệch chuẩn được ghi riêng ở [mục 8](#8-hiện-trạng-những-điểm-code-cũ-lệch-chuẩn), không trộn vào quy tắc.

> **Stack:** Spring Boot 3.5.16 · Java 21 · Spring Security · Spring Data JPA (SQL Server) · Thymeleaf + Layout Dialect · Bean Validation · Spring Mail · Apache POI (xuất Excel) · Lombok.
>
> **Giao diện:** mục 6 chỉ là tổng quan; chi tiết nằm ở bộ tài liệu [`UI/`](UI/README.md).

---

## 1. Kiến trúc Backend (Package-by-Feature)

Mã nguồn đóng gói theo **nghiệp vụ**, không theo tầng. Quy tắc **"Phẳng mặc định, phân nhánh khi phình to"**:

- **Module nhỏ (< 10 file):** mọi class nằm phẳng trong package của module (hiện tại: `dashboard`, `notification`).
- **Module lớn (≥ 10 file):** bắt buộc chia sub-package `controller/`, `service/`, `repository/`, `entity/`, `dto/`, và khi cần `exception/`, `validator/`.

```text
src/main/java/com/group2/rms/
├── core/                  ← Dùng chung toàn hệ thống (KHÔNG chứa nghiệp vụ)
│   ├── base/              ← BaseEntity
│   ├── config/            ← SecurityConfig, SchemaNamingConfig
│   ├── dto/               ← ApiResponse<T>
│   ├── exception/         ← BaseBusinessException, ResourceNotFoundException, GlobalExceptionHandler, FormErrorViewHelper
│   ├── security/          ← DatabaseUserDetailsService, CurrentUserService, RoleAuthorities, AccountSessionGuardFilter
│   ├── validation/        ← @PasswordMatches + validator
│   └── web/               ← @ControllerAdvice cấp model cho mọi view: TopbarUserAdvice (topbarUser), ViewHelpersAdvice (pageLinks)
├── {module_nhỏ}/          ← dashboard, notification: Controller/Service/Repository/DTO nằm phẳng
├── {module_lớn}/          ← auth, user, requisition, interview, offer, admin, career, candidate
│   ├── controller/  service/  repository/  entity/  dto/
│   └── exception/  validator/   (khi cần)
└── RmsApplication.java
```

### 1.1. Bản đồ Module

| Module | Nghiệp vụ | Route chính (Controller) | View (`templates/`) |
|---|---|---|---|
| `career` | Cổng tuyển dụng công khai: landing, Jobs Board, chi tiết tin, nút ứng tuyển | `/`, `/jobs`, `/jobs/{id}`, `/jobs/{id}/apply` (`CareerPortalController`) | `candidate/landing`, `candidate/job-board`, `candidate/job-detail` |
| `auth` | Đăng nhập, đăng ký ứng viên (OTP email), quên/đặt lại mật khẩu (OTP) | `/login`, `/register/**`, `/forgot-password`, `/reset-password/**` | `auth/*`, `auth/email/*` |
| `user` | Tài khoản nội bộ, tài khoản ứng viên, phòng ban, hồ sơ cá nhân, đổi mật khẩu | `/admin/accounts/**`, `/admin/candidate-accounts/**`, `/admin/departments/**`, `/profile/**` | `admin/accounts/*`, `admin/departments/*`, `user/profile` |
| `requisition` | Yêu cầu tuyển dụng + phê duyệt + lịch sử; tin tuyển dụng nội bộ; tiêu chí sàng lọc | `/requisitions/**` (`RequisitionController`), `/internal/job-postings/**` (`InternalJobPostingController`) | `requisitions/*`, `job-postings/*` |
| `candidate` | **Chỉ tầng dữ liệu:** `Candidate`, `Application`, `ApplicationReview`, `AIScreeningResult` + repository. Chưa có controller | — | — |
| `interview` | Lập lịch phỏng vấn, hội đồng (panel), kết quả | `/interviews/**` (`InterviewSchedulingController`) | `interview/*` |
| `offer` | Đề xuất offer, phê duyệt, gửi, xuất Excel | `/offers/**` (`OfferController`, có cả endpoint JSON) | `offers/*` |
| `notification` | Thông báo trong ứng dụng | `/notifications/**` | `notifications/list` |
| `dashboard` | Bảng điều khiển theo vai trò (nội bộ + ứng viên) | `/dashboard` | `dashboard/index` (+ `dashboard/candidate` là fragment) |
| `admin` | Giám sát API, health check, audit log, system config | `/admin/api-monitoring/**` | `admin/api-monitoring/index` |
| `demo` | Controller thử kết nối DB/web. **Không phải nghiệp vụ** (xem mục 8) | `/test-db`, `/test-web` | `hello` |

### 1.2. Chiều phụ thuộc giữa các module (thực tế)

```text
career ──► requisition, user          interview ──► candidate, user
offer  ──► candidate, interview, user requisition ──► user, admin (AuditLog), notification
auth   ──► user                       dashboard ──► user, interview, admin
user   ──► candidate                  mọi module ──► core
```

`user` và `candidate` là module **nền** (nhiều module khác phụ thuộc vào): đổi entity ở đây phải rà các module phía trên.

---

## 2. Luồng Request: Form, AJAX, Validation, Exception

> Mục này là **chuẩn cho code mới**. Code cũ chưa theo chuẩn được liệt kê ở mục 8, không bắt chước.

### 2.1. Hai cách giao tiếp với giao diện

| | Form truyền thống (**mặc định**) | AJAX (`fetch` + JSON) |
|---|---|---|
| Dùng khi | Màn hình tạo/sửa/lọc thông thường | Thao tác nhỏ **cần ở lại trang**: form trong modal, nút bấm trên danh sách |
| Controller nhận | `@Valid @ModelAttribute("form") XxxRequest form, BindingResult errors` | `@Valid @RequestBody XxxRequest req` + `@ResponseBody` |
| Thành công | `return "redirect:/..."` (kèm flash message) | `ApiResponse(true, message, data)` |
| Lỗi | Render lại **đúng template form**, lỗi hiện cạnh ô, dữ liệu đã nhập được giữ | `ApiResponse(false, message)` + HTTP 400, JS tự hiện lỗi |
| Ví dụ trong code | `AccountController`, `DepartmentController`, `RequisitionController` | Đổi mật khẩu trong `profile.js`, thao tác offer trong `offers.js` |

- **Mỗi thao tác chỉ chọn một cách**: không viết cả endpoint form lẫn endpoint JSON cho cùng một việc.
- AJAX bắt buộc gửi header `X-CSRF-TOKEN` (mục 6.3).

### 2.2. Ba loại lỗi, mỗi loại một cách xử lý

| Loại lỗi | Ví dụ | Ai phát hiện | Người dùng thấy |
|---|---|---|---|
| **Nhập sai** | Bỏ trống, sai định dạng, mật khẩu nhập lại không khớp | Bean Validation trên DTO (mục 2.4) | Form + lỗi cạnh ô |
| **Vi phạm nghiệp vụ** | Email đã tồn tại, sai trạng thái duyệt, vượt ngân sách | Service ném **exception của module** | Form + thông báo (form) / `ApiResponse(false)` (AJAX) |
| **Không thể tiếp tục** | Không tìm thấy, không có quyền, lỗi hệ thống | Service ném exception chung / lỗi tự phát sinh | Trang `error/404`, `403`, `500` do `GlobalExceptionHandler` render |

### 2.3. Exception: kiểu nằm ở module, Controller bắt đúng kiểu, Global lo phần còn lại

**Service ném gì:**

| Tình huống | Ném |
|---|---|
| Không tìm thấy bản ghi | `core.exception.ResourceNotFoundException` |
| Không có quyền với một bản ghi cụ thể | `org.springframework.security.access.AccessDeniedException` |
| Vi phạm nghiệp vụ | `{module}/exception/{Ngữ cảnh}Exception extends BaseBusinessException`; cần chỉ ra ô lỗi thì thêm field `field` (mẫu: `AccountFieldException`) |

**Không** dùng cho lỗi nghiệp vụ: `IllegalArgumentException`, `IllegalStateException`, `EntityNotFoundException`, `ResponseStatusException` (Global không hiểu đúng các kiểu này, dễ ra trang 500).

**Controller xử lý thế nào:** chỉ `try-catch` **đúng exception của module** để render lại form hoặc trả JSON. Mọi lỗi khác để bay lên Global.

```java
@PostMapping
public String create(@Valid @ModelAttribute("form") CreateAccountRequest form, BindingResult errors, Model model) {
    if (!errors.hasErrors()) {
        try {
            accountService.create(form);
            return "redirect:/admin/accounts";
        } catch (AccountFieldException e) {                 // chỉ bắt đúng kiểu của module
            errors.rejectValue(e.getField(), "account.invalid", e.getMessage());
        }
    }
    return "admin/accounts/form";                           // render lại form, dữ liệu còn nguyên
}
```

Không `catch (Exception e)`, không nuốt lỗi rồi trả về như thành công.

**`GlobalExceptionHandler` (`core/exception/`)** chỉ dùng cho loại "không thể tiếp tục":

| Exception | Trang |
|---|---|
| `ResourceNotFoundException`, `NoResourceFoundException` | `error/404` |
| `AccessDeniedException` (ném từ controller/service) | `error/403` |
| `BaseBusinessException` không được Controller bắt | `error/500` với HTTP 400 |
| `Exception` (còn lại) | `error/500` + log |

- **Không thêm handler riêng của module vào Global.** Lỗi cần quay lại form thì bắt ở Controller như mẫu trên.
- Lỗi bị Spring Security chặn ở tầng filter (sai quyền theo URL, thiếu CSRF) không đi qua Global; Spring Boot tự render `templates/error/{status}.html`.

### 2.4. Validation: ba tầng

| Tầng | Dùng khi | Đặt ở đâu | Ví dụ |
|---|---|---|---|
| Annotation trên trường | Kiểm tra từng ô | Trên DTO: `@NotBlank`, `@Size`, `@Email`, `@Pattern` | Hầu hết `*Request` |
| Annotation tự viết cho cả class | So sánh nhiều ô với nhau | Dùng chung: `core/validation/`; riêng module: `{module}/validator/` | `@PasswordMatches` |
| Validator nghiệp vụ | Cần đọc DB hoặc quy tắc nghiệp vụ | `@Component` trong `{module}/validator/`, **Service** gọi, lỗi thì ném exception của module | `RequisitionValidator` |

- Lỗi của hai tầng đầu tự vào `BindingResult` (form) hoặc thành `MethodArgumentNotValidException` (JSON), không cần tự ném exception.
- Họ tên tiếng Việt: **không** dùng regex ASCII `[a-zA-Z ]*`; dùng `^[\p{L}][\p{L}\s.'-]*$`.

### 2.5. DTO: `record` hay `class`

- `*Response`: **`record`**.
- `*Request`: **`record`** nếu đơn giản; **`class`** (Lombok `@Getter @Setter @NoArgsConstructor`) khi cần tạo rỗng rồi gán giá trị mặc định cho form "tạo mới", hoặc form có danh sách lồng nhau.
- Cả hai đều dùng được cho form lẫn JSON.

### 2.6. `ApiResponse<T>`

Endpoint JSON trả `core.dto.ApiResponse<T>` gồm `success`, `message`, `data` (có builder và constructor `(success, message)`).

---

## 3. Quy chuẩn Đặt tên

| Loại | Quy tắc | Ví dụ có thật trong code |
|---|---|---|
| Controller | `{Nghiệp vụ}Controller` | `RequisitionController`, `UserProfileController`, `InterviewSchedulingController` |
| Service | Module lớn: interface `{X}Service` + `{X}ServiceImpl`; module nhỏ / service đơn giản: một class `{X}Service` | `OfferService`/`OfferServiceImpl`, `DepartmentService` |
| DTO | Ưu tiên `record`. Input `*Request`, output `*Response`. Không dùng `Form`/`Dto`. Đặt thẳng trong `dto/`, **không** tạo `request/`, `response/` | `CreateAccountRequest`, `OfferDetailResponse` |
| Entity | PascalCase, danh từ số ít | `JobRequisition`, `InterviewSchedule` |
| Exception | `{Ngữ cảnh}Exception extends BaseBusinessException` | `OfferValidationException` |

---

## 4. Truy xuất Dữ liệu (Database & JPA)

- **Nguồn chân lý của schema:** `database/schema/db.sql`. Script khác: `database/migrations/`, `database/seeds/`. Tài liệu ERD: `docs/database/model.md`.
- **Không dựa vào Hibernate để đổi schema.** Mọi thay đổi bảng/cột phải viết vào `db.sql` (và migration). Lưu ý cấu hình hiện tại là `ddl-auto=update` (mục 8).
- **`spring.jpa.open-in-view=false`** → session đóng khi ra khỏi Service. Vì vậy **bắt buộc** gắn `@Transactional(readOnly = true)` cho Service/method đọc dữ liệu và **map Entity → DTO ngay trong Service**; không trả Entity LAZY ra Controller/View (sẽ gặp `LazyInitializationException`).
- Method ghi dữ liệu: `@Transactional`.

---

## 5. Phân quyền và Bảo mật

**6 vai trò** (`RoleAuthorities.fromRoleName`): `Candidate`, `Interviewer`, `HR`, `Hiring Manager`, `Director`, `System Admin` → authority `ROLE_CANDIDATE`, `ROLE_INTERVIEWER`, `ROLE_HR`, `ROLE_HIRING_MANAGER`, `ROLE_DIRECTOR`, `ROLE_SYSTEM_ADMIN`.

Phân quyền URL nằm tập trung trong `core/config/SecurityConfig` (chưa dùng `@PreAuthorize`):

| URL | Quyền |
|---|---|
| Static, `/favicon.ico`, `/error`, `GET /`, `/fonts/**` | Công khai |
| `/login`, `/register/**`, `/forgot-password`, `/reset-password/**` | Công khai |
| `GET /jobs`, `/jobs/**`, `/public/jobs/**` | Công khai |
| `/jobs/*/apply/**` | `CANDIDATE` |
| `/admin/accounts`, `/admin/departments`, `/admin/candidate-accounts`, `/admin/api-monitoring`, `/admin/ai-configuration` (+ `/**`) | `SYSTEM_ADMIN` |
| `/interviews/new`, `/interviews/*/edit`, mọi thao tác ghi `/interviews/**` | `HR`, `SYSTEM_ADMIN` |
| `GET /interviews/**` | `HR`, `SYSTEM_ADMIN`, `DIRECTOR`, `HIRING_MANAGER`, `INTERVIEWER` |
| `GET /portal/interviews` | `CANDIDATE` (các method khác: chặn) |
| `/offers/**`, `/api/v1/hr/offers/**` | `CANDIDATE`, `HR`, `DIRECTOR`, `SYSTEM_ADMIN` |
| `/requisitions/**` | `HIRING_MANAGER`, `DIRECTOR`, `HR`, `SYSTEM_ADMIN` |
| `/internal/job-postings/**` | `HR`, `SYSTEM_ADMIN` |
| `/dashboard/**`, `/notifications/**`, mọi URL còn lại | Đã đăng nhập |

- Đăng nhập: form `/login` → `/dashboard`. Đăng xuất: `POST /logout` → `/login?logout`.
- **Thêm route mới = thêm rule vào `SecurityConfig`.** Không có rule thì route rơi vào "đã đăng nhập", tức mọi vai trò (kể cả Candidate) đều vào được.
- Người dùng hiện tại: dùng `core/security/CurrentUserService`, không tự query lại từ `Authentication` trong Controller.

---

## 6. Giao diện (tổng quan)

> Chi tiết: [`UI/README.md`](UI/README.md) → `UI_RULES.md` (luật), `UI_CATALOG.md` (tra cứu), `UI_NEW_PAGE.md` (trang mới), `UI_MIGRATION.md` (migrate trang cũ), `UI_CSS_GUIDE.md` (CSS), `UI_LEGACY_CLEANUP.md` (file cũ).

### 6.1. Ba cấp kế thừa (Thymeleaf Layout Dialect)

```text
Cấp 1  layout/base.html       ← <head> chung (tokens, global, components, CSRF meta), interface.js
Cấp 2  layout/internal.html   ← sidebar theo role + topbar nội bộ + <main>
       layout/public.html     ← topbar công khai + footer
       layout/auth.html       ← dự phòng cho trang xác thực/lỗi
Cấp 3  {feature}/*.html       ← chỉ chứa nội dung: <html layout:decorate="~{layout/internal}"> … layout:fragment="content"
```

- **Sidebar tách file theo role** (`fragments/layout/sidebars/{admin|candidate|interviewer|recruiter}.html`), `sidebar-shell` tự chọn theo role. **Không** gom mọi menu vào một sidebar rồi rẽ nhánh bằng `th:if`.
- Controller chỉ đặt `model.addAttribute("activeMenu", "...")`; thông tin người dùng ở topbar do `TopbarUserAdvice` cấp sẵn.
- `layout:decorate` **chỉ** dùng để kế thừa layout; `th:replace` **chỉ** dùng để nhúng fragment.

### 6.2. Fragment: Khung vs Nội dung
- `fragments/layout/`: mảnh của **khung** (topbar, sidebar, góc người dùng). Chỉ layout cấp 2 gọi.
- `fragments/ui/`: linh kiện **nội dung** (`cards`, `dialogs`, `pagination`...). Trang cấp 3 gọi.
- **Rule of 3:** chỉ tách fragment mới khi một khối markup **có cấu trúc và tham số** lặp lại **≥ 3 nơi**. Không tách các `div` bọc đơn thuần.

### 6.3. Quy tắc bắt buộc (tóm tắt)
- Không `style="..."`, không `<style>`/`<script>` nội tuyến, không mã màu hex: dùng token `var(--...)` và file trong `static/`.
- Tra `UI_CATALOG.md` trước khi viết CSS mới. Nút: chữ nhật bo 8px (`var(--radius-md)`).
- Không đổi tên/dời template, không đổi biến backend (`${...}`, `th:field`, `name`, `id`).
- Mọi request AJAX gửi kèm CSRF: đọc `meta[name="_csrf"]` → header `X-CSRF-TOKEN` (thiếu sẽ bị 403).
- Form chỉnh sửa quan trọng: nút Lưu chỉ bật khi có thay đổi (dirty check), có nút Hủy; thao tác xóa/cập nhật lớn phải qua hộp thoại xác nhận.
- Text từ DB có `\n`: thay bằng `<br/>` ở Service rồi render bằng `th:utext`.

---

## 7. Giao tiếp chéo giữa các Module

- Được tiêm Service/Repository của module khác khi nghiệp vụ cần. Ưu tiên gọi **Service** của module kia hơn là dùng thẳng Repository của nó.
- **Cấm** phụ thuộc vòng (A → B → A).
- `core` **không được** phụ thuộc module nghiệp vụ (hiện còn vi phạm, xem mục 8).
- Không import DTO nội bộ của module khác nếu không thật cần. Nghiệp vụ đan chéo từ 3 module trở lên → cân nhắc một service điều phối riêng.

---

## 8. Hiện trạng: những điểm code cũ lệch chuẩn

Code cũ chạy được, **chưa cần sửa ngay**. Khi viết code mới thì theo các mục 1–7, không bắt chước bảng dưới. Ai sửa xong điểm nào thì xóa dòng đó.

| # | Hiện trạng | Hướng xử lý sau |
|---|---|---|
| 1 | `GlobalExceptionHandler` đang chứa handler riêng của `auth` (đăng ký, khôi phục mật khẩu), `user` (phòng ban), `admin` (health check) và import DTO của các module này; đây là file hay bị conflict | Chuyển dần về Controller/module theo mục 2.3 |
| 2 | `interview` ném `IllegalArgumentException`, `EntityNotFoundException` (không tìm thấy dữ liệu đang ra trang 500); `requisition` dùng `ResponseStatusException`; `offer` ném `BaseBusinessException` trần | Đổi sang kiểu ở mục 2.3 |
| 3 | `offer` có cả endpoint form lẫn JSON cho cùng thao tác tạo/sửa | Giữ một cách (mục 2.1) |
| 4 | `BaseBusinessException` không được bắt đang hiện bằng `error/500` | Thêm `error/400.html` |
| 5 | `core/exception/FormErrorViewHelper` (rỗng), `user/exception/AccountConflictException` không ai dùng | Xóa |
| 6 | Hai `NotificationService` trùng tên (`notification` và `offer.service`) | Gộp về module `notification` |
| 7 | `spring.jpa.hibernate.ddl-auto=update` | Chuyển `validate` khi `db.sql` khớp entity |
| 8 | Package `demo` (`/test-db`, `/test-web`, `hello.html`), `templates/careers/*` không còn dùng | Xóa (xem `UI/UI_LEGACY_CLEANUP.md`) |
| 9 | ~21 trang nội bộ còn dựng khung cũ (`interfaceHead` + `fragments/sidebar`) | Migrate theo `UI/UI_MIGRATION.md`; đến lúc đó **thêm menu phải sửa cả 2 nguồn sidebar** |
| 10 | Module `candidate` chưa có controller/service; bước ứng tuyển dừng ở `ITERATION_2_PENDING` | Iteration 2 |
| 11 | Còn 4 chỗ `@Autowired` field; test đặt tên lẫn `*Test`/`*Tests` | Code mới: `@RequiredArgsConstructor` + `private final`; test tên `*Tests` |

---

## 9. Pre-flight Checklist (trước khi mở PR, cho cả Dev và AI)

- [ ] Class mới đặt đúng module và đúng sub-package (mục 1); không thêm nghiệp vụ vào `core`.
- [ ] DTO là `record`/POJO có annotation validation, tên `*Request`/`*Response`, nằm thẳng trong `dto/`.
- [ ] Form hay AJAX: chọn đúng một cách cho mỗi thao tác (mục 2.1).
- [ ] Nhập sai → Bean Validation; vi phạm nghiệp vụ → exception của module, Controller bắt đúng kiểu đó; không tìm thấy/không có quyền → để Global xử lý (mục 2.2–2.4).
- [ ] **Không sửa `GlobalExceptionHandler` vì một module.**
- [ ] Service đọc có `@Transactional(readOnly = true)` và map sang DTO trong Service.
- [ ] Route mới đã có rule trong `SecurityConfig`.
- [ ] Thay đổi bảng/cột đã cập nhật `database/schema/db.sql` (+ `docs/database/model.md` nếu đổi thiết kế).
- [ ] Trang mới dùng `layout:decorate`, đặt `activeMenu`, không có CSS/JS nội tuyến, dùng linh kiện trong `UI_CATALOG.md`.
- [ ] AJAX có header `X-CSRF-TOKEN`; thao tác xóa có hộp thoại xác nhận.
- [ ] Validate họ tên hỗ trợ tiếng Việt có dấu.
- [ ] Có test cho Service/Controller mới (`src/test/java/com/group2/rms/...`) và `./mvnw test` pass.

# RULE CODE & TEST — RMS (HoangNH, cập nhật 2026-10-18)

> Gộp từ 2 file cũ `rule_code_nhh.md` + `rule_code_s.md` (đã xóa ngày 2026-10-18), sửa cho khớp code thật, thêm các rule rút ra khi làm sạch module Requisition.
> Dùng cho: viết code mới, sửa module cũ, review, và làm hướng dẫn cho AI.
> Mục tiêu: **người mới đọc hiểu được nghiệp vụ mà không cần hỏi người viết; GV yêu cầu sửa trực tiếp thì biết sửa ở đúng một chỗ.**

Mẫu tham chiếu trong code: module `requisition/` và `jobposting/` (đã làm theo đúng file này, 18/10/2026). Khi không chắc, mở file tương ứng trong hai module này ra xem. Còn lệch đã biết: `RequisitionAccess.actor()` tự đọc `SecurityContextHolder` thay vì `CurrentUserService` (mục 10).

---

## Mục lục

- **Phần A — Chung:** 0. Phạm vi & ưu tiên · 1. Quy trình làm việc · 2. Công nghệ & lệnh chạy
- **Phần B — Kiến trúc:** 3. Package & `core` · 4. Vai trò từng class trong module
- **Phần C — Viết code:** 5. Đặt tên · 6. Code dễ đọc · 7. Không hard-code · 8. Thời gian (`Clock`) · 9. Controller · 10. Service · 11. Phân quyền · 12. Validation · 13. Exception · 14. Trạng thái · 15. Database / JPA / N+1 · 16. Màn danh sách · 17. DTO · 18. Audit & thông báo · 19. Nhãn & message · 20. Giao diện
- **Phần D — Nghiệp vụ:** 21. Requisition · 22. Job Posting
- **Phần E — Test:** 23. Rule test
- **Phần F — Quy trình nhóm:** 24. Logging · 25. Git & commit · 26. Checklist push / review · 27. Cách làm sạch một module cũ · 28. Nguyên tắc quan trọng nhất
- **Phụ lục:** những điểm đã đổi so với 2 file cũ

---

# PHẦN A — CHUNG

## 0. Phạm vi & thứ tự ưu tiên

Áp dụng cho toàn bộ RMS (`auth`, `user`, `requisition`, `jobposting`, `candidate`, `interview`, `offer`, `notification`, `admin`, `career`, `core`).

Khi các yêu cầu đụng nhau, ưu tiên theo thứ tự:

1. Business rule đúng.
2. Security / phân quyền đúng.
3. Khớp SRS / Database / UI / wireframe đã được GV duyệt.
4. Code dễ đọc, dễ test, dễ sửa.
5. Hạn chế lặp code.
6. Tối ưu hiệu năng chỉ sau khi hành vi đã đúng (ngoại lệ: N+1 ở màn danh sách phải sửa ngay, mục 15.3).

Nguồn rule có hiệu lực cao hơn file này:

| File | Nội dung |
|---|---|
| `docs/architecture/ARCHITECTURE_GUIDE.md` (AG) | Backend chuẩn của cả nhóm. **§8 liệt kê code cũ lệch chuẩn — không bắt chước.** |
| `docs/architecture/UI/README.md` → `UI_RULES.md`, `UI_CATALOG.md`, `UI_NEW_PAGE.md`, `UI_CSS_GUIDE.md` | Giao diện. `UI_RULES.md` §2A: file chỉ Lead (Dũng LT) được sửa. |

File này cụ thể hóa AG cho cách nhóm đang code; chỗ nào khác AG thì làm theo AG và báo để sửa file này.

## 1. Quy trình làm việc

1. **Đọc trước khi sửa:** AG, UI rules, file này, và code mẫu trong `requisition/`.
2. **Màn mới hoặc đổi giao diện lớn:** wireframe → GV xác nhận → mới code. Wireframe chỉ cần: cấu trúc màn, thứ tự thông tin, điều hướng, ô nhập, cột bảng, thao tác.
3. **Chốt kế hoạch trước khi code:** liệt kê việc sẽ làm, chờ chủ module đồng ý rồi mới sửa (áp dụng cả khi nhờ AI).
4. **Code cũ lệch chuẩn:** không âm thầm viết lại. Đưa 2–3 phương án sửa (ưu / nhược + đoạn code mẫu) để chủ module chọn.
5. **Sửa một nghiệp vụ phải đi đủ các tầng:**

   ```text
   db.sql → Entity → DTO → Validator → Repository → Service → Controller → View → Test → Docs
   ```

   Không sửa một tầng rồi bỏ quên các tầng còn lại. Không tự thêm trường / chức năng ngoài SRS, DB, wireframe.
6. **Ghi lại:** mỗi đợt sửa thêm 1 dòng ngắn vào `docs/members/<tên>/checklist.md` (`[x]` khi đã làm và đã kiểm, `[ ]` khi còn treo). Sửa file dùng chung (`core`, `SecurityConfig`, file của người khác) thì ghi **"cần báo nhóm"**.

## 2. Công nghệ & lệnh chạy

Spring Boot 3.5, Java 21, Spring Security, Spring Data JPA trên **SQL Server**, Thymeleaf + Layout Dialect, Lombok, Spring Mail, Apache POI. Tài liệu và chữ trên giao diện bằng tiếng Việt.

```bash
./mvnw -o clean compile spring-boot:run                                # chạy app (Windows: .\mvnw.cmd)
./mvnw -o clean test                                                   # toàn bộ test
./mvnw -o test -Dtest='Requisition*'                                   # test một module (theo tiền tố tên class)
./mvnw -o test -Dtest='JobPosting*' '-DargLine=-Xmx384m -XX:+UseSerialGC' -Djacoco.skip=true   # máy thiếu RAM ("forked VM terminated")
./mvnw -o test -Dtest=RequisitionValidatorTests                        # một class
./mvnw -o test -Dtest='RequisitionServiceTests*#deleteRequisition_shouldReject_whenLaterRoundExists'  # một hàm (class có @Nested thì thêm *)
```

- Cấu hình cá nhân (mật khẩu DB, cổng, SMTP, secret) trong `application-local.properties` (gitignore), chép từ `application-local.properties.example` (không đổi tên file example).
- **Không có DB nhúng / H2.** Test `@SpringBootTest` nối SQL Server local thật; `@WebMvcTest` và unit test không cần DB.
- Thấy lỗi lạ kiểu "Unresolved compilation problems" → chạy với `clean` (VS Code cũng biên dịch vào `target/`).
- Kết quả test: `target/surefire-reports/*.txt`.

---

# PHẦN B — KIẾN TRÚC

## 3. Package & `core`

### 3.1. Package-by-feature

```text
com.group2.rms/
├── core/            ← dùng chung toàn hệ thống, KHÔNG chứa nghiệp vụ
├── dashboard/, notification/                  ← module nhỏ (< 10 file): để phẳng
└── auth/, user/, requisition/, jobposting/, candidate/, interview/, offer/, admin/, career/
    ├── controller/  service/  repository/  entity/  dto/
    └── validator/   exception/                ← khi cần
```

- Không tạo thêm sub-package ngoài danh sách trên (ví dụ `mapper/`, `util/` trong module): Mapper, Labels, Sort, Specifications, Access đặt trong `service/`.
- `user` và `candidate` là module nền: đổi entity ở đây phải rà các module phụ thuộc.
- Gọi chéo module (AG §7): ưu tiên gọi **Service** của module kia; được dùng Repository khi chỉ cần một câu truy vấn đơn giản. **Cấm phụ thuộc vòng** (A → B → A).
- Không import DTO nội bộ của module khác nếu không thật cần. Nghiệp vụ đan chéo từ 3 module trở lên → cân nhắc một service điều phối riêng.

### 3.2. Đưa gì vào `core`

| Thư mục | Chứa | Ví dụ |
|---|---|---|
| `core/config/` | Cấu hình, bean dùng chung | `SecurityConfig`, `ClockConfig` |
| `core/dto/` | DTO dùng ở ≥ 2 module, không mang nghiệp vụ | `ApiResponse<T>`, `FilterOption`, `FilterChip` |
| `core/util/` | Hàm tiện ích thuần (static, không truy cập DB) | `ListFilters`, `FilterLinks`, `FieldChanges` |
| `core/exception/` | Exception chung + `GlobalExceptionHandler` | `ResourceNotFoundException`, `BaseBusinessException` |
| `core/security/`, `core/validation/`, `core/base/` | Đăng nhập, validator dùng chung, `BaseEntity` | `CurrentUserService`, `@PasswordMatches` |

Quy tắc:

- **Đưa vào `core` khi** cùng một đoạn code xuất hiện ở **≥ 2 module** và **không chứa nghiệp vụ riêng** (không nhắc tới Requisition, JobPosting...). Nếu chỉ một module dùng thì để trong module.
- **`core/web/*` (`TopbarUserAdvice`, `ViewHelpersAdvice`, `PageLinks`) là vùng chỉ Lead được sửa** (UI_RULES §2A). Không thêm gì vào đó; cần dùng chung thì đặt ở `core/util/` rồi đề xuất với Lead.
- **Không thêm handler riêng của module vào `GlobalExceptionHandler`.**
- Thêm file vào `core` → ghi "cần báo nhóm" trong checklist và cập nhật cây thư mục ở AG §1.

## 4. Vai trò từng class trong một module

Mẫu đầy đủ (module Requisition). Module khác đặt tên tương tự: `{X}ListService`, `{X}Mapper`, `{X}Access`...

| Class | Làm | KHÔNG làm |
|---|---|---|
| `{X}Controller` | Nhận request, bind DTO, gọi service, chọn view / redirect / flash, bắt **đúng** exception của module để hiện lại form | Truy vấn DB, tự quyết định trạng thái, so sánh role |
| `{X}Service` + `{X}ServiceImpl` | Luồng nghiệp vụ một bản ghi: tạo, sửa, xóa, chuyển trạng thái, xem chi tiết; transaction | Lọc / phân trang màn danh sách; dựng DTO dài dòng |
| `{X}ListService` | Màn danh sách: phạm vi theo role, làm sạch bộ lọc, tìm kiếm, sắp xếp, phân trang, chip | Ghi dữ liệu |
| `{X}Mapper` (`@Component`) | Entity → DTO (form, dòng danh sách, chi tiết); hỏi `{X}Access` để gắn cờ quyền | **Truy vấn DB** (service tra trước rồi truyền vào) |
| `{X}Access` (`@Component`) | Luật "ai được làm gì": `canX(user, entity)` trả boolean, `requireX(...)` ném `AccessDeniedException`; hằng số role / trạng thái | Truy vấn DB (trừ `actor()` lấy người đang đăng nhập) |
| `{X}Validator` (`@Component`) | Làm sạch chuỗi + kiểm tra dữ liệu form; hằng số giới hạn (`POSITIONS_MAX`...) và danh mục (`EMPLOYMENT_TYPES`) | Kiểm tra quyền |
| `{X}AuditLogger` (`@Component`) | Snapshot trước/sau, ghi và đọc bảng `AuditLog` | Quyết định nghiệp vụ |
| `{X}Labels` (static) | Nơi **duy nhất** dịch giá trị DB → chữ tiếng Việt, tông màu badge, mã hiển thị | Logic nghiệp vụ |
| `{X}Specifications` (static) | Mỗi hàm = một mẩu điều kiện WHERE | Ghép điều kiện theo role (để ở ListService) |
| `{X}Sort` (enum) | Whitelist cách sắp xếp: `param`, `label`, `Sort` | Nhận tên cột từ client |
| `{X}Repository` | Truy cập dữ liệu, câu JPQL rõ nghĩa | Business rule |

Một business rule chỉ có **một nơi chịu trách nhiệm**: quyền ở `Access`, dữ liệu form ở `Validator`, chữ hiển thị ở `Labels`, chuyển trạng thái ở `ServiceImpl`.

---

# PHẦN C — VIẾT CODE

## 5. Đặt tên

| Loại | Quy tắc | Ví dụ |
|---|---|---|
| Biến | `camelCase`, đúng nghĩa nghiệp vụ | `departmentId`, `applicationDeadline`, `latestPosting` |
| Boolean | `is` / `has` / `can` / `should` | `isSubmitAction`, `hasLaterRound`, `canApprove` |
| Class | `PascalCase` | `RequisitionListService`, `OfferApprovalRequest` |
| Method | Động từ + nghiệp vụ | `createRequisition`, `requireActiveDepartment`, `findLatestByRequisitionIds` |
| Hằng số | `UPPER_SNAKE_CASE` | `COMMENT_CHAR_LIMIT`, `STATUS_PENDING_DIRECTOR` |
| DTO | `*Request` / `*Response`, đặt thẳng trong `dto/` | `RequisitionSearchRequest`, `RequisitionListResponse` |
| Exception | `{Ngữ cảnh}Exception extends BaseBusinessException` | `RequisitionValidationException` |
| Test class | `{ClassĐượcTest}Tests` (AG §8 #11) | `RequisitionServiceTests` |
| Test method | `hàm_shouldKếtQuả_whenĐiềuKiện` | `decide_shouldReject_whenRejectReasonIsBlank` |

Không dùng: `a`, `x`, `tmp`, `data1`, `obj`, `value1`, `process()`, `handle()`, `doIt()`, `updateData()`. Ngoại lệ: `i`, `j` cho vòng lặp đơn giản.

Thuật ngữ trên giao diện (không tự đổi):

```text
Job Requisition → Yêu cầu tuyển dụng       Job Posting → Tin tuyển dụng
Recruitment Round → Đợt tuyển dụng         Interview Round → Vòng phỏng vấn
Requester → Người tạo yêu cầu              Department → Phòng ban
Screening Criteria → Tiêu chí sàng lọc     Reason for Hiring → Lý do tuyển dụng
Application Deadline → Hạn nộp hồ sơ
```

"Đợt tuyển dụng" ≠ "vòng phỏng vấn". DB / Java vẫn là `RecruitmentRound` / `recruitmentRound`; muốn đổi tên kỹ thuật phải đổi cùng lúc DB → Entity → DTO → Repository → Service → View → Test.

## 6. Code dễ đọc (cho người mới)

- Mỗi method một việc; method service dài quá ~40 dòng thì tách hàm nhỏ có tên nghiệp vụ (`requireNoOtherDraft`, `assignRecruitmentRound`).
- **Guard clause** trước, luồng chính sau:

  ```java
  // Rule: Chỉ người tạo yêu cầu mới được rút lại yêu cầu đang chờ duyệt
  if (!access.canWithdraw(currentUser, jobRequisition)) {
      throw new AccessDeniedException("Chỉ người tạo yêu cầu mới có quyền rút lại yêu cầu đang chờ duyệt.");
  }
  jobRequisition.setApprovalStatus(RequisitionAccess.STATUS_DRAFT);
  ```

- **`if / else` rõ ràng thay cho ternary lồng nhau.** Ternary chỉ dùng cho một phép chọn đơn giản trên một dòng.

  ```java
  // Không
  .departmentName(r.getDepartment() == null ? "Chưa xác định" : r.getDepartment().getDepartmentName())
  // Nên
  String departmentName = "Chưa xác định";
  if (jobRequisition.getDepartment() != null) {
      departmentName = jobRequisition.getDepartment().getDepartmentName();
  }
  ```

- **Vòng `for` thay cho stream dài.** Stream ngắn một bước (`.map(...).toList()`) thì được; không lồng `collect(joining(...))` trong lời gọi hàm.
- `if` / `else` / `for` **luôn có ngoặc nhọn**, kể cả khi chỉ một dòng.
- Không wildcard import (`java.util.*`), không viết tên đầy đủ package trong thân code (`java.time.LocalDate`) — import ở đầu file.
- Hằng số và field khai báo ở **đầu class**, không rải giữa các method.
- Không code chết, import thừa, biến không dùng, code cũ để trong comment (Git đã lưu lịch sử).

**Comment:**

- Giải thích **vì sao / luật nghiệp vụ**, không lặp lại code. 1–2 dòng, đặt ngay trên logic, **tiếng Việt** (nhóm và GV đọc tiếng Việt).
- Tiền tố thống nhất: `// Rule:` (luật nghiệp vụ), `// Security:` (phân quyền), `// NOTE:`, `// TODO: <việc cụ thể>`, `// FIXME: <lỗi cụ thể>`.
- Đầu mỗi class có Javadoc ngắn: class lo việc gì, không lo việc gì, liên quan class nào.

```java
// Rule: Mã yêu cầu sinh từ RequisitionId, nên chỉ có sau khi DB cấp id ở lần lưu đầu tiên.
jobRequisition.setRequisitionCode(RequisitionLabels.requisitionCode(jobRequisition.getRequisitionId()));
```

Không: `// Set status to approved` trên dòng `setStatus(APPROVED)`; `// TODO` trống.

## 7. Không hard-code

Mọi giá trị cố định có ý nghĩa nghiệp vụ phải là hằng số / enum / cấu hình, đặt **đúng một chỗ**:

| Loại giá trị | Đặt ở | Ví dụ (Requisition) |
|---|---|---|
| Trạng thái lưu DB | `public static final` trong `{X}Access` (hoặc enum nếu module mới chưa dính DB cũ) | `RequisitionAccess.STATUS_PENDING_DIRECTOR`, `JobPostingAccess.STATUS_PUBLISHED` |
| Tên role | `{X}Access` | `RequisitionAccess.ROLE_HR` |
| Giá trị nút / action của form | `{X}Validator` | `ACTION_DRAFT`, `ACTION_SUBMIT`, `isSubmit(action)` |
| Giới hạn độ dài, min / max, số ngày | `{X}Validator` (`public` để controller, template và test dùng chung) | `TITLE_CHAR_LIMIT`, `POSITIONS_MAX`, `COMMENT_CHAR_LIMIT` |
| Danh mục lựa chọn | `{X}Validator` | `EMPLOYMENT_TYPES`, `GENDERS` |
| Chữ hiển thị, tông badge, định dạng mã | `{X}Labels` | `approvalStatus()`, `requisitionCode()` |
| Câu thông báo dùng ≥ 2 lần | hằng số `private static final` trong class dùng nó | `STORAGE_ERROR` trong controller |
| URL ngoài, email gửi, secret, API key, ngưỡng hệ thống | `application.properties` / bảng `SystemConfig` | `AI_SCORE_THRESHOLD` |

```java
// Không
if ("submit".equals(action)) ...
if (comment.length() > 1000) ...
// Nên
if (RequisitionValidator.isSubmit(action)) ...
if (comment.length() > RequisitionValidator.COMMENT_CHAR_LIMIT) ...
```

Giá trị trạng thái phải **đúng như trong DB** (`Draft`, `Pending_Director`, `Approved`, `Rejected`...), xem CHECK constraint trong `db.sql`.

## 8. Thời gian: dùng `Clock`, không gọi `now()` trực tiếp

- Bean `Clock` ở `core/config/ClockConfig`. Class cần "bây giờ" tiêm `private final Clock clock;` rồi gọi `LocalDate.now(clock)` / `LocalDateTime.now(clock)`.
- Lý do: test truyền `Clock.fixed(...)` → "hôm nay" cố định → test biên ngày (hôm qua / hôm nay / +1 năm) không lỗi khi chạy qua nửa đêm, và kiểm tra được giá trị chính xác (`submittedAt == NOW`).
- Rule ngày nằm ở **một chỗ** và các nơi khác hỏi chỗ đó: form lấy `min` / `max` bằng `validator.earliestStartDate()` / `latestStartDate()`, không tự tính `LocalDate.now().plusYears(1)` trong controller.
- Ngoại lệ được phép: `BaseEntity` / `AuditLog` `@PrePersist` (createdAt / updatedAt / timestamp mặc định). Requisition và Job Posting đã dùng `Clock` toàn bộ.

```java
private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
private final RequisitionValidator validator = new RequisitionValidator(CLOCK);   // test
```

## 9. Controller

Controller chỉ làm: **nhận request → bind DTO → gọi service → trả view / redirect / flash**.

- Mặc định **form POST truyền thống**; AJAX (`fetch` + `ApiResponse<T>`) chỉ cho thao tác nhỏ cần ở lại trang (modal, nút trên danh sách). Mỗi thao tác chọn **một** cách. AJAX bắt buộc header `X-CSRF-TOKEN`.
- **Chỉ `try-catch` đúng exception của module** để render lại form (AG §2.3). Không `catch (Exception e)`. Không bắt `ResourceNotFoundException` / `AccessDeniedException` — để Global trả 404 / 403.

  ```java
  @PostMapping("/create")
  public String create(@Valid @ModelAttribute("requisitionDto") RequisitionRequest request, BindingResult errors,
          Model model, RedirectAttributes flash) {
      if (!errors.hasErrors()) {
          try {
              Integer id = service.createRequisition(request);
              flash.addFlashAttribute("successMessage", savedMessage(request.getAction()));
              return "redirect:/requisitions/" + id;
          } catch (RequisitionValidationException e) {
              addErrors(errors, e);                      // lỗi hiện cạnh đúng ô
          } catch (DataIntegrityViolationException e) {
              errors.reject("storage", STORAGE_ERROR);
          }
      }
      return form(model, false, null);                   // render lại form, dữ liệu còn nguyên
  }
  ```

- Form nhận DTO bằng **`@Valid`** để lỗi Bean Validation (mục 12) tự vào `BindingResult` trước khi gọi service (AG §2.1).
- **Whitelist trường form** bằng `@InitBinder("tênDto")` + `setAllowedFields(...)`: trường hệ thống (`approvalStatus`, `createdBy`, `recruitmentRound`...) gửi lên bị bỏ qua.
- Mọi đường hiển thị form (tạo, sửa, sao chép, render lại khi lỗi) đi qua **một hàm** `form(model, ...)` để không quên thuộc tính nào.
- Trang dùng layout: đặt `activeMenu` (sáng menu) và `bodyClass` nếu trang có CSS riêng.
- Giá trị nút (`decision=approve|reject`) là hằng số trong controller; giá trị lạ → flash lỗi, không gọi service.
- Controller không: gọi Repository (trừ lấy danh sách lựa chọn đơn giản cho form), so sánh role, tự đổi trạng thái, tạo audit / notification.

## 10. Service

- Module lớn: interface `{X}Service` + `{X}ServiceImpl`. Màn danh sách tách `{X}ListService` (một class, không cần interface).
- `@Transactional` cho method ghi; `@Transactional(readOnly = true)` cho method đọc. `open-in-view=false` → **map Entity → DTO ngay trong service** (qua Mapper), không trả entity LAZY ra controller / view.
- Constructor injection: `@RequiredArgsConstructor` + `private final`. Không `@Autowired` field.
- Luồng chuẩn của một method ghi (đọc từ trên xuống là hiểu nghiệp vụ):

  ```text
  1. currentUser = access.actor()                  ← người dùng lấy từ phiên, không từ form (AG §5: nguồn chuẩn là CurrentUserService; RequisitionAccess.actor() hiện tự đọc SecurityContextHolder — nên chuyển sang gọi CurrentUserService)
  2. entity = findXById(id, lockForUpdate)        ← không có → ResourceNotFoundException
  3. access.requireX(currentUser, entity)          ← không đủ quyền → AccessDeniedException
  4. ghi đè trường server tự quyết (phòng ban, người tạo...)
  5. validator.validate(request)                   ← sai → {X}ValidationException(field → message)
  6. kiểm tra nghiệp vụ cần DB (trùng, phụ thuộc)  ← requireNoOtherDraft(...)
  7. áp dữ liệu vào entity, chuyển trạng thái
  8. save → audit → notification
  ```

- Kiểm tra lặp ở ≥ 2 method → tách `private` method `requireXxx(...)` (ném exception) hoặc `isXxx(...)` (trả boolean).
- Sửa nhiều người cùng lúc dễ đụng nhau (duyệt, cấp số đợt) → đọc bằng `findForUpdate` (khóa ghi).
- Không gửi email / thông báo trước khi lưu xong dữ liệu chính.

## 11. Phân quyền

Hai lớp, cả hai bắt buộc:

| Lớp | Ở đâu | Kiểm tra |
|---|---|---|
| Theo URL | `core/config/SecurityConfig` (không dùng `@PreAuthorize`) | Role nào vào được đường dẫn nào. **Route mới phải có rule**, nếu không sẽ rơi vào "mọi người đã đăng nhập", kể cả ứng viên |
| Theo bản ghi | `{X}Access` | Người này có được xem / sửa / duyệt **bản ghi này** không (chủ sở hữu, phòng ban, trạng thái) |

Rule cho `{X}Access`:

- `canX(user, entity, ...)` là **hàm thuần**: chỉ đọc role, người tạo, phòng ban, trạng thái → test không cần DB. Thông tin cần truy vấn (đã có tin chưa, có đợt sau chưa) thì service tra trước rồi truyền vào tham số.
- `requireX(...)` gọi `canX` và ném `AccessDeniedException` với câu tiếng Việt.
- Trạng thái chỉ cần so chuỗi → hàm `static` (`isFinishedStatus(status)`), vì Mockito không giả lập hàm static nên service test vẫn chạy luật thật.
- **Cờ quyền cho giao diện** (`editable`, `deletable`, `copyable`, `postable`...) do Mapper gắn vào DTO bằng cách gọi `access.canX`. **Template chỉ đọc cờ** (`th:if="${req.editable}"`), không tự so sánh role / trạng thái. `th:if` không thay cho kiểm tra ở backend.
- Phạm vi dữ liệu ở màn danh sách = một `Specification` theo role, mọi bộ lọc khác **nối AND** vào đó → sửa URL cũng không thấy dữ liệu ngoài phạm vi. Phạm vi danh sách phải khớp `requireView`.
- Không tin dữ liệu frontend: người tạo, người duyệt, phòng ban (với HM), trạng thái, số đợt đều do server tự xác định.
- **System Admin chỉ cấu hình hệ thống** (tài khoản, phân quyền, phòng ban, giám sát API, cấu hình AI, audit log). Không cho Admin vào nghiệp vụ (requisition, job posting, hồ sơ, phỏng vấn, offer), kể cả chỉ xem: không thêm `ROLE_SYSTEM_ADMIN` / `"System Admin"` vào `SecurityConfig` nghiệp vụ, `{X}Access`, `ListService#scope` hay `th:if`. Role không thuộc nhánh nào trong `scope` / `buildFilterOptions` thì ném `AccessDeniedException`, không trả "xem tất cả". Xem ARCHITECTURE_GUIDE §5.

## 12. Validation

Ba tầng (AG §2.4):

```text
HTML (required, maxlength, min, max, type) — chỉ hỗ trợ UX
→ Bean Validation trên DTO (@NotBlank, @Size...) — lỗi tự vào BindingResult
→ Validator nghiệp vụ ({module}/validator/, service gọi) — ném {X}ValidationException
```

- **Tầng 2 là bắt buộc** (AG §2.4, §9): DTO form có annotation cho từng ô (`@Size(max = ...)`, `@Min` / `@Max`, `@DecimalMin` / `@DecimalMax` / `@Digits`, `@Email`, `@Pattern`), controller nhận bằng `@Valid`, danh sách lồng nhau gắn `@Valid`. Validator nghiệp vụ chỉ lo phần cần DB / so nhiều ô / phân biệt nháp và gửi duyệt, nhưng vẫn kiểm tra lại toàn bộ (service không tin controller). Mẫu: `RequisitionRequest`.
  - Giới hạn và câu lỗi trong annotation lấy **hằng số của Validator** (`message = RequisitionValidator.TITLE_TOO_LONG`) để hai tầng báo cùng một câu.
  - Form có "Lưu nháp" thì **không** đặt `@NotBlank` / `@NotNull` (nháp được để trống); `null` tự qua các annotation khác.
  - Ô **nhiều dòng không đặt `@Size`**: Bean Validation chạy trước `cleanMultiline`, trình duyệt gửi `\r\n` (2 ký tự) nên sẽ chặn oan đoạn văn gần giới hạn. Validator đếm sau khi chuẩn hóa.
  - `@Digits(integer = ...)` lấy theo độ chính xác cột DB, để vượt max chỉ báo một câu (của `@DecimalMax`).
  - Kiểm tra nhiều ô dùng chung thì viết annotation cho cả class (`core/validation/`, mẫu `@PasswordMatches`).
- **Độ dài khớp 3 nơi:** `HTML maxlength = DTO / Validator limit = cột DB`. Không để dữ liệu vượt độ dài rồi mới lỗi SQL.
- Hằng số giới hạn đặt `public` trong Validator; controller đưa vào model cho template (`th:attr="maxlength=${...}"`), test dùng `LIMIT + 1`.
- **Làm sạch chuỗi trước khi kiểm tra:** `cleanString` (trim, chuỗi chỉ khoảng trắng → `null`), `cleanMultiline` (`\r\n` → `\n`, đếm độ dài sau khi chuẩn hóa).
- **Key lỗi = tên trường trên form** (`"title"`, `"screeningCriteria[0].weight"`) để lỗi hiện đúng ô. Gom **tất cả** lỗi rồi ném một lần (người dùng sửa một lượt).
- Phân biệt "Lưu nháp" và "Gửi duyệt": nháp chỉ bắt buộc cột NOT NULL, nhưng giá trị đã nhập vẫn phải hợp lệ (số âm, ngoài danh mục, quá dài vẫn chặn).
- Họ tên tiếng Việt: `^[\p{L}][\p{L}\s.'-]*$`, không dùng `[a-zA-Z ]`.
- Hiện trạng cần biết: `RequisitionValidator.validate()` vừa làm sạch vừa kiểm tra (có sửa DTO). Code mới nên tách `normalize(dto)` và `validate(dto)`, hoặc đặt tên `normalizeAndValidate`.

Danh sách case phải nghĩ tới cho **mọi form** (xem cách test ở mục 23.5):

```text
null · rỗng "" · chỉ khoảng trắng "   " · min · max · max + 1 · sai định dạng · số âm · số 0
ngày quá khứ / hôm nay / biên trên · trùng · ID giả (id của bản ghi khác) · giá trị ngoài danh mục
email / SĐT sai định dạng · file sai đuôi / sai MIME / quá dung lượng (nếu có)
```

## 13. Exception

| Tình huống | Service ném | Người dùng thấy |
|---|---|---|
| Không tìm thấy bản ghi | `core.exception.ResourceNotFoundException` | Trang 404 |
| Không có quyền với bản ghi | `org.springframework.security.access.AccessDeniedException` | Trang 403 |
| Vi phạm nghiệp vụ / dữ liệu sai | `{X}ValidationException extends BaseBusinessException` (mang `Map<field, message>`) | Form + lỗi cạnh ô |

- **Không dùng** cho lỗi nghiệp vụ: `RuntimeException`, `IllegalArgumentException`, `IllegalStateException`, `EntityNotFoundException`, `ResponseStatusException`.
- Câu lỗi tiếng Việt, nói rõ cần làm gì ("Vui lòng nhập lý do để Hiring Manager chỉnh sửa.").
- Optional: luôn `orElseThrow(...)`, không `get()`. Không trả `null` cho collection — trả list rỗng.

## 14. Trạng thái & chuyển trạng thái

Không nhận trạng thái từ client. Mỗi chuyển trạng thái là **một method service** có kiểm tra quyền + trạng thái hiện tại.

**Requisition** (giá trị DB):

```text
Draft            → Pending_Director   (HM gửi duyệt)
Draft            → Approved           (NGOẠI LỆ: Giám đốc tự tạo bấm "Phê duyệt trực tiếp";
                                        vẫn lưu RequisitionApproval người duyệt = chính Giám đốc + audit)
Pending_Director → Approved | Rejected (Giám đốc, không tự duyệt yêu cầu của mình; trả lại bắt buộc lý do)
Pending_Director → Draft              (người tạo rút lại)
Rejected         → Pending_Director   (sửa rồi gửi lại)
```

Không cho: `Approved → Draft`, `Draft → Approved` (trừ ngoại lệ Giám đốc), sửa / xóa khi không ở `Draft` / `Rejected`.

**Job Posting** (CHECK chỉ 4 giá trị `Draft`, `Published`, `Paused`, `Closed`):

```text
Draft      → Published   (đăng ngay: PostingDate = giờ server; hoặc hẹn đăng: PostingDate trong 90 ngày tới)
Published hẹn đăng (PostingDate > hiện tại) → Published (đăng ngay) | Draft (hủy lịch: PostingDate = NULL)
Published  → Paused | Closed (Closed chỉ HR)
Paused     → Published (mở lại: PostingDate = giờ server) | Closed (chỉ HR)
Closed     → không đổi nữa; "Đăng lại" = tạo tin MỚI cùng yêu cầu, PostingNo + 1
```

"Hẹn đăng" **không phải** trạng thái trong DB: là `Published` + `PostingDate` > hiện tại. Mọi truy vấn trang công khai phải kèm `PostingDate <= hiện tại` (`JobPosting#isVisibleToCandidates`).

## 15. Database / JPA / N+1

### 15.1. Schema

- Nguồn chân lý: `database/schema/db.sql` (+ `database/migrations/`, ERD `docs/database/model.md`). `ddl-auto=validate`: entity lệch `db.sql` thì app không khởi động.
- Đổi bảng / cột: sửa `db.sql` + migration (+ `model.md` nếu đổi thiết kế) + entity + DTO + validator + test cùng lúc.
- Chữ tiếng Việt: `NVARCHAR`. Giữ kiểu SQL Server sẵn có (`IDENTITY`, `DATETIME2`, `SYSDATETIME()`, `BIT`). Không chuyển sang MySQL.
- Seed: `seed_data.sql` rồi `seed_extra_jobs_and_pipeline.sql`. Không sinh lại `seed_data.sql` bằng `build_seed.py` (có dữ liệu thêm tay).
- `nullable = false` trên entity chỉ để mô tả; ràng buộc thật ở DB. Cột NOT NULL mà nháp được để trống → lưu chuỗi rỗng `""` (có comment `// Rule:` giải thích).

### 15.2. Repository

- Tên method nói rõ truy vấn gì: `existsDraftByTitleAndDepartment`, `findMaxRoundInChain`, `findIdsWithLaterRounds`.
- JPQL nhiều dòng dùng text block `"""`, có comment một dòng phía trên.
- Không đặt business rule trong repository; không trả entity khổng lồ khi chỉ cần một con số (`COUNT`, `MAX`, `SELECT id`).

### 15.3. Chống N+1 (bắt buộc ở màn danh sách)

N+1 = mỗi dòng trong bảng chạy thêm vài câu SQL. Trang 10 dòng × 4 câu = 40 câu.

- **Mapper không được gọi repository.** Service / ListService tra dữ liệu liên quan trước rồi truyền vào mapper.
- Dữ liệu phụ của **cả trang** tra bằng **một câu `IN (:ids)`**, rồi dựng `Map<id, ...>` / `Set<id>`:

  ```java
  List<Integer> ids = ...;                                           // id các dòng trong trang
  Map<Integer, JobPosting> latestPostings = latestPostingsOf(ids);  // 1 câu
  Set<Integer> idsWithLaterRounds = idsWithLaterRounds(ids);         // 1 câu
  return page.map(r -> mapper.toListRow(r, viewer,
          latestPostings.get(r.getRequisitionId()),
          idsWithLaterRounds.contains(r.getRequisitionId())));
  ```

- Trang rỗng → **không chạy** câu `IN ()` (trả map / set rỗng luôn).
- Quan hệ `@ManyToOne` hiển thị ở mọi dòng (phòng ban, người tạo) → `@EntityGraph(attributePaths = {...})` trên `findAll(spec, pageable)` của repository.
- Có test đếm số lần gọi (mục 23.6).

## 16. Màn danh sách

Bắt buộc có **Tìm kiếm + Lọc + Sắp xếp + Phân trang + Thao tác (menu ⋯)**, xử lý **server-side** (`Pageable` + `Specification`). Không `findAll()` rồi để JavaScript tự lọc.

`{X}ListService.getListPage(rawFilter)` luôn theo **4 bước** (mẫu: `RequisitionListService`):

```text
1. buildFilterOptions(viewer)   — role này thấy ô lọc nào, mỗi ô có lựa chọn gì
2. normalizeFilter(raw, options) — không tin URL: giá trị role không được dùng / ngoài danh sách → bỏ qua (không báo lỗi)
3. search(filter, viewer)        — scope theo role AND từng bộ lọc → 1 trang; batch dữ liệu phụ (mục 15.3)
4. describeActiveFilters(...)    — mỗi bộ lọc đang bật thành một chip có nút ✕
```

Dùng chung từ `core`:

| Việc | Dùng |
|---|---|
| Làm sạch từ khóa (trim, null → "", cắt ở 120 ký tự) | `ListFilters.cleanKeyword(raw.q())` |
| Giữ giá trị hợp lệ, bỏ trùng | `ListFilters.keepAllowedValues(values, options)` |
| Kiểm tra một giá trị nằm trong lựa chọn | `ListFilters.isAllowedValue(value, options)` |
| Chữ hiển thị của chip | `ListFilters.labelOf(value, options)` |
| LIKE có chứa, escape `% _ [` | `cb.like(cb.lower(field), ListFilters.containsPattern(keyword), '\\')` |
| Lựa chọn ô lọc / chip | `core.dto.FilterOption`, `core.dto.FilterChip` |
| Link "bỏ một bộ lọc" giữ các tham số khác | `model.addAttribute("filterLinks", new FilterLinks(uri, query))` |

Rule khác:

- **Sắp xếp là enum whitelist** (`{X}Sort`), luôn kèm id làm tiêu chí phụ để thứ tự ổn định giữa các trang. Giá trị lạ → mặc định.
- Phân trang: `page < 0` → 0; `size` kẹp trong `[1, MAX_PAGE_SIZE]`; số trang vượt tổng → tự lùi về trang cuối.
- Danh sách lựa chọn "Người tạo" chỉ gồm người có bản ghi trong phạm vi người xem (không lộ tên ngoài phạm vi).
- Search request là `record` nhận thẳng từ query string, có Javadoc từng tham số.

## 17. DTO

- `*Response`: **`record`** (AG §2.5, §3). DTO nhiều trường thì dựng một lần trong Mapper rồi trả record, không gán dần.
  Record có nhiều trường: `@Builder(toBuilder = true)`; Mapper tính đủ giá trị rồi `build()` một lần, màn chi tiết dùng `toListRow(...).toBuilder()...build()` (mẫu `RequisitionMapper`). Test gọi `row.editable()` thay cho `row.isEditable()`; template vẫn viết `${req.editable}`.
- `*Request`: `record` nếu đơn giản; `class` (`@Getter @Setter @NoArgsConstructor` hoặc `@Data @Builder`) khi form cần giá trị mặc định hoặc có danh sách lồng nhau (AG §2.5).
- Không bind entity trực tiếp từ form.
- Không nhận từ frontend các trường server tự xác định: `approvalStatus`, `postingStatus`, `createdBy`, `createdAt`, `postingDate` (trừ "hẹn ngày đăng" có kiểm tra), `requesterId`, `recruitmentRound`.
- Trường chỉ để hiển thị trên form (ví dụ `originalTitle`) ghi rõ Javadoc "server không dùng giá trị này".

## 18. Audit & thông báo

- Ghi audit qua `{X}AuditLogger` (một chỗ biết tên entity, cách mô tả thay đổi).
- Sửa bản ghi: chụp `snapshot` trước và sau, so bằng `FieldChanges.changedKeys(before, after)`. **Không đổi gì thì không lưu và không ghi audit.** Có đổi thì ghi `FieldChanges.describeBefore` / `describeChange` ("Số lượng cần tuyển: 2 → 3").
- Snapshot dùng tên trường tiếng Việt (người đọc lịch sử là người dùng), giá trị trống hiện "Chưa xác định".
- Chuyển trạng thái: audit ghi "Status: cũ → mới" + ý kiến (nếu có).
- Notification gọi **sau** khi lưu xong, qua service của module `notification`.

## 19. Nhãn & message

- Dịch giá trị DB → tiếng Việt **chỉ ở `{X}Labels`**; template không tự dịch, không `th:switch` theo giá trị DB.
- Tông màu badge: Labels trả ý nghĩa (`success` / `warning` / `danger` / `neutral`), template ghép class `badge--...`.
- Văn bản nhiều dòng người dùng nhập: `HtmlUtils.htmlEscape` **trước**, rồi mới đổi `\n` → `<br/>`, hiển thị bằng `th:utext` (AG §6.3). Escape sau là lỗ hổng XSS. AG §6.3 chưa ghi bước escape → đã đề xuất Lead bổ sung; trong lúc chờ, làm theo dòng này.
- Message lỗi / thành công tiếng Việt, thống nhất giữa HTML, validator và test (test so sánh nguyên câu hoặc dùng hằng số).

## 20. Giao diện (Thymeleaf)

- Kế thừa 3 cấp: `layout/base.html` → `layout/internal.html` / `layout/public.html` → trang dùng `layout:decorate="~{layout/internal}"` và điền `layout:fragment="content"`.
- Controller chỉ đặt `activeMenu` (+ `bodyClass` nếu cần); `topbarUser`, `pageLinks` có sẵn từ `core/web`. Menu theo role ở `templates/fragments/layout/sidebars/*.html`.
- **Không** `style` inline, `<style>`, `<script>` trong HTML, mã màu hex — dùng token `var(--...)` trong `tokens.css`. CSS / JS riêng trang: `static/css/pages/<trang>.css`, `static/js/pages/<trang>.js`, class có tiền tố riêng.
- Xem `UI_CATALOG.md` trước khi viết markup / CSS mới. File dùng chung (`components.css`, `fragments/ui/`, layout) là "chỉ thêm / không chạm" theo UI_RULES §2 — hỏi trước khi sửa.
- Không đổi tên / chuyển template, không đổi tên bind với backend (`${...}`, `th:field`, `name`, `id`).
- Một chức năng cho nhiều role = **một màn** + dữ liệu theo role + nút theo cờ quyền. Không tạo `hm/detail.html`, `director/detail.html`.
- Tạo và sửa cùng cấu trúc = **một template form** (`requisitions/form.html`).
- Textarea full width; ô chỉ đọc nhìn khác ô sửa được; lỗi nằm cạnh ô; xóa phải có hộp xác nhận.
- Form sửa quan trọng (AG §6.3): nút Lưu chỉ bật khi có thay đổi (dirty check), luôn có nút Hủy; xóa / cập nhật lớn qua hộp thoại xác nhận.
- Tách fragment mới theo **Rule of 3** (AG §6.2): chỉ khi một khối markup có cấu trúc và tham số lặp lại ở ≥ 3 nơi. Không tách `div` bọc đơn thuần. Fragment khung ở `fragments/layout/`, linh kiện nội dung ở `fragments/ui/`.
- Điều hướng bằng menu / nút / link của hệ thống, không phụ thuộc gõ URL tay.

---

# PHẦN D — NGHIỆP VỤ

## 21. Requisition

- **Phân quyền:** HM thao tác yêu cầu của mình / phòng ban mình (thuộc hoặc làm trưởng phòng); Director xem mọi yêu cầu trừ nháp, duyệt / trả lại yêu cầu `Pending_Director` của người khác; HR chỉ xem `Approved`; System Admin không truy cập nghiệp vụ Requisition.
- **Phòng ban:** HM luôn lấy phòng ban của tài khoản (bỏ giá trị trên form), tài khoản không có phòng ban `Active` thì không tạo được. Director tự chọn phòng ban `Active`.
- **Gửi duyệt phải kiểm tra:** trường bắt buộc, blank / space, độ dài, khoảng lương (1 → 1.000.000.000, min ≤ max, ≤ 2 số thập phân), số lượng 1–30, ngày bắt đầu từ hôm nay đến +1 năm, thử việc 1–180 ngày, tiêu chí sàng lọc.
- **Tiêu chí:** tối đa 50 dòng; dòng trống bỏ qua khi nháp; dòng đã bắt đầu nhập phải đủ; tên không trùng (không phân biệt hoa thường); trọng số 0.01–100, ≤ 2 số thập phân; **gửi duyệt tổng đúng 100%** (kiểm ở backend, không chỉ JS). `criteriaId` gửi lên phải thuộc chính yêu cầu đó.
- **Một phòng ban không có hai bản nháp cùng tên vị trí.**
- **Sao chép / đợt mới:** chỉ từ yêu cầu `Approved` / `Rejected`; giữ tên vị trí (không phân biệt hoa thường, bỏ khoảng trắng thừa) → trỏ về phiếu đợt 1, đợt = max + 1; đổi tên → yêu cầu mới ở đợt 1. Không copy: id, trạng thái, lịch sử duyệt, audit, createdAt / updatedAt, lý do trả lại, `criteriaId`.
- **Xóa:** chỉ khi sửa được, chưa có tin tuyển dụng, chưa có đợt sau; xóa lịch sử duyệt rồi xóa phiếu, audit ghi trước khi xóa.
- **Mã yêu cầu:** `REQ-` + id đệm 4 chữ số, sinh sau lần lưu đầu.

## 22. Job Posting

- Chỉ tạo từ **yêu cầu `Approved`**, kiểm lại khi đăng.
- Chỉ **HR** tạo / sửa / đăng / tạm dừng / đóng / xóa tin; HM, Director chỉ xem (không thấy tin nháp); System Admin không vào màn tin.
- Thông tin yêu cầu nguồn hiển thị **chỉ đọc** và luôn nạp lại từ DB (không tin số gửi lên). "Lý do tuyển dụng" chỉ nội bộ, không copy sang tin, không hiện cho khách / ứng viên.
- HR chỉ sửa trường thuộc bảng `JobPosting`: `PostingTitle`, `JobDescription`, `JobRequirements`, `Benefits`, `SalaryDisplay`, `NumberOfPositionsDisplay`, `WorkLocation`, `ApplicationDeadline` (+ thời điểm hẹn đăng: > hiện tại, ≤ 90 ngày, < hạn nộp). Không nhập trực tiếp: `PostingStatus`, `CreatedBy`, `CreatedAt`, `UpdatedAt`, yêu cầu nguồn sau khi tạo.
- **Lưu nháp:** `Draft`, `PostingDate = NULL`; vẫn bắt buộc Tiêu đề, Mô tả, Yêu cầu (NOT NULL); không giữ lịch hẹn.
- **Đăng:** kiểm tra đầy đủ → yêu cầu nguồn vẫn `Approved` → `Published` → `PostingDate` = giờ server hoặc giờ hẹn → hạn nộp bắt buộc và sau `PostingDate` (`CK_JobPosting_Deadline`) → audit → thông báo HM.
- Sửa tin **không** sửa ngược yêu cầu.
- **Trang công khai:** query tự lọc `Published` và `PostingDate <= hiện tại` (gõ URL trực tiếp cũng không thấy tin nháp / tạm dừng / đóng / chưa tới giờ). HM, Director mở tin nháp → 403.
- Nội dung công khai dùng chung một fragment giữa trang chi tiết nội bộ và công khai; trang công khai không có dữ liệu nội bộ (trạng thái, người tạo, lịch sử, yêu cầu nguồn).

---

# PHẦN E — TEST

## 23. Rule test

### 23.1. Mỗi business rule quan trọng phải có test

"Xong" = có test cho rule đó và test PASS. Sửa bug → thêm test tái hiện bug trước rồi mới sửa.

### 23.2. Chọn tầng test

| Tầng | Kiểm tra | Spring? DB? | Class mẫu |
|---|---|---|---|
| Unit — Validator / util | Một quy tắc nhập liệu, hàm tiện ích | Không / Không | `RequisitionValidatorTests`, `ListFiltersTests`, `FieldChangesTests` |
| Unit — Access | Bảng "ai được làm gì" | Không / Không | `RequisitionAccessTests` |
| Unit — Mapper | Cờ quyền, nhãn, mốc thời gian trên DTO | Không / Không | `RequisitionMapperTests` |
| Unit — Service (Mockito) | Nghiệp vụ một hàm: ghi gì, chặn gì, gọi ai | Không / Không | `RequisitionServiceTests`, `RequisitionListServiceTests` |
| Controller (`@WebMvcTest`) | URL → view / redirect / flash, lỗi theo ô, 403 / 404, CSRF, phiên hết hạn | Web / Không | `RequisitionControllerTests` |
| Integration (`@SpringBootTest`) | JPQL / Specification chạy đúng trên SQL Server, khóa ngoại, NVARCHAR, render trang thật | Đủ / **Có** | `RequisitionIntegrationTests` |
| System (Postman) | Cả app qua HTTP như trình duyệt (đăng nhập, CSRF, session) | App chạy / Có | `requisition.postman_collection.json` |

Quy tắc chọn: rule nhập liệu → validator; "ai được làm gì" → access; "ghi bảng nào, chặn gì" → service; "bấm nút về trang nào, hiện thông báo gì" → controller; "câu SQL / ràng buộc DB đúng không" → integration; "demo cả luồng" → Postman.

Mỗi class production quan trọng có **một class test cùng tên + `Tests`**, cùng package.

### 23.3. Đặt tên & cấu trúc

```java
@Test
@DisplayName("Trả lại mà lý do chỉ có khoảng trắng -> Báo lỗi, giữ Pending, không thông báo")  // điều kiện -> kết quả
void decide_shouldReject_whenRejectReasonIsBlank() {                                        // hàm_shouldKếtQuả_whenĐiềuKiện
    // Arrange: chuẩn bị dữ liệu và giả lập
    // Act:     gọi đúng 1 hàm cần test
    // Assert:  kết quả + những gì KHÔNG được xảy ra
}
```

- Tên method tiếng Anh theo mẫu `hàm_should..._when...`; `@DisplayName` tiếng Việt dạng **"điều kiện -> kết quả"**. Không `test1()`, `testApprove()`, `val003_xxx()`.
- Ghi `// Arrange`, `// Act`, `// Assert` (test 1–2 dòng thì gộp `// Act + Assert`).
- Đầu class có comment: test class nào, tầng nào, setup gì, quy ước.
- Nhiều test cho một hàm → gom bằng `@Nested @DisplayName("tênHàm")`.
- Cùng một quy tắc, nhiều giá trị → `@ParameterizedTest` (`@ValueSource`, `@CsvSource`, `@NullAndEmptySource`), có `name = "..."` dễ đọc.
- Bảng quyền (role × trạng thái) → `@CsvSource` một dòng một trường hợp (mẫu: `requireView_shouldFollowRoleAndStatusTable...`).

### 23.4. Dựng đối tượng cần test

- **Service test: tạo service bằng tay** trong `@BeforeEach` để thấy rõ nó cần gì; repository / access / validator / notification là `@Mock`; Mapper và AuditLogger dùng **bản thật** (truyền mock repository vào) → verify được `audit.save(...)` và dữ liệu DTO thật.

  ```java
  RequisitionMapper mapper = new RequisitionMapper(access, validator, CLOCK);
  RequisitionAuditLogger auditLogger = new RequisitionAuditLogger(audit);
  service = new RequisitionServiceImpl(requisitions, departments, approvals, postings, access, validator,
          mapper, auditLogger, notificationService, CLOCK);
  ```

- **Mapper / Access test:** dùng bản thật (`new RequisitionAccess(null)`, `new RequisitionValidator(CLOCK)`) — không cần mock vì chúng là hàm thuần.
- Thời gian: luôn `Clock.fixed(...)`; so sánh với `NOW = LocalDateTime.now(CLOCK)` / `TODAY`. **Không** `LocalDate.now()` trong test.
- Stub dùng chung đặt `lenient()` trong `@BeforeEach`; stub riêng của test đặt trong test đó.
- Hàm hỗ trợ có tên nghiệp vụ: `valid()`, `minimalDraft()`, `requisitionWithStatus(id, status)`, `savedRequisition()` (lấy entity bằng `ArgumentCaptor`), `savedAudit()`.
- Dùng **hằng số của production** trong test (`POSITIONS_MAX + 1`, `COMMENT_CHAR_LIMIT`, `STATUS_APPROVED`, `ACTION_SUBMIT`) thay vì gõ số / chuỗi.

### 23.5. Case bắt buộc

| Nhóm | Case tối thiểu |
|---|---|
| Happy path | Dữ liệu hợp lệ đầy đủ; dữ liệu tối thiểu (nháp) |
| Ô chữ | null · `""` · chỉ khoảng trắng · đúng giới hạn · giới hạn + 1 · khoảng trắng đầu / cuối được cắt |
| Số / tiền | 0 · âm · min · max · max + 1 · quá số chữ số thập phân |
| Ngày | hôm qua · hôm nay (biên) · đúng biên trên · biên trên + 1 ngày · null |
| Danh mục | mỗi giá trị hợp lệ · giá trị lạ · sai hoa / thường |
| Trùng | trùng tên · trùng không phân biệt hoa thường · cùng id gửi hai lần |
| ID giả | id của bản ghi khác / không tồn tại gửi từ form |
| Trạng thái | thao tác ở mỗi trạng thái không hợp lệ bị chặn, trạng thái **không đổi** |
| Quyền | đúng người được; người khác / role khác / tự duyệt bị chặn (`AccessDeniedException` / 403) |
| Không tìm thấy | id không tồn tại → `ResourceNotFoundException` / 404 |
| Gõ URL trực tiếp | role không được vào → 403; ứng viên vào trang nội bộ → 403 |
| Màn danh sách | lọc theo từng role · giá trị lọc lạ bị bỏ · sort lạ · page âm · size 0 / quá lớn · trang vượt tổng |
| Controller | thành công → redirect + flash · lỗi → render lại form + lỗi đúng ô · thiếu CSRF → 403 · trường lạ trên form bị bỏ qua |

### 23.6. Kiểm tra cả những gì KHÔNG được xảy ra

Một test chặn chưa đủ nếu chỉ kiểm tra exception. Phải kiểm tra thêm:

```java
verify(requisitions, never()).saveAndFlush(any());          // không lưu
verifyNoInteractions(audit, notificationService);           // không audit, không thông báo
assertEquals(RequisitionAccess.STATUS_PENDING_DIRECTOR, pending.getApprovalStatus());  // trạng thái giữ nguyên
```

Chống N+1: test màn danh sách có nhiều dòng phải verify mỗi câu batch được gọi **đúng 1 lần** và các hàm tra theo từng id **không** được gọi:

```java
verify(postings, times(1)).findLatestByRequisitionIds(anyCollection());
verify(postings, never()).findTopByRequisition_RequisitionIdOrderByJobPostingIdDesc(any());
```

### 23.7. Controller test

- `@WebMvcTest(controllers = XController.class)` + `@Import({SecurityConfig.class, DatabaseUserDetailsService.class})` → bảo mật **thật**.
- `@MockitoBean` mọi phụ thuộc của controller (service, list service, access, validator...) và `UserRepository` (bộ lọc phiên đọc tài khoản ở mỗi request).
- `@WithMockUser(username = "...", roles = "...")`; POST luôn `.with(csrf())` (trừ test thiếu CSRF).
- Kiểm tra: `status()`, `view().name(...)`, `redirectedUrl(...)`, `flash().attribute(...)`, `model().attributeHasFieldErrors(...)`, và `verifyNoInteractions(service)` khi bị chặn.

### 23.8. Integration test

- `@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")` + `@Transactional` (rollback sau mỗi test, không để lại dữ liệu) + `@AutoConfigureMockMvc` nếu cần render trang.
- Tạo dữ liệu bằng hàm hỗ trợ có đủ cột NOT NULL (`validRequisition()`, `savePosting(...)`), tiêu đề riêng cho mỗi test để tìm kiếm không đụng dữ liệu seed.
- `entityManager.clear()` trước khi đọc lại để chắc dữ liệu đi qua JDBC (kiểm NVARCHAR), không lấy từ cache.
- Bắt buộc có integration test cho mỗi câu JPQL / Specification mới (ví dụ `findLatestByRequisitionIds`).

### 23.9. Trước khi báo "xong"

```text
[ ] ./mvnw -o clean test cho module PASS (test integration cần SQL Server đang chạy)
[ ] Test lỗi có sẵn từ trước (không do mình) đã ghi tên vào checklist, không giấu
[ ] Không còn test tên kiểu test1 / val003_ / thiếu @DisplayName
[ ] Rule mới có test; bug đã sửa có test tái hiện
[ ] Cập nhật bảng "hàm nào đã có test" trong <module>-testing-guide.md
```

---

# PHẦN F — QUY TRÌNH NHÓM

## 24. Logging

- Không `System.out.println`. Dùng logger: `log.info("Requisition {} submitted by user {}", id, userId);`
- Không log mật khẩu, OTP, token, API key, secret, dữ liệu cá nhân không cần thiết.

## 25. Git & commit

- Nhánh một mục tiêu: `feat/<tên>-<feature>`, `fix/<tên>-<bug>`, `refactor/<tên>-<phạm vi>`, `docs/<tên>-<chủ đề>`. Không trộn sửa của module người khác vào nhánh mình.
- Đồng bộ với `main`: `git fetch origin && git rebase origin/main` (tránh commit merge chéo). Merge vào `main` thường xuyên; demo chỉ chạy từ source đã tích hợp.
- **Commit theo feature hoàn chỉnh**, không commit từng bước nhỏ. Thân commit liệt kê các phần đã làm.
- Mẫu: `<type>(<scope>): <mô tả ngắn>` với `type` ∈ `feat`, `fix`, `refactor`, `style`, `test`, `docs`.

  ```text
  refactor(requisition): tách service, cờ quyền vào Access, Clock, sửa N+1, test theo rule

  - core: ClockConfig, FilterOption/FilterChip, ListFilters, FilterLinks, FieldChanges
  - requisition: RequisitionListService, RequisitionMapper, RequisitionAuditLogger
  - test: đổi tên *Tests + method_should_when, Clock cố định, bổ sung case biên / quyền / not found
  ```

- **Không** thêm dòng tác giả AI (`Co-authored-by: Claude`...) vào commit.
- Không commit: `target/`, log, file tạm, IDE cache, `application-local.properties`, mật khẩu / token.
- Xóa nhánh sau khi PR đã merge.

## 26. Checklist

### 26.1. Trước khi push / tạo PR

```text
[ ] Rebase trên origin/main mới nhất; diff chỉ chứa file thuộc phạm vi feature
[ ] Compile, không còn lỗi IDE quan trọng (lỗi Lombok giả của VS Code thì bỏ qua)
[ ] Không System.out, import thừa, code chết, code cũ trong comment, biến vô nghĩa
[ ] Không hard-code trạng thái / role / giới hạn / message lặp (mục 7)
[ ] Không gọi LocalDate.now() / LocalDateTime.now() trực tiếp trong code mới (mục 8)
[ ] Validation blank / space / độ dài; maxlength = validator = DB; DTO form có Bean Validation + controller @Valid
[ ] *Response mới là record; form sửa có dirty check + nút Hủy
[ ] Phân quyền backend: SecurityConfig có rule cho route mới + {X}Access cho từng bản ghi
[ ] Chuyển trạng thái hợp lệ (mục 14)
[ ] Màn danh sách: search / filter / sort / paging server-side, không N+1
[ ] Test PASS (mục 23.9)
[ ] Khớp db.sql, SRS, wireframe đã duyệt, thuật ngữ tiếng Việt; không phá layout / sidebar dùng chung
[ ] Đã ghi checklist; sửa file dùng chung thì ghi "cần báo nhóm"
[ ] PR có mô tả: mục tiêu, màn hình liên quan, cách test cho reviewer
```

### 26.2. Review code (theo thứ tự)

1. Business rule đúng chưa? Nằm đúng **một** chỗ chưa (mục 4)?
2. Phân quyền đúng chưa — URL và từng bản ghi? Template có tự so sánh role không?
3. Validation chặn trước DB chưa? Blank / space / max length?
4. Chuyển trạng thái hợp lệ chưa?
5. Controller mỏng chưa? Service có method quá dài không?
6. Có hard-code, `now()` trực tiếp, ternary lồng, stream dài không?
7. Màn danh sách có N+1 không (mapper có gọi repository không)?
8. Có code lặp với module khác nên đưa vào `core/util` không?
9. Exception đúng loại chưa? Transaction hợp lý chưa?
10. Có test cho rule mới, có kiểm tra "không được xảy ra" chưa?
11. Khớp SQL Server / SRS / wireframe / UI chưa?

## 27. Cách làm sạch một module cũ

Đã áp dụng cho Requisition (2026-10-18). Module khác làm theo thứ tự này, **ít rủi ro trước**, chạy test sau mỗi bước:

| Bước | Việc | Dấu hiệu cần làm |
|---|---|---|
| 0 | Chạy test của module, ghi lại số test và các lỗi có sẵn làm mốc | — |
| 1 | Chuỗi viết cứng → hằng số (mục 7); thêm ngoặc nhọn; bỏ wildcard import | `"Approved"`, `"submit"`, `1000` rải trong code |
| 2 | Cờ quyền → `{X}Access` (hàm thuần `canX`) + test bảng quyền | Service / mapper tự ghép `owns && status == ...` |
| 3 | `now()` → tiêm `Clock`; rule ngày ở một chỗ; test dùng `Clock.fixed` | `LocalDate.now()` trong validator / service / controller |
| 4 | Tách service: `{X}ListService`, `{X}Mapper`, `{X}AuditLogger`; gộp kiểm tra lặp thành `requireXxx` | ServiceImpl > ~500 dòng; hàm `response()` > 50 dòng |
| 5 | Chống N+1: batch theo trang, `@EntityGraph`; test đếm số câu | Mapper / vòng lặp gọi repository |
| 6 | Chuyển helper lặp giữa module vào `core/util` | Cùng hàm private ở 2 module |
| 7 | Viết lại / bổ sung test theo Phần E; cập nhật testing guide + checklist | Tên test `xxx_001`, thiếu case mục 23.5 |

Đã áp dụng cho Job Posting (2026-10-18): `JobPostingAccess`, `JobPostingListService`, `JobPostingMapper`, `JobPostingAuditLogger` (+ `FieldChanges`), `Clock`, `@EntityGraph`, record DTO, test theo Phần E (xem `posting-testing-guide.md`).

Việc còn để sau (biết trước để trả lời GV): `RequisitionValidator.validate()` vẫn vừa làm sạch vừa kiểm tra (Job Posting đã đặt tên `normalizeAndValidate`); tách `PropertyEditor` ô lương thành class riêng có test; `job-postings/dashboard.html` còn khung cũ + `style` inline (chuyển sang `layout/internal` cần Lead duyệt).

## 28. Nguyên tắc quan trọng nhất

> Code phải được viết để thành viên khác đọc vào hiểu nghiệp vụ mà không cần hỏi lại người viết.

> Một business rule chỉ có một nơi chịu trách nhiệm chính.

> Một chức năng cho nhiều role thì một source dùng chung, phân quyền theo role / trạng thái ở backend.

> Validation chặn dữ liệu sai trước khi lỗi tới SQL Server.

> Mỗi rule quan trọng có test, và test kiểm tra cả những gì không được xảy ra.

> Không chỉ làm cho code chạy — code phải rõ ràng, test được, sửa nhanh khi review.

---

# PHỤ LỤC — Những điểm đã đổi so với 2 file cũ

| Chủ đề | File cũ ghi | File này | Lý do |
|---|---|---|---|
| Trạng thái | enum `RequisitionStatus.APPROVED`, `PENDING_APPROVAL` | Hằng số `RequisitionAccess.STATUS_*`, giá trị đúng DB (`Pending_Director`) | Khớp code và CHECK constraint thật |
| Controller try-catch | "Controller không tự try-catch" | Chỉ bắt **đúng** exception của module để render lại form | Theo AG §2.3 |
| Exception | `RequisitionNotFoundException`, `RequisitionNotEditableException`... | `ResourceNotFoundException`, `AccessDeniedException`, `{X}ValidationException` | Theo AG §2.3, khớp code |
| Comment | Ưu tiên tiếng Anh | Tiếng Việt, ngắn, giải thích vì sao | Nhóm / GV đọc tiếng Việt, code hiện tại đã dùng tiếng Việt |
| `mapper/` trong module | Được tạo | Mapper đặt trong `service/` | AG §1 không có sub-package `mapper/` |
| Layout | `fragments/header.html`, `sidebar.html` | `layout/base` → `internal` / `public` + `fragments/layout/sidebars/*` | Khớp cấu trúc hiện tại |
| `ddl-auto` | `none` | `validate` | Khớp `application.properties` |
| Commit | Chia nhỏ theo tầng (4 commit) | Một commit cho một feature hoàn chỉnh | Thống nhất 17/10/2026 |
| Tên test | `val003_...`, `svc10_...` trong testing guide | `hàm_should..._when...` + `@DisplayName` | Theo §26 cũ, áp dụng đồng bộ |
| `*Response` | (file này bản đầu) cho phép `class` + Lombok | `record` + `@Builder(toBuilder = true)`; Requisition đã chuyển | Khớp AG §2.5 |
| Bean Validation | Ví dụ controller không có `@Valid` | Bắt buộc annotation trên DTO + `@Valid` | Khớp AG §2.4, §9 |
| Dirty check, Rule of 3, gọi chéo module | Chưa có | Thêm vào mục 20, 3.1 | Lấy từ AG §6.2, §6.3, §7 |
| **Mới** | — | `Clock`; cờ quyền ở Access; Mapper không truy vấn DB; chống N+1; `ListService` 4 bước; helper `core/util`; `core/web` chỉ Lead; AuditLogger + `FieldChanges`; rule test chi tiết (Phần E); cách làm sạch module cũ (mục 27) | Rút ra khi làm sạch Requisition |

# Review PR8 (`dunglt/chore/refactor-config-docs`) & tác động tới nhánh interview

- **Người review:** Đức (Masanori-Satoh) — có AI hỗ trợ, đã kiểm chứng bằng lệnh/test thực tế
- **Ngày:** 2026-10-03
- **Commit được kiểm tra:**
  - `origin/main` = `1517775` (Merge PR #7)
  - PR8 = `c228c3b` (1 commit, đi trước main 1 commit, không chậm commit nào)
  - Nhánh của tôi `feature/iter1-interview-schedule_duc` (merge-base với main = `8c2754f`, đi trước 15 commit, chậm 6 commit)
- **Cách kiểm chứng:** tạo `git worktree` tạm ngoài repo cho `main`, PR8 và một bản thử merge (interview + PR8, `--no-commit`). **Không sửa code / không commit gì trên repo.**

> [!IMPORTANT]
> **Giới hạn môi trường:** SQL Server trên máy (`MSSQLSERVER`) đang **Stopped**, nên mọi test `@SpringBootTest` cần DB đều lỗi kết nối
> (`The TCP/IP connection to the host localhost, port 1433 has failed`). Kết quả của các test DB dưới đây là **CHƯA XÁC MINH ĐƯỢC**,
> không phải lỗi code. Chỉ các test `@WebMvcTest` / unit test (không cần DB) là có kết quả đáng tin.

---

## 0. Phát hiện quan trọng nhất trước khi đọc tiếp

| # | Phát hiện | Bằng chứng |
|---|---|---|
| A | `./mvnw test` trên **cả `main` lẫn PR8** đều **không biên dịch được test** → toàn bộ test suite không chạy. | `CareerFlowTests.java:[4,40] cannot find symbol: class CareerController`, `CareerReadOnlyTests.java:[24,16] cannot find symbol: class CareerService`. PR7 đã xóa 2 class này nhưng quên xóa/sửa test. |
| B | PR8 **thực tế chỉ đổi 5 file cấu hình**, **không có thay đổi nào trong `docs/`** dù mô tả PR nói "cấu trúc lại docs/". | `git diff --stat origin/main...PR8` → `.gitignore`, `PasswordResetEmailSender.java`, `PasswordResetService.java`, `application-local.properties.example`, `application.properties`. |
| C | Merge PR8 sẽ **ghi đè âm thầm** file `src/main/resources/application.properties` cá nhân của mọi người. | Trên máy tôi file này đang *untracked + ignored* (`.gitignore:93` của nhánh hiện tại). PR8 bắt đầu **track** file này. Theo hành vi chuẩn của Git, file bị ignore được coi là "expendable" và bị ghi đè khi checkout/merge mà không báo. → **Phải backup trước khi pull main.** |

Để vẫn lấy được kết quả SecurityFlowTests, tôi đã **xóa tạm 2 file test hỏng trong worktree nháp** (không phải repo thật) rồi chạy `mvnw -B test`.

---

## 1. GlobalExceptionHandler — **SAI**

**Code** ([GlobalExceptionHandler.java](file:///d:/Coding_source/swp_project/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/exception/GlobalExceptionHandler.java) — giống hệt nhau trên main và PR8):

| Dòng | Handler | Vấn đề |
|---|---|---|
| L13-20 | `ResourceNotFoundException` → 404 | OK |
| L22-29 | `BaseBusinessException` → view `error/500` | **Không có `@ResponseStatus`** → HTTP trả **200** kèm trang lỗi 500. Lỗi bảo mật PR7 ném `BaseBusinessException("...", "UNAUTHORIZED")` (`CareerPortalService.java:122`) cũng thành 200. |
| L31-38 | `@ExceptionHandler(Exception.class)` → 500 | **Có**, và **không có** handler riêng / cơ chế loại trừ cho `ResponseStatusException`, `AccessDeniedException`, `NoResourceFoundException`, `HttpRequestMethodNotSupportedException`, `MethodArgumentTypeMismatchException`. Handler cũng **không log** exception → nguyên nhân gốc bị nuốt mất. |

**Test thực tế (PR8, sau khi loại 2 test không compile):** `SecurityFlowTests: Tests run: 18, Failures: 3`

| Test | Kết quả | Nguyên nhân (đã xác minh) |
|---|---|---|
| `adminCanEnterAdminRouteAndHrCannot` (SecurityFlowTests.java:208) | `Status expected:<404> but was:<500>` | `/admin/ai-configuration` không có controller → `NoResourceFoundException` → bị `Exception.class` bắt → 500. |
| `internalFormCannotSubmitCandidateRoleOrEditCandidateIdentity` (:333) | `Status expected:<404> but was:<500>` | Service ném `ResponseStatusException(NOT_FOUND)` → bị `Exception.class` bắt → 500. Code thật cũng ném kiểu này: `AccountListService.java:125,128`, `AccountManagementService.java:68,135`. |
| `accountListRendersCandidateAccountWithoutPasswordHash` (:280) | `Status expected:<200> but was:<500>` | Chạy lại với log DEBUG: `Resolved [NoResourceFoundException: No static resource admin/candidate-accounts.] to ModelAndView [view="error/500"]`. Hai lỗi chồng nhau: (1) `CandidateAccountController` **không có** trong slice `@WebMvcTest` (SecurityFlowTests.java:64-66); (2) handler biến 404 thành 500. |

**CareerReadOnlyTests:** **không chạy được** — lỗi compile (`cannot find symbol: class CareerService`, CareerReadOnlyTests.java:24). Đây cũng là `@SpringBootTest` nên kể cả sửa compile vẫn cần DB.

**Các test khác (PR8):**
- PASS: `MailEnvironmentBindingTests` (2), `RequisitionReviewWebProbeTests` (2), `AccountListServiceTests` (2), `AccountManagementServiceTests` (7), `ApiMonitoringServiceTests` (3).
- ERROR do DB tắt (**chưa xác minh**): `RmsApplicationTests`, `AccountListQueryTests`, `AccountManagementDatabaseTests`, `AuthenticationDatabaseTests` (3), `DashboardDatabaseTests` (4), `RequisitionReviewProbeTests` (8).

> [!WARNING]
> **Rủi ro do chính PR8 gây ra (suy luận từ code, chưa chạy được vì DB tắt):** `AuthenticationDatabaseTests.java:27` vẫn truyền
> `@SpringBootTest(properties = "APP_PASSWORD_RESET_SECRET=...")`. PR8 đổi `@Value` sang `${app.password.reset.secret:}`
> (`PasswordResetService.java:40`). Property inline **không** được relaxed-binding như biến môi trường thật, nên secret sẽ rỗng →
> 2 test reset mật khẩu nhiều khả năng sẽ fail khi bật DB. Biến môi trường OS `APP_PASSWORD_RESET_SECRET` (script `scripts/run-password-reset-local.ps1`) thì vẫn hoạt động.

---

## 2. Route — **ĐÚNG (không trùng)**, nhưng còn rác

Toàn bộ mapping liên quan (main = PR8):

| File:dòng | Mapping |
|---|---|
| `CareerPortalController.java:39` | `GET {"/", "/jobs"}` |
| `CareerPortalController.java:73` | `GET /jobs/{id}` |
| `CareerPortalController.java:81` | `GET /jobs/{id}/apply` |

- Không có `@RequestMapping` cấp class trên `CareerPortalController` → **không có** URL lặp kiểu `/jobs/jobs/{id}`.
- **Không có** controller nào map `/careers` hay `/public/jobs`. Không còn trùng `GET /jobs`.
- `grep "/jobs/jobs"` trong templates/static: 0 kết quả.

**Còn tồn tại:**
- Thư mục `templates/careers/` (`index.html`, `detail.html`, `apply.html`, `unavailable.html`, `fragments.html`) + `css/careers.css`, `js/careers.js` là **view mồ côi**: không controller nào trả `"careers/..."`. `careers/apply.html:11` còn POST tới `/jobs/{id}/apply` — không có `@PostMapping` → nếu ai đó dùng sẽ ra 405 → bị handler biến thành 500.
- `CareerPortalController.java:90` luôn ném `BaseBusinessException(..., "ITERATION_2_PENDING")` → trả **HTTP 200** + trang `error/500` (xem mục 1).

## 3. Spring Security — **ĐÚNG** (có 1 rule thừa)

[SecurityConfig.java](file:///d:/Coding_source/swp_project/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java):
- L45: `GET "/"` permitAll — khớp `CareerPortalController:39`.
- L47: `/jobs/*/apply` → `hasAuthority("ROLE_CANDIDATE")`, đặt **trước** rule permitAll → thứ tự đúng.
- L48: `GET /jobs, /jobs/**` permitAll — khớp `:39`, `:73`.
- L48: `/public/jobs`, `/public/jobs/**` permitAll — **rule thừa**, không có controller (Thấp).
- `/interviews/**` (nhánh của tôi) rơi vào L55 `anyRequest().authenticated()`; phân quyền HR đang kiểm tra thủ công trong controller.

## 4. Role — **ĐÚNG (khớp)**

| Nguồn | Giá trị |
|---|---|
| `database/seeds/seed_data.sql:21-26` | `System Admin`, `HR`, `Hiring Manager`, `Director`, `Interviewer`, `Candidate` |
| `RoleAuthorities.java:7, 19-27` | cùng 6 tên → `ROLE_SYSTEM_ADMIN`, `ROLE_HR`, `ROLE_HIRING_MANAGER`, `ROLE_DIRECTOR`, `ROLE_INTERVIEWER`, `ROLE_CANDIDATE` |
| `fragments/sidebar.html:42-47`, `dashboard/index.html:49-65` | so sánh `'System Admin'`, `'Director'`, `'Candidate'` |
| `DashboardService.java:48` | `case "Hiring Manager"` |
| Nhánh interview `interview/list.html:78` | `currentUser.role.roleName == 'Hiring Manager'` |

- `grep "HiringManager"` chỉ xuất hiện trong tên cột/field Java (`HiringManagerId`, `hiringManager`) — không phải điều kiện role.
- `grep "HIRING_MANAGER"` chỉ có ở `RoleAuthorities.java:22`. Không template nào dùng `hasRole('HIRING_MANAGER')` / `sec:authorize`.
- **Không tìm thấy chỗ không khớp.**

## 5. Cấu hình PR8 — **SAI một phần**

| Câu hỏi | Kết luận | Bằng chứng |
|---|---|---|
| `.gitignore` chặn `application-local.properties`? | **ĐÚNG** | `git check-ignore -v` → `.gitignore:93:application-local.properties` |
| Còn `.env`? | **SAI** — còn nhiều | `.env.example` vẫn ở root; `.gitignore:88-89` vẫn liệt kê `.env`; `README.md:56` "đổi tên `.env.example` thành `.env`"; `README.md:62` nói `application.properties` bị ignore (giờ đã bị track); `docs/testing/password-reset-test-setup.md:14,26-31` và `docs/tests/2026-09-29-auth-screens.md:22-26` vẫn hướng dẫn `APP_*` + `.env.example`; `scripts/run-password-reset-local.ps1:48,78-80` dùng biến `APP_*` (vẫn chạy được nhờ relaxed binding). |
| Dockerfile / docker-compose / CI | **Không có** trong repo | `git ls-files` không có `Dockerfile`, `*compose*`, `.github/` |
| Profile `local` kích hoạt bằng gì? | `spring.profiles.active=local` | `application.properties:8`. Không dùng `spring.config.import`. Nếu thiếu file local thì Spring bỏ qua (không lỗi). Lưu ý: dòng này cũng bật profile `local` cho **test**. |
| README đã hướng dẫn chưa? | **SAI — chưa** | `README.md:50-53` vẫn bảo tự tạo `application.properties` trên máy; không nhắc `application-local.properties.example`. |
| Mâu thuẫn mới | **SAI** | `application.properties:18` đặt `spring.jpa.hibernate.ddl-auto=update`, trong khi `README.md:52` yêu cầu `validate`. Với `update`, Hibernate có thể tự ALTER schema `RitirementManagement2` theo entity của từng nhánh. |
| Port | Ghi chú | PR8 cố định `server.port=8080`; README:53 nói local kiểm tra bằng `8082`. |

## 6. Quy chuẩn của Dũng — **SAI (còn nhiều chỗ chưa theo)**

> Theo `ARCHITECTURE_GUIDE.md:70-71`: code **mới** cấm try-catch trong Controller; code cũ được ghi chú "WIP, dọn sau". Vì vậy dưới đây phân biệt code cũ/mới.

**6.1 try-catch trong Controller (main):**

| File:dòng | Loại | Tự render view lỗi? |
|---|---|---|
| `TestDbController.java:101` | `catch (Exception e)` | Có (trang demo) — nên xóa cả controller demo |
| `PasswordRecoveryController.java:51` | `catch (RuntimeException)` | render lại form |
| `PasswordRecoveryController.java:83`, `RegistrationController.java:43,45`, `AccountController.java:81,83,118,120` | `AccountFieldException` / `DataIntegrityViolationException` | render lại form với lỗi field |
| `HealthController.java:31` | `DataAccessException` | trả JSON DOWN (hợp lý) |

Không còn chỗ nào trả `"error/..."` trực tiếp từ controller. Nhánh interview của tôi: `InterviewSchedulingController.java:143,228,256` (vi phạm quy chuẩn code mới).

**6.2 DTO còn là class thường (main):**
`admin/dto/ActivityLogResponse`, `auth/dto/ForgotPasswordRequest`, `RegisterAccountRequest`, `ResetPasswordRequest`, `requisition/dto/ApprovalResponse`, `RequisitionRequest`, `RequisitionResponse`, `ScreeningCriteriaRequest`, `ScreeningCriteriaResponse`, `user/dto/CreateAccountRequest`, `UpdateAccountRequest`.
`dashboard/dto/DashboardView` là record nhưng **sai hậu tố** (không phải `...Response`). Chỉ `career/dto/*` đã chuẩn.
(Guide L52-55 ghi record là "khuyến khích", hậu tố `Request`/`Response` là bắt buộc.)

**6.3 Template còn CSS riêng / không dùng fragment chung:**

| Template | `<style>` | `style="..."` | Ghi chú |
|---|---|---|---|
| `requisitions/detail.html` | 1 | 24 | vi phạm guide L59 |
| `requisitions/form.html` | 1 | 19 | vi phạm |
| `requisitions/list.html` | 1 | 3 | vi phạm |
| `candidate/job-detail.html` | 0 | 10 | inline style; không dùng fragment head/header chung |
| `candidate/job-board.html` | 0 | 5 | như trên |
| `hello.html` | 0 | 6 | file demo |
| `careers/*.html` | 0 | 0 | mồ côi (mục 2) |

Ngoài `global.css` vẫn còn nhiều file CSS song song: `main.css`, `interface.css`, `design-tokens.css`, `app-layout.css`, `workspace.css`, `career-pages.css`, `careers.css`, `job-board.css`… → nguy cơ lệch style tiếp tục (chưa đánh giá chi tiết từng file).

## 7. Cấu trúc `docs/` và file rác — **SAI**

Do PR8 **không đụng `docs/`**, cấu trúc dưới đây là của `main` hiện tại:

- **Đúng:** `docs/architecture/`, `docs/database/`, `docs/management/work_logs/`, `docs/members/{dunglt,hoangnh,linhdn}/`.
- **Thiếu so với đề xuất:** **không có** `docs/prototype-reference/`; `prototype/` vẫn nằm ở root (4 file).
- **Trùng lặp:** cùng tồn tại `docs/testing/` (1 file) và `docs/tests/` (11 file + 273 ảnh asset); `docs/management/WORK_LOG.md` song song `docs/management/work_logs/`.
- **File lẻ ở root docs:** `docs/recruitment-homepage-design-system.md`.
- **File cá nhân trong chỗ chung:** `docs/archive/LINHDN_WORK_LOG.md`.
- **File nháp/AI trong chỗ chung `docs/design/`:** `*-assessment-a.md`, `*-assessment-b.md`, `*-impeccable-critique.md`, `2026-10-01-homepage-gemini-audit.md`, `2026-10-02-accounts-browser-b.json`, `2026-10-02-accounts-detector.json`, 3 ảnh `*-overlay.png` (và `docs/tests/2026-10-01-homepage-gemini-audit.md`, `2026-10-01-impeccable-init.md`). Đây là output của công cụ AI/đánh giá A-B, nên chuyển vào `members/<tên>/` hoặc `archive/`.
- `docs/members/duc/` hiện chỉ có trên máy tôi (nhánh interview), chưa có trên main.

---

## Bảng tóm tắt lỗi còn tồn tại

| # | Lỗi | Mức độ | Thuộc PR | Ai nên sửa |
|---|---|---|---|---|
| 1 | Test không compile (`CareerFlowTests`, `CareerReadOnlyTests` tham chiếu class đã xóa) → `mvnw test` đỏ trên main | **Cao** | PR7 | Linh / Dũng |
| 2 | `@ExceptionHandler(Exception.class)` nuốt `ResponseStatusException`, `NoResourceFoundException`, `AccessDeniedException`… → 500; không log | **Cao** | PR5 | Dũng (owner `core`) |
| 3 | Handler `BaseBusinessException` không set status → HTTP 200 cho lỗi nghiệp vụ/bảo mật | **Cao** | PR5/PR7 | Dũng |
| 4 | SecurityFlowTests thiếu `CandidateAccountController` trong `@WebMvcTest` | Trung bình | PR5 | Linh |
| 5 | Merge PR8 ghi đè `application.properties` cá nhân của cả nhóm (file đang bị ignore) | **Cao** (vận hành) | PR8 | Dũng thông báo trước; mọi người backup |
| 6 | `ddl-auto=update` trong file chung, trái README (`validate`) | **Cao** | PR8 | Dũng |
| 7 | README chưa cập nhật (vẫn `.env`, vẫn nói `application.properties` bị ignore, không nhắc file `.example`) | Trung bình | PR8 | Dũng |
| 8 | `AuthenticationDatabaseTests:27` vẫn dùng key `APP_PASSWORD_RESET_SECRET` (chưa xác minh bằng chạy test) | Trung bình | PR8 | Dũng |
| 9 | `.env.example`, docs `testing/`, `tests/` còn hướng dẫn cách cũ | Thấp | PR8 | Dũng |
| 10 | PR8 mô tả "cấu trúc lại docs" nhưng không có thay đổi; docs còn trùng `testing/` vs `tests/`, file AI nháp trong `design/`, thiếu `prototype-reference/` | Trung bình | PR8 | Dũng |
| 11 | View mồ côi `templates/careers/*` + `careers.css/js` | Thấp | PR7 | Linh / Dũng |
| 12 | Rule thừa `/public/jobs/**` trong SecurityConfig | Thấp | PR7 | Dũng |
| 13 | DTO class thường / sai hậu tố (11 file + `DashboardView`) | Thấp (code cũ) | — | Linh (auth/user/admin/dashboard), Hoàng (requisition) |
| 14 | `<style>`/inline style trong `requisitions/*.html`, `candidate/*.html` | Trung bình | — | Hoàng (requisition), Linh/Dũng (candidate) |
| 15 | Demo `TestDbController`, `TestWebController`, `hello.html` còn trong code | Thấp | — | Dũng |

## Kết luận về PR8

**Chưa nên approve ngay — đề nghị "Request changes" nhỏ.**

- PR8 **không làm tình trạng test tệ hơn** main (cùng 3 failure SecurityFlowTests, cùng lỗi compile kế thừa PR7). Phần code (đổi key `@Value`) đúng hướng.
- Nhưng trước khi merge, Dũng nên:
  1. Đổi `ddl-auto` về `validate` (hoặc `none`) trong `application.properties` chung, để `update` (nếu cần) vào `application-local.properties.example`.
  2. Cập nhật `README.md` mục 2.2–2.3 & dòng 62: hướng dẫn copy `application-local.properties.example` → `application-local.properties`; bỏ hướng dẫn `.env`; xóa `.env.example` (hoặc ghi rõ đã bỏ).
  3. Sửa `AuthenticationDatabaseTests.java:27` sang `app.password.reset.secret=...`.
  4. Hoặc thêm commit cấu trúc lại `docs/` như mô tả, hoặc sửa mô tả PR cho đúng (chỉ là refactor config).
  5. **Thông báo cả nhóm backup `src/main/resources/application.properties` trước khi pull main**, sau đó chuyển mật khẩu sang `application-local.properties`.
- Lỗi #1–#3 (test không compile, GlobalExceptionHandler) **không thuộc PR8** nhưng làm `main` đỏ; nên có PR riêng (Dũng/Linh) ngay sau PR8, trước khi các nhánh tính năng merge.

## Việc cần làm cho nhánh `feature/iter1-interview-schedule_duc` sau khi main có PR8

Kết quả thử merge (worktree nháp, `git merge --no-commit` PR8 vào nhánh interview):

1. **Backup** `src/main/resources/application.properties` cá nhân trước khi merge; sau merge tạo `application-local.properties` từ file `.example`, chỉ chứa mật khẩu.
2. **Giải quyết 1 conflict thật:** `CONFLICT (modify/delete): src/main/java/com/group2/rms/interview/InterviewSchedule.java` — nhánh mình đã chuyển sang `interview/entity/InterviewSchedule.java`, main chỉ đổi import. → `git rm` file cũ ở `interview/`, giữ bản trong `entity/`.
3. **Sửa import (lỗi compile sau merge, đã xác minh):** `com.group2.rms.candidate.Application` → `com.group2.rms.candidate.entity.Application`; `com.group2.rms.candidate.ApplicationRepository` → `com.group2.rms.candidate.repository.ApplicationRepository` tại:
   - `interview/controller/InterviewSchedulingController.java:3-4`
   - `interview/dto/InterviewScheduleResponse.java:3`
   - `interview/entity/InterviewSchedule.java:3`
   - `interview/service/InterviewSchedulingServiceImpl.java:3-4`
   - `src/test/.../interview/InterviewSchedulingServiceTests.java:3-4`

   Sau khi sửa trong bản nháp: `compile` OK; `InterviewSchedulingServiceTests: Tests run: 5, Failures: 0`; các test khác giữ nguyên kết quả như PR8 (3 fail SecurityFlowTests, test DB lỗi kết nối).
4. **Theo quy chuẩn exception (ARCHITECTURE_GUIDE L70):** cho `InterviewStatusException` kế thừa `BaseBusinessException` thay vì `RuntimeException`; bỏ 3 khối try-catch ở `InterviewSchedulingController.java:143,228,256`. *Lưu ý:* hiện `GlobalExceptionHandler` chưa có cách đưa lỗi nghiệp vụ về lại form (chỉ render `error/500` với status 200) — nên **chờ/đề nghị Dũng bổ sung** trước khi gỡ try-catch, nếu không UX form sẽ tệ hơn.
5. **DTO:** `InterviewScheduleRequest` còn là class thường — cân nhắc chuyển sang record (guide chỉ "khuyến khích"; nếu dùng `@Valid` + `th:field` thì class vẫn chấp nhận được).
6. **Template:** `interview/list.html`, `form.html` có link `global.css` nhưng **không dùng fragment chung** (`fragments/head`, `sidebar`, `workspace-header`) và tự dựng topbar riêng; `list.html:78` hardcode `href="/requisitions"` (nên dùng `th:href="@{/requisitions}"`). Nên chuyển sang fragment chung để thống nhất với dashboard.
7. Chạy lại `./mvnw test` **khi đã bật SQL Server** để xác minh phần test DB mà báo cáo này chưa kiểm được.

---

### Phụ lục — lệnh đã chạy (tóm tắt)

```text
git fetch --all --prune
git diff --stat origin/main...origin/dunglt/chore/refactor-config-docs
git worktree add --detach <scratch>/wt-pr8  origin/dunglt/chore/refactor-config-docs
git worktree add --detach <scratch>/wt-main origin/main
mvnw -q -B test            (PR8)   -> testCompile FAILED (CareerController/CareerService)
mvnw -q -B test-compile    (main)  -> FAILED, cùng lỗi
mvnw -B test               (PR8, đã xóa tạm 2 test hỏng trong worktree nháp)
mvnw test -Dtest=SecurityFlowTests#accountListRendersCandidateAccountWithoutPasswordHash
          -Dlogging.level.org.springframework.web.servlet.mvc.method.annotation=DEBUG
git merge-tree --write-tree --name-only feature/iter1-interview-schedule_duc <PR8>
git worktree add <scratch>/wt-merge feature/iter1-interview-schedule_duc
  + git merge --no-commit --no-ff <PR8> (chỉ trong worktree nháp)
Get-Service *SQL*  -> MSSQLSERVER Stopped
```
------------------------------------------------------------------------
### Trả lời từ Sonnet 5.5: ###
Mình hiểu rồi. Loại phần test ra thì báo cáo nhẹ đi nhiều, nhưng kết luận chính không đổi: nên nhờ Dũng sửa vài điểm trước khi approve PR8. Mình chỉ dựa vào file báo cáo bạn dán, chưa tự chạy lại gì.

## Phần bỏ qua được vì chỉ liên quan test
- 2 test không compile (`CareerFlowTests`, `CareerReadOnlyTests`).
- 3 test fail trong `SecurityFlowTests`, kể cả lỗi thiếu `CandidateAccountController` trong `@WebMvcTest`.
- Rủi ro `AuthenticationDatabaseTests` dùng key `APP_PASSWORD_RESET_SECRET`.
- Việc chạy lại `./mvnw test` khi bật SQL Server.

**Một lưu ý nhỏ:** nếu chạy app bằng `mvnw spring-boot:run` thay vì nút Run trong IDE, test không compile có thể chặn luôn việc khởi động. Mình nhớ plugin này chạy `test-compile` trước, nhưng chưa kiểm chứng. Cách tránh đơn giản là xóa 2 file test hỏng đó. `-DskipTests` không giúp, vì nó vẫn compile test. Sau khi merge, file `InterviewSchedulingServiceTests` của bạn cũng cần sửa 2 dòng import, nếu không nó sẽ làm hỏng compile theo cách tương tự. Hoặc bạn xóa file đó nếu nhóm không định viết test.

## Phần vẫn còn dù bỏ test
1. **`GlobalExceptionHandler` là lỗi của code chạy thật, không phải của test.**
   - `@ExceptionHandler(Exception.class)` nuốt `ResponseStatusException`, nên mọi lỗi 404 trong `AccountListService` và `AccountManagementService` thành 500.
   - `NoResourceFoundException` (gõ sai URL) cũng thành 500, và handler không ghi log.
   - Handler của `BaseBusinessException` trả HTTP 200.
   - Lỗi này thuộc PR5/PR7, không phải PR8, nên cần PR riêng của Dũng.
2. **`ddl-auto=update` trong `application.properties` chung** trái với README (`validate`). Hibernate có thể tự ALTER schema theo entity của từng nhánh. Đây là điểm quan trọng nhất của PR8.
3. **Merge PR8 sẽ ghi đè `application.properties` cá nhân của mọi người**, vì file đó đang bị ignore trên máy từng người. Cả nhóm cần backup trước khi pull.
4. **README chưa cập nhật.** Nó vẫn nói `.env`, vẫn nói `application.properties` bị ignore, và không nhắc `application-local.properties.example`. `.env.example` cũng còn trong repo.
5. **Mô tả PR8 nói có cấu trúc lại `docs/` nhưng thực tế không có thay đổi nào trong `docs/`.** Dũng đã hẹn sẽ đẩy phần docs lên PR8 sau khi mọi người góp ý, nên có thể anh ấy chưa kịp làm.

Mấy kết luận "conflict ẩn" từ lần Gemini phân tích đầu (trùng route `/jobs`, thiếu `permitAll` cho `/careers`, role `HiringManager` viết liền) đã được báo cáo kiểm tra và hầu hết không còn đúng trên main hiện tại. Đừng nhắc lại chúng trong nhóm.

## Về PR8
Mình nghiêng về chọn **Request changes nhẹ** thay vì approve ngay. Điều kiện bắt buộc chỉ có 2 cái: đổi `ddl-auto` về `validate` hoặc `none`, và thông báo cả nhóm backup trước khi pull. README và docs có thể sửa ngay trong PR hoặc làm PR sau. Bạn có thể comment như sau:

> PR8 hướng đi ổn. Trước khi merge, nhờ Dũng: (1) đổi `spring.jpa.hibernate.ddl-auto` trong `application.properties` về `validate` (README đang yêu cầu `validate`), nếu cần `update` thì để trong `application-local.properties.example`; (2) báo cả nhóm backup `application.properties` cá nhân trước khi pull vì file này sẽ bị track và ghi đè; (3) cập nhật README mục config (bỏ `.env`, thêm hướng dẫn copy file `.example`) và xóa `.env.example`; (4) mô tả PR nói có sửa docs nhưng diff chưa có, nhờ cập nhật mô tả hoặc thêm commit. Ngoài PR8: `GlobalExceptionHandler` đang nuốt `ResponseStatusException` và `NoResourceFoundException` thành 500, và lỗi nghiệp vụ trả HTTP 200, nên cần một PR riêng.

## Việc cho nhánh interview sau khi PR8 vào main
1. Backup `application.properties`, rồi sau khi pull tạo `application-local.properties` từ file `.example`. Trên PowerShell dùng `Copy-Item`, không dùng `cp`.
2. Merge main và xử lý 1 conflict thật: xóa `interview/InterviewSchedule.java` cũ, giữ bản trong `entity/`.
3. Sửa import `Application` và `ApplicationRepository` ở 5 file (danh sách có trong báo cáo, mục "Việc cần làm cho nhánh interview").
4. Đổi `InterviewStatusException` sang kế thừa `BaseBusinessException`. **Chưa gỡ try-catch** ở `InterviewSchedulingController` (dòng 143, 228, 256) cho đến khi handler có cách đưa lỗi về lại form, nếu không UX tệ hơn.
5. Chuyển template `interview/*.html` sang dùng fragment chung và sửa `href="/requisitions"` thành `th:href="@{/requisitions}"`.
6. Đặt báo cáo và mọi file nháp trong `docs/members/duc/`, hoặc không push.


## Hai giá trị `ddl-auto` khác nhau thế nào

Thuộc tính này quyết định Hibernate làm gì với schema DB mỗi lần app khởi động:

| Giá trị | Hành vi |
|---|---|
| `validate` | Chỉ so entity với schema DB. Nếu lệch (thiếu bảng, thiếu cột, sai kiểu) thì **app không khởi động** và báo lỗi. Không sửa gì trong DB. |
| `update` | Tự động **tạo bảng, thêm cột** cho khớp entity. Không xóa cột cũ. |
| `none` | Không làm gì. |

Theo báo cáo, `application.properties:18` của PR8 đặt `update`, còn `README.md:52` yêu cầu `validate`. Hai chỗ mâu thuẫn nhau.

## Vì sao `update` là vấn đề trong dự án của bạn

Dự án có thư mục `database/` chứa script `.sql` (cả `seed_data.sql`), nghĩa là **script SQL được coi là nguồn chuẩn của schema**. `update` phá điều đó:

1. **Mỗi nhánh tự "sửa" DB của người chạy nó.** Nhánh interview của bạn có entity `InterviewSchedule`. Nếu bạn chạy với `update`, Hibernate tự tạo bảng này trong DB local của bạn, dù bạn chưa viết script SQL cho nó. Nhánh khác thêm cột vào entity khác thì DB của người đó cũng bị ALTER theo. Hai máy chạy cùng một commit nhưng DB có thể khác nhau.
2. **Che lỗi thiếu script.** Với `update`, app vẫn chạy bình thường trên máy bạn. Khi người khác tạo DB từ script `.sql` thì thiếu bảng hoặc cột, và chỉ lúc đó mới phát hiện. Với `validate`, lỗi hiện ra ngay trên máy bạn, trước khi push.
3. **Bảng do Hibernate sinh ra không giống script.** Tên constraint, index, kiểu dữ liệu (ví dụ `nvarchar` hay độ dài cột) có thể khác script gốc. Schema local dần lệch khỏi schema nhóm, và rất khó truy ra lý do.
4. **`update` không xóa hay sửa kiểu cột an toàn.** Đổi tên field trong entity sẽ để lại cột cũ và thêm cột mới, DB tích rác dần.

## Mức độ nghiêm trọng thực sự

Mình đã gọi đây là điểm quan trọng nhất của PR8, nhưng cần chỉnh lại cho đúng mức:

- Báo cáo cho thấy SQL Server chạy **trên máy mỗi người** (`MSSQLSERVER` local). Nếu mỗi người dùng DB riêng thì hậu quả là **schema local bị lệch dần**, chưa đến mức hỏng dữ liệu chung.
- Nếu cả nhóm dùng chung một DB (mình không biết, báo cáo không nói), thì `update` nguy hiểm hơn nhiều, vì mỗi nhánh có thể ALTER DB của người khác.
- Điểm nặng là PR8 đưa giá trị này vào **file chung được track**, nên nó thành mặc định cho cả nhóm. Trước đó mỗi người tự đặt trong file cá nhân.

## Cách sửa

- Trong `application.properties` chung: `spring.jpa.hibernate.ddl-auto=validate` (hoặc `none`).
- Ai thật sự muốn thử nhanh với `update` thì đặt trong `application-local.properties` (file cá nhân, bị gitignore). File này nạp sau nên sẽ ghi đè giá trị chung.

## Ảnh hưởng tới nhánh interview của bạn

Nếu Dũng đổi sang `validate`, bảng `InterviewSchedule` (và bất kỳ bảng nào bạn thêm) **phải có trong script SQL ở `database/`**, nếu không app sẽ không khởi động. Bạn nên kiểm tra nhánh mình đã có script tạo bảng đó chưa. Việc này cũng là cách đúng để nhóm quản lý schema, nên nên làm cùng lúc.

Bạn có muốn mình soạn đoạn comment ngắn gửi Dũng riêng về điểm này không?

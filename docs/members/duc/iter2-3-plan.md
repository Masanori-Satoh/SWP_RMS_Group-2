# Kế hoạch Iter 2 + Iter 3 (DucNM, P4) — bản đã validate với code

> Ngày validate: 2026-10-11, trên `main` @ `332b5b5` (sau PR #34).
> File này **bổ sung và sửa** [`iter2-3-prompts.md`](iter2-3-prompts.md). KHỐI NỀN trong file đó vẫn áp dụng; chỗ nào hai file khác nhau thì **theo file này**.
> Cách dùng giữ nguyên: mỗi bước một session, gõ
> `Đọc @docs/members/duc/iter2-3-prompts.md và @docs/members/duc/iter2-3-plan.md. Áp dụng KHỐI NỀN (đã sửa theo plan) và chỉ thực hiện BƯỚC n.`
> Rủi ro conflict với đồng đội: xem [`iter2-3-conflict-risk.md`](iter2-3-conflict-risk.md).

---

## 1. Kết quả validate: cái gì đúng, cái gì lệch

### 1.1. Đúng với code (giữ nguyên)

| Nội dung trong prompts | Kiểm chứng |
|---|---|
| Entity `InterviewEvaluation`, `InterviewFinalResult` đã có, không tạo lại | Có, và **khớp `db.sql`**: tên cột, kiểu, `NVARCHAR(MAX)`, LAZY, `Recommendation` là `String` nên không vướng CHECK `Hire/No_Hire/Consider` |
| `InterviewEvaluationRepository` chưa có | Đúng, phải tạo |
| `db.sql` **không có** `UNIQUE (InterviewId, InterviewerId)` và index cho `InterviewEvaluation` | Đúng (`db.sql` dòng 418–448) |
| `InterviewStatus` lưu giá trị đúng như DB (`Scheduled`, `Completed`...) qua getter/setter tự viết | Đúng (`InterviewSchedule.java` dòng 92–109); `RoleInPanel` dùng `@Enumerated(STRING)` nhưng tên enum trùng giá trị DB nên an toàn |
| GBR-01 nằm ở `InterviewStatus.canTransitionTo` + `InterviewSchedule.transitionTo` | Đúng |
| GBR-05 đã có query `findByIdAndAssignedInterviewer`, `existsById_InterviewIdAndId_InterviewerId` | Đúng, **tái dùng**, không cần thêm method kiểm tra thành viên |
| Rule GET `/interviews/**` cho HR, SA, Director, HM, Interviewer; ghi chỉ HR, SA | Đúng (`SecurityConfig.java` dòng 110–116) |
| `offer` đọc `InterviewFinalResult` qua `findAllPassedWithDetails`, `findByApplicationIdOrderByApprovedAtDesc`, builder trong `OfferServiceTests` | Đúng, **không đổi** entity/builder |
| Không ai ghi `InterviewFinalResult`/`InterviewEvaluation` trong code chính | Đúng (chỉ test của offer dùng builder) → D6 giữ nguyên: chỉ hiển thị nếu có |

### 1.2. Lệch so với prompts (phải sửa kế hoạch)

| # | Prompts nói | Thực tế | Sửa thành |
|---|---|---|---|
| L1 | "Controller đặt `activeMenu` theo đúng giá trị `InterviewSchedulingController` đang dùng" | Controller iter1 **không** đặt `activeMenu`; `list.html` vẫn dùng khung cũ `fragments/sidebar :: workspace('interviews', ...)` | Trang mới đặt `model.addAttribute("activeMenu", "interviews")`, giá trị mà cả 4 sidebar đang so khớp |
| L2 | Lấy người dùng qua `CurrentUserService` | Đúng chuẩn, nhưng controller iter1 dùng `resolveEffectiveUser()` **có fallback lấy đại user HR khi chưa đăng nhập** | Controller mới chỉ dùng `CurrentUserService.requireUser()`, **không** copy `resolveEffectiveUser` |
| L3 | AJAX "bắt chước đúng mẫu đổi mật khẩu `UserProfileController`" + trả `ApiResponse` | `UserProfileController` trả `ResponseEntity<Map>`, không phải `ApiResponse`. Còn `GlobalExceptionHandler` (dòng 191–213) chỉ trả JSON cho lỗi `@Valid` khi URL chứa `/api/`, nên `/interviews/{id}/evaluations` sẽ nhận **trang HTML `error/500`** | Controller nhận `@Valid @RequestBody SubmitEvaluationRequest req, BindingResult errors` để tự xử lý lỗi validation, trả `ApiResponse<Map<String,String>>(false, msg, fieldErrors)` + 400. Không đụng Global |
| L4 | D3: lịch → `Completed` khi **tất cả** panel đã nộp | `DashboardMetricsRepository.pendingInterviewEvaluations` (LinhDN) đếm "chờ đánh giá" = lịch **đã `Completed`** mà chưa có phiếu. Nếu theo D3 thì chỉ số này luôn bằng 0 | **D3'** (xem mục 2) |
| L5 | D1: "mọi `RoleInPanel`" | Service iter1 **cấm HR vào panel** (HR Isolation); thực tế panel chỉ có `Interviewer`, `HM`, `Director` | Rule POST mới cho `ROLE_INTERVIEWER`, `ROLE_HIRING_MANAGER`, `ROLE_DIRECTOR`; HR không bao giờ thấy nút "Đánh giá" |
| L6 | Bước 3 thêm rule cho cả `GET /interviews/*/result` | Rule GET hiện tại **đã bao** route này với đúng các role của D5 | Chỉ thêm **một** rule: `POST /interviews/*/evaluations`, đặt trước dòng 115 |
| L7 | `transitionTo` + "ném `InterviewStatusException` nếu vi phạm" | `transitionTo` ném `IllegalStateException` | Kiểm tra `canTransitionTo` trước rồi mới gọi, giống `cancelSchedule` (`InterviewSchedulingServiceImpl.java` dòng 261–267) |
| L8 | `InterviewFinalResultRepository`: "thêm tìm theo lịch nếu chưa có" | Đã có `findByInterviewSchedule_InterviewId(**Integer**)`, nhưng `InterviewSchedule.interviewId` là **`Long`** nên sẽ lỗi kiểu khi gọi. Hiện chưa ai gọi | **Thêm** method mới `Optional<InterviewFinalResult> findByScheduleId(Long)` (`@Query` + `JOIN FETCH hiringManager`), **không sửa/xóa** method cũ |
| L9 | Tùy chọn Notifications: "chỉ gọi API sẵn có của `notification`" | `NotificationService` chỉ có `notifyRequisitionRejected`; không có API chung. HoangNH đang thêm `notifyJobPostingPublished` trên nhánh khác | **Bỏ khỏi iter này** (ghi vào worklog). Muốn làm thì phải xin HoangNH/Lead thêm một method chung trước |
| L10 | Bước 4 ghi log vào `docs/management/logs/` | Thư mục thật là `docs/management/work_logs/` | Dùng `work_logs/YYYY-MM-DD-ten.md` + 1 dòng trong `WORK_LOG.md` (Accept Both khi conflict) |
| L11 | Section `db.sql` "3.14–3.17" | `db.sql`: Evaluation = 3.16, FinalResult = 3.17; javadoc entity ghi 3.17/3.18 | Chỉ là chú thích. Đừng sửa javadoc (tránh diff thừa) |
| L12 | — | `list.html` đã có tab trỏ tới `/interviews/evaluation` (dòng 49), **route này không tồn tại** (404) | Không làm route này. Ghi "Điểm lệch" vào worklog; xử lý ở mục 4 (D7) |
| L13 | — | Controller iter1 cho Director tạo/sửa lịch, nhưng `SecurityConfig` chặn Director ở `/interviews/new`, `/*/edit` | Ngoài phạm vi iter2/3. Chỉ ghi vào worklog |

---

## 2. Quyết định (đã chỉnh sau validate)

| Mã | Quyết định | Trạng thái |
|---|---|---|
| D1' | Người được nộp: thành viên `InterviewPanel` của lịch (thực tế role Interviewer / Hiring Manager / Director), mỗi người **một phiếu** cho mỗi lịch | Mặc định |
| D2' | Được nộp khi `now >= StartTime` và trạng thái là `Scheduled`, `Rescheduled` hoặc `Completed`; `Cancelled` thì chặn. Iter3 chỉ nộp mới, không sửa phiếu | Mặc định |
| D3' | **Phiếu đầu tiên** của lịch đang `Scheduled`/`Rescheduled` → chuyển lịch sang `Completed` (buổi phỏng vấn coi như đã diễn ra). Các thành viên còn lại sẽ hiện trong "chờ đánh giá" ở dashboard, đúng với định nghĩa hiện có của LinhDN | **Cần bạn xác nhận** (thay D3 cũ) |
| D4' | Từng điểm 1–5 được bỏ trống (vd HR chỉ chấm kỹ năng mềm / văn hóa), nhưng phải chấm ít nhất 1 mục; `recommendation` bắt buộc (`Hire`/`No_Hire`/`Consider`); `strengths`/`weaknesses`/`comments` tùy chọn, `@Size(max = 2000)` | Đã chốt (thay D4) |
| D5 | HR, HM, Director, SA xem mọi phiếu + trung bình. Interviewer chỉ vào được lịch mình được gán (GBR-05) và chỉ thấy phiếu của mình cho đến khi có Final Result | Giữ |
| D6 | Không thêm chức năng ghi `InterviewFinalResult`; trang Result chỉ hiển thị nếu có bản ghi | Giữ |
| D7 | Tab chết `/interviews/evaluation` trong `list.html`: **giữ nguyên** ở iter này (sửa = đổi markup cũ). Nút "Kết quả" và "Đánh giá" thêm trên từng thẻ lịch | Mặc định |
| D8 | Trùng phiếu: Service kiểm tra `existsBy...` (đủ cho iter3). Ràng buộc `UNIQUE` trong DB **chưa thêm**: soạn sẵn SQL trong worklog, chờ bạn duyệt | **Cần bạn xác nhận** nếu muốn thêm |
| D9 | Không cập nhật `Application.applicationStatus` sau khi đánh giá (thuộc `candidate` của DungLT) | Mặc định |

---

## 3. Danh sách file

**Tạo mới (trong allowlist):**
- `interview/repository/InterviewEvaluationRepository.java`
- `interview/dto/SubmitEvaluationRequest.java`, `InterviewEvaluationResponse.java`, `InterviewResultDetailResponse.java`
- `interview/exception/InterviewEvaluationException.java` (có `field`)
- `interview/service/InterviewEvaluationService.java` + `InterviewEvaluationServiceImpl.java`
- `interview/service/InterviewResultService.java` + `InterviewResultServiceImpl.java`
- `interview/controller/InterviewResultController.java`, `InterviewEvaluationController.java`
- `templates/interview/result.html`, `templates/interview/evaluation-dialog.html`
- `static/css/pages/interview-result.css`, `static/js/pages/interview-evaluation.js`
- `test/.../interview/InterviewEvaluationServiceTests.java`, `InterviewResultServiceTests.java`, `SubmitEvaluationRequestTests.java`, `InterviewResultWebTests.java`
- `docs/members/duc/iter2-3-worklog.md`, `docs/management/work_logs/2026-10-xx-interview-result-evaluation.md`

**Sửa (chỉ thêm):**
- `interview/repository/InterviewFinalResultRepository.java`: thêm `findByScheduleId(Long)` (L8)
- `templates/interview/list.html`: thêm 2 link trên thẻ lịch, không inline style mới, không đổi biến
- `core/config/SecurityConfig.java` (**ngoại lệ, cần duyệt**): +1 rule (L6)
- `docs/management/WORK_LOG.md`: +1 dòng

**Không đụng:** `GlobalExceptionHandler`, `notification/**`, `offer/**`, `candidate/**`, `dashboard/**`, `db.sql`/migrations (trừ khi duyệt D8), `fragments/**`, `layout/**`, sidebar.

---

## 4. Các bước (thay phần BƯỚC 1–4 của prompts ở các điểm sau)

### BƯỚC 1 — Tầng dữ liệu (nhỏ hơn dự kiến)
Recon đã xong ở mục 1, session chỉ cần đọc lại để xác nhận rồi làm:
1. Tạo `InterviewEvaluationRepository`:
   - `boolean existsByInterviewSchedule_InterviewIdAndInterviewer_UserId(Long, Integer)`
   - `@Query ... JOIN FETCH e.interviewer WHERE e.interviewSchedule.interviewId = :id ORDER BY e.evaluatedAt` → `findByScheduleIdWithInterviewer(Long)`
   - `long countByInterviewSchedule_InterviewId(Long)`
2. `InterviewFinalResultRepository`: thêm `findByScheduleId(Long)` có `LEFT JOIN FETCH r.hiringManager`.
3. **Không** sửa entity. **Không** đụng `InterviewPanelRepository` (đã đủ: `findByInterviewIdWithInterviewer`, `existsById_InterviewIdAndId_InterviewerId`).
4. Soạn SQL cho D8 vào worklog (chưa ghi `db.sql`):
   ```sql
   ALTER TABLE InterviewEvaluation ADD CONSTRAINT UQ_InterviewEvaluation_Interview_Interviewer UNIQUE (InterviewId, InterviewerId);
   ```
   Số migration: **xem `iter2-3-conflict-risk.md` mục migration** (nhánh HoangNH đã chiếm 006, 007).
5. Tạo worklog. Xong khi: `./mvnw -q -DskipTests compile` OK.

### BƯỚC 2 — DTO, Exception, Service
Như prompts, kèm các điểm sau:
- `InterviewEvaluationServiceImpl.submit(Long interviewId, SubmitEvaluationRequest req, Integer userId)`, `@Transactional`:
  1. `interviewScheduleRepository.findByIdWithDetails(id)`, không có thì `ResourceNotFoundException`
  2. `!panelRepo.existsById_InterviewIdAndId_InterviewerId(id, userId)` → `AccessDeniedException`
  3. `Cancelled` hoặc `now < startTime` → `InterviewEvaluationException`
  4. Đã có phiếu → `InterviewEvaluationException`
  5. Lưu phiếu (`recommendation` lưu đúng chuỗi `Hire`/`No_Hire`/`Consider`)
  6. D3': nếu trạng thái là `Scheduled`/`Rescheduled` thì kiểm tra `canTransitionTo(Completed)` rồi gọi `transitionTo` (L7)
- `InterviewResultServiceImpl.getResult(Long interviewId, User viewer)`, `@Transactional(readOnly = true)`:
  - Interviewer: dùng `findByIdAndAssignedInterviewer`, không có thì `AccessDeniedException` (**không** ném `InterviewStatusException` như `getMyScheduleDetailForInterviewer`, vì cái đó sẽ ra trang 500)
  - Role khác: `findByIdWithDetails`
  - Map sang DTO trong Service. `canEvaluate` = là thành viên panel + chưa nộp + D2'
- Recommendation nên là `enum EvaluationRecommendation { Hire, No_Hire, Consider }` trong `interview/entity/` (tên trùng giá trị DB) và dùng ở **DTO**; entity giữ `String` để không đổi cột.
- Test Mockito theo `InterviewSchedulingServiceTests`. Thêm case: phiếu đầu → `Completed`; lịch đã `Completed` thì vẫn nộp được, không gọi transition.

### BƯỚC 3 — Controller, Security, View
- `InterviewResultController` `GET /interviews/{id}/result` → `interview/result`, model `result`, `activeMenu = "interviews"`.
- `InterviewEvaluationController` `POST /interviews/{id}/evaluations` (consumes/produces JSON):
  - Có `BindingResult` → `ApiResponse<>(false, "Vui lòng kiểm tra lại các ô.", fieldErrors)` + 400 (L3)
  - `catch (InterviewEvaluationException e)` → `ApiResponse<>(false, e.getMessage(), Map.of(e.getField(), ...))` + 400
  - Thành công → `ApiResponse<>(true, "Đã gửi đánh giá.", null)`
  - `AccessDeniedException` / `ResourceNotFoundException`: để Global xử lý (JS hiện thông báo chung khi nhận 403/404)
- `SecurityConfig`, chèn **ngay trước** dòng 115:
  ```java
  .requestMatchers(HttpMethod.POST, "/interviews/*/evaluations")
  .hasAnyAuthority("ROLE_INTERVIEWER", "ROLE_HIRING_MANAGER", "ROLE_DIRECTOR")
  ```
- View `result.html` dùng `layout:decorate="~{layout/internal}"` (trang mẫu: `notifications/list.html`). Dialog dùng markup `.confirm-dialog`/`.dialog-*` của `UI_CATALOG.md` mục 8, nhúng bằng `th:replace` từ `interview/evaluation-dialog.html`. JS lấy CSRF từ `meta[name="_csrf"]`/`_csrf_header` như `profile.js` dòng 329–332.
- `list.html`: thêm link `@{/interviews/{id}/result}` dùng class có sẵn (`button button-secondary button-sm` như link Sửa ở dòng 278), **không** thêm `style=`.
- Test web theo `SecurityFlowTests`/`UserProfileWebTests`: thêm case HR `POST` → 403 (HR không phải panel), Interviewer ngoài panel `GET result` → 403.

### BƯỚC 4 — Kiểm chứng + bàn giao
Như prompts, với L10 (`work_logs/`) và thêm các mục:
- Kiểm tra `DashboardMetricsRepository.pendingInterviewEvaluations` cho ra số đúng sau khi nộp phiếu đầu (D3').
- `git diff origin/main -- src/main/java/com/group2/rms/core/config/SecurityConfig.java` chỉ có +2 dòng.
- Trước khi mở PR: `git fetch` rồi xem lại `iter2-3-conflict-risk.md`.

---

## 5. Quyết định đã chốt (2026-10-11)

1. **D3' — ĐỒNG Ý**: phiếu đầu tiên chuyển lịch sang `Completed` (panel có thể không đến đủ).
2. **D8 — KHÔNG làm**: tuyệt đối không sửa `db.sql`/migration. Chặn trùng phiếu chỉ ở Service.
3. **SecurityConfig — DUYỆT** +2 dòng.
4. Nhánh `feature/hoangnh/jobposting` đã cũ, bỏ qua khi đánh giá conflict.
5. Nhánh làm việc: `feature/ducnm/iter2-interview_evaluation`. **Không tự commit.**

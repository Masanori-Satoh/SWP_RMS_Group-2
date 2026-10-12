# Bộ lệnh Claude Code — Iter 2 + Iter 3 (DucNM) — 4 bước

**Cách dùng**
1. Lưu file này vào repo: `docs/members/duc/iter2-3-prompts.md`.
2. Mỗi bước mở session mới (hoặc `/clear`), gõ đúng một dòng:
   `Đọc @docs/members/duc/iter2-3-prompts.md. Áp dụng KHỐI NỀN và chỉ thực hiện BƯỚC n.`
3. Đọc báo cáo cuối của Claude Code rồi mới sang bước sau. Các bước bàn giao qua `docs/members/duc/iter2-3-worklog.md`, không dựa vào lịch sử chat.

Thứ tự: **BƯỚC 1** Recon + Entity/Repository → **BƯỚC 2** DTO, Exception, Service → **BƯỚC 3** Controller, Security, View → **BƯỚC 4** Kiểm chứng + bàn giao.

---

## KHỐI NỀN (áp dụng cho mọi bước)

### 1. Phạm vi (SCOPE-LOCK)

Tôi là DucNM, phụ trách luồng "Interview scheduling & evaluation" (module `interview`). Iter1 (Interview Schedule List, Create/Update Schedule) đã xong. Trong project tracking, các dòng Iter2/Iter3 chưa ghi tên mà dùng P1–P5; tôi là **P4**:

| Màn hình | Plan | SRS | Use case |
|---|---|---|---|
| Interview Result Details | Iter2 | 5.1.28 | View Interview Result |
| Submit Evaluation Form (pop-up) | Iter3 | 5.1.29 | Submit Interview Evaluate |
| Notifications View | Iter3 | 5.1.39 | View Notification (chỉ phần liên quan phỏng vấn, làm sau cùng, tùy chọn) |

Nếu bạn thấy phạm vi này không đúng với repo hoặc tracking (ví dụ tôi là P5/offer, hoặc cả hai) thì **DỪNG & HỎI**, đừng tự đoán.

**Chỉ được sửa (allowlist):**
- `src/main/java/com/group2/rms/interview/**`
- `src/main/resources/templates/interview/**`
- `static/css/pages/interview-*.css`, `static/js/pages/interview-*.js` (và các file phẳng `static/css/interview-*`, `static/js/interview-*` của iter1)
- `src/test/java/com/group2/rms/interview/**`
- `docs/members/duc/**`

**Không phải của tôi — chỉ được ĐỌC/GỌI, cấm sửa:**
- Toàn bộ module `offer` (HuyenPT: Internal Offer Management, Create/Update Offer; P5: Offer Proposal Details, Candidate Offer Details, Negotiation Form), gồm `templates/offers`, `offers.css`, `offers.js`.
- `requisition` (HoangNH); `user`, `admin`, `dashboard`, API monitoring (LinhDN); `career`, `candidate`, profile (DungLT); `auth`; các màn hình Iter2/3 của P1, P2, P3.
- `core/**` (đặc biệt `GlobalExceptionHandler`, `core/web`), `layout/*`, `fragments/layout/*`, `tokens.css`, `components.css`, `global.css`, `interface.js` (vùng đỏ/vàng của `UI_RULES.md`).

**Ngoại lệ được chạm tối thiểu, phải liệt kê để tôi duyệt:** `core/config/SecurityConfig` (chỉ thêm rule), `database/schema/db.sql` + `database/migrations/` + `docs/database/model.md` (chỉ khi cần đổi schema, và phải hỏi trước), `fragments/layout/sidebars/<role>.html` (chỉ thêm menu).

### 2. Nguồn sự thật (ưu tiên giảm dần)

1. Code thực tế trong repo.
2. `database/schema/db.sql` trong repo (nếu khác bản khác thì theo repo).
3. `docs/architecture/ARCHITECTURE_GUIDE.md`, `docs/architecture/UI/*.md` (`UI_RULES`, `UI_CATALOG`, `UI_NEW_PAGE`, `UI_CSS_GUIDE`), `docs/database/model.md`.
4. Project tracking, `docs/members/**`, `docs/management/**`.
5. `RMS_SRS`, `RMS_SDS`: **bản cũ, chưa cập nhật theo nội dung từ 03/10/2026**, chỉ tham khảo ý nghĩa nghiệp vụ. Mã `GBR-xx`, `OR-xx` trong SRS phải kiểm chứng bằng code (đã thấy trong code iter1: GBR-01 = trạng thái chỉ đi tiến, GBR-05 = Interviewer chỉ thấy lịch được gán).

Khi các nguồn mâu thuẫn: theo nguồn ưu tiên cao hơn, ghi vào mục "Điểm lệch" của worklog, không tự "sửa cho khớp SRS".

### 3. Luật cứng (đã đối chiếu với ARCHITECTURE_GUIDE + UI_RULES; thắng mọi prompt cũ)

**Backend**
- Java 21, Spring Boot 3.5, SQL Server. `open-in-view=false`: method đọc `@Transactional(readOnly = true)`, map Entity → DTO **trong Service**; method ghi `@Transactional`. Không trả Entity LAZY ra Controller/View.
- Entity/Repository của `interview` và `offer` đã tồn tại. **Không tạo lại**: đối chiếu với `db.sql` rồi chỉ sửa chỗ lệch. `@ManyToOne(fetch = LAZY)`.
- Giá trị trong DB dạng `No_Hire`, `Online_GoogleMeet`, `Pending_Director`... (có CHECK constraint). **Không** gắn mù quáng `@Enumerated(EnumType.STRING)` lên enum tên `UPPER_CASE` (sẽ ghi `NO_HIRE` và vi phạm CHECK). Bắt chước đúng cách `InterviewSchedule` đang map `Format`/`Status`, hoặc dùng `AttributeConverter`. Không đổi giá trị trong DB.
- DTO nằm phẳng trong `interview/dto/` (không tạo `request/`, `response/`). `*Response` là `record`; `*Request` là `record` nếu đơn giản, `class` Lombok nếu cần default/danh sách lồng. Không dùng hậu tố `Form`/`Dto`.
- Lỗi nghiệp vụ: `interview/exception/{Ngữ cảnh}Exception extends BaseBusinessException` (thêm `field` nếu cần chỉ ô lỗi, mẫu `AccountFieldException`). Không tìm thấy: `core.exception.ResourceNotFoundException`. Không có quyền với bản ghi: `AccessDeniedException`. **Cấm** `IllegalArgumentException`, `IllegalStateException`, `EntityNotFoundException`, `ResponseStatusException` cho lỗi nghiệp vụ mới. **Không sửa `GlobalExceptionHandler`.**
- Controller chỉ `try-catch` **đúng exception của module** để render lại form/trả `ApiResponse(false, ...)`; không `catch (Exception)`; không nuốt lỗi. Mỗi thao tác chọn **một** cách: pop-up (Submit Evaluation) = AJAX (`@Valid @RequestBody` + `ApiResponse` + header `X-CSRF-TOKEN`, bắt chước đúng mẫu đổi mật khẩu trong `UserProfileController`/`profile.js`); trang xem = `GET`.
- Route nằm dưới `/interviews/**`. Mọi route mới phải có rule trong `core/config/SecurityConfig`. Rule hiện tại cho phép ghi `/interviews/**` chỉ HR/SYSTEM_ADMIN, nên rule mới phải đặt **trước** rule chung đó. Người dùng hiện tại lấy từ `CurrentUserService`. Dự án không dùng `@PreAuthorize`.
- `interview` được phụ thuộc `candidate`, `user`, `core`. **Cấm** import `com.group2.rms.offer.*` và cấm vòng phụ thuộc.
- Constructor injection: `@RequiredArgsConstructor` + `private final`. Test đặt tên `*Tests`.
- Đổi schema chỉ khi thật cần: sửa `db.sql` + migration + `model.md`, và phải hỏi trước. Không dựa vào `ddl-auto=update` (DB local có thể đã có cột thừa do nó).

**UI**
- `layout:decorate="~{layout/internal}"`, nội dung trong `<main id="main" layout:fragment="content" class="page-content container" tabindex="-1">`. Controller đặt `activeMenu` (dùng đúng giá trị mà `InterviewSchedulingController` đang dùng).
- Tra `UI_CATALOG.md` trước khi viết CSS. Nút: `.btn .btn--primary|secondary|outline|danger` (chữ nhật bo 8px, **không** pill, **không** `.button`). Form: `.form-group/.form-label/.form-input`. Bảng: `.table-wrapper/.table`. Card, badge, dialog, alert, phân trang: dùng class/fragment trong catalog.
- **Cấm:** `style="..."`, `<style>`/`<script>` nội tuyến, màu hex/rgb (dùng `var(--...)`), Bootstrap/Tailwind, định nghĩa lại class dùng chung, chép markup topbar/sidebar/dialog đăng xuất.
- CSS/JS riêng: `static/css/pages/<trang>.css`, `static/js/pages/<trang>.js`, class có tiền tố trang (`ir-` cho result, `iev-` cho evaluation), chỉ chứa phần catalog chưa có.
- Không đổi tên/dời template cũ, không đổi biến backend của trang cũ (`interview/list.html`, `interview/form.html` chỉ được THÊM nút/link).
- Thymeleaf: không đặt `th:if`/`th:each` cùng thẻ với `th:replace`.

### 4. Quyết định mặc định (chưa được xác nhận hết — cứ dùng, ghi vào worklog, tôi sẽ sửa nếu sai)

- **D1** Người được nộp đánh giá: thành viên `InterviewPanel` của lịch đó (mọi `RoleInPanel`), mỗi người một phiếu/lịch.
- **D2** Được nộp khi lịch ở `Scheduled`/`Rescheduled`/`Completed` và `now >= StartTime`; không nộp khi `Cancelled`; iter3 chỉ nộp mới, không sửa phiếu đã nộp.
- **D3** Lịch chuyển sang `Completed` khi **tất cả** thành viên panel đã nộp (không phải ngay sau phiếu đầu), qua phương thức chuyển trạng thái có sẵn của `InterviewSchedule` (GBR-01 chỉ đi tiến); kiểm tra hợp lệ trước khi gọi, ném `InterviewStatusException` nếu vi phạm.
- **D4** Ba điểm (`TechnicalScore`, `SoftSkillScore`, `CulturalFitScore`) bắt buộc nhập, 1–5 (DB cho null nhưng form bắt buộc); `Recommendation` bắt buộc (`Hire`/`No_Hire`/`Consider`); `Strengths`/`Weaknesses`/`Comments` tùy chọn. Tên trường theo `db.sql`.
- **D5** Xem Interview Result: HR, Hiring Manager, Director, System Admin thấy mọi phiếu + điểm trung bình; Interviewer chỉ thấy lịch được gán và chỉ thấy phiếu của chính mình cho đến khi có Final Result (GBR-05).
- **D6** `InterviewFinalResult` (quyết định Passed/Failed của Hiring Manager): nếu recon cho thấy chưa có chỗ nào ghi bảng này, **không** tự thêm chức năng quyết định; trang Result chỉ hiển thị nếu có bản ghi. Không đổi kiểu dữ liệu mà `offer` đang đọc (ví dụ `PassedCandidateResponse`).

### 5. Cách làm việc

- Làm trực tiếp, không viết kế hoạch dài chờ duyệt. Mỗi bước: recon (chỉ đọc) → báo cáo ≤ 20 dòng "điểm lệch + file sẽ tạo/sửa" → làm luôn → build/test → ghi worklog.
- **DỪNG & HỎI chỉ khi:** cần sửa file ngoài allowlist (trừ ngoại lệ tối thiểu ở mục 1), cần đổi schema, chức năng chồng với module của người khác, code thật khác hẳn mô tả ở đây, hoặc phạm vi mơ hồ.
- Không đổi nhánh, không `pull`/`merge`/`commit`/`push` trừ khi tôi yêu cầu.
- Cuối mỗi bước: kết quả build/test, `git status --short`, đối chiếu mọi file thay đổi với allowlist, và append ≤ 15 dòng vào `docs/members/duc/iter2-3-worklog.md` (quyết định, điểm lệch, việc còn lại).

---

## BƯỚC 1 — Recon + Entity & Repository

**Mục tiêu:** nắm đúng hiện trạng, chốt phạm vi, và làm cho tầng dữ liệu của `interview` (Evaluation + FinalResult) khớp `db.sql`. **Không** viết Service, Controller, View.

**Phase 0 — Recon (chỉ đọc)**
1. Đọc `ARCHITECTURE_GUIDE.md`, `UI_RULES.md`, `UI_CATALOG.md` (đủ để dùng ở bước 3), `db.sql` mục 3.14–3.17, `docs/members/duc/*` (worklog iter1 làm mẫu), dòng mới nhất của `docs/management/WORK_LOG.md`.
2. Đọc toàn bộ `interview/**`: entity, repository, service, controller, dto, exception. Chú ý: cách `InterviewSchedule` map enum, phương thức chuyển trạng thái, `InterviewPanel`/`InterviewPanelRepository`.
3. Đọc `SchemaNamingConfig` và một entity mẫu để theo đúng quy tắc `@Table/@Column`.
4. Đọc phần `/interviews/**` trong `SecurityConfig`, `CurrentUserService`, API công khai của module `notification`.
5. `grep` toàn repo (kể cả `offer`) xem ai đang **ghi** và **đọc** `InterviewEvaluation` và `InterviewFinalResult`.
6. Đọc `templates/interview/*` và các file css/js `interview-*` hiện có.
7. Báo cáo ≤ 20 dòng: (i) bảng so khớp entity ↔ `db.sql`, **chỉ liệt kê dòng lệch** (tên cột, kiểu, enum, ràng buộc); (ii) ai ghi/đọc Evaluation và FinalResult; (iii) rule `/interviews/**` hiện tại; (iv) điểm nào mâu thuẫn với KHỐI NỀN; (v) danh sách file sẽ tạo/sửa. Không dừng chờ nếu không dính điều kiện DỪNG & HỎI.

**Phase 1 — Code**
- `InterviewEvaluation`: chỉnh cho khớp `db.sql` nếu lệch: `interview` → `@ManyToOne(LAZY) InterviewSchedule`, `interviewer` → `@ManyToOne(LAZY) User`, ba điểm `Integer` nullable, `strengths/weaknesses/comments` (`NVARCHAR(MAX)`), `recommendation` map đúng `Hire/No_Hire/Consider`, `evaluatedAt`.
- `InterviewFinalResult`: tương tự (quan hệ 1-1 với lịch, `hiringManager` LAZY, `finalDecision` `Passed/Failed`, `recommendedSalary`, `finalSummaryComments`, `approvedAt`). Không đổi chữ ký public mà `offer` đang dùng.
- Tạo `InterviewEvaluationRepository` (chưa có): kiểm tra tồn tại theo (lịch, người đánh giá); lấy danh sách phiếu của một lịch kèm người đánh giá bằng `JOIN FETCH`/`@EntityGraph` (tránh N+1 và `LazyInitializationException`); đếm phiếu theo lịch.
- `InterviewFinalResultRepository`: thêm tìm theo lịch nếu chưa có. `InterviewPanelRepository`: chỉ **thêm** method cần cho GBR-05/D1/D3 (kiểm tra thành viên, đếm/liệt kê thành viên), không sửa method cũ.
- Nếu cần `UNIQUE (InterviewId, InterviewerId)` và index `InterviewId` cho `InterviewEvaluation`: soạn sẵn SQL (migration + dòng trong `db.sql` + `model.md`) nhưng **DỪNG & HỎI** trước khi ghi file. Trong lúc chờ, Service vẫn chặn trùng bằng kiểm tra tồn tại.
- Nếu repo đã có mẫu test JPA thì viết test repository tương ứng; nếu không thì bỏ qua.
- Tạo `docs/members/duc/iter2-3-worklog.md` theo mẫu worklog iter1: phạm vi đã chốt, điểm lệch, quyết định D1–D6 đã áp dụng.

**Xong khi:** `./mvnw -q -DskipTests compile` OK; test hiện có không hỏng; `git status` chỉ có file trong allowlist; worklog đã ghi.

---

## BƯỚC 2 — DTO, Exception, Service

**Mục tiêu:** nghiệp vụ nộp đánh giá và xem kết quả phỏng vấn. **Không** viết Controller/View. Đọc worklog bước 1 trước.

**DTO** (`interview/dto/`, phẳng)
- `SubmitEvaluationRequest`: `technicalScore`, `softSkillScore`, `culturalFitScore` (`@NotNull @Min(1) @Max(5)`), `recommendation` (`@NotNull`), `strengths`/`weaknesses`/`comments` (`@Size(max = ...)` hợp lý, tùy chọn). Thông báo lỗi tiếng Việt. `interviewId` lấy từ path, không nhận từ body.
- `InterviewEvaluationResponse` (record): id, tên người đánh giá, vai trò trong panel, ba điểm, strengths/weaknesses/comments, recommendation (nhãn hiển thị), thời điểm đánh giá.
- `InterviewResultDetailResponse` (record): thông tin lịch (id, ứng viên, vị trí, thời gian, hình thức, trạng thái), danh sách panel kèm cờ đã/chưa nộp, danh sách phiếu theo D5, điểm trung bình ba tiêu chí (làm tròn 1 chữ số) + phân bố recommendation, `finalResult` (nullable), cờ `canEvaluate`, `canSeeAll`. Dùng record lồng hoặc record riêng, tránh trùng tên với DTO có sẵn.

**Exception** (`interview/exception/`)
- `InterviewEvaluationException extends BaseBusinessException` (có `field` nếu lỗi gắn ô) cho: đã nộp rồi, chưa tới giờ/đã hủy, dữ liệu không hợp lệ. Tái dùng `InterviewStatusException` nếu đã phù hợp. Không đụng `GlobalExceptionHandler`.

**Service** (interface + Impl; hai service nhỏ)
- `InterviewEvaluationService.submit(interviewId, request, currentUser...)` — `@Transactional`: tải lịch (`ResourceNotFoundException`) → kiểm tra thành viên panel (`AccessDeniedException`, GBR-05) → chặn trùng → kiểm tra D2 → lưu → nếu đủ phiếu thì chuyển `Completed` theo D3. Service nhận id/role của người dùng làm tham số, không phụ thuộc lớp web.
- `InterviewResultService.getResult(interviewId, currentUser...)` — `@Transactional(readOnly = true)`: kiểm tra quyền xem (D5/GBR-05), dùng query có fetch-join, map sang DTO ngay trong Service, tính trung bình.
- Phát thông báo qua `notification`: **không làm ở bước này** (xem phần tùy chọn bước 3).

**Test** (`src/test/java/com/group2/rms/interview/`, mẫu `InterviewSchedulingServiceTests`, Mockito): nộp thành công; nộp trùng; không thuộc panel → `AccessDeniedException`; lịch `Cancelled`; chưa đến giờ; người cuối cùng nộp → lịch thành `Completed`; người đầu tiên nộp thì chưa; xem kết quả theo từng role (D5); điểm ngoài 1–5 bị Bean Validation chặn (test `Validator` trên Request).

**Xong khi:** compile + test mới pass + test cũ không hỏng; không import `offer.*`; không `catch (Exception`; mọi method đọc có `@Transactional(readOnly = true)`.

---

## BƯỚC 3 — Controller, Security, View

**Mục tiêu:** hai màn hình Iter2/Iter3 chạy được end-to-end. Đọc worklog bước 1–2 trước.

**Controller** (`interview/controller/`)
- `InterviewResultController`: `GET /interviews/{id}/result` → view `interview/result` (model: `result`, `activeMenu`).
- `InterviewEvaluationController`: `POST /interviews/{id}/evaluations`, AJAX `@Valid @RequestBody SubmitEvaluationRequest` → `ApiResponse`. Bắt `InterviewEvaluationException` → `ApiResponse(false, message)` + HTTP 400. Xử lý lỗi validation theo đúng mẫu đổi mật khẩu trong `UserProfileController`.
- Quyền thật nằm ở Service (thành viên panel). URL chỉ là lớp chặn thô.

**SecurityConfig** (diff tối thiểu, không format lại file): thêm rule cho `GET /interviews/*/result` và `POST /interviews/*/evaluations` với các role có thể là thành viên panel/được xem kết quả (theo D1, D5), đặt **trước** rule ghi chung `/interviews/**`. Liệt kê diff ở báo cáo cuối.

**View**
- `templates/interview/result.html`: `page-header` + nút Quay lại + nút "Đánh giá" khi `canEvaluate`; card thông tin lịch; card panel (badge đã/chưa nộp); bảng phiếu đánh giá; thẻ điểm trung bình; card Final Result khi có; trạng thái rỗng.
- `templates/interview/evaluation-dialog.html` (fragment): `<dialog>` theo mẫu trong catalog; ba lựa chọn điểm 1–5, `recommendation`, ba textarea; lỗi hiện cạnh ô; Lưu `.btn.btn--primary`, Hủy `.btn.btn--outline`; khóa nút khi đang gửi.
- `static/css/pages/interview-result.css` (tiền tố `ir-`/`iev-`, chỉ phần catalog chưa có) và `static/js/pages/interview-evaluation.js` (`fetch` + CSRF, đóng dialog và tải lại khi thành công, hiện lỗi từ `ApiResponse`).
- `templates/interview/list.html` (file iter1 của tôi): thêm nút/link "Kết quả" (và "Đánh giá" nếu phù hợp), **không** đổi biến cũ.
- Không thêm menu sidebar (trang vào từ danh sách lịch). Nếu bắt buộc: chỉ append vào `sidebars/<role>.html` và ghi lại.

**Test web** (mẫu `UserProfileWebTests`, `SecurityFlowTests`): 200 với HR/thành viên panel; 403 với Candidate; `POST` thiếu CSRF → 403; `POST` hợp lệ → 200 JSON; điểm 6 → 400 kèm thông báo; `result.html` render không lỗi Thymeleaf; `SidebarMenuTests` vẫn pass.

**Việc cuối, tùy chọn (Notifications 5.1.39):** chỉ khi mọi thứ trên đã xong. Chỉ **gọi** API sẵn có của `notification` để phát thông báo sự kiện phỏng vấn (ví dụ có phiếu mới). Không sửa entity/schema/controller/template của `notification`. Nếu không rõ API thì bỏ qua và ghi vào worklog.

**Xong khi:** test pass; nếu chạy được `./mvnw spring-boot:run` thì mở trang với từng role (HR, Interviewer thuộc panel, Interviewer không thuộc panel, Candidate), nếu không chạy được thì nói rõ là **chưa** kiểm tra trên trình duyệt; không có `style=`, `<style>`, mã hex trong template/css mới.

---

## BƯỚC 4 — Kiểm chứng + bàn giao

**Vai trò:** người review, **không thêm tính năng mới**.

1. `./mvnw test` toàn bộ; báo số pass/fail. Test cũ fail do code người khác thì báo, không sửa.
2. **Scope audit:** `git status --short` + `git diff --name-only`; phân loại từng file: allowlist / ngoại lệ đã duyệt / **vi phạm**. Có vi phạm thì hoàn nguyên đúng file do phiên này sửa và báo.
3. **Tự grep trên code mới:** `IllegalArgumentException|IllegalStateException|EntityNotFoundException|ResponseStatusException`; `catch (Exception`; `@Autowired`; `com.group2.rms.offer`; `style=`, `<style`, mã hex trong template/css; `git diff` của `GlobalExceptionHandler` phải rỗng; mọi route mới đã có rule trong `SecurityConfig`; map enum khớp CHECK trong `db.sql`; method đọc đều có `readOnly = true`.
4. Lập bảng **D1–D6 → nơi thực thi trong code (`file:dòng`)**.
5. Tick từng mục checklist §9 của `ARCHITECTURE_GUIDE.md`; mục nào chưa đạt thì ghi lý do.
6. Hoàn tất `docs/members/duc/iter2-3-worklog.md`; thêm một dòng vào `docs/management/WORK_LOG.md` và file log theo ngày trong `docs/management/logs/` theo đúng mẫu iter1 (kiểm tra `.gitignore` không chặn thư mục `logs/`).
7. **Báo cáo cuối ≤ 20 dòng:** đã làm / chưa làm (kể cả Notifications) / cần tôi quyết (các D chưa xác nhận, đổi schema nếu có) / rủi ro merge conflict (`SecurityConfig`, `db.sql`, sidebar).

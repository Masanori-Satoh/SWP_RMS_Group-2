# Plan cho Gemini: đồng bộ UI luồng Interview + Giám đốc chốt kết quả khi tuyển HM

> Người giao việc: Đức (ducnm), owner module Interview. Người thực hiện: Gemini Flash (high).
> Đọc hết file này TRƯỚC khi gõ lệnh đầu tiên. Làm đúng thứ tự. Không chắc chỗ nào thì dừng và hỏi Đức, không đoán.

## 0. Luật cứng (vi phạm là hỏng việc)

1. **Không commit, không push, không tạo PR** nếu Đức chưa nói rõ. Xong việc thì để thay đổi ở working tree, báo cáo theo §7.
2. **Tuyệt đối không thêm** dòng `Co-Authored-By` hay dấu hiệu AI nào vào commit/PR (quy tắc của repo này).
3. **Không chạy cả bộ test** (`mvnw test` không có `-Dtest`). Chỉ chạy đúng các lớp test nêu ở §6. Chạy cả bộ vừa lâu vừa cần DB.
4. **Nhánh cũ `feat/ducnm/interview-ui-sync` đã bỏ** (đang kẹt merge dở). Đừng checkout/merge/rebase/pull/cherry-pick nó, đừng cố "dọn" nó. Chỉ được ĐỌC nó bằng `git show <commit>:<path>` như §2.1. Mọi việc làm trên nhánh mới ở §1.
5. **Không sửa file của module khác** (offer, requisition, talent-pool, admin...) trừ khi §5 cho phép đích danh. Chỉ *đọc* chúng để học cách làm.
6. Không `git push --force`, không `git reset --hard`, không xóa file ngoài dự án. Thấy trạng thái lạ (merge dở, file lạ) thì dừng và báo.
7. Không ghi mật khẩu DB, token, email thật vào bất kỳ file nào.
8. CSS: chỉ dùng token (`tokens.css`), không hex, không `!important`, không `style=""`, không `<script>` inline.

## 1. Tạo nhánh mới từ main mới nhất

```bash
git status -sb                      # phải sạch và KHÔNG có merge/rebase dở; nếu không thì DỪNG, báo Đức
git fetch origin
git switch -c feat/ducnm/interview-adw-director-finalize origin/main
git log --oneline -3                # kiểm tra đang đứng trên đỉnh origin/main
```

Nếu `git switch` báo có thay đổi chưa commit làm vướng: DỪNG, đừng `stash`/`checkout --` giùm. Hỏi Đức.

## 2. Phần A: đồng bộ UI Interview sang phong cách Admin Workspace (adw-)

### 2.1 Có sẵn một bản làm xong để THAM KHẢO (nhánh cũ đã bỏ, không merge/cherry-pick/rebase nó)

Nhánh cũ `feat/ducnm/interview-ui-sync` đã bị bỏ. Đừng `merge`, `cherry-pick`, `rebase`, `pull` từ nó (sẽ sinh đúng đống conflict ta đang né). Chỉ **đọc** nội dung các file qua `git show`, rồi chép vào nhánh mới. Commit tham khảo, thử theo thứ tự:

- `b9bc51e8` (trên remote `origin/feat/ducnm/interview-ui-sync`; nếu remote đã xóa nhánh thì dùng bản local)
- `a077fcdc` hoặc nhánh local `backup/interview-ui-sync-before-rebase` (= `b9bc51e8`)

Nếu cả hai không còn: tự làm theo bảng §2.3.

Cách chép (chạy trên nhánh MỚI ở §1, không checkout nhánh cũ):

```bash
REF=b9bc51e8
git show --stat $REF                                  # xem danh sách file nó đụng
# Các file sau main KHÔNG đổi từ lúc tách nhánh, chép nguyên được:
for f in \
  src/main/resources/templates/interview/list.html \
  src/main/resources/templates/interview/form.html \
  src/main/resources/templates/interview/candidate-list.html \
  src/main/resources/static/css/pages/interview-workspace.css \
  src/test/java/com/group2/rms/interview/InterviewScheduleFormWebTests.java ; do
  git show $REF:$f > $f
done
```
Với `docs/architecture/UI/UI_CATALOG.md`: chỉ chép **mục cuối** "Cập nhật 31/10/2026 — Interview dùng giao diện workspace Admin (`ivw-`)" từ `git show $REF:docs/architecture/UI/UI_CATALOG.md` nối vào cuối bản hiện tại, không ghi đè cả file (phòng `main` đã sửa file này).
Lưu ý xuống dòng: file trong repo có thể là CRLF; dùng `git show` rồi kiểm `git diff --stat` thấy chỉ các dòng cần đổi, không phải cả file bị đổi hết thì mới ổn.

**`interview/result.html` KHÔNG được chép nguyên** (bản cũ thiếu thay đổi nút "Lưu tiềm năng" của `main`). Mở bản hiện tại trên nhánh mới và áp công thức §2.2, dùng bản cũ (`git show $REF:src/main/resources/templates/interview/result.html`) làm mẫu cho các class `adw-*` và khối flash.

Sau khi chép: chạy test §6. Đây chỉ là điểm xuất phát, vẫn phải tự xem lại từng trang theo §2.3.

### 2.2 Cách làm `interview/result.html` trên nền `main` hiện tại

`main` đã đổi trang này cho nút "Lưu tiềm năng" (PR #126–#129). Ta giữ **cả hai** (nền `main` + khung `adw-`):

1. Phần đầu trang: dùng khung `adw-heading` của ta, đặt hàng tên ứng viên + nút toggle của `main` vào trong:
   ```html
   <div class="adw-heading ivw-heading">
       <div class="app-candidate-name-row">
           <h1 class="page-title adw-title" th:text="${result.candidateName ?: 'Ứng viên'}">Nguyễn Văn An</h1>
           <th:block th:if="${result.applicationId != null and topbarUser != null and (topbarUser.roleName == 'HR' or topbarUser.roleName == 'Hiring Manager')}">
               <span th:replace="~{talent-pool/potential-candidate-action :: action(applicationId=${result.applicationId}, candidateName=${result.candidateName})}"></span>
           </th:block>
       </div>
       <div class="iv-header-actions"> ...giữ nguyên các nút của ta... </div>
   </div>
   ```
   Bỏ hẳn breadcrumb `iv-breadcrumb`, `page-header`, và block `<section th:replace="...potential-candidate-action...">` cũ (nếu còn sót).
2. Cuối file, trong `layout:fragment="scripts"`: giữ `interview-result.js`, thêm `flash-toast.js`, `admin-workspace :: scripts`, và script có điều kiện là **`potential-candidate-toggle.js`** (KHÔNG phải `applications.js` nữa).
3. Giữ link `applications.css` có điều kiện (HR/HM) vì CSS `app-candidate-name-row` và `app-potential-toggle` nằm trong đó.

Sau khi sửa: `grep -nE "<<<<<<<|=======|>>>>>>>" src/main/resources/templates/interview/result.html` phải rỗng; không còn chuỗi `applications.js` hay `iv-breadcrumb` trong file.

### 2.3 Bảng chuẩn để tự làm / rà lại từng trang

Tham chiếu phong cách đúng (chỉ đọc): `templates/offers/*.html`, `templates/requisitions/*.html`, `templates/admin/**/*.html`,
`fragments/ui/admin-workspace.html`, `fragments/ui/flash-toast.html`, `static/css/pages/offer-workspace.css`,
và 4 tài liệu `docs/architecture/UI/UI_RULES.md`, `UI_NEW_PAGE.md`, `UI_CATALOG.md`, `UI_CSS_GUIDE.md`.
Áp cho 4 trang: `interview/list.html`, `form.html`, `result.html`, `candidate-list.html`:

| Việc | Cách làm |
|---|---|
| Đánh dấu trang | `<main class="page-content container adw-page ivw-page ...">` |
| Nền workspace | dòng đầu trong main: `<th:block th:replace="~{fragments/ui/admin-workspace :: scene}"></th:block>` |
| Tiêu đề | `<div class="adw-heading ivw-heading"><h1 class="page-title adw-title">…</h1>` + nhóm nút; bỏ breadcrumb và subtitle |
| Thẻ/khối | thêm `adw-surface`; `card-header` thêm `adw-panel-heading`; `card-title` thêm `adw-panel-title` |
| Bảng | `table adw-data-table` |
| Lỗi tổng của form | `<div class="adw-validation-summary" role="alert">` |
| Flash | `fragments/ui/flash-toast :: flashToast(success, error)` + `css/pages/flash-toast.css` + `js/pages/flash-toast.js` |
| Scripts | thêm `<th:block th:replace="~{fragments/ui/admin-workspace :: scripts}">` |
| CSS riêng | `static/css/pages/interview-workspace.css`, tiền tố `ivw-`, chỉ token |
| Test | sửa `InterviewScheduleFormWebTests` assert thành `class="card il-card adw-surface"` |
| Tài liệu | thêm mục cuối `UI_CATALOG.md` ghi Interview dùng workspace `ivw-` |

Bẫy Thymeleaf 3.1 đã dính rồi, đừng dẫm lại:
- Cấm `${param.x}` ngay trong tham số fragment. Tính trước bằng `th:with` ở thẻ ngoài rồi truyền biến (xem `result.html` ở `b9bc51e8`).
- `th:replace`/`th:insert` chạy TRƯỚC `th:if`/`th:each` trên cùng một thẻ; muốn điều kiện thì bọc `th:block th:if`.
- Giữ nguyên mọi `id` mà test/Playwright dùng (vd `btn-create-interview`).

## 3. Phần B: Giám đốc chốt kết quả phỏng vấn khi tuyển Hiring Manager

### 3.1 Hiện trạng code (đã khảo sát, đừng khảo sát lại)

- Người chốt hiện chỉ là **Hiring Manager phụ trách phòng ban** của job: `InterviewFinalResultServiceImpl.isDecisionMaker(User, InterviewSchedule)` (dòng ~117) = có role HM **và** `requisitionAccess.inManagedDepartment(user, jobPosting.getRequisition())`.
- `decide(...)` (dòng ~53) gọi `isDecisionMaker`; sai thì ném `AccessDeniedException("Chỉ Hiring Manager phụ trách phòng ban ... mới được chốt kết quả.")`.
- `InterviewResultServiceImpl.getResult` (dòng ~123-132) dùng cùng `isDecisionMaker` để bật `canFinalize` cho giao diện. Trang `interview/result.html` hiển thị nút "Chốt kết quả" theo `result.canFinalize`.
- Bảng `InterviewFinalResult` có cột `HiringManagerId NOT NULL` (entity `InterviewFinalResult.hiringManager`, FK tới `User`). Lưu id của Giám đốc vào đây vẫn hợp lệ về kiểu dữ liệu; **không đổi tên cột/field** (tránh đụng DB và seed). Chỉ cần ghi chú trong javadoc: "người chốt (HM, hoặc Giám đốc khi tuyển HM)".
- Director đã xem được trang kết quả (`FULL_ACCESS_ROLES` có `Director`) nhưng chưa bao giờ chốt được.
- `ApplicationStatusService.onInterviewDecided` đổi trạng thái hồ sơ theo Passed/Failed, **không phụ thuộc người chốt**, giữ nguyên.

### 3.2 Vấn đề chặn: hệ thống CHƯA có cách biết job nào là tuyển Hiring Manager

Đã kiểm `JobRequisition`, `JobPosting`, `Application`, `Department`: không có cột/enum nào cho biết vị trí tuyển là HM (`JobRequisition` chỉ có `Title`, `HiringType` New/Replacement, `Department`, `HiringManagerId` = người tạo/phụ trách yêu cầu, `CreatedBy`). Không thể suy ra chắc chắn từ `Title` (chuỗi tự do) hay từ người tạo (Director cũng được tạo yêu cầu).

**=> Phần B bị CHẶN cho tới khi Đức chọn một phương án trong §3.3.** Gemini KHÔNG tự chọn. Làm Phần A trước và xong hẳn; khi tới Phần B mà ô "QUYẾT ĐỊNH CỦA ĐỨC" dưới đây còn trống thì dừng, báo cáo, chờ.

### 3.3 Các phương án để Đức chọn (Claude đã soạn sẵn)

| # | Phương án | Ưu | Nhược |
|---|---|---|---|
| 1 | Thêm cột `JobRequisition.IsHiringManagerPosition BIT NOT NULL DEFAULT 0` (migration + `db.sql`), Giám đốc tick khi tạo/sửa yêu cầu | Rõ ràng, đúng nghiệp vụ, kiểm thử dễ | Đụng module Requisition (form, entity, seed) của người khác, cần phối hợp nhóm |
| 2 | Suy ra: yêu cầu do **Director tạo** (`createdBy` có role Director) thì coi là tuyển HM | Không đổi DB | Sai nếu Director tạo yêu cầu cho vị trí thường; dễ nhầm |
| 3 | Suy ra theo `Title` chứa "Hiring Manager"/"Trưởng phòng" | Không đổi DB | Mong manh, dễ lọt/lọt nhầm |
| 4 | Cho Director chốt MỌI buổi phỏng vấn (cùng HM) | Đơn giản nhất | Đổi nghiệp vụ nhiều hơn yêu cầu; HM mất độc quyền |

Claude khuyến nghị phương án **1**. Phương án 2–4 chỉ là phương án tạm.

**QUYẾT ĐỊNH CỦA ĐỨC:** _(để trống; Đức điền số phương án, và nói rõ HM có còn được chốt các job tuyển HM không, mặc định là KHÔNG)_

### 3.4 Thiết kế (áp dụng sau khi Đức chọn, giữ đường may duy nhất)

Gom toàn bộ quy tắc vào MỘT chỗ để đổi phương án sau chỉ sửa một hàm:

1. Thêm vào `InterviewFinalResultService` một phương thức, ví dụ `boolean requiresDirectorDecision(InterviewSchedule schedule)`. Thân hàm hiện thực theo phương án Đức chọn (đọc dữ liệu từ `schedule.getApplication().getJobPosting().getRequisition()`).
2. Sửa `isDecisionMaker(user, schedule)`:
   - Nếu `requiresDirectorDecision(schedule)`: chỉ `true` khi `user` có role **Director** (mọi Director đều được; không ràng buộc phòng ban). HM, kể cả HM phụ trách phòng ban, trả `false`.
   - Ngược lại: giữ nguyên logic cũ (HM phụ trách phòng ban). Director **không** được chốt job thường (trừ khi Đức chọn phương án 4).
3. Sửa thông báo lỗi trong `decide` cho đúng người: trường hợp tuyển HM → "Chỉ Giám đốc mới được chốt kết quả phỏng vấn cho vị trí Hiring Manager."; trường hợp thường giữ câu cũ.
4. Soát các nơi dùng `isDecisionMaker` / `canFinalize` / `ROLE_HIRING_MANAGER` trong module `interview`:
   `grep -rn "isDecisionMaker\|canFinalize\|decisionStates" src/main/java src/main/resources/templates` và xử lý từng nơi:
   - `InterviewResultServiceImpl.getResult` (đã dùng `isDecisionMaker`, tự đúng).
   - `InterviewResultServiceImpl.decisionStates(...)` + thẻ trong `interview/list.html`: nếu có nhãn/nút kiểu "chờ HM chốt" thì đổi chữ theo người chốt thật (không để HM thấy lời nhắc chốt job mà họ không được chốt).
   - Dialog `interview/final-result-dialog.html` và chữ trong `result.html`: bỏ cụm "Hiring Manager" cứng nếu có; dùng "Người chốt".
   - Dashboard/thông báo (nếu có việc "cần HM chốt phỏng vấn"): chỉ báo cho người được chốt. Nếu phát hiện sửa ở đây phải đụng module dashboard/notification của người khác thì **dừng và hỏi Đức**, đừng sửa.
5. Javadoc `InterviewFinalResultServiceImpl` (đầu lớp) và `InterviewFinalResultService`: cập nhật mô tả người chốt.
6. Nếu Đức chọn phương án 1: sửa `database/schema/db.sql`, thêm script migration trong thư mục đang dùng cho migration của repo (xem file `database/` có sẵn để theo đúng mẫu), thêm field vào entity `JobRequisition`, thêm checkbox vào `requisitions/form.html` **chỉ cho Director**. Phần này đụng module Requisition: làm tối thiểu, ghi rõ trong báo cáo để Đức báo nhóm.

### 3.5 Test bắt buộc cho Phần B (viết mới, đúng chỗ của module interview)

Ma trận cho `isDecisionMaker`/`decide` (Mockito như các test service hiện có trong `src/test/java/com/group2/rms/interview/`):

| Job | Người dùng | Kỳ vọng |
|---|---|---|
| Thường | HM phụ trách phòng ban | được chốt (như cũ) |
| Thường | HM phòng khác | không |
| Thường | Director | không (hoặc có nếu Đức chọn phương án 4) |
| Thường | HR / Interviewer | không |
| Tuyển HM | Director | được chốt; kết quả lưu với `hiringManager` = id Giám đốc; trạng thái hồ sơ đổi đúng như cũ |
| Tuyển HM | HM phụ trách phòng ban | **không** (AccessDenied) |
| Tuyển HM | HR / Interviewer | không |

Thêm một web test cho `GET /interviews/{id}/result`: Director thấy nút "Chốt kết quả" khi job tuyển HM; HM không thấy; và ngược lại với job thường. Mẫu: `InterviewPotentialWebTests`, `InterviewResultWebTests` (xem lớp nào có sẵn trong `src/test/java/com/group2/rms/interview/` và `.../candidate/talentpool/`).

## 4. Kiểm tra bằng mắt (tùy chọn, làm nếu app chạy được)

Script mẫu (đọc thêm, đừng ghi đè) nằm ở `target/ui-check/shots.py` nếu còn: Playwright Python, đăng nhập `hr_lan` rồi chụp `/interviews`, `/interviews/new`, `/interviews/{id}/result`. Mật khẩu/tài khoản dev lấy từ cấu hình local, không ghi vào tài liệu. Chụp thêm bằng tài khoản Director cho trang kết quả của job tuyển HM (Phần B). So với trang `offers/`: tiêu đề có gạch chân ngắn 48×4px, thẻ là `adw-surface`, không còn breadcrumb.

## 5. Phạm vi file được phép sửa

Phần A: `templates/interview/{list,form,result,candidate-list}.html`, `static/css/pages/interview-workspace.css` (mới), `docs/architecture/UI/UI_CATALOG.md`, `src/test/.../interview/InterviewScheduleFormWebTests.java`.
Phần B: `src/main/java/com/group2/rms/interview/**`, `templates/interview/*` (kể cả dialog), test trong `src/test/.../interview/**`. Chỉ khi Đức chọn phương án 1 mới được thêm: `requisition/entity/JobRequisition.java`, `requisitions/form.html`, `database/schema/db.sql` + migration.
Cấm: mọi thứ còn lại. `.gitignore` đã bỏ qua `integration-test/`, đừng sửa.

## 6. Lệnh test (chỉ những lệnh này)

```bash
./mvnw -o test -Dtest='InterviewPotentialWebTests'
./mvnw -o test -Dtest='Interview*Tests,SubmitEvaluationRequestTests,SidebarMenuTests'
```
Phải 0 failure / 0 error. Có test nào fail do thay đổi của mình thì sửa code (hoặc sửa test nếu assertion cũ sai thật sự, nói rõ lý do trong báo cáo). Không "sửa test cho xanh" bằng cách xóa assertion.
Mọi test dạng `@SpringBootTest` cần DB local; nếu lỗi kết nối DB thì báo Đức, đừng cố copy file cấu hình bí mật đi chỗ khác.

## 7. Báo cáo cuối (gửi Đức, ngắn gọn)

1. Tên nhánh, `git status --short` (liệt kê file đổi), và xác nhận chưa commit.
2. Phần A: file nào chép từ commit tham khảo, `result.html` đã áp công thức §2.2 thế nào; 4 trang đã đúng `adw-` chưa.
3. Phần B: đã làm hay đang chờ quyết định §3.3; nếu đã làm thì bảng ma trận test pass/fail.
4. Kết quả từng lệnh test ở §6 (số test, pass/fail).
5. Điều chưa chắc hoặc cần Đức quyết (đặc biệt: có đụng module Requisition/dashboard không).

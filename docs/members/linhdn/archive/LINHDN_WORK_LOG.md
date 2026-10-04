# Nhật ký công việc và điểm tiếp tục

Tài liệu này là điểm bàn giao của project. Sau **mỗi lần làm việc**, thêm một mục
mới ở cuối file: ngày, yêu cầu, quyết định đã xác nhận, file đã sửa/tạo/xóa,
cách kiểm tra và kết quả thực tế, việc còn lại. Không ghi credential, API key,
password hash hay dữ liệu cá nhân từ database. Trước khi tiếp tục, đọc mục mới
nhất, `git status`, `docs/schema-migration-impact.md` và `docs/TEST_PLAN.md`.

Mẫu cho mục tiếp theo:

```markdown
## YYYY-MM-DD — Tên việc
**Yêu cầu/quyết định:** ...
**File DB:** file nào chỉ đọc, sửa hoặc đã thực thi; có/không thay đổi dữ liệu.
**File code/tài liệu:** ...
**Kiểm tra:** lệnh hoặc thao tác, PASS/FAIL/BLOCKED, bằng chứng và giới hạn.
**Còn lại/bước tiếp:** ...
```

## 2026-09-29 — Đồng bộ project theo database mới

**Yêu cầu và quyết định:** `database/schema/db.sql` là source of truth. Tạo DB
mới, không chuyển dữ liệu cũ. Một Application được phép có nhiều lần chấm AI
để lưu lịch sử. Không tự sửa schema khi nghiệp vụ chưa rõ.

**Database đã đọc:** `database/schema/db.sql` có 20 bảng SQL Server.
`database/seeds/seed_data.sql` và `database/seeds/build_seed.py` đã được rà tên
bảng/cột. Tại thời điểm kiểm tra, `RitirementManagement2` đã tồn tại với 20
bảng và khoảng 1.846 dòng seed. Không chạy script xóa/tạo DB và không chạy lại
seed. Cấu hình local bị Git ignore trỏ tới DB này với `ddl-auto=validate`.

| File database | Trong lượt đồng bộ schema đã làm gì? |
| --- | --- |
| `database/schema/db.sql` | Chỉ đọc; **không sửa, không chạy lại**. Đầu file có lệnh xóa DB đích. |
| `database/seeds/seed_data.sql` | Chỉ đọc/đối chiếu tĩnh; **không sửa, không chạy lại**. |
| `database/seeds/build_seed.py` | Chỉ đọc; **không sửa, không chạy lại**. |
| `database/migrations/001_candidate_account_link.sql` | Thêm chú thích đầu file: migration này chỉ dành cho schema snake_case cũ; **không chạy** trên DB mới. File đã tồn tại từ mốc trước ở trạng thái Git chưa theo dõi. |

**Mã đã đổi:** Các Entity khác schema được chỉnh tối thiểu; trọng tâm là
`Candidate.UserId` bắt buộc/duy nhất, bỏ cột liên hệ trùng, `ApplicationReview`
đúng bảng review, `AIScreeningResult` nhiều–một với Application, ngày đăng tin
dùng `LocalDateTime`, trường Offer/trạng thái/độ dài cột. Thêm
`SchemaNamingConfig` để Hibernate giữ tên PascalCase và quote bảng `User`.
Account form/service và Dashboard query được chỉnh theo các mapping mới.
Chi tiết từng file, dependency và NEED CONFIRMATION nằm trong
`docs/schema-migration-impact.md`.

**Đã xác minh:** `mvn test` có 28 tests PASS, 0 failure/error/skip;
`mvn -DskipTests package` PASS và tạo JAR. `RmsApplicationTests` khởi động
Spring Context; Hibernate `validate` qua toàn bộ mapping với DB mới. Test DB
đã ghi hai kết quả AI cho một Application trong transaction rồi rollback.
Tìm tên field cũ trong `src/main` và `src/test` không còn kết quả. Chưa xác nhận
HTTP/browser end-to-end trên server thật; xem `docs/TEST_PLAN.md` để chạy tay.

**Còn chờ nghiệp vụ:** `InterviewEvaluation` không có unique theo
`(InterviewId, InterviewerId)` và không ràng buộc thành viên `InterviewPanel`.
Cần xác nhận có cho phép nhiều phiếu hoặc người ngoài panel không trước khi
thay đổi schema/luồng nộp phiếu. Cũng cần quyết định quy tắc khi Admin đổi role
của User đã có Candidate profile; hiện code giữ profile. AI Configuration và
health check AI/email chưa có contract được duyệt, không tự tạo form/probe.

## 2026-09-29 — Tài liệu bàn giao và kịch bản test

**Yêu cầu:** Nói rõ file DB nào đã sửa; từ nay ghi tiến trình vào Markdown và
có tài liệu từng bước test.

**Thay đổi:** Tạo `docs/WORK_LOG.md` (file này) và `docs/TEST_PLAN.md`; cập nhật
README để người đọc đi tới schema/test/log thay vì tạo nhầm `RMS_DB` trống.
Không sửa schema, seed, Entity hoặc logic ứng dụng trong lượt này.

**Kiểm tra:** Rà các route/form/test class trước khi viết kịch bản. Bước test
tự động 28/28 và package PASS được **ghi lại từ lượt đồng bộ schema**, không
được diễn đạt như một lần chạy mới của lượt viết tài liệu. Test trình duyệt
trong `docs/TEST_PLAN.md` là **chưa chạy**.

**Cách nối tiếp:** Sau mỗi thay đổi tiếp theo, thêm mục mới ở cuối file này và
cập nhật `docs/TEST_PLAN.md` nếu phạm vi test thay đổi. Ghi bằng chứng PASS,
FAIL hoặc BLOCKED đúng với cách đã chạy; không coi compile hay MockMvc là bằng
chứng đã test trình duyệt thật.

## 2026-09-29 — Sáu màn hình xác thực/tài khoản, giữ nguyên 20 bảng

**Yêu cầu và quyết định:** Audit Login, Register, Dashboard, Account List,
Forgot/Reset/Verify Email và Create/Update Account. Chủ dự án chọn Username bắt
buộc cho Candidate tự đăng ký. Sau khi ban đầu duyệt bảng reset token, chủ dự
án đổi yêu cầu thành **không thêm bảng, giữ nguyên schema**. Luồng reset hiện
dùng link ký HMAC hết hạn 15 phút, không dùng JWT; chữ ký phụ thuộc email và
password hash hiện tại, nên sau đổi mật khẩu link cũ không thể dùng lại.

**File database:** `database/schema/db.sql` đã được phục hồi đúng nội dung ban
đầu (`git diff --exit-code` PASS), không còn thêm bảng. Xóa file migration mới
`database/migrations/002_password_reset_token.sql`. Bảng tạm đã từng tạo trên
DB local `RitirementManagement2` có **0 dòng** trước khi xóa; đã xóa riêng bảng
đó. Truy vấn metadata sau cùng: **20 bảng, `PasswordResetToken` = 0**. Không
chạy script xóa/tạo DB và không đụng dữ liệu các bảng khác.

**File code:** Thêm `RegistrationController`, `RegisterAccountForm`,
`CandidateRegistrationService`, template Register; thêm
`PasswordRecoveryController`, `ForgotPasswordForm`, `ResetPasswordForm`,
`PasswordResetService`, `PasswordResetEmailSender`, template Forgot/Reset; thêm
CSS auth chung và liên kết Register/Forgot trên Login. `RoleRepository` có
lookup Candidate; `UserRepository` có truy vấn khóa row khi reset. Xóa Entity
và Repository của bảng reset token vừa tạo. `pom.xml` thêm mail starter;
`.env.example` ghi tên biến SMTP, public URL và khóa HMAC, không chứa giá trị
thật. Dashboard, Account List và Account Create/Update đã có từ mốc trước, chỉ
được audit trong đợt này.

**Test/tài liệu:** Tạo `docs/tests/` và file từng bước
`docs/tests/2026-09-29-auth-screens.md`; cập nhật `docs/TEST_PLAN.md`. Thêm
test Register, link reset ký/hết hạn/dùng lại, CSRF và render form trong
`AuthenticationDatabaseTests`/`SecurityFlowTests`.

**Kiểm tra thực tế:** `mvn.cmd -q -DskipTests compile` PASS sau khi bỏ bảng và
sau chỉnh sửa cuối; `mvn.cmd -q -DskipTests test-compile` PASS, chỉ compile
test. `git diff --exit-code -- database/schema/db.sql` PASS; SQL metadata DB
local 20 bảng và không có bảng reset token. `git diff --check` PASS; tìm trong
`database/schema`, `database/migrations`, `src/main` và `src/test` không còn
tham chiếu Entity/bảng reset token. Maven test mới chưa chạy: yêu cầu quyền
chạy `mvn test` trong đợt này
đã bị từ chối. Thử `mvn.cmd -q -DskipTests spring-boot:run`: Spring kết nối DB,
khởi tạo EntityManagerFactory và Repository, nhưng Tomcat báo
`Unable to establish loopback connection` / `Invalid argument: connect`, nên
**HTTP startup FAIL**; Maven lại trả exit 0. Chưa chạy browser/SMTP E2E; không
dùng kết quả 28 test cũ làm bằng chứng cho mã vừa thêm.

**Còn lại/bước tiếp:** Cấu hình SMTP và `APP_PASSWORD_RESET_SECRET` ngoài Git,
chạy test suite và AS01–AS06 theo file test khi được phép. Thiết kế không bảng
không thể thu hồi riêng một link chưa dùng hay giới hạn gửi lại bền vững qua
nhiều node; cần quyết định riêng nếu nghiệp vụ yêu cầu hai tính chất đó. Luồng
reset hiện cũng chưa thu hồi mọi session đang đăng nhập của account, cần quyết
định chính sách session sau đổi mật khẩu.

## 2026-09-30 — Quy tắc tài liệu kỹ thuật một file cho mỗi flow

**Yêu cầu:** Từ nay một flow chỉ coi là DONE khi có tài liệu riêng dưới
`docs/flows/` ghi browser → Spring → project class/method/package → DB → response,
nhánh lỗi, test bên ngoài từng bước và điểm debug. Phân biệt `[PROJECT CODE]`
với `[SPRING FRAMEWORK]`; phần Spring internals chưa kiểm tra source phải ghi
`Not verified at source-code level`.

**Nguồn đã đối chiếu:** checkout `main` tại commit nền `13b44be` (full feature
đã merge), `SecurityConfig`, các controller/service/repository/entity/form,
Thymeleaf/JS, `database/schema/db.sql`, `pom.xml` Spring Boot 3.5.16 và tests.
Không dựa vào mô tả cũ chỉ có ba route. Tạo `docs/flows/README.md` và 12 file
flow riêng: protected URL/login, logout, Candidate register, forgot, reset,
Dashboard, Account List, Create, Update, Deactivate, API Monitoring và startup
schema validation. Mỗi file có End-to-End Execution Flow, Detailed Execution
Trace, Failure/Alternative, Database Interaction, Data Transformation,
External Test Cases, Test Procedure, Test Data, Debugging Points, Code References
và Flow Completion Checklist. Tạo danh mục 45 case ở
`docs/tests/2026-09-30-flow-cases.md`; cập nhật `docs/TEST_PLAN.md`.

**File database:** không sửa và không chạy `database/schema/db.sql` hoặc seed.
**File ứng dụng:** không sửa Java, Thymeleaf, JS, properties hay dependency.

**Xác minh lần này:** `mvn.cmd -q -DskipTests compile` **PASS** sau khi được cấp
quyền truy cập kho Maven. `mvn.cmd -q test` **PASS** (9 suite XML, 34 tests,
0 failures, 0 errors, 0 skipped). `RmsApplicationTests` đã khởi tạo Spring
Context; Hibernate 6.6.53.Final đã kết nối SQL Server và tạo
EntityManagerFactory với `ddl-auto=validate`. Đây là bằng chứng context/JPA,
không phải browser E2E. Lần chạy Maven đầu trong sandbox **FAIL** vì không thể
tạo `C:\.m2\repository`; sau khi chạy với quyền phù hợp thì PASS. Các case
browser/SMTP/probe HTTP trong danh mục **CHƯA CHẠY** và mọi file flow ghi
`FLOW VERIFICATION: PARTIAL`.

**Cách nối tiếp:** Khi đổi code flow nào, sửa đúng file flow đó và case tương
ứng trong cùng lần làm; chỉ đánh `[x]` cho hạng mục thực sự kiểm chứng. Nếu
thực hiện case thủ công, điền bảng kết quả ở file test kèm ngày/môi trường/HTTP
và DB read-only. Không ghi secret, password/hash, token hay cookie vào log.

**Thử HTTP startup bổ sung:** `mvn.cmd -q spring-boot:run` khởi tạo JPA xong
nhưng **FAIL** ở web server vì port 8082 đang được dùng. Thử cổng 18082
(`mvn.cmd -q spring-boot:run '-Dspring-boot.run.arguments=--server.port=18082'`)
vẫn **FAIL** tại Tomcat NIO: `Unable to establish loopback connection`,
nguyên nhân sâu `java.net.SocketException: Invalid argument: connect`.
Maven lần thứ hai trả exit code 0 dù log `APPLICATION FAILED TO START`.
Không dừng tiến trình ở 8082 và không chạy browser/HTTP cases trên app không
khởi động được. HTTP/browser/SMTP/probe E2E tiếp tục **BLOCKED/CHƯA CHẠY**
trong môi trường này; compile, 34 tests và JPA vẫn PASS như ghi trên.

## 2026-09-30 — Chuẩn bị local E2E Forgot Password → SMTP → Reset → Login

**Yêu cầu:** Giữ nguyên luồng HMAC 15 phút, database/schema, Spring Security và CSRF; cấu hình local SMTP bằng biến môi trường không chứa credential trong Git; viết hướng dẫn thao tác và test ngoài hệ thống. Đọc attachment yêu cầu, hai flow doc, `pom.xml`, `SecurityConfig`, controller/service/repository/entity, local properties và `.env.example` trước khi sửa.

**Phát hiện:** `spring-boot-starter-mail` đã có; không thêm dependency. Cấu hình thực tế là `SPRING_MAIL_*`, `APP_MAIL_FROM`, `APP_PUBLIC_BASE_URL`, `APP_PASSWORD_RESET_SECRET`. `.env` không tự nạp; `application.properties` local bị Git ignore, cổng mặc định 8082. `PasswordResetEmailSender.isConfigured()` chỉ kiểm tra bean/From/base URL, không xác minh SMTP username/password hoặc kết nối; vì vậy `?sent` không chứng minh đã gửi thư. `database/schema/db.sql` có lệnh drop/create DB nên không chạy lại. Candidate Register tạo User Active và profile liên kết, phù hợp làm fixture test.

**File sửa/tạo:** `.env.example` bổ sung SMTP Gmail AUTH + STARTTLS và timeout không chứa credential. Tạo `scripts/run-password-reset-local.ps1`: nhập mailbox và App Password bằng prompt ẩn, sinh secret ngẫu nhiên 48 byte nếu chưa có, đặt biến môi trường Process cho mail/base URL/cổng, chạy Maven rồi khôi phục biến cũ. Tạo `src/test/java/com/group2/rms/config/MailEnvironmentBindingTests.java` kiểm tra bind tên env và MailSender auto-config mà không gửi mail. Tạo `docs/testing/password-reset-test-setup.md` với cách chuẩn bị DB/User, command Windows, test FORGOT-01–04 và RESET-01–06, SQL so sánh hash không in giá trị, troubleshooting. Cập nhật hai flow doc, `docs/flows/README.md`, `docs/tests/2026-09-30-flow-cases.md` (47 case), `docs/TEST_PLAN.md`, `docs/tests/README.md` và tạo biên bản `docs/tests/2026-09-30-password-reset-local-setup.md`.

**Xác minh:** `mvn.cmd -q -DskipTests compile` PASS. `mvn.cmd -q test` PASS: 10 suite, 36 test, 0 failure/error/skipped; Context, SQL Server 16.0 và Hibernate/JPA khởi tạo. Script PowerShell parse 0 lỗi; `git diff --check` PASS. Chạy `mvn.cmd -q spring-boot:run '-Dspring-boot.run.arguments=--server.port=18083'` kết nối DB và dựng EntityManagerFactory nhưng **HTTP startup FAIL** tại Tomcat: `Unable to establish loopback connection`, root `java.net.SocketException: Invalid argument: connect`; Maven trả exit 0 dù log lỗi. Không có SMTP credential/mailbox trong tiến trình, nên không gửi email và không chạy browser E2E. Flow docs giữ **PARTIAL**.

**Nối tiếp:** Người dùng cần tạo Google App Password cho mailbox test và nhập vào prompt trên máy; sau khi xử lý lỗi Tomcat loopback tại môi trường chạy, dùng script và tài liệu setup để làm từng bước. Chỉ đổi trạng thái flow thành PASS sau khi có bằng chứng browser + SMTP + mailbox + DB hash đổi + login password mới; không chép token/secret/password/hash vào log. Không đổi schema, Java nghiệp vụ hoặc SecurityConfig trong đợt này.

## 2026-09-30 — Prototype homepage tuyển dụng cho một công ty

**Yêu cầu:** Dùng UI/UX Pro Max và ảnh tham chiếu để tạo đúng một trang `index.html` độc lập cho career site của **một doanh nghiệp**, không phải chợ việc làm. Trả lời bằng tiếng Việt; giữ bản mẫu tách khỏi Spring Boot, backend, DB và xác thực thật. Lệnh `py --version` trả `Python 3.14.6`.

**Phân tích thiết kế:** Ảnh có header mỏng, hero chữ serif, khoảng trắng rộng, search gọn, minh họa nét vẽ người làm việc/cây và card việc làm viền nhẹ. Truy vấn UI/UX Pro Max gợi ý Hero + Features + CTA cùng serif/sans editorial; không có kết quả xác minh riêng cho single-company corporate careers. Chốt hệ thiết kế nền trắng ấm, chữ than, xanh lá trầm, viền mảnh, Georgia + Segoe UI, không gradient/glassmorphism/số liệu giả. Ghi chi tiết tại `docs/recruitment-homepage-design-system.md`.

**Chỉnh sửa:** Tạo `prototype/index.html` với toàn bộ CSS/JS/SVG nội tuyến: thương hiệu Mộc minh họa, hero và search chỉ cho vị trí Mộc, sáu job card, lọc từ khóa/phòng ban/địa điểm, trạng thái rỗng, dialog chi tiết, thông báo ứng tuyển demo, các phần phòng ban/về công ty/văn hóa/quy trình/CTA/footer và responsive. Sau khi brief bổ sung làm rõ mô hình một công ty, bỏ file prototype marketplace cũ `docs/recruitment-homepage.html`; không giữ hai homepage. Tạo tài liệu thiết kế và `docs/tests/2026-09-30-corporate-career-homepage.md`, thêm mục lục test tại `docs/tests/README.md`. Không sửa Java, schema, API hay trang Thymeleaf đang dùng.

**Kiểm tra:** `node --check` phần JavaScript PASS. Chrome headless kiểm tra sáu card, từ khóa `java`, trạng thái rỗng/reset, nút phòng ban, dialog và thông báo ứng tuyển: PASS. Kiểm tra DOM không cuộn ngang tại 375/768/1024/1440 px: PASS. Xem ảnh chụp tại 1440 và khoảng 500 px, sau đó giảm chiều cao hero/card và giản lược copy. Các bước bàn phím, screen reader, mở file offline và các tổ hợp lọc đầy đủ còn ghi CHƯA TEST trong file test. Không tuyên bố có nộp hồ sơ hay đăng nhập thật.

**Nối tiếp:** Nếu đem vào ứng dụng RMS, phải thay nội dung Mộc minh họa bằng thương hiệu và dữ liệu tuyển dụng được xác nhận, nối search/chi tiết/ứng tuyển tới endpoint hiện có và làm test tích hợp riêng. Hiện tại file chỉ là prototype độc lập; không áp thiết kế này lên UI Spring Boot khi chưa có yêu cầu.

## 2026-10-01 — Impeccable init cho toàn bộ RMS

**Yêu cầu/xác nhận:** Người dùng chạy `/impeccable init`; xác nhận PRODUCT.md mô tả toàn bộ RMS cho một công ty, và chọn viết code trực tiếp cho các trang mới. Skill local là `.agents/skills/impeccable/SKILL.md` phiên bản 4.3.1. Launcher `context --target prototype/index.html` chạy một lần, resolve repo root đúng và báo chưa có PRODUCT.md/DESIGN.md nhưng đã có UI.

**Nguồn đã đọc:** playbook init/live setup, README/pom, routes controller, SecurityConfig, DashboardService, ApiMonitoringService, schema table/constraint, flow docs, design-system doc và các yêu cầu đã chốt trong hội thoại. Hỏi hai điểm còn thiếu bằng công cụ structured input; không yêu cầu người dùng trả lời lại các business rule đã chốt.

**File tạo/sửa:** Tạo `PRODUCT.md` cho product truth, role, scope, chức năng hiện có và quyết định còn mở; Mộc vẫn là tên minh họa. Tạo `.impeccable/config.json` với `buildPath:code`, giữ nguyên config local hook consent đã có. Tạo `.impeccable/live/config.json` chỉ target `prototype/index.html`; chưa inject hoặc khởi động live. `detect-csp` trả shape null/signals rỗng nên không sửa CSP. Tạo `docs/tests/2026-10-01-impeccable-init.md` và cập nhật mục lục tests.

**Kiểm tra/nối tiếp:** File PRODUCT có schema marker, platform/sections đúng và mọi link nội bộ tồn tại: PASS. JSON parse, buildPath, local hook consent, target/anchor live và không có live injection: PASS. Review source/document INIT-01…05 và `git diff --check` PASS; INIT-06 live helper/browser NOT RUN. Kết quả nằm trong `docs/tests/2026-10-01-impeccable-init.md`. Đợt này không sửa Java, UI, DB, Spring Security hay tạo DESIGN.md. Chức năng job/application/interview/offer chưa có controller, notifications thiếu datasource và AI Configuration còn thiếu catalogue được ghi rõ để lần sau không tạo UI hứa khả năng chưa có. Lệnh live hoặc một yêu cầu chỉnh UI riêng sẽ tiếp tục dựa trên PRODUCT.md và incumbent code.

## 2026-10-01 — Impeccable polish homepage trong prototype

**Yêu cầu:** Refinement trang tuyển dụng một công ty: spacing, typography, hierarchy, job card density, section rhythm, responsive và accessibility; giữ hướng editorial/minimal. Đọc playbook polish/craft-floor, PRODUCT.md, incumbent CSS/markup/JS và design-system doc. Giữ palette, font hệ thống offline, minh họa SVG và copy/label hiện có theo phạm vi preserve; không áp gợi ý thay thế media/font thành redesign. Không có critique snapshot cung cấp body/id để nhận backlog.

**Phát hiện/sửa:** 28 control/link ở desktop và 27 ở mobile có chiều cao dưới 44px; skip link/submit/phòng ban chưa chuyển focus tới nội dung; bấm padding dialog đóng modal; culture figure bị margin mặc định thu hẹp. Sửa trong `prototype/index.html`: scale spacing section fluid, H1 gọn hơn, heading balanced, metadata card lớn hơn nhưng group spacing chặt hơn, bỏ min-height card thừa, target/input tối thiểu 44px và input 16px, search tablet/mobile dễ thao tác, figure margin 0, màu selection/caret/control border và số tabular nhất quán. Thêm focus target, tên riêng cho nút xem từng vị trí, dialog description, backdrop bounds check, Escape/tap ngoài cho menu mobile.

**Xác minh:** Hai vòng ảnh desktop/mobile trước và sau; layout/bounds ở 1440/1024/768/375/320 không tràn ngang và sau sửa không còn target dưới 44×44px. Card đo 220 → 194px, hero desktop 698 → 672px. Tương phản 134 phần tử chữ/placeholder PASS, thấp nhất 4.87:1. Chrome CDP kiểm tra search/empty/reset/department/location/combine/dialog/keyboard/mobile touch/reduced-motion/long title; không runtime error. Hai fail Enter/Escape trong driver ban đầu được chẩn đoán riêng: thiếu carriage-return và kiểm tra touch quá sớm; gửi phím đúng/chờ gesture cho kết quả PASS, không sửa source để test qua. Native dialog containment được kiểm tra với Tab và Shift+Tab, cho phép body sentinel nhưng không background control. Browser tool không khởi động được nên dùng Chrome headless/CDP, không coi là người dùng test thiết bị thật. Raw JSON và ảnh sau nằm trong `docs/tests/assets/2026-10-01-homepage-polish/`.

**Tài liệu/nối tiếp:** Cập nhật design-system doc, tạo `docs/tests/2026-10-01-homepage-polish.md` với 14 case từng bước và giới hạn, thêm mục lục tests. Không thay app Java/DB/security hoặc nội dung minh họa. Tab từ heading tới control kết quả và click backdrop ngoài đã xác nhận PASS riêng. `node --check`, standalone dependency check, link tài liệu và `git diff --check` PASS; harness/captures tạm đã dọn. Browser zoom thật, NVDA/VoiceOver, Safari/Firefox và native select vẫn cần các bước thủ công được ghi rõ; prototype tiếp tục chưa nối backend.

## 2026-10-01 — Audit Gemini và cải thiện corporate homepage

**Yêu cầu:** Dùng review Gemini làm audit cần xác minh, dùng UI/UX Pro Max + Impeccable, giữ editorial/minimal cho một công ty. Đọc toàn bộ attachment `71597c48-833d-43cf-91df-90bab39c7577`, HTML/CSS/JS, PRODUCT và design system trước khi sửa; phân loại từng khuyến nghị và trình bày critical/high/polish/preserve. User cho phép hai agent review độc lập sau implementation. Không đổi backend/schema/security/Thymeleaf.

**Audit:** Confirm title/card affordance, mất salary/date/detail/URL, search ở Hero xa results, clear teleport, SVG người, disclaimers lặp, nav/dead modal, benefits thiếu và tablet squeeze. Điều chỉnh nhận xét “mọi section giống nhau”, giữ native details/dialog, không áp đề nghị giảm footer targets. Review contrast 2,65:1 sai với cặp màu hiện có: đo #7B8B7D/#E7F0E8 = 3,09:1. Không kết luận AI authorship từ vẻ ngoài. Không tự bịa doanh nghiệp thật hoặc backend.

**Files sửa/tạo và lý do:**

| File | Thay đổi |
|---|---|
| `prototype/index.html` | Title anchor phủ card; sáu JD đầy đủ + salary/date/facts; hash `#role/<slug>`/Back/Forward/clipboard; filters/counts/clear gần results; benefits/copy cụ thể; nav đúng narrative; bỏ hai human SVG/info modal/disclaimer lặp; hình học trung tính; fonts nhúng; grid/spacing/type/focus/contrast/reduced motion; sticky close; favicon; contact .example. |
| `prototype/FONT_LICENSES.txt` (mới) | Lora/Source Sans 3 official OFL để font nhúng chạy offline hợp giấy phép. |
| `PRODUCT.md` | Cập nhật capability prototype: structured hash details/contact, sample policy/JD; không nhận hồ sơ thật. |
| `docs/recruitment-homepage-design-system.md` | Ghi trạng thái mới ở đầu; giữ quyết định cũ như lịch sử, không coi search Hero/SVG người là cam kết hiện tại. |
| `docs/design/2026-10-01-homepage-gemini-audit.md` (mới) | Confirmed/Partially/Not applicable, evidence, phạm vi và danh mục sample values. |
| `docs/design/2026-10-01-homepage-impeccable-critique.md` (mới) | Assessment độc lập, heuristic synthesis 30/36 trước batch cuối, detector disposition, priority fixes. |
| `.impeccable/critique/2026-10-01T03-34-39Z__prototype-index-html.md` (mới) | Snapshot fingerprint của bytes đã review; đọc latest identity rồi close sau khi xử lý hai priority groups; first run, chưa có trend so sánh. |
| `scripts/check-career-homepage.mjs` (mới) | Browser test Windows Chrome CDP không package mới; timestamp artifacts, exit1 nếu check fail, profile tạm có guard cleanup. |
| `docs/tests/2026-10-01-homepage-gemini-audit.md` (mới) | 24 case từng bước, expected results, actual evidence/limits và cách chạy lại script. |
| `docs/tests/README.md` | Thêm mục lục đợt test. |
| `docs/tests/assets/2026-10-01-homepage-audit/` (mới) | Hai vòng ảnh/JSON, driver failures, fresh-tab focus evidence và Assessment B đã redacted localhost token. |
| `docs/WORK_LOG.md` | Nhật ký để nối tiếp. |

**Không có file DB nào được sửa; không xóa file dự án lâu dài.** Không đổi Java/application properties, không chạy Maven vì thay đổi chỉ là standalone frontend. Các thay đổi `.env.example`, `docs/TEST_PLAN.md`, auth tests/flows có từ trước đợt này được giữ nguyên.

**Nội dung mẫu được phép:** Mộc làm workflow software; hybrid theo nhóm tối đa 2 ngày từ xa, 09–18 Mon–Fri, 12 ngày phép, bảo hiểm theo quy định, học tập 3 triệu/năm, laptop, review 6 tháng. Giữ sáu job/lương/city/date; JD mới là fiction. Email/website reserved `.example`; không gửi email, không fake upload hoặc success, không thêm legal page/địa chỉ đường phố/testimonial/headcount không có nguồn.

**Review và sửa cuối:** A xem source/bốn viewport/hai dialog + probe mobile, không xem detector. B một CLI scan/fresh tab/geometry, không xem A hoặc Gemini; root đọc B sau A. B báo 13 warning: CAREERS 10px thật →11px; 12 cramped-padding false positives đối chiếu container inset/padding 16px. P2 close ra ngoài viewport khi scroll và focus body sau hash navigation, P3 metadata mobile quá cao → sticky header + RAF restore focus + facts hai cột. Caption culture rộng hơn, favicon inline. Không rerun detector hoặc tạo vòng ảnh thứ ba.

**Xác minh:** JS syntax và script syntax PASS; git diff --check PASS. Round1 61 PASS/2 FAIL (focus thật và favicon404) đã sửa. Round2 75 PASS/1 driver FAIL: Page.navigate chỉ đổi hash giữ document cũ; driver được sửa buộc reload và test hẹp bằng tab mới ở 1440/375 PASS. Giữ JSON FAIL gốc, không sửa thành xanh. Bốn viewport 1440/1024/768/375 không tràn ngang; thêm 600/581/320. Job 3/2/2/1, process 4/2/2/1. Card ~205px desktop/tablet, ~189px mobile; text minimum 5,94:1 trên 122 DOM texts, control border white 3,60:1. Search không dấu/combine/empty/clear, sáu facts/JD, URL/history/keyboard/actual CDP touch/AX names/sticky close/reduced motion/offline PASS trong phạm vi Chrome headless. Chưa chạy lại toàn bộ script đã đóng gói sau chỉnh driver; case đó có fresh-tab proof riêng.

**Run notes:** Target slug `prototype-index-html`; không có ignore list; assessments độc lập, snapshot write/trend/latest/close thành công. Native CUA lỗi Windows deny-read ACL; dùng Chrome CDP fallback. B preflight mutation/script execution PASS; overlay chưa inject vì helper parse port thất bại dù start trả JSON; không có overlay user-visible và không xem unavailable runtime findings là zero. Live-server 8400 đã stop, Chrome B/profile đã dọn; localhost preview 8766 dừng bằng Ctrl+C. Root dọn script/font scratch và profile Chrome tạm sau khi copy bằng chứng. Chỉ giữ homepage, giấy phép, test script và durable evidence.

**Nối tiếp:** Thay fixture/contact bằng nội dung doanh nghiệp duyệt; job/application/backend integration là nhiệm vụ riêng. Chưa NVDA/VoiceOver, Safari/Firefox, device thật, browser zoom200 hoặc OS mail client/SMTP. Không tuyên bố production readiness. Dùng test doc/script và trạng thái hiện tại trong design system; không khôi phục search Hero, info modal, SVG người hoặc password/schema decisions cũ từ lịch sử.

## 2026-10-01 — Career integration vào Spring và chuyển UI sang English

**Yêu cầu/xác nhận:** Đọc toàn bộ attachment e9557e6d-7a6d-46a4-b583-0888c3019a97; tiếp tục homepage hiện tại vào Spring/Thymeleaf, chỉ một công ty. User giữ nguyên DB/seed và nhắc mọi UI English, trừ nội dung từ DB tiếng Việt giữ nguyên. Không JWT, schema/table mới hoặc backend giả. Dùng UI UX Pro Max/Impeccable và quyền hai agent review đã được user duyệt.

**Inspect/impact trước sửa:** Schema JobPosting public fields, JobRequisition/Department relationships, User/Role/CandidateUserId FK, Application AppliedCvUrl NOT NULL; repositories/services/controllers/security/routes/templates/config/tests. Dashboard đã định nghĩa open = Published + deadline null/>=now; reuse rule này. Không có CV upload/storage/submit hoặc profile-edit backend. Impact KEEP/MODIFY/CREATE/gaps lưu `docs/design/2026-10-01-career-integration-impact.md`. Không suy schema từ Entity hoặc tự thêm rule phê duyệt/postingDate.

**Implementation:** Thêm CareerService read-only/PublicJob/Viewer/CandidateDetails và CareerController root/jobs/detail/apply GET/404/409; JobPostingRepository query + EntityGraph. Template `careers/` English, cards `th:each` duy nhất, filter DOM/count/no-dấu/location/department, metadata/JD/requirements/benefits DB nguyên văn. Dedicated role URL, header guest/auth mobile/desktop, logout POST CSRF, useful project-notes hero/working principles, company policy một lần. Prototype cũ thành entrypoint Spring, bỏ fixture job array/hash dialog/contact `.example`; legacy browser script chuyển compatibility entrypoint sau dependency search. Fonts/CSS reuse direction đã duyệt; không tải stock hoặc thêm decoration.

**Auth:** Career root/fonts public, Candidate apply matcher trước public jobs; HttpSessionRequestCache matchingRequestParameterName=null, defaultSuccessUrl Dashboard false. Guest Apply→login→đúng URL (retry giữ target), direct login→Dashboard. Chỉ lookup own UserId/profile; internal403, missing profile409 không auto-create, inactive session revoked. CV/Submit disabled, no POST handler/no Application.save; token thiếu403/hợp lệ405. AuthController root redirect requisitions được bỏ để không xung đột homepage; Dashboard vẫn `/dashboard`.

**English:** Đổi literals template auth/admin/dashboard/demo/requisition và validation/service/email feedback; static activity-type label English. Không đổi query/rule/role/status/constraints hoặc dịch DB. Danh sách từng file và lý do ở `docs/design/2026-10-01-career-integration-report.md`. Search literals còn lại chỉ brand Mộc và comments; UI “Theo InterviewPanel” cũng đổi sang Assigned through InterviewPanel. Python scan ban đầu lỗi stdout cp1252, rerun `py -X utf8` thành công; không sửa dữ liệu để né encoding.

**Concurrency/merge:** Checkout nhận code Requisition teammate và Git index SecurityConfig UU trong phiên. Working file không có markers, giữ production session/CSRF/admin rules; tests compile PASS. Không stage/finish/abort merge hoặc ghi đè nghiệp vụ teammate. Chỉ English literal/fallback trong requisition templates thuộc yêu cầu UI toàn English.

**Review:** A hoàn tất độc lập trước khi root đọc B. A baseline27/40 all10heuristics, cognitive2/8fails, 1P1/3P2/1P3 priorities; B CLI một lần2 flat-type warnings đều false positives theo computedCSS46/34/20/16 desktop,32/28/20/16mobile; detect.js injected3pages/0runtimeissues. Báo cáoA/B/synthesis ở docs/design. Batch cuối: notice submission trước login, evaluated fragment titles detail/apply/unavailable, department openings trước0 + mobileoverflowcue, read-only legend/style/Notprovided, giữ DB text bỏ double-native-bullet, account CTA/footer theo capability/state. Giữ palette/fonts/spacing/editorial identity; không tự chấm lại score.

**Verification:** Selected Maven9 explicitclasses36PASS/0fail/error/skip; confirmation CareerFlow5+CareerReadOnly3+SecurityFlow14=22PASS sau batch. Compile/SpringContext/JPAvalidate/actualSQLServer query-FK-publicrender PASS. Actual HTTP Tomcat startup vẫn FAIL `Unable to establish loopback connection` / `SocketException: Invalid argument: connect`; Mavenexit0 không phải bằng chứng startup. Không fix JDK/environment không liên quan hoặc tuyên bố realbrowserauthE2E. JS syntax và gitdiffwhitespace PASS.

**Browser:** Native CUA deny-readACL, fallback approved private headlessChromeCDP. First DOM-ready fail trước capture; round1inspection21PASS rồi regexdriverfail; recovery38PASS/0FAIL không thêmshots. AgentA chỉ thêm missingdetail/apply firstinspectionshots. Onefinalconfirmation round2 **46PASS/0FAIL** bốnviewport1440/1024/768/375, job3/2/2/1/process4/2/2/1/apply2/2/2/1, nooverflow/runtimeerrors, homepage mintext5.36:1/controlcontrastPASS. Xem finaldesktop/mobilepositions/applyshots; stop polish, không thirdround. Public preview là actualDB→MockMvc→Thymeleaf; authenticated/apply screenshots Taylor Nguyen fixture, không credentials/profile thật. Không test upload/submission hoặc login trên Python. C17 ghi zoom/screenreader/browser/device chưa test.

**Sự cố test phải giữ rõ:** Lượt wildcard `*Tests,!*DatabaseTests` đã bắt RequisitionReviewProbeTests mới có INSERT/UPDATE thử; source+javapannotations Transactional/Rollback xác nhận rollback nhưng identity có thể tăng. Không tuyên bố toàn phiên “không có DB writes”. Không SQL/schema/seed file thay đổi, không drop/create/seed/reset DB; từ đó chỉ explicit safe classes, không chạy probe lần nữa/không che constraintlogs. Testdoc ghi riêng.

**Artifacts/nối tiếp:** Report từng file +9topics, test C01–C18/commands/expected/actual/limits, careerflow class/package/method, indexes/PRODUCT/design-system được cập nhật. `.impeccable/critique/2026-10-01T13-34-51Z__src-main-resources-templates-careers-index-html.md` fingerprint baseline được write/trend/latest identity/close đúng; first targetrun, chưa có trend. Temp critique body/translation-scan scripts dọn; detector8400/privateprofiles dọn. Previewserver8766 cần dừng khi kết thúc. Chưa có CVstorage/submit/duplicate policy/profileedit/contact thật; controls tương ứng chưa bật. Giữ các artifact FAIL gốc và không suy production-ready từ build/tests.

**Kết thúc:** Link checks các tài liệu mới/index/PRODUCT/design-system PASS, tổng đúng9 XMLsuite36tests/0fail và confirmationJSON46PASS/0FAIL. Previewserver8766 đã dừng bằng Ctrl+C; helper8400 và Chrome profiles của root/A/B đã dọn. Không thêm lượt test/ảnh/polish sau confirmation. Hai one-off scripts dọn bằng native Remove-Item sau apply_patch delete bị filesystem từ chối; compatibility harness mới syntax PASS. Điểm tiếp tục là HTTP môi trường, backend CV/submit và Git merge đang UU, không chạy lại DB/probes.

## 01/10/2026 — Auth/Admin UI đồng bộ với Careers

**Yêu cầu/phạm vi:** Attachment `fa512cc3-e140-42a6-8e60-0d16979d8725`: synchronize existing
Auth + Admin, UI UX Pro Max + Impeccable, preserve backend/Thymeleaf bindings/CSRF/session/
RBAC/DB, English static UI/DB verbatim. Trước code ghi
`docs/design/2026-10-01-auth-admin-impact.md`. Giữ quyết định đã chốt: Candidate có
trong Account Management; Delete vẫn visible confirmation→Inactive, không harddelete;
không JWT/newtables. Legacy teammate Requisition/Hello không redesign.

**Shared system:** Extract chính font faces/palette/spacing homepage vào
design-tokens.css; interface.css/workspace.css và brand/auth-layout/header/messages
fragments mới. `head.interfaceHead`/`sidebar.workspace` mới giữ legacy fragments.
careers.css/fragment chỉ reuse tokens/wordmark, không đổi art direction hay DB jobs.
Auth viewport brand/form/mobileheader; Admin dense native tables/metric rows/semantic
fieldset/error feedback. Production routes/fields/options/formactions/constraints
giữ nguyên; noremember-me/profile/AIconfig giả. Fullfilelist+why ở report10topics.

**UI logic:** interface.js safe passwordtoggle, focused linked BindingResult summary,
progressive Menu/Escape, ResizeObserver contextual table cue. Account listdialog
Cancel/Escape returntrigger; uniqueusername context +stickyidentity; mobile native
MoreFilters Department/Sort keepsGETnames/values/autoopens active. Candidate newnav
Dashboard/Careers, staffRequisitionlink internal-only; **không đổi direct-route security**.
Dashboard tablet2cols, Unavailable Features literal; statuswordsnowrap; existing
username/password/status hints now associated. Onefinalbatch handled5P2; no full redesign.

**Regression sequence/errors:** First19Security/Career testsPASS. Added2meaningful
SecurityFlowTests contract/error cases and opt-in MockMvc fixture export. First
explicit9suite38gate37PASS/1XMLXPathparsererror (`defer` HTML boolean attr); fixed
assertion rather than validHTML; recovery38PASS. No wildcard/DBwriteprobe.
Afterpolish,21tests ContextERROR due config value `spring.thymeleaf.encoding=UTF-8`
with trailingbacktick that appeared after baseline. User explicitly approved removing
onechar; nootherconfig edit/no secret printed. A subsequentMavencommand was rejected
(`rejected by user`), notexecuted; user thenexplicitlyapproved rerun. **Final38PASS,
0failure/error/skip,9suite**, compile/Context/JPA/SELECTqueriesPASS usingrealconfig,
no propertyoverride. `final-regression.json`: **87protectedJava/DB/config hashes all
match initialbaseline**, includingconfig afterapproved correction. No DB/schema/seed/
JavaProduction/Security netchange or datamigration inthisnewtask. GitindexUU untouched.

**HTTP/external boundary:** Existing8082curltimeout; oneHTTP startup on8769 fails
TomcatNIO/PipeImpl `Unable to establish loopback connection` /
`SocketException: Invalid argument: connect`. Mavenexit0 isnotstartupPASS. Didnot
changeJDK/security/backend tohideenvironmentfailure. Realbrowserlogin/logout/
accountmutation/SMTP E2E **NOT TESTED/BLOCKED**. MockMvcPOST contracts and static
browserclient evidence areseparate; no POST/email/AIprobe throughpreview.

**Independent review:** User's two-agent authorization persists; A source/design +
own64views/19reversiblecontrols completed and read before root receivedBfindings.
BaselineA28/40Good, cognitive3/8fail,5P2groups,0P0/P1; no inflatedpostscore. B single
CLI templates scan33warnings (exit0);3inscope shared-CSS flat-type falsepositives,
30outsideCareers/Requisitionnotfixed/ignored. Runtimeinjection5pages gave5rawfindings:
4intentionalforestcaption occurrences5.99:1 +1tablewrapperfalsepositive12pxcellinset.
Audit16/20baseline; noWCAGcertification. Noignore/hookchanges. NativeCUAACL/kernel
failure; headlessCDP newprofiles fallback, **no verifieduser-visibleoverlay**. Bsole
live-serverstdoutpipe neededsameattemptserver.json; originalrunner timeoutkept,
recoveryreusedhelper, thenstop--keep-inject+temp/token/profilecleanupdone.

**Browser:** Rootfirst144basicchecksPASS thenresource/drivercompletionFAIL; rawbadLogin
shotsretained/rejectedforvisualconclusion. Recoveryfirstinspection183/184PASS;
ArrowRightfailure wasbackgroundtabdriver (freshforeground+codeArrowRight0→40),
fixeddriver withoutUIworkaround. Finaloneconfirmation `round2-results.json`
**203/204PASS** across9base×4widths +errorstates/activefilters/keyboard/contrast/focus/
references/nooverflow/runtimeerrors. TheonlyFAIL wascountingcollapsedauto-fit0px
track asafifthcolumn. Targetedfreshcomputedcheck **PASS4visibletracks/4metrics** in
`round2-grid-confirmation.json`, noUIedit/newshots/fullround3. KeepfailedJSON unchanged.
RootviewedfinalLogin1440,List375,Dashboard768,Delete375,Create-invalid375. Stopoptional
polish/testing. Testfixturesfictionalnames/.testemails/metrics/visualtokens isolated
fromruntime/DB. Browserfont/type/controlcontrast gatesPASS; device/zoom/screenreader
Safari/Firefox remainNOTTESTED withmanualsteps.

**Artifacts/bookkeeping:** Testdoc U01–U23 +commands/expected/actual/limits;
`docs/flows/auth-admin-ui-flow.md` actualpackage/class/method/nativecallbacks;
report10topics,combinedcritique,auditA/B, indexes/PRODUCT/designsystemupdated. Baseline
snapshot `.impeccable/critique/2026-10-01T15-27-41Z__ources-templates-admin-accounts-list-html-ba200b8e.md`
write/trendPASS firsttargetrun28/40; close exacttarget/identity after5prioritieshandled.
No scorepostpolishclaimed. JSsyntax/gitdiffcheckPASS. Rootpreview8768stoppedCtrl+C;
HTTP8769failedrunnerclosed; root/A/BvalidatedprivateChromeprofiles/helperdọn. Scratch
sync/diagnosis/grid scripts dọn byexactLiteralPath; durableQAhelpers andraw evidence retained.

**Tiếp tục:** `docs/design/2026-10-01-auth-admin-report.md`,
`docs/tests/2026-10-01-auth-admin-ui.md`, `docs/flows/auth-admin-ui-flow.md`.
NeedrealHTTPenvironment repair, U02–U19 onapprovedDBtest +SMTP externalguide,
officialbranding/usernamemodel ifdesired, teammateRequisitionauthorizationconfirmed
initsownscope. No feature/businessmodel/schema inferred from a visualreview.

## 2026-10-02 — System Admin account separation: audit + thuật ngữ

**Yêu cầu mới:** tách backend/UI Internal Accounts và Candidate Accounts, Dashboard counts riêng, sidebar Accounts/System; Delete đổi Deactivate và không hiện khi Inactive. Yêu cầu này thay UI merged-list/Delete cũ. Giữ DB/seed/auth/public registration và phong cách Mộc.

**Audit trước backend:** User.Role xác định authority/Dashboard; Candidate.account mapping UserId NOT NULL UNIQUE FK. AccountListService.findAccounts chưa predicate nhóm, findRoles trả cả Candidate; DashboardMetricsRepository.accounts/accountsWithStatus đếm mọi User. Seed có sáu role đã biết. AccountManagementService.update giữ Candidate profile khi đổi role nội bộ, nên role và sự tồn tại profile có thể mâu thuẫn về phân nhóm. Đã hỏi NEED CONFIRMATION: role hiện tại (đề xuất, link đọc profile/hiển thị thiếu profile) hay sự tồn tại Candidate.UserId. Chưa sửa query/service/form scope/Dashboard/sidebar/security trước khi chốt.

**Đã sửa độc lập:** `src/main/resources/templates/admin/accounts/list.html`: Deactivate accessible name/label/modal, th:if status != Inactive. Giữ data attributes/action/CSRF/Cancel-focus/related history. `SecurityFlowTests.accountListOffersDeactivationOnlyForActiveOrBlockedAccounts`: ba status, kiểm tra action Active/Blocked, ẩn Inactive, Edit còn, không Delete copy. Không sửa Java production/schema/seeds/config/login/logout/register. Git merge index SecurityConfig UU giữ nguyên.

**Tài liệu:** `docs/design/2026-10-02-account-separation-impact.md` ghi 7 audit points, impact/rule alternatives/route plan; `docs/tests/2026-10-02-account-separation.md` ghi D01–D05 và S01–S10 từng bước, pending/NOT TESTED rõ ràng; index tests cập nhật. UI UX Pro Max guidance table/status đã đọc; bulk edit không áp dụng ngoài scope. Impeccable craft-floor đọc ngay trước UI edit. Review/polish toàn bộ surface sẽ chạy sau implementation cấu trúc được duyệt; không có browser proof mới trong preflight này.

**Verification:** explicit `SecurityFlowTests,AccountListServiceTests` **PASS 18 tests** (17+1), 0failure/error/skip, Maven exit0; compile + MVC test Context PASS. Log `target/account-separation-preflight.log`, Surefire reports. git diff --check đúng tracked files PASS. Lần đọc đầu dùng sai path report service package, đã xác định lại `TEST-com.group2.rms.service.AccountListServiceTests.xml`; không phải test failure. Không wildcard/test ghi DB thật; full Spring HTTP startup/JPA/live SQL/browser E2E NOT TESTED trong lượt này.

**Điểm tiếp tục:** nhận quyết định phân nhóm → shared query rule cho lists/dashboard → scoped guards/internal roles → candidate view/matcher admin → compile → UI summaries/sidebar → safe selected tests/SQL SELECT → hai independent reviews + batched browser four widths → một fix/confirmation → report 12 mục. Task chưa hoàn tất: frontend/backend hiện vẫn trộn nhóm. Không sửa dữ liệu để làm rule pass.

### Tiếp tục sau xác nhận lifecycle (cùng ngày)

User chốt: Candidate và Employee account lifecycle riêng, không đổi qua lại; khi hired tạo User mới và Department/internal role, Candidate cũ giữ lịch sử hoặc deactivation. Classification theo **current role**, không profile lịch sử. Trước sửa đã review oldupdate setRole+auto-createCandidate mismatch, dependency toàn project chỉ AccountManagementService gán role trong update. Không đổi record hiện có.

Backend: RoleAuthorities.INTERNAL_ROLE_NAMES shared5roles; AccountListService.findAccounts scoped internal, findRoles lọc internal; findCandidateAccounts scoped Candidate role với batch CandidateRepository.findAllByAccountUserIdIn và ProfileId/CreatedAt thực. Deactivate/deactivateCandidate kiểm tra target group, idempotent Inactive. AccountManagementService createInternal/updateInternal/findForEdit guards; genericupdate chặn đổi type trước field writes, không auto-create missing profile. Candidate không nhận Department. Controller nội bộ đổi sang guarded methods; new CandidateAccountController GET list/POST deactivate. SecurityConfig chỉ thêm matcher mới SystemAdmin; UU index không đụng. Dashboard thêm grouped statuses theo sharedrole và AccountSummary DTO; admin counts riêng/health separate, role khác không đổi.

UI: Internal title/subtitle/filter roles; internal form English/internalonly/nativeDepartmentrequired theo existingrule; bỏ account-form.js Candidate-special-case sau dependency search. Candidate Accounts mới không Department/role selector/Create/Edit fake, profile linkID/missingstate/createddate, existingDeactivateconfirmation. Sidebar groupAccounts/System thật, legacyRequisition fragment/mobile behavior giữ. Dashboard2summaryrows4counts,2cols mobile, health block riêng. JavaScript chỉdialog/disclosure, không account arrays. Auth/login/register/logout UI/logic không sửa.

Initial regression **41PASS/0fail/error/skip** explicit9suites. SELECT-only SQLServer actual **10internal/265Candidate**, allstatus counts match lists/repository/Dashboard. Compile earlyPASS. InitialrootChrome4width×5surfaces **112/112PASS**, files docs/tests/assets/2026-10-02-accounts/round1-*. No POST DBwrite browser. NativeCUA kernelfailed, ownCDPprivateprofile fallbackclosed. HTTP8771 freshattempt **FAIL** TomcatPipeImpl loopback/SocketException invalidargumentconnect, Mavenrunnerexit0 not HTTPPASS. Logtarget/account-separation-startup.log. Two isolatedagentreviews inprogress, one finalbatch planned; no productionready claim.

**Final:** thêm MockMvc forgedCandidateRole/internalCandidateId rejection, realProfileId/CreatedAt comparisons và legacydata SELECTaudit, CandidateDepartment guard. Final explicit9suite **42PASS/0failure/error/skip**, compile/MVC/JPA/SQLSELECTPASS; actual10internal/265Candidate, historicalinternalprofiles0/CandidateDepartments0. Logs target/account-separation-final-tests.log +XMLreports. NoDBwrite/seed/schema/config/auth redesign. node--check scripts/browserhelper +account-list.js/gitdiffcheckPASS.

**UI review:** A (/root/account_design_review) hoàn tất28/40, cognitive2fail,2P2 trước parent nhận B (/root/account_detector_review) findings. Aown5pages2widths/controlbatchclosed. B oneCLIscan3markup,2flat-typefalsepositives verified36/20/16px; one livehelper8400PID5244 inject3tabs aftermutableproof, rawfindings1/2/0 (tableinsetfalsepositives +candidateparagraphminor);126sampletext/mincontrast5.94/0fail, nativefocusPASS. Helperstop--keep-inject exit0/portclosed/browserprofile/tempscriptscleaned. Headlessno verifiedHuman tab. FullA/Breports +rawJSON/overlays retained. Impeccable fullscore delivered commentary, Questions skipped scopeauthorized.

**One finalfixbatch:** explicitsecondaryfiltergrid-span2, navgroupfullwidth inexistingmobile2cols, candidateparagraph70ch/spacing, fixtureEditCandidate6null→Interviewer5Dept1. Rootoneconfirmation **119/119PASS** 5surfaces×4width +dialogESC/Cancel/selectedfields/filterbaseline/navgroup. Finalshot Internal1440/menu375 viewed; noextraUIround. RootChromeprofileclosed, static8770CtrlCfinished(exit1expectedinterrupt/closedconnection), HTTP8771failedrunnerclosed. Impeccable archivewrite/latest/exactclosePASS: `.impeccable/critique/2026-10-02T02-19-13Z__ources-templates-admin-accounts-list-html-ba200b8e.md`; trend28/40→28/40 baseline scopesdifferent, no postscore invented, no tempbody (durable reportinput).

**Handoff:** current implementation +12topicreport `docs/design/2026-10-02-account-separation-report.md`; manualsteps/evidence `docs/tests/2026-10-02-account-separation.md`; actualclass/package/methodsteps `docs/flows/account-lifecycle-flow.md`. PRODUCT/currentflowindex updated; oldmergedflow notes markedhistorical. Rootonly deleted `static/js/account-form.js` after dependencysearch/nativeDepartmentrequired replacement; olddocsrefs historical explicitly marked. Scope code/UI done; liveHTTP/auth/accountmutation/SMTP E2E blocked environment, next work needs resolveTomcatloopback/runmanualA03/A04/A08/A13/A14 onapprovedtestDB. CandidateActivate/Edit/recovery/applicationcountsnotimplementedwithoutrequirement. Newemployeeusername/emailmustuniquetoCandidate(existingDBconstraints preserved). No remainingclassificationquestion; don't reversehistoricaldata automatically.

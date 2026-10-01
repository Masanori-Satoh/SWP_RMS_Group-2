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

**File code:** Thêm `RegistrationController`, `RegisterAccountRequest`,
`CandidateRegistrationService`, template Register; thêm
`PasswordRecoveryController`, `ForgotPasswordRequest`, `ResetPasswordRequest`,
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

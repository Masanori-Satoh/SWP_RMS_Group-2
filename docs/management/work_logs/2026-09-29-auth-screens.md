# 2026-09-29 — Sáu màn hình xác thực/tài khoản, giữ nguyên 20 bảng

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
`docs/tests/2026-09-29-auth-screens.md`; cập nhật `docs/management/TEST_PLAN.md`. Thêm
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

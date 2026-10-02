# Biên bản chuẩn bị test local Forgot/Reset Password — 30/09/2026

## Phạm vi và điều kiện đầu vào

Checkout `main`, commit nền `13b44be`; SQL Server local `RitirementManagement2` theo schema hiện có. Không chạy script schema, không gửi email thật, không nhập/xuất credential. Mục tiêu là chuẩn bị cấu hình đủ để người sở hữu mailbox tự chạy [kịch bản từng bước](../testing/password-reset-test-setup.md).

## Các bước đã thực hiện và kết quả

| Bước | Thao tác | Kỳ vọng | Kết quả thực tế | Trạng thái |
|---|---|---|---|---|
| C01 | Đọc `pom.xml`, cấu hình local, SecurityConfig, Controller, Service, Repository, User, schema và hai flow doc | Xác định đúng dependency/biến/route | Đã có `spring-boot-starter-mail`; dùng `SPRING_MAIL_*`, `APP_MAIL_FROM`, `APP_PUBLIC_BASE_URL`, `APP_PASSWORD_RESET_SECRET`; CSRF còn bật, GET/POST public | PASS |
| C02 | `mvn.cmd -q -DskipTests compile` | Main compile | Exit 0 | PASS |
| C03 | `mvn.cmd -q test` | Test suite qua; Context/JPA kết nối DB | Exit 0; 10 suite, 36 test, 0 failures/errors/skipped; Hikari kết nối SQL Server 16.0, EntityManagerFactory khởi tạo | PASS |
| C04 | `MailEnvironmentBindingTests` | Biến `SPRING_MAIL_*` bind vào `MailProperties`; MailSender bean được tạo khi có host | 2 test PASS, không gửi mail | PASS |
| C04b | `AuthenticationDatabaseTests` và kiểm tra script/code | Signing key ≥32 byte kích hoạt `PasswordResetService`; base URL dùng để tạo reset link | Test service PASS với key thử nghiệm; script đặt `APP_PUBLIC_BASE_URL` theo `-Port`. Chưa xác minh base URL qua email thật | PARTIAL |
| C05 | Parse `scripts/run-password-reset-local.ps1` và `git diff --check` | Script cú pháp đúng, patch không lỗi whitespace | Parser 0 lỗi; diff check PASS | PASS |
| C06 | `mvn.cmd -q spring-boot:run '-Dspring-boot.run.arguments=--server.port=18083'` | Web server STARTED, GET Forgot có thể truy cập | Context/JPA khởi tạo, nhưng Tomcat FAIL: `Unable to establish loopback connection`; root `java.net.SocketException: Invalid argument: connect`. Maven vẫn trả exit 0 | FAIL |
| C07 | Browser → SMTP → mailbox → reset → DB → login | Email được nhận, hash đổi, login password mới | Chưa có credential/mailbox do người dùng nhập; HTTP app chưa chạy được tại môi trường thực thi này | BLOCKED |

## Test ngoài hệ thống cần chạy tiếp

1. Khắc phục lỗi Tomcat loopback trên máy chạy; mở được `http://localhost:8082/forgot-password` sau khi log có `Started RmsApplication`. Nếu chọn cổng khác, dùng tham số `-Port` của script và URL cùng cổng.
2. Người sở hữu Gmail test tạo App Password, nhập trực tiếp tại prompt ẩn của script; không gửi cho Codex. Script tạo signing secret runtime và cấu hình SMTP/base URL.
3. Tạo/kiểm tra User Active theo tài liệu setup; chạy FORGOT-01–04 và RESET-01–06. Với trường hợp password cũ/mới, chỉ báo kết quả login; không ghi password hoặc hash.
4. Trong một phiên SSMS test, dùng temp table ở tài liệu setup để so sánh hash trước/sau mà chỉ in `CHANGED`/`UNCHANGED`. Điền từng case vào bảng kết quả của [danh mục flow](2026-09-30-flow-cases.md).

**Kết luận:** Cấu hình và test tự động sẵn sàng. `FLOW VERIFICATION: PARTIAL`; chưa có bằng chứng SMTP/mailbox/browser E2E. Không sửa schema hay code nghiệp vụ.

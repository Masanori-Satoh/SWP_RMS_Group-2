# Danh mục kiểm thử ngoài hệ thống cho tài liệu flow (30/09/2026)

Mỗi mã case có **Preconditions, Action, Input, Expected HTTP, Expected Result, DB Change** và **Test Procedure từng bước** trong file flow được liên kết. Chạy trên môi trường test với DB theo `database/schema/db.sql`, không dùng account production. Base URL lấy từ `server.port`/context path runtime; checkout hiện cấu hình `http://localhost:8082`.

| Nhóm | Mã case | Bước và kỳ vọng chi tiết |
|---|---|---|
| Login/protected URL | LOGIN-01 → LOGIN-05 | [authentication-login-flow.md](../flows/authentication-login-flow.md#external-test-cases) |
| Logout | LOGOUT-01 → LOGOUT-02 | [authentication-logout-flow.md](../flows/authentication-logout-flow.md#external-test-cases) |
| Candidate Register | REG-01 → REG-04 | [candidate-registration-flow.md](../flows/candidate-registration-flow.md#external-test-cases) |
| Forgot Password | FORGOT-01 → FORGOT-04 | [forgot-password-flow.md](../flows/forgot-password-flow.md#external-test-cases) |
| Reset Password | RESET-01 → RESET-06 | [reset-password-flow.md](../flows/reset-password-flow.md#external-test-cases) |
| Dashboard | DASH-01 → DASH-04 | [role-dashboard-flow.md](../flows/role-dashboard-flow.md#external-test-cases) |
| Account List | LIST-01 → LIST-03 | [account-list-flow.md](../flows/account-list-flow.md#external-test-cases) |
| Create Account | CREATE-01 → CREATE-04 | [create-account-flow.md](../flows/create-account-flow.md#external-test-cases) |
| Update Account | UPDATE-01 → UPDATE-04 | [update-account-flow.md](../flows/update-account-flow.md#external-test-cases) |
| Deactivate Account | DEACT-01 → DEACT-04 | [deactivate-account-flow.md](../flows/deactivate-account-flow.md#external-test-cases) |
| API Monitoring | MON-01 → MON-04 | [api-monitoring-flow.md](../flows/api-monitoring-flow.md#external-test-cases) |
| Schema validation/startup | SCHEMA-01 → SCHEMA-03 | [schema-validation-flow.md](../flows/schema-validation-flow.md#external-test-cases) |

**Tổng:** 47 case thủ công/ngoài hệ thống. Trạng thái: **CHƯA CHẠY** các case này; HTTP startup đã lỗi Tomcat loopback ở cổng 18082 và 18083 trong môi trường thực thi hiện tại. Test tự động là bằng chứng riêng, không chứng minh mailbox E2E. Điền bảng sau khi thực sự chạy, mỗi case một dòng; không chép token, cookie, mật khẩu, hash hoặc SMTP secret vào bằng chứng. Thiết lập cụ thể FORGOT/RESET tại [password-reset-test-setup.md](../testing/password-reset-test-setup.md).

| Ngày/giờ | Môi trường + commit | Case ID | PASS/FAIL/BLOCKED | Bằng chứng HTTP/UI/DB | Lỗi thực tế hoặc ghi chú |
|---|---|---|---|---|---|
|  |  |  |  |  |  |

## Thứ tự chạy khuyến nghị

1. Chạy `mvn.cmd test`; tách kết quả unit/MockMvc/Spring Context/JPA khỏi browser. Chỉ xác nhận HTTP server sau khi GET `/login` từ trình duyệt hoặc HTTP client thành công.
2. Chạy SCHEMA-01, LOGIN-01/02, rồi REG-01 và CREATE-01/02 để tạo account test. Thực hiện Dashboard/List với fixture đã chuẩn bị.
3. Chạy UPDATE/DEACT chỉ với account test có thể mất quyền đăng nhập. Kiểm tra DB bằng SELECT chỉ đọc; không chạy script `db.sql` vì đầu file có lệnh drop/create database.
4. Chạy FORGOT/RESET theo [hướng dẫn setup](../testing/password-reset-test-setup.md) khi có SMTP test và secret runtime. Chạy MON-03 chỉ trong môi trường cô lập, tránh gián đoạn DB đang dùng chung.
5. Ghi PASS/FAIL/BLOCKED theo kết quả thực tế; khi sửa code, cập nhật file flow và case liên quan cùng lần làm.

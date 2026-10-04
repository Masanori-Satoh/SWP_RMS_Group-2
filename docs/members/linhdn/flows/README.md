# Luồng kỹ thuật của RMS

Nguồn ban đầu: commit `13b44be` (30/09/2026), schema và Spring Boot `3.5.16`. Cập nhật career flow ngày 01/10/2026 theo working tree hiện tại. `PASS` trong test tự động không thay thế kiểm thử browser/SMTP/HTTP thật.

| Luồng | Tài liệu | Trạng thái xác minh |
|---|---|---|
| Department CRUD, Account restore, Inactive assignment và Admin Dashboard | [admin-departments-account-activation-flow.md](admin-departments-account-activation-flow.md) | 04/10: 45 Java/2 JS PASS; JPA SELECT PASS; Chrome fixture115/115; live CRUD chưa test |
| Account lifecycles: Internal/Candidate lists, scoped create/edit/deactivate và Admin counts | [account-lifecycle-flow.md](account-lifecycle-flow.md) | Current rule02/10; selected tests/JPA SELECT PASS; live HTTP blocked bởi environment |
| Preflight lịch sử trước business confirmation (02/10) | [2026-10-02-account-separation-preflight.md](2026-10-02-account-separation-preflight.md) | Historical18testgate; hiện dùng account-lifecycle-flow.md |
| Truy cập URL bảo vệ và đăng nhập | [authentication-login-flow.md](authentication-login-flow.md) | Mã nguồn; HTTP thực tế chưa chạy trong lượt này |
| Đăng xuất | [authentication-logout-flow.md](authentication-logout-flow.md) | Mã nguồn; HTTP thực tế chưa chạy |
| Candidate đăng ký | [candidate-registration-flow.md](candidate-registration-flow.md) | Mã nguồn; HTTP thực tế chưa chạy |
| Yêu cầu email khôi phục | [forgot-password-flow.md](forgot-password-flow.md) | Mã nguồn; SMTP thực tế chưa chạy |
| Xác minh link và đặt lại mật khẩu | [reset-password-flow.md](reset-password-flow.md) | Mã nguồn; SMTP/browser thực tế chưa chạy |
| Dashboard theo vai trò | [role-dashboard-flow.md](role-dashboard-flow.md) | Mã nguồn; browser thực tế chưa chạy |
| Candidate Dashboard tiếng Việt, scoped panel, filter và logout xác nhận | [candidate-dashboard-ui-flow.md](candidate-dashboard-ui-flow.md) | Verification theo tài liệu test ngày 04/10/2026 |
| Danh sách tài khoản | [account-list-flow.md](account-list-flow.md) | Mã nguồn; browser thực tế chưa chạy |
| Tạo tài khoản | [create-account-flow.md](create-account-flow.md) | Mã nguồn; browser thực tế chưa chạy |
| Cập nhật tài khoản | [update-account-flow.md](update-account-flow.md) | Mã nguồn; browser thực tế chưa chạy |
| Vô hiệu hóa tài khoản | [deactivate-account-flow.md](deactivate-account-flow.md) | Mã nguồn; browser thực tế chưa chạy |
| API Monitoring nội bộ | [api-monitoring-flow.md](api-monitoring-flow.md) | Mã nguồn; loopback HTTP thực tế chưa chạy |
| Kiểm tra mapping khi khởi động | [schema-validation-flow.md](schema-validation-flow.md) | Context/JPA PASS; HTTP startup FAIL tại máy kiểm thử |
| Career homepage → job detail → saved login URL → Candidate prefill | [career-homepage-apply-flow.md](career-homepage-apply-flow.md) | MockMvc/SELECT-only SQL Server PASS; browser static preview PASS; real HTTP/auth E2E chưa chạy |
| Shared Auth/Admin UI, error focus, password toggle, mobile menu/filter, Delete dialog | [auth-admin-ui-flow.md](auth-admin-ui-flow.md) | MockMvc/regression PASS; Chrome fixture verification riêng với real HTTP/auth/SMTP |

Job public browse/detail và apply prefill đã có CareerController; submission/CV upload, interview/offer workflows đầy đủ chưa có backend. Requisition code mới của teammate tồn tại nhưng ngoài phạm vi kiểm chứng career integration. AI Configuration chưa có form/controller. Khi code đổi URL/Filter/DTO/Service/Repository/schema/phản hồi, cập nhật flow và test tương ứng. Danh sách gốc ở `docs/tests/2026-09-30-flow-cases.md`; career cases C01–C18 ở `docs/tests/2026-10-01-career-integration.md`.

Thiết lập local để chạy thật chuỗi Forgot Password → SMTP → mailbox → Reset Password → Login: [password-reset-test-setup.md](../testing/password-reset-test-setup.md). Trạng thái hai flow này vẫn **PARTIAL** cho đến khi hoàn thành kiểm thử ngoài hệ thống.

# Work log — ứng viên tự cập nhật email

- Ngày 08/11/2026, nhánh `feature/update-ui-authen`.
- Người phụ trách LinhDN.
- [Báo cáo và bảng quyền theo vai trò](../../members/linhdn/2026-11-08-candidate-profile-email.md).

## Triển khai

1. Bỏ hướng dẫn liên hệ Quản trị viên trên `/portal/profile`. Cho sửa email trong form hiện tại và để tên đăng nhập chỉ đọc, không bind vào yêu cầu lưu.
2. Xác thực định dạng, độ dài tối đa 150 ký tự, báo lỗi cạnh ô email và giữ giá trị đã nhập. Yêu cầu từ form cũ không gửi email vẫn lưu các trường khác.
3. Dùng chung kiểm tra trùng email hoặc tên đăng nhập với quản lý tài khoản. Dùng ràng buộc `UQ_User_Email` để xử lý khi hai giao dịch cố lưu cùng địa chỉ. Cập nhật thông tin và ghi audit trong cùng giao dịch.
4. Đối chiếu quyền từ template nội bộ, route quản lý tài khoản và kiểm tra vai trò ở service. Các vai trò nội bộ xem email tại Hồ sơ cá nhân. Quản trị hệ thống chỉnh được email tài khoản nội bộ qua Quản lý tài khoản.

## Kiểm chứng

- Maven offline chạy 101/101 ca qua trong 7 bộ test Profile, Account và Password Reset. MockMvc kiểm tra render ô email, lỗi định dạng và lỗi trùng, gửi kèm tên đăng nhập giả, cùng các luồng lưu hồ sơ hiện có.
- Unit test xác nhận cập nhật email của ứng viên, chặn trùng với email hoặc tên đăng nhập, chuyển vi phạm ràng buộc trùng thành lỗi tại ô email và từ chối vai trò nội bộ ở luồng đổi email dành cho ứng viên.
- `git diff --check` qua. Chưa chạy trực tiếp qua trình duyệt hoặc SQL Server cho thay đổi này.

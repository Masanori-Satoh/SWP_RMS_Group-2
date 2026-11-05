# Thông báo sau đăng xuất — 2026-11-04

- Người thực hiện: LinhDN.
- Yêu cầu: hiển thị thông báo nổi “Bạn đã đăng xuất” thay vì chỉ chuyển tới màn hình đăng nhập.
- Trạng thái khi bắt đầu: đích logout trong source đã được đổi thành `/login` sau lượt sửa trước; giữ màn hình này và đổi thành `/login?logout` để kích hoạt thông báo.
- Mã nguồn: `SecurityConfig` gửi dấu hiệu logout tới trang đăng nhập; `auth/login.html` dùng fragment `flashToast` cùng CSS/JS có sẵn, thay cho dòng thông báo trong thẻ form. Toast tự ẩn sau 3 giây, tạm dừng khi hover/focus và có nút đóng. `auth.css` đặt toast dưới thanh đầu trang trên màn hình nhỏ.
- Kiểm thử: cập nhật `LogoutWebTests` theo redirect mới và bổ sung kiểm tra toast chỉ có trên `/login?logout`.
- Kiểm chứng trực tiếp bằng Playwright trên app `localhost:8081`: GET `/login?logout` trả 200 với toast, CSS và JS; xem ảnh desktop 1280×720 và mobile 390×844; toast tự gỡ khỏi DOM; nút đóng gỡ toast; HTML `/login` bình thường không chứa toast. POST `/logout` với CSRF hợp lệ từ phiên anonymous chuyển tới `/login?logout`. Chưa kiểm thử toàn bộ luồng đăng nhập rồi đăng xuất bằng tài khoản thật.
- Maven: `mvn -o "-Dtest=LogoutWebTests" test` hoàn tất compile/testCompile, sau đó không hoàn tất khởi tạo Spring/Mockito trong vài phút; đã dừng, không có kết quả pass/fail. `git diff --check` đạt.
- Database: không thay đổi.

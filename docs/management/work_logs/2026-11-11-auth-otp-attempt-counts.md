# Đồng bộ thông báo số lần thử OTP

Ngày thực hiện **2026-11-11**.

- `RegistrationVerificationService` và `PasswordRecoveryService` dùng bộ đếm trên máy chủ và chung định dạng thông báo. Số lần còn lại được tính theo giới hạn 5 lần hiện hành.
- Sau lần sai thứ 5, hệ thống xóa trạng thái xác minh và hiển thị số lượt còn lại bằng 0 ở trang Đăng ký hoặc Quên mật khẩu. Các lỗi định dạng được xử lý ở bước kiểm tra biểu mẫu trước khi xác minh OTP.
- Mẫu giao diện dùng lại khung báo lỗi và thông báo cạnh trường OTP hiện có. Nội dung được xuất dưới dạng văn bản đã escape.

## Kiểm chứng

- 53/53 test tập trung đạt, gồm 33 test dịch vụ đăng ký, 13 test xác minh OTP đặt lại mật khẩu và 7 test MockMvc. Test kiểm tra số đếm từ lần sai 1 đến lần 5 và HTML sau chuyển hướng.
- Lượt chạy rộng hơn đạt 66/73. Bảy test thời gian chờ gửi lại mã đặt lại mật khẩu vẫn dựa trên mốc 20 phút, trong khi mã nguồn trước thay đổi đã dùng 5 phút.
- Maven chạy với Mockito agent nạp sẵn để tránh lỗi gắn agent qua Windows pipe. `git diff --check` đạt. Chưa kiểm tra trực tiếp trên trình duyệt.

# Hiển thị số lần thử OTP

Hai luồng Đăng ký và Quên mật khẩu hiển thị số lần nhập sai và số lần còn lại ngay trong thông báo lỗi hiện có. Sau lần nhập sai đầu tiên, thông báo hiện “Bạn đã nhập sai 1 lần, còn 4 lần thử.”

Sau lần sai thứ 5, mã bị vô hiệu hóa và thông báo hiện “Bạn đã nhập sai 5 lần, còn 0 lần thử.” Luồng Đăng ký hướng dẫn đăng ký lại. Luồng Quên mật khẩu hướng dẫn yêu cầu mã mới.

[Chi tiết triển khai và kiểm chứng](../../management/work_logs/2026-11-11-auth-otp-attempt-counts.md)

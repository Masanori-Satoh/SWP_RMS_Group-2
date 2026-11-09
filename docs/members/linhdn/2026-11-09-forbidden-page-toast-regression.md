# Khôi phục toast khi mở trang ngoài quyền

Sau merge `e85273c1`, `SecurityConfig` cấu hình hai bộ xử lý từ chối truy cập. Cấu hình cho phiên hết hạn được gọi sau nên ghi đè `PageAccessDeniedHandler`. Do đó, khi Spring Security chặn truy cập theo URL, trình duyệt nhận trang lỗi 403 toàn màn hình.

Spring Security nay dùng một bộ xử lý. Nếu yêu cầu gửi mã phiên đã hết hạn và lỗi CSRF xảy ra khi chưa đăng nhập, bộ xử lý chuyển về `/login?session-expired`. Với các trường hợp còn lại, nó gọi `PageAccessDeniedHandler` để áp dụng lại luồng điều hướng và toast hiện có.

Ứng viên mở trang quản trị sẽ về **Đơn ứng tuyển của tôi** và thấy thông báo “Bạn không có quyền truy cập vào trang này.” một lần. Tài khoản nội bộ và quản trị viên về màn hình tương ứng với vai trò. API, iframe và yêu cầu sai CSRF của tài khoản đang đăng nhập tiếp tục nhận 403.

[Nhật ký và kiểm chứng](../../management/work_logs/2026-11-09-forbidden-page-toast-regression.md)

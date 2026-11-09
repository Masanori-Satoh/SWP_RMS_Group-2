# Khôi phục toast lỗi phân quyền ngày 09/11/2026

## Phạm vi

- Đối chiếu `7a871659` với merge `e85273c1` và xác định cấu hình `accessDeniedHandler` được gọi lần thứ hai đã thay bộ xử lý toast bằng bộ xử lý phiên hết hạn.
- Gỡ đăng ký trùng trong `SecurityConfig` và dùng `PageAccessDeniedHandler` làm nhánh xử lý sau kiểm tra CSRF của phiên hết hạn. Dùng lại điều kiện nhận diện điều hướng GET HTML, đích theo vai trò và FlashMap của luồng ngày 08/11.
- Thêm ca kiểm thử ứng viên mở `/admin/accounts`, đi theo chuyển hướng tới `/portal/applications` và xác nhận toast biến mất ở lần tải kế tiếp.

## Kiểm chứng

- Trước khi sửa, 2 trong 22 ca kiểm thử thất bại. Cả Admin mở danh sách hồ sơ và Nhân sự mở trang quản trị đều nhận 403 thay vì chuyển hướng 303.
- Sau khi sửa, Maven offline chạy thành công 43/43 ca trong `MyApplicationWebTests`, `AdminDashboardViewTests`, `InternalDashboardControllerTests`, `LogoutWebTests` và `ErrorPagesTests`. Hai ca lỗi ban đầu và ca ứng viên mới đều đạt. Bộ kiểm thử cũng xác nhận API, iframe và CSRF trả mã phù hợp, còn form của phiên hết hạn chuyển về đăng nhập.
- `git diff --check` đạt. Phạm vi kiểm chứng là MockMvc và kiểm thử trang lỗi. Chưa kiểm tra trực tiếp trên trình duyệt.

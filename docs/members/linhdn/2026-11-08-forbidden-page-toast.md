# Thông báo khi mở trang ngoài quyền

Khi người đã đăng nhập mở một trang HTML ngoài quyền, hệ thống đưa họ về màn hình đúng vai trò và hiển thị toast “Bạn không có quyền truy cập vào trang này.” Ví dụ, HR mở `/admin/candidate-accounts` sẽ về bảng điều khiển nội bộ; System Admin mở `/applications` sẽ về tổng quan hệ thống.

Dùng lại fragment `flashToast` đã có ở các màn Tài khoản, Phòng ban và Tin tuyển dụng. Thông báo được lưu cho lượt chuyển trang và tự biến mất theo cơ chế toast hiện tại. Các màn Phỏng vấn đang hiện banner riêng vẫn giữ cách hiển thị đó.

Quy tắc phân quyền giữ nguyên. Yêu cầu API, tải CV, iframe, thao tác ghi và lỗi CSRF giữ đường xử lý lỗi hiện tại. Khách mở URL cần đăng nhập vẫn được đưa về màn đăng nhập.

# Thông báo khi mở trang ngoài quyền — 2026-11-08

- Người thực hiện: LinhDN.
- Phạm vi: lượt điều hướng GET tới trang HTML bị từ chối sau khi đăng nhập. Xử lý cả chặn URL trong Spring Security và `AccessDeniedException` đến bộ xử lý lỗi MVC chung.
- `PageAccessDeniedHandler` dùng đích dashboard theo vai trò đã có; ứng viên về Đơn ứng tuyển của tôi. FlashMap chỉ áp dụng tại đích và chỉ hiển thị một lần. Nếu đích cũng bị chặn, handler giữ phản hồi 403 để tránh lặp.
- Layout nội bộ và ứng viên dùng lại fragment, CSS và JS của `flashToast`. Không đổi quyền, dữ liệu hay schema.
- Kiểm thử tập trung: Admin mở danh sách hồ sơ và thấy toast một lần trên tổng quan; HR mở quản lý ứng viên; ứng viên mở dashboard nội bộ; giữ 403 với API và iframe.
- Maven offline: 28/28 ca đạt trong 5 bộ test dashboard, logout và trang lỗi. Lượt chạy cuối nạp Mockito agent lúc khởi động JVM để tránh lỗi attach của Windows. `git diff --check` đạt. Chưa thử trực tiếp trên trình duyệt với dữ liệu thật.

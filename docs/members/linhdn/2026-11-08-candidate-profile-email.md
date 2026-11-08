# Cập nhật email trên Hồ sơ cá nhân của ứng viên

Ứng viên tự đổi email trên `/portal/profile`, kể cả khi hồ sơ đã được đánh dấu tiềm năng. Tên đăng nhập chỉ đọc. Form dùng nút “Lưu thay đổi” hiện có và bỏ dòng hướng dẫn liên hệ Quản trị viên.

Email được kiểm tra định dạng, giới hạn 150 ký tự và kiểm tra trùng với email hoặc tên đăng nhập của tài khoản khác. Sau khi lưu, email mới có hiệu lực cho liên hệ, đăng nhập và khôi phục mật khẩu. Form lưu trực tiếp trong phiên đăng nhập hiện tại.

| Vai trò | Sửa email ở Hồ sơ cá nhân | Sửa email qua Quản lý tài khoản |
| --- | --- | --- |
| Ứng viên | Có, email của mình | Không có quyền |
| Nhân sự, Trưởng phòng tuyển dụng, Giám đốc, Người phỏng vấn | Email chỉ đọc | Không có quyền |
| Quản trị hệ thống | Email chỉ đọc | Sửa được email tài khoản nội bộ. Quản lý tài khoản ứng viên hỗ trợ kích hoạt và vô hiệu hóa. |

[Nhật ký triển khai và kiểm chứng](../../management/work_logs/2026-11-08-candidate-profile-email.md)

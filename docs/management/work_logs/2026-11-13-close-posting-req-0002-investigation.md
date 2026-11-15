# 2026-11-13 — Kiểm tra đóng tin REQ-0002

- Truy ngược thông báo từ ảnh tới điều kiện chặn trước khi chuyển trạng thái tin.
- Đối chiếu seed: hồ sơ #70 có thư mời #16 chờ Giám đốc duyệt; hai thư mời được chấp nhận không chặn.
- Phát hiện điều kiện truy vấn chưa bỏ qua đề xuất đã xóa mềm. Chưa có bằng chứng đây là nguyên nhân của REQ-0002.
- Kiểm chứng tĩnh trên mã nguồn, schema và seed; không kết nối được SQL Server local từ phiên này. Không sửa mã hoặc dữ liệu.

[Báo cáo và SQL đối chiếu](../../members/linhdn/2026-11-13-close-posting-req-0002-investigation.md)

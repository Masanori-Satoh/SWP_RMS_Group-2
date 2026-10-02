# Nhật ký Công việc (Work Log)

Tài liệu này là lịch sử tóm tắt các thay đổi quan trọng và bàn giao của project. Sau **mỗi lần làm việc**, hãy thêm một mục mới vào bảng dưới đây và tạo một file chi tiết trong thư mục `work_logs/` để tham chiếu.

## Lịch sử Thay đổi

| Ngày | Tên công việc / Yêu cầu chính | Người thực hiện | Chi tiết thay đổi |
| **2026-10-03** | Sửa lỗi Droplist Profile & Đăng xuất trên Topbar `/interviews` | Đức | [Xem chi tiết](work_logs/2026-10-03-fix-interview-topbar-dropdown.md) |
| **2026-10-03** | Đồng bộ Module Lịch phỏng vấn với Phân quyền & Bảo mật của Linh | Đức | [Xem chi tiết](work_logs/2026-10-03-interview-sync-linhdn-security.md) |
| **2026-10-03** | Tái cấu trúc Module Lịch phỏng vấn & Sửa lỗi Frontend/Backend | Đức | [Xem chi tiết](work_logs/2026-10-03-interview-refactor-and-bugfix.md) |
| **2026-10-02** | Cải tiến Giao diện & Tách CSS/JS Lịch phỏng vấn | Đức | [Xem chi tiết](work_logs/2026-10-02-interview-schedule-ui-refactor.md) |
| **2026-10-02** | Xây dựng Public Job Board & Details chuẩn kiến trúc | dunglt | [Xem chi tiết](work_logs/2026-10-02-plan-job-board.md) |
| **2026-10-01** | Đại phẫu Kiến trúc & Database | Nhóm 2 | [Xem chi tiết](work_logs/2026-10-01-architecture-refactor.md) |
| **2026-09-29** | Sáu màn hình xác thực/tài khoản | Nhóm 2 | [Xem chi tiết](work_logs/2026-09-29-auth-screens.md) |
| **2026-09-29** | Tài liệu bàn giao và kịch bản test | Nhóm 2 | [Xem chi tiết](work_logs/2026-09-29-documentation-update.md) |
| **2026-09-29** | Đồng bộ project theo database mới | Nhóm 2 | [Xem chi tiết](work_logs/2026-09-29-database-sync.md) |

---

### Quy trình cập nhật

Mỗi khi có một cụm thay đổi lớn, cần tạo mới một file Markdown trong thư mục `work_logs/` theo định dạng `YYYY-MM-DD-ten-cong-viec.md`. File này phải ghi rõ:
1. **Người thực hiện:** Ai là người code hoặc tổng hợp tài liệu.
2. **Yêu cầu/Quyết định:** Lý do tại sao thực hiện.
3. **Database:** Có thay đổi gì tới cấu trúc DB, script seed không.
4. **Mã nguồn/Tài liệu:** Đã tạo, xóa hay sửa những module nào.
5. **Kiểm tra (Testing):** Cách kiểm tra (Run tay, Unit Test) và kết quả.
6. **Vấn đề tồn đọng:** Việc cần làm tiếp theo.

Sau đó, thêm 1 dòng vào bảng trên với liên kết tới file bạn vừa tạo.

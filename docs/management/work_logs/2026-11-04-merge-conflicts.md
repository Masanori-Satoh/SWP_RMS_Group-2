# Gỡ conflict merge — 2026-11-04

## Phạm vi

- `WORK_LOG.md`: giữ cả hai mục lịch sử logout ngày 04/11 từ nhánh hiện tại.
- `RecruitmentEmailService.java`: giữ phần dựng HTML thư mời phỏng vấn từ nhánh được merge, tương thích với DTO có trường `html`, template email và nhánh gửi MIME đã tự merge. Giữ nội dung chữ thuần, điều kiện lịch/đơn đủ điều kiện gửi và cơ chế audit.
- Chỉ đánh dấu các file conflict và nhật ký lần xử lý này; chưa tạo commit merge hay push.

## Kiểm chứng

- Mã Java chính và mã test biên dịch thành công.
- Chạy `./mvnw.cmd -Dtest=InterviewScheduleFormWebTests,InterviewPotentialWebTests,AuditLogLabelsTests test`: `AuditLogLabelsTests` qua 6/6. Nhóm web chưa có kết quả; tiến trình đứng sau khi khởi tạo Mockito/Spring, Surefire ghi lỗi attach không mở được Windows pipe (access denied). Đã yêu cầu dừng lượt chạy; không tính nhóm web là đã qua.
- Không còn dấu conflict trong `src` và `WORK_LOG.md`; `git diff --name-only --diff-filter=U` không còn file; `git diff --cached --check` qua sau khi stage kết quả.
- Chưa kiểm chứng gửi SMTP thực tế hoặc hiển thị trong ứng dụng email.

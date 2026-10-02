# Cập nhật Quy trình làm việc (2026-10-01)

Tài liệu này hướng dẫn quy trình làm việc mới, tiêu chuẩn code và các chỉ dẫn cụ thể dành cho cả thành viên trong team và AI Agents.

## 1. Hướng dẫn Code (Dành cho Lập trình viên & AI Agent)

**Kiến trúc & Tiêu chuẩn:**
- Tuân thủ tuyệt đối cấu trúc **Package-by-Feature**.
- **Tính năng cũ/hiện có:** Phải rà soát và sắp xếp code vào đúng thư mục feature tương ứng (VD: `admin`, `auth`, `candidate`, `core`, `dashboard`, `interview`, `offer`, `requisition`, `user`).
- **Tính năng mới:** Cần cân nhắc việc tạo package feature mới hay gom vào package cũ. **Bắt buộc** phải hỏi ý kiến người dùng hoặc báo lên nhóm trước khi quyết định tạo/tách module mới.
- Mọi DTO phải đặt trực tiếp vào thư mục `dto/` của từng feature, tuyệt đối không tạo thêm thư mục con như `request/` hay `response/`.

**Quy trình Ghi Log Công việc & Xử lý rủi ro `WORK_LOG.md`:**
- File `WORK_LOG.md` là Mục lục chung nên rất dễ bị **Git Conflict** nếu nhiều người cùng push code vào cuối ngày. Để tránh mất dữ liệu, **bắt buộc** làm theo các bước sau:
  1. **Tạo file chi tiết trước:** Viết file báo cáo trong thư mục `docs/management/logs/` (VD: `YYYY-MM-DD-ten-task.md`) và ghi rõ "Người thực hiện".
  2. **Pull code trước khi ghi log:** Trước khi sửa `WORK_LOG.md`, hãy chạy `git pull` để lấy cập nhật mới nhất từ nhánh chung.
  3. **Thêm log vào bảng:** Bổ sung một dòng tóm tắt vào bảng mục lục trong `docs/management/WORK_LOG.md` kèm link dẫn tới file báo cáo của bạn.
  4. **Xử lý Conflict (Nếu có):** Nếu lúc `git push` bị báo lỗi conflict ở file `WORK_LOG.md`, tuyệt đối **không được chọn "Accept Yours" (Ghi đè của mình)** làm mất dòng log của người khác. Phải chọn "Accept Both Changes" và chỉnh lại bảng cho ngay ngắn rồi mới push tiếp.

---

## 2. Chỉ dẫn nạp ngữ cảnh cho AI (Agent Instructions)

*Ghi chú cho AI: Trước khi tiến hành sửa code hay phân tích, AI BẮT BUỘC phải đọc các tài liệu dưới đây để nắm được bối cảnh, cấu trúc và thiết kế mới nhất của dự án:*

- **Để hiểu Cơ sở dữ liệu (Database):** 
  Hãy đọc file `database/schema/db.sql` (source of truth của DB) và `docs/database/model.md` để hiểu cấu trúc bảng, các mối quan hệ và mapping hiện tại. Đừng tự suy diễn cấu trúc.
- **Để hiểu Kiến trúc và Quy chuẩn (Architecture):** 
  Hãy đọc `docs/architecture/ARCHITECTURE_GUIDE.md`. Đây là file quy định cách chia package, cách đặt tên Class/Controller/Entity, và triết lý thiết kế của hệ thống.
- **Để hiểu Tổ chức tài liệu (Docs Organization):** 
  Hãy đọc phần "TÀI LIỆU DỰ ÁN (DOCS HUB)" trong file `README.md` (thư mục gốc). Thư mục `docs/` đã được cơ cấu rõ ràng, AI phải tuân thủ và không được tạo file rác sai vị trí.

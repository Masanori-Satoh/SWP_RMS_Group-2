-- 015: Thêm thời gian làm việc cho tin tuyển dụng (hoangnh, 02/11).
-- HR nhập 1 dòng chữ tự do, ví dụ "Thứ 2 - Thứ 6 (09:00 - 18:00)"; NULL = không hiển thị.
-- Đồng bộ với database/schema/db.sql. Chạy trên database mà app đang cấu hình (không có USE).
-- Chạy lại nhiều lần vẫn an toàn.
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.JobPosting') AND name = N'WorkingHours')
    ALTER TABLE dbo.JobPosting ADD WorkingHours NVARCHAR(255) NULL;
GO

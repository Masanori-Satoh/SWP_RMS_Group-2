-- 015: Ứng viên xác nhận đồng ý Điều khoản sử dụng + Chính sách bảo mật (hoangnh, 04/11).
-- Candidate.PolicyAgreedAt: NULL = chưa đồng ý (popup hiện khi đăng nhập), có giá trị = lúc bấm "Tôi đồng ý".
-- Đồng bộ với database/schema/db.sql. Chạy trên database mà app đang cấu hình (không có USE).
-- Chạy lại nhiều lần vẫn an toàn.
SET XACT_ABORT ON;
SET NOCOUNT ON;

IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID(N'dbo.Candidate') AND name = N'PolicyAgreedAt')
    ALTER TABLE dbo.Candidate ADD PolicyAgreedAt DATETIME2 NULL;
GO

-- =============================================================================
-- 009: Drop JobRequisition.ExpectedStartDate (Ngày bắt đầu dự kiến)
-- Loại bỏ trường Ngày bắt đầu dự kiến khỏi JobRequisition theo yêu cầu nghiệp vụ.
-- Chạy lại nhiều lần vẫn an toàn (idempotent).
-- =============================================================================
USE RitirementManagement2;
GO

IF EXISTS (
    SELECT 1 
    FROM sys.columns 
    WHERE object_id = OBJECT_ID(N'dbo.JobRequisition') 
      AND name = N'ExpectedStartDate'
)
BEGIN
    ALTER TABLE dbo.JobRequisition DROP COLUMN ExpectedStartDate;
    PRINT N'Đã xóa cột ExpectedStartDate khỏi bảng JobRequisition thành công.';
END
ELSE
BEGIN
    PRINT N'Cột ExpectedStartDate không tồn tại trong bảng JobRequisition hoặc đã được xóa trước đó.';
END
GO

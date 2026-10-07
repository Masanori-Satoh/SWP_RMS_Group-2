-- =============================================================================
-- Migration 007: Thêm các cột còn thiếu cho JobRequisition và OfferProposal
-- JobRequisition: SubmittedAt, DecidedAt
-- OfferProposal: IsDeleted
-- =============================================================================

-- 1. Bổ sung các cột thời gian cho quy trình phê duyệt JobRequisition
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'JobRequisition' AND COLUMN_NAME = 'SubmittedAt')
BEGIN
    ALTER TABLE dbo.JobRequisition ADD SubmittedAt DATETIME2 NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'JobRequisition' AND COLUMN_NAME = 'DecidedAt')
BEGIN
    ALTER TABLE dbo.JobRequisition ADD DecidedAt DATETIME2 NULL;
END
GO

-- 2. Bổ sung cờ xóa mềm (IsDeleted) cho OfferProposal
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'OfferProposal' AND COLUMN_NAME = 'IsDeleted')
BEGIN
    ALTER TABLE dbo.OfferProposal ADD IsDeleted BIT NULL CONSTRAINT DF_OfferProposal_IsDeleted DEFAULT 0;
END
GO

-- Cập nhật giá trị mặc định cho dữ liệu cũ
UPDATE dbo.OfferProposal SET IsDeleted = 0 WHERE IsDeleted IS NULL;
GO

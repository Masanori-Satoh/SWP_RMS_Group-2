-- =============================================================================
-- 006: Thêm cột xóa mềm OfferProposal.IsDeleted (đồng bộ với database/schema/db.sql)
-- Chạy lại nhiều lần vẫn an toàn.
-- =============================================================================
USE RitirementManagement2;
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.OfferProposal') AND name = N'IsDeleted')
    ALTER TABLE dbo.OfferProposal
        ADD IsDeleted BIT NOT NULL CONSTRAINT DF_OfferProposal_IsDeleted DEFAULT 0;
GO

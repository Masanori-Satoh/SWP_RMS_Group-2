-- =============================================================================
-- 008: OfferProposal.ResponsePeriodDays + ResponseDeadline (hạn ứng viên phản hồi Offer, PR #70)
-- Đồng bộ với database/schema/db.sql. Chạy lại nhiều lần vẫn an toàn.
-- DB tạo trước PR #70 thiếu 2 cột này nên app không khởi động (ddl-auto=validate).
-- =============================================================================
USE RitirementManagement2;
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.OfferProposal') AND name = N'ResponsePeriodDays')
    ALTER TABLE dbo.OfferProposal ADD ResponsePeriodDays INT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.OfferProposal') AND name = N'ResponseDeadline')
    ALTER TABLE dbo.OfferProposal ADD ResponseDeadline DATETIME2 NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_OfferProposal_ResponsePeriodDays')
    ALTER TABLE dbo.OfferProposal
        ADD CONSTRAINT CK_OfferProposal_ResponsePeriodDays
            CHECK (ResponsePeriodDays IS NULL OR ResponsePeriodDays > 0);
GO

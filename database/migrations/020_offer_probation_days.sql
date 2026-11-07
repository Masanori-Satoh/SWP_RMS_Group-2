-- Persist the probation duration entered by HR.
-- Existing offers retain the 60-day value previously displayed by the application.
SET XACT_ABORT ON;
SET NOCOUNT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.OfferProposal', N'U') IS NULL
        THROW 50001, 'OfferProposal table does not exist.', 1;
    IF COL_LENGTH(N'dbo.OfferProposal', N'ProbationDays') IS NULL
        ALTER TABLE dbo.OfferProposal ADD ProbationDays INT NOT NULL
            CONSTRAINT DF_OfferProposal_ProbationDays DEFAULT 60 WITH VALUES;
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
                   WHERE parent_object_id = OBJECT_ID(N'dbo.OfferProposal')
                     AND name = N'CK_OfferProposal_ProbationDays')
        EXEC sys.sp_executesql N'ALTER TABLE dbo.OfferProposal WITH CHECK
            ADD CONSTRAINT CK_OfferProposal_ProbationDays CHECK (ProbationDays BETWEEN 1 AND 365)';
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

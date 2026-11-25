-- Add Priority column to JobPosting (3 = High, 2 = Medium, 1 = Low).
SET XACT_ABORT ON;
SET NOCOUNT ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;

BEGIN TRY
    BEGIN TRANSACTION;

    IF COL_LENGTH(N'dbo.JobPosting', N'Priority') IS NULL
    BEGIN
        ALTER TABLE dbo.JobPosting ADD
            Priority INT NOT NULL CONSTRAINT DF_JobPosting_Priority DEFAULT 1;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
                   WHERE parent_object_id = OBJECT_ID(N'dbo.JobPosting')
                     AND name = N'CK_JobPosting_Priority')
    BEGIN
        ALTER TABLE dbo.JobPosting WITH CHECK ADD
            CONSTRAINT CK_JobPosting_Priority
            CHECK (Priority IN (1, 2, 3));
    END;

    IF EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID(N'dbo.JobPosting') AND name = N'IX_JobPosting_Public')
        DROP INDEX IX_JobPosting_Public ON dbo.JobPosting;

    CREATE NONCLUSTERED INDEX IX_JobPosting_Public
        ON dbo.JobPosting(PostingStatus, ApplicationDeadline)
        INCLUDE (PostingTitle, WorkLocation, SalaryDisplay, NumberOfPositionsDisplay, PostingDate, Priority);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

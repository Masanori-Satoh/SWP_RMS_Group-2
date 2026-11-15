-- Store the displayed number of positions as a positive integer.
-- Existing values such as "2 người" become 2; NULL remains NULL.
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

    IF COL_LENGTH(N'dbo.JobPosting', N'NumberOfPositionsDisplay') IS NULL
        THROW 50001, 'JobPosting.NumberOfPositionsDisplay is missing.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID(N'dbo.JobPosting')
                 AND name = N'NumberOfPositionsDisplay'
                 AND system_type_id <> 56)
    BEGIN
        -- Build " người" from code points so sqlcmd code pages cannot change the suffix.
        DECLARE @PeopleSuffix NVARCHAR(10) = NCHAR(32) + NCHAR(110) + NCHAR(103)
            + NCHAR(432) + NCHAR(7901) + NCHAR(105);
        IF EXISTS (SELECT 1 FROM dbo.JobPosting
                   WHERE NumberOfPositionsDisplay IS NOT NULL
                     AND (TRY_CONVERT(INT, REPLACE(TRIM(NumberOfPositionsDisplay), @PeopleSuffix, N'')) IS NULL
                          OR TRY_CONVERT(INT, REPLACE(TRIM(NumberOfPositionsDisplay), @PeopleSuffix, N'')) <= 0))
            THROW 50002, 'Some displayed position counts cannot be converted to positive integers.', 1;

        IF EXISTS (SELECT 1 FROM sys.indexes
                   WHERE object_id = OBJECT_ID(N'dbo.JobPosting') AND name = N'IX_JobPosting_Public')
            DROP INDEX IX_JobPosting_Public ON dbo.JobPosting;

        UPDATE dbo.JobPosting
           SET NumberOfPositionsDisplay = REPLACE(TRIM(NumberOfPositionsDisplay), @PeopleSuffix, N'')
         WHERE NumberOfPositionsDisplay IS NOT NULL;

        ALTER TABLE dbo.JobPosting ALTER COLUMN NumberOfPositionsDisplay INT NULL;
    END;

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
                   WHERE parent_object_id = OBJECT_ID(N'dbo.JobPosting')
                     AND name = N'CK_JobPosting_NumberOfPositionsDisplay')
        ALTER TABLE dbo.JobPosting WITH CHECK ADD
            CONSTRAINT CK_JobPosting_NumberOfPositionsDisplay
            CHECK (NumberOfPositionsDisplay IS NULL OR NumberOfPositionsDisplay > 0);

    IF NOT EXISTS (SELECT 1 FROM sys.indexes
                   WHERE object_id = OBJECT_ID(N'dbo.JobPosting') AND name = N'IX_JobPosting_Public')
        CREATE NONCLUSTERED INDEX IX_JobPosting_Public
            ON dbo.JobPosting(PostingStatus, ApplicationDeadline)
            INCLUDE (PostingTitle, WorkLocation, SalaryDisplay, NumberOfPositionsDisplay, PostingDate);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

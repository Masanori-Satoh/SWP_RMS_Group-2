-- Replace requisition rounds with an estimated staffing date and remove
-- gender/probation fields from requisitions and job postings.
-- Existing requisitions keep a NULL estimated date; users supply one on new submissions.
SET XACT_ABORT ON;
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.JobRequisition', N'U') IS NULL
       OR OBJECT_ID(N'dbo.JobPosting', N'U') IS NULL
        THROW 50001, 'JobRequisition or JobPosting is missing.', 1;

    IF COL_LENGTH(N'dbo.JobRequisition', N'ExpectedStaffingDate') IS NULL
        ALTER TABLE dbo.JobRequisition ADD ExpectedStaffingDate DATE NULL;

    IF EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID(N'dbo.JobRequisition')
                 AND name = N'UX_JobRequisition_Original_Round')
        DROP INDEX UX_JobRequisition_Original_Round ON dbo.JobRequisition;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes
                   WHERE object_id = OBJECT_ID(N'dbo.JobRequisition')
                     AND name = N'IX_JobRequisition_Original')
        CREATE NONCLUSTERED INDEX IX_JobRequisition_Original
            ON dbo.JobRequisition(OriginalRequisitionId)
            WHERE OriginalRequisitionId IS NOT NULL;

    IF EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID(N'dbo.JobRequisition')
                 AND name = N'CK_JobRequisition_RecruitmentRound')
        ALTER TABLE dbo.JobRequisition DROP CONSTRAINT CK_JobRequisition_RecruitmentRound;
    IF EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID(N'dbo.JobRequisition')
                 AND name = N'CK_JobRequisition_RequiredGender')
        ALTER TABLE dbo.JobRequisition DROP CONSTRAINT CK_JobRequisition_RequiredGender;
    IF EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID(N'dbo.JobPosting')
                 AND name = N'CK_JobPosting_RequiredGender')
        ALTER TABLE dbo.JobPosting DROP CONSTRAINT CK_JobPosting_RequiredGender;

    DECLARE @dropDefaults NVARCHAR(MAX) = N'';
    SELECT @dropDefaults += N'ALTER TABLE dbo.' + QUOTENAME(OBJECT_NAME(dc.parent_object_id))
        + N' DROP CONSTRAINT ' + QUOTENAME(dc.name) + N';'
    FROM sys.default_constraints dc
    JOIN sys.columns c ON c.object_id = dc.parent_object_id
                      AND c.column_id = dc.parent_column_id
    WHERE (dc.parent_object_id = OBJECT_ID(N'dbo.JobRequisition')
           AND c.name IN (N'RecruitmentRound', N'RequiredGender', N'ProbationPeriod'))
       OR (dc.parent_object_id = OBJECT_ID(N'dbo.JobPosting')
           AND c.name IN (N'RequiredGender', N'ProbationPeriod'));
    IF @dropDefaults <> N'' EXEC sys.sp_executesql @dropDefaults;

    IF COL_LENGTH(N'dbo.JobRequisition', N'RecruitmentRound') IS NOT NULL
        ALTER TABLE dbo.JobRequisition DROP COLUMN RecruitmentRound;
    IF COL_LENGTH(N'dbo.JobRequisition', N'RequiredGender') IS NOT NULL
        ALTER TABLE dbo.JobRequisition DROP COLUMN RequiredGender;
    IF COL_LENGTH(N'dbo.JobRequisition', N'ProbationPeriod') IS NOT NULL
        ALTER TABLE dbo.JobRequisition DROP COLUMN ProbationPeriod;
    IF COL_LENGTH(N'dbo.JobPosting', N'RequiredGender') IS NOT NULL
        ALTER TABLE dbo.JobPosting DROP COLUMN RequiredGender;
    IF COL_LENGTH(N'dbo.JobPosting', N'ProbationPeriod') IS NOT NULL
        ALTER TABLE dbo.JobPosting DROP COLUMN ProbationPeriod;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

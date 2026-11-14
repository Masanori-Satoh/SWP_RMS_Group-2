-- Rename the employment type Contract to Freelance in requisitions and postings.
-- Keep both updates and both trusted constraints in one transaction.
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

    IF OBJECT_ID(N'dbo.JobRequisition', N'U') IS NULL
       OR OBJECT_ID(N'dbo.JobPosting', N'U') IS NULL
        THROW 50001, 'JobRequisition or JobPosting is missing.', 1;

    IF EXISTS (SELECT 1 FROM dbo.JobRequisition
               WHERE EmploymentType NOT IN (N'Full-time', N'Part-time', N'Internship',
                                            N'Contract', N'Freelance'))
       OR EXISTS (SELECT 1 FROM dbo.JobPosting
                  WHERE EmploymentType IS NOT NULL
                    AND EmploymentType NOT IN (N'Full-time', N'Part-time', N'Internship',
                                               N'Contract', N'Freelance'))
        THROW 50002, 'An unknown EmploymentType must be resolved first.', 1;

    IF EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID(N'dbo.JobRequisition')
                 AND name = N'CK_JobRequisition_EmploymentType')
        ALTER TABLE dbo.JobRequisition DROP CONSTRAINT CK_JobRequisition_EmploymentType;

    IF EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID(N'dbo.JobPosting')
                 AND name = N'CK_JobPosting_EmploymentType')
        ALTER TABLE dbo.JobPosting DROP CONSTRAINT CK_JobPosting_EmploymentType;

    UPDATE dbo.JobRequisition SET EmploymentType = N'Freelance'
    WHERE EmploymentType = N'Contract';

    UPDATE dbo.JobPosting SET EmploymentType = N'Freelance'
    WHERE EmploymentType = N'Contract';

    ALTER TABLE dbo.JobRequisition WITH CHECK ADD CONSTRAINT CK_JobRequisition_EmploymentType
        CHECK (EmploymentType IN (N'Full-time', N'Part-time', N'Internship', N'Freelance'));

    ALTER TABLE dbo.JobPosting WITH CHECK ADD CONSTRAINT CK_JobPosting_EmploymentType
        CHECK (EmploymentType IS NULL OR EmploymentType IN
               (N'Full-time', N'Part-time', N'Internship', N'Freelance'));

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

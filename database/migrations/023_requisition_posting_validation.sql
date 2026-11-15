-- Align database invariants with the requisition and job-posting service validators.
-- Run on the configured application database. No existing rows are changed.
-- Fix any reported legacy rows before rerunning this migration.
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
       OR OBJECT_ID(N'dbo.RequisitionApproval', N'U') IS NULL
       OR OBJECT_ID(N'dbo.JobPosting', N'U') IS NULL
        THROW 50001, 'Requisition or posting tables are missing.', 1;

    IF EXISTS (SELECT 1 FROM dbo.JobRequisition
               WHERE MinSalary <= 0 OR MinSalary > 1000000000
                  OR MaxSalary <= 0 OR MaxSalary > 1000000000)
        THROW 50002, 'JobRequisition contains a salary outside 0.01..1000000000.', 1;
    IF EXISTS (SELECT 1 FROM dbo.JobRequisition WHERE LEN(TRIM(Title)) = 0)
        THROW 50003, 'JobRequisition contains a blank title.', 1;
    IF EXISTS (SELECT 1 FROM dbo.JobRequisition
               WHERE ApprovalStatus NOT IN (N'Draft', N'Cancelled') AND (
                   SubmittedAt IS NULL OR ReasonForHiring IS NULL
                   OR LEN(TRIM(ReasonForHiring)) = 0
                   OR LEN(TRIM(JobDescription)) = 0
                   OR LEN(TRIM(RequirementDetails)) = 0
                   OR (WorkModel <> N'Remote' AND
                       (WorkLocation IS NULL OR LEN(TRIM(WorkLocation)) = 0))))
        THROW 50004, 'Submitted JobRequisition contains incomplete content.', 1;
    IF EXISTS (SELECT 1 FROM dbo.RequisitionApproval
               WHERE [Status] = N'Rejected'
                 AND (Comments IS NULL OR LEN(TRIM(Comments)) = 0))
        THROW 50005, 'Rejected RequisitionApproval contains no reason.', 1;
    IF EXISTS (SELECT 1 FROM dbo.JobPosting
               WHERE LEN(TRIM(PostingTitle)) = 0
                  OR LEN(TRIM(JobDescription)) = 0
                  OR LEN(TRIM(JobRequirements)) = 0)
        THROW 50006, 'JobPosting contains blank required content.', 1;
    IF EXISTS (SELECT 1 FROM dbo.JobPosting
               WHERE PostingStatus <> N'Draft'
                 AND (PostingDate IS NULL OR ApplicationDeadline IS NULL))
        THROW 50007, 'Published or closed JobPosting has missing dates.', 1;
    IF EXISTS (SELECT 1 FROM dbo.JobPosting
               WHERE PostingStatus <> N'Closed'
               GROUP BY RequisitionId HAVING COUNT(*) > 1)
        THROW 50008, 'A requisition has multiple open JobPosting rows.', 1;

    IF EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID(N'dbo.JobRequisition')
                 AND name = N'CK_JobRequisition_Salary_NonNegative')
        ALTER TABLE dbo.JobRequisition DROP CONSTRAINT CK_JobRequisition_Salary_NonNegative;
    ALTER TABLE dbo.JobRequisition WITH CHECK ADD CONSTRAINT CK_JobRequisition_Salary_NonNegative
        CHECK ((MinSalary IS NULL OR MinSalary BETWEEN 0.01 AND 1000000000)
            AND (MaxSalary IS NULL OR MaxSalary BETWEEN 0.01 AND 1000000000));

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
                   WHERE parent_object_id = OBJECT_ID(N'dbo.JobRequisition')
                     AND name = N'CK_JobRequisition_Title_NotBlank')
        ALTER TABLE dbo.JobRequisition WITH CHECK ADD CONSTRAINT CK_JobRequisition_Title_NotBlank
            CHECK (LEN(TRIM(Title)) > 0);
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
                   WHERE parent_object_id = OBJECT_ID(N'dbo.JobRequisition')
                     AND name = N'CK_JobRequisition_Submitted_Content')
        ALTER TABLE dbo.JobRequisition WITH CHECK ADD CONSTRAINT CK_JobRequisition_Submitted_Content
            CHECK (ApprovalStatus IN (N'Draft', N'Cancelled') OR (
                SubmittedAt IS NOT NULL
                AND ReasonForHiring IS NOT NULL AND LEN(TRIM(ReasonForHiring)) > 0
                AND LEN(TRIM(JobDescription)) > 0
                AND LEN(TRIM(RequirementDetails)) > 0
                AND (WorkModel = N'Remote' OR (WorkLocation IS NOT NULL AND LEN(TRIM(WorkLocation)) > 0))
            ));
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
                   WHERE parent_object_id = OBJECT_ID(N'dbo.RequisitionApproval')
                     AND name = N'CK_RequisitionApproval_Rejection_Comment')
        ALTER TABLE dbo.RequisitionApproval WITH CHECK ADD CONSTRAINT CK_RequisitionApproval_Rejection_Comment
            CHECK ([Status] <> N'Rejected' OR (Comments IS NOT NULL AND LEN(TRIM(Comments)) > 0));
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
                   WHERE parent_object_id = OBJECT_ID(N'dbo.JobPosting')
                     AND name = N'CK_JobPosting_Content_NotBlank')
        ALTER TABLE dbo.JobPosting WITH CHECK ADD CONSTRAINT CK_JobPosting_Content_NotBlank
            CHECK (LEN(TRIM(PostingTitle)) > 0 AND LEN(TRIM(JobDescription)) > 0
                AND LEN(TRIM(JobRequirements)) > 0);
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
                   WHERE parent_object_id = OBJECT_ID(N'dbo.JobPosting')
                     AND name = N'CK_JobPosting_Published_Dates')
        ALTER TABLE dbo.JobPosting WITH CHECK ADD CONSTRAINT CK_JobPosting_Published_Dates
            CHECK (PostingStatus = N'Draft' OR (PostingDate IS NOT NULL AND ApplicationDeadline IS NOT NULL));

    IF EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID(N'dbo.JobPosting')
                 AND name = N'UX_JobPosting_ActivePerRequisition')
        DROP INDEX UX_JobPosting_ActivePerRequisition ON dbo.JobPosting;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes
                   WHERE object_id = OBJECT_ID(N'dbo.JobPosting')
                     AND name = N'UX_JobPosting_OpenPerRequisition')
        CREATE UNIQUE NONCLUSTERED INDEX UX_JobPosting_OpenPerRequisition
            ON dbo.JobPosting(RequisitionId) WHERE PostingStatus <> N'Closed';

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

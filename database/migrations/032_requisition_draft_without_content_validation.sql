-- Allow incomplete or business-invalid requisition drafts. Submission remains validated.
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @hadTitleNotBlank BIT = CASE WHEN EXISTS (
        SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID(N'dbo.JobRequisition')
          AND name = N'CK_JobRequisition_Title_NotBlank') THEN 1 ELSE 0 END;
    DECLARE @hadSubmittedContent BIT = CASE WHEN EXISTS (
        SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID(N'dbo.JobRequisition')
          AND name = N'CK_JobRequisition_Submitted_Content') THEN 1 ELSE 0 END;

    ALTER TABLE dbo.JobRequisition DROP CONSTRAINT CK_JobRequisition_NumberOfPositions;
    ALTER TABLE dbo.JobRequisition DROP CONSTRAINT CK_JobRequisition_EmploymentType;
    ALTER TABLE dbo.JobRequisition DROP CONSTRAINT CK_JobRequisition_WorkModel;
    ALTER TABLE dbo.JobRequisition DROP CONSTRAINT CK_JobRequisition_Salary_Range;
    ALTER TABLE dbo.JobRequisition DROP CONSTRAINT CK_JobRequisition_Salary_NonNegative;
    IF @hadTitleNotBlank = 1
        ALTER TABLE dbo.JobRequisition DROP CONSTRAINT CK_JobRequisition_Title_NotBlank;
    ALTER TABLE dbo.JobRequisition DROP CONSTRAINT CK_JobRequisition_ExpectedStaffingDate_Chronology;
    IF @hadSubmittedContent = 1
        ALTER TABLE dbo.JobRequisition DROP CONSTRAINT CK_JobRequisition_Submitted_Content;
    ALTER TABLE dbo.JobRequisition DROP CONSTRAINT DF_JobRequisition_WorkModel;
    DROP INDEX IX_JobRequisition_DepartmentId ON dbo.JobRequisition;
    ALTER TABLE dbo.JobRequisition DROP CONSTRAINT FK_JobRequisition_Department;

    ALTER TABLE dbo.JobRequisition ALTER COLUMN Title NVARCHAR(200) NULL;
    ALTER TABLE dbo.JobRequisition ALTER COLUMN DepartmentId INT NULL;
    ALTER TABLE dbo.JobRequisition ALTER COLUMN NumberOfPositions INT NULL;
    ALTER TABLE dbo.JobRequisition ALTER COLUMN EmploymentType NVARCHAR(50) NULL;
    ALTER TABLE dbo.JobRequisition ALTER COLUMN WorkModel NVARCHAR(20) NULL;
    ALTER TABLE dbo.JobRequisition ADD CONSTRAINT FK_JobRequisition_Department
        FOREIGN KEY (DepartmentId) REFERENCES dbo.Department(DepartmentId);
    CREATE NONCLUSTERED INDEX IX_JobRequisition_DepartmentId ON dbo.JobRequisition(DepartmentId);
    ALTER TABLE dbo.JobRequisition ADD CONSTRAINT DF_JobRequisition_WorkModel DEFAULT N'On-site' FOR WorkModel;

    ALTER TABLE dbo.JobRequisition WITH CHECK ADD CONSTRAINT CK_JobRequisition_NumberOfPositions
        CHECK (ApprovalStatus = N'Draft' OR (NumberOfPositions IS NOT NULL AND NumberOfPositions > 0));
    ALTER TABLE dbo.JobRequisition WITH CHECK ADD CONSTRAINT CK_JobRequisition_EmploymentType
        CHECK (ApprovalStatus = N'Draft' OR (EmploymentType IS NOT NULL AND EmploymentType IN (N'Full-time', N'Part-time', N'Internship', N'Freelance')));
    ALTER TABLE dbo.JobRequisition WITH CHECK ADD CONSTRAINT CK_JobRequisition_WorkModel
        CHECK (ApprovalStatus = N'Draft' OR (WorkModel IS NOT NULL AND WorkModel IN (N'On-site', N'Remote', N'Hybrid')));
    ALTER TABLE dbo.JobRequisition WITH CHECK ADD CONSTRAINT CK_JobRequisition_Salary_Range
        CHECK (ApprovalStatus = N'Draft' OR MinSalary IS NULL OR MaxSalary IS NULL OR MinSalary <= MaxSalary);
    ALTER TABLE dbo.JobRequisition WITH CHECK ADD CONSTRAINT CK_JobRequisition_Salary_NonNegative
        CHECK (ApprovalStatus = N'Draft' OR ((MinSalary IS NULL OR MinSalary BETWEEN 0.01 AND 1000000000)
            AND (MaxSalary IS NULL OR MaxSalary BETWEEN 0.01 AND 1000000000)));
    IF @hadTitleNotBlank = 1
        ALTER TABLE dbo.JobRequisition WITH CHECK ADD CONSTRAINT CK_JobRequisition_Title_NotBlank
            CHECK (ApprovalStatus = N'Draft' OR (Title IS NOT NULL AND LEN(TRIM(Title)) > 0));
    ALTER TABLE dbo.JobRequisition WITH CHECK ADD CONSTRAINT CK_JobRequisition_Department_Required
        CHECK (ApprovalStatus = N'Draft' OR DepartmentId IS NOT NULL);
    ALTER TABLE dbo.JobRequisition WITH CHECK ADD CONSTRAINT CK_JobRequisition_ExpectedStaffingDate_Chronology
        CHECK (ApprovalStatus = N'Draft' OR ExpectedStaffingDate IS NULL OR
            (ExpectedStaffingDate >= CONVERT(DATE, CreatedAt)
             AND (SubmittedAt IS NULL OR ExpectedStaffingDate >= CONVERT(DATE, SubmittedAt))));
    IF @hadSubmittedContent = 1
        ALTER TABLE dbo.JobRequisition WITH CHECK ADD CONSTRAINT CK_JobRequisition_Submitted_Content
            CHECK (ApprovalStatus = N'Draft' OR (
                SubmittedAt IS NOT NULL
                AND ReasonForHiring IS NOT NULL AND LEN(TRIM(ReasonForHiring)) > 0
                AND LEN(TRIM(JobDescription)) > 0
                AND LEN(TRIM(RequirementDetails)) > 0
                AND (WorkModel = N'Remote' OR (WorkLocation IS NOT NULL AND LEN(TRIM(WorkLocation)) > 0))
            ));

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

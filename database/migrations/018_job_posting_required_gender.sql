-- Job Posting stores its own gender choice, initially copied from the source requisition.
-- Older postings receive the source value; NULL remains supported for legacy fallback.
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

    IF OBJECT_ID(N'dbo.JobPosting', N'U') IS NULL
        THROW 50001, 'JobPosting table does not exist.', 1;

    IF COL_LENGTH(N'dbo.JobPosting', N'RequiredGender') IS NULL
        ALTER TABLE dbo.JobPosting ADD RequiredGender NVARCHAR(20) NULL;

    EXEC sys.sp_executesql N'UPDATE p
        SET p.RequiredGender = COALESCE(r.RequiredGender, N''Any'')
        FROM dbo.JobPosting AS p
        JOIN dbo.JobRequisition AS r ON r.RequisitionId = p.RequisitionId
        WHERE p.RequiredGender IS NULL';

    IF NOT EXISTS (
        SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID(N'dbo.JobPosting', N'U')
          AND name = N'CK_JobPosting_RequiredGender'
    )
        EXEC sys.sp_executesql N'ALTER TABLE dbo.JobPosting WITH CHECK
            ADD CONSTRAINT CK_JobPosting_RequiredGender
                CHECK (RequiredGender IS NULL OR RequiredGender IN (N''Male'', N''Female'', N''Any''))';

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

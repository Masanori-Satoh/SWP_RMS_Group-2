-- Remove the unused JobRequisition.Cancelled status without changing historical rows.
-- Resolve existing Cancelled requisitions manually before applying this migration.
SET XACT_ABORT ON;
SET NOCOUNT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    IF EXISTS (SELECT 1 FROM dbo.JobRequisition WHERE ApprovalStatus = N'Cancelled')
        THROW 50031, 'Resolve existing Cancelled JobRequisition rows before removing the status.', 1;

    IF EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID(N'dbo.JobRequisition')
                 AND name = N'CK_JobRequisition_ApprovalStatus')
        ALTER TABLE dbo.JobRequisition DROP CONSTRAINT CK_JobRequisition_ApprovalStatus;
    ALTER TABLE dbo.JobRequisition WITH CHECK ADD CONSTRAINT CK_JobRequisition_ApprovalStatus
        CHECK (ApprovalStatus IN (N'Draft', N'Pending_Director', N'Approved', N'Rejected', N'Closed'));

    IF EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID(N'dbo.JobRequisition')
                 AND name = N'CK_JobRequisition_Submitted_Content')
    BEGIN
        ALTER TABLE dbo.JobRequisition DROP CONSTRAINT CK_JobRequisition_Submitted_Content;
        ALTER TABLE dbo.JobRequisition WITH CHECK ADD CONSTRAINT CK_JobRequisition_Submitted_Content
            CHECK (ApprovalStatus = N'Draft' OR (
                SubmittedAt IS NOT NULL
                AND ReasonForHiring IS NOT NULL AND LEN(TRIM(ReasonForHiring)) > 0
                AND LEN(TRIM(JobDescription)) > 0
                AND LEN(TRIM(RequirementDetails)) > 0
                AND (WorkModel = N'Remote' OR (WorkLocation IS NOT NULL AND LEN(TRIM(WorkLocation)) > 0))
            ));
    END;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

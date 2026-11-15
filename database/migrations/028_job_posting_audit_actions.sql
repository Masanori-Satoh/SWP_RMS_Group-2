-- Permit explicit publication actions for JobPosting audit entries.
-- Older rows remain readable in their original text format.
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

    IF OBJECT_ID(N'dbo.AuditLog', N'U') IS NULL
        THROW 50001, 'AuditLog is missing.', 1;

    IF EXISTS (SELECT 1 FROM dbo.AuditLog
               WHERE [Action] NOT IN (N'CREATE', N'UPDATE', N'DELETE', N'LOGIN',
                                      N'PUBLISH', N'UNPUBLISH', N'RESTORE'))
        THROW 50002, 'AuditLog contains an unknown action.', 1;

    IF EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID(N'dbo.AuditLog')
                 AND name = N'CK_AuditLog_Action')
        ALTER TABLE dbo.AuditLog DROP CONSTRAINT CK_AuditLog_Action;

    ALTER TABLE dbo.AuditLog WITH CHECK ADD CONSTRAINT CK_AuditLog_Action
        CHECK ([Action] IN (N'CREATE', N'UPDATE', N'DELETE', N'LOGIN',
                           N'PUBLISH', N'UNPUBLISH', N'RESTORE'));

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

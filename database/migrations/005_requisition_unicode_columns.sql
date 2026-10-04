-- Apply to the configured application database; safe to rerun.
-- Earlier Hibernate configuration created these columns as VARCHAR. JDBC
-- getNString cannot read them with use_nationalized_character_data=true.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.JobRequisition') AND name = N'WorkLocation' AND TYPE_NAME(system_type_id) = N'varchar')
    ALTER TABLE dbo.JobRequisition ALTER COLUMN WorkLocation NVARCHAR(255) NULL;
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.JobRequisition') AND name = N'ProbationDuration' AND TYPE_NAME(system_type_id) = N'varchar')
    ALTER TABLE dbo.JobRequisition ALTER COLUMN ProbationDuration NVARCHAR(255) NULL;
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.JobRequisition') AND name = N'WorkModel' AND TYPE_NAME(system_type_id) = N'varchar')
    ALTER TABLE dbo.JobRequisition ALTER COLUMN WorkModel NVARCHAR(50) NULL;
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.RequisitionWorkflowEvent') AND name = N'EventType' AND TYPE_NAME(system_type_id) = N'varchar')
    ALTER TABLE dbo.RequisitionWorkflowEvent ALTER COLUMN EventType NVARCHAR(20) NOT NULL;
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.RequisitionWorkflowEvent') AND name = N'Comment' AND TYPE_NAME(system_type_id) = N'varchar')
    ALTER TABLE dbo.RequisitionWorkflowEvent ALTER COLUMN Comment NVARCHAR(1000) NULL;

COMMIT TRANSACTION;

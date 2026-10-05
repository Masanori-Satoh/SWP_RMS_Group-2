-- Migration 006: Add RequisitionCode and RecruitmentRound to JobRequisition
-- Safe to rerun on SQL Server.
-- NOTE: UPDATE statements use dynamic SQL (EXEC) because SQL Server compiles the whole
-- batch before running it; referencing a column added in the same batch would fail
-- with "Invalid column name".

SET XACT_ABORT ON;
BEGIN TRANSACTION;

-- 1. Add RequisitionCode column if not exists
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.JobRequisition') AND name = N'RequisitionCode'
)
BEGIN
    ALTER TABLE dbo.JobRequisition ADD RequisitionCode NVARCHAR(50) NULL;
END

-- 2. Add RecruitmentRound column if not exists
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.JobRequisition') AND name = N'RecruitmentRound'
)
BEGIN
    ALTER TABLE dbo.JobRequisition ADD RecruitmentRound INT NOT NULL CONSTRAINT DF_JobRequisition_RecruitmentRound DEFAULT 1;
END

-- 3. Backfill existing records: generate RequisitionCode and set RecruitmentRound = 1 if null
EXEC(N'
UPDATE dbo.JobRequisition
SET RequisitionCode = CONCAT(''REQ-'', YEAR(ISNULL(CreatedAt, SYSDATETIME())), ''-'', RIGHT(''000'' + CAST(RequisitionId AS VARCHAR(10)), 3))
WHERE RequisitionCode IS NULL;
');

EXEC(N'
UPDATE dbo.JobRequisition
SET RecruitmentRound = 1
WHERE RecruitmentRound IS NULL;
');

COMMIT TRANSACTION;

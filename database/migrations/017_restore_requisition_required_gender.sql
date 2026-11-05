-- Restore JobRequisition.RequiredGender on databases that already ran an older 016 which dropped it.
-- Safe to run after the current 016, which preserves RequiredGender.
-- Existing requisitions receive 'Any' because their previous values are not available.
-- Run against the configured application database; no USE statement is needed.
SET XACT_ABORT ON;
SET NOCOUNT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.JobRequisition', N'U') IS NULL
        THROW 50001, 'JobRequisition table does not exist.', 1;

    IF COL_LENGTH(N'dbo.JobRequisition', N'RequiredGender') IS NULL
    BEGIN
        ALTER TABLE dbo.JobRequisition
            ADD RequiredGender NVARCHAR(20) NOT NULL
                CONSTRAINT DF_JobRequisition_RequiredGender DEFAULT N'Any' WITH VALUES;
    END
    ELSE IF EXISTS (
        SELECT 1
        FROM sys.columns AS c
        JOIN sys.types AS t ON t.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.JobRequisition', N'U')
          AND c.name = N'RequiredGender'
          AND (t.name <> N'nvarchar' OR c.max_length <> 40 OR c.is_nullable <> 0)
    )
        THROW 50002, 'RequiredGender exists with a different type or nullability.', 1;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.default_constraints AS dc
        JOIN sys.columns AS c
          ON c.object_id = dc.parent_object_id AND c.column_id = dc.parent_column_id
        WHERE dc.parent_object_id = OBJECT_ID(N'dbo.JobRequisition', N'U')
          AND c.name = N'RequiredGender'
    )
    BEGIN
        EXEC sys.sp_executesql N'ALTER TABLE dbo.JobRequisition
            ADD CONSTRAINT DF_JobRequisition_RequiredGender
                DEFAULT N''Any'' FOR RequiredGender';
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID(N'dbo.JobRequisition', N'U')
          AND name = N'CK_JobRequisition_RequiredGender'
    )
    BEGIN
        EXEC sys.sp_executesql N'ALTER TABLE dbo.JobRequisition WITH CHECK
            ADD CONSTRAINT CK_JobRequisition_RequiredGender
                CHECK (RequiredGender IN (N''Male'', N''Female'', N''Any''))';
    END;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

-- HISTORICAL ONLY: migration for the old snake_case database.
-- Do not run against database/schema/db.sql or RitirementManagement2.
-- Candidate profiles stay separate from authentication accounts.
-- Existing profiles remain unlinked until a deliberate registration/linking flow.
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF COL_LENGTH(N'dbo.candidate', N'user_id') IS NULL
        EXEC(N'ALTER TABLE dbo.candidate ADD user_id INT NULL');

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_keys
        WHERE parent_object_id = OBJECT_ID(N'dbo.candidate')
          AND name = N'fk_candidate_user_account'
    )
        EXEC(N'ALTER TABLE dbo.candidate WITH CHECK
            ADD CONSTRAINT fk_candidate_user_account
            FOREIGN KEY (user_id) REFERENCES dbo.[user](user_id)');

    -- SQL Server needs a filtered index so multiple legacy NULL links remain valid.
    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE object_id = OBJECT_ID(N'dbo.candidate')
          AND name = N'ux_candidate_user_account'
    )
        EXEC(N'CREATE UNIQUE INDEX ux_candidate_user_account
            ON dbo.candidate(user_id) WHERE user_id IS NOT NULL');

    IF NOT EXISTS (SELECT 1 FROM dbo.[role] WHERE role_name = N'Candidate')
        INSERT INTO dbo.[role] (role_name, description)
        VALUES (N'Candidate', N'External candidate account');

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

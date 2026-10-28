-- 012: Retire the Blocked account status without changing database schema.
-- Run against the application's configured database. No tables, columns or constraints are altered.
-- Idempotent: former Blocked accounts stay disabled as Inactive; never set them Active.
SET XACT_ABORT ON;
SET NOCOUNT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    UPDATE dbo.[User]
    SET AccountStatus = N'Inactive', UpdatedAt = SYSDATETIME()
    WHERE AccountStatus = N'Blocked';
    SELECT @@ROWCOUNT AS ConvertedAccounts;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

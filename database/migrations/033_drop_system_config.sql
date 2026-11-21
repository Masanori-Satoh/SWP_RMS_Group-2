-- 033: Gỡ bỏ bảng SystemConfig (không còn chức năng nào sử dụng)
SET XACT_ABORT ON;
SET NOCOUNT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.SystemConfig', N'U') IS NOT NULL
        DROP TABLE dbo.SystemConfig;

    DELETE FROM dbo.AuditLog WHERE EntityName = N'SystemConfig';

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

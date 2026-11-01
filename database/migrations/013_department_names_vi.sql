-- 013: Translate only the five known seed department names on existing databases.
-- Custom department names remain unchanged. Safe to run again after a successful update.
SET XACT_ABORT ON;
SET NOCOUNT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    UPDATE dbo.Department SET DepartmentName = N'Kỹ thuật' WHERE DepartmentName = N'Engineering';
    UPDATE dbo.Department SET DepartmentName = N'Kinh doanh & Marketing' WHERE DepartmentName = N'Sales & Marketing';
    UPDATE dbo.Department SET DepartmentName = N'Nhân sự' WHERE DepartmentName = N'Human Resources';
    UPDATE dbo.Department SET DepartmentName = N'Tài chính – Kế toán' WHERE DepartmentName = N'Finance & Accounting';
    UPDATE dbo.Department SET DepartmentName = N'Sản phẩm & Thiết kế' WHERE DepartmentName = N'Product & Design';
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

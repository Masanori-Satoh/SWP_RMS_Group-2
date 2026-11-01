-- 014: Gỡ tiêu chí sàng lọc của yêu cầu tuyển dụng (dunglt, 01/11).
-- Tiêu chí và trọng số chỉ phục vụ máy chấm AI (đã gỡ ở 013); yêu cầu ứng viên vẫn ghi bằng văn bản
-- trong JobRequisition.RequirementDetails.
-- Chạy trên database mà app đang cấu hình (không có USE). Chạy lại nhiều lần vẫn an toàn.
SET XACT_ABORT ON;
SET NOCOUNT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.ScreeningCriteria', N'U') IS NOT NULL
    BEGIN
        SELECT COUNT(*) AS CriteriaRowsDropped FROM dbo.ScreeningCriteria;
        DROP TABLE dbo.ScreeningCriteria;  -- index IX_ScreeningCriteria_RequisitionId đi theo bảng
    END;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

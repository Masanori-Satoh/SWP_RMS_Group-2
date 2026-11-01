-- 013: Gỡ chấm điểm AI khỏi hồ sơ ứng tuyển (dunglt, 01/11).
-- Chạy trên database mà app đang cấu hình (không có USE). Chạy lại nhiều lần vẫn an toàn.
-- Thứ tự: chuyển dữ liệu AI_Screened sang Applied TRƯỚC, rồi mới bỏ giá trị đó khỏi CHECK.
--   1. Đơn AI_Screened -> Applied (cùng là "chờ HR sàng lọc"); nhật ký trạng thái cũ cũng đổi theo.
--   2. CK_Application_Status không còn AI_Screened.
--   3. Xóa bảng AIScreeningResult, cột Application.OverallScore (không code nào dùng).
--   4. Xóa cấu hình AI_SCREENING_ENDPOINT, AI_SCORE_THRESHOLD và nhật ký tạo chúng; thông báo loại AI_SCREENING_HIGH_SCORE.
-- Bảng ScreeningCriteria (tiêu chí của yêu cầu tuyển dụng) xóa ở 014.
SET XACT_ABORT ON;
SET NOCOUNT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    UPDATE dbo.Application SET ApplicationStatus = N'Applied' WHERE ApplicationStatus = N'AI_Screened';
    SELECT @@ROWCOUNT AS ApplicationsMovedToApplied;

    UPDATE dbo.AuditLog SET OldValue = N'Applied' WHERE EntityName = N'Application' AND OldValue = N'AI_Screened';
    UPDATE dbo.AuditLog SET NewValue = N'Applied' WHERE EntityName = N'Application' AND NewValue = N'AI_Screened';

    IF EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE name = N'CK_Application_Status' AND definition LIKE N'%AI[_]Screened%')
    BEGIN
        ALTER TABLE dbo.Application DROP CONSTRAINT CK_Application_Status;
        ALTER TABLE dbo.Application ADD CONSTRAINT CK_Application_Status
            CHECK (ApplicationStatus IN (
                N'Applied', N'HR_Passed', N'HM_Passed', N'HM_Suggested',
                N'Interviewing', N'Offered', N'Hired', N'Rejected'));
    END;

    IF OBJECT_ID(N'dbo.AIScreeningResult', N'U') IS NOT NULL
        DROP TABLE dbo.AIScreeningResult;

    IF COL_LENGTH(N'dbo.Application', N'OverallScore') IS NOT NULL
        ALTER TABLE dbo.Application DROP COLUMN OverallScore;

    DELETE FROM dbo.SystemConfig WHERE ConfigKey IN (N'AI_SCREENING_ENDPOINT', N'AI_SCORE_THRESHOLD');
    DELETE FROM dbo.AuditLog
    WHERE EntityName = N'SystemConfig' AND EntityId IN (N'AI_SCREENING_ENDPOINT', N'AI_SCORE_THRESHOLD');

    IF OBJECT_ID(N'dbo.Notification', N'U') IS NOT NULL
        DELETE FROM dbo.Notification WHERE EventType = 'AI_SCREENING_HIGH_SCORE';

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

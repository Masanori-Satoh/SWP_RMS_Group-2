-- 019: Tách trạng thái kết thúc của đơn ứng tuyển (dunglt, 05/11).
-- Chạy trên database mà app đang cấu hình (không có USE). Chạy lại nhiều lần vẫn an toàn.
-- Trước: rút đơn, từ chối offer, đóng tin đều ghi Rejected; muốn biết "ai kết thúc đơn" phải lục AuditLog.
-- Sau, mỗi trạng thái kết thúc trả lời "ai kết thúc đơn":
--   Hired      nhận việc
--   Rejected   công ty đánh trượt (HR/HM chấm Không đạt, phỏng vấn Failed)
--   Withdrawn  ứng viên tự dừng (rút đơn, từ chối offer)          <- mới
--   Cancelled  đơn dừng vì tin tuyển dụng bị đóng                  <- mới
-- Thứ tự: nới CHECK trước, rồi mới chuyển dữ liệu cũ. Đơn Rejected được xếp lại theo (ưu tiên từ trên xuống):
--   1. Có nhật ký NewValue = 'Withdrawn'    -> Withdrawn
--   2. Có nhật ký NewValue = 'PostingClosed' -> Cancelled
--   3. Offer mới nhất của đơn là Declined   -> Withdrawn
-- Còn lại giữ Rejected. Không đụng bản nháp talent pool (HM_Suggested bị hủy vẫn Rejected + nhật ký
-- TalentPoolCancelled / TalentPoolHrWithdrawn). Không đổi UpdatedAt (dashboard dùng nó làm mốc).
SET XACT_ABORT ON;
SET NOCOUNT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
                   WHERE name = N'CK_Application_Status' AND parent_object_id = OBJECT_ID(N'dbo.Application')
                     AND definition LIKE N'%Withdrawn%' AND definition LIKE N'%Cancelled%')
    BEGIN
        IF EXISTS (SELECT 1 FROM sys.check_constraints
                   WHERE name = N'CK_Application_Status' AND parent_object_id = OBJECT_ID(N'dbo.Application'))
            ALTER TABLE dbo.Application DROP CONSTRAINT CK_Application_Status;
        ALTER TABLE dbo.Application ADD CONSTRAINT CK_Application_Status
            CHECK (ApplicationStatus IN (
                N'Applied', N'HR_Passed', N'HM_Passed', N'HM_Suggested',
                N'Interviewing', N'Offered', N'Hired', N'Rejected', N'Withdrawn', N'Cancelled'));
    END;

    -- 1. Ứng viên tự rút đơn
    UPDATE a SET a.ApplicationStatus = N'Withdrawn'
    FROM dbo.Application a
    WHERE a.ApplicationStatus = N'Rejected'
      AND EXISTS (SELECT 1 FROM dbo.AuditLog l
                  WHERE l.EntityName = N'Application' AND l.EntityId = CAST(a.ApplicationId AS NVARCHAR(50))
                    AND l.NewValue = N'Withdrawn')
      AND NOT EXISTS (SELECT 1 FROM dbo.AuditLog l
                      WHERE l.EntityName = N'Application' AND l.EntityId = CAST(a.ApplicationId AS NVARCHAR(50))
                        AND l.NewValue IN (N'TalentPoolCancelled', N'TalentPoolHrWithdrawn'));
    SELECT @@ROWCOUNT AS WithdrawnByRetraction;

    -- 2. Tin bị đóng
    UPDATE a SET a.ApplicationStatus = N'Cancelled'
    FROM dbo.Application a
    WHERE a.ApplicationStatus = N'Rejected'
      AND EXISTS (SELECT 1 FROM dbo.AuditLog l
                  WHERE l.EntityName = N'Application' AND l.EntityId = CAST(a.ApplicationId AS NVARCHAR(50))
                    AND l.NewValue = N'PostingClosed');
    SELECT @@ROWCOUNT AS CancelledByPostingClosed;

    -- 3. Ứng viên từ chối offer (offer mới nhất của đơn là Declined)
    UPDATE a SET a.ApplicationStatus = N'Withdrawn'
    FROM dbo.Application a
    WHERE a.ApplicationStatus = N'Rejected'
      AND (SELECT TOP 1 o.OfferStatus FROM dbo.OfferProposal o
           WHERE o.ApplicationId = a.ApplicationId ORDER BY o.OfferId DESC) = N'Declined';
    SELECT @@ROWCOUNT AS WithdrawnByDeclinedOffer;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

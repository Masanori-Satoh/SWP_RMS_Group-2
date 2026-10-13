-- =============================================================================
-- RMS - SEED SỬA: ĐƯA TRẠNG THÁI HỒ SƠ VỀ ĐÚNG LUỒNG
-- Chạy SAU: database/schema/db.sql -> database/seeds/seed_data.sql
--           -> database/seeds/seed_extra_jobs_and_pipeline.sql
-- Chỉ UPDATE Application.ApplicationStatus (+ UpdatedAt). Không đổi schema, không thêm/xóa dòng.
-- Chạy lại nhiều lần an toàn: chỉ sửa hồ sơ đang lệch, lần sau không còn dòng nào để sửa.
--
-- Vì sao cần: dữ liệu seed gán trạng thái hồ sơ độc lập với bảng phỏng vấn / offer
-- (ví dụ Hired nhưng chưa có offer, Rejected nhưng đã Đạt phỏng vấn).
--
-- Quy tắc (dòng khớp đầu tiên thắng), áp cho hồ sơ đang ở
-- HM_Passed, Interviewing, Offered, Hired hoặc Rejected:
--   1. Offer Accepted                      -> Hired
--   2. Offer Declined                      -> Rejected
--   3. Offer Sent_Candidate                -> Offered
--   4. Kết quả phỏng vấn mới nhất Failed   -> Rejected
--   5. Có lịch phỏng vấn (chưa Cancelled)  -> Interviewing
--   6. Còn lại                             -> giữ nguyên
-- Bỏ qua: hồ sơ ứng viên đã rút (AuditLog NewValue = 'Withdrawn') và hồ sơ bị
-- HR/HM chấm Fail ở vòng CV (Rejected hợp lệ).
-- "Lịch mới nhất" = InterviewSchedule có StartTime lớn nhất, InterviewStatus <> 'Cancelled'.
-- Không dùng ID cố định: mọi điều kiện tra theo dữ kiện của bảng.
-- =============================================================================
USE RitirementManagement2;
GO
SET NOCOUNT ON;
GO

SET XACT_ABORT ON;
BEGIN TRY
BEGIN TRANSACTION;

-- Ảnh chụp trước khi sửa
SELECT N'TRUOC' AS Giai_doan, ApplicationStatus, COUNT(*) AS SoHoSo
FROM Application GROUP BY ApplicationStatus ORDER BY ApplicationStatus;

;WITH latest AS (
    SELECT s.InterviewId, s.ApplicationId,
           ROW_NUMBER() OVER (PARTITION BY s.ApplicationId
                              ORDER BY s.StartTime DESC, s.InterviewId DESC) AS rn
    FROM InterviewSchedule s
    WHERE s.InterviewStatus <> N'Cancelled'
),
facts AS (
    SELECT a.ApplicationId,
           a.ApplicationStatus AS CurrentStatus,
           l.InterviewId       AS LatestInterviewId,
           fr.FinalDecision    AS FinalDecision,
           o.OfferStatus       AS OfferStatus
    FROM Application a
    LEFT JOIN latest l  ON l.ApplicationId = a.ApplicationId AND l.rn = 1
    LEFT JOIN InterviewFinalResult fr ON fr.InterviewId = l.InterviewId
    LEFT JOIN OfferProposal o ON o.ApplicationId = a.ApplicationId
    WHERE a.ApplicationStatus IN (N'HM_Passed', N'Interviewing', N'Offered', N'Hired', N'Rejected')
      AND NOT EXISTS (SELECT 1 FROM AuditLog al
                      WHERE al.EntityName = N'Application'
                        AND al.EntityId = CAST(a.ApplicationId AS NVARCHAR(50))
                        AND al.NewValue = N'Withdrawn')
      AND NOT EXISTS (SELECT 1 FROM ApplicationReview r
                      WHERE r.ApplicationId = a.ApplicationId AND r.Decision = N'Fail')
),
target AS (
    SELECT ApplicationId, CurrentStatus,
           CASE
               WHEN OfferStatus = N'Accepted'       THEN N'Hired'
               WHEN OfferStatus = N'Declined'       THEN N'Rejected'
               WHEN OfferStatus = N'Sent_Candidate' THEN N'Offered'
               WHEN FinalDecision = N'Failed'       THEN N'Rejected'
               WHEN LatestInterviewId IS NOT NULL   THEN N'Interviewing'
               ELSE CurrentStatus
           END AS NewStatus
    FROM facts
)
UPDATE a
SET a.ApplicationStatus = t.NewStatus,
    a.UpdatedAt = SYSDATETIME()
FROM Application a
JOIN target t ON t.ApplicationId = a.ApplicationId
WHERE t.NewStatus <> t.CurrentStatus;

PRINT N'>>> Số hồ sơ đã sửa: ' + CAST(@@ROWCOUNT AS NVARCHAR(10));

-- Ảnh chụp sau khi sửa
SELECT N'SAU' AS Giai_doan, ApplicationStatus, COUNT(*) AS SoHoSo
FROM Application GROUP BY ApplicationStatus ORDER BY ApplicationStatus;

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    PRINT N'>>> LỖI, đã hoàn tác: ' + ERROR_MESSAGE();
    THROW;
END CATCH
GO

-- =============================================================================
-- RMS - SEED DEMO DASHBOARD NỘI BỘ (HR / Hiring Manager / Director / Interviewer)
-- Chạy SAU: database/schema/db.sql -> database/seeds/seed_data.sql
--           -> database/seeds/seed_extra_jobs_and_pipeline.sql
--           -> database/seeds/seed_application_flow_fix.sql
-- Không đổi schema, không xóa dòng. Mọi mốc thời gian tính theo GETDATE() lúc chạy,
-- nên chạy lại trước buổi demo để dashboard luôn có số "gần đây".
-- Chạy lại nhiều lần an toàn: bước nào đủ dữ liệu rồi thì bỏ qua.
--
-- Vì sao cần: dashboard chỉ đếm 7 ngày tới / 8 tuần gần nhất / tháng này / hôm nay,
-- còn dữ liệu seed có ngày cố định nên nhiều khối bị trống.
--   1. Hồ sơ có ngày nộp ở tương lai -> rải về 8 tuần qua (biểu đồ "Xu hướng hồ sơ ứng tuyển" của HR).
--   2. Thêm 3 offer chờ Giám đốc duyệt ("Đề nghị tuyển dụng chờ duyệt" của Director).
--   3. 4 hồ sơ Hired có ngày cập nhật trong tháng này (KPI "Ứng viên trúng tuyển trong tháng").
--   4. 2 tin đang đăng có ngày đăng hôm nay (KPI "Tin mới đăng trong ngày" của HR).
--   5. Interviewer demo viet.bui@rms-tech.vn có 7 buổi trong tuần này + tuần sau
--      (lịch phỏng vấn, KPI buổi sắp tới, phiếu đánh giá cần hoàn thành).
-- Không dùng ID cố định: mọi điều kiện tra theo dữ kiện của bảng.
-- =============================================================================
USE RitirementManagement2;
GO
SET NOCOUNT ON;
-- Bảng có filtered index: sqlcmd mặc định tắt QUOTED_IDENTIFIER, SSMS thì bật sẵn
SET QUOTED_IDENTIFIER ON;
GO

SET XACT_ABORT ON;
BEGIN TRY
BEGIN TRANSACTION;

DECLARE @now DATETIME2 = SYSDATETIME();
DECLARE @monthStart DATETIME2 = DATEFROMPARTS(YEAR(@now), MONTH(@now), 1);
DECLARE @today DATETIME2 = CAST(CAST(@now AS DATE) AS DATETIME2);

-- 1. Hồ sơ nộp "ở tương lai" -> lùi về 1..52 ngày trước, rải theo ApplicationId để đủ 8 tuần.
--    Nếu hồ sơ đã có lịch phỏng vấn thì ngày nộp phải trước buổi phỏng vấn sớm nhất ít nhất 1 ngày.
UPDATE a
SET SubmissionDate = x.NewDate,
    CreatedAt = CASE WHEN a.CreatedAt > @now THEN x.NewDate ELSE a.CreatedAt END
FROM Application a
CROSS APPLY (
    SELECT CASE
               WHEN fi.FirstInterview IS NOT NULL AND fi.FirstInterview <= spread.Candidate
                   THEN DATEADD(day, -1, fi.FirstInterview)
               ELSE spread.Candidate
           END AS NewDate
    FROM (SELECT DATEADD(day, -(1 + (a.ApplicationId * 7) % 52), @now) AS Candidate) spread
    CROSS APPLY (SELECT MIN(s.StartTime) AS FirstInterview
                 FROM InterviewSchedule s WHERE s.ApplicationId = a.ApplicationId) fi
) x
WHERE a.SubmissionDate > @now;
PRINT N'>>> 1. Hồ sơ đã dời ngày nộp về 8 tuần qua: ' + CAST(@@ROWCOUNT AS NVARCHAR(10));

-- 2. Offer chờ Giám đốc duyệt: lấy hồ sơ đã có kết quả phỏng vấn Passed, chưa có offer,
--    người đề xuất = Hiring Manager của yêu cầu tuyển dụng. Chỉ thêm cho đủ 3 offer đang chờ.
DECLARE @needOffers INT = 3 - (SELECT COUNT(*) FROM OfferProposal
                               WHERE OfferStatus = N'Pending_Director' AND IsDeleted = 0);
IF @needOffers > 0
BEGIN
    INSERT INTO OfferProposal (ApplicationId, OfferedPositionTitle, ProposedSalary, ProbationSalary,
                               ExpectedStartDate, WorkLocation, OfferStatus, ProposedBy, CreatedAt, UpdatedAt)
    SELECT TOP (@needOffers)
           a.ApplicationId,
           jp.PostingTitle,
           COALESCE(r.MaxSalary, r.MinSalary, 15000000),
           ROUND(COALESCE(r.MaxSalary, r.MinSalary, 15000000) * 0.85, 0),
           DATEADD(day, 30, CAST(@now AS DATE)),
           r.WorkLocation,
           N'Pending_Director',
           r.HiringManagerId,
           DATEADD(day, -(a.ApplicationId % 5), @now),
           DATEADD(day, -(a.ApplicationId % 5), @now)
    FROM Application a
    JOIN JobPosting jp ON jp.JobPostingId = a.JobPostingId
    JOIN JobRequisition r ON r.RequisitionId = jp.RequisitionId
    WHERE r.HiringManagerId IS NOT NULL
      AND a.ApplicationStatus NOT IN (N'Rejected', N'Hired')
      AND NOT EXISTS (SELECT 1 FROM OfferProposal o WHERE o.ApplicationId = a.ApplicationId)
      AND EXISTS (SELECT 1 FROM InterviewFinalResult f
                  JOIN InterviewSchedule s ON s.InterviewId = f.InterviewId
                  WHERE s.ApplicationId = a.ApplicationId AND f.FinalDecision = N'Passed')
    ORDER BY a.ApplicationId;
    PRINT N'>>> 2. Offer chờ Giám đốc duyệt đã thêm: ' + CAST(@@ROWCOUNT AS NVARCHAR(10));
END
ELSE
    PRINT N'>>> 2. Đã đủ 3 offer chờ Giám đốc duyệt, bỏ qua.';

-- 3. Hired trong tháng này: đưa ngày cập nhật của vài hồ sơ Hired vào tháng này cho đủ 4.
DECLARE @needHired INT = 4 - (SELECT COUNT(*) FROM Application
                              WHERE ApplicationStatus = N'Hired' AND UpdatedAt >= @monthStart AND UpdatedAt < @now);
IF @needHired > 0
BEGIN
    UPDATE a
    SET UpdatedAt = DATEADD(hour, -(a.ApplicationId % 48) - 1, @now)
    FROM Application a
    WHERE a.ApplicationId IN (SELECT TOP (@needHired) ApplicationId FROM Application
                              WHERE ApplicationStatus = N'Hired'
                                AND (UpdatedAt IS NULL OR UpdatedAt < @monthStart OR UpdatedAt >= @now)
                              ORDER BY ApplicationId);
    PRINT N'>>> 3. Hồ sơ Hired đưa vào tháng này: ' + CAST(@@ROWCOUNT AS NVARCHAR(10));
END
ELSE
    PRINT N'>>> 3. Đã đủ 4 hồ sơ Hired trong tháng, bỏ qua.';

-- 4. Tin đăng hôm nay: chuyển ngày đăng của tin đang Published (đã đăng trước hôm nay) sang hôm nay cho đủ 2.
DECLARE @needPostings INT = 2 - (SELECT COUNT(*) FROM JobPosting
                                 WHERE PostingStatus = N'Published' AND PostingDate >= @today AND PostingDate <= @now);
IF @needPostings > 0
BEGIN
    UPDATE JobPosting
    SET PostingDate = CASE WHEN DATEADD(minute, -30, @now) < @today THEN @today ELSE DATEADD(minute, -30, @now) END
    WHERE JobPostingId IN (SELECT TOP (@needPostings) JobPostingId FROM JobPosting
                           WHERE PostingStatus = N'Published' AND PostingDate < @today
                           ORDER BY JobPostingId DESC);
    PRINT N'>>> 4. Tin đổi ngày đăng sang hôm nay: ' + CAST(@@ROWCOUNT AS NVARCHAR(10));
END
ELSE
    PRINT N'>>> 4. Đã đủ 2 tin đăng hôm nay, bỏ qua.';

-- 5. Lịch của tài khoản Interviewer demo (viet.bui@rms-tech.vn): 7 buổi trong tuần này + tuần sau.
--    Lấy các buổi Scheduled còn xa (> 14 ngày nữa), dời giờ vào 7 khung dưới đây và thêm Việt vào hội đồng.
--    Khung đã qua giờ -> Completed (chưa có phiếu = "Đánh giá cần hoàn thành"); khung chưa tới -> Scheduled.
--    Chỉ lấy hồ sơ nộp trước thứ Hai tuần này để ngày phỏng vấn không sớm hơn ngày nộp.
DECLARE @demoInterviewer INT = (SELECT UserId FROM [User] WHERE Email = N'viet.bui@rms-tech.vn');
DECLARE @monday DATETIME2 = DATEADD(day, -((DATEPART(weekday, @now) + @@DATEFIRST - 2) % 7), @today);
IF @demoInterviewer IS NULL
    PRINT N'>>> 5. Không có tài khoản viet.bui@rms-tech.vn, bỏ qua.';
ELSE IF (SELECT COUNT(*) FROM InterviewPanel p JOIN InterviewSchedule s ON s.InterviewId = p.InterviewId
         WHERE p.InterviewerId = @demoInterviewer AND s.InterviewStatus <> N'Cancelled'
           AND s.StartTime >= @monday AND s.StartTime < DATEADD(day, 14, @monday)) >= 5
    PRINT N'>>> 5. Interviewer demo đã có lịch tuần này / tuần sau, bỏ qua.';
ELSE
BEGIN
    DECLARE @slots TABLE (SlotNo INT PRIMARY KEY, StartTime DATETIME2);
    INSERT INTO @slots VALUES
        (1, DATEADD(hour, 9,  @monday)),                  -- T2 tuần này 09:00
        (2, DATEADD(hour, 38, @monday)),                  -- T3 14:00
        (3, DATEADD(hour, 58, @monday)),                  -- T4 10:00
        (4, DATEADD(minute, (3 * 24 + 15) * 60 + 30, @monday)), -- T5 15:30
        (5, DATEADD(hour, 7 * 24 + 9,  @monday)),         -- T2 tuần sau 09:00
        (6, DATEADD(hour, 8 * 24 + 14, @monday)),         -- T3 tuần sau 14:00
        (7, DATEADD(hour, 9 * 24 + 10, @monday));         -- T4 tuần sau 10:00

    DECLARE @moved TABLE (InterviewId INT PRIMARY KEY, StartTime DATETIME2);
    INSERT INTO @moved (InterviewId, StartTime)
    SELECT picked.InterviewId, sl.StartTime
    FROM (SELECT s.InterviewId, ROW_NUMBER() OVER (ORDER BY s.InterviewId) AS SlotNo
          FROM InterviewSchedule s JOIN Application a ON a.ApplicationId = s.ApplicationId
          WHERE s.InterviewStatus = N'Scheduled' AND s.StartTime > DATEADD(day, 14, @now)
            AND a.SubmissionDate < @monday) picked
    JOIN @slots sl ON sl.SlotNo = picked.SlotNo;

    UPDATE s
    SET StartTime = m.StartTime,
        EndTime = DATEADD(hour, 1, m.StartTime),
        InterviewStatus = CASE WHEN DATEADD(hour, 1, m.StartTime) < @now THEN N'Completed' ELSE N'Scheduled' END
    FROM InterviewSchedule s JOIN @moved m ON m.InterviewId = s.InterviewId;
    PRINT N'>>> 5. Buổi phỏng vấn dời vào lịch Interviewer demo: ' + CAST(@@ROWCOUNT AS NVARCHAR(10));

    INSERT INTO InterviewPanel (InterviewId, InterviewerId, RoleInPanel)
    SELECT m.InterviewId, @demoInterviewer, N'Interviewer'
    FROM @moved m
    WHERE NOT EXISTS (SELECT 1 FROM InterviewPanel p WHERE p.InterviewId = m.InterviewId AND p.InterviewerId = @demoInterviewer);
    PRINT N'>>> 5. Thêm Interviewer demo vào hội đồng: ' + CAST(@@ROWCOUNT AS NVARCHAR(10));
END

COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    PRINT N'>>> LỖI, đã hoàn tác: ' + ERROR_MESSAGE();
    THROW;
END CATCH
GO

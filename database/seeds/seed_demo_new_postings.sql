-- Dữ liệu demo bổ sung cho các tin tạo tay (không có trong seed_data.sql): điền các ô còn trống
-- và thêm hồ sơ đủ trạng thái để trang chi tiết tin hiện đủ số liệu / tab Ứng viên.
-- Chỉ THÊM, không xóa. Chạy lại an toàn: bỏ qua ứng viên đã nộp vào tin đó.
USE RitirementManagement2;
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON; -- bảng có filtered index; sqlcmd mặc định tắt
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @Now DATETIME2 = SYSDATETIME();
DECLARE @HR INT = (SELECT UserId FROM [User] WHERE Username = N'hr_lan');

-- 1) Điền các ô nội dung còn trống của tin đang đăng / tạm dừng
UPDATE JobPosting SET
    WorkingHours = ISNULL(NULLIF(WorkingHours, N''), N'Thứ 2 - Thứ 6 (08:30 - 17:30)'),
    Benefits     = ISNULL(NULLIF(Benefits, N''), N'- Lương tháng 13, thưởng dự án' + CHAR(10) + N'- Bảo hiểm sức khỏe PVI'
                       + CHAR(10) + N'- 12 ngày phép năm' + CHAR(10) + N'- Review lương 2 lần/năm'),
    WorkLocation = ISNULL(NULLIF(WorkLocation, N''), N'Tầng 8, RMS Tower, Cầu Giấy, Hà Nội')
WHERE PostingStatus IN (N'Published', N'Paused')
  AND (WorkingHours IS NULL OR WorkingHours = N'' OR Benefits IS NULL OR Benefits = N'' OR WorkLocation IS NULL OR WorkLocation = N'');

-- 2) Kịch bản hồ sơ cho mỗi tin: trạng thái, số ngày trước khi nộp, HR đạt?, HM đạt?
DECLARE @Plan TABLE (Seq INT, Status NVARCHAR(30), DaysAgo INT, HrDecision NVARCHAR(10), HmDecision NVARCHAR(10));
INSERT INTO @Plan VALUES
    (1, N'Applied', 1, NULL, NULL),
    (2, N'Applied', 3, NULL, NULL),
    (3, N'Applied', 9, NULL, NULL),
    (4, N'HR_Passed', 6, N'Pass', NULL),
    (5, N'HR_Passed', 8, N'Pass', NULL),
    (6, N'HM_Passed', 10, N'Pass', N'Pass'),
    (7, N'Interviewing', 12, N'Pass', N'Pass'),
    (8, N'Interviewing', 14, N'Pass', N'Pass'),
    (9, N'Hired', 20, N'Pass', N'Pass'),
    (10, N'Rejected', 5, N'Fail', NULL);

-- Tin đang đăng chưa có hồ sơ nào
DECLARE @Targets TABLE (JobPostingId INT, HmId INT, Offset INT);
INSERT INTO @Targets
SELECT p.JobPostingId, rq.HiringManagerId, ROW_NUMBER() OVER (ORDER BY p.JobPostingId) * 10
FROM JobPosting p JOIN JobRequisition rq ON rq.RequisitionId = p.RequisitionId
WHERE p.PostingStatus = N'Published'
  AND NOT EXISTS (SELECT 1 FROM Application a WHERE a.JobPostingId = p.JobPostingId);

-- Ứng viên đang hoạt động, đánh số để mỗi tin lấy một nhóm khác nhau
DECLARE @Cands TABLE (Rn INT, CandidateId INT);
INSERT INTO @Cands
SELECT ROW_NUMBER() OVER (ORDER BY c.CandidateId), c.CandidateId
FROM Candidate c JOIN [User] u ON u.UserId = c.UserId
WHERE u.AccountStatus = N'Active';
DECLARE @CandCount INT = (SELECT COUNT(*) FROM @Cands);

DECLARE @New TABLE (ApplicationId INT, JobPostingId INT, Seq INT);
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, CreatedAt, UpdatedAt)
OUTPUT inserted.ApplicationId, inserted.JobPostingId, NULL INTO @New (ApplicationId, JobPostingId, Seq)
SELECT c.CandidateId, t.JobPostingId, N'/samples/cv/sample_cv_business_development.pdf',
       DATEADD(DAY, -pl.DaysAgo, @Now), pl.Status, DATEADD(DAY, -pl.DaysAgo, @Now),
       CASE WHEN pl.HrDecision IS NULL THEN NULL ELSE DATEADD(DAY, -pl.DaysAgo + 1, @Now) END
FROM @Targets t
CROSS JOIN @Plan pl
JOIN @Cands c ON c.Rn = ((t.Offset + pl.Seq - 1) % @CandCount) + 1
WHERE NOT EXISTS (SELECT 1 FROM Application a WHERE a.CandidateId = c.CandidateId AND a.JobPostingId = t.JobPostingId);

-- Ghép lại Seq theo ngày nộp để gắn đánh giá HR / HM đúng kịch bản
UPDATE n SET Seq = pl.Seq
FROM @New n
JOIN Application a ON a.ApplicationId = n.ApplicationId
JOIN @Plan pl ON pl.Status = a.ApplicationStatus AND DATEDIFF(DAY, a.SubmissionDate, @Now) = pl.DaysAgo;

INSERT INTO ApplicationReview (ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt)
SELECT n.ApplicationId, @HR, N'HR', pl.HrDecision,
       CASE pl.HrDecision WHEN N'Pass' THEN N'CV phù hợp yêu cầu.' ELSE N'Chưa đủ kinh nghiệm theo yêu cầu.' END,
       DATEADD(DAY, -pl.DaysAgo + 1, @Now)
FROM @New n JOIN @Plan pl ON pl.Seq = n.Seq
WHERE pl.HrDecision IS NOT NULL;

INSERT INTO ApplicationReview (ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt)
SELECT n.ApplicationId, t.HmId, N'HiringManager', pl.HmDecision, N'Đồng ý mời phỏng vấn.', DATEADD(DAY, -pl.DaysAgo + 2, @Now)
FROM @New n JOIN @Plan pl ON pl.Seq = n.Seq JOIN @Targets t ON t.JobPostingId = n.JobPostingId
WHERE pl.HmDecision IS NOT NULL;

COMMIT;

SELECT p.JobPostingId, p.PostingTitle, COUNT(a.ApplicationId) AS Apps
FROM JobPosting p LEFT JOIN Application a ON a.JobPostingId = p.JobPostingId
WHERE p.JobPostingId IN (SELECT JobPostingId FROM @Targets)
GROUP BY p.JobPostingId, p.PostingTitle;

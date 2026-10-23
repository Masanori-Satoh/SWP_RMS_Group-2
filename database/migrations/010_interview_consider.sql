-- =============================================================================
-- 010: Kết quả phỏng vấn "Cân nhắc" + UNIQUE phiếu đánh giá
--   - InterviewFinalResult.FinalDecision thêm giá trị Considering
--   - InterviewFinalResult.ConsideredAt (lúc HM chọn Cân nhắc lần đầu)
--   - InterviewEvaluation: mỗi người phỏng vấn 1 phiếu / buổi (BR-INT-01)
-- Đồng bộ với database/schema/db.sql. Chạy lại nhiều lần vẫn an toàn.
-- =============================================================================
USE RitirementManagement2;
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.InterviewFinalResult') AND name = N'ConsideredAt')
    ALTER TABLE dbo.InterviewFinalResult ADD ConsideredAt DATETIME2 NULL;
GO

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_InterviewFinalResult_FinalDecision')
    ALTER TABLE dbo.InterviewFinalResult DROP CONSTRAINT CK_InterviewFinalResult_FinalDecision;
GO

ALTER TABLE dbo.InterviewFinalResult
    ADD CONSTRAINT CK_InterviewFinalResult_FinalDecision
        CHECK (FinalDecision IN (N'Passed', N'Failed', N'Considering'));
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_InterviewFinalResult_ConsideredAt')
    ALTER TABLE dbo.InterviewFinalResult
        ADD CONSTRAINT CK_InterviewFinalResult_ConsideredAt
            CHECK (FinalDecision <> N'Considering' OR ConsideredAt IS NOT NULL);
GO

-- Phiếu trùng (nếu có) phải được xử lý tay trước; câu SELECT dưới đây liệt kê chúng.
IF EXISTS (SELECT 1 FROM dbo.InterviewEvaluation GROUP BY InterviewId, InterviewerId HAVING COUNT(*) > 1)
BEGIN
    SELECT InterviewId, InterviewerId, COUNT(*) AS Total
    FROM dbo.InterviewEvaluation GROUP BY InterviewId, InterviewerId HAVING COUNT(*) > 1;
    RAISERROR(N'InterviewEvaluation có phiếu trùng (InterviewId, InterviewerId). Xóa bản trùng rồi chạy lại.', 16, 1);
END
ELSE IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_InterviewEvaluation_Interview_Interviewer')
    ALTER TABLE dbo.InterviewEvaluation
        ADD CONSTRAINT UQ_InterviewEvaluation_Interview_Interviewer UNIQUE (InterviewId, InterviewerId);
GO

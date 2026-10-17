-- =============================================================================
-- 007: JobRequisition.ProbationDuration (NVARCHAR) -> ProbationDays (INT, 1..180)
-- Đồng bộ với database/schema/db.sql. Chạy lại nhiều lần vẫn an toàn.
-- Quy đổi dữ liệu cũ: "N tháng" -> N*30 ngày, "N ngày" hoặc "N" -> N ngày,
-- giá trị khác hoặc ngoài khoảng 1..180 -> NULL.
-- =============================================================================
USE RitirementManagement2;
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.JobRequisition') AND name = N'ProbationDays')
    ALTER TABLE dbo.JobRequisition ADD ProbationDays INT NULL;
GO

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.JobRequisition') AND name = N'ProbationDuration')
BEGIN
    EXEC(N'
        UPDATE r SET ProbationDays = CASE
                WHEN v.Days BETWEEN 1 AND 180 THEN v.Days
                ELSE NULL
            END
        FROM dbo.JobRequisition r
        CROSS APPLY (SELECT LTRIM(RTRIM(r.ProbationDuration)) AS Txt) t
        CROSS APPLY (SELECT TRY_CAST(LEFT(t.Txt, PATINDEX(N''%[^0-9]%'', t.Txt + N''x'') - 1) AS INT) AS N) n
        CROSS APPLY (SELECT CASE
                WHEN t.Txt LIKE N''%tháng%'' THEN n.N * 30
                ELSE n.N
            END AS Days) v
        WHERE r.ProbationDays IS NULL AND r.ProbationDuration IS NOT NULL;');

    ALTER TABLE dbo.JobRequisition DROP COLUMN ProbationDuration;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_JobRequisition_ProbationDays')
    ALTER TABLE dbo.JobRequisition
        ADD CONSTRAINT CK_JobRequisition_ProbationDays
            CHECK (ProbationDays IS NULL OR ProbationDays BETWEEN 1 AND 180);
GO

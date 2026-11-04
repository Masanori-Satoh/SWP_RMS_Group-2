-- 016: Thử việc dạng chữ, tin có Hình thức / Mô hình / Thử việc riêng (hoangnh, 03/11).
--  1. JobRequisition: ProbationDays INT -> ProbationPeriod NVARCHAR(100) (số cũ chuyển thành "N ngày").
--  2. JobRequisition: giữ RequiredGender và dữ liệu giới tính hiện có.
--  3. JobPosting: thêm EmploymentType, WorkModel, ProbationPeriod (NULL được); tin cũ chép giá trị từ yêu cầu.
--  4. JobRequisition: xóa cột HiringType (Loại tuyển dụng) kèm CHECK.
--  5. JobRequisition: bỏ CK_JobRequisition_RoundOrigin để HM nhập tay đợt tuyển dụng (vẫn giữ RecruitmentRound > 0).
-- Đồng bộ với database/schema/db.sql. Chạy trên database mà app đang cấu hình (không có USE).
-- Chạy lại nhiều lần vẫn an toàn.

-- ---- 1. JobRequisition.ProbationPeriod ----
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.JobRequisition') AND name = N'ProbationPeriod')
    ALTER TABLE dbo.JobRequisition ADD ProbationPeriod NVARCHAR(100) NULL;
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.JobRequisition') AND name = N'ProbationDays')
BEGIN
    EXEC(N'UPDATE dbo.JobRequisition SET ProbationPeriod = CONCAT(ProbationDays, N'' ngày'') WHERE ProbationDays IS NOT NULL AND ProbationPeriod IS NULL');
    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_JobRequisition_ProbationDays')
        ALTER TABLE dbo.JobRequisition DROP CONSTRAINT CK_JobRequisition_ProbationDays;
    ALTER TABLE dbo.JobRequisition DROP COLUMN ProbationDays;
END
GO

-- ---- 3. JobPosting: Hình thức, Mô hình, Thử việc riêng của tin ----
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.JobPosting') AND name = N'EmploymentType')
    ALTER TABLE dbo.JobPosting ADD EmploymentType NVARCHAR(50) NULL;
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.JobPosting') AND name = N'WorkModel')
    ALTER TABLE dbo.JobPosting ADD WorkModel NVARCHAR(20) NULL;
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.JobPosting') AND name = N'ProbationPeriod')
    ALTER TABLE dbo.JobPosting ADD ProbationPeriod NVARCHAR(100) NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_JobPosting_EmploymentType')
    ALTER TABLE dbo.JobPosting ADD CONSTRAINT CK_JobPosting_EmploymentType
        CHECK (EmploymentType IS NULL OR EmploymentType IN (N'Full-time', N'Part-time', N'Internship', N'Contract'));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_JobPosting_WorkModel')
    ALTER TABLE dbo.JobPosting ADD CONSTRAINT CK_JobPosting_WorkModel
        CHECK (WorkModel IS NULL OR WorkModel IN (N'On-site', N'Remote', N'Hybrid'));
GO
-- Tin cũ: chép từ yêu cầu để trang ứng viên hiện như trước
UPDATE p SET p.EmploymentType = r.EmploymentType
FROM dbo.JobPosting p JOIN dbo.JobRequisition r ON r.RequisitionId = p.RequisitionId
WHERE p.EmploymentType IS NULL;
UPDATE p SET p.WorkModel = r.WorkModel
FROM dbo.JobPosting p JOIN dbo.JobRequisition r ON r.RequisitionId = p.RequisitionId
WHERE p.WorkModel IS NULL;
UPDATE p SET p.ProbationPeriod = r.ProbationPeriod
FROM dbo.JobPosting p JOIN dbo.JobRequisition r ON r.RequisitionId = p.RequisitionId
WHERE p.ProbationPeriod IS NULL AND r.ProbationPeriod IS NOT NULL;
GO

-- ---- 4. Bỏ JobRequisition.HiringType ----
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.JobRequisition') AND name = N'HiringType')
BEGIN
    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_JobRequisition_HiringType')
        ALTER TABLE dbo.JobRequisition DROP CONSTRAINT CK_JobRequisition_HiringType;
    ALTER TABLE dbo.JobRequisition DROP COLUMN HiringType;
END
GO

-- ---- 5. Đợt tuyển dụng nhập tay: bỏ ràng buộc đợt 1 <-> không có yêu cầu gốc ----
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_JobRequisition_RoundOrigin')
    ALTER TABLE dbo.JobRequisition DROP CONSTRAINT CK_JobRequisition_RoundOrigin;
GO

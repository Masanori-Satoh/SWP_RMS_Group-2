-- =============================================================================
-- RMS - SEED BỔ SUNG: TIN TUYỂN DỤNG ĐA DẠNG + DỮ LIỆU PHỄU ỨNG VIÊN
-- Chạy SAU: database/schema/db.sql -> database/seeds/seed_data.sql
-- Chỉ THÊM dữ liệu, không đổi schema, không sửa dòng có sẵn (trừ AppliedCvUrl -> CV mẫu).
-- Chạy lại nhiều lần an toàn: nếu đã chạy (có user hm_khanh) thì bỏ qua toàn bộ.
-- Mật khẩu tài khoản mới: 12345678
--
-- Nội dung:
--   1. 2 phòng ban mới (Finance & Accounting, Product & Design) + 2 Hiring Manager
--   2. 19 requisition: 13 Approved, 2 Draft, 2 Pending_Director, 2 Rejected
--   3. 12 tin tuyển dụng: 9 Published còn hạn (hiện trên Jobs Board), 1 Published đã hết hạn,
--      1 Closed, 1 Paused; 1 requisition Approved chưa có tin (để HR tạo tin)
--   4. Tiêu chí sàng lọc cho 17 requisition (đạt chuẩn 100% trọng số theo quy tắc nghiệp vụ)
--   4.1 Lịch sử thao tác (AuditLog) cho toàn bộ 19 requisition
--   5. 22 đơn ứng tuyển trên 4 tin mới (đủ trạng thái Applied -> HM_Passed, Rejected ở vòng sàng lọc)
--   6. AppliedCvUrl của mọi đơn trỏ về CV mẫu trong src/main/resources/static/samples/cv/
-- =============================================================================
USE RitirementManagement2;
GO
SET NOCOUNT ON;
GO

IF EXISTS (SELECT 1 FROM [User] WHERE Username = N'hm_khanh')
BEGIN
    PRINT N'>>> seed_extra_jobs_and_pipeline.sql đã được chạy trước đó. Bỏ qua.';
    SET NOEXEC ON;
END
GO

SET XACT_ABORT ON;
BEGIN TRY
BEGIN TRANSACTION;

-- Tra cứu dữ liệu có sẵn theo khóa tự nhiên (không phụ thuộc ID)
DECLARE @pwd NVARCHAR(255) = N'$2a$10$p.RWJeHxArmCbQBg3GNXEew66veoolEXULwkrjzeoof8Me1dAD8PO'; -- 12345678
DECLARE @roleHM INT = (SELECT RoleId FROM [Role] WHERE RoleName = N'Hiring Manager');
DECLARE @director INT = (SELECT UserId FROM [User] WHERE Username = N'director');
DECLARE @hrLan INT = (SELECT UserId FROM [User] WHERE Username = N'hr_lan');
DECLARE @hrQuang INT = (SELECT UserId FROM [User] WHERE Username = N'hr_quang');
DECLARE @hmTuan INT = (SELECT UserId FROM [User] WHERE Username = N'hm_tuan');
DECLARE @hmHuong INT = (SELECT UserId FROM [User] WHERE Username = N'hm_huong');
DECLARE @deptEng INT = (SELECT DepartmentId FROM Department WHERE DepartmentName = N'Engineering');
DECLARE @deptSales INT = (SELECT DepartmentId FROM Department WHERE DepartmentName = N'Sales & Marketing');
IF @roleHM IS NULL OR @director IS NULL OR @hrLan IS NULL OR @hrQuang IS NULL OR @hmTuan IS NULL
   OR @hmHuong IS NULL OR @deptEng IS NULL OR @deptSales IS NULL
    THROW 50001, N'Thiếu dữ liệu nền. Hãy chạy seed_data.sql trước.', 1;

-- =============================================================================
-- 1. PHÒNG BAN MỚI + HIRING MANAGER
-- =============================================================================
DECLARE @deptFin INT, @deptProd INT, @hmKhanh INT, @hmTrang INT;
INSERT INTO Department (DepartmentName, ManagerId, DepartmentStatus) VALUES (N'Finance & Accounting', NULL, N'Active');
SET @deptFin = SCOPE_IDENTITY();
INSERT INTO Department (DepartmentName, ManagerId, DepartmentStatus) VALUES (N'Product & Design', NULL, N'Active');
SET @deptProd = SCOPE_IDENTITY();
INSERT INTO [User] (RoleId, Username, PasswordHash, Email, FullName, PhoneNumber, AvatarUrl, DepartmentId, AccountStatus, CreatedAt) VALUES (@roleHM, N'hm_khanh', @pwd, N'khanh.do@rms-tech.vn', N'Đỗ Minh Khánh', N'0906111222', N'https://ui-avatars.com/api/?name=Đỗ+Minh+Khánh&background=0D8ABC&color=fff', @deptFin, N'Active', '2026-09-01 09:00:00');
SET @hmKhanh = SCOPE_IDENTITY();
INSERT INTO [User] (RoleId, Username, PasswordHash, Email, FullName, PhoneNumber, AvatarUrl, DepartmentId, AccountStatus, CreatedAt) VALUES (@roleHM, N'hm_trang', @pwd, N'trang.le@rms-tech.vn', N'Lê Thu Trang', N'0906333444', N'https://ui-avatars.com/api/?name=Lê+Thu+Trang&background=0D8ABC&color=fff', @deptProd, N'Active', '2026-09-01 09:30:00');
SET @hmTrang = SCOPE_IDENTITY();
UPDATE Department SET ManagerId = @hmKhanh WHERE DepartmentId = @deptFin;
UPDATE Department SET ManagerId = @hmTrang WHERE DepartmentId = @deptProd;

-- =============================================================================
-- 2. JOB REQUISITION
-- =============================================================================
DECLARE @r5 INT, @r6 INT, @r7 INT, @r8 INT, @r9 INT, @r10 INT, @r11 INT, @r12 INT, @r13 INT, @r14 INT, @r15 INT, @r16 INT, @r17 INT, @r18 INT, @r19 INT, @r20 INT, @r21 INT, @r22 INT, @r23 INT;
-- Junior Java Developer [Approved]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'Junior Java Developer', N'REQ-0005', 1, @deptEng, @hmTuan, @hmTuan, N'New', 3, N'Full-time', 15000000.00, 22000000.00, N'Bổ sung nhân sự cho đội phát triển sản phẩm tại văn phòng TP. Hồ Chí Minh', N'Phát triển và bảo trì các module backend bằng Spring Boot, viết unit test, tham gia code review.', N'Tối thiểu 1 năm kinh nghiệm Java, nắm vững OOP, Spring Boot cơ bản, SQL; ưu tiên biết Docker.', N'Any', 60, N'On-site', N'Tầng 5, Tòa nhà RMS Sài Gòn, 72 Lê Thánh Tôn, Quận 1, TP. Hồ Chí Minh', '2026-11-15', N'Approved', '2026-09-18 09:00:00', '2026-09-18 09:00:00', '2026-09-19 10:00:00');
SET @r5 = SCOPE_IDENTITY();
-- DevOps Engineer (AWS / Kubernetes) [Approved]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'DevOps Engineer (AWS / Kubernetes)', N'REQ-0006', 1, @deptEng, @hmTuan, @hmTuan, N'New', 1, N'Full-time', 35000000.00, 50000000.00, N'Chuẩn hóa hạ tầng CI/CD và vận hành hệ thống trên AWS', N'Xây dựng pipeline CI/CD, vận hành cụm Kubernetes trên AWS, giám sát hệ thống bằng Prometheus/Grafana.', N'Tối thiểu 3 năm kinh nghiệm DevOps, thành thạo AWS, Docker, Kubernetes, Terraform; có chứng chỉ AWS là lợi thế.', N'Any', 60, N'Remote', N'Làm việc từ xa (Remote) – toàn quốc', '2026-12-01', N'Approved', '2026-09-19 10:00:00', '2026-09-19 10:00:00', '2026-09-20 11:00:00');
SET @r6 = SCOPE_IDENTITY();
-- Mobile Developer (Flutter) [Approved]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'Mobile Developer (Flutter)', N'REQ-0007', 1, @deptEng, @hmTuan, @hmTuan, N'New', 2, N'Contract', 20000000.00, 30000000.00, N'Dự án ứng dụng di động cho khách hàng trong 12 tháng', N'Phát triển ứng dụng di động đa nền tảng bằng Flutter, tích hợp REST API và thanh toán điện tử.', N'Tối thiểu 2 năm kinh nghiệm Flutter/Dart, đã phát hành ít nhất 1 ứng dụng lên App Store hoặc Google Play.', N'Any', 30, N'Hybrid', N'Tầng 3, Tòa nhà Phần mềm Đà Nẵng, Hải Châu, Đà Nẵng', '2026-11-20', N'Approved', '2026-09-20 09:30:00', '2026-09-20 09:30:00', '2026-09-21 10:00:00');
SET @r7 = SCOPE_IDENTITY();
-- Thực tập sinh Kỹ sư Phần mềm [Approved]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'Thực tập sinh Kỹ sư Phần mềm', N'REQ-0008', 1, @deptEng, @hmTuan, @hmTuan, N'New', 5, N'Internship', 4000000.00, 6000000.00, N'Chương trình thực tập sinh kỳ Xuân 2027', N'Tham gia phát triển tính năng thực tế cùng đội sản phẩm dưới sự hướng dẫn của mentor.', N'Sinh viên năm 3–4 ngành CNTT, biết Java hoặc JavaScript, có thể làm tối thiểu 4 buổi/tuần.', N'Any', NULL, N'On-site', N'Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội', '2027-01-05', N'Approved', '2026-09-21 14:00:00', '2026-09-21 14:00:00', '2026-09-22 09:00:00');
SET @r8 = SCOPE_IDENTITY();
-- Digital Marketing Executive [Approved]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'Digital Marketing Executive', N'REQ-0009', 1, @deptSales, @hmHuong, @hmHuong, N'New', 1, N'Full-time', 14000000.00, 20000000.00, N'Đẩy mạnh kênh marketing số cho sản phẩm SaaS', N'Lập kế hoạch và triển khai chiến dịch quảng cáo Google, Facebook, LinkedIn; theo dõi và tối ưu chỉ số chuyển đổi.', N'Tối thiểu 2 năm kinh nghiệm digital marketing B2B, thành thạo Google Ads, Meta Ads, Google Analytics.', N'Any', 60, N'Hybrid', N'Tầng 5, Tòa nhà RMS Sài Gòn, 72 Lê Thánh Tôn, Quận 1, TP. Hồ Chí Minh', '2026-11-10', N'Approved', '2026-09-22 09:00:00', '2026-09-22 09:00:00', '2026-09-23 09:30:00');
SET @r9 = SCOPE_IDENTITY();
-- Nhân viên Telesales (Bán thời gian) [Approved]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'Nhân viên Telesales (Bán thời gian)', N'REQ-0010', 1, @deptSales, @hmHuong, @hmHuong, N'New', 4, N'Part-time', 6000000.00, 8000000.00, N'Hỗ trợ đội kinh doanh tiếp cận khách hàng tiềm năng', N'Gọi điện tư vấn sản phẩm cho khách hàng doanh nghiệp từ danh sách có sẵn, cập nhật thông tin lên CRM.', N'Giọng nói rõ ràng, giao tiếp tốt; làm ca 4 tiếng/ngày; ưu tiên sinh viên năm cuối.', N'Any', 30, N'On-site', N'Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội', '2026-11-01', N'Approved', '2026-09-23 10:00:00', '2026-09-23 10:00:00', '2026-09-24 09:00:00');
SET @r10 = SCOPE_IDENTITY();
-- Kế toán tổng hợp [Approved]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'Kế toán tổng hợp', N'REQ-0011', 1, @deptFin, @hmKhanh, @hmKhanh, N'Replacement', 1, N'Full-time', 15000000.00, 20000000.00, N'Thay thế nhân sự nghỉ việc', N'Hạch toán nghiệp vụ, lập báo cáo tài chính và báo cáo thuế định kỳ, làm việc với kiểm toán.', N'Tối thiểu 3 năm kinh nghiệm kế toán tổng hợp, thành thạo MISA và Excel, nắm vững chuẩn mực VAS.', N'Any', 60, N'On-site', N'Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội', '2026-11-01', N'Approved', '2026-09-17 09:00:00', '2026-09-17 09:00:00', '2026-09-18 09:00:00');
SET @r11 = SCOPE_IDENTITY();
-- UI/UX Designer [Approved]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'UI/UX Designer', N'REQ-0012', 1, @deptProd, @hmTrang, @hmTrang, N'New', 1, N'Full-time', 18000000.00, 28000000.00, N'Thành lập đội thiết kế sản phẩm', N'Nghiên cứu người dùng, thiết kế wireframe, prototype và giao diện cho sản phẩm web và mobile; xây dựng design system.', N'Tối thiểu 2 năm kinh nghiệm UI/UX, thành thạo Figma, có portfolio sản phẩm thực tế.', N'Any', 60, N'Hybrid', N'Tầng 5, Tòa nhà RMS Sài Gòn, 72 Lê Thánh Tôn, Quận 1, TP. Hồ Chí Minh', '2026-11-15', N'Approved', '2026-09-18 15:00:00', '2026-09-18 15:00:00', '2026-09-19 16:00:00');
SET @r12 = SCOPE_IDENTITY();
-- Product Owner [Approved]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'Product Owner', N'REQ-0013', 1, @deptProd, @hmTrang, @hmTrang, N'New', 1, N'Full-time', 30000000.00, 45000000.00, N'Phụ trách dòng sản phẩm RMS cho khách hàng doanh nghiệp vừa và nhỏ', N'Quản lý product backlog, viết user story, làm việc với khách hàng và đội phát triển theo Scrum.', N'Tối thiểu 3 năm kinh nghiệm Product Owner/BA trong công ty phần mềm; có chứng chỉ PSPO là lợi thế.', N'Any', 60, N'Hybrid', N'Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội', '2026-12-01', N'Approved', '2026-09-24 09:00:00', '2026-09-24 09:00:00', '2026-09-25 10:00:00');
SET @r13 = SCOPE_IDENTITY();
-- Thực tập sinh Kiểm thử Phần mềm [Approved]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'Thực tập sinh Kiểm thử Phần mềm', N'REQ-0014', 1, @deptEng, @hmTuan, @hmTuan, N'New', 2, N'Internship', 3000000.00, 5000000.00, N'Chương trình thực tập kỳ Thu 2026 (đã hết hạn nhận hồ sơ)', N'Thiết kế test case, kiểm thử chức năng và báo lỗi trên JIRA.', N'Sinh viên năm 3–4 ngành CNTT, cẩn thận, có tư duy logic.', N'Any', NULL, N'On-site', N'Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội', '2026-10-15', N'Approved', '2026-09-01 09:00:00', '2026-09-01 09:00:00', '2026-09-02 09:00:00');
SET @r14 = SCOPE_IDENTITY();
-- Chuyên viên Phân tích Tài chính [Approved]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'Chuyên viên Phân tích Tài chính', N'REQ-0015', 1, @deptFin, @hmKhanh, @hmKhanh, N'New', 1, N'Full-time', 20000000.00, 30000000.00, N'Hỗ trợ lập kế hoạch ngân sách năm 2027', N'Phân tích báo cáo tài chính, lập mô hình dự báo và báo cáo quản trị cho Ban Giám đốc.', N'Tối thiểu 2 năm kinh nghiệm phân tích tài chính, thành thạo Excel và Power BI; ưu tiên có CFA/ACCA.', N'Any', 60, N'On-site', N'Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội', '2026-10-20', N'Approved', '2026-09-05 09:00:00', '2026-09-05 09:00:00', '2026-09-06 09:00:00');
SET @r15 = SCOPE_IDENTITY();
-- Data Engineer [Approved]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'Data Engineer', N'REQ-0016', 1, @deptEng, @hmTuan, @hmTuan, N'New', 1, N'Full-time', 30000000.00, 42000000.00, N'Xây dựng kho dữ liệu báo cáo tuyển dụng', N'Thiết kế pipeline ETL, xây dựng data warehouse và phục vụ dữ liệu cho dashboard.', N'Tối thiểu 2 năm kinh nghiệm Data Engineer, thành thạo SQL, Python, Airflow hoặc tương đương.', N'Any', 60, N'Hybrid', N'Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội', '2026-12-15', N'Approved', '2026-09-12 09:00:00', '2026-09-12 09:00:00', '2026-09-13 09:00:00');
SET @r16 = SCOPE_IDENTITY();
-- Business Analyst [Approved]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'Business Analyst', N'REQ-0017', 1, @deptProd, @hmTrang, @hmTrang, N'New', 1, N'Full-time', 20000000.00, 30000000.00, N'Phân tích nghiệp vụ cho các module mới', N'Thu thập yêu cầu, viết tài liệu đặc tả và phối hợp kiểm thử chấp nhận với khách hàng.', N'Tối thiểu 2 năm kinh nghiệm BA phần mềm, viết use case và vẽ BPMN thành thạo.', N'Any', 60, N'Hybrid', N'Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội', '2026-12-01', N'Approved', '2026-10-02 09:00:00', '2026-10-02 09:00:00', '2026-10-03 10:00:00');
SET @r17 = SCOPE_IDENTITY();
-- Tech Lead .NET [Draft]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'Tech Lead .NET', N'REQ-0018', 1, @deptEng, @hmTuan, @hmTuan, N'New', 1, N'Full-time', 45000000.00, 65000000.00, N'Dẫn dắt đội phát triển dự án outsource Nhật Bản', N'Thiết kế kiến trúc, dẫn dắt đội 6 người phát triển hệ thống .NET Core trên Azure.', N'Tối thiểu 6 năm kinh nghiệm .NET, 2 năm vai trò Tech Lead; tiếng Nhật N3 là lợi thế.', N'Any', 60, N'Hybrid', N'Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội', '2027-01-10', N'Draft', NULL, '2026-10-06 09:00:00', NULL);
SET @r18 = SCOPE_IDENTITY();
-- Chuyên viên Kiểm toán Nội bộ [Draft]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'Chuyên viên Kiểm toán Nội bộ', N'REQ-0019', 1, @deptFin, @hmKhanh, @hmKhanh, N'New', 1, N'Full-time', 18000000.00, 25000000.00, N'Tăng cường kiểm soát nội bộ theo yêu cầu của Hội đồng Quản trị', N'Lập kế hoạch và thực hiện kiểm toán nội bộ các quy trình tài chính, mua hàng và nhân sự.', N'Tối thiểu 2 năm kinh nghiệm kiểm toán (ưu tiên Big4).', N'Any', 60, N'On-site', N'Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội', '2027-01-05', N'Draft', NULL, '2026-10-07 10:00:00', NULL);
SET @r19 = SCOPE_IDENTITY();
-- Key Account Manager [Pending_Director]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'Key Account Manager', N'REQ-0020', 1, @deptSales, @hmHuong, @hmHuong, N'New', 1, N'Full-time', 30000000.00, 45000000.00, N'Quản lý nhóm khách hàng doanh nghiệp lớn', N'Chăm sóc và phát triển doanh thu từ 20 khách hàng chiến lược, đàm phán gia hạn hợp đồng năm.', N'Tối thiểu 4 năm kinh nghiệm bán hàng B2B phần mềm, trong đó 2 năm quản lý khách hàng lớn.', N'Any', 60, N'Hybrid', N'Tầng 5, Tòa nhà RMS Sài Gòn, 72 Lê Thánh Tôn, Quận 1, TP. Hồ Chí Minh', '2026-12-15', N'Pending_Director', '2026-10-03 09:00:00', '2026-10-03 09:00:00', '2026-10-03 11:00:00');
SET @r20 = SCOPE_IDENTITY();
-- Chuyên viên Phân tích Dữ liệu Sản phẩm [Pending_Director]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'Chuyên viên Phân tích Dữ liệu Sản phẩm', N'REQ-0021', 1, @deptProd, @hmTrang, @hmTrang, N'New', 1, N'Full-time', 22000000.00, 32000000.00, N'Đo lường hành vi người dùng để định hướng sản phẩm', N'Xây dựng dashboard sản phẩm, phân tích phễu chuyển đổi và thí nghiệm A/B.', N'Tối thiểu 2 năm kinh nghiệm phân tích dữ liệu, thành thạo SQL và một công cụ BI.', N'Any', 60, N'Remote', N'Làm việc từ xa (Remote) – toàn quốc', '2026-12-15', N'Pending_Director', '2026-10-05 14:00:00', '2026-10-05 14:00:00', '2026-10-05 16:00:00');
SET @r21 = SCOPE_IDENTITY();
-- Blockchain Developer [Rejected]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'Blockchain Developer', N'REQ-0022', 1, @deptEng, @hmTuan, @hmTuan, N'New', 2, N'Full-time', 40000000.00, 60000000.00, N'Nghiên cứu tích hợp chứng chỉ số trên blockchain', N'Phát triển smart contract và tích hợp ví điện tử vào hệ thống.', N'Tối thiểu 2 năm kinh nghiệm Solidity.', N'Any', 60, N'Remote', N'Làm việc từ xa (Remote) – toàn quốc', '2026-12-01', N'Rejected', '2026-09-25 09:00:00', '2026-09-25 09:00:00', '2026-09-27 10:00:00');
SET @r22 = SCOPE_IDENTITY();
-- Event Marketing Specialist [Rejected]
INSERT INTO JobRequisition (Title, RequisitionCode, RecruitmentRound, DepartmentId, HiringManagerId, CreatedBy, HiringType, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDays, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, SubmittedAt, CreatedAt, UpdatedAt) VALUES (N'Event Marketing Specialist', N'REQ-0023', 1, @deptSales, @hmHuong, @hmHuong, N'New', 1, N'Contract', 15000000.00, 20000000.00, N'Tổ chức chuỗi sự kiện ra mắt sản phẩm quý 1/2027', N'Lên kế hoạch, điều phối và đánh giá hiệu quả các sự kiện offline và webinar.', N'Tối thiểu 2 năm kinh nghiệm tổ chức sự kiện B2B.', N'Any', NULL, N'On-site', N'Tầng 5, Tòa nhà RMS Sài Gòn, 72 Lê Thánh Tôn, Quận 1, TP. Hồ Chí Minh', '2027-01-05', N'Rejected', '2026-09-26 09:00:00', '2026-09-26 09:00:00', '2026-09-28 09:00:00');
SET @r23 = SCOPE_IDENTITY();

-- =============================================================================
-- 3. REQUISITION APPROVAL (quyết định của Director)
-- =============================================================================
INSERT INTO RequisitionApproval (RequisitionId, DirectorId, [Status], Comments, ApprovalDate) VALUES (@r5, @director, N'Approved', N'Đồng ý tuyển 3 lập trình viên Junior cho văn phòng TP. Hồ Chí Minh.', '2026-09-19 10:00:00');
INSERT INTO RequisitionApproval (RequisitionId, DirectorId, [Status], Comments, ApprovalDate) VALUES (@r6, @director, N'Approved', N'Phê duyệt, ưu tiên ứng viên có chứng chỉ AWS.', '2026-09-20 11:00:00');
INSERT INTO RequisitionApproval (RequisitionId, DirectorId, [Status], Comments, ApprovalDate) VALUES (@r7, @director, N'Approved', N'Đồng ý theo hình thức hợp đồng 12 tháng của dự án.', '2026-09-21 10:00:00');
INSERT INTO RequisitionApproval (RequisitionId, DirectorId, [Status], Comments, ApprovalDate) VALUES (@r8, @director, N'Approved', N'Phê duyệt chương trình thực tập kỳ Xuân 2027.', '2026-09-22 09:00:00');
INSERT INTO RequisitionApproval (RequisitionId, DirectorId, [Status], Comments, ApprovalDate) VALUES (@r9, @director, N'Approved', N'Đồng ý, ngân sách marketing số đã được duyệt.', '2026-09-23 09:30:00');
INSERT INTO RequisitionApproval (RequisitionId, DirectorId, [Status], Comments, ApprovalDate) VALUES (@r10, @director, N'Approved', N'Đồng ý tuyển 4 nhân viên bán thời gian.', '2026-09-24 09:00:00');
INSERT INTO RequisitionApproval (RequisitionId, DirectorId, [Status], Comments, ApprovalDate) VALUES (@r11, @director, N'Approved', N'Đồng ý tuyển thay thế, cần sớm bàn giao trước kỳ quyết toán.', '2026-09-18 09:00:00');
INSERT INTO RequisitionApproval (RequisitionId, DirectorId, [Status], Comments, ApprovalDate) VALUES (@r12, @director, N'Approved', N'Phê duyệt vị trí đầu tiên của đội thiết kế.', '2026-09-19 16:00:00');
INSERT INTO RequisitionApproval (RequisitionId, DirectorId, [Status], Comments, ApprovalDate) VALUES (@r13, @director, N'Approved', N'Phê duyệt, mức lương theo khung đã đề xuất.', '2026-09-25 10:00:00');
INSERT INTO RequisitionApproval (RequisitionId, DirectorId, [Status], Comments, ApprovalDate) VALUES (@r14, @director, N'Approved', N'Phê duyệt chương trình thực tập kỳ Thu 2026.', '2026-09-02 09:00:00');
INSERT INTO RequisitionApproval (RequisitionId, DirectorId, [Status], Comments, ApprovalDate) VALUES (@r15, @director, N'Approved', N'Phê duyệt cho kế hoạch ngân sách 2027.', '2026-09-06 09:00:00');
INSERT INTO RequisitionApproval (RequisitionId, DirectorId, [Status], Comments, ApprovalDate) VALUES (@r16, @director, N'Approved', N'Đồng ý, tạm dừng đăng tin khi đủ ứng viên vòng trong.', '2026-09-13 09:00:00');
INSERT INTO RequisitionApproval (RequisitionId, DirectorId, [Status], Comments, ApprovalDate) VALUES (@r17, @director, N'Approved', N'Đồng ý tuyển BA cho các module mới.', '2026-10-03 10:00:00');
INSERT INTO RequisitionApproval (RequisitionId, DirectorId, [Status], Comments, ApprovalDate) VALUES (@r22, @director, N'Rejected', N'Chưa nằm trong định hướng sản phẩm năm 2027. Đề nghị nghiên cứu thêm tính khả thi trước khi đề xuất lại.', '2026-09-27 10:00:00');
INSERT INTO RequisitionApproval (RequisitionId, DirectorId, [Status], Comments, ApprovalDate) VALUES (@r23, @director, N'Rejected', N'Ngân sách sự kiện quý 1 chưa được duyệt. Đề nghị gộp vào kế hoạch marketing chung.', '2026-09-28 09:00:00');

-- =============================================================================
-- 4. SCREENING CRITERIA
-- =============================================================================
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r5, N'Kinh nghiệm Java & Spring Boot', N'Experience', N'>= 1 năm', 57.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r5, N'Kiến thức SQL', N'Skill', N'SQL Server hoặc MySQL', 29.00, 0);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r5, N'Docker cơ bản', N'Skill', N'Docker', 14.00, 0);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r6, N'Kinh nghiệm DevOps', N'Experience', N'>= 3 năm', 50.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r6, N'AWS & Kubernetes', N'Skill', N'AWS, Kubernetes, Terraform', 50.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r7, N'Kinh nghiệm Flutter', N'Experience', N'>= 2 năm', 67.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r7, N'Ứng dụng đã phát hành', N'Knockout', N'Ít nhất 1 ứng dụng trên store', 33.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r8, N'Sinh viên ngành CNTT', N'Education', N'Năm 3 trở lên', 50.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r8, N'Ngôn ngữ lập trình', N'Skill', N'Java hoặc JavaScript', 50.00, 0);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r9, N'Kinh nghiệm Digital Marketing B2B', N'Experience', N'>= 2 năm', 57.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r9, N'Công cụ quảng cáo', N'Skill', N'Google Ads, Meta Ads', 43.00, 0);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r10, N'Kỹ năng giao tiếp qua điện thoại', N'Skill', N'Giọng nói rõ ràng', 100.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r11, N'Kinh nghiệm kế toán tổng hợp', N'Experience', N'>= 3 năm', 50.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r11, N'Phần mềm kế toán', N'Skill', N'MISA, Excel', 25.00, 0);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r11, N'Trình độ chuyên môn', N'Education', N'Cử nhân Kế toán/Tài chính', 25.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r12, N'Kinh nghiệm UI/UX', N'Experience', N'>= 2 năm', 45.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r12, N'Portfolio', N'Knockout', N'Có portfolio sản phẩm thực tế', 22.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r12, N'Figma', N'Skill', N'Figma, Design System', 33.00, 0);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r13, N'Kinh nghiệm Product Owner/BA', N'Experience', N'>= 3 năm', 67.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r13, N'Scrum', N'Skill', N'Scrum, viết user story', 33.00, 0);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r14, N'Sinh viên ngành CNTT / Toán tin', N'Education', N'Năm 3 trở lên', 50.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r14, N'Tư duy logic & Kiểm thử cơ bản', N'Skill', N'Viết test case, tư duy logic', 50.00, 0);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r15, N'Kinh nghiệm phân tích tài chính', N'Experience', N'>= 2 năm', 50.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r15, N'Kỹ năng Excel & Power BI', N'Skill', N'Excel, Power BI, mô hình tài chính', 30.00, 0);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r15, N'Bằng cấp chuyên ngành Tài chính / Kế toán', N'Education', N'Đại học chuyên ngành', 20.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r16, N'Kinh nghiệm Data Engineer & ETL', N'Experience', N'>= 2 năm', 50.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r16, N'Thành thạo SQL & Python', N'Skill', N'SQL Server/PostgreSQL, Python', 30.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r16, N'Kiến thức Data Warehouse & Airflow', N'Skill', N'DWH, Apache Airflow', 20.00, 0);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r17, N'Kinh nghiệm Business Analyst phần mềm', N'Experience', N'>= 2 năm', 50.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r17, N'Kỹ năng Use Case & BPMN', N'Skill', N'Use Case, User Story, BPMN', 30.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r17, N'Trình độ Đại học', N'Education', N'Đại học', 20.00, 0);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r20, N'Kinh nghiệm bán hàng B2B phần mềm', N'Experience', N'>= 4 năm', 40.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r20, N'Kinh nghiệm quản lý khách hàng lớn (Key Account)', N'Experience', N'>= 2 năm', 40.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r20, N'Kỹ năng thương lượng & đàm phán cấp cao', N'Skill', N'Đàm phán hợp đồng, thuyết trình', 20.00, 0);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r21, N'Kinh nghiệm phân tích dữ liệu sản phẩm', N'Experience', N'>= 2 năm', 50.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r21, N'Thành thạo SQL & BI Dashboard', N'Skill', N'SQL, Power BI hoặc Tableau', 30.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r21, N'Phân tích phễu & A/B testing', N'Skill', N'A/B Testing, Funnel Analysis', 20.00, 0);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r22, N'Kinh nghiệm lập trình Solidity & Smart Contract', N'Experience', N'>= 2 năm', 60.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r22, N'Kiến thức Web3 & tích hợp ví điện tử', N'Skill', N'Web3.js, Ethers.js', 40.00, 0);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r23, N'Kinh nghiệm tổ chức sự kiện B2B', N'Experience', N'>= 2 năm', 60.00, 1);
INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) VALUES (@r23, N'Kỹ năng điều phối & quản lý ngân sách sự kiện', N'Skill', N'Quản lý sự kiện, Điều phối vendor', 40.00, 0);

-- =============================================================================
-- 4.1 AUDIT LOG CHO JOB REQUISITION (Lịch sử thao tác cho các yêu cầu tuyển dụng)
-- =============================================================================
-- Req 5: Junior Java Developer (Approved)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmTuan, N'CREATE', N'JobRequisition', CAST(@r5 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: Junior Java Developer · Pending_Director', N'192.168.1.25', '2026-09-18 09:00:00');
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@director, N'UPDATE', N'JobRequisition', CAST(@r5 AS NVARCHAR(50)), N'Status: Pending_Director', N'Status: Pending_Director → Approved' + CHAR(10) + N'Ý kiến Giám đốc: Đồng ý tuyển 3 lập trình viên Junior cho văn phòng TP. Hồ Chí Minh.', N'192.168.1.15', '2026-09-19 10:00:00');

-- Req 6: DevOps Engineer (Approved)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmTuan, N'CREATE', N'JobRequisition', CAST(@r6 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: DevOps Engineer (AWS / Kubernetes) · Pending_Director', N'192.168.1.25', '2026-09-19 10:00:00');
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@director, N'UPDATE', N'JobRequisition', CAST(@r6 AS NVARCHAR(50)), N'Status: Pending_Director', N'Status: Pending_Director → Approved' + CHAR(10) + N'Ý kiến Giám đốc: Phê duyệt, ưu tiên ứng viên có chứng chỉ AWS.', N'192.168.1.15', '2026-09-20 11:00:00');

-- Req 7: Mobile Developer (Flutter) (Approved)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmTuan, N'CREATE', N'JobRequisition', CAST(@r7 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: Mobile Developer (Flutter) · Pending_Director', N'192.168.1.25', '2026-09-20 09:30:00');
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@director, N'UPDATE', N'JobRequisition', CAST(@r7 AS NVARCHAR(50)), N'Status: Pending_Director', N'Status: Pending_Director → Approved' + CHAR(10) + N'Ý kiến Giám đốc: Đồng ý theo hình thức hợp đồng 12 tháng của dự án.', N'192.168.1.15', '2026-09-21 10:00:00');

-- Req 8: Thực tập sinh Kỹ sư Phần mềm (Approved)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmTuan, N'CREATE', N'JobRequisition', CAST(@r8 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: Thực tập sinh Kỹ sư Phần mềm · Pending_Director', N'192.168.1.25', '2026-09-21 14:00:00');
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@director, N'UPDATE', N'JobRequisition', CAST(@r8 AS NVARCHAR(50)), N'Status: Pending_Director', N'Status: Pending_Director → Approved' + CHAR(10) + N'Ý kiến Giám đốc: Phê duyệt chương trình thực tập kỳ Xuân 2027.', N'192.168.1.15', '2026-09-22 09:00:00');

-- Req 9: Digital Marketing Executive (Approved)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmHuong, N'CREATE', N'JobRequisition', CAST(@r9 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: Digital Marketing Executive · Pending_Director', N'192.168.1.30', '2026-09-22 09:00:00');
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@director, N'UPDATE', N'JobRequisition', CAST(@r9 AS NVARCHAR(50)), N'Status: Pending_Director', N'Status: Pending_Director → Approved' + CHAR(10) + N'Ý kiến Giám đốc: Đồng ý, ngân sách marketing số đã được duyệt.', N'192.168.1.15', '2026-09-23 09:30:00');

-- Req 10: Nhân viên Telesales (Bán thời gian) (Approved)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmHuong, N'CREATE', N'JobRequisition', CAST(@r10 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: Nhân viên Telesales (Bán thời gian) · Pending_Director', N'192.168.1.30', '2026-09-23 10:00:00');
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@director, N'UPDATE', N'JobRequisition', CAST(@r10 AS NVARCHAR(50)), N'Status: Pending_Director', N'Status: Pending_Director → Approved' + CHAR(10) + N'Ý kiến Giám đốc: Đồng ý tuyển 4 nhân viên bán thời gian.', N'192.168.1.15', '2026-09-24 09:00:00');

-- Req 11: Kế toán tổng hợp (Approved)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmKhanh, N'CREATE', N'JobRequisition', CAST(@r11 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: Kế toán tổng hợp · Pending_Director', N'192.168.1.35', '2026-09-17 09:00:00');
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@director, N'UPDATE', N'JobRequisition', CAST(@r11 AS NVARCHAR(50)), N'Status: Pending_Director', N'Status: Pending_Director → Approved' + CHAR(10) + N'Ý kiến Giám đốc: Đồng ý tuyển thay thế, cần sớm bàn giao trước kỳ quyết toán.', N'192.168.1.15', '2026-09-18 09:00:00');

-- Req 12: UI/UX Designer (Approved)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmTrang, N'CREATE', N'JobRequisition', CAST(@r12 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: UI/UX Designer · Pending_Director', N'192.168.1.40', '2026-09-18 15:00:00');
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@director, N'UPDATE', N'JobRequisition', CAST(@r12 AS NVARCHAR(50)), N'Status: Pending_Director', N'Status: Pending_Director → Approved' + CHAR(10) + N'Ý kiến Giám đốc: Phê duyệt vị trí đầu tiên của đội thiết kế.', N'192.168.1.15', '2026-09-19 16:00:00');

-- Req 13: Product Owner (Approved)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmTrang, N'CREATE', N'JobRequisition', CAST(@r13 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: Product Owner · Pending_Director', N'192.168.1.40', '2026-09-24 09:00:00');
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@director, N'UPDATE', N'JobRequisition', CAST(@r13 AS NVARCHAR(50)), N'Status: Pending_Director', N'Status: Pending_Director → Approved' + CHAR(10) + N'Ý kiến Giám đốc: Phê duyệt, mức lương theo khung đã đề xuất.', N'192.168.1.15', '2026-09-25 10:00:00');

-- Req 14: Thực tập sinh Kiểm thử Phần mềm (Approved)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmTuan, N'CREATE', N'JobRequisition', CAST(@r14 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: Thực tập sinh Kiểm thử Phần mềm · Pending_Director', N'192.168.1.25', '2026-09-01 09:00:00');
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@director, N'UPDATE', N'JobRequisition', CAST(@r14 AS NVARCHAR(50)), N'Status: Pending_Director', N'Status: Pending_Director → Approved' + CHAR(10) + N'Ý kiến Giám đốc: Phê duyệt chương trình thực tập kỳ Thu 2026.', N'192.168.1.15', '2026-09-02 09:00:00');

-- Req 15: Chuyên viên Phân tích Tài chính (Approved)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmKhanh, N'CREATE', N'JobRequisition', CAST(@r15 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: Chuyên viên Phân tích Tài chính · Pending_Director', N'192.168.1.35', '2026-09-05 09:00:00');
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@director, N'UPDATE', N'JobRequisition', CAST(@r15 AS NVARCHAR(50)), N'Status: Pending_Director', N'Status: Pending_Director → Approved' + CHAR(10) + N'Ý kiến Giám đốc: Phê duyệt cho kế hoạch ngân sách 2027.', N'192.168.1.15', '2026-09-06 09:00:00');

-- Req 16: Data Engineer (Approved)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmTuan, N'CREATE', N'JobRequisition', CAST(@r16 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: Data Engineer · Pending_Director', N'192.168.1.25', '2026-09-12 09:00:00');
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@director, N'UPDATE', N'JobRequisition', CAST(@r16 AS NVARCHAR(50)), N'Status: Pending_Director', N'Status: Pending_Director → Approved' + CHAR(10) + N'Ý kiến Giám đốc: Đồng ý, tạm dừng đăng tin khi đủ ứng viên vòng trong.', N'192.168.1.15', '2026-09-13 09:00:00');

-- Req 17: Business Analyst (Approved)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmTrang, N'CREATE', N'JobRequisition', CAST(@r17 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: Business Analyst · Pending_Director', N'192.168.1.40', '2026-10-02 09:00:00');
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@director, N'UPDATE', N'JobRequisition', CAST(@r17 AS NVARCHAR(50)), N'Status: Pending_Director', N'Status: Pending_Director → Approved' + CHAR(10) + N'Ý kiến Giám đốc: Đồng ý tuyển BA cho các module mới.', N'192.168.1.15', '2026-10-03 10:00:00');

-- Req 18: Tech Lead .NET (Draft)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmTuan, N'CREATE', N'JobRequisition', CAST(@r18 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: Tech Lead .NET · Draft', N'192.168.1.25', '2026-10-06 09:00:00');

-- Req 19: Chuyên viên Kiểm toán Nội bộ (Draft)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmKhanh, N'CREATE', N'JobRequisition', CAST(@r19 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: Chuyên viên Kiểm toán Nội bộ · Draft', N'192.168.1.35', '2026-10-07 10:00:00');

-- Req 20: Key Account Manager (Pending_Director)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmHuong, N'CREATE', N'JobRequisition', CAST(@r20 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: Key Account Manager · Pending_Director', N'192.168.1.30', '2026-10-03 09:00:00');

-- Req 21: Chuyên viên Phân tích Dữ liệu Sản phẩm (Pending_Director)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmTrang, N'CREATE', N'JobRequisition', CAST(@r21 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: Chuyên viên Phân tích Dữ liệu Sản phẩm · Pending_Director', N'192.168.1.40', '2026-10-05 14:00:00');

-- Req 22: Blockchain Developer (Rejected)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmTuan, N'CREATE', N'JobRequisition', CAST(@r22 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: Blockchain Developer · Pending_Director', N'192.168.1.25', '2026-09-25 09:00:00');
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@director, N'UPDATE', N'JobRequisition', CAST(@r22 AS NVARCHAR(50)), N'Status: Pending_Director', N'Status: Pending_Director → Rejected' + CHAR(10) + N'Ý kiến Giám đốc: Chưa nằm trong định hướng sản phẩm năm 2027. Đề nghị nghiên cứu thêm tính khả thi trước khi đề xuất lại.', N'192.168.1.15', '2026-09-27 10:00:00');

-- Req 23: Event Marketing Specialist (Rejected)
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@hmHuong, N'CREATE', N'JobRequisition', CAST(@r23 AS NVARCHAR(50)), NULL, N'Tạo yêu cầu: Event Marketing Specialist · Pending_Director', N'192.168.1.30', '2026-09-26 09:00:00');
INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) VALUES (@director, N'UPDATE', N'JobRequisition', CAST(@r23 AS NVARCHAR(50)), N'Status: Pending_Director', N'Status: Pending_Director → Rejected' + CHAR(10) + N'Ý kiến Giám đốc: Ngân sách sự kiện quý 1 chưa được duyệt. Đề nghị gộp vào kế hoạch marketing chung.', N'192.168.1.15', '2026-09-28 09:00:00');

-- =============================================================================
-- 5. JOB POSTING
-- =============================================================================
DECLARE @p_r5 INT, @p_r6 INT, @p_r7 INT, @p_r8 INT, @p_r9 INT, @p_r10 INT, @p_r11 INT, @p_r12 INT, @p_r13 INT, @p_r14 INT, @p_r15 INT, @p_r16 INT;
-- Junior Java Developer (Spring Boot) [Published]
INSERT INTO JobPosting (RequisitionId, PostingNo, PostingTitle, JobDescription, JobRequirements, Benefits, SalaryDisplay, NumberOfPositionsDisplay, WorkLocation, PostingDate, ApplicationDeadline, PostingStatus, ClosedAt, CreatedBy, CreatedAt) VALUES (@r5, 1, N'Junior Java Developer (Spring Boot)', N'Tham gia đội phát triển sản phẩm RMS tại TP. Hồ Chí Minh, làm việc với Spring Boot, SQL Server và Docker.', N'- Tối thiểu 1 năm kinh nghiệm Java
- Nắm vững OOP, Spring Boot cơ bản
- Biết viết SQL, ưu tiên biết Docker', N'- Lương tháng 13 và thưởng hiệu quả công việc
- Bảo hiểm sức khỏe cao cấp cho nhân viên
- 12 ngày phép năm, du lịch công ty hằng năm
- Ngân sách đào tạo 10 triệu/năm', N'15,000,000 - 22,000,000 VND', N'3', N'Tầng 5, Tòa nhà RMS Sài Gòn, 72 Lê Thánh Tôn, Quận 1, TP. Hồ Chí Minh', '2026-09-28 08:00:00', '2026-12-31 23:59:59', N'Published', NULL, @hrLan, '2026-09-28 08:00:00');
SET @p_r5 = SCOPE_IDENTITY();
-- DevOps Engineer (AWS / Kubernetes) – Remote [Published]
INSERT INTO JobPosting (RequisitionId, PostingNo, PostingTitle, JobDescription, JobRequirements, Benefits, SalaryDisplay, NumberOfPositionsDisplay, WorkLocation, PostingDate, ApplicationDeadline, PostingStatus, ClosedAt, CreatedBy, CreatedAt) VALUES (@r6, 1, N'DevOps Engineer (AWS / Kubernetes) – Remote', N'Vận hành hạ tầng đám mây và chuẩn hóa quy trình CI/CD cho toàn bộ sản phẩm, làm việc hoàn toàn từ xa.', N'- Tối thiểu 3 năm kinh nghiệm DevOps
- Thành thạo AWS, Docker, Kubernetes, Terraform
- Có chứng chỉ AWS là lợi thế', N'- Lương tháng 13 và thưởng hiệu quả công việc
- Bảo hiểm sức khỏe cao cấp cho nhân viên
- 12 ngày phép năm, du lịch công ty hằng năm
- Ngân sách đào tạo 10 triệu/năm
- Hỗ trợ chi phí internet và thiết bị làm việc tại nhà', N'35,000,000 - 50,000,000 VND', N'1', N'Làm việc từ xa (Remote) – toàn quốc', '2026-09-29 08:00:00', '2027-01-31 23:59:59', N'Published', NULL, @hrQuang, '2026-09-29 08:00:00');
SET @p_r6 = SCOPE_IDENTITY();
-- Mobile Developer (Flutter) – Hợp đồng 12 tháng [Published]
INSERT INTO JobPosting (RequisitionId, PostingNo, PostingTitle, JobDescription, JobRequirements, Benefits, SalaryDisplay, NumberOfPositionsDisplay, WorkLocation, PostingDate, ApplicationDeadline, PostingStatus, ClosedAt, CreatedBy, CreatedAt) VALUES (@r7, 1, N'Mobile Developer (Flutter) – Hợp đồng 12 tháng', N'Phát triển ứng dụng di động đa nền tảng cho khách hàng lĩnh vực bán lẻ tại văn phòng Đà Nẵng.', N'- Tối thiểu 2 năm kinh nghiệm Flutter/Dart
- Đã phát hành ít nhất 1 ứng dụng lên store
- Biết tích hợp REST API, thanh toán điện tử', N'- Lương hợp đồng cạnh tranh
- Thưởng hoàn thành dự án
- Làm việc hybrid 3 ngày tại văn phòng', N'20,000,000 - 30,000,000 VND', N'2', N'Tầng 3, Tòa nhà Phần mềm Đà Nẵng, Hải Châu, Đà Nẵng', '2026-09-30 08:00:00', '2026-12-31 23:59:59', N'Published', NULL, @hrQuang, '2026-09-30 08:00:00');
SET @p_r7 = SCOPE_IDENTITY();
-- Thực tập sinh Kỹ sư Phần mềm – Kỳ Xuân 2027 [Published]
INSERT INTO JobPosting (RequisitionId, PostingNo, PostingTitle, JobDescription, JobRequirements, Benefits, SalaryDisplay, NumberOfPositionsDisplay, WorkLocation, PostingDate, ApplicationDeadline, PostingStatus, ClosedAt, CreatedBy, CreatedAt) VALUES (@r8, 1, N'Thực tập sinh Kỹ sư Phần mềm – Kỳ Xuân 2027', N'Chương trình thực tập 3 tháng dành cho sinh viên CNTT, làm việc trên sản phẩm thực tế cùng mentor.', N'- Sinh viên năm 3–4 ngành CNTT
- Biết Java hoặc JavaScript
- Làm tối thiểu 4 buổi/tuần', N'- Trợ cấp thực tập hằng tháng
- Có mentor kèm cặp 1-1
- Cơ hội trở thành nhân viên chính thức sau kỳ thực tập
- Thời gian linh hoạt theo lịch học', N'4,000,000 - 6,000,000 VND (trợ cấp)', N'5', N'Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội', '2026-09-30 09:00:00', '2027-01-15 23:59:59', N'Published', NULL, @hrLan, '2026-09-30 09:00:00');
SET @p_r8 = SCOPE_IDENTITY();
-- Digital Marketing Executive (B2B SaaS) [Published]
INSERT INTO JobPosting (RequisitionId, PostingNo, PostingTitle, JobDescription, JobRequirements, Benefits, SalaryDisplay, NumberOfPositionsDisplay, WorkLocation, PostingDate, ApplicationDeadline, PostingStatus, ClosedAt, CreatedBy, CreatedAt) VALUES (@r9, 1, N'Digital Marketing Executive (B2B SaaS)', N'Phụ trách các chiến dịch quảng cáo số giúp tăng khách hàng tiềm năng cho sản phẩm phần mềm doanh nghiệp.', N'- Tối thiểu 2 năm kinh nghiệm digital marketing B2B
- Thành thạo Google Ads, Meta Ads, Google Analytics
- Có khả năng viết nội dung tốt', N'- Lương tháng 13 và thưởng hiệu quả công việc
- Bảo hiểm sức khỏe cao cấp cho nhân viên
- 12 ngày phép năm, du lịch công ty hằng năm
- Ngân sách đào tạo 10 triệu/năm', N'14,000,000 - 20,000,000 VND', N'1', N'Tầng 5, Tòa nhà RMS Sài Gòn, 72 Lê Thánh Tôn, Quận 1, TP. Hồ Chí Minh', '2026-10-01 08:00:00', '2026-12-31 23:59:59', N'Published', NULL, @hrQuang, '2026-10-01 08:00:00');
SET @p_r9 = SCOPE_IDENTITY();
-- Nhân viên Telesales – Bán thời gian (ca 4 tiếng) [Published]
INSERT INTO JobPosting (RequisitionId, PostingNo, PostingTitle, JobDescription, JobRequirements, Benefits, SalaryDisplay, NumberOfPositionsDisplay, WorkLocation, PostingDate, ApplicationDeadline, PostingStatus, ClosedAt, CreatedBy, CreatedAt) VALUES (@r10, 1, N'Nhân viên Telesales – Bán thời gian (ca 4 tiếng)', N'Tư vấn sản phẩm qua điện thoại cho khách hàng doanh nghiệp, phù hợp sinh viên muốn đi làm thêm.', N'- Giọng nói rõ ràng, giao tiếp tốt
- Làm ca sáng hoặc ca chiều 4 tiếng/ngày
- Không yêu cầu kinh nghiệm', N'- Lương cứng + hoa hồng theo doanh số
- Được đào tạo kỹ năng bán hàng
- Thời gian linh hoạt', N'6,000,000 - 8,000,000 VND', N'4', N'Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội', '2026-10-02 08:00:00', '2027-02-28 23:59:59', N'Published', NULL, @hrLan, '2026-10-02 08:00:00');
SET @p_r10 = SCOPE_IDENTITY();
-- Kế toán tổng hợp [Published]
INSERT INTO JobPosting (RequisitionId, PostingNo, PostingTitle, JobDescription, JobRequirements, Benefits, SalaryDisplay, NumberOfPositionsDisplay, WorkLocation, PostingDate, ApplicationDeadline, PostingStatus, ClosedAt, CreatedBy, CreatedAt) VALUES (@r11, 1, N'Kế toán tổng hợp', N'Phụ trách toàn bộ nghiệp vụ kế toán và báo cáo thuế cho công ty, báo cáo trực tiếp Trưởng phòng Tài chính.', N'- Tối thiểu 3 năm kinh nghiệm kế toán tổng hợp
- Thành thạo MISA, Excel
- Nắm vững chuẩn mực kế toán Việt Nam', N'- Lương tháng 13 và thưởng hiệu quả công việc
- Bảo hiểm sức khỏe cao cấp cho nhân viên
- 12 ngày phép năm, du lịch công ty hằng năm
- Ngân sách đào tạo 10 triệu/năm', N'15,000,000 - 20,000,000 VND', N'1', N'Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội', '2026-09-28 09:00:00', '2026-12-31 23:59:59', N'Published', NULL, @hrLan, '2026-09-28 09:00:00');
SET @p_r11 = SCOPE_IDENTITY();
-- UI/UX Designer (Web & Mobile) [Published]
INSERT INTO JobPosting (RequisitionId, PostingNo, PostingTitle, JobDescription, JobRequirements, Benefits, SalaryDisplay, NumberOfPositionsDisplay, WorkLocation, PostingDate, ApplicationDeadline, PostingStatus, ClosedAt, CreatedBy, CreatedAt) VALUES (@r12, 1, N'UI/UX Designer (Web & Mobile)', N'Thành viên đầu tiên của đội thiết kế sản phẩm, tham gia định hình trải nghiệm người dùng cho toàn bộ hệ thống RMS.', N'- Tối thiểu 2 năm kinh nghiệm UI/UX
- Thành thạo Figma, hiểu về design system
- Có portfolio sản phẩm thực tế', N'- Lương tháng 13 và thưởng hiệu quả công việc
- Bảo hiểm sức khỏe cao cấp cho nhân viên
- 12 ngày phép năm, du lịch công ty hằng năm
- Ngân sách đào tạo 10 triệu/năm
- Cấp Macbook Pro và bản quyền Figma', N'18,000,000 - 28,000,000 VND', N'1', N'Tầng 5, Tòa nhà RMS Sài Gòn, 72 Lê Thánh Tôn, Quận 1, TP. Hồ Chí Minh', '2026-09-28 10:00:00', '2027-01-15 23:59:59', N'Published', NULL, @hrQuang, '2026-09-28 10:00:00');
SET @p_r12 = SCOPE_IDENTITY();
-- Product Owner – RMS cho doanh nghiệp vừa và nhỏ [Published]
INSERT INTO JobPosting (RequisitionId, PostingNo, PostingTitle, JobDescription, JobRequirements, Benefits, SalaryDisplay, NumberOfPositionsDisplay, WorkLocation, PostingDate, ApplicationDeadline, PostingStatus, ClosedAt, CreatedBy, CreatedAt) VALUES (@r13, 1, N'Product Owner – RMS cho doanh nghiệp vừa và nhỏ', N'Định hướng và quản lý backlog cho dòng sản phẩm RMS, làm việc trực tiếp với khách hàng và đội phát triển.', N'- Tối thiểu 3 năm kinh nghiệm Product Owner/BA
- Thành thạo Scrum, viết user story
- Tiếng Anh giao tiếp tốt', N'- Lương tháng 13 và thưởng hiệu quả công việc
- Bảo hiểm sức khỏe cao cấp cho nhân viên
- 12 ngày phép năm, du lịch công ty hằng năm
- Ngân sách đào tạo 10 triệu/năm', N'Thỏa thuận', N'1', N'Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội', '2026-10-03 08:00:00', '2027-01-31 23:59:59', N'Published', NULL, @hrLan, '2026-10-03 08:00:00');
SET @p_r13 = SCOPE_IDENTITY();
-- Thực tập sinh Kiểm thử Phần mềm – Kỳ Thu 2026 [Published, HẾT HẠN]
INSERT INTO JobPosting (RequisitionId, PostingNo, PostingTitle, JobDescription, JobRequirements, Benefits, SalaryDisplay, NumberOfPositionsDisplay, WorkLocation, PostingDate, ApplicationDeadline, PostingStatus, ClosedAt, CreatedBy, CreatedAt) VALUES (@r14, 1, N'Thực tập sinh Kiểm thử Phần mềm – Kỳ Thu 2026', N'Chương trình thực tập kiểm thử phần mềm, đã hết hạn nhận hồ sơ.', N'- Sinh viên năm 3–4 ngành CNTT
- Cẩn thận, có tư duy logic', N'- Trợ cấp thực tập hằng tháng
- Có mentor kèm cặp 1-1
- Cơ hội trở thành nhân viên chính thức sau kỳ thực tập
- Thời gian linh hoạt theo lịch học', N'3,000,000 - 5,000,000 VND (trợ cấp)', N'2', N'Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội', '2026-09-03 08:00:00', '2026-10-01 23:59:59', N'Published', NULL, @hrLan, '2026-09-03 08:00:00');
SET @p_r14 = SCOPE_IDENTITY();
-- Chuyên viên Phân tích Tài chính [Closed]
INSERT INTO JobPosting (RequisitionId, PostingNo, PostingTitle, JobDescription, JobRequirements, Benefits, SalaryDisplay, NumberOfPositionsDisplay, WorkLocation, PostingDate, ApplicationDeadline, PostingStatus, ClosedAt, CreatedBy, CreatedAt) VALUES (@r15, 1, N'Chuyên viên Phân tích Tài chính', N'Phân tích tài chính và lập kế hoạch ngân sách. Tin đã đóng do tuyển đủ.', N'- Tối thiểu 2 năm kinh nghiệm phân tích tài chính
- Thành thạo Excel, Power BI', N'- Lương tháng 13 và thưởng hiệu quả công việc
- Bảo hiểm sức khỏe cao cấp cho nhân viên
- 12 ngày phép năm, du lịch công ty hằng năm
- Ngân sách đào tạo 10 triệu/năm', N'20,000,000 - 30,000,000 VND', N'1', N'Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội', '2026-09-07 08:00:00', '2026-11-30 23:59:59', N'Closed', '2026-10-10 17:00:00', @hrQuang, '2026-09-07 08:00:00');
SET @p_r15 = SCOPE_IDENTITY();
-- Data Engineer (ETL / Data Warehouse) [Paused]
INSERT INTO JobPosting (RequisitionId, PostingNo, PostingTitle, JobDescription, JobRequirements, Benefits, SalaryDisplay, NumberOfPositionsDisplay, WorkLocation, PostingDate, ApplicationDeadline, PostingStatus, ClosedAt, CreatedBy, CreatedAt) VALUES (@r16, 1, N'Data Engineer (ETL / Data Warehouse)', N'Xây dựng kho dữ liệu phục vụ báo cáo tuyển dụng. Tin đang tạm dừng nhận hồ sơ.', N'- Tối thiểu 2 năm kinh nghiệm Data Engineer
- Thành thạo SQL, Python, Airflow', N'- Lương tháng 13 và thưởng hiệu quả công việc
- Bảo hiểm sức khỏe cao cấp cho nhân viên
- 12 ngày phép năm, du lịch công ty hằng năm
- Ngân sách đào tạo 10 triệu/năm', N'30,000,000 - 42,000,000 VND', N'1', N'Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội', '2026-09-14 08:00:00', '2026-12-31 23:59:59', N'Paused', NULL, @hrLan, '2026-09-14 08:00:00');
SET @p_r16 = SCOPE_IDENTITY();

-- =============================================================================
-- 6. ĐƠN ỨNG TUYỂN TRÊN TIN MỚI (+ kết quả AI, review của HR/HM)
--    Ứng viên lấy từ seed_data.sql theo Username. AI chỉ là điểm tham khảo;
--    Rejected ở đây là quyết định của HR/HM (có ApplicationReview Fail).
-- =============================================================================
DECLARE @c INT, @a INT;
-- thang.duong -> Junior Java Developer (Spring Boot) [Applied]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'thang.duong');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r5, N'/samples/cv/sample_cv_fresher_intern.pdf', '2026-10-08 09:15:00', N'Applied', NULL, '2026-10-08 09:15:00');
SET @a = SCOPE_IDENTITY();
-- quynh.ho -> Junior Java Developer (Spring Boot) [Applied]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'quynh.ho');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r5, N'/samples/cv/sample_cv_java_backend.pdf', '2026-10-09 14:40:00', N'Applied', NULL, '2026-10-09 14:40:00');
SET @a = SCOPE_IDENTITY();
-- phuc.le -> Junior Java Developer (Spring Boot) [AI_Screened]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'phuc.le');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r5, N'/samples/cv/sample_cv_java_backend.pdf', '2026-10-05 10:20:00', N'AI_Screened', 81.50, '2026-10-05 10:20:00');
SET @a = SCOPE_IDENTITY();
INSERT INTO AIScreeningResult (ApplicationId, AIMatchScore, ScreenedAt) VALUES (@a, 81.50, '2026-10-05 10:23:00');
-- vinh.hoang -> Junior Java Developer (Spring Boot) [AI_Screened]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'vinh.hoang');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r5, N'/samples/cv/sample_cv_java_backend.pdf', '2026-10-06 16:05:00', N'AI_Screened', 64.20, '2026-10-06 16:05:00');
SET @a = SCOPE_IDENTITY();
INSERT INTO AIScreeningResult (ApplicationId, AIMatchScore, ScreenedAt) VALUES (@a, 64.20, '2026-10-06 16:08:00');
-- an.dinh -> Junior Java Developer (Spring Boot) [HR_Passed]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'an.dinh');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r5, N'/samples/cv/sample_cv_java_backend.pdf', '2026-10-02 08:50:00', N'HR_Passed', 86.40, '2026-10-02 08:50:00');
SET @a = SCOPE_IDENTITY();
INSERT INTO AIScreeningResult (ApplicationId, AIMatchScore, ScreenedAt) VALUES (@a, 86.40, '2026-10-02 08:53:00');
INSERT INTO ApplicationReview (ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt) VALUES (@a, @hrLan, N'HR', N'Pass', N'Kinh nghiệm Spring Boot phù hợp, chuyển Trưởng bộ phận đánh giá.', '2026-10-03 08:50:00');
-- nga.do -> Junior Java Developer (Spring Boot) [HM_Passed]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'nga.do');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r5, N'/samples/cv/sample_cv_java_backend.pdf', '2026-10-01 11:30:00', N'HM_Passed', 90.10, '2026-10-01 11:30:00');
SET @a = SCOPE_IDENTITY();
INSERT INTO AIScreeningResult (ApplicationId, AIMatchScore, ScreenedAt) VALUES (@a, 90.10, '2026-10-01 11:33:00');
INSERT INTO ApplicationReview (ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt) VALUES (@a, @hrLan, N'HR', N'Pass', N'Hồ sơ đạt yêu cầu sàng lọc, chuyển Trưởng bộ phận.', '2026-10-02 11:30:00');
INSERT INTO ApplicationReview (ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt) VALUES (@a, @hmTuan, N'HiringManager', N'Pass', N'Nền tảng tốt, đề xuất mời phỏng vấn kỹ thuật.', '2026-10-03 11:30:00');
-- phong.nguyen -> Junior Java Developer (Spring Boot) [Rejected]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'phong.nguyen');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r5, N'/samples/cv/sample_cv_fresher_intern.pdf', '2026-10-03 13:10:00', N'Rejected', 42.30, '2026-10-03 13:10:00');
SET @a = SCOPE_IDENTITY();
INSERT INTO AIScreeningResult (ApplicationId, AIMatchScore, ScreenedAt) VALUES (@a, 42.30, '2026-10-03 13:13:00');
INSERT INTO ApplicationReview (ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt) VALUES (@a, @hrLan, N'HR', N'Fail', N'Chưa đáp ứng tiêu chí bắt buộc: chưa có kinh nghiệm Spring Boot thực tế.', '2026-10-04 13:10:00');
-- van.ly -> Junior Java Developer (Spring Boot) [Rejected]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'van.ly');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r5, N'/samples/cv/sample_cv_java_backend.pdf', '2026-10-02 15:45:00', N'Rejected', 71.80, '2026-10-02 15:45:00');
SET @a = SCOPE_IDENTITY();
INSERT INTO AIScreeningResult (ApplicationId, AIMatchScore, ScreenedAt) VALUES (@a, 71.80, '2026-10-02 15:48:00');
INSERT INTO ApplicationReview (ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt) VALUES (@a, @hrLan, N'HR', N'Pass', N'Hồ sơ đạt yêu cầu sàng lọc, chuyển Trưởng bộ phận.', '2026-10-03 15:45:00');
INSERT INTO ApplicationReview (ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt) VALUES (@a, @hmTuan, N'HiringManager', N'Fail', N'Kiến thức thiết kế cơ sở dữ liệu chưa đạt yêu cầu của vị trí.', '2026-10-04 15:45:00');
-- viet.trinh -> Thực tập sinh Kỹ sư Phần mềm – Kỳ Xuân 2027 [Applied]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'viet.trinh');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r8, N'/samples/cv/sample_cv_fresher_intern.pdf', '2026-10-09 10:00:00', N'Applied', NULL, '2026-10-09 10:00:00');
SET @a = SCOPE_IDENTITY();
-- thang.dang -> Thực tập sinh Kỹ sư Phần mềm – Kỳ Xuân 2027 [Applied]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'thang.dang');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r8, N'/samples/cv/sample_cv_fresher_intern.pdf', '2026-10-08 19:30:00', N'Applied', NULL, '2026-10-08 19:30:00');
SET @a = SCOPE_IDENTITY();
-- long.pham -> Thực tập sinh Kỹ sư Phần mềm – Kỳ Xuân 2027 [AI_Screened]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'long.pham');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r8, N'/samples/cv/sample_cv_fresher_intern.pdf', '2026-10-04 09:40:00', N'AI_Screened', 77.00, '2026-10-04 09:40:00');
SET @a = SCOPE_IDENTITY();
INSERT INTO AIScreeningResult (ApplicationId, AIMatchScore, ScreenedAt) VALUES (@a, 77.00, '2026-10-04 09:43:00');
-- yen.pham -> Thực tập sinh Kỹ sư Phần mềm – Kỳ Xuân 2027 [HR_Passed]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'yen.pham');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r8, N'/samples/cv/sample_cv_fresher_intern.pdf', '2026-10-02 20:10:00', N'HR_Passed', 83.60, '2026-10-02 20:10:00');
SET @a = SCOPE_IDENTITY();
INSERT INTO AIScreeningResult (ApplicationId, AIMatchScore, ScreenedAt) VALUES (@a, 83.60, '2026-10-02 20:13:00');
INSERT INTO ApplicationReview (ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt) VALUES (@a, @hrLan, N'HR', N'Pass', N'Điểm học tập tốt, có dự án nhóm Spring Boot.', '2026-10-03 20:10:00');
-- hieu.dinh -> Thực tập sinh Kỹ sư Phần mềm – Kỳ Xuân 2027 [Rejected]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'hieu.dinh');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r8, N'/samples/cv/sample_cv_fresher_intern.pdf', '2026-10-03 07:55:00', N'Rejected', 38.90, '2026-10-03 07:55:00');
SET @a = SCOPE_IDENTITY();
INSERT INTO AIScreeningResult (ApplicationId, AIMatchScore, ScreenedAt) VALUES (@a, 38.90, '2026-10-03 07:58:00');
INSERT INTO ApplicationReview (ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt) VALUES (@a, @hrLan, N'HR', N'Fail', N'Chưa phải sinh viên năm 3 trở lên theo tiêu chí bắt buộc.', '2026-10-04 07:55:00');
-- khanh.ho -> Kế toán tổng hợp [AI_Screened]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'khanh.ho');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r11, N'/samples/cv/sample_cv_accountant.pdf', '2026-10-04 08:30:00', N'AI_Screened', 88.20, '2026-10-04 08:30:00');
SET @a = SCOPE_IDENTITY();
INSERT INTO AIScreeningResult (ApplicationId, AIMatchScore, ScreenedAt) VALUES (@a, 88.20, '2026-10-04 08:33:00');
-- nam.ly -> Kế toán tổng hợp [AI_Screened]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'nam.ly');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r11, N'/samples/cv/sample_cv_accountant.pdf', '2026-10-06 11:20:00', N'AI_Screened', 58.70, '2026-10-06 11:20:00');
SET @a = SCOPE_IDENTITY();
INSERT INTO AIScreeningResult (ApplicationId, AIMatchScore, ScreenedAt) VALUES (@a, 58.70, '2026-10-06 11:23:00');
-- tuan.bui -> Kế toán tổng hợp [Rejected]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'tuan.bui');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r11, N'/samples/cv/sample_cv_accountant.pdf', '2026-10-02 14:00:00', N'Rejected', 33.10, '2026-10-02 14:00:00');
SET @a = SCOPE_IDENTITY();
INSERT INTO AIScreeningResult (ApplicationId, AIMatchScore, ScreenedAt) VALUES (@a, 33.10, '2026-10-02 14:03:00');
INSERT INTO ApplicationReview (ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt) VALUES (@a, @hrLan, N'HR', N'Fail', N'Hồ sơ chưa có kinh nghiệm kế toán tổng hợp.', '2026-10-03 14:00:00');
-- viet.do -> Kế toán tổng hợp [Applied]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'viet.do');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r11, N'/samples/cv/sample_cv_accountant.pdf', '2026-10-09 09:05:00', N'Applied', NULL, '2026-10-09 09:05:00');
SET @a = SCOPE_IDENTITY();
-- an.vo -> UI/UX Designer (Web & Mobile) [HM_Passed]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'an.vo');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r12, N'/samples/cv/sample_cv_uiux_designer.pdf', '2026-10-01 09:10:00', N'HM_Passed', 91.30, '2026-10-01 09:10:00');
SET @a = SCOPE_IDENTITY();
INSERT INTO AIScreeningResult (ApplicationId, AIMatchScore, ScreenedAt) VALUES (@a, 91.30, '2026-10-01 09:13:00');
INSERT INTO ApplicationReview (ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt) VALUES (@a, @hrQuang, N'HR', N'Pass', N'Hồ sơ đạt yêu cầu sàng lọc, chuyển Trưởng bộ phận.', '2026-10-02 09:10:00');
INSERT INTO ApplicationReview (ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt) VALUES (@a, @hmTrang, N'HiringManager', N'Pass', N'Portfolio ấn tượng, đề xuất mời phỏng vấn và làm bài test thiết kế.', '2026-10-03 09:10:00');
-- hang.nguyen -> UI/UX Designer (Web & Mobile) [HR_Passed]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'hang.nguyen');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r12, N'/samples/cv/sample_cv_uiux_designer.pdf', '2026-10-03 10:45:00', N'HR_Passed', 84.90, '2026-10-03 10:45:00');
SET @a = SCOPE_IDENTITY();
INSERT INTO AIScreeningResult (ApplicationId, AIMatchScore, ScreenedAt) VALUES (@a, 84.90, '2026-10-03 10:48:00');
INSERT INTO ApplicationReview (ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt) VALUES (@a, @hrQuang, N'HR', N'Pass', N'Có kinh nghiệm design system, chuyển Trưởng bộ phận đánh giá.', '2026-10-04 10:45:00');
-- hai.huynh -> UI/UX Designer (Web & Mobile) [AI_Screened]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'hai.huynh');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r12, N'/samples/cv/sample_cv_uiux_designer.pdf', '2026-10-05 15:30:00', N'AI_Screened', 73.40, '2026-10-05 15:30:00');
SET @a = SCOPE_IDENTITY();
INSERT INTO AIScreeningResult (ApplicationId, AIMatchScore, ScreenedAt) VALUES (@a, 73.40, '2026-10-05 15:33:00');
-- binh.hoang -> UI/UX Designer (Web & Mobile) [Rejected]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'binh.hoang');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r12, N'/samples/cv/sample_cv_uiux_designer.pdf', '2026-10-04 17:20:00', N'Rejected', 47.60, '2026-10-04 17:20:00');
SET @a = SCOPE_IDENTITY();
INSERT INTO AIScreeningResult (ApplicationId, AIMatchScore, ScreenedAt) VALUES (@a, 47.60, '2026-10-04 17:23:00');
INSERT INTO ApplicationReview (ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt) VALUES (@a, @hrQuang, N'HR', N'Fail', N'Chưa có portfolio sản phẩm thực tế theo tiêu chí bắt buộc.', '2026-10-05 17:20:00');
-- duc.le -> UI/UX Designer (Web & Mobile) [Applied]
SET @c = (SELECT c.CandidateId FROM Candidate c JOIN [User] u ON u.UserId = c.UserId WHERE u.Username = N'duc.le');
INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) VALUES (@c, @p_r12, N'/samples/cv/sample_cv_uiux_designer.pdf', '2026-10-09 16:50:00', N'Applied', NULL, '2026-10-09 16:50:00');
SET @a = SCOPE_IDENTITY();

-- =============================================================================
-- 7. CV MẪU: trỏ đơn cũ (seed_data.sql) về file PDF mở được thay cho link S3 giả
--    File nằm ở src/main/resources/static/samples/cv/ (cần đăng nhập để xem)
-- =============================================================================
UPDATE a SET a.AppliedCvUrl = N'/samples/cv/sample_cv_java_backend.pdf'
FROM Application a JOIN JobPosting jp ON jp.JobPostingId = a.JobPostingId
WHERE a.AppliedCvUrl LIKE N'https://rms-storage.s3.%' AND jp.PostingTitle = N'Senior Java Backend Engineer (Spring Boot / Microservices)';
UPDATE a SET a.AppliedCvUrl = N'/samples/cv/sample_cv_frontend_react.pdf'
FROM Application a JOIN JobPosting jp ON jp.JobPostingId = a.JobPostingId
WHERE a.AppliedCvUrl LIKE N'https://rms-storage.s3.%' AND jp.PostingTitle = N'Frontend ReactJS Developer (TypeScript / Next.js)';
UPDATE a SET a.AppliedCvUrl = N'/samples/cv/sample_cv_qa_automation.pdf'
FROM Application a JOIN JobPosting jp ON jp.JobPostingId = a.JobPostingId
WHERE a.AppliedCvUrl LIKE N'https://rms-storage.s3.%' AND jp.PostingTitle = N'QA Automation Engineer (Playwright / Selenium / API)';
UPDATE a SET a.AppliedCvUrl = N'/samples/cv/sample_cv_business_development.pdf'
FROM Application a JOIN JobPosting jp ON jp.JobPostingId = a.JobPostingId
WHERE a.AppliedCvUrl LIKE N'https://rms-storage.s3.%' AND jp.PostingTitle = N'Chuyên Viên Phát Triển Kinh Doanh B2B (Software Solutions)';

COMMIT TRANSACTION;
PRINT N'>>> HOÀN TẤT: đã nạp seed bổ sung (tin tuyển dụng + phễu ứng viên).';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH
GO
SET NOEXEC OFF;
GO

-- Kiểm tra nhanh
SELECT ApprovalStatus, COUNT(*) AS Total FROM JobRequisition GROUP BY ApprovalStatus;
SELECT PostingStatus, CASE WHEN ApplicationDeadline < SYSDATETIME() THEN N'Hết hạn' ELSE N'Còn hạn' END AS Deadline, COUNT(*) AS Total
FROM JobPosting GROUP BY PostingStatus, CASE WHEN ApplicationDeadline < SYSDATETIME() THEN N'Hết hạn' ELSE N'Còn hạn' END;
SELECT COUNT(*) AS CvConLinkS3Gia FROM Application WHERE AppliedCvUrl LIKE N'https://rms-storage.s3.%';
GO

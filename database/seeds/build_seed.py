import datetime
import random
import os

# Set deterministic seed in python so generated data is reproducible and high quality
random.seed(42)

OUT_FILE = os.path.join(os.path.dirname(__file__), "seed_data.sql")

ho_list = [
    "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ",
    "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý", "Đoàn", "Đinh", "Trịnh"
]

dem_nam = ["Văn", "Minh", "Tuấn", "Đức", "Hoàng", "Quốc", "Đình", "Hữu", "Thành", "Thế", "Trọng", "Duy", "Tiến", "Khắc"]
ten_nam = [
    "An", "Bình", "Cường", "Dũng", "Đạt", "Đức", "Hải", "Hiếu", "Hoàng", "Hùng", "Huy",
    "Khánh", "Khoa", "Long", "Minh", "Nam", "Nghĩa", "Phong", "Phúc", "Quân", "Quang",
    "Sơn", "Tài", "Thắng", "Thịnh", "Tiến", "Toàn", "Trung", "Tuấn", "Tùng", "Việt", "Vinh", "Vũ"
]

dem_nu = ["Thị", "Mai", "Phương", "Ngọc", "Thu", "Thảo", "Thanh", "Diệu", "Khánh", "Mỹ", "Quỳnh", "Ánh", "Hải"]
ten_nu = [
    "Anh", "Châu", "Chi", "Diệp", "Dung", "Duyên", "Giang", "Hà", "Hân", "Hằng", "Hạnh",
    "Hoa", "Hương", "Huyền", "Lan", "Linh", "Ly", "Mai", "My", "Nga", "Ngân", "Ngọc",
    "Nhung", "Oanh", "Phương", "Quyên", "Quỳnh", "Thảo", "Thu", "Trang", "Trâm", "Trinh", "Uyên", "Vân", "Vy", "Yến"
]

addresses_hn = [
    "Số 15 Duy Tân, Dịch Vọng Hậu, Cầu Giấy, Hà Nội",
    "Số 48 Tố Hữu, Trung Văn, Nam Từ Liêm, Hà Nội",
    "Số 102 Nguyễn Trãi, Thanh Xuân, Hà Nội",
    "Số 25 Vũ Phạm Hàm, Yên Hòa, Cầu Giấy, Hà Nội",
    "Số 88 Láng Hạ, Đống Đa, Hà Nội",
    "Số 14 Phố Huế, Hàng Bài, Hoàn Kiếm, Hà Nội",
    "Số 68 Hoàng Quốc Việt, Nghĩa Đô, Cầu Giấy, Hà Nội",
    "Số 120 Kim Mã, Ba Đình, Hà Nội",
    "Số 35 Lê Văn Lương, Nhân Chính, Thanh Xuân, Hà Nội",
    "Số 50 Trần Phú, Văn Quán, Hà Đông, Hà Nội",
    "Số 77 Xuân Thủy, Cầu Giấy, Hà Nội",
    "Số 91 Chùa Bộc, Quang Trung, Đống Đa, Hà Nội",
    "Số 234 Phạm Văn Đồng, Cổ Nhuế, Bắc Từ Liêm, Hà Nội",
    "Số 16 Đại Cồ Việt, Bách Khoa, Hai Bà Trưng, Hà Nội"
]

addresses_hcm = [
    "Số 12 Lê Thánh Tôn, Bến Nghé, Quận 1, TP. Hồ Chí Minh",
    "Số 45 Nguyễn Thị Minh Khai, Đa Kao, Quận 1, TP. Hồ Chí Minh",
    "Số 88 Điện Biên Phủ, Phường 15, Bình Thạnh, TP. Hồ Chí Minh",
    "Số 120 Cách Mạng Tháng 8, Phường 7, Quận 3, TP. Hồ Chí Minh",
    "Số 30 Quang Trung, Phường 10, Gò Vấp, TP. Hồ Chí Minh",
    "Số 15 Võ Văn Ngân, Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh",
    "Số 210 Nguyễn Văn Linh, Tân Thuận Tây, Quận 7, TP. Hồ Chí Minh",
    "Số 56 Hoàng Văn Thụ, Phường 4, Tân Bình, TP. Hồ Chí Minh",
    "Số 72 Ba Tháng Hai, Phường 12, Quận 10, TP. Hồ Chí Minh"
]

sources = ["TopCV", "VietnamWorks", "LinkedIn", "ITViec", "Employee Referral"]
email_domains = ["gmail.com", "gmail.com", "gmail.com", "fpt.edu.vn", "hust.edu.vn", "vnu.edu.vn", "outlook.com"]

# Helper to remove accents for email/username
def remove_accents(input_str):
    s1 = "àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ"
    s0 = "aaaaaaaaaaaaaaaaaeeeeeeeeeeeiiiiiooooooooooooooooouuuuuuuuuuuyyyyydAAAAAAAAAAAAAAAAAEEEEEEEEEEEIIIIIOOOOOOOOOOOOOOOOOUUUUUUUUUUUYYYYYD"
    res = ""
    for ch in input_str:
        idx = s1.find(ch)
        if idx >= 0:
            res += s0[idx]
        else:
            res += ch
    return res

lines = []

# Header
lines.append("-- =============================================================================")
lines.append("-- RECRUITMENT MANAGEMENT SYSTEM (RMS) - PRODUCTION SEED DATA")
lines.append("-- Target Database : RitirementManagement2")
lines.append("-- DBMS            : Microsoft SQL Server (T-SQL)")
lines.append("-- Standard        : 100% Pure Explicit INSERTs (Zero-Loop, Anti-Laziness Protocol)")
lines.append("-- Tables Covered  : 20/20 PascalCase Tables")
lines.append("-- Funnel Scope    : 77 Days (2026-09-30 to 2026-12-15), 3-4 Applications/Day")
lines.append("-- =============================================================================")
lines.append("USE RitirementManagement2;")
lines.append("GO")
lines.append("")
lines.append("SET NOCOUNT ON;")
lines.append("GO")
lines.append("")
lines.append("BEGIN TRANSACTION;")
lines.append("BEGIN TRY")
lines.append("")

# 1. ROLE
lines.append("-- =============================================================================")
lines.append("-- 1. ROLE (6 System Roles)")
lines.append("-- =============================================================================")
roles = [
    ("System Admin", "Quản trị viên toàn quyền cấu hình và vận hành hệ thống"),
    ("HR", "Chuyên viên tuyển dụng phụ trách quản lý hồ sơ và quy trình ứng tuyển"),
    ("Hiring Manager", "Trưởng bộ phận chuyên môn duyệt yêu cầu và phỏng vấn chuyên sâu"),
    ("Director", "Giám đốc phê duyệt phiếu yêu cầu tuyển dụng và đề xuất đãi ngộ"),
    ("Interviewer", "Thành viên hội đồng đánh giá chuyên môn phỏng vấn ứng viên"),
    ("Candidate", "Ứng viên tham gia nộp hồ sơ và ứng tuyển vào doanh nghiệp")
]
for r_name, r_desc in roles:
    lines.append(f"INSERT INTO [Role] (RoleName, [Description]) VALUES (N'{r_name}', N'{r_desc}');")
lines.append("")

# 2. DEPARTMENT
lines.append("-- =============================================================================")
lines.append("-- 2. DEPARTMENT (3 SME Departments)")
lines.append("-- =============================================================================")
depts = ["Engineering", "Sales & Marketing", "Human Resources"]
for d in depts:
    lines.append(f"INSERT INTO Department (DepartmentName, ManagerId, DepartmentStatus) VALUES (N'{d}', NULL, N'Active');")
lines.append("")

# 3. INTERNAL USERS
lines.append("-- =============================================================================")
lines.append("-- 3. INTERNAL USERS (10 Accounts: Admin, Director, 2 HR, 2 HM, 4 Interviewers)")
lines.append("-- Password: '12345678' -> BCrypt: $2a$10$p.RWJeHxArmCbQBg3GNXEew66veoolEXULwkrjzeoof8Me1dAD8PO")
lines.append("-- =============================================================================")
bcrypt_hash = "$2a$10$p.RWJeHxArmCbQBg3GNXEew66veoolEXULwkrjzeoof8Me1dAD8PO"

internal_users = [
    # (RoleId, Username, Email, FullName, Phone, DeptId, CreatedAt)
    (1, "admin", "admin@rms-tech.vn", "Trần Minh Đức", "0901234567", 1, "2026-08-01 08:00:00"),
    (4, "director", "hoang.director@rms-tech.vn", "Nguyễn Thế Hoàng", "0902345678", 3, "2026-08-01 08:30:00"),
    (2, "hr_lan", "lan.le@rms-tech.vn", "Lê Thị Mai Lan", "0903456789", 3, "2026-08-05 09:00:00"),
    (2, "hr_quang", "quang.pham@rms-tech.vn", "Phạm Quang Huy", "0904567890", 3, "2026-08-05 09:15:00"),
    (3, "hm_tuan", "tuan.vu@rms-tech.vn", "Vũ Anh Tuấn", "0905678901", 1, "2026-08-10 10:00:00"),
    (3, "hm_huong", "huong.do@rms-tech.vn", "Đỗ Thu Hương", "0906789012", 2, "2026-08-10 10:30:00"),
    (5, "dev_nam", "nam.hoang@rms-tech.vn", "Hoàng Nhật Nam", "0907890123", 1, "2026-08-15 08:45:00"),
    (5, "dev_viet", "viet.bui@rms-tech.vn", "Bùi Quốc Việt", "0908901234", 1, "2026-08-15 09:00:00"),
    (5, "qa_thao", "thao.ngo@rms-tech.vn", "Ngô Phương Thảo", "0909012345", 1, "2026-08-15 09:30:00"),
    (5, "sales_trung", "trung.dang@rms-tech.vn", "Đặng Thành Trung", "0909123456", 2, "2026-08-15 10:00:00")
]

for role_id, uname, email, fname, phone, dept_id, created_at in internal_users:
    avatar = f"https://ui-avatars.com/api/?name={fname.replace(' ', '+')}&background=0D8ABC&color=fff"
    lines.append(
        f"INSERT INTO [User] (RoleId, Username, PasswordHash, Email, FullName, PhoneNumber, AvatarUrl, DepartmentId, AccountStatus, CreatedAt) "
        f"VALUES ({role_id}, N'{uname}', N'{bcrypt_hash}', N'{email}', N'{fname}', N'{phone}', N'{avatar}', {dept_id}, N'Active', '{created_at}');"
    )

lines.append("")
lines.append("-- Update Department Managers (Engineering -> hm_tuan (5), Sales -> hm_huong (6), HR -> director (2))")
lines.append("UPDATE Department SET ManagerId = 5 WHERE DepartmentId = 1;")
lines.append("UPDATE Department SET ManagerId = 6 WHERE DepartmentId = 2;")
lines.append("UPDATE Department SET ManagerId = 2 WHERE DepartmentId = 3;")
lines.append("")

# 4. SYSTEM CONFIG
lines.append("-- =============================================================================")
lines.append("-- 4. SYSTEM CONFIG (4 SME System Configurations)")
lines.append("-- =============================================================================")
configs = [
    ("AI_SCREENING_ENDPOINT", "https://api.rms-ai.internal/v1/cv-match", "Endpoint dịch vụ AI sàng lọc hồ sơ CV tự động", "2026-09-01 08:00:00"),
    ("AI_SCORE_THRESHOLD", "70.00", "Ngưỡng điểm AI tối thiểu để hệ thống gợi ý PASS vòng sơ loại hồ sơ", "2026-09-01 08:00:00"),
    ("SMTP_HOST", "smtp.office365.com", "Máy chủ mail gateway gửi email tự động cho ứng viên", "2026-09-01 08:00:00"),
    ("SME_COMPANY_NAME", "RMS Technology Solutions Vietnam", "Tên doanh nghiệp công nghệ SME triển khai hệ thống RMS", "2026-09-01 08:00:00")
]
for k, v, desc, u_at in configs:
    lines.append(f"INSERT INTO SystemConfig (ConfigKey, ConfigValue, [Description], UpdatedAt) VALUES (N'{k}', N'{v}', N'{desc}', '{u_at}');")
lines.append("")

# 5. AUDIT LOG (15 logs before Sep 30, 2026)
lines.append("-- =============================================================================")
lines.append("-- 5. AUDIT LOG (15 SME System Activity Logs Prior to 2026-09-30)")
lines.append("-- =============================================================================")
audit_logs = [
    (1, "LOGIN", "User", "1", None, "User logged in successfully", "192.168.1.10", "2026-08-01 08:05:00"),
    (1, "CREATE", "Department", "1", None, "Created Department Engineering", "192.168.1.10", "2026-08-01 08:10:00"),
    (1, "CREATE", "Department", "2", None, "Created Department Sales & Marketing", "192.168.1.10", "2026-08-01 08:12:00"),
    (1, "CREATE", "Department", "3", None, "Created Department Human Resources", "192.168.1.10", "2026-08-01 08:15:00"),
    (1, "CREATE", "User", "2", None, "Created director account", "192.168.1.10", "2026-08-01 08:35:00"),
    (2, "LOGIN", "User", "2", None, "Director initial login", "192.168.1.15", "2026-08-02 09:00:00"),
    (1, "CREATE", "SystemConfig", "AI_SCREENING_ENDPOINT", None, "Initialized AI endpoint", "192.168.1.10", "2026-09-01 08:05:00"),
    (1, "CREATE", "SystemConfig", "AI_SCORE_THRESHOLD", None, "Set threshold 70.00", "192.168.1.10", "2026-09-01 08:10:00"),
    (5, "CREATE", "JobRequisition", "1", None, "Drafted Senior Java Engineer requisition", "192.168.1.25", "2026-09-10 09:15:00"),
    (5, "CREATE", "JobRequisition", "2", None, "Drafted Frontend React requisition", "192.168.1.25", "2026-09-11 10:20:00"),
    (5, "CREATE", "JobRequisition", "3", None, "Drafted QA Automation requisition", "192.168.1.25", "2026-09-12 08:45:00"),
    (6, "CREATE", "JobRequisition", "4", None, "Drafted B2B Sales requisition", "192.168.1.30", "2026-09-15 14:10:00"),
    (2, "UPDATE", "JobRequisition", "1", "Pending_Director", "Approved by Director", "192.168.1.15", "2026-09-15 10:00:00"),
    (3, "CREATE", "JobPosting", "1", None, "Published Senior Java Engineer posting", "192.168.1.20", "2026-09-25 08:05:00"),
    (3, "CREATE", "JobPosting", "2", None, "Published Frontend React posting", "192.168.1.20", "2026-09-25 08:35:00")
]

for u_id, act, ent_name, ent_id, old_v, new_v, ip, ts in audit_logs:
    old_str = f"N'{old_v}'" if old_v else "NULL"
    lines.append(
        f"INSERT INTO AuditLog (UserId, [Action], EntityName, EntityId, OldValue, NewValue, IpAddress, [Timestamp]) "
        f"VALUES ({u_id}, N'{act}', N'{ent_name}', N'{ent_id}', {old_str}, N'{new_v}', N'{ip}', '{ts}');"
    )
lines.append("")

# 6. JOB REQUISITION (4 Approved positions)
lines.append("-- =============================================================================")
lines.append("-- 6. JOB REQUISITION (4 SME Requisitions: Java, React, QA, Business Dev)")
lines.append("-- =============================================================================")
requisitions = [
    (
        "Senior Java Backend Engineer", 1, 5, 2, "Full-time", 30000000.00, 45000000.00,
        "Mở rộng đội ngũ kỹ thuật phát triển hệ thống Core RMS & Microservices",
        "Phát triển backend RESTful APIs bằng Spring Boot, tối ưu hóa cơ sở dữ liệu SQL Server, triển khai Docker/Kubernetes.",
        "Tối thiểu 3 năm kinh nghiệm lập trình Java, thành thạo Spring Boot, JPA/Hibernate, hiểu sâu về Caching Redis và Message Queue RabbitMQ.",
        "Any", "2 tháng", "Hybrid", "Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội", "2026-11-01",
        "Approved", "2026-09-10 09:00:00", "2026-09-15 10:00:00"
    ),
    (
        "Frontend ReactJS Developer", 1, 5, 2, "Full-time", 20000000.00, 32000000.00,
        "Xây dựng giao diện web portal quản trị và cổng ứng viên hiện đại, responsive",
        "Phát triển module giao diện người dùng bằng React 18, TypeScript, TailwindCSS/Vanilla CSS, tích hợp REST APIs.",
        "Tối thiểu 2 năm kinh nghiệm ReactJS, thành thạo Redux Toolkit hoặc Zustand, có kinh nghiệm tối ưu hóa hiệu năng web và UI/UX.",
        "Any", "2 tháng", "Hybrid", "Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội", "2026-11-01",
        "Approved", "2026-09-11 10:00:00", "2026-09-16 11:00:00"
    ),
    (
        "QA Automation Engineer", 1, 5, 1, "Full-time", 18000000.00, 28000000.00,
        "Thiết lập quy trình kiểm thử tự động, đảm bảo chất lượng hệ thống phần mềm trước release",
        "Xây dựng test automation framework cho API và Web UI bằng Playwright/Selenium và Java/Python, tích hợp CI/CD.",
        "Tối thiểu 2 năm kinh nghiệm Automation Test, thành thạo Postman/Newman, k6 hoặc JMeter, hiểu biết về quy trình Agile/Scrum.",
        "Any", "2 tháng", "On-site", "Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội", "2026-11-15",
        "Approved", "2026-09-12 08:30:00", "2026-09-17 14:00:00"
    ),
    (
        "Business Development Executive (B2B)", 2, 6, 2, "Full-time", 15000000.00, 25000000.00,
        "Mở rộng thị trường khách hàng doanh nghiệp SME sử dụng giải pháp phần mềm",
        "Tìm kiếm khách hàng tiềm năng B2B, tư vấn giải pháp chuyển đổi số, đàm phán và ký kết hợp đồng dịch vụ phần mềm.",
        "Tối thiểu 1 năm kinh nghiệm Sales B2B khối phần mềm/IT, kỹ năng giao tiếp và thuyết trình xuất sắc, tiếng Anh giao tiếp tốt.",
        "Any", "2 tháng", "On-site", "Quận 1, TP. Hồ Chí Minh & Cầu Giấy, Hà Nội", "2026-11-15",
        "Approved", "2026-09-15 14:00:00", "2026-09-18 09:30:00"
    )
]

for title, d_id, hm_id, num_pos, emp_type, min_sal, max_sal, reason, jd, req_det, req_gender, prob_duration, work_model, work_loc, exp_start, app_status, cr_at, up_at in requisitions:
    lines.append(
        f"INSERT INTO JobRequisition (Title, DepartmentId, HiringManagerId, NumberOfPositions, EmploymentType, MinSalary, MaxSalary, ReasonForHiring, JobDescription, RequirementDetails, RequiredGender, ProbationDuration, WorkModel, WorkLocation, ExpectedStartDate, ApprovalStatus, CreatedAt, UpdatedAt) "
        f"VALUES (N'{title}', {d_id}, {hm_id}, {num_pos}, N'{emp_type}', {min_sal:.2f}, {max_sal:.2f}, N'{reason}', N'{jd}', N'{req_det}', N'{req_gender}', N'{prob_duration}', N'{work_model}', N'{work_loc}', '{exp_start}', N'{app_status}', '{cr_at}', '{up_at}');"
    )
lines.append("")

# 7. REQUISITION APPROVAL
lines.append("-- =============================================================================")
lines.append("-- 7. REQUISITION APPROVAL (Director Approvals for 4 Requisitions)")
lines.append("-- =============================================================================")
req_approvals = [
    (1, 2, "Approved", "Đã rà soát ngân sách quý 4/2026. Đồng ý phê duyệt tuyển 2 kỹ sư Java Backend.", "2026-09-15 10:00:00"),
    (2, 2, "Approved", "Phê duyệt tuyển 2 vị trí Frontend ReactJS phục vụ kế hoạch ra mắt Portal mới.", "2026-09-16 11:00:00"),
    (3, 2, "Approved", "Đồng ý tuyển 1 kỹ sư QA Automation để củng cố quy trình kiểm thử CI/CD.", "2026-09-17 14:00:00"),
    (4, 2, "Approved", "Phê duyệt mở 2 vị trí Business Development mở rộng tệp khách hàng B2B.", "2026-09-18 09:30:00")
]
for req_id, dir_id, st, com, ap_dt in req_approvals:
    lines.append(
        f"INSERT INTO RequisitionApproval (RequisitionId, DirectorId, [Status], Comments, ApprovalDate) "
        f"VALUES ({req_id}, {dir_id}, N'{st}', N'{com}', '{ap_dt}');"
    )
lines.append("")

# 8. SCREENING CRITERIA (3-4 per requisition, total 14)
lines.append("-- =============================================================================")
lines.append("-- 8. SCREENING CRITERIA (Weighted AI Screening Criteria for 4 Positions)")
lines.append("-- =============================================================================")
screening_criteria = [
    # Req 1: Java
    (1, "Trình độ Đại học chuyên ngành CNTT / Phần mềm", "Education", "Đại học", 1.00, 1),
    (1, "Kinh nghiệm Java & Spring Boot", "Experience", ">= 3 năm", 2.00, 1),
    (1, "Kiến thức SQL Server & Database Optimization", "Skill", "SQL Server, Redis", 1.50, 0),
    (1, "Kiến thức Microservices & Docker", "Skill", "Docker, REST API", 1.00, 0),
    # Req 2: React
    (2, "Bằng cử nhân CNTT hoặc chứng chỉ lập trình Frontend uy tín", "Education", "Đại học / Cao đẳng", 1.00, 0),
    (2, "Kinh nghiệm phát triển ReactJS & TypeScript", "Experience", ">= 2 năm", 2.00, 1),
    (2, "Kỹ năng State Management & UI Component Styling", "Skill", "Redux/Zustand, CSS", 1.50, 0),
    # Req 3: QA
    (3, "Bằng cấp chuyên ngành CNTT hoặc Toán tin ứng dụng", "Education", "Đại học", 1.00, 0),
    (3, "Kinh nghiệm Automation Test với Selenium/Playwright", "Experience", ">= 2 năm", 2.00, 1),
    (3, "Kỹ năng kiểm thử API & Performance Testing", "Skill", "Postman, k6, JMeter", 1.50, 0),
    (3, "Cam kết làm việc onsite tại Hà Nội", "Knockout", "Hà Nội", 1.00, 1),
    # Req 4: Sales B2B
    (4, "Tốt nghiệp Cao đẳng/Đại học chuyên ngành QTKD/Kinh tế/CNTT", "Education", "Cao đẳng / Đại học", 1.00, 0),
    (4, "Kinh nghiệm Sales B2B giải pháp phần mềm", "Experience", ">= 1 năm", 2.00, 1),
    (4, "Kỹ năng thuyết trình, đàm phán và giao tiếp tự tin", "Skill", "Giao tiếp, Đàm phán", 1.50, 1)
]
for r_id, c_name, c_type, req_val, weight, is_man in screening_criteria:
    lines.append(
        f"INSERT INTO ScreeningCriteria (RequisitionId, CriteriaName, CriteriaType, RequiredValue, [Weight], IsMandatory) "
        f"VALUES ({r_id}, N'{c_name}', N'{c_type}', N'{req_val}', {weight:.2f}, {is_man});"
    )
lines.append("")

# 9. JOB POSTING (4 Published postings)
lines.append("-- =============================================================================")
lines.append("-- 9. JOB POSTING (4 Published Job Postings Active from 2026-09-25 to 2026-12-31)")
lines.append("-- =============================================================================")
job_postings = [
    (
        1, "Senior Java Backend Engineer (Spring Boot / Microservices)",
        "Chúng tôi tìm kiếm Senior Java Engineer tham gia xây dựng nền tảng quản trị tuyển dụng và nhân sự RMS hiệu năng cao.",
        "- Tối thiểu 3 năm làm việc với Java, Spring Boot, Hibernate\\n- Thành thạo SQL Server, Redis Cache\\n- Có tư duy thiết kế hệ thống Microservices sạch sẽ.",
        "- Mức lương cạnh tranh từ 30 - 45 triệu\\n- Thưởng dự án và tháng lương 13\\n- Bảo hiểm sức khỏe PVI cao cấp\\n- Môi trường làm việc trẻ trung, trang thiết bị Macbook Pro.",
        "30,000,000 - 45,000,000 VND", "Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội",
        "2026-09-25 08:00:00", "2026-12-31 23:59:59", "Published", 3, "2026-09-25 08:00:00"
    ),
    (
        2, "Frontend ReactJS Developer (TypeScript / Next.js)",
        "Gia nhập đội ngũ Frontend để phát triển cổng thông tin quản trị và trải nghiệm ứng viên mượt mà, chuẩn UI/UX.",
        "- Tối thiểu 2 năm kinh nghiệm ReactJS, TypeScript\\n- Khả năng xây dựng Responsive Web và Design System hiện đại\\n- Thành thạo Redux Toolkit hoặc Zustand.",
        "- Thu nhập 20 - 32 triệu\\n- Review lương 2 lần/năm\\n- Du lịch công ty hằng năm\\n- Hỗ trợ chi phí chứng chỉ chuyên môn quốc tế.",
        "20,000,000 - 32,000,000 VND", "Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội",
        "2026-09-25 08:30:00", "2026-12-31 23:59:59", "Published", 3, "2026-09-25 08:30:00"
    ),
    (
        3, "QA Automation Engineer (Playwright / Selenium / API)",
        "Đảm bảo chất lượng các bản phát hành phần mềm bằng hệ thống kiểm thử tự động toàn diện và liên tục.",
        "- Tối thiểu 2 năm kinh nghiệm Automation Test web và API\\n- Nắm vững Playwright hoặc Selenium\\n- Kinh nghiệm CI/CD với GitLab/GitHub Actions.",
        "- Thu nhập 18 - 28 triệu\\n- Phụ cấp ăn trưa, gửi xe\\n- Tham gia các khóa đào tạo nâng cao kỹ năng kiểm thử và bảo mật.",
        "18,000,000 - 28,000,000 VND", "Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội",
        "2026-09-25 09:00:00", "2026-12-31 23:59:59", "Published", 3, "2026-09-25 09:00:00"
    ),
    (
        4, "Chuyên Viên Phát Triển Kinh Doanh B2B (Software Solutions)",
        "Tìm kiếm và phát triển mạng lưới khách hàng doanh nghiệp sử dụng hệ thống phần mềm quản trị RMS.",
        "- Tối thiểu 1 năm kinh nghiệm kinh doanh B2B ngành CNTT / SaaS\\n- Khả năng giao tiếp, thương lượng và chốt deal xuất sắc\\n- Tinh thần chủ động, trách nhiệm cao.",
        "- Lương cứng 15 - 25 triệu + Hoa hồng doanh số hấp dẫn\\n- Thu nhập trung bình 30 - 45 triệu/tháng\\n- Lộ trình thăng tiến rõ ràng lên Trưởng nhóm kinh doanh.",
        "15,000,000 - 25,000,000 VND + Hoa hồng", "Quận 1, TP. Hồ Chí Minh & Cầu Giấy, Hà Nội",
        "2026-09-25 09:30:00", "2026-12-31 23:59:59", "Published", 3, "2026-09-25 09:30:00"
    )
]

for req_id, title, jd, jr, ben, sal_disp, loc, p_date, dead_date, st, cr_by, cr_at in job_postings:
    lines.append(
        f"INSERT INTO JobPosting (RequisitionId, PostingTitle, JobDescription, JobRequirements, Benefits, SalaryDisplay, WorkLocation, PostingDate, ApplicationDeadline, PostingStatus, CreatedBy, CreatedAt) "
        f"VALUES ({req_id}, N'{title}', N'{jd}', N'{jr}', N'{ben}', N'{sal_disp}', N'{loc}', '{p_date}', '{dead_date}', N'{st}', {cr_by}, '{cr_at}');"
    )
lines.append("")

# =============================================================================
# CANDIDATES & APPLICATION FLOW (77 Days: 2026-09-30 to 2026-12-15)
# Every single day has EXACTLY 3 or 4 candidates.
# =============================================================================
lines.append("-- =============================================================================")
lines.append("-- 10. CANDIDATES & APPLICATIONS FLOW (2026-09-30 TO 2026-12-15 = 77 DAYS)")
lines.append("-- STRICT PROTOCOL: Exactly 3 or 4 applications per day, fully written out.")
lines.append("-- =============================================================================")

start_date = datetime.date(2026, 9, 30)
end_date = datetime.date(2026, 12, 15)
num_days = (end_date - start_date).days + 1  # 77 days

# Pre-determine apps per day: 3 or 4
# Let's create an alternating/realistic sequence of 3s and 4s
daily_counts = []
for i in range(num_days):
    # Weekday vs weekend realistic variation: 4 on busy days, 3 on others
    day_obj = start_date + datetime.timedelta(days=i)
    if day_obj.weekday() in [1, 2, 3]:  # Tue, Wed, Thu usually have more applications
        daily_counts.append(4)
    else:
        daily_counts.append(3)

total_candidates = sum(daily_counts)
print(f"Total days: {num_days}, Total candidates to generate: {total_candidates}")

# We will collect downstream records and output them by logical day or batches
# Let's generate all candidate metadata first
all_candidates_data = []

user_id_counter = 11  # Internal users take 1..10
candidate_id_counter = 1
app_id_counter = 1

used_usernames = set()
used_emails = set()

for day_idx in range(num_days):
    curr_date = start_date + datetime.timedelta(days=day_idx)
    date_str = curr_date.strftime("%Y-%m-%d")
    count_for_today = daily_counts[day_idx]
    
    for c_idx in range(count_for_today):
        # Gender
        is_female = random.random() < 0.35
        ho = random.choice(ho_list)
        if is_female:
            dem = random.choice(dem_nu)
            ten = random.choice(ten_nu)
            gender_vn = "Nữ"
        else:
            dem = random.choice(dem_nam)
            ten = random.choice(ten_nam)
            gender_vn = "Nam"
        
        full_name = f"{ho} {dem} {ten}"
        
        # Username & Email
        ten_raw = remove_accents(ten).lower()
        ho_raw = remove_accents(ho).lower()
        dem_raw = remove_accents(dem).lower()
        
        uname_base = f"{ten_raw}.{ho_raw}"
        uname = uname_base
        u_suffix = 1
        while uname in used_usernames:
            uname = f"{uname_base}{u_suffix}"
            u_suffix += 1
        used_usernames.add(uname)
        
        domain = random.choice(email_domains)
        email_base = f"{ten_raw}.{dem_raw}.{ho_raw}"
        email = f"{email_base}@{domain}"
        e_suffix = 1
        while email in used_emails:
            email = f"{email_base}{e_suffix}@{domain}"
            e_suffix += 1
        used_emails.add(email)
        
        # Phone
        phone_prefixes = ["090", "091", "093", "097", "098", "086", "088", "038", "039", "070"]
        phone = f"{random.choice(phone_prefixes)}{random.randint(1000000, 9999999)}"
        
        # DOB
        birth_year = random.randint(1993, 2002)
        birth_month = random.randint(1, 12)
        birth_day = random.randint(1, 28)
        dob_str = f"{birth_year:04d}-{birth_month:02d}-{birth_day:02d}"
        
        # Address
        if random.random() < 0.75:
            addr = random.choice(addresses_hn)
        else:
            addr = random.choice(addresses_hcm)
            
        # Time of application (between 08:15 and 21:45)
        hour = random.randint(8, 21)
        minute = random.randint(0, 59)
        second = random.randint(0, 59)
        sub_time_str = f"{date_str} {hour:02d}:{minute:02d}:{second:02d}"
        
        # Source & JobPosting
        source = random.choice(sources)
        job_posting_id = random.choice([1, 1, 2, 2, 3, 4])  # Weights towards Dev & React
        
        cv_slug = f"cand_{user_id_counter:03d}_{ten_raw}_{curr_date.strftime('%Y%m%d')}.pdf"
        cv_url = f"https://rms-storage.s3.ap-southeast-1.amazonaws.com/cvs/2026/{curr_date.strftime('%m')}/{cv_slug}"
        
        # Funnel determination
        # 10% Offer (Passed)
        # 20% Interviewing
        # 30% Review (HR/HM Passed or Rejected at review)
        # 40% AI Screened / Applied
        funnel_rand = random.random()
        
        all_candidates_data.append({
            "user_id": user_id_counter,
            "candidate_id": candidate_id_counter,
            "app_id": app_id_counter,
            "full_name": full_name,
            "username": uname,
            "email": email,
            "phone": phone,
            "gender": gender_vn,
            "dob": dob_str,
            "address": addr,
            "source": source,
            "posting_id": job_posting_id,
            "sub_time": sub_time_str,
            "cv_url": cv_url,
            "date_obj": curr_date,
            "funnel_rand": funnel_rand
        })
        
        user_id_counter += 1
        candidate_id_counter += 1
        app_id_counter += 1

# Now generate INSERT statements day by day
# In each day block, we write:
# - User INSERTs for the day's candidates
# - Candidate INSERTs for the day's candidates
# - Application INSERTs for the day's candidates
# - Downstream tables (AIScreeningResult, ApplicationReview, InterviewSchedule, InterviewPanel, InterviewEvaluation, InterviewFinalResult, OfferProposal, OfferApproval, OfferNegotiation)

interview_id_counter = 1
offer_id_counter = 1

for day_idx in range(num_days):
    curr_date = start_date + datetime.timedelta(days=day_idx)
    date_str = curr_date.strftime("%Y-%m-%d")
    day_cands = [c for c in all_candidates_data if c["date_obj"] == curr_date]
    
    lines.append(f"-- -----------------------------------------------------------------------------")
    lines.append(f"-- DAY {day_idx + 1:02d} / {num_days:02d}: {date_str} ({len(day_cands)} Applications)")
    lines.append(f"-- -----------------------------------------------------------------------------")
    
    # 1. User records
    for c in day_cands:
        avatar = f"https://ui-avatars.com/api/?name={c['full_name'].replace(' ', '+')}&background=random"
        lines.append(
            f"INSERT INTO [User] (RoleId, Username, PasswordHash, Email, FullName, PhoneNumber, AvatarUrl, DepartmentId, AccountStatus, CreatedAt) "
            f"VALUES (6, N'{c['username']}', N'{bcrypt_hash}', N'{c['email']}', N'{c['full_name']}', N'{c['phone']}', N'{avatar}', NULL, N'Active', '{c['sub_time']}');"
        )
    
    # 2. Candidate records
    for c in day_cands:
        linkedin = f"https://linkedin.com/in/{c['username'].replace('.', '-')}"
        github = f"https://github.com/{c['username'].replace('.', '')}" if c['posting_id'] in [1, 2, 3] else "NULL"
        gh_sql = f"N'{github}'" if github != "NULL" else "NULL"
        lines.append(
            f"INSERT INTO Candidate (UserId, DateOfBirth, Gender, Address, LinkedInUrl, PortfolioUrl, CandidateSource, CreatedAt) "
            f"VALUES ({c['user_id']}, '{c['dob']}', N'{c['gender']}', N'{c['address']}', N'{linkedin}', {gh_sql}, N'{c['source']}', '{c['sub_time']}');"
        )
        
    # 3. Application records
    for c in day_cands:
        # Determine status based on funnel
        fr = c["funnel_rand"]
        if fr < 0.10: # Stage 4: Offer
            status = "Offered" if c["date_obj"] > datetime.date(2026, 12, 1) else "Hired"
            score = round(random.uniform(86.0, 96.5), 2)
        elif fr < 0.30: # Stage 3: Interviewing
            status = "Interviewing" if c["date_obj"] > datetime.date(2026, 12, 5) else ("HM_Passed" if random.random() < 0.5 else "Rejected")
            score = round(random.uniform(75.0, 88.0), 2)
        elif fr < 0.60: # Stage 2: Review (HR/HM)
            status = "HM_Passed" if random.random() < 0.6 else "HR_Passed"
            score = round(random.uniform(68.0, 82.0), 2)
        else: # Stage 1: AI Screened or Applied
            status = "AI_Screened" if random.random() < 0.85 else "Applied"
            score = round(random.uniform(42.0, 68.0), 2) if status == "AI_Screened" else None
            
        c["computed_status"] = status
        c["computed_score"] = score
        score_sql = f"{score:.2f}" if score is not None else "NULL"
        
        lines.append(
            f"INSERT INTO Application (CandidateId, JobPostingId, AppliedCvUrl, SubmissionDate, ApplicationStatus, OverallScore, CreatedAt) "
            f"VALUES ({c['candidate_id']}, {c['posting_id']}, N'{c['cv_url']}', '{c['sub_time']}', N'{status}', {score_sql}, '{c['sub_time']}');"
        )
        
    # 4. AIScreeningResult (for almost all except pure 'Applied')
    for c in day_cands:
        if c["computed_status"] != "Applied":
            ai_score = c["computed_score"] if c["computed_score"] is not None else round(random.uniform(50.0, 92.0), 2)
            # Screened 5 - 15 minutes after submission
            screen_dt = (datetime.datetime.strptime(c["sub_time"], "%Y-%m-%d %H:%M:%S") + datetime.timedelta(minutes=random.randint(5, 20))).strftime("%Y-%m-%d %H:%M:%S")
            lines.append(
                f"INSERT INTO AIScreeningResult (ApplicationId, AIMatchScore, ScreenedAt) "
                f"VALUES ({c['app_id']}, {ai_score:.2f}, '{screen_dt}');"
            )
            
    # 5. ApplicationReview (for Stage 2, 3, 4)
    for c in day_cands:
        if c["funnel_rand"] < 0.60: # Review stage or further
            hr_rev_dt = (datetime.datetime.strptime(c["sub_time"], "%Y-%m-%d %H:%M:%S") + datetime.timedelta(days=1, hours=random.randint(1, 4))).strftime("%Y-%m-%d %H:%M:%S")
            hr_id = random.choice([3, 4]) # lan.le or quang.pham
            hr_dec = "Pass"
            hr_comment = f"Hồ sơ ứng viên {c['full_name']} đáp ứng tốt tiêu chí cơ bản về học vấn và kinh nghiệm, liên hệ xếp lịch phỏng vấn chuyên môn."
            lines.append(
                f"INSERT INTO ApplicationReview (ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt) "
                f"VALUES ({c['app_id']}, {hr_id}, N'HR', N'{hr_dec}', N'{hr_comment}', '{hr_rev_dt}');"
            )
            
            # HM Review
            hm_id = 6 if c["posting_id"] == 4 else 5 # hm_huong for Sales, hm_tuan for Tech
            hm_rev_dt = (datetime.datetime.strptime(hr_rev_dt, "%Y-%m-%d %H:%M:%S") + datetime.timedelta(days=1, hours=random.randint(2, 5))).strftime("%Y-%m-%d %H:%M:%S")
            hm_dec = "Pass" if c["funnel_rand"] < 0.30 else ("Pass" if random.random() < 0.5 else "Hold")
            hm_comment = f"Đánh giá CV có kinh nghiệm thực chiến phù hợp với yêu cầu vị trí {c['posting_id']}, phê duyệt chuyển sang hội đồng phỏng vấn."
            lines.append(
                f"INSERT INTO ApplicationReview (ApplicationId, ReviewerId, ReviewerRole, Decision, Comments, ReviewedAt) "
                f"VALUES ({c['app_id']}, {hm_id}, N'HiringManager', N'{hm_dec}', N'{hm_comment}', '{hm_rev_dt}');"
            )
            
    # 6. Interview Flow (InterviewSchedule, InterviewPanel, InterviewEvaluation, InterviewFinalResult) (for Stage 3 & 4: ~30%)
    for c in day_cands:
        if c["funnel_rand"] < 0.30:
            int_id = interview_id_counter
            interview_id_counter += 1
            
            # Interview scheduled 3 - 6 days after submission
            sub_dt = datetime.datetime.strptime(c["sub_time"], "%Y-%m-%d %H:%M:%S")
            int_date = sub_dt + datetime.timedelta(days=random.randint(3, 6))
            start_hour = random.choice([9, 10, 14, 15, 16])
            int_start_dt = int_date.replace(hour=start_hour, minute=0, second=0)
            int_end_dt = int_date.replace(hour=start_hour + 1, minute=0, second=0)
            
            start_str = int_start_dt.strftime("%Y-%m-%d %H:%M:%S")
            end_str = int_end_dt.strftime("%Y-%m-%d %H:%M:%S")
            
            fmt = "Offline_Office" if random.random() < 0.6 else "Online_GoogleMeet"
            loc = "Phòng họp 802, Tầng 8 Tòa nhà RMS Tower" if fmt == "Offline_Office" else f"https://meet.google.com/rms-int-{int_id:04d}"
            int_status = "Completed" if int_end_dt < datetime.datetime(2026, 12, 16) else "Scheduled"
            
            lines.append(
                f"INSERT INTO InterviewSchedule (ApplicationId, InterviewFormat, StartTime, EndTime, LocationOrLink, InterviewStatus, CreatedBy, CreatedAt) "
                f"VALUES ({c['app_id']}, N'{fmt}', '{start_str}', '{end_str}', N'{loc}', N'{int_status}', 3, '{c['sub_time']}');"
            )
            
            # InterviewPanel (Composite PK: InterviewId, InterviewerId)
            # RoleInPanel CHECK: IN (N'HR', N'HM')
            hr_interviewer = random.choice([3, 4])
            lines.append(
                f"INSERT INTO InterviewPanel (InterviewId, InterviewerId, RoleInPanel) "
                f"VALUES ({int_id}, {hr_interviewer}, N'HR');"
            )
            
            if c["posting_id"] == 4:
                tech_interviewer = 6 # hm_huong
            elif c["posting_id"] == 1:
                tech_interviewer = random.choice([5, 7, 8]) # hm_tuan, dev_nam, dev_viet
            elif c["posting_id"] == 2:
                tech_interviewer = random.choice([5, 8])
            else:
                tech_interviewer = 9 # qa_thao
                
            lines.append(
                f"INSERT INTO InterviewPanel (InterviewId, InterviewerId, RoleInPanel) "
                f"VALUES ({int_id}, {tech_interviewer}, N'HM');"
            )
            
            # InterviewEvaluation
            if int_status == "Completed":
                eval_time = (int_end_dt + datetime.timedelta(minutes=30)).strftime("%Y-%m-%d %H:%M:%S")
                is_passed = c["funnel_rand"] < 0.10
                
                # Eval 1: HR
                hr_rec = "Hire" if is_passed else ("Consider" if random.random() < 0.5 else "No_Hire")
                hr_tech = random.randint(3, 5) if is_passed else random.randint(2, 4)
                hr_soft = 5 if is_passed else random.randint(2, 4)
                hr_cult = 5 if is_passed else random.randint(3, 4)
                lines.append(
                    f"INSERT INTO InterviewEvaluation (InterviewId, InterviewerId, TechnicalScore, SoftSkillScore, CulturalFitScore, Strengths, Weaknesses, Recommendation, Comments, EvaluatedAt) "
                    f"VALUES ({int_id}, {hr_interviewer}, {hr_tech}, {hr_soft}, {hr_cult}, N'Tác phong tự tin, giao tiếp mạch lạc, gắn bó lâu dài', N'Cần tìm hiểu thêm về quy trình Agile nội bộ', N'{hr_rec}', N'Đánh giá ứng viên phù hợp với văn hóa SME công ty', '{eval_time}');"
                )
                
                # Eval 2: Tech / HM
                tech_rec = "Hire" if is_passed else ("No_Hire" if random.random() < 0.6 else "Consider")
                tech_score = random.randint(4, 5) if is_passed else random.randint(2, 3)
                lines.append(
                    f"INSERT INTO InterviewEvaluation (InterviewId, InterviewerId, TechnicalScore, SoftSkillScore, CulturalFitScore, Strengths, Weaknesses, Recommendation, Comments, EvaluatedAt) "
                    f"VALUES ({int_id}, {tech_interviewer}, {tech_score}, {hr_soft}, {hr_cult}, N'Nắm vững chuyên môn, giải quyết bài toán thuật toán/case study tốt', N'Cần đào tạo thêm về cloud architecture thực tế', N'{tech_rec}', N'Đồng thuận tiếp nhận ứng viên vào dự án', '{eval_time}');"
                )
                
                # InterviewFinalResult (1-1 with InterviewSchedule)
                final_decision = "Passed" if is_passed else "Failed"
                hm_final_id = 6 if c["posting_id"] == 4 else 5
                final_comments = f"Tổng hợp kết quả phỏng vấn ứng viên {c['full_name']}: Đạt yêu cầu đầu vào, đề xuất gửi thư mời nhận việc (Offer)." if is_passed else f"Ứng viên {c['full_name']} chưa đạt yêu cầu về chiều sâu chuyên môn ở vòng phỏng vấn kỹ thuật."
                
                pos_titles = {
                    1: "Senior Java Backend Engineer",
                    2: "Frontend ReactJS Developer",
                    3: "QA Automation Engineer",
                    4: "Business Development Executive"
                }
                base_salaries = {
                    1: (36000000.00, 32000000.00), # proposed, probation (32M / 36M = 88.8% >= 85%)
                    2: (26000000.00, 23000000.00), # 23M / 26M = 88.4% >= 85%
                    3: (22000000.00, 19500000.00), # 19.5M / 22M = 88.6% >= 85%
                    4: (20000000.00, 17500000.00)  # 17.5M / 20M = 87.5% >= 85%
                }
                prop_sal, prob_sal = base_salaries[c["posting_id"]]
                rec_sal_sql = f"{prop_sal:.2f}" if is_passed else "NULL"

                lines.append(
                    f"INSERT INTO InterviewFinalResult (InterviewId, HiringManagerId, FinalDecision, RecommendedSalary, FinalSummaryComments, ApprovedAt) "
                    f"VALUES ({int_id}, {hm_final_id}, N'{final_decision}', {rec_sal_sql}, N'{final_comments}', '{eval_time}');"
                )
                
                # 7. Offer Proposal, Approval, Negotiation (for ~10% Passed)
                if is_passed:
                    off_id = offer_id_counter
                    offer_id_counter += 1
                    off_title = pos_titles[c["posting_id"]]
                    
                    # Start date 2-3 weeks after interview
                    exp_start_date = (int_date + datetime.timedelta(days=random.randint(14, 21))).strftime("%Y-%m-%d")
                    offer_created_dt = (int_end_dt + datetime.timedelta(days=1)).strftime("%Y-%m-%d %H:%M:%S")
                    
                    off_status = random.choice(["Accepted", "Accepted", "Negotiating", "Director_Approved", "Sent_Candidate", "Declined"])
                    lines.append(
                        f"INSERT INTO OfferProposal (ApplicationId, OfferedPositionTitle, ProposedSalary, ProbationSalary, ExpectedStartDate, WorkLocation, BenefitsPackage, OfferStatus, ProposedBy, CreatedAt) "
                        f"VALUES ({c['app_id']}, N'{off_title}', {prop_sal:.2f}, {prob_sal:.2f}, '{exp_start_date}', N'Tầng 8, Tòa nhà RMS Tower, Duy Tân, Cầu Giấy, Hà Nội', N'Bảo hiểm PVI, 14 ngày phép năm, thưởng dự án, xét lương 2 lần/năm', N'{off_status}', 3, '{offer_created_dt}');"
                    )
                    
                    # OfferApproval (DirectorId = 2)
                    off_app_dt = (datetime.datetime.strptime(offer_created_dt, "%Y-%m-%d %H:%M:%S") + datetime.timedelta(hours=6)).strftime("%Y-%m-%d %H:%M:%S")
                    lines.append(
                        f"INSERT INTO OfferApproval (OfferId, DirectorId, [Status], DirectorComments, ApprovedAt) "
                        f"VALUES ({off_id}, 2, N'Approved', N'Phê duyệt mức đãi ngộ theo đề xuất của HR và HM.', '{off_app_dt}');"
                    )
                    
                    # OfferNegotiation (if Negotiating or some Accepted/Declined)
                    if off_status in ["Negotiating", "Accepted", "Declined"]:
                        neg_dt = (datetime.datetime.strptime(off_app_dt, "%Y-%m-%d %H:%M:%S") + datetime.timedelta(days=1)).strftime("%Y-%m-%d %H:%M:%S")
                        if off_status == "Declined":
                            counter_sal = prop_sal + 5000000.00
                            c_notes = "Ứng viên nhận được offer khác với mức đãi ngộ cao hơn và mong muốn trao đổi lại."
                            hr_notes = "HR đã trao đổi nhưng ngân sách vị trí hiện tại không thể đáp ứng, bảo lưu hồ sơ ứng viên."
                        else:
                            counter_sal = prop_sal + 2000000.00
                            c_notes = "Ứng viên mong muốn hỗ trợ thêm 2 triệu phụ cấp đi lại hoặc chứng chỉ chuyên môn."
                            hr_notes = "HR trao đổi và thống nhất hỗ trợ phụ cấp đào tạo chứng chỉ hàng năm."
                        lines.append(
                            f"INSERT INTO OfferNegotiation (OfferId, CandidateCounterSalary, CandidateNotes, HRResponseNotes, NegotiationDate) "
                            f"VALUES ({off_id}, {counter_sal:.2f}, N'{c_notes}', N'{hr_notes}', '{neg_dt}');"
                        )
                        
    lines.append("")

lines.append("COMMIT TRANSACTION;")
lines.append("PRINT N'>>> DỮ LIỆU ĐÃ ĐƯỢC COMMIT THÀNH CÔNG VÀO CƠ SỞ DỮ LIỆU!';")
lines.append("END TRY")
lines.append("BEGIN CATCH")
lines.append("    IF @@TRANCOUNT > 0")
lines.append("        ROLLBACK TRANSACTION;")
lines.append("    DECLARE @ErrMsg NVARCHAR(4000) = ERROR_MESSAGE();")
lines.append("    DECLARE @ErrSev INT = ERROR_SEVERITY();")
lines.append("    DECLARE @ErrState INT = ERROR_STATE();")
lines.append("    RAISERROR(@ErrMsg, @ErrSev, @ErrState);")
lines.append("END CATCH;")
lines.append("GO")
lines.append("")

# ASSERTION BLOCK
lines.append("-- =============================================================================")
lines.append("-- 🔍 KHỐI ĐỐI SOÁT & KIỂM TRA CHẤT LƯỢNG TỰ ĐỘNG (ASSERTION BLOCK)")
lines.append("-- =============================================================================")
lines.append("PRINT N'';")
lines.append("PRINT N'=============================================================================';")
lines.append("PRINT N'               BÁO CÁO TỔNG SỐ BẢN GHI THEO TOÀN BỘ 20 BẢNG                   ';")
lines.append("PRINT N'=============================================================================';")
lines.append("SELECT 'Role' AS TableName, COUNT(*) AS TotalRecords FROM [Role]")
lines.append("UNION ALL SELECT 'Department', COUNT(*) FROM Department")
lines.append("UNION ALL SELECT '[User]', COUNT(*) FROM [User]")
lines.append("UNION ALL SELECT 'AuditLog', COUNT(*) FROM AuditLog")
lines.append("UNION ALL SELECT 'SystemConfig', COUNT(*) FROM SystemConfig")
lines.append("UNION ALL SELECT 'JobRequisition', COUNT(*) FROM JobRequisition")
lines.append("UNION ALL SELECT 'RequisitionApproval', COUNT(*) FROM RequisitionApproval")
lines.append("UNION ALL SELECT 'ScreeningCriteria', COUNT(*) FROM ScreeningCriteria")
lines.append("UNION ALL SELECT 'JobPosting', COUNT(*) FROM JobPosting")
lines.append("UNION ALL SELECT 'Candidate', COUNT(*) FROM Candidate")
lines.append("UNION ALL SELECT 'Application', COUNT(*) FROM Application")
lines.append("UNION ALL SELECT 'ApplicationReview', COUNT(*) FROM ApplicationReview")
lines.append("UNION ALL SELECT 'AIScreeningResult', COUNT(*) FROM AIScreeningResult")
lines.append("UNION ALL SELECT 'InterviewSchedule', COUNT(*) FROM InterviewSchedule")
lines.append("UNION ALL SELECT 'InterviewPanel', COUNT(*) FROM InterviewPanel")
lines.append("UNION ALL SELECT 'InterviewEvaluation', COUNT(*) FROM InterviewEvaluation")
lines.append("UNION ALL SELECT 'InterviewFinalResult', COUNT(*) FROM InterviewFinalResult")
lines.append("UNION ALL SELECT 'OfferProposal', COUNT(*) FROM OfferProposal")
lines.append("UNION ALL SELECT 'OfferApproval', COUNT(*) FROM OfferApproval")
lines.append("UNION ALL SELECT 'OfferNegotiation', COUNT(*) FROM OfferNegotiation;")
lines.append("GO")
lines.append("")
lines.append("-- Kiểm tra không có bảng nào trong 20 bảng bị rỗng (COUNT = 0)")
lines.append("DECLARE @EmptyTables INT = 0;")
lines.append("IF (SELECT COUNT(*) FROM [Role]) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM Department) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM [User]) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM AuditLog) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM SystemConfig) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM JobRequisition) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM RequisitionApproval) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM ScreeningCriteria) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM JobPosting) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM Candidate) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM Application) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM ApplicationReview) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM AIScreeningResult) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM InterviewSchedule) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM InterviewPanel) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM InterviewEvaluation) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM InterviewFinalResult) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM OfferProposal) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM OfferApproval) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("IF (SELECT COUNT(*) FROM OfferNegotiation) = 0 SET @EmptyTables = @EmptyTables + 1;")
lines.append("")
lines.append("IF @EmptyTables > 0")
lines.append("    RAISERROR(N'CẢNH BÁO: Phát hiện có bảng chưa có dữ liệu trong 20 bảng!', 16, 1);")
lines.append("ELSE")
lines.append("    PRINT N'>>> XÁC NHẬN HOÀN TOÀN: ĐỦ 20/20 BẢNG ĐỀU ĐÃ ĐƯỢC NẠP DỮ LIỆU CHUẨN MỰC!';")
lines.append("GO")
lines.append("")
lines.append("-- Kiểm tra số lượng đơn ứng tuyển theo từng ngày (Yêu cầu nghiêm ngặt: không dưới 3 và không quá 4 đơn/ngày)")
lines.append("PRINT N'=============================================================================';")
lines.append("PRINT N'   KIỂM TRA DÒNG THỜI GIAN ĐƠN ỨNG TUYỂN (3-4 ĐƠN/NGÀY TỪ 30/09 ĐẾN 15/12)     ';")
lines.append("PRINT N'=============================================================================';")
lines.append("SELECT CAST(SubmissionDate AS DATE) AS ApplyDate, COUNT(*) AS TotalApps")
lines.append("FROM Application")
lines.append("GROUP BY CAST(SubmissionDate AS DATE)")
lines.append("HAVING COUNT(*) < 3 OR COUNT(*) > 4;")
lines.append("GO")
lines.append("")
lines.append("PRINT N'>>> KẾT QUẢ: Nếu bảng trên rỗng (0 rows), toàn bộ 77 ngày đều đạt chuẩn 3-4 đơn/ngày!';")
lines.append("PRINT N'>>> HOÀN TẤT: Script seed_data.sql sẵn sàng khởi chạy trên môi trường thử nghiệm / phát triển!';")
lines.append("GO")

output_content = "\n".join(lines)
with open(OUT_FILE, "w", encoding="utf-8") as f:
    f.write(output_content)

print(f"Generated {len(lines)} lines of SQL successfully written to {OUT_FILE}.")

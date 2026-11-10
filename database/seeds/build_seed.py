"""Sinh database/seeds/seed_data.sql: dữ liệu mẫu phủ mọi trạng thái và luồng mà code hiện tại xử lý.

Chạy:  python database/seeds/build_seed.py
Mọi mốc thời gian trong file SQL tính theo lúc chạy seed (@D0 = 00:00 hôm nay, @Now = SYSDATETIME()),
nên dashboard, lịch phỏng vấn sắp tới, hạn phản hồi offer... luôn "mới" mà không cần seed phụ.
ID gán tường minh (IDENTITY_INSERT) để test case tra được: xem bảng ID ở đầu file SQL.
"""
import os
import random

random.seed(2026)
OUT_FILE = os.path.join(os.path.dirname(__file__), "seed_data.sql")
BCRYPT = "$2a$10$p.RWJeHxArmCbQBg3GNXEew66veoolEXULwkrjzeoof8Me1dAD8PO"  # 12345678


def q(v):
    """Giá trị Python -> literal T-SQL."""
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "1" if v else "0"
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, Raw):
        return v.sql
    return "N'" + str(v).replace("'", "''") + "'"


class Raw:
    def __init__(self, sql):
        self.sql = sql


def at(day, hour=9, minute=0):
    """Số phút tính từ 00:00 hôm nay."""
    return day * 1440 + hour * 60 + minute


def T(m):
    return None if m is None else Raw(f"DATEADD(MINUTE, {m}, @D0)")


def ago(minutes):
    """Mốc tính lùi từ lúc chạy (dùng cho dữ liệu 'hôm nay', luôn nằm trong quá khứ)."""
    return Raw(f"DATEADD(MINUTE, -{minutes}, @Now)")


def DAY(d):
    return Raw(f"CAST(DATEADD(DAY, {d}, @D0) AS DATE)")


tables = {}  # tên bảng -> (cột, [dòng])


def add(table, row):
    cols, rows = tables.setdefault(table, (list(row.keys()), []))
    rows.append(row)
    return row


# =============================================================================
# 1. ROLE, DEPARTMENT, USER NỘI BỘ
# =============================================================================
ROLES = [(1, "System Admin", "Quản trị viên toàn quyền cấu hình và vận hành hệ thống"),
         (2, "HR", "Chuyên viên tuyển dụng phụ trách quản lý hồ sơ và quy trình ứng tuyển"),
         (3, "Hiring Manager", "Trưởng bộ phận chuyên môn duyệt yêu cầu và phỏng vấn chuyên sâu"),
         (4, "Director", "Giám đốc phê duyệt phiếu yêu cầu tuyển dụng và đề xuất đãi ngộ"),
         (5, "Interviewer", "Thành viên hội đồng đánh giá chuyên môn phỏng vấn ứng viên"),
         (6, "Candidate", "Ứng viên tham gia nộp hồ sơ và ứng tuyển vào doanh nghiệp")]
for rid, name, desc in ROLES:
    add("[Role]", {"RoleId": rid, "RoleName": name, "[Description]": desc})

DEPTS = [(1, "Kỹ thuật", 5, "Active"), (2, "Kinh doanh & Marketing", 6, "Active"), (3, "Nhân sự", 2, "Active"),
         (4, "Tài chính – Kế toán", 11, "Active"), (5, "Sản phẩm & Thiết kế", None, "Inactive")]
for did, name, _, st in DEPTS:
    add("Department", {"DepartmentId": did, "DepartmentName": name, "ManagerId": None, "DepartmentStatus": st})

ADMIN, DIRECTOR, HR_LAN, HR_QUANG, HM_TUAN, HM_HUONG = 1, 2, 3, 4, 5, 6
IV_NAM, IV_VIET, IV_THAO, IV_TRUNG, HM_MINH, HR_NGOC, IV_HOA = 7, 8, 9, 10, 11, 12, 13
HRS = [HR_LAN, HR_QUANG]
INTERNAL = [
    # id, role, username, email, name, phone, dept, status, created(day)
    (1, 1, "admin", "admin@rms-tech.vn", "Trần Minh Đức", "0901234567", 1, "Active", -120),
    (2, 4, "director", "hoang.director@rms-tech.vn", "Nguyễn Thế Hoàng", "0902345678", 3, "Active", -120),
    (3, 2, "hr_lan", "lan.le@rms-tech.vn", "Lê Thị Mai Lan", "0903456789", 3, "Active", -115),
    (4, 2, "hr_quang", "quang.pham@rms-tech.vn", "Phạm Quang Huy", "0904567890", 3, "Active", -115),
    (5, 3, "hm_tuan", "tuan.vu@rms-tech.vn", "Vũ Anh Tuấn", "0905678901", 1, "Active", -110),
    (6, 3, "hm_huong", "huong.do@rms-tech.vn", "Đỗ Thu Hương", "0906789012", 2, "Active", -110),
    (7, 5, "interviewer_nam", "nam.hoang@rms-tech.vn", "Hoàng Nhật Nam", "0907890123", 1, "Active", -105),
    (8, 5, "interviewer_viet", "viet.bui@rms-tech.vn", "Bùi Quốc Việt", "0908901234", 1, "Active", -105),
    (9, 5, "interviewer_thao", "thao.ngo@rms-tech.vn", "Ngô Phương Thảo", "0909012345", 1, "Active", -105),
    (10, 5, "interviewer_trung", "trung.dang@rms-tech.vn", "Đặng Thành Trung", "0909123456", 2, "Active", -105),
    (11, 3, "hm_minh", "minh.tran@rms-tech.vn", "Trần Đức Minh", "0909234567", 4, "Active", -100),
    (12, 2, "hr_ngoc", "ngoc.vo@rms-tech.vn", "Võ Bảo Ngọc", "0909345678", 3, "Inactive", -100),
    (13, 5, "interviewer_hoa", "hoa.ly@rms-tech.vn", "Lý Thanh Hoa", "0909456789", 4, "Active", -95),
]
USER_NAME = {}
for uid, role, uname, email, name, phone, dept, st, cday in INTERNAL:
    USER_NAME[uid] = name
    add("[User]", {"UserId": uid, "RoleId": role, "Username": uname, "PasswordHash": BCRYPT, "Email": email,
                   "FullName": name, "PhoneNumber": phone, "AvatarUrl": None, "DepartmentId": dept,
                   "AccountStatus": st, "CreatedAt": T(at(cday, 8)),
                   "UpdatedAt": T(at(-20, 10)) if st == "Inactive" else None})

add("SystemConfig", {"ConfigKey": "SME_COMPANY_NAME", "ConfigValue": "RMS Technology Solutions Vietnam",
                     "[Description]": "Tên doanh nghiệp triển khai hệ thống RMS", "UpdatedAt": T(at(-120, 8))})
add("SystemConfig", {"ConfigKey": "SMTP_HOST", "ConfigValue": "smtp.gmail.com",
                     "[Description]": "Máy chủ gửi email tự động cho ứng viên", "UpdatedAt": T(at(-120, 8))})

# =============================================================================
# 2. AUDIT & NOTIFICATION HELPERS
# =============================================================================
def audit(user, action, entity, eid, old, new, when, ip="192.168.1.20"):
    add("AuditLog", {"UserId": user, "[Action]": action, "EntityName": entity, "EntityId": str(eid),
                     "OldValue": old, "NewValue": new, "IpAddress": ip, "[Timestamp]": T(when)})


notified = set()


def notify(recipient, actor, title, content, event_type, ref_id, link, event_id, when):
    if (recipient, event_id) in notified or recipient == actor:
        return
    notified.add((recipient, event_id))
    is_read = when < at(-2)  # cũ hơn 2 ngày thì đã đọc
    add("[Notification]", {"RecipientId": recipient, "ActorId": actor, "Title": title, "Content": content,
                           "EventType": Raw(f"'{event_type}'"), "ReferenceType": None,
                           "ReferenceId": Raw(f"'{ref_id}'"), "LinkUrl": link, "IsRead": is_read,
                           "ReadAt": T(when + 120) if is_read else None,
                           "EventId": Raw(f"'{event_id}'"), "CreatedAt": T(when)})


for uid in (ADMIN, DIRECTOR, HR_LAN, HR_QUANG, HM_TUAN, HM_HUONG, HM_MINH, IV_VIET):
    for d in (-30, -7, -1):
        audit(uid, "LOGIN", "User", uid, None, "Đăng nhập thành công", at(d, 8, 5 + uid))
audit(ADMIN, "CREATE", "Department", 4, None, "Tạo phòng ban: Tài chính – Kế toán", at(-118, 9))
audit(ADMIN, "CREATE", "Department", 5, None, "Tạo phòng ban: Sản phẩm & Thiết kế", at(-118, 9, 5))
audit(ADMIN, "UPDATE", "Department", 5, "Active", "Inactive", at(-25, 14))
audit(ADMIN, "CREATE", "User", HM_MINH, None, "Tạo tài khoản hm_minh (Hiring Manager)", at(-100, 9))
audit(ADMIN, "UPDATE", "User", HR_NGOC, "Active", "Inactive", at(-20, 10))
audit(ADMIN, "UPDATE", "SystemConfig", "SMTP_HOST", "smtp.office365.com", "smtp.gmail.com", at(-60, 15))
audit(ADMIN, "UPDATE", "PolicyDocument", "privacy", None, "Chính sách bảo mật: thay file privacy.pdf (120 KB)", at(-40, 11))

# =============================================================================
# 3. JOB REQUISITION + APPROVAL
# =============================================================================
JD = {
    "java": ("Phát triển RESTful API bằng Spring Boot, tối ưu SQL Server, triển khai Docker.",
             "Tối thiểu 3 năm Java, thành thạo Spring Boot, JPA/Hibernate, Redis, RabbitMQ."),
    "react": ("Phát triển giao diện web bằng React 18, TypeScript, tích hợp REST API.",
              "Tối thiểu 2 năm ReactJS, thành thạo Redux Toolkit hoặc Zustand."),
    "qa": ("Xây dựng framework kiểm thử tự động API và Web UI bằng Playwright, tích hợp CI/CD.",
           "Tối thiểu 2 năm Automation Test, thành thạo Postman, Playwright hoặc Selenium."),
    "bd": ("Tìm kiếm khách hàng B2B, tư vấn giải pháp chuyển đổi số, đàm phán hợp đồng.",
           "Tối thiểu 1 năm Sales B2B ngành phần mềm, giao tiếp và thuyết trình tốt."),
    "mkt": ("Lập kế hoạch và triển khai chiến dịch quảng cáo Facebook, Google, TikTok.",
            "Tối thiểu 1 năm Digital Marketing, biết đo lường hiệu quả chiến dịch."),
    "acc": ("Hạch toán, lập báo cáo thuế tháng/quý, quyết toán cuối năm.",
            "Tốt nghiệp Kế toán – Kiểm toán, tối thiểu 2 năm kế toán tổng hợp, thành thạo MISA."),
    "intern": ("Tham gia phát triển module nội bộ cùng đội Backend dưới sự hướng dẫn của mentor.",
               "Sinh viên năm 3-4 ngành CNTT, nắm vững Java cơ bản và OOP."),
    "devops": ("Vận hành hạ tầng Kubernetes, xây dựng pipeline CI/CD.",
               "Tối thiểu 2 năm DevOps, thành thạo Docker, Kubernetes, Terraform."),
    "data": ("Phân tích dữ liệu kinh doanh, xây dựng dashboard Power BI.",
             "Thành thạo SQL, Excel nâng cao, Power BI."),
    "salesadmin": ("Hỗ trợ đội kinh doanh soạn hợp đồng, theo dõi công nợ.",
                   "Cẩn thận, thành thạo tin học văn phòng."),
    "mobile": ("Phát triển ứng dụng di động bằng Flutter.", "Tối thiểu 1 năm Flutter hoặc React Native."),
    "cs": ("Tiếp nhận và xử lý yêu cầu hỗ trợ của khách hàng qua điện thoại, email, chat.",
           "Giao tiếp tốt, kiên nhẫn, ưu tiên có kinh nghiệm chăm sóc khách hàng."),
    "ba": ("Thu thập yêu cầu, viết tài liệu đặc tả, phối hợp đội phát triển.",
           "Tối thiểu 2 năm Business Analyst, thành thạo UML, BPMN."),
}
HN = "Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội"
HCM = "Số 12 Lê Thánh Tôn, Bến Nghé, Quận 1, TP. Hồ Chí Minh"
# id, code, title, key, dept, hm, created_by, positions, type, min, max, gender, probation, model, location,
# status, created(day), submitted(day), round, original
REQS = [
    (1, "Senior Java Backend Engineer", "java", 1, HM_TUAN, 2, "Full-time", 30e6, 45e6, "Any", "2 tháng", "Hybrid", HN, "Approved", -75, -74),
    (2, "Frontend ReactJS Developer", "react", 1, HM_TUAN, 2, "Full-time", 20e6, 32e6, "Any", "2 tháng", "Hybrid", HN, "Approved", -74, -73),
    (3, "QA Automation Engineer", "qa", 1, HM_TUAN, 1, "Full-time", 18e6, 28e6, "Any", "2 tháng", "On-site", HN, "Approved", -73, -72),
    (4, "Business Development Executive (B2B)", "bd", 2, HM_HUONG, 2, "Full-time", 15e6, 25e6, "Any", "2 tháng", "On-site", HCM, "Approved", -72, -71),
    (5, "Digital Marketing Specialist", "mkt", 2, HM_HUONG, 1, "Contract", 14e6, 20e6, "Female", "1 tháng", "Hybrid", HCM, "Approved", -70, -69),
    (6, "Kế toán tổng hợp", "acc", 4, HM_MINH, 1, "Full-time", 15e6, 22e6, "Any", "Thỏa thuận", "On-site", HN, "Approved", -68, -67),
    (7, "Thực tập sinh Java Backend", "intern", 1, HM_TUAN, 3, "Internship", 3e6, 5e6, "Any", None, "Remote", None, "Approved", -100, -99),
    (8, "DevOps Engineer", "devops", 1, HM_TUAN, 1, "Full-time", 30e6, 40e6, "Any", "2 tháng", "Hybrid", HN, "Pending_Director", -3, -2),
    (9, "Data Analyst", "data", 4, HM_MINH, 1, "Full-time", None, None, "Any", "2 tháng", "On-site", HN, "Draft", -1, None),
    (10, "Sales Admin bán thời gian", "salesadmin", 2, HM_HUONG, 1, "Part-time", 6e6, 8e6, "Female", None, "On-site", HCM, "Rejected", -20, -19),
    (11, "Mobile Developer (Flutter)", "mobile", 1, HM_TUAN, 1, "Full-time", 20e6, 30e6, "Any", "2 tháng", "On-site", HN, "Cancelled", -30, None),
    (12, "Customer Support Executive", "cs", 2, HM_HUONG, 2, "Full-time", 9e6, 12e6, "Any", "1 tháng", "On-site", HCM, "Closed", -110, -109),
    (13, "Customer Support Executive", "cs", 2, HM_HUONG, 1, "Full-time", 9e6, 13e6, "Any", "1 tháng", "On-site", HCM, "Approved", -6, -5),
    (14, "Business Analyst", "ba", 1, HM_TUAN, 1, "Full-time", 22e6, 32e6, "Male", "2 tháng", "Hybrid", HN, "Approved", -5, -4),
]
REQ = {}
for rid, title, key, dept, hm, pos, etype, mn, mx, gender, prob, model, loc, st, cday, sday in REQS:
    rnd, orig = (2, 12) if rid == 13 else (1, None)
    REQ[rid] = dict(title=title, key=key, dept=dept, hm=hm, pos=pos, type=etype, min=mn, max=mx, model=model,
                    loc=loc, gender=gender, prob=prob)
    approved_day = (sday or cday) + 2
    add("JobRequisition", {
        "RequisitionId": rid, "Title": title, "RequisitionCode": f"REQ-{rid:04d}", "DepartmentId": dept,
        "HiringManagerId": hm, "CreatedBy": hm, "RecruitmentRound": rnd, "OriginalRequisitionId": orig,
        "NumberOfPositions": pos, "EmploymentType": etype,
        "MinSalary": None if mn is None else f"{mn:.2f}", "MaxSalary": None if mx is None else f"{mx:.2f}",
        "ReasonForHiring": f"Bổ sung nhân sự cho vị trí {title} theo kế hoạch quý." + (" Tuyển đợt 2 do đợt 1 chưa đủ người." if rid == 13 else ""),
        "JobDescription": JD[key][0], "RequirementDetails": JD[key][1], "RequiredGender": gender,
        "ProbationPeriod": prob, "WorkModel": model, "WorkLocation": loc, "ApprovalStatus": st,
        "SubmittedAt": T(at(sday, 9)) if sday is not None else None,
        "ClosedAt": T(at(-12, 17)) if st == "Closed" else None,
        "CreatedAt": T(at(cday, 9)),
        "UpdatedAt": T(at(min(approved_day, -1), 10)) if st not in ("Draft", "Pending_Director") else None})
    # MinSalary/MaxSalary là DECIMAL: bỏ dấu nháy
    row = tables["JobRequisition"][1][-1]
    for c in ("MinSalary", "MaxSalary"):
        if row[c] is not None:
            row[c] = Raw(row[c])
    audit(hm, "CREATE", "JobRequisition", rid, None,
          f"Tạo yêu cầu: {title} · {'Draft' if sday is None else 'Pending_Director'}", at(cday, 9))
    if sday is not None:
        notify(DIRECTOR, hm, "Yêu cầu tuyển dụng mới cần duyệt", f"{USER_NAME[hm]} gửi yêu cầu \"{title}\" chờ phê duyệt.",
               "REQUISITION_PENDING_DIRECTOR", rid, f"/requisitions/{rid}", f"REQ_PENDING_DIR_{rid}_{DIRECTOR}_{rid}", at(sday, 9))
    if rid == 6:  # bị từ chối một lần, sửa rồi được duyệt
        add("RequisitionApproval", {"RequisitionId": rid, "DirectorId": DIRECTOR, "[Status]": "Rejected",
                                    "Comments": "Mức lương đề xuất vượt ngân sách, đề nghị điều chỉnh.", "ApprovalDate": T(at(cday + 1, 15))})
    if st in ("Approved", "Closed"):
        add("RequisitionApproval", {"RequisitionId": rid, "DirectorId": DIRECTOR, "[Status]": "Approved",
                                    "Comments": f"Đồng ý tuyển {pos} vị trí {title}.", "ApprovalDate": T(at(approved_day, 10))})
        audit(DIRECTOR, "UPDATE", "JobRequisition", rid, "Pending_Director", "Approved", at(approved_day, 10))
        notify(hm, DIRECTOR, "Yêu cầu tuyển dụng đã được duyệt", f"Yêu cầu \"{title}\" đã được Giám đốc phê duyệt.",
               "REQUISITION_APPROVED", rid, f"/requisitions/{rid}", f"REQ_APPROVE_{rid}", at(approved_day, 10))
        for hr in HRS:
            notify(hr, DIRECTOR, "Yêu cầu tuyển dụng đã được duyệt", f"Yêu cầu \"{title}\" đã được duyệt, có thể tạo tin tuyển dụng.",
                   "REQUISITION_APPROVED", rid, f"/internal/job-postings/create?requisitionId={rid}", f"REQ_APPROVE_{rid}", at(approved_day, 10))
    if st == "Rejected":
        add("RequisitionApproval", {"RequisitionId": rid, "DirectorId": DIRECTOR, "[Status]": "Rejected",
                                    "Comments": "Chưa cần thiết trong quý này, đề nghị phân bổ lại công việc nội bộ.", "ApprovalDate": T(at(sday + 1, 11))})
        audit(DIRECTOR, "UPDATE", "JobRequisition", rid, "Pending_Director", "Rejected", at(sday + 1, 11))
        notify(hm, DIRECTOR, "Yêu cầu tuyển dụng bị từ chối", f"Yêu cầu \"{title}\" đã bị Giám đốc từ chối.",
               "REQUISITION_REJECTED", rid, f"/requisitions/{rid}", f"REQ_REJECT_{rid}", at(sday + 1, 11))
    if st == "Cancelled":
        audit(hm, "UPDATE", "JobRequisition", rid, "Draft", "Cancelled", at(cday + 3, 16))
    if st == "Closed":
        audit(HR_LAN, "UPDATE", "JobRequisition", rid, "Approved", "Closed", at(-12, 17))

# =============================================================================
# 4. JOB POSTING
# =============================================================================
BENEFITS = "- Lương tháng 13, thưởng dự án\n- Bảo hiểm sức khỏe PVI\n- 12 ngày phép năm\n- Review lương 2 lần/năm"
HOURS = "Thứ 2 - Thứ 6 (08:30 - 17:30)"
# id, req, no, status, posted(day), deadline(day), creator, overrides
POSTINGS = [
    (1, 1, 1, "Published", -60, 30, HR_LAN, {}),
    (2, 2, 1, "Published", -58, 25, HR_LAN, {"NumberOfPositionsDisplay": None}),
    (3, 3, 1, "Paused", -55, 20, HR_QUANG, {}),
    (4, 4, 1, "Published", -56, 21, HR_QUANG, {"WorkingHours": "Thứ 2 - Thứ 7 (08:00 - 17:00)"}),
    (5, 5, 1, "Published", -45, -2, HR_QUANG, {}),  # đã quá hạn nộp nhưng chưa đóng
    (6, 6, 1, "Published", -40, 35, HR_LAN, {"ProbationPeriod": None}),
    (7, 7, 1, "Closed", -95, -50, HR_LAN, {}),
    (8, 7, 2, "Published", 0, 40, HR_LAN, {"WorkModel": "Hybrid", "EmploymentType": "Internship"}),  # đăng lại hôm nay
    (9, 12, 1, "Closed", -105, -15, HR_QUANG, {}),
    (10, 13, 1, "Draft", None, None, HR_QUANG, {}),
]
POST = {}
for pid, rid, no, st, pday, dday, creator, ov in POSTINGS:
    r = REQ[rid]
    POST[pid] = dict(req=rid, title=r["title"], hm=r["hm"], key=r["key"], status=st)
    salary = "Thỏa thuận" if r["min"] is None else f"{int(r['min']):,} - {int(r['max']):,} VND"
    row = {"JobPostingId": pid, "RequisitionId": rid, "PostingNo": no,
           "PostingTitle": r["title"] + (" (Đợt 2)" if rid == 13 else ""),
           "JobDescription": JD[r["key"]][0] + "\nLàm việc cùng đội ngũ trẻ, quy trình Agile/Scrum.",
           "JobRequirements": "- " + JD[r["key"]][1].replace(", ", "\n- ", 1), "Benefits": BENEFITS,
           "SalaryDisplay": salary, "NumberOfPositionsDisplay": f"{r['pos']} người", "WorkLocation": r["loc"] or "Làm việc từ xa",
           "WorkingHours": HOURS, "EmploymentType": None, "WorkModel": None, "RequiredGender": r["gender"],
           "ProbationPeriod": r["prob"],
           # tin đăng hôm nay: lùi 1 giờ so với lúc chạy để luôn ở quá khứ
           "PostingDate": None if pday is None else (ago(60) if pday == 0 else T(at(pday, 8))),
           "ApplicationDeadline": None if dday is None else T(at(dday, 23, 59)),
           "PostingStatus": st, "ClosedAt": None, "CreatedBy": creator,
           "CreatedAt": ago(90) if pday == 0 else T(at((pday or -2) - 1, 16)), "UpdatedAt": None}
    row.update(ov)
    if st == "Closed":
        row["ClosedAt"] = T(at(-12, 17) if pid == 9 else at(-50, 17))
        row["UpdatedAt"] = row["ClosedAt"]
    if st == "Paused":
        row["UpdatedAt"] = T(at(-5, 11))
    add("JobPosting", row)
    if pday is not None:
        audit(creator, "CREATE", "JobPosting", pid, None, f"Đăng tin: {row['PostingTitle']}", at(pday, 8) if pday else at(0, 0, 1))
        ev, title_ev = ("JOB_POSTING_REOPENED", "Tin tuyển dụng được đăng lại") if no > 1 else ("JOB_POSTING_PUBLISHED", "Tin tuyển dụng đã đăng")
        prefix = "JOB_REOPEN_" if no > 1 else "JOB_PUB_"
        notify(r["hm"], creator, title_ev, f"Tin \"{row['PostingTitle']}\" đã được đăng công khai.", ev, pid,
               f"/internal/job-postings/{pid}", f"{prefix}{pid}_{pid}", at(pday, 8) if pday else at(0, 0, 1))
    if st == "Paused":
        audit(creator, "UPDATE", "JobPosting", pid, "Published", "Paused", at(-5, 11))
        notify(r["hm"], creator, "Tin tuyển dụng tạm dừng", f"Tin \"{row['PostingTitle']}\" đang tạm dừng nhận hồ sơ.",
               "JOB_POSTING_PAUSED", pid, f"/internal/job-postings/{pid}", f"JOB_PAUSE_{pid}_1", at(-5, 11))
    if st == "Closed":
        cday = -12 if pid == 9 else -50
        audit(creator, "UPDATE", "JobPosting", pid, None, "Closed", at(cday, 17))
        notify(r["hm"], creator, "Tin tuyển dụng đã đóng", f"Tin \"{row['PostingTitle']}\" đã đóng đợt tuyển.",
               "JOB_POSTING_CLOSED", pid, f"/internal/job-postings/{pid}", f"JOB_CLOSE_{pid}_1", at(cday, 17))

# =============================================================================
# 5. ỨNG VIÊN
# =============================================================================
HO = ["Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Phan", "Vũ", "Võ", "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý", "Đinh"]
DEM_NAM = ["Văn", "Minh", "Tuấn", "Đức", "Quốc", "Hữu", "Thành", "Trọng", "Duy", "Tiến"]
TEN_NAM = ["Bình", "Cường", "Dũng", "Đạt", "Hải", "Hiếu", "Hùng", "Huy", "Khánh", "Khoa", "Long", "Nam", "Phong",
           "Phúc", "Quân", "Sơn", "Tài", "Thắng", "Toàn", "Trung", "Tùng", "Việt", "Vinh"]
DEM_NU = ["Thị", "Mai", "Phương", "Ngọc", "Thu", "Thanh", "Khánh", "Mỹ", "Quỳnh", "Hải"]
TEN_NU = ["Anh", "Chi", "Dung", "Giang", "Hà", "Hằng", "Hạnh", "Hoa", "Hương", "Huyền", "Lan", "Linh", "Ly",
          "Mai", "My", "Ngân", "Nhung", "Phương", "Quỳnh", "Thảo", "Trang", "Uyên", "Vân", "Vy", "Yến"]
ADDR = ["Số 15 Duy Tân, Cầu Giấy, Hà Nội", "Số 102 Nguyễn Trãi, Thanh Xuân, Hà Nội", "Số 88 Láng Hạ, Đống Đa, Hà Nội",
        "Số 120 Kim Mã, Ba Đình, Hà Nội", "Số 50 Trần Phú, Hà Đông, Hà Nội", HCM,
        "Số 88 Điện Biên Phủ, Bình Thạnh, TP. Hồ Chí Minh", "Số 30 Quang Trung, Gò Vấp, TP. Hồ Chí Minh"]
SOURCES = ["TopCV", "VietnamWorks", "LinkedIn", "ITViec", "Employee Referral", "Website"]
S1 = "àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ"
S0 = "aaaaaaaaaaaaaaaaaeeeeeeeeeeeiiiiiooooooooooooooooouuuuuuuuuuuyyyyyd"


def remove_accents(s):
    return "".join(S0[S1.index(c)] if c in S1 else c for c in s.lower())


CAND = {}  # candidateId -> dict
used = set()
next_uid, next_cid = 101, 1


def new_candidate(name=None, female=None, username=None, email=None, status="Active", agreed=True,
                  potential=False, created=None):
    global next_uid, next_cid
    if female is None:
        female = random.random() < 0.4
    if name is None:
        name = f"{random.choice(HO)} {random.choice(DEM_NU if female else DEM_NAM)} {random.choice(TEN_NU if female else TEN_NAM)}"
    if username is None:
        parts = remove_accents(name).split()
        base = f"{parts[-1]}.{parts[0]}"
        username, i = base, 1
        while username in used:
            username, i = f"{base}{i}", i + 1
        used.add(username)
    email = email or f"{username}@gmail.com"
    uid, cid = next_uid, next_cid
    next_uid, next_cid = uid + 1, cid + 1
    created = created if created is not None else at(-90, 10)
    add("[User]", {"UserId": uid, "RoleId": 6, "Username": username, "PasswordHash": BCRYPT, "Email": email,
                   "FullName": name, "PhoneNumber": f"09{random.randint(10000000, 99999999)}", "AvatarUrl": None,
                   "DepartmentId": None, "AccountStatus": status, "CreatedAt": T(created), "UpdatedAt": None})
    add("Candidate", {"CandidateId": cid, "UserId": uid,
                      "DateOfBirth": f"{random.randint(1992, 2004)}-{random.randint(1, 12):02d}-{random.randint(1, 28):02d}",
                      "Gender": "Nữ" if female else "Nam", "Address": random.choice(ADDR),
                      "LinkedInUrl": f"https://linkedin.com/in/{username.replace('.', '-')}", "PortfolioUrl": None,
                      "CandidateSource": random.choice(SOURCES), "IsPotential": potential,
                      "PolicyAgreedAt": T(created + 1) if agreed else None, "CreatedAt": T(created), "UpdatedAt": None})
    CAND[cid] = dict(uid=uid, name=name, female=female, row=tables["Candidate"][1][-1])
    return cid


DEMO = new_candidate("Nguyễn Văn An", False, "candidate_demo", "candidate.demo@gmail.com", created=at(-80, 20))
NEW_ACC = new_candidate("Trần Thị Bích", True, "ung_vien_moi", "ung.vien.moi@gmail.com", agreed=False, created=at(-1, 21))
INACTIVE_CAND = new_candidate("Lê Hoàng Phúc", False, "ung_vien_khoa", "ung.vien.khoa@gmail.com", status="Inactive", created=at(-100, 9))

# =============================================================================
# 6. ỨNG TUYỂN THEO KỊCH BẢN
# =============================================================================
CV = {"java": "sample_cv_java_backend", "react": "sample_cv_frontend_react", "qa": "sample_cv_qa_automation",
      "bd": "sample_cv_business_development", "mkt": "sample_cv_business_development", "acc": "sample_cv_accountant",
      "intern": "sample_cv_fresher_intern", "cs": "sample_cv_business_development"}
INTERVIEWER = {1: IV_NAM, 2: IV_VIET, 3: IV_THAO, 4: IV_TRUNG, 5: IV_TRUNG, 6: IV_HOA, 7: IV_NAM, 8: IV_NAM, 9: IV_TRUNG}
SALARY = {"java": (36e6, 32e6), "react": (26e6, 23e6), "qa": (22e6, 19.5e6), "bd": (20e6, 17.5e6),
          "mkt": (17e6, 15e6), "acc": (18e6, 16e6), "intern": (4e6, 4e6), "cs": (11e6, 10e6)}
ids = {"app": 0, "int": 0, "offer": 0}
busy = set()  # (userId, minute) chống trùng lịch


def slot(day, people):
    # lịch quá khứ chỉ lùi về trước (không được trượt sang tương lai), lịch tương lai thì tiến lên
    days = range(day, day - 6, -1) if day < 0 else range(day, day + 10)
    for d in days:
        for h in (8, 9, 10, 11, 13, 14, 15, 16, 17):
            m = at(d, h)
            if all((p, m) not in busy for p in people):
                for p in people:
                    busy.add((p, m))
                return m
    raise RuntimeError("hết slot phỏng vấn")


def application(cid, pid, status, sub, updated=None, cv=None):
    ids["app"] += 1
    aid = ids["app"]
    key = POST[pid]["key"]
    sub_v = sub if isinstance(sub, Raw) else T(sub)
    add("Application", {"ApplicationId": aid, "CandidateId": cid, "JobPostingId": pid,
                        "AppliedCvUrl": cv or f"/samples/cv/{CV[key]}.pdf", "SubmissionDate": sub_v,
                        "ApplicationStatus": status, "CreatedAt": sub_v, "UpdatedAt": T(updated) if updated is not None else None})
    return aid


def review(aid, reviewer, role, decision, when, comment=None):
    comments = {"Pass": "Hồ sơ phù hợp yêu cầu vị trí, chuyển bước tiếp theo.",
                "Fail": "Kinh nghiệm chưa đáp ứng yêu cầu tối thiểu.",
                "Hold": "Tạm giữ, chờ đối chiếu thêm với các hồ sơ khác."}
    add("ApplicationReview", {"ApplicationId": aid, "ReviewerId": reviewer, "ReviewerRole": role, "Decision": decision,
                              "Comments": comment if comment is not None else comments[decision], "ReviewedAt": T(when)})


def interview(aid, pid, cid, day, status, created, director=False, online=None):
    ids["int"] += 1
    iid = ids["int"]
    hm, iv, hr = POST[pid]["hm"], INTERVIEWER[pid], HRS[iid % 2]
    panel = [(hr, "HR"), (hm, "HM"), (iv, "Interviewer")] + ([(DIRECTOR, "Director")] if director else [])
    start = slot(day, [p for p, _ in panel])
    assert start > created, (aid, day, start, created)
    assert (day < 0) == (start < 0), (aid, day, start)
    online = (iid % 3 == 0) if online is None else online
    add("InterviewSchedule", {"InterviewId": iid, "ApplicationId": aid,
                              "InterviewFormat": "Online_GoogleMeet" if online else "Offline_Office",
                              "StartTime": T(start), "EndTime": T(start + 60),
                              "LocationOrLink": f"https://meet.google.com/rms-int-{iid:04d}" if online else "Phòng họp 802, Tầng 8 Tòa nhà RMS Tower",
                              "InterviewStatus": status, "CreatedBy": hr, "CreatedAt": T(created)})
    for p, role in panel:
        add("InterviewPanel", {"InterviewId": iid, "InterviewerId": p, "RoleInPanel": role})
    audit(hr, "UPDATE", "InterviewSchedule", iid, None, "MailSent:InterviewInvitation", created + 2)
    for p, _ in panel:
        notify(p, hr, "Lịch phỏng vấn mới", f"Bạn có lịch phỏng vấn ứng viên {CAND[cid]['name']} - {POST[pid]['title']}.",
               "INTERVIEW_SCHEDULED", iid, "/interviews", f"INT_SCHED_{iid}", created)
    notify(CAND[cid]["uid"], hr, "Lịch phỏng vấn mới", f"Bạn được mời phỏng vấn vị trí {POST[pid]['title']}.",
           "INTERVIEW_SCHEDULED", iid, "/portal/interviews", f"INT_SCHED_{iid}", created)
    return iid, start, hm, iv, hr


def evaluate(iid, who, rec, when):
    good = rec == "Hire"
    add("InterviewEvaluation", {"InterviewId": iid, "InterviewerId": who,
                                "TechnicalScore": random.randint(4, 5) if good else random.randint(2, 3) if rec == "No_Hire" else 3,
                                "SoftSkillScore": random.randint(3, 5) if good else random.randint(2, 4),
                                "CulturalFitScore": random.randint(4, 5) if good else random.randint(2, 4),
                                "Strengths": "Nắm vững kiến thức nền tảng, trình bày mạch lạc." if good else "Thái độ cầu tiến.",
                                "Weaknesses": "Cần thêm kinh nghiệm hệ thống lớn." if good else "Kiến thức chuyên môn còn hạn chế.",
                                "Recommendation": rec, "Comments": "Đánh giá sau buổi phỏng vấn.", "EvaluatedAt": T(when)})


def final(iid, pid, hm, decision, when, considered=None):
    prop = SALARY[POST[pid]["key"]][0]
    add("InterviewFinalResult", {"InterviewId": iid, "HiringManagerId": hm, "FinalDecision": decision,
                                 "RecommendedSalary": Raw(f"{prop:.2f}") if decision == "Passed" else None,
                                 "FinalSummaryComments": {"Passed": "Ứng viên đạt yêu cầu, đề xuất gửi thư mời nhận việc.",
                                                          "Failed": "Ứng viên chưa đạt yêu cầu chuyên môn.",
                                                          "Considering": "Cần cân nhắc thêm, so sánh với ứng viên khác."}[decision],
                                 "ApprovedAt": T(when), "ConsideredAt": T(considered) if considered is not None else None})
    for hr in HRS:
        notify(hr, hm, "Đã có kết quả phỏng vấn", f"Kết quả phỏng vấn #{iid}: {decision}.", "INTERVIEW_RESULT_DECIDED",
               iid, f"/interviews/{iid}/result", f"INT_RESULT_{iid}_{decision}", when)


def offer(aid, pid, cid, status, created, approved=None, sent=None, answered=None, deleted=False, period=5):
    ids["offer"] += 1
    oid = ids["offer"]
    prop, prob = SALARY[POST[pid]["key"]]
    hr = HRS[oid % 2]
    deadline = sent + period * 1440 if sent is not None else None
    start_day = (deadline // 1440 + 10) if deadline is not None else (created // 1440 + 25)
    add("OfferProposal", {"OfferId": oid, "ApplicationId": aid, "OfferedPositionTitle": POST[pid]["title"],
                          "ProposedSalary": Raw(f"{prop:.2f}"), "ProbationSalary": Raw(f"{prob:.2f}"),
                          "ProbationDays": 30 if POST[pid]["key"] in ("mkt", "cs") else 60,
                          "ExpectedStartDate": DAY(start_day), "WorkLocation": REQ[POST[pid]["req"]]["loc"] or "Làm việc từ xa",
                          "BenefitsPackage": "Bảo hiểm PVI, 12 ngày phép năm, thưởng dự án, xét lương 2 lần/năm",
                          "OfferStatus": status,
                          "DeclineReason": "Đã nhận được đề nghị khác phù hợp hơn về địa điểm làm việc." if status == "Declined" else None,
                          "ResponsePeriodDays": period, "ResponseDeadline": T(deadline), "IsDeleted": deleted,
                          "ProposedBy": hr, "CreatedAt": T(created),
                          "UpdatedAt": T(answered or sent or approved) if (answered or sent or approved) else None})
    if status == "Draft":
        return oid
    notify(DIRECTOR, hr, "Thư mời nhận việc chờ duyệt", f"Thư mời cho ứng viên {CAND[cid]['name']} chờ Giám đốc phê duyệt.",
           "OFFER_PENDING_DIRECTOR", oid, f"/offers/{oid}", f"OFFER_PENDING_DIR_{oid}_{DIRECTOR}_1", created)
    if approved is not None:
        ok = status != "Director_Rejected"
        add("OfferApproval", {"OfferId": oid, "DirectorId": DIRECTOR, "[Status]": "Approved" if ok else "Rejected",
                              "DirectorComments": "Đồng ý mức đãi ngộ đề xuất." if ok else "Mức lương cao hơn khung, đề nghị HR thương lượng lại.",
                              "ApprovedAt": T(approved)})
        for r in (hr, POST[pid]["hm"]):
            notify(r, DIRECTOR, "Giám đốc đã " + ("duyệt" if ok else "từ chối") + " thư mời",
                   f"Thư mời cho ứng viên {CAND[cid]['name']} đã được xử lý.",
                   "OFFER_DIRECTOR_APPROVED" if ok else "OFFER_DIRECTOR_REJECTED", oid, f"/offers/{oid}",
                   f"{'OFFER_DEC_APPV_' if ok else 'OFFER_DEC_REJ_'}{oid}_{r}_1", approved)
    if sent is not None:
        notify(CAND[cid]["uid"], hr, f"Bạn có thư mời nhận việc mới: {POST[pid]['title']}",
               f"Vui lòng phản hồi trước hạn {period} ngày.", "OFFER_LETTER_SENT", oid, f"/portal/offers/{oid}",
               f"OFFER_SENT_{oid}_1", sent)
    if answered is not None:
        acc = status == "Accepted"
        ev = "OFFER_ACCEPTED" if acc else "OFFER_DECLINED"
        notify(hr, CAND[cid]["uid"], "Ứng viên đã " + ("chấp nhận" if acc else "từ chối") + " thư mời",
               f"{CAND[cid]['name']} đã phản hồi thư mời.", ev, oid, f"/offers/{oid}",
               f"OFFER_RESP_{'ACC' if acc else 'DEC'}_{oid}", answered)
        notify(POST[pid]["hm"], CAND[cid]["uid"], "Ứng viên đã " + ("chấp nhận" if acc else "từ chối") + " thư mời",
               f"{CAND[cid]['name']} đã phản hồi thư mời.", ev, oid, f"/applications/{aid}",
               f"OFFER_RESP_HM_{'ACC' if acc else 'DEC'}_{oid}", answered)
    return oid


def screened(aid, pid, cid, S, hm_role="HiringManager"):
    """HR Pass (S+1) rồi HM Pass (S+2)."""
    review(aid, HRS[aid % 2], "HR", "Pass", S + 1440 + 60)
    notify(POST[pid]["hm"], HRS[aid % 2], "Hồ sơ chờ trưởng bộ phận duyệt", f"Hồ sơ {CAND[cid]['name']} đã qua vòng HR.",
           "APPLICATION_HR_PASSED", aid, f"/applications/{aid}", f"APP_HR_PASS_{aid}", S + 1440 + 60)
    reviewer = DIRECTOR if hm_role == "Director" else POST[pid]["hm"]
    review(aid, reviewer, hm_role, "Pass", S + 2 * 1440 + 120)
    for hr in HRS:
        notify(hr, reviewer, "Hồ sơ sẵn sàng phỏng vấn", f"Hồ sơ {CAND[cid]['name']} đã qua vòng chuyên môn.",
               "APPLICATION_HM_PASSED", aid, f"/applications/{aid}", f"APP_HM_PASS_{aid}", S + 2 * 1440 + 120)


def rejected_notice(aid, cid, when):
    notify(CAND[cid]["uid"], None, "Kết quả ứng tuyển", "Rất tiếc hồ sơ của bạn chưa phù hợp với vị trí lần này.",
           "APPLICATION_REJECTED", aid, f"/portal/applications/{aid}", f"APP_REJECTED_{aid}", when)


def run(scn, pid, cid=None, S=None):
    """Tạo một hồ sơ đi tới đúng trạng thái của kịch bản. S = ngày nộp (phút từ @D0)."""
    if cid is None:
        cid = new_candidate(female=True if REQ[POST[pid]["req"]]["gender"] == "Female" else None,
                            created=(S if isinstance(S, int) else at(-1)) - 30)
    hm = POST[pid]["hm"]
    if scn == "A":
        return application(cid, pid, "Applied", S)
    if scn == "HR_HOLD":
        aid = application(cid, pid, "Applied", S)
        review(aid, HRS[0], "HR", "Hold", S + 1440)
        return aid
    if scn == "HR_PASS":
        aid = application(cid, pid, "HR_Passed", S, S + 1440 + 60)
        review(aid, HRS[aid % 2], "HR", "Pass", S + 1440 + 60)
        notify(hm, HRS[aid % 2], "Hồ sơ chờ trưởng bộ phận duyệt", f"Hồ sơ {CAND[cid]['name']} đã qua vòng HR.",
               "APPLICATION_HR_PASSED", aid, f"/applications/{aid}", f"APP_HR_PASS_{aid}", S + 1440 + 60)
        return aid
    if scn == "HR_FAIL":
        aid = application(cid, pid, "Rejected", S, S + 1440)
        review(aid, HRS[aid % 2], "HR", "Fail", S + 1440)
        rejected_notice(aid, cid, S + 1440)
        return aid
    if scn in ("HM_PASS", "HM_PASS_DIRECTOR"):
        aid = application(cid, pid, "HM_Passed", S, S + 2 * 1440 + 120)
        screened(aid, pid, cid, S, "Director" if scn == "HM_PASS_DIRECTOR" else "HiringManager")
        return aid
    if scn == "HM_FAIL":
        aid = application(cid, pid, "Rejected", S, S + 2 * 1440)
        review(aid, HRS[aid % 2], "HR", "Pass", S + 1440)
        review(aid, hm, "HiringManager", "Fail", S + 2 * 1440)
        rejected_notice(aid, cid, S + 2 * 1440)
        return aid
    if scn == "HM_HOLD":
        aid = application(cid, pid, "HR_Passed", S, S + 1440)
        review(aid, HRS[aid % 2], "HR", "Pass", S + 1440)
        review(aid, hm, "HiringManager", "Hold", S + 2 * 1440)
        return aid
    if scn == "WITHDRAWN":
        aid = application(cid, pid, "Withdrawn", S, S + 1440 * 2)
        audit(CAND[cid]["uid"], "UPDATE", "Application", aid, "Applied", "Withdrawn", S + 1440 * 2)
        return aid

    # ---- từ đây hồ sơ đã qua 2 vòng duyệt CV ----
    aid = application(cid, pid, "Interviewing", S)
    screened(aid, pid, cid, S)
    created = S + 3 * 1440
    app_row = tables["Application"][1][-1]
    app_row["UpdatedAt"] = T(created)
    director = pid == 1 and aid % 2 == 0

    if scn == "INT_UPCOMING":
        interview(aid, pid, cid, 1 + aid % 6, "Scheduled", created, director)
    elif scn == "INT_RESCHEDULED":
        iid, *_ = interview(aid, pid, cid, 2 + aid % 4, "Rescheduled", created, director)
        hr = HRS[iid % 2]
        notify(CAND[cid]["uid"], hr, "Lịch phỏng vấn đã thay đổi", "Lịch phỏng vấn của bạn đã được dời sang thời gian mới.",
               "INTERVIEW_RESCHEDULED", iid, "/portal/interviews", f"INT_RESCHED_{iid}_1", at(-1, 15))
    elif scn == "INT_CANCELLED_THEN_NEW":
        iid, *_ = interview(aid, pid, cid, -2, "Cancelled", created)
        notify(CAND[cid]["uid"], HRS[iid % 2], "Lịch phỏng vấn đã hủy", "Buổi phỏng vấn đã bị hủy, HR sẽ liên hệ xếp lịch mới.",
               "INTERVIEW_CANCELLED", iid, "/portal/interviews", f"INT_CANCEL_{iid}", at(-3, 10))
        interview(aid, pid, cid, 3, "Scheduled", at(-3, 11))
    elif scn == "INT_AWAITING_EVAL":  # buổi đã diễn ra, chưa đủ phiếu
        iid, start, hm_, iv, _ = interview(aid, pid, cid, -1, "Scheduled", created)
        evaluate(iid, hm_, "Hire", start + 90)
    elif scn == "INT_EVALUATED":  # đủ phiếu, chờ HM chốt
        iid, start, hm_, iv, _ = interview(aid, pid, cid, -3, "Completed", created)
        evaluate(iid, iv, "Hire", start + 120)
        evaluate(iid, hm_, "Consider", start + 1440 + 600)  # nộp trễ hạn 24h
    elif scn == "INT_CONSIDERING":
        iid, start, hm_, iv, _ = interview(aid, pid, cid, -6, "Completed", created)
        evaluate(iid, iv, "Consider", start + 120)
        evaluate(iid, hm_, "Consider", start + 180)
        final(iid, pid, hm_, "Considering", start + 1440, considered=start + 1440)
    elif scn == "INT_FAILED":
        iid, start, hm_, iv, _ = interview(aid, pid, cid, S // 1440 + 5, "Completed", created)
        evaluate(iid, iv, "No_Hire", start + 120)
        evaluate(iid, hm_, "No_Hire", start + 180)
        final(iid, pid, hm_, "Failed", start + 1440)
        app_row["ApplicationStatus"], app_row["UpdatedAt"] = "Rejected", T(start + 1440)
        rejected_notice(aid, cid, start + 1440)
    elif scn == "INT_CONSIDER_FAILED":  # cân nhắc rồi chốt Không đạt
        iid, start, hm_, iv, _ = interview(aid, pid, cid, -15, "Completed", created)
        evaluate(iid, iv, "Consider", start + 120)
        evaluate(iid, hm_, "No_Hire", start + 180)
        final(iid, pid, hm_, "Failed", start + 4 * 1440, considered=start + 1440)
        app_row["ApplicationStatus"], app_row["UpdatedAt"] = "Rejected", T(start + 4 * 1440)
        rejected_notice(aid, cid, start + 4 * 1440)
    else:
        # các kịch bản còn lại: phỏng vấn đạt -> offer
        past = {"PASSED_NO_OFFER": -6, "OFFER_DRAFT": -9, "OFFER_DELETED": -9, "OFFER_PENDING": -8,
                "OFFER_DIR_REJECTED": -10, "OFFER_DIR_APPROVED": -9, "OFFER_SENT": -10, "OFFER_EXPIRED": -16,
                "OFFER_ACCEPTED": -14, "OFFER_DECLINED": -14, "OFFER_ACCEPTED_RECENT": -8}[scn]
        iday = (S // 1440) + 5 if scn in ("OFFER_ACCEPTED", "OFFER_DECLINED") else max(past, (S // 1440) + 5)
        iid, start, hm_, iv, _ = interview(aid, pid, cid, iday, "Completed", created, director)
        evaluate(iid, iv, "Hire", start + 120)
        evaluate(iid, hm_, "Hire", start + 180)
        if director:
            evaluate(iid, DIRECTOR, "Hire", start + 240)
        decided = start + 1440
        final(iid, pid, hm_, "Passed", decided)
        app_row["UpdatedAt"] = T(decided)
        c = decided + 1440  # HR lập offer
        if scn == "OFFER_DRAFT":
            offer(aid, pid, cid, "Draft", c)
        elif scn == "OFFER_DELETED":
            offer(aid, pid, cid, "Draft", c, deleted=True)
        elif scn == "OFFER_PENDING":
            offer(aid, pid, cid, "Pending_Director", c)
        elif scn == "OFFER_DIR_REJECTED":
            offer(aid, pid, cid, "Director_Rejected", c, approved=c + 1440)
        elif scn == "OFFER_DIR_APPROVED":
            offer(aid, pid, cid, "Director_Approved", c, approved=c + 1440)
        elif scn == "OFFER_SENT":  # còn hạn phản hồi
            sent = at(-2, 10)
            offer(aid, pid, cid, "Sent_Candidate", c, approved=sent - 600, sent=sent)
            app_row["ApplicationStatus"], app_row["UpdatedAt"] = "Offered", T(sent)
        elif scn == "OFFER_EXPIRED":
            sent = c + 2 * 1440
            offer(aid, pid, cid, "Expired", c, approved=sent - 600, sent=sent, period=3)
            app_row["ApplicationStatus"], app_row["UpdatedAt"] = "Offered", T(sent)
        elif scn == "OFFER_ACCEPTED_RECENT":  # nhận việc hôm nay (KPI "trúng tuyển trong tháng")
            sent, ans = at(-3, 10), at(0, 0, 1)
            offer(aid, pid, cid, "Accepted", c, approved=sent - 600, sent=sent, answered=ans)
            app_row["ApplicationStatus"], app_row["UpdatedAt"] = "Hired", T(ans)
        elif scn in ("OFFER_ACCEPTED", "OFFER_DECLINED"):
            sent = c + 2 * 1440
            ans = sent + 2 * 1440
            ok = scn == "OFFER_ACCEPTED"
            offer(aid, pid, cid, "Accepted" if ok else "Declined", c, approved=sent - 600, sent=sent, answered=ans)
            app_row["ApplicationStatus"], app_row["UpdatedAt"] = ("Hired" if ok else "Withdrawn"), T(ans)
    return aid


def close_posting(aid, pid, old_status, with_interview, close_at):
    """Hồ sơ đang xử lý khi tin bị đóng -> Cancelled (+ hủy lịch phỏng vấn)."""
    row = tables["Application"][1][aid - 1]
    row["ApplicationStatus"], row["UpdatedAt"] = "Cancelled", T(close_at)
    if with_interview:
        audit(POSTINGS[pid - 1][6], "UPDATE", "Application", aid, None, "InterviewCancelledOnClose", close_at)
    audit(POSTINGS[pid - 1][6], "UPDATE", "Application", aid, old_status, "PostingClosed", close_at)
    audit(POSTINGS[pid - 1][6], "UPDATE", "Application", aid, None,
          "MailSent:PostingClosed" if aid % 3 else "MailFailed:PostingClosed", close_at + 5)


# ---- Đợt tuyển đã đóng (tạo trước để làm nguồn CV cho talent pool) ----
def past_sub(lo, hi):
    return at(random.randint(lo, hi), random.randint(8, 21), random.randint(0, 59))


CLOSE9 = at(-12, 17)
pool_sources = []  # (candidateId, applicationId) - ứng viên tiềm năng có CV cũ
for scn in ["OFFER_ACCEPTED", "OFFER_ACCEPTED", "OFFER_DECLINED", "INT_FAILED", "HR_FAIL", "WITHDRAWN"]:
    run(scn, 9, S=past_sub(-60, -45))
for old, with_int in [("Applied", False), ("HR_Passed", False), ("HM_Passed", False), ("Interviewing", True),
                      ("Applied", False), ("HR_Passed", False), ("HM_Passed", False), ("Interviewing", True)]:
    scn = {"Applied": "A", "HR_Passed": "HR_PASS", "HM_Passed": "HM_PASS", "Interviewing": "HM_PASS"}[old]
    aid = run(scn, 9, S=past_sub(-40, -25))
    if with_int:  # đã có lịch nhưng bị hủy khi đóng tin
        interview(aid, 9, tables["Application"][1][aid - 1]["CandidateId"], -10, "Cancelled", at(-20, 9))
    close_posting(aid, 9, old, with_int, CLOSE9)
    pool_sources.append((tables["Application"][1][aid - 1]["CandidateId"], aid))
# đợt 1 của thực tập sinh (đóng 50 ngày trước)
for scn in ["OFFER_ACCEPTED", "INT_FAILED", "HM_FAIL", "HR_FAIL", "WITHDRAWN"]:
    run(scn, 7, S=past_sub(-90, -70))
for old in ["Applied", "HR_Passed"]:
    aid = run("A" if old == "Applied" else "HR_PASS", 7, S=past_sub(-70, -60))
    close_posting(aid, 7, old, False, at(-50, 17))
    pool_sources.append((tables["Application"][1][aid - 1]["CandidateId"], aid))

# ---- Ứng viên demo: mỗi đơn một trạng thái trên các tin khác nhau ----
DEMO_APPS = {
    "sent": run("OFFER_SENT", 2, DEMO, at(-24, 20)),          # có offer chờ phản hồi
    "upcoming": run("INT_UPCOMING", 4, DEMO, at(-12, 21)),    # có lịch phỏng vấn sắp tới
    "applied": run("A", 6, DEMO, at(-1, 22)),                 # rút đơn được
    "rejected": run("INT_FAILED", 5, DEMO, at(-30, 19)),
}
aid = run("A", 7, DEMO, at(-75, 20))
close_posting(aid, 7, "Applied", False, at(-50, 17))  # Cancelled do đóng tin
run("HR_FAIL", 9, INACTIVE_CAND, at(-55, 10))

# ---- Các tin đang tuyển: phủ mọi kịch bản + số lượng cho dashboard 8 tuần ----
FULL = ["A", "A", "HR_HOLD", "HR_PASS", "HR_FAIL", "HM_PASS", "HM_PASS_DIRECTOR", "HM_FAIL", "HM_HOLD", "WITHDRAWN",
        "INT_UPCOMING", "INT_UPCOMING", "INT_RESCHEDULED", "INT_CANCELLED_THEN_NEW", "INT_AWAITING_EVAL",
        "INT_EVALUATED", "INT_CONSIDERING", "INT_FAILED", "INT_CONSIDER_FAILED", "PASSED_NO_OFFER", "OFFER_DRAFT",
        "OFFER_DELETED", "OFFER_PENDING", "OFFER_DIR_REJECTED", "OFFER_DIR_APPROVED", "OFFER_SENT", "OFFER_EXPIRED",
        "OFFER_ACCEPTED", "OFFER_ACCEPTED_RECENT", "OFFER_DECLINED"]
PLAN = {
    1: FULL,
    2: ["A", "A", "HR_PASS", "HR_FAIL", "HM_PASS", "HM_FAIL", "WITHDRAWN", "INT_UPCOMING", "INT_AWAITING_EVAL",
        "INT_EVALUATED", "INT_FAILED", "PASSED_NO_OFFER", "OFFER_PENDING", "OFFER_ACCEPTED", "OFFER_ACCEPTED_RECENT"],
    3: ["A", "A", "HR_PASS", "HR_FAIL", "HM_PASS", "INT_FAILED", "OFFER_ACCEPTED"],  # tin đang tạm dừng
    4: ["A", "A", "HR_HOLD", "HR_PASS", "HR_FAIL", "HM_PASS", "HM_FAIL", "WITHDRAWN", "INT_UPCOMING", "INT_RESCHEDULED",
        "INT_EVALUATED", "INT_CONSIDERING", "INT_FAILED", "PASSED_NO_OFFER", "OFFER_DRAFT", "OFFER_PENDING",
        "OFFER_DIR_REJECTED", "OFFER_SENT", "OFFER_EXPIRED", "OFFER_ACCEPTED", "OFFER_DECLINED"],
    5: ["A", "HR_PASS", "HR_FAIL", "HM_PASS", "INT_FAILED", "OFFER_ACCEPTED", "OFFER_DECLINED"],  # tin quá hạn
    6: ["A", "A", "HR_PASS", "HR_FAIL", "HM_PASS", "INT_UPCOMING", "INT_AWAITING_EVAL", "PASSED_NO_OFFER",
        "OFFER_PENDING", "OFFER_SENT", "OFFER_ACCEPTED", "OFFER_ACCEPTED_RECENT"],
    8: ["A", "A", "A"],  # tin vừa đăng lại hôm nay
}
EARLIEST = {"A": (-14, -1), "HR_HOLD": (-10, -3), "HR_PASS": (-12, -3), "HR_FAIL": (-50, -3), "HM_PASS": (-15, -4),
            "HM_PASS_DIRECTOR": (-15, -4), "HM_FAIL": (-50, -4), "HM_HOLD": (-12, -4), "WITHDRAWN": (-50, -3),
            "INT_UPCOMING": (-14, -5), "INT_RESCHEDULED": (-14, -6), "INT_CANCELLED_THEN_NEW": (-14, -8),
            "INT_AWAITING_EVAL": (-14, -7), "INT_EVALUATED": (-16, -9), "INT_CONSIDERING": (-20, -12),
            "INT_FAILED": (-45, -16), "INT_CONSIDER_FAILED": (-30, -21), "PASSED_NO_OFFER": (-25, -12),
            "OFFER_DRAFT": (-28, -15), "OFFER_DELETED": (-28, -15), "OFFER_PENDING": (-26, -14),
            "OFFER_DIR_REJECTED": (-30, -16), "OFFER_DIR_APPROVED": (-28, -15), "OFFER_SENT": (-30, -16),
            "OFFER_EXPIRED": (-35, -22), "OFFER_ACCEPTED": (-56, -20), "OFFER_DECLINED": (-50, -20),
            "OFFER_ACCEPTED_RECENT": (-20, -13)}
for pid, plan in PLAN.items():
    for scn in plan:
        lo, hi = EARLIEST[scn]
        if pid == 5:  # hết hạn 2 ngày trước: mọi đơn nộp trước hạn
            hi = min(hi, -3)
            lo = min(lo, hi)
        if pid == 8:
            run(scn, pid, S=ago(random.randint(10, 50)))
        else:
            run(scn, pid, S=past_sub(lo, hi))
# đơn nộp hôm nay (KPI "hồ sơ mới hôm nay"), luôn ở quá khứ so với lúc chạy
for pid in (1, 2, 4):
    run("A", pid, S=ago(15 + pid * 7))

# ---- Talent pool ----
for cid, _ in pool_sources:
    CAND[cid]["row"]["IsPotential"] = True
for i, (cid, src) in enumerate(pool_sources):
    marker = HRS[i % 2] if i % 3 else POST[9]["hm"]
    audit(marker, "UPDATE", "Candidate", cid, "false", "TalentPoolMarked", at(-11, 9, i))
    add("TalentPoolNote", {"CandidateId": cid, "UserId": marker,
                           "NoteContent": ["Giao tiếp tốt, phù hợp vị trí chăm sóc khách hàng hoặc kinh doanh.",
                                           "Ứng viên chủ động, nên liên hệ lại khi có vị trí mới.",
                                           "Kỹ năng chuyên môn khá, cần thêm kinh nghiệm thực tế."][i % 3],
                           "CreatedAt": T(at(-11, 10, i))})
# một ứng viên đã từng được đánh dấu rồi bỏ đánh dấu
unmarked = pool_sources[-1][0]
CAND[unmarked]["row"]["IsPotential"] = False
audit(HR_LAN, "UPDATE", "Candidate", unmarked, "true", "TalentPoolUnmarked", at(-4, 14))
audit(HR_QUANG, "DELETE", "TalentPoolNote", 999, "Ghi chú nhập nhầm ứng viên", None, at(-9, 16))


def tp(kind, pid, src_cid, src_aid):
    hm = POST[pid]["hm"]
    cv = tables["Application"][1][src_aid - 1]["AppliedCvUrl"]
    created = at(-6, 10) if kind != "HR_RECO_PENDING" else at(-1, 9)
    from_hr = kind.startswith("HR_")
    actor = HR_LAN if from_hr else hm
    aid = application(src_cid, pid, "HM_Suggested", created, None, cv)
    if from_hr:
        review(aid, HR_LAN, "HR", "Hold", created, "Gợi ý ứng viên tiềm năng từ đợt tuyển trước.")
    audit(actor, "CREATE", "Application", aid, str(src_aid), "TalentPoolHrRecommended" if from_hr else "TalentPoolSuggested", created)
    row = tables["Application"][1][aid - 1]
    later = created + 1440
    if kind == "HR_RECO_ACCEPTED":
        review(aid, hm, "HiringManager", "Pass", later)
        audit(hm, "UPDATE", "Application", aid, "HM_Suggested", "TalentPoolSuggested", later)
    elif kind == "HR_RECO_REJECTED":
        review(aid, hm, "HiringManager", "Fail", later)
        audit(hm, "UPDATE", "Application", aid, "HM_Suggested", "TalentPoolCancelled", later)
        row["ApplicationStatus"], row["UpdatedAt"] = "Rejected", T(later)
    elif kind == "HR_RECO_WITHDRAWN":
        audit(HR_LAN, "UPDATE", "Application", aid, "HM_Suggested", "TalentPoolCancelled", later)
        audit(HR_LAN, "UPDATE", "Application", aid, "HM_Suggested", "TalentPoolHrWithdrawn", later)
        row["ApplicationStatus"], row["UpdatedAt"] = "Rejected", T(later)
    elif kind == "HM_ACTIVATED":
        audit(HR_QUANG, "UPDATE", "Application", aid, "HM_Suggested", "TalentPoolActivated", later)
        row["ApplicationStatus"], row["UpdatedAt"], row["SubmissionDate"] = "HM_Passed", T(later), T(later)
    elif kind == "HM_CANDIDATE_SUBMITTED":
        audit(CAND[src_cid]["uid"], "UPDATE", "Application", aid, "HM_Suggested", "TalentPoolCandidateSubmitted", later)
        row["ApplicationStatus"], row["UpdatedAt"], row["SubmissionDate"] = "Applied", T(later), T(later)
        row["AppliedCvUrl"] = "/samples/cv/sample_cv_business_development.pdf"
    elif kind == "HM_HR_DECLINED":
        audit(HR_QUANG, "UPDATE", "Application", aid, "HM_Suggested", "TalentPoolCancelled", later)
        row["ApplicationStatus"], row["UpdatedAt"] = "Rejected", T(later)
    return aid


TP_PLAN = [("HM_PENDING", 4), ("HR_RECO_PENDING", 4), ("HR_RECO_ACCEPTED", 4), ("HR_RECO_REJECTED", 4),
           ("HR_RECO_WITHDRAWN", 1), ("HM_ACTIVATED", 4), ("HM_CANDIDATE_SUBMITTED", 4), ("HM_HR_DECLINED", 1),
           ("HM_PENDING", 1)]
for (kind, pid), (cid, src) in zip(TP_PLAN, pool_sources):
    tp(kind, pid, cid, src)

# =============================================================================
# 7. NHÓM DEMO OFFER: thêm cuối để không đổi ID/dữ liệu các kịch bản cũ
# =============================================================================
OFFER_DEMO_PLAN = [
    ("Draft", "draft", "OFFER_DRAFT", ("Nguyễn Minh Khang", "Trần Ngọc Mai")),
    ("Pending_Director", "pending", "OFFER_PENDING", ("Lê Quốc Bảo", "Phạm Thanh Hà")),
    ("Director_Approved", "approved", "OFFER_DIR_APPROVED", ("Hoàng Đức Anh", "Vũ Thu Trang")),
    ("Expired", "expired", "OFFER_EXPIRED", ("Đặng Hải Nam", "Bùi Khánh Linh")),
    ("Director_Rejected", "rejected", "OFFER_DIR_REJECTED", ("Đỗ Tuấn Kiệt", "Hồ Minh Thư")),
    ("Sent_Candidate", "sent", "OFFER_SENT", ("Ngô Gia Huy", "Dương Bảo Ngọc")),
    ("Accepted", "accepted", "OFFER_ACCEPTED_RECENT", ("Đinh Quang Minh", "Mai Phương Anh")),
    ("Declined", "declined", "OFFER_DECLINED", ("Tạ Thành Đạt", "Cao Thảo Vy")),
]
OFFER_DEMO = []
for status, key, scenario, names in OFFER_DEMO_PLAN:
    for number, name in enumerate(names, 1):
        username = f"offer_{key}_{number:02d}"
        cid = new_candidate(name, female=number == 2, username=username,
                            email=f"{username}@example.test", created=at(-45))
        aid = run(scenario, 1 if number == 1 else 4, cid, S=at(-40))
        oid = ids["offer"]
        row = tables["OfferProposal"][1][-1]
        assert row["ApplicationId"] == aid and row["OfferStatus"] == status and not row["IsDeleted"]
        OFFER_DEMO.append((status, username, cid, aid, oid))

# =============================================================================
# 8. XUẤT SQL
# =============================================================================
ORDER = ["[Role]", "Department", "[User]", "SystemConfig", "JobRequisition", "RequisitionApproval", "JobPosting",
         "Candidate", "TalentPoolNote", "Application", "ApplicationReview", "InterviewSchedule", "InterviewPanel",
         "InterviewEvaluation", "InterviewFinalResult", "OfferProposal", "OfferApproval", "[Notification]", "AuditLog"]
IDENTITY = {"[Role]": "RoleId", "Department": "DepartmentId", "[User]": "UserId", "JobRequisition": "RequisitionId",
            "JobPosting": "JobPostingId", "Candidate": "CandidateId", "Application": "ApplicationId",
            "InterviewSchedule": "InterviewId", "OfferProposal": "OfferId"}

app_rows = tables["Application"][1]
status_count = {}
for r in app_rows:
    status_count[r["ApplicationStatus"]] = status_count.get(r["ApplicationStatus"], 0) + 1

out = []
w = out.append
w("-- =============================================================================")
w("-- RMS - SEED DATA (sinh bởi database/seeds/build_seed.py, đừng sửa tay: sửa script rồi chạy lại)")
w("-- Chạy SAU database/schema/db.sql trên database RitirementManagement2 vừa tạo (bảng rỗng).")
w("-- File này thay thế seed_data + seed_extra_jobs_and_pipeline + seed_application_flow_fix + seed_dashboard_demo.")
w("-- Mọi mốc thời gian tính theo lúc chạy (@D0 = 00:00 hôm nay) nên dashboard / lịch sắp tới / hạn offer luôn mới.")
w("-- Mật khẩu mọi tài khoản: 12345678")
w("--")
w("-- TÀI KHOẢN NỘI BỘ (UserId - username - vai trò)")
for uid, role, uname, email, name, phone, dept, st, cday in INTERNAL:
    w(f"--   {uid:>2} {uname:<18} {ROLES[role - 1][1]:<15} {DEPTS[dept - 1][1]}{' (Inactive)' if st != 'Active' else ''}")
w("-- ỨNG VIÊN ĐẶC BIỆT")
w(f"--   candidate_demo  ứng viên demo: đơn #{DEMO_APPS['sent']} có offer chờ phản hồi, #{DEMO_APPS['upcoming']} có lịch PV sắp tới,")
w(f"--                   #{DEMO_APPS['applied']} Applied (rút được), #{DEMO_APPS['rejected']} Rejected, 1 đơn Cancelled do đóng tin")
w("--   ung_vien_moi    chưa đồng ý điều khoản (PolicyAgreedAt NULL), chưa ứng tuyển")
w("--   ung_vien_khoa   tài khoản Inactive")
w("-- YÊU CẦU: 1-7,13,14 Approved (14 chưa có tin) | 8 Pending_Director | 9 Draft | 10 Rejected | 11 Cancelled | 12 Closed (13 là đợt 2 của 12)")
w("-- TIN: 1,2,4,6,8 Published (5 Published nhưng quá hạn) | 3 Paused | 7,9 Closed | 10 Draft | 8 = lần đăng 2 của yêu cầu 7")
w("-- SỐ HỒ SƠ THEO TRẠNG THÁI: " + ", ".join(f"{k} {v}" for k, v in sorted(status_count.items())))
w("-- OFFER DEMO BỔ SUNG: mỗi trạng thái 2 tài khoản Active riêng (Status | Username | CandidateId | ApplicationId | OfferId)")
for status, username, cid, aid, oid in OFFER_DEMO:
    w(f"--   {status} | {username} | {cid} | {aid} | {oid}")
w("-- =============================================================================")
w("USE RitirementManagement2;")
w("GO")
w("SET NOCOUNT ON;")
w("SET QUOTED_IDENTIFIER ON; -- bảng có filtered index; sqlcmd mặc định tắt")
w("SET XACT_ABORT ON;")
w("GO")
w("")
w("IF EXISTS (SELECT 1 FROM [User])")
w("    THROW 50000, N'Database đã có dữ liệu. Chạy lại database/schema/db.sql trước khi seed.', 1;")
w("GO")
w("")
w("BEGIN TRY")
w("BEGIN TRANSACTION;")
w("DECLARE @Now DATETIME2 = SYSDATETIME();")
w("DECLARE @D0 DATETIME2 = CAST(CAST(@Now AS DATE) AS DATETIME2);")
w("")
for t in ORDER:
    cols, rows = tables.get(t, (None, []))
    if not rows:
        continue
    w(f"-- {t} ({len(rows)} dòng)")
    ident = IDENTITY.get(t)
    insert_cols = cols if ident else [c for c in cols if c not in ("NoteId",)]
    if ident:
        w(f"SET IDENTITY_INSERT {t} ON;")
    for i in range(0, len(rows), 50):
        w(f"INSERT INTO {t} ({', '.join(insert_cols)}) VALUES")
        chunk = rows[i:i + 50]
        w(",\n".join("    (" + ", ".join(q(r[c]) for c in insert_cols) + ")" for r in chunk) + ";")
    if ident:
        w(f"SET IDENTITY_INSERT {t} OFF;")
    w("")
for did, _, mgr, _ in DEPTS:
    if mgr:
        w(f"UPDATE Department SET ManagerId = {mgr} WHERE DepartmentId = {did};")
w("")
w("COMMIT TRANSACTION;")
w("PRINT N'>>> Seed thành công.';")
w("END TRY")
w("BEGIN CATCH")
w("    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;")
w("    THROW;")
w("END CATCH;")
w("GO")
w("")
w("-- =============================================================================")
w("-- TỰ KIỂM TRA: mọi giá trị trạng thái trong CHECK constraint phải có ít nhất 1 dòng")
w("-- =============================================================================")
CHECKS = [
    ("Application", "ApplicationStatus", ["Applied", "HR_Passed", "HM_Passed", "HM_Suggested", "Interviewing", "Offered", "Hired", "Rejected", "Withdrawn", "Cancelled"]),
    ("JobRequisition", "ApprovalStatus", ["Draft", "Pending_Director", "Approved", "Rejected", "Closed", "Cancelled"]),
    ("JobPosting", "PostingStatus", ["Draft", "Published", "Paused", "Closed"]),
    ("OfferProposal", "OfferStatus", ["Draft", "Pending_Director", "Director_Approved", "Director_Rejected", "Sent_Candidate", "Expired", "Accepted", "Declined"]),
    ("InterviewSchedule", "InterviewStatus", ["Scheduled", "Completed", "Cancelled", "Rescheduled"]),
    ("InterviewSchedule", "InterviewFormat", ["Online_GoogleMeet", "Offline_Office"]),
    ("InterviewPanel", "RoleInPanel", ["HR", "HM", "Interviewer", "Director"]),
    ("InterviewEvaluation", "Recommendation", ["Hire", "No_Hire", "Consider"]),
    ("InterviewFinalResult", "FinalDecision", ["Passed", "Failed", "Considering"]),
    ("ApplicationReview", "Decision", ["Pass", "Fail", "Hold"]),
    ("ApplicationReview", "ReviewerRole", ["HR", "HiringManager", "Director"]),
    ("RequisitionApproval", "[Status]", ["Approved", "Rejected"]),
    ("OfferApproval", "[Status]", ["Approved", "Rejected"]),
    ("[User]", "AccountStatus", ["Active", "Inactive"]),
    ("Department", "DepartmentStatus", ["Active", "Inactive"]),
    ("JobRequisition", "EmploymentType", ["Full-time", "Part-time", "Internship", "Contract"]),
    ("JobRequisition", "WorkModel", ["On-site", "Remote", "Hybrid"]),
    ("JobRequisition", "RequiredGender", ["Male", "Female", "Any"]),
    ("AuditLog", "[Action]", ["CREATE", "UPDATE", "DELETE", "LOGIN"]),
    ("AuditLog", "NewValue", ["Withdrawn", "PostingClosed", "InterviewCancelledOnClose", "TalentPoolMarked", "TalentPoolUnmarked",
                              "TalentPoolSuggested", "TalentPoolHrRecommended", "TalentPoolHrWithdrawn", "TalentPoolCancelled",
                              "TalentPoolActivated", "TalentPoolCandidateSubmitted", "MailSent:PostingClosed",
                              "MailFailed:PostingClosed", "MailSent:InterviewInvitation"]),
]
w("DECLARE @Missing NVARCHAR(MAX) = N'';")
for status, username, cid, aid, oid in OFFER_DEMO:
    w(f"IF NOT EXISTS (SELECT 1 FROM OfferProposal o JOIN Application a ON a.ApplicationId = o.ApplicationId "
      f"JOIN Candidate c ON c.CandidateId = a.CandidateId JOIN [User] u ON u.UserId = c.UserId "
      f"WHERE o.OfferId = {oid} AND a.ApplicationId = {aid} AND c.CandidateId = {cid} "
      f"AND u.Username = N'{username}' AND u.AccountStatus = N'Active' "
      f"AND o.OfferStatus = N'{status}' AND o.IsDeleted = 0) SET @Missing += N'Offer demo {username}; ';")
for t, col, values in CHECKS:
    for v in values:
        w(f"IF NOT EXISTS (SELECT 1 FROM {t} WHERE {col} = N'{v}') SET @Missing += N'{t}.{col}={v}; ';")
w("IF NOT EXISTS (SELECT 1 FROM OfferProposal WHERE IsDeleted = 1) SET @Missing += N'OfferProposal.IsDeleted=1; ';")
w("IF NOT EXISTS (SELECT 1 FROM Candidate WHERE PolicyAgreedAt IS NULL) SET @Missing += N'Candidate.PolicyAgreedAt NULL; ';")
w("IF NOT EXISTS (SELECT 1 FROM JobRequisition WHERE OriginalRequisitionId IS NOT NULL) SET @Missing += N'JobRequisition round 2; ';")
w("IF NOT EXISTS (SELECT 1 FROM JobPosting WHERE PostingNo > 1) SET @Missing += N'JobPosting PostingNo 2; ';")
w("IF NOT EXISTS (SELECT 1 FROM InterviewSchedule WHERE StartTime > SYSDATETIME() AND InterviewStatus = N'Scheduled') SET @Missing += N'lịch PV sắp tới; ';")
w("IF NOT EXISTS (SELECT 1 FROM OfferProposal WHERE OfferStatus = N'Sent_Candidate' AND ResponseDeadline > SYSDATETIME()) SET @Missing += N'offer còn hạn; ';")
w("IF NOT EXISTS (SELECT 1 FROM Application WHERE SubmissionDate >= CAST(CAST(SYSDATETIME() AS DATE) AS DATETIME2)) SET @Missing += N'hồ sơ hôm nay; ';")
w("IF @Missing <> N'' THROW 50001, @Missing, 1;")
w("PRINT N'>>> Tự kiểm tra: đủ mọi trạng thái.';")
w("SELECT ApplicationStatus, COUNT(*) AS SoHoSo FROM Application GROUP BY ApplicationStatus ORDER BY ApplicationStatus;")
w("GO")

with open(OUT_FILE, "w", encoding="utf-8") as f:
    f.write("\n".join(out) + "\n")
print(f"OK: {OUT_FILE} | " + ", ".join(f"{t} {len(r)}" for t, (c, r) in tables.items()))
print("Application:", status_count)

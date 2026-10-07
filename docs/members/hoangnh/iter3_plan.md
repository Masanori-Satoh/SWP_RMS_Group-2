# PLAN ITERATION 3 — HOANGNH
## Internal Job Details & Job Posting Hardening

**Người phụ trách:** Nguyễn Huy Hoàng — HoangNH  
**Iteration:** Iteration 3  
**Phạm vi chính:** Internal Job Details + hoàn thiện Job Posting integration + regression + QA + documentation

---

# 1. Mục tiêu Iteration 3

Iteration 3 không thay đổi Trigger/End condition của BF-01; tập trung hoàn thiện màn hình chi tiết và độ ổn định sau khi Job Posting đã được tạo/publish.

**BF-01 End condition vẫn là:** The Job Posting is successfully published and the requester is notified.

Theo phân công hiện tại, phần chính của HoangNH trong Iteration 3 là:

```text
Internal Job Details
```

Iteration 3 không mở rộng lại toàn bộ Requisition/Posting từ đầu mà tập trung:

```text
Internal Job Detail
→ quyền truy cập
→ hiển thị đầy đủ dữ liệu
→ action theo Posting Status
→ liên kết Source Requisition
→ public preview
→ regression
→ integration/security test
→ documentation
```

Các module không thuộc scope chính của HoangNH:

```text
Apply Job
AI Screening
Candidate List / Ranked CV
Interview Evaluation
Offer
Negotiation
Talent Pool
```

Chỉ phối hợp khi có dependency.

---

# 2. Điều kiện đầu vào từ Iteration 2

Trước khi làm Internal Job Details phải có:

```text
HR Approved Requisition List
Internal Job Management
Create Job Posting
Save Draft
Update Job Posting
Publish Job Posting
Public Job integration
Notification integration
```

JobPosting tối thiểu có:

```text
JobPostingId
RequisitionId
PostingTitle
JobDescription
JobRequirements
Benefits
SalaryDisplay
WorkLocation
PostingDate
ApplicationDeadline
PostingStatus
CreatedBy
CreatedAt
UpdatedAt
```

Không sử dụng `Version` trong scope hiện tại.

---

# 3. Internal Job Details Screen

Route đề xuất:

```text
GET /internal/job-postings/{id}
```

Actor:

```text
HR
```

Các role khác chỉ truy cập nếu SRS/business rule đã cấp quyền rõ ràng.

---

# 4. Nội dung màn Job Detail nội bộ

## Section 1 — Basic Information

Hiển thị:

```text
Job Posting ID
Posting Title
Posting Status
Source Requisition Code
Position
Recruitment Round
Department
Requester
Number of Openings
Created By
Created At
Updated At
```

---

## Section 2 — Public Job Content

Hiển thị:

```text
Job Description
Job Requirements
Benefits
Salary Display
Work Location
Application Deadline
Posting Date
```

---

## Section 3 — Source Requisition

Cho HR xem liên kết nguồn:

```text
Requisition Code
Position
Department
Recruitment Round
Approval Status
Requester
```

Rule:

```text
Job Posting Detail
→ có thể mở Requisition Detail nếu HR được phép xem nguồn đó
```

Không cho sửa Requisition từ Job Posting Detail.

---

# 5. Actions theo Posting Status

## Draft

Cho phép:

```text
Edit
Publish
Back to List
```

Không public preview như một job đang hoạt động nếu chưa publish.

---

## Published

Cho phép:

```text
View Public Job
Edit nếu business rule Iter2 đã chốt cho phép
Back to List
```

Không publish lại bằng action trùng.

---

## Paused

Nếu project đã hỗ trợ Paused:

```text
View
Back to List
```

Không tự thêm Resume nếu business chưa có rule.

---

## Closed

Nếu project đã hỗ trợ Closed:

```text
View
Back to List
```

Không tự thêm Reopen nếu business chưa được chốt.

---

# 6. Access Control

Backend phải kiểm tra:

```text
Current User
↓
Role / Account Active
↓
Permission to view internal Job Posting
↓
Find Job Posting
```

Không chỉ hide menu/button.

Test:

```text
Guest → denied
Candidate → denied
HM → denied nếu SRS không cấp quyền
Director → denied nếu SRS không cấp quyền
HR → allowed
```

Nếu sau này Director/HM được read-only thì phải cập nhật SRS + SecurityConfig + test trước khi mở quyền.

---

# 7. Not Found / Invalid Access

Các case:

```text
JobPosting ID không tồn tại
→ 404

JobPosting tồn tại nhưng user không có quyền
→ 403

Source Requisition bị lỗi dữ liệu
→ xử lý rõ, không NullPointerException / 500 tùy tiện
```

---

# 8. Public Preview Integration

Với Posting Published:

```text
Internal Job Detail
→ View Public Job
→ /jobs/{id}
```

Server public vẫn phải kiểm tra:

```text
PostingStatus = Published
```

Không dựa vào việc link chỉ hiện cho Published.

Draft / Paused / Closed không được public qua URL nếu policy hiện tại chặn.

---

# 9. Deadline Behavior

Màn Internal Detail phải hiển thị rõ:

```text
No deadline
Open
Expired
```

Nếu deadline đã qua:

```text
HR vẫn xem Internal Detail
Candidate không Apply
```

Không để trạng thái hiển thị khác với Public Job Service.

---

# 10. Notification Link Integration

Notification `JOB_POSTING_PUBLISHED` phải có link hợp lệ.

Đề xuất:

```text
Requester Notification
→ Public Job Detail
```

hoặc:

```text
HR Notification
→ Internal Job Detail
```

tùy actor/event.

Không nhận redirect URL tùy ý từ client.

---

# 11. Audit Information

Nếu Audit Log đã có, Internal Job Detail có thể hiển thị history tối thiểu hoặc cung cấp dữ liệu cho màn audit sau này.

Các event quan trọng:

```text
CREATE_POSTING
UPDATE_POSTING
PUBLISH_POSTING
```

Nếu AuditLog hiện chỉ hỗ trợ action generic như CREATE/UPDATE thì không tự đổi schema ngoài scope; dùng đúng convention hiện có.

---

# 12. Internal Job Detail UI

Recommended layout:

```text
Breadcrumb
Internal Job Management / Job Details

Header
Posting Title
Status Badge
Actions

Basic Information

Source Requisition

Job Description

Requirements

Benefits

Salary / Location / Deadline

Metadata
Created By
Created At
Updated At
```

Giữ cùng design system với RMS:

```text
same header
same sidebar
same typography
same green theme
same button/status convention
```

---

# 13. UX Rules

Không hiển thị action user không có quyền.

Ví dụ:

```text
Draft
→ Edit | Publish

Published
→ View Public Job

Closed
→ View only
```

Không để:

```text
Edit Requisition
Delete Requisition
Approve Requisition
```

trong màn HR Job Detail.

---

# 14. Regression Requisition → Posting

Iteration 3 phải đảm bảo các flow Iter1 + Iter2 không bị hỏng.

Regression flow:

```text
HM Create Requisition
→ Submit
→ Director Approve
→ HR Create Posting
→ Save Draft
→ Edit
→ Publish
→ Internal Detail
→ Public Detail
```

Reject path:

```text
HM Submit
→ Director Reject
→ HM Resubmit
→ Director Approve
→ HR Posting
```

---

# 15. Unit Test

Test service/detail mapping:

```text
findInternalJobDetail_shouldReturnCompleteData()

findInternalJobDetail_shouldIncludeSourceRequisition()

findInternalJobDetail_shouldFail_whenNotFound()

findInternalJobDetail_shouldRespectAccessRule()

publishedJob_shouldExposePublicLink()

draftJob_shouldNotExposePublicAction()
```

---

# 16. Controller Test

Kiểm tra:

```text
GET /internal/job-postings/{id}
```

Cases:

```text
HR + existing ID → 200
HR + missing ID → 404
Unauthorized role → 403
Published → public action/model available
Draft → no public action
```

---

# 17. Integration Test

## Flow 1 — Draft Detail

```text
Approved Requisition
→ Create Posting Draft
→ Open Internal Detail
→ Verify source + content + status
```

## Flow 2 — Published Detail

```text
Draft
→ Publish
→ Internal Detail
→ View Public Job
→ Public data matches
```

## Flow 3 — Source integrity

```text
Edit Job Posting
→ Source Requisition unchanged
→ Internal Detail still points to same Requisition
```

## Flow 4 — Deadline

```text
Published + expired deadline
→ HR internal detail accessible
→ Candidate cannot apply
```

---

# 18. Security Test

Tối thiểu:

```text
Guest cannot access internal detail
Candidate cannot access internal detail
Unauthorized role cannot bypass with URL
HR cannot change source using crafted request
Draft cannot be exposed through public URL
Closed/Paused public behavior matches policy
CSRF remains required for Edit/Publish mutation
```

---

# 19. UI / UX QA

Kiểm tra:

```text
Long Posting Title
Long Job Description
Long Requirements
Benefits empty
SalaryDisplay empty
No deadline
Expired deadline
Long Requester name
Long Department
Status badge
Mobile/laptop width
Action menu
Breadcrumb
Back navigation
```

---

# 20. Performance / Query QA

Detail page không nên tạo N+1 query không cần thiết.

Kiểm tra:

```text
JobPosting
Source Requisition
Department
Requester
CreatedBy
```

Fetch strategy phải phù hợp.

Không load toàn bộ collection không cần dùng chỉ để render 1 detail page.

---

# 21. Error Handling

Không biến mọi lỗi thành 500.

Phân biệt:

```text
403 → Forbidden
404 → Not Found
400 → Invalid request nếu có
Business validation → proper message/view
500 → unexpected server error
```

---

# 22. Documentation Iteration 3

Update:

```text
Screen Specification — Internal Job Details
Use Case mapping
Business Rules
Security Matrix
System Messages
Integration Test
Security Test
Checklist
```

Nếu detail screen không tạo Use Case riêng theo SRS hiện tại thì giữ mapping:

```text
View Job
```

Không tự tạo UC mới chỉ vì thêm một screen.

---

# 23. Evidence

Chuẩn bị:

```text
Internal Job Detail screenshot
Draft screenshot
Published screenshot
Public Job screenshot
JUnit results
Integration results
Security results
JaCoCo
Database evidence
```

---

# 24. Thứ tự triển khai Iter3

```text
1. Rà lại Iter2 + regression baseline
        ↓
2. Chốt Internal Detail fields/actions
        ↓
3. Service detail response
        ↓
4. Access control
        ↓
5. Controller route
        ↓
6. Internal Job Detail UI
        ↓
7. Source Requisition link
        ↓
8. Public Job link
        ↓
9. Deadline/status handling
        ↓
10. Unit Test
        ↓
11. Controller Test
        ↓
12. Integration Test
        ↓
13. Security Test
        ↓
14. Full regression Iter1 + Iter2
        ↓
15. UI QA
        ↓
16. Documentation + Demo evidence
```

---

# 25. Definition of Done — Iteration 3

```text
[ ] HR mở được Internal Job Detail
[ ] Detail hiển thị đủ thông tin Posting
[ ] Detail hiển thị đúng Source Requisition
[ ] Source Requisition không bị thay đổi từ màn Detail
[ ] Action thay đổi theo Posting Status
[ ] Published có link Public Job
[ ] Draft không bị public
[ ] Deadline hiển thị và xử lý nhất quán
[ ] Unauthorized role bị chặn backend
[ ] 403/404 xử lý đúng
[ ] Unit Test PASS
[ ] Controller Test PASS
[ ] Integration Test PASS
[ ] Security Test PASS
[ ] Regression Iter1/Iter2 PASS
[ ] UI QA hoàn tất
[ ] Documentation đồng bộ
[ ] Có evidence demo
```

---

## Demo cuối Iteration 3

```text
HR
→ Internal Job Management
→ Open Job Posting Detail
→ Xem Source Requisition
→ Xem toàn bộ nội dung Posting
→ Edit Draft
→ Publish
→ Quay lại Internal Detail
→ View Public Job
```

Kèm regression:

```text
Requisition
→ Approval
→ Job Posting
→ Publish
→ Internal Detail
→ Public Job
```

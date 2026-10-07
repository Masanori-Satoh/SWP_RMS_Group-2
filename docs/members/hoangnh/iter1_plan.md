# PLAN ITERATION 1 — HOANGNH
## Job Requisition Management

**Người phụ trách:** Nguyễn Huy Hoàng — HoangNH  
**Iteration:** Iteration 1  
**Phạm vi chính:** Requisition List + Create/Update Requisition + Requisition Details + Approval flow + Clone/Reuse + các rule nghiệp vụ nền

---

# 1. Mục tiêu Iteration 1

Iteration 1 tập trung hoàn thiện đầy đủ nghiệp vụ Job Requisition trước khi chuyển sang Job Posting ở Iteration 2.

**BF-01 Trigger:** A Hiring Manager identifies a hiring need and creates a Job Requisition.

Luồng chính:

```text
Hiring Need
        ↓
Requester / Hiring Manager
        ↓
Create Job Requisition
        ↓
Save Draft hoặc Submit
        ↓
System Validate
        ↓
Director Review
   ├── Reject → Feedback → Edit → Resubmit
   └── Approve
        ↓
Approved Requisition
```

Các chức năng chính của HoangNH:

```text
Requisition List
Create Requisition
Update Requisition
Requisition Details
Save Draft
Submit
Withdraw
Reject
Resubmit
Approve
Clone / Reuse
Search / Filter / Pagination
Approval History
```

---

# 2. Scope màn hình Iteration 1

| Screen / Function | Mục tiêu |
|---|---|
| Requisition List Screen | Xem danh sách Requisition theo role, search/filter/paging |
| Create/Update Requisition Screen | Tạo mới, lưu Draft, sửa Draft/Rejected, Submit |
| Job Requisition Details | Xem đầy đủ thông tin, screening criteria, approval history và action theo status |
| Director Review | Approve hoặc Reject kèm feedback |
| Clone / Reuse Requisition | Tạo Requisition mới từ dữ liệu cũ |

Iteration 1 chưa triển khai:

```text
Internal Job Management
Post / Update Job
Public Job Board integration
Candidate Application
AI Screening
Interview
Offer
Talent Pool
Contract / Joining
```

---

# 3. Business Rules phải chốt

## BR-I1-01 — Requisition Identity

Mỗi Requisition có:

```text
RequisitionId
= khóa kỹ thuật của database

RequisitionCode
= mã nghiệp vụ duy nhất

Title / Position
= không unique

RecruitmentRound
= đợt tuyển dụng nội bộ
```

Ví dụ:

```text
REQ-2026-001
Business Analyst
Round 1

REQ-2026-024
Business Analyst
Round 2
```

Không dùng Title để phân biệt các đợt tuyển.

---

## BR-I1-02 — Requester

Requisition phải lưu rõ người yêu cầu tuyển dụng.

Đề xuất:

```text
RequesterId
```

thay cho việc hard-code mọi Requisition đều thuộc Hiring Manager.

Requester có thể là người có thẩm quyền với Department/Position theo rule doanh nghiệp.

Nếu nhóm chưa chốt Director tự tạo Requisition thì phải chốt trước khi sửa permission.

---

## BR-I1-03 — Department Authorization

Hiring Manager / Requester chỉ được tạo Requisition cho Department thuộc phạm vi mình quản lý.

Frontend:

```text
Department dropdown
→ chỉ hiển thị Department hợp lệ
```

Backend:

```text
System vẫn phải kiểm tra lại quyền Department
```

Không tin dữ liệu từ request.

---

## BR-I1-04 — Save Draft

Save Draft cho phép lưu form chưa hoàn thiện.

Draft validation chỉ kiểm tra:

```text
format
length
numeric range
salary range nếu đã nhập
```

Không bắt full business validation.

---

## BR-I1-05 — Submit

Submit to Director phải validate đầy đủ:

```text
Position
Department
Recruitment Round
Number of Openings
Employment Type
Reason for Hiring
Job Description
Candidate Requirements
Expected Start Date
Screening Criteria
Total Screening Weight = 100%
```

Chỉ khi hợp lệ mới:

```text
Draft / Rejected
→ Pending_Director
```

---

## BR-I1-06 — Screening Criteria

Mỗi criterion:

```text
Criteria Name
Criteria Type
Required Value
Weight
Mandatory
```

Rule:

```text
Criteria name unique trong cùng Requisition
Total Weight = exactly 100%
```

Submit với 99% hoặc 101% phải bị chặn.

---

## BR-I1-07 — Approval

Director chỉ review Requisition ở trạng thái:

```text
Pending_Director
```

Approve:

```text
Pending_Director
→ Approved
```

Reject:

```text
Pending_Director
→ Rejected
```

Reject bắt buộc feedback.

---

## BR-I1-08 — Resubmit

Rejected Requisition được:

```text
Edit
→ Validate
→ Resubmit
→ Pending_Director
```

Giữ cùng `RequisitionId`.

Approval History cũ không bị xóa.

---

## BR-I1-09 — Withdraw

Requester có thể rút Requisition đang Pending nếu business rule nhóm giữ chức năng này:

```text
Pending_Director
→ Draft
```

Sau đó có thể sửa và submit lại.

---

## BR-I1-10 — Clone / Reuse

Clone dùng để tái sử dụng Requisition cũ.

```text
Existing Requisition
        ↓
Clone / Reuse
        ↓
Create NEW Requisition
        ↓
New RequisitionId
New RequisitionCode
New RecruitmentRound
Status = Draft
```

Có thể copy:

```text
Title
Department
Employment Type
Salary Range
Job Description
Candidate Requirements
Screening Criteria
Work Model
Work Location
Probation Duration
```

Không copy:

```text
RequisitionId
RequisitionCode
ApprovalStatus
Approval History
Audit History
CreatedAt
UpdatedAt
```

---

# 4. Recruitment Plan / Quota Integration

Theo feedback mới của giảng viên, Requisition nên có điểm tích hợp với Recruitment Plan.

Ví dụ:

```text
Approved Plan = 40
Already Filled = 25
Reserved / Approved = 5
Remaining Quota = 10

Current Request = 8
→ hợp lệ
```

Nếu:

```text
Current Request = 12
Remaining Quota = 10
```

hệ thống phải cảnh báo hoặc chặn theo rule nhóm chốt.

Nếu module Recruitment Planning chưa được triển khai trong Iter1:

```text
[ ] Chốt interface / dữ liệu cần đọc
[ ] Không hard-code quota giả trong Service
[ ] Ghi dependency rõ trong tài liệu
[ ] Bổ sung integration sau khi module Plan có sẵn
```

---

# 5. Database Update

Rà lại bảng `JobRequisition`.

Các field cơ bản nên có:

```text
RequisitionId
RequisitionCode
Title
DepartmentId
RequesterId
RecruitmentRound
NumberOfPositions
EmploymentType
MinSalary
MaxSalary
ReasonForHiring
JobDescription
RequirementDetails
RequiredGender
ProbationDuration (Số ngày nguyên > 0 khi Submit, Draft tùy chọn)
WorkModel
WorkLocation
ExpectedStartDate
ApprovalStatus
CreatedAt
UpdatedAt
```

Constraint chính:

```text
RequisitionCode UNIQUE
RecruitmentRound > 0
NumberOfPositions > 0
MinSalary >= 0
MaxSalary >= 0
MinSalary <= MaxSalary
```

Lưu ý Draft:

Các field chỉ bắt buộc khi Submit không nên khiến DB chặn việc Save Draft chưa hoàn thiện.

---

# 6. Entity & DTO

## Entity

Rà lại:

```text
JobRequisition
ScreeningCriteria
RequisitionApproval
```

Mapping đúng FK:

```text
Department
Requester
```

## DTO

Khuyến nghị:

```text
CreateRequisitionRequest
UpdateRequisitionRequest
RequisitionResponse
RequisitionDetailResponse
RequisitionDecisionRequest
```

Không cho frontend tự gán:

```text
ApprovalStatus
RequesterId nếu lấy từ current user
CreatedAt
UpdatedAt
RequisitionCode nếu system auto-generate
```

---

# 7. Validation

Tách rõ:

```text
Draft Validation
Submit Validation
Director Decision Validation
```

Các case bắt buộc:

```text
Missing required field
Salary min > max
Negative salary
Openings <= 0
RecruitmentRound <= 0
Invalid Employment Type
Invalid Work Model
Duplicate Screening Criteria
Screening Weight != 100
Reject without feedback
Expected Start Date invalid
Unauthorized Department
```

---

# 8. Service Layer

Các nghiệp vụ chính:

```text
createDraft()
updateRequisition()
submitRequisition()
withdrawRequisition()
approveRequisition()
rejectRequisition()
resubmitRequisition()
cloneRequisition()
findRequisitionById()
searchRequisitions()
```

Service chịu trách nhiệm:

```text
business validation
authorization
status transition
mapping
transaction
approval history
audit
```

Controller phải mỏng.

---

# 9. Requisition List — HM / Requester

Recommended filter:

```text
Search: Requisition Code / Position
Status
Recruitment Round
Department nếu quản lý nhiều phòng
Sort
```

Table:

```text
STT
Requisition Code
Position
Recruitment Round
Department
Openings
Employment Type
Status
Created Date
Actions
```

Action theo status:

```text
Draft
→ View / Edit / Submit / Clone / Delete nếu rule cho phép

Pending_Director
→ View / Withdraw / Clone

Rejected
→ View Feedback / Edit / Resubmit / Clone

Approved
→ View / Clone

Closed / Cancelled
→ View / Clone
```

---

# 10. Requisition Details

Hiển thị:

```text
Requisition Code
Position
Recruitment Round
Requester
Department
Number of Openings
Employment Type
Salary Range
Work Model
Work Location
Probation Duration
Expected Start Date
Gender Requirement
Reason for Hiring
Job Description
Candidate Requirements
Screening Criteria
Approval Status
Approval History
Created At
Updated At
```

Director view thêm:

```text
Approval Actions
Rejection Feedback
Quota / Plan summary nếu module đã có
```

---

# 11. Director Approval / Reject

Flow:

```text
Pending Requisition
        ↓
Director Review
        ↓
Approve?
├── Yes
│    ↓
│  Save Approval
│    ↓
│  Status = Approved
│
└── No
     ↓
   Require Feedback
     ↓
   Save Rejection
     ↓
   Status = Rejected
```

Không cho:

```text
Draft → Approved
Rejected → Approved trực tiếp
Approved → Approved lần nữa
```

---

# 12. Audit / History

Tối thiểu lưu:

```text
CREATE
UPDATE
SUBMIT
WITHDRAW
APPROVE
REJECT
RESUBMIT
CLONE
```

Approval History phải cho biết:

```text
Director
Decision
Comments
Decision Date
```

---

# 13. Unit Test

Tối thiểu:

```text
saveDraft_shouldAllowPartialData()

submit_shouldPass_whenDataValid()

submit_shouldFail_whenRequiredFieldMissing()

submit_shouldFail_whenScreeningWeightNot100()

submit_shouldFail_whenDepartmentUnauthorized()

approve_shouldPass_whenPending()

approve_shouldFail_whenNotPending()

reject_shouldRequireFeedback()

withdraw_shouldReturnPendingToDraft()

resubmit_shouldKeepSameRequisitionId()

clone_shouldCreateNewRequisition()

clone_shouldNotCopyApprovalHistory()

clone_shouldGenerateNewCodeAndRound()
```

---

# 14. Controller Test

Kiểm tra:

```text
GET list
GET create
POST save draft
POST submit
GET detail
GET edit
POST update
POST withdraw
POST approve
POST reject
POST resubmit
POST clone
```

Kiểm tra:

```text
status
redirect
view name
model attributes
validation errors
security
CSRF
```

---

# 15. Integration Test

## Flow 1 — Happy path

```text
HM Create
→ Save Draft
→ Edit
→ Submit
→ Director Approve
→ Approved
```

## Flow 2 — Reject / Resubmit

```text
HM Submit
→ Director Reject + Feedback
→ HM Edit
→ Resubmit
→ Director Approve
```

## Flow 3 — Clone

```text
Approved old Requisition
→ Clone
→ New Draft
→ New Code
→ New RecruitmentRound
→ Old record unchanged
```

## Flow 4 — Unauthorized Department

```text
HM-A
→ create for Department of HM-B
→ blocked
```

---

# 16. Security Test

Tối thiểu:

```text
Guest cannot access internal requisitions
Candidate cannot access internal requisitions
HM cannot edit another HM/requester's requisition
HM cannot approve requisition
Director cannot modify Draft as requester unless business allows
HR cannot modify requisition
Forged requesterId ignored
Forged ApprovalStatus ignored
CSRF required for mutation
Unauthorized Department blocked at backend
```

---

# 17. UI / UX QA

Kiểm tra:

```text
100% browser zoom
Laptop width
Form responsive
Long Position Title
Long Department Name
Validation messages
Screening Criteria add/remove
Total Weight display
Status badges
Action menu
Pagination
Empty state
```

Không để bộ filter tự wrap xấu; chủ động bố trí responsive layout.

---

# 18. Documentation

Update:

```text
SRS
Use Case
Swimlane
ERD
Database Design
Screen Specification
Business Rules
System Messages
Test Cases
```

Swimlane Iter1 phải phản ánh:

```text
Create / Clone
→ Authorization
→ Validate
→ Submit
→ Director Review
→ Approve / Reject
→ Revise / Resubmit
```

Recruitment Plan / Quota chỉ đưa vào nếu business đã chốt.

---

# 19. Thứ tự triển khai Iter1

```text
1. Chốt business rule
        ↓
2. Database update
        ↓
3. Entity / DTO
        ↓
4. Validator
        ↓
5. Department / Requester authorization
        ↓
6. Requisition Service
        ↓
7. Create / Save Draft
        ↓
8. Update
        ↓
9. Submit / Withdraw
        ↓
10. Director Approve / Reject
        ↓
11. Resubmit
        ↓
12. Clone / Reuse
        ↓
13. Requisition List
        ↓
14. Requisition Details
        ↓
15. Unit Test
        ↓
16. Controller Test
        ↓
17. Integration Test
        ↓
18. Security Test
        ↓
19. Regression
        ↓
20. Documentation + Demo
```

---

# 20. Definition of Done — Iteration 1

```text
[ ] Create Requisition hoạt động
[ ] Save Draft cho phép partial data
[ ] Update Draft/Rejected hoạt động
[ ] Submit full validation
[ ] Screening Criteria total = 100%
[ ] Requester/Department permission đúng
[ ] RequisitionCode unique
[ ] RecruitmentRound hoạt động
[ ] Director Approve hoạt động
[ ] Director Reject bắt buộc feedback
[ ] Withdraw hoạt động nếu business giữ
[ ] Resubmit giữ cùng RequisitionId
[ ] Clone tạo record mới
[ ] Clone không copy approval history
[ ] Search/filter/pagination hoạt động
[ ] Audit/history đúng
[ ] Unit Test PASS
[ ] Controller Test PASS
[ ] Integration Test PASS
[ ] Security Test PASS
[ ] Documentation đồng bộ
```

---

## Demo cuối Iteration 1

```text
Hiring Need
→ HM Create Job Requisition
→ Save Draft
→ Edit
→ Submit
→ Director Reject
→ HM xem Feedback
→ Edit / Resubmit
→ Director Approve
→ Requisition = Approved

Sau đó:

Approved Requisition
→ Clone
→ New Draft
→ New RequisitionCode
→ New RecruitmentRound
```

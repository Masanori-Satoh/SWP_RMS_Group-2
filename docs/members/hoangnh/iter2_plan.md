# PLAN ITERATION 2 — HOANGNH
## Job Requisition → Internal Job Management → Job Posting

**Người phụ trách:** Nguyễn Huy Hoàng — HoangNH  
**Iteration:** Iteration 2  
**Phạm vi chính:** Job Requisition integration + Internal Job Management + Post/Update Job + Notification + Public Job handoff + Testing

---

# 1. Mục tiêu Iteration 2

Iteration 2 tập trung hoàn thiện luồng:

```text
Approved Requisition
        ↓
HR xem danh sách requisition được duyệt
        ↓
Create Job Posting
        ↓
Save Draft / Update
        ↓
Publish
        ↓
Public Job Board
        ↓
Notify Requester
```

Iteration 2 **không mở rộng sang**:
- AI Screening / CV Ranking
- Candidate Application
- Interview
- Offer
- Talent Pool
- Contract / Joining
- Recruitment Tracking toàn pipeline
- Recruitment Planning đầy đủ

Các module trên chỉ được tích hợp khi cần, không đưa vào scope code chính của HoangNH trong Iter2.

---

# 2. Điều kiện đầu vào từ Iteration 1

Trước khi code Job Posting cần xác nhận Requisition đã có và hoạt động đúng:

```text
RequisitionId
RequisitionCode
Title / Position
Department
Requester
RecruitmentRound
NumberOfPositions
EmploymentType
Salary Range
JobDescription
RequirementDetails
WorkModel
WorkLocation
ApprovalStatus
CreatedAt
UpdatedAt
```

Các rule bắt buộc:

1. HR chỉ xem Requisition có trạng thái `Approved`.
2. HR không được Create / Edit / Delete / Withdraw / Approve Requisition.
3. Job Posting chỉ được tạo từ Requisition vẫn `Approved`.
4. `RequisitionCode` dùng làm business identifier.
5. `RecruitmentRound` dùng phân biệt các đợt tuyển cùng vị trí.
6. Requester/owner phải lấy từ dữ liệu hệ thống, không tin hidden field từ client.

> Clone/Reuse Requisition, Recruitment Plan/Quota và requester-authority là nghiệp vụ Requisition. Nếu chưa hoàn thiện ở Iter1 thì phải hoàn tất trước hoặc xử lý như dependency, không trộn business logic này vào JobPostingService.

---

# 3. Scope màn hình của HoangNH trong Iter2

| Màn hình / chức năng | Mục tiêu |
|---|---|
| Requisition List for HR | HR chỉ xem Approved Requisition và trạng thái Job Posting liên quan |
| Internal Job Management | HR quản lý danh sách Job Posting nội bộ |
| Post / Update Job Screen | HR tạo Draft, cập nhật và Publish Job Posting |
| Notification integration | Notify Requester khi Requisition bị Reject hoặc Job Posting được Publish |
| Public Job integration | Job Published xuất hiện đúng ở public list/detail |

`Internal Job Details` nếu trong phân công chính thức vẫn thuộc Iter3 thì Iter2 chỉ làm detail tối thiểu phục vụ create/update/publish, không mở rộng ngoài scope.

---

# 4. Business Rules cần chốt trước khi code

## BR-I2-01 — HR Requisition Scope

HR chỉ được:

```text
View Approved Requisition
Create Job Posting from Approved Requisition
```

Không được:

```text
Create Requisition
Edit Requisition
Withdraw Requisition
Approve / Reject Requisition
Delete Requisition
```

---

## BR-I2-02 — Nguồn Job Posting

Job Posting bắt buộc phải có:

```text
RequisitionId → Approved JobRequisition
```

Server phải kiểm tra lại trạng thái Requisition tại thời điểm:

- mở form Create;
- Save Draft;
- Update;
- Publish.

Không chỉ kiểm tra bằng frontend.

---

## BR-I2-03 — Một Requisition và Job Posting

Đề xuất Iter2:

```text
1 Approved Requisition
→ tối đa 1 active Job Posting
```

Active:

```text
Draft
Published
```

Nếu muốn re-post sau `Closed`, phải chốt business rule riêng.

---

## BR-I2-04 — Job Posting độc lập sau khi tạo

Requisition là dữ liệu yêu cầu tuyển nội bộ.

Job Posting là nội dung public.

Sau khi HR chỉnh nội dung Job Posting:

```text
Job Posting Update
≠ Update Job Requisition
```

Không cập nhật ngược Requisition.

---

## BR-I2-05 — Draft và Publish

### Save Draft

Cho phép HR lưu tiến độ.

### Publish

Phải validate đầy đủ:

```text
Posting Title
Job Description
Job Requirements
Work Location
Application Deadline policy
Source Requisition still Approved
Posting Status valid
```

---

## BR-I2-06 — Published Job

Khi publish thành công:

```text
PostingStatus = Published
PostingDate = server time
Audit Log
Notification → Requester
Public Job Board available
```

Không cho client tự gửi:

```text
PostingStatus
PostingDate
CreatedBy
```

---

## BR-I2-07 — Deadline

Đề xuất:

```text
ApplicationDeadline = NULL
→ không có deadline cố định

ApplicationDeadline != NULL
→ phải > thời điểm/ngày publish theo policy đã chốt
```

Tin hết hạn:

- không cho Apply;
- có thể ẩn khỏi danh sách jobs đang tuyển;
- public detail có thể vẫn xem và báo “không còn nhận hồ sơ”.

---

# 5. Phase A — Baseline & Regression

## Công việc

1. Pull/merge branch mới nhất.
2. Kiểm tra diff trước khi sửa.
3. Chạy compile.
4. Chạy Requisition tests hiện có.
5. Sửa test dependency/config nếu đang lỗi.
6. Chuẩn bị test profile riêng.
7. Chuẩn bị fixture:
   - HM-A
   - HM-B
   - Director
   - HR-A
   - HR-B
   - Admin
   - Candidate
8. Có Requisition:
   - Draft
   - Pending_Director
   - Approved
   - Rejected
9. Có Job Posting:
   - Draft
   - Published
   - Paused / Closed nếu schema hiện tại có.

## Done khi

```text
[ ] Project compile
[ ] Test nền chạy được
[ ] Không dùng DB demo làm DB test
[ ] Có fixture role/status cơ bản
```

---

# 6. Phase B — HR Approved Requisition List

## Backend

Cập nhật scope:

```text
HR → Approved only
```

Áp dụng đồng nhất cho:

```text
List
Count
Pagination
Filter
Detail
Create Job Posting source
```

Backend phải chặn:

```text
Draft
Pending_Director
Rejected
```

dù HR tự sửa URL/query.

## UI HR

Recommended columns:

```text
STT
Requisition Code
Position
Recruitment Round
Department
Openings
Requester
Approval Status
Posting Status
Created Date
Action
```

Recommended filter:

```text
Search: Requisition Code / Position
Department
Position
Recruitment Round
Posting Status
Sort
```

Không cần Employment Type ở filter chính nếu UI quá chật.

## Actions

```text
Approved + no posting
→ View
→ Create Job Posting

Approved + posting Draft
→ View
→ Continue Editing Posting

Approved + Published
→ View Posting
```

## Done khi

```text
[ ] HR chỉ thấy Approved
[ ] HR không edit Requisition
[ ] Pagination đúng
[ ] Search/filter server-side
[ ] Action đúng Posting Status
```

---

# 7. Phase C — JobPosting Database & Entity

Rà lại:

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

Indexes cân nhắc:

```text
RequisitionId
PostingStatus
CreatedBy
PostingDate / CreatedAt
```

Constraint phải bảo vệ rule:

```text
JobPosting → valid Requisition
```

Không chỉ dùng `exists()` nếu có nguy cơ concurrent create.

## Done khi

```text
[ ] Schema match Entity
[ ] Migration chạy được
[ ] Dữ liệu cũ không bị xóa
[ ] Index/ràng buộc phù hợp query
```

---

# 8. Phase D — Internal Job Management

Tạo màn:

```text
/internal/job-postings
```

## Backend

Các thành phần đề xuất:

```text
InternalJobPostingController
InternalJobPostingService
JobPostingAccess
JobPostingValidator
JobPostingRepository
InternalJobPostingResponse
```

## Search / Filter

```text
Search: Posting Title / Requisition Code
Department
Posting Status
Sort
Pagination
Rows per page
```

## Table

```text
STT
Posting Title
Source Requisition
Department
Recruitment Round
Status
Deadline
Created By
Created Date
Actions
```

## Done khi

```text
[ ] HR xem dữ liệu thật từ DB
[ ] Search/filter/paging hoạt động
[ ] Role khác không truy cập trái phép
[ ] Empty state rõ ràng
```

---

# 9. Phase E — Create Job Posting

Route đề xuất:

```text
GET /internal/job-postings/create?requisitionId={id}
POST /internal/job-postings/create
```

## GET Create

Server:

```text
Check HR
↓
Find Requisition
↓
Check Approved
↓
Check active posting duplicate
↓
Prefill form
```

Prefill có thể lấy:

```text
Title
Job Description
Requirements
Work Location
Salary information
```

Không tạo Entity trong GET.

## POST Create

Server tự gán:

```text
RequisitionId
CreatedBy
PostingStatus = Draft
CreatedAt
```

Không nhận các field hệ thống từ client.

## Done khi

```text
[ ] Chỉ Approved được dùng
[ ] Invalid source bị block
[ ] Save Draft thành công
[ ] Reload vẫn đúng dữ liệu
[ ] Không tạo duplicate trái rule
```

---

# 10. Phase F — Update Job Posting

Routes:

```text
GET /internal/job-postings/{id}/edit
POST /internal/job-postings/{id}/edit
```

Rule:

```text
Requisition source → read-only
CreatedBy → read-only
PostingDate → server controlled
PostingStatus → không đổi tùy tiện qua form
```

## Done khi

```text
[ ] Update cùng PostingId
[ ] Không đổi nguồn Requisition
[ ] Validation error giữ lại input
[ ] Không ghi đè field hệ thống
```

---

# 11. Phase G — Publish Job Posting

Route:

```text
POST /internal/job-postings/{id}/publish
```

Flow:

```text
HR Publish
↓
Check HR permission
↓
Load Posting + Requisition
↓
Posting must be publishable
↓
Requisition still Approved
↓
Full validation
↓
PostingStatus = Published
PostingDate = server time
↓
Audit Log
↓
Notification Requester
↓
Commit
```

Nếu bất kỳ bước nào fail:

```text
No Published
No false success notification
No partial state
```

## Done khi

```text
[ ] Draft → Published đúng
[ ] Invalid Draft không publish
[ ] double click không tạo kết quả sai/trùng
[ ] requester nhận đúng notification
```

---

# 12. Phase H — Public Job Integration

Phối hợp với DungLT.

Public chỉ được xem:

```text
Published
```

Không được bypass bằng URL:

```text
Draft
Paused
Closed
```

Kiểm tra:

```text
/jobs
/jobs/{id}
Apply availability
Deadline
```

Không để tình trạng:

```text
List chặn expired
Detail vẫn Apply được
```

hoặc ngược lại.

## Done khi

```text
[ ] Published xuất hiện public
[ ] Draft không public
[ ] Closed/Paused không public theo policy
[ ] Deadline đồng nhất list/detail/apply
```

---

# 13. Phase I — Notification Integration

Iter2 cần tối thiểu:

### Event 1

```text
REQUISITION_REJECTED
Director Reject
→ Notify Requester
```

### Event 2

```text
JOB_POSTING_PUBLISHED
HR Publish
→ Notify Requester
```

Notification data:

```text
NotificationId
RecipientUserId
EventType
EventKey
EntityType
EntityId
Title
Body
CreatedAt
ReadAt
```

Rule:

```text
Same event retry
→ không tạo duplicate notification
```

Notification phải đi cùng transaction nghiệp vụ nếu đang dùng cùng DB.

## Done khi

```text
[ ] đúng recipient
[ ] unread count đúng
[ ] mark read đúng owner
[ ] không duplicate
[ ] rollback không để notification giả
```

---

# 14. Phase J — Unit Test

## Requisition integration tests cần giữ

```text
HR Approved-only
Director Approve/Reject
HM Resubmit
Notification on Reject
```

## Job Posting Unit Test

```text
createPosting_shouldCreateDraft_whenSourceApproved()

createPosting_shouldReject_whenSourceNotApproved()

updatePosting_shouldKeepSourceRequisition()

publishPosting_shouldPublish_whenValid()

publishPosting_shouldReject_whenSourceNoLongerApproved()

publishPosting_shouldNotDuplicateNotification_onRetry()
```

---

# 15. Phase K — Integration Test

## Flow 1 — Approve → Publish

```text
HM Submit Requisition
↓
Director Approve
↓
HR sees Approved
↓
HR Create Posting
↓
Save Draft
↓
Edit
↓
Publish
↓
Public Job visible
↓
Requester receives notification
```

## Flow 2 — Reject → Resubmit

```text
HM Submit
↓
Director Reject + Feedback
↓
Requester Notification
↓
HM Edit
↓
Resubmit
↓
Director Approve
```

## Flow 3 — Invalid source

```text
Pending / Rejected Requisition
↓
HR attempts Create Job Posting
↓
Blocked
↓
No JobPosting created
```

---

# 16. Phase L — Security Test

Tối thiểu:

```text
Guest → cannot access internal posting

Candidate → cannot access internal posting

HM → cannot create/update/publish posting

Director → cannot use HR posting endpoints

HR → cannot create from non-Approved source

HR → cannot forge CreatedBy

HR → cannot forge PostingStatus

HR → cannot forge PostingDate

Missing/wrong CSRF → 403

HR cannot read/mark another user's notification
```

---

# 17. Phase M — UI / UX QA

Kiểm tra:

```text
Desktop 100%
Laptop width
Table overflow
Pagination
Empty state
Validation errors
Long title
Long department
Long requester name
Status badges
Action menu
```

HR UI cần khác HM về chức năng nhưng giữ cùng design system.

---

# 18. Phase N — Documentation & Evidence

Update:

```text
SRS
Use Case
Swimlane
Screen Specification
Database Design
Business Rules
System Messages
Integration Test
Security Test
Checklist
```

Evidence:

```text
Screenshots
JUnit result
Integration result
Security result
JaCoCo
DB evidence
Demo accounts
```

---

# 19. Thứ tự code Iter2 đề xuất

```text
1. Baseline + Test profile
        ↓
2. HR Approved-only Requisition
        ↓
3. JobPosting DB / Entity
        ↓
4. JobPosting Access + Repository
        ↓
5. Internal Job Management
        ↓
6. Create Job Posting
        ↓
7. Update Job Posting
        ↓
8. Publish Job Posting
        ↓
9. Notification integration
        ↓
10. Public Job integration
        ↓
11. Unit Test
        ↓
12. Integration Test
        ↓
13. Security Test
        ↓
14. Regression
        ↓
15. Documentation + Demo
```

---

# 20. Definition of Done Iteration 2

```text
[ ] HR chỉ xem Approved Requisition
[ ] HR có Internal Job Management
[ ] HR tạo Job Posting từ Approved Requisition
[ ] HR Save Draft
[ ] HR Update Draft
[ ] HR Publish
[ ] Không tạo Posting từ non-Approved Requisition
[ ] Không thay đổi Requisition khi edit Posting
[ ] Published Job xuất hiện public
[ ] Draft/internal Job không bị lộ public
[ ] Requester nhận notification sau Publish
[ ] Reject notification vẫn hoạt động
[ ] Authorization backend đúng
[ ] CSRF đúng
[ ] Unit Test PASS
[ ] Integration Test PASS
[ ] Security Test PASS
[ ] Regression PASS
[ ] Có JaCoCo/evidence
[ ] SRS/Swimlane/DB/UI/Test docs đồng bộ
```

---

## Kết quả Iter2 cần demo

```text
HM Submit Requisition
        ↓
Director Approve
        ↓
HR nhìn thấy Approved Requisition
        ↓
Create Job Posting
        ↓
Save Draft
        ↓
Edit
        ↓
Publish
        ↓
Job xuất hiện Career Site
        ↓
HM / Requester nhận Notification
```

Đây là demo chính của Iteration 2.

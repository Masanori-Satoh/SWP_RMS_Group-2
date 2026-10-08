# RULE CODE - HOANGNH (UPDATED 2026-10-08)

> Coding Convention & Development Rules for RMS Project  
> Mục tiêu: code rõ ràng, dễ review, dễ test, dễ bảo trì, dễ sửa trực tiếp khi review và thống nhất giữa toàn nhóm.

---

## 0. Phạm vi áp dụng

Áp dụng cho toàn bộ RMS, đặc biệt các module:

- Authentication / User
- Requisition
- Job Posting
- Candidate
- Interview
- Offer
- Notification
- Admin
- Core / Shared

Các rule trong file này ưu tiên theo thứ tự:

1. Business rule đúng.
2. Security / Authorization đúng.
3. Match SRS / Database / UI / Wireframe đã được duyệt.
4. Code dễ đọc, dễ test, dễ sửa.
5. Hạn chế duplicate.
6. Tối ưu chỉ thực hiện sau khi hành vi đúng.

---

# 1. Nguyên tắc chung

- Ưu tiên code dễ đọc, dễ hiểu, dễ maintain hơn code ngắn.
- Mỗi class/method chỉ nên có một trách nhiệm chính.
- Không viết business logic phức tạp trực tiếp trong Controller.
- Không copy-paste logic nếu có thể tái sử dụng hợp lý.
- Không để code chết, import thừa, biến không dùng hoặc commented-out code.
- Tên class, method, biến phải thể hiện đúng ý nghĩa nghiệp vụ.
- Khi sửa nghiệp vụ phải kiểm tra đồng bộ:

```text
Database
→ Entity
→ DTO
→ Validation
→ Repository
→ Service
→ Controller
→ View
→ Test
```

- Không sửa một tầng rồi bỏ quên các tầng còn lại.
- Không tự thêm field/function ngoài SRS, DB, wireframe hoặc business rule đã thống nhất.

---

# 2. Kiến trúc source

RMS dùng **package-by-feature**.

```text
src/main/java/.../
├── auth/
├── user/
├── requisition/
├── candidate/
├── interview/
├── offer/
├── notification/
├── admin/
└── core/
```

Mỗi feature có thể chứa:

```text
controller/
service/
repository/
entity/
dto/
validator/
exception/
mapper/
```

Không quay về cấu trúc toàn project kiểu:

```text
controller/
service/
repository/
```

nếu làm mất ranh giới feature.

`core/` chỉ chứa phần dùng chung thật sự:

- security
- configuration
- global exception
- shared constants
- shared utilities
- common base classes

Không đưa business logic riêng của Requisition / JobPosting vào `core`.

---

# 3. Naming Rules

## 3.1. Biến

Dùng `camelCase`.

Đúng:

```java
candidateName
departmentId
requisitionStatus
jobPostingId
applicationDeadline
```

Không dùng:

```text
a, b, c, d, x, y, tmp, data1, obj, value1
```

Ngoại lệ: `i`, `j` cho vòng lặp đơn giản.

Boolean ưu tiên:

```text
is
has
can
should
```

Ví dụ:

```java
boolean isActive;
boolean hasPermission;
boolean canApprove;
boolean shouldSendNotification;
```

## 3.2. Class

Dùng `PascalCase`.

```java
JobRequisitionService
InternalJobPostingService
CandidateRepository
OfferApprovalRequest
InterviewScheduleResponse
```

## 3.3. Method

Tên phải thể hiện hành động nghiệp vụ.

Nên:

```java
createRequisition()
submitRequisition()
approveRequisition()
rejectRequisition()
publishJobPosting()
findPublishedJobPostingById()
validateScreeningCriteria()
```

Không nên:

```java
process()
handle()
doIt()
run()
updateData()
```

---

# 4. Terminology của RMS

UI tiếng Việt phải dùng thống nhất:

```text
Job Requisition          → Yêu cầu tuyển dụng
Job Posting              → Tin tuyển dụng
Recruitment Round        → Đợt tuyển dụng
Interview Round          → Vòng phỏng vấn
Requester                → Người tạo yêu cầu
Department               → Phòng ban
Screening Criteria       → Tiêu chí sàng lọc
Reason for Hiring        → Lý do tuyển dụng
Application Deadline     → Hạn nộp hồ sơ
```

## 4.1. Đợt tuyển dụng và vòng phỏng vấn

Không được nhầm:

```text
Đợt tuyển dụng 2
├── Sàng lọc hồ sơ
├── Vòng phỏng vấn 1
├── Vòng phỏng vấn 2
└── Phỏng vấn cuối
```

Hiện tại backend/DB có thể vẫn dùng technical field:

```text
DB: RecruitmentRound
Java: recruitmentRound
```

nhưng **UI phải hiển thị "Đợt tuyển dụng"**.

Không rename một mình View hoặc Entity nếu chưa migration đồng bộ.

Nếu sau này đổi technical name sang `RecruitmentCampaignNo`, phải đổi cùng lúc:

```text
DB → Entity → DTO → Repository → Service → View → Test
```

---

# 5. Không Hard-code

Mọi giá trị cố định có ý nghĩa nghiệp vụ phải dùng:

- constant
- enum
- configuration
- message resource
- application properties

Không dùng magic number:

```java
if (screeningWeight != 100) { ... }
```

Nên:

```java
private static final int REQUIRED_SCREENING_WEIGHT = 100;
```

Không hard-code status:

```java
if ("APPROVED".equals(status)) { ... }
```

Nên:

```java
if (requisition.getStatus() == RequisitionStatus.APPROVED) { ... }
```

Không hard-code:

- URL
- email sender
- secret
- API key
- role string
- business threshold

---

# 6. Comment Rules

Comment phải giải thích **WHY / BUSINESS RULE**, không lặp lại code.

Prefix thống nhất:

```text
// Rule:
// Security:
// NOTE:
// TODO:
// FIXME:
```

Ví dụ:

```java
// Rule: screening criteria must total exactly 100% before submission
validateScreeningWeight(criteria);
```

```java
// Security: requester is derived from the authenticated user, never from form input
Long requesterId = currentUserService.getCurrentUserId();
```

```java
// NOTE: public queries must filter Published status to prevent direct URL exposure
JobPosting posting = findPublishedPosting(id);
```

Không comment kiểu:

```java
// Set status to approved
requisition.setStatus(RequisitionStatus.APPROVED);
```

Không giữ code cũ bằng comment. Git đã lưu lịch sử.

Comment phải:

- ngắn, thường 1–2 dòng;
- đặt gần logic;
- không lỗi thời;
- không chứa password/token/secret;
- ưu tiên English trong source code.

---

# 7. If / Else Rules

- Điều kiện phải dễ đọc.
- Không viết `if` quá dài.
- Business rule phức tạp phải tách method.
- Ưu tiên Guard Clause.

Nên:

```java
if (!requisitionAccessService.canManage(user, requisition)) {
    throw new AccessDeniedException();
}

if (requisition.getStatus() != RequisitionStatus.DRAFT) {
    throw new RequisitionNotEditableException();
}

updateRequisition(requisition);
```

---

# 8. Controller Rules

Controller chỉ làm:

```text
Receive Request
→ Bind DTO
→ @Valid
→ gọi Service
→ trả View / Redirect / Response
```

Controller không:

- kiểm tra quota phức tạp;
- tự quyết định state transition;
- update Entity trực tiếp;
- gọi Repository trực tiếp;
- tạo Notification/Audit business logic;
- chứa nhiều `try-catch` business exception.

Ví dụ:

```java
@PostMapping("/{id}/approve")
public String approveRequisition(
        @PathVariable Long id,
        @Valid ApproveRequisitionRequest request) {

    requisitionService.approveRequisition(id, request);
    return "redirect:/requisitions/" + id;
}
```

---

# 9. Service Rules

Service xử lý:

- business rule;
- authorization nghiệp vụ;
- transaction;
- state transition;
- mapping / phối hợp Entity;
- phối hợp nhiều repository/service;
- audit;
- notification trigger.

Các method ghi dữ liệu quan trọng dùng:

```java
@Transactional
```

Read-only:

```java
@Transactional(readOnly = true)
```

Không để một service method 100–150 dòng nếu có thể tách thành method nghiệp vụ rõ nghĩa.

---

# 10. Repository Rules

Repository chỉ truy cập dữ liệu.

Nên:

```java
Optional<JobRequisition> findById(Long id);

Page<JobRequisition> findByDepartmentId(
        Long departmentId,
        Pageable pageable);
```

Không:

```java
boolean checkIfDirectorCanApproveAndSendMailAndUpdateStatus(...);
```

Business rule không đặt trong Repository.

---

# 11. DTO Rules

Không bind Entity trực tiếp từ form/API nếu có thể tránh.

Nên dùng:

```text
CreateRequisitionRequest
UpdateRequisitionRequest
ApproveRequisitionRequest
RejectRequisitionRequest
RequisitionResponse

CreateJobPostingRequest
UpdateJobPostingRequest
JobPostingResponse
JobPostingDetailResponse
```

Code mới ưu tiên Java `record` khi phù hợp.

Không nhận từ frontend các field hệ thống như:

```text
approvalStatus
postingStatus
createdBy
createdAt
postingDate
requesterId
```

nếu server phải tự xác định.

---

# 12. Validation Rules

Validation có 3 tầng:

```text
HTML basic validation
→ Bean Validation
→ Business Validation
```

## 12.1. HTML

Dùng đúng khi phù hợp:

```text
required
maxlength
min
max
type="date"
type="email"
accept
```

Frontend validation chỉ hỗ trợ UX, **không thay backend validation**.

## 12.2. Bean Validation

Ví dụ:

```java
@NotBlank
@Size(max = 200)
String postingTitle;

@NotNull
@Positive
Integer numberOfPositions;
```

## 12.3. Business Validation

Đặt trong Service hoặc Validator riêng.

Ví dụ:

```java
validateSalaryRange(minSalary, maxSalary);
validateScreeningWeight(criteria);
validateManagedDepartment(userId, departmentId);
validateApprovedRequisition(requisition);
```

## 12.4. Case bắt buộc test

Mọi form phải nghĩ tới:

```text
null
blank ""
space-only "    "
min length
max length
max length + 1
invalid format
negative number
zero
boundary value
invalid date
duplicate
forged ID
unauthorized value
```

Nếu có email/mobile/file:

```text
email sai format
mobile sai format
file sai extension
file sai MIME/type
file vượt size
```

Không để input vượt DB length rồi mới lỗi SQL.

Quy tắc:

```text
HTML maxlength
=
DTO @Size
=
DB column length
```

---

# 13. Exception Rules

Không:

```java
throw new RuntimeException("Error");
```

Dùng custom exception có nghĩa:

```java
RequisitionNotFoundException
RequisitionNotEditableException
RejectionReasonRequiredException
JobPostingNotFoundException
JobPostingNotPublishableException
ApplicationDeadlineInvalidException
```

Business exception nên kế thừa:

```java
BaseBusinessException
```

Controller không tự `try-catch` nếu đã có `GlobalExceptionHandler`.

---

# 14. Status / State Transition

Không update status tùy ý từ client.

## Requisition

Ví dụ:

```text
DRAFT
→ PENDING_APPROVAL

PENDING_APPROVAL
→ APPROVED
→ REJECTED
→ DRAFT (withdraw, nếu business cho phép)

REJECTED
→ PENDING_APPROVAL
```

Không cho:

```text
DRAFT → APPROVED
APPROVED → DRAFT
```

nếu business rule không cho phép.

## Job Posting

```text
DRAFT
→ PUBLISHED

PUBLISHED
→ PAUSED
→ CLOSED

PAUSED
→ PUBLISHED
→ CLOSED
```

Không bind `postingStatus` trực tiếp từ form.

---

# 15. Security / Authorization

- Không tin dữ liệu frontend.
- Frontend `th:if` không thay backend authorization.
- Owner/current user lấy từ authentication/session.
- Không dùng `requesterId`, `createdBy` từ client nếu server phải tự xác định.
- Không cho user sửa dữ liệu ngoài scope.
- Không đưa secret/password/token vào log.

Ví dụ:

```java
Long currentUserId = authenticationService.getCurrentUserId();
```

---

# 16. Optional / Null

Không dùng:

```java
optional.get();
```

khi chưa chắc có giá trị.

Nên:

```java
JobRequisition requisition = requisitionRepository.findById(id)
        .orElseThrow(() -> new RequisitionNotFoundException(id));
```

Không trả `null` collection nếu có thể trả list rỗng.

---

# 17. Logging

Không:

```java
System.out.println();
```

Dùng logger:

```java
log.info("Requisition {} submitted by user {}", requisitionId, userId);
log.warn("Unauthorized requisition access: user={}, requisition={}", userId, requisitionId);
```

Không log:

- password
- OTP
- access token
- API key
- secret
- dữ liệu nhạy cảm không cần thiết

---

# 18. Message Rules

Không hard-code cùng một message ở nhiều nơi.

Dùng:

- constant;
- message resource;
- properties/i18n.

UI hiện tại dùng tiếng Việt thì validation/success/error message phải thống nhất tiếng Việt.

---

# 19. Frontend / Thymeleaf Rules

- Không viết inline CSS nếu có thể tránh.
- Không viết JavaScript dài trực tiếp trong HTML.
- CSS đặt trong `static/css/`.
- JS đặt trong `static/js/`.
- Form phải hiển thị validation message rõ ràng.
- UI terminology phải match SRS / wireframe / business terminology.

## 19.1. Shared Layout

Header/sidebar/footer/menu chỉ code một lần:

```text
templates/fragments/
├── header.html
├── sidebar.html
├── footer.html
└── messages.html
```

Không copy-paste sidebar sang từng trang.

## 19.2. Shared Screen

Nếu cùng function và UI tương tự cho nhiều role:

```text
1 screen
+ role-based data scope
+ role/status-based actions
+ backend authorization
```

Không tạo:

```text
hm/requisition-detail.html
director/requisition-detail.html
hr/requisition-detail.html
```

nếu cấu trúc giống nhau.

## 19.3. Create / Update

Nếu Create và Update dùng cùng form structure:

```text
requisitions/form.html
job-postings/form.html
```

Không tạo hai HTML gần như giống nhau.

## 19.4. Form Layout

- Textarea/long content full width.
- Read-only field có visual khác editable field.
- Validation message nằm gần field.

---

# 20. List Screen Rules

Mọi màn hình dạng bảng dữ liệu bắt buộc có:

```text
Search
Filter
Sort
Paging
```

Ví dụ:

```text
Requisition List
Job Posting Management
Candidate List
Interview Schedule List
Offer Management
```

Ưu tiên xử lý server-side:

```text
Query params
→ Controller
→ Service
→ Pageable / Specification / Repository
→ Database
```

Không:

```text
findAll()
→ load toàn bộ
→ JavaScript tự filter/page
```

Sort field phải whitelist.

---

# 21. Reuse / Duplicate Rules

Nếu cùng logic xuất hiện từ 2 lần trở lên, xem xét tách:

```text
private method
shared service
validator
mapper
fragment
utility
constant
```

Không tách quá mức làm code khó đọc.

Đặc biệt:

- cùng function nhiều role → dùng chung source;
- cùng form create/update → reuse;
- cùng job content giữa Internal Detail và Public Detail → reuse fragment/component;
- header/footer/sidebar → reuse.

---

# 22. SQL Server / Database Rules

RMS hiện tại tiếp tục dùng **SQL Server**.

Không tự chuyển sang MySQL khi chưa có quyết định mới.

Các kiểu SQL Server hiện có như:

```text
IDENTITY(1,1)
NVARCHAR
DATETIME2
SYSDATETIME()
BIT
```

được giữ nếu phù hợp schema hiện tại.

Tên field phải thống nhất:

```text
Database
Entity
DTO
HTML
Test
```

Nếu DB là:

```text
PostingTitle NVARCHAR(200)
```

thì DTO/View phải giới hạn đúng 200 ký tự trước khi chạm DB.

Không phụ thuộc vào DB exception cho validation người dùng.

---

# 23. Requisition Rules

## 23.1. Authorization

- HM/Requester chỉ thao tác requisition trong scope được phép.
- Director approve/reject theo quyền.
- HR không sửa nội dung requisition nếu business rule không cho.

## 23.2. Submit

Submit phải kiểm tra:

- required fields;
- blank/space;
- salary range;
- number of positions;
- department authorization;
- screening criteria;
- quota/authority nếu feature đang áp dụng.

## 23.3. Screening Criteria

Trước Submit:

```text
Total Weight = 100%
```

Rule phải ở backend, không chỉ JS.

## 23.4. Clone / Reuse

Clone tạo record mới.

Không copy:

```text
RequisitionId
ApprovalStatus
Approval History
Audit History
CreatedAt
UpdatedAt
Rejection Feedback
```

---

# 24. Job Posting Rules

## 24.1. Nguồn tạo

Job Posting chỉ được tạo từ **Approved Requisition**.

```java
// Rule: only approved requisitions can be used as the source of a job posting
validateApprovedRequisition(requisition);
```

## 24.2. Source Requisition

Trong Create/Update Job Posting:

Thông tin nguồn được hiển thị read-only, ví dụ:

```text
Requisition Code
Position
Department
Đợt tuyển dụng
Requester
Number of Positions
Employment Type
Work Model
Expected Start Date
Work Location
Required Gender
Probation Duration
Min/Max Salary
Reason for Hiring
```

`Reason for Hiring`:

- vẫn hiển thị cho HR;
- read-only;
- là thông tin internal;
- không copy thành public JobPosting content;
- không hiển thị cho Guest/Candidate.

## 24.3. Field JobPosting editable

HR chỉ chỉnh các field phù hợp bảng `JobPosting`:

```text
PostingTitle
JobDescription
JobRequirements
Benefits
SalaryDisplay
WorkLocation
ApplicationDeadline
```

Không cho nhập trực tiếp:

```text
PostingStatus
PostingDate
CreatedBy
CreatedAt
UpdatedAt
RequisitionId nguồn sau khi đã tạo
```

## 24.4. Save Draft

```text
Save Draft
→ PostingStatus = Draft
→ PostingDate = null
```

Draft có thể cho phép thiếu một số field nếu business rule đã chốt như vậy.

## 24.5. Publish

```text
Publish
→ full validation
→ source requisition vẫn Approved
→ PostingStatus = Published
→ PostingDate = server time
→ Audit
→ Notification nếu có
```

## 24.6. Update

Update JobPosting không được update ngược `JobRequisition`.

## 24.7. Public Access

Guest/Candidate chỉ được đọc:

```text
PostingStatus = Published
```

Không chỉ hide nút ở frontend.

Query/service public phải tự filter Published để direct URL không lộ Draft/Paused/Closed nếu policy không cho.

---

# 25. Internal Job Detail / Public Job Detail Reuse

Phần nội dung công khai nên reuse giữa:

```text
Internal Job Posting Detail
Public Job Detail
```

Có thể tách Thymeleaf fragment:

```text
job-postings/fragments/job-content.html
```

Shared content:

- title
- department
- job description
- requirements
- benefits
- salary display
- work location
- employment type (nếu lấy từ requisition)
- deadline

Internal Detail được phép thêm:

- PostingStatus
- PostingDate
- CreatedBy
- CreatedAt
- UpdatedAt
- Source Requisition
- Activity History
- management actions

Public Detail không hiển thị dữ liệu internal.

---

# 26. Test Rules

Mỗi business rule quan trọng phải có test.

Tối thiểu:

```text
Happy path
Invalid input
Blank
Space-only
Length boundary
Invalid format
Invalid status
Unauthorized access
Not found
Boundary value
Duplicate data
Direct URL access
```

Tên test mô tả behavior:

```java
submitRequisition_shouldChangeStatusToPending_whenDataIsValid()

approveRequisition_shouldThrowException_whenStatusIsDraft()

publishJobPosting_shouldReject_whenSourceRequisitionIsNotApproved()

createJobPosting_shouldRejectWhitespaceOnlyTitle()
```

Không:

```java
test1()
test2()
testApprove()
```

---

# 27. Integration Rules của nhóm

Source phải được tích hợp vào **source chung**.

- Merge code vào `main` thường xuyên, ưu tiên hằng ngày.
- Demo chỉ chạy từ một bản source đã tích hợp.
- Không mỗi member demo trên project/source riêng.
- Không để đến sát ngày review mới merge.
- Navigation giữa các màn phải đi bằng menu/button/link của hệ thống, không phụ thuộc vào việc gõ URL thủ công.
- CSS/layout/menu/header phải thống nhất toàn hệ thống.
- Code phải đủ modular để có thể sửa trực tiếp khi giảng viên yêu cầu mà không phải tìm logic rải rác nhiều nơi.

---

# 28. Wireframe / UI Implementation Rules

Trước khi implement màn mới hoặc redesign lớn:

```text
Wireframe
→ GV confirm
→ Implement
```

Wireframe chỉ tập trung:

- screen structure;
- information hierarchy;
- navigation;
- fields;
- table columns;
- actions.

Không cần nhồi business explanation dài vào wireframe.

Mọi list screen phải thể hiện:

```text
Search + Filter + Sort + Paging + Actions
```

Actions trong table có thể dùng:

```text
⋮
```

và dropdown chứa action theo role/status.

---

# 29. Git / Commit Convention

Nên:

```text
feat(requisition): add recruitment campaign filter
fix(requisition): reject whitespace-only position title
feat(jobposting): add publish workflow
test(jobposting): add invalid deadline tests
refactor(ui): reuse shared sidebar fragment
```

Không:

```text
update
fix
code
abc
final
final2
```

---

# 30. TODO / FIXME

TODO phải ghi rõ việc:

```java
// TODO: add email retry after notification outbox is implemented
```

Bug:

```java
// FIXME: pagination count currently includes inactive requisitions
```

Không:

```java
// TODO
```

---

# 31. Checklist trước khi Push

```text
[ ] Pull/merge latest main trước khi bắt đầu tích hợp
[ ] Code compile
[ ] Không còn lỗi IDE quan trọng
[ ] Không còn System.out.println
[ ] Không còn biến vô nghĩa
[ ] Không còn hard-code business value
[ ] Không còn import thừa
[ ] Không còn commented-out code
[ ] Validation blank/space/length đã kiểm tra
[ ] Authorization backend đã kiểm tra
[ ] Status transition hợp lệ
[ ] Search/filter/sort/paging hoạt động nếu là list screen
[ ] Test liên quan PASS
[ ] Không commit password/API key/token
[ ] Không commit file build thừa
[ ] Match SQL Server schema
[ ] Match SRS
[ ] Match wireframe/mockup đã duyệt
[ ] Match terminology tiếng Việt
[ ] Không phá shared layout/header/sidebar
```

---

# 32. Checklist Review Code

Review theo thứ tự:

1. Business rule đúng chưa?
2. Authorization đúng chưa?
3. Validation có chặn trước DB chưa?
4. Blank/space/max length đã xử lý chưa?
5. State transition hợp lệ chưa?
6. Controller có mỏng không?
7. Business logic có nằm ở Service/Validator không?
8. Có duplicate source/template không?
9. Shared UI có được reuse không?
10. Search/filter/sort/paging có server-side hợp lý không?
11. Exception có đúng loại không?
12. Transaction có hợp lý không?
13. Có test cho rule mới không?
14. Code có match SQL Server / SRS / Wireframe / UI không?


---

# 33. Nguyên tắc quan trọng nhất

> Code phải được viết để thành viên khác đọc vào có thể hiểu nghiệp vụ mà không cần hỏi lại người viết.

> Một business rule chỉ nên có một nơi chịu trách nhiệm chính.

> Một function dùng cho nhiều role thì ưu tiên một source dùng chung và phân quyền theo role/status.

> Các màn Create/Update hoặc UI tương tự phải ưu tiên reuse.

> Validation phải chặn dữ liệu sai trước khi lỗi tới SQL Server.

> Không chỉ làm cho code chạy — code phải rõ ràng, test được, maintain được và sửa nhanh khi review.


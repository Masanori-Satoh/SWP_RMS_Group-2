# RULE CODE - HOANGNH

> Coding Convention & Development Rules for RMS Project  
> Mục tiêu: code rõ ràng, dễ review, dễ test, dễ bảo trì và thống nhất giữa các thành viên trong nhóm.

## 1. Nguyên tắc chung

- Ưu tiên code dễ đọc, dễ hiểu, dễ maintain hơn viết ngắn.
- Mỗi class/method chỉ nên chịu trách nhiệm cho một mục đích chính.
- Không viết business logic phức tạp trực tiếp trong Controller.
- Không copy-paste logic nếu có thể tái sử dụng hợp lý.
- Không để code chết, import thừa, biến không dùng hoặc comment code cũ.
- Tên class, method, biến phải thể hiện rõ ý nghĩa nghiệp vụ.
- Khi sửa nghiệp vụ phải kiểm tra đồng bộ: Entity → DTO → Validation → Service → Controller → View → Test.

## 2. Quy tắc đặt tên

### 2.1. Không đặt tên biến vô nghĩa

Không được dùng:

```java
var c = candidateRepository.findById(id);
var d = departmentRepository.findById(departmentId);
var x = requisition.getStatus();
String a = request.getTitle();
```

Phải dùng tên có nghĩa:

```java
Candidate candidate = candidateRepository.findById(candidateId);
Department department = departmentRepository.findById(departmentId);
RequisitionStatus requisitionStatus = requisition.getStatus();
String positionTitle = request.getPositionTitle();
```

Tránh các tên như:

```text
a, b, c, d, x, y, tmp, temp1, data1, obj, object1, test1, value1
```

Ngoại lệ: biến vòng lặp đơn giản như `i`, `j`.

### 2.2. Tên biến

Dùng `camelCase`.

```java
candidateName
departmentId
requisitionStatus
officialSalary
recruitmentRound
```

Boolean nên bắt đầu bằng:

```text
is, has, can, should
```

Ví dụ:

```java
boolean isActive;
boolean hasPermission;
boolean canApprove;
boolean shouldSendEmail;
```

### 2.3. Tên class

Dùng `PascalCase`.

```java
JobRequisitionService
JobRequisitionController
CandidateRepository
OfferApprovalRequest
InterviewScheduleResponse
```

### 2.4. Tên method

Method phải mô tả đúng hành động nghiệp vụ.

Nên dùng:

```java
createRequisition()
submitRequisition()
approveRequisition()
rejectRequisition()
findRequisitionById()
validateScreeningCriteria()
sendOfferEmail()
```

Không nên dùng tên mơ hồ:

```java
process()
handle()
doIt()
run()
updateData()
```

## 3. Không Hard-code trong logic nghiệp vụ

Mọi giá trị cố định có ý nghĩa nghiệp vụ phải được đưa thành:

- `static final`
- `enum`
- configuration
- message constant
- application properties

tùy trường hợp.

### 3.1. Không dùng Magic Number

Sai:

```java
if (screeningWeight != 100) {
    throw new BusinessException("Invalid weight");
}
```

Đúng:

```java
private static final int REQUIRED_SCREENING_WEIGHT = 100;

if (screeningWeight != REQUIRED_SCREENING_WEIGHT) {
    throw new BusinessException("Invalid weight");
}
```

Ví dụ với lương thử việc:

```java
private static final BigDecimal MIN_PROBATION_SALARY_RATE =
        new BigDecimal("0.85");
```

### 3.2. Không Hard-code String nghiệp vụ

Sai:

```java
if (requisition.getStatus().equals("APPROVED")) {
    ...
}
```

Đúng:

```java
if (requisition.getStatus() == RequisitionStatus.APPROVED) {
    ...
}
```

Dùng `enum`:

```java
public enum RequisitionStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED
}
```

### 3.3. Không Hard-code URL / Email / Config

Sai:

```java
String apiUrl = "https://api.example.com/send-mail";
String sender = "hr@company.com";
```

Đúng:

```properties
mail.sender=hr@company.com
mail.api-url=https://api.example.com/send-mail
```

## 4. Quy tắc Comment

Comment phải ngắn gọn, giải thích **tại sao** có logic đó hoặc business rule nào đang được áp dụng.

Không comment lại đúng điều code đã nói.

### 4.1. Phải có comment ngắn trước nhánh rẽ quan trọng

```java
// Draft can be saved without full submission validation
if (request.isSaveDraft()) {
    saveDraft(request);
    return;
}
```

```java
// Only pending requisitions can be approved
if (requisition.getStatus() != RequisitionStatus.PENDING_APPROVAL) {
    throw new InvalidRequisitionStatusException();
}
```

```java
// Reject action requires Director feedback
if (StringUtils.isBlank(request.rejectionReason())) {
    throw new RejectionReasonRequiredException();
}
```

```java
// HM can access only departments under their responsibility
if (!departmentAccessService.canManage(currentUserId, departmentId)) {
    throw new AccessDeniedException();
}
```

### 4.2. Không comment thừa

Không nên:

```java
// Set status to approved
requisition.setStatus(RequisitionStatus.APPROVED);
```

Nên:

```java
// Approval is final; approved requisitions can no longer be edited by HM
requisition.setStatus(RequisitionStatus.APPROVED);
```

### 4.3. Comment cho Business Rule

Với rule quan trọng, ưu tiên format:

```java
// Rule: <mô tả ngắn gọn business rule>
```

Ví dụ:

```java
// Rule: Screening criteria must total exactly 100% before submission
validateScreeningCriteriaWeight(criteria);
```

```java
// Rule: HM can create requisitions only for managed departments
validateManagedDepartment(currentUserId, departmentId);
```

## 5. Quy tắc If / Else

- Điều kiện phải dễ đọc.
- Không viết `if` quá dài.
- Business rule phức tạp phải tách thành method.
- Ưu tiên return sớm để giảm nested if.

Không nên:

```java
if (user != null && user.getRole() != null
        && user.getRole().equals("HM")
        && requisition != null
        && requisition.getDepartment() != null
        && requisition.getDepartment().getManagerId().equals(user.getId())) {
    ...
}
```

Nên:

```java
boolean canManageRequisition =
        requisitionAccessService.canManage(user, requisition);

if (!canManageRequisition) {
    throw new AccessDeniedException();
}
```

Ưu tiên Guard Clause:

```java
if (requisition == null) {
    throw new RequisitionNotFoundException();
}

// Only draft requisitions are editable
if (requisition.getStatus() != RequisitionStatus.DRAFT) {
    throw new RequisitionNotEditableException();
}

updateRequisition(requisition);
```

## 6. Controller Rules

Controller chỉ nên:

1. Nhận request.
2. Validate input.
3. Gọi Service.
4. Trả response/view.

Controller không chứa business logic lớn.

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

## 7. Service Rules

Service là nơi xử lý:

- Business rule
- Transaction
- Authorization nghiệp vụ
- State transition
- Mapping Entity
- Phối hợp giữa các feature/service khác

Ví dụ:

```java
@Transactional
public void approveRequisition(
        Long requisitionId,
        ApproveRequisitionRequest request) {

    JobRequisition requisition = findRequisition(requisitionId);

    // Only pending requisitions can be approved
    validateApprovalStatus(requisition);

    requisition.approve(request.approvedBy());
    requisitionRepository.save(requisition);
}
```

## 8. Repository Rules

Repository chỉ chịu trách nhiệm truy cập dữ liệu.

Không đặt business logic trong Repository.

Nên:

```java
Optional<JobRequisition> findById(Long id);

Page<JobRequisition> findByDepartmentId(
        Long departmentId,
        Pageable pageable);
```

Không nên:

```java
boolean checkIfDirectorCanApproveAndSendMailAndUpdateStatus(...);
```

## 9. DTO Rules

Không truyền Entity trực tiếp giữa Form/API và Controller nếu có thể tránh.

Dùng DTO rõ ràng:

```text
CreateRequisitionRequest
UpdateRequisitionRequest
ApproveRequisitionRequest
RequisitionResponse
```

Với code mới, ưu tiên Java `record` khi phù hợp.

```java
public record CreateRequisitionRequest(
        String positionTitle,
        Long departmentId,
        Integer recruitmentRound
) {
}
```

## 10. Validation Rules

Input validation dùng annotation:

```java
@NotBlank
@Size(max = 100)
@Email
@NotNull
@Positive
```

Business validation đặt trong Service hoặc Validator chuyên trách.

```java
// Screening criteria must total exactly 100 percent before submission
validateScreeningCriteriaWeight(criteria);
```

## 11. Exception Rules

Không dùng:

```java
throw new RuntimeException("Error");
```

Phải dùng custom exception có ý nghĩa:

```java
throw new RequisitionNotFoundException(requisitionId);
throw new RequisitionNotEditableException(requisitionId);
throw new RejectionReasonRequiredException();
```

Business exception nên kế thừa:

```java
BaseBusinessException
```

Controller không tự `try-catch` business exception nếu đã có `GlobalExceptionHandler`.

## 12. Status / State Transition Rules

Không update status tùy ý.

Mỗi thay đổi trạng thái phải kiểm tra trạng thái hiện tại.

Ví dụ:

```text
DRAFT
→ PENDING_APPROVAL

PENDING_APPROVAL
→ APPROVED
→ REJECTED
→ DRAFT (withdraw)

REJECTED
→ PENDING_APPROVAL
```

Không cho phép:

```text
DRAFT → APPROVED
APPROVED → DRAFT
```

nếu business rule không cho phép.

## 13. Transaction Rules

Method thay đổi dữ liệu quan trọng nên có:

```java
@Transactional
```

Method chỉ đọc dữ liệu nên ưu tiên:

```java
@Transactional(readOnly = true)
```

## 14. Optional / Null Rules

Không gọi:

```java
optional.get();
```

khi chưa chắc chắn có dữ liệu.

Không nên:

```java
JobRequisition requisition =
        requisitionRepository.findById(id).get();
```

Nên:

```java
JobRequisition requisition =
        requisitionRepository.findById(id)
                .orElseThrow(() ->
                        new RequisitionNotFoundException(id));
```

Không trả `null` cho collection nếu có thể trả list rỗng.

```java
return Collections.emptyList();
```

## 15. Logging Rules

Không dùng:

```java
System.out.println();
```

Dùng logger:

```java
log.info("Requisition {} submitted by user {}", requisitionId, userId);
log.warn("Unauthorized requisition access: user={}, requisition={}",
        userId, requisitionId);
log.error("Email submission failed for offer {}", offerId, exception);
```

Không log:

- password
- OTP
- access token
- dữ liệu bí mật không cần thiết

## 16. Security Rules

- Không tin dữ liệu gửi từ frontend.
- Permission phải kiểm tra lại ở backend.
- Không dùng ID từ request để tự quyết định owner nếu owner phải lấy từ session.
- Không để user sửa dữ liệu ngoài phạm vi quyền.
- Không đưa password/OTP/token vào log.

Ví dụ:

```java
Long currentUserId = authenticationService.getCurrentUserId();

// Owner is determined from authenticated user, not from client input
requisition.setCreatedBy(currentUserId);
```

## 17. Message Rules

Không hard-code message ở nhiều Controller/Service.

Không nên:

```java
redirectAttributes.addFlashAttribute(
        "success",
        "Job requisition submitted successfully");
```

Nên đưa về constant hoặc resource:

```java
public static final String MSG_REQUISITION_SUBMITTED =
        "Job requisition submitted for approval.";
```

hoặc:

```properties
MSG13=Job requisition submitted for approval.
```

## 18. Frontend Rules

- Không viết CSS inline nếu có thể tránh.
- Không viết JavaScript dài trực tiếp trong HTML.
- Dùng `static/css/` và `static/js/`.
- Không dùng frontend authorization thay cho backend authorization.
- Form phải hiển thị validation message rõ ràng.
- UI phải dùng cùng terminology với backend/SRS.

## 19. Test Rules

Mỗi business rule quan trọng phải có test.

Tối thiểu kiểm tra:

```text
Happy path
Invalid input
Invalid status
Unauthorized access
Not found
Boundary value
Duplicate data
```

Tên test phải mô tả behavior.

Nên:

```java
submitRequisition_shouldChangeStatusToPending_whenDataIsValid()
approveRequisition_shouldThrowException_whenStatusIsDraft()
```

Không nên:

```java
test1()
test2()
testApprove()
```

## 20. Method Size Rules

Nếu một method quá dài hoặc làm nhiều việc, phải tách nhỏ.

Không nên:

```java
public void submitRequisition(...) {
    // 150 dòng xử lý
}
```

Nên:

```java
public void submitRequisition(...) {
    JobRequisition requisition = findRequisition(...);

    validateOwnership(requisition);
    validateSubmissionStatus(requisition);
    validateRequiredFields(requisition);
    validateScreeningCriteria(requisition);

    markAsPendingApproval(requisition);
    saveRequisition(requisition);
    createAuditLog(requisition);
    notifyDirector(requisition);
}
```

## 21. Không tạo code duplicate

Nếu cùng logic xuất hiện từ 2 lần trở lên, xem xét tách thành:

```text
private method
shared service
validator
mapper
utility
constant
```

Không tách quá mức làm code khó đọc.

## 22. Database Field Rules

Tên field phải thống nhất giữa:

```text
Database
Entity
DTO
HTML
Test
```

Ví dụ:

```text
DB: RecruitmentRound
Entity: recruitmentRound
DTO: recruitmentRound
HTML: recruitmentRound
```

## 23. TODO Convention

Nếu chưa làm xong:

```java
// TODO: Add async email retry after notification module is completed
```

Nếu là bug:

```java
// FIXME: Current pagination count includes inactive requisitions
```

Không để:

```java
// TODO
```

mà không ghi rõ nội dung.

## 24. Git / Commit Convention

Nên:

```text
feat(requisition): add recruitment round
fix(requisition): validate screening weight before submit
test(requisition): add submit integration tests
refactor(auth): extract OTP validation service
```

Không nên:

```text
update
fix
code
abc
final
final2
```

## 25. Quy tắc trước khi Push Code

```text
[ ] Code compile thành công
[ ] Không còn lỗi IDE quan trọng
[ ] Không còn System.out.println
[ ] Không còn biến vô nghĩa
[ ] Không còn hard-code business value
[ ] Không còn import thừa
[ ] Không còn code comment cũ
[ ] Test liên quan PASS
[ ] Không commit password/API key
[ ] Không commit file build thừa
[ ] Không phá convention của module hiện tại
```

## 26. Quy tắc ưu tiên khi Review Code

1. Business rule có đúng không?
2. Authorization có đúng không?
3. Có hard-code không?
4. Tên biến/method có rõ nghĩa không?
5. Controller có chứa business logic không?
6. Service có quá dài không?
7. Exception có đúng loại không?
8. Status transition có hợp lệ không?
9. Có test cho rule mới không?
10. Code có match SRS/Database/UI không?

## 27. Checklist bắt buộc

```text
[ ] Naming rõ nghĩa
[ ] Không magic number / magic string
[ ] Hard-code đã chuyển sang constant/config/enum
[ ] Có comment ngắn trước business branch quan trọng
[ ] Controller mỏng
[ ] Business logic nằm ở Service
[ ] Có validation
[ ] Có custom exception phù hợp
[ ] Có authorization backend
[ ] Status transition hợp lệ
[ ] Không dùng Optional.get() tùy tiện
[ ] Không dùng System.out.println()
[ ] Có test cho happy path và error path
[ ] Code compile
[ ] Test PASS
[ ] Match DB / SRS / UI
```

---

## Nguyên tắc quan trọng nhất

> Code phải được viết để thành viên khác đọc vào có thể hiểu nghiệp vụ mà không cần hỏi lại người viết.

> Không dùng hard-code cho giá trị có ý nghĩa nghiệp vụ.

> Không đặt tên biến, object, class hoặc method bằng các từ vô nghĩa.

> Các nhánh xử lý business rule quan trọng phải có comment ngắn giải thích rule trước đoạn xử lý.

> Không chỉ làm cho code chạy — code phải rõ ràng, test được và maintain được.

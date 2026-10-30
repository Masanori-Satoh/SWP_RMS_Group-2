# Offer Management - Sequence Diagram Specification

Tài liệu này đặc tả chi tiết luồng trình tự (Sequence Flow Specification) dựa trên **mã nguồn thực tế (Source of Truth)** của module Offer Management trong hệ thống Mộc RMS. Đặc tả này làm đầu vào trực tiếp để chuyển đổi thành biểu đồ PlantUML chuẩn xác, không chứa phương thức giả định hay quy trình lỗi thời.

---

## 4.1 HR Create & Submit Offer Proposal

### Purpose
Mô tả quy trình Chuyên viên Nhân sự (HR) mở giao diện tạo mới Đề xuất Offer, hệ thống tải và gợi ý ứng viên đã đỗ phỏng vấn (Final Decision = `Passed`), HR chọn ứng viên, hệ thống tự động điền các thông tin đề xuất, HR nhập và điều chỉnh các điều khoản tuyển dụng, xác thực nghiệp vụ (luật lao động, thời hạn), và lựa chọn **Lưu bản thảo (Save Draft)** hoặc **Nộp trình Giám đốc (Submit to Director)**.

### Actor
- **HR** (Tài khoản người dùng có quyền `ROLE_HR` hoặc `SYSTEM_ADMIN`)

### Participants

| Order | Participant | Actual Class / Component | Responsibility |
|---|---|---|---|
| 1 | `HR` | Actor | Chuyên viên Nhân sự thao tác tạo và nộp đề xuất Offer |
| 2 | `Offer UI` | `offers/form.html` / `offers/list.html` / `offers.js` | Giao diện tạo mới Offer (Màn hình riêng Screen 30 hoặc Pop-up Modal) |
| 3 | `OfferController` | `com.group2.rms.offer.controller.OfferController` | Tiếp nhận HTTP request tạo mới Offer, điều hướng hoặc phản hồi JSON |
| 4 | `OfferServiceImpl` | `com.group2.rms.offer.service.OfferServiceImpl` | Kiểm tra quy tắc nghiệp vụ (lương, ngày), quản lý trạng thái OfferProposal |
| 5 | `InterviewFinalResultRepository` | `com.group2.rms.interview.repository.InterviewFinalResultRepository` | Truy vấn danh sách ứng viên đỗ phỏng vấn (`Passed`) |
| 6 | `OfferProposalRepository` | `com.group2.rms.offer.repository.OfferProposalRepository` | Kiểm tra quy tắc Offer duy nhất (GBR-07) và lưu `OfferProposal` |
| 7 | `ApplicationRepository` | `com.group2.rms.candidate.repository.ApplicationRepository` | Xác thực sự tồn tại của hồ sơ ứng tuyển (`Application`) |
| 8 | `NotificationServiceImpl` | `com.group2.rms.notification.NotificationServiceImpl` | Tạo thông báo in-app cho toàn bộ Giám đốc hoạt động |
| 9 | `UserRepository` | `com.group2.rms.user.repository.UserRepository` | Lấy danh sách tài khoản Director và xác định người tạo (`proposedBy`) |

### Entry Point

| Item | Value |
|---|---|
| UI action | HR nhấn nút "Tạo mới Offer" (`openCreateOfferModal()` trên danh sách hoặc mở trang tạo mới) |
| HTTP Method (GET Form) | `GET` `/offers/create` (hoặc `GET /offers/create?applicationId={id}`) |
| Controller Method (GET) | `OfferController.createOfferForm(Integer applicationId, Model model)` |
| HTTP Method (Submit) | `POST` `/offers/create` (Hỗ trợ Form Submit hoặc AJAX JSON Submit) |
| Controller Method (Submit) | `OfferController.createOffer(...)` (Form) hoặc `OfferController.createOfferJson(...)` (AJAX) |

### Main Sequence

1. **HR** mở trang tạo mới Offer Proposal hoặc bấm nút "Tạo mới Offer" trên màn hình danh sách.
2. **Offer UI** gửi yêu cầu `GET /offers/create` đến `OfferController`.
3. `OfferController` gọi `OfferServiceImpl.getPassedCandidatesForOffer()`.
4. `OfferServiceImpl` gọi `InterviewFinalResultRepository.findAllPassedWithDetails()` để lấy danh sách kết quả phỏng vấn có `finalDecision = 'Passed'`.
5. Với mỗi ứng viên đỗ, `OfferServiceImpl` gọi `OfferProposalRepository.findByApplication_ApplicationId(appId)` để kiểm tra quy tắc Single Active Offer (GBR-07). Nếu ứng viên đã có Offer chưa xóa mềm (`isDeleted = false`), ứng viên sẽ bị loại khỏi danh sách gợi ý tạo mới.
6. `OfferServiceImpl` tổng hợp thông tin gợi ý (vị trí ứng tuyển, phòng ban, địa điểm làm việc, mức lương gợi ý `recommendedSalary`, nhận xét của Hiring Manager) thành `List<PassedCandidateResponse>` và trả về cho `OfferController`.
7. `OfferController` khởi tạo `CreateOfferRequest` (thiết lập mặc định `probationDays = 60`), gán vào model và trả về view `offers/form.html` (hoặc dữ liệu tải sẵn cho Modal trên `offers/list.html`).
8. **HR** chọn một ứng viên từ danh sách dropdown.
9. **Offer UI** (`offers.js: handleSelectPassedCandidate(appId)`) tự động điền:
   - Tên ứng viên, Email, Số điện thoại, Mã yêu cầu tuyển dụng, Hiring Manager.
   - Vị trí chức danh đề xuất (`offeredPositionTitle`).
   - Địa điểm làm việc (`workLocation`).
   - Lương chính thức đề xuất (`proposedSalary` lấy từ `recommendedSalary`).
   - Lương thử việc (`probationSalary` tự tính bằng 85% của lương chính thức).
10. **HR** nhập và điều chỉnh các trường dữ liệu:
    - `offeredPositionTitle`, `proposedSalary`, `probationSalary`, `probationDays` (mặc định 60).
    - `expectedStartDate` (ngày bắt đầu đi làm dự kiến).
    - `responsePeriodDays` (thời hạn phản hồi, mặc định 5 ngày).
    - `workLocation`, `benefitsPackage`.
11. **HR** nhấn nút **"Lưu bản thảo"** (`isDraft = true`) hoặc **"Trình Giám đốc"** (`isDraft = false`).
12. **Offer UI** (`offers.js: submitCreateOffer(isDraft)`) thực hiện kiểm tra sơ bộ trên trình duyệt:
    - Bắt buộc chọn ứng viên và nhập vị trí.
    - `proposedSalary > 0` và `probationSalary > 0`.
    - `probationSalary >= 85% proposedSalary` và `<= proposedSalary`.
    - `probationDays > 0`.
    - `expectedStartDate > today + responsePeriodDays`.
    - `workLocation` không được để trống.
13. **Offer UI** gửi request `POST /offers/create` (kèm dữ liệu `CreateOfferRequest`, CSRF token) tới `OfferController`.
14. `OfferController` gọi `OfferServiceImpl.createOfferByHr(createRequest)`.
15. `OfferServiceImpl` gọi `validateOfferBusinessRules(...)` kiểm tra chặt chẽ các điều kiện nghiệp vụ:
    - Chức danh, địa điểm làm việc không để trống.
    - Lương chính thức và lương thử việc > 0.
    - Lương thử việc đạt từ 85% đến 100% lương chính thức.
    - Thời gian thử việc > 0.
    - Ngày bắt đầu dự kiến phải sau ngày hiện tại cộng với số ngày phản hồi (`expectedStartDate > now + responsePeriodDays`).
16. `OfferServiceImpl` gọi `ApplicationRepository.findById(applicationId)` để kiểm tra hồ sơ ứng tuyển (ném `ResourceNotFoundException` nếu không tìm thấy).
17. `OfferServiceImpl` xác định danh tính HR tạo đề xuất (`resolveProposedBy(proposedById)` từ Spring Security context).
18. `OfferServiceImpl` kiểm tra lại ràng buộc GBR-07: gọi `OfferProposalRepository.findByApplication_ApplicationId(applicationId)`. Nếu đã tồn tại bản ghi chưa bị xóa, ném `OfferValidationException("OFFER_LOCKED_STATE")`.
19. `OfferServiceImpl` xác định trạng thái ban đầu:
    - Nếu `isDraft == true`: `offerStatus = "Draft"`.
    - Nếu `isDraft == false`: `offerStatus = "Pending_Director"`.
20. `OfferServiceImpl` khởi tạo thực thể `OfferProposal` với `responseDeadline = null`, `isDeleted = false`.
21. `OfferServiceImpl` gọi `OfferProposalRepository.save(offerToSave)`.

### Alternative / Conditional Flows

#### alt Save Draft (Lưu bản thảo)
- **Điều kiện:** HR nhấn nút "Lưu bản thảo" (`dto.isDraft == true`).
- `OfferServiceImpl` gán `offerStatus = "Draft"`.
- Không gửi email và không tạo thông báo hệ thống.
- `OfferServiceImpl` lưu và trả về `OfferResponse` với trạng thái `Draft`.
- `OfferController` phản hồi thông báo thành công: `"Lưu bản thảo Offer (Draft) thành công!"` (Redirect về `/offers` kèm flash attribute hoặc trả JSON HTTP 201).

#### else Submit to Director (Trình Giám đốc phê duyệt)
- **Điều kiện:** HR nhấn nút "Trình Giám đốc" (`dto.isDraft == false`).
- `OfferServiceImpl` gán `offerStatus = "Pending_Director"`.
- `OfferServiceImpl` gọi `NotificationServiceImpl.notifyOfferPendingDirector(saved, proposedBy)`:
  - `NotificationServiceImpl` gọi `UserRepository.findAllActiveDirectors()` lấy danh sách toàn bộ Giám đốc đang hoạt động.
  - Với mỗi Giám đốc, tạo thực thể `Notification` với `eventType = "OFFER_PENDING_DIRECTOR"`, `referenceId = offerId`, `linkUrl = "/offers/{id}"`.
  - Lưu thông báo vào cơ sở dữ liệu (không gửi email nội bộ, chỉ gửi thông báo in-app).
- `OfferServiceImpl` lưu và trả về `OfferResponse` với trạng thái `Pending_Director`.
- `OfferController` phản hồi thông báo thành công: `"Đã nộp trình đề xuất Offer lên Giám đốc thành công!"` (Redirect về `/offers` hoặc trả JSON HTTP 201).

### Database Effects

| Table / Entity | Operation | Important Fields / State |
|---|---|---|
| `OfferProposal` | `INSERT` | `OfferId` (Identity), `ApplicationId`, `OfferedPositionTitle`, `ProposedSalary`, `ProbationSalary`, `ExpectedStartDate`, `WorkLocation`, `BenefitsPackage`, `ResponsePeriodDays`, `ResponseDeadline = NULL`, `ProposedBy = hrUserId`, `OfferStatus = 'Draft'` hoặc `'Pending_Director'`, `IsDeleted = 0`, `CreatedAt`, `UpdatedAt` |
| `Notification` (Khi Submit to Director) | `INSERT` (mỗi Director 1 bản ghi) | `RecipientId = directorUserId`, `ActorId = hrUserId`, `Title`, `Content`, `EventType = 'OFFER_PENDING_DIRECTOR'`, `ReferenceId = offerId`, `LinkUrl = '/offers/{id}'`, `IsRead = 0`, `CreatedAt` |

### Notifications / Emails
- **Save Draft:** Không có email, không có thông báo.
- **Submit to Director:**
  - **Email:** Không gửi email nội bộ.
  - **In-App:** `NotificationServiceImpl.notifyOfferPendingDirector(saved, proposedBy)` tạo thông báo chuông cho tất cả tài khoản Giám đốc đang active.

### Final State
- Nếu Lưu nháp: `OfferProposal.OfferStatus = 'Draft'`. HR có thể tiếp tục chỉnh sửa hoặc xóa bản nháp.
- Nếu Nộp trình: `OfferProposal.OfferStatus = 'Pending_Director'`. Đề xuất xuất hiện trong danh sách "Chờ duyệt" của Giám đốc.

### PlantUML Notes
- **Actor:** `actor HR`
- **Participants:**
  - `boundary "Offer UI" as UI`
  - `control "OfferController" as Controller`
  - `participant "OfferServiceImpl" as Service`
  - `database "InterviewFinalResultRepository" as FinalResultRepo`
  - `database "ApplicationRepository" as AppRepo`
  - `database "OfferProposalRepository" as OfferRepo`
  - `participant "NotificationServiceImpl" as NotifService`
  - `database "UserRepository" as UserRepo`
- **UML Constructs:**
  - `opt applicationId is passed`: Tự động fill ứng viên được chỉ định.
  - `alt isDraft == true`: Luồng lưu bản thảo.
  - `else isDraft == false`: Luồng nộp trình Giám đốc kèm vòng lặp `loop for each active Director` tạo thông báo.

### Source Discrepancies / Risks
- **Hai hình thức nộp:** Mã nguồn hiện tại hỗ trợ song song 2 cách nộp dữ liệu tạo mới: Form submission thông thường (`offers/form.html`) và AJAX JSON (`openCreateOfferModal()` trên `offers/list.html`). Cả hai đều gọi chung phương thức `offerService.createOfferByHr(createRequest)`.
- **Phương thức Service:** Trong `OfferServiceImpl.java`, phương thức đang được `OfferController` sử dụng là `createOfferByHr(CreateOfferRequest dto)`. Phương thức cũ `createOfferProposal(CreateOfferProposalRequest req)` không còn được Controller gọi.

---

## 4.2 Director Review & Decide Offer Proposal

### Purpose
Mô tả quy trình Giám đốc (Director) truy cập danh sách các đề xuất Offer đang ở trạng thái Chờ duyệt (`Pending_Director`), mở xem chi tiết điều khoản Offer và kết quả phỏng vấn liên quan trên Màn hình 32 (`/offers/{id}`), sau đó đưa ra quyết định **Phê duyệt (Approve)** hoặc **Từ chối (Reject)** kèm ghi chú. Hệ thống cập nhật trạng thái Offer, lưu vết lịch sử phê duyệt (`OfferApproval`), gửi email và thông báo in-app kết quả về cho HR phụ trách.

### Actor
- **Director** (Tài khoản người dùng có quyền `ROLE_DIRECTOR` hoặc `SYSTEM_ADMIN`)

### Participants

| Order | Participant | Actual Class / Component | Responsibility |
|---|---|---|---|
| 1 | `Director` | Actor | Giám đốc xem xét và ra quyết định phê duyệt / từ chối |
| 2 | `Director UI` | `offers/director-list.html` / `offers/detail.html` / `offers.js` | Giao diện danh sách chờ duyệt và chi tiết quyết định của Director |
| 3 | `OfferController` | `com.group2.rms.offer.controller.OfferController` | Tiếp nhận yêu cầu xem danh sách, xem chi tiết và phê duyệt/từ chối Offer |
| 4 | `OfferServiceImpl` | `com.group2.rms.offer.service.OfferServiceImpl` | Xác thực quyền duyệt theo trạng thái, cập nhật Offer và tạo bản ghi duyệt |
| 5 | `OfferProposalRepository` | `com.group2.rms.offer.repository.OfferProposalRepository` | Đọc dữ liệu đề xuất và lưu trạng thái mới (`Director_Approved` / `Director_Rejected`) |
| 6 | `OfferApprovalRepository` | `com.group2.rms.offer.repository.OfferApprovalRepository` | Lưu lịch sử phê duyệt (`OfferApproval`) của Director |
| 7 | `InterviewFinalResultRepository` | `com.group2.rms.interview.repository.InterviewFinalResultRepository` | Truy vấn kết quả phỏng vấn để hiển thị đối chiếu cho Director |
| 8 | `NotificationServiceImpl` | `com.group2.rms.notification.NotificationServiceImpl` | Tạo thông báo in-app cho HR về quyết định của Giám đốc |

### Entry Point

| Item | Value |
|---|---|
| UI action (Xem danh sách) | Director mở menu Đề xuất Offer (`/offers?status=Pending_Director`) |
| HTTP Method & Endpoint (List) | `GET` `/offers?status=Pending_Director` (hoặc `GET /offers`) |
| Controller Method (List) | `OfferController.listOffers(...)` -> trả về `"offers/director-list"` |
| UI action (Xem chi tiết) | Director click icon `fa-clipboard-check` ("Xem xét & ra quyết định") trên hàng Offer |
| HTTP Method & Endpoint (Detail) | `GET` `/offers/{id}` |
| Controller Method (Detail) | `OfferController.viewOfferDetail(@PathVariable Integer id, Model model)` -> trả về `"offers/detail"` |
| UI action (Quyết định) | Director nhập nhận xét vào `#directorDetailComments` và nhấn nút "Phê duyệt" hoặc "Từ chối" |
| HTTP Method & Endpoint (Approve) | `POST` `/offers/{id}/approve` (Form POST hoặc AJAX JSON với `DirectorDecisionRequest`) |
| HTTP Method & Endpoint (Reject) | `POST` `/offers/{id}/reject` (Form POST hoặc AJAX JSON với `DirectorDecisionRequest`) |
| Controller Methods (Decision) | `OfferController.approveOfferJson(...)` / `approveOfferForm(...)`, `rejectOfferJson(...)` / `rejectOfferForm(...)` |

### Main Sequence

1. **Director** truy cập vào mục Quản lý Offer (`/offers`).
2. `OfferController.listOffers(...)` nhận diện vai trò `userRole == "Director"`, gọi:
   - `offerService.getDirectorStatusCounts()`
   - `offerService.getDirectorOfferStats()`
   - `offerService.getDirectorOffers(search, "Pending_Director", timeSort, pageable)`
3. `OfferController` trả về giao diện `offers/director-list.html`.
4. **Director** nhấn vào nút "Xem xét & ra quyết định" (icon `fa-clipboard-check`) tại hàng đề xuất có ID `#id`.
5. **Director UI** điều hướng tới `GET /offers/{id}`.
6. `OfferController.viewOfferDetail(id, model)` gọi `OfferServiceImpl.getOfferDetailForHr(id)`.
7. `OfferServiceImpl` thực hiện:
   - Gọi `OfferProposalRepository.findById(id)` để lấy thông tin đề xuất Offer.
   - Gọi `InterviewFinalResultRepository.findByApplicationIdOrderByApprovedAtDesc(applicationId)` để lấy kết quả đánh giá phỏng vấn và mức lương đề xuất của Hiring Manager.
   - Gọi `OfferApprovalRepository.findByOfferProposal_OfferIdOrderByApprovedAtDesc(id)` để lấy lịch sử các lần phê duyệt trước (nếu có).
8. `OfferController` chuẩn bị model (`detail`, `isDirector = true`, `isPendingDirector = true`) và render view `offers/detail.html` với thẻ thông tin ra quyết định (`#directorDecisionSection`).
9. **Director** xem xét các thông tin đối chiếu (Lương chính thức, Lương thử việc, Địa điểm, Thời hạn, Đánh giá phỏng vấn), nhập ý kiến nhận xét vào ô `directorDetailComments`.
10. **Director** nhấn nút **"Phê duyệt Đề xuất"** hoặc **"Từ chối Đề xuất"**.
11. **Director UI** (`offers.js: submitDirectorDecisionFromScreen(offerId, action)`) gửi `POST /offers/{id}/approve` hoặc `POST /offers/{id}/reject` kèm `{ comments: "..." }`.
12. `OfferController` gọi phương thức tương ứng trong `OfferServiceImpl`:
    - Nếu duyệt: `offerService.approveOfferByDirector(id, comments)`
    - Nếu từ chối: `offerService.rejectOfferByDirector(id, comments)`
13. `OfferServiceImpl` gọi `OfferProposalRepository.findById(id)`.
14. **Validation nghiệp vụ:** `OfferServiceImpl` kiểm tra `offer.getOfferStatus().equalsIgnoreCase("Pending_Director")`.
    - Nếu trạng thái khác `Pending_Director`: ném `OfferValidationException("Chỉ được phê duyệt/từ chối khi ở trạng thái Pending_Director", "OFFER_STATUS_INVALID")`.
15. `OfferServiceImpl` xác định danh tính Director từ context đăng nhập hiện tại (`resolveProposedBy(null)`).

### Alternative / Conditional Flows

#### alt Approve (Giám đốc phê duyệt)
- **Điều kiện:** Director nhấn "Phê duyệt Đề xuất".
- `OfferServiceImpl` cập nhật `offer.setOfferStatus("Director_Approved")`.
- `OfferServiceImpl` gọi `OfferProposalRepository.save(offer)`.
- `OfferServiceImpl` tạo thực thể `OfferApproval`:
  - `offerProposal = saved`
  - `director = director`
  - `status = "Approved"`
  - `directorComments = comments`
  - `approvedAt = LocalDateTime.now()`
- `OfferServiceImpl` gọi `OfferApprovalRepository.save(approval)`.
- `OfferServiceImpl` gọi `NotificationServiceImpl.notifyDirectorDecisionOnOffer(saved, director, true, comments)`:
  - Tạo thực thể `Notification` (`eventType = "OFFER_DIRECTOR_APPROVED"`) gửi tới tài khoản HR (`offer.getProposedBy()`) (không gửi email nội bộ, chỉ gửi thông báo in-app).
- `OfferController` trả về phản hồi thành công: `"Đã phê duyệt đề xuất Offer thành công!"`.

#### else Reject (Giám đốc từ chối)
- **Điều kiện:** Director nhấn "Từ chối Đề xuất".
- `OfferServiceImpl` cập nhật `offer.setOfferStatus("Director_Rejected")`.
- `OfferServiceImpl` gọi `OfferProposalRepository.save(offer)`.
- `OfferServiceImpl` tạo thực thể `OfferApproval`:
  - `offerProposal = saved`
  - `director = director`
  - `status = "Rejected"`
  - `directorComments = comments`
  - `approvedAt = LocalDateTime.now()`
- `OfferServiceImpl` gọi `OfferApprovalRepository.save(approval)`.
- `OfferServiceImpl` gọi `NotificationServiceImpl.notifyDirectorDecisionOnOffer(saved, director, false, comments)`:
  - Tạo thực thể `Notification` (`eventType = "OFFER_DIRECTOR_REJECTED"`) gửi tới tài khoản HR (`offer.getProposedBy()`) (không gửi email nội bộ, chỉ gửi thông báo in-app).
- `OfferController` trả về phản hồi thành công: `"Đã từ chối đề xuất Offer thành công!"`.

### Database Effects

| Table / Entity | Operation | Important Fields / State |
|---|---|---|
| `OfferProposal` | `UPDATE` | `OfferStatus = 'Director_Approved'` hoặc `'Director_Rejected'`, `UpdatedAt = CURRENT_TIMESTAMP` |
| `OfferApproval` | `INSERT` | `OfferApprovalId` (Identity), `OfferId`, `DirectorId`, `Status = 'Approved'` hoặc `'Rejected'`, `DirectorComments`, `ApprovedAt = CURRENT_TIMESTAMP` |
| `Notification` | `INSERT` | `RecipientId = proposedBy.userId`, `ActorId = director.userId`, `Title`, `Content`, `EventType = 'OFFER_DIRECTOR_APPROVED'` hoặc `'OFFER_DIRECTOR_REJECTED'`, `ReferenceId = offerId`, `LinkUrl = '/offers/{id}'`, `IsRead = 0` |

### Notifications / Emails
- **Email:** Không gửi email nội bộ.
- **In-App:** `NotificationServiceImpl.notifyDirectorDecisionOnOffer(...)` tạo thông báo chuông cho HR về quyết định của Giám đốc.

### Final State
- Nếu Phê duyệt: `OfferProposal.OfferStatus = 'Director_Approved'`. Đề xuất sẵn sàng để HR phát hành thư mời cho ứng viên.
- Nếu Từ chối: `OfferProposal.OfferStatus = 'Director_Rejected'`. HR có thể xem lý do từ chối và thực hiện chỉnh sửa lại bản thảo.
- Bản ghi `OfferApproval` mới được lưu lại trong cơ sở dữ liệu phục vụ audit trail.

### PlantUML Notes
- **Actor:** `actor Director`
- **Participants:**
  - `boundary "Director UI" as UI`
  - `control "OfferController" as Controller`
  - `participant "OfferServiceImpl" as Service`
  - `database "OfferProposalRepository" as OfferRepo`
  - `database "OfferApprovalRepository" as ApprovalRepo`
  - `database "InterviewFinalResultRepository" as FinalResultRepo`
  - `participant "NotificationServiceImpl" as NotifService`
- **UML Constructs:**
  - `alt offerStatus != "Pending_Director"`: Ném ngoại lệ `OfferValidationException("OFFER_STATUS_INVALID")`.
  - `alt Director Approves`: Nhánh phê duyệt (`Director_Approved`).
  - `else Director Rejects`: Nhánh từ chối (`Director_Rejected`).

### Source Discrepancies / Risks
- **Xác định Director:** Phương thức `resolveProposedBy(null)` trong `OfferServiceImpl` lấy `Authentication.getName()` từ `SecurityContextHolder` và tìm `User` theo username/email. Nếu context bị mất hoặc không tìm thấy user, sẽ ném lỗi `OfferValidationException("USER_NOT_FOUND")`.

---

## 4.3 HR Issue Offer Letter to Candidate

### Purpose
Mô tả quy trình Chuyên viên Nhân sự (HR) mở xem một đề xuất Offer đã được Giám đốc phê duyệt (`Director_Approved`), thực hiện phát hành Thư mời nhận việc (Offer Letter) chính thức cho ứng viên. Hệ thống kiểm tra điều kiện trạng thái, kiểm tra tính hợp lệ của thời hạn phản hồi (`responsePeriodDays`) đối chiếu với ngày bắt đầu đi làm dự kiến (`expectedStartDate`), tính toán mốc thời hạn chót (`responseDeadline`), chuyển đổi đồng bộ trạng thái `OfferProposal` sang `Sent_Candidate` và `Application` sang `Offered`, đồng thời gửi email Thư mời làm việc và tạo thông báo in-app cho ứng viên.

### Actor
- **HR** (Tài khoản người dùng có quyền `ROLE_HR` hoặc `SYSTEM_ADMIN`)

### Participants

| Order | Participant | Actual Class / Component | Responsibility |
|---|---|---|---|
| 1 | `HR` | Actor | Chuyên viên Nhân sự thực hiện phát hành thư mời làm việc |
| 2 | `Offer UI` | `offers/detail.html` / `offers/list.html` / `offers.js` | Giao diện hiển thị nút "Phát hành Offer Letter" và modal xác nhận |
| 3 | `OfferController` | `com.group2.rms.offer.controller.OfferController` | Tiếp nhận yêu cầu phát hành thư mời từ HR |
| 4 | `OfferServiceImpl` | `com.group2.rms.offer.service.OfferServiceImpl` | Xác thực điều kiện phát hành, tính deadline, cập nhật trạng thái Offer và Application |
| 5 | `OfferProposalRepository` | `com.group2.rms.offer.repository.OfferProposalRepository` | Đọc Offer và lưu trạng thái mới `Sent_Candidate` kèm `responseDeadline` |
| 6 | `ApplicationRepository` | `com.group2.rms.candidate.repository.ApplicationRepository` | Cập nhật trạng thái hồ sơ ứng tuyển `Application` sang `Offered` |
| 7 | `OfferMailSender` | `com.group2.rms.notification.OfferMailSender` | Gửi email Job Offer Letter chính thức tới hòm thư ứng viên |
| 8 | `NotificationServiceImpl` | `com.group2.rms.notification.NotificationServiceImpl` | Tạo thông báo in-app cho tài khoản ứng viên |

### Entry Point

| Item | Value |
|---|---|
| UI action | HR nhấn nút "Phát hành Offer Letter" trên màn hình chi tiết Offer (`/offers/{id}`) |
| JS Trigger | `confirmSendOffer(offerId)` trong `offers.js` hiển thị hộp thoại xác nhận |
| HTTP Method | `POST` |
| Endpoint | `/offers/{id}/send` |
| Controller Method | `OfferController.sendOfferToCandidateJson(@PathVariable Integer id)` (AJAX) hoặc `sendOfferToCandidate(@PathVariable Integer id, RedirectAttributes flash)` (Form) |

### Main Sequence

1. **HR** mở trang chi tiết của một Offer Proposal tại `/offers/{id}`.
2. `OfferController.viewOfferDetail(id, model)` tải thông tin Offer có `offerStatus == 'Director_Approved'`. Giao diện hiển thị nút thao tác "Phát hành Offer Letter" (`btn-send-offer`).
3. **HR** nhấn nút "Phát hành Offer Letter".
4. **Offer UI** (`offers.js: confirmSendOffer(offerId)`) hiển thị hộp thoại xác nhận phát hành.
5. Khi HR xác nhận, **Offer UI** gửi request `POST /offers/{id}/send` (kèm header `X-CSRF-TOKEN`) tới `OfferController`.
6. `OfferController` gọi `OfferServiceImpl.sendOfferToCandidate(id)`.
7. `OfferServiceImpl` gọi `OfferProposalRepository.findById(id)` để lấy thông tin `OfferProposal`.
8. **Validation 1 (Trạng thái duyệt):**
   - `OfferServiceImpl` kiểm tra `offer.getOfferStatus()`.
   - Điều kiện hợp lệ: Trạng thái phải là `"Director_Approved"` hoặc `"Approved"`.
   - Nếu không thỏa mãn: ném `OfferValidationException("Chỉ được gửi thư mời nhận việc khi Offer đã được Director phê duyệt...", "OFFER_NOT_APPROVED")`.
9. **Validation 2 (Thời hạn phản hồi):**
   - `OfferServiceImpl` kiểm tra `offer.getResponsePeriodDays() != null && offer.getResponsePeriodDays() > 0`.
   - Nếu không hợp lệ: ném `OfferValidationException("Offer chưa có thời gian phản hồi hợp lệ.")`.
10. **Validation 3 (Ràng buộc ngày bắt đầu dự kiến):**
    - `OfferServiceImpl` tính toán thời hạn phản hồi chính xác:
      `deadline = LocalDateTime.now().plusDays(offer.getResponsePeriodDays())`
    - Kiểm tra: `if (offer.getExpectedStartDate() != null && !deadline.toLocalDate().isBefore(offer.getExpectedStartDate()))`
    - Nếu hạn phản hồi không trước ngày đi làm dự kiến: ném `OfferValidationException("Thời hạn phản hồi phải trước ngày bắt đầu dự kiến.")`.
11. **Cập nhật OfferProposal:**
    - `offer.setResponseDeadline(deadline)`
    - `offer.setOfferStatus("Sent_Candidate")`
12. `OfferServiceImpl` gọi `OfferProposalRepository.save(offer)`.
13. **Gửi Email Offer Letter:**
    - `OfferServiceImpl` gọi `OfferMailSender.sendOfferLetterToCandidate(saved)`.
    - `OfferMailSender` lấy thông tin email, họ tên ứng viên từ `saved.getApplication().getCandidate().getAccount()`.
    - Soạn thảo email Thư mời nhận việc chứa: Vị trí chức danh, Lương chính thức, Lương thử việc, Địa điểm làm việc, Hạn phản hồi (`HH:mm dd/MM/yyyy`).
    - Thực hiện gửi email qua JavaMailSender (hoặc ghi log mock email nếu chưa cấu hình SMTP).
14. **Gửi Thông báo In-App:**
    - `OfferServiceImpl` gọi `NotificationServiceImpl.notifyOfferLetterSent(saved)`.
    - `NotificationServiceImpl` tạo bản ghi `Notification` gửi tới tài khoản User của ứng viên:
      - `eventType = "OFFER_LETTER_SENT"`
      - `referenceId = String.valueOf(saved.getOfferId())`
      - `linkUrl = "/portal/offers/" + saved.getOfferId()`
15. **Đồng bộ trạng thái Application:**
    - `OfferServiceImpl` lấy thực thể `Application = saved.getApplication()`.
    - Gán `application.setApplicationStatus("Offered")`.
    - Gọi `ApplicationRepository.save(application)`.
16. `OfferServiceImpl` chuyển đổi thực thể sang `OfferResponse` và trả về cho `OfferController`.
17. `OfferController` phản hồi kết quả thành công tới UI (JSON `ApiResponse(true, ...)` hoặc redirect kèm flash attribute).
18. **Offer UI** hiển thị toast thông báo thành công và reload lại giao diện chi tiết Offer.

### Alternative / Conditional Flows

#### alt Offer chưa được Giám đốc phê duyệt
- **Điều kiện:** `offer.getOfferStatus()` không phải là `Director_Approved` hoặc `Approved`.
- `OfferServiceImpl` ném ngoại lệ `OfferValidationException("OFFER_NOT_APPROVED")`.
- `OfferController` bắt lỗi `BaseBusinessException`, trả về HTTP 400 Bad Request kèm thông báo lỗi.
- `Offer UI` hiển thị toast lỗi cảnh báo và dừng tiến trình.

#### alt Hạn phản hồi xung đột với Ngày bắt đầu dự kiến
- **Điều kiện:** `deadline.toLocalDate()` không trước `expectedStartDate` (tức là hạn chót phản hồi rơi vào hoặc sau ngày ứng viên nhận việc).
- `OfferServiceImpl` ném ngoại lệ `OfferValidationException("Thời hạn phản hồi phải trước ngày bắt đầu dự kiến.")`.
- `OfferController` trả về HTTP 400 Bad Request.
- `Offer UI` thông báo lỗi và yêu cầu HR điều chỉnh lại ngày bắt đầu hoặc số ngày phản hồi.

#### else Điều kiện phát hành hợp lệ
- Tiến hành cập nhật `OfferProposal` sang `Sent_Candidate`.
- Lưu `responseDeadline`.
- Đồng bộ `Application` sang `Offered`.
- Gửi email và thông báo cho ứng viên.

### Database Effects

| Table / Entity | Operation | Important Fields / State |
|---|---|---|
| `OfferProposal` | `UPDATE` | `OfferStatus = 'Sent_Candidate'`, `ResponseDeadline = deadline`, `UpdatedAt = CURRENT_TIMESTAMP` |
| `Application` | `UPDATE` | `ApplicationStatus = 'Offered'`, `UpdatedAt = CURRENT_TIMESTAMP` |
| `Notification` | `INSERT` | `RecipientId = candidateUserId`, `ActorId = hrUserId`, `Title`, `Content`, `EventType = 'OFFER_LETTER_SENT'`, `ReferenceId = offerId`, `LinkUrl = '/portal/offers/{id}'`, `IsRead = 0` |

### Notifications / Emails
- **Email:** `OfferMailSender.sendOfferLetterToCandidate(saved)` gửi Thư mời nhận việc chính thức đến địa chỉ email cá nhân của ứng viên.
- **In-App:** `NotificationServiceImpl.notifyOfferLetterSent(saved)` gửi thông báo chuông vào tài khoản ứng viên trong hệ thống Mộc RMS.

### Final State
- `OfferProposal.OfferStatus = 'Sent_Candidate'`.
- `OfferProposal.ResponseDeadline` được lưu mốc thời gian cụ thể (VD: 5 ngày kể từ thời điểm phát hành).
- `Application.ApplicationStatus = 'Offered'`.
- Ứng viên có thể đăng nhập vào Candidate Portal để xem chi tiết và đưa ra câu trả lời.

### PlantUML Notes
- **Actor:** `actor HR`
- **Participants:**
  - `boundary "Offer UI" as UI`
  - `control "OfferController" as Controller`
  - `participant "OfferServiceImpl" as Service`
  - `database "OfferProposalRepository" as OfferRepo`
  - `database "ApplicationRepository" as AppRepo`
  - `participant "OfferMailSender" as MailSender`
  - `participant "NotificationServiceImpl" as NotifService`
- **UML Constructs:**
  - `alt !isApproved`: Ném lỗi `OFFER_NOT_APPROVED`.
  - `else if deadline >= expectedStartDate`: Ném lỗi thời hạn xung đột.
  - `else Valid`: Thực hiện cập nhật trạng thái kép (Offer & Application), gửi mail và notification.

### Source Discrepancies / Risks
- **Trạng thái kép Approved:** Trong mã nguồn `OfferServiceImpl.java` (dòng 606), hệ thống chấp nhận cả `"Approved"` và `"Director_Approved"` nhằm đảm bảo tương thích ngược với dữ liệu cũ từ schema ban đầu trước khi chuẩn hóa tiền tố `Director_`.

---

## 4.4 Candidate Respond to Offer

### Purpose
Mô tả quy trình Ứng viên (Candidate) đăng nhập vào Cổng thông tin ứng viên (Candidate Portal), truy cập danh sách và chi tiết Thư mời nhận việc (`/portal/offers/{id}`), hệ thống xác thực quyền sở hữu và kiểm tra các ràng buộc về tính hợp lệ (trạng thái chờ, thời hạn phản hồi, ngày bắt đầu làm việc). Ứng viên đưa ra câu trả lời: **Nhận Offer (Accept)** hoặc **Từ chối (Decline)** kèm lý do. Hệ thống thực hiện cập nhật nguyên tử chống tranh chấp dữ liệu, đồng bộ trạng thái hồ sơ ứng tuyển (`Hired` hoặc `Rejected`), đánh dấu thông báo đã đọc và gửi thông báo in-app đến HR cùng Hiring Manager phụ trách.

### Actor
- **Candidate** (Tài khoản người dùng có quyền `ROLE_CANDIDATE`)

### Participants

| Order | Participant | Actual Class / Component | Responsibility |
|---|---|---|---|
| 1 | `Candidate` | Actor | Ứng viên xem và phản hồi Thư mời nhận việc |
| 2 | `Candidate UI` | `offers/candidate-list.html` / `candidate-detail.html` / `candidate-decline.html` | Giao diện cổng thông tin ứng viên xem và phản hồi Offer |
| 3 | `CandidateOfferController` | `com.group2.rms.offer.controller.CandidateOfferController` | Điều hướng giao diện portal và tiếp nhận action Accept / Decline |
| 4 | `CandidateOfferServiceImpl` | `com.group2.rms.offer.service.CandidateOfferServiceImpl` | Kiểm tra quyền sở hữu, thời hạn phản hồi, thực hiện cập nhật trạng thái Offer |
| 5 | `CurrentUserService` | `com.group2.rms.core.security.CurrentUserService` | Lấy danh tính người dùng hiện tại từ Spring Security Context |
| 6 | `CandidateOfferRepository` | `com.group2.rms.offer.repository.CandidateOfferRepository` | Truy vấn Offer theo ứng viên và cập nhật nguyên tử (`answerIfSent`) |
| 7 | `ApplicationStatusService` | `com.group2.rms.candidate.service.ApplicationStatusService` | Điều phối chuyển trạng thái hồ sơ ứng tuyển dựa trên kết quả trả lời Offer |
| 8 | `ApplicationRepository` | `com.group2.rms.candidate.repository.ApplicationRepository` | Chuyển đổi trạng thái `Application` sang `Hired` hoặc `Rejected` (`transition`) |
| 9 | `NotificationServiceImpl` | `com.group2.rms.notification.NotificationServiceImpl` | Đánh dấu thông báo đã đọc và thông báo kết quả phản hồi tới HR, Hiring Manager |

### Entry Point

| Item | Value |
|---|---|
| UI action (Xem danh sách) | Ứng viên truy cập menu "Thư mời nhận việc" trên sidebar ứng viên |
| HTTP Method & Endpoint (List) | `GET` `/portal/offers` |
| Controller Method (List) | `CandidateOfferController.list(Model model)` -> trả về `"offers/candidate-list"` |
| UI action (Xem chi tiết) | Ứng viên nhấn xem chi tiết thư mời |
| HTTP Method & Endpoint (Detail) | `GET` `/portal/offers/{id}` |
| Controller Method (Detail) | `CandidateOfferController.detail(@PathVariable Integer id, Model model)` -> trả về `"offers/candidate-detail"` |
| UI action (Màn hình từ chối) | Ứng viên nhấn nút "Từ chối" trên trang chi tiết |
| HTTP Method & Endpoint (Decline Page) | `GET` `/portal/offers/{id}/decline` |
| Controller Method (Decline Page) | `CandidateOfferController.declinePage(...)` -> trả về `"offers/candidate-decline"` |
| UI action (Xác nhận Nhận Offer) | Ứng viên nhấn "Đồng ý nhận việc" |
| HTTP Method & Endpoint (Accept) | `POST` `/portal/offers/{id}/accept?fromDetail=true` |
| Controller Method (Accept) | `CandidateOfferController.accept(...)` |
| UI action (Xác nhận Từ chối) | Ứng viên nhập lý do từ chối và nhấn "Xác nhận từ chối" |
| HTTP Method & Endpoint (Decline) | `POST` `/portal/offers/{id}/decline?fromDetail=true&declineReason={reason}` |
| Controller Method (Decline) | `CandidateOfferController.decline(...)` |

### Main Sequence

1. **Candidate** truy cập vào chi tiết thư mời nhận việc tại `/portal/offers/{id}`.
2. `CandidateOfferController.detail(id, model)` gọi `CandidateOfferServiceImpl.getDetail(id)`.
3. `CandidateOfferServiceImpl` gọi `CurrentUserService.requireUser().getUserId()` để lấy ID tài khoản ứng viên đang đăng nhập.
4. `CandidateOfferServiceImpl` gọi `CandidateOfferRepository.findVisibleById(id, userId, ['Sent_Candidate', 'Accepted', 'Declined'])`.
   - **Bảo mật & Quyền sở hữu:** Nếu ID offer không thuộc về ứng viên này hoặc đang ở trạng thái nội bộ (`Draft`, `Pending_Director`, `Director_Approved`), truy vấn trả về rỗng -> ném `ResourceNotFoundException("Không tìm thấy thư mời nhận việc.")`.
5. `CandidateOfferServiceImpl` gọi `NotificationServiceImpl.markOfferNotificationRead(userId, id)` để đánh dấu thông báo gửi Offer trước đó là đã đọc.
6. `CandidateOfferServiceImpl` đóng gói dữ liệu thành `CandidateOfferDetailResponse`:
   - Tính toán cờ `canRespond`: Offer có trạng thái là `Sent_Candidate` VÀ chưa bị quá hạn (`!isExpired(offer)`).
7. `CandidateOfferController` render trang `offers/candidate-detail.html`. Nếu `canRespond == true`, giao diện hiển thị 2 nút: **"Nhận offer"** và **"Từ chối"**.
8. **Candidate** đưa ra phản hồi:
   - Nếu nhấn "Nhận offer": Trình duyệt gửi `POST /portal/offers/{id}/accept?fromDetail=true`.
   - Nếu nhấn "Từ chối": Trình duyệt chuyển tới `GET /portal/offers/{id}/decline` để ứng viên nhập lý do, sau đó gửi `POST /portal/offers/{id}/decline?fromDetail=true&declineReason=...`.
9. `CandidateOfferController` tiếp nhận request và gọi phương thức tương ứng trong `CandidateOfferServiceImpl`:
   - Nhận: `candidateOffers.accept(id)`
   - Từ chối: `candidateOffers.decline(id, declineReason)`
10. Cả hai nhánh đều gọi vào phương thức xử lý chung `answer(offerId, accepted, declineReason)`.
11. `CandidateOfferServiceImpl` xác thực lại sự tồn tại và quyền sở hữu: gọi `CandidateOfferRepository.findVisibleById(...)`.
12. **Kiểm tra tính hợp lệ để trả lời (`requireAnswerable(offer)`):**
    - **Trạng thái:** `offer.getOfferStatus()` bắt buộc phải là `"Sent_Candidate"`. Nếu không, ném `OfferResponseException("Thư mời này đã được trả lời nên không thể thay đổi.")`.
    - **Thời hạn phản hồi:** Nếu `offer.getResponseDeadline() != null` và `LocalDateTime.now().isAfter(responseDeadline)` -> ném `OfferResponseException("Thư mời này đã quá thời hạn phản hồi...")`.
    - **Ngày bắt đầu dự kiến:** Nếu `offer.getExpectedStartDate() != null` và `offer.getExpectedStartDate().isBefore(LocalDate.now())` -> ném `OfferResponseException("Thư mời này đã quá ngày bắt đầu nhận việc dự kiến...")`.
13. **Cập nhật nguyên tử trạng thái Offer (Optimistic Concurrency Protection):**
    - Xác định `toStatus = accepted ? "Accepted" : "Declined"`.
    - Làm sạch lý do: `cleanReason = (!accepted && declineReason != null && !declineReason.isBlank()) ? declineReason.trim() : null`.
    - Gọi query cập nhật nguyên tử:
      - Nếu có lý do: `CandidateOfferRepository.answerIfSent(offerId, toStatus, cleanReason)`
      - Nếu không có lý do: `CandidateOfferRepository.answerIfSent(offerId, toStatus)`
      - Câu lệnh SQL: `UPDATE OfferProposal o SET o.offerStatus = :to [, o.declineReason = :reason], o.updatedAt = CURRENT_TIMESTAMP WHERE o.offerId = :id AND o.offerStatus = 'Sent_Candidate'`.
    - Nếu số dòng cập nhật `updated == 0`: ném `OfferResponseException("Thư mời vừa được cập nhật. Vui lòng tải lại trang.")`.
14. **Đồng bộ trạng thái đơn ứng tuyển:**
    - `CandidateOfferServiceImpl` gọi `ApplicationStatusService.onOfferAnswered(applicationId, accepted)`.
    - `ApplicationStatusService` gọi `ApplicationRepository.transition(applicationId, Set.of("Offered"), accepted ? "Hired" : "Rejected")`.
    - Câu lệnh SQL: `UPDATE Application a SET a.applicationStatus = :to, a.updatedAt = CURRENT_TIMESTAMP WHERE a.applicationId = :id AND a.applicationStatus IN ('Offered')`.
15. **Xử lý Thông báo hệ thống:**
    - `CandidateOfferServiceImpl` gọi `NotificationServiceImpl.markOfferNotificationRead(userId, offerId)`.
    - `CandidateOfferServiceImpl` gọi `NotificationServiceImpl.notifyOfferResponseFromCandidate(offer, accepted, cleanReason)`.
    - `NotificationServiceImpl` tạo thông báo in-app gửi tới HR tạo đề xuất (`offer.getProposedBy()`):
      - Tiêu đề & nội dung tương ứng `OFFER_ACCEPTED` (kèm thông báo hồ sơ chuyển sang Hired) hoặc `OFFER_DECLINED` (kèm lý do từ chối nếu có).
    - `NotificationServiceImpl` kiểm tra xem Requisition có Hiring Manager hay không; nếu có và Hiring Manager khác với HR tạo đề xuất, hệ thống tiếp tục tạo thêm một thông báo in-app gửi tới tài khoản của Hiring Manager.
16. `CandidateOfferServiceImpl` trả về chức danh công việc (`postingTitle`).
17. `CandidateOfferController` bổ sung flash attributes (`successMessage`, `decisionType = "ACCEPTED"` hoặc `"DECLINED"`) và điều hướng `redirect:/portal/offers/{id}`.
18. **Candidate UI** hiển thị trạng thái hoàn tất: huy hiệu trạng thái mới (`Đã nhận offer` - `badge--success` hoặc `Đã từ chối` - `badge--neutral`), thông điệp chúc mừng / xác nhận, và ẩn các nút thao tác.

### Alternative / Conditional Flows

#### alt Không có quyền sở hữu hoặc không tìm thấy Thư mời
- **Điều kiện:** `CandidateOfferRepository.findVisibleById(...)` trả về rỗng (do sai `userId` hoặc offer ở trạng thái nội bộ).
- `CandidateOfferServiceImpl` ném `ResourceNotFoundException("Không tìm thấy thư mời nhận việc.")`.
- `GlobalExceptionHandler` bắt ngoại lệ và hiển thị trang lỗi `error/404`.

#### alt Thư mời đã được trả lời trước đó (Race condition hoặc tải lại trang)
- **Điều kiện:** `offer.getOfferStatus() != "Sent_Candidate"` hoặc query `answerIfSent` trả về 0 dòng.
- `CandidateOfferServiceImpl` ném `OfferResponseException`.
- `CandidateOfferController` bắt ngoại lệ `OfferResponseException`, đặt flash attribute `errorMessage` và redirect lại trang chi tiết.

#### alt Quá thời hạn phản hồi hoặc quá ngày bắt đầu dự kiến
- **Điều kiện:** `now.isAfter(responseDeadline)` hoặc `expectedStartDate.isBefore(now.toLocalDate())`.
- `CandidateOfferServiceImpl` ném `OfferResponseException` tương ứng.
- `CandidateOfferController` bắt lỗi, thêm flash message cảnh báo đề xuất đã hết hạn phản hồi và chuyển hướng.

#### alt Accept Branch (Ứng viên nhận Offer)
- `toStatus = "Accepted"`.
- `OfferProposal.OfferStatus` chuyển thành `"Accepted"`.
- `Application.ApplicationStatus` chuyển thành `"Hired"`.
- Thông báo `OFFER_ACCEPTED` được gửi tới HR và Hiring Manager.

#### else Decline Branch (Ứng viên từ chối Offer)
- `toStatus = "Declined"`.
- `OfferProposal.OfferStatus` chuyển thành `"Declined"`.
- `OfferProposal.DeclineReason` được lưu lại nếu ứng viên có nhập lý do.
- `Application.ApplicationStatus` chuyển thành `"Rejected"`.
- Thông báo `OFFER_DECLINED` được gửi tới HR và Hiring Manager (kèm trích dẫn lý do từ chối).

### Database Effects

| Table / Entity | Operation | Important Fields / State |
|---|---|---|
| `OfferProposal` | `UPDATE` (nguyên tử) | `OfferStatus = 'Accepted'` hoặc `'Declined'`, `DeclineReason = cleanReason` (nếu từ chối), `UpdatedAt = CURRENT_TIMESTAMP` |
| `Application` | `UPDATE` (nguyên tử) | `ApplicationStatus = 'Hired'` (khi Accept) hoặc `'Rejected'` (khi Decline), `UpdatedAt = CURRENT_TIMESTAMP` |
| `Notification` (Đánh dấu đọc) | `UPDATE` | `IsRead = 1`, `ReadAt = CURRENT_TIMESTAMP` đối với thông báo offer của candidate |
| `Notification` (Tạo mới) | `INSERT` (1 cho HR, 1 cho Hiring Manager nếu có) | `RecipientId = hrUserId / hmUserId`, `ActorId = candidateUserId`, `Title`, `Content`, `EventType = 'OFFER_ACCEPTED'` hoặc `'OFFER_DECLINED'`, `ReferenceId = offerId`, `LinkUrl = '/offers'`, `IsRead = 0` |

### Notifications / Emails
- **Emails:** Trong mã nguồn hiện tại, phản hồi của ứng viên **không** kích hoạt gửi email qua `OfferMailSender` mà tập trung vào thông báo tức thời trong hệ thống.
- **In-App Notifications:**
  - Thông báo cho HR tạo đề xuất (`offer.getProposedBy()`) với sự kiện `OFFER_ACCEPTED` hoặc `OFFER_DECLINED`.
  - Thông báo cho Hiring Manager của Requisition (`requisition.getHiringManager()`) nếu có tài khoản và khác với HR.

### Final State
- Nếu Ứng viên Đồng ý:
  - `OfferProposal.OfferStatus = 'Accepted'`.
  - `Application.ApplicationStatus = 'Hired'`.
  - Ứng viên sẵn sàng cho giai đoạn Onboarding.
- Nếu Ứng viên Từ chối:
  - `OfferProposal.OfferStatus = 'Declined'`.
  - `OfferProposal.DeclineReason` được ghi nhận.
  - `Application.ApplicationStatus = 'Rejected'`.

### PlantUML Notes
- **Actor:** `actor Candidate`
- **Participants:**
  - `boundary "Candidate UI" as UI`
  - `control "CandidateOfferController" as Controller`
  - `participant "CandidateOfferServiceImpl" as Service`
  - `participant "CurrentUserService" as CurrentUser`
  - `database "CandidateOfferRepository" as CandOfferRepo`
  - `participant "ApplicationStatusService" as AppStatusService`
  - `database "ApplicationRepository" as AppRepo`
  - `participant "NotificationServiceImpl" as NotifService`
- **UML Constructs:**
  - `alt Offer Not Found / Not Owner`: Ném `ResourceNotFoundException`.
  - `alt Offer Expired / Already Answered`: Ném `OfferResponseException`.
  - `alt Candidate Accepts`:
    - `CandOfferRepo.answerIfSent(id, "Accepted")`
    - `AppStatusService.onOfferAnswered(appId, true)` -> `AppRepo.transition(appId, ["Offered"], "Hired")`
    - Gửi thông báo `OFFER_ACCEPTED` cho HR & Hiring Manager.
  - `else Candidate Declines`:
    - `CandOfferRepo.answerIfSent(id, "Declined", reason)`
    - `AppStatusService.onOfferAnswered(appId, false)` -> `AppRepo.transition(appId, ["Offered"], "Rejected")`
    - Gửi thông báo `OFFER_DECLINED` cho HR & Hiring Manager.

### Source Discrepancies / Risks
- **Ràng buộc kiểm tra hạn kép:** Hệ thống kiểm tra cả `responseDeadline` (ngày giờ cụ thể) và `expectedStartDate` (ngày đi làm). Nếu ngày hiện tại vượt quá mốc nào thì cũng coi là đã hết hạn phản hồi (`isExpired = true`).
- **Giao diện từ chối:** Việc từ chối có thể thực hiện thông qua màn hình riêng `/portal/offers/{id}/decline` (có ô nhập lý do) hoặc gọi trực tiếp từ modal xác nhận nếu không nhập lý do (`declineReason` để trống).

# KẾ HOẠCH HOÀN THIỆN REQUISITION VÀ JOB POSTING — ITERATION 2

- **Người phụ trách:** Nguyễn Huy Hoàng — HoangNH.
- **Ngày lập:** 03/10/2026.
- **Trạng thái:** Kế hoạch triển khai; chưa đánh dấu chức năng hoặc test mới là hoàn thành.
- **Đối chiếu công việc:** [Checklist HoangNH](checklist.md), mục VI.
- **Quy chuẩn:** [ARCHITECTURE_GUIDE.md](../../architecture/ARCHITECTURE_GUIDE.md).
- **Mục tiêu:** Hoàn thiện luồng từ yêu cầu tuyển dụng của Hiring Manager đến tin tuyển dụng do HR đăng, kèm thông báo cho đúng người và kiểm thử trên database.

## 1. Phạm vi và kết quả cần bàn giao

| Phần việc | Phạm vi | Kết quả cần có |
| --- | --- | --- |
| Requisition từ Iter1 | 5.1.14, 5.1.15, 5.1.16 | HR chỉ xem Approved; HM nhận thông báo riêng khi bị reject; giữ luồng tạo/sửa/duyệt hiện tại |
| Internal Job Management | 5.1.18 — HoangNH | HR xem danh sách, lọc, tìm kiếm, phân trang và xem chi tiết posting nội bộ |
| Post/Update Job Screen | 5.1.19 — HoangNH | HR tạo, lưu Draft, sửa và publish posting từ requisition Approved |
| Notification | Hỗ trợ hai nhánh swimlane | HM nhận thông báo Requisition Rejected và Job Posting Published, có trạng thái đã đọc/chưa đọc |
| Tích hợp public | Phối hợp DungLT, 5.1.34 và 5.1.35 | Tin hợp lệ xuất hiện trên public list/detail; tin nội bộ không bị lộ qua URL trực tiếp |
| Kiểm thử và tài liệu | Toàn bộ phần trên | Test tự động phù hợp, kết quả Integration/Security Test và bằng chứng demo |

Không đưa chấm điểm CV, ứng tuyển, lịch phỏng vấn hoặc offer vào phần triển khai này. Không refactor toàn bộ RequisitionServiceImpl chỉ để làm file ngắn hơn. Chỉ tách thành phần khi thay đổi đang thực hiện cần đến và có test bảo vệ hành vi cũ.

## 2. Hiện trạng đã đối chiếu với code

| Thành phần | Hiện trạng | Việc cần làm |
| --- | --- | --- |
| RequisitionController / Service / Validator | Có create, draft, submit, update, approve, reject, withdraw, copy, delete và lịch sử | Giữ nghiệp vụ; sửa scope HR và tích hợp thông báo |
| RequisitionServiceImpl.scope() | HR và Director cùng xem trạng thái khác Draft | Tách HR chỉ Approved; áp dụng cả count và phân trang |
| RequisitionAccess.requireView() | Chủ sở hữu/Admin được xét trước; HR xem các trạng thái khác Draft | HR phải chịu quy tắc chỉ Approved kể cả tài khoản từng là HM và còn sở hữu request cũ |
| JobPosting entity/repository | Có trường nội dung, người tạo, requisition nguồn, ngày đăng, deadline, status | Thêm version và các ràng buộc cần thiết; chốt policy trước migration |
| JobPostingService | Phục vụ đọc public, chưa có luồng tạo/sửa/publish nội bộ | Thêm service ghi riêng hoặc bổ sung có phân tách trách nhiệm rõ |
| Public detail | getPublishedJobDetail() lấy theo ID, chưa chặn status ngoài Published | Kiểm tra policy public tại truy vấn/service, không chỉ ở nút Apply |
| Public deadline | findPublishedJobs() loại deadline null; findOpenPosting(s) cho phép null | Thống nhất một policy cho list, detail và khả năng ứng tuyển |
| Public route | JobPostingController ghép `/jobs` với `/jobs/{id}` thành `/jobs/jobs/{id}` | Phối hợp DungLT rà route/link hiện dùng trước khi đổi, giữ tương thích cần thiết |
| Notification | Sidebar có `/notifications`; dashboard báo chưa có bảng/service | Xây dựng backend, dữ liệu và UI; không coi link menu là chức năng đã hoàn thành |
| GlobalExceptionHandler | BaseBusinessException đang đưa về view error/500 | Bổ sung xử lý lỗi nghiệp vụ/form phù hợp cho code mới; không dùng trang 500 cho mọi lỗi nhập liệu |
| Test | Lần rà soát: 147 validator Passed, 4 service Passed, 11 controller Errors | Sửa cấu hình và chạy lại; kết quả này không chứng minh toàn bộ luồng DB đã Passed |

Đây là mốc hiện trạng để lập kế hoạch. Khi bắt đầu từng giai đoạn phải kiểm tra lại diff, schema và code mới từ các thành viên khác, không ghi đè công việc đang thay đổi.

## 3. Quy tắc đã chốt và các quyết định còn mở

### 3.1. Quy tắc bắt buộc

1. HR chỉ được xem requisition `Approved`, bao gồm list, count, detail và ô chọn nguồn posting.
2. HM tạo/sửa requisition của mình theo trạng thái hiện có; Director quyết định request Pending và không tự duyệt.
3. Reject phải có lý do. HM nhận thông báo riêng ngoài nội dung trong detail/timeline.
4. HR chỉ tạo và publish posting từ requisition vẫn Approved tại thời điểm xử lý trên server.
5. Sửa nội dung posting không cập nhật ngược requisition đã được duyệt.
6. HM nhận thông báo khi posting được publish thành công. Người nhận là chủ requisition, không phải HR tạo tin.
7. Quyền, owner, status và thời gian hệ thống phải do server xác định; không tin hidden input.
8. Không tạo thông báo thành công khi thao tác nghiệp vụ thất bại hoặc transaction rollback.

### 3.2. Đề xuất để nhóm chốt trước phần triển khai phụ thuộc

| ID | Quyết định | Phương án đề xuất | Ảnh hưởng cần kiểm tra |
| --- | --- | --- | --- |
| DEC-01 | Kênh thông báo | In-app là bắt buộc; email là phần bổ sung sau | Có thể hoàn thành nhánh nhận feedback bằng in-app; nếu nhóm yêu cầu email thì thêm test gửi/retry |
| DEC-02 | Ai quản lý posting nội bộ | HR quản lý posting; HM nhận thông báo và xem bản public. Không cấp quyền ghi cho Admin khi chưa chốt | Security matcher, PostingAccess, menu và test ma trận role |
| DEC-03 | HR được sửa tin nào | Đề xuất mọi HR Active quản lý các tin trong phạm vi tuyển dụng chung, không tự thêm giới hạn theo phòng ban/owner | Nếu nhóm chọn owner-only phải đổi query scope, thao tác edit/publish và test tương ứng |
| DEC-04 | Số posting cho một requisition | Đề xuất tối đa một posting đang hoạt động (Draft/Published); tái đăng sau Closed cần quy tắc riêng | Ràng buộc DB và khóa khi tạo; không áp đặt lên dữ liệu cũ trước khi rà trùng |
| DEC-05 | Sửa Published | Đề xuất cho HR sửa nội dung với version, audit; không phát lại notification Published khi chỉ sửa | Cần xác nhận nội dung đổi có hiệu lực public ngay, hay phải tạo bản sửa chờ publish |
| DEC-06 | Đổi requisition nguồn sau tạo | Đề xuất không cho đổi; chọn đúng Approved ngay từ Create | Request DTO, validator và UI readonly khi edit |
| DEC-07 | Deadline | Đề xuất cho null nghĩa là không ấn định hạn; nếu có thì phải tương lai khi publish | Đồng bộ public list/detail, timezone và test biên; tránh hai query hiểu null khác nhau |
| DEC-08 | Tin Published hết hạn | Đề xuất ẩn khỏi danh sách còn tuyển nhưng detail vẫn xem được với thông báo hết hạn, không Apply | Chốt cùng DungLT; trạng thái ngoài Published vẫn không public |
| DEC-09 | Pause/Close/Reopen, lịch đăng | Chưa đưa vào luồng bắt buộc của Iter2 | Không hiển thị action chưa có business rule/test; vẫn chặn truy cập public cho Paused/Closed |

Ghi quyết định cuối cùng và ngày chốt ngay tại bảng này trước khi viết migration hoặc test phụ thuộc. Các quyết định trên là đề xuất, không phải yêu cầu đã được người dùng xác nhận. Có thể triển khai sửa quyền HR và nền test trong khi chờ chốt policy posting.

## 4. Hai luồng nghiệp vụ cần chạy trọn vẹn

### 4.1. Nhánh từ chối và gửi lại

1. HM tạo requisition, nhập dữ liệu và Submit to Director.
2. Server validate; lỗi trả lại form và giữ input; hợp lệ lưu `Pending_Director`.
3. Director mở đúng request, chọn Reject và nhập feedback.
4. Trong một transaction: kiểm tra quyền/status dưới khóa ghi, lưu `Rejected`, approval, event, audit và notification cho HM.
5. HM thấy thông báo chưa đọc, mở được detail với lý do từ chối.
6. Nếu HM tiếp tục: sửa request đó, validate, resubmit cùng ID, giữ lịch sử reject trước đó.
7. Nếu HM không tiếp tục: request giữ Rejected; không tự xóa hoặc đổi trạng thái ngoài sơ đồ.

### 4.2. Nhánh duyệt và đăng tin

1. HM submit hợp lệ; Director approve; server lưu Approved và lịch sử.
2. HR thấy request Approved, chọn Create Job Posting.
3. Form gợi ý dữ liệu nguồn; HR bổ sung nội dung công khai, benefits, salary display và deadline.
4. Lưu Draft theo các trường tối thiểu đã chốt; Draft chỉ hiển thị nội bộ.
5. HR chọn Publish. Server kiểm tra lại Approved, quyền, dữ liệu, version và chống trùng.
6. Khi có lỗi: trả form/thông báo phù hợp; không public, không gửi thông báo thành công.
7. Khi hợp lệ: lưu Published, postingDate, audit và notification cho chủ requisition trong cùng transaction.
8. Tin xuất hiện ở public list/detail theo policy deadline; HM nhận notification và mở tin được đăng.

Save Draft là bước hỗ trợ lưu tiến độ. Không bắt người dùng tạo lại một posting khác khi publish bản Draft hiện có. Việc hỗ trợ nút Publish ngay trên form Create chỉ thực hiện nếu cùng dùng một transaction và cùng quy tắc chống trùng.

## 5. Kiến trúc và danh sách thành phần dự kiến

Các đường dẫn dưới đây tính từ root repository. Tên file mới là thiết kế đề xuất; tái sử dụng thành phần tương đương nếu dự án đã bổ sung trong lúc triển khai.

| Nhóm | File/thành phần | Thay đổi dự kiến |
| --- | --- | --- |
| Cấu hình test | `pom.xml`, `src/test/resources/application-test.properties` | Security test dependency và kết nối DB test qua biến môi trường |
| HR scope | `src/main/java/com/group2/rms/requisition/service/RequisitionAccess.java`, `RequisitionServiceImpl.java` | Quyền Approved-only, query/count nhất quán |
| Requisition UI | `requisition/controller/RequisitionController.java`, `templates/requisitions/list.html`, `detail.html` | Option phù hợp role, link Create Posting đúng quyền |
| Posting entity/data | `requisition/entity/JobPosting.java`, `repository/JobPostingRepository.java` | Version, query nội bộ, khóa/ràng buộc và điều kiện public |
| Posting nghiệp vụ | `requisition/service/InternalJobPostingService.java`, `JobPostingAccess.java`, `validator/JobPostingValidator.java` | Query nội bộ, create/update/publish, quyền và validation |
| Posting DTO | `requisition/dto/JobPostingRequest.java`, `InternalJobPostingResponse.java` | Record cho code mới, không nhận entity trực tiếp hoặc field hệ thống từ client |
| Posting web | `requisition/controller/InternalJobPostingController.java` | Endpoint nội bộ riêng, controller mỏng |
| Posting UI | `templates/job-postings/list.html`, `form.html`, `detail.html` | Reuse layout/fragments; CSS/JS riêng trong static |
| Public tích hợp | `requisition/service/JobPostingService.java`, `controller/JobPostingController.java` | Đồng bộ policy public và route với DungLT |
| Notification | Package `com.group2.rms.notification` | Dự kiến entity, repository, service, controller, request/response; giữ package phẳng nếu dưới 10 file theo guide |
| Notification UI | `templates/notifications/list.html`, fragments header/sidebar, static CSS/JS | Danh sách, badge, read action; không thêm logic gửi email ở frontend |
| Lỗi nghiệp vụ | `core/exception/GlobalExceptionHandler.java` và exception đặc thù posting | Trả lỗi nhập liệu về form; giữ 403/404/409 phù hợp |
| Database | `database/migrations/<next>_job_posting_notification.sql` | Chọn số tiếp theo sau khi kiểm tra migration của nhóm, không ghi đè script cũ |

Quy tắc triển khai: constructor injection; service đọc dùng `@Transactional(readOnly=true)`, ghi dùng transaction; mapping DTO trong phạm vi transaction; Bean Validation cho input và validator cho quy tắc chéo. Controller mới không tự try/catch nghiệp vụ; xử lý qua advice chung với context form đủ để render lại input và options.

Không đưa nghiệp vụ notification vào `core` chỉ vì nhiều module dùng. Chiều gọi đề xuất: Requisition/Posting Service → NotificationService → NotificationRepository. NotificationService nhận recipient và dữ liệu sự kiện, không gọi ngược RequisitionService/PostingService. Có thể dùng `CurrentUserService` cho actor Active thay vì đọc session và username tùy ý.

## 6. Ma trận quyền và endpoint dự kiến

### 6.1. Quyền Requisition cần giữ/sửa

| Actor | Xem list/detail | Tạo/sửa | Approve/Reject | Withdraw |
| --- | --- | --- | --- | --- |
| HM | Request của chính mình | Tạo; sửa Draft/Rejected của mình | Không | Pending của mình |
| Director | Các request không phải Draft | Không qua chức năng HM | Pending, không tự duyệt | Không request của người khác |
| HR | Chỉ Approved, không có ngoại lệ ownership | Không | Không | Không |
| System Admin | Giữ quyền xem/sửa hiện hành | Không mở rộng ngoài code hiện có | Không tự cấp quyền Director | Giữ quy tắc owner + Pending hiện có |
| Guest/Candidate | Không truy cập module nội bộ | Không | Không | Không |

HR filter `status=Draft/Pending_Director/Rejected` trả danh sách rỗng trong scope HR; detail của bản ghi tồn tại nhưng không được phép xem trả 403. ID không tồn tại trả 404. Quy tắc HR phải đứng trước nhánh `owns()` để tránh ngoại lệ khi đổi role tài khoản.

### 6.2. Hợp đồng HTTP mới đề xuất

| Method/route | Actor | Hành vi và kết quả |
| --- | --- | --- |
| GET `/internal/job-postings` | HR theo DEC-02/03 | 200 list có paging/filter; chỉ dữ liệu trong scope |
| GET `/internal/job-postings/create?requisitionId={id}` | HR | 200 form từ Approved; 403 khi role sai, 404 khi ID không tồn tại, báo lỗi nghiệp vụ nếu nguồn chưa Approved |
| POST `/internal/job-postings/create` | HR + CSRF | Tạo Draft hợp lệ; 302 về detail; form lỗi 200 với input/errors |
| GET `/internal/job-postings/{id}` | HR | 200 detail nội bộ; 403/404 theo quyền và tồn tại |
| GET `/internal/job-postings/{id}/edit` | HR | 200 form có version; chỉ trạng thái được DEC-05 cho sửa |
| POST `/internal/job-postings/{id}/edit` | HR + CSRF | Update cùng ID; 302 khi thành công; 200 lỗi field, 409 khi stale version |
| POST `/internal/job-postings/{id}/publish` | HR + CSRF | Chuyển Draft sang Published; 302 về detail khi thành công; 409 cho stale/transition không hợp lệ |
| GET `/notifications` | User Active | 200 danh sách của user đang đăng nhập; không nhận recipientId từ query để đổi người xem |
| GET `/notifications/unread-count` | User Active | Count chỉ của user hiện tại, nếu UI dùng cập nhật badge |
| POST `/notifications/{id}/read` | Chủ thông báo + CSRF | Đánh dấu đọc idempotent; 302 về danh sách, 404 cho ID không thuộc người dùng |

Đường dẫn mới phải được khai báo trong SecurityConfig trước matcher tổng quát. GET không thay đổi read state; không dùng link GET để publish, reject hay đánh dấu đã đọc. Notification chứa liên kết được server tạo, không nhận URL chuyển hướng tùy ý từ client.

Hợp đồng lỗi của posting mới không tự thay đổi HTTP behavior của Requisition cũ. Lỗi field phải gắn lại request DTO, danh sách Approved, options và quyền viewer trước khi trả form; lỗi stale phải hướng dẫn reload, không tự submit lại dữ liệu cũ.

## 7. Thiết kế dữ liệu và transaction

### 7.1. JobPosting

- [ ] Kiểm tra schema thực tế trước migration; so sánh độ dài/nullability với entity và dữ liệu hiện có.
- [ ] Bổ sung `Version` kiểu phù hợp JPA `@Version`; backfill giá trị cho bản ghi cũ và giữ nguyên ID/status/nội dung.
- [ ] `RequisitionId` bắt buộc trỏ tới nguồn tồn tại; tính Approved được kiểm tra trong service khi tạo/lưu/publish.
- [ ] `CreatedBy` lấy từ actor HR; `PostingDate` do server đặt khi publish lần đầu; DTO không có quyền tự gán các field này.
- [ ] Thực thi DEC-04 bằng ràng buộc DB phù hợp và locking, không chỉ `exists()` rồi `save()` dễ race. Nếu dữ liệu cũ vi phạm thì báo danh sách để xử lý, không tự xóa/gộp.
- [ ] Chọn chỉ mục theo query list thực tế: status, ngày đăng/ngày tạo, requisition; giữ phân trang và sort ổn định bằng ID phụ.

### 7.2. Notification

| Trường đề xuất | Ý nghĩa/ràng buộc |
| --- | --- |
| NotificationId | ID tự sinh |
| RecipientUserId | User nhận; có foreign key và index phục vụ list/count |
| EventType | `REQUISITION_REJECTED` hoặc `JOB_POSTING_PUBLISHED` |
| EventKey | Mã sự kiện ổn định, unique cùng RecipientUserId để chống gửi trùng |
| EntityType / EntityId | Loại và ID đối tượng để server dựng link, không lưu URL do client cung cấp |
| Title / Body | Snapshot nội dung thông báo có giới hạn độ dài thống nhất |
| CreatedAt / ReadAt | Thời gian server; ReadAt null là chưa đọc |

Đề xuất giữ notification khi requisition Rejected bị HM xóa theo quyền hiện hành: không dùng cascade delete notification theo requisition; link đích không còn thì hiển thị trạng thái không khả dụng/404 phù hợp. Chỉ giữ snapshot thông báo cho đúng recipient, không làm lộ đối tượng mới hoặc đối tượng khác.

### 7.3. Tính nhất quán và chống trùng

- Rejection: dùng ID approval/workflow event đã tạo trong cùng transaction làm nguồn EventKey. Mỗi lần reject mới sau resubmit có key mới; retry cùng sự kiện dùng lại key cũ.
- Publish: key ổn định theo lần publish hợp lệ; nếu Iter2 chỉ có publish lần đầu, dùng posting ID và loại sự kiện. Không dùng UUID mới mỗi lần retry làm khóa chống trùng.
- Giao dịch cùng database bao gồm state, audit/event và notification. Không dùng `REQUIRES_NEW` cho notification vì có thể thông báo thành công dù nghiệp vụ rollback.
- Lock/check version phải được thực hiện trước transition. Nếu cần khóa cả requisition và posting, thống nhất thứ tự khóa giữa các thao tác để giảm deadlock.
- Submit/publish lặp với version cũ trả lỗi xung đột và không thêm notification/audit thành công. Hành động đọc thông báo lặp giữ nguyên ReadAt đầu tiên.
- Nếu notification không lưu được thì rollback giao dịch nghiệp vụ trong phương án cùng DB. Nếu chọn gửi bất đồng bộ phải bổ sung outbox bền vững và test retry; không chỉ phát event trong RAM rồi coi như đã giao thông báo.
- Email, nếu được chọn, xử lý sau commit với retry; SMTP lỗi không đảo ngược quyết định đã commit. Không gửi email trong transaction đang giữ khóa.

## 8. Kế hoạch triển khai theo giai đoạn

### Giai đoạn A — Khôi phục nền test và fixture (I2-TEST-01/02)

1. Kiểm tra diff trước khi sửa; giữ các file test và tài liệu người dùng đang thay đổi.
2. Thêm `org.springframework.security:spring-security-test` scope test theo dependency management hiện có.
3. Rebuild test từ source để loại bỏ lỗi class cũ không resolve `WithMockUser`/`csrf()`; kiểm tra lại Java 21 và Maven path.
4. Chạy riêng validator/service/controller tests. Phân biệt lỗi test fixture, lỗi security setup, lỗi template và lỗi nghiệp vụ; không xóa assertion chỉ để đạt Passed.
5. Tạo profile test SQL Server riêng; credentials lấy qua môi trường. Khởi tạo schema/migration trên DB test, không dùng profile mặc định kết nối dữ liệu demo.
6. Fixture gồm HM-A/HM-B, Director, HR-A/HR-B, Admin, Candidate; bốn trạng thái requisition và các trạng thái posting. Reset/rollback có kiểm soát từng test.

**Đầu ra:** Bộ test hiện có chạy được; báo cáo mới chỉ rõ test Passed/Failed; cấu hình DB test độc lập. Không cần chạy toàn bộ module không liên quan khi chưa có thay đổi chung tác động chúng.

### Giai đoạn B — HR chỉ xem Approved (I2-REQ-01…04)

1. Viết regression test bắt hành vi HR đang xem Pending/Rejected.
2. Sửa scope: HM own, Director non-Draft, HR Approved, Admin theo quyền cũ. Dùng cùng predicate cho list/count.
3. Sửa access detail với HR trước nhánh owner; kiểm tra actor Active từ database.
4. Điều chỉnh filter UI theo role. Giữ filtering ở server dù UI chỉ có Approved.
5. Thêm điểm vào Create Job Posting trên Approved detail/list cho HR khi route đã sẵn sàng; không hiển thị nút dẫn tới endpoint chưa triển khai.
6. Test HR tự gửi status khác, thay ID, đổi page/size và tài khoản HR từng sở hữu request khi còn role HM.

**Đầu ra:** Không rò Draft/Pending/Rejected qua list/count/detail của HR; HM/Director không bị mất quyền hợp lệ.

### Giai đoạn C — Notification nền và reject (I2-NOTI-01…06)

1. Tạo migration Notification, entity/repository/service theo thiết kế mục 7; kiểm tra chưa có triển khai tương đương từ thành viên khác.
2. Viết service list/count/read luôn lấy actor hiện tại. Không cho truy vấn tùy ý notification của recipient khác.
3. Thêm tạo notification vào nhánh reject thành công của `decide()`, đúng chủ requisition và cùng transaction.
4. Tạo notification list: title, thời gian, snippet, read state, liên kết; empty state rõ, text đã escape.
5. Kết nối menu `/notifications` đã có, badge unread từ DB. Nếu dùng polling phải giới hạn tần suất; không cần websocket cho phạm vi Iter2 này.
6. Giữ feedback trong detail/history; notification bổ sung cách HM biết có phản hồi, không thay thế lịch sử.
7. Test rollback, trùng request, read ownership và reject lần hai sau resubmit.

**Đầu ra:** Demo được Director reject → HM nhận thông báo → mở detail → sửa → resubmit, không dùng thao tác đọc trực tiếp DB thay cho UI thông báo.

### Giai đoạn D — Posting data, quyền và danh sách (I2-JOB-01…04)

1. Ghi quyết định DEC-02/03/04 vào tài liệu trước thiết kế constraint/access.
2. Migration Version và index/ràng buộc; xác minh query cũ public còn hoạt động.
3. Thêm `JobPostingAccess` và service list nội bộ; dữ liệu thật, filter status/department/title, page-size có giới hạn, sort whitelist.
4. Thêm routes nội bộ và SecurityConfig matcher cho đúng role; public `/jobs` giữ tách biệt.
5. Xây list/detail nội bộ, trạng thái rỗng/loading/error, action theo quyền; liên kết requisition nguồn vẫn kiểm tra Approved-only của HR.
6. Reuse layout/sidebar/global CSS; chỉ thêm CSS/JS riêng cần thiết. Chạy kiểm tra giao diện desktop và màn hình hẹp cho bảng/form.

**Đầu ra:** HR xem danh sách/chi tiết posting nội bộ từ DB, role ngoài phạm vi bị chặn ở server.

### Giai đoạn E — Form Create/Update và validation (I2-JOB-05…11)

1. GET Create nhận nguồn Approved; prefill dữ liệu qua DTO, không tạo entity trong GET. Phân biệt nội dung gợi ý với dữ liệu đã lưu.
2. Tạo request record chỉ chứa field được nhập và version khi edit. Có validation groups hoặc validator phân biệt Draft/Publish.
3. Quy tắc Draft tối thiểu theo schema hiện tại: nguồn Approved, title không rỗng ≤200, description/requirements có nội dung. Không nới nullable hoặc tạo placeholder âm thầm chỉ để lưu form rỗng.
4. Quy tắc field khác: salaryDisplay ≤100, workLocation ≤255; giới hạn description/requirements/benefits thống nhất với form và backend trước khi code. Không áp dụng so sánh số trực tiếp lên salaryDisplay vốn là chuỗi hiển thị.
5. Publish kiểm tra mọi trường công khai bắt buộc, deadline theo DEC-07, nguồn còn Approved. Server là nơi quyết định cuối cùng dù browser có validation.
6. Create: gán creator từ session, trạng thái Draft, audit CREATE. Edit: tải entity có khóa/version phù hợp, cập nhật field được phép, giữ ID/creator/requisition nguồn theo DEC-06.
7. Thêm xử lý lỗi cụ thể qua GlobalExceptionHandler để giữ form/input/options. Không chuyển lỗi nhập liệu vào trang error/500. Không sửa response contract cũ ngoài phạm vi cần thiết.
8. Test field thiếu/sai/Unicode, injected owner/status/date, stale version, invalid source và create song song cùng requisition theo DEC-04.

**Đầu ra:** Lưu Draft → reload → edit cùng ID chạy đúng, không có bản ghi dở dang hoặc thay đổi requisition nguồn.

### Giai đoạn F — Publish và thông báo đăng tin (I2-PUB-01…06)

1. Chốt chính sách Published edit/deadline, route detail public cùng DungLT.
2. Trong transaction publish: xác thực actor → tải/khóa nguồn và posting theo thứ tự thống nhất → kiểm tra version/status/Approved → validate → cập nhật Published/postingDate → ghi audit và notification HM.
3. Không sửa `postingDate` mỗi lần cập nhật nội dung Published; không gửi lại thông báo đăng tin chỉ vì edit.
4. Bổ sung điều kiện public cho detail, không chỉ list. Chặn Draft/Paused/Closed theo ID; giữ hành vi hết hạn theo DEC-08.
5. Đồng bộ deadline null và mốc thời gian giữa các query list/detail/accepting-applications. Định nghĩa timezone server và test sát deadline, không dựa vào đồng hồ máy khác.
6. Kiểm tra liên kết notification Published và link public list trỏ đúng route thực tế; không tự đổi route làm hỏng phần Candidate.
7. Test double-click/retry/rollback: tối đa một transition thành công cho cùng version và một notification cho sự kiện đó.

**Đầu ra:** HR publish → public hiển thị đúng → HM nhận thông báo mở được tin, với DB và lịch sử nhất quán.

### Giai đoạn G — Hoàn thiện tài liệu, regression và demo (I2-QA-01…07)

1. Cập nhật bộ Security Test: tách kỳ vọng HR khỏi Director trong case Draft/Pending visibility cũ. HR xem Pending là lỗi, không phải positive case nữa.
2. Bổ sung test đầy đủ nhánh reject → notification và approve → posting → publish → notification.
3. Chạy lại các test bị ảnh hưởng bởi SecurityConfig, GlobalExceptionHandler, header/sidebar và public service; phối hợp xác nhận phần của DungLT.
4. Ghi kết quả từng lần chạy vào Integration/Security Test, kèm ngày, tester HoangNH, dữ liệu, HTTP thực tế và Defect ID.
5. Cập nhật checklist theo bằng chứng; ghi rõ case chưa chạy/Blocked thay vì đánh dấu Passed dựa trên source code.

## 9. Ma trận kiểm thử tối thiểu

| Mã nhóm | Kịch bản bắt buộc | Tầng kiểm tra / kết quả chính |
| --- | --- | --- |
| T-REQ-01 | HR list/count/detail với đủ bốn status | Access + MVC + DB; chỉ Approved, status khác 403 khi detail |
| T-REQ-02 | HR đổi query/status/ID, HR từng là owner | Security; không bypass Approved-only |
| T-REQ-03 | HM submit; Director approve/reject; HM resubmit | Service + integration; đúng state, approval/event/audit |
| T-NOT-01 | Reject hợp lệ | Notification đúng HM, đúng feedback/link, chưa đọc |
| T-NOT-02 | Reject thiếu lý do, sai quyền, sai trạng thái | Không thay state, không thêm notification thành công |
| T-NOT-03 | Retry cùng sự kiện; reject mới sau resubmit | Một thông báo cho retry; thông báo mới cho quyết định mới |
| T-NOT-04 | HM khác list/read/count hoặc giả recipientId | Không lộ nội dung/count; read sai chủ bị chặn |
| T-NOT-05 | Read hai lần; đối tượng nguồn đã xóa | Read idempotent; link không còn xử lý rõ, không lỗi 500 |
| T-JOB-01 | HR create từ Approved rồi reload/edit | Đúng liên kết, owner, ID; không sửa nguồn |
| T-JOB-02 | Nguồn Draft/Pending/Rejected hoặc không tồn tại | Chặn ở GET chuẩn bị form và POST service; không có posting |
| T-JOB-03 | Thiếu field, quá độ dài, ký tự Unicode, deadline sai | Lỗi đúng field, giữ input; DB không ghi dở dang |
| T-JOB-04 | Giả CreatedBy, PostingStatus, PostingDate, đổi nguồn | Server bỏ qua/chặn field không được phép; không tự Published |
| T-JOB-05 | Hai HR edit/publish cùng version | Không lost update, lỗi 409 cho thao tác stale |
| T-JOB-06 | Hai request create cùng nguồn | Kết quả đúng policy DEC-04, không chỉ kiểm tra tuần tự |
| T-PUB-01 | Publish thành công | Published + thời điểm + audit + một notification HM |
| T-PUB-02 | Publish thất bại/rollback, retry | Không lộ tin hoặc gửi thông báo sai; không tạo trùng |
| T-PUB-03 | Public list/detail với Draft/Published/Paused/Closed | Chỉ tin được phép công khai; không bypass bằng URL |
| T-PUB-04 | Deadline null/quá hạn/đúng biên | List/detail/Apply nhất quán theo DEC-07/08 |
| T-SEC-01 | Guest/Candidate/HM/Director gọi endpoint ghi HR | Chặn theo role; POST có CSRF hợp lệ để cô lập quyền |
| T-SEC-02 | Thiếu/sai CSRF trên create/edit/publish/read/reject | 403, không mutation |
| T-SEC-03 | HTML/script trong nội dung và feedback | Không thực thi khi render ở form/detail/notification/public |
| T-UI-01 | Form lỗi, bảng rỗng, pagination, thông báo, màn hình hẹp | Không mất input, không link chết, không che nút thao tác |

Unit test dùng mock để kiểm tra logic. MVC test mock service chỉ chứng minh hợp đồng web; không thay thế integration test dùng repository/database thật. Integration persistence assertions cần flush/clear hoặc đọc ở giao dịch mới để không nhầm entity cache với dữ liệu thực đã lưu. Test concurrency dùng transaction/session riêng, không bọc mọi thao tác cạnh tranh trong cùng transaction test.

## 10. Chia phần bàn giao và phụ thuộc

| Đợt | Nội dung | Phụ thuộc | Bằng chứng kết thúc |
| --- | --- | --- | --- |
| 1 | Test dependency/profile và HR Approved-only | Không phụ thuộc policy posting | Test HR scope + regression HM/Director Passed |
| 2 | Notification data/UI và reject integration | Đợt 1, thống nhất module chung | Demo reject → HM nhận thông báo → resubmit |
| 3 | Posting schema/access/list/detail nội bộ | DEC-02/03/04 và nền DB test | Migration test + role/list/detail tests |
| 4 | Create/Update Draft và lỗi form | Đợt 3, DEC-05/06/07 | Test field/mapping/concurrency và form reload |
| 5 | Publish/public integration/notification HM | Đợt 2/4, DEC-08, phối hợp DungLT | Demo approve → HR publish → public → HM nhận thông báo |
| 6 | Security/regression/tài liệu nghiệm thu | Tất cả đợt trước | Báo cáo test và checklist cập nhật theo kết quả thực tế |

Mỗi đợt nên có diff/commit tập trung để review và rollback riêng. Không commit file chứa credentials, log runtime, database dump hoặc kết quả build. Migration có hướng dẫn chạy, xác minh và khôi phục phù hợp dữ liệu; không tự drop bảng khi rollback phần ứng dụng.

## 11. Điều kiện nghiệm thu cuối cùng

- [ ] HR chỉ xem Approved ở mọi entry point liên quan, không có ngoại lệ do ownership cũ.
- [ ] Reject có feedback và notification riêng cho đúng HM; HM nhận được qua UI, sửa/resubmit thành công.
- [ ] Hai màn hình 5.1.18 và 5.1.19 hoạt động với database và đúng quyền HR đã chốt.
- [ ] Posting chỉ xuất phát từ Approved; nội dung public độc lập với requisition nguồn.
- [ ] Publish thành công mới public và thông báo HM; lỗi/retry không tạo dữ liệu hoặc thông báo sai/trùng.
- [ ] Public route/list/detail/deadline thống nhất với DungLT; không lộ tin nội bộ.
- [ ] Lỗi nhập liệu trở lại form; lỗi quyền/tồn tại/version có phản hồi đúng, không biến tất cả thành 500.
- [ ] Test thuộc phạm vi chạy trên môi trường phù hợp và có báo cáo mới; các lỗi còn lại được ghi rõ thay vì bỏ qua.
- [ ] Hai file Integration/Security Test, checklist và quyết định DEC-01…09 phản ánh trạng thái thực tế.
- [ ] Demo hoàn chỉnh cả hai nhánh swimlane bằng các tài khoản HM, Director, HR và dữ liệu test đã chuẩn bị.

**Bước triển khai đầu tiên:** Giai đoạn A và B — sửa nền test, viết test Approved-only và điều chỉnh quyền HR. Sau đó mới thêm Notification và Job Posting. Tài liệu này chưa cấp trạng thái hoàn thành cho bất kỳ chức năng mới nào.

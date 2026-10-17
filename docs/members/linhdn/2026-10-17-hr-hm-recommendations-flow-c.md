# Flow C — HR gợi ý ứng viên cho Hiring Manager

Ngày cập nhật: 17/10/2026. Tiếp nối Flow A/B; không commit/push.

## Luồng đã triển khai

HR mở tin tuyển dụng → Gợi ý ứng viên cho HM → chọn Candidate và CV cũ trong Talent Pool → kiểm tra nguồn/trùng → gửi gợi ý → HM mở Gợi ý từ HR → xem toàn bộ CV lịch sử, ghi chú → Chấp nhận hoặc Từ chối.

Chấp nhận: cùng bản nháp vào danh sách HM đề xuất, chờ HR liên hệ. Chưa chuyển sang phỏng vấn. Chỉ khi HR xác nhận ứng viên đồng ý dùng CV cũ mới chuyển sang `HM_Passed`, tiếp nối luồng Interview hiện có. Từ chối: giữ lịch sử, không đưa vào danh sách HM đề xuất hoặc hồ sơ ứng tuyển chính thức.

## Audit source và schema trước triển khai

| Thành phần | Kết quả đối chiếu |
| --- | --- |
| `database/schema/db.sql`: Application | `AppliedCvUrl` bắt buộc; constraint đã cho phép `HM_Suggested`, `HM_Passed`, `Rejected`. Mỗi gợi ý cần CV cũ hợp lệ. Không thêm trạng thái DB. |
| ApplicationReview | Có ApplicationId, ReviewerId, ReviewerRole, Decision, ReviewedAt. Constraint cho phép vai trò `HR`/`HiringManager`, quyết định `Hold`/`Pass`/`Fail`. Lưu được người gửi và phản hồi mà không ghi vào shared notes. |
| AuditLog | Cơ chế nguồn Talent Pool hiện có lưu EntityName, EntityId, UserId, OldValue/NewValue. Bổ sung mã sự kiện `TalentPoolHrRecommended`; OldValue giữ ID Application nguồn. Đây là metadata sự kiện, không phải nội dung ghi chú người dùng. |
| Candidate/TalentPoolNote | Tái sử dụng cờ IsPotential và ghi chú gắn Candidate, không tạo profile hoặc note entity thứ hai. |
| Notification | Có dữ liệu thông báo nhưng không dùng `IsRead` làm trạng thái nghiệp vụ. Flow C đọc trạng thái từ Application/Review; không phát email hoặc thông báo riêng trong lượt này. |
| Flow B/TalentPoolService | Dùng lại validation nguồn, batch, khóa Job Posting và transaction. Hai endpoint ghi riêng để HR không gọi nhầm nghiệp vụ HM chủ động thêm. |
| Application/portal/dashboard | Bộ lọc chính thức tiếp tục loại `HM_Suggested` và các dòng có audit `TalentPoolCancelled`. Gợi ý chờ HM không đi vào danh sách/số đếm chính thức. |
| Job Posting đóng | Kết thúc mọi bản nháp, gồm gợi ý chờ HM. Không nhầm các dòng này thành đơn chính thức bị loại. |
| Interview/Offer/AI | Không sửa module. HM chấp nhận gợi ý chưa đủ điều kiện Interview. HR consent dùng luồng `HM_Passed` đã có; không tự tạo lịch, Offer hoặc sự kiện AI. |

Không sửa file schema/migration/entity mapping/constraint, không bổ sung bảng/cột hoặc enum. Các lượt test DB đặt `spring.jpa.hibernate.ddl-auto=validate`.

## Lưu trạng thái bền vững

| Tình huống | Application | ApplicationReview | Audit và cách đọc |
| --- | --- | --- | --- |
| HM chủ động thêm — Flow B | HM_Suggested | Không có review gửi gợi ý HR của Flow C | Nguồn `TalentPoolSuggested`; chờ HR liên hệ như trước. |
| HR gửi — chờ HM | HM_Suggested | HR/Hold, lưu HR và thời gian thật; chưa có HiringManager/Pass hoặc Fail | Nguồn `TalentPoolHrRecommended`; chỉ nằm ở Gợi ý từ HR. |
| HM chấp nhận | Giữ nguyên HM_Suggested và ApplicationId | Thêm HiringManager/Pass | Thêm `TalentPoolSuggested` bởi HM; hiện trong danh sách HM đề xuất, vẫn bị loại khỏi pipeline chính thức. |
| HM từ chối | Rejected | Thêm HiringManager/Fail | `TalentPoolCancelled`; không vào danh sách HM đề xuất hoặc pipeline chính thức. |
| HR xác nhận ứng viên đồng ý | HM_Passed | Giữ nguyên lịch sử gửi/phản hồi | `TalentPoolActivated`; đủ điều kiện chờ phỏng vấn. |
| Candidate tự nộp CV mới | Applied theo luồng hiện có | Giữ lịch sử gợi ý | `TalentPoolCandidateSubmitted`; đây là hành động ứng tuyển thật của Candidate. Không sửa module nộp hồ sơ. |

`Hold` chỉ là đánh dấu gửi HR trong ngữ cảnh bản nháp có nguồn Flow C. Không diễn giải mọi review HR/Hold của hồ sơ chính thức thành gợi ý. Predicate chờ HM kiểm tra nguồn, trạng thái bản nháp, review HR/Hold và chưa có quyết định HM. Quyết định HM đầu tiên được giữ làm lịch sử gợi ý, không bị một review chuyên môn về sau ghi đè.

Không lưu quyết định trong localStorage/sessionStorage, ghi chú text hoặc Notification.IsRead. Draft trình duyệt chỉ giữ Candidate ID và source Application ID, theo User/Job Posting như Flow B.

## Backend, bảo mật và transaction

- `POST /talent-pool/jobs/{id}/recommend`: HR, JSON `sourceApplicationIds`, Bean Validation, CSRF; trả ApiResponse sau khi transaction hoàn tất.
- `POST /talent-pool/jobs/{id}/selection-review`: dùng chung cho preflight HR/HM, chỉ đọc. Backend kiểm tra lại toàn bộ nguồn khi gửi thật.
- `GET /talent-pool/jobs/{id}/recommendations`: HR theo dõi, HM xem trong phạm vi phòng ban theo RequisitionAccess hiện có; có tìm tên, phân trang và số đang chờ.
- `GET /talent-pool/{candidateId}?recommendationId={applicationId}`: tái sử dụng trang Candidate; kiểm tra gợi ý thuộc đúng Candidate và job được phép xem. Job và Back được dựng từ dữ liệu backend, không nhận returnUrl tùy ý.
- `POST /talent-pool/recommendations/{id}/decision`: chỉ HM; `Pass`/`Fail`; cả danh sách và trang chi tiết gọi cùng `decideRecommendation`.
- Khóa pessimistic Job Posting trước kiểm tra pending/ghi quyết định. Các đường thêm, Candidate nộp, HR confirm/decline và đóng job dùng cùng cơ chế khóa hiện có. Kiểm tra lại dưới khóa để request sau không xử lý lại kết quả trước.
- Chặn Candidate–Job Posting đã có hồ sơ chính thức hoặc bản nháp, chọn cùng Candidate nhiều lần, nguồn cùng job, nguồn không chính thức, Candidate không còn tiềm năng, thiếu CV, job không mở/hết hạn và thiếu HM phụ trách hợp lệ.
- Xác thực toàn bộ batch trước insert. Application mới tham chiếu Candidate và CV hiện có; review/audit nằm cùng transaction. Không sửa Application nguồn, không tạo Candidate.
- HR không thể confirm/decline khi gợi ý còn chờ HM. Link `/applications/{id}` của bản nháp chờ HM chuyển về màn review Candidate, tránh hiển thị nhầm bước HR liên hệ.

## UI và điều hướng

- Job Posting chỉ thêm hành động liên quan: HR có Gợi ý ứng viên cho HM; HM có Gợi ý từ HR với số đang chờ. Giữ các nút quản lý tin khác.
- Dùng lại trang Talent Pool, bộ lọc, phân trang, chọn nhiều, CV selector, preview, notes, draft và sticky bar. HR có nhãn gửi gợi ý, modal nêu tin đích/HM nhận; endpoint ghi tách với Flow B.
- Card gợi ý có tên, HR gửi, thời gian và badge. Icon `file-user` riêng cạnh badge mở trang Candidate; không dùng icon mắt để điều hướng hồ sơ.
- Trang Candidate có profile header, CV lịch sử trái và liên hệ/ghi chú phải, khu vực quyết định cuối trang. Icon mắt chỉ xem nhanh đúng CV bằng viewer cũ. Hiện tất cả CV từ Application chính thức cũ được phép xem, không chỉ CV nguồn HR chọn.
- Shared notes giữ tác giả/thời gian/nội dung, mới nhất trước, escape nội dung và giới hạn 1.000 ký tự. Form giữ ngữ cảnh recommendation, keyword/page và nội dung khi lỗi.
- Nút quyết định có trạng thái đang lưu/chặn gửi lặp. Backend vẫn chặn request lặp hoặc đã xử lý; refresh đọc lại dữ liệu DB và ẩn hành động hết hiệu lực. Trình duyệt không quyết định trạng thái nghiệp vụ.
- CSS riêng `tp-`, token/catalog có sẵn; không sửa layout, topbar, sidebar, token chung hoặc thêm dependency frontend.

## File thay đổi của Flow C

Đường dẫn tính từ project root; working tree còn chứa các thay đổi Flow A/B và tài liệu từ lượt trước.

- `candidate/service/TalentPoolService.java`: dùng chung validation/lưu draft, đọc gợi ý, phản hồi HM và bảo vệ bước consent HR.
- `candidate/repository/ApplicationRepository.java`: predicate nguồn/chờ HM, danh sách gợi ý và toàn bộ draft khi đóng job, sự kiện timeline.
- `candidate/dto/TalentPoolRecommendationResponse.java`, `TalentPoolRecommendationDecisionRequest.java`: dữ liệu render và validation quyết định.
- `candidate/controller/TalentPoolController.java`: endpoint Flow C, tái sử dụng detail/notes/CV và ngữ cảnh Back.
- `candidate/controller/ApplicationPipelineController.java`, `candidate/service/ApplicationPipelineService.java`: điều hướng bản nháp chờ HM và timeline đúng sự kiện.
- `jobposting/controller/JobPostingController.java`, `templates/job-postings/detail.html`: hành động truy cập và số chờ.
- `core/config/SecurityConfig.java`: rule HR gửi và HM quyết định, giữ CSRF.
- `templates/talent-pool/list.html`, `detail.html`, `recommendations.html`: selection theo vai trò, Candidate review và card gợi ý.
- `static/js/pages/talent-pool.js`, `talent-pool-recommendations.js`, `static/css/pages/talent-pool.css`: nhãn/feedback đúng luồng, chống gửi lặp và layout riêng.
- Tests: `TalentPoolRecommendationTests`, `TalentPoolFlowIntegrationTests`, `ApplicationPipelineWebTests`, `ApplicationStatusServiceTests`, `TalentPoolSelectionReviewTests`, `JobPostingControllerTest`, `talent-pool-ui.test.cjs`, `talent-pool-recommendations.test.cjs`.

## Kiểm chứng và giới hạn

Kết quả cuối: **317 test Java được báo cáo trong 20 suite: 316 passed, 1 skipped, 0 failure/error**. Test skipped thuộc query hồ sơ đã rút, vì DB local chưa có mẫu tương ứng. **24 test JavaScript passed**, không skipped/failure. Kiểm tra cú pháp script, ID template, CSS token, inline markup của trang mới, link tài liệu và `git diff --check` qua.

| Nhóm | Kết quả |
| --- | --- |
| TalentPoolRecommendationTests | 13 passed: gửi HR, giữ nguồn/Candidate, batch lỗi, chống trùng, quyền, nguồn HM, accept/reject, chặn xử lý lại, chặn HR consent trước HM và giữ quyết định gợi ý khi có review chính thức về sau. |
| ApplicationPipelineWebTests | 53 passed: MVC/Security/CSRF/validation, selection HR riêng endpoint, card file-user, detail nhiều CV/notes, hai nơi dùng cùng action và điều hướng an toàn. |
| TalentPoolFlowIntegrationTests | 4 passed trên SQL Server thật: 2 Flow C và 2 Flow B. Kiểm tra row mới, review/audit, nguồn giữ nguyên, số đếm chính thức, danh sách chờ HM/chờ HR, consent, điều kiện Interview và refresh/từ chối lặp. |
| Các suite hồi quy còn lại | 247 test được báo cáo, 246 passed, 1 skipped; bao gồm quyền truy cập, nộp hồ sơ, bookmark, pipeline/query/portal, Interview và Job Posting controller. |
| JavaScript | 24 passed: state 9, selection UI 9, CV preview 3, decision form 3. Kiểm tra giá trị quyết định vẫn được gửi khi nút bị disabled, gửi lặp bị chặn và khôi phục form khi quay lại bằng browser history. |

Lượt integration và hồi quy cuối đều exit code 0, dùng `ddl-auto=validate`; hai log không có câu CREATE/ALTER/DROP TABLE. Các test ghi DB dùng transaction rollback, có thể tiêu thụ giá trị identity của SQL Server. Không tạo lịch phỏng vấn hay gửi email thật trong integration test.

Lượt chạy ban đầu trong sandbox gặp Mockito và class output bị biên dịch lại trong lúc test. Đã dừng và dùng bản POM kiểm chứng tạm trong `target/`, output `target/flow-c-verification/`, làm việc từ project root; không thay POM runtime hoặc cấu trúc dự án để giải quyết vấn đề môi trường. Một assertion URL CV phát hiện query `recommendationId` rỗng trong Flow A/B; đã sửa template để giữ URL cũ khi không có ngữ cảnh Flow C, rồi chạy lại thành công.

Bằng chứng local (thư mục target không đưa vào Git): `target/flow-c-integration-tests.log`, `target/flow-c-regression-tests.log`, `target/flow-c-js-tests.log`, `target/flow-c-verification/surefire-reports/`. Không dùng lượt test lỗi hoặc bị dừng làm kết quả nghiệm thu.

Lệnh chạy lại trực tiếp từ project root, khi không có tiến trình IDE biên dịch chồng vào output Maven:

```powershell
mvn.cmd -q "-Dspring.jpa.hibernate.ddl-auto=validate" "-Dtest=TalentPoolRecommendationTests,TalentPoolSuggestionTests,TalentPoolSelectionReviewTests,TalentPoolBookmarkTests,ApplicationAccessTests,ApplicationSubmissionServiceTests,ApplicationPipelineServiceTests,ApplicationStatusServiceTests,ApplicationPipelineWebTests,ApplicationPipelineQueryTests,MyApplicationQueryTests,ApplicationProgressResolverTests,ApplicationStepperTests,ApplicationReviewServiceTests,MyApplicationServiceTests,MyApplicationWebTests,TalentPoolFlowIntegrationTests,InterviewScheduleFormWebTests,InterviewSchedulingServiceTests,JobPostingControllerTest" test
node --test src/test/js/talent-pool-selection.test.cjs src/test/js/talent-pool-ui.test.cjs src/test/js/application-preview.test.cjs src/test/js/talent-pool-recommendations.test.cjs
```

Đã kiểm tra trình duyệt với tài khoản HR mẫu: click từ Job Posting mở đúng selection, hiện HM phụ trách; checkbox/sticky bar mở đúng modal và preflight phát hiện trùng Candidate–Job Posting thật, khóa gửi. Không bấm gửi gợi ý vào dữ liệu đang dùng để tạo bằng chứng giả; các nhánh lưu/phản hồi được kiểm tra bằng HTTP MockMvc tới SQL Server thật trong transaction rollback.

Chưa coi đây là nghiệm thu đầy đủ mọi thao tác trên browser hoặc bằng chứng hai request chạy đồng thời trên hai kết nối DB. Cơ chế serialize được kiểm ở thứ tự khóa/kiểm tra và các request xử lý lặp; cần kiểm tra hai phiên trình duyệt khi nghiệm thu tích hợp. Không thay đổi cấu hình runtime local sẵn có; `validate` áp dụng cho các lệnh test của lượt này.

## Kịch bản nghiệm thu thủ công

1. HR mở job Published → Gợi ý đã gửi cho HM → Gợi ý ứng viên cho HM; lọc/sang trang, chọn hai Candidate chưa có hồ sơ cho job, đổi CV nguồn và mở preview/ghi chú.
2. Mở tổng kết, đối chiếu tin/HM/Candidate/CV, gửi. Chỉ sau response thành công mới có thông báo và xóa draft; mở danh sách gợi ý đã gửi.
3. HM đúng phòng ban mở job → Gợi ý từ HR. Mở `file-user`, đối chiếu toàn bộ CV cũ và ghi chú; thêm ghi chú, kiểm tra Back về đúng keyword/page.
4. Chấp nhận một người từ danh sách, một người từ detail: số chờ giảm, người đó vào danh sách HM đề xuất; vẫn chưa xuất hiện trong Application chính thức/chờ phỏng vấn.
5. HR liên hệ rồi xác nhận consent: chuyển cùng ID sang HM_Passed và trang tạo lịch; chỉ lưu lịch khi muốn tạo phỏng vấn thật.
6. Từ chối một gợi ý ở mỗi vị trí: refresh không hiện nút quyết định nữa, Candidate/CV/notes vẫn còn và không vào pipeline chính thức.
7. Thử hai phiên HM cùng xử lý một gợi ý; phiên sau phải báo đã xử lý. Thử HR giả quyết định HM, HM sai phòng ban, thiếu CSRF, nguồn CV thiếu hoặc chọn trùng.
8. Kiểm tra lại Flow B: HM tự thêm vẫn vào danh sách chờ HR ngay, không cần bước phản hồi HR → HM.

## Bổ sung trang quản lý gợi ý và thu hồi 17/10

- Chi tiết tin phía HR giữ một nút **Gợi ý đã gửi cho HM**. Nút **Gợi ý ứng viên cho HM** chuyển vào header của trang danh sách này, mở đúng `/talent-pool?jobPostingId=...` và dùng selection/CV/modal/backend cũ. Nút gửi mới chỉ hiện khi tin còn nhận hồ sơ và có HM hợp lệ.
- Mỗi gợi ý đang chờ HM có **Xóa gợi ý**, mở một dialog dùng class catalog hiện có, hiển thị tên ứng viên. Không tạo thêm file template/CSS/JS production. Fragment `fragments/ui/dialogs :: confirm` cũ không dùng được do Thymeleaf chặn nội suy chuỗi vào `onclick`; giữ nguyên file chung, dùng JS của trang và `requestSubmit()` trên form gốc. Người dùng xác nhận mới POST; form giữ CSRF, keyword và trang tìm kiếm. Hủy/ESC không ghi dữ liệu; khóa xác nhận lặp. Lỗi nghiệp vụ trả thông báo và quay lại đúng danh sách, không nhận return URL từ client.
- Backend HR-only: `POST /talent-pool/jobs/{jobId}/recommendations/{id}/withdraw`. Service kiểm tra role, đúng tin, nguồn HR và trạng thái chưa có HM quyết định; khóa JobPosting giống luồng HM phản hồi. Transition có điều kiện `HM_Suggested → Rejected`; nếu đã xử lý thì báo lỗi, không ghi audit thành công.
- Đây là thu hồi mềm: ghi `TalentPoolCancelled` để giữ bản nháp ngoài pipeline/số đếm Application chính thức; thêm metadata audit `TalentPoolHrWithdrawn` để ẩn khỏi danh sách gợi ý của HR và HM. Metadata là chuỗi trong AuditLog sẵn có, không thêm enum/constraint/bảng/cột. Link detail gợi ý đã thu hồi trả 404. Candidate, CV nguồn và ghi chú giữ nguyên; có thể gửi gợi ý mới sau khi thu hồi.
- Gợi ý HM đã chấp nhận/từ chối hoặc đã thành Application chính thức không được thu hồi bằng hành động này. Việc HR liên hệ và xác nhận Candidate đồng ý tiếp tục dùng luồng trước.

Kiểm thử cuối của lượt bổ sung: **86 Java passed, 0 failure/error/skipped**, gồm `TalentPoolRecommendationTests` (20), `ApplicationPipelineWebTests` (56) và `TalentPoolFlowIntegrationTests` (10). Integration chạy SQL Server thật với `ddl-auto=validate`, transaction rollback; kiểm tra thu hồi, ẩn khỏi query HR/HM và số đếm chính thức, giữ nguyên Candidate/CV/ghi chú, gửi lại sau thu hồi, chặn link cũ và chặn thu hồi khi HM đã chấp nhận. Test web kiểm tra role/CSRF, success/error flash, search context, HR/HM action và render dialog. **40 JavaScript passed**, trong đó test xác nhận/hủy nêu đúng tên ứng viên, dùng form gốc và không submit lặp. `node --check` và `git diff --check` qua. Không commit/push. Các số liệu phía trên thuộc lượt triển khai Flow C trước đó, không cộng dồn.

Log lượt cuối: `target/hr-recommendation-withdraw-tests-final2.log` và `target/hr-recommendation-js-tests.log` (file build tạm, không đưa vào Git).

Giới hạn browser của lượt này: đã thử khởi động preview riêng cổng 8082, `ddl-auto=validate`; Tomcat không mở được do Windows báo `Unable to establish loopback connection` / `Invalid argument: connect`, kể cả bật IPv4 cho tiến trình preview. Vì vậy chưa có xác nhận hình ảnh desktop/mobile cho giao diện mới. Tiến trình preview đã tự kết thúc; không dừng/restart ứng dụng IDE của người dùng. Bằng chứng end-to-end hiện có là các request MockMvc qua controller/security/service/repository đến SQL Server thật, không phải thao tác lưu dữ liệu qua browser.

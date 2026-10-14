# Hoàn thiện luồng ứng viên tiềm năng — 14/10/2026

## Phạm vi và nguồn chuẩn

- Nguồn cấu trúc DB: `database/schema/db.sql`, đã đọc toàn bộ trước khi sửa.
- Giữ các thay đổi người dùng vừa làm: Candidate.IsPotential, DTO hồ sơ, Repository và bước HM_Suggested.
- Không chạy script SQL, không thêm bảng/cột, không sửa luồng tài khoản, đăng ký, đăng nhập hoặc cấu hình SMTP.
- Luồng đã duyệt: HM chọn Candidate tiềm năng và CV cũ cho job mới → Application.HM_Suggested (ẩn với Candidate) → HR liên hệ và xác nhận → chính bản ghi chuyển HM_Passed → lên lịch phỏng vấn.
- Candidate chọn CV mới vẫn đi luồng Applied → AI → HR → HM bình thường.
- HR/HM xem toàn bộ kho tiềm năng; quyền với job đích vẫn theo phạm vi HM đang có.

## Kiểm tra trước khi sửa / Impact Analysis

| Nhóm | Hiện trạng | Thay đổi cần làm | Lý do |
|---|---|---|---|
| Schema | Đã sửa dấu phẩy Candidate; thêm TalentPoolNote, HM_Suggested, Offer.DeclineReason | Giữ nguyên | DB là nguồn chuẩn; DeclineReason ngoài phạm vi Talent Pool |
| Candidate | Đã có isPotential và JpaSpecificationExecutor | Giữ; nối UI/Service và lọc | Tận dụng code vừa sửa |
| Query CV | Đang dùng a.cvFileUrl không tồn tại | Dùng appliedCvUrl | Tránh lỗi validate query / Context startup |
| DTO danh sách | Có isPotential nhưng constructor query luôn đặt false | Truyền cờ từ Candidate trong JPQL | Badge phải phản ánh DB |
| HM_Suggested | Đã có ProgressStep nhưng thiếu nhãn, quyền, hành động | Nối luồng đề xuất và xác nhận | Đề xuất chưa là đơn chính thức |
| Candidate data | Chỉ loại HM_Suggested | Loại cả đề xuất kết thúc chưa xác nhận bằng audit | Tránh lộ đề xuất Rejected nội bộ |
| Job Posting | HM chưa được đọc; chưa có close nghiệp vụ | Mở đọc đúng phạm vi, thêm thao tác Talent Pool và close | Đúng đường thao tác đã duyệt |
| Interview | Dropdown lấy mọi Application; chưa gửi thư mời | Lọc/validate đơn đủ điều kiện, gửi sau commit | Chặn lên lịch khi chưa đồng ý ứng tuyển |
| Audit | Chưa lưu người đánh dấu/người đề xuất | Ghi vào AuditLog hiện có, không đổi schema | Lọc theo người thực hiện, giữ nguồn CV và lịch sử |

## Các quyết định đã được người dùng duyệt

1. Đề xuất bị từ chối → Rejected + audit TalentPoolCancelled; giữ lịch sử và tiếp tục ẩn với Candidate.
2. Đóng bài: giữ Hired/Offer Accepted, hủy Scheduled/Rescheduled của đơn bị dừng; chặn nếu còn Offer chưa kết thúc.
3. Commit nghiệp vụ trước khi gửi SMTP. Gửi lỗi: alert "Đã lưu dữ liệu nhưng hệ thống gửi mail đang gián đoạn"; HR có nút Gửi lại Email, không chạy lại nghiệp vụ.
4. UI: đọc và tuân thủ UI_RULES, dùng layout/component chung, tiếng Việt, cột STT. Không sửa file docs/architecture/UI, layout, global CSS, tokens, topbar hoặc sidebar-shell.

## Nhật ký thực hiện

- Đã kiểm tra working tree: có thay đổi của người dùng tại candidate, db.sql và ba file ngoài phạm vi; sẽ không ghi đè các thay đổi ngoài phạm vi.
- Đã xác nhận cột CV thực tế là AppliedCvUrl; cờ tiềm năng đã được mapping đúng.
- Đã đọc architecture và UI_RULES: dùng package-by-feature, DTO trong dto/, layout nội bộ và component chung, không sửa khung/CSS chung.

## Danh sách file sửa / tạo và mục đích

### File hiện có được bổ sung / chỉnh sửa

| File (từ gốc repository) | Sửa gì | Để làm gì |
|---|---|---|
| src/main/java/com/group2/rms/candidate/repository/ApplicationRepository.java | Sửa appliedCvUrl; truyền isPotential vào projection; FORMAL_FILTER; quyền HM có audit nguồn; truy vấn đề xuất/CV; cập nhật có điều kiện; audit nộp CV mới | Query khớp Entity; không lộ đề xuất; giữ lịch sử và chống ghi đè trạng thái |
| src/main/java/com/group2/rms/candidate/repository/CandidateRepository.java | Query kho tiềm năng phân trang, từ khóa, người đánh dấu, job nguồn | Lọc đúng dữ liệu DB; không tạo dòng trùng |
| src/main/java/com/group2/rms/candidate/dto/ApplicationPipelineResponse.java | Constructor projection nhận cờ thật từ Candidate | Badge phản ánh DB, không luôn false |
| src/main/java/com/group2/rms/candidate/service/ApplicationAccess.java | Kiểm tra nguồn Talent Pool bên cạnh HR Pass, vẫn giữ phạm vi phòng ban; HM mở chi tiết job | HM thấy đề xuất hợp lệ; không mở mọi hồ sơ cho HM |
| src/main/java/com/group2/rms/candidate/service/ApplicationStatusLabels.java | Bổ sung nhãn/bộ lọc/badge/bước HM_Suggested | HR biết phải liên hệ, chưa lên lịch |
| src/main/java/com/group2/rms/candidate/service/ApplicationPipelineService.java | Lý do dừng lấy từ audit PostingClosed; nhãn chờ HR; method closedByPosting | Không suy đoán mọi Rejected ở job Closed đều do đóng đợt |
| src/main/java/com/group2/rms/candidate/service/ApplicationSubmissionService.java | Phân biệt đề xuất/đơn chính thức; khóa job khi nộp; nhận CV mới vào đề xuất rồi chuyển Applied, ghi audit, phát event AI | Candidate không bị khóa nút nộp vì HM đề xuất; CV mới đi quy trình bình thường; giữ file CV cũ |
| src/main/java/com/group2/rms/candidate/service/ApplicationStatusService.java | requireScheduleable đọc trạng thái thực và khóa job | Chặn lịch cho đề xuất chưa đồng ý, đơn kết thúc hoặc job đóng |
| src/main/java/com/group2/rms/candidate/controller/ApplicationPipelineController.java | Cấp ghi chú nội bộ và trạng thái đóng đợt từ Service | Controller không truy vấn DB để xử lý nghiệp vụ |
| src/main/java/com/group2/rms/core/config/SecurityConfig.java | Rule Talent Pool theo vai trò; HM chỉ đọc danh sách/chi tiết job; giữ tạo/sửa/xóa HR/Admin | Chặn Candidate/Director/Interviewer vào luồng; giữ CSRF/session |
| src/main/java/com/group2/rms/dashboard/DashboardMetricsRepository.java | Áp dụng FORMAL_FILTER cho số đơn, giai đoạn, đơn gần đây | Không đếm/hiện đề xuất chưa ứng tuyển hoặc đề xuất bị từ chối |
| src/main/java/com/group2/rms/requisition/repository/JobPostingRepository.java | findForUpdate | Serialize nộp đơn/đề xuất/xác nhận/đóng bài trên cùng job; không cần thêm cột khóa |
| src/main/java/com/group2/rms/requisition/service/JobPostingServiceImpl.java | Quyền đọc HM theo phòng ban; LEFT JOIN manager để giữ phòng ban chưa có trưởng phòng | HM vào job được phép; quyền quản lý cũ giữ nguyên |
| src/main/java/com/group2/rms/interview/service/InterviewSchedulingServiceImpl.java | Gọi requireScheduleable trước khi lưu | Validate ở backend, không chỉ ẩn nút |
| src/main/java/com/group2/rms/interview/controller/InterviewSchedulingController.java | Dropdown đủ điều kiện; giữ đơn hiện tại khi sửa lịch; gọi SMTP sau Service commit; catch đúng exception gửi mail | Email lỗi không rollback lịch; HR có cảnh báo |
| src/main/java/com/group2/rms/interview/controller/InterviewResultController.java | Model quyền gửi lại Email | Nút Resend chỉ HR |
| src/main/resources/templates/candidate/applications.html | Badge tiềm năng, STT liên tục theo trang | Quét danh sách và kiểm tra từng bản ghi |
| src/main/resources/templates/candidate/application-detail.html | Đánh dấu, ghi chú, HR xác nhận/từ chối, dialog, resend email đóng đợt | Thao tác thật với POST/CSRF |
| src/main/resources/templates/job-postings/detail.html | Link kho/danh sách đề xuất, dialog đóng đợt, flash, điều kiện quyền sửa/xóa | HM chọn ứng viên từ job; HR xử lý đợt |
| src/main/resources/templates/job-postings/list.html | Ẩn tạo/sửa/xóa cho HM | HM là người đọc job, không được quản lý bài đăng |
| src/main/resources/templates/interview/list.html | Nút gửi lại thư mời lịch đang Scheduled/Rescheduled | HR gửi lại mà không tạo lịch mới |
| src/main/resources/templates/interview/result.html | Nút gửi lại, flash thành công/lỗi | Có thao tác ở màn chi tiết lịch |
| src/main/resources/templates/fragments/layout/sidebars/recruiter.html | Chỉ thêm link Talent Pool HR/HM | Có đường truy cập nhìn thấy; giữ menu của đồng đội |

### File mới

| Package / thư mục | File | Mục đích |
|---|---|---|
| candidate/entity | TalentPoolNote.java | Mapping đúng NoteId, CandidateId, UserId, NoteContent, CreatedAt; không kế thừa BaseEntity |
| candidate/repository | TalentPoolNoteRepository.java | Đọc timeline cùng tác giả |
| candidate/repository | TalentPoolAuditRepository.java | Ghi nguồn đề xuất/người đánh dấu; đối chiếu Offer, hủy lịch, ghi kết quả mail; không import Entity admin/interview/offer |
| candidate/dto | TalentPoolSearch.java | Input bộ lọc |
| candidate/dto | TalentPoolCandidateResponse.java | Dòng ứng viên và các CV nguồn |
| candidate/dto | TalentPoolCvResponse.java | ID đơn nguồn, job, ngày nộp; không nhận URL CV từ browser |
| candidate/dto | TalentPoolDetailResponse.java | Hồ sơ, ghi chú, CV |
| candidate/dto | TalentPoolNoteRequest.java, TalentPoolNoteResponse.java | Validate ghi chú 1.000 ký tự, trả tác giả/thời gian |
| candidate/dto | TalentPoolSuggestRequest.java | Danh sách CV chọn, tối đa 20 mỗi lần |
| candidate/dto | TalentPoolSuggestionResponse.java | HR xem liên hệ, HM đề xuất, thời gian |
| candidate/dto | TalentPoolConfirmRequest.java | AssertTrue xác nhận đồng ý dùng CV cũ |
| candidate/dto | RecruitmentEmailMessage.java | Snapshot thông tin gửi mail, không truyền Entity LAZY |
| candidate/exception | TalentPoolException.java, RecruitmentEmailException.java | Lỗi nghiệp vụ và cảnh báo SMTP sau khi lưu |
| candidate/service | TalentPoolService.java | Điều phối toàn bộ nghiệp vụ tiềm năng và đóng đợt, ghi DB trong transaction |
| candidate/service | RecruitmentEmailService.java | SMTP ngoài transaction nghiệp vụ; audit kết quả riêng; gửi lại thủ công |
| candidate/controller | TalentPoolController.java | Routes SSR, validation, gọi Service, redirect/flash |
| templates/talent-pool | list.html, detail.html, suggestions.html | Danh sách, ghi chú/CV, đề xuất chờ HR liên hệ |
| static/js/pages | talent-pool.js | Checkbox theo CV được chọn và mở/đóng dialog riêng của luồng |

### Test hiện có được cập nhật, không tạo file source test mới

- ApplicationPipelineWebTests.java: cập nhật DTO fixture, mock Service mới; thêm test render, CSRF, quyền, thứ tự lưu rồi gửi mail; HTML render kiểm tra ở docs/members/linhdn/tmp/talent-pool/.
- ApplicationAccessTests.java: bổ sung dependency Repository mới vào fixture.
- ApplicationStatusServiceTests.java: cập nhật fixture; thêm nhóm TalentPoolFlows kiểm tra xác nhận, thiếu consent, từ chối, Offer chặn đóng.
- ApplicationSubmissionServiceTests.java: mock đúng method mới, test nộp CV mới vào đề xuất và vẫn phát event AI, giữ CV cũ.
- ApplicationProgressResolverTests.java: test HM_Suggested chờ HR, không thuộc giai đoạn phỏng vấn.

### Code của người dùng được giữ

Candidate.java, ApplicationDetailResponse.java, ApplicationProgressResolver.java, ApplicationProgressStep.java đã được người dùng bổ sung trước đợt này. Các thay đổi ở db.sql, ApiMonitoringService.java, AccountSessionGuardFilter.java, RequisitionWorkflowEvent.java giữ nguyên; không coi là thay đổi do đợt này thực hiện.

Không xóa file/source, không thêm dependency, không sửa pom/config SMTP/schema. Không dùng JWT.

## Luồng kỹ thuật

### Đánh dấu và ghi chú

1. POST /talent-pool/applications/{id}/mark → candidate.controller.TalentPoolController.mark().
2. candidate.service.TalentPoolService.markPotential() → ApplicationAccess.requireView() → Candidate.setPotential(true) → TalentPoolAuditRepository.record().
3. POST /talent-pool/{candidateId}/notes → TalentPoolController.note() (@Valid) → TalentPoolService.addNote() → TalentPoolNoteRepository.save(). Tác giả lấy từ CurrentUserService, không lấy từ form.

### HM đề xuất

1. GET /internal/job-postings/{id} → requisition.controller.InternalJobPostingController.showJobPostingDetail() → JobPostingServiceImpl.getInternalJobPostingDetail(), kiểm tra phạm vi HM.
2. Link Thêm ứng viên tiềm năng → GET /talent-pool?jobPostingId={id} → TalentPoolController.list() → TalentPoolService.search() → CandidateRepository.searchPotential().
3. Chọn CV → POST /talent-pool/jobs/{id}/suggest → TalentPoolController.suggest() → TalentPoolService.suggest().
4. Service kiểm tra role HM, job Published/chưa hết hạn, phòng ban, cờ tiềm năng, CV nguồn, trùng Candidate/job. Dùng JobPostingRepository.findForUpdate() và ApplicationRepository.saveAndFlush().
5. Audit CREATE Application: OldValue là ID đơn nguồn; NewValue TalentPoolSuggested; UserId là HM. Không phát ApplicationSubmittedEvent, không tạo lượt HR Pass hoặc điểm AI giả.

### HR liên hệ / xác nhận / từ chối

1. GET /talent-pool/jobs/{id}/suggestions → TalentPoolController.suggestions() → TalentPoolService.suggestions() → ApplicationRepository.findSuggestionsByJobPostingId().
2. HR mở /applications/{id}, xem liên hệ và CV, liên hệ ngoài hệ thống.
3. Checkbox đồng ý → POST /talent-pool/applications/{id}/confirm → TalentPoolService.confirm() → promoteSuggestionToHmPassed() → audit TalentPoolActivated. SubmissionDate cập nhật lúc ứng tuyển chính thức; CreatedAt vẫn là ngày đề xuất.
4. Redirect /interviews/new?applicationId={id}; không tạo bản ghi Application thứ hai.
5. Từ chối → POST /talent-pool/applications/{id}/decline → TalentPoolService.decline() → transition(HM_Suggested, Rejected), audit TalentPoolCancelled. FORMAL_FILTER ẩn cả đề xuất Rejected này khỏi Candidate.

### Candidate muốn CV mới

1. /jobs/{id} dùng ApplicationSubmissionService.hasApplied(): đề xuất nội bộ chưa khóa nút nộp.
2. POST /jobs/{id}/apply → career.controller.CareerPortalController.submitApplication() → candidate.service.ApplicationSubmissionService.apply().
3. Kiểm tra CV, lưu file mới; nếu có HM_Suggested thì submitSuggestionWithNewCv() → Applied + SubmissionDate mới + audit TalentPoolCandidateSubmitted.
4. ApplicationSubmittedEvent → AiScreeningListener.onApplicationSubmitted() sau commit → AiScreeningService.screen(). Luồng HR/HM giữ nguyên.
5. Rollback chỉ xóa file mới; file CV cũ đang được đơn lịch sử tham chiếu được giữ.

### Đóng đợt

1. Dialog tại chi tiết Job → POST /talent-pool/jobs/{id}/close → TalentPoolController.close().
2. TalentPoolService.closeJob() (@Transactional): kiểm tra HR, khóa job, chặn Offer chưa kết thúc, đặt Closed; kết thúc đề xuất nội bộ; đơn đang xử lý chuyển Rejected trừ Hired/Accepted; hủy Scheduled/Rescheduled; ghi audit PostingClosed/InterviewCancelledOnClose.
3. Service trả về sau commit. Controller gọi RecruitmentEmailService.sendJobClosure(). Chỉ gửi cho đơn chính thức có audit PostingClosed; đề xuất chưa đồng ý không nhận email cáo lỗi.
4. SMTP lỗi: recordMail(...false) trong transaction riêng; RecruitmentEmailException được Controller bắt đúng kiểu, flash cảnh báo. Dữ liệu đã commit giữ nguyên.
5. Đóng lại không gửi lặp thư đã gửi thành công; HR chủ động Resend khi cần.

### Lập lịch và gửi lại

1. InterviewSchedulingController.createSchedule() → InterviewSchedulingServiceImpl.createSchedule() → ApplicationStatusService.requireScheduleable() → lưu lịch/panel → onInterviewScheduled() → commit.
2. Controller gọi RecruitmentEmailService.sendInterview() sau Service trả về. Query SMTP lấy dữ liệu thật của lịch và Candidate; lịch không đủ điều kiện không gửi.
3. POST /talent-pool/interviews/{id}/resend-email → TalentPoolController.resendInterview() → RecruitmentEmailService.resendInterview(). Không tạo/sửa lịch.
4. POST /talent-pool/applications/{id}/resend-email → resendApplication() → resendClosure(). Không đổi trạng thái đơn.

### Ranh giới phân quyền / thông tin

- Candidate, Interviewer, Director không vào Talent Pool; System Admin không được tự nhận quyền HR trong luồng này.
- Kho tiềm năng toàn bộ cho HR/HM. Job đích của HM giữ phạm vi phòng ban; quyền Application bình thường không mở toàn bộ.
- Notes, người đề xuất, thông tin đánh giá nội bộ không đưa vào DTO Candidate.
- FORMAL_FILTER dùng chung cho My Applications, hasApplied và thống kê Dashboard.
- Ghi SQL AuditLog theo mẫu sẵn có, không thêm Entity/bảng nguồn đề xuất. Không hardcode metrics.

## Test và kết quả

### Kịch bản thủ công (không gửi thư thật tới địa chỉ seed)

Chuẩn bị HR và HM Active, Candidate có CV PDF thật đã nộp ở Job A, Job B Published/chưa hết hạn thuộc phòng ban HM; cấu hình SMTP ngoài Git, dùng email thử nghiệm do người dùng kiểm soát.

| ID | Các bước | Kết quả cần thấy |
|---|---|---|
| TP01 | HR mở hồ sơ của Job A → Đánh dấu tiềm năng → tải lại | Badge còn, IsPotential=1; có audit đúng HR |
| TP02 | HR và HM mỗi người thêm một ghi chú; mở lại hồ sơ | Hai bản ghi riêng, đúng tác giả/thời gian; không ghi đè |
| TP03 | Ghi chú rỗng hoặc >1.000 ký tự | Không lưu, có thông báo; nội dung nhập được giữ |
| TP04 | HM mở Job B → Thêm ứng viên tiềm năng → lọc họ tên/email, người đánh dấu, job nguồn | Danh sách đúng điều kiện, một Candidate một dòng, STT tiếp theo trang |
| TP05 | HM chọn CV của Candidate → Đề xuất | Application mới HM_Suggested, job B, URL CV cũ; đơn A không đổi; không có AIScreeningResult mới |
| TP06 | Bấm đề xuất lại / chọn hai CV cùng Candidate / sửa source ID sang đề xuất chưa chính thức | Bị chặn; không tạo trùng |
| TP07 | Candidate mở My Applications, Dashboard và Job B trước xác nhận | Không thấy đề xuất, không bị báo đã nộp; vẫn có nút nộp CV |
| TP08 | HR mở danh sách đề xuất Job B → liên hệ → tick đồng ý → xác nhận | Chính Application chuyển HM_Passed; ngày nộp mới; vào form lập lịch đúng ID; không AI/HR review giả |
| TP09 | Không tick đồng ý hoặc HM gọi POST confirm | Không chuyển; validation hoặc HTTP403 |
| TP10 | HR kết thúc đề xuất do Candidate từ chối → Candidate mở lại dashboard/my-applications | Rejected + audit TalentPoolCancelled giữ lịch sử nội bộ; không hiện cho Candidate |
| TP11 | Candidate tự nộp CV mới khi đang HM_Suggested | Applied, URL mới, chạy AI bình thường; file/đơn A giữ nguyên |
| TP12 | HR tạo lịch của đơn HM_Passed | Lịch/panel lưu, đơn Interviewing, thư mời có đúng tên job/thời gian/hình thức/link |
| TP13 | Tạo lịch cho HM_Suggested, Rejected, job Closed | Backend chặn ngay cả khi sửa POST thủ công |
| TP14 | Job B có Offer chưa kết thúc → Đóng đợt | Không đóng, yêu cầu xử lý Offer trước |
| TP15 | Job không còn Offer mở, có đơn dở dang + Hired + Accepted → Đóng | Dở dang Rejected; Hired/Accepted giữ; lịch Scheduled/Rescheduled của đơn dừng Cancelled; tag/notes giữ |
| TP16 | SMTP không cấu hình/lỗi khi tạo lịch hoặc đóng bài | DB vẫn lưu; alert đã lưu nhưng mail gián đoạn; audit MailFailed; không báo đã gửi thành công |
| TP17 | Khôi phục SMTP → Gửi lại Email từ lịch/đơn | Gửi thư, audit MailSent; không tạo lịch/đổi trạng thái lần nữa |
| TP18 | Candidate/Director/Interviewer truy cập GET/POST Talent Pool; HR POST không CSRF | HTTP403 hoặc Guest redirect login; không rò notes |
| TP19 | HM mở job phòng ban khác hoặc sửa URL nguồn CV/Candidate không khớp | Backend chặn; không chỉ ẩn nút |
| TP20 | 1440/1024/768/375px; Tab, Enter, Escape trên dialog | Bảng cuộn trong wrapper; label/focus rõ; modal mở/hủy; không sửa khung chung |

### Kiểm tra thực tế

- Compile từng nhóm: đã PASS; không coi compile là hoàn thành.
- Test-compile phát hiện DTO fixture cũ và dependency mới; đã cập nhật test hiện có, không comment/bỏ test.
- Lượt test fork đầu bị kẹt Java agent attach trên Windows (named pipe error). Đã dừng lượt này, không tính là PASS.
- Maven cache được đọc offline; chạy validate để không tự đổi DB. Không sửa pom/config hay cài dependency.
- Kết quả cuối của test, Context, query, render và browser sẽ được bổ sung bên dưới khi chạy xong.

### Giới hạn cần biết

- Candidate tiềm năng trong seed chưa có audit người đánh dấu: filter người đánh dấu chỉ trả những hồ sơ có audit thật, không suy ra tác giả từ người viết note.
- CV /samples hoặc URL ngoài có thể không còn file. Local/static kiểm tra tồn tại; URL ngoài được giữ tương thích nhưng không tự fetch để kiểm tra dịch vụ ngoài.
- SMTP hoạt động thực tế cần người dùng cấu hình và kiểm tra với hộp thư thử nghiệm; các test dùng mock không chứng minh mail đã tới inbox.
- Offer.DeclineReason mới không thuộc phần triển khai Talent Pool đợt này.

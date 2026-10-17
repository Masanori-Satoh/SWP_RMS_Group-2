# System Test thủ công trên website RMS

Đây là **System Test**, không gọi những bước browser là bằng chứng Integration Test DB. Người kiểm thử tự chạy sau. Chưa mở website hoặc gửi request nghiệp vụ trong lượt lập tài liệu này.

Base URL local thường `http://localhost:8081` theo cấu hình đang đọc; xác nhận lại port thực tế. Chuẩn bị các tài khoản giả Admin, HR, HM_A, HM_B, Director, Candidate C1/C2, dữ liệu và file theo TEST_PLAN. Dùng browser profile/cửa sổ riêng cho từng role; hai tab cùng profile thường dùng chung session, không đủ để chứng minh hai người dùng độc lập. Tên nút bên dưới dựa trên template hiện tại; nếu không thấy nút thì ghi Actual và dừng ở bước đó, không tự đoán URL mới để bỏ qua UI lỗi.

Không điền Passed trước khi thực hiện. Mỗi biến thể cần ghi kết quả riêng. DB status trong Expected chỉ là đối chiếu khi có quyền đọc DB test; qua UI ghi lại nhãn hiển thị và ApplicationId/JobId, không tự suy ra raw status chỉ từ màu badge.

## ST-01 Đăng ký OTP, đăng nhập, quên mật khẩu và đăng xuất

- **Test Case ID:** ST-01
- **Test Type:** System Test — Manual; State Transition/EP
- **Test Scenario:** Candidate mới đi qua toàn bộ vòng đời xác thực.
- **Preconditions:** Website local chạy; mail test nhận được; username/email chưa dùng; cửa sổ guest riêng.
- **Test Data:** Full name “QA Linh Đăng ký”, username `qa_linh_<suffix>`, email test riêng, password `QaLinh123!`, password mới `QaLinh456!`.
- **Test Steps:**
  1. Mở trang chủ, bấm Đăng nhập rồi link Đăng ký.
  2. Điền đủ năm field: họ tên, username, email, password, confirm; bấm gửi mã/xác minh trên form.
  3. Kiểm tra sang màn OTP có email đúng; mở hộp thư test, ghi thời gian nhận, không chụp rõ mã OTP.
  4. Nhập mã đủ sáu số nhưng sai một lần; kiểm tra lỗi ở bước OTP.
  5. Nhập mã đúng còn hạn; kiểm tra về đăng nhập có thông báo đăng ký thành công.
  6. Login bằng username; xác nhận dashboard Candidate và không có menu admin. Logout bằng topbar, xác nhận dialog.
  7. Login lại bằng email, logout; tại Login bấm Quên mật khẩu.
  8. Nhập email trên, lấy OTP mới, verify; nhập password mới và confirm rồi lưu.
  9. Dùng session mới thử password cũ, sau đó password mới; kiểm tra cũ bị từ chối, mới vào được.
  10. Logout, nhập lại địa chỉ `/dashboard`; xác nhận phải login.
- **Expected Result:** Các bước đúng thứ tự; sai OTP không tạo account sớm; đăng ký thành công một lần; hai định danh login cùng account; reset chỉ sau OTP; password cũ mất hiệu lực; logout mất quyền. Không hiển thị hash/secret.
- **Actual Result:**
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-AUTH; UC56; GAP-01; NEW-OTP, định nghĩa trong INTEGRATION_TEST_CASES.
- **Source Code Reference:** `auth/controller/RegistrationController.java#register,#verifyOtp`; `auth/controller/PasswordRecoveryController.java`; `auth/service/RegistrationVerificationService.java#verifyAndRegister`; `auth/service/PasswordRecoveryService.java#completeReset`; `core/config/SecurityConfig.java#filterChain`.

## ST-02 Admin vô hiệu hóa Candidate đang online và kích hoạt lại

- **Test Case ID:** ST-02
- **Test Type:** System Test — Manual; State Transition/Decision Table
- **Test Scenario:** Hai người dùng độc lập kiểm tra Candidate Account lifecycle.
- **Preconditions:** Admin ở profile A; C1 Active ở profile B; C1 có hồ sơ cũ.
- **Test Data:** Email/username C1 để search; ghi CandidateId/ApplicationId và nhãn trước test.
- **Test Steps:**
  1. Profile B login C1, mở Hồ sơ đã ứng tuyển, ghi lại đơn đang có.
  2. Profile A login Admin, mở menu tài khoản ứng viên tại `/admin/candidate-accounts`.
  3. Tìm C1 bằng email; kiểm tra đúng nhóm Candidate và đúng người.
  4. Bấm Vô hiệu hóa, xác nhận hộp thoại nếu UI hiển thị, kiểm trạng thái danh sách.
  5. Profile B bấm tải lại Hồ sơ đã ứng tuyển để gửi request mới; ghi URL/thông báo sau khi bị loại session.
  6. B thử login lại bằng password đúng; phải bị từ chối.
  7. A kích hoạt lại C1; B login mới và mở lại danh sách đơn.
  8. Đối chiếu các đơn trước test; kiểm không tạo thêm profile/đơn hoặc mất CV.
- **Expected Result:** Inactive chặn cả session cũ ở request tiếp theo và login mới; activate khôi phục login. Hồ sơ và CV giữ nguyên. Candidate không được vào trang admin khi nhập URL trực tiếp.
- **Actual Result:**
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-ACC; GBR-05; OR-03; GAP-08.
- **Source Code Reference:** `user/controller/CandidateAccountController.java#deactivate,#activate`; `user/service/AccountListService.java#deactivateCandidate,#activateCandidate`; `core/security/AccountSessionGuardFilter.java#doFilterInternal`.

## ST-03 Candidate nộp CV thường, HR duyệt rồi HM duyệt

- **Test Case ID:** ST-03
- **Test Type:** System Test — Manual; State Transition/EP
- **Test Scenario:** Hồ sơ thường đi hết vòng CV sang chờ phỏng vấn.
- **Preconditions:** C1 chưa có đơn/proposal tại J1; J1 Published còn hạn phòng A; HR, HM_A và HM_B có profile riêng.
- **Test Data:** Một file thật `.pdf` <=5 MiB và một file `.pdf` header sai; C1, J1.
- **Test Steps:**
  1. Candidate mở danh sách việc làm từ trang chủ, tìm J1 rồi vào chi tiết.
  2. Bấm ứng tuyển, chọn file giả PDF và gửi; kiểm lỗi tại CV, chưa có đơn thành công.
  3. Chọn PDF thật, gửi một lần; ghi tên tin, giờ nộp và ID nếu UI hiển thị.
  4. Mở Hồ sơ đã ứng tuyển, kiểm có đúng một dòng J1. Tải lại sau một khoảng chờ; không coi chấm AI chậm là mất đơn.
  5. HR mở danh sách Application, lọc J1/email C1, vào chi tiết, mở preview CV; đối chiếu đúng file.
  6. HR chọn Đạt/Pass và lưu; kiểm chuyển sang chờ HM.
  7. HM_B khác phòng tìm đơn và thử địa chỉ detail đã ghi; không được xem/duyệt.
  8. HM_A mở đơn đã HR chuyển, xem CV rồi chọn Đạt/Pass.
  9. HR mở lại hồ sơ, bấm hành động lên lịch phỏng vấn; kiểm đúng C1/J1 được prefill, chưa cần lưu lịch ở test này.
  10. Candidate trở lại J1 và thử nộp lại; kiểm không có đơn active thứ hai.
- **Expected Result:** File sai bị chặn; đơn thường qua vòng HR và HM; UI đúng quyền/phòng ban; HR chỉ có đường xếp lịch khi đến giai đoạn phù hợp. AI hiện tại là stub, điểm không chứng minh hệ thống đọc đúng CV.
- **Actual Result:**
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-APPLY; OLD-PIPE; UC30/49/54/55; GAP-07.
- **Source Code Reference:** `candidate/service/ApplicationSubmissionService.java#apply`; `candidate/validator/CvFileValidator.java#validate`; `candidate/service/ApplicationReviewService.java#review`; `candidate/service/ApplicationAccess.java#canView,#canReview`; `candidate/service/ApplicationStatusService.java#requireScheduleable`.

## ST-04 HM chọn Talent Pool và HR xác nhận dùng CV cũ

- **Test Case ID:** ST-04
- **Test Type:** System Test — Manual; State Transition/Decision Table
- **Test Scenario:** HM đề xuất ứng viên; chỉ sau Candidate consent mới thành hồ sơ chính thức.
- **Preconditions:** C1 potential có CV chính thức ở J1; J2 Published phòng A, khác J1, còn hạn; chưa có đơn/proposal C1 ở J2.
- **Test Data:** C1, CV nguồn J1; HR ghi nhận đồng ý qua liên hệ ngoài hệ thống trong tình huống giả lập.
- **Test Steps:**
  1. Login HM_A, mở chi tiết tin nội bộ J2, bấm **Ứng viên tiềm năng**.
  2. Trong danh sách của tin, bấm **Thêm ứng viên tiềm năng** để mở kho chọn.
  3. Tìm C1, mở chi tiết/CV lịch sử để chắc đúng CV J1 rồi quay lại; tick chọn CV.
  4. Chuyển trang hoặc thay filter rồi quay lại; kiểm selection còn đúng, không chọn hai CV của C1.
  5. Mở popup tổng kết, kiểm C1/CV nguồn/tin J2, chờ kiểm tra hợp lệ rồi bấm **Xác nhận thêm** một lần.
  6. HR mở danh sách HM đề xuất của J2; kiểm C1 chờ liên hệ, chưa ở pipeline chính thức hoặc danh sách xếp lịch.
  7. Mở application đề xuất; thử xác nhận khi chưa tick consent. Kiểm bị chặn, chưa vào lịch.
  8. Trong tình huống test, ghi nhận Candidate đã đồng ý dùng CV cũ; tick consent rồi xác nhận.
  9. Kiểm điều hướng đến form tạo lịch với đúng application; quay lại pipeline và kiểm hồ sơ đã chính thức.
  10. Mở hồ sơ nguồn J1, kiểm CV/note/trạng thái cũ không đổi; không có Candidate bản sao.
- **Expected Result:** Proposal ban đầu HM_Suggested, chưa formal. Consent hợp lệ chuyển cùng application thành HM_Passed, đủ xếp lịch; chưa xếp lịch chỉ bằng việc HM thêm vào tin. CV cũ và source giữ nguyên.
- **Actual Result:**
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** NEW-TALENT; GAP-04; OLD-PIPE; OR-07.
- **Source Code Reference:** `candidate/service/TalentPoolService.java#reviewSelection,#suggest,#confirm`; `candidate/controller/TalentPoolController.java#suggest,#confirm`; `templates/talent-pool/list.html`; `templates/talent-pool/suggestions.html`.

## ST-05 HR gợi ý cho HM, HM phản hồi và HR thu hồi

- **Test Case ID:** ST-05
- **Test Type:** System Test — Manual; State Transition/Decision Table
- **Test Scenario:** Phân biệt HR gửi gợi ý, HM đồng ý và Candidate đồng ý.
- **Preconditions:** J2 mở có HM_A; C1/C2/C3 potential với CV nguồn hợp lệ; mỗi Candidate chưa có đơn ở J2.
- **Test Data:** C1 cho nhánh HM Pass; C2 cho HM Fail; C3 cho HR withdraw.
- **Test Steps:**
  1. HR vào chi tiết J2, mở **Gợi ý đã gửi cho HM**, dùng nút chọn thêm ứng viên hiện có trên trang.
  2. Chọn CV của C1/C2/C3, kiểm người nhận HM/tin đích trong tổng kết rồi gửi.
  3. Kiểm cả ba có trạng thái chờ phản hồi; chưa thành đơn chính thức, chưa xuất hiện trong hàng đợi HR xác nhận liên hệ.
  4. HR chọn **Xóa gợi ý** cho C3; tải lại trang để xác nhận mất khỏi danh sách gợi ý đang dùng.
  5. Login HM_A, mở chi tiết J2 rồi **Gợi ý từ HR**; kiểm không còn C3.
  6. Mở C1, xem CV lịch sử và ghi chú chung; bấm **Chấp nhận**, xác nhận nếu có dialog.
  7. Quay lại danh sách, mở C2 rồi **Từ chối**; tải lại để kiểm status và không có nút xử lý lại.
  8. HR kiểm C1 đã chuyển sang chờ liên hệ nhưng chưa formal; không còn **Xóa gợi ý** theo điều kiện pending cũ.
  9. HR thử xác nhận C1 khi chưa consent rồi thực hiện consent hợp lệ như ST-04.
  10. Kiểm C2 không vào pipeline chính thức, CV/note nguồn vẫn còn; HR có thể chọn gửi lại C3 qua thao tác mới.
- **Expected Result:** HM Pass chưa tự thành HM_Passed. Fail/withdraw không xóa Candidate/CV/note, draft bị loại khỏi formal counts. Withdraw chỉ trước HM phản hồi; form/lịch đúng quyền.
- **Actual Result:**
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** NEW-TALENT; GAP-04; OR-01/06/07.
- **Source Code Reference:** `candidate/service/TalentPoolService.java#recommend,#decideRecommendation,#withdrawRecommendation,#confirm`; `templates/talent-pool/recommendations.html`; `templates/talent-pool/detail.html`.

## ST-06 Candidate nộp CV mới cho đề xuất đã HM duyệt và đang chờ HM

- **Test Case ID:** ST-06
- **Test Type:** System Test — Manual; Decision Table/State Transition
- **Test Scenario:** Kiểm nhánh bỏ qua vòng CV chỉ cho đúng proposal đã duyệt.
- **Preconditions:** C1 có proposal đã HM chấp nhận ở J2; C2 có HR recommendation chưa HM phản hồi ở J2; một J3 mở không có proposal C1. Các account profile riêng.
- **Test Data:** PDF mới khác file nguồn cho C1/C2; ghi proposal IDs trước thao tác nếu UI hiển thị.
- **Test Steps:**
  1. C1 vào public J2 từ danh sách việc làm, mở form ứng tuyển và nộp PDF mới.
  2. HR tải lại J2/application; kiểm đúng C1 và CV mới; mở hành động xếp lịch, chưa cần lưu lịch.
  3. HM/HR mở hồ sơ nguồn J1, kiểm CV nguồn cũ vẫn xem được.
  4. C2 nộp CV mới vào J2 khi gợi ý còn chờ HM.
  5. HR kiểm C2 đi vào vòng CV thông thường và chưa có quyền xếp lịch chỉ do có gợi ý; chờ AI nếu cần.
  6. C1 nộp CV vào J3 chưa có proposal; HR kiểm J3 cũng vào vòng sàng lọc thông thường.
  7. Ghi lại tên CV/đơn, trạng thái UI và ID ở cả ba nhánh; nếu không có ID trên UI, không tự kết luận tái dùng cùng bản ghi mà ghi cần kiểm integration DB T12/T13.
- **Expected Result:** C1/J2 đã duyệt chuyển HM_Passed và dùng CV mới. C2/J2 đang chờ HM và C1/J3 không có proposal phải Applied/AI_Screened, không bypass. Không lẫn CV giữa tin hoặc Candidate.
- **Actual Result:**
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** NEW-CV; NEW-TALENT; GAP-04; UC54/55.
- **Source Code Reference:** `candidate/service/ApplicationSubmissionService.java#apply`; `candidate/repository/ApplicationRepository.java#submitSuggestionWithNewCv`; `candidate/service/ApplicationStatusService.java#requireScheduleable`.

## ST-07 Bookmark và ghi chú tại kết quả Interview

- **Test Case ID:** ST-07
- **Test Type:** System Test — Manual; EP/State Transition/BVA
- **Test Scenario:** Dùng chung potential/note tại Interview và Application, độc lập với kết quả Pass/Fail.
- **Preconditions:** Hai fixture Interview đã final Passed/Failed; HR/HM được xem; Candidate chưa potential; chụp trạng thái kết quả/offer trước thao tác.
- **Test Data:** Note “QA Linh ghi chú sau phỏng vấn”; note mới của HR; chuỗi 1001 ký tự để thử giới hạn.
- **Test Steps:**
  1. HM mở lịch rồi kết quả Interview tại `/interviews/{id}/result`, ghi nhãn kết quả, tên Candidate và tin.
  2. Bấm nút **Ứng viên tiềm năng** trên trang; kiểm popup đúng Candidate và lịch sử note.
  3. Lưu potential kèm note; đóng/mở popup rồi tải lại trang.
  4. HR ở profile khác mở cùng hồ sơ Application và popup note; kiểm thấy tác giả/nội dung vừa nhập.
  5. HR thêm note thứ hai; thử 1001 ký tự và kiểm không được lưu vượt giới hạn.
  6. HM bỏ lưu qua popup; mở Talent Pool để kiểm Candidate không còn được liệt kê như potential.
  7. Mở lại popup tại Application, kiểm lịch sử note vẫn còn; mở kết quả Interview, đối chiếu Pass/Fail và Offer trước test.
  8. Lặp các bước 1–7 với kết quả còn lại, ghi kết quả thành hai biến thể riêng.
- **Expected Result:** Popup thao tác thật, cờ/note dùng chung giữa hai màn hình/role; bỏ lưu không xóa note/CV/đơn; không đổi Pass/Fail hoặc Offer. Note được render như văn bản, không chạy HTML/script do người dùng nhập.
- **Actual Result:**
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-INTERVIEW screen 28; NEW-TALENT; GBR-02; OR-04.
- **Source Code Reference:** `candidate/service/TalentPoolService.java#saveBookmark,#removeBookmark,#bookmark`; `templates/interview/result.html`; template bookmark Application dùng chung.

## ST-08 Requisition từ HM đến Director, HR đăng tin và đóng đợt

- **Test Case ID:** ST-08
- **Test Type:** System Test — Manual; State Transition/Decision Table
- **Test Scenario:** Kiểm liên kết duyệt headcount → tin được phép tuyển → dừng đợt đúng nghiệp vụ.
- **Preconditions:** HM_A, Director khác owner và HR ở ba browser profile; title mới; có khả năng chuẩn bị Candidate/đơn thử trên tin sau publish. Không dùng offer thực của dự án làm fixture phá hủy.
- **Test Data:** Requisition “QA Linh workflow <suffix>”, form đầy đủ theo validator; lý do reject tiếng Việt; CV test; một fixture offer chưa kết thúc để kiểm guard riêng.
- **Test Steps:**
  1. HM mở menu yêu cầu tuyển dụng, bấm tạo mới, nhập thông tin/criteria hợp lệ, lưu nháp; ghi mã yêu cầu.
  2. Gửi duyệt; tải lại kiểm nhãn chờ Director. HM không được tự bấm duyệt như Director.
  3. Director mở đúng yêu cầu, thử Từ chối không có lý do; kiểm lỗi. Nhập lý do và từ chối.
  4. HM mở lại, kiểm feedback/history, chỉnh sửa rồi gửi duyệt lại.
  5. Director chấp nhận; HR mở danh sách requisition Approved và kiểm thấy đúng yêu cầu.
  6. HR tạo tin từ yêu cầu này, điền form hợp lệ và đăng theo UI hiện có; mở public job bằng cửa sổ guest để kiểm được hiển thị.
  7. Chuẩn bị đơn Candidate và pending proposal thử cho tin, như ST-03/ST-04. Ghi trạng thái/CV trước đóng.
  8. Với fixture có Offer chưa kết thúc, HR thử đóng đợt; kiểm bị chặn và tin còn mở. Dùng fixture độc lập không còn open offer cho bước tiếp theo.
  9. HR bấm đóng đợt và xác nhận; tải lại chi tiết tin, Application, đề xuất và lịch liên quan.
  10. Kiểm không nhận hồ sơ mới, proposal dừng, đơn đang chờ có thông báo dừng do đóng đợt; CV/note không mất. Hồ sơ Accepted offer được kiểm riêng bằng fixture E03.
  11. Nếu test mail, chỉ dùng hộp thư test: kiểm thư báo dừng; lỗi SMTP phải có cảnh báo thao tác đã lưu, không tự thao tác đóng nhiều lần để “thử lại mail”.
- **Expected Result:** Feedback/approval đúng role và trạng thái; tin bắt nguồn requisition Approved; open offer chặn close; closure hợp lệ không biến draft thành đơn ứng tuyển chính thức hoặc báo CV không đạt sai nguyên nhân. Mail không quyết định rollback business.
- **Actual Result:**
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-REQ; GBR-09; NEW-CLOSURE; GAP-02; OR-06/07.
- **Source Code Reference:** `requisition/service/RequisitionServiceImpl.java#createRequisition,#updateRequisition,#decide`; `jobposting/service/JobPostingServiceImpl.java#createJobPosting,#publishJobPosting`; `candidate/service/TalentPoolService.java#closeJob`; `candidate/service/RecruitmentEmailService.java#sendJobClosure`.

Source Code Reference trong file này là đường dẫn tương đối dưới `src/main/java/com/group2/rms/`, trừ đường dẫn `templates/` dưới `src/main/resources/`. Tất cả integration case có link file tuyệt đối và method trong INTEGRATION_TEST_CASES. Các tình huống race/transaction/file rollback cần bộ Integration Test, không chứng minh được chỉ bằng các bước manual bên trên.

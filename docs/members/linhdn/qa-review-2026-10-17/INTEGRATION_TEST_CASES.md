# Integration Test cases RMS của Linh

Ngày lập: 17/10/2026. **64 ca thiết kế**, chưa thực thi. Actual Result để trống và Status Pending ở từng ca. Code hiện tại là cơ sở Expected Result; SRS cũ chỉ dùng truy vết theo xác nhận của chủ dự án.

Đọc [Test Plan](TEST_PLAN.md) trước khi thực hiện, đặc biệt phần transaction, DB test riêng, mail/file và fixture. Nếu chỉ thao tác website, dùng [System Test thủ công](SYSTEM_TEST_MANUAL.md). Mỗi biến thể cần evidence riêng, có thể thêm hậu tố `.v1`, `.v2`.

## Quy ước truy vết

SRS đọc tại `D:/KÌ 5/SWP/hireflow-api/RMS_SRS.docx`. Pxxxx là thứ tự paragraph OOXML, không phải số trang Word. OLD không phải acceptance oracle. GAP có trong Test Plan; NEW là tài liệu/code hiện tại, không phải mã yêu cầu SRS được tự tạo.

| Reference | Phạm vi |
|---|---|
| OLD-AUTH | SRS 5.1 Screen Inventory 1–5, P0913–P0932; UC56; reset có GAP-01 |
| OLD-ACC | Screens 6–11, P0933–P0956; UC10–18; deactivate có GAP-08 |
| OLD-PIPE | Screens 22–24, P0997–P1008; UC27–30/48–49; GAP-03/05/07 |
| OLD-APPLY | Screen 36, P1053–P1056; UC54/55, P0728–P0735; GAP-06 |
| OLD-MY | Screen 38, P1061–P1064; UC57/58, P0740–P0747 |
| OLD-REQ | Screens 14–20, P0965–P0992; UC19–22/32–35/43; GAP-02 |
| OLD-INTERVIEW | Screens 25–29, P1009–P1028 |
| OLD-TALENT | Chưa xác định requirement Talent Pool chi tiết khớp luồng mới trong SRS cũ; dùng NEW-TALENT |
| NEW-OTP | RegistrationVerificationService, PasswordRecoveryService, PasswordResetService, OTP flow hiện tại |
| NEW-TALENT | TalentPoolService; [HM suggestions](../2026-10-17-hm-suggestions-functional.md); [HR → HM Flow C](../2026-10-17-hr-hm-recommendations-flow-c.md); [interview bookmark](../2026-10-17-interview-bookmark-toggle.md) |
| NEW-CV | ApplicationSubmissionService.apply; [CV mới cho đề xuất đã duyệt](../2026-10-17-new-cv-approved-proposal-interview.md) |
| NEW-CLOSURE | TalentPoolService.closeJob, TalentPoolAuditRepository, RecruitmentEmailService; chưa gán mã SRS mới |
| GBR / OR / MSG | ID có thật trong Appendix SRS cũ; đọc GAP trước khi dùng làm tiêu chí nghiệm thu |

EP = Equivalence Partitioning; BVA = Boundary Value Analysis; DT = Decision Table; ST = State Transition. Existing coverage là nhận xét đọc source, không phải kết quả chạy. Mỗi source link trỏ đến file và khai báo method đã kiểm tra tồn tại trong checkout.

## Bảng quyết định CV mới

Giả sử Candidate/job/CV hợp lệ và chưa có đơn active.

| Proposal tại đúng Candidate và job | ID sau submit | Trạng thái ban đầu | Event AI | Case |
|---|---|---|---|---|
| HM tự suggest | Tái dùng proposal | HM_Passed | Không | T12 |
| HR recommend, HM đã Pass | Tái dùng proposal | HM_Passed | Không | T12 |
| HR recommend, HM chưa phản hồi | Tái dùng proposal | Applied | Sau commit | T13 |
| Không proposal, kể cả potential hoặc proposal tin khác | Đơn mới | Applied | Sau commit | T14/P01 |

Applied có thể chuyển AI_Screened sau async; không kiểm snapshot muộn với expected cố định Applied.

## Bảng quyết định CV cũ

| Role | HM cho phép | Consent | Job mở và CV còn | Không trùng đơn active | Kết quả | Case |
|---|---|---|---|---|---|---|
| HR | Có | Có | Có | Có | Cùng proposal → HM_Passed | T06 |
| HR | Có | Không | Có | Có | Chặn | T06 |
| HR | Chưa, recommendation pending | Có | Có | Có | Chặn | T11 |
| HM | Có | Có | Có | Có | Chặn theo quyền | T11 |
| HR | Có | Có | Không | Có | Chặn | T11/T03 |
| HR | Có | Có | Có | Không | Chặn | T16 |

Đây là nhóm rule rủi ro chính, không tuyên bố mọi tổ hợp đã chạy.

## Danh sách nhanh

| Test Case ID | Scenario |
|---|---|
| [IT-A01](#it-a01) | Đăng nhập bằng username hoặc email qua Security và DB thật |
| [IT-A02](#it-a02) | Sai password, user không có, Inactive và Blocked không được login |
| [IT-A03](#it-a03) | Đăng ký chỉ ghi User và Candidate sau OTP đúng |
| [IT-A04](#it-a04) | Chặn đăng ký trùng và xung đột identifier |
| [IT-A05](#it-a05) | Biên validation form đăng ký và OTP |
| [IT-A06](#it-a06) | Hết hạn và số lần nhập sai OTP đăng ký |
| [IT-A07](#it-a07) | OTP đăng ký không dùng chéo session, resend làm mã cũ mất hiệu lực |
| [IT-A08](#it-a08) | Forgot → OTP → đổi password → login bằng password mới |
| [IT-A09](#it-a09) | Không bypass bước OTP và không tái sử dụng phiên reset |
| [IT-A10](#it-a10) | Reset bị vô hiệu khi account snapshot đổi hoặc account không Active |
| [IT-A11](#it-a11) | Biên reset OTP, resend và lỗi SMTP |
| [IT-A12](#it-a12) | Logout và quay lại tài nguyên bảo vệ |
| [IT-S01](#it-s01) | Ma trận quyền ở route admin, pipeline, Talent Pool và quyết định |
| [IT-S02](#it-s02) | HM không xem hoặc duyệt application ngoài phòng ban |
| [IT-S03](#it-s03) | Admin deactivate Candidate làm session đang mở mất quyền |
| [IT-S04](#it-s04) | Session cũ bị loại khi role DB thay đổi |
| [IT-S05](#it-s05) | CSRF bảo vệ các ghi dữ liệu đang có |
| [IT-S06](#it-s06) | Candidate không đọc internal note, AI review và CV người khác |
| [IT-C01](#it-c01) | Candidate Account list chỉ đúng nhóm và tìm lọc/phân trang |
| [IT-C02](#it-c02) | Deactivate → activate Candidate giữ nguyên hồ sơ |
| [IT-C03](#it-c03) | Chặn activate Blocked và sai nhóm account |
| [IT-C04](#it-c04) | Tài khoản nội bộ không tạo/sửa identity Candidate qua form nội bộ |
| [IT-C05](#it-c05) | Writer User + Candidate rollback khi ghi profile thất bại |
| [IT-P01](#it-p01) | Candidate nộp CV thường và event sau commit |
| [IT-P02](#it-p02) | CV trống, giả PDF và vượt giới hạn bị chặn trước ghi dữ liệu |
| [IT-P03](#it-p03) | Không nộp được vào tin Closed, Paused hoặc hết hạn |
| [IT-P04](#it-p04) | Chống nộp trùng tuần tự và đồng thời |
| [IT-P05](#it-p05) | Rollback nộp đơn xóa file đã lưu và không kích hoạt AI |
| [IT-P06](#it-p06) | HR và HM review theo đúng vai trò, trạng thái và lịch sử |
| [IT-P07](#it-p07) | AI lỗi không mất đơn, rescreen không kéo lùi trạng thái |
| [IT-P08](#it-p08) | Rút đơn chỉ được ở vòng đầu, đúng owner và không xóa dữ liệu |
| [IT-P09](#it-p09) | Review stale hoặc song song không tạo quyết định tiến bước hai lần |
| [IT-T01](#it-t01) | Bookmark Candidate và ghi chú dùng chung từ Application |
| [IT-T02](#it-t02) | Bookmark rỗng, note dài và bỏ lưu giữ lịch sử |
| [IT-T03](#it-t03) | Xem CV nguồn phải khớp Candidate và file còn đọc được |
| [IT-T04](#it-t04) | Selection review read-only và giới hạn số CV |
| [IT-T05](#it-t05) | HM chọn kho tiềm năng tạo đề xuất ở tin đích |
| [IT-T06](#it-t06) | HR xác nhận consent dùng CV cũ kích hoạt cùng proposal |
| [IT-T07](#it-t07) | HR kết thúc liên hệ không đồng ý dùng CV cũ |
| [IT-T08](#it-t08) | HR gợi ý cho HM chưa được xem là consent hoặc formal application |
| [IT-T09](#it-t09) | HM Pass gợi ý HR chỉ cho phép HR liên hệ, chưa tự active |
| [IT-T10](#it-t10) | HM Fail và HM khác phòng không được xử lý gợi ý |
| [IT-T11](#it-t11) | Không confirm/decline trước HM, sai role hoặc ở job hết hạn |
| [IT-T12](#it-t12) | Nộp CV mới cho đề xuất đã được HM duyệt đi thẳng HM_Passed |
| [IT-T13](#it-t13) | CV mới khi HR recommendation còn chờ HM vẫn phải sàng lọc |
| [IT-T14](#it-t14) | Chỉ là potential hoặc proposal ở tin khác không được bỏ qua sàng lọc |
| [IT-T15](#it-t15) | HR thu hồi gợi ý chưa HM phản hồi và gửi lại |
| [IT-T16](#it-t16) | Chống gửi trùng, batch không ghi một phần và revalidate khi confirm selection |
| [IT-T17](#it-t17) | Bookmark từ Interview Passed/Failed không làm đổi kết quả tuyển dụng |
| [IT-E01](#it-e01) | Đóng đợt có open offer phải bị chặn |
| [IT-E02](#it-e02) | Đóng job hủy draft và dừng các đơn đang chờ có audit phân biệt |
| [IT-E03](#it-e03) | Job closure không loại hồ sơ đã nhận offer, gọi lại không xử lý đôi |
| [IT-E04](#it-e04) | Lỗi SMTP sau close không rollback business hoặc gửi nhầm người |
| [IT-E05](#it-e05) | Invite/resend Interview dùng dữ liệu lịch hiện tại và quyền đúng |
| [IT-R01](#it-r01) | HM submit requisition và Director khác người tạo approve |
| [IT-R02](#it-r02) | Reject requisition bắt buộc lý do và lưu feedback |
| [IT-R03](#it-r03) | Chặn tự duyệt, sai role và quyết định khi không pending |
| [IT-R04](#it-r04) | Owner rút yêu cầu đang chờ về Draft và quyền sửa sau rút |
| [IT-R05](#it-r05) | Approve và withdraw cạnh tranh không ghi lịch sử mâu thuẫn |
| [IT-R06](#it-r06) | HR chỉ lấy Approved requisition và Job Posting dùng requisition hợp lệ |
| [IT-M01](#it-m01) | Tạo/sửa internal account không đổi username và password khi update thông tin |
| [IT-M02](#it-m02) | Department lifecycle và chọn manager đúng phòng |
| [IT-M03](#it-m03) | Dashboard theo role và Candidate ownership với số liệu DB biết trước |
| [IT-M04](#it-m04) | Pipeline query không trùng và detail/CV không lỗi lazy load |

<a id="it-a01"></a>

## IT-A01 Đăng nhập bằng username hoặc email qua Security và DB thật

- **Test Case ID:** IT-A01
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** EP/DT
- **Test Scenario:** Đăng nhập bằng username hoặc email qua Security và DB thật
- **Preconditions:** Có Candidate Active và một internal account Active, BCrypt hợp lệ.
- **Test Data:** Mỗi account: username; email; identifier có khoảng trắng đầu/cuối; đúng mật khẩu.
- **Test Steps:** 1. GET /login lấy CSRF. 2. POST /login bằng từng định danh với session mới. 3. Dùng session nhận được GET /dashboard. 4. Đối chiếu principal với user DB.
- **Expected Result:** Login thành công, principal là username chuẩn của đúng account, truy cập dashboard đúng role; không tạo User/Candidate mới.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-AUTH; GBR-04; OR-02
- **Source Code Reference:** [SecurityConfig.filterChain](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java:50>); [DatabaseUserDetailsService.loadUserByUsername](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/security/DatabaseUserDetailsService.java:25>)
- **Existing Test Coverage (static review):** AuthBotanicalWebTests có web mock; thiếu proof login DB xuyên stack.

<a id="it-a02"></a>

## IT-A02 Sai password, user không có, Inactive và Blocked không được login

- **Test Case ID:** IT-A02
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** EP/DT
- **Test Scenario:** Sai password, user không có, Inactive và Blocked không được login
- **Preconditions:** Các account trạng thái riêng đã chuẩn bị.
- **Test Data:** Sai password; identifier không tồn tại; Inactive đúng password; Blocked đúng password. Mỗi biến thể một session.
- **Test Steps:** 1. GET login lấy token. 2. POST login từng biến thể. 3. Thử GET dashboard bằng session vừa dùng.
- **Expected Result:** Không có session xác thực truy cập tài nguyên; về lỗi login chung, không lộ có/không có username/email. Hash và trạng thái DB không đổi.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-AUTH; GBR-04/05; OR-02/03
- **Source Code Reference:** [SecurityConfig.filterChain](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java:50>); [DatabaseUserDetailsService.loadUserByUsername](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/security/DatabaseUserDetailsService.java:25>)
- **Existing Test Coverage (static review):** SecurityFlowTests đang comment; AuthBotanicalWebTests không thay thế DB login.

<a id="it-a03"></a>

## IT-A03 Đăng ký chỉ ghi User và Candidate sau OTP đúng

- **Test Case ID:** IT-A03
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** Đăng ký chỉ ghi User và Candidate sau OTP đúng
- **Preconditions:** Email thử nghiệm nhận được mail; chưa có username/email.
- **Test Data:** qa_linh_<suffix>; email test duy nhất; password QaLinh123!; OTP nhận qua sender được kiểm soát.
- **Test Steps:** 1. POST /register form hợp lệ. 2. Đọc DB từ transaction khác trước OTP. 3. POST /register/otp đúng trong cùng session. 4. Đọc lại User/Candidate. 5. Thử login.
- **Expected Result:** Trước OTP không có account/profile mới. Sau OTP có đúng 1 User Active role Candidate, 1 Candidate liên kết, BCrypt khớp password; không có department; state đăng ký bị xóa; login được.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-AUTH screen 2; UC56
- **Source Code Reference:** [RegistrationController.register](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/controller/RegistrationController.java:35>); [RegistrationController.verifyOtp](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/controller/RegistrationController.java:58>); [RegistrationVerificationService.verifyAndRegister](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/RegistrationVerificationService.java:87>); [AccountManagementService.createCandidateWithPasswordHash](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountManagementService.java:105>)
- **Existing Test Coverage (static review):** AuthenticationDatabaseTests có writer-only; chưa chứng minh toàn bộ OTP → DB.

<a id="it-a04"></a>

## IT-A04 Chặn đăng ký trùng và xung đột identifier

- **Test Case ID:** IT-A04
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** EP/DT
- **Test Scenario:** Chặn đăng ký trùng và xung đột identifier
- **Preconditions:** Có user username/email cố định trong DB.
- **Test Data:** Trùng username khác hoa/thường; trùng email; username mới bằng email account khác; email mới bằng username account khác nếu định dạng email hợp lệ.
- **Test Steps:** 1. Mỗi lần chỉ làm trùng một identifier. 2. Gửi register với các field còn lại hợp lệ. 3. Đối chiếu DB và số lần gửi mail. 4. Riêng trường hợp tranh chấp: sau bước gửi OTP, tạo account cạnh tranh trong DB test rồi verify.
- **Expected Result:** Thông báo conflict hợp lệ, không tạo account/profile trùng hoặc orphan; kiểm tra availability trước gửi và writer kiểm tra lại khi verify. Không coi case định dạng email sai là đã test cross-identifier conflict.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-AUTH screen 2; OR-07
- **Source Code Reference:** [RegistrationVerificationService.requestRegistration](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/RegistrationVerificationService.java:43>); [AccountManagementService.assertLoginIdentifiersAvailable](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountManagementService.java:95>); [AccountManagementService.createCandidateWithPasswordHash](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountManagementService.java:105>)
- **Existing Test Coverage (static review):** AccountManagementServiceTests kiểm rule; cần persistence/transaction và conflict lúc OTP.

<a id="it-a05"></a>

## IT-A05 Biên validation form đăng ký và OTP

- **Test Case ID:** IT-A05
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** BVA
- **Test Scenario:** Biên validation form đăng ký và OTP
- **Preconditions:** Username/email mới; sender kiểm soát; giữ các field khác đúng.
- **Test Data:** Password 7/8/9/31/32/33 ký tự; fullname 99/100/101; username 49/50/51; OTP 5/6/7 chữ số; confirm không khớp; OTP có chữ.
- **Test Steps:** 1. POST register với từng biến thể riêng. 2. Với form hợp lệ đi đến OTP. 3. POST OTP sai cấu trúc. 4. Kiểm tra lỗi field, mail và DB.
- **Expected Result:** Password nhận 8–32, fullname tối đa 100, username tối đa 50; mismatch bị chặn; OTP DTO nhận đúng 6 số. Validation không hợp lệ không ghi User/Candidate. OTP 6 số nhưng không khớp vẫn bị từ chối nghiệp vụ.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-AUTH screen 2; MSG02/04/05
- **Source Code Reference:** [RegistrationController.register](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/controller/RegistrationController.java:35>); [RegistrationController.verifyOtp](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/controller/RegistrationController.java:58>); [RegistrationVerificationService.requestRegistration](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/RegistrationVerificationService.java:43>)
- **Existing Test Coverage (static review):** AuthBotanicalWebTests cover một số form; bổ sung biên xuyên controller.

<a id="it-a06"></a>

## IT-A06 Hết hạn và số lần nhập sai OTP đăng ký

- **Test Case ID:** IT-A06
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** BVA/ST
- **Test Scenario:** Hết hạn và số lần nhập sai OTP đăng ký
- **Preconditions:** Có pending registration, thời gian tạo đã ghi lại; dữ liệu mới mỗi biến thể.
- **Test Data:** OTP đúng tại 899/900/901 giây sau phát hành; sai lần 4 và lần 5.
- **Test Steps:** 1. Tạo pending. 2. Ở đúng mốc thời gian test, verify OTP. 3. Với fixture riêng nhập sai đủ lần rồi nhập mã đúng. 4. Kiểm tra session và DB.
- **Expected Result:** Trước expiry được verify; đúng expiry và sau expiry bị chặn. Sai lần 4 vẫn ở OTP; sai lần 5 xóa pending, phải đăng ký lại; không sinh User/Candidate. Khi thử qua HTTP, DTO sai cấu trúc có thể bị chặn trước service nên dùng mã đủ 6 số nhưng sai để đếm attempts.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-AUTH screen 2; NEW-OTP
- **Source Code Reference:** [RegistrationVerificationService.verifyAndRegister](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/RegistrationVerificationService.java:87>)
- **Existing Test Coverage (static review):** Chưa thấy suite DB toàn bộ OTP; cần test clock/session, không đổi clock máy chung.

<a id="it-a07"></a>

## IT-A07 OTP đăng ký không dùng chéo session, resend làm mã cũ mất hiệu lực

- **Test Case ID:** IT-A07
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** BVA/ST
- **Test Scenario:** OTP đăng ký không dùng chéo session, resend làm mã cũ mất hiệu lực
- **Preconditions:** Hai session S1/S2; chưa tạo account.
- **Test Data:** OTP1; resend tại 299/300/301 giây; OTP2 từ lần gửi lại.
- **Test Steps:** 1. Request register ở S1. 2. Dùng OTP1 ở S2. 3. Resend ở S1 theo từng mốc với fixture riêng. 4. Sau resend thử OTP1 rồi OTP2. 5. Submit lại sau thành công.
- **Expected Result:** S2 thiếu state không đăng ký được. Trước cooldown chưa gửi lại, từ hết cooldown cho gửi. Mã cũ không còn đúng sau resend, mã mới đúng tạo đúng một account; replay sau thành công không tạo thêm.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-AUTH screen 2; NEW-OTP
- **Source Code Reference:** [RegistrationVerificationService.requestRegistration](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/RegistrationVerificationService.java:43>); [RegistrationVerificationService.resendOtp](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/RegistrationVerificationService.java:80>); [RegistrationVerificationService.verifyAndRegister](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/RegistrationVerificationService.java:87>)
- **Existing Test Coverage (static review):** Cần integration session + sender + writer; không dùng test signed link cũ.

<a id="it-a08"></a>

## IT-A08 Forgot → OTP → đổi password → login bằng password mới

- **Test Case ID:** IT-A08
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** Forgot → OTP → đổi password → login bằng password mới
- **Preconditions:** Account Active; mail test kiểm soát; biết password cũ.
- **Test Data:** Email account; OTP đúng; password mới khác cũ và dài 8–32.
- **Test Steps:** 1. POST /forgot-password. 2. GET /reset-password trước verify. 3. POST /reset-password/otp đúng. 4. POST /reset-password mới. 5. Đọc hash DB từ transaction khác. 6. Login cũ/mới bằng session mới.
- **Expected Result:** Trước OTP không mở được bước reset hợp lệ. Sau verify đổi hash bằng BCrypt, password mới login được, cũ không được. State reset bị xóa; không thay identity/role/profile.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-AUTH screen 3; GAP-01; OR-02
- **Source Code Reference:** [PasswordRecoveryController.requestReset](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/controller/PasswordRecoveryController.java:35>); [PasswordRecoveryService.verifyOtp](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordRecoveryService.java:107>); [PasswordRecoveryService.completeReset](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordRecoveryService.java:146>); [PasswordResetService.changePassword](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordResetService.java:91>)
- **Existing Test Coverage (static review):** AuthenticationDatabaseTests phần reset signed link đã comment; web slice không chứng minh hash DB.

<a id="it-a09"></a>

## IT-A09 Không bypass bước OTP và không tái sử dụng phiên reset

- **Test Case ID:** IT-A09
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** EP/ST
- **Test Scenario:** Không bypass bước OTP và không tái sử dụng phiên reset
- **Preconditions:** Có reset S1; session S2 trống.
- **Test Data:** POST trực tiếp password mới; S2 dùng mã S1; replay form sau reset thành công.
- **Test Steps:** 1. Gửi reset khi chưa có state. 2. Tạo state chưa verify rồi POST reset. 3. Dùng session khác verify/reset. 4. Hoàn tất ở S1 rồi submit lần nữa. 5. So hash trước/sau mỗi request.
- **Expected Result:** Chỉ state server cùng session đã verify còn hạn mới đổi được password; các bước bypass/replay không ghi hash; không tin cờ verified do client gửi.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-AUTH screen 3; GAP-01; OR-02/06
- **Source Code Reference:** [PasswordRecoveryService.canResetPassword](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordRecoveryService.java:140>); [PasswordRecoveryService.verifyOtp](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordRecoveryService.java:107>); [PasswordRecoveryService.completeReset](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordRecoveryService.java:146>)
- **Existing Test Coverage (static review):** Khoảng trống security/reset integration hiện tại.

<a id="it-a10"></a>

## IT-A10 Reset bị vô hiệu khi account snapshot đổi hoặc account không Active

- **Test Case ID:** IT-A10
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT/ST
- **Test Scenario:** Reset bị vô hiệu khi account snapshot đổi hoặc account không Active
- **Preconditions:** Đã nhận OTP hoặc đã verify trong S1; có thao tác quản trị fixture độc lập.
- **Test Data:** Sau phát OTP: đổi password bằng luồng khác; đổi email; deactivate account. Mỗi biến thể riêng.
- **Test Steps:** 1. Chụp snapshot ban đầu. 2. Thực hiện một thay đổi account trong transaction khác. 3. Tiếp tục verify/reset bằng state cũ. 4. Đọc lại hash/status.
- **Expected Result:** State cũ không còn current, không ghi đè password hoặc phục hồi account Inactive; yêu cầu lại flow phù hợp. Snapshot/expiry được kiểm tra lại trong writer có lock.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** GBR-05; OR-03/07; GAP-01
- **Source Code Reference:** [PasswordResetService.isCurrent](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordResetService.java:79>); [PasswordResetService.changePassword](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordResetService.java:91>); [PasswordRecoveryService.completeReset](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordRecoveryService.java:146>)
- **Existing Test Coverage (static review):** Cần DB lock/snapshot integration, không chỉ mock repository.

<a id="it-a11"></a>

## IT-A11 Biên reset OTP, resend và lỗi SMTP

- **Test Case ID:** IT-A11
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** BVA/ST
- **Test Scenario:** Biên reset OTP, resend và lỗi SMTP
- **Preconditions:** Account Active; sender có thể trả lỗi có kiểm soát.
- **Test Data:** Reset expiry 899/900/901 giây; cooldown 299/300/301; sai OTP lần 4/5; mail failure.
- **Test Steps:** 1. Tạo reset fixture độc lập cho từng mốc. 2. Verify/resend tương ứng. 3. Một lượt giả lập mail thất bại. 4. Kiểm tra state/điều hướng/hash DB.
- **Expected Result:** OTP hết hạn từ mốc 900s; resend trước 300s bị chặn. Sai lần 5 xóa state về Forgot. Lỗi gửi mail không đổi password và không báo gửi thành công; không tạo User/Candidate. Xác nhận UI countdown khớp service, không dùng countdown làm cơ chế bảo vệ duy nhất.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-AUTH screen 3; NEW-OTP; OR-02
- **Source Code Reference:** [PasswordRecoveryService.requestReset](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordRecoveryService.java:52>); [PasswordRecoveryService.verifyOtp](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordRecoveryService.java:107>); [PasswordRecoveryService.getResendRemainingSeconds](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/auth/service/PasswordRecoveryService.java:169>)
- **Existing Test Coverage (static review):** Cần session/clock/sender integration; dùng thời gian điều khiển trong test harness sau này.

<a id="it-a12"></a>

## IT-A12 Logout và quay lại tài nguyên bảo vệ

- **Test Case ID:** IT-A12
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** Logout và quay lại tài nguyên bảo vệ
- **Preconditions:** Đã login thật, session có thể vào dashboard.
- **Test Data:** POST /logout có CSRF; URL /dashboard trước logout.
- **Test Steps:** 1. Mở dashboard. 2. POST logout. 3. Gửi request dashboard bằng cookie cũ. 4. Mở URL bảo vệ lại.
- **Expected Result:** Redirect /login?logout; session cũ không còn quyền; tài nguyên bảo vệ yêu cầu login. Không kết luận cache Back của browser là session còn sống nếu chưa gửi request mới.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-AUTH screen 4; OR-03
- **Source Code Reference:** [SecurityConfig.filterChain](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java:50>)
- **Existing Test Coverage (static review):** SecurityFlowTests đang comment; bổ sung integration session thật.

<a id="it-s01"></a>

## IT-S01 Ma trận quyền ở route admin, pipeline, Talent Pool và quyết định

- **Test Case ID:** IT-S01
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT
- **Test Scenario:** Ma trận quyền ở route admin, pipeline, Talent Pool và quyết định
- **Preconditions:** Các account Active với role thật trong DB; ID tài nguyên tồn tại.
- **Test Data:** Candidate; HR; HM; Director; Interviewer; System Admin; guest.
- **Test Steps:** 1. Mỗi role login session mới. 2. GET /admin/candidate-accounts, /applications, /talent-pool. 3. Thử POST một hành động tương ứng với CSRF hợp lệ. 4. So DB trước/sau request bị cấm.
- **Expected Result:** Admin route chỉ System Admin. Pipeline HR/HM/Director/Admin; Talent Pool chỉ HR/HM. Candidate/Interviewer không vào pipeline chung. Guest phải login. Role được xem chưa chắc được review/confirm; request bị cấm không ghi DB.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OR-01; OLD-PIPE; GAP-03/05
- **Source Code Reference:** [SecurityConfig.filterChain](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java:50>); [ApplicationAccess.canReview](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationAccess.java:72>); [TalentPoolService.actor](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:39>)
- **Existing Test Coverage (static review):** Web tests có role mocks; cần account DB và route thực.

<a id="it-s02"></a>

## IT-S02 HM không xem hoặc duyệt application ngoài phòng ban

- **Test Case ID:** IT-S02
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT
- **Test Scenario:** HM không xem hoặc duyệt application ngoài phòng ban
- **Preconditions:** HM_A, HM_B; đơn chính thức của phòng A đã HR Pass.
- **Test Data:** ID application phòng A; HM_B có role HM hợp lệ.
- **Test Steps:** 1. Login HM_B. 2. Tìm đơn ở /applications. 3. GET trực tiếp detail/CV bằng ID. 4. POST review với CSRF đúng. 5. So review/status.
- **Expected Result:** Không xuất hiện trong list; detail/CV/review ngoài scope bị từ chối; không có review hoặc thay đổi trạng thái. HM_A chỉ xem hồ sơ thường sau HR forward hoặc hồ sơ có talent-origin theo rule hiện tại.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OR-01/04; OLD-PIPE; GAP-05
- **Source Code Reference:** [ApplicationAccess.canView](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationAccess.java:52>); [ApplicationAccess.canReview](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationAccess.java:72>); [ApplicationPipelineService.list](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationPipelineService.java:68>); [ApplicationPipelineService.cv](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationPipelineService.java:177>)
- **Existing Test Coverage (static review):** ApplicationAccessTests và PipelineQueryTests có scope; thêm xuyên request + DB.

<a id="it-s03"></a>

## IT-S03 Admin deactivate Candidate làm session đang mở mất quyền

- **Test Case ID:** IT-S03
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** Admin deactivate Candidate làm session đang mở mất quyền
- **Preconditions:** C1 Active đã login ở session S1; Admin ở S2.
- **Test Data:** Candidate UserId đúng của C1; S1 đang có cookie.
- **Test Steps:** 1. S1 GET /portal/applications. 2. S2 POST deactivate Candidate. 3. S1 gửi request bảo vệ mới. 4. Thử login C1 lại. 5. Đọc Candidate/Application/CV metadata.
- **Expected Result:** User thành Inactive, request S1 tiếp theo bị session guard loại, login lại thất bại; Candidate/Application/CV không bị xóa. Không cần restart server để status có hiệu lực.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** GBR-05; OR-03; OLD-ACC
- **Source Code Reference:** [CandidateAccountController.deactivate](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/controller/CandidateAccountController.java:37>); [AccountListService.deactivateCandidate](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountListService.java:137>); [AccountSessionGuardFilter.doFilterInternal](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/security/AccountSessionGuardFilter.java:28>)
- **Existing Test Coverage (static review):** Thiếu evidence xuyên session thật; SecurityFlowTests không hoạt động.

<a id="it-s04"></a>

## IT-S04 Session cũ bị loại khi role DB thay đổi

- **Test Case ID:** IT-S04
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** Session cũ bị loại khi role DB thay đổi
- **Preconditions:** Internal account Active đã login; Admin có thể cập nhật role bằng luồng thật.
- **Test Data:** Role ban đầu HR, đổi sang role nội bộ khác hợp lệ.
- **Test Steps:** 1. Login user. 2. Admin đổi role. 3. Dùng session cũ GET route bảo vệ. 4. Login lại và kiểm quyền role mới.
- **Expected Result:** Role authority cũ không còn khớp DB nên guard loại session; login lại nhận role mới; quyền cũ không tiếp tục dùng được.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OR-01/03
- **Source Code Reference:** [AccountSessionGuardFilter.doFilterInternal](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/security/AccountSessionGuardFilter.java:28>); [AccountManagementService.updateInternal](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountManagementService.java:60>)
- **Existing Test Coverage (static review):** Đề xuất integration account update → session guard.

<a id="it-s05"></a>

## IT-S05 CSRF bảo vệ các ghi dữ liệu đang có

- **Test Case ID:** IT-S05
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT
- **Test Scenario:** CSRF bảo vệ các ghi dữ liệu đang có
- **Preconditions:** Login đúng role từng hành động; dữ liệu hợp lệ khác không đổi.
- **Test Data:** POST /register, /logout, /applications/{id}/review, /talent-pool/jobs/{id}/suggest, Candidate deactivate: token thiếu/sai/đúng.
- **Test Steps:** 1. Gửi mỗi request không token. 2. Token sai. 3. Token đúng ở fixture mới. 4. So DB/mail/state.
- **Expected Result:** Thiếu/sai token bị 403, không ghi DB hoặc gửi OTP. Token đúng mới đến validation/business rule. /api/** là ngoại lệ cấu hình hiện tại, không ghi kỳ vọng 403 CSRF chung cho mọi API.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OR-01/02; OLD-AUTH/PIPE
- **Source Code Reference:** [SecurityConfig.filterChain](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java:50>)
- **Existing Test Coverage (static review):** Web slices có một số CSRF; cần smoke với context đầy đủ.

<a id="it-s06"></a>

## IT-S06 Candidate không đọc internal note, AI review và CV người khác

- **Test Case ID:** IT-S06
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** EP/DT
- **Test Scenario:** Candidate không đọc internal note, AI review và CV người khác
- **Preconditions:** C1/C2 có đơn khác nhau; note HR/HM tồn tại.
- **Test Data:** C1 truy cập ID C2 ở /portal/applications/{id}/withdraw; ID ở /applications/{id}/cv và bookmark API.
- **Test Steps:** 1. Login C1. 2. Xem danh sách của mình. 3. Gửi trực tiếp các URL thuộc internal hoặc C2. 4. Kiểm HTML/JSON trả về và DB.
- **Expected Result:** C1 chỉ thấy đơn của mình; không có nội dung note/evaluation nội bộ trong portal; endpoint internal bị cấm, rút đơn người khác không tìm thấy/không được thực hiện; CV/note không lọt qua response lỗi.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** GBR-02; OR-01/04; OLD-MY
- **Source Code Reference:** [MyApplicationService.list](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/MyApplicationService.java:33>); [MyApplicationService.withdraw](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/MyApplicationService.java:47>); [SecurityConfig.filterChain](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java:50>)
- **Existing Test Coverage (static review):** MyApplicationWebTests/QueryTests có phần coverage; cần fixture hai Candidate.

<a id="it-c01"></a>

## IT-C01 Candidate Account list chỉ đúng nhóm và tìm lọc/phân trang

- **Test Case ID:** IT-C01
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** EP/BVA
- **Test Scenario:** Candidate Account list chỉ đúng nhóm và tìm lọc/phân trang
- **Preconditions:** Ít nhất 11 Candidate, có internal account, nhiều status; biết tập expected độc lập.
- **Test Data:** Tên/email/username, hoa thường; search %, _; status Active/Inactive/Blocked; page 0/1; sort name.
- **Test Steps:** 1. Admin GET /admin/candidate-accounts với từng bộ lọc. 2. Đối chiếu tập UserId biết trước. 3. Chuyển trang, refresh. 4. Kiểm response không chứa passwordHash.
- **Expected Result:** Chỉ role Candidate, không trộn nội bộ; page size 10, tìm/lọc không tạo bản ghi trùng; ký tự LIKE được xử lý theo escape của service; không lộ hash. Không dùng chính query cần test để tạo expected.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-ACC screen 6; GAP-08
- **Source Code Reference:** [CandidateAccountController.list](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/controller/CandidateAccountController.java:25>); [AccountListService.findCandidateAccounts](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountListService.java:60>)
- **Existing Test Coverage (static review):** AccountListQueryTests đang comment; AccountListServiceTests có unit.

<a id="it-c02"></a>

## IT-C02 Deactivate → activate Candidate giữ nguyên hồ sơ

- **Test Case ID:** IT-C02
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** Deactivate → activate Candidate giữ nguyên hồ sơ
- **Preconditions:** Candidate Active đã có application/CV; Admin đăng nhập.
- **Test Data:** UserId C1; snapshot CandidateId, ApplicationId, CV URL, hash.
- **Test Steps:** 1. Admin deactivate. 2. Đọc DB và thử login. 3. Admin activate. 4. Đọc lại DB và login mới.
- **Expected Result:** Active → Inactive → Active; khi Inactive không login, sau activate login được; không tạo profile mới hoặc xóa application/CV; hash/identity giữ nguyên.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-ACC; GBR-05; GAP-08
- **Source Code Reference:** [CandidateAccountController.activate](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/controller/CandidateAccountController.java:44>); [CandidateAccountController.deactivate](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/controller/CandidateAccountController.java:37>); [AccountListService.activateCandidate](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountListService.java:147>); [AccountListService.deactivateCandidate](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountListService.java:137>)
- **Existing Test Coverage (static review):** Có persistence account cơ bản; cần lifecycle + security integration.

<a id="it-c03"></a>

## IT-C03 Chặn activate Blocked và sai nhóm account

- **Test Case ID:** IT-C03
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT
- **Test Scenario:** Chặn activate Blocked và sai nhóm account
- **Preconditions:** Có Candidate Blocked, internal account và ID không tồn tại.
- **Test Data:** Admin POST Candidate activate với từng UserId.
- **Test Steps:** 1. Activate Candidate Blocked. 2. Gọi Candidate endpoint với internal UserId. 3. Dùng ID không tồn tại. 4. Đọc lại các account.
- **Expected Result:** Blocked không được mở bằng activate thông thường, phản hồi conflict 409; sai nhóm/không tồn tại 404; không đổi role/status/profile của internal account.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-ACC; OR-01/03; GAP-08
- **Source Code Reference:** [AccountListService.activateCandidate](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountListService.java:147>); [CandidateAccountController.activate](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/controller/CandidateAccountController.java:44>)
- **Existing Test Coverage (static review):** Cần web → scoped service → DB, không chỉ gọi service mock.

<a id="it-c04"></a>

## IT-C04 Tài khoản nội bộ không tạo/sửa identity Candidate qua form nội bộ

- **Test Case ID:** IT-C04
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** EP/DT
- **Test Scenario:** Tài khoản nội bộ không tạo/sửa identity Candidate qua form nội bộ
- **Preconditions:** Admin; có Candidate thật và internal account.
- **Test Data:** Create internal gửi role Candidate; edit URL nội bộ trỏ Candidate UserId; form hợp lệ còn lại.
- **Test Steps:** 1. Gửi create internal với Candidate role. 2. Thử sửa Candidate bằng internal path của AccountController. 3. Đối chiếu User/Candidate.
- **Expected Result:** Scope nội bộ từ chối Candidate; không tạo hoặc đổi Candidate thành internal và không tạo profile orphan. Candidate registration vẫn qua luồng OTP riêng.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-ACC screen 7; OR-01; GAP-08
- **Source Code Reference:** [AccountManagementService.createInternal](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountManagementService.java:54>); [AccountManagementService.updateInternal](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountManagementService.java:60>)
- **Existing Test Coverage (static review):** AccountManagementServiceTests có scope; cần controller + DB.

<a id="it-c05"></a>

## IT-C05 Writer User + Candidate rollback khi ghi profile thất bại

- **Test Case ID:** IT-C05
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** Writer User + Candidate rollback khi ghi profile thất bại
- **Preconditions:** DB test riêng; có cơ chế fault injection trong harness, chưa cài trong repo.
- **Test Data:** Username/email duy nhất; hash BCrypt hợp lệ; lỗi persistence xảy ra sau save User trước hoàn tất Candidate.
- **Test Steps:** 1. Ghi baseline. 2. Gọi createCandidateWithPasswordHash qua Spring proxy, gây lỗi ghi Candidate có kiểm soát. 3. Kết thúc transaction. 4. Đọc lại từ transaction khác.
- **Expected Result:** Không có User mồ côi hoặc Candidate dở dang; writer rollback toàn bộ. Không cố tình làm hỏng schema/shared DB để tạo lỗi.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** UC56; OR-07
- **Source Code Reference:** [AccountManagementService.createCandidateWithPasswordHash](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountManagementService.java:105>)
- **Existing Test Coverage (static review):** AuthenticationDatabaseTests/AccountManagementDatabaseTests chủ yếu happy path.

<a id="it-p01"></a>

## IT-P01 Candidate nộp CV thường và event sau commit

- **Test Case ID:** IT-P01
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** Candidate nộp CV thường và event sau commit
- **Preconditions:** C1 có profile, chưa nộp J1 Published chưa hết hạn; CvStorage test root riêng.
- **Test Data:** PDF thật hợp lệ; J1 không có đề xuất C1.
- **Test Steps:** 1. POST /jobs/{id}/apply multipart có session/CSRF. 2. Cho transaction nghiệp vụ commit. 3. Đọc Application và file từ transaction khác. 4. Quan sát event/AI trong thời gian chờ hữu hạn.
- **Expected Result:** Một Application Applied được ghi và CV tồn tại; event chỉ xử lý sau commit. Nếu AI thành công có thể thành AI_Screened; không yêu cầu trạng thái Applied cố định sau khi async hoàn tất. Candidate cũ được tái sử dụng.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-APPLY screen 36, UC54/55; OR-08
- **Source Code Reference:** [ApplicationSubmissionService.apply](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationSubmissionService.java:40>); [AiScreeningListener.onApplicationSubmitted](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/AiScreeningListener.java:23>); [AiScreeningService.screen](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/AiScreeningService.java:33>)
- **Existing Test Coverage (static review):** CareerFlowTests web mock + SubmissionServiceTests unit; cần commit thật.

<a id="it-p02"></a>

## IT-P02 CV trống, giả PDF và vượt giới hạn bị chặn trước ghi dữ liệu

- **Test Case ID:** IT-P02
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** EP/BVA
- **Test Scenario:** CV trống, giả PDF và vượt giới hạn bị chặn trước ghi dữ liệu
- **Preconditions:** C1/J1 đủ điều kiện nộp; mỗi biến thể fixture độc lập.
- **Test Data:** File rỗng; .docx; .pdf header sai; .PDF header %PDF-; PDF 5242879/5242880/5242881 bytes.
- **Test Steps:** 1. Submit từng file qua form multipart. 2. Kiểm lỗi cvFile. 3. Đếm Application và kiểm thư mục test. 4. Với file hợp lệ, rollback/dọn fixture theo plan.
- **Expected Result:** Nhận extension .pdf không phân biệt hoa thường, header %PDF- và <=5242880 bytes; file sai/rỗng/lớn hơn 5 MiB bị chặn, không lưu Application/file. File >10 MB còn qua giới hạn servlet, cần ghi riêng phản hồi thực tế, không ép expected cùng lỗi field 5 MB.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-APPLY; MSG40; GAP-06
- **Source Code Reference:** [CvFileValidator.validate](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/validator/CvFileValidator.java:26>); [ApplicationSubmissionService.apply](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationSubmissionService.java:40>)
- **Existing Test Coverage (static review):** CvFileValidatorTests unit đã có; kiểm thêm binding multipart/storage boundary.

<a id="it-p03"></a>

## IT-P03 Không nộp được vào tin Closed, Paused hoặc hết hạn

- **Test Case ID:** IT-P03
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** EP/DT
- **Test Scenario:** Không nộp được vào tin Closed, Paused hoặc hết hạn
- **Preconditions:** C1 Active; các job cùng điều kiện trừ status/deadline.
- **Test Data:** Published tương lai; Published quá hạn; Paused; Closed; ID không có.
- **Test Steps:** 1. Gửi form apply trực tiếp từng job. 2. Đối chiếu Application/file. 3. Kiểm tin hợp lệ vẫn nhận.
- **Expected Result:** Chỉ Published chưa hết hạn nhận CV; status sai/không có bị not-found, quá hạn bị lỗi nghiệp vụ; không sinh application/file ở các ca âm. Không giả định mốc bằng deadline qua browser có thể giữ đúng thời điểm nanosecond.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-APPLY screen 36; UC54
- **Source Code Reference:** [ApplicationSubmissionService.apply](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationSubmissionService.java:40>)
- **Existing Test Coverage (static review):** SubmissionServiceTests có unit; cần DB status + route thật.

<a id="it-p04"></a>

## IT-P04 Chống nộp trùng tuần tự và đồng thời

- **Test Case ID:** IT-P04
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT/ST
- **Test Scenario:** Chống nộp trùng tuần tự và đồng thời
- **Preconditions:** C1/J1 chưa có đơn chính thức; hai request/transaction độc lập dùng file riêng.
- **Test Data:** Hai lần apply cùng C1/J1; hai phiên request đồng thời.
- **Test Steps:** 1. Với fixture một, nộp thành công rồi nộp lại. 2. Với fixture khác, phát hai request apply cùng lúc. 3. Sau commit đếm application chính thức và file còn lại.
- **Expected Result:** Chỉ một đơn chính thức tồn tại cho lần nộp đang active; request còn lại không tạo thêm đơn hoặc file orphan. Đọc DB từ transaction mới, không chỉ đếm response 200. Không thêm cooldown 3–6 tháng vào expected vì code chưa có rule đó.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** UC54; OR-07; GAP-06
- **Source Code Reference:** [ApplicationSubmissionService.apply](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationSubmissionService.java:40>); [ApplicationRepository.existsActiveByCandidateAndJobPosting](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/repository/ApplicationRepository.java:287>)
- **Existing Test Coverage (static review):** Cần concurrency SQL Server thật; không suy ra từ test Mockito.

<a id="it-p05"></a>

## IT-P05 Rollback nộp đơn xóa file đã lưu và không kích hoạt AI

- **Test Case ID:** IT-P05
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** Rollback nộp đơn xóa file đã lưu và không kích hoạt AI
- **Preconditions:** Storage test root riêng, DB riêng; lỗi sau khi store file có thể tạo bằng harness.
- **Test Data:** PDF hợp lệ; lỗi persistence/transition có kiểm soát trước commit.
- **Test Steps:** 1. Ghi baseline file/DB/events. 2. Gọi apply trong transaction độc lập rồi gây rollback. 3. Chờ afterCompletion. 4. Kiểm DB, file, event listener.
- **Expected Result:** Không có Application mới, file của lượt lỗi bị xóa bởi synchronization, listener AFTER_COMMIT không chạy cho transaction rollback. Không tuyên bố SMTP/filesystem tự rollback chỉ do có @Transactional.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OR-07/08; OLD-APPLY
- **Source Code Reference:** [ApplicationSubmissionService.apply](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationSubmissionService.java:40>); [AiScreeningListener.onApplicationSubmitted](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/AiScreeningListener.java:23>)
- **Existing Test Coverage (static review):** LocalCvStorageTests và SubmissionServiceTests chưa thay thế rollback thật.

<a id="it-p06"></a>

## IT-P06 HR và HM review theo đúng vai trò, trạng thái và lịch sử

- **Test Case ID:** IT-P06
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT/ST
- **Test Scenario:** HR và HM review theo đúng vai trò, trạng thái và lịch sử
- **Preconditions:** Đơn Applied/AI_Screened phòng A; HR và HM_A Active.
- **Test Data:** HR Pass/Fail/Hold; HM Pass/Fail/Hold sau HR_Passed; fixture mới cho nhánh terminal.
- **Test Steps:** 1. POST /applications/{id}/review đúng vai trò. 2. Flush/clear đọc status và ApplicationReview. 3. Nếu HR Pass, login HM_A để review tiếp. 4. So timestamp/reviewerRole/comments.
- **Expected Result:** HR Pass → HR_Passed, HM Pass → HM_Passed; Fail → Rejected; Hold giữ nguyên. Mỗi lượt hợp lệ có review đúng actor, reviewerRole HR hoặc HiringManager. Hai ghi review/status phải đồng bộ transaction.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-PIPE screen 22–24, UC30/49; OR-06/07; GAP-07
- **Source Code Reference:** [ApplicationReviewService.review](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationReviewService.java:40>); [ApplicationAccess.canReview](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationAccess.java:72>); [ApplicationRepository.transition](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/repository/ApplicationRepository.java:113>)
- **Existing Test Coverage (static review):** ApplicationReviewServiceTests có 12 unit; cần full workflow DB.

<a id="it-p07"></a>

## IT-P07 AI lỗi không mất đơn, rescreen không kéo lùi trạng thái

- **Test Case ID:** IT-P07
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** EP/DT
- **Test Scenario:** AI lỗi không mất đơn, rescreen không kéo lùi trạng thái
- **Preconditions:** Đơn Applied có criteria; HR; AI client có success/failure điều khiển.
- **Test Data:** AI score thành công; client lỗi; hồ sơ đã HR_Passed; HR rescreen và HM rescreen.
- **Test Steps:** 1. Cho application commit và AI client lỗi. 2. Đọc đơn. 3. HR POST ai-screening lại thành công khi còn vòng HR. 4. Thử rescreen sau HR_Passed và bằng HM. 5. Kiểm AIScreeningResult.
- **Expected Result:** Lỗi nền không mất đơn; còn Applied nếu chưa được xử lý khác. Rescreen hợp lệ lưu lịch sử điểm và chuyển Applied → AI_Screened; không auto Reject theo điểm. Sai role/stage bị chặn; không kéo HR_Passed về AI_Screened. Đây là AI client/stub của code hiện tại, không chứng minh dịch vụ AI production.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OR-08; UC55; OLD-PIPE
- **Source Code Reference:** [AiScreeningService.screen](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/AiScreeningService.java:33>); [AiScreeningListener.onApplicationSubmitted](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/AiScreeningListener.java:23>); [ApplicationReviewService.rescreen](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationReviewService.java:84>)
- **Existing Test Coverage (static review):** AiScreeningServiceTests/ListenerTests unit; cần commit/listener boundary.

<a id="it-p08"></a>

## IT-P08 Rút đơn chỉ được ở vòng đầu, đúng owner và không xóa dữ liệu

- **Test Case ID:** IT-P08
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST/DT
- **Test Scenario:** Rút đơn chỉ được ở vòng đầu, đúng owner và không xóa dữ liệu
- **Preconditions:** C1/C2; đơn Applied, AI_Screened, HR_Passed; lịch sử riêng từng fixture.
- **Test Data:** C1 rút đơn mình; C1 dùng ID C2; rút lại lần hai; rút đơn đã HR_Passed.
- **Test Steps:** 1. POST /portal/applications/{id}/withdraw. 2. Đọc DB/audit và My Applications. 3. Lặp request ở các biến thể cấm.
- **Expected Result:** Applied/AI_Screened của mình chuyển Rejected kèm audit withdrawal, UI phân biệt đã rút với bị loại; file/profile giữ nguyên. Khác owner không tìm thấy; sau vòng HR hoặc replay bị chặn và không ghi audit trùng thành công.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-MY screen 38, UC57/58; GAP-07
- **Source Code Reference:** [MyApplicationService.withdraw](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/MyApplicationService.java:47>); [MyApplicationService.list](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/MyApplicationService.java:33>); [ApplicationRepository.recordWithdrawal](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/repository/ApplicationRepository.java:206>)
- **Existing Test Coverage (static review):** MyApplicationServiceTests/WebTests/QueryTests cover từng tầng; cần route+DB+audi­t nhất quán.

<a id="it-p09"></a>

## IT-P09 Review stale hoặc song song không tạo quyết định tiến bước hai lần

- **Test Case ID:** IT-P09
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST/DT
- **Test Scenario:** Review stale hoặc song song không tạo quyết định tiến bước hai lần
- **Preconditions:** Một đơn ở đúng vòng HR; hai transaction riêng; HR_A/HR_B hợp lệ.
- **Test Data:** Pass và Fail cùng application; request sau đã cũ; riêng Hold được test ở P06.
- **Test Steps:** 1. Hai request đọc cùng trạng thái ban đầu. 2. Gửi Pass/Fail cạnh tranh. 3. Hoàn tất transaction. 4. Đọc status và số review mới từ connection khác.
- **Expected Result:** Conditional transition chỉ cho một quyết định tiến bước thành công; lượt thua báo hồ sơ đã xử lý và review của transaction thất bại rollback. Không có hai kết quả terminal cùng thành công, không có orphan review.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OR-06/07; OLD-PIPE
- **Source Code Reference:** [ApplicationReviewService.review](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationReviewService.java:40>); [ApplicationRepository.transition](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/repository/ApplicationRepository.java:113>)
- **Existing Test Coverage (static review):** PipelineQueryTests có conditional update; chưa đủ proof concurrent service transaction.

<a id="it-t01"></a>

## IT-T01 Bookmark Candidate và ghi chú dùng chung từ Application

- **Test Case ID:** IT-T01
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** Bookmark Candidate và ghi chú dùng chung từ Application
- **Preconditions:** HR/HM xem được application nguồn; Candidate chưa potential.
- **Test Data:** Note tiếng Việt hợp lệ; application có CV và history.
- **Test Steps:** 1. POST bookmark với note. 2. Flush/clear kiểm Candidate, TalentPoolNote, AuditLog. 3. Login role còn lại đọc bookmark và Talent Pool. 4. So snapshot application/CV.
- **Expected Result:** Candidate.IsPotential true; ghi note đúng author; actor khác đọc được cùng lịch sử. Không đổi application status/CV/Interview/Offer, không tạo Candidate hoặc Application mới; audit đánh dấu lần đầu đúng một lần.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-PIPE; OLD-TALENT; GBR-02; OR-07
- **Source Code Reference:** [TalentPoolService.saveBookmark](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:139>); [TalentPoolService.markPotential](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:103>); [TalentPoolService.bookmark](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:114>)
- **Existing Test Coverage (static review):** TalentPoolBookmarkTests unit; InterviewPotentialIntegrationTests cover một đường DB.

<a id="it-t02"></a>

## IT-T02 Bookmark rỗng, note dài và bỏ lưu giữ lịch sử

- **Test Case ID:** IT-T02
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** BVA/ST
- **Test Scenario:** Bookmark rỗng, note dài và bỏ lưu giữ lịch sử
- **Preconditions:** Candidate có bookmark và note gốc; HR/HM đúng scope.
- **Test Data:** content rỗng; 999/1000/1001 ký tự; remove hai lần.
- **Test Steps:** 1. Lưu bookmark không note. 2. Lưu từng note với fixture độc lập. 3. Bỏ lưu. 4. Đọc notes từ application và kiểm flags/snapshots.
- **Expected Result:** Note rỗng chỉ lưu cờ, không thêm note rỗng. Tối đa 1000 ký tự được nhận; 1001 bị chặn và không thay đổi cờ/note. Bỏ lưu chỉ đưa potential=false, giữ CV/đơn/note; gọi bỏ lưu lại không nhân audit unmark.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-TALENT; GBR-02; NEW-TALENT
- **Source Code Reference:** [TalentPoolService.saveBookmark](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:139>); [TalentPoolService.removeBookmark](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:125>)
- **Existing Test Coverage (static review):** Bookmark unit và InterviewPotentialIntegrationTests có phần coverage.

<a id="it-t03"></a>

## IT-T03 Xem CV nguồn phải khớp Candidate và file còn đọc được

- **Test Case ID:** IT-T03
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** EP/DT
- **Test Scenario:** Xem CV nguồn phải khớp Candidate và file còn đọc được
- **Preconditions:** HR/HM; C1 potential, C2 khác; có source CV local/sample.
- **Test Data:** CandidateId C1 + appId C2; file local mất; PDF sample còn; source không có URL.
- **Test Steps:** 1. GET /talent-pool/{id}/cv/{appId} từng biến thể. 2. Đọc response/header/location và kiểm file. 3. So DB không đổi.
- **Expected Result:** CV thuộc Candidate khác bị cấm; thiếu/mất file báo lỗi, không preview nhầm. Local file được trả resource hoặc sample redirect theo implementation. Không suy diễn mọi URL http/https đều được kiểm tồn tại từ server vì requireCv chỉ kiểm local/classpath.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-TALENT; OR-04; GAP-05
- **Source Code Reference:** [TalentPoolService.cv](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:425>)
- **Existing Test Coverage (static review):** TalentPoolSelectionReviewTests/BookmarkTests có rule; cần file + request thật.

<a id="it-t04"></a>

## IT-T04 Selection review read-only và giới hạn số CV

- **Test Case ID:** IT-T04
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** EP/BVA
- **Test Scenario:** Selection review read-only và giới hạn số CV
- **Preconditions:** Có nhiều potential Candidate và CV nguồn chính thức ở tin khác.
- **Test Data:** 0/1/20/21 source IDs; null/0/ID âm; hai CV cùng Candidate; CV cùng tin đích; Candidate đã unmark; CV không có.
- **Test Steps:** 1. POST /talent-pool/jobs/{id}/selection-review bằng JSON có CSRF. 2. Kiểm item errors/validation. 3. Đếm mọi bảng trước/sau. 4. Với selection hợp lệ chuyển sang xác nhận ở case T05.
- **Expected Result:** DTO nhận 1–20 ID dương không null; rule nguồn sai báo lý do tương ứng; preflight không tạo proposal/review/note/audit. Không coi selection-review thành công là đã lưu gợi ý.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-TALENT; NEW-TALENT
- **Source Code Reference:** [TalentPoolService.reviewSelection](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:171>); [TalentPoolController.reviewSelection](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/controller/TalentPoolController.java:96>)
- **Existing Test Coverage (static review):** TalentPoolSelectionReviewTests + web slice đã có; cần assert read-only DB thật.

<a id="it-t05"></a>

## IT-T05 HM chọn kho tiềm năng tạo đề xuất ở tin đích

- **Test Case ID:** IT-T05
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** HM chọn kho tiềm năng tạo đề xuất ở tin đích
- **Preconditions:** HM_A; J2 Published phòng A; C1 potential, source chính thức ở J1 khác, chưa có đơn/draft ở J2.
- **Test Data:** Một sourceApplicationId hợp lệ; snapshot source Candidate/App/CV/count.
- **Test Steps:** 1. POST /talent-pool/jobs/{id}/suggest. 2. Flush/clear đọc proposal/audit. 3. Kiểm suggestions và các danh sách chính thức/dashboard/Candidate portal.
- **Expected Result:** Tạo đúng một Application HM_Suggested ở J2, dùng lại Candidate và URL CV nguồn; audit TalentPoolSuggested liên kết source. Source giữ nguyên. Proposal chưa được tính là đơn chính thức, chưa đủ điều kiện Interview.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-TALENT; NEW-TALENT; OR-07
- **Source Code Reference:** [TalentPoolService.suggest](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:212>); [ApplicationRepository.findSuggestionsByJobPostingId](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/repository/ApplicationRepository.java:298>); [DashboardMetricsRepository.applications](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/DashboardMetricsRepository.java:83>)
- **Existing Test Coverage (static review):** TalentPoolFlowIntegrationTests.realHmSubmissionHrConsentAndInterviewEligibilityPreserveOriginalCandidateAndCv đã có đường chính.

<a id="it-t06"></a>

## IT-T06 HR xác nhận consent dùng CV cũ kích hoạt cùng proposal

- **Test Case ID:** IT-T06
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT/ST
- **Test Scenario:** HR xác nhận consent dùng CV cũ kích hoạt cùng proposal
- **Preconditions:** Có proposal do HM tạo, hoặc HR recommend đã được HM Pass; job còn mở; CV còn tồn tại.
- **Test Data:** consent=false rồi true trên fixture hợp lệ; ApplicationId proposal trước/sau.
- **Test Steps:** 1. HR POST /talent-pool/applications/{id}/confirm thiếu đồng ý. 2. Kiểm chưa đổi. 3. Gửi consent=true. 4. Đọc status, audit, formal queries và interview eligibility.
- **Expected Result:** Không consent bị chặn. Có consent và đủ điều kiện: cùng ApplicationId chuyển HM_Passed, có TalentPoolActivated; không thêm Candidate/application mới. Xuất hiện trong hồ sơ chính thức và đủ requireScheduleable/findEligibleForInterview; không tạo AIScreeningResult mới cho nhánh dùng CV cũ.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** NEW-TALENT; OLD-PIPE; OR-06/07; GAP-04
- **Source Code Reference:** [TalentPoolService.confirm](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:373>); [ApplicationRepository.promoteSuggestionToHmPassed](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/repository/ApplicationRepository.java:338>); [ApplicationStatusService.requireScheduleable](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationStatusService.java:45>)
- **Existing Test Coverage (static review):** TalentPoolFlowIntegrationTests có HR consent; thêm biến thể false để không nhân happy path.

<a id="it-t07"></a>

## IT-T07 HR kết thúc liên hệ không đồng ý dùng CV cũ

- **Test Case ID:** IT-T07
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** HR kết thúc liên hệ không đồng ý dùng CV cũ
- **Preconditions:** Proposal đã qua HM, chưa active; job và nguồn tồn tại.
- **Test Data:** HR decline; snapshot profile/CV/note/source và formal count.
- **Test Steps:** 1. HR POST /talent-pool/applications/{id}/decline. 2. Refresh suggestions/list/dashboard/portal. 3. Gọi lại confirm/decline.
- **Expected Result:** Draft chuyển Rejected có TalentPoolCancelled; không lẫn đơn chính thức, không tính KPI ứng tuyển, không lên lịch được; Candidate/CV/notes/source giữ nguyên. Replay không kích hoạt lại draft.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** NEW-TALENT; GAP-04; OR-06
- **Source Code Reference:** [TalentPoolService.decline](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:391>); [ApplicationRepository.findPipelineIds](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/repository/ApplicationRepository.java:131>)
- **Existing Test Coverage (static review):** TalentPoolFlowIntegrationTests.realHrDeclineRetainsPrivateDraftButExcludesItFromFormalQueriesAndCounts đã có.

<a id="it-t08"></a>

## IT-T08 HR gợi ý cho HM chưa được xem là consent hoặc formal application

- **Test Case ID:** IT-T08
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** HR gợi ý cho HM chưa được xem là consent hoặc formal application
- **Preconditions:** HR; J2 có HM hợp lệ; source tiềm năng hợp lệ.
- **Test Data:** POST recommend một C1, chưa có review HM.
- **Test Steps:** 1. HR POST /talent-pool/jobs/{id}/recommend. 2. Đọc Application/Review/Audit. 3. Xem danh sách Gợi ý đã gửi/Gợi ý từ HR. 4. Kiểm suggestions chờ liên hệ và formal counts.
- **Expected Result:** Application HM_Suggested + HR Hold + TalentPoolHrRecommended; xuất hiện hàng đợi HM, chưa vào hàng đợi HR xác nhận liên hệ, không vào formal pipeline/portal/Interview/KPI. Không tạo bản sao Candidate.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** NEW-TALENT; GAP-04; OR-07
- **Source Code Reference:** [TalentPoolService.recommend](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:219>); [TalentPoolService.recommendations](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:290>); [ApplicationRepository.countPendingHrRecommendation](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/repository/ApplicationRepository.java:312>)
- **Existing Test Coverage (static review):** TalentPoolFlowIntegrationTests.realHrRecommendationHmReviewAllCvsAndNotesThenConsentUseOneDraft cover.

<a id="it-t09"></a>

## IT-T09 HM Pass gợi ý HR chỉ cho phép HR liên hệ, chưa tự active

- **Test Case ID:** IT-T09
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT/ST
- **Test Scenario:** HM Pass gợi ý HR chỉ cho phép HR liên hệ, chưa tự active
- **Preconditions:** HR draft pending ở J2 phòng A; HM_A đúng scope.
- **Test Data:** decision=Pass; có history CV/notes; snapshot application ID.
- **Test Steps:** 1. HM đọc recommendation/detail/CV. 2. POST /talent-pool/recommendations/{id}/decision Pass. 3. Đọc review/audit và các danh sách. 4. Thử schedule trước HR confirm rồi làm T06.
- **Expected Result:** Có HiringManager Pass; application vẫn HM_Suggested; pending HR recommendation về 0, chuyển sang danh sách chờ HR liên hệ. Trước consent vẫn chưa formal/Interview; T06 mới active cùng bản ghi.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** NEW-TALENT; GAP-04
- **Source Code Reference:** [TalentPoolService.decideRecommendation](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:334>); [TalentPoolService.suggestions](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:160>); [ApplicationStatusService.requireScheduleable](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationStatusService.java:45>)
- **Existing Test Coverage (static review):** Đã có integration đường recommend → HM Pass → consent; không viết thêm một happy path y hệt nếu chỉ chạy lại.

<a id="it-t10"></a>

## IT-T10 HM Fail và HM khác phòng không được xử lý gợi ý

- **Test Case ID:** IT-T10
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT/ST
- **Test Scenario:** HM Fail và HM khác phòng không được xử lý gợi ý
- **Preconditions:** HR pending draft J2 phòng A; HM_A/HM_B.
- **Test Data:** HM_B Pass; HM_A Fail; fixture riêng hoặc thử HM_B trước.
- **Test Steps:** 1. HM_B truy cập recommendation/detail và POST decision. 2. Kiểm không ghi. 3. HM_A Fail. 4. Refresh list/detail và kiểm formal queries.
- **Expected Result:** HM_B bị cấm theo job đích. HM_A Fail làm draft Rejected + HiringManager Fail + cancellation audit; bị loại khỏi formal pipeline/count/Interview, nguồn và note giữ nguyên. Không đưa Reject proposal thành từ chối đơn chính thức.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** NEW-TALENT; OR-01/06; GAP-04/05
- **Source Code Reference:** [TalentPoolService.decideRecommendation](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:334>); [TalentPoolService.recommendationDetail](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:88>)
- **Existing Test Coverage (static review):** TalentPoolFlowIntegrationTests.realHmRejectionStaysOutOfProposalsAndFormalApplicationsAfterRefresh cover nhánh Fail.

<a id="it-t11"></a>

## IT-T11 Không confirm/decline trước HM, sai role hoặc ở job hết hạn

- **Test Case ID:** IT-T11
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT
- **Test Scenario:** Không confirm/decline trước HM, sai role hoặc ở job hết hạn
- **Preconditions:** HR recommendation còn pending; proposal đã HM accept riêng; HM/HR có session.
- **Test Data:** HR confirm pending; HR decline pending; HM confirm; proposal tại job Closed/quá hạn với confirm.
- **Test Steps:** 1. Gửi từng request với CSRF đúng. 2. Đọc draft/review/audit. 3. Kiểm không xuất hiện trong formal queries.
- **Expected Result:** Pending HR recommendation chưa có HM decision không confirm/decline được; HM không có quyền confirm; confirm job không còn mở bị chặn. Không tự chuyển HM_Passed chỉ vì client gửi consent=true.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** NEW-TALENT; OR-01/06; GAP-04
- **Source Code Reference:** [TalentPoolService.confirm](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:373>); [TalentPoolService.decline](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:391>); [SecurityConfig.filterChain](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java:50>)
- **Existing Test Coverage (static review):** Nhiều unit/web negatives đã có; cần đủ guard xuyên DB.

<a id="it-t12"></a>

## IT-T12 Nộp CV mới cho đề xuất đã được HM duyệt đi thẳng HM_Passed

- **Test Case ID:** IT-T12
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT/ST
- **Test Scenario:** Nộp CV mới cho đề xuất đã được HM duyệt đi thẳng HM_Passed
- **Preconditions:** C1 có proposal cùng J2 do HM suggest, hoặc HR recommend đã HM Pass; J2 mở.
- **Test Data:** origin=HM; origin=HR đã accepted; PDF mới khác CV nguồn, cùng Candidate/J2.
- **Test Steps:** 1. C1 login và POST apply CV mới vào J2. 2. Flush/clear đọc ApplicationId/status/CV/audit. 3. So source và số Candidate/Application. 4. Quan sát event/Interview eligibility.
- **Expected Result:** Tái dùng đúng proposal ID, đổi CV của proposal thành CV mới, status HM_Passed; không publish ApplicationSubmittedEvent/không chấm AI lại ở nhánh này; source CV/đơn gốc giữ nguyên; đủ điều kiện HR xếp lịch.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** NEW-TALENT; NEW-CV; GAP-04; UC54
- **Source Code Reference:** [ApplicationSubmissionService.apply](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationSubmissionService.java:40>); [ApplicationRepository.submitSuggestionWithNewCv](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/repository/ApplicationRepository.java:352>); [ApplicationStatusService.requireScheduleable](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationStatusService.java:45>)
- **Existing Test Coverage (static review):** TalentPoolFlowIntegrationTests.candidateNewCvUsesMatchingApprovedProposalToEnterInterview parameterized đã có.

<a id="it-t13"></a>

## IT-T13 CV mới khi HR recommendation còn chờ HM vẫn phải sàng lọc

- **Test Case ID:** IT-T13
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT/ST
- **Test Scenario:** CV mới khi HR recommendation còn chờ HM vẫn phải sàng lọc
- **Preconditions:** HR recommend cho C1/J2 nhưng HM chưa decision; J2 mở.
- **Test Data:** PDF mới đúng C1/J2; pending count=1 trước submit.
- **Test Steps:** 1. Candidate nộp CV mới. 2. Kiểm ID/status/file. 3. Commit thật và quan sát event/AI. 4. So source data.
- **Expected Result:** Dùng cùng draft ID, thành Applied rồi có thể AI_Screened sau async, publish event; không bỏ qua thành HM_Passed. Proposal trở thành đơn chính thức do Candidate tự nộp, nguồn giữ nguyên. Không ép Applied cố định sau khi AI hoàn tất.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** NEW-CV; NEW-TALENT; GAP-04; UC54/55
- **Source Code Reference:** [ApplicationSubmissionService.apply](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationSubmissionService.java:40>); [ApplicationRepository.countPendingHrRecommendation](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/repository/ApplicationRepository.java:312>)
- **Existing Test Coverage (static review):** Có biến thể HR pending trong TalentPoolFlowIntegrationTests; cần kiểm AFTER_COMMIT riêng.

<a id="it-t14"></a>

## IT-T14 Chỉ là potential hoặc proposal ở tin khác không được bỏ qua sàng lọc

- **Test Case ID:** IT-T14
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT
- **Test Scenario:** Chỉ là potential hoặc proposal ở tin khác không được bỏ qua sàng lọc
- **Preconditions:** C1 potential; có đề xuất ở J2; J3 Published khác chưa có đề xuất.
- **Test Data:** C1 apply J3; PDF hợp lệ; optional C2 không potential làm control.
- **Test Steps:** 1. Candidate apply vào J3. 2. Đọc đơn mới và event. 3. So proposal J2 và source trước/sau.
- **Expected Result:** J3 là đơn thường Applied rồi AI_Screened nếu AI thành công, không HM_Passed; proposal J2 không đổi. IsPotential không phải quyền bypass mọi job.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** NEW-CV; GAP-04; UC54/55
- **Source Code Reference:** [ApplicationSubmissionService.apply](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationSubmissionService.java:40>)
- **Existing Test Coverage (static review):** ApplicationSubmissionServiceTests có guard; bổ sung DB exact job match.

<a id="it-t15"></a>

## IT-T15 HR thu hồi gợi ý chưa HM phản hồi và gửi lại

- **Test Case ID:** IT-T15
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST/DT
- **Test Scenario:** HR thu hồi gợi ý chưa HM phản hồi và gửi lại
- **Preconditions:** HR pending recommendation J2; source/profile/note còn.
- **Test Data:** Withdraw đúng job; sai jobId; attempt sau HM Pass ở fixture khác.
- **Test Steps:** 1. HR POST /talent-pool/jobs/{jobId}/recommendations/{id}/withdraw. 2. Refresh HR/HM list và formal counts. 3. HR recommend lại C1/J2. 4. Thử withdraw gợi ý đã HM Pass.
- **Expected Result:** Pending draft thành Rejected có TalentPoolCancelled và TalentPoolHrWithdrawn, ẩn khỏi danh sách và formal queries; giữ nguồn/CV/note. Gửi lại tạo draft ID mới. Sai job bị not-found; đã HM phản hồi không thu hồi được bằng đường pending này.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** NEW-TALENT; OR-06/07
- **Source Code Reference:** [TalentPoolService.withdrawRecommendation](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:358>); [TalentPoolService.recommend](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:219>)
- **Existing Test Coverage (static review):** Hai integration withdrawal hiện hữu đã cover chính; dùng lại trước khi bổ sung.

<a id="it-t16"></a>

## IT-T16 Chống gửi trùng, batch không ghi một phần và revalidate khi confirm selection

- **Test Case ID:** IT-T16
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT/ST
- **Test Scenario:** Chống gửi trùng, batch không ghi một phần và revalidate khi confirm selection
- **Preconditions:** HM/HR đúng quyền; 2 nguồn hợp lệ ban đầu, J2 mở.
- **Test Data:** Hai CV cùng Candidate; batch có một nguồn mất file; candidate unmark sau preflight; hai suggest cùng C1/J2 chạy đồng thời.
- **Test Steps:** 1. Review selection rồi thay đổi một điều kiện trong transaction khác. 2. Submit suggest/recommend. 3. Đếm application/review/audit. 4. Hai transaction độc lập gửi cạnh tranh cùng target.
- **Expected Result:** Service kiểm tra lại dưới posting lock; không ghi batch một phần khi nguồn không hợp lệ; không tạo hai pending draft hoặc draft song song với đơn active. Preflight trước đó hợp lệ không bảo đảm submit sau cũng hợp lệ.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** NEW-TALENT; OR-07
- **Source Code Reference:** [TalentPoolService.reviewSelection](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:171>); [TalentPoolService.suggest](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:212>); [TalentPoolService.recommend](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:219>)
- **Existing Test Coverage (static review):** Unit selection/suggestion có guard; race/transaction batch là coverage cần ưu tiên.

<a id="it-t17"></a>

## IT-T17 Bookmark từ Interview Passed/Failed không làm đổi kết quả tuyển dụng

- **Test Case ID:** IT-T17
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** Bookmark từ Interview Passed/Failed không làm đổi kết quả tuyển dụng
- **Preconditions:** Có Interview final result Passed và Failed; HR/HM được xem result; snapshot Interview/Offer/Application.
- **Test Data:** Note từ HM, thêm note HR, bỏ lưu; case độc lập cho Passed/Failed.
- **Test Steps:** 1. Mở /interviews/{id}/result. 2. Lưu potential bằng bookmark API của application. 3. Role khác đọc/thêm note. 4. Remove bookmark. 5. So snapshot các bảng và CV.
- **Expected Result:** Cùng Candidate flag/note dùng chung với Application; không đổi final result, score, Offer hoặc application status; Passed/Failed đều có thể bookmark theo quyền hiện có. Bỏ lưu giữ history.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** NEW-TALENT; OLD-INTERVIEW screen 28; OR-07
- **Source Code Reference:** [TalentPoolService.saveBookmark](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:139>); [TalentPoolService.removeBookmark](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:125>); [TalentPoolService.bookmark](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:114>)
- **Existing Test Coverage (static review):** InterviewPotentialIntegrationTests hai methods đã cover DB; cần manual browser để xác minh popup thật.

<a id="it-e01"></a>

## IT-E01 Đóng đợt có open offer phải bị chặn

- **Test Case ID:** IT-E01
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT/ST
- **Test Scenario:** Đóng đợt có open offer phải bị chặn
- **Preconditions:** HR; job Published/Paused; có offer thuộc tập open trong query hasOpenOffers.
- **Test Data:** Offer status lấy đúng từ TalentPoolAuditRepository.hasOpenOffers, không tự đặt status.
- **Test Steps:** 1. Chụp job/app/interview/audit. 2. HR POST close job. 3. Đọc lại từ transaction khác.
- **Expected Result:** Bị lỗi còn Offer chưa kết thúc; job/app/interview và audit chuyển trạng thái không bị ghi dở. Không coi gửi mail lỗi là lý do của chặn này vì business chưa hoàn tất.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** NEW-CLOSURE; OR-06/07
- **Source Code Reference:** [TalentPoolService.closeJob](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:402>); [TalentPoolAuditRepository.hasOpenOffers](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/repository/TalentPoolAuditRepository.java:32>)
- **Existing Test Coverage (static review):** Cần integration close job thật, không chỉ web error mock.

<a id="it-e02"></a>

## IT-E02 Đóng job hủy draft và dừng các đơn đang chờ có audit phân biệt

- **Test Case ID:** IT-E02
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** Đóng job hủy draft và dừng các đơn đang chờ có audit phân biệt
- **Preconditions:** HR; job Published/Paused, không open offers; có draft HM_Suggested và hồ sơ pending/lịch chưa diễn ra đúng query.
- **Test Data:** Snapshot tất cả ApplicationId, interview status, note/CV và Candidate; bộ fixture biết trước.
- **Test Steps:** 1. Close job. 2. Flush/clear đọc từng application và schedule. 3. Xem lại pipeline/detail và đề xuất. 4. Kiểm AuditLog.
- **Expected Result:** Job Closed; draft chuyển Rejected + TalentPoolCancelled, không thành formal rejection. Pending formal chuyển Rejected + PostingClosed; lịch phù hợp được cancel và có InterviewCancelledOnClose khi thực sự hủy. Hồ sơ/CV/note được giữ. UI phân biệt đóng đợt với CV không đạt theo closure fact.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** NEW-CLOSURE; OR-06/07; GAP-04
- **Source Code Reference:** [TalentPoolService.closeJob](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:402>); [ApplicationPipelineService.closedByPosting](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationPipelineService.java:156>); [ApplicationProgressResolver.resolve](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationProgressResolver.java:27>)
- **Existing Test Coverage (static review):** Code hiện tại đã có PostingClosed; cần kiểm phân biệt với Rejected vì lý do CV.

<a id="it-e03"></a>

## IT-E03 Job closure không loại hồ sơ đã nhận offer, gọi lại không xử lý đôi

- **Test Case ID:** IT-E03
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT/ST
- **Test Scenario:** Job closure không loại hồ sơ đã nhận offer, gọi lại không xử lý đôi
- **Preconditions:** HR; job không còn open offers, có Accepted offer và pending formal khác; fixture phù hợp native query.
- **Test Data:** App Accepted offer; App pending; close cùng job hai lần.
- **Test Steps:** 1. Chụp trạng thái hai app và audit. 2. Close job lần đầu. 3. Close lần hai. 4. So application/cancel audit/notifications.
- **Expected Result:** Application có Accepted offer được giữ, pending khác được xử lý theo closure rule. Job đã Closed khiến business action lần hai no-op; không nhân transition/audit closure. Việc gửi/resend mail kiểm tách ở E04, không suy ra tự động không bao giờ gửi lại.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** NEW-CLOSURE; OR-06/07
- **Source Code Reference:** [TalentPoolService.closeJob](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/TalentPoolService.java:402>); [TalentPoolAuditRepository.hasAcceptedOffer](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/repository/TalentPoolAuditRepository.java:37>)
- **Existing Test Coverage (static review):** Cần fixture riêng cho protected offer và idempotency.

<a id="it-e04"></a>

## IT-E04 Lỗi SMTP sau close không rollback business hoặc gửi nhầm người

- **Test Case ID:** IT-E04
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** Lỗi SMTP sau close không rollback business hoặc gửi nhầm người
- **Preconditions:** Job closure fixture ở E02; JavaMailSender test có success/failure; business transaction commit thật.
- **Test Data:** SMTP failure rồi retry một Application đã có PostingClosed; một application không có closure audit làm control.
- **Test Steps:** 1. Gọi close qua controller cho commit. 2. Sender trả lỗi. 3. Kiểm job/app/audit và thông báo. 4. HR gọi resend-email cho app hợp lệ. 5. Thử resend app không bị closure.
- **Expected Result:** Business vẫn Closed/Rejected đúng rule; UI báo thao tác đã lưu nhưng mail lỗi. Audit mail failed/sent tách biệt; retry không lặp close/transition, chỉ gửi đúng recipient từ app bị closure. App không có PostingClosed không được gửi closure email.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** MSG39; OR-07; NEW-CLOSURE
- **Source Code Reference:** [TalentPoolController.close](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/controller/TalentPoolController.java:290>); [RecruitmentEmailService.sendJobClosure](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/RecruitmentEmailService.java:31>); [RecruitmentEmailService.resendClosure](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/RecruitmentEmailService.java:41>)
- **Existing Test Coverage (static review):** Web test savedClosureShowsMailFailureWithoutRepeatingBusinessAction chỉ mock; cần commit + DB + sender integration.

<a id="it-e05"></a>

## IT-E05 Invite/resend Interview dùng dữ liệu lịch hiện tại và quyền đúng

- **Test Case ID:** IT-E05
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** EP/DT
- **Test Scenario:** Invite/resend Interview dùng dữ liệu lịch hiện tại và quyền đúng
- **Preconditions:** HR; lịch Scheduled/Rescheduled thuộc formal application; sender test.
- **Test Data:** Lịch hợp lệ; app HM_Suggested/Rejected; lịch đã không còn Scheduled/Rescheduled; role HM gửi trực tiếp URL resend.
- **Test Steps:** 1. Gọi sendInterview từ luồng lịch hoặc resend endpoint hiện có. 2. Kiểm recipient/time/link và audit. 3. Thử biến thể lịch/app sai và role HM.
- **Expected Result:** Chỉ lịch còn hợp lệ và app không HM_Suggested/Rejected được gửi; body theo DB current. HM bị chặn ở route resend; gửi mail chạy sau business transaction, không tạo hoặc đổi kết quả Interview. Không dùng success của mock mail để khẳng định hộp thư thật đã nhận.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-INTERVIEW screen 26; MSG39; NEW-CLOSURE
- **Source Code Reference:** [RecruitmentEmailService.sendInterview](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/RecruitmentEmailService.java:42>); [RecruitmentEmailService.resendInterview](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/RecruitmentEmailService.java:47>); [TalentPoolController.resendInterview](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/controller/TalentPoolController.java:319>)
- **Existing Test Coverage (static review):** Khoảng trống mail DB/after-commit; scope là điểm nối của Linh với Interview.

<a id="it-r01"></a>

## IT-R01 HM submit requisition và Director khác người tạo approve

- **Test Case ID:** IT-R01
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** HM submit requisition và Director khác người tạo approve
- **Preconditions:** HM_A có department; Director_D khác owner; form submit đầy đủ theo validator.
- **Test Data:** Title QA-Linh-<suffix>; criteria hợp lệ; action=submit.
- **Test Steps:** 1. Tạo qua RequisitionServiceImpl hoặc controller thật. 2. Đọc Pending_Director, criteria, Submitted event/audit. 3. Login Director khác owner để approve. 4. Flush/clear đọc approval/event/audit/HR list.
- **Expected Result:** Requisition Pending_Director → Approved; có RequisitionApproval đúng director/status, workflow event và audit nhất quán; HR xem được Approved. Không dùng status tên Pending Approval của SRS cũ làm DB expected.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-REQ screen 14–17, UC22/32/33; OR-07; GAP-02
- **Source Code Reference:** [RequisitionServiceImpl.createRequisition](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/requisition/service/RequisitionServiceImpl.java:229>); [RequisitionServiceImpl.decide](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/requisition/service/RequisitionServiceImpl.java:435>); [RequisitionAccess.canDecide](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/requisition/service/RequisitionAccess.java:94>)
- **Existing Test Coverage (static review):** RequisitionIntegrationTest.it04_directorApprove_createsApprovalAndEvent đã có.

<a id="it-r02"></a>

## IT-R02 Reject requisition bắt buộc lý do và lưu feedback

- **Test Case ID:** IT-R02
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** BVA/ST
- **Test Scenario:** Reject requisition bắt buộc lý do và lưu feedback
- **Preconditions:** Pending_Director, Director khác owner; fixture riêng.
- **Test Data:** Comment rỗng/space; 999/1000/1001 ký tự; một lý do tiếng Việt hợp lệ.
- **Test Steps:** 1. Reject không lý do hoặc quá dài. 2. So DB chưa đổi. 3. Reject có lý do hợp lệ. 4. Đọc status/Approval/Event/Audit và notification cho owner.
- **Expected Result:** Rỗng/quá 1000 bị chặn; hợp lệ chuyển Rejected, feedback được lưu đúng Unicode và notification hướng đến owner; không ghi một phần approval trước lỗi validation.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-REQ UC34/35; OR-07
- **Source Code Reference:** [RequisitionServiceImpl.decide](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/requisition/service/RequisitionServiceImpl.java:435>)
- **Existing Test Coverage (static review):** RequisitionIntegrationTest.it05_directorReject_recordsFeedback có happy path; bổ sung biên.

<a id="it-r03"></a>

## IT-R03 Chặn tự duyệt, sai role và quyết định khi không pending

- **Test Case ID:** IT-R03
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT
- **Test Scenario:** Chặn tự duyệt, sai role và quyết định khi không pending
- **Preconditions:** Director_Owner tự tạo requisition; HR/HM; request Draft/Approved/Rejected.
- **Test Data:** Director owner approve; HR/HM approve; Director khác approve request không Pending_Director.
- **Test Steps:** 1. Gửi từng decide request với CSRF đúng. 2. Đối chiếu status, Approval, Event và Audit.
- **Expected Result:** Chỉ Director khác owner với Pending_Director mới decide được. Các ca cấm không tạo approval/event chuyển trạng thái; không vì Admin có view/create mà mặc nhiên được approve.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OLD-REQ UC32–35; OR-01/06
- **Source Code Reference:** [RequisitionAccess.canDecide](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/requisition/service/RequisitionAccess.java:94>); [RequisitionServiceImpl.decide](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/requisition/service/RequisitionServiceImpl.java:435>)
- **Existing Test Coverage (static review):** RequisitionAccessTest unit có rule; cần route/service/DB thật.

<a id="it-r04"></a>

## IT-R04 Owner rút yêu cầu đang chờ về Draft và quyền sửa sau rút

- **Test Case ID:** IT-R04
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST/DT
- **Test Scenario:** Owner rút yêu cầu đang chờ về Draft và quyền sửa sau rút
- **Preconditions:** Requisition Pending_Director của HM_A; HM_B không owner.
- **Test Data:** Withdraw HM_B; Withdraw HM_A; chỉnh sửa Draft; attempt withdraw Approved ở fixture khác.
- **Test Steps:** 1. HM_B gọi withdraw rồi kiểm không đổi. 2. Owner withdraw. 3. Đọc Draft/Withdrawn workflow event/audit. 4. Owner sửa và submit lại theo luồng hiện có.
- **Expected Result:** Chỉ owner của Pending_Director rút được; về Draft có audit/workflow event; sửa/gửi lại dùng cùng requisition. Không biến withdrawal thành xóa và không áp rule một chiều của Application cứng nhắc sang Requisition.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-REQ UC20/21; OR-06; GAP-02
- **Source Code Reference:** [RequisitionServiceImpl.withdraw](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/requisition/service/RequisitionServiceImpl.java:477>); [RequisitionServiceImpl.updateRequisition](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/requisition/service/RequisitionServiceImpl.java:289>); [RequisitionAccess.canEdit](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/requisition/service/RequisitionAccess.java:84>)
- **Existing Test Coverage (static review):** RequisitionIntegrationTest.it06_withdraw_revertsToDraft cover chính.

<a id="it-r05"></a>

## IT-R05 Approve và withdraw cạnh tranh không ghi lịch sử mâu thuẫn

- **Test Case ID:** IT-R05
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT/ST
- **Test Scenario:** Approve và withdraw cạnh tranh không ghi lịch sử mâu thuẫn
- **Preconditions:** Một Pending_Director; owner và Director khác owner; hai connection/transaction riêng.
- **Test Data:** Director approve đồng thời owner withdraw; lượt approve thứ hai stale.
- **Test Steps:** 1. Đọc cùng trạng thái ban đầu từ hai transaction. 2. Phát hai thao tác cạnh tranh. 3. Commit cả hai theo kết quả. 4. Đọc status/approval/events/audit từ transaction mới.
- **Expected Result:** Lock/guard serialize quyết định: chỉ một action hợp lệ với trạng thái tại thời điểm ghi; action thua bị chặn. Không có Approved rồi bị request withdraw cũ đưa Draft, không có hai approval do replay.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P0
- **SRS Reference:** OR-06/07; OLD-REQ
- **Source Code Reference:** [RequisitionServiceImpl.decide](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/requisition/service/RequisitionServiceImpl.java:435>); [RequisitionServiceImpl.withdraw](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/requisition/service/RequisitionServiceImpl.java:477>); [JobRequisitionRepository.findForUpdate](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/requisition/repository/JobRequisitionRepository.java:17>)
- **Existing Test Coverage (static review):** Cần DB concurrency bổ sung; suite 7 method hiện hữu chưa đủ proof race.

<a id="it-r06"></a>

## IT-R06 HR chỉ lấy Approved requisition và Job Posting dùng requisition hợp lệ

- **Test Case ID:** IT-R06
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** EP/DT
- **Test Scenario:** HR chỉ lấy Approved requisition và Job Posting dùng requisition hợp lệ
- **Preconditions:** HR; requisitions Draft/Pending_Director/Rejected/Approved, HM_A/B; form job theo service hiện có.
- **Test Data:** HR list/detail từng status; job create/publish chọn non-Approved và Approved.
- **Test Steps:** 1. HR tìm/xem requisition. 2. Thử tạo/publish job với requisition chưa duyệt qua luồng JobPostingService. 3. Dùng Approved làm control. 4. So JobPosting/Requisition DB.
- **Expected Result:** HR chỉ thấy/đọc Approved theo code; job từ non-Approved bị chặn; Approved đi tiếp nếu các điều kiện khác đúng. Không quy toàn bộ Job Posting module thành code của Linh; đây là điểm nối workflow.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** GBR-09; OLD-REQ UC43; GAP-02
- **Source Code Reference:** [RequisitionAccess.requireView](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/requisition/service/RequisitionAccess.java:140>); [RequisitionServiceImpl.search](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/requisition/service/RequisitionServiceImpl.java:76>); [JobPostingServiceImpl.createJobPosting](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/jobposting/service/JobPostingServiceImpl.java:287>); [JobPostingServiceImpl.publishJobPosting](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/jobposting/service/JobPostingServiceImpl.java:359>)
- **Existing Test Coverage (static review):** JobPostingServiceTest có unit, kiểm signature/guard source trước hiện thực harness.

<a id="it-m01"></a>

## IT-M01 Tạo/sửa internal account không đổi username và password khi update thông tin

- **Test Case ID:** IT-M01
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** ST
- **Test Scenario:** Tạo/sửa internal account không đổi username và password khi update thông tin
- **Preconditions:** Admin; department Active; role internal hợp lệ; internal account đã tồn tại.
- **Test Data:** Create account mới; update tên/email/phone/role/department hợp lệ; snapshot username/hash.
- **Test Steps:** 1. Create internal bằng luồng thật. 2. Đọc User và thử login. 3. Update thông tin của account đã có. 4. Đọc DB so username/hash và Candidate count.
- **Expected Result:** Account nội bộ tạo với BCrypt, không tạo Candidate profile. Update thông tin giữ username/hash hiện hữu; role/department/contact lưu theo input hợp lệ; đổi role ảnh hưởng session theo S04.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-ACC screen 7; OR-02/07
- **Source Code Reference:** [AccountManagementService.createInternal](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountManagementService.java:54>); [AccountManagementService.updateInternal](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/AccountManagementService.java:60>)
- **Existing Test Coverage (static review):** AccountManagementServiceTests có unit; cần web/DB persistence.

<a id="it-m02"></a>

## IT-M02 Department lifecycle và chọn manager đúng phòng

- **Test Case ID:** IT-M02
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** EP/ST
- **Test Scenario:** Department lifecycle và chọn manager đúng phòng
- **Preconditions:** Admin; department D1; internal Active ở D1/D2; Candidate; manager Inactive.
- **Test Data:** Tạo department không manager; update với manager đúng D1; các lựa chọn sai role/status/phòng; deactivate/activate D1.
- **Test Steps:** 1. Create department. 2. Gán user thuộc phòng bằng luồng account. 3. Update manager với các fixture độc lập. 4. Deactivate rồi activate. 5. Đọc Department/User và manager choices.
- **Expected Result:** Tạo department Active không gán manager trước khi có nhân viên. Manager phải internal Active thuộc chính phòng đó; sai bị từ chối. Deactivate/activate chỉ đổi DepartmentStatus, giữ User/requisition, không hard-delete; không suy diễn deactivate department tự khóa mọi user.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-ACC UC15–18; screens 9–11
- **Source Code Reference:** [DepartmentService.create](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/DepartmentService.java:57>); [DepartmentService.update](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/DepartmentService.java:69>); [DepartmentService.deactivate](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/DepartmentService.java:87>); [DepartmentService.activate](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/service/DepartmentService.java:90>)
- **Existing Test Coverage (static review):** DepartmentServiceTests unit + DepartmentReadOnlyTests DB đọc, còn thiếu workflow write integration.

<a id="it-m03"></a>

## IT-M03 Dashboard theo role và Candidate ownership với số liệu DB biết trước

- **Test Case ID:** IT-M03
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** DT
- **Test Scenario:** Dashboard theo role và Candidate ownership với số liệu DB biết trước
- **Preconditions:** Fixture riêng có C1/C2, HR/HM_A, Director, Admin và Interviewer; biết tập đơn/lịch/offer.
- **Test Data:** Dashboard từng account; formal app và draft HM_Suggested; Candidate có 0 dữ liệu làm control.
- **Test Steps:** 1. Login từng role GET /dashboard. 2. So metric/link với fixture độc lập. 3. So C1 không có dữ liệu C2. 4. Tạo proposal rồi confirm để đo count trước/sau.
- **Expected Result:** Đúng model/quyền của role và số liệu fixture; Candidate chỉ dữ liệu mình; pending/cancelled talent draft không cộng ứng tuyển chính thức, chỉ active formal mới tăng count phù hợp. Không lộ internal notes trên Candidate dashboard.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-AUTH screen 5; OR-01/04; NEW-TALENT
- **Source Code Reference:** [DashboardService.forUsername](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/DashboardService.java:52>); [DashboardMetricsRepository.applications](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/DashboardMetricsRepository.java:83>); [DashboardMetricsRepository.candidateApplications](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/DashboardMetricsRepository.java:184>)
- **Existing Test Coverage (static review):** AdminDashboardIntegrationTests có DB; dashboard legacy suites comment không được tính.

<a id="it-m04"></a>

## IT-M04 Pipeline query không trùng và detail/CV không lỗi lazy load

- **Test Case ID:** IT-M04
- **Test Type:** Integration Test; Spring context + real SQL Server, controlled external adapters when needed
- **Test Technique:** EP/DT
- **Test Scenario:** Pipeline query không trùng và detail/CV không lỗi lazy load
- **Preconditions:** HR/HM_A; đơn có nhiều Review/AIResult/Interview/Offer, source potential; open-in-view=false.
- **Test Data:** Keyword tên/email; job/status filter; newest sort; app có/không AI score; Unicode.
- **Test Steps:** 1. Gọi list/filter và ghi ID. 2. Gọi detail/CV sau service transaction đã trả DTO. 3. So tập ID và giá trị fixture độc lập. 4. Refresh sau review/withdraw/closure.
- **Expected Result:** Mỗi formal application một dòng đúng scope/filter, sort newest đúng submissionDate; detail có CandidateId/IsPotential và timeline đúng sự kiện; không LazyInitializationException, không nhân dòng do join nhiều bảng, không lộ draft/cancelled proposal.
- **Actual Result:** 
- **Status:** Pending
- **Priority:** P1
- **SRS Reference:** OLD-PIPE screens 22/23; OR-04; NEW-TALENT
- **Source Code Reference:** [ApplicationPipelineService.list](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationPipelineService.java:68>); [ApplicationPipelineService.detail](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationPipelineService.java:112>); [ApplicationPipelineService.cv](<D:/KÌ 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/service/ApplicationPipelineService.java:177>)
- **Existing Test Coverage (static review):** PipelineQueryTests + service unit/web slices đã nhiều coverage; chọn bộ fixture đa quan hệ thay vì lặp mọi UI snapshot.


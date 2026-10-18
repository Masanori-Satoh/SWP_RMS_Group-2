# Test Plan RMS cho các luồng của Linh

Ngày lập: 17/10/2026, Asia/Saigon. Chủ thể rà soát: `linhkata1806` và `linh hoang`. Đây là kế hoạch và thiết kế test; chưa thực thi, chưa chứng nhận Passed.

## 1. Phạm vi và cơ sở xác định kết quả

Ưu tiên Integration Test theo yêu cầu cuối của chủ dự án. Checklist tập trung Authentication, phân quyền, Candidate Account, Application, Talent Pool, HM Suggested Candidate và giao tiếp với Interview, Requisition, Job Posting, Dashboard. Account/Department là phần phụ trợ của Linh. Requisition Approval là luồng tích hợp liên phân hệ, không quy toàn bộ module này thành đóng góp riêng của Linh.

Đã thấy tác giả `linhkata1806` ở các commit `ab1adba`, `9ad50e8`, `c24b43a`, `a4b711e`; `linh hoang` ở `1954472`, `4490817`. Không lấy nội dung toàn bộ merge commit làm bằng chứng sở hữu code. Bản working files đang đọc là cơ sở thiết kế; không khẳng định mọi file trùng HEAD. Một số lệnh Git status gặp lỗi môi trường, nên không chứng nhận working tree sạch.

**Chủ dự án đã xác nhận SRS sai và chưa cập nhật trong hội thoại này.** Thứ tự dùng làm test basis:

1. Code hiện có, DTO, route, transaction, repository query và template tương ứng.
2. Quyết định nghiệp vụ mới nhất trong `docs/members/linhdn/`, đối chiếu lại code.
3. SRS cũ chỉ dùng làm lịch sử truy vết; những mục chưa khớp ghi rõ OLD hoặc GAP.

Expected Result trong bộ này mô tả hành vi hiện được code hỗ trợ, chưa có nghĩa mọi hành vi đó đã được nghiệm thu là yêu cầu cuối. Không dùng việc code khác SRS cũ để tự động kết luận bug. Không đưa rule chỉ có ở SRS vào ca test có expected chắc chắn.

SRS đã đọc: `D:/KÌ 5/SWP/hireflow-api/RMS_SRS.docx`. Số Pxxxx dưới đây là thứ tự paragraph trong `word/document.xml`, gồm cả đoạn trong bảng; dùng kèm tên mục, không phải số trang Word. Không chọn bản `fe/RMS_SRS.docx` hoặc SRS của repository khác làm chuẩn thay thế.

## 2. Các sai lệch tài liệu được báo cáo trước khi thiết kế test

| Mã | SRS cũ | Code hiện tại và cách xử lý khi test |
|---|---|---|
| GAP-01 | Screen 3 Forgot Password là popup gửi reset link, P0924 | `PasswordRecoveryController` + `PasswordRecoveryService` dùng Email → OTP → Password, state trong session. Test OTP; không test signed reset link cũ. |
| GAP-02 | Screen 14: HM chỉ thấy yêu cầu của mình, HR thấy tất cả, P0968 | `RequisitionAccess.requireView` cho HM phạm vi phòng ban, HR chỉ Approved. Expected theo code, cần cập nhật ma trận SRS. |
| GAP-03 | Screen 22 cho Interviewer xem candidate được phân công, P1000 | SecurityConfig chặn ROLE_INTERVIEWER ở `/applications/**`; Interviewer có luồng Interview riêng. Không giả định được vào pipeline chung. |
| GAP-04 | SRS dùng trạng thái/thuật ngữ tổng quát, không mô tả đầy đủ proposal mới | Code phân biệt hồ sơ chính thức và `HM_Suggested` bằng status, review và audit. HM nhận gợi ý HR chưa bằng ứng viên đồng ý. |
| GAP-05 | GBR-03 chỉ staff assigned được xem CV, P1598 | Pipeline cho HR/Director/System Admin view all; Talent Pool HR/HM dùng kho chung, còn job đích kiểm tra phòng ban. Đây là khác biệt cần chốt quyền nghiệp vụ; không tự thu hẹp scope code. |
| GAP-06 | GBR-07 consent xử lý dữ liệu, P1618; GBR-13 cooldown 3–6 tháng, P1648 | `ApplicationSubmissionService.apply` chưa thể hiện hai rule này. Consent dùng CV cũ trong `TalentPoolConfirmRequest` là rule khác. Không chế thêm checkbox, cooldown hoặc endpoint. |
| GAP-07 | Code comment ApplicationReviewService nhắc UC42/46, MyApplicationService nhắc UC39 | Bảng UC trong SRS đọc được: UC30 Approve CV, UC49 Forward, UC54 Apply, UC55 AI, UC57 My Applications, UC58 Withdraw; UC42 là lịch của Interviewer. Không lấy comment UC sai làm SRS reference. |
| GAP-08 | Screen 8/UC12 có chữ delete account | Lifecycle hiện tại là deactivate/activate; Candidate không bị xóa profile/CV/application khi vô hiệu hóa. Không test hard delete như chức năng có sẵn. |

Các rule chỉ ở SRS về erasure 72 giờ, probation, blind review, audit mọi lượt đọc không được tuyên bố đã có coverage trong phạm vi này. Không đánh giá tuân thủ pháp luật từ SRS cũ.

## 3. Hiện trạng kỹ thuật đã đọc

| Hạng mục | Hiện trạng |
|---|---|
| Runtime | `pom.xml`: Spring Boot **3.5.16**, Java **21**, packaging jar |
| Web | Spring MVC + Thymeleaf + layout dialect; không thấy JSP engine trong pom |
| Persistence | Spring Data JPA/Hibernate, Microsoft SQL Server driver; SQLServerDialect, Unicode nationalized data |
| Security | Form login/session, BCrypt, CSRF; `/api/**` được loại khỏi CSRF. `@EnableMethodSecurity` đang comment: không được coi `@PreAuthorize` tự hoạt động; cần kiểm chứng route rules và service access checks. |
| DB local | `localhost:1433`, DB name `RitirementManagement2`; không mở kết nối, không chạy SQL |
| Profile/port | Base active `local`; base port 8080, local override 8081. Khi tự chạy phải xác nhận port thực tế của máy. Không xuất credential/SMTP secret ra báo cáo. |
| Hibernate | App base `ddl-auto=update`, `open-in-view=false`; Surefire đặt `ddl-auto=validate`. `validate` chỉ chặn Hibernate DDL, không biến test thành read-only. |
| Test dependencies | starter-test, Mockito, Mockito JUnit Jupiter, spring-security-test; JaCoCo 0.8.12. Chưa có H2/Testcontainers dependency. |
| File | Servlet tối đa 10 MB; `CvFileValidator` nhận PDF có extension `.pdf` và header `%PDF-`, giới hạn nghiệp vụ 5,242,880 bytes. Không suy diễn validator xác minh toàn bộ cấu trúc PDF. |
| Side effects | CV ở local filesystem; AI listener async AFTER_COMMIT; email tuyển dụng gọi sau business transaction. Rollback DB không tự hoàn tác SMTP, file, RAM/session hoặc transaction async. |

## 4. Phân tầng kiểm thử vừa đủ cho đồ án local

| Tầng | Mục tiêu | Cách làm khi được yêu cầu thực thi sau này |
|---|---|---|
| Unit | Rule thuần, biên, trạng thái, quyền, chọn dữ liệu | JUnit 5 + Mockito hiện có; không cần Spring context/DB cho mọi case |
| Integration web slice | Binding, validation, CSRF, security filter, route/view | `@WebMvcTest` + MockMvc; service/repository bị mock thì không chứng minh persistence |
| Integration DB/workflow | Spring service thật + repository SQL Server thật, transaction/audit/query consistency | Tận dụng `@SpringBootTest` + MockMvc hiện có, DB local riêng dành cho test, schema đã chuẩn bị độc lập; flush/clear và đọc lại |
| System manual | Người dùng thao tác website, nhiều role/session, upload/preview/email thực tế | Làm theo `SYSTEM_TEST_MANUAL.md`; không quy thao tác browser thành bằng chứng DB/transaction nếu chưa quan sát được |

Không cần Docker, Selenium grid, microservice contract framework hay Testcontainers ngay. Nếu sau này cần DB tái lập được, cân nhắc MSSQL Testcontainers với phiên bản tương thích Boot 3.5; không đổi pom trong công việc này. DB local riêng là phương án đủ dùng. H2 không thay thế được kiểm tra query SQL Server, Unicode, lock và native SQL.

## 5. Bộ test hiện có và khoảng trống

Kiểm kê bằng đọc source sau loại comment, không chạy test; số dưới là test method đang khai báo, **không phải số case đã chạy**, parameterized có thể tạo nhiều lượt.

| File dưới `src/test/java/com/group2/rms/` | Loại thực tế | Coverage đã thấy trong source / giới hạn |
|---|---|---|
| `service/AuthenticationDatabaseTests.java` | SpringBootTest + DB, 1 method | Writer tạo Active Candidate + User; các test signed-link reset cũ nằm trong comment. Không chứng minh OTP xuyên controller/service/DB. |
| `auth/AuthBotanicalWebTests.java` | WebMvcTest, 6 methods | Form/OTP/CSRF, các service auth mock; không xác thực account/mật khẩu DB thật. |
| `SecurityFlowTests.java` | Nội dung đang comment | Không tính là coverage security đang hoạt động. |
| `service/AccountManagementDatabaseTests.java` | SpringBootTest + DB, 1 method | Persistence User/Candidate/contact; còn thiếu kiểm tra session xuyên vòng deactivate → login. |
| `service/AccountListQueryTests.java` | Nội dung đang comment | Không coi như query test đang chạy. |
| `candidate/ApplicationPipelineWebTests.java` | WebMvcTest, 56 methods | UI/controller/security, mock service; nhiều tình huống Talent Pool đã có nhưng không thay thế DB thật. |
| `candidate/repository/ApplicationPipelineQueryTests.java` | SpringBootTest + DB, 11 methods | Scope, filter, timeline/progress, conditional transition; một số fixture phụ thuộc dữ liệu local. |
| `candidate/TalentPoolFlowIntegrationTests.java` | Full context + MockMvc + DB, 7 methods | HM suggest/HR consent/decline, HR recommend/HM review/withdraw, CV mới theo origin; phải đọc expected thực tế trước khi bổ sung trùng. |
| `candidate/InterviewPotentialIntegrationTests.java` | Full context + MockMvc + DB, 2 methods | Bookmark sau Passed/Failed, HR shared notes; không có nghĩa toàn bộ Interview/Offer đã được nghiệm thu. |
| `candidate/service/*Tests.java` | Unit Mockito | Submission, review, status, access, progress, bookmark, selection, recommendation đã có nhiều ca rule. Ưu tiên bổ sung integration boundaries. |
| `requisition/integration/RequisitionIntegrationTest.java` | Full context + DB, 7 methods | Create, Unicode read, approve, reject, withdraw, copy, delete; thiếu race/unauthorized xuyên full stack trong suite này. |
| `dashboard/AdminDashboardIntegrationTests.java` | Full context + DB, 7 methods | Query/model Admin; cần kiểm tra proposal không cộng vào số đơn chính thức và scope từng role. |
| `offer/OfferIntegrationTest.java` | **WebMvcTest**, service mock, 21 methods | Tên Integration không có nghĩa DB integration. Offer service/query suites là các lớp khác. |
| JS tại `src/test/js/` | Node tests | Selection/bookmark/recommendations/preview/account UI; không thay thế thao tác browser hoặc Spring security. |

Không dùng tổng Passed từ báo cáo ngày trước làm kết quả hiện tại. Những file dashboard/admin test cũ có nội dung comment cũng không được cộng vào coverage đang hoạt động. Không có số % JaCoCo mới vì chưa chạy và không dùng target/report cũ làm bằng chứng hiện tại.

## 6. Các lớp và phương thức cần kiểm thử

Tất cả đường dẫn Java dưới `src/main/java/com/group2/rms/`. Catalog case ghi chính xác class#method. Unit và integration cùng nhắc một method nhưng có mục tiêu khác nhau.

| Nhóm | Unit cần kiểm rule | Integration cần kiểm điểm nối |
|---|---|---|
| Auth | `auth/service/RegistrationVerificationService.requestRegistration`, `verifyAndRegister`, `resendOtp`; `PasswordRecoveryService.requestReset`, `verifyOtp`, `completeReset`, `canResetPassword`; `PasswordResetService.findActiveAccount`, `isCurrent`, `changePassword` | RegistrationController → services → AccountManagementService → UserRepository/CandidateRepository; PasswordRecoveryController → service → DB hash; security login qua `core/security/DatabaseUserDetailsService.loadUserByUsername` |
| Security | `candidate/service/ApplicationAccess.canView`, `canReview`, `canRescreen`; `requisition/service/RequisitionAccess.canDecide`, `requireView`, `inManagedDepartment` | `core/config/SecurityConfig.filterChain`; `core/security/AccountSessionGuardFilter.doFilterInternal` với user DB thật; route + record scope + CSRF |
| Candidate/internal account | `user/service/AccountListService.findCandidateAccounts`, `activateCandidate`, `deactivateCandidate`; `AccountManagementService.assertLoginIdentifiersAvailable` | `user/controller/CandidateAccountController.list`, `activate`, `deactivate`; `AccountManagementService.createCandidateWithPasswordHash`, `createInternal`, `updateInternal` qua repositories |
| Application | `CvFileValidator.validate`; `ApplicationReviewService.review`, `rescreen`; `ApplicationProgressResolver.resolve`; `MyApplicationService.withdraw` | `ApplicationSubmissionService.apply` + CvStorage + application repository + event; `ApplicationPipelineService.list`, `detail`, `cv`; `ApplicationStatusService.requireScheduleable`; `MyApplicationService.list`, `withdraw` |
| Talent Pool | `TalentPoolService.reviewSelection`, `saveBookmark`, `removeBookmark`, `suggest`, `recommend`, `decideRecommendation`, `confirm`, `decline`, `withdrawRecommendation`, `closeJob`, `cv` | TalentPoolController → service thật → Application/Review/Candidate/Note/Audit; source CV file; interview eligibility; repository exclusion of drafts in all formal queries |
| AI/mail | `AiScreeningService.screen`, `AiScreeningListener.onApplicationSubmitted`; email content/recipient rule | Commit thật → listener async; `RecruitmentEmailService.sendJobClosure`, `resendClosure`, `sendInterview`, `resendInterview` sau business commit; SMTP mock có kiểm soát, không gửi ra người thật |
| Requisition | `RequisitionValidator.validate`; `RequisitionAccess.canCreate`, `canEdit`, `canDecide` | `RequisitionServiceImpl.createRequisition`, `updateRequisition`, `decide`, `withdraw`, `copy`, `deleteRequisition`, `getById`; Approval + WorkflowEvent + AuditLog + notification |
| Phụ trợ | `DepartmentService`, `UserProfileService` validation/access rule | `DepartmentService.create`, `update`, `deactivate`, `activate`; `DashboardService.forUsername`; `DashboardMetricsRepository.applications`, `candidateApplications`; `AccountListService.findAccounts` |

Đọc method signature trong source trước khi hiện thực test; danh sách không phải yêu cầu tạo method mới. Getter/setter Lombok, constant label và constructor DTO không cần test riêng chỉ để tăng coverage.

## 7. Thiết kế kỹ thuật test

- **EP**: role hợp lệ/sai role; owner/khác owner; job mở/đóng; CV hợp lệ/rỗng/sai header; proposal pending/đã xử lý. Mỗi ca âm chỉ làm sai một điều kiện để tránh lỗi này che lỗi khác.
- **BVA**: password 7/8/9 và 31/32/33; OTP 5/6/7 chữ số; sai OTP lần 4/5; hết hạn tại 899/900/901 giây; resend tại 299/300/301 giây; note 999/1000/1001; source selection 0/1/20/21; CV 5,242,879/5,242,880/5,242,881 bytes.
- **Decision Table**: login (Active + đúng password); HM approval (đúng role + khác owner + Pending_Director); old-CV activation (HR + đã HM chấp nhận + consent + job mở + không trùng); new CV (proposal đã HM chấp nhận / đang chờ / không có proposal).
- **State Transition**: Applied → AI_Screened → HR_Passed → HM_Passed; Fail → Rejected; Hold giữ nguyên; Requisition Draft → Pending_Director → Approved/Rejected, withdraw về Draft; proposal giữ `HM_Suggested` sau HM Pass, activation mới `HM_Passed`.

Các biến thể trong một ca BVA/decision table phải ghi kết quả riêng bằng hậu tố `.v1`, `.v2` khi thực hiện; một biến thể đạt không làm cả nhóm Passed. Vì đây là thiết kế, tất cả biến thể đều chưa thực hiện.

## 8. Chuẩn bị trước khi người dùng tự chạy

1. Dùng bản local và database riêng cho kiểm thử; tự xác nhận schema/migrations khớp entity. Không chạy ứng dụng với `ddl-auto=update` trỏ DB cần giữ chỉ để xem test plan. Chưa tạo DB/profile/config trong phiên này.
2. Tạo fixture qua UI hoặc dữ liệu kiểm thử được chủ dự án cho phép: Admin, HR, HM phòng A/B, Director khác người tạo, Interviewer, Candidate C1/C2; Active/Inactive/Blocked. Không in password/OTP thật vào báo cáo.
3. Chuẩn bị tin J1 Published chưa hết hạn phòng A, J2 Published khác tin nguồn, J3 Closed, một requisition Pending_Director/Approved; file PDF mẫu local và source application chính thức của C1.
4. Với ca DB: ghi baseline số Candidate/Application/Review/Note/Audit, flush và clear trước đọc lại. Snapshot những trường không được thay đổi. Test rollback cuối mỗi case nếu phù hợp.
5. Với commit/event/SMTP/race: không bọc toàn bộ case trong transaction rollback rồi tuyên bố đã kiểm chứng AFTER_COMMIT. Cho transaction nghiệp vụ commit trên DB test, quan sát từ transaction khác, chờ hữu hạn sự kiện async và dọn đúng fixture sau. Mail sender kiểm soát, filesystem test root riêng. Hai request concurrent phải dùng hai transaction/connection độc lập.
6. Chụp evidence gồm case ID, role, dữ liệu giả, URL, trước/sau, DB record IDs khi cần, thời gian và log liên quan; redact hash, OTP, secret. Trường Actual Result chỉ điền sau khi làm.

## 9. Ưu tiên và điều kiện hoàn thành

| Thứ tự | Rủi ro | Case cần làm trước |
|---|---|---|
| 1 — P0 | Vào sai quyền, lộ CV/note, account bị khóa vẫn có session, chiếm reset | IT-S01–S06, IT-A01/A02/A07/A09/A10, IT-C02/C03 |
| 2 — P0 | Nhảy bước, proposal chưa có consent thành đơn chính thức, duplicate/mất CV | IT-P01/P04/P05/P06/P08/P09, IT-T05/T06/T08–T14 |
| 3 — P0 | Đóng đợt làm mất offer/đơn, phê duyệt sai người | IT-E01–E04, IT-R01/R02/R03/R05 |
| 4 — P1 | Note/selection/query/report sai, lỗi mail ảnh hưởng business | Các ca còn lại và ST-01–ST-08 |
| 5 — P2 | Trải nghiệm, sorting/empty state mở rộng | Làm sau đường đi chính; không lấy UI snapshot làm ưu tiên cao hơn integrity |

Entry: đã chốt expected của GAP liên quan, có DB test và fixture đủ, app chạy local bởi người kiểm thử. Exit đề xuất: P0 thực hiện đầy đủ, lỗi nghiêm trọng được xử lý/ghi nhận, từng case có evidence hoặc lý do chưa làm, GAP SRS còn mở được ghi rõ. Không kết luận toàn hệ thống Passed khi chỉ hoàn tất checklist phạm vi Linh.

## 10. Trùng lặp và giá trị test

- Giữ unit review/permission để kiểm nhánh nhanh; DB test chỉ cần xác minh trạng thái, review, audit và rollback thực sự đi cùng nhau. Không lặp nguyên mọi tổ hợp unit thành full context.
- Suite TalentPoolFlowIntegrationTests đã có happy path, reject, withdraw, CV mới. Khi tự chạy dùng lại ca hiện hữu; ưu tiên bổ sung consent=false, pending-HR, concurrent request, closure và commit/SMTP hơn việc viết lại happy path tương tự.
- Web slice và system manual cùng thao tác một nút vẫn có mục đích khác: slice xác minh binding/security, manual xác minh browser và trạng thái nhiều màn hình. Không cộng đôi thành “hai nghiệp vụ được coverage”.
- Không tính test bị comment/skip, contextLoads, HTTP 200 đơn lẻ, chỉ verify mock called hoặc snapshot HTML vào bằng chứng business persistence. Một suite 7 method không có nghĩa đúng 7 requirements.
- Fixture phụ thuộc DB local có thể thiếu đúng trường hợp, hoặc `assume` làm skip. Ghi “thiếu dữ liệu/Pending”, không Passed. Dùng fixture biết trước thay cho kỳ vọng tự tính từ chính query đang được kiểm tra.
- Tên `OfferIntegrationTest` cần phân loại theo annotation/mock; không cần đổi tên hay sửa source trong lượt này.

## 11. Tệp đi kèm

- `INTEGRATION_TEST_CASES.md`: danh sách ca tích hợp, đầy đủ field yêu cầu, kết quả trống.
- `SYSTEM_TEST_MANUAL.md`: bước thao tác website cho 8 hành trình chính; chưa chạy.
- `RESEARCH_SOURCES.md`: ghi chú nghiên cứu nguồn chính thức và giới hạn tương thích phiên bản.

Không sửa Java, template, test source, schema, database, pom, SRS, cấu hình hoặc memory. Chỉ tạo tài liệu kiểm thử mới trong thư mục này.

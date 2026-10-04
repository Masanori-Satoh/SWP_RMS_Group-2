# Rà soát project trước khi tiếp tục Candidate UI — 04/10/2026

## Phạm vi và trạng thái

- Yêu cầu: kiểm tra lại project một vòng trước khi quay lại UI/UX; frontend phải thống nhất với Mộc.
- Đây là lượt audit. Không sửa Java, template, CSS, JS, security, schema hay dữ liệu DB.
- Source thay đổi trong lúc kiểm tra: đầu lượt có Dashboard phẳng và lồng cùng tồn tại; cuối lượt bốn file Dashboard lồng đã bị xóa. Không được lấy kết quả đầu lượt làm trạng thái cuối.
- Patch Candidate Workspace ở lượt trước đã bị từ chối; chưa có implementation Candidate Workspace từ patch đó.
- Checkout: `D:/kì 5/SWP/hireflow-api/RetireManagement/SWP_RMS_Group-2`.

## Kết quả kiểm chứng

| Hạng mục | Kết quả | Bằng chứng / giới hạn |
|---|---|---|
| Compile đầu lượt | PASS | `mvn.cmd -q -DskipTests compile` |
| Context đầu lượt | FAIL | `ConflictingBeanDefinitionException`: `dashboardController` phẳng trùng với controller lồng |
| Unit tests đầu lượt | 14 PASS | AccountListServiceTests, AccountManagementServiceTests, ApiMonitoringServiceTests, MailEnvironmentBindingTests |
| Web tests giữa lượt | testCompile FAIL, sau đó đã sửa import | Lỗi cũ được giữ làm lịch sử; không phải lỗi import còn tồn tại ở cuối lượt |
| Build sạch sau khi đổi package | PASS | `mvn.cmd -q -DskipTests clean compile` |
| Build/test sạch cuối | Compile + testCompile PASS; tests FAIL | 45 test: 26 PASS, 1 FAILURE, 18 ERROR; chi tiết dưới đây |
| Startup trực tiếp từ target/classes | FAIL | VerifyError: AccountFieldException bytecode không kế thừa Throwable, trong khi source có extends BaseBusinessException |
| Hibernate validate ở lần startup trực tiếp | PASS | EntityManagerFactory đã khởi tạo; không thay thế query/constraint/business verification |
| Startup từ bản build chụp riêng | FAIL ở HTTP/Tomcat | Không còn VerifyError; `Unable to establish loopback connection` → `SocketException: Invalid argument: connect` khi khởi động NioEndpoint |
| Browser / responsive | NOT TESTED | Chưa sửa UI; chưa xác nhận bằng trình duyệt |

Log đầu lượt và lỗi testCompile nằm ở `../tests/assets/2026-10-04-project-audit/`.
Các test có Context đã ép `spring.jpa.hibernate.ddl-auto=none`, `spring.sql.init.mode=never`.

## Findings và hướng xử lý

### A01 — Đổi package Dashboard và test (imports đã được sửa trong lượt)

Source cuối lượt dùng:

```text
com.group2.rms.dashboard.DashboardController
com.group2.rms.dashboard.DashboardService
com.group2.rms.dashboard.DashboardMetricsRepository
com.group2.rms.dashboard.DashboardResponse
```

Các file đã gặp lỗi import cũ ở giữa lượt, rồi được cập nhật trong workspace:

- `src/test/java/com/group2/rms/SecurityFlowTests.java`.
- `src/test/java/com/group2/rms/service/AccountListQueryTests.java`.
- `src/test/java/com/group2/rms/service/DashboardDatabaseTests.java`.

Cuối lượt các file trên đã dùng package phẳng và DashboardResponse; testCompile đã đi qua. Giữ nguyên assertion nghiệp vụ. Sau chuyển package cần build sạch để không còn `.class` cũ trong target/classes; không khôi phục package lồng để chữa lỗi test.

### A02 — CandidateAccountController trong user chưa hoạt động

- `user/controller/CandidateAccountController.java` chưa có `@Controller`.
- Bản auth có annotation ở đầu lượt, nhưng đã bị xóa trong workspace ở cuối lượt.
- Khi chỉ còn bản user chưa có @Controller, route sẽ không được đăng ký bởi component scan. Đây là phần migration chưa hoàn tất.
- Hướng sửa ở trạng thái cuối: thêm @Controller vào bản user, xác nhận không còn implementation cũ, retest list/deactivate. Assistant chưa sửa hoặc xóa Java trong lượt audit.

### A03 — Refactor exception chưa nối vào các flow

- AccountController.create/update và RegistrationController.register vẫn catch lỗi nghiệp vụ/DB và render form.
- RequestReset đã bỏ catch RuntimeException trong lượt; SMTP adapter bắt MailAuthenticationException/MailSendException và handler giữ thông báo chung. reset vẫn catch AccountFieldException.
- HealthController đã chuyển sang HealthService, trả HealthResponse; handler DatabaseHealthException đã có JSON 503/no-store. Đây là phần source đã nối đúng hướng; test slice chưa cập nhật dependency.
- FormErrorViewHelper hiện có sáu method ném UnsupportedOperationException; chưa tìm thấy lời gọi. Không nối helper này vào handler trước khi triển khai đầy đủ.
- Các recovery exception đã chuyển sang auth và SMTP flow đã sử dụng hai exception gửi mail. Controller Account/Register/Reset vẫn còn tự catch nên refactor exception chưa hoàn tất.

Hướng sửa: service ném exception phù hợp; handler chung xử lý; Controller chỉ xử lý BindingResult từ @Valid. Khi render lại form phải giữ dữ liệu không nhạy cảm, options và lỗi đúng field; xóa password khỏi model. Giữ hợp đồng health 200 UP / 503 DOWN và no-store.

### A04 — PasswordMatches đã áp dụng trong lượt

Validator có initialize, so sánh và gắn lỗi vào confirmPassword; cấu trúc dùng chung đã có.
Cuối lượt cả CreateAccountRequest, RegisterAccountRequest, ResetPasswordRequest đã gắn annotation; kiểm tra mismatch thủ công đã được bỏ. Giữ @NotBlank/@Size và validation service. Cần retest field errors sau khi WebMvcTest context được sửa; chưa ghi validation browser PASS.

### A09 — Test chưa theo kịp refactor Health/Monitoring

- SecurityFlowTests có 18 ERROR cùng root cause: No qualifying bean of type HealthService. WebMvcTest loại Service khỏi slice, nhưng HealthController nay cần HealthService.
- Hướng sửa để giữ health contract test: @Import HealthService trong slice đang mock JdbcTemplate, hoặc mock HealthService và cho mock ném DatabaseHealthException ở test DOWN. Không chỉ bỏ health test để test pass.
- ApiMonitoringServiceTests.unauthenticatedRequestCannotStartProbe còn expect IllegalStateException; code hiện ném MonitoringSessionRequiredException. Cập nhật loại exception assertion và giữ verifyNoInteractions(transport).
- CareerFlowTests: 13 PASS; AccountListServiceTests: 2 PASS; AccountManagementServiceTests: 7 PASS; MailEnvironmentBindingTests: 2 PASS; ApiMonitoringServiceTests: 2 PASS/1 FAIL.

### A10 — Source và bytecode từng không đồng nhất

Ở lần startup từ target/classes: javap cho thấy AccountFieldException.class không có extends, trong khi source có extends BaseBusinessException. JVM ném VerifyError tại PasswordRecoveryController.reset. Sau build/test sạch cuối, javap đã xác nhận đúng extends BaseBusinessException.
Không sửa source đúng chỉ để chữa bytecode cũ. Build sạch và kiểm tra trên bản sao classes riêng khi có build/IDE cập nhật target đồng thời. Không khẳng định IDE là nguyên nhân duy nhất vì chưa kiểm tra cấu hình IDE.

### A11 — HTTP startup còn bị lỗi môi trường loopback

Bản chụp `target/audit-runtime/classes` có AccountFieldException kế thừa đúng. Startup cuối đi qua lỗi VerifyError, tới bước khởi động Tomcat connector và FAIL với:

```text
WebServerException: Unable to start embedded Tomcat server
IOException: Unable to establish loopback connection
SocketException: Invalid argument: connect
```

Không phải bằng chứng port 8080 bị chiếm: lần kiểm tra dùng server.port=0. Chưa xác định cấu hình Windows/JDK/network cụ thể gây lỗi; không tự đổi SecurityConfig hoặc DB để chữa. HTTP/browser vẫn NOT TESTED; không công bố production readiness.

Lượt audit tạo hai file Markdown và thêm entry tests/README.md, lưu hai log đầu lượt. Các thay đổi Java trong Git status là thay đổi workspace diễn ra đồng thời, không phải chỉnh sửa do lượt audit này thực hiện.

### A05 — Requisition có thao tác xóa qua GET và thiếu kiểm tra actor

- RequisitionController.deleteRequisitionGet tại `/requisitions/delete/{id}` gọi xóa thật.
- SecurityConfig hiện chỉ yêu cầu authenticated cho Requisition; không có rule giới hạn role riêng ở route này.
- RequisitionServiceImpl.deleteRequisition gọi repository.delete, không kiểm tra quyền của actor.
- Create dùng mockHiringManagerId = 1; service dùng SYSTEM_USER_ID = 1 cho log.
- Submit/approve/reject trong service hiện là method rỗng.

Hướng sửa: bỏ mutation qua GET, giữ POST có CSRF, dùng actor từ session và kiểm tra quyền/ownership ở service. Quyền chi tiết và hard-delete/lifecycle của Requisition thuộc feature teammate: cần xác nhận trước khi thay nghiệp vụ. Không chạy thử endpoint delete trên dữ liệu thật để audit.

### A06 — Dashboard Candidate mới chỉ có phần chuẩn bị

- DashboardResponse có CandidatePanel; repository có candidateRecentApplications/candidateNextInterviews/candidateOffers.
- DashboardService.candidate chưa tạo CandidatePanel; template dashboard/index chưa render panel.
- CANDIDATE_VISIBLE_OFFER_STATUSES dùng Rejected trong khi schema Offer dùng Declined. HM offer query còn dùng Approved thay vì bộ OfferStatus hiện tại; cần đối chiếu feature owner trước khi sửa rule HM.
- Các câu về Candidate.UserId/datasource và Notifications chưa có bảng đang xuất hiện trên UI.

Hướng sửa UI Candidate: dùng các projection/query đã có, giới hạn UserId trong query, chỉ render dữ liệu được phép theo GBR-02. Sửa Rejected thành Declined cho Offer; giữ phong cách Mộc, empty state có CTA thật. Không thêm feature/dữ liệu giả.

### A07 — Public Jobs: query không thống nhất và nội dung HTML chưa escape

- findPublishedJobs loại job có ApplicationDeadline NULL; findOpenPostings/findOpenPosting và detail lại cho phép NULL.
- CareerPortalService.formatRichText chỉ thay newline bằng br; candidate/job-detail dùng th:utext trực tiếp. HTML trong dữ liệu chưa được escape/sanitize.
- hasApplied đang hardcode false; Apply chỉ ném ITERATION_2_PENDING, chưa có POST submit/upload CV.

Hướng sửa: thống nhất xử lý deadline theo rule đã có, render text escaped với CSS white-space hoặc escape trước khi thêm br. Giữ nguyên nội dung tiếng Việt trong DB. Không coi Apply đã hoàn tất hoặc tự tạo storage/duplicate-application policy.

### A08 — Frontend chưa thống nhất

- Auth/Admin/Dashboard dùng interfaceHead + design-tokens/interface/workspace và fragment chung.
- Public Jobs dùng header/head riêng, global.css/job-board.css; nhiều inline style và nhãn tĩnh tiếng Việt.
- Font assets Lora/Source Sans 3 có sẵn nhưng head Public Jobs chưa load design-tokens chứa @font-face.
- Requisition/Hello dùng shell khác; pagination fragment có inline CSS; error pages có inline style và nhãn tiếng Việt.
- Controller hiện render candidate/job-board và candidate/job-detail; template careers cũ không phải nguồn UI đang chạy ở các route này. Chưa xóa template vì cần kiểm tra dependency đầy đủ.

Hướng làm UI tiếp theo: chọn fragment/token chung hiện có, không tạo thêm bộ typography/color/control. Candidate dùng shell workspace; Public Jobs dùng shared public header cùng brand tokens. Tách CSS/JS trang ra static, tiếng Anh cho nhãn tĩnh, nội dung DB giữ nguyên. Rà 1440/1024/768/375, keyboard, focus, table overflow và empty state.

## Những phần giữ lại

- Form login + session, BCrypt, CSRF mặc định đang bật; không JWT.
- Admin route protection, AccountSessionGuardFilter kiểm tra Active và role hiện tại.
- Account lifecycle tách Candidate/internal; update account không thay hash; deactivate giữ record.
- Candidate.UserId là FK NOT NULL UNIQUE; query Dashboard cá nhân scope theo UserId.
- Schema có 20 table. Kiểm tra tên mapping tĩnh không thay thế Hibernate validate hoặc chạy repository query thật. User dùng quoted identifier hợp lệ, không phải bảng mới.

## Điểm cần xác nhận trước chức năng ghi dữ liệu Candidate

- Stage/Status suy ra từ ApplicationStatus; chưa có nguồn trực tiếp cho Inactive.
- Trạng thái được Withdraw, tác động lên interview/offer và quyền ứng tuyển lại.
- Offer Date: không có ngày gửi trong schema; không tự dùng CreatedAt làm ngày gửi.
- Quy tắc Candidate phản hồi Offer và validation negotiation.
- Loại/size CV, storage, submit transaction.
- Notifications/Personal Reminder không có datasource; không thêm bảng khi chưa được phép.

## Điểm nối tiếp

1. Hoàn tất annotation CandidateAccountController; imports Dashboard đã cập nhật, không làm lại.
2. Sửa test slice HealthService và assertion MonitoringSessionRequiredException.
3. Xác nhận startup/JPA/query ở trạng thái sạch, không dùng ddl-auto=update trong audit.
4. Tiếp tục refactor exception theo dependency; PasswordMatches/Health source đã nối, không kích hoạt helper stub.
5. Xử lý hoặc xác nhận các lỗi bảo mật thuộc Requisition.
6. Sau đó nối CandidatePanel và thống nhất frontend theo shared CSS/fragments; có flow doc và test từng bước.

Kịch bản và kết quả riêng: `../tests/2026-10-04-project-audit.md`.

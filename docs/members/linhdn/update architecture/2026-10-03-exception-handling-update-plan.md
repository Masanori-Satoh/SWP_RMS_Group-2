# Kế hoạch cập nhật kiến trúc xử lý lỗi

Ngày lập: 03/10/2026.

**Trạng thái: tài liệu hướng dẫn, chưa triển khai các thay đổi ứng dụng.** Lượt này chỉ tạo file Markdown này. Các bước và tên class mới dưới đây là đề xuất để người thực hiện nối tiếp; không phải chức năng đã có hoặc kết quả đã kiểm thử.

## 1. Phạm vi và quy tắc

Tập trung sửa bảy khối `try-catch` trong năm Controller đã được rà soát:

1. `AccountController.create()`.
2. `AccountController.update()`.
3. `RegistrationController.register()`.
4. `PasswordRecoveryController.requestReset()`.
5. `PasswordRecoveryController.reset()`.
6. `HealthController.health()`.
7. `TestDbController.testDatabaseConnection()`.

Các đường dẫn bên dưới tính từ thư mục gốc `SWP_RMS_Group-2`. Số dòng là vị trí tại thời điểm đọc source ngày 03/10/2026, có thể thay đổi sau refactor; tìm theo tên hàm để nối tiếp.

- Controller nhận request, gọi Service và xử lý nhánh thành công. Exception đi tới `com.group2.rms.core.exception.GlobalExceptionHandler`.
- Exception nghiệp vụ kế thừa `BaseBusinessException`. Tầng Service/adapter được chuyển exception kỹ thuật thành exception nghiệp vụ phù hợp rồi ném tiếp; không bắt rồi bỏ qua thất bại.
- Nếu tập trung cả validation: dùng DTO + `@Valid` và để lỗi validation của Spring MVC tới handler chung; bỏ nhánh Controller tự render lỗi qua `BindingResult`.
- Giữ route, form login, session, CSRF và quyền đã xác nhận. Không dùng JWT.
- Giữ nguyên schema, entity relationship, dữ liệu DB và cơ chế reset HMAC 15 phút. Đợt xử lý này không cần migration DB.
- Literal UI/thông báo viết bằng tiếng Anh; dữ liệu DB hiển thị nguyên văn.
- View lỗi/form tái sử dụng CSS và Fragment chung. Không tạo thêm nhiều layout gần giống nhau.

## 2. Trạng thái source mới nhất

| Thành phần | Trạng thái kiểm tra | Việc tiếp theo |
| --- | --- | --- |
| `user/exception/AccountFieldException.java` | **Đã kế thừa `BaseBusinessException`**, còn thuộc tính `field`. Constructor gọi `super(message)`. | Giữ phần kế thừa hiện tại. Thêm handler cụ thể cho lỗi theo field; chỉ bổ sung mã lỗi riêng nếu thật sự cần phân loại. |
| `core/exception/GlobalExceptionHandler.java` | Có handler 404, handler `BaseBusinessException`, handler `Exception`. Handler nghiệp vụ luôn chọn `error/500` và chưa đặt HTTP status. | Hoàn thiện phân loại lỗi, status và phản hồi HTML/JSON trước khi bỏ catch ở Controller. |
| `templates/auth/forgot-password.html` | Thông báo `param.sent` đã ghi recovery request **has been received**, không khẳng định email đã gửi thành công. | Giữ thông báo công khai chung này khi chuyển xử lý SMTP khỏi Controller. |
| Năm Controller trong phạm vi | Vẫn có tổng cộng bảy khối `try-catch`. | Sửa lần lượt theo các mục dưới đây. |
| `templates/error/404.html`, `500.html` | Đã tồn tại. | Giữ nơi đặt view. Khi dùng view chung cho status khác, tiêu đề/mã hiển thị phải đúng status; không hiển thị “500” cho lỗi 400/409/503. |

Ghi chú: nhận định trước đây rằng `AccountFieldException` chưa kế thừa `BaseBusinessException` đã không còn đúng với source vừa đọc. Tài liệu không yêu cầu thực hiện lại thay đổi đó.

## 3. Chuẩn bị GlobalExceptionHandler trước

**File:** `src/main/java/com/group2/rms/core/exception/GlobalExceptionHandler.java`.

### Cần sửa gì

| Loại lỗi | Phản hồi đề xuất | Lý do |
| --- | --- | --- |
| Validation DTO / lỗi field nghiệp vụ | Form tương ứng với lỗi theo field; HTTP 400. | Người dùng cần biết ô nào cần sửa; lỗi đầu vào không phải lỗi hệ thống 500. |
| Trùng username/email do ràng buộc UNIQUE đã xác định | Form với thông báo xung đột an toàn; HTTP 409. | Chặn cả trường hợp hai request đồng thời vượt qua bước kiểm tra tồn tại. |
| Không tìm thấy dữ liệu | Giữ handler `ResourceNotFoundException`, HTTP 404. | Đã có xử lý phù hợp. |
| Thiếu cấu hình password recovery | Thông báo chung không lộ cấu hình, HTTP 503; áp dụng trước tra cứu tài khoản. | Cùng điều kiện dịch vụ không khả dụng cho mọi email hợp lệ. |
| Lỗi gửi mail reset dự kiến | Cùng redirect/thông báo “đã nhận yêu cầu” như luồng thông thường; ghi nhận thất bại nội bộ bằng mã lỗi chung. | Tránh phân biệt email tồn tại và không tồn tại qua phản hồi SMTP. Đây không phải xác nhận gửi mail thành công. |
| Lỗi Health Check | JSON `{"status":"DOWN"}`, HTTP 503, `Cache-Control: no-store`. | API Monitoring phải nhận đúng định dạng và trạng thái thất bại. |
| Lỗi hệ thống không dự kiến | View lỗi chung, HTTP 500; message an toàn. | Không đưa SQL, credentials hoặc nội dung exception kỹ thuật lên UI. |

### Cách sửa theo bước

1. Giữ `@ControllerAdvice` hiện tại. Handler web trả `ModelAndView`; handler Health trả `ResponseEntity` JSON.
2. Thêm nhánh cụ thể cho `AccountFieldException`, validation của Spring MVC và các exception mới thực sự cần cho recovery/health.
3. Đặt HTTP status rõ ràng. Phân loại nhánh `BaseBusinessException`; không để mọi lỗi nghiệp vụ đi vào `error/500`.
4. Khi dựng lại form, chỉ nhận các field được phép của DTO tương ứng. Chọn view theo mapping route/method xác định ở server, không nhận tên template hoặc URL redirect tùy ý từ client.
5. Tạo model `form` và `BindingResult` tương ứng để `th:errors`/`#fields.hasErrors()` hoạt động. Các form Account còn cần `roles`, `departments`, `createMode`; Edit cần `accountId` và username lấy lại từ server.
6. Chỉ gọi Service đọc để bổ sung model; không gọi lại thao tác create/update/reset trong handler.
7. Xóa password/confirmPassword khỏi dữ liệu được render lại. Không đưa raw request hoặc `BindingResult` chứa password/token vào flash/session/log.
8. Giữ header không cache cho trang reset, kể cả khi handler trả lỗi. Không log nguyên URL reset có token.

Không đưa tên template, HTTP redirect hoặc DTO của giao diện vào exception ở Service chỉ để phục vụ render. Nếu cần view lỗi chung, dùng layout/Fragment chung với tiêu đề/status động thay vì copy nhiều trang lỗi.

### Lưu ý lỗi DB và transaction

- Kiểm tra UNIQUE ở Service giúp có lỗi theo field, nhưng vẫn cần ràng buộc UNIQUE của DB.
- Không gắn mọi `DataIntegrityViolationException` thành “username/email already in use”: lỗi còn có thể do FK, NOT NULL hoặc dữ liệu khác.
- Chỉ chuyển thành lỗi trùng account khi đã xác định đúng constraint liên quan. Lỗi không phân loại được giữ thông báo hệ thống chung, không lộ SQL.
- Nếu dịch exception DB tại Service, bảo đảm lỗi phát sinh trong phạm vi xử lý, ví dụ ở `flush` trong transaction. `save()` có thể chưa thực thi SQL; lỗi có thể phát sinh khi commit bên ngoài thân hàm.
- Ném tiếp exception phải làm transaction rollback. Không bắt exception rồi trả thành công.

## 4. AccountController.create()

**File:** `src/main/java/com/group2/rms/user/controller/AccountController.java`.
**Package/hàm:** `com.group2.rms.user.controller.AccountController.create()`.
**Vị trí:** `try` dòng 77; catch dòng 81/83; render form dòng 90.

**Lý do sửa:** Controller đang tự chuyển `AccountFieldException`/`DataIntegrityViolationException` thành lỗi UI, khiến handler chung không nhận được exception.

**Cách sửa:**

1. Giữ `CreateAccountRequest` và `@Valid`. Đưa kiểm tra Confirm Password sang validator DTO.
2. Nếu áp dụng validation tập trung, bỏ nhánh `BindingResult` tự render lỗi; chuẩn bị handler validation trước.
3. Giữ lời gọi `AccountManagementService.createInternal(form.toCommand())`, bỏ khối `try-catch` bao quanh.
4. Giữ kiểm tra username/email, Role, Department và password ở Service; ném exception phù hợp khi sai.
5. Chỉ thêm flash success và redirect `/admin/accounts` sau khi Service hoàn tất.
6. Handler dựng lại form khi có lỗi, tải lại Role/Department và không render lại password.

**Giữ nguyên nghiệp vụ:** chỉ internal role, Candidate có lifecycle riêng; account mới Active; password BCrypt; username/email duy nhất toàn hệ thống.

## 5. AccountController.update()

**File/package:** cùng `AccountController` ở mục 4.
**Hàm/vị trí:** `update()`, `try` dòng 114; catch dòng 118/120; render form dòng 125.

**Lý do sửa:** cùng một lỗi đang bị xử lý theo cách riêng ở Controller thay vì handler chung.

**Cách sửa:**

1. Giữ `UpdateAccountRequest` và validation đầu vào.
2. Giữ `AccountManagementService.updateInternal(userId, form.toCommand())`; bỏ catch và việc tự `rejectValue()/reject()` cho exception.
3. Service tiếp tục kiểm tra account tồn tại, đúng nhóm internal, Role/Department/Status hợp lệ và email unique loại trừ chính account đang sửa.
4. Handler lỗi Edit lấy username từ server, tạo lại model Edit và lỗi theo field; không tin username gửi từ client.
5. Chỉ thêm success và redirect khi cập nhật hoàn tất.

**Giữ nguyên nghiệp vụ:** username cố định; không thêm thay password vào Update; password hash không đổi; không cho chuyển Candidate/internal lifecycle.

## 6. RegistrationController.register()

**File:** `src/main/java/com/group2/rms/auth/controller/RegistrationController.java`.
**Package/hàm:** `com.group2.rms.auth.controller.RegistrationController.register()`.
**Vị trí:** `try` dòng 40; catch dòng 43/45; render lỗi dòng 51.

**Lý do sửa:** Controller tự hiển thị lỗi nghiệp vụ/DB; cách xử lý trùng với Create Account nhưng nằm ở nơi khác.

**Cách sửa:**

1. Giữ `RegisterAccountRequest`; chuyển Confirm Password sang validator DTO.
2. Controller gọi `CandidateRegistrationService.register(form.toCommand())` trực tiếp, không catch.
3. Service kiểm tra định danh, password, role Candidate và tạo User/Candidate trong cùng transaction.
4. Handler xử lý lỗi field hoặc UNIQUE, trả form đăng ký với dữ liệu an toàn và password trống.
5. Thành công mới redirect `/login?registered`.

**Giữ nguyên:** Username bắt buộc; account Candidate; User/Candidate không được tạo dở; không đổi schema.

## 7. PasswordRecoveryController.requestReset()

**File:** `src/main/java/com/group2/rms/auth/controller/PasswordRecoveryController.java`.
**Package/hàm:** `com.group2.rms.auth.controller.PasswordRecoveryController.requestReset()`.
**Vị trí:** kiểm tra cấu hình dòng 42; `try` gửi mail dòng 49; catch dòng 51; redirect dòng 56.

**Lý do sửa:** Controller điều phối cấu hình, sinh link, gửi mail và bắt exception SMTP. Thất bại bị nuốt tại Controller.

**Cách sửa:**

1. Đưa phần điều phối sang một Service auth nhỏ, đề xuất `PasswordRecoveryService.requestReset(String email)`.
2. Service mới gọi hai thành phần hiện có: `PasswordResetService.request()` và `PasswordResetEmailSender.send()`. Controller chỉ gọi Service điều phối.
3. Kiểm tra cấu hình signing key/SMTP trước khi tra cứu account. Thiếu cấu hình ném exception dịch vụ không khả dụng.
4. Email không tồn tại hoặc account không Active vẫn nhận phản hồi công khai chung và không gửi mail.
5. Khi SMTP lỗi dự kiến, adapter/Service ném exception delivery riêng kế thừa `BaseBusinessException`; GlobalExceptionHandler ghi mã lỗi chung và trả cùng phản hồi công khai như nhánh request hợp lệ.
6. Không bắt rộng `RuntimeException` để bỏ qua mọi lỗi. Lỗi không dự kiến cần được phân loại riêng tại handler chung.
7. Giữ thông báo hiện tại trong `auth/forgot-password.html`: recovery request đã được nhận. Không biến thông báo này thành xác nhận gửi thành công.

**Transaction:** gọi `PasswordResetService` qua bean hiện có để giữ hành vi `@Transactional` của nó. Không cần mở transaction DB bao quanh thời gian gửi SMTP.

**Bảo mật phản hồi:** không tạo một thông báo/status chỉ xuất hiện khi email có trong DB mà SMTP thất bại. Không log email, token, link reset, secret hay raw exception SMTP. Thất bại gửi phải được ghi nhận nội bộ dù UI giữ thông báo chung.

## 8. PasswordRecoveryController.reset()

**File/package:** cùng Controller ở mục 7.
**Hàm/vị trí:** `reset()`, `try` dòng 78; xử lý `false` dòng 82; catch dòng 83; render lỗi dòng 91.

**Lý do sửa:** lỗi token dùng boolean rồi tự render ở Controller, lỗi password dùng exception rồi cũng tự render ở Controller.

**Cách sửa:**

1. Giữ `ResetPasswordRequest`; chuyển Confirm Password sang validator DTO.
2. Service kiểm tra token; trường hợp không hợp lệ/hết hạn/đã dùng ném exception reset token riêng thay vì dùng `false` để Controller tự dựng lỗi.
3. Giữ validation password, BCrypt, transaction và khóa account hiện có khi reset.
4. Controller gọi Service trực tiếp; thành công mới redirect `/login?reset`.
5. Handler trả form reset với thông báo an toàn và `validToken` phù hợp, password trống, header `no-store` và `no-referrer`.
6. Rà dependency trước khi đổi kiểu trả về `reset()`: Controller và tests đang dùng kết quả boolean phải được cập nhật cùng đợt.

**Giữ nguyên:** HMAC 15 phút, không JWT, không bảng token mới. Link cũ phải mất hiệu lực sau khi đổi mật khẩu; lỗi token không được thay password hash.

## 9. HealthController.health()

**File:** `src/main/java/com/group2/rms/admin/controller/HealthController.java`.
**Package/hàm:** `com.group2.rms.admin.controller.HealthController.health()`.
**Vị trí:** `try` dòng 25; catch dòng 31.

**Lý do sửa:** Controller đang trực tiếp dùng JdbcTemplate, bắt lỗi DB và tự chuyển thành kết quả thất bại. Catch-all HTML hiện tại cũng không phù hợp endpoint JSON này.

**Cách sửa:**

1. Tách truy vấn `SELECT 1` sang Service admin, đề xuất `HealthCheckService.checkDatabase()`.
2. Service kiểm tra kết quả; DB lỗi hoặc kết quả không như mong đợi ném exception Health riêng, đề xuất `DatabaseHealthException` kế thừa `BaseBusinessException`.
3. Controller gọi Service, chỉ trả nhánh thành công: JSON `{"status":"UP"}`, HTTP 200, `Cache-Control: no-store`.
4. GlobalExceptionHandler có handler cụ thể cho exception Health: JSON `{"status":"DOWN"}`, HTTP 503, `Cache-Control: no-store`.
5. Giữ URL `/admin/api-monitoring/internal/health`, quyền System Admin và hợp đồng phản hồi mà API Monitoring dùng.

Không để exception Health rơi vào handler trả `error/500` HTML. Không trả tên DB/server, SQL, credentials hoặc message DataAccessException trong JSON.

## 10. TestDbController.testDatabaseConnection()

**File:** `src/main/java/com/group2/rms/demo/TestDbController.java`.
**Package/hàm:** `com.group2.rms.demo.TestDbController.testDatabaseConnection()`.
**Vị trí:** HTML text block từ dòng 22; `try` dòng 83; catch/render `e.getMessage()` dòng 101–102.

**Lý do sửa:** cùng một Controller chứa JDBC, HTML, CSS, bắt exception và render lỗi kỹ thuật. Đây là trang chẩn đoán, không phải màn hình nghiệp vụ.

**Cách sửa nếu vẫn giữ trang test:**

1. Giới hạn Controller bằng profile phát triển; không kích hoạt mặc định trong môi trường vận hành.
2. Chuyển query SQL version/edition/protocol/port sang Service chẩn đoán. Service trả DTO dữ liệu; không trả chuỗi HTML.
3. Chuyển HTML ra một template demo, dùng head/layout/Fragment và CSS chung. Chỉ tạo CSS riêng khi dữ liệu chẩn đoán thật sự cần style riêng.
4. Controller gọi Service, đặt DTO vào model và trả view thành công; không `try-catch`.
5. Exception đi tới handler chung. UI chỉ có thông báo an toàn, không hiển thị `e.getMessage()`.
6. Query SQL hiện tại là truy vấn chẩn đoán chỉ đọc; không thêm thao tác sửa DB để thử kết nối.

Nếu team không cần route chẩn đoán nữa, quyết định bỏ route phải được xác nhận và search dependency trước khi xóa. Tài liệu này chưa yêu cầu xóa Controller/template nào.

## 11. Danh sách file cần sửa / có thể cần tạo

Các file mới là đề xuất, chưa được tạo trong lượt lập tài liệu này.

| File trong `src/main/` | Hành động dự kiến | Lý do |
| --- | --- | --- |
| `java/com/group2/rms/core/exception/GlobalExceptionHandler.java` | Sửa | Tập trung lỗi, status, form model, JSON Health và recovery privacy. |
| `java/com/group2/rms/user/exception/AccountFieldException.java` | Giữ kế thừa hiện tại; sửa thêm chỉ nếu cần | Đã kế thừa BaseBusinessException; không làm lại thay đổi đã có. |
| `java/com/group2/rms/user/controller/AccountController.java` | Sửa Create/Update | Bỏ hai khối catch và render lỗi nghiệp vụ. |
| `java/com/group2/rms/auth/controller/RegistrationController.java` | Sửa Register | Bỏ catch, dùng handler chung. |
| `java/com/group2/rms/auth/controller/PasswordRecoveryController.java` | Sửa Request/Reset | Chuyển điều phối SMTP và xử lý lỗi sang Service/handler. |
| `java/com/group2/rms/user/dto/CreateAccountRequest.java` | Sửa validation nếu cần | Kiểm tra Confirm Password ở DTO. |
| `java/com/group2/rms/auth/dto/RegisterAccountRequest.java`, `ResetPasswordRequest.java` | Sửa validation nếu cần | Dùng một cách kiểm tra password confirmation thống nhất. |
| Validator password confirmation trong feature phù hợp | Có thể tạo/tái sử dụng | Chỉ thêm nếu chưa có validator tương ứng; không ép đổi toàn bộ DTO sang record. |
| `java/com/group2/rms/auth/service/PasswordRecoveryService.java` | Đề xuất tạo | Chuyển điều phối hai thành phần recovery hiện có khỏi Controller. |
| `java/com/group2/rms/auth/service/PasswordResetService.java` | Sửa nhánh reset thất bại | Ném exception token rõ ràng, giữ HMAC/transaction hiện có. |
| `java/com/group2/rms/auth/service/PasswordResetEmailSender.java` | Sửa cách ném lỗi | Chuyển lỗi cấu hình/delivery dự kiến thành exception phù hợp. |
| Exception recovery trong `java/com/group2/rms/auth/exception/` | Đề xuất tạo theo nhu cầu thực tế | Phân biệt cấu hình, SMTP delivery, token invalid. |
| `java/com/group2/rms/admin/controller/HealthController.java` | Sửa | Controller chỉ trả nhánh UP. |
| `java/com/group2/rms/admin/service/HealthCheckService.java` | Đề xuất tạo | Chứa kiểm tra DB. |
| `java/com/group2/rms/admin/exception/DatabaseHealthException.java` | Đề xuất tạo | Handler nhận đúng lỗi để trả DOWN/503 JSON. |
| `java/com/group2/rms/demo/TestDbController.java` | Sửa nếu giữ route | Tách JDBC/HTML và giới hạn môi trường phát triển. |
| Service chẩn đoán + template demo | Đề xuất tạo nếu giữ trang test | Phân tách dữ liệu và view; tái sử dụng UI chung. |
| Tests liên quan các flow trên | Cập nhật/bổ sung | Kiểm chứng status, rollback, UI binding, privacy và hợp đồng JSON. |

`AccountManagementService` và `CandidateRegistrationService` đang có validation/transaction hữu ích: giữ phần còn đúng. Chỉ sửa thêm nếu cần chuẩn hóa exception hoặc xử lý lỗi UNIQUE tại transaction boundary; không viết lại hai Service.

## 12. Thứ tự thực hiện để không làm hỏng flow

1. Ghi nhận trạng thái build/test trước thay đổi và dependency của các hàm sẽ đổi signature.
2. Hoàn thiện handler validation, field error, UNIQUE và model form; kiểm tra trước khi bỏ catch.
3. Sửa Create Account, compile và kiểm tra flow Create.
4. Sửa Update Account, compile và kiểm tra flow Update.
5. Sửa Register, compile và kiểm tra transaction User/Candidate.
6. Sửa Forgot/Reset Password, compile và kiểm tra privacy/token/password.
7. Sửa Health Check, compile và kiểm tra JSON 200/503 cùng API Monitoring.
8. Xử lý TestDbController theo quyết định giữ route test.
9. Search lại Controller, chạy bộ test liên quan và full Maven test khi baseline cho phép.
10. Cập nhật trạng thái từng mục và kết quả thực tế trong phần nhật ký cuối tài liệu.

Không chuyển toàn bộ catch sang handler chung nếu handler chưa biết dựng đúng form hoặc chưa phân biệt phản hồi JSON. Không bỏ/comment test để che lỗi baseline hoặc lỗi refactor.

## 13. Kịch bản test từng bước sau khi triển khai

**Hiện tại: tất cả kịch bản bên dưới NOT RUN.** Đây là hướng dẫn kiểm thử tương lai, không phải bằng chứng PASS.

### Chuẩn bị

1. Dùng môi trường test riêng, DB/schema hiện có. Không chạy script drop/create/migration để thử handler.
2. Khởi động ứng dụng; dùng tài khoản System Admin test có sẵn cho Account/Health và Guest cho Register/Forgot.
3. Mở DevTools > Network, bật Preserve log để xem status POST trước redirect và JSON Health.
4. Tạo định danh test duy nhất, ví dụ username `arch-test-<timestamp>`, email test do người kiểm thử quản lý. Không ghi password/token/SMTP secret vào tài liệu kết quả.
5. Gửi POST qua form để giữ CSRF. Các ca gây lỗi SMTP/DB chỉ dùng môi trường test hoặc test adapter, không gây gián đoạn hệ thống vận hành.

### A. Create Account — TC01 đến TC03

1. Mở Internal Accounts > Create. Nhập đầy đủ dữ liệu hợp lệ, internal Role/Department và password confirmation đúng; Submit. **TC01:** redirect về list, success chỉ sau khi lưu thành công, User Active và password được hash.
2. Tạo lại với username hoặc email đã tồn tại. **TC02:** lỗi đúng field/xung đột, HTTP 400 hoặc 409 theo nguồn lỗi đã phân loại; không phải 500, không có User mới và không lộ SQL.
3. Nhập Confirm Password sai rồi Submit. **TC03:** validation được handler xử lý, password input trống sau lỗi, Role/Department vẫn có; CSRF vẫn hợp lệ khi sửa và gửi lại.

### B. Update Account — TC04 đến TC06

1. Edit account test, giữ nguyên email, sửa Full Name; Save. **TC04:** thành công, không báo trùng chính account; username/password hash không đổi.
2. Đổi email sang email của account khác; Save. **TC05:** HTTP 400/409 và lỗi email; dữ liệu cũ không bị ghi một phần.
3. Với test HTTP có CSRF, gửi Role Candidate vào endpoint Update Internal. **TC06:** Service từ chối, handler trả lỗi an toàn; role/lifecycle cũ được giữ.

### C. Register — TC07 đến TC09

1. Guest mở Register, nhập username/email mới và password hợp lệ; Submit. **TC07:** redirect login, User role Candidate và Candidate profile liên kết đúng.
2. Đăng ký với email/username đã có. **TC08:** lỗi tập trung, không tạo thêm User/Candidate.
3. Trong integration test, mô phỏng lỗi lưu Candidate sau khi đã lưu User. **TC09:** transaction rollback cả hai; không để User mồ côi. Không phá schema để tạo ca này.

### D. Forgot Password — TC10 đến TC13

1. Cấu hình SMTP test và signing key hợp lệ. Gửi email account Active test. **TC10:** nhận thông báo chung; mail thực đến hộp thư test, link reset có hạn theo cơ chế hiện tại.
2. Gửi email không tồn tại rồi email account Inactive/Blocked. **TC11:** cùng status/redirect/thông báo công khai như request hợp lệ; không gửi mail cho account không đủ điều kiện.
3. Dùng SMTP test adapter trả lỗi gửi, gửi email Active test. **TC12:** vẫn giữ phản hồi công khai chung; có ghi nhận delivery thất bại nội bộ, không khẳng định gửi thành công; không lộ email/token/secret/raw SMTP exception.
4. Khởi động test instance thiếu cấu hình recovery. Thử cả email có và không có account. **TC13:** cùng phản hồi không khả dụng 503 cho mọi email hợp lệ; không lộ cấu hình cụ thể.

### E. Reset Password — TC14 đến TC17

1. Mở link hợp lệ, nhập password mới và confirmation đúng. **TC14:** redirect login; password mới đăng nhập được, password cũ không được.
2. Dùng lại link sau reset. **TC15:** lỗi token an toàn, không đổi password lần nữa.
3. Trong test dùng Clock kiểm soát được, tạo token đã quá 15 phút; thêm ca token bị sửa chữ ký. **TC16:** bị từ chối, không ghi password hash, không 500 do lỗi input.
4. Nhập confirmation sai hoặc password ngoài 8–32 ký tự. **TC17:** lỗi validation/field, password không được lưu; phản hồi lỗi giữ `Cache-Control: no-store`, `Referrer-Policy: no-referrer` và không render lại password.

### F. Health và API Monitoring — TC18 đến TC20

1. Admin chạy probe Internal API khi DB test hoạt động. **TC18:** endpoint Health trả HTTP 200, JSON `{"status":"UP"}`, no-store; monitoring báo thành công theo phản hồi thật.
2. Trong integration test hoặc test instance riêng, mô phỏng query DB thất bại. **TC19:** endpoint trả HTTP 503, JSON `{"status":"DOWN"}`, no-store; không trả HTML/SQL details, monitoring ghi thất bại.
3. Guest và tài khoản không phải System Admin truy cập endpoint. **TC20:** quyền truy cập vẫn bị chặn theo SecurityConfig; không bị thay đổi bởi refactor handler.

### G. Test DB — TC21 đến TC22

1. Nếu giữ route, chạy instance với profile phát triển rồi mở trang test. **TC21:** dữ liệu thành công qua Service/DTO/template; lỗi DB đi qua handler và không có raw exception trên UI.
2. Chạy instance không bật profile phát triển. **TC22:** Controller chẩn đoán không được đăng ký; không dựa riêng vào một response 404/403 để kết luận profile đúng, cần kiểm tra mapping/bean trong test.

### H. Handler / regression — TC23 đến TC25

1. Cho hai request test tạo cùng một định danh tại thời điểm gần nhau. **TC23:** chỉ một transaction thành công; request thua nhận lỗi UNIQUE an toàn, không success giả.
2. Search `try`/`catch` trong năm Controller; xem lại các nhánh render lỗi. **TC24:** bảy khối catch trong phạm vi đã bỏ; exception đi tới handler đúng và form submit lại được sau lỗi.
3. Run compile và tests. **TC25:** ghi exit code, lỗi thực tế và kết quả từng ca. Test Career hiện còn tham chiếu `CareerService` cũ và UI cũ; nếu full suite bị lỗi baseline này, ghi rõ riêng, không coi targeted test PASS là full suite PASS.

Lệnh Maven dùng sau khi triển khai, từ project root: `mvn.cmd -DskipTests compile`, sau đó `mvn.cmd test`. Tài liệu này không chạy hai lệnh đó. Nếu full suite chưa chạy được, vẫn cần test tập trung các Controller/handler với Spring MVC và kiểm tra HTTP/browser/SMTP thật theo phạm vi có thể thực hiện; ghi rõ giới hạn.

## 14. Nhật ký và điểm nối tiếp

### 03/10/2026 — Lập tài liệu

- Đọc lại năm Controller, handler chung, AccountFieldException và các Service recovery/account liên quan.
- Ghi nhận AccountFieldException đã kế thừa BaseBusinessException; bảy Controller catch vẫn còn.
- Ghi nhận Forgot Password UI đã dùng thông báo “đã nhận yêu cầu”.
- Tạo duy nhất file tài liệu này trong `docs/members/linhdn/update architecture/`.
- Chưa triển khai refactor; chưa chạy Maven, Spring startup, browser hoặc SMTP tests.
- Điểm bắt đầu lần tiếp theo: mục 3, hoàn thiện handler và model form trước khi sửa Controller.

### Mẫu ghi tiếp sau mỗi nhóm thay đổi

- Ngày/người thực hiện:
- Mục đã làm / file và hàm đã sửa:
- Cách sửa và lý do:
- Phần nghiệp vụ được giữ:
- Compile/tests/HTTP/browser đã chạy; kết quả và lỗi thực tế:
- Mục chưa làm và điểm nối tiếp:

### Trạng thái verification của lượt lập tài liệu

| Hạng mục | Trạng thái |
| --- | --- |
| Refactor code ứng dụng | CHƯA THỰC HIỆN |
| Build / Tests / Startup / Browser / SMTP | NOT RUN |
| Thay đổi schema/dữ liệu | KHÔNG THỰC HIỆN |
| Kết quả TC01–TC25 | NOT RUN |


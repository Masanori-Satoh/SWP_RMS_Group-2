# Kịch bản kiểm thử sau khi đồng bộ schema

Cập nhật: 2026-09-30. Nguồn chuẩn của database là `database/schema/db.sql`.
Tài liệu này phân biệt **đã kiểm thử tự động** và **bước cần chạy thủ công**.
Trên checkout hiện tại, `mvn.cmd -q test` PASS 36 test/10 suite; Spring Context,
SQL Server và JPA `validate` PASS. HTTP startup vẫn FAIL ở Tomcat loopback
socket trên cổng 18083 (Maven trả exit 0 dù log báo lỗi), nên chưa xác nhận
browser/SMTP/mailbox E2E. Biên bản ở [`docs/tests/`](tests/README.md); cách
thiết lập riêng cho Forgot/Reset tại
[`docs/testing/password-reset-test-setup.md`](testing/password-reset-test-setup.md).

## 1. Chuẩn bị

1. Mở project tại thư mục chứa `pom.xml`. Cần JDK 21, Maven và SQL Server.
2. Dùng database `RitirementManagement2` theo `database/schema/db.sql`. Bộ test
   database hiện cần dữ liệu seed (ít nhất các Role và JobPosting). Kiểm tra
   `src/main/resources/application.properties` trên máy: JDBC URL trỏ đúng DB,
   `spring.jpa.hibernate.ddl-auto=validate`, và cổng chạy web (hiện là `8082`).
   File này bị Git ignore; không chép mật khẩu vào tài liệu hoặc commit.
3. **Không chạy lại** `database/schema/db.sql` trên DB đã có dữ liệu: đầu script
   có lệnh xóa/tạo lại database. Không dùng
   `database/migrations/001_candidate_account_link.sql` cho schema mới.
4. Chuẩn bị tài khoản test còn Active cho từng role. Mật khẩu lấy qua kênh nội bộ;
   tài liệu này không chứa credential. Các bước tạo/sửa/xóa bên dưới chỉ dùng
   tài khoản thử nghiệm và nên thực hiện trên DB có thể khôi phục.

## 2. Test tự động: chạy trước khi thử giao diện

Mở PowerShell ở thư mục project:

```powershell
mvn.cmd test
mvn.cmd -DskipTests package
```

Nếu Maven trên máy cần chỉ định local repository, dùng cấu hình Maven của máy;
lần kiểm tra ngày 2026-09-29 đã chạy với
`-Dmaven.repo.local=C:\Users\Administrator\.m2\repository`.

| Bước | Kiểm tra | Kết quả mong đợi | Bằng chứng |
| --- | --- | --- | --- |
| A01 | `mvn.cmd test` | Exit code 0, mọi test PASS | `target/surefire-reports/*.txt` |
| A02 | `mvn.cmd -DskipTests package` | Exit code 0, có `target/rms-0.0.1-SNAPSHOT.jar` | File JAR và Maven output |
| A03 | Spring Context + JPA `validate` | Không có lỗi khởi động, thiếu bảng/cột hoặc query | `RmsApplicationTests`, test database |
| A04 | Candidate/User 1–1, phone tùy chọn, mật khẩu hash | Tạo account và profile trong cùng giao dịch, không có dữ liệu liên hệ lặp trên Candidate | `AccountManagementDatabaseTests`, `AccountManagementServiceTests` |
| A05 | Dashboard theo role, phạm vi Candidate, HR review | Giá trị lấy từ DB và theo đúng user/role | `DashboardDatabaseTests` |
| A06 | Chấm AI nhiều lần | Hai `AIScreeningResult` cùng một `ApplicationId` được lưu thành công | `DashboardDatabaseTests.applicationCanKeepMultipleAiScreeningResults` |
| A07 | Login, quyền Admin, CSRF, form và Thymeleaf | Guest bị chuyển đến login; role không hợp lệ bị chặn; form render | `SecurityFlowTests` |
| A08 | Account list, soft delete, API probe | Giữ User record khi deactivate; probe chỉ dùng kết quả phản hồi thật | `AccountList*Tests`, `ApiMonitoringServiceTests` |
| A09 | Register Candidate | User và Candidate được tạo cùng giao dịch, Active, BCrypt; form và CSRF đúng | `AuthenticationDatabaseTests`, `SecurityFlowTests` — PASS trong 36 test |
| A10 | Forgot/Verify Email/Reset | Token HMAC hết hạn 15 phút, không dùng lại sau đổi hash, account Inactive bị chặn; form và CSRF đúng | `AuthenticationDatabaseTests`, `SecurityFlowTests` — PASS tự động; SMTP E2E chưa chạy |
| A11 | Binding mail local | Biến `SPRING_MAIL_*` map đúng và Spring tạo `JavaMailSender` khi có host; không gửi email | `MailEnvironmentBindingTests` — 2 test PASS |

Rà tên field cũ trong mã đang chạy:

```powershell
rg -n -i 'ResumeId|resume_id|HRReviewNotes|HMReviewNotes|ReviewedBy|ProposedPosition|ProbationDays|MatchedSkills|UnmatchedSkills' src/main src/test
```

Kết quả mong đợi: không có dòng khớp. `rg` trả exit code 1 khi không tìm thấy
chuỗi; đây là kết quả bình thường của bước này. Tài liệu lịch sử có thể còn
nhắc tới schema cũ nhưng phải ghi rõ không áp dụng.

## 3. Kiểm tra database chỉ đọc

Chạy trong SSMS, không sửa dữ liệu:

```sql
USE RitirementManagement2;
SELECT COUNT(*) AS TableCount
FROM sys.tables
WHERE schema_id = SCHEMA_ID(N'dbo');

SELECT c.name AS ColumnName, c.is_nullable AS IsNullable
FROM sys.columns AS c
JOIN sys.tables AS t ON t.object_id = c.object_id
WHERE t.name = N'Candidate' AND c.name = N'UserId';
```

Kết quả mong đợi: 20 bảng; `Candidate.UserId` có `IsNullable = 0`.
Hibernate `validate` và test A04/A06 kiểm tra thêm mapping và khả năng ghi dữ
liệu theo ràng buộc thực tế. Chỉ đọc metadata không chứng minh toàn bộ CHECK/FK.

## 4. Test thủ công qua giao diện

Kịch bản từng bước cho sáu màn hình Login, Register, Dashboard, Account List,
Forgot/Reset và Create/Update nằm trong
[`docs/tests/2026-09-29-auth-screens.md`](tests/2026-09-29-auth-screens.md).
Forgot/Reset cần SMTP và `APP_PASSWORD_RESET_SECRET` từ môi trường, **không dùng
JWT và không thêm bảng**. Nếu thiếu cấu hình, ghi BLOCKED cho email end-to-end.

Chạy `mvn.cmd spring-boot:run`, chờ thông báo ứng dụng đã khởi động. Mở
`http://localhost:8082/` nếu cấu hình vẫn dùng cổng 8082. Sau bước mở trang
đầu, dùng các nút/liên kết hiển thị để điều hướng. Nếu server không khởi động
hoặc không nhận HTTP, ghi **BLOCKED**; không đổi trạng thái thành PASS chỉ vì
`@SpringBootTest` đã chạy.

| ID | Thao tác từng bước | Kết quả mong đợi |
| --- | --- | --- |
| M01 Guest | 1. Mở trang chủ khi chưa đăng nhập. 2. Thử mở Dashboard và Account List. | Chuyển đến trang đăng nhập; không thấy dữ liệu bảo vệ. |
| M02 Admin | 1. Đăng nhập bằng account System Admin Active. 2. Vào Dashboard. 3. Bấm **Quản lý tài khoản**. | Dashboard có tổng số User và Active/Inactive/Blocked; Account List có cả role Candidate. |
| M03 Phân quyền | 1. Đăng xuất. 2. Đăng nhập bằng HR Active. 3. Mở Dashboard. 4. Kiểm tra từ chối quyền bằng cách mở `/admin/accounts` và `/admin/api-monitoring`. | Dashboard mở; hai trang Admin bị từ chối. Lặp lại Dashboard với Hiring Manager, Director, Interviewer và Candidate Active. |
| M04 Tạo Candidate | 1. Admin bấm **Tạo tài khoản**. 2. Nhập họ tên, username/email mới, role Candidate, mật khẩu 8–32 ký tự và xác nhận đúng. 3. Để trống phone và department. 4. Bấm **Tạo tài khoản**. | Hiện “Tạo tài khoản thành công.”; có dòng Candidate trạng thái Active. Kiểm tra SQL bên dưới: một User và một Candidate liên kết cùng `UserId`. |
| M05 Validation | 1. Thử tạo account với username hoặc email đã tồn tại. 2. Thử mật khẩu xác nhận khác mật khẩu. 3. Thử role nội bộ nhưng không chọn department. | Form báo lỗi phù hợp; không tạo thêm account. |
| M06 Sửa account | 1. Tại Account List bấm **Sửa** account test. 2. Đổi họ tên/email hợp lệ. 3. Lưu. 4. Đăng xuất và đăng nhập lại bằng username cùng mật khẩu cũ. | Hiện “Cập nhật tài khoản thành công.”; username chỉ đọc, không có ô mật khẩu ở form sửa; đăng nhập bằng mật khẩu cũ vẫn được. |
| M07 Xóa = deactivate | 1. Ở dòng account test bấm **Xóa**. 2. Bấm **Hủy**, kiểm tra trạng thái chưa đổi. 3. Bấm **Xóa** lại và chọn **Vô hiệu hóa**. | Có modal xác nhận; sau xác nhận hiển thị “Vô hiệu hóa tài khoản thành công.”; User và lịch sử liên quan vẫn còn, status = Inactive; account không đăng nhập được. |
| M08 Blocked | 1. Admin sửa một account test khác sang Blocked. 2. Đăng xuất và thử đăng nhập account đó. | Đăng nhập thất bại; tài khoản không vào được Dashboard. |
| M09 API Monitoring | 1. Admin mở **API Monitoring**. 2. Ở hàng nội bộ bấm **Gửi GET**. 3. Xem phản hồi và thống kê. | Khi app/DB khỏe: hiển thị HTTP 200, thời gian thật và trạng thái thành công; nếu lỗi thì báo thất bại thật. Hàng AI/email ghi chưa cấu hình, không có nút thử giả. |
| M10 Dashboard Candidate | 1. Đăng nhập bằng hai Candidate test có dữ liệu ứng tuyển khác nhau. 2. So sánh “Đơn ứng tuyển của tôi” và trạng thái đơn với câu SQL bên dưới. | Mỗi account chỉ thấy số liệu gắn với `Candidate.UserId` của mình; không thấy ứng tuyển của account kia. Nếu chưa có fixture phù hợp, ghi **BLOCKED** và dùng A05 làm bằng chứng tự động. |

Kiểm tra M04/M07 và số đơn của M10 bằng truy vấn **chỉ đọc**, thay placeholder
bằng username vừa dùng; không chọn `PasswordHash`:

```sql
USE RitirementManagement2;
DECLARE @Username NVARCHAR(50) = N'<username_test>';

SELECT u.UserId, u.Username, u.Email, u.AccountStatus,
       c.CandidateId, c.UserId AS CandidateUserId
FROM dbo.[User] AS u
LEFT JOIN dbo.Candidate AS c ON c.UserId = u.UserId
WHERE u.Username = @Username;

SELECT COUNT(*) AS OwnApplications
FROM dbo.Application AS a
JOIN dbo.Candidate AS c ON c.CandidateId = a.CandidateId
JOIN dbo.[User] AS u ON u.UserId = c.UserId
WHERE u.Username = @Username;
```

Sau khi đổi email Candidate, kiểm tra `dbo.[User].Email` bằng truy vấn trên.
Schema mới không có `Candidate.Email` để đồng bộ riêng. Không dùng các account
thật cho M07/M08 vì hai bước đó chủ ý vô hiệu hóa đăng nhập.

## 5. Phạm vi chưa chốt

- Chưa test nghiệp vụ nộp `InterviewEvaluation` trùng hoặc từ người ngoài panel:
  schema cho phép nhưng quy tắc nghiệp vụ đang chờ xác nhận. Xem
  `docs/schema-migration-impact.md` trước khi bổ sung test hay ràng buộc.
- AI Configuration form chưa triển khai vì chưa duyệt catalogue khóa/kiểu/range.
  API Monitoring chỉ kiểm tra endpoint nội bộ; AI/email hiện không có target
  health check được xác nhận.
- Thao tác trình duyệt M01–M10 chưa được chạy trong lượt đồng bộ schema. Kết quả
  tự động không thay thế chứng cứ browser E2E.

## 6. Ghi kết quả sau mỗi lần chạy

Điền một bản ghi trong `docs/WORK_LOG.md`: ngày, DB/môi trường, mã test đã chạy,
PASS/FAIL/BLOCKED, lỗi thực tế và file bằng chứng. Phân biệt rõ kết quả Maven,
Spring Context/JPA với thao tác HTTP/browser. Không ghi username/mật khẩu thật,
connection string có credential hoặc giá trị secret vào log.

Khi có tính năng mới, thêm mã test vào tài liệu này với: điều kiện đầu vào,
từng thao tác/lệnh, kết quả mong đợi, nơi xem bằng chứng và trạng thái đã chạy.

## 7. Tài liệu trace và case chi tiết theo từng flow

Từ 30/09/2026, dùng [`docs/flows/README.md`](flows/README.md) để tìm trace
class/method/package từ browser đến DB/response. Danh mục 45 case có liên kết
tới từng procedure ở [`docs/tests/2026-09-30-flow-cases.md`](tests/2026-09-30-flow-cases.md).
Các case ngoài hệ thống hiện chưa chạy; kết quả `mvn test` không được ghi thay
cho các bước browser/SMTP/probe HTTP.

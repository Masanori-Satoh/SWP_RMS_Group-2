# Kiểm thử sáu màn hình xác thực và tài khoản — 2026-09-29

## Quyết định và phạm vi

- Dùng Spring Security form login, session, BCrypt và CSRF; **không dùng JWT**.
- Giữ nguyên `database/schema/db.sql`: 20 bảng. Không có bảng reset token.
- Candidate tự đăng ký phải nhập Username duy nhất, theo quyết định của chủ dự án.
- Forgot/Verify Email dùng link ký HMAC, hết hạn sau 15 phút. Chữ ký gắn với
  email và BCrypt hash hiện tại. Đổi mật khẩu làm mọi link cũ mất hiệu lực.
- SMTP và khóa ký chỉ lấy từ biến môi trường của máy chạy, không ghi giá trị vào
  tài liệu/Git. Chưa có SMTP thật thì không thể xác nhận email E2E.

## Chuẩn bị

1. Dùng DB thử nghiệm `RitirementManagement2` với seed có role `Candidate` và
   tài khoản System Admin. **Không chạy lại** `database/schema/db.sql` trên DB
   đang có dữ liệu vì đầu script xóa/tạo DB.
2. Kiểm tra `src/main/resources/application.properties` local trỏ đúng DB,
   `spring.jpa.hibernate.ddl-auto=validate`. File này bị Git ignore. Không ghi
   credential vào log hoặc ảnh chụp màn hình.
3. Cho luồng Forgot/Reset, cấu hình `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`,
   `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `APP_MAIL_FROM`,
   `APP_PUBLIC_BASE_URL`, `APP_PASSWORD_RESET_SECRET` trong môi trường/IDE.
   Khóa ký cần ít nhất 32 byte ngẫu nhiên và phải giữ ổn định qua lần chạy
   server; thay khóa sẽ vô hiệu các link cũ. `.env.example` chỉ là danh sách tên
   biến, Spring Boot không tự đọc file `.env`.
4. Chạy `mvn.cmd spring-boot:run`. Cổng hiện được cấu hình local là 8082; nếu
   máy khác dùng cổng khác, thay URL tương ứng. Chuẩn bị email test mà bạn kiểm
   soát và ít nhất một account Active thử nghiệm. Không dùng account thật để
   thử vô hiệu hóa.
5. Mỗi kịch bản ghi `PASS`, `FAIL` hoặc `BLOCKED` cùng thời điểm, môi trường và
   lỗi thực tế. Không đưa URL reset có token vào log/tài liệu.

## Các bước kiểm thử thủ công

### AS01 — Login / Log In

1. Trong cửa sổ riêng tư chưa đăng nhập, mở `http://localhost:8082/`.
2. Xác nhận được chuyển tới Login; thấy liên kết **Đăng ký tài khoản Candidate**
   và **Quên mật khẩu?**.
3. Đăng nhập bằng account Active hợp lệ, rồi mở Dashboard bằng liên kết/giao
   diện. Xác nhận đúng nội dung theo role.
4. Đăng xuất, thử mật khẩu sai; phải ở Login và thấy thông báo lỗi. Thử account
   Inactive hoặc Blocked; cũng không được vào Dashboard.
5. Gửi POST `/login` không có CSRF bằng công cụ HTTP; kỳ vọng HTTP 403. Không
   cần nhập token CSRF bằng tay khi dùng form trình duyệt.

### AS02 — Register / Register Account

1. Từ Login bấm **Đăng ký tài khoản Candidate**.
2. Nhập họ tên, Username mới, email mới, mật khẩu 8–32 ký tự, xác nhận trùng;
   bấm **Đăng ký**.
3. Kỳ vọng về Login với thông báo tạo tài khoản thành công. Đăng nhập bằng
   Username hoặc email vừa tạo và mật khẩu vừa chọn; Dashboard Candidate mở.
4. Trong SSMS, dùng truy vấn chỉ đọc bên dưới: phải có đúng một User role
   Candidate, trạng thái Active, và một Candidate nối bằng `UserId`. Không xem
   hoặc chụp `PasswordHash`; test tự động kiểm tra BCrypt.
5. Thử lại với Username trùng, email trùng, xác nhận khác, mật khẩu dưới 8 hoặc
   trên 32 ký tự. Form phải báo lỗi, không có User/Candidate tạo dở.

### AS03 — Dashboard theo role

1. Đăng nhập lần lượt bằng System Admin, HR, Hiring Manager, Director,
   Interviewer, Candidate Active.
2. Mỗi role mở được Dashboard và chỉ thấy widget được phân quyền. Candidate
   chỉ thấy số liệu đơn của chính mình. Guest thử mở Dashboard phải về Login.
3. Không kết luận widget có số 0 là lỗi nếu DB thực tế không có dữ liệu tương ứng;
   so sánh với truy vấn DB/test fixture trong `docs/TEST_PLAN.md`.

### AS04 — Account List / View Account

1. Đăng nhập System Admin, từ Dashboard bấm **Quản lý tài khoản**.
2. Tìm/lọc role Candidate, kiểm tra account ở AS02 xuất hiện cùng trạng thái
   Active; tìm/sắp xếp/phân trang thử bằng dữ liệu seed.
3. Đăng xuất rồi vào bằng HR hoặc Candidate; mở Account List trực tiếp phải bị
   từ chối. Trang không được hiện password hash.

### AS05 — Forgot Password / Verify Email / Reset Password

1. Từ Login bấm **Quên mật khẩu?**. Nhập email Active ở AS02, bấm **Gửi liên
   kết xác minh**. Ghi thời điểm yêu cầu và kiểm tra hộp thư test nhận email
   chứa link đúng domain `APP_PUBLIC_BASE_URL`. Không chép link vào biên bản.
2. Gửi lại form với email chưa tồn tại. Giao diện phải trả cùng thông báo chung;
   hộp thư không nhận thêm email cho địa chỉ không có account.
3. Mở link từ email Active trong vòng 15 phút. Form đặt mật khẩu mới xuất hiện;
   việc mở link là bước xác minh quyền truy cập email. Nhập mật khẩu mới 8–32
   ký tự và xác nhận trùng, bấm lưu. Kỳ vọng về Login với thông báo thành công.
4. Đăng nhập bằng mật khẩu cũ phải thất bại; mật khẩu mới thành công. Mở lại
   cùng link phải hiện không hợp lệ/đã dùng. Không đưa token vào log.
5. Yêu cầu một link khác và đợi quá 15 phút rồi mở: phải hết hạn. Link cho
   account vừa chuyển Inactive/Blocked cũng không được dùng.
6. Thử không cấu hình SMTP hoặc khóa HMAC: form phải báo dịch vụ chưa cấu hình,
   không tạo link có thể gửi. Thử POST không có CSRF: HTTP 403.

**Giới hạn thiết kế:** Không có bảng token nên không thể thu hồi riêng một link
chưa dùng mà vẫn giữ nguyên email, mật khẩu và khóa ký. Thay email, mật khẩu,
trạng thái account hoặc khóa ký sẽ vô hiệu link. Không có giới hạn gửi lại bền
vững qua nhiều máy chủ trong phiên bản này; cần quyết định riêng nếu nghiệp vụ
yêu cầu chống spam ở mức đó.

### AS06 — Create/Update Account

1. Admin từ Account List bấm **Tạo tài khoản**; nhập dữ liệu hợp lệ, bấm lưu;
   kiểm tra thông báo thành công và dòng mới. Candidate được phép không có
   department; role nội bộ phải có department.
2. Bấm **Sửa** account test. Username chỉ đọc, không có ô thay mật khẩu. Đổi
   họ tên/email hợp lệ, role/department/status khi phù hợp; lưu. Kiểm tra thông
   báo thành công. Đăng nhập lại với Username và mật khẩu cũ nếu account Active.
3. Thử đổi email thành email đã thuộc account khác; form phải báo trùng. Giữ
   email cũ của chính account phải lưu được.
4. Tại Account List bấm **Xóa**, thử Hủy rồi xác nhận lần thứ hai. Hủy không đổi
   trạng thái; xác nhận chuyển sang Inactive, giữ User và lịch sử. Tài khoản đó
   không còn đăng nhập được.

## Truy vấn DB chỉ đọc cho AS02/AS04/AS06

```sql
USE RitirementManagement2;
DECLARE @Username NVARCHAR(50) = N'<username_test>';

SELECT u.UserId, u.Username, u.Email, u.AccountStatus,
       r.RoleName, c.CandidateId, c.UserId AS CandidateUserId
FROM dbo.[User] AS u
JOIN dbo.[Role] AS r ON r.RoleId = u.RoleId
LEFT JOIN dbo.Candidate AS c ON c.UserId = u.UserId
WHERE u.Username = @Username;
```

## Bằng chứng thực tế của đợt này

| Kiểm tra | Kết quả | Giới hạn |
| --- | --- | --- |
| `git diff --exit-code -- database/schema/db.sql` | PASS, không có diff | Kiểm tra file schema trong source. |
| SQL `sys.tables` trên `RitirementManagement2` | PASS, 20 bảng, không có `PasswordResetToken` | Chỉ kiểm tra metadata DB local. |
| `mvn.cmd -q -DskipTests compile` | PASS, exit 0 | Không compile/chạy test. |
| `mvn.cmd -q -DskipTests test-compile` | PASS, exit 0 | Compile test, không chạy test. |
| `mvn.cmd -q -DskipTests spring-boot:run` | FAIL khi mở Tomcat: `Unable to establish loopback connection`, `Invalid argument: connect` | JPA/Repository khởi tạo và kết nối DB trước lỗi; không có HTTP/browser proof. Maven trả exit 0 dù ứng dụng báo lỗi. |
| `mvn.cmd test` cho thay đổi đợt này | BLOCKED: yêu cầu quyền chạy Maven test đã bị từ chối | Không được gọi kết quả 28 test cũ là bằng chứng của mã mới. |
| Browser/SMTP end-to-end AS01–AS06 | CHƯA CHẠY | Cần server, SMTP thật và thao tác kiểm thử. |

Các test mới trong `AuthenticationDatabaseTests` và `SecurityFlowTests` kiểm tra
đăng ký, hash, liên kết ký/hết hạn/dùng lại, CSRF và hiển thị form, nhưng chưa
được chạy cho thay đổi này. Khi chạy lại, ghi kết quả và file surefire vào bảng.

# Tài khoản chỉ dùng Active / Inactive

Ngày: 28/10/2026. Theo yêu cầu bỏ trạng thái Blocked; không commit/push.

## Hành vi

- Quản lý tài khoản chỉ chấp nhận `Active` và `Inactive`; form và bộ lọc không còn lựa chọn “Bị khóa”. Request sửa tài khoản gửi `Blocked` bị từ chối ở `accountStatus` trước khi lưu.
- Giữ hai thao tác cũ: `Active` → `Inactive` (Vô hiệu hóa), `Inactive` → `Active` (Kích hoạt). Trạng thái không hỗ trợ vẫn bị chặn khi kích hoạt, không tự mở tài khoản lạ/legacy.
- Dashboard và inspector chỉ hiển thị hai trạng thái; bỏ cột dữ liệu `blocked` và hàm `blockedTotal` khỏi DTO nội bộ. Các nơi dùng DTO trong source/test đã được cập nhật cùng thay đổi.
- URL bộ lọc cũ `status=Blocked` được xử lý như một bộ lọc trạng thái không hỗ trợ theo cơ chế hiện có, không mở rộng phạm vi role của danh sách.
- Các kiểm tra bảo mật chỉ cho phép `Active` giữ nguyên: đăng nhập, phiên hiện tại, CurrentUser và đặt lại mật khẩu. Test dùng `Blocked` còn lại là ca dữ liệu legacy/không hợp lệ để xác nhận hệ thống vẫn chặn truy cập.

## Database và tương thích

Không thay bảng, cột, FK hoặc CHECK constraint; `database/schema/db.sql` giữ nguyên. CHECK cũ vẫn cho phép giá trị legacy `Blocked`, nhưng ứng dụng không còn cho chọn/lưu giá trị này. Không thêm migration tự chạy lúc khởi động.

Script dữ liệu: `database/migrations/012_blocked_accounts_to_inactive.sql`, chỉ UPDATE `AccountStatus` và `UpdatedAt` trên bản ghi đang `Blocked`. Chạy lặp an toàn, có transaction và rollback khi lỗi; không xóa tài khoản và không chuyển tài khoản sang Active.

Đã chạy trên database `RitirementManagement2` lấy từ cấu hình ứng dụng của checkout hiện tại:

- Trước: `Active=204`, `Inactive=0`, `Blocked=0`.
- Số bản ghi chuyển đổi: **0**.
- Sau: `Active=204`, `Inactive=0`, `Blocked=0`; tổng số và số Active giữ nguyên.
- Kiểm chứng riêng bằng một User thử `Blocked` trong transaction: chuyển sang `Inactive` thành công, chạy lại chuyển 0 bản ghi; rollback xóa bản ghi thử và khôi phục số đếm ban đầu. Không tạo Candidate hoặc hồ sơ tuyển dụng.

## Phạm vi file

- `user/service/AccountManagementService`, `AccountListService` và chú thích trạng thái của entity `User`.
- `dashboard/DashboardService`, `dashboard/dto/AdminDashboardResponse`.
- `templates/admin/accounts/{list,candidates,inspector,form}.html`, `templates/dashboard/admin.html`, `static/js/pages/admin-dashboard.js`.
- Test quản lý tài khoản, ánh xạ inspector, bộ lọc, Dashboard unit/render/integration. Không sửa backend Interview/Offer/Talent Pool hoặc file SecurityConfig.
- Giữ nguyên các sửa đổi đang có của người dùng ở auth/diagram và thay đổi Candidate nộp CV trở lại Applied của lượt trước.

## Kiểm chứng

- **174 Java test qua, 0 failure/error/skipped**, gồm quản lý tài khoản, bộ lọc, kích hoạt, inspector, bảo mật đăng nhập/phiên/CurrentUser, PasswordReset, Dashboard service và 3 ca render Dashboard bằng MockMvc.
- **2 JavaScript test qua** của `account-ui.test.cjs`; `node --check` cho `admin-dashboard.js` qua.
- Script dữ liệu đã được kiểm chứng trên SQL Server như trên. Không gửi email hoặc chạy AI.
- **7/7 ca Dashboard integration qua trên SQL Server + MockMvc**, gồm đối chiếu số đếm với SQL và danh sách đã lọc, render hai trạng thái, giữ form POST kích hoạt/vô hiệu hóa và phân quyền HR/HM. Lượt đầu 5 qua, 2 lỗi ghi snapshot do thiếu thư mục `target` tương đối khi chạy POM tạm; tạo thư mục output rồi chạy lại đúng 2 ca đó, cả 2 qua, không sửa code sản phẩm. Tổng 181 ca Java khác nhau qua; không cộng lượt chạy lại thành ca mới. Chưa xác minh giao diện bằng trình duyệt; không coi MockMvc render là browser proof.

`git diff --check` và cú pháp JavaScript đều qua. Bằng chứng JUnit nằm dưới `target/account-state-verification/surefire-reports`; kết quả lượt integration đầu lưu ở `target/account-state-verification/integration-first-attempt.xml`.

Build kiểm chứng dùng POM tạm `target/account-state-verification-pom.xml`, output `target/account-state-verification` để tránh xung đột IDE; không sửa POM chính. Khi áp dụng ở môi trường khác, chạy script dữ liệu 012 trên đúng database và triển khai/restart bản ứng dụng mới cùng đợt.

Checkout thực tế: `D:\KÌ 5\SWP391\hireflow-api\RetireManagement\SWP_RMS_Group-2`. Đường dẫn chat cũ có `SWP` đã không còn tồn tại, nên app có thể không hiển thị diff cho checkout mới tự động.


## Bổ sung: cập nhật bản web đang chạy (28/10)

Người dùng vẫn thấy “Bị khóa” vì source đã đổi nhưng `target/classes/templates/admin/accounts/form.html` còn option Blocked. Các lượt test trước dùng output riêng nên chưa cập nhật tài nguyên của server. Tiến trình cổng 8081 ban đầu chạy đúng checkout SWP391 nhưng tắt DevTools restart.

Đã chạy `mvn.cmd -B -DskipTests compile` ở POM chính để đồng bộ tài nguyên vào `target/classes`. Kiểm tra bytecode `AccountManagementService` chỉ còn hai giá trị Active/Inactive. Các lần agent thử khởi động lại gặp lỗi Java Unix-domain loopback trên Windows; sau đó cổng 8081 đã có tiến trình RMS mới PID 9716 và GET `/login` trả 200. Không sửa cấu hình Windows, POM hoặc source để xử lý lỗi môi trường đó.

Đã xác minh HTTP thật trên `http://127.0.0.1:8081` bằng tài khoản System Admin kiểm thử riêng, không dùng mật khẩu/tài khoản thật:

- Đăng nhập qua form với CSRF hợp lệ, mở GET `/admin/accounts/{testId}/edit`: select `accountStatus` chỉ có **Active, Inactive**.
- POST form sửa với `accountStatus=Blocked` và các trường khác hợp lệ: HTTP 200 render lỗi tại `account-status-error` (`Invalid account status.`); truy vấn SQL xác nhận tài khoản thử vẫn Active.
- POST `accountStatus=Inactive`: HTTP 302 thành công, SQL xác nhận Inactive.
- GET danh sách bằng phiên vừa bị vô hiệu hóa: HTTP 302, phiên không còn được truy cập.
- Xóa tài khoản thử trong finally sau mỗi lượt; số đếm cuối DB vẫn **Active=204**, không còn bản ghi thử. Các lần chuẩn bị test thiếu phòng ban chỉ bị validation từ chối, đã sửa dữ liệu test rồi kiểm tra lại; không đổi luật phòng ban của sản phẩm.

Bằng chứng này là HTTP trực tiếp + SQL, không phải kiểm thử qua trình duyệt. Trang người dùng đã mở trước khi build/restart cần tải lại; session cũ có thể cần đăng nhập lại. Không commit/push.

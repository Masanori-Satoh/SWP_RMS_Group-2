# 02/11/2026 — Việt hóa lỗi tạo/sửa tài khoản

## Yêu cầu và phạm vi

- Người thực hiện: LinhDN.
- Người dùng yêu cầu chuyển các lỗi tạo/sửa tài khoản sang tiếng Việt, gồm lỗi nhập thông tin, chọn vai trò, mật khẩu xác nhận không khớp và trùng email.
- Dịch đồng bộ các thông báo nghiệp vụ liên quan đến tài khoản nội bộ, phòng ban, vai trò và trạng thái.

## Thay đổi

| File | Nội dung |
| --- | --- |
| `src/main/java/com/group2/rms/user/dto/CreateAccountRequest.java` | Việt hóa 13 thông báo kiểm tra dữ liệu: bắt buộc nhập, định dạng email, giới hạn độ dài, chọn vai trò và xác nhận mật khẩu. |
| `src/main/java/com/group2/rms/user/dto/UpdateAccountRequest.java` | Việt hóa 8 thông báo kiểm tra dữ liệu khi sửa tài khoản. |
| `src/main/java/com/group2/rms/user/controller/AccountController.java` | Việt hóa 2 lỗi xung đột tên đăng nhập/email khi lưu dữ liệu. |
| `src/main/java/com/group2/rms/user/service/AccountManagementService.java` | Việt hóa 21 vị trí thông báo: thiếu thông tin, trùng tên/email, vai trò/phòng ban/trạng thái không hợp lệ, phòng ban đã có trưởng phòng và tài khoản không tồn tại. |
| `src/main/resources/templates/auth/fragments.html` | Cho phép các thông báo đã dịch nằm trong bảng giá trị tiếng Việt được hiển thị trực tiếp. Phần đăng ký dùng chung service tài khoản vẫn nhận được lỗi trùng tên/email cụ thể thay vì rơi vào thông báo chung. |
| `src/test/java/com/group2/rms/user/service/AccountManagementCoverageTests.java` | Cập nhật kết quả mong đợi của kiểm tra lỗi chọn vai trò ứng viên trên form nội bộ sang tiếng Việt. |

Không đổi điều kiện validation, phân quyền, mã vai trò/trạng thái, thao tác lưu tài khoản hoặc cấu trúc DB. Ngoài fragment cho lỗi đăng ký dùng chung nêu trên, các màn hình khác trong báo cáo rà soát tiếng Anh không thuộc lượt sửa này.

## Kiểm chứng

Chạy Maven offline với Mockito 5.17.0 được nạp bằng `-javaagent` qua tham số dòng lệnh. Lượt đầu bị kẹt khi Mockito tự gắn agent do lỗi Windows named pipe; đã dừng lượt đó và chạy lại với agent có sẵn, không sửa POM.

| Lớp kiểm tra | Kết quả |
| --- | --- |
| `AdminCreateFormsWebTests` | 7/7 qua; render form thật, service được mock. |
| `AccountEditWebTests` | 7/7 qua; render form thật, service được mock. |
| `AccountManagementServiceTests` | 7/7 qua. |
| `AccountManagementCoverageTests` | 37/37 qua. |
| `AuthBotanicalWebTests` | 6/6 qua. |
| `RegistrationVerificationServiceTests` | 25/33 qua; 3 failure và 5 error liên quan thời hạn OTP. |

- Tổng lượt Maven: **97 test, 89 qua, 3 failure, 5 error**, nên lệnh Maven kết thúc `BUILD FAILURE`. Riêng 5 lớp tài khoản/giao diện xác thực phía trên có **64/64 test qua**.
- Các lỗi ở lớp đăng ký xuất phát từ bất đồng thời hạn: `RegistrationVerificationService` hiện có `OTP_LIFETIME` và `RESEND_DELAY` bằng 1 phút, trong khi các ca lỗi giả định thời hạn dài hơn (mốc 299/300/899 giây). Service đăng ký và lớp test đó không được chỉnh trong lượt này; dependency tài khoản trong lớp test được mock. Không sửa luật OTP ngoài phạm vi yêu cầu.
- Đối chiếu nội dung bốn file Java production trước/sau khi loại bỏ string literal: không có thay đổi logic ngoài câu chữ.
- Quét lại DTO tạo/sửa: toàn bộ message validation khai báo trực tiếp đã là tiếng Việt.
- Kiểm tra nội dung HTML sinh từ các ca form không hợp lệ: lỗi email ở form tạo/sửa và mật khẩu không khớp ở form tạo hiển thị tiếng Việt, không còn câu tiếng Anh cũ.
- Render trực tiếp fragment lỗi xác thực: **4/4 trường hợp qua**, gồm hai lỗi trùng tên/email đã là tiếng Việt, ánh xạ lỗi email tiếng Anh cũ và fallback tiếng Việt cho lỗi không thuộc bảng. Log: `target/account-vietnamese-message-render.log`.
- Log Maven: `target/account-vietnamese-validation-test.log`.
- Chưa chạy trình duyệt hoặc kiểm thử với SQL Server/SMTP thật.

## Database và bàn giao

- Không thay đổi schema, migration, seed hoặc dữ liệu DB.
- Các thay đổi sẵn có trong `Professional_Bug_Tracker_Template.xlsx` và `.qa/` được giữ nguyên.
- Chưa commit/push.

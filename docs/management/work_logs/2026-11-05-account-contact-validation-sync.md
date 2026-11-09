# 05/11/2026 — Đồng bộ validation họ tên và số điện thoại

## Yêu cầu và quyết định

- Người thực hiện: LinhDN.
- Khắc phục việc Admin/Đăng ký chấp nhận dữ liệu nhưng Profile từ chối khi người dùng lưu lại.
- Dùng SRS `RMS_SRS (3).docx` do người dùng cung cấp làm chuẩn: mục II.1.1 Register Account, II.1.8 Edit profile, II.3.7 Create Internal Account detail và II.3.8 Edit Internal Account.
- SRS mô tả họ tên tối đa 100 ký tự; Admin tạo/sửa có SĐT tùy chọn tối đa 20 ký tự. Mục Edit profile chưa mô tả SĐT. Không tìm thấy yêu cầu tối thiểu 2 ký tự, regex chỉ chữ cho họ tên hoặc giới hạn SĐT 15 ký tự.
- Đối chiếu `database/schema/db.sql`: `[User].FullName NVARCHAR(100) NOT NULL`, `PhoneNumber NVARCHAR(20) NULL`; không có CHECK định dạng cho hai trường này.

## Quy tắc thống nhất

| Trường | Validation | Khi lưu |
| --- | --- | --- |
| Họ và tên | Bắt buộc, không toàn khoảng trắng, tối đa 100 ký tự; không giới hạn bộ ký tự hoặc bắt buộc tối thiểu 2 ký tự | Trim khoảng trắng đầu/cuối |
| Số điện thoại | Không bắt buộc, tối đa 20 ký tự; không áp dụng regex riêng ở Profile | Trim đầu/cuối; giá trị trống chuyển thành null |

- Giới hạn độ dài trên DTO kiểm tra chuỗi nhận được, giống các form Admin/Đăng ký; service tiếp tục chuẩn hóa khi lưu.
- Đăng ký không có ô SĐT, tài khoản mới có SĐT null.
- `A`, `A1`, `Nguyễn 123` và SĐT chứa chữ trong giới hạn độ dài được chấp nhận theo hợp đồng hiện tại. Nếu cần quy tắc định dạng chặt hơn, phải bổ sung vào SRS và áp dụng đồng bộ mọi luồng.
- Expected Results của bug cần dùng quy tắc trên; mô tả cũ “họ tên 2–100, chỉ chữ; SĐT ≤15” không phải chuẩn đã chốt.

## Mã nguồn

- `UpdateProfileRequest`: bỏ min=2, bỏ hai regex, đổi SĐT max=20; thông báo lỗi đồng bộ với DTO Admin.
- `templates/user/profile.html` và `templates/candidate/my-profile.html`: bỏ minlength=2, pattern/title cũ; SĐT maxlength và gợi ý là 20.
- `static/js/profile.js`: bỏ regex họ tên; giữ kiểm tra tên không trống, kiểm tra HTML5 và bước xác nhận trước khi lưu.
- Các service đã trim họ tên/SĐT và chuyển SĐT trống thành null, không cần đổi.
- Không đổi schema, migration, seed hoặc dữ liệu DB; không sửa file SRS nguồn bên ngoài repository.

## Kiểm chứng

- Thêm `AccountContactValidationTests`: so sánh cùng dữ liệu giữa DTO Admin tạo/sửa, Đăng ký và Profile; kiểm tra tên một ký tự, có chữ số, 100/101 ký tự, null/trống/toàn khoảng trắng; SĐT tùy chọn, có chữ, 18/20/21 ký tự.
- Bổ sung kiểm tra POST qua Spring MVC cho cả `/profile/update` và `/portal/profile/update` với các tên `A`, `A1`, `Nguyễn 123` và SĐT 20 ký tự có chữ.
- Chạy Maven offline cho 5 lớp bên dưới, nạp Mockito 5.17.0 bằng `-javaagent` ở dòng lệnh, không đổi POM. Java production và test compile thành công.

| Lớp kiểm tra | Kết quả |
| --- | --- |
| `AccountContactValidationTests` | 14/14 qua |
| `UserProfileWebTests` | 17/17 qua |
| `CandidateProfileWebTests` | 12/13 qua |
| `UserProfileServiceTests` | 15/15 qua |
| `CandidateProfileServiceTests` | 5/5 qua |

- Tổng **64 test: 63 qua, 1 failure, 0 error**; lệnh Maven kết thúc `BUILD FAILURE`.
- Failure duy nhất: `CandidateProfileWebTests.candidateProfileUsesAdwContentWithoutWorkspaceScene` đòi HTML có class `adw-form-wrap`, nhưng template không có class này. Đối chiếu cả template và assertion ở HEAD trước lượt sửa cũng đã có sự không khớp đó; lượt sửa validation không thay đổi layout/class. Giữ nguyên lỗi kiểm tra giao diện ngoài phạm vi này.
- Toàn bộ **20 ca hồi quy mới** qua: 14 ca đối chiếu validation DTO và 6 ca POST lưu hồ sơ (3 tên × 2 controller).
- Log Maven: `target/profile-validation-tests.log` (file kiểm chứng cục bộ, không đưa vào Git).
- `node --check src/main/resources/static/js/profile.js`: qua.
- `git diff --check`: qua.
- Chưa chạy ứng dụng thật hoặc kiểm tra lưu dữ liệu trên DB thật; các web test dùng service/repository mock.

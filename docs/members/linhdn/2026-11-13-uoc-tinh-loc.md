# Ước tính LOC của LinhDN theo mẫu RMS

Ngày đối chiếu 13/11/2026. Nguồn là [Tính Loc.xlsx](T%C3%ADnh%20Loc.xlsx), sheet `LOC Estimation guide`, tài liệu LinhDN, [WORK_LOG.md](../../management/WORK_LOG.md) và mã nguồn nhánh `feature/update-ui-authen` tại `bafdf65d`. Git ghi nhận 104 commit không merge mang email chứa `linhkata` có thay đổi trong `src/main` trên lịch sử HEAD. Con số commit dùng để đối chiếu phạm vi. Trong đó có hai commit hợp nhất dạng squash `19544727` và `37bf608d` cho Talent Pool.

**Tổng ước tính theo mức trần của mẫu là 2.790 LOC cho 26 màn hình/chức năng.**

## Cách quy đổi

- Dùng số thành phần thao tác trên màn hình. Một bảng, nhóm lọc cùng thuộc tính, nhóm phân trang và nút thao tác lặp trên các hàng được đếm theo loại. Sáu ô nhập một OTP là một trường. Bỏ qua điều hướng chung, thông tin chỉ đọc và nút kỹ thuật để mở/đóng phụ nếu thao tác đã được tính.
- Dựa trên ba ví dụ nhúng trong sheet. Ví dụ danh sách tính cả bảng, nút tìm và phân trang. Ví dụ form nhân đôi **level** khi dùng chung cho tạo/sửa. Ví dụ checkout cộng hai fields cho một giao dịch ẩn. Cột Notes dùng AND, trong khi phần hướng dẫn và ví dụ cho phép chọn theo fields. Mình dùng fields cho màn hình và giao dịch cho nghiệp vụ nền có bằng chứng.
- Level 1–7 tương ứng 60, 90, 120, 150, 180, 210 và 240 LOC. File chỉ có bảy mức nên lấy Level 7 làm trần. Form tạo/sửa tài khoản chung có hai route và hai chế độ trong cùng template. Dùng mức của chế độ tạo làm cơ sở, nhân đôi level và áp trần. Form phòng ban áp dụng cùng quy tắc.
- Mỗi dòng đại diện cho một màn hình hoặc chức năng được ghi nhận có phần LinhDN triển khai. Khi nhiều người góp chung, LOC thể hiện mức của phần chức năng được mô tả, chưa có tỷ lệ chia cá nhân chính thức. Bổ sung kiểm thử, tài liệu, diagram, sửa merge, các lượt chỉnh giao diện lặp lại và sửa lỗi trên chức năng đã chấm không tạo dòng LOC mới.

## Authentication — 450 LOC

| Màn hình/chức năng | Fields được tính | Level | LOC |
| --- | --- | ---: | ---: |
| Đăng nhập | 5 = username/email, mật khẩu, hiện mật khẩu, đăng nhập, quên mật khẩu | 1 | 60 |
| Đăng ký | 6 = họ tên, username, email, mật khẩu, xác nhận mật khẩu, gửi mã | 2 | 90 |
| OTP đăng ký | 4 = mã, xác minh, gửi lại, sửa thông tin đăng ký | 1 | 60 |
| Quên mật khẩu | 3 = email, gửi mã, về đăng nhập | 1 | 60 |
| OTP khôi phục | 3 = mã, xác minh, gửi lại | 1 | 60 |
| Đặt mật khẩu mới | 5 = hai trường mật khẩu, hai nút hiện mật khẩu, lưu | 1 | 60 |
| Đăng xuất | 3 = mở xác nhận, hủy, xác nhận đăng xuất | 1 | 60 |

Đối chiếu `templates/auth/`, `RegistrationController`, `PasswordRecoveryController`, `SecurityConfig` và [tổng hợp Auth/Admin](SUMMARY_AUTH_DASHBOARD_ADMIN_2026-10-12.md). Gửi SMTP đã gắn với nút gửi mã hoặc gửi lại. Cập nhật số lần OTP sai nằm trong hai màn OTP.

## Quản trị — 1.230 LOC

| Màn hình/chức năng | Fields hoặc giao dịch được tính | Level | LOC |
| --- | --- | ---: | ---: |
| Danh sách tài khoản nội bộ | 13 = từ khóa, tìm, trạng thái, phòng, sắp xếp, vai trò, áp dụng, đặt lại, tạo mới, bảng, sửa, đổi trạng thái, phân trang | 5 | 180 |
| Tạo/sửa tài khoản nội bộ | Chế độ tạo 10 = họ tên, username, email, SĐT, vai trò, phòng, mật khẩu, xác nhận mật khẩu, hủy, lưu. Level 4 × 2, áp trần 7 | 7 | 240 |
| Danh sách tài khoản ứng viên | 10 = trạng thái, sắp xếp, từ khóa, tìm, áp dụng, đặt lại, bảng, mở hồ sơ tóm tắt, đổi trạng thái, phân trang | 4 | 150 |
| Danh sách phòng ban | 10 = trạng thái, từ khóa, tìm, đặt lại, tạo mới, bảng, xem tóm tắt, sửa, đổi trạng thái, phân trang | 4 | 150 |
| Tạo/sửa phòng ban | 4 = tên, trưởng phòng, hủy, lưu. Level 1 × 2 | 2 | 90 |
| Audit Log | 10 = từ ngày, đến ngày, người thực hiện, hành động, đối tượng, từ khóa, tìm, đặt lại, bảng, phân trang | 4 | 150 |
| Dashboard Admin | 7 nhóm truy xuất = trạng thái nội bộ, trạng thái ứng viên, trạng thái phòng ban, theo vai trò, tổng ứng viên, ứng viên mới, audit gần đây | 6 | 210 |
| API Monitoring | Hai lần probe độc lập theo đích = ứng dụng/SQL Server và SMTP | 1 | 60 |

Đối chiếu `templates/admin/`, `dashboard/admin.html`, `AccountController`, `CandidateAccountController`, `DepartmentController`, `AdminDashboardService`, `AdminDashboardController`, `AuditLogService` và `ApiMonitoringController`. Dashboard nội bộ theo vai trò có đóng góp UI lịch sử, nhưng các màn theo vai trò hiện tại chưa có căn cứ để nhận toàn bộ. Bảng chấm dùng Dashboard Admin.

## Talent Pool — 990 LOC

| Màn hình/chức năng | Fields hoặc giao dịch được tính | Level | LOC |
| --- | --- | ---: | ---: |
| Danh sách kho tiềm năng | 10 = từ khóa, tìm, người đánh dấu, tin nguồn, áp dụng, đặt lại, bảng, mở hồ sơ, xem ghi chú, phân trang. Chọn và xem CV được chấm cùng luồng chọn hoặc chi tiết | 4 | 150 |
| Chọn CV/ứng viên và gửi đề xuất | 8 = chọn dòng, chọn trang, chọn CV, xem trước CV, bỏ chọn, mở tổng kết, kiểm tra lại, xác nhận. HR/HM dùng chung quy trình chọn | 3 | 120 |
| Chi tiết ứng viên tiềm năng | 7 = bảng CV, mở CV, phân trang CV, email, LinkedIn, Portfolio, quay lại. Ghi chú và quyết định HM có dòng riêng | 2 | 90 |
| Đánh dấu/bỏ lưu tiềm năng | 3 thao tác mở bookmark, lưu, bỏ lưu. Tính chung nơi gọi từ Application và Interview | 1 | 60 |
| Ghi chú chung | 8 = danh sách, nhập, lưu, mở menu, sửa, xóa, hủy sửa, lưu sửa. Tính chung các màn sử dụng | 3 | 120 |
| Gợi ý HR → HM | 7 = bảng, mở hồ sơ, phân trang, mở luồng gửi, thu hồi, đồng ý, từ chối. Quyết định từ trang chi tiết dùng chung | 2 | 90 |
| Danh sách HM đề xuất | 5 = bảng, mở luồng thêm, mở và xử lý hồ sơ, liên hệ email, về tin | 1 | 60 |
| HR xử lý đề xuất dùng CV cũ | 4 = xác nhận đã có đồng ý, xác nhận và lên lịch, ghi nhận từ chối, xác nhận kết thúc đề xuất | 1 | 60 |
| Đóng đợt tuyển từ Talent Pool | Trên 7 thao tác dữ liệu/tích hợp ở mức nghiệp vụ gồm đọc/khóa tin, kiểm tra offer mở, cập nhật tin, nạp/xử lý đề xuất, nạp/xử lý đơn chính thức, kiểm tra offer đã nhận, hủy lịch, ghi audit và gửi thông báo. Tính một lần, kể cả sửa điều kiện offer quá hạn | 7 | 240 |

Đối chiếu `templates/talent-pool/`, `candidate/application-detail.html`, `TalentPoolController`, `TalentPoolService` và [tổng hợp thay đổi Talent Pool](README.md). Các phần sàng lọc chung của ứng viên và nghiệp vụ lên lịch phỏng vấn thuộc luồng rộng hơn. Việc kiểm tra REQ-0002 ngày 13/11 chưa tạo thêm chức năng.

## Hồ sơ cá nhân — 120 LOC

| Chức năng | Fields được tính | Level | LOC |
| --- | --- | ---: | ---: |
| Ứng viên tự sửa email | 3 = nhập email, lưu, xác nhận lưu trong form hồ sơ hiện hữu | 1 | 60 |
| Đổi mật khẩu và xử lý phiên cũ | 4 = mật khẩu hiện tại, mật khẩu mới, xác nhận, lưu. Phần ghi audit và xử lý phiên gắn với thao tác | 1 | 60 |

Đối chiếu `candidate/my-profile.html`, `user/profile.html`, [work log email ứng viên](../../management/work_logs/2026-11-08-candidate-profile-email.md) và [work log Auth/Account](../../management/work_logs/2026-11-06-auth-account-talent-pool-bugfix.md). Phạm vi chỉ nhận phần email, đổi mật khẩu và xử lý phiên đã ghi nhận, không nhận toàn bộ Profile hoặc avatar.

## Kiểm tra

Cộng lại theo số LOC từng dòng và theo bốn nhóm: 450 + 1.230 + 990 + 120 = **2.790**. Bản Excel được đọc để lấy phương pháp và dữ liệu ví dụ. Phân tích tĩnh, không chạy ứng dụng. Kết quả là ước tính theo mức trần của mẫu, có thể đổi nếu quy tắc chấm chia LOC cho các chức năng làm chung hoặc tính hai chế độ form riêng.

# 06/11/2026 — Sửa lỗi Auth, Tài khoản, Phòng ban và Kho ứng viên tiềm năng

## Yêu cầu và quyết định

- Người thực hiện: LinhDN.
- Rà soát các module Authentication, Authorization, Account, Department, Potential Candidate (Talent Pool); lập danh sách 18 lỗi kèm cách tái hiện, sau đó sửa 6 nhóm lỗi dưới đây theo yêu cầu.
- Không đổi schema, migration, seed hoặc dữ liệu DB.

## Các lỗi đã sửa

| # | Lỗi | Cách tái hiện (trước khi sửa) | Cách sửa |
| --- | --- | --- | --- |
| 3 | Một phòng ban có 2 trưởng phòng đang hoạt động | Vô hiệu hóa `hm_tuan`, tạo HM mới cho phòng Kỹ thuật, rồi bấm Kích hoạt lại `hm_tuan` ở danh sách | `AccountListService`: Kích hoạt lại HM bị chặn (409) nếu phòng đã có HM Active khác; Vô hiệu hóa trưởng phòng thì gỡ khỏi `Department.ManagerId`; kích hoạt HM vào phòng chưa có trưởng phòng thì làm trưởng phòng |
| 6 | Đổi mật khẩu không đăng xuất phiên cũ | Đăng nhập 2 trình duyệt, đổi mật khẩu ở B, bấm trang bất kỳ ở A vẫn dùng được | `AccountSessionGuardFilter`: phiên ghi nhớ hash mật khẩu ở request đầu; hash đổi thì đăng xuất và chuyển `/login?session-expired`. Lời gọi nền `/api/` chỉ trả 401, không đá phiên, để lần bấm trang kế tiếp hiện thông báo hết hạn. `UserProfileController`: người tự đổi mật khẩu giữ phiên hiện tại |
| 10 | Ứng viên đã vô hiệu hóa vẫn nằm trong kho tiềm năng | Admin vô hiệu hóa ứng viên, HR vào `/talent-pool` vẫn thấy và gợi ý được | `TalentPoolCandidateRepository`: chỉ lấy tài khoản Active. `TalentPoolService`: HR gợi ý, HM đề xuất và bước kiểm tra trước khi gửi từ chối tài khoản không Active |
| 14 | Lọc theo người đánh dấu dùng dữ liệu cũ | A đánh dấu, bỏ đánh dấu, B đánh dấu lại; lọc theo A vẫn hiện | `TalentPoolCandidateRepository`: chỉ tính người ghi log đánh dấu gần nhất |
| 18a | Số điện thoại nhập chữ vẫn lưu được | Nhập `abcxyz` ở tạo/sửa tài khoản hoặc hồ sơ | `CreateAccountRequest`, `UpdateAccountRequest`, `UpdateProfileRequest`: SĐT để trống hoặc đúng 10 chữ số bắt đầu bằng 0 (`( *\|0[0-9]{9})`). Ô nhập trên 3 giao diện đổi `maxlength` 20 thành 10 và gợi ý mới |
| 18b | Đổi mật khẩu và sửa hồ sơ không ghi audit log | Đổi mật khẩu hoặc sửa tên/SĐT, không có dòng trong `AuditLog` | `UserProfileService`: ghi giá trị cũ/mới của tên và SĐT khi có thay đổi; ghi "Đổi mật khẩu thành công", không ghi mật khẩu hay hash |

## Quy tắc mới thay thế quy tắc cũ

- Work log `2026-11-05-account-contact-validation-sync.md` ghi SĐT "không áp dụng regex". Quy tắc đó **được thay bằng** quy tắc 10 chữ số bắt đầu bằng 0 ở trên. Họ tên giữ nguyên.
- SĐT toàn khoảng trắng vẫn được chấp nhận và lưu thành null như trước.
- Tài khoản cũ có SĐT sai định dạng sẽ bị báo lỗi khi sửa hồ sơ hoặc tài khoản đó, cho tới khi nhập lại số đúng. Chưa kiểm tra dữ liệu DB hiện có.

## Kiểm chứng

- Chạy lại trên app đang chạy cổng 8081 (chỉ GET, riêng lỗi 6 đổi rồi khôi phục mật khẩu `hr_quang`): phiên A bị chuyển `/login?session-expired`, phiên B tự đổi mật khẩu vẫn ở lại.
- Test mới: `AccountSessionGuardFilterTests` (đổi mật khẩu ở nơi khác, và lời gọi `/api/` trả 401 không đăng xuất), `UserProfileWebTests` (SĐT có chữ bị từ chối), `AccountContactValidationTests` (cập nhật ca SĐT theo luật mới).
- Test cũ được sửa dữ liệu mẫu: 4 lớp `talentpool/service` thêm `accountStatus = "Active"` cho tài khoản ứng viên; `UserProfileServiceTests`, `TopbarUserAdviceTests` truyền thêm `UserAuditLogger`; hai test hồ sơ đổi SĐT mẫu sang `0901234567`.

| Lớp kiểm tra | Kết quả |
| --- | --- |
| `AccountSessionGuardFilterTests` | 15/15 qua |
| `AccountActivationDepartmentTests` | 6/6 qua |
| `AccountContactValidationTests` | 18/18 qua |
| `UserProfileWebTests` | 18/18 qua |
| `CandidateProfileWebTests` | 13/13 qua |
| `UserProfileServiceTests` | 15/15 qua |
| `AdminCreateFormsWebTests`, `AccountEditWebTests` | 7/7 và 7/7 qua |
| Nhóm `candidate.talentpool` | Không có lỗi mới so với bản gốc |

- Lỗi có sẵn từ trước (đã stash toàn bộ thay đổi và chạy lại để xác nhận): 7 test `ApplicationPipelineWebTests` và 6 test trong nhóm `candidate.talentpool` (gồm `TalentPoolFlowIntegrationTests`, `TalentPoolClosureQueryTests`). Không thuộc phạm vi lượt sửa này.
- Chưa kiểm tra trên trình duyệt thật và chưa chạy câu SQL lọc theo người đánh dấu trên dữ liệu thật.

## Vấn đề tồn đọng

- Chưa sửa: lỗi 1 (lưu phòng ban đổi vai trò Giám đốc/Admin), 2 (Admin tự khóa mình), 4 (mật khẩu có ký tự nhiều byte vượt 72 byte BCrypt gây 500), 5 (Giám đốc tạo lịch phỏng vấn bị 403 ở bước gửi mail), 7 (đăng nhập xong rơi vào JSON), 8, 9, 11, 12, 13, 15, 16, 17; ở lỗi 18 còn username có dấu cách và giới hạn đăng nhập sai.
- Lỗi 3: hai Admin kích hoạt hai HM cùng lúc vẫn có thể lọt qua kiểm tra vì chưa khóa dòng phòng ban.
- Lỗi 10: đề xuất đã tạo trước khi ứng viên bị khóa vẫn chấp nhận/xác nhận được; trang chi tiết `/talent-pool/{id}` vẫn mở được nếu biết link.

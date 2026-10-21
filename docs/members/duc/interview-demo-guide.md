# Kịch bản demo luồng phỏng vấn

Luồng: tạo lịch, xem lịch (danh sách và chi tiết), chấm điểm, chốt kết quả. Mỗi bước dùng đúng role.

Dữ liệu demo: [database/seeds/demo_interview_flow.sql](../../../database/seeds/demo_interview_flow.sql).

## 1. Chuẩn bị (khoảng 5 phút)

1. Chạy script trên DB `RitirementManagement2`. Chọn một trong hai cách:
   - SSMS: mở file, chọn đúng database, bấm Execute.
   - Terminal:
     ```
     sqlcmd -S localhost -U sa -P 123 -C -d RitirementManagement2 -f 65001 -i database\seeds\demo_interview_flow.sql
     ```
2. Bảng kết quả cuối script cho biết `InterviewId` của từng ứng viên. Ghi lại để mở nhanh `/interviews/{id}/result`.
3. Chạy app với profile `local` và mở 2 trình duyệt (hoặc 1 cửa sổ thường + 1 cửa sổ ẩn danh) để đổi role nhanh.
4. Sau khi test thử, chạy lại script một lần trước giờ review. Script xóa dữ liệu demo cũ rồi tạo lại, nên mọi thứ về trạng thái ban đầu.

Mật khẩu của mọi tài khoản là `12345678`.

| Tài khoản | Role | Vai trò trong demo |
|---|---|---|
| `hr_lan` | HR | Tạo lịch cho demo.an. Ở lịch demo.chi, là thành viên hội đồng |
| `hr_quang` | HR | Thành viên hội đồng (HR) của lịch demo.binh và demo.dung |
| `hm_tuan` | Hiring Manager | Trưởng phòng Engineering, người duy nhất chốt được kết quả |
| `director` | Director | Thành viên hội đồng, xem toàn bộ lịch |
| `interviewer_nam` | Interviewer | Thành viên hội đồng demo.binh và demo.dung |
| `interviewer_viet` | Interviewer | Thành viên hội đồng demo.chi (đã nộp phiếu) |
| `interviewer_thao` | Interviewer | Không thuộc hội đồng nào, dùng để chứng minh GBR-05 |
| `hm_huong` | Hiring Manager | Phòng Sales, dùng để chứng minh HM khác phòng không chốt được |
| `demo.an`, `demo.binh`, `demo.chi`, `demo.dung` | Candidate | 4 ứng viên demo |

Trạng thái 4 ứng viên sau khi chạy script (cùng tin "Senior Java Backend Engineer", REQ-0001):

| Ứng viên | Hồ sơ | Lịch | Phiếu | Kết quả | Dùng cho cảnh |
|---|---|---|---|---|---|
| demo.an (Nguyễn Văn An) | HM_Passed | Chưa có | | | 1 – Tạo lịch |
| demo.binh (Trần Thị Bình) | Interviewing | Bắt đầu 2 tiếng trước lúc chạy script, Scheduled | 0/4 | | 3, 4 – Chấm điểm, chốt Đạt |
| demo.chi (Lê Minh Chi) | Interviewing | Hôm qua 14:00, Completed | 2/4 | | 5 – Chốt khi chưa đủ phiếu |
| demo.dung (Phạm Quốc Dũng) | Interviewing | Hôm qua 09:00, Completed | 4/4 | Đạt | 6 – Kết quả đã khóa |

## 2. Kịch bản

### Cảnh 1 – HR tạo lịch phỏng vấn (`hr_lan`)

1. Vào **Hồ sơ ứng tuyển**, mở hồ sơ **Nguyễn Văn An**. Hồ sơ ở bước "Chờ xếp lịch phỏng vấn".
2. Bấm **Lên lịch phỏng vấn**. Trang chuyển sang `/interviews/new?applicationId=...` và ô **Hồ sơ ứng tuyển** đã chọn sẵn An.
3. Điền form:
   - Hình thức: Online (Google Meet), liên kết `https://meet.google.com/abc-defg-hij`
   - Thời gian: giữ mặc định (9:00–10:00 ngày mai)
   - Hội đồng: `interviewer_nam`, `hm_tuan`, `hr_quang`, `director`
4. Các điểm nên chỉ ra trong form:
   - Danh sách hội đồng **không có `hr_lan`**: HR không tự xếp mình vào hội đồng, nhưng xếp được HR khác (`hr_quang`).
   - Dùng các nút lọc theo role (Hiring Manager / Interviewer / HR / Director) để tìm người.
5. Validate (nếu có thời gian):
   - Đặt giờ kết thúc trước giờ bắt đầu thì bị chặn (MSG26).
   - Đặt giờ bắt đầu ở quá khứ thì bị chặn: lịch mới phải ở tương lai.
6. Bấm **Lên lịch phỏng vấn**. Trang chuyển về danh sách và báo "Tạo lịch phỏng vấn thành công!".

### Cảnh 2 – Xem lịch theo từng role

- **HR / Director** (`hr_lan`, `director`) vào `/interviews` thấy toàn bộ lịch. HR có thêm Sửa lịch, Hủy lịch, Gửi lại Email.
- **Hiring Manager** (`hm_tuan`) chỉ thấy lịch của phòng Engineering. Đăng nhập `hm_huong` thì không thấy lịch của 4 ứng viên demo.
- **Interviewer** (`interviewer_nam`) chỉ thấy lịch mình được phân công, nút là **Kết quả & đánh giá**.
  - **GBR-05**: đăng nhập `interviewer_thao` thì không thấy lịch demo. Gõ thẳng `/interviews/{id của Bình}/result` thì bị từ chối truy cập.
- **Xem chi tiết**: bấm **Kết quả** trên một thẻ lịch. Trang chi tiết gồm Thông tin buổi phỏng vấn, Hội đồng (ai đã nộp phiếu), Phiếu đánh giá, Tổng hợp điểm và Kết quả cuối.
- **Ứng viên** (`demo.an`): trên dashboard bấm **Lịch phỏng vấn của tôi** (`/portal/interviews`) để thấy lịch vừa tạo ở cảnh 1. Ứng viên chỉ xem, không sửa được gì.
- **Chặn chấm trước giờ**: mở chi tiết lịch của An bằng `interviewer_nam`. Không có nút Đánh giá, chỉ có cảnh báo "Chưa đến giờ phỏng vấn...". Đây là lý do cần script: lịch tạo trên giao diện luôn ở tương lai nên không chấm ngay được.

### Cảnh 3 – Hội đồng chấm điểm (lịch của Trần Thị Bình)

Mở `/interviews/{id của Bình}/result` lần lượt bằng từng tài khoản và bấm **Đánh giá**.

| Tài khoản | Gợi ý điền | Điểm nên chỉ ra |
|---|---|---|
| `interviewer_nam` | Kỹ thuật 4, Kỹ năng mềm 3, Văn hóa 4, khuyến nghị Nên tuyển | Phiếu đầu tiên chuyển lịch từ Scheduled sang Completed |
| `hr_quang` | Kỹ thuật **Bỏ qua**, mềm 5, văn hóa 4, khuyến nghị Cân nhắc | Không bắt buộc chấm đủ 3 mục, HR chỉ chấm phần mình |
| `director` | Kỹ thuật 4, mềm 4, văn hóa 5, khuyến nghị Nên tuyển | Director cũng là thành viên hội đồng |
| `hm_tuan` | Kỹ thuật 5, mềm 4, văn hóa 4, khuyến nghị Nên tuyển | HM vừa chấm vừa là người chốt |

Các điểm nên chỉ ra thêm:
- Chọn **Bỏ qua** cả 3 mục điểm thì không gửi được, vì phải chấm ít nhất một mục.
- Gửi xong, nút Đánh giá biến mất: mỗi người chỉ nộp một phiếu.
- **D5**: trước khi chốt kết quả, `interviewer_nam` chỉ thấy phiếu của mình. HR, HM và Director thấy tất cả các phiếu.
- `hr_lan` mở lịch này thì không có nút Đánh giá, vì `hr_lan` tạo lịch nhưng không ngồi hội đồng.

### Cảnh 4 – HM chốt Đạt (`hm_tuan`, lịch của Bình)

1. Bấm **Chốt kết quả**, chọn **Đạt**, lương đề xuất `40000000`, nhận xét ngắn, rồi bấm **Chốt kết quả**.
2. Trang hiện khối Kết quả cuối. Nút Chốt kết quả biến mất: chốt xong là khóa, không chốt lại được.
3. Đăng nhập `interviewer_nam` mở lại lịch này: giờ đã thấy phiếu của cả hội đồng.
4. Để thấy kết quả "đỗ": HR mở hồ sơ của Bình, hồ sơ ở bước **"Đạt phỏng vấn, chờ lập offer"**. Phần lập offer là việc của module offer.
5. Chứng minh phân quyền: `hm_huong` (phòng Sales) không có nút Chốt kết quả. Các role khác cũng không có.

### Cảnh 5 – Chốt khi chưa đủ phiếu (`hm_tuan`, lịch của Lê Minh Chi)

1. Lịch đang có 2/4 phiếu: `interviewer_viet` khuyến nghị Không tuyển, `hr_lan` khuyến nghị Cân nhắc và bỏ qua điểm kỹ thuật. Mục Hội đồng cho thấy HM và Director chưa nộp.
2. Phần Tổng hợp điểm chỉ tính trung bình trên các mục có chấm (mục bỏ qua không bị tính là 0).
3. Bấm **Chốt kết quả**, chọn **Không đạt**. Lý do được chốt dù thiếu phiếu: hội đồng có thể không đến đủ.
4. Hồ sơ của Chi chuyển sang **Không đạt phỏng vấn**.
5. (Tùy chọn) `director` vẫn nộp phiếu muộn được sau khi đã chốt.

### Cảnh 6 – Kết quả đã khóa (lịch của Phạm Quốc Dũng)

- Đủ 4/4 phiếu, đã chốt **Đạt** với lương 42.000.000. Dùng lịch này để cho xem nhanh trạng thái cuối nếu thiếu thời gian.
- Đăng nhập `demo.dung` vào **Hồ sơ đã ứng tuyển**: hồ sơ ở bước chờ offer.

## 3. Nếu có sự cố

| Hiện tượng | Nguyên nhân | Cách xử lý |
|---|---|---|
| Không thấy An trong ô Hồ sơ ứng tuyển | Đã tạo lịch cho An lúc test, nên hồ sơ không còn ở bước chờ xếp lịch | Chạy lại script |
| "Thành viên hội đồng ... trùng vào khoảng thời gian" | Người đó đã có lịch khác cùng giờ | Đổi giờ, ví dụ 9:00 hoặc 10:30 ngày mai |
| Báo lỗi gửi email sau khi tạo lịch | Email ứng viên demo là địa chỉ giả, hoặc SMTP lỗi | Lịch vẫn đã được tạo. Báo lỗi này không chặn luồng |
| Script báo "Thiếu dữ liệu seed" | DB chưa có `seed_data.sql` | Chạy `seed_data.sql` trước |

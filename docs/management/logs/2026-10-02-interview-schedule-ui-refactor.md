# Báo cáo Thay đổi: Cải tiến Giao diện & Tách Module Lịch phỏng vấn (Interview Schedule)

- **Ngày thực hiện:** 2026-10-02
- **Người thực hiện:** Đức (Masanori-Satoh - Nhóm 2)
- **Nhánh (Branch):** `feature/iter1-interview-schedule_duc`
- **Module phụ trách:** `interview/schedule`

---

## 1. Yêu cầu / Quyết định (Requirements & Decisions)

1. **Khắc phục lỗi đăng nhập và kết nối cơ sở dữ liệu:**
   - Điều tra nguyên nhân lỗi đăng nhập: phân tích cấu hình `application.properties`, mã băm BCrypt của dữ liệu mẫu trong DB, và thông số tài khoản mặc định. Cập nhật `application.properties` để kết nối chính xác tới database `RitirementManagement2`.
2. **Khắc phục lỗi phông chữ tiếng Việt & Chuẩn hóa Typography theo Mockup:**
   - Rà soát encoding UTF-8 và áp dụng font chữ `Lora` (serif) kết hợp `Source Sans 3` (sans-serif) theo hệ thống Design Tokens từ tài liệu thiết kế chuẩn `docs/mockup/mockup_design.html`.
3. **Cố định vị trí Badge trạng thái trên Card phỏng vấn:**
   - Tạo khối bao (`slot`) cố định cho badge trạng thái cuộc họp, giải quyết triệt để tình trạng lệch tên ứng viên giữa các cột card trên lưới giao diện khi badge có độ dài khác nhau.
4. **Nâng cấp Bảng điều khiển Bộ lọc & Cuốn lịch mini tương tác:**
   - Xóa bỏ các dòng thông báo debug thừa và loại bỏ toàn bộ emoji cạnh nhãn bộ lọc để đảm bảo tính chuyên nghiệp của hệ thống doanh nghiệp.
   - Thêm Mini Calendar trực quan có chấm tròn (`cal-dot`) đánh dấu những ngày có lịch họp phỏng vấn. Mặc định chỉ hiển thị lịch trong ngày hôm nay (`today`).
   - Tích hợp bộ lọc đa chiều phong phú: theo khoảng thời gian (Hôm nay, Tuần này, Tháng này, Tất cả), vị trí tuyển dụng (Job Posting), cấp bậc (Job Level: Intern, Junior/Fresher, Middle, Senior, Lead), trạng thái (Scheduled, In Progress, Completed, Cancelled), và người phỏng vấn trong hội đồng (Panel Member).
   - Tìm kiếm từ khóa theo thời gian thực (tên ứng viên, email, mã hồ sơ, chức vụ).
   - Thêm thanh filter pills hiển thị các điều kiện lọc đang chọn, có nút xóa nhanh từng điều kiện hoặc xóa tất cả.
   - Trải nghiệm tương tác nâng cao: Hover card zoom-out nhẹ để hiển thị đầy đủ thông tin tránh khuất chữ; Bấm vào thẻ Job trên card để tự động lọc theo Job; Bấm vào tên người phỏng vấn để lọc theo người đó.
5. **Hoàn thiện thanh Topbar (Site Header) theo chuẩn Use Cases của RMS:**
   - Tích hợp nhận diện thương hiệu Mộc Careers trỏ về Dashboard (`/dashboard`).
   - Bổ sung đầy đủ menu điều hướng: Lịch phỏng vấn (`/interviews`), Đánh giá (`/interviews/evaluation`), CV ứng viên (`/candidates/cv`), Yêu cầu tuyển dụng (`/requisitions` - phân quyền cho Hiring Manager).
   - Bổ sung nút Thông báo (`/notifications`) kèm badge, và Dropdown menu Profile cá nhân: avatar ký tự đầu, họ tên, role, link Xem hồ sơ (`/profile`), Danh sách vị trí mở (`/jobs`), và Đăng xuất an toàn (`/logout`).
6. **Bảo toàn chức năng & Tinh chỉnh trải nghiệm người dùng:**
   - Khôi phục nút "Tạo lịch phỏng vấn" (`/interviews/new`) dành riêng cho quyền HR (`th:if="${isHr}"`).
   - Tinh chỉnh nút thu gọn/mở rộng bảng bộ lọc đặt gọn gàng ở góc dưới bên phải khung điều khiển.
7. **Refactor mã nguồn & Tách riêng CSS / JS (Clean Code & Separation of Concerns):**
   - Tách toàn bộ CSS inline thành file riêng `src/main/resources/static/css/interview-list.css`.
   - Tách toàn bộ JS inline thành file riêng `src/main/resources/static/js/interview-list.js`.
   - Giảm dung lượng file `list.html` từ 1.551 dòng xuống còn 392 dòng dễ bảo trì.
   - Nghiêm túc tuân thủ ranh giới module, không can thiệp hay gây ảnh hưởng sang code của các thành viên khác.

---

## 2. Thay đổi Cơ sở dữ liệu (Database)

- **Cấu trúc bảng (Schema):** Không thay đổi.
- **Dữ liệu mẫu (Seeds):** Không thay đổi.
- **Cấu hình kết nối:** Cập nhật file `src/main/resources/application.properties` (được gitignore an toàn) để trỏ đúng DB `RitirementManagement2`.

---

## 3. Mã nguồn & Tài liệu (Source Code & Docs)

### 3.1. Các file tạo mới:
- `src/main/resources/static/css/interview-list.css`: File stylesheet chuyên biệt cho màn hình danh sách phỏng vấn (Design Tokens, Topbar, Filter Dashboard, Mini Calendar, Card Grid, Status Badges, Responsive).
- `src/main/resources/static/js/interview-list.js`: File xử lý logic Client-side (Trích xuất dữ liệu, render mini calendar, lọc đa tiêu chí, filter pills, thu gọn bộ lọc, menu profile).
- `docs/management/logs/2026-10-02-interview-schedule-ui-refactor.md`: Báo cáo chi tiết này.
- `docs/members/duc/duc-interview-schedule-worklog.md`: Nhật ký cá nhân theo dõi công việc thành viên.

### 3.2. Các file chỉnh sửa:
- `src/main/resources/templates/interview/list.html`: Xóa bỏ các khối `<style>` và `<script>` nội dòng, thay thế bằng `<link rel="stylesheet">` và `<script src="...">` liên kết tới các file static tương ứng; làm sạch và tối ưu cấu trúc HTML.
- `docs/management/WORK_LOG.md`: Bổ sung mục nhật ký công việc vào bảng theo dõi chung của dự án.

---

## 4. Kiểm thử (Testing)

1. **Kiểm tra biên dịch dự án:**
   - Lệnh thực thi: `.\mvnw.cmd test-compile`
   - Kết quả: `BUILD SUCCESS` (toàn bộ resource và mã nguồn biên dịch thành công, không có lỗi cú pháp hay thiếu phụ thuộc).
2. **Kiểm tra giao diện & Chức năng Client-side:**
   - Font chữ hiển thị chuẩn tiếng Việt, không bị lỗi font hay vỡ dấu.
   - Thẻ card phỏng vấn thẳng hàng, badge trạng thái nằm đúng slot cố định.
   - Hiệu ứng hover phóng to nhẹ và click-to-filter hoạt động chính xác.
   - Mini calendar hiển thị đúng tháng/năm hiện tại, đánh dấu chấm chính xác các ngày có cuộc họp.
   - Mặc định chỉ hiển thị lịch họp của "Hôm nay", chuyển đổi linh hoạt qua các preset tuần/tháng/tất cả hoặc click ngày trên lịch.
   - Nút thu gọn / mở rộng bộ lọc hoạt động mượt mà ở góc dưới bên phải.
   - Nút "Tạo lịch phỏng vấn" hiển thị đúng điều kiện khi người dùng đăng nhập là HR.
   - Menu Topbar hiển thị đầy đủ, dropdown thông tin cá nhân mở/đóng chính xác.

---

## 5. Vấn đề tồn đọng & Kế hoạch tiếp theo (To-Do)

1. Tiếp tục triển khai màn hình Tạo mới lịch phỏng vấn (`/interviews/new`) và Chỉnh sửa lịch phỏng vấn (`/interviews/{id}/edit`).
2. Tích hợp chức năng Đánh giá phỏng vấn (`/interviews/evaluation`) theo đúng tiến độ Iteration 1.
3. Liên kết luồng thông báo thực tế khi module Notification của nhóm được hoàn thiện.

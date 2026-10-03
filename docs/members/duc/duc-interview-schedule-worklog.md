# Báo cáo tiến độ cá nhân (Member Work Log) — Đức

- **Thành viên:** Đức (Masanori-Satoh)
- **Nhánh làm việc:** `feature/iter1-interview-schedule_duc`
- **Module phụ trách:** `interview` (Lịch phỏng vấn & Đánh giá phỏng vấn)

---

## 1. Đã hoàn thành (Done)

- **Khắc phục lỗi đăng nhập & Cấu hình môi trường:**
  - Phân tích nguyên nhân không thể đăng nhập do sai lệch cấu hình database và mật khẩu mã hóa BCrypt.
  - Cập nhật chuẩn hóa `application.properties` để kết nối đúng database `RitirementManagement2`.
- **Chuẩn hóa Giao diện & Hệ thống Typography Mộc Careers:**
  - Fix lỗi phông chữ tiếng Việt hiển thị sai trên giao diện phỏng vấn.
  - Đồng bộ typography theo chuẩn thiết kế `docs/mockup/mockup_design.html` với font `Lora` và `Source Sans 3`.
- **Ổn định bố cục thẻ lịch phỏng vấn (Card Layout Alignment):**
  - Cố định vùng hiển thị trạng thái (`.card-status-slot` / fixed height) giúp tên ứng viên và các thông tin bên dưới luôn thẳng hàng trên cùng một hàng ngang của grid, bất kể trạng thái badge ngắn hay dài.
- **Xây dựng Bảng điều khiển Bộ lọc & Lịch mini (Filter & Interactive Calendar):**
  - Dọn dẹp các dòng debug text và loại bỏ emoji bên cạnh tên categories để giao diện gọn gàng, tinh tế.
  - Xây dựng widget Mini Calendar tự động tính toán số cuộc họp theo từng ngày và hiển thị dấu chấm đỏ/xanh chỉ báo. Mặc định chỉ hiển thị các cuộc phỏng vấn trong ngày hôm nay.
  - Hỗ trợ các nút chọn nhanh: *Hôm nay, Tuần này, Tháng này, Xem tất cả*.
  - Bổ sung bộ lọc đa tiêu chí: theo Vị trí tuyển dụng, Cấp bậc (Senior, Lead, Junior, Intern, ...), Trạng thái, và Thành viên hội đồng phỏng vấn.
  - Tìm kiếm trực tiếp theo tên ứng viên, email, mã hồ sơ.
  - Hiển thị danh sách Filter Pills đang áp dụng với nút xóa từng bộ lọc và nút xóa tất cả.
  - Thêm tính năng tương tác nhanh: Hover card zoom-out nhẹ để hiển thị trọn vẹn thông tin; Bấm vào thẻ Job trên card để auto-filter theo Job; Bấm vào tên người phỏng vấn để auto-filter theo người đó.
- **Hoàn thiện thanh Topbar (Site Header) theo UC hệ thống:**
  - Tích hợp đầy đủ các chức năng theo Use Case chuẩn: Logo về Dashboard, Lịch phỏng vấn, Đánh giá, CV ứng viên, Yêu cầu tuyển dụng (cho HiringManager).
  - Chuông thông báo và Dropdown menu thông tin cá nhân (Xem profile, Danh sách việc làm, Đăng xuất).
- **Tinh chỉnh giao diện & Phục hồi chức năng:**
  - Khôi phục nút hành động "Tạo lịch phỏng vấn" dành cho vai trò HR.
  - Bố trí nút thu gọn / mở rộng bảng bộ lọc ở góc dưới bên phải trực quan, kèm hiệu ứng chuyển đổi mượt mà.
- **Đồng bộ Main (PR7 & PR8) & Giải quyết Xung đột Kiến trúc (2026-10-03):**
  - Thực hiện merge `origin/main` vào nhánh `feature/iter1-interview-schedule_duc`, giải quyết xung đột modify/delete ở thực thể `InterviewSchedule.java` (giữ bản trong `entity/`, xóa bản cũ ở root package).
  - Cập nhật import cho 5 tệp trong module `interview` tương thích với cấu trúc package mới của `candidate` (`Application`, `ApplicationRepository`).
  - Thiết lập cấu hình local `application-local.properties` an toàn, giữ mật khẩu máy cá nhân ngoài Git.
  - Quy hoạch lại tài liệu: dời `docs/mockup/` về `docs/members/duc/mockup/mockup_design.html` theo chuẩn `docs/README.md`.
  - Biên dịch toàn dự án thành công (`mvnw test-compile`: BUILD SUCCESS) và bộ test nghiệp vụ interview đạt 5/5 pass 100%. Chi tiết tại [work_logs/2026-10-03-merge-main-resolve-conflicts.md](../../management/work_logs/2026-10-03-merge-main-resolve-conflicts.md).

---

## 2. Công việc tiếp theo (To-Do)

1. Xây dựng giao diện và luồng xử lý cho màn hình Tạo mới lịch phỏng vấn (`/interviews/new`).
2. Xây dựng màn hình Chi tiết lịch phỏng vấn (`/interviews/{id}`).
3. Tích hợp tính năng Đánh giá phỏng vấn (`/interviews/evaluation`).

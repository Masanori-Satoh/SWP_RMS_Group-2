# Nhật ký Công việc: Sửa lỗi Droplist User Profile / Đăng xuất trên Topbar `/interviews`

- **Ngày thực hiện:** 2026-10-03
- **Người thực hiện:** Đức (Masanori-Satoh)
- **Nhiệm vụ:** Sửa lỗi giao diện web trên màn hình `/interviews`: tại thanh topbar, droplist để hiển thị profile và đăng xuất bị liệt (click vào không hiển thị ra). Tuân thủ quy chuẩn kiến trúc [ARCHITECTURE_GUIDE.md](../../architecture/ARCHITECTURE_GUIDE.md) và quy trình ghi log theo [dunglt-2026-10-01-update-quy-trinh.md](../../members/dunglt/dunglt-2026-10-01-update-quy-trinh.md).

---

## 1. Yêu cầu & Quyết định Kỹ thuật

### 1.1 Nguyên nhân gốc rễ (Root Cause)
1. **Xung đột sự kiện (Event Listener Collision):**
   - File [global.js](../../../src/main/resources/static/js/global.js) đã đăng ký sự kiện `click` để toggle class `open` cho mọi `.user-menu` và trigger `.user-trigger`.
   - Trong khi đó, [interview-list.js](../../../src/main/resources/static/js/interview-list.js) cũng tự bọc một IIFE đăng ký thêm một sự kiện `click` vào cùng `#user-menu-trigger` để toggle class `open`.
   - Khi người dùng click vào trigger, cả 2 listener cùng kích hoạt trong một tick: listener thứ nhất thêm class `open`, listener thứ hai ngay lập tức gỡ bỏ class `open`. Kết quả là dropdown không bao giờ duy trì class `open`, tạo cảm giác nút bị "liệt" hoàn toàn.
2. **Vi phạm kiến trúc phân chia Frontend ([ARCHITECTURE_GUIDE.md](../../architecture/ARCHITECTURE_GUIDE.md) §3):**
   - Quy chuẩn yêu cầu: *"Logic JS dùng chung (như Toggle Header, Menu Dropdown, Notification) phải đặt ở `static/js/global.js`"* và *"Style, mã màu chung, typography và layout gốc phải đặt ở `static/css/global.css`"*.
   - Trước đó, các CSS style của `.user-menu`, `.user-trigger`, `.user-dropdown`, `.notif-btn`, `.header-right` lại nằm cục bộ ở [interview-list.css](../../../src/main/resources/static/css/interview-list.css), đồng thời logic JS bị duplicate trong file script riêng của trang.

### 1.2 Giải pháp khắc phục
1. **Chuẩn hóa CSS chung vào `global.css`:**
   - Di chuyển các class dùng chung của topbar header (`.nav-divider`, `.header-right`, `.notif-btn`, `.notif-badge`, `.user-menu`, `.user-trigger`, `.user-avatar`, `.user-role-tag`, `.user-dropdown`, `.dropdown-header`, `.dropdown-item`, `.dropdown-divider`) vào [global.css](../../../src/main/resources/static/css/global.css).
   - Đảm bảo `.user-dropdown.open` hiển thị với `display: block !important;` và `z-index: 200` để không bị đè bởi các container bên dưới.
   - Loại bỏ các khối CSS trùng lặp trong [interview-list.css](../../../src/main/resources/static/css/interview-list.css).
2. **Củng cố Logic JS dùng chung trong `global.js`:**
   - Xóa bỏ hoàn toàn đoạn code dropdown bị lặp trong [interview-list.js](../../../src/main/resources/static/js/interview-list.js).
   - Nâng cấp hàm khởi tạo trong [global.js](../../../src/main/resources/static/js/global.js):
     - Chạy an toàn với cả `DOMContentLoaded` lẫn khi trang đã load xong (`document.readyState`).
     - Đặt cờ `dataset.dropdownBound` để chống bind nhiều lần.
     - Đóng các menu khác nếu đang mở khi bật menu mới.
     - Hỗ trợ đóng menu khi click ra ngoài hoặc khi ấn phím `Escape`.
     - Cập nhật đúng thuộc tính `aria-expanded` (chuẩn accessibility WAI-ARIA).

---

## 2. Thay đổi Cơ sở dữ liệu (Database)
- Không có thay đổi cấu trúc DB hay dữ liệu seed.

---

## 3. Thay đổi Mã nguồn & Tài liệu

| Tệp tin | Thao tác | Mô tả chi tiết |
| :--- | :---: | :--- |
| `src/main/resources/static/css/global.css` | Chỉnh sửa | Đưa toàn bộ styling của Header Right, Notification button, User Menu & Dropdown từ `interview-list.css` về `global.css` theo đúng §3 Architecture Guide. |
| `src/main/resources/static/css/interview-list.css` | Chỉnh sửa | Loại bỏ hơn 130 dòng CSS bị duplicate (header, user menu, alert, base button) để file chỉ tập trung cho bộ lọc, mini calendar và grid phỏng vấn. |
| `src/main/resources/static/js/global.js` | Chỉnh sửa | Cải tiến logic toggle dropdown người dùng: hỗ trợ Escape key, click-outside, quản lý `aria-expanded`, chống double-binding và chạy an toàn qua document readyState. |
| `src/main/resources/static/js/interview-list.js` | Chỉnh sửa | Xóa bỏ đoạn mã duplicate toggle user-dropdown (nguyên nhân gây bug click 2 lần làm liệt menu). |
| `docs/management/work_logs/2026-10-03-fix-interview-topbar-dropdown.md` | Tạo mới | Ghi chép chi tiết nguyên nhân, giải pháp và kết quả kiểm thử giao diện. |
| `docs/management/WORK_LOG.md` | Chỉnh sửa | Bổ sung dòng tóm tắt công việc vào bảng theo dõi tiến độ. |

---

## 4. Kiểm thử & Xác minh (Testing)

### 4.1 Kiểm thử tự động trên Browser (Browser Subagent)
- **Môi trường:** Chrome Headless / Automation trên `http://localhost:8080`.
- **Kịch bản:**
  1. Đăng nhập với tài khoản HR `hr_lan` / `12345678`.
  2. Điều hướng đến `/interviews`.
  3. Kiểm tra topbar hiển thị nút người dùng "Lê Thị Mai Lan" (Role: HR).
  4. Click vào `#user-menu-trigger`:
     - Dropdown `#user-dropdown` mở ra lập tức, hiển thị:
       - Tên: **Lê Thị Mai Lan**
       - Email: **lan.le@rms-tech.vn**
       - Nút điều hướng: **Thông tin cá nhân** (`/profile`)
       - Nút điều hướng: **Danh sách vị trí mở** (`/jobs`)
       - Nút hành động: **Đăng xuất** (form POST `/logout`)
  5. Click lại vào trigger hoặc click ra ngoài: Dropdown thu gọn và đóng lại mượt mà.
- **Kết quả:** Đạt 100%.

### 4.2 Kiểm thử Backend Unit Test
- **Lệnh chạy:** `.\mvnw.cmd test "-Dtest=InterviewSchedulingServiceTests"`
- **Kết quả:**
  ```text
  [INFO] Running com.group2.rms.interview.InterviewSchedulingServiceTests
  [INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.504 s
  [INFO] BUILD SUCCESS
  ```

---

## 5. Vấn đề Tồn đọng & Bước tiếp theo
- Kiểm tra tính tương thích của topbar dropdown trên các trang khác (`/jobs`, `/profile`) để đảm bảo trải nghiệm thống nhất trên toàn hệ thống.

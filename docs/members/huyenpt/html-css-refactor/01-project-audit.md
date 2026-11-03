# BÁO CÁO KIỂM KÊ & KHẢO SÁT DỰ ÁN (PROJECT AUDIT)
**Nhiệm vụ:** Task 01 — Safe HTML/CSS Refactoring Audit  
**Hệ thống:** Recruitment Management System (Mộc RMS - Group 2, SWP391)  
**Phạm vi phụ trách:** HuyenPT — Phân hệ Quản lý Đề xuất & Thư mời nhận việc (Offer Management)  
**Nhánh Git:** `refactor/huyenpt/offer-ui-js-to-html-css`  
**Ngày thực hiện:** 04/11/2026  
**Nguyên tắc bất biến:** Phân tích khảo sát 100%, KHÔNG tự ý sửa đổi mã nguồn, KHÔNG phá vỡ hành vi hiện tại.

---

## 1. Kiến trúc Frontend, Công cụ Build & Cơ chế Điều hướng (Routing)

### 1.1. Khung kiến trúc Frontend (Frontend Architecture)
* **Mô hình kiến trúc:** Ứng dụng Web Đa trang (Multi-Page Application - **MPA**) với kết xuất giao diện hoàn toàn từ máy chủ (**Server-Side Rendering - SSR**).
* **Công nghệ View Template:** **Thymeleaf 3.1.2.RELEASE** kết hợp phương ngữ bố cục `thymeleaf-layout-dialect`.
* **Công cụ xây dựng (Build Tools):** **Apache Maven 3.9+** với Java 21 (`pom.xml`).
  * Hệ thống **KHÔNG SỬ DỤNG** bất kỳ bộ công cụ đóng gói JavaScript nào như Webpack, Vite, Rollup, Parcel, Babel, hay Gulp.
  * Hệ thống **KHÔNG CÓ** thư mục `node_modules` hay file `package.json`.
  * Toàn bộ tài nguyên Frontend (CSS, Vanilla JavaScript, Icons, Images) là các tệp tĩnh nguyên bản (Static Assets) được lưu trữ tại `src/main/resources/static/` và được Spring Boot phân phát trực tiếp qua `ResourceHttpRequestHandler`.

### 1.2. Cơ chế Điều hướng & Quản lý Trạng thái (Routing & State Management)
* **Routing Cơ bản:** Điều hướng URL máy chủ thông qua Spring MVC Controllers:
  * [`com.group2.rms.offer.controller.OfferController`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java): Quản lý các route quản trị nội bộ (`/offers`, `/offers/create`, `/offers/{id}`, `/offers/{id}/edit`).
  * [`com.group2.rms.offer.controller.CandidateOfferController`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/CandidateOfferController.java): Quản lý các route Cổng thông tin Ứng viên (`/portal/offers`, `/portal/offers/{id}`).
* **Mô hình Xử lý Form:** Triển khai mô hình tiêu chuẩn **PRG (Post/Redirect/Get)**:
  * Client gửi `POST` $\rightarrow$ Server xử lý nghiệp vụ trong `@Transactional` $\rightarrow$ Server phản hồi `HTTP 302 Redirect` về URL GET $\rightarrow$ Trình duyệt hiển thị View mới kèm Flash Attributes thông báo.
* **Tương tác Cục bộ (In-Page Interactions):** 
  * Hiện tại có sự pha trộn giữa form POST truyền thống và các hàm Ajax Fetch JSON bất đồng bộ (như thao tác Giám đốc phê duyệt nhanh `/offers/{id}/approve` qua modal dialog).

### 1.3. Cấu trúc Thư mục Frontend Toàn cục

```
src/main/resources/
├── static/
│   ├── css/
│   │   ├── main.css                         (CSS nền tảng dùng chung toàn hệ thống)
│   │   ├── offers.css                       (CSS phân hệ quản trị Offer nội bộ - 2,348 dòng)
│   │   └── pages/
│   │       ├── candidate-offers.css         (CSS thư mời trang trọng cho ứng viên - 2,513 dòng)
│   │       ├── offer-workspace.css          (CSS bố cục form tạo mới/chỉnh sửa - 1,223 dòng)
│   │       └── interview-common.css         (CSS dùng chung phân hệ tuyển dụng)
│   └── js/
│       ├── offers.js                        (JavaScript phân hệ Offer HR & Director - 2,005 dòng)
│       ├── global.js                        (Script tiện ích toàn cục)
│       ├── topbar-notifications.js          (Polling thông báo chuông trên topbar - 271 dòng)
│       └── pages/
│           ├── candidate-offers.js          (JavaScript cổng ứng viên - 285 dòng)
│           └── flash-toast.js               (Script hiển thị thông báo nổi - 56 dòng, DÙNG CHUNG)
└── templates/
    ├── fragments/                           (Header, sidebar, modal dùng chung)
    └── offers/                              (6 Template HTML phân hệ Offer)
        ├── list.html                        (Screen 30: Danh sách quản lý Offer của HR - 508 dòng)
        ├── form.html                        (Screen 30: Tạo mới & chỉnh sửa Offer - 291 dòng)
        ├── detail.html                      (Screen 32: Chi tiết Offer nội bộ - 490 dòng)
        ├── director-list.html               (Bàn làm việc phê duyệt của Giám đốc - 284 dòng)
        ├── candidate-list.html              (Danh sách thư mời của ứng viên - 215 dòng)
        └── candidate-detail.html            (Letterhead chi tiết thư mời ứng viên - 374 dòng)
```

---

## 2. Kiểm kê Chi tiết Tệp tin JavaScript & Template HTML

### 2.1. Tệp tin JavaScript Phân hệ Offer Quản lý Trực tiếp
1. **[`src/main/resources/static/js/offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js)**:
   * **Kích thước:** 2,005 dòng (88,569 bytes).
   * **Số lượng hàm:** 77 functions.
   * **Số lượng Event Listeners:** 7 listeners.
   * **Phạm vi phục vụ:** `offers/list.html`, `offers/form.html`, `offers/detail.html`, `offers/director-list.html`.
   * **Tính chất:** File nguyên khối (monolithic script), chứa logic lọc danh sách, mở đóng modal `<dialog>`, đếm ký tự, tính toán tự động 85% lương, render HTML động dạng template strings, và gọi Ajax API.
2. **[`src/main/resources/static/js/pages/candidate-offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/candidate-offers.js)**:
   * **Kích thước:** 285 dòng (10,781 bytes).
   * **Số lượng hàm:** 5 functions chính.
   * **Số lượng Event Listeners:** 14 listeners.
   * **Phạm vi phục vụ:** `offers/candidate-list.html`, `offers/candidate-detail.html`.
   * **Tính chất:** Điều khiển modal chấp thuận/từ chối, bộ đếm ngược thời gian thực (`Countdown Timer`), gán chip lý do từ chối vào textarea, và xử lý sự kiện `pageshow`.

### 2.2. Kiểm tra Thẻ `<script>` Nhúng Nội tuyến (Inline Scripts)
* **Kết quả quét toàn bộ 6 file template tại `src/main/resources/templates/offers/*.html`:**
  * **0 thẻ `<script>` nội tuyến.** Hệ thống tuân thủ nghiêm ngặt quy tắc tách biệt mã nguồn: Không có bất kỳ khối mã JavaScript nào viết trực tiếp trong thẻ `<script>...</script>` của HTML.
  * Cả 6 template đều chỉ nạp script qua thẻ liên kết ngoài:
    * `detail.html`, `director-list.html`, `form.html`, `list.html`: Nạp `pages/flash-toast.js` và `offers.js` (gắn cờ `defer`).
    * `candidate-list.html`, `candidate-detail.html`: Nạp `pages/flash-toast.js` và `pages/candidate-offers.js` (gắn cờ `defer`).

---

## 3. Tệp tin và Sự phụ thuộc Thuộc Quyền Sở hữu của Offer Management (HuyenPT)

Toàn bộ các tệp tin dưới đây thuộc quyền sở hữu độc quyền của phân hệ Offer Management và nằm trong phạm vi được phép đề xuất tối ưu hóa:

| Nhóm tệp tin | Đường dẫn tệp tin | Số dòng | Vai trò |
| :--- | :--- | :---: | :--- |
| **Thymeleaf Template** | `src/main/resources/templates/offers/list.html` | 508 | Giao diện quản lý danh sách của HR |
| **Thymeleaf Template** | `src/main/resources/templates/offers/form.html` | 291 | Giao diện form lập đề xuất và chỉnh sửa |
| **Thymeleaf Template** | `src/main/resources/templates/offers/detail.html` | 490 | Giao diện xem chi tiết đề xuất và phát hành |
| **Thymeleaf Template** | `src/main/resources/templates/offers/director-list.html` | 284 | Bàn làm việc phê duyệt của Giám đốc |
| **Thymeleaf Template** | `src/main/resources/templates/offers/candidate-list.html` | 215 | Cổng ứng viên xem danh sách thư mời |
| **Thymeleaf Template** | `src/main/resources/templates/offers/candidate-detail.html` | 374 | Thư mời trang trọng và phản hồi trực tuyến |
| **Dedicated JavaScript**| `src/main/resources/static/js/offers.js` | 2,005 | Logic tương tác quản trị HR & Director |
| **Dedicated JavaScript**| `src/main/resources/static/js/pages/candidate-offers.js` | 285 | Logic đếm ngược và modal cổng ứng viên |
| **Dedicated Stylesheet**| `src/main/resources/static/css/offers.css` | 2,348 | Định kiểu bảng, thẻ KPI, modal quản trị |
| **Dedicated Stylesheet**| `src/main/resources/static/css/pages/candidate-offers.css`| 2,513 | Bố cục letterhead 3 cột cổng ứng viên |
| **Dedicated Stylesheet**| `src/main/resources/static/css/pages/offer-workspace.css` | 1,223 | Định kiểu form tạo mới và chỉnh sửa |

---

## 4. Tệp tin và Sự phụ thuộc Dùng chung (Shared Dependencies - BẤT KHẢ XÂM PHẠM)

Các tệp tin sau đây được chia sẻ xuyên suốt với các thành viên khác trong nhóm (DungLT, HoangNH, DucNM). **TUYỆT ĐỐI KHÔNG ĐƯỢC PHÉP CHỈNH SỬA HOẶC XÓA BỎ** trong phạm vi refactor HTML/CSS của HuyenPT:

1. **[`src/main/resources/static/js/pages/flash-toast.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/flash-toast.js)** (56 dòng):
   * **Sử dụng bởi 19 template khác nhau:** `admin/accounts/*`, `candidate/applications.html`, `interview/*`, `job-postings/*`, `requisitions/*`, `talent-pool/*`.
   * **Trạng thái:** **BLOCKED** đối với mọi chỉnh sửa trực tiếp. Phân hệ Offer chỉ đọc và kế thừa cơ chế hiển thị thông báo flash.
2. **[`src/main/resources/static/js/topbar-notifications.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/topbar-notifications.js)** (271 dòng):
   * Phụ trách việc polling định kỳ `/api/notifications/unread-count` và cập nhật chuông thông báo trên thanh topbar chung của ứng dụng.
   * **Trạng thái:** **BLOCKED**. Thuộc module Core/Admin.
3. **[`src/main/resources/static/css/main.css`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/main.css)** (345 dòng):
   * Chứa các biến màu sắc gốc (`--primary-color`, `--text-color`), reset CSS toàn trang và hệ thống typography.
   * **Trạng thái:** **BLOCKED**. Mọi điều chỉnh giao diện của Offer phải viết trong `offers.css` hoặc `candidate-offers.css`.
4. **Các dịch vụ Backend dùng chung (Backend Services & Entities):**
   * [`ApplicationStatusService.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/domain/rule/ApplicationStatusService.java): Đồng bộ trạng thái đơn tuyển dụng.
   * [`Application.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/domain/entity/Application.java), [`Candidate.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/domain/entity/Candidate.java), [`User.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/user/entity/User.java).
   * **Trạng thái:** **BẤT BIẾN 100%**. Không thay đổi interface, method hay schema.

---

## 5. Kết luận Giai đoạn Khảo sát (Audit Summary)
* **Tổng số file JavaScript liên quan cần phân tích:** **2 file** cốt lõi (`offers.js` và `candidate-offers.js`), tổng cộng **2,290 dòng mã nguồn**.
* **Độ phức tạp hiện tại:** `offers.js` hiện đảm nhận quá nhiều trách nhiệm (lọc, đếm, tính toán, Ajax, DOM injection, format dữ liệu) mà phần lớn có thể giải quyết thanh thoát hơn thông qua kết xuất phía máy chủ của Thymeleaf và các thuộc tính native của HTML5/CSS3.
* **Cơ hội tái cấu trúc an toàn:** Rất cao, đặc biệt ở các khu vực form validation, tab filtering, dialog dismiss, và auto-calculation.


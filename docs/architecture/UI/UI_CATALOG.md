# Bảng Tra Cứu Linh Kiện Giao Diện (UI Catalog for RMS)

> **CẨM NANG TRA CỨU NHANH DÀNH CHO DEVELOPERS & TRỢ LÝ AI**  
> **Nguyên tắc vàng:** Tra cứu Tên Class BEM, Biến CSS Tokens và Cú pháp Thymeleaf Fragment tại đây TRƯỚC KHI viết code. Tuyệt đối không tự bịa class mới hoặc style inline nếu catalog đã định nghĩa.  
> **Lộ trình & file nào cho việc gì:** [`README.md`](README.md). Tạo trang mới: [`UI_NEW_PAGE.md`](UI_NEW_PAGE.md). Migrate trang cũ: [`UI_MIGRATION.md`](UI_MIGRATION.md). Viết CSS: [`UI_CSS_GUIDE.md`](UI_CSS_GUIDE.md).

---

## Mục Lục Tra Cứu Nhanh
1. [Hệ Thống 4 Master Layouts](#1-hệ-thống-4-master-layouts)
2. [Hệ Thống Biến & Tokens CSS (`tokens.css`)](#2-hệ-thống-biến--tokens-css-tokenscss)
3. [Nút Bấm & Hành Động (Buttons DNA)](#3-nút-bấm--hành-động-buttons-dna)
4. [Huy Hiệu Trạng Thái (Badges & Tags)](#4-huy-hiệu-trạng-thái-badges--tags)
5. [Biểu Mẫu Nhập Liệu (Form Controls)](#5-biểu-mẫu-nhập-liệu-form-controls)
6. [Thẻ Chứa, Card & Lưới Bố Cục (Cards & Layout Grid)](#6-thẻ-chứa-card--lưới-bố-cục-cards--layout-grid)
7. [Bảng Dữ Liệu (Data Tables)](#7-bảng-dữ-liệu-data-tables)
8. [Hộp Thoại & Thông Báo (Dialogs & Alerts)](#8-hộp-thoại--thông-báo-dialogs--alerts)
9. [Không Gian Làm Việc Nội Bộ (Workspace Topbar & Sidebar)](#9-không-gian-làm-việc-nội-bộ-workspace-topbar--sidebar)
10. [Linh Kiện Tái Sử Dụng Bằng Fragment (`templates/fragments/ui/`)](#10-linh-kiện-tái-sử-dụng-bằng-fragment)
11. [Menu Thao Tác Khác "⋯" (More Actions)](#11-menu-thao-tác-khác--more-actions)

---

## 1. Hệ Thống 4 Master Layouts

Mọi trang HTML con kế thừa thông qua: `layout:decorate="~{layout/[tên-layout]}"`.

| Tên Layout | Mục đích & Loại trang sử dụng | Bố cục & Thành phần cố định | Cú pháp Decorate |
|---|---|---|---|
| **`layout/internal`** | **Tất cả các trang nghiệp vụ nội bộ:** Dashboard, Requisitions, Job Postings, Interviews, Offers, Admin Accounts, Departments, Candidate Portal, Profile | • Sidebar đa trạng thái (thu gọn 68px, mở rộng 250px)<br>• Topbar nội bộ: Avatar + Tên + Role + Email (click về Profile) + Nút Đăng xuất chuẩn DNA kèm Modal xác nhận | `layout:decorate="~{layout/internal}"` |
| **`layout/public`** | **Trang cổng thông tin công khai:** Jobs Board `/jobs` (tìm kiếm + phân trang), Chi tiết vị trí `/jobs/{id}`. *(Landing `/` = `candidate/landing.html` là ngoại lệ tự dựng khung nhưng vẫn dùng topbar công khai.)* | • Topbar công khai (`public-topbar`: Logo + Menu + Trạng thái đăng nhập)<br>• Footer thương hiệu 4 cột + Bản quyền | `layout:decorate="~{layout/public}"` |
| **`layout/auth`** | **Trang xác thực & Ngoại lệ:** Login `/login`, Register, Forgot Password, Reset Password, 403, 404, 500 | • Centered Card: Khung thẻ căn giữa màn hình sang trọng<br>• Không có sidebar hay header phức tạp | `layout:decorate="~{layout/auth}"` |
| **`layout/base`** | **Khung xương tầng 1 (Core Shell)** | • Định nghĩa HTML5, Head Merging, CSRF meta tags, nạp CSS Tokens toàn cục. View nghiệp vụ không decorate trực tiếp vào file này. | *(Chỉ để layout trên kế thừa)* |

---

## 2. Hệ Thống Biến & Tokens CSS (`tokens.css`)

> **Quy định:** CẤM viết mã màu Hex (`#3e7157`, `#252923`, v.v.) trực tiếp trong template HTML hoặc CSS con. Bắt buộc dùng `var(--tên-token)`.

### A. Màu Sắc Thương Hiệu Mộc & Giao Diện
| Tên Biến Token | Giá trị Hex | Ý nghĩa & Vị trí áp dụng |
|---|---|---|
| `var(--brand)` | `#3e7157` | Màu xanh thương hiệu chính (Nút bấm chính, liên kết nổi bật, viền focus) |
| `var(--brand-dark)` | `#2e5a45` | Xanh đậm hơn (Hover trạng thái nút chính, tiêu đề quan trọng) |
| `var(--brand-soft)` | `#e7f0e8` | Xanh nhạt dịu mắt (Nền Badge Success, Avatar fallback, Menu active) |
| `var(--page)` | `#faf9f6` | Nền toàn trang (Off-white ấm áp phong cách Mộc) |
| `var(--surface)` | `#ffffff` | Nền thẻ Card, Modal dialog, Input form, Dropdown |
| `var(--ink)` | `#252923` | Màu chữ chính, độ tương phản cao, dễ đọc |
| `var(--muted)` | `#59635b` | Màu chữ phụ, mô tả, chú thích ngày giờ, nhãn meta |
| `var(--line)` | `#d9ded7` | Viền thẻ Card, đường phân cách divider, viền bảng Table |
| `var(--control-line)` | `#7b8b7d` | Viền ô nhập liệu Input, Select lúc bình thường |
| `var(--focus)` | `#a06325` | Màu cam đất ấm áp khi focus hoặc highlight mức lương |

### B. Màu Sắc Trạng Thái (Semantic Colors)
| Trạng Thái | Token Màu Chữ / Icon | Token Màu Nền | Ví dụ áp dụng |
|---|---|---|---|
| **Thành công (Success)** | `var(--success)` (`#2e5a45`) | `var(--success-soft)` (`#e7f0e8`) | Đã duyệt, Hoạt động, Đã tuyển, Trúng tuyển |
| **Cảnh báo (Warning)** | `var(--warning)` (`#a06325`) | `var(--warning-soft)` (`#fff4e5`) | Chờ duyệt, Chờ phỏng vấn, Đang xử lý |
| **Nguy hiểm (Error/Danger)** | `var(--error)` (`#87372f`) | `var(--error-soft)` (`#f7ece8`) | Bị từ chối, Đã hủy, Bị khóa tài khoản, Lỗi form |
| **Trung tính (Neutral)** | `var(--neutral)` (`#59635b`) | `var(--neutral-soft)` (`#f1f5f9`) | Bản nháp, Loại hợp đồng, Mã phòng ban |

### C. Bo Góc & Khoảng Cách
| Phân loại | Tên Biến Token | Giá trị | Ứng dụng |
|---|---|---|---|
| **Bo góc** | `var(--radius-sm)` | `4px` | Phân trang `.page-link`, viền phụ chi tiết |
| | `var(--radius-md)` | `8px` | **Chuẩn DNA C++ chính:** Nút bấm `.btn`, Nút nhỏ `.btn--sm`, Ô Input `.form-input`, Thẻ `.card`, Modal `.confirm-dialog` |
| | `var(--radius-lg)` | `12px` | Khung lớn, Hero container, Panel lớn |
| | `var(--radius-full)`| `9999px` | Huy hiệu `.badge`, Pill badge, Avatar tròn |
| **Khoảng cách** | `var(--s1)` / `var(--s2)` / `var(--s3)` | `4px` / `8px` / `12px` | Padding trong nút, icon gap, lề nhỏ |
| | `var(--s4)` / `var(--s6)` / `var(--s8)` | `16px` / `24px` / `32px` | Padding thẻ Card, khoảng cách giữa các khối |
| **Kích thước** | `var(--topbar-h)` | `74px` | Chiều cao Topbar chuẩn DNA C++ (menu tab font 15.5px, bo góc 8px) |
| | `var(--btn-h)` / `var(--btn-sm-h)` | `48px` / `40px` | Chiều cao Nút bấm tiêu chuẩn (font 16px) / Nút nhỏ (font 14.5px) |
| | `var(--input-h)` | `48px` | Chiều cao Ô nhập liệu chuẩn (font 16px, bo góc 8px) |
| **Cỡ chữ topbar & sidebar** | `var(--chrome-step)` | `1px` | Cộng thêm vào cỡ gốc của mọi chữ/icon ở topbar, sidebar, hộp thoại đăng xuất (định nghĩa trong `topbar.css`). Đổi một chỗ để tăng/giảm cả hệ thống "khung". Logo không bị ảnh hưởng. |

---

## 3. Nút Bấm & Hành Động (Buttons DNA)

> **DNA Nút Bấm:** Tất cả các trang ứng dụng (Dashboard, Quản lý, Chi tiết tin, Hồ sơ) sử dụng hình chữ nhật bo góc `8px` (`var(--radius-md)`), chiều cao tiêu chuẩn `48px`, cỡ chữ `16px` (`var(--font-md)`).  
> *(Áp dụng cả trang Landing `candidate/landing.html`. Không dùng nút pill; bo tròn `var(--radius-full)` chỉ dành cho badge, chip lọc, avatar).*

```html
<!-- Cú pháp chuẩn: .btn + .btn--[loại] hoặc .btn-[loại] -->
<button type="submit" class="btn btn--primary">Hành động chính</button>
<button type="button" class="btn btn--secondary">Hành động phụ</button>
<a href="/login" class="btn btn--outline">Nút viền Mộc</a>
<button type="button" class="btn btn--danger">Hành động xóa/nguy hiểm</button>
```

| Tên Class | Loại nút & Đặc điểm | Ví dụ trường hợp sử dụng |
|---|---|---|
| `.btn.btn--primary` | Nền xanh `var(--brand-dark)`, chữ trắng, font 16px, chiều cao 48px, bo góc 8px | Nút Submit form, "Lưu thay đổi", "Tạo yêu cầu mới", "Nộp đơn ứng tuyển" |
| `.btn.btn--secondary` | Nền trắng `var(--surface)`, viền `var(--control-line)`, chữ `var(--ink)`, font 16px | Nút "Hủy bỏ", "Quay lại danh sách", "Đóng" |
| `.btn.btn--outline` | Nền trong suốt, viền xanh `var(--brand)`, chữ xanh `var(--brand)`, font 16px | Nút phụ trên nền sáng, "Đăng xuất", "Xem trước", "Tải báo cáo" |
| `.btn.btn--danger` | Nền trong suốt/đỏ `var(--error)`, chữ đỏ/trắng, font 16px | Nút "Xóa tài khoản", "Hủy yêu cầu tuyển dụng", "Từ chối đề nghị" |
| `.btn.btn--sm` | Nút kích thước nhỏ (chiều cao 40px, font 14.5px, bo góc 8px) | Đặt trong cột hành động của bảng dữ liệu (Table Action) |
| `.btn:disabled`<br>`.btn[disabled]` | Mờ 55%, con trỏ `not-allowed`, không nhận click | Form chưa thay đổi dữ liệu, hoặc đang trong trạng thái gửi request |

---

## 4. Huy Hiệu Trạng Thái (Badges & Tags)

```html
<!-- Cú pháp: .badge + .badge--[loại] -->
<span class="badge badge--success">Hoạt động</span>
<span class="badge badge--warning">Chờ duyệt</span>
<span class="badge badge--danger">Từ chối</span>
<span class="badge badge--neutral">Kỹ thuật</span>
```

| Tên Class | Màu hiển thị | Dùng cho trạng thái |
|---|---|---|
| `.badge.badge--success` | Chữ xanh đậm trên nền xanh nhạt | "Hoạt động", "Đã duyệt", "Đã tuyển", "Đã ký", "Trúng tuyển" |
| `.badge.badge--warning` | Chữ cam đất trên nền vàng nhạt | "Chờ duyệt", "Chờ phỏng vấn", "Chờ phản hồi", "Tạm dừng" |
| `.badge.badge--danger` | Chữ đỏ trên nền hồng đỏ nhạt | "Bị khóa", "Từ chối", "Đã hủy", "Không đạt" |
| `.badge.badge--neutral` | Chữ xám trên nền xám nhạt | "Bản nháp", "Full-time", "Part-time", Tên phòng ban |

---

## 5. Biểu Mẫu Nhập Liệu (Form Controls)

```html
<form th:action="@{/example}" method="post">
  <!-- Trường nhập text/email/password -->
  <div class="form-group">
    <label class="form-label form-label--required" for="jobTitle">Tiêu đề công việc</label>
    <input type="text" id="jobTitle" class="form-input" name="title" placeholder="VD: Senior Java Developer" required>
    <p class="form-hint">Nhập tên vị trí rõ ràng, súc tích.</p>
    <p class="form-error" th:if="${#fields.hasErrors('title')}" th:errors="*{title}">Lỗi validation</p>
  </div>

  <!-- Trường Dropdown Select -->
  <div class="form-group">
    <label class="form-label" for="dept">Phòng ban</label>
    <select id="dept" class="form-select" name="departmentId">
      <option value="1">Kỹ thuật</option>
      <option value="2">Nhân sự</option>
    </select>
  </div>

  <!-- Trường Textarea -->
  <div class="form-group">
    <label class="form-label" for="desc">Mô tả chi tiết</label>
    <textarea id="desc" class="form-textarea" rows="4" name="description"></textarea>
  </div>

  <!-- Cụm nút hành động cuối form -->
  <div class="form-actions">
    <a th:href="@{/dashboard}" class="btn btn--secondary">Hủy bỏ</a>
    <button type="submit" class="btn btn--primary">Lưu thông tin</button>
  </div>
</form>
```

| Tên Class | Vị trí & Công dụng |
|---|---|
| `.form-group` | Khối bọc ngoài từng cặp label/input, có khoảng cách đáy chuẩn |
| `.form-label` | Nhãn chữ đậm cho ô nhập liệu |
| `.form-label--required` | Thêm dấu hoa thị đỏ `*` bắt buộc nhập tự động |
| `.form-input` / `.form-control` | Ô nhập text, email, password, số, ngày tháng |
| `.form-select` | Dropdown menu chọn lựa |
| `.form-textarea` | Ô nhập văn bản nhiều dòng (tự co giãn dọc) |
| `.form-error` | Dòng thông báo lỗi màu đỏ cạnh ô nhập liệu |
| `.form-hint` | Dòng hướng dẫn người dùng màu xám nhạt |
| `.form-actions` | Khung căn phải cho các nút Lưu / Hủy ở cuối form |

---

## 6. Thẻ Chứa, Card & Lưới Bố Cục (Cards & Layout Grid)

```html
<!-- Tiêu đề trang chuẩn -->
<div class="page-header">
  <div>
    <h1 class="page-title">Quản lý Yêu Cầu Tuyển Dụng</h1>
    <p class="page-subtitle">Theo dõi và phê duyệt các đề xuất bổ sung nhân sự</p>
  </div>
  <a th:href="@{/requisitions/new}" class="btn btn--primary">+ Tạo yêu cầu mới</a>
</div>

<!-- Lưới 2 cột hoặc 3 cột responsive -->
<div class="grid-2-col">
  <div class="card">
    <div class="card-header">
      <h2 class="card-title">Thông tin cơ bản</h2>
    </div>
    <div class="card-body">...</div>
  </div>
  <div class="card card--elevated">
    <div class="card-header">
      <h2 class="card-title">Quy trình phê duyệt</h2>
    </div>
    <div class="card-body">...</div>
  </div>
</div>
```

| Tên Class | Đặc điểm bố cục |
|---|---|
| `.card` | Nền trắng `var(--surface)`, bo góc 8px, viền xám nhẹ `var(--line)` |
| `.card--elevated` | Thẻ có bóng đổ `var(--shadow-md)` (dùng cho Hero box, Form đăng nhập) |
| `.card-header` / `.card-title` | Tiêu đề đầu thẻ Card kèm đường kẻ phân cách nhẹ |
| `.page-header` / `.page-title` | Cụm tiêu đề to trang nghiệp vụ + nút thao tác nhanh bên phải |
| `.grid-2-col` | Chia 2 cột bằng nhau (trên màn hình điện thoại tự xếp chồng 1 cột) |
| `.grid-3-col` | Chia 3 cột bằng nhau |
| `.text-muted` | Đổi màu chữ xám `var(--muted)` |
| `.text-brand` | Đổi màu chữ xanh thương hiệu `var(--brand)` |
| `.text-danger` | Đổi màu chữ đỏ cảnh báo `var(--error)` |

---

## 7. Bảng Dữ Liệu (Data Tables)

```html
<div class="table-wrapper">
  <table class="table">
    <thead>
      <tr>
        <th>Mã</th>
        <th>Vị trí</th>
        <th>Phòng ban</th>
        <th>Trạng thái</th>
        <th style="text-align: right;">Thao tác</th>
      </tr>
    </thead>
    <tbody>
      <tr th:each="item : ${list}">
        <td th:text="${item.code}">REQ-001</td>
        <td><strong th:text="${item.title}">Java Dev</strong></td>
        <td th:text="${item.department}">Kỹ thuật</td>
        <td><span class="badge badge--success" th:text="${item.status}">Đã duyệt</span></td>
        <td style="text-align: right;">
          <a th:href="@{'/requisitions/' + ${item.id}}" class="btn btn--secondary btn--sm">Chi tiết</a>
        </td>
      </tr>
    </tbody>
  </table>
</div>
```

| Tên Class | Công dụng |
|---|---|
| `.table-wrapper` | Khung bọc bảng hỗ trợ cuộn ngang mượt mà trên mobile mà không vỡ giao diện |
| `.table` | Bảng chuẩn có đường kẻ, hàng header xám nhạt, hiệu ứng hover từng dòng |

---

## 8. Hộp Thoại & Thông Báo (Dialogs & Alerts)

```html
<!-- Alert thông báo -->
<div class="alert alert--success">Cập nhật hồ sơ thành công!</div>
<div class="alert alert--danger">Có lỗi xảy ra trong quá trình xử lý.</div>

<!-- Hộp thoại Modal native HTML5 <dialog> -->
<dialog id="deleteConfirmDialog" class="confirm-dialog">
  <div class="dialog-content">
    <div class="dialog-header">
      <h3 class="dialog-title">Xác nhận xóa</h3>
    </div>
    <div class="dialog-body">
      <p>Bạn có chắc chắn muốn xóa mục này? Thao tác này không thể hoàn tác.</p>
    </div>
    <div class="dialog-actions">
      <button type="button" class="btn btn--secondary btn--sm" onclick="document.getElementById('deleteConfirmDialog').close()">Ở lại</button>
      <button type="button" class="btn btn--danger btn--sm" onclick="document.getElementById('deleteForm').submit()">Đồng ý xóa</button>
    </div>
  </div>
</dialog>
```

---

## 9. Không Gian Làm Việc Nội Bộ (Workspace Topbar & Sidebar)

> **Nguyên tắc:** Có 2 loại topbar (nội bộ, công khai) nhưng **góc phải, logo, chiều cao, nút và hộp thoại đăng xuất là MỘT nguồn**. Không chép lại markup; luôn gọi fragment.

### A. Bản đồ nguồn dùng chung của Topbar

| Thành phần | Nguồn HTML (fragment) | Nguồn CSS | Ghi chú |
|---|---|---|---|
| Logo | `fragments/brand :: wordmark(subtitle)` / `wordmarkLink(subtitle, href)` | `topbar.css` mục 4 | Công khai: `'TUYỂN DỤNG'`. Nội bộ (sidebar): `'RMS'`. Không tự vẽ SVG logo. |
| Góc phải (avatar, tên, role, email, Đăng xuất, hộp thoại) | `fragments/layout/topbar-user :: actions(name, email, role, avatar, idPrefix)` | `topbar.css` mục 1–3 | Role truyền tên gốc (`'HR'`, `'Candidate'`...), fragment tự dịch sang tiếng Việt. |
| Dữ liệu người dùng | Model attribute `topbarUser` (`core/web/TopbarUserAdvice`) | – | Có sẵn ở **mọi** trang khi đã đăng nhập: `fullName`, `email`, `roleName`, `avatarUrl`. Không tự lấy từ biến riêng của controller. |
| Topbar nội bộ | `fragments/workspace-header :: header(role, fullName)` | `workspace.css` (`@import topbar.css`) | Không có logo (logo nằm ở đầu sidebar). |
| Topbar công khai | `fragments/layout/public-topbar :: topbar(activeNav, onLanding)` | `topbar.css` mục 5 | Dùng cho landing (`candidate/landing.html`) và `layout/public` (jobs board, job-detail). Trang chứa phải nạp `css/topbar.css`. |
| Font Lora / Source Sans 3 | – | `fonts.css` | `tokens.css` và `topbar.css` cùng `@import`. |

**Kích thước chung:** cao `var(--topbar-h)` = 74px (đầu sidebar cũng 74px), lề trái/phải 32px, dãn hết chiều ngang (không dồn vào giữa).

**Cỡ chữ & icon thực tế** (gốc + `--chrome-step` 1px):

| Thành phần | Cỡ | Thành phần | Cỡ |
|---|---|---|---|
| Menu topbar công khai | 15.5px | Menu sidebar | 15px |
| Nút topbar / Đăng xuất | 15.5px | Icon sidebar (ô icon) | 21px (29px) |
| Tên người dùng | 17px | Tooltip sidebar | 13.5px |
| Badge role | 14px | Icon nút ghim | 17px |
| Email | 14.5px | Hộp thoại: tiêu đề / nội dung | 21px / 16px |
| Logo "mộc." / nhãn phụ | 25px / 10px (không đổi) | | |

### B. Góc phải dùng chung (`topbar-user :: actions`)

```html
<!-- Nội bộ (workspace-header.html) -->
<div th:replace="~{fragments/layout/topbar-user :: actions(${userName}, ${userEmail}, ${userRole}, ${userAvatar}, 'workspace')}"></div>

<!-- Công khai (public-topbar.html) -->
<div th:replace="~{fragments/layout/topbar-user :: actions(${topbarUser.fullName}, ${topbarUser.email}, ${topbarUser.roleName}, ${topbarUser.avatarUrl}, 'public')}"></div>
```

| Tên Class | Vai trò |
|---|---|
| `.topbar-user` | Khung bọc góc phải (flex, gap 12px). Nút Đăng xuất luôn **ngoài cùng bên phải**. |
| `.workspace-identity` | Khối Avatar + Tên + Role + Email, click về `/profile`, hover nền `var(--brand-soft)`. |
| `.workspace-avatar` / `.workspace-avatar-fallback` | Avatar tròn 44px; không có ảnh thì hiện chữ cái đầu trên nền `var(--brand-soft)`. |
| `.workspace-user-name` / `.workspace-role` / `.workspace-user-email` | Tên đậm 16px / Badge role bo 8px / Email xám, cắt bớt khi dài. |
| `.workspace-logout-btn` | Nút Đăng xuất: viền `var(--brand)`, bo 8px, cao 40px, icon SVG; hover chuyển đỏ `var(--error)`. Tự đủ kiểu, **không cần** thêm `.btn`. |
| `.topbar-dialog` + `__content` / `__title` / `__body` / `__actions` | Hộp thoại xác nhận đăng xuất (native `<dialog>`). Id: `{idPrefix}-logout-dialog`, form: `{idPrefix}-logout-form`. |
| `.topbar-btn` / `.topbar-btn--primary` / `.topbar-btn--danger` | Nút trên topbar và trong hộp thoại: viền xanh / nền xanh đậm / nền đỏ. Cao 40px, bo 8px, chữ 14.5px. |

### C. Topbar Công Khai (`public-topbar :: topbar`)

```html
<!-- Trang landing: Quy trình / Văn hóa / Đãi ngộ cuộn tới section trong trang, ban đầu chưa sáng mục nào -->
<header th:replace="~{fragments/layout/public-topbar :: topbar('', true)}"></header>
<!-- layout/public (jobs board, job-detail): sáng "Vị trí tuyển dụng"; các mục khác dẫn về landing kèm #section -->
<header th:replace="~{fragments/layout/public-topbar :: topbar('jobs', false)}"></header>
```

| Tên Class | Vai trò |
|---|---|
| `.public-topbar` / `__inner` | Khung sticky, nền trắng, viền dưới; `__inner` dãn hết chiều ngang, lề 32px. Gắn sẵn token chuẩn nên không bị trang landing đổi màu. |
| `.public-topbar__nav` / `__link` | Menu section. `__link` không bao giờ xuống dòng; hover và mục đang xem (`.active` / `aria-current`) sáng nền `var(--brand-soft)` + gạch chân. |
| `.public-topbar__actions` | Nút Đăng nhập / Tạo tài khoản (khách) hoặc nút "Bảng điều khiển" + `topbar-user` (đã đăng nhập). |
| `.public-topbar__dashboard` | Nút "Bảng điều khiển →", ẩn trên điện thoại. |

- **Menu:** "Vị trí tuyển dụng" luôn dẫn tới Jobs Board `/jobs` (sáng khi `activeNav = 'jobs'`). "Quy trình", "Văn hóa Mộc", "Chế độ đãi ngộ" là section của landing (`#process`, `#culture`, `#perks`).
- **Làm sáng menu:** `static/js/public-topbar.js` (fragment tự nạp): sáng khi bấm, và sáng theo section đang cuộn tới trên landing.
- **Logo ngoài topbar** (vd footer landing) cũng đúng chuẩn vì khối cầu nối token trong `topbar.css` áp dụng cho cả `.wordmark`.
- **Thu gọn khi màn hình hẹp** (mốc được kiểm tra với `--chrome-step` đến 2px; tăng quá 2px thì kiểm tra lại): dưới 1440px ẩn email; dưới 1366px ẩn badge role, thu hẹp menu; dưới 1300px (đã đăng nhập) hoặc 1150px (khách) ẩn menu; dưới 640px chỉ còn logo, avatar và icon đăng xuất.

### D. Sidebar Đa Trạng Thái (`sidebar-shell.html`)
- **Trạng thái thu gọn (68px):** Tối ưu không gian làm việc, hiển thị icon kèm Tooltip nổi giải thích tính năng khi hover.
- **Trạng thái mở rộng (250px):** Mở ra khi rê chuột hoặc bấm nút ghim (`#sidebar-pin-toggle`).
- **Nút ghim (icon đinh ghim):** chưa ghim = đinh ghim viền xám; đã ghim = đinh ghim tô xanh trên nền `var(--sidebar-brand-soft)`; đã ghim + rê chuột = icon "bỏ ghim" (đinh ghim gạch chéo). Trạng thái lưu ở `localStorage` (`rms_sidebar_pinned`), `aria-pressed` do `interface.js` cập nhật.
- **Phân nhánh Menu theo Role:** Tự động nạp menu tương ứng (`admin.html`, `candidate.html`, `interviewer.html`, `recruiter.html`).

---

## 10. Linh Kiện Tái Sử Dụng Bằng Fragment

Gọi bằng cú pháp Thymeleaf: `th:replace="~{fragments/ui/[file] :: [tênFragment](...)}"`.

| Tên Fragment | Vị trí File | Cú pháp gọi Thymeleaf | Tham số truyền vào |
|---|---|---|---|
| **`jobCard`** | `fragments/ui/cards.html` | `<article th:replace="~{fragments/ui/cards :: jobCard(job=${item})}"></article>` | `job`: Đối tượng DTO bài đăng tuyển dụng. Lặp: `<th:block th:each="job : ${jobsPage.content}"><article th:replace="~{fragments/ui/cards :: jobCard(job=${job})}"></article></th:block>` (không gộp `th:each` với `th:replace`). Dùng ở `candidate/job-board.html` |
| **`statCard`** | `fragments/ui/cards.html` | `<article th:replace="~{fragments/ui/cards :: statCard(title='Tổng hồ sơ', value=${total}, detail='Tháng này')}"></article>` | `title`, `value`, `detail` |
| **`paged`** (phân trang) | `fragments/ui/pagination.html` | `<nav th:replace="~{fragments/ui/pagination :: paged(page=${jobsPage})}"></nav>` | `page`: Spring Data `Page<T>`. **Tự giữ mọi tham số lọc/tìm kiếm** trên URL, chỉ đổi `?page=` (qua `pageLinks`). Ẩn khi chỉ có 1 trang. Số phần tử/trang do Controller đặt (`@PageableDefault(size = …)`). CSS: `components.css` mục L. Bản cũ `pagedWithUrl(page, baseUrl)` và `fragments/pagination :: paged/pager` chỉ cho trang chưa migrate. |
| **`confirm`** | `fragments/ui/dialogs.html` | `<dialog th:replace="~{fragments/ui/dialogs :: confirm(dialogId='logoutDialog', formId='logoutForm', title='Xác nhận', message='Bạn có chắc chắn?')}"></dialog>` | `dialogId`, `formId`, `title`, `message` |
| **`topbar`** (công khai) | `fragments/layout/public-topbar.html` | `<header th:replace="~{fragments/layout/public-topbar :: topbar('jobs', false)}"></header>` | `activeNav` (`'jobs'` hoặc `''`), `onLanding`. Trang chứa phải nạp `css/topbar.css` |
| **`wordmark`** | `fragments/brand.html` | `<a th:replace="~{fragments/brand :: wordmark('TUYỂN DỤNG')}"></a>` | `subtitle` (link về `/`) |
| **`wordmarkLink`** | `fragments/brand.html` | `<a th:replace="~{fragments/brand :: wordmarkLink('RMS', '/dashboard')}"></a>` | `subtitle`, `href` |
| **`actions`** (góc phải topbar) | `fragments/layout/topbar-user.html` | `<div th:replace="~{fragments/layout/topbar-user :: actions(${name}, ${email}, ${role}, ${avatar}, 'workspace')}"></div>` | `name`, `email`, `role`, `avatar`, `idPrefix`. Xem mục 9B |
| **`header`** | `fragments/workspace-header.html` | `<header th:replace="~{fragments/workspace-header :: header(${role}, ${fullName})}"></header>` | `role`, `fullName` |
| **`shell`** | `fragments/layout/sidebar-shell.html` | `<aside th:replace="~{fragments/layout/sidebar-shell :: shell(${activeMenu}, ${role})}"></aside>` | `activeMenu`, `role` |
| **`timeline`** | `fragments/ui/timeline.html` | `<th:block th:replace="~{fragments/ui/timeline :: timeline(items=${detail.timeline})}"></th:block>` | `items`: `List<core.web.TimelineItem>` (`at`, `title`, `actor`, `body`, `tone` ∈ `neutral`/`brand`/`success`/`warning`/`danger`), sắp xếp sẵn. Rỗng thì hiện "Chưa có hoạt động". Mỗi module tự dựng danh sách mốc từ bảng của mình. CSS: `components.css` mục M. Dùng ở `candidate/application-detail.html` |

---

## 11. Menu Thao Tác Khác "⋯" (More Actions)

Chỉ là class + markup (không có fragment), không cần JS: thẻ `<details>` tự mở/đóng, dùng được bằng bàn phím.

```html
<details class="more-menu">
  <summary class="more-menu__toggle" aria-label="Thao tác khác cho hồ sơ của An Võ" title="Thao tác khác">⋯</summary>
  <div class="more-menu__list">
    <a class="more-menu__item" href="/applications/298/cv" target="_blank" rel="noopener">Mở CV</a>
    <a class="more-menu__item" href="/interviews/new?applicationId=298">Lên lịch phỏng vấn</a>
  </div>
</details>
```

| Tên Class | Công dụng |
|---|---|
| `.more-menu` | Khung `<details>` |
| `.more-menu__toggle` | Nút "⋯" (`<summary>`), cùng cỡ `btn--sm`. **Luôn có `aria-label`** nói rõ thao tác cho bản ghi nào |
| `.more-menu__list` | Danh sách mục, mở **trong luồng** ngay dưới nút (không nổi đè) để không bị `.table-wrapper` (cuộn ngang) cắt ở các dòng cuối bảng |
| `.more-menu__item` | Một mục: `<a>` hoặc `<button>` |

- Chỉ đưa vào menu những mục người xem **dùng được** (kiểm quyền ở server); không còn mục nào thì không render menu.
- Chưa tự đóng khi bấm ra ngoài (cần JS). CSS: `components.css` mục N.

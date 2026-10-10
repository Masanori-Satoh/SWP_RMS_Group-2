# Tổ Chức CSS (CSS Organization Guide)

> **Dành cho:** Developers và Trợ lý AI khi cần viết CSS.
> **Mục tiêu:** dùng lại tối đa catalog, chỉ viết thêm phần thật sự riêng, không lặp code, không làm rò rỉ style sang trang khác.

---

## 1. Các tầng CSS và thứ tự nạp

Tầng sau ghi đè tầng trước. Mỗi tầng chỉ làm đúng việc của nó.

| Tầng | File | Chứa gì | Ai sửa |
|---|---|---|---|
| 1. Token | `tokens.css` (+ `fonts.css`) | Biến: màu, khoảng cách, bo góc, chiều cao, font offline | 🔴 Lead |
| 2. Nền | `global.css` | Reset, typography, `.container`, `.section` | 🟡 chỉ thêm |
| 3. Linh kiện | `components.css` | `.btn--*`, `.badge--*`, `.form-*`, `.card`, `.table`, `.alert--*`, dialog, `.page-header`, `.grid-*`, phân trang, thẻ việc làm, footer công khai | 🟡 chỉ thêm |
| 4. Khung | `topbar.css`, `workspace.css` | Topbar, góc phải, logo, hộp thoại đăng xuất, sidebar, `--chrome-step` | 🔴 Lead |
| 5. Trang | `pages/<trang>.css` (mới) hoặc `<trang>.css` phẳng (cũ) | Chỉ phần riêng của một trang | 🟢 người phụ trách trang |

**Layout nạp gì:**

| Trang dùng | Thứ tự nạp |
|---|---|
| `layout/internal` | tokens → global → components → workspace (tự `@import` topbar, fonts) → CSS trang |
| `layout/public` | tokens → global → components → topbar → CSS trang |
| Landing `candidate/landing.html` (ngoại lệ) | global → `landing.css` → topbar |
| Trang nội bộ cũ (`fragments/head :: interfaceHead`) | design-tokens → interface → workspace → CSS trang. **Bộ cũ, không dùng cho trang mới.** |

CSS trang luôn nạp **sau cùng** qua `<th:block layout:fragment="styles">`, nên nó ghi đè được, và chính vì thế nó **không được** ghi đè class dùng chung (rò rỉ thiết kế).

---

## 2. Quyết định: dùng catalog hay viết mới?

Đi theo thứ tự, dừng ở bước đầu tiên đáp ứng được:

| # | Tình huống | Làm gì |
|---|---|---|
| 1 | Catalog có class/fragment cho thứ này | **Dùng nguyên.** Không viết lại, không copy CSS của nó. |
| 2 | Có rồi nhưng cần khác chút (khoảng cách, số cột, bố cục bên trong) | Giữ class catalog, **thêm class phụ có tiền tố trang**: `class="card jb-search"`. CSS trang chỉ chứa phần khác. |
| 3 | Chưa có, chỉ trang này dùng | Viết trong `pages/<trang>.css`, tiền tố trang, chỉ token. |
| 4 | Thứ đó đã có ở **2 trang trở lên** | Không chép sang trang thứ 3. Đề xuất đưa lên tầng 3 (`components.css`) hoặc fragment `fragments/ui/` theo mục 4. |

Ví dụ thật — Jobs Board (`/jobs`):
- Thẻ việc làm → fragment `jobCard` (bước 1). Phân trang → fragment `paged` (bước 1). Nút, form, lưới 3 cột → catalog (bước 1).
- Form tìm kiếm 4 cột → `class="card jb-search"` + `.jb-search { grid-template-columns: 2fr 1fr 1fr auto; }` (bước 2).
- Thanh "Tìm thấy N vị trí", trạng thái rỗng → `.jb-results`, `.jb-empty` (bước 3).

---

## 3. Viết CSS trang

### Vị trí & đầu file
- Trang mới: `static/css/pages/<ten-trang>.css` (trùng tên file template khi được). JS riêng: `static/js/pages/<ten-trang>.js`.
- Trang cũ: giữ file phẳng hiện có (`static/css/offers.css`…), **không dời** khi migrate; chỉ dọn phần trùng catalog.
- Đầu file luôn có comment:
  ```css
  /* =============================================================================
     PAGES/JOB-BOARD.CSS — Jobs Board (/jobs, candidate/job-board.html)
     Chỉ chứa phần catalog chưa có: …
     Tiền tố class: jb-
     ============================================================================= */
  ```

### Đặt tên
- **Mọi class có tiền tố trang** 2–5 ký tự: `jb-` (jobs board), `req-` (requisition), `offer-`, `notif-`, `landing-`.
- Kiểu BEM nhẹ: `tiền-tố-khối`, `tiền-tố-khối__phần`, `tiền-tố-khối--biến-thể` (vd `.jb-search__keyword`, `.jb-results__count`).
- Không đặt tên chung chung không tiền tố (`.title`, `.wrapper`, `.box`, `.section-title`) — dễ đụng trang khác.

### Giá trị
- Màu: chỉ `var(--brand)`, `var(--brand-dark)`, `var(--brand-soft)`, `var(--ink)`, `var(--muted)`, `var(--line)`, `var(--surface)`, `var(--surface-subtle)`, `var(--page)`, `var(--success|warning|error|neutral)` và bản `-soft`. **Không hex, không `rgb()/rgba()`.**
- Khoảng cách: `var(--s1|s2|s3|s4|s6|s8|s12|s16|s20)` (4 → 80px). Bo góc: `var(--radius-sm|md|lg|full)`. Bóng: `var(--shadow-sm|md|lg)`.
- Cần một giá trị chưa có token (vd độ rộng cột 340px) thì viết số trực tiếp được; cần một **màu** chưa có token thì hỏi Lead thêm token, không tự đặt.

### Không được
- Định nghĩa lại class dùng chung (`.btn`, `.card`, `.badge`, `.form-input`, `.page-link`, `.workspace-*`, `.sidebar-*`, `.wordmark`…), kể cả qua selector lồng (`.jb-page .btn { … }`). Ngoại lệ duy nhất: chỉnh **vị trí/khoảng cách** của linh kiện trong khung trang, vd `.jb-page .pagination-wrapper { margin-top: var(--s8); }`.
- Ghi đè token ở `:root` trong CSS trang. (Landing là ngoại lệ lịch sử, xem mục 6.)
- `!important` (trừ khi sửa xung đột với CSS cũ, kèm comment lý do).

### Responsive
- Viết `@media` ngay cuối file trang, theo mốc của catalog: `max-width: 992px` (tablet), `768px` (điện thoại).
- Lưới catalog đã tự co: `.grid-3-col` còn 2 cột dưới 992px, `.grid-2-col`/`.grid-3-col` còn 1 cột dưới 768px. Không cần viết lại.

---

## 4. Thêm vào file dùng chung (quy tắc "chỉ được thêm")

Khi một khối đã xuất hiện ở ≥ 2 trang:

1. **Chọn tên catalog** không tiền tố trang, theo kiểu BEM của catalog (`.stat-strip`, `.stat-strip__item`, `.filter-bar`…). Kiểm tra chưa có tên trùng: tìm trong `components.css` và `UI_CATALOG.md`.
2. **Thêm vào cuối mục tương ứng** của `components.css` (file chia mục A–L: A. Nút, B. Badge, C. Form, D. Card & lưới, E. Tiện ích chữ, F. Alert, G. Bảng, H. Dialog, I. Header/footer công khai, J. Stat card, K. Job card, L. Phân trang). Không có mục phù hợp thì thêm mục mới cuối file (M, N…) với comment tiêu đề cùng kiểu. Không đổi thuộc tính của selector đã có.
3. Nếu là khối markup lặp lại: tạo fragment trong `templates/fragments/ui/<nhóm>.html`, ghi cách gọi + tham số ở comment đầu fragment.
4. **Cập nhật `UI_CATALOG.md`** (cú pháp, bảng class, tham số) trong cùng PR.
5. Chuyển các trang đang có bản riêng sang dùng bản chung, xóa CSS trùng trong file trang.
6. PR gắn Lead Architect duyệt.

---

## 5. Cỡ chữ & mốc thu gọn của khung (topbar, sidebar)

- Mọi chữ/icon của topbar, sidebar, hộp thoại đăng xuất = cỡ gốc + `var(--chrome-step)` (hiện `1px`, khai báo trong `topbar.css`). Muốn tăng/giảm cả khung: đổi **một** giá trị này (Lead).
- Mốc thu gọn topbar công khai đã kiểm tra với step 1–2px. Tăng quá 2px phải kiểm tra lại các `@media` cuối `topbar.css`.

---

## 6. Ngoại lệ đã biết

| File | Ngoại lệ | Lý do |
|---|---|---|
| `landing.css` | Tự định nghĩa `:root` (màu, bo góc, font riêng); không nạp `tokens.css`/`components.css` | Trang "bộ mặt" có phong cách riêng. Topbar và logo vẫn chuẩn nhờ khối cầu nối token trong `topbar.css`. |
| `topbar.css` (khối `.public-topbar, .wordmark`) | Ghi lại giá trị token bằng hex | Cầu nối để topbar/logo đúng màu trên landing. Giá trị phải luôn khớp `tokens.css`. |
| `design-tokens.css`, `interface.css` | Bộ token/CSS cũ song song | Còn phục vụ trang nội bộ chưa migrate; xóa theo `UI_LEGACY_CLEANUP.md`. |

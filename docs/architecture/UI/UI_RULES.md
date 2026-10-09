# Bản Quy ước Phối hợp Frontend (UI Rules & Team Contract)

> **Dành cho:** Toàn bộ thành viên Team RMS (Devs) và Trợ lý AI
> **Nguyên tắc cốt lõi:** Phân quyền rõ ràng, không làm vỡ code của nhau, bảo toàn 100% dữ liệu Backend.
> **Đọc kèm:** [`README.md`](README.md) (lộ trình) · [`UI_CATALOG.md`](UI_CATALOG.md) (tra cứu) · [`UI_CSS_GUIDE.md`](UI_CSS_GUIDE.md) (tổ chức CSS)

---

## 1. Phân quyền Sửa Đổi (Ai được sửa cái gì?)

Để tránh xung đột Git (Merge Conflict) và giữ sự nhất quán toàn hệ thống:

| Khu vực / Thư mục | Người phụ trách | Quy định |
|---|---|---|
| Khung xương dùng chung (danh sách đầy đủ ở mục 2A, 2B) | **Lead Architect (Dũng LT)** | Thành viên khác muốn thêm linh kiện dùng chung thì gửi yêu cầu hoặc PR nhỏ. |
| `templates/{feature}/*.html`<br>(ví dụ `requisitions/`, `interview/`, `offers/`) | **Người phụ trách Feature đó** | Tự chuyển đổi trang của mình sang dùng `layout:decorate`. Giữ nguyên tên file và đường dẫn view. |
| `templates/fragments/layout/sidebars/*.html` | **Người phụ trách Role đó** | Chỉ thêm/sửa thẻ `<a>` menu của role mình, không đụng vào khung vỏ thu gọn. |
| `static/css/pages/<tên-trang>.css`, `static/js/pages/<tên-trang>.js` | **Người phụ trách trang đó** | CSS/JS đặc thù chỉ trang đó có. Class phải có tiền tố riêng của trang để không rò rỉ sang trang khác. |

---

## 2. Vùng Nguy Hiểm: Không Được Chạm / Chỉ Được Thêm

Các file dưới đây được **mọi trang** dùng. Sửa sai một dòng là vỡ cả hệ thống, nên chia 3 mức.

### A. 🔴 KHÔNG ĐƯỢC CHẠM (chỉ Lead Architect sửa)

| File | Vì sao |
|---|---|
| `templates/layout/base.html`, `internal.html`, `public.html`, `auth.html` | Khung của mọi trang |
| `templates/fragments/layout/sidebar-shell.html` | Chọn menu theo role, nút ghim |
| `templates/fragments/layout/topbar-user.html` | Góc phải của mọi topbar (avatar, đăng xuất, hộp thoại) |
| `templates/fragments/layout/public-topbar.html` | Topbar công khai |
| `templates/fragments/workspace-header.html` | Topbar nội bộ |
| `templates/fragments/brand.html` | Nguồn logo duy nhất |
| `static/css/tokens.css`, `fonts.css` | Token thiết kế, font. Đổi một giá trị là đổi màu/cỡ cả hệ thống |
| `static/css/topbar.css`, `workspace.css` | CSS topbar, sidebar, logo, `--chrome-step` |
| `static/js/interface.js`, `public-topbar.js` | Nút ghim, menu mobile, làm sáng menu topbar |
| `src/main/java/.../core/web/*` (`TopbarUserAdvice`, `ViewHelpersAdvice`, `PageLinks`) | Dữ liệu `topbarUser`, `pageLinks` cho mọi view |

AI gặp yêu cầu cần sửa các file này: **dừng lại và hỏi người dùng**, không tự sửa.

### B. 🟡 CHỈ ĐƯỢC THÊM (append) — không sửa, không xóa cái đã có

| File | Được thêm gì | Quy tắc |
|---|---|---|
| `static/css/components.css` | Linh kiện dùng chung mới (đã xuất hiện ở ≥ 2 trang) | Thêm **cuối mục tương ứng**, có comment tiêu đề. Không đổi thuộc tính của selector đã có. Cùng PR phải cập nhật `UI_CATALOG.md`. |
| `static/css/global.css` | Tiện ích nền rất chung (hiếm khi cần) | Như trên. Không thêm style cho trang cụ thể. |
| `templates/fragments/ui/*.html` | Fragment mới, hoặc tham số tùy chọn mới | Không đổi tên/tham số của fragment đã có (trang khác đang gọi). |
| `templates/fragments/layout/sidebars/<role>.html` | Mục menu mới | Copy đúng khuôn một mục `<a class="sidebar-nav-link">` có sẵn (icon + `nav-label-text` + `data-tooltip`). |
| `docs/architecture/UI/UI_CATALOG.md` | Mục tra cứu cho linh kiện vừa thêm | Giữ nguyên cấu trúc mục lục. |

"Sửa" một linh kiện đã có (đổi màu nút, đổi khoảng cách card…) = thay đổi thiết kế toàn hệ thống → thuộc mức A, chỉ Lead quyết.

### C. 🟢 TỰ DO TRONG PHẠM VI CỦA MÌNH

`templates/{feature}/*.html` của feature mình, `static/css/pages/<trang>.css`, `static/js/pages/<trang>.js`, file CSS phẳng cũ của trang mình (`static/css/offers.css`…). Vẫn phải theo mục 7 (Điều cấm) và [`UI_CSS_GUIDE.md`](UI_CSS_GUIDE.md).

---

## 3. Khung Mẫu Chuẩn Cho Mọi Trang (Boilerplate)

Mọi trang mới hoặc được refactor **bắt buộc** theo khung tối giản dưới đây. Hướng dẫn từng bước: [`UI_NEW_PAGE.md`](UI_NEW_PAGE.md).

```html
<!DOCTYPE html>
<html lang="vi"
      xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/internal}"> <!-- hoặc layout/public -->
<head>
  <title>Tiêu đề trang của bạn</title>
  <!-- (Tùy chọn) Chỉ nạp nếu trang có CSS riêng không có trong components.css -->
  <th:block layout:fragment="styles">
    <link rel="stylesheet" th:href="@{/css/pages/ten-trang.css}">
  </th:block>
</head>
<body>
  <!-- TOÀN BỘ RUỘT TRANG ĐẶT TRONG THẺ MAIN NÀY -->
  <main id="main" layout:fragment="content" class="page-content container" tabindex="-1">
    <div class="page-header">
      <h1 class="page-title">Tiêu đề</h1>
      <button type="button" class="btn btn--primary">Lưu thay đổi</button>
    </div>
  </main>
  <!-- (Tùy chọn) Script xử lý riêng của trang -->
  <th:block layout:fragment="scripts">
    <script th:src="@{/js/pages/ten-trang.js}" defer></script>
  </th:block>
</body>
</html>
```

---

## 4. Quy Chuẩn Topbar (User Identity & Logout DNA)

Áp dụng cho **mọi topbar** (nội bộ `workspace-header.html` và công khai `public-topbar.html`). Góc phải luôn gọi fragment chung `fragments/layout/topbar-user :: actions(...)`, dữ liệu lấy từ model attribute `topbarUser` (xem `UI_CATALOG.md` mục 9). Không chép lại markup.

1. **Khối nhận diện người dùng:** Avatar (hoặc chữ cái đầu làm fallback), Họ và Tên, Huy hiệu Role (tự dịch tiếng Việt), Email. Click vào bất kỳ đâu trong khối đều về `/profile`.
2. **Nút Đăng xuất:** `.workspace-logout-btn` (viền xanh, bo 8px, cao 40px, icon SVG), luôn **ngoài cùng bên phải**. Bấm mở hộp thoại `<dialog class="topbar-dialog">` (id `workspace-logout-dialog` ở nội bộ, `public-logout-dialog` ở công khai), xác nhận rồi POST `/logout` kèm CSRF.
3. **Đồng nhất topbar:**
   - Cùng chiều cao `var(--topbar-h)` (74px); đầu sidebar cũng 74px để đường kẻ thẳng hàng.
   - Topbar công khai chỉ có một bản `fragments/layout/public-topbar.html`, dùng cho landing (`candidate/landing.html`) và `layout/public` (jobs board, job-detail). Nội dung landing được phép có DNA riêng, **topbar thì không**.
   - Logo chỉ lấy từ `fragments/brand :: wordmark(subtitle)` / `wordmarkLink(subtitle, href)`.

---

## 5. Quy Chuẩn Nút Bấm (Button DNA)

1. **Áp dụng toàn hệ thống**, kể cả landing `/`, jobs board `/jobs`, chi tiết `/jobs/{id}`, các trang auth và nội bộ.
   - **Không dùng nút bo tròn kiểu pill.** Bo tròn `var(--radius-full)` chỉ dành cho badge, chip lọc và avatar.
2. **Nút chuẩn DNA hình chữ nhật bo góc 8px** (`var(--radius-md)`), cao 48px (`var(--btn-h)`), font 16px:
   - `.btn.btn--primary` (chính), `.btn.btn--secondary` (phụ), `.btn.btn--outline` (viền xanh), `.btn.btn--danger` (nguy hiểm), `.btn.btn--sm` (nhỏ, trong bảng).

---

## 6. Lưu Ý Kỹ Thuật Thymeleaf Sống Còn (Thứ tự ưu tiên thuộc tính)

> **CẢNH BÁO:** `th:replace` / `th:insert` chạy **trước** `th:if` và `th:each` trên cùng một thẻ.
> Viết `th:if` + `th:replace` trên cùng thẻ → mọi nhánh đều hiện. Viết `th:each` + `th:replace` trên cùng thẻ → chỉ ra **một** phần tử.

**Cách viết đúng (bắt buộc):** điều kiện / vòng lặp ở thẻ ngoài, `th:replace` ở thẻ trong.

```html
<th:block th:if="${userRole == 'System Admin'}">
    <th:block th:replace="~{fragments/layout/sidebars/admin :: menu(${activeMenu})}"></th:block>
</th:block>

<th:block th:each="job : ${jobsPage.content}">
    <article th:replace="~{fragments/ui/cards :: jobCard(job=${job})}"></article>
</th:block>
```

---

## 7. Điều CẤM (Luật Cứng cho cả Dev và AI)

1. **CẤM tự đổi tên hoặc dời file template.** Tên file gắn với `return "view/name"` trong Controller; dời file là lỗi 404 ngay. Trường hợp kiến trúc bắt buộc đổi (vd. tách `job-board` → `landing` + `job-board` mới): chỉ Lead quyết, sửa Controller và mọi link liên quan **trong cùng commit**, dùng `git mv` để giữ lịch sử.
2. **CẤM xóa hoặc đổi tên biến Backend.** Giữ nguyên 100% `${...}`, `th:each`, `th:if`, `th:object`, `th:field`, `name`, `id` gắn với form và data. Chỉ được đổi thẻ HTML bao bọc và class CSS.
3. **CẤM style inline và thẻ `<style>`** trong template. CSS riêng đặt ở `static/css/pages/<trang>.css`.
4. **CẤM hardcode màu** (hex `#...`, `rgb()`, `rgba()`) trong template và CSS trang. Dùng token: `var(--brand)`, `var(--ink)`, `var(--muted)`… Ngoại lệ duy nhất: `tokens.css` và khối cầu nối token trong `topbar.css`.
5. **CẤM tự chế class mới nếu `UI_CATALOG.md` đã có**, và **CẤM định nghĩa lại** class dùng chung trong CSS trang (`.btn*`, `.badge*`, `.form-*`, `.card*`, `.table*`, `.alert*`, `.page-*`, `.pagination*`, `.workspace-*`, `.topbar-*`, `.public-topbar*`, `.sidebar-*`, `.wordmark`, `.brand-*`).
6. **CẤM chép markup** của topbar, sidebar, logo, góc phải, hộp thoại đăng xuất, thẻ việc làm, phân trang. Luôn gọi fragment.

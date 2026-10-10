# Migrate Trang Cũ Sang Kiến Trúc Layout (Migration Playbook)

> **Dành cho:** Developers và Trợ lý AI khi **chuyển một trang đã có** sang kiến trúc layout.
> Trang mới hoàn toàn: [`UI_NEW_PAGE.md`](UI_NEW_PAGE.md). Viết CSS: [`UI_CSS_GUIDE.md`](UI_CSS_GUIDE.md). Dọn file cũ sau migrate: [`UI_LEGACY_CLEANUP.md`](UI_LEGACY_CLEANUP.md).
> **Người làm:** người phụ trách feature của trang đó (`UI_RULES.md` mục 1). Mỗi trang (hoặc nhóm trang cùng feature) một PR.

---

## 1. Nhận diện trang cũ

Trang cũ tự dựng khung, có các dấu hiệu:

```html
<head th:replace="~{fragments/head :: interfaceHead('Tên trang', ~{::pageStyles})}">…</head>
<div class="page-wrapper">
  <aside th:replace="~{fragments/sidebar :: workspace('offers', ${role})}"></aside>
  <div class="main-content">
    <header th:replace="~{fragments/workspace-header :: header(${role}, ${fullName})}"></header>
    <main>… nội dung …</main>
```

Chúng **vẫn hiển thị đúng** (topbar, logo, góc phải, nút ghim, hộp thoại đăng xuất đã dùng chung nguồn với trang mới). Mục đích migrate: bỏ khung chép tay, bỏ bộ CSS cũ (`interface.css`, `design-tokens.css`), dọn CSS trùng catalog, để sau này chỉ cần sửa một chỗ.

---

## 2. Các bước migrate

1. **Giữ nguyên tên file và đường dẫn view** (Controller `return "offers/list"`). Giữ 100% `${...}`, `th:each`, `th:object`, `th:field`, `name`, `id` của form.
2. Đổi thẻ `<html>`:
   ```html
   <html lang="vi" xmlns:th="http://www.thymeleaf.org"
         xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
         layout:decorate="~{layout/internal}">
   ```
3. Thay `<head th:replace=… interfaceHead(…)>` bằng:
   ```html
   <head>
     <title>Tên trang</title>
     <th:block layout:fragment="styles"><link rel="stylesheet" th:href="@{/css/offers.css}"></th:block>
   </head>
   ```
4. **Xóa** khối khung tự dựng: `page-wrapper`, `main-content`, `<aside th:replace="~{fragments/sidebar :: …}">`, `<header th:replace="~{fragments/workspace-header :: …}">`, mọi `<dialog>`/form đăng xuất chép tay, `<script>` của `interface.js`/`global.js` (layout đã nạp).
5. Bọc phần nội dung còn lại: `<main id="main" layout:fragment="content" class="page-content container" tabindex="-1">…</main>`. JS riêng của trang chuyển vào `<th:block layout:fragment="scripts">`.
6. **Controller:** thêm `model.addAttribute("activeMenu", "...")` với giá trị trước đây truyền vào `fragments/sidebar :: workspace('...', role)`. Không cần truyền `role`/`fullName` cho topbar nữa (giữ lại nếu nội dung trang còn dùng).
7. **Phân trang:** đổi `fragments/pagination :: paged(...)` / `pager(...)` sang `fragments/ui/pagination :: paged(page=${...Page})` nếu Controller trả về `Page<T>`. (Controller đang trả từng số `currentPage/totalPages` thì giữ `pager` đến khi Controller được đổi sang `Page<T>`.)
8. **Đổi class cũ sang catalog** (alias cũ vẫn chạy nhưng code đã migrate phải dùng tên BEM):

   | Cũ | Mới |
   |---|---|
   | `btn btn-primary` / `btn-secondary` / `btn-danger` | `btn btn--primary` / `btn--secondary` / `btn--danger` |
   | `btn-sm` | `btn--sm` |
   | `badge-success` / `badge-danger` / `badge-secondary` | `badge badge--success` / `badge--danger` / `badge--neutral` |
   | `form-control` | `form-input` (giữ `form-control` nếu JS đang tìm theo class này) |
   | `rms-page-link`, `rms-pagination-*` | dùng fragment `paged` |
   | `style="..."` | class trong CSS trang hoặc tiện ích catalog (`.text-muted`, `.text-brand`, `.text-danger`) |
   | Badge/nút/card tự chế trong CSS trang | class catalog tương ứng (xem ví dụ `job-detail`: `badge-department` → `badge badge--neutral`) |

9. **Dọn CSS trang** theo `UI_CSS_GUIDE.md`: xóa rule trùng catalog, đổi hex/`rgba()` → token, đổi biến tự đặt → token chuẩn, xóa rule không còn class nào dùng.
10. **Menu:** đảm bảo trang có mục trong `fragments/layout/sidebars/<role>.html` của mọi role được vào. Đây là nguồn menu duy nhất (trang cũ cũng hiển thị từ đây qua cầu nối `fragments/sidebar.html`).
11. **Kiểm tra** (mục 6), rồi **cập nhật bảng trạng thái** (mục 5).

---

## 3. Thứ tự migrate đề xuất

1. Trang đơn giản, ít JS: `notifications` (đã xong), `admin/departments/*`, `admin/api-monitoring`.
2. Trang danh sách có lọc + phân trang: `requisitions/list`, `offers/list`, `job-postings/list`, `admin/accounts/*` (đổi luôn sang `fragments/ui/pagination`).
3. Trang form/chi tiết có JS riêng: `requisitions/{form,detail}`, `offers/{form,detail}`, `interview/*`, `job-postings/{create,detail,dashboard}`.
4. `user/profile`, `dashboard/index` (dashboard có panel ứng viên `dashboard/candidate.html` và JS riêng `candidate-dashboard.js` — làm cuối, kiểm tra kỹ từng role).

---

## 4. Bẫy thường gặp

- **`th:each` + `th:replace` cùng thẻ** → chỉ ra một phần tử. Tách 2 thẻ (`UI_RULES.md` mục 6).
- **JS tìm theo id/class của khung cũ** (`workspace-logout-dialog`, `candidate-logout-dialog`, `.page-wrapper`…): tìm trong `static/js/` trước khi xóa markup.
- **CSS trang ghi đè class dùng chung** (`.btn`, `.card`…) để "sửa" giao diện cũ: sau migrate sẽ đè lên catalog. Xóa hoặc đổi sang class phụ có tiền tố.
- **Biến CSS chỉ có trong `design-tokens.css`/`interface.css`** (vd `--color-primary`): đổi sang token `tokens.css` tương ứng (`--brand-dark`…), vì layout mới không nạp bộ cũ.
- **Cầu nối sidebar:** `fragments/sidebar.html` chỉ chuyển tiếp sang `sidebar-shell`; xóa sau khi migrate trang cuối cùng.

---

## 5. Trạng thái migrate (cập nhật khi chuyển xong một trang)

| Nhóm | Trang | Trạng thái |
|---|---|---|
| Công khai | `candidate/landing.html` (`/`) | ⚪ Ngoại lệ "bộ mặt" (tự dựng khung, dùng `public-topbar`) |
| Công khai | `candidate/job-board.html` (`/jobs`) | ✅ `layout/public` — trang mẫu |
| Công khai | `candidate/job-detail.html` (`/jobs/{id}`) | ✅ `layout/public` |
| Nội bộ | `notifications/list.html` | ✅ `layout/internal` |
| Nội bộ | `dashboard/index.html` (+ `dashboard/candidate.html`), `user/profile.html` | ⏳ Cũ |
| Nội bộ | `requisitions/{list,detail,form}.html` | ⏳ Cũ |
| Nội bộ | `job-postings/{list,detail,create,dashboard}.html` | ⏳ Cũ |
| Nội bộ | `interview/{list,form}.html` | ⏳ Cũ |
| Nội bộ | `offers/{list,detail,form}.html` | ⏳ Cũ |
| Nội bộ | `admin/accounts/{list,form,candidates}.html`, `admin/departments/{list,form}.html`, `admin/api-monitoring/index.html` | ⏳ Cũ |
| Auth | `auth/*.html` | ⚪ Giữ `auth/fragments` (chưa chuyển sang `layout/auth`) |

---

## 6. Checklist trước khi báo "xong"

- [ ] Trang dùng `layout:decorate`; không còn `interfaceHead`, `fragments/sidebar`, `fragments/workspace-header`, dialog đăng xuất chép tay.
- [ ] Tên file template, đường dẫn view, biến backend giữ nguyên.
- [ ] Controller đặt `activeMenu`; sidebar sáng đúng mục.
- [ ] Mọi khối có trong catalog đã dùng class/fragment catalog; class cũ đã đổi theo bảng mục 2.8.
- [ ] CSS trang: không trùng catalog, không hex/`rgba()`, không ghi đè class dùng chung, không biến của bộ cũ.
- [ ] Không `style="..."`, `<style>` trong template.
- [ ] Đã mở trang với từng role được vào; thu nhỏ màn hình không vỡ; Đăng xuất ra hộp thoại chung; JS của trang còn chạy.
- [ ] Đã cập nhật bảng mục 5. File cũ nào hết người dùng → ghi vào `UI_LEGACY_CLEANUP.md` mục 3.

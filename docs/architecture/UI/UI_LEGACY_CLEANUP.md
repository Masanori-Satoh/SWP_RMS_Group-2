# Dọn File Cũ & Lỗi Thời (Legacy Cleanup)

> **Dành cho:** Lead Architect, Developers và Trợ lý AI khi một file giao diện không còn ai dùng.
> **Nguyên tắc:** không xóa theo cảm giác. Chỉ xóa khi đã chứng minh không còn nơi nào tham chiếu, mỗi lần xóa một commit riêng để dễ hoàn tác.

---

## 1. Quy trình xóa an toàn

1. **Tìm mọi tham chiếu** (template, CSS `@import`, JS, Java, test):
   ```bash
   # Ví dụ cho file static/css/app-layout.css
   grep -rn "app-layout" src/ --include=*.html --include=*.css --include=*.js --include=*.java
   # Template: tìm cả tên view (return "...") và đường dẫn fragment (~{...})
   grep -rn "careers/" src/main/java src/main/resources/templates
   ```
2. **Không còn tham chiếu** → xóa bằng `git rm` (không xóa tay), commit riêng: `chore(ui): remove unused <file>`.
3. **Còn tham chiếu** → chưa xóa. Ghi vào mục 3 kèm điều kiện cần đạt.
4. Chạy app, mở các trang liên quan với từng role, kiểm tra không lỗi 404 tài nguyên (DevTools → Network) và không lỗi Thymeleaf.
5. Cập nhật tài liệu: `UI_CATALOG.md` (nếu file từng có trong catalog), bảng trạng thái ở `UI_MIGRATION.md`, và bảng dưới đây.

**AI không tự xóa file** ở mục 3 hoặc thuộc khu "Không được chạm" (`UI_RULES.md` mục 2A). Đề xuất danh sách để người dùng duyệt.

---

## 2. Có thể xóa ngay (kiểm kê ngày 2026-10-09: không còn tham chiếu)

| File | Bằng chứng | Ghi chú |
|---|---|---|
| `static/css/app-layout.css` | Không template nào nạp | Bộ khung cũ trước `workspace.css` |
| `static/js/account-form.js` | Không template nào nạp | Kiểm tra với người phụ trách Account trước |
| `templates/careers/{index,detail,apply,unavailable,fragments}.html` | Không Controller nào trả về, không trang nào include | Cổng tuyển dụng cũ, đã thay bằng `candidate/landing`, `candidate/job-board`, `candidate/job-detail` |
| `static/css/careers.css`, `static/css/career-pages.css`, `static/js/careers.js` | Chỉ `careers/fragments.html` (đã chết) nạp | Xóa cùng commit với `templates/careers/` |
| Rule alias `.rms-page-*`, `.rms-pagination-*` trong `components.css` (và bản gốc trong `interface.css`) | Không template/JS nào dùng class `rms-page*` | Khu "chỉ được thêm" → Lead xóa |
| `static/js/global.js` | Chỉ còn nạp ở `candidate/landing.html` (đã gỡ khỏi `interview/{list,form}.html` ngày 2026-10-11), chỉ xử lý menu `.user-menu` — markup này **không còn ở đâu** | Gỡ thẻ `<script>` ở `landing.html` rồi xóa file |

---

## 3. Xóa sau khi đạt điều kiện

| File | Đang dùng bởi | Xóa khi |
|---|---|---|
| `templates/fragments/sidebar.html` (chỉ còn cầu nối: `workspace(menu, role)` → `sidebar-shell :: shell`) | `hello.html` (demo) và test (`SidebarMenuTests`, `SidebarRoleWebTests`, probe trong `src/test/resources`). Không còn trang nội bộ nào | Xóa cùng lúc sửa các test đó và xóa `hello.html` |
| `templates/fragments/head.html` → `globalHead` | `auth/fragments.html` | Các trang auth chuyển sang `layout/auth` |
| `templates/fragments/head.html` → `head` (main.css) | `hello.html` | Xóa trang demo `hello.html` + `demo/TestWebController` (hỏi team) |
| `static/css/interface.css` (gồm khối cầu nối phân trang ở cuối file, bản sao mục L `components.css`, giữ khớp bằng `LegacyPaginationBridgeTests`), `static/css/design-tokens.css` | `auth/fragments`, `careers.css` (`@import design-tokens`). `interface.css` hiện không template nào nạp (`interfaceHead` đã xóa 28/10) | `globalHead` đã bị xóa và `careers.css` bị xóa |
| `static/css/main.css` | `fragments/head :: head` (chỉ `hello.html`) | `hello.html` bị xóa |
| `templates/fragments/pagination.html` (adapter `paged(page, baseUrl)`, `pager(...)`) | `admin/accounts/{list,candidates}`, `requisitions/list`, `offers/list`, `job-postings/list` | Các trang này dùng `fragments/ui/pagination :: paged(page)` (Controller trả `Page<T>`) |
| File CSS phẳng của trang (`offers.css`, `requisition-*.css`…) | Trang tương ứng | **Không xóa** — chỉ dọn phần trùng catalog khi migrate. Trang mới mới dùng `css/pages/`. |

Khi xóa xong toàn bộ `fragments/head.html`: cập nhật `UI_CATALOG.md`, `UI_CSS_GUIDE.md` mục 1 (bỏ dòng "Trang nội bộ cũ") và mục 6.

---

## 4. Giữ lại (trông như thừa nhưng có chủ đích)

| File | Lý do |
|---|---|
| `templates/layout/auth.html`, `templates/fragments/auth-layout.html` | Chưa trang nào dùng, nhưng là đích migrate của các trang auth |
| `templates/fragments/ui/dialogs.html` (`confirm`) | Linh kiện catalog, sẵn cho trang mới cần hộp thoại xác nhận |
| `docs/prototype-reference/` | Bản nháp HTML ban đầu, chỉ giá trị lịch sử (không phải code chạy) |

---

## 5. Lịch sử dọn dẹp

| Ngày | Việc | Commit |
|---|---|---|
| 2026-10-09 | `candidate/job-board.html` (cũ) → `candidate/landing.html`; `css/job-board.css` → `css/landing.css`; tách CSS chi tiết sang `css/pages/job-detail.css`; xóa 93 rule chết khỏi `landing.css` (topbar cũ, ô tìm kiếm, phân trang) | `edd3cc7` |
| 2026-10-09 | Xóa `docs/prototype/` (bản trùng, thiếu `indexv1.html` so với `docs/prototype-reference/`) | `chore/dunglt/docs-cleanup` |
| 2026-10-28 | `user/profile.html` (trang nội bộ cuối cùng) sang `layout/internal`; xóa fragment `interfaceHead` khỏi `fragments/head.html` | `feat/dunglt/profile-adw` |

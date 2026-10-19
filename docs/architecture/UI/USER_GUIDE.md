# Hướng Dẫn Sử Dụng Bộ Tài Liệu UI & Cách Nhờ AI Làm Giao Diện

> **Dành cho:** thành viên team khi cần làm giao diện, đặc biệt khi dùng AI (Claude Code, Cursor, Codex, Copilot…) để viết code.
> **Đọc mất:** khoảng 5 phút. Không cần thuộc hết các file còn lại, AI sẽ đọc thay bạn nếu bạn chỉ đúng file.

---

## 1. Hiểu nhanh kiến trúc (1 phút)

Một trang giao diện bây giờ gồm 3 phần:

1. **Layout** – khung chung: topbar, sidebar, footer. Có sẵn, bạn **không** viết lại.
2. **Sidebar theo role** – tự chọn menu theo người đang đăng nhập. Bạn chỉ thêm mục menu khi có trang mới.
3. **File nội dung** – phần riêng của trang bạn. **Đây là phần duy nhất bạn viết.**

Mọi thứ dùng chung (nút, badge, form, card, bảng, phân trang, thẻ việc làm, topbar, logo, nút đăng xuất) đã có sẵn trong **catalog**. Việc của bạn và AI là **dùng lại**, chỉ viết CSS riêng cho phần thật sự chưa có.

---

## 2. Bộ tài liệu có những gì?

Tất cả nằm trong `docs/architecture/UI/`:

| File | Bạn đọc khi nào | AI đọc khi nào |
|---|---|---|
| `USER_GUIDE.md` (file này) | Lần đầu, và khi quên cách nhờ AI | Không cần |
| `README.md` | Muốn biết lộ trình chung, file nào cho việc gì | **Luôn** |
| `UI_RULES.md` | Muốn biết điều gì bị cấm, file nào **không được chạm** | **Luôn** |
| `UI_CATALOG.md` | Tra tên class/fragment khi tự viết | **Luôn** (khi viết markup) |
| `UI_NEW_PAGE.md` | Làm chức năng mới | Khi tạo trang mới |
| `UI_MIGRATION.md` | Chuyển trang cũ của mình sang kiến trúc mới | Khi migrate |
| `UI_CSS_GUIDE.md` | Cần viết CSS riêng | Khi tạo trang mới / migrate |
| `UI_LEGACY_CLEANUP.md` | Muốn xóa file cũ | Khi được giao dọn file |

Phần backend (Controller, Service, DTO…) vẫn theo `docs/architecture/ARCHITECTURE_GUIDE.md`.

---

## 3. Ba việc **tuyệt đối không** làm (và không để AI làm)

1. **Không sửa file khung dùng chung**: `templates/layout/*`, `templates/fragments/layout/*` (trừ menu `sidebars/` của role mình), `fragments/workspace-header.html`, `fragments/brand.html`, `static/css/tokens.css`, `topbar.css`, `workspace.css`. Cần đổi thì báo Lead (Dũng LT). Danh sách đầy đủ: `UI_RULES.md` mục 2.
2. **Không đổi tên/dời file template, không đổi biến backend** (`${...}`, `th:field`, `name`, `id` của form).
3. **Không `style="..."`, không thẻ `<style>`, không mã màu `#...`** trong template.

---

## 4. Cách nhờ AI (copy prompt bên dưới)

### Cách A — AI tự đọc hướng dẫn (khuyến khích)

Nếu repo có file `AGENTS.md` và `CLAUDE.md` ở **thư mục gốc**, các AI agent phổ biến sẽ tự đọc chúng mỗi lần làm việc và tự biết phải đọc tài liệu UI nào. Bạn chỉ cần nói việc cần làm và dặn thêm một câu "làm theo tài liệu UI". Nếu chưa thấy hai file đó ở thư mục gốc, dùng Cách B.

### Cách B — Dán prompt có đường dẫn cụ thể

Thay phần `<...>` rồi dán nguyên khối cho AI.

**1) Tạo trang mới**
```text
Tạo trang mới: <mô tả chức năng>, URL <...>, dành cho role <...>.
Trước khi viết code, đọc theo thứ tự:
1. docs/architecture/UI/README.md
2. docs/architecture/UI/UI_RULES.md
3. docs/architecture/UI/UI_NEW_PAGE.md
4. docs/architecture/UI/UI_CSS_GUIDE.md
5. docs/architecture/UI/UI_CATALOG.md (tra mọi class/fragment trước khi viết)
Bắt chước trang mẫu src/main/resources/templates/candidate/job-board.html
và src/main/resources/static/css/pages/job-board.css.
Backend theo docs/architecture/ARCHITECTURE_GUIDE.md.
Không sửa file thuộc vùng "không được chạm" (UI_RULES mục 2A); cần thì dừng lại hỏi tôi.
Kết thúc bằng checklist ở UI_NEW_PAGE.md mục 6.
```

**2) Migrate trang cũ của mình**
```text
Migrate trang src/main/resources/templates/<feature>/<trang>.html sang kiến trúc layout mới.
Trước khi sửa, đọc theo thứ tự:
1. docs/architecture/UI/README.md
2. docs/architecture/UI/UI_RULES.md
3. docs/architecture/UI/UI_MIGRATION.md
4. docs/architecture/UI/UI_CSS_GUIDE.md
5. docs/architecture/UI/UI_CATALOG.md
Bắt chước trang mẫu src/main/resources/templates/notifications/list.html.
Giữ nguyên tên file, đường dẫn view và mọi biến backend.
Không sửa file thuộc vùng "không được chạm" (UI_RULES mục 2A); cần thì dừng lại hỏi tôi.
Kết thúc bằng checklist ở UI_MIGRATION.md mục 6 và cập nhật bảng trạng thái mục 5.
```

**3) Sửa giao diện nhỏ trong trang của mình**
```text
Trong trang src/main/resources/templates/<feature>/<trang>.html, <mô tả thay đổi>.
Đọc docs/architecture/UI/UI_RULES.md và tra docs/architecture/UI/UI_CATALOG.md trước.
Ưu tiên dùng class/fragment có sẵn; CSS riêng chỉ viết trong file CSS của trang này,
class có tiền tố của trang, chỉ dùng token (var(--...)), không mã màu hex.
```

**4) Thêm mục menu sidebar**
```text
Thêm mục menu "<tên>" dẫn tới <URL> cho role <...>.
Đọc docs/architecture/UI/UI_NEW_PAGE.md mục 5.
Sửa src/main/resources/templates/fragments/layout/sidebars/<role>.html (nguồn menu duy nhất,
không sửa fragments/sidebar.html: file đó chỉ chuyển tiếp).
Copy đúng khuôn một mục <a class="sidebar-nav-link"> có sẵn.
```

**5) Dọn file cũ**
```text
Kiểm tra file <đường dẫn> còn được dùng không theo docs/architecture/UI/UI_LEGACY_CLEANUP.md mục 1.
Chỉ báo kết quả và đề xuất; KHÔNG tự xóa khi chưa có tôi đồng ý.
```

---

## 5. Kiểm tra kết quả AI trả về (2 phút)

Trước khi commit, tự soát nhanh:

- [ ] File trang có `layout:decorate="~{layout/...}"`, **không** tự chép topbar, sidebar, nút đăng xuất.
- [ ] Không có `style="`, `<style>`, `#xxxxxx` trong template (tìm nhanh bằng Ctrl+F).
- [ ] AI **không** sửa file nào trong danh sách mục 3. Nếu `git status` có các file đó → hỏi lại AI vì sao, hoặc hoàn tác.
- [ ] CSS mới (nếu có) nằm trong file CSS của trang, class có tiền tố trang.
- [ ] Mở trang bằng tài khoản từng role được phép: menu sáng đúng, topbar đúng tên, bấm Đăng xuất ra hộp thoại.
- [ ] Thu nhỏ trình duyệt (~ điện thoại): không vỡ bố cục.

---

## 6. Hỏi nhanh

**Catalog không có thứ mình cần thì sao?**
Cho AI viết trong CSS của trang (có tiền tố trang). Nếu thứ đó đã có ở trang khác → báo Lead để đưa vào catalog, không chép sang trang thứ 3.

**AI đòi sửa `components.css` hay `topbar.css`?**
Dừng lại. `components.css` chỉ được **thêm** (không sửa cái có sẵn) và phải cập nhật catalog; `topbar.css`, `tokens.css` chỉ Lead sửa. Xem `UI_RULES.md` mục 2.

**Trang cũ của mình vẫn chạy, có cần migrate ngay không?**
Không bắt buộc ngay. Topbar, logo, nút đăng xuất của trang cũ đã dùng chung với trang mới. Migrate khi bạn sửa đến trang đó, theo `UI_MIGRATION.md`.

**Muốn đổi màu, cỡ chữ cho cả hệ thống?**
Báo Lead. Màu nằm trong `tokens.css`; cỡ chữ topbar/sidebar chỉnh bằng một biến `--chrome-step` trong `topbar.css`.

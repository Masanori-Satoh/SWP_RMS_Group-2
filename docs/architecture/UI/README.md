# Bộ Tài Liệu Giao Diện (UI) — RMS Mộc

> **Dành cho:** Developers và Trợ lý AI làm việc với `templates/`, `static/css/`, `static/js/`.
> **Đọc file này trước tiên.** Nó chỉ ra cần đọc file nào cho từng việc, và lộ trình vận hành chung.
> **Thành viên team lần đầu dùng / muốn prompt AI:** đọc [`USER_GUIDE.md`](USER_GUIDE.md) (5 phút, có prompt mẫu để copy).

---

## 1. Kiến trúc trong một câu

**Một trang = Layout (khung chung) + Sidebar tự chọn theo role + File nội dung (chỉ phần riêng của trang).**
Topbar, logo, góc phải (avatar, đăng xuất), sidebar, phân trang, thẻ việc làm... là **fragment dùng chung**, không bao giờ chép lại markup.

```text
layout/base.html  ── <head> chung: tokens.css, global.css, components.css, CSRF meta, interface.js
├─ layout/internal.html   Trang nội bộ: sidebar theo role + topbar nội bộ + <main>
├─ layout/public.html     Trang công khai: topbar công khai + footer + <main>
└─ layout/auth.html       Dự phòng cho trang xác thực/lỗi (chưa dùng)

File nội dung = layout:decorate="~{layout/...}" + <main layout:fragment="content">…</main>
```

---

## 2. Cần làm gì thì đọc file nào?

| Việc cần làm | Đọc |
|---|---|
| Tra class, token, fragment có sẵn (nút, badge, form, card, bảng, dialog, phân trang, topbar…) | [`UI_CATALOG.md`](UI_CATALOG.md) |
| Biết điều gì bị cấm, ai được sửa file nào, file nào **không được chạm** / **chỉ được thêm** | [`UI_RULES.md`](UI_RULES.md) |
| Tạo một trang **mới hoàn toàn** theo kiến trúc mới | [`UI_NEW_PAGE.md`](UI_NEW_PAGE.md) |
| Chuyển một trang **cũ** sang layout | [`UI_MIGRATION.md`](UI_MIGRATION.md) |
| Viết CSS: đặt ở đâu, đặt tên ra sao, khi nào được thêm vào file dùng chung | [`UI_CSS_GUIDE.md`](UI_CSS_GUIDE.md) |
| Xóa file cũ, file lỗi thời sau khi migrate | [`UI_LEGACY_CLEANUP.md`](UI_LEGACY_CLEANUP.md) |
| Hướng dẫn cho thành viên + prompt mẫu để nhờ AI | [`USER_GUIDE.md`](USER_GUIDE.md) |

---

## 3. Lộ trình vận hành (mọi thay đổi giao diện đi theo vòng này)

```text
 1. Tra catalog ──► có sẵn?  ── có ──► Dùng nguyên (class / fragment)
        │                              │
        │ không / cần khác chút         ▼
        ▼                        4. Dựng trang bằng layout (UI_NEW_PAGE / UI_MIGRATION)
 2. Viết CSS riêng của trang (static/css/pages/<trang>.css, tiền tố trang, chỉ dùng token)
        │
        │ thứ đó xuất hiện ở trang thứ 2?
        ▼
 3. Đề xuất đưa vào khu dùng chung (components.css / fragments/ui/) — Lead duyệt,
    THÊM vào cuối file theo quy tắc "chỉ được thêm", cập nhật UI_CATALOG.md cùng PR
        │
        ▼
 5. Migrate xong một trang ──► cập nhật bảng trạng thái (UI_MIGRATION mục 5)
        │
        ▼
 6. File cũ không còn ai dùng ──► dọn theo UI_LEGACY_CLEANUP
```

Nguyên tắc xuyên suốt:
- **Dùng trước, viết sau.** Viết CSS mới chỉ cho phần catalog thật sự chưa có.
- **Một nguồn cho mỗi thứ dùng chung.** Thấy markup/CSS của topbar, logo, nút, phân trang bị chép ở 2 nơi là lỗi kiến trúc.
- **Không xóa, không sửa đè ở khu dùng chung.** Chỉ thêm (xem `UI_RULES.md` mục 2).
- **Catalog luôn khớp code.** Thêm/đổi linh kiện dùng chung mà không cập nhật catalog = chưa xong.

---

## 4. Bản đồ nhanh: cái gì nằm ở đâu

| Loại | Vị trí |
|---|---|
| Layout | `templates/layout/{base,internal,public,auth}.html` |
| Khung dùng chung (topbar, sidebar, góc phải) | `templates/fragments/layout/`, `templates/fragments/workspace-header.html`, `templates/fragments/brand.html` |
| Menu sidebar theo role | `templates/fragments/layout/sidebars/{admin,candidate,interviewer,recruiter}.html` |
| Linh kiện tái sử dụng | `templates/fragments/ui/{cards,pagination,dialogs}.html` |
| Token & font | `static/css/tokens.css`, `static/css/fonts.css` |
| CSS dùng chung | `static/css/global.css`, `components.css`, `topbar.css`, `workspace.css` |
| CSS riêng của trang (mới) | `static/css/pages/<trang>.css` |
| Dữ liệu dùng chung cho mọi view | `core/web/TopbarUserAdvice` (`topbarUser`), `core/web/ViewHelpersAdvice` (`pageLinks`) |
| Trang mẫu đã đúng kiến trúc | `candidate/job-board.html` (công khai, có tìm kiếm + phân trang), `candidate/job-detail.html`, `notifications/list.html` (nội bộ) |

---

## 5. Dành cho Trợ lý AI

1. Đọc file này → đọc file đúng việc ở mục 2 → **luôn** mở `UI_CATALOG.md` khi viết markup.
2. Trước khi sửa file nào ở `UI_RULES.md` mục 2 (khu "Không được chạm" / "Chỉ được thêm"), dừng lại và hỏi người dùng.
3. Kết thúc bằng checklist của file hướng dẫn tương ứng (`UI_NEW_PAGE.md` mục 6, `UI_MIGRATION.md` mục 6).
4. Không tự xóa file cũ nếu chưa làm đủ các bước kiểm tra trong `UI_LEGACY_CLEANUP.md`.

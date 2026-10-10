# Tạo Trang Mới Theo Kiến Trúc UI (New Page Guide)

> **Dành cho:** Developers và Trợ lý AI khi làm một trang **chưa từng tồn tại** (chức năng mới).
> Trang cũ cần chuyển đổi thì xem [`UI_MIGRATION.md`](UI_MIGRATION.md).
> **Trang mẫu để bắt chước:** `templates/candidate/job-board.html` (+ `static/css/pages/job-board.css`, `CareerPortalController#viewPublicJobList`).

---

## 1. Chọn layout

| Trang dành cho | Layout | Có sẵn |
|---|---|---|
| Người dùng đã đăng nhập, làm việc nội bộ (HR, Director, Interviewer, Admin, cả ứng viên trong dashboard) | `layout/internal` | Sidebar theo role, topbar nội bộ |
| Khách / ứng viên xem thông tin công khai | `layout/public` | Topbar công khai, footer |

Không tạo layout mới. Cần biến thể mới → hỏi Lead (khu "Không được chạm", `UI_RULES.md` mục 2A).

---

## 2. Controller

```java
@GetMapping("/reports")
public String viewReports(@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                          Model model) {
    model.addAttribute("reportsPage", reportService.search(pageable)); // dữ liệu của trang
    model.addAttribute("activeMenu", "reports");                         // chỉ trang nội bộ: mục sidebar đang sáng
    return "reports/list";                                               // = templates/reports/list.html
}
```

- **Không cần** truyền tên, email, role, avatar người dùng: `topbarUser` có sẵn ở mọi view (`TopbarUserAdvice`).
- **Không cần** truyền URL cho phân trang: `pageLinks` có sẵn ở mọi view (`ViewHelpersAdvice`) và tự giữ mọi tham số lọc.
- Số phần tử mỗi trang do Controller quyết định (`@PageableDefault(size = …)`). Fragment phân trang dùng chung cho mọi kích thước.
- Trang công khai cần URL được phép trong `SecurityConfig` (`permitAll`).

---

## 3. File nội dung

```html
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/internal}">
<head>
  <title>Báo cáo tuyển dụng</title>
  <th:block layout:fragment="styles">                 <!-- chỉ khi có CSS riêng -->
    <link rel="stylesheet" th:href="@{/css/pages/reports.css}">
  </th:block>
</head>
<body>
<main id="main" layout:fragment="content" class="page-content container" tabindex="-1">

  <div class="page-header">                            <!-- catalog mục 6 -->
    <div>
      <h1 class="page-title">Báo cáo tuyển dụng</h1>
      <p class="page-subtitle">Theo dõi số liệu theo tháng</p>
    </div>
    <a th:href="@{/reports/export}" class="btn btn--outline">Xuất báo cáo</a>
  </div>

  <form th:action="@{/reports}" method="get" class="card rpt-filter">   <!-- catalog card + class phụ của trang -->
    <div class="form-group">
      <label class="form-label" for="rpt-month">Tháng</label>
      <input id="rpt-month" type="month" name="month" class="form-input" th:value="${selectedMonth}">
    </div>
    <button type="submit" class="btn btn--primary">Lọc</button>
  </form>

  <div class="table-wrapper">                          <!-- catalog mục 7 -->
    <table class="table">…</table>
  </div>

  <!-- Phân trang dùng chung: giữ bộ lọc khi chuyển trang -->
  <nav th:replace="~{fragments/ui/pagination :: paged(page=${reportsPage})}"></nav>
</main>
<th:block layout:fragment="scripts">                    <!-- chỉ khi có JS riêng -->
  <script defer th:src="@{/js/pages/reports.js}"></script>
</th:block>
</body>
</html>
```

Quy tắc khi viết nội dung:
- **Tra `UI_CATALOG.md` cho từng khối** trước khi viết: tiêu đề trang, form, nút, badge, bảng, card, thẻ việc làm, dialog, alert, phân trang.
- Lặp fragment: `th:each` ở thẻ ngoài, `th:replace` ở thẻ trong (`UI_RULES.md` mục 6).
- Không `style="..."`, không `<style>`, không màu hex.

---

## 4. CSS riêng của trang (nếu cần)

Tạo `static/css/pages/<trang>.css` theo [`UI_CSS_GUIDE.md`](UI_CSS_GUIDE.md). Tóm tắt:
- Mở đầu file bằng comment: trang nào, tiền tố class là gì, file chỉ chứa phần catalog chưa có.
- Mọi class có **tiền tố trang** (`rpt-`, `jb-`…). Chỉ dùng token (`var(--s4)`, `var(--radius-md)`, `var(--brand)`).
- Không định nghĩa lại class dùng chung. Muốn khác một chút: thêm class phụ có tiền tố bên cạnh class catalog (`class="card rpt-filter"`).

Ví dụ thật: `static/css/pages/job-board.css` chỉ có bố cục lưới của form tìm kiếm, thanh kết quả và trạng thái rỗng; thẻ việc làm, form, nút, phân trang đều là catalog.

---

## 5. Menu sidebar (trang nội bộ)

1. Thêm một mục `<a>` vào `templates/fragments/layout/sidebars/<role>.html` của **mỗi role được phép vào trang**, copy đúng khuôn mục có sẵn (icon SVG + `nav-label-text` + `data-tooltip`), `activeMenu` khớp giá trị Controller đặt.
2. **Giai đoạn chuyển tiếp:** trang cũ vẫn dùng menu trong `templates/fragments/sidebar.html`. Cho đến khi migrate xong, mục menu mới phải thêm ở **cả hai** nơi.
3. Kiểm tra quyền truy cập URL trong `SecurityConfig` khớp với các role có menu.

---

## 6. Checklist trước khi báo "xong"

- [ ] Trang dùng `layout:decorate`; không tự dựng sidebar, topbar, logo, footer, hộp thoại đăng xuất.
- [ ] Controller đặt `activeMenu` (trang nội bộ); không tự truyền thông tin người dùng cho topbar.
- [ ] Mọi khối có trong catalog đều dùng class/fragment của catalog; phân trang dùng `fragments/ui/pagination :: paged`.
- [ ] CSS riêng (nếu có) ở `static/css/pages/<trang>.css`, class có tiền tố, chỉ dùng token, không định nghĩa lại class dùng chung.
- [ ] Không `style="..."`, `<style>`, màu hex trong template.
- [ ] Menu đã thêm cho đúng role (cả 2 nguồn menu trong giai đoạn chuyển tiếp).
- [ ] Đã mở trang với từng role được phép; thu nhỏ màn hình không vỡ; bấm Đăng xuất ra hộp thoại chung.
- [ ] Nếu đã thêm linh kiện dùng chung: đã cập nhật `UI_CATALOG.md` cùng PR.

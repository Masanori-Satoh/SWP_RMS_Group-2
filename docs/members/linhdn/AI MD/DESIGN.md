# Mộc RMS — Shared Design Contract

## Overview

Đây là hợp đồng thiết kế chung của Codex và Claude Code, được rút từ source hiện tại ngày 2026-10-27, không sao chép một bộ Awesome DESIGN.md. CSS đang được trang nạp là nguồn giá trị triển khai; tài liệu này chỉ rõ cách giữ và áp dụng chúng, không tự cấp phép sửa source.

Mộc có các bề mặt khác nhau: workspace nội bộ phục vụ thao tác tuyển dụng; candidate portal; landing/jobs công khai; xác thực. Giữ nhận diện xanh lá trầm, nền sáng ấm, tương phản dễ đọc, logo lá và tên Mộc. Workspace ưu tiên bảng, bộ lọc, số liệu và hành động rõ ràng; landing có hình ảnh văn phòng/cây xanh; auth dùng câu chuyện bên trái và form bên phải. Không ép mọi trang thành cùng một bố cục.

**Nguồn đã đọc:** `templates/layout/{base,internal,public,auth}.html`, `templates/auth/{login,fragments}.html`, `templates/candidate/landing.html`, `templates/fragments/brand.html`, `static/css/{tokens,fonts,global,components,topbar,workspace,auth,candidate-shell,landing}.css`, `static/css/pages/admin-dashboard.css`, cùng `docs/architecture/UI/{UI_RULES,UI_CATALOG,UI_CSS_GUIDE}.md` (các đường dẫn templates/static đều dưới `src/main/resources/`).

**Giới hạn bằng chứng:** đây là baseline từ template/CSS/component thật của dự án. Truy cập trình duyệt `http://localhost:8081/login` trả `ERR_CONNECTION_REFUSED`; chưa xác nhận computed styles hoặc ảnh chụp runtime. Không suy ra lỗi sản phẩm từ việc server không chạy. Không khởi động backend, không thay đổi UI trong lần thiết lập này.

`PRODUCT.md` chưa cần cho cấu hình và refinement có phạm vi hiện tại. Chỉ tạo khi công việc tương lai cần ngữ cảnh sản phẩm bền vững và đã có dữ kiện xác thực; không tự chạy init hoặc suy diễn định vị, số liệu hay lời hứa tuyển dụng.

## Colors

Nguồn token chung đang hoạt động là `static/css/tokens.css`; `layout/base.html` và `auth/fragments.html` nạp nó trước `global.css` và `components.css`. `design-tokens.css` là file riêng cũ; không xem hai file là hai nguồn chủ đạo ngang nhau.

| Vai trò | Token chung | Giá trị đang khai báo |
| --- | --- | --- |
| Thương hiệu | `--brand` / `--brand-dark` / `--brand-soft` | `#3e7157` / `#2e5a45` / `#e7f0e8` |
| Nền | `--page` / `--surface` / `--surface-subtle` | `#faf9f6` / `#ffffff` / `#f8faf8` |
| Chữ | `--ink` / `--muted` | `#252923` / `#59635b` |
| Viền | `--line` / `--control-line` | `#d9ded7` / `#7b8b7d` |
| Focus / cảnh báo | `--focus` / `--warning` | `#a06325` |
| Thành công | `--success` / `--success-soft` | `#2e5a45` / `#e7f0e8` |
| Cảnh báo nền | `--warning-soft` | `#fff4e5` |
| Lỗi | `--error` / `--error-soft` | `#87372f` / `#f7ece8` |
| Trung tính | `--neutral` / `--neutral-soft` | `#59635b` / `#f1f5f9` |

Đây là giá trị để tra cứu, không phải lời mời hardcode. Dùng `var(...)` trong CSS trang. Không đổi màu trạng thái theo một palette do skill gợi ý.

Landing có bộ biến riêng ở `landing.css`, gồm `--brand: #2e5a45`, `--brand-dark: #1b4332`, `--pine: #154b38` và các biến theo section. Landing hiện nạp `global.css` → `landing.css` → `topbar.css`, không nạp trực tiếp `tokens.css` hoặc `components.css`; kiểm tra cầu nối token và component riêng của trang trước khi chỉnh. Giữ biến thể này trong phạm vi landing, không lan sang workspace. Những khác biệt có sẵn không phải yêu cầu hợp nhất theme.

## Typography

- `tokens.css`: `--sans: 'Inter', 'Source Sans 3', 'Segoe UI', system-ui, -apple-system, sans-serif`; `--serif: 'Lora', 'Times New Roman', Georgia, serif`. `fonts.css` khai báo Lora và Source Sans 3 WOFF2 nội bộ; token nhắc Inter không chứng minh Inter đã tải thành công.
- `global.css`: body 16px/1.55, weight 400; h1/h2 dùng serif weight 600, tracking -0.015em. H1 `clamp(26px, 2.8vw, 36px)`/1.22; h2 `clamp(20px, 2.2vw, 28px)`/1.25; h3 sans 18px/1.35 weight 600. Token `--font-base` là 15px, khác cỡ body thực tế 16px: chọn theo component hiện có.
- Auth hiện override `--sans` trong `body.auth-page` thành Arial/Segoe UI; tiêu đề form là sans, không tự đổi về serif. `.auth-statement` và `.auth-card h1` có thang riêng trong `auth.css`.
- Candidate shell dùng Plus Jakarta Sans qua `.page-wrapper--candidate`; landing có hệ chữ riêng. Template hiện đã có Google Fonts cho Plus Jakarta Sans/Lora/Playwrite VN ở landing và Plus Jakarta Sans ở candidate. Đây là hiện trạng được ghi nhận; không thêm CDN/font mới trong việc refinement thông thường.
- Giữ ngôn ngữ theo bề mặt và nội dung đang có. Auth/landing có tiếng Việt; layout chung có `th:lang` phụ thuộc context. Không tự dịch toàn bộ, đổi copy nghiệp vụ hoặc bỏ dấu. Kiểm tra xuống dòng với tiếng Việt, tên dài, email và zoom.

## Layout

- Chuỗi layout chung: `layout/base` → `layout/internal` hoặc `layout/public`; `layout/auth` cũng tồn tại. Login hiện là template độc lập gọi `auth/fragments`, không giả định mọi trang auth decorate `layout/auth`.
- `global.css` định nghĩa `.container` rộng `min(calc(100% - 64px), 1180px)`, còn `calc(100% - 32px)` dưới 768px. Thang `--s1/s2/s3/s4/s6/s8/s12/s16/s20` tương ứng 4/8/12/16/24/32/48/64/80px.
- Workspace có sidebar 68px khi thu gọn, 250px khi ghim; topbar 74px. Giữ logic role, menu, active state, ghim, mobile và margin liên quan. Candidate shell không có sidebar, dùng topbar riêng; không áp navigation nhân viên cho ứng viên.
- Auth hiện có hai cột (`minmax(0, 1fr)` và cột form 420–541px), ảnh nền trang trí `.auth-scene`, card form và tabs; đổi về một cột dưới 992px, form đăng ký về một cột dưới 768px. Không để ảnh/decoration chắn link, form hoặc topbar.
- Dashboard admin hiện dùng `.adm-dashboard`: dải số liệu, các panel phân tích và service ledger. Giữ số liệu từ model/script; không tự thêm KPI, biểu đồ hoặc claim giả.
- CSS trang mới đặt trong `static/css/pages/`, JS trang trong `static/js/pages/`; dùng prefix riêng theo trang. Giữ stylesheet cũ của feature khi chưa có yêu cầu chuyển đổi.
- Bảng có wrapper cuộn ngang có chủ đích; không loại bỏ khả năng xem cột để đạt một quy tắc chung “không có horizontal scroll”. Giữ điều hướng, bộ lọc, phân trang và hành động truy cập được ở màn hình nhỏ.

## Elevation & Depth

Card chung ưu tiên nền trắng và viền `--line`. Các bóng trong `tokens.css`: `--shadow-sm` = `0 1px 2px rgba(37, 41, 35, 0.05)`, `--shadow-md` = `0 4px 12px rgba(37, 41, 35, 0.08)`, `--shadow-lg` = `0 12px 28px rgba(37, 41, 35, 0.12)`. `.card--elevated` dùng shadow-md; auth card dùng shadow-lg. Không thêm glow/glass/shadow đậm theo gợi ý mặc định của skill.

Transition chung `--transition` là 0.18s, sidebar 0.22s. Giữ focus và `prefers-reduced-motion` của bề mặt hiện tại; thêm chuyển động chỉ khi cần giải thích trạng thái hoặc được yêu cầu, không bật hiệu ứng toàn trang chỉ vì Taste được cài.

## Shapes

Token góc chung: `--radius-sm: 4px`, `--radius-md: 8px`, `--radius-lg: 12px`, `--radius-full: 9999px`. Nút và input chung dùng radius-md, không dùng pill; pill dành cho badge/chip/avatar theo UI_RULES.

`.card` chung là 8px; panel admin có 12px. Auth card hiện có override 17px trong `auth.css`. Ghi nhận ngoại lệ theo bề mặt, không sửa nó về token chung trong đợt cấu hình và không nhân rộng thành quy chuẩn mới.

## Components

Tra `UI_CATALOG.md` và CSS/fragment thật trước khi thêm class hoặc markup. `components.css` có các alias cũ (ví dụ `.btn-primary`); giữ tương thích, ưu tiên cú pháp BEM đã được catalog mô tả khi viết mới.

| Thành phần | Hợp đồng hiện có |
| --- | --- |
| Button | `.btn` + `--primary/--secondary/--outline/--danger/--sm`; min-height 48px, nhỏ 40px, radius 8px; primary dùng brand-dark, hover ink; giữ disabled/busy state. |
| Form | `.form-group`, `.form-label`, `.form-input/.form-select/.form-textarea`; input 48px, label hiện hữu, lỗi gần field và `aria-invalid`; giữ `id/name`, `th:field`, validation và submit. |
| Card | `.card`, `.card-header/.card-title/.card-body/.card-footer`; nền surface, viền line, padding s6; elevated là biến thể có sẵn. |
| Status | `.badge` và `.alert` cùng biến thể semantic; luôn giữ chữ/trạng thái, không truyền nghĩa chỉ bằng màu. |
| Table / pagination | Tái sử dụng `.table-wrapper/.table` và fragment phân trang hiện có; giữ sort/filter/query params, quyền và row actions. |
| Brand / navigation | `fragments/brand`, topbar/sidebar/footer theo layout; không chép logo hoặc logout dialog sang trang riêng. |
| Dialog / logout | Giữ dialog, thao tác xác nhận, focus/keyboard, POST logout và CSRF; không thay bằng link GET hoặc nút giả. |

Focus chung: `:focus-visible` có outline 3px màu focus, offset 4px; `.btn` dùng offset 2px. Giữ skip link `#main`, label, accessible name, thứ tự keyboard, `role=alert/status` và cơ chế password/OTP đã có. Cài skill không chứng minh toàn bộ UI đã đạt accessibility.

## Do's and Don'ts

**Thứ tự quyết định thiết kế:** DESIGN.md → UI UX Pro Max → Impeccable → Taste Skill. Đây là routing bằng hướng dẫn và metadata, không phải automatic hook. Yêu cầu trực tiếp của người dùng và giới hạn của UI_RULES vẫn ràng buộc. Nếu tài liệu và source lệch nhau, kiểm tra cascade của đúng trang và hỏi trước khi đổi nhận diện hoặc cấu hình cũ; không âm thầm “chuẩn hóa”.

1. Đọc DESIGN.md và UI_RULES/UI_CATALOG/UI_CSS_GUIDE; chọn đúng surface và layout.
2. UI UX Pro Max bổ sung UX/accessibility/responsive. Không dùng bộ palette/font/React/Tailwind mặc định của skill để thay hệ thống Mộc.
3. Impeccable hỗ trợ critique, refinement và tài liệu theo yêu cầu. Giữ nhận diện, copy thật và chức năng khi refinement. Không tự tạo PRODUCT.md, thay DESIGN.md hoặc cài/bật hook.
4. Taste chỉ dành cho yêu cầu cải thiện mạnh về thiết kế hoặc lời gọi tường minh cho mục đích đó. Routine fix, audit, polish nhỏ và cài cấu hình không kích hoạt Taste. Codex dùng `$design-taste-frontend`; Claude Code dùng `/design-taste-frontend` (manual-only). Taste phải giữ palette/type hiện có (dùng `--keep palette,type` khi chạy draw), stack và các thành phần ở trên; việc có một gợi ý layout không tự cho phép đổi brand, protected file hoặc chức năng.

Giữ template routes, `${...}`, `th:*`, layout slots, fragment signature, CSRF, validation, OTP/countdown, data attributes và hành vi phân quyền. Không đổi backend, schema hay nghiệp vụ trong tác vụ presentation/configuration. Bọc `th:if/th:each` bên ngoài phần tử `th:replace` khi cần, theo UI_RULES.

Không tự sửa các file shared được UI_RULES đánh dấu bảo vệ (layout, tokens/fonts, topbar/workspace, navigation JS, brand, core web advice). Shared components/global/fragments thuộc vùng chỉ append theo hợp đồng; sửa selector/API cũ cần quyền rõ ràng. CSS trang không redefine class shared hay thêm inline style/raw color.

Báo cáo phạm vi file và bằng chứng kiểm tra thật. Không commit/push, xóa file, thay cấu hình khác hoặc bật automatic hooks khi chưa được duyệt. Hook Impeccable có sẵn trong `.codex/hooks.json` được giữ nguyên theo quyết định người dùng ngày 2026-10-27; không tuyên bố nó đã bị tắt.

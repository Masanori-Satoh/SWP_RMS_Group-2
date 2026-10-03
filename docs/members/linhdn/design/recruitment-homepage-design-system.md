# Design system — corporate career site Mộc (prototype)

## Trạng thái hiện tại — Spring career integration 01/10/2026

Source UI: `src/main/resources/templates/careers/`, `static/css/careers.css`, `static/css/career-pages.css`, `static/js/careers.js`. `/` và `/jobs` được Spring phục vụ; prototype chỉ là entrypoint. Tất cả literal UI dùng tiếng Anh; DB tiếng Việt nguyên văn. Source of truth SQL/seed không đổi.

Giữ warm off-white/forest, Lora600 và Source Sans3, hairline, compact scale, spacing4/8px; fonts self-host trong static/fonts với OFL. Hero có illustrative project notes có nghĩa với workflow software; không geometry giả ảnh/người SVG. Không tải stock, gradients, marketplace/SaaS hay metrics giả.

Header sticky, anchor offset; menu native details dưới1250px để toàn bộ navigation/auth actions vừa đủ. Job grid3/2/2/1 và process4/2/2/1 ở1440/1024/768/375. Mobile department một hàng scroll với cue khi overflow, openings trước0, zero-count muted/dashed. Filters sát results, NFD+đ/Đ; All Departments giữ focus khi Clear ẩn. Cards là DB markup duy nhất, title native anchor phủ hit area.

Role detail là `/jobs/{id}`, giữ sáu metadata/source wording và cấu trúc public JobPosting. Không tách trách nhiệm từ internal requisition hoặc bịa preferred qualifications. Paragraph lines giữ dấu list DB, không double bullet. Guest Apply → session login → đúng role/apply, Candidate readonly prefill với legend/Not provided; CV/Submit disabled. Notice availability có trước login. Header/footer theo actual viewer; logout CSRF POST. Common benefits fiction chỉ ở homepage, role benefits theo DB. Contact/website chưa có nguồn duyệt nên bỏ link `.example`.

Review baseline độc lập27/40, CLI2 false positives type được computed browser xác nhận. Xem [critique](design/2026-10-01-career-impeccable-critique.md), [test/limits](tests/2026-10-01-career-integration.md) và [flow](flows/career-homepage-apply-flow.md). Không xem static preview/auth fixture là E2E hoặc production ready.

## Lịch sử — audit Gemini 01/10/2026 (standalone, đã được thay thế ở trên)

Phần này thay thế các quyết định về search, font, SVG người, copy demo và CTA trong phần lịch sử phía dưới. Người dùng cho phép các thay đổi này; giữ direction editorial minimal, một công ty, trắng ấm/xanh rừng, hairline, compact type. Không sửa ứng dụng Spring Boot.

### Layout

Header → hero giới thiệu + CTA → open positions cùng filters → về Mộc → benefits/working policy → đời sống → quy trình → gửi CV qua email → footer. Container 1180px; spacing chủ đạo 4/8/12/16/24/32/48/64/80px. Section 80px desktop, 64px tablet, 48px mobile.

Positions 3 cột trên 1100px, 2 cột từ 601–1100px, 1 cột tới 600px. Process 4/2/1 cột cùng breakpoint; about/life split thành một cột ở 768px. Native mobile menu dưới 900px; CAREERS luôn hiện. Benefits là dl theo hàng, culture dùng hình học phẳng + danh sách. Không còn SVG người hoặc eyebrow lặp.

### Typography và media

Lora 600 thật cho heading; Source Sans 3 variable 400–600 cho body/controls. WOFF2 subset Latin/Latin-ext/Vietnamese chính thức được nhúng trong HTML để chạy offline, font-display swap. Giấy phép tại [FONT_LICENSES.txt](../prototype/FONT_LICENSES.txt). Nguồn: [Lora](https://fonts.google.com/specimen/Lora), [Source Sans 3](https://fonts.google.com/specimen/Source+Sans+3).

H1 tối đa 46px desktop, 32px mobile; H2 28–34px; job title 20px; body/input 16px; metadata 13–14px. Heading tracking −0.015em, line-height thoáng. Không synthetic Georgia 600, không max-width 310px cho đoạn mobile. Logo lá giữ nguyên; visual culture chỉ là disc/bar/frame hình học, không giả ảnh đội ngũ.

### Tương tác và accessibility

- Keyword/location/department pills ngay trên results; lọc tức thời, NFD + đ/Đ mapping, nhiều token kết hợp AND. Counts theo keyword/location, chỉ một department pressed.
- Clear chỉ hiện khi active; không cuộn về Hero. Chỉ chuyển focus về keyword trong positions khi nút clear/reset hiện tại sắp bị ẩn.
- Một title anchor native với hit area phủ card; Ctrl/middle click vẫn là link. Trade-off: khó chọn chữ trên card bằng chuột; nội dung detail chọn được.
- Hash `#role/<slug>` mở native dialog: department/location/type/salary/date + overview/responsibilities/requirements/preferred/benefits. Back/Forward và URL dán trực tiếp hỗ trợ. Không SPA.
- Copy link phản ánh clipboard thật hoặc hướng dẫn lấy URL. Apply chỉ mở mail client, không báo đã nhận CV.
- Giữ skip link, focus-visible, targets ≥44px, input labels, live count atomic, aria-pressed và reduced motion. Dialog labelledby; bỏ describedby vì nội dung nhiều section. Close/Escape/backdrop trả focus theo ngữ cảnh.
- Hairline trang trí tách viền control #7B8B7D: trên white đo 3,60:1; trên soft green 3,09:1. Kết quả test không thay chứng nhận WCAG.

### Product truth và nội dung mẫu

Tên Mộc/JD/lương/ngày đăng/benefits/culture là fiction được user cho phép, liệt kê tại [audit](design/2026-10-01-homepage-gemini-audit.md). Không lặp disclaimer trên UI; source comments/docs ghi fixture. Email `tuyendung@moc.example` và company URL `https://moc.example` là reserved placeholder, không nhận hồ sơ. Bỏ account/privacy/terms/FAQ chưa có trang; không fake modal hoặc bịa street address/testimonial/headcount.

### Skills và bằng chứng

UI/UX Pro Max focused queries cho typography, live-result context và touch spacing; không có verified exact match cho single-company careers hoặc cặp Lora + Source Sans 3. Cặp font là lựa chọn theo brief và nguồn official, không áp palette/gợi ý wellness khác ngành.

Impeccable polish/craft floor giữ art direction; critique hai assessment độc lập được user duyệt. [Tài liệu test từng bước](tests/2026-10-01-homepage-gemini-audit.md) ghi kết quả và giới hạn Chrome/frontend.

---

## Lịch sử — thiết kế 30/09 và polish đầu 01/10

Các quyết định dưới đây mô tả phiên bản cũ; khi mâu thuẫn với phần trạng thái hiện tại, dùng phần hiện tại.

**Phạm vi:** Một trang tuyển dụng độc lập cho **một doanh nghiệp**. Tên “Mộc”, mô tả doanh nghiệp, vị trí và lương là nội dung minh họa; trang không kết nối backend, không nhận hồ sơ thật. Không có nhà tuyển dụng thứ ba hoặc danh bạ công ty.

## 1. Phân tích ảnh tham chiếu

- **Bố cục:** header mỏng; hero nhiều khoảng trắng với tiêu đề serif, lời dẫn ngắn, thanh tìm kiếm gọn; minh họa người ở nơi làm việc; phần vị trí tuyển dụng ngay sau hero.
- **Tỷ lệ và nhịp:** chữ hero rõ nhưng không chiếm trọn màn hình; minh họa là điểm nhấn duy nhất. Các khối tiếp theo tách nhau bằng khoảng trắng lớn hơn là nền màu hoặc card.
- **Ngôn ngữ tạo hình:** nét vẽ đen mảnh, mảng xanh lá nhạt ở cây/người, nút tối, đường viền nhạt, card việc làm nhỏ và phẳng.
- **Điều chỉnh:** không sao chép bố cục từng pixel. Search có ba trường để tìm **chỉ trong vị trí của Mộc**; văn bản không dùng khẩu hiệu “dream job” hoặc Lorem ipsum.

## 2. Layout pattern

**Editorial corporate careers:** header ngắn → hero giới thiệu công ty đang tuyển + minh họa nét vẽ → tìm vị trí trong công ty → vị trí đang tuyển → phòng ban → vì sao làm việc tại đây → đời sống công ty → quy trình tuyển dụng → CTA ứng viên → footer. Đây là site thương hiệu nhà tuyển dụng, không phải marketplace.

- Container tối đa 1180px; hero giữ nội dung trong khoảng 880px, minh họa rộng vừa phải.
- Vị trí tuyển dụng hiển thị lưới ba cột desktop, hai cột tablet, một cột mobile; card viền 1px, không shadow nổi.
- Phòng ban là các nút lọc văn bản gọn, không phải danh bạ nhà tuyển dụng.
- Responsive tại 1024/768/375px; điều hướng thu gọn, form xếp dọc và hình giảm chiều cao trên mobile.

## 3. Typography

Tiêu đề dùng `Georgia`, `Times New Roman`, serif; giao diện dùng `Segoe UI Variable`, `Segoe UI`, `Noto Sans`, Arial, sans-serif. Chọn font hệ thống để **một `index.html` hoạt động offline**. Sau polish 01/10: H1 tối đa 46px desktop, 30–36px mobile; h2 28–34px; tiêu đề vị trí 20px; body 16px/1.55. Metadata card 12–14px, label search 12px và input/select 16px. Chữ serif dùng ở hero, section và tiêu đề vị trí; không dùng cho form hoặc metadata. Heading dùng `text-wrap: balance`; đoạn giới thiệu giới hạn khoảng 64–65ch.

## 4. Màu, khoảng cách và đường viền

| Token | Value | Use |
|---|---|---|
| `--page` | `#FAF9F6` | Nền trắng ấm |
| `--surface` | `#FFFFFF` | Form và card việc làm |
| `--ink` | `#252923` | Chữ chính, CTA tối |
| `--muted` | `#59635B` | Chữ phụ |
| `--line` | `#D9DED7` | Divider/border 1px |
| `--brand` | `#3E7157` | Điểm nhấn xanh lá trầm |
| `--brand-soft` | `#E7F0E8` | Lá cây, nhãn nhẹ, nền section |
| `--focus` | `#A06325` | Focus ring đủ rõ trên nền sáng |

Spacing 4/8/12/16/24/32/48/72px; bán kính 7–12px. Nút chính hình viên gọn như ảnh tham chiếu, không glow. Shadow rất nhẹ hoặc không dùng. Màu xanh chỉ nhấn điểm quan trọng; không dùng gradient.

## 5. Component và tương tác

- Header gồm wordmark Mộc, điều hướng nội bộ, CTA xem vị trí và thao tác hồ sơ ứng viên ở trạng thái demo trung thực.
- Search có nhãn riêng cho từ khóa, phòng ban, địa điểm; Enter hoặc nút tìm lọc danh sách tĩnh của **Mộc**. Số kết quả chỉ đếm các card minh họa đang hiện; trạng thái rỗng có nút xóa lọc.
- Mỗi card có chức danh, phòng ban, địa điểm, hình thức, lương nếu có, ngày đăng minh họa và hành động xem chi tiết. Hành động ứng tuyển giải thích rõ prototype chưa gửi hồ sơ.
- Bộ phận là nút lọc; nav và CTA cuộn đến section thật. Không có link dẫn đến trang chưa tồn tại.
- Ảnh hero là SVG nội tuyến người, bàn làm việc và cây theo một ngôn ngữ nét vẽ nhất quán; không cần asset/CDN.

## 6. Accessibility

Semantic landmarks, một h1, thứ bậc heading, skip link, label thật, nút ≥44px, tương phản chữ thường ≥4.5:1, focus-visible, `aria-live` cho kết quả, dialog đóng bằng Escape và có nút đóng. Mobile không cuộn ngang. Chuyển động hover nhẹ và tôn trọng `prefers-reduced-motion`.

Sau polish: cả liên kết/input/select/summary có vùng tương tác tối thiểu 44px; skip link chuyển focus tới main. Submit hoặc chọn phòng ban chuyển focus đến heading kết quả. Nút xem vị trí có accessible name gồm chức danh. Dialog chỉ đóng khi click thật ở ngoài khung hoặc dùng nút/Escape; khoảng đệm trong dialog không làm đóng. Menu mobile đóng bằng lựa chọn, tap ngoài hoặc Escape.

## 7. Anti-patterns

Không có nhiều công ty, “Post a Job”, gói nhà tuyển dụng, công ty đang tuyển, chỉ số giả, logo đối tác giả, testimonial giả, gradient tím/xanh, glassmorphism, bento SaaS, carousel, card lồng nhau, hero quá khổ, nút phát sáng hoặc minh họa 3D.

## Cơ sở chọn lựa từ UI/UX Pro Max

Truy vấn `--design-system` cho corporate career site đưa ra mẫu **Hero + Features + CTA**, tương thích với nhịp trang; truy vấn typography đưa ra cặp **Newsreader + Roboto** và **Libre Bodoni + Public Sans** cho editorial serif/sans. Dữ liệu skill **không có kết quả xác minh** cho product type “single-employer corporate careers”; bảng màu hồng và font Outfit mặc định cũng không hợp ảnh/yêu cầu. Vì vậy hệ màu, chữ offline và cấu trúc vị trí tuyển dụng ở trên là lựa chọn thiết kế theo ảnh tham chiếu và ràng buộc người dùng, không phải kết quả dữ liệu skill được áp nguyên xi.

## Polish 01/10/2026 bằng Impeccable

Giữ màu/font/copy/minh họa/thứ tự section của incumbent theo yêu cầu refinement. Không thay tên công ty minh họa hoặc thêm feature. Giữ các label sẵn có và SVG đã chốt trong lần thiết kế trước; không áp các gợi ý thay media/font của skill thành một redesign.

- Spacing section chuyển sang một scale fluid 48–76px; phần phòng ban ngắn hơn để nhịp trang rõ. Card giảm khoảng trống giữa metadata/footer; với nội dung hiện tại đo được khoảng 194px thay vì 220px.
- Hero desktop gọn hơn; tablet có keyword cả hàng, hai select cùng hàng và nút submit cả hàng; mobile xếp dọc. Controls lớn hơn là lựa chọn ưu tiên khả năng thao tác, nên chiều cao search ở tablet/mobile tăng.
- Reset `figure` margin về 0 để culture illustration thẳng container. Focus, caret, selection và số ngày/lương dùng các giá trị nhất quán với palette hiện có.
- Xác minh Chrome CDP và giới hạn ở [biên bản polish](tests/2026-10-01-homepage-polish.md).

## Shared Auth/Admin system — 01/10/2026

Design hiện tại sau career integration dùng local **Lora600 + Source Sans 3 (400–600)**,
warm offwhite/forest/hairline. Auth/Admin reuse `design-tokens.css`, shared brand,
native form/feedback và operational workspace. Các font/media/copy ghi ở phần
prototype lịch sử phía trên không mô tả literal UI hiện tại. Không thay DB content.

- Auth: forest/editorial panel + focused440px form; mobile compact brand header.
- Admin:208/184px sidebar,72px header, native tables, grouped2→1col forms; metric rows
  thay card decoration, tablet overview2cols. Có Candidate account/confirmed deactivation.
- Form control48px; buttons/toggles/summary≥44px; semantic hint/error references,
  native focus/menu/dialog behaviors; local scroll tables giữ account identity.
- [Report10topics](design/2026-10-01-auth-admin-report.md),
  [critique/audit](design/2026-10-01-auth-admin-critique.md),
  [four-width/test steps](tests/2026-10-01-auth-admin-ui.md).

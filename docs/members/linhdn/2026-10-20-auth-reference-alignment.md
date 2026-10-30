# Áp dụng mẫu Auth — Mộc RMS (20/10/2026)

## Mẫu và phạm vi

Tham khảo `docs/prototype-reference/moc_register_single_progress.html`; đối chiếu UI Rules, CSS Guide, Catalog và ngoại lệ auth tại `UI_MIGRATION.md` mục 5. Giữ `auth/fragments`, không sửa các master layout, token, font hoặc component dùng chung.

Áp dụng cho sáu màn: đăng nhập, đăng ký, xác minh đăng ký, quên mật khẩu, xác minh khôi phục và đặt mật khẩu mới.

## Thay đổi

- Bố cục hai cột ở desktop, một cột khi màn hình hẹp; cột giới thiệu và form có khoảng cách theo mẫu. Login/Recovery căn card theo chiều dọc, Register giữ chiều cao tự nhiên. Màn thấp cuộn toàn trang, không có scrollbar riêng trong form.
- Dùng minh họa gốc nhúng trong prototype ở đáy trang, nền xanh rất nhẹ và các điểm nhấn nhỏ. Không tạo mới nội dung quảng cáo hoặc dữ liệu nghiệp vụ.
- Tiêu đề/form dùng `var(--sans)` và font offline hiện có. Bỏ Google Fonts và bộ token legacy khỏi phần auth; dùng `tokens.css`, `components.css` và style logo từ `topbar.css`, không sửa các file này.
- Tái sử dụng `.card`, `.btn`, `.form-input`, `.form-label`, `.form-hint`, `.form-error`, `.alert`; CSS riêng chỉ chứa bố cục và thành phần auth đặc thù. Không có mã màu trực tiếp, inline style, `!important` hoặc ghi đè token root trong CSS auth mới.
- Register giữ đủ năm trường; hai cột trên desktop, email chiếm hàng riêng. Khi hẹp, chuyển một cột.
- Một đường tiến trình ngang cho hai bước đăng ký và ba bước khôi phục, giữ `aria-current="step"`. Các bước chỉ báo tiến trình, không tạo điều hướng giả.
- Nút chính có mũi tên SVG cùng hệ icon auth. Script chỉ thay nhãn khi đang gửi để giữ icon; khôi phục nhãn khi quay lại trang.
- Giữ nút hiện/ẩn mật khẩu, label/aria, focus-visible, error summary liên kết field, nhập/dán sáu chữ số OTP và input dự phòng khi không có JavaScript.

## File sửa/tạo

| File | Nội dung |
|---|---|
| `src/main/resources/templates/auth/login.html` | Card, input/button theo catalog, scene và icon submit |
| `src/main/resources/templates/auth/register.html` | Như trên, giữ lưới năm trường và cooldown |
| `src/main/resources/templates/auth/register-otp.html` | Scene, card/form và icon submit |
| `src/main/resources/templates/auth/forgot-password.html` | Đồng bộ khôi phục bước 1 |
| `src/main/resources/templates/auth/reset-password-otp.html` | Đồng bộ khôi phục bước 2 |
| `src/main/resources/templates/auth/reset-password.html` | Đồng bộ khôi phục bước 3 |
| `src/main/resources/templates/auth/fragments.html` | Assets chuẩn, scene auth, SVG mũi tên và dữ liệu hiển thị tiến trình |
| `src/main/resources/static/css/auth.css` | Thay các override cũ bằng CSS auth dùng token và catalog |
| `src/main/resources/static/js/auth.js` | Cập nhật nhãn submit mà không xóa icon |
| `src/main/resources/static/images/auth-register-scene.png` | Trích nguyên ảnh PNG từ prototype, 900 × 339, 356486 byte |

Ảnh có SHA-256 `2F6E37EB0ED263B8C32E4850117A239225DF1340840A8DC55795F5CD3D30F309`; không chỉnh sửa nội dung ảnh. Ảnh cũ được giữ nguyên.

## Kiểm chứng

Lệnh kiểm thử cuối:

```powershell
mvn.cmd '-Dmaven.repo.local=C:\Users\Administrator\.m2\repository' '-DforkCount=0' '-Djacoco.skip=true' '-Dauth.preview=true' '-Dtest=AuthBotanicalWebTests' test
```

- **BUILD SUCCESS; 6 test passed; 0 failure/error/skipped.** MockMvc render Thymeleaf và chạy Spring Security thật, mock repository/service gửi email/đăng ký/khôi phục. Có kiểm tra CSRF, POST đăng ký/OTP/resend, thông báo lỗi tiếng Việt và đăng nhập Candidate/HR.
- Đối chiếu với HEAD: **50 phần tử form/input/button/label giữ nguyên mọi thuộc tính ngoài class**; giữ nguyên các điều kiện/vòng lặp backend trong sáu template, các tên tham số, giới hạn ký tự, endpoint và method.
- Chrome headless mở HTML do Thymeleaf render, phục vụ qua preview chỉ đọc trên loopback; mọi POST tới preview bị chặn.
- **36 tổ hợp**: sáu màn × 1440×1000, 1366×768, 1920×1080, 768×1024, 390×844 và 320×740. Không tràn ngang cấp trang, không có input vượt card hoặc scrollbar riêng trong card. H1 dùng font token Sans-serif.
- Kiểm tra nút mắt và aria-label/aria-pressed, focus bằng Tab, OTP sáu ô và nhập tự động đủ mã, error summary nhận focus/liên kết về field, trạng thái đang gửi và khôi phục nút/giữ SVG, input OTP khi tắt JS, reduced motion. Không có lỗi JavaScript.
- `node --check auth.js` và `git diff --check` đạt.

Ảnh trước/sau, ảnh từng màn, kết quả browser JSON và log nằm tại thư mục QA cục bộ (không đưa vào Git). Bản HTML render nằm tại `target/auth-botanical-preview/`.

## Giới hạn và bảo toàn

Đây là kiểm chứng UI và hợp đồng auth qua MockMvc, không phải E2E với tài khoản/email/database thật. Chưa gửi OTP thật, tạo tài khoản thật hoặc đổi mật khẩu thật trong đợt này. Không thay đổi Controller, Service, DTO, Security, schema, database hoặc dependency.

Các thay đổi đã có trước trong `docs/prototype-reference/` được giữ nguyên. Không commit, push, merge hoặc deploy.

## Sửa regression theo phản hồi về zoom và navbar (20/10/2026)

Phản hồi screenshot phát hiện điểm mà ma trận kiểm tra trước chưa bao phủ: ảnh PNG có chữ được đặt absolute ở đáy toàn trang nên có thể chồng lên CTA/highlights trên màn thấp hoặc sau zoom. Header rút gọn cũng thiếu navbar công khai chuẩn.

- `auth/fragments.html`: đưa minh họa vào cuối `aside.auth-story`, sau cụm highlights; thay header rút gọn bằng lời gọi fragment `fragments/layout/public-topbar :: topbar('', false)`. Không sửa fragment/navbar dùng chung.
- `auth.css`: bỏ định vị absolute/bottom của ảnh, dùng chiều rộng cột và margin-top 32px trong luồng bố cục. Trên mobile ẩn ảnh trang trí. Bỏ các selector của header rút gọn đã hết dùng.
- `auth.js`: cập nhật selector logo sang navbar chuẩn, giữ nhãn trợ năng tiếng Việt.
- Không đổi nội dung form, backend, CSRF, routing, token hoặc stylesheet/layout dùng chung.

Kết quả: BUILD SUCCESS, 6 test auth đạt. Chrome kiểm tra 108 trường hợp reflow của sáu màn theo vùng CSS tương đương zoom 75/90/100/110/125/150/175/200% trên màn 1366×768 và 1920×900, cộng mobile 390/320px; ảnh hiển thị luôn nằm dưới highlights, không tràn ngang và navbar hiện nền trắng/sticky. Có thêm ba kiểm tra phóng đại CSS và kiểm tra các href navbar. Chưa tự động hóa zoom native bằng menu trình duyệt; không đồng nhất phép mô phỏng này với kiểm thử native zoom.

Browser dùng HTML Thymeleaf đã render, preview chặn POST; riêng loader chat được chặn trong bài kiểm tra bố cục vì ngoài phạm vi auth, không sửa chat trong ứng dụng. Ảnh `auth-navbar-zoom-fixed.png`, kết quả `auth-zoom-fix-results.json`, log `auth-zoom-fix-tests.log`.

## Gắn minh họa với form thành một khung (20/10/2026)

Theo phản hồi về cảm giác ảnh rời rạc, sáu template auth đặt `.card` trên `.auth-shell`, bỏ card con ở `main.auth-card`. CSS dùng Grid không có khoảng hở giữa hai cột; cột giới thiệu nền nhẹ, vùng form nền trắng cùng một viền ngoài và bo góc. Hai cột kéo giãn cùng chiều cao, form căn giữa theo nội dung.

Cột trái dùng Flexbox; ảnh vẫn ở luồng bố cục, có khoảng cách tối thiểu 32px sau highlights và tự bám đáy khi cột form cao hơn. Ảnh tràn phần padding nội bộ để chạm mép cột trái và mép form, được cắt theo góc của khung ngoài. Không dùng ảnh absolute, chiều cao cố định hoặc transform để giữ vị trí. Khi về một cột, ảnh trang trí ẩn và hai vùng nội dung vẫn nối liền.

File của lượt này: `static/css/auth.css` và sáu template `auth/{login,register,register-otp,forgot-password,reset-password-otp,reset-password}.html`. Giữ nguyên navbar chuẩn, form bindings, JavaScript và backend.

BUILD SUCCESS, 6 test đạt. Browser đã kiểm tra 108 trường hợp reflow tương đương 75–200%/mobile và ba mức CSS zoom. Có thêm assertion ảnh chạm đáy/cạnh form, hai cột cùng đáy, vùng mobile nối nhau và không có card lồng. Không phát hiện ảnh đè nội dung hoặc overflow cấp trang trong ma trận reflow. Chưa tự động hóa zoom native của trình duyệt; preview Thymeleaf chỉ đọc, không gửi email/đổi mật khẩu thật.

Ảnh: `auth-unified-panel.png`; kết quả browser: `auth-unified-results.json`; log build/test: `auth-unified-tests.log`. Chưa commit/push.

## Quyết định cuối: bỏ khung bao lớn

Theo phản hồi tiếp theo, **không giữ thiết kế một card bao hai cột**. Bỏ `.card` khỏi `.auth-shell`, bỏ viền/ngăn cột/nền riêng ở vùng giới thiệu; chỉ giữ `.card` cho form, với chiều cao tự nhiên. Giữ ảnh ở luồng bố cục an toàn và navbar công khai chuẩn. Đây là trạng thái thay thế mục “Gắn minh họa với form thành một khung” phía trên.

Kiểm chứng cuối: `auth-open-layout-tests.log` BUILD SUCCESS, 6 test đạt; `auth-open-layout-results.json` có 108 trường hợp reflow và ba kiểm tra CSS zoom đạt. Có assertion riêng xác nhận shell không có border/nền Card và chỉ form dùng Card. Ảnh cuối: `auth-open-layout.png`. Chưa kiểm thử zoom native và E2E gửi email/đổi mật khẩu thật. Không commit/push.

## Hòa mép ảnh với nền

Mép chữ nhật của PNG gốc còn lộ trên nền trang. Lượt này chỉ bổ sung CSS mask trên `.auth-botanical-art img` để làm mềm bốn mép, giữ phần chữ và minh họa trung tâm; không crop/chỉnh bitmap, không thêm khung hoặc đổi bố cục. Chrome xác nhận mask hoạt động, 108 trường hợp reflow và ba kiểm tra CSS zoom đạt. Ảnh: `auth-soft-edges.png`; kết quả: `auth-soft-edges-results.json`. Không chạy lại Maven cho thay đổi thuần CSS này; lần build/test gần nhất vẫn là sáu test đạt ở lượt bỏ khung. Chưa kiểm thử native browser zoom.

## Căn lại lề ảnh

Sửa tiếp phần kích thước ảnh còn sót từ bố cục full-bleed: bỏ `width: calc(100% + var(--s16))` và margin ngang âm 32px. Ảnh hiện rộng 100% phần nội dung cột trái, lề ngang bằng 0; giữ mask hòa mép, bố cục mở và navbar. Chỉ đổi một rule trong `auth.css`.

Browser đo 14 trường hợp Login/Register ở 1440, 1920, 1536, 1280, 960, 390 và 320px: mép ảnh khớp phần nội dung cột trong sai số dưới 1px khi hiển thị, không margin âm/tràn ngang. Ảnh `auth-aligned-login.png`, số đo `auth-art-alignment-results.json`. Không chạy lại Maven cho thay đổi thuần CSS; không thay backend và không commit/push.

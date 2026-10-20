# Auth — áp dụng HTML mẫu và kiểm tra native browser zoom (21/10/2026)

## Phạm vi và nguồn tham khảo

Nguồn trực tiếp: `D:/dowload/moc_register_exact_mockup (1).html`. Áp dụng cùng bố cục cho Login, Register, Register OTP, Forgot Password, Reset Password OTP và Reset Password. Bản này thay thế các thử nghiệm khung bao/mask/căn ảnh được ghi trong báo cáo ngày 20/10.

Giữ navbar công khai chuẩn của RMS theo yêu cầu trước đó. Không sửa file layout, navbar, token hoặc component dùng chung. Bảo toàn form Thymeleaf, endpoint, method, validation, CSRF và JavaScript nghiệp vụ đang có; không lấy JavaScript giả lập submit của mockup.

## Thay đổi

- `src/main/resources/static/css/auth.css`: khôi phục kích thước hai cột, khoảng cách, vị trí minh họa dưới trái và các mốc co giãn của HTML mẫu. Bỏ mask và margin âm cũ. `.auth-stage` giới hạn riêng nền minh họa dưới navbar. `.auth-story` dành chỗ bằng chiều cao minh họa cộng token `--s8`, để ảnh không chạm/chồng lên highlights khi màn thấp hoặc zoom. Khi vùng CSS còn 800px trở xuống, chuyển sang một cột; form đăng ký còn một cột field dưới 470px.
- `src/main/resources/templates/auth/fragments.html`: đưa ảnh vào scene như HTML nguồn; dùng ba nội dung highlights từ mẫu. Giữ fragment navbar chuẩn và các fragment thông báo lỗi.
- Sáu template `auth/{login,register,register-otp,forgot-password,reset-password-otp,reset-password}.html`: đặt nội dung và scene trong `.auth-stage` dưới navbar. Register dùng xuống dòng intro theo mẫu; gợi ý username vẫn giữ `aria-describedby` nhưng hiển thị riêng cho trình đọc màn hình.
- `.auth-steps`, `.auth-step-number`: dùng rail cạnh số bước và gạch dưới nhãn theo chính file HTML mới cung cấp.

Để theo UI Rules của RMS, giữ token màu/font, nút và input catalog cao 48px, bo góc card theo token. Giữ giới hạn dữ liệu thật (họ tên 100, username 50) thay vì giới hạn demo trong mockup. Không dựng thêm khung bao hai cột. Ảnh nhúng của mockup trùng SHA-256 với ảnh `auth-register-scene.png` hiện có nên không thêm/sửa bitmap.

## Kiểm chứng

### Native Chrome zoom

Chạy Chrome 155 với profile QA riêng. Dùng `chrome.tabs.setZoom` và xác nhận bằng `chrome.tabs.getZoom`, `window.innerWidth`, `devicePixelRatio`. Đây là page zoom gốc của Chrome, không phải CSS zoom hay chỉ resize viewport.

Ma trận: 6 màn × 3 kích thước cửa sổ (1280, 1440, 1920px; cao 900px) × 7 mức zoom (67%, 80%, 100%, 125%, 150%, 175%, 200%) = **126 trường hợp**, cùng **21 trường hợp HTML tham khảo**.

| Cửa sổ | 100% | 125% | 150% | 175% | 200% |
|---|---:|---:|---:|---:|---:|
| 1280px | 1280 | 1024 | 853 | 731 | 640 |
| 1440px | 1440 | 1152 | 960 | 823 | 720 |
| 1920px | 1920 | 1536 | 1280 | 1097 | 960 |

Các số trong bảng là chiều rộng CSS đo được, làm tròn. Layout chuyển theo chiều rộng này, không khóa theo độ phân giải vật lý. Khi zoom lớn, trang được phép dài hơn và cuộn dọc để đọc hết form.

Bài đo đầu phát hiện ảnh sát highlights trên cửa sổ 1920×900 ở 100–125%. Đã bổ sung khoảng cách `--s8` vào vùng dự phòng của ảnh và chạy lại ma trận cuối.

Kết quả cuối và ảnh native zoom lưu tại `../.codex-tmp/auth-native-zoom-results.json` và `auth-native-*.png`. Chụp bằng bề mặt CDP của Chrome để tránh công cụ chụp full-page cắt ảnh khi native zoom khác 100%. Kiểm tra cuộn tới nút submit ở 200% để bảo đảm phần cuối form vẫn truy cập được.

### Kiểm tra bổ sung

- `AuthBotanicalWebTests`: **BUILD SUCCESS, 6 test đạt**, không lỗi/skipped; log `../.codex-tmp/auth-exact-tests.log`. Lần chạy này sau thay đổi template; thay đổi cuối chỉ thêm khoảng cách CSS cho ảnh.
- Browser trước native zoom: 48 trường hợp / 6 màn ở 1440×1000, 1366×768, 1920×1080, 1280×720, 960×450, 768×600, 390×844, 320×740. Không tràn ngang; navbar còn nguyên. Register ở 1440px khớp vị trí/chiều rộng hai cột và gap của mẫu trong sai số 2px. Kiểm tra nút mắt, focus bàn phím, OTP và error summary đạt, không lỗi JS.
- Đối chiếu HEAD: **50 phần tử form/input/button/label** giữ nguyên các thuộc tính ngoài class; điều kiện/vòng lặp Thymeleaf của sáu màn không đổi.
- Không thêm inline style, raw color, root token override hoặc `!important`. `node --check auth.js` đạt.

## Giới hạn

Browser kiểm tra HTML do Thymeleaf render qua MockMvc, phục vụ từ preview chỉ đọc trên loopback; POST bị chặn, loader chat được bỏ qua trong QA. Không phải E2E với database/email thật. Không gửi OTP, tạo tài khoản hay đổi mật khẩu thật. Không sửa Controller, Service, DTO, Security, database hoặc dependency. Không commit/push.

Kết quả nghiệm thu cuối: toàn bộ 126 trường hợp auth và 21 trường hợp mẫu đạt; không tràn ngang, không control vượt Card, không lỗi JavaScript. Khoảng cách nhỏ nhất giữa highlights và minh họa trên bố cục Desktop đo được là 31,98px. Cuộn tới submit ở zoom 200% đạt cho cả ba kích thước cửa sổ. git diff --check đạt (chỉ có cảnh báo chuyển LF/CRLF ở báo cáo cũ).

## Sửa vị trí minh họa theo screenshot mới (21/10/2026)

Mục này thay thế phần dự phòng ảnh `+ --s8` và kết luận khoảng cách 31,98px ở trên. Kiểm tra trước chỉ bảo đảm không chồng box, chưa bảo đảm vị trí minh họa gần với mẫu.

Nguyên nhân: `.auth-stage` dùng chiều cao tối thiểu 909px; `.auth-story` đồng thời chừa toàn bộ chiều cao minh họa cộng 32px. Ảnh absolute bám đáy stage nên bị đẩy thấp hơn vùng dưới form.

Chỉ sửa `src/main/resources/static/css/auth.css` trong lượt này:

- `.auth-stage`: bỏ min-height cố định và min-height theo viewport ở mobile, để chiều cao đi theo nội dung.
- `.auth-story` và hai breakpoint 1350/1050px: vùng dự phòng bằng chiều cao ảnh trừ `--s8`, thay vì cộng. Mép nền mờ sẵn ở đầu ảnh nối với highlights như mẫu; phần chữ trong ảnh nằm bên dưới highlights. Không sửa bitmap, thêm mask, transform dịch ảnh hoặc khung bao mới.

Ảnh được đưa lên **64px** tại 1280, 1440, 1920px / zoom 100%. Đã xem screenshot Login 1920px và Register 1440px; minh họa hiện nối sát vùng dưới nội dung trái, đồng thời nằm ngang vùng cuối Card form.

Chạy lại native Chrome zoom 67–200%: **126 trường hợp auth + 21 trường hợp tham khảo đạt**, không tràn ngang, control không vượt Card, không lỗi JavaScript. Kiểm tra riêng phần lettering trong ảnh nằm dưới highlights và ảnh không bị tụt thấp ở zoom 100%. Cuộn tới submit ở 200% vẫn đạt. Kết quả `../.codex-tmp/auth-art-height-results.json`, ảnh `auth-art-height-*.png`.

Đối chiếu 50 phần tử form và điều kiện Thymeleaf vẫn đạt; `git diff --check` đạt. Không chạy lại Maven cho lượt thay đổi thuần CSS này; sáu test auth đạt ở lần chạy trước. Không thay navbar, form, JavaScript, backend; không commit/push. Browser vẫn là preview Thymeleaf chỉ đọc như giới hạn đã nêu.

## Sửa điểm neo khi zoom 67–100% (21/10/2026, quyết định hiện tại)

Phần này thay thế cách chừa chiều cao ảnh và dịch ảnh 64px ở trên. Screenshot mới cho thấy hai lỗi mà các kiểm tra overflow trước chưa phản ánh: ảnh bám mép viewport nhưng form bám container căn giữa; form đăng nhập ngắn lại căn đầu giống form đăng ký dài, khiến toàn bộ ảnh nằm dưới form.

Lượt này chỉ thay đổi frontend tại `static/css/auth.css`, `templates/auth/fragments.html` và cập nhật báo cáo này:

- `.auth-stage` làm lưới bố cục chung; giữ kích thước cột và khoảng cách của mẫu. `.auth-shell` là wrapper `display: contents`, không thêm Card hoặc khung.
- `.auth-story` dùng subgrid để phần chữ vẫn ở cột cũ, còn minh họa nối từ mép trái trang đến đúng ranh giới cột form. `.auth-story-content` chứa nội dung giới thiệu hiện có.
- Fragment `artwork` nằm sau nội dung trong `story`, thay vì nằm absolute trong `scene`. Ảnh đi theo luồng nội dung, chiều cao tự nhiên; không dùng width theo viewport, cap 900px hoặc padding giả để cố định điểm rơi của ảnh. Giữ nguyên bitmap.
- `.auth-card` căn giữa theo chiều cao cột trái cho các form ngắn; `.auth-register .auth-card` tiếp tục căn đầu. Mobile vẫn chuyển thành một cột, ảnh trang trí mờ như trước.
- Giữ navbar chuẩn, token, form, validation, OTP, CSRF và toàn bộ backend. Không thêm thư viện hoặc script xử lý layout.

Chrome native zoom được kiểm tra ở 67%, 75%, 80%, 90%, 100%, 125%, 150%, 200% tại 1280/1440/1920px, cộng mobile 390/320px: tổng 156 tổ hợp / 6 màn. Có ảnh khi cuộn tới nhãn field như screenshot phản hồi. Các assertion mới đo: mép trái ảnh bằng mép trang; mép phải ảnh bằng mép trái form; đầu ảnh nối ngay sau highlights; form đăng nhập có phần giao theo chiều dọc với ảnh; form dài hoặc lỗi validation không đẩy ảnh; đổi chiều cao cửa sổ 900 xuống 700px không làm ảnh đổi điểm neo.

Kết quả và ảnh cuối ở `../.codex-tmp/auth-grid-anchor-results.json`, `auth-grid-*.png`. Test Thymeleaf/auth chạy lại: BUILD SUCCESS, 6 test đạt (`auth-grid-anchor-tests.log`); lần đổi căn giữa cuối chỉ là CSS. Đối chiếu 50 phần tử form và điều kiện Thymeleaf giữ nguyên. Browser dùng preview Thymeleaf chỉ đọc; chưa xác minh E2E với email/database thật. Không commit/push hoặc thay đổi staging của người dùng.

Nghiệm thu cuối: 156 tổ hợp đạt; sai lệch mép phải ảnh với mép trái form đo được là 0px trên toàn bộ bố cục hai cột. Mép trái ảnh bằng 0px; đầu ảnh khớp đáy highlights trong sai số 0,000031px. Không lỗi JS hoặc tràn ngang. Đã xem ảnh chụp thực ở 67% và 100% sau thay đổi căn giữa form ngắn. Giới hạn bằng chứng: đây là native Chrome trên preview HTML Thymeleaf, chưa kiểm chứng trên phiên ứng dụng đang chạy của người dùng.

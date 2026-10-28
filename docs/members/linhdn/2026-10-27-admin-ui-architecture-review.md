# Báo cáo thay đổi Admin UI và các điểm chưa tuân thủ kiến trúc

- **Thành viên:** LinhDN.
- **Ngày lập:** 27/10/2026, múi giờ Việt Nam.
- **Mốc source đối chiếu khi lập báo cáo:** `82564dafdff83714a80271f97d8bd75387da568d`.
- **Phạm vi:** giao diện Dashboard và các trang quản lý của System Admin, cùng lần xử lý conflict sau khi pull `main`.
- **Lần xuất tài liệu này:** chỉ thêm báo cáo; không sửa source, cấu hình, dữ liệu, không commit/push.

## 1. Kết luận cần hiểu trước

Giao diện Admin đã được đồng bộ về hình thức, nhưng **chưa thể kết luận tuân thủ hoàn toàn kiến trúc UI**. Việc không sửa trực tiếp một file được bảo vệ không đồng nghĩa với việc không thay đổi trách nhiệm hoặc bố cục của khung dùng chung.

Không có yêu cầu thiết kế nào bắt buộc phải vi phạm kiến trúc. Các lý do dưới đây giải thích **vì sao đã chọn cách xử lý tạm**, không phải căn cứ hợp thức hóa các điểm sai hoặc đổ trách nhiệm cho yêu cầu của người dùng.

Các vấn đề chính đã xác minh:

1. Một phần CSS của **tầng khung** đang được đặt trong `components.css`, kết hợp JavaScript để áp dụng lên sidebar/topbar.
2. Stylesheet trang xuất hiện **3 lần trong HTML đã render** ở các trang được kiểm tra.
3. CSS Admin cũ vẫn có **màu hardcode**, gồm cả giá trị được bọc trong biến CSS cục bộ.
4. Một số nút thao tác vẫn **32×32px**, trong khi biến thể nút nhỏ của catalog là 40px.

Ngoài ra còn nợ cascade do các theme cũ/mới cùng tồn tại. Các declaration `!important` có lý do giải quyết xung đột legacy được xem riêng vì quy tắc hệ thống cho phép ngoại lệ này khi có comment.

## 2. Quy tắc đối chiếu

Các đường dẫn trong báo cáo tính từ thư mục gốc repository `SWP_RMS_Group-2`.

| Tài liệu | Quy định liên quan |
|---|---|
| `DESIGN.md` | Giữ nhận diện Mộc, stack Thymeleaf/CSS/JavaScript hiện có; bảo toàn nghiệp vụ, binding và accessibility. Yêu cầu thiết kế cụ thể có ưu tiên hơn gợi ý mặc định của skill. |
| `docs/architecture/UI/UI_RULES.md`, mục 2A | Layout, sidebar shell, topbar, token và navigation script dùng chung thuộc vùng được bảo vệ. |
| `docs/architecture/UI/UI_RULES.md`, mục 2B | `components.css` được append linh kiện dùng chung mới; không đổi selector cũ và phải cập nhật catalog. |
| `docs/architecture/UI/UI_RULES.md`, mục 7.4–7.6 | Cấm hardcode màu trong CSS trang, cấm tự chế lại linh kiện đã có, không sao chép markup khung dùng chung. |
| `docs/architecture/UI/UI_CSS_GUIDE.md`, mục 1 | Mỗi tầng chỉ làm đúng trách nhiệm: tầng 3 là component; tầng 4 là topbar/sidebar/khung. |
| `docs/architecture/UI/UI_CSS_GUIDE.md`, mục 2–3 | Ưu tiên catalog, dùng class phụ có scope; không redefine class dùng chung trong CSS trang. |
| `docs/architecture/UI/UI_CSS_GUIDE.md`, mục 3 | `!important` chỉ được ngoại lệ khi sửa xung đột CSS cũ, kèm comment giải thích. |
| `docs/architecture/UI/UI_CATALOG.md`, `components.css`, `tokens.css` | Nguồn tra cứu component và kích thước: nút thường 48px, nút nhỏ 40px; token màu, góc và shadow chung. |

Các skill đã sử dụng trong đợt triển khai: UI UX Pro Max, Impeccable, Awesome Design Reference và Taste. Chúng là công cụ hỗ trợ; **không có quyền miễn trừ quy tắc của dự án**.

## 3. Danh sách file đã chỉnh trong chuỗi cập nhật Admin

Danh sách dưới đây gồm thay đổi Dashboard trước đó và lượt đồng bộ các trang Admin. Không có nghĩa tất cả file hiện đang là thay đổi chưa commit; phần lớn đã nằm trong lịch sử commit/merge ở mốc source trên.

### 3.1. CSS và JavaScript — 5 file

| STT | File | Nội dung thay đổi và mục đích |
|---|---|---|
| 1 | `src/main/resources/static/css/components.css` | Append nhóm class `adw-`: scene nền, frame, sidebar/topbar, tiêu đề, surface, bảng, nút đóng, password toggle và validation summary. Không sửa phần component có sẵn trong lượt đồng bộ. **Phần bố trí khung trong block này là điểm chưa đúng tầng được phân tích ở mục 5.1.** |
| 2 | `src/main/resources/static/css/pages/admin-surfaces.css` | Thêm điều chỉnh composition khi có `.adw-page`: ẩn trang trí lá cũ, căn vùng hành động, KPI 36px, khoảng cách field, xử lý panel trong drawer và bố cục nút trên mobile. Giữ các biến thể `ads-page--refined` cũ, dẫn tới CSS chồng lớp. |
| 3 | `src/main/resources/static/css/pages/admin-dashboard.css` | Chỉnh KPI/panel và hierarchy; sau đó tách bản CSS khung Dashboard riêng sang nhóm `adw-`. Giữ CSS đặc thù của thống kê, biểu đồ và bảng dịch vụ. |
| 4 | `src/main/resources/static/js/pages/admin-dashboard.js` | Trình bày trạng thái bằng progress lấy từ số liệu server; giữ biểu đồ vai trò và link lọc. Trong lượt đồng bộ tiếp theo, bỏ phần gắn class khung vì đã tách sang `admin-workspace.js`; phần analytics còn lại được đối chiếu giữ nguyên. |
| 5 | `src/main/resources/static/js/pages/admin-workspace.js` | File mới. Tìm `.adw-page`, thêm class phụ vào frame/sidebar/topbar hiện có, chuyển scene ra `page-wrapper`. Thêm alias trình bày cho password toggle/validation summary đã được `interface.js` xử lý; không đăng ký lại handler nghiệp vụ. |

### 3.2. Template và fragment — 10 file

| STT | File | Nội dung thay đổi và mục đích |
|---|---|---|
| 6 | `src/main/resources/templates/dashboard/admin.html` | Dùng fragment Admin workspace, class `adw-`, tiêu đề lớn và các surface đồng bộ. Bỏ subtitle/CTA ở header theo yêu cầu trước đó; giữ nguồn thống kê, các link lọc và bảng dịch vụ. |
| 7 | `src/main/resources/templates/admin/accounts/list.html` | Bật giao diện Admin mới cho danh sách nội bộ; thêm alias vào header/card/bảng, gọi scene/script; bỏ breadcrumb theo yêu cầu. Giữ form GET, bộ lọc vai trò, phân trang, inspector và row actions. |
| 8 | `src/main/resources/templates/admin/accounts/candidates.html` | Đồng bộ danh sách tài khoản ứng viên theo cùng component; bỏ breadcrumb. Giữ trạng thái, sort/search, query params, inspector và các form xác nhận hiện có. Đây cũng là một file đã resolve conflict. |
| 9 | `src/main/resources/templates/admin/accounts/form.html` | Chuyển từ `interfaceHead` và khung dựng trực tiếp sang `layout/internal`; thêm scene/class/script Admin, bỏ dòng breadcrumb. Giữ `name`, `id`, `th:field`, `th:object`, action, method, điều kiện tạo/sửa, validation và nút Hủy/Lưu. |
| 10 | `src/main/resources/templates/admin/accounts/inspector.html` | Thêm class phụ cho surface, header, tiêu đề và nút đóng. Giữ fragment signature, nội dung tổng quan/chi tiết, ID và handler dock/drawer. |
| 11 | `src/main/resources/templates/admin/departments/list.html` | Đồng bộ nền, header, toolbar, card và table; bỏ breadcrumb. Giữ bộ lọc trạng thái, tìm kiếm, phân trang, link tạo/sửa và thao tác trạng thái. |
| 12 | `src/main/resources/templates/admin/departments/form.html` | Chuyển sang `layout/internal`; thêm giao diện Admin và bỏ breadcrumb. Giữ binding phòng ban/trưởng phòng, điều kiện tạo/sửa, validation, action và CSRF. |
| 13 | `src/main/resources/templates/admin/departments/inspector.html` | Thêm alias trình bày cho panel tương tự tài khoản; không đổi logic chọn dòng hoặc hành động của phòng ban. |
| 14 | `src/main/resources/templates/admin/api-monitoring/index.html` | Đồng bộ header và các card dịch vụ; thêm scene/script/class mới, bỏ breadcrumb. Giữ badge, số đo, notice, endpoint và form kiểm tra hiện có. Đây cũng là một file đã resolve conflict. |
| 15 | `src/main/resources/templates/fragments/ui/admin-workspace.html` | Fragment mới gồm `scene` và `scripts`, tái sử dụng giữa các trang Admin. Không chép lại sidebar, logo, khối identity hay form logout. |

### 3.3. Asset, test và tài liệu — 4 file

| STT | File | Nội dung thay đổi và mục đích |
|---|---|---|
| 16 | `src/main/resources/static/images/admin-tech-office.png` | Asset người dùng cung cấp được đưa vào dự án để làm nền văn phòng có yếu tố công nghệ. Không phải ảnh mới do AI tạo trong lượt đồng bộ. |
| 17 | `src/main/resources/static/images/admin-tech-plant.png` | Asset cây tách nền người dùng cung cấp, dùng ở khoảng trống sidebar. Giữ kích thước artwork cố định khi sidebar đóng/mở. |
| 18 | `src/test/java/com/group2/rms/dashboard/AdminDashboardViewTests.java` | Điều chỉnh kỳ vọng hiển thị sau yêu cầu bỏ CTA tạo tài khoản/bộ điều khiển thời gian trên Dashboard. Không thay nghiệp vụ authentication hoặc dữ liệu backend. File này đã thay đổi từ lượt Dashboard trước, không được sửa thêm khi đồng bộ các trang hoặc resolve conflict. |
| 19 | `docs/architecture/UI/UI_CATALOG.md` | Bổ sung ghi nhận Dashboard/component `adw-`, phạm vi áp dụng và việc bỏ breadcrumb. Khi resolve conflict, giữ cả cập nhật menu Candidate từ `main`. Việc ghi nhận vào catalog **không tự hợp thức hóa** các điểm sai phân tầng. |

### 3.4. Những file có vấn đề được đọc nhưng không sửa trong lượt đồng bộ

- `static/css/pages/internal-accounts.css`: còn màu hex cục bộ và kích thước icon button 32px.
- `static/css/pages/account-inspector.css`: còn kích thước nút đóng 32px.
- `templates/layout/base.html`, `internal.html`: liên quan chuỗi nạp stylesheet cần điều tra.
- `static/css/workspace.css`, `topbar.css`, `tokens.css`, `static/js/interface.js` và các fragment shell/brand: không sửa chủ động để thực hiện lượt đồng bộ Admin.

Các đường dẫn rút gọn ở mục này đều nằm dưới `src/main/resources/`.

## 4. Phân biệt thay đổi Admin với thay đổi từ merge `main`

Trong lần pull `main`, repository có nhiều thay đổi được tự merge liên quan Candidate portal, Application Pipeline, Security, `topbar.css`, `workspace.css` và test của các module đó. **Không quy các thay đổi này thành sửa đổi chủ động của lượt Admin.**

Ví dụ, bản tự merge của `workspace.css` tăng scope selector `.workspace-identity` thành `.workspace-header .workspace-identity`; phần `topbar.css` bổ sung navigation Candidate. Chúng được giữ nguyên trong lần resolve, không dùng việc sửa Admin làm lý do chỉnh thêm.

Bốn conflict đã xử lý:

| File | Cách giải quyết |
|---|---|
| `docs/architecture/UI/UI_CATALOG.md` | Giữ phần Admin mới của HEAD, đồng thời giữ đầy đủ tài liệu topbar/menu Candidate từ `main`; phần append giống nhau không nhân đôi. |
| `src/main/resources/templates/admin/accounts/candidates.html` | Giữ header Admin mới và việc bỏ breadcrumb. Class `ads-page--refined` từ `main` đã có trong HEAD, không bị mất. |
| `src/main/resources/templates/admin/api-monitoring/index.html` | Giữ header Admin mới, scene/component và các form dịch vụ; không khôi phục breadcrumb. Thay đổi class từ `main` đã được bao hàm. |
| `docs/members/linhdn/diagram/sequence/login/login.vpp` | Giữ bản Login tối giản mới hơn của HEAD trong lần resolve. Lưu nguyên bản incoming để đối chiếu; không ghép SQLite của Visual Paradigm bằng tay. **Đây là giải quyết conflict tài liệu UML, không phải thay đổi UI Admin.** |

Tại thời điểm resolve, 876 file ngoài conflict được đối chiếu cả nội dung và index, không bị thay đổi bởi thao tác xử lý conflict. Chỉ chạy `git add` cho bốn đường dẫn conflict; không tự commit/push. Các commit merge xuất hiện sau đó là trạng thái lịch sử repository khi lập báo cáo, không phải commit do lần xuất tài liệu này thực hiện.

## 5. Các vấn đề đang vi phạm hoặc chưa đáp ứng kiến trúc

### 5.1. Trách nhiệm tầng khung bị đặt trong tầng component

**Phân loại:** chưa đúng phân tầng kiến trúc; cần ưu tiên xử lý trước khi nhân rộng sang các role khác.

**Bằng chứng:**

- `components.css`, block `Admin workspace opt-in component`, từ khoảng dòng 1220: có margin của main, vị trí/kích thước sidebar, sticky menu, padding topbar, footer và backdrop.
- `.adw-sidebar` chuyển `position: fixed` của khung cũ thành `absolute`; `.adw-main` thay lề trái; `.adw-topbar` thay cách trình bày header.
- `admin-workspace.js`, dòng 8–22: thêm modifier vào DOM khung và chuyển scene từ nội dung trang ra `page-wrapper`.
- `UI_CSS_GUIDE.md`, mục 1: các trách nhiệm sidebar/topbar/khung thuộc tầng 4; `components.css` thuộc tầng 3.

**Vì sao đã chọn cách này:** cần đồng bộ khung giữa nhiều trang Admin, đồng thời tránh sửa trực tiếp các file shell được bảo vệ và tránh ảnh hưởng role khác. Vì vậy đã append class có scope và dùng script riêng của Admin để áp dụng.

**Vì sao lý do đó chưa đủ:** cách này đáp ứng được điều kiện “không sửa nội dung file protected”, nhưng vẫn thay bố cục của chính khung đó. Nó tạo thêm một nơi điều khiển shell bên ngoài tầng khung. Append vào file được phép không có nghĩa mọi loại CSS đều phù hợp để đặt ở đó.

**Có bắt buộc phải vi phạm không?** Không. Đáng lẽ cần thống nhất một biến thể Admin chính thức ở tầng khung với người phụ trách, hoặc giới hạn thay đổi ở phần nội dung nếu chưa được phép mở rộng phạm vi khung.

**Hướng xử lý:** xác lập biến thể khung Admin có scope rõ ràng ở đúng tầng, giữ menu/role/identity chung; giảm việc dùng JavaScript để thiết lập hình học của layout. Đây cần là thay đổi được duyệt riêng, không sửa lén vào file dùng chung.

### 5.2. Stylesheet trang được chèn lặp ba lần

**Phân loại:** lỗi tổ chức nạp CSS đã xác minh bằng DOM render.

**Bằng chứng:**

- Candidate Accounts: 7 stylesheet trang được tìm thấy với `count = 3` cho mỗi URL.
- API Monitoring: `internal-accounts.css`, `api-monitoring.css`, `admin-surfaces.css` đều có `count = 3`.
- Hiện tượng cũng được quan sát ở Department và form tài khoản.
- Các vị trí cần kiểm tra: slot `styles` trong `layout/base.html`, `layout/internal.html` và template con.

Đây là xác nhận **thẻ `<link>` bị lặp**, không phải bằng chứng browser tải mạng cùng một file ba lần. Cache hoặc cơ chế gom request có thể tránh tải lặp nhưng không làm markup và cascade trở nên đúng.

**Vì sao còn tồn tại:** các trang danh sách vốn đã dùng chuỗi layout nhiều tầng; hai form được chuyển sang layout chuẩn rồi cũng nhận hiện tượng này. Đợt đồng bộ giữ chuỗi include hiện hữu thay vì sửa cơ chế head/styles chung để tránh mở rộng ảnh hưởng.

**Có bắt buộc phải giữ lỗi không?** Không. Giữ scope hẹp chỉ giải thích vì sao chưa sửa, không phải lý do để coi kết quả đạt. Nguyên nhân chính xác của từng bước head merge chưa được cô lập bằng một ca render tối thiểu.

**Hướng xử lý:** kiểm tra cơ chế ghép `<head>` của Thymeleaf Layout Dialect và quy định một điểm nạp CSS trang; kiểm thử đại diện các role trước khi thay layout chung. Không xóa ngẫu nhiên các link ở từng trang để che triệu chứng.

### 5.3. Màu hardcode trong CSS Admin cũ vẫn được mang sang

**Phân loại:** vi phạm quy tắc token màu còn tồn tại từ source cũ.

**Bằng chứng:**

- `pages/internal-accounts.css`, dòng 6–17: 12 giá trị hex cho nhóm nút sửa/nguy hiểm, ví dụ `--iac-edit-color: #2563eb` và `--iac-danger-color: #dc2626`.
- `pages/admin-surfaces.css`, khoảng dòng 118 và 129: shadow viết trực tiếp bằng `rgba(...)`.
- `UI_RULES.md`, mục 7.4, cấm hardcode màu trong CSS trang. Khai báo biến `--iac-*` chứa hex ngay trong CSS trang không biến nó thành token trung tâm hợp lệ.

Một số shadow cũ đã bị class `adw-` ghi đè nên không phải mọi declaration sai đều đang quyết định hình ảnh cuối cùng. Tuy nhiên chúng vẫn tồn tại trong source và tiếp tục làm cascade khó đọc.

**Vì sao còn giữ:** ưu tiên giữ palette hành động và các trạng thái hover đã có, tránh thay màu ngoài phần đồng bộ; chưa dọn lớp CSS cũ hoặc bổ sung token qua người phụ trách.

**Có bắt buộc phải hardcode không?** Không. Có thể dùng token semantic sẵn có nếu phù hợp, hoặc đề xuất bổ sung token được duyệt nếu cần giữ chính xác palette hiện tại. Việc giữ hành vi không bắt buộc phải giữ cách khai báo màu sai kiến trúc.

**Hướng xử lý:** thống nhất token cho edit/destructive state và shadow; thay dần declaration hardcode sau khi xác minh nơi sử dụng. Không tự đổi `tokens.css` hoặc làm khác màu đã duyệt chỉ để đạt lint.

### 5.4. Kích thước nút nhỏ chưa đúng biến thể catalog

**Phân loại:** lệch hợp đồng component và UX thao tác.

**Bằng chứng:**

- Đo trên Department ở desktop: nút sửa và vô hiệu hóa là **32×32px**.
- `pages/internal-accounts.css`, selector `.iac-icon-button`: đặt chiều rộng/cao theo `--s8` = 32px.
- `pages/account-inspector.css`, khoảng dòng 54: `.aim-close` cũng dùng 32px.
- `components.css`, biến thể `.btn--sm`: min-height theo `--btn-sm-h`, giá trị mặc định 40px.

**Vì sao chưa sửa:** các kích thước này có từ CSS cũ. Lượt đồng bộ giữ mật độ hàng/cột và class nghiệp vụ hiện hữu, chủ yếu đổi surface và hierarchy; chưa chuẩn hóa biến thể nút.

**Có bắt buộc phải để 32px không?** Không. Bảng nhiều cột là ràng buộc bố cục, không phải miễn trừ cho hợp đồng nút. Đây là phần còn thiếu của lượt đồng bộ, không phải kích thước mới do `adw-` tạo ra.

**Hướng xử lý:** đưa nút thao tác/nút đóng về biến thể nhỏ thống nhất, điều chỉnh cột thao tác nếu cần và kiểm tra trên mobile. Không suy ra toàn bộ WCAG đã đạt hoặc trượt chỉ từ con số kích thước này.

## 6. Những cách xử lý tạm cần phân biệt với vi phạm rõ ràng

### 6.1. Nhiều lớp theme cùng tồn tại và selector ngày càng mạnh

Một trang đang mang đồng thời các class nghiệp vụ `iac-`/`cac-`/`apm-`, biến thể `ads-page--refined` và nhóm mới `adw-`.

Class nghiệp vụ không tự thân là lỗi. Vấn đề là cùng một thuộc tính hình thức đang có nhiều nguồn quyết định: theme refined đặt tiêu đề Lora, header đậm và shadow riêng; nhóm adw lại đặt sans, header sáng và shadow token. Selector mới có tới bốn class để thắng quy tắc cũ.

**Lý do đã chọn:** giữ toàn bộ stylesheet/class cũ để giảm nguy cơ làm hỏng filter, inspector, dialog và các trạng thái chưa kiểm tra hết; thêm một lớp opt-in để chỉ thay Admin.

**Đánh giá:** đây là nợ cascade. Scope đã được giới hạn, nhưng cách mở rộng chưa gọn. Nên thống nhất một biến thể thay vì liên tục tăng specificity. Chỉ xóa declaration cũ sau khi kiểm kê nơi sử dụng; không xóa cả stylesheet đang phục vụ chức năng.

### 6.2. `!important` để thắng CSS legacy

Các declaration của nhóm adw gồm `position: absolute !important` trên desktop, `position: static !important` trên mobile và `display: none !important` cho sidebar có `[hidden]`.

**Lý do kỹ thuật:** `workspace.css` đã dùng `position: fixed !important` và `display: flex !important`. Selector thường, dù nhiều class hơn, không thể thắng một declaration `!important` tương ứng. Nếu giữ nguyên các declaration legacy và vẫn đổi cách đặt rail/ẩn menu, cần một cơ chế override tương ứng.

**Đánh giá:** đây là **ngoại lệ được `UI_CSS_GUIDE.md` cho phép khi có comment giải thích**, không phải vi phạm chỉ vì có từ khóa `!important`. Các rule mới đã có comment. Tuy nhiên sự phụ thuộc vẫn là nợ kỹ thuật cần được xử lý khi chuẩn hóa tầng khung.

### 6.3. Giao diện khung phụ thuộc vào script trình bày

`admin-workspace.js` không đổi handler nghiệp vụ nhưng quyết định lúc nào frame nhận class, scene được chuyển ra ngoài main, và password/validation có alias mới.

**Lý do đã chọn:** fragment của trang con chỉ nằm trong slot nội dung; script giúp áp dụng giao diện lên các phần tử cha mà không sửa template shell.

**Đánh giá:** không có quy tắc cấm mọi thao tác thêm class bằng JavaScript. Điểm chưa tốt là trách nhiệm thiết lập khung bị phân tán giữa CSS dùng chung, template con và script. Khả năng nháy/nhảy giao diện khi script chậm chưa được đo, nên chưa kết luận có lỗi runtime đó.

## 7. Ngoại lệ đã được yêu cầu và quan sát chưa đủ bằng chứng

### 7.1. Không coi các yêu cầu đã duyệt là vi phạm mặc định

- Nền văn phòng công nghệ và cây ở sidebar là asset/phong cách người dùng đã chọn.
- Tiêu đề sans lớn theo Dashboard, bảng 14px và KPI 36px là định hướng đã duyệt.
- Bỏ breadcrumb và chỉ giữ header lớn là yêu cầu trực tiếp.
- Giới hạn theme cho Admin, giữ Homepage/Auth/Candidate portal riêng là đúng phạm vi.

Các lựa chọn này có thể khác house rules mặc định của Taste hoặc Impeccable. Skill phải tuân theo brief và quy tắc Mộc; không được lấy gợi ý generic để tự ý bỏ hình, đổi font hoặc thay nhận diện.

### 7.2. Nợ hiệu năng và tương phản cần đo tiếp

- Hai PNG trang trí có kích thước lần lượt **2.307.809** và **1.671.347 byte**, tổng **3.979.156 byte**, khoảng 3,98 MB. Đây là dung lượng file, không phải kết luận mỗi lần chuyển trang đều tải lại toàn bộ.
- Có `blur()` trên lớp ảnh và `backdrop-filter` ở chrome. Chưa đo FPS hoặc Core Web Vitals, nên không khẳng định chúng đang gây giật.
- Chữ phụ của form còn nằm trực tiếp trên ảnh. Chưa đo tương phản theo từng crop/viewport; không thể kết luận đạt WCAG chỉ vì màu chữ dùng token.

Đây là các mục cần kiểm chứng, chưa được trình bày như vi phạm đã xác nhận.

## 8. Những phần đã giữ đúng và bằng chứng kiểm tra

### 8.1. Phạm vi nghiệp vụ

- Giữ routes, template filenames, tên biến backend và hợp đồng form/control.
- Hai form Admin dùng `layout/internal`; không sao chép lại logo, sidebar, identity hoặc logout.
- Giữ CSRF, validation, handler mật khẩu, bộ lọc, phân trang, inspector và menu/pin.
- Component mới dùng token Mộc; `components.css` được append, không sửa phần selector cũ trong lượt đồng bộ.
- Không thêm số liệu giả, chức năng audit mới hoặc probe tự chạy để trang trông đầy hơn.

### 8.2. Bằng chứng đã có từ các lượt triển khai/resolve trước

| Loại kiểm tra | Kết quả và giới hạn |
|---|---|
| Source contract | Đối chiếu 9 template: hợp đồng interactive giữ nguyên, trừ link breadcrumb được yêu cầu bỏ. |
| Bảo vệ source khi đồng bộ | 367 file được đối chiếu không đổi; prefix component cũ giữ nguyên. Đây là kết quả ở lượt đồng bộ, trước các cập nhật được pull sau đó. |
| Bảo toàn khi resolve | 876 file ngoài conflict giữ nguyên nội dung và index so với lúc bắt đầu resolve. |
| Test | `AdminDashboardViewTests` và `AdminDashboardAnalyticsTests`: tổng 10 test qua, không failure/error/skip. Không phải regression toàn bộ hệ thống. |
| Browser | Có kiểm tra 7 trang tại 1440×900 và 390×844; bổ sung một số trang tại 1366×768, trạng thái ghim 1920×1080 và reflow ở 1152×720, 960×600, 834×1112. |
| Hành vi UI | Bộ lọc mở trong viewport; chọn dòng mở inspector; Escape đóng drawer và trả focus; menu mobile mở đủ mục; nút hiện/ẩn mật khẩu hoạt động. |
| Kiểm tra sau resolve | Hai trang conflict render còn theme Admin, không breadcrumb, không tràn ngang, form POST có CSRF. |
| Chưa xác minh đầy đủ | Native browser zoom, toàn bộ WCAG, FPS/Core Web Vitals, các luồng Candidate được main cập nhật, khả năng nhận email hoặc sức khỏe tích hợp. |

Không chạy SQL/SMTP probe, không gửi email thử hay submit dữ liệu nghiệp vụ trong các lượt kiểm tra UI này. Lần lập báo cáo Markdown không chạy lại build/test hoặc browser.

Kết quả trước đó “10 test qua”, “không tràn ngang” hoặc “lint hai file mới không có cảnh báo” **không đủ để khẳng định toàn bộ kiến trúc UI đã đúng**. Kết luận trước đây về việc tuân thủ hoàn toàn cần được hiểu lại theo các giới hạn ở báo cáo này.

## 9. Thứ tự khắc phục đề xuất

| Thứ tự | Việc cần làm | Điều kiện để tránh ảnh hưởng người khác |
|---|---|---|
| 1 | Thống nhất biến thể khung Admin ở đúng tầng và nguồn nạp style duy nhất. | Người phụ trách khung duyệt phạm vi; không tiếp tục dùng component/script để né ranh giới ownership. |
| 2 | Cô lập và sửa nguyên nhân stylesheet bị chèn lặp. | Có ca render đại diện Admin, HR/HM/Director/Interviewer, Candidate và trang công khai trước/sau thay đổi layout chung. |
| 3 | Thu gọn các declaration theme cũ đang bị ghi đè, giảm specificity. | Kiểm kê nơi dùng và giữ style phục vụ filter/dialog/inspector; không xóa chức năng hoặc stylesheet hàng loạt. |
| 4 | Chuẩn hóa token màu/shadow và kích thước nút nhỏ. | Dùng token/component hiện có hoặc đề xuất token mới được duyệt; kiểm tra cột thao tác, hover, focus và mobile. |
| 5 | Tối ưu asset và đo tương phản/hiệu năng. | Giữ hình ảnh đã duyệt; kiểm tra bằng render thực tế thay vì suy đoán từ source. |

Các bước này là đề xuất, **chưa được triển khai trong lần lập báo cáo**. Tài liệu không sửa `DESIGN.md`, không thay quyền sở hữu shared UI và không cấp phép bỏ qua quy tắc hiện hành.

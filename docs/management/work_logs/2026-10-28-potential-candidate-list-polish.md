# Potential Candidate — phân cấp danh sách theo Quản lý tài khoản Admin

Ngày: 28/10/2026. Thực hiện: LinhDN.

## Yêu cầu và phạm vi

Làm rõ danh sách và bảng bằng layout đang dùng ở Quản lý tài khoản Admin;
giảm khoảng trống toolbar, không để CV nguồn thành cột riêng, xử lý tên tin dài,
đồng bộ chi tiết/đề xuất/gợi ý HR. Chỉ thay phần trình bày của Talent Pool.
HEAD khi bắt đầu: `ddd73d63a061ec771311a414d720ba56f85f1b14`.

## File change trong lần này

Đường dẫn tính từ gốc repository; tổng cộng 10 file: 7 sửa, 3 tạo mới.

| File | Trạng thái | Nội dung |
| --- | --- | --- |
| `src/main/resources/templates/talent-pool/list.html` | Sửa | Gom title/search/filter/bảng/pager vào result card; CV nằm dưới tên ứng viên; avatar, count thực từ model, caption tên tin đầy đủ. |
| `src/main/resources/templates/talent-pool/detail.html` | Sửa | Header hồ sơ gọn; quay lại cùng hàng; một workspace chia CV và ghi chú, CV có nút mở gọn. |
| `src/main/resources/templates/talent-pool/suggestions.html` | Sửa | Header/count, bảng và tên ứng viên rõ hơn; job title xuống dòng có giới hạn độ dài dòng. |
| `src/main/resources/templates/talent-pool/recommendations.html` | Sửa | Danh sách gợi ý chuyển từ card lặp thành bảng; giữ form/quyền và các liên kết; màn rỗng gọn. |
| `src/main/resources/static/css/pages/talent-pool-workspace.css` | Sửa | Composition `tpw-`, header bảng, tên/avatars, spacing, wrapper cuộn, responsive và CV disclosure. |
| `src/main/resources/static/js/pages/talent-pool-workspace.js` | Mới | Đồng bộ caption/ngày/tooltip theo option CV đang chọn, chỉ dùng `textContent`. |
| `src/test/js/talent-pool-workspace.test.cjs` | Mới | 4 kiểm tra metadata CV, đổi lựa chọn, text an toàn và trường hợp thiếu dữ liệu. |
| `docs/architecture/UI/UI_CATALOG.md` | Sửa, append | Ghi composition, selector và ranh giới của đợt polish. |
| `docs/management/WORK_LOG.md` | Sửa | Thêm dòng tham chiếu đợt làm việc này. |
| `docs/management/work_logs/2026-10-28-potential-candidate-list-polish.md` | Mới | Tài liệu này. |

File `docs/members/linhdn/2026-10-28-potential-candidate-ui-change-summary.md`
đã có trước đợt này, vẫn untracked và không sửa. Không tính vào 10 file trên.

Hai file ignored `docs/members/linhdn/tmp/talent-pool/list.html` và `detail.html`
được test có sẵn tự ghi lại khi chạy Maven (`ApplicationPipelineWebTests`,
hai lệnh `Files.writeString` tại vùng capture HTML). Đây là output sinh tự động,
không sửa thủ công, không stage hay đưa vào danh sách source bàn giao.

## Quyết định UI

- Tái sử dụng bề mặt trắng và cấp tiêu đề `adw-` của Admin, giữ palette/font/nền
  Mộc đã duyệt; không thêm panel tổng quan làm thu hẹp bảng.
- Search và bộ lọc sát nhau trong result card, còn vùng bảng là nội dung chính.
- Dữ liệu bảng 14px; tên ứng viên 15px semibold và avatar chữ cái để nhận diện
  đầu dòng. Header xanh nhạt, đường phân cách vừa đủ và phản hồi trên hàng.
- CV là thông tin phụ có thể mở dưới tên; khi nhiều CV trong chế độ chọn,
  disclosure mở sẵn. Native select giữ nguyên dữ liệu, title hiển thị đầy đủ ở
  caption phía trên, không chèn thêm cột rộng. Một CV vẫn mở xem được.
- Bảng đề xuất có min-width 1120px để tên ứng viên không bị ép thành từng
  chữ ở tablet; toàn bộ bảng cuộn trong wrapper, không làm tràn trang.
- Job title không bị cắt ở tiêu đề/caption. Native select có thể hiển thị rút
  gọn theo độ rộng control; caption và tooltip giữ toàn bộ nội dung.
- CV/ghi chú chi tiết dùng cùng bề mặt để giảm cảm giác hai card rời. Chiều cao
  theo nội dung; không thêm dữ liệu hoặc khoảng trắng giả khi ít CV/ghi chú.
- Recommendations giữ rõ hàng ứng viên, HR gửi, thời điểm, trạng thái và hành
  động. Không đổi nghiệp vụ HR recommend và HM suggest.

## Skills và kiến trúc

Đọc UI_RULES/UI_CSS_GUIDE/UI_CATALOG và cấu trúc template/CSS/JS trước khi sửa.
Áp dụng UI UX Pro Max cho density/reflow; Impeccable cho Operate/polish và
disclosure; Taste tiếp tục direction `accent-ladder`, seed `378800654`, giữ
palette/type; Awesome DESIGN.md/SAP chỉ tham khảo hierarchy của bảng.

- Không sửa layout/internal, workspace.css, topbar, tokens, interface.js,
  components.css, sidebar hoặc ảnh nền. Không chép lại shell/pagination.
- CSS page-owned, token hiện có; không raw color, `!important`, inline style,
  hiệu ứng nâng card, animation mới hoặc CSS tác động module khác.
- Giữ routes, query names, IDs/names, CSRF, Thymeleaf và các `data-pool-*` cũ.
  Script nghiệp vụ hiện có giữ nguyên. JS mới không submit, fetch, lưu session
  hoặc thay option/value; chỉ hiển thị metadata.
- Nợ phân tầng shell/style của component `adw-` từ lần trước vẫn được kế thừa;
  không tuyên bố toàn bộ hệ thống đã sạch vi phạm kiến trúc. Đợt này không thêm
  override shared shell để chữa nợ đó.

## Kiểm chứng

| Hạng mục | Kết quả |
| --- | --- |
| JavaScript Talent Pool | 39/39 pass: selection, UI, recommendations, potential action và 4 test presentation mới. |
| Maven | BUILD SUCCESS, 75/75 pass: ApplicationPipelineWebTests 60; SidebarRoleWebTests 4; AdminDashboardAnalyticsTests 7; AdminDashboardViewTests 3; capture tạm 1. |
| Hợp đồng template | So sánh snapshot trước/sau trên 5 template, không mất binding nghiệp vụ cũ; potential-candidate-action.html không sửa. |
| Chrome trước/sau | Cùng dữ liệu kiểm thử: 6 ứng viên, có người không CV, có 2 CV và job title dài; các biến thể empty/HR/HM/selection. Ảnh trước ở 1440×900 và 1920×1080. |
| Chrome bản cuối | 9 biến thể × 5 viewport: 1920×1080, 1440×900, 1366×768, 768×1024, 390×844; thêm 4 reflow checks. 49 lần render, không document horizontal overflow hoặc JS exception. |
| Tương tác Chrome | Filter mở/đóng/Escape/focus; đổi CV cập nhật caption/ngày/preview/checkbox; no-CV disabled; chọn/bỏ chọn; confirmation; pin sidebar và menu mobile; bảng cuộn tới thao tác; filter mobile trong viewport. |
| Reflow tương đương zoom | Trên list HM selection: 1800×1125 (~80%), 1152×720 (~125%), 960×600 (~150%) so với 1440×900; thêm 1920×1080. Đây là viewport reflow, không phải thao tác zoom native của trình duyệt. |
| Tương phản badge | Đo từ màu computed trong Chrome: warning 5.02:1; neutral 5.70:1; success 6.77:1, đều vượt AA 4.5:1 cho chữ thường. Chỉ là kiểm tra các badge được render, không thay thế audit toàn bộ UI. |
| Taste lint | 0 FAIL / 0 WARN trên CSS/JS feature sau sửa. |
| Taste inspect --app | Chạy chính thức trên 4 màn ở 1440/768/390. 0 FAIL sau allowance QA riêng `placeholder-data` vì fixture chủ động dùng email example.com, không phải dữ liệu sản phẩm; giữ báo cáo ban đầu. Các WARN còn lại được ghi dưới đây. |

Taste vẫn cảnh báo nền cream kế thừa, h1 36px và Inter được khai báo nhưng
không load. Không đổi shared palette/type chỉ để xóa cảnh báo; font fallback
là vấn đề của hệ thống hiện có cần xử lý trong phạm vi riêng. Hai bảng có STT
còn cảnh báo numeric-align/tabular: đây là cột chỉ mục, giữ alignment như mẫu
Admin, không phải cột số đo để so sánh. Không tuyên bố inspector hoàn toàn
không có cảnh báo. Không sửa direction dự án hoặc thêm allowance UI khác.

HTML được render bằng Thymeleaf/MockMvc với service mocks, sau đó mở trong Chrome
cô lập với CSS/JS/font/ảnh thật của project. Fixtures nằm ngoài source; preview
chặn mọi POST. Kiểm tra này chứng minh layout và hành vi client trong fixture,
không phải kiểm chứng ghi dữ liệu SQL, gửi gợi ý/ghi chú hay đọc PDF thật trên
phiên HR/HM. Không tạo dữ liệu giả trong ứng dụng hoặc sửa DB.

Evidence tại máy thực hiện:
thư mục QA cục bộ `potential-polish-20261028/`.
Các mục chính: `before/`, `final/`, `final-render.json`, `interactions.json`,
`contracts.json`, `source-before.json`, `taste-summary.json`,
`taste-fixture-summary.json`, `final-verification.json`.
Capture Java tạm ở thư mục QA; class sinh trong target được dọn đúng prefix sau
khi kiểm thử. Không thêm Java capture class vào source dự án.

Không commit/push/stage trong đợt này.

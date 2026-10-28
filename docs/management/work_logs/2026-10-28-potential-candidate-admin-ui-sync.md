# Đồng bộ Potential Candidate theo layout Admin

- **Ngày:** 28/10/2026.
- **Người thực hiện:** LinhDN / Codex, theo yêu cầu người dùng.
- **Yêu cầu đã làm rõ:** dùng cùng layout và giao diện Admin cho các trang liên quan Potential Candidate; báo rõ file change và cập nhật work log.
- **Phạm vi:** frontend Thymeleaf/CSS, một assertion kiểm thử và tài liệu. Không đổi nghiệp vụ.

## 1. File change

| Loại | File | Nội dung |
| --- | --- | --- |
| Sửa | `src/main/resources/templates/talent-pool/list.html` | Bật workspace Admin; toolbar lọc/tìm kiếm; card dữ liệu, tiêu đề, inset bảng và phân trang. |
| Sửa | `src/main/resources/templates/talent-pool/detail.html` | Workspace chung; profile/CV/ghi chú/quyết định dùng card và phân cấp tiêu đề tương ứng Admin. |
| Sửa | `src/main/resources/templates/talent-pool/suggestions.html` | Workspace chung; đề xuất HM nằm trong card bảng có header rõ ràng. |
| Sửa | `src/main/resources/templates/talent-pool/recommendations.html` | Workspace chung; search toolbar, card gợi ý HR, badge trạng thái. |
| Tạo | `src/main/resources/static/css/pages/talent-pool-workspace.css` | Chỉ alias `tpw-`: spacing, responsive, trạng thái, điều khiển và bố cục feature. |
| Sửa | `src/test/java/com/group2/rms/candidate/application/pipeline/controller/ApplicationPipelineWebTests.java` | Một assertion kiểm tra text trong H1, tránh phụ thuộc toàn bộ chuỗi class khi thêm alias UI. |
| Thêm mục cuối | `docs/architecture/UI/UI_CATALOG.md` | Ghi cách tái sử dụng `adw-`, `mft-`, alias `tpw-` và phạm vi role. |
| Thêm dòng | `docs/management/WORK_LOG.md` | Ghi công việc hôm nay; bổ sung dòng phân cách để bảng Markdown render đúng. |
| Tạo | `docs/management/work_logs/2026-10-28-potential-candidate-admin-ui-sync.md` | File change, quyết định, bằng chứng và giới hạn kiểm tra. |

Tổng cộng **9 file dự án**. Các script/capture phục vụ QA nằm ngoài repository, không phải source tính năng.

## 2. Cách áp dụng layout

- Dùng nguyên `fragments/ui/admin-workspace :: scene`, `:: scripts` và nhóm class `adw-` hiện có. Vì vậy sidebar, topbar, nền công nghệ, vùng nội dung rộng, title, card và bảng theo cùng hệ bố cục với Admin.
- Giữ `layout/internal` và role hiện tại. Không ép HR/Hiring Manager thành System Admin, không sao chép menu Admin vào Talent Pool.
- Giữ font/token hiện hành; bảng 14px, body theo token 15px, title desktop 36px/mobile 28px, panel title 20px. Nút icon 40px, toolbar search/toggle 44px, các CTA dùng component sẵn có.
- `list` tái sử dụng `mft-` với disclosure gốc của HTML. Giữ bộ lọc Người đánh dấu/Vị trí từng ứng tuyển; từ khóa vẫn nằm ngoài. Không tạo bộ lọc trạng thái không có trong backend.
- Card, table và panel dùng `adw-surface`, `adw-panel-title`, `adw-data-table`; CSS mới không redefine button/card/table/shell dùng chung, không thêm raw color, inline style, `!important` hoặc animation.
- Bỏ breadcrumb lặp để giữ cách đặt title như Admin. Các đường quay lại danh sách/tin tuyển dụng và menu điều hướng vẫn có sẵn.
- Giữ nguyên phân trang bằng fragment catalog, không copy markup. Chỉ thu gọn khoảng cách quanh pager. Bảng vẫn cuộn ngang trong wrapper trên màn hình nhỏ.
- Màu badge dùng alias feature `tpw-status` để tránh foreground legacy ghi đè; không sửa `.badge` hoặc `workspace.css`.

## 3. Nghiệp vụ và dữ liệu được bảo toàn

- Không sửa Controller, Service, Repository, Security, API, schema, migration, seed hoặc dữ liệu SQL.
- Không đổi route/view name, biểu thức `th:if`, `th:each`, `th:field`, ID/name của form, query params, `data-*` handler cũ hoặc CSRF.
- Giữ chọn nhiều ứng viên, chọn CV nguồn, lưu draft, guard khi rời trang, xem CV, ghi chú, xác nhận gửi và lỗi/thử lại.
- Giữ riêng HM đề xuất và HR gợi ý, quyền chấp nhận/từ chối/xóa gợi ý, liên kết sang hồ sơ ứng tuyển và ngữ cảnh quay lại.
- `potential-candidate-action.html` và các handler nghiệp vụ JS hiện tại không thay đổi; nút lưu tiềm năng nhúng trong màn Phỏng vấn/Hồ sơ không bị đổi giao diện ngoài phạm vi.
- Git ban đầu có `TalentPoolService.java` đã sửa và một file khóa Excel chưa theo dõi. Cả hai là thay đổi có trước; không đưa vào file change của công việc này. SHA-256 của Service trước/sau vẫn là `4ae3ded63ad1ad6c64dd2d97b9cd3b18a797fee23b32b3cd15d136a652eed8ed`.
- Không commit, push hoặc stage file.

## 4. Quy tắc kiến trúc và skill

- Đọc UI_RULES, UI_CSS_GUIDE, UI_CATALOG, CSS/template Admin thực tế và hợp đồng DESIGN hiện có trong `docs/members/linhdn/AI MD/DESIGN.md`.
- UI UX Pro Max: đối chiếu table overflow và reflow của filter; giữ bảng cuộn trong vùng của nó.
- Impeccable: dùng hướng Operate/Polish và craft floor; ưu tiên khả năng quét dữ liệu, trạng thái và bố cục có sẵn, không thêm màn hình hoặc hiệu ứng mới.
- Taste: `Accent ladder`, seed `378800654`, `--keep palette,type`; không redraw. Giữ palette Mộc, shell và component Admin đã chốt thay vì thay bằng shell/cỡ nút mẫu của skill. Tính năng đặc trưng vẫn là CV nguồn và danh sách chọn theo tin tuyển dụng.
- Awesome: tham khảo phân cấp chữ/ô dữ liệu trong bản phân tích SAP ở `.agents/references/awesome-design-md/upstream/design-md/sap/DESIGN.md` (mục Hierarchy và `ex-data-table-cell`). Đây là tham khảo bên thứ ba; không copy màu, font hoặc coi là tài liệu SAP chính thức.
- Không chỉnh các file protected: layout, tokens/fonts, workspace/topbar, sidebar-shell, brand hoặc interface.js. Không chỉnh CSS/JS của Admin, Authentication, Homepage hoặc module khác.
- Tái sử dụng `adw-` có nghĩa vẫn phụ thuộc lớp shell hiện trạng đã được ghi trong báo cáo Admin UI trước đây: trách nhiệm shell nằm trong component/JS opt-in và vấn đề stylesheet lặp chưa được xử lý. **Không tuyên bố lần đồng bộ này đã làm sạch toàn bộ kiến trúc.** Chuyển trách nhiệm đó về tầng 4 cần một thay đổi riêng theo quyền Lead; không tự mở rộng tác vụ hiện tại.

## 5. Kiểm tra đã thực hiện

### Source và kiểm thử tự động

- So sánh binding/ID/name/data-handler của 5 template trong thư mục: không mất binding cũ, ngoại trừ markup breadcrumb lặp được loại bỏ có chủ đích.
- CSS mới: không raw color, không `!important`; class có prefix; tái sử dụng component catalog.
- **74/74 test Java đạt:** ApplicationPipelineWebTests 60, SidebarRoleWebTests 4, AdminDashboardAnalyticsTests 7, AdminDashboardViewTests 3. Không failure/error/skip.
- **35/35 test JavaScript đạt:** selection, UI, recommendations và potential-action. Bao gồm đổi CV, giữ draft, CSRF, HR/HM submit khác nhau và chống gửi lặp.
- Harness tạm bên ngoài source xuất HTML bằng Spring MVC/Thymeleaf thật với service mock: **5/5 test đạt**. Dữ liệu hiển thị trong ảnh là fixture kiểm thử; không đưa dữ liệu giả vào sản phẩm hoặc database.
- Taste lint: CSS mới 0 FAIL/0 WARN; kiểm tra tổng CSS/4 template 0 FAIL/1 WARN về dấu gạch ngang trong title/comment có từ trước. Giữ nguyên nội dung này theo phạm vi.
- `git diff --check` trong phạm vi file thay đổi đạt sau khi bỏ whitespace thừa.

### Render và tương tác trên Chrome

- Trước: ảnh list/detail tại 1440×900 và 1366×768.
- Sau: **11 biến thể × 4 viewport = 44 lượt**, gồm danh sách HR/HM, chế độ chọn, dữ liệu rỗng, đề xuất HM, gợi ý HR và detail có/không ngữ cảnh recommendation.
- Viewport: 1440×900, 1366×768, 768×1024, 390×844; thêm list ở 1920×1080 và reflow 1800×1125, 1152×720, 960×600: tổng **48 lượt**. Không tràn ngang document, không JS exception; bảng cuộn trong wrapper là chủ đích.
- Các kích thước reflow tương ứng khoảng nhìn khi 1440×900 được zoom 80%, 125%, 150%; **không coi đây là bằng chứng đã thao tác zoom gốc của Chrome**.
- Bộ lọc mở trong viewport; Escape đóng và trả focus; query vẫn có `jobPostingId`, `markedBy`, `sourceJobId`, `keyword`.
- Chọn/bỏ chọn cập nhật highlight, count và thanh hành động; dialog xác nhận nằm trong viewport. Preview chặn POST nên không gửi đề xuất thật; khi preflight không có backend, nút xác nhận vẫn khóa an toàn.
- Ghim sidebar đạt 250px; menu mobile có 9 link theo role, đóng bằng Escape và trả focus; document 390px sau khi transition hoàn tất.
- Form POST render có CSRF. Tương phản chữ badge đã đo: warning **5.02:1**, success **6.77:1**, neutral **5.70:1** trên nền badge tương ứng; không suy rộng thành chứng nhận WCAG cho toàn trang.

## 6. Giới hạn và việc còn lại

- `localhost:8081` không chạy khi kiểm tra. Ảnh được chụp trên Chrome từ HTML MockMvc và CSS/JS thật, không phải phiên có SQL thật. Chưa kiểm thử tích hợp SQL, lưu ghi chú/đề xuất thật, gửi email hay CV thật.
- Chưa đo native browser zoom, FPS, Core Web Vitals hoặc tương phản mọi vùng ảnh nền. Không thay ảnh nền để xử lý các rủi ro chưa đo.
- Không mở rộng thay đổi sang layout hay handler của màn Hồ sơ ứng tuyển/Phỏng vấn; các đường chuyển tiếp vẫn giữ nguyên.
- Bằng chứng QA cục bộ ở `C:/Users/Administrator/.codex/visualizations/2026/10/14/01a13a55-2ffe-7341-ac69-6a4f1c685469/potential-ui-20261028/`: `before/`, `final/`, `contracts.json`, `quality.json`, `interactions.json`, `source-verification.json` và `taste/`.

# Dashboard Candidate — giao diện tiếng Việt và quá trình chỉnh sửa

Ngày: 04/10/2026. Phạm vi: Dashboard Candidate tại `/dashboard`.

## Yêu cầu và quyết định

- Người dùng yêu cầu làm frontend Candidate theo danh mục đã cung cấp, thống nhất với Mộc, dùng tiếng Việt và ghi lại luồng/cách test.
- Dùng lại `interfaceHead`, workspace header/sidebar, design tokens, native controls, bảng và badge chung. Không tạo một bộ Global CSS mới.
- Chỉ nối phần dữ liệu đọc đã tồn tại trong `DashboardMetricsRepository`; không thêm Entity/table/column, không sửa DB/schema/seed, không thêm JWT hay thay SecurityConfig.
- Tên vị trí, họ tên, email, địa điểm, quyền lợi được giữ nguyên từ DB và render bằng `th:text`. Các mã trạng thái được đổi **nhãn hiển thị** sang tiếng Việt; không cập nhật giá trị DB.
- Dữ liệu ứng viên chỉ truy vấn bằng UserId của tài khoản đăng nhập, qua `Candidate.account`. Không lấy userId từ request.

## Kiểm tra trước khi sửa

1. Đọc kiến trúc, schema và cả bốn class Dashboard dạng flat đang được sử dụng.
2. Đối chiếu `CandidatePanel` đã có nhưng chưa được Service/template sử dụng.
3. Đối chiếu route thực: Public Jobs/detail đã có; Apply còn báo `ITERATION_2_PENDING`; không có endpoint hoàn chỉnh để rút đơn, phản hồi offer, sửa hồ sơ hay thông báo.
4. Schema Offer dùng `Declined`, trong query Candidate cũ lại có `Rejected`.
5. UI UX Pro Max: tra table/empty state — bảng cuộn cục bộ, trạng thái rỗng có hướng tiếp theo, điều hướng rõ. Impeccable: Operate/polish, giữ hệ thống hiện tại, không redesign.

## Files sửa và lý do

| File | Thay đổi | Lý do |
|---|---|---|
| `src/main/java/com/group2/rms/dashboard/DashboardService.java` | Nối applications/interviews/offers/profile vào CandidatePanel; thêm count thư mời chờ phản hồi; nhãn Candidate tiếng Việt; kiểm tra link họp http(s) | UI phải có dữ liệu thật, scope cá nhân, không đưa URI nguy hiểm lên link |
| `src/main/java/com/group2/rms/dashboard/DashboardMetricsRepository.java` | Thay `Rejected` bằng `Declined` trong allowlist Candidate; đọc thêm probationSalary/workLocation/benefitsPackage của offer chính thức | Khớp CHECK schema, hỗ trợ chi tiết đãi ngộ mà không đọc ghi chú/phê duyệt nội bộ |
| `src/main/java/com/group2/rms/dashboard/DashboardResponse.java` | CandidateProfile chỉ đọc và thêm fields offer công khai | Không chuyển Entity/credentials/internal reviews sang view |
| `src/main/resources/templates/dashboard/index.html` | Tách nhánh Candidate; `lang=vi`, title Việt, nạp JS riêng | Giữ Dashboard các vai trò nội bộ và không render hai giao diện cùng lúc |
| `src/main/resources/templates/fragments/workspace-header.html` | Nhãn Candidate Việt, đánh dấu form logout để xác nhận | Dùng chung header, giữ nguyên POST/CSRF |
| `src/main/resources/templates/fragments/sidebar.html` | Nhánh Candidate điều hướng đến từng section; link `/jobs` | Entry point nhìn thấy được; không dẫn tới route chưa triển khai |
| `src/main/resources/static/css/dashboard.css` | CSS scoped Candidate dùng tokens chung; bảng/sections/dialog responsive | Thống nhất màu, font, khoảng cách; không gây overflow toàn trang |
| `docs/members/linhdn/flows/README.md`, `tests/README.md` | Thêm link tài liệu đợt này | Các lần sau có thể nối tiếp |

## Files tạo

- `src/main/resources/templates/dashboard/candidate.html`: tổng quan, đơn ứng tuyển, thống kê trạng thái, lịch phỏng vấn, thư mời/chi tiết, thông báo chưa khả dụng, hồ sơ chỉ xem, dialog logout.
- `src/main/resources/static/js/candidate-dashboard.js`: filter keyword/status không dấu, live count, clear tại chỗ, xác nhận trước POST logout.
- `src/test/java/com/group2/rms/service/CandidateDashboardServiceTests.java`: scope, nhãn, inactive, link, query offer công khai.
- `src/test/java/com/group2/rms/CandidateDashboardWebTests.java`: render Thymeleaf thật với MockMvc, security/session/CSRF, escape, empty và regression HR; xuất fixture vào target để QA.
- `src/test/java/com/group2/rms/service/CandidateDashboardReadOnlyTests.java`: Spring Context/JPA validate và toàn bộ query Candidate trên DB thật bằng SELECT, không tạo dữ liệu test.
- `docs/members/linhdn/scripts/candidate-preview-server.py`, `check-candidate-dashboard-ui.mjs`: preview cục bộ và kiểm tra Chrome trên fixture; không emulation endpoint ứng dụng.
- `docs/members/linhdn/tests/assets/2026-10-04-candidate-dashboard/`: screenshot populated/empty ở bốn kích thước và JSON bằng chứng của hai lượt.
- Tài liệu này, [luồng kỹ thuật](../flows/candidate-dashboard-ui-flow.md), [kịch bản test](../tests/2026-10-04-candidate-dashboard-ui.md).

Files xóa/rename trong đợt này: **không có**. Các file Dashboard nested đã bị xóa trước đợt này là chỉnh sửa có sẵn của người dùng, không phải thao tác của đợt UI.

## Những phần không tự quyết định

| Yêu cầu | Hiện trạng / cách hiển thị |
|---|---|
| Stage + Status Active/Inactive/Closed | Schema chỉ có ApplicationStatus, chưa có mapping nghiệp vụ được duyệt. UI hiển thị một cột **Trạng thái tuyển dụng**, không tự suy ra hai trạng thái |
| Withdraw → Rejected | Chưa có endpoint/rule về thời điểm rút đơn và ảnh hưởng interview/offer. Nút bị disabled, có giải thích |
| Offer Date | Schema có CreatedAt/UpdatedAt nhưng không có thời điểm gửi chính thức. Không gán nhầm ngày tạo thành ngày gửi |
| Accept/Decline/Negotiate | Chưa có flow backend hoàn chỉnh. Hiện action disabled, không mở form giả hoặc cập nhật status qua JS |
| Notifications/Add/Delete reminder | Chưa có datasource. Hiện trạng thái **Chưa khả dụng**, không hardcode số unread |
| Save Profile/Change Password | Chưa có endpoint hoàn chỉnh trong workspace. Hồ sơ chỉ đọc, action disabled; không tái dùng Forgot Password làm Change Password |
| Apply/CV upload | Link tới Public Jobs thật. Không tự tạo upload/backend giả |

Phạm vi list trên Dashboard: 20 hồ sơ mới nhất, 10 lịch sắp tới, 20 thư mời gần nhất. Count tổng và breakdown truy vấn toàn bộ dữ liệu thuộc tài khoản. Filters chỉ lọc các dòng đã tải, không giả pagination/search toàn DB. UI ghi rõ giới hạn.

## Tiến trình

1. Scan code/route/schema và Git; giữ các chỉnh sửa đang có của người dùng.
2. Nối CandidatePanel ở tầng Service/Repository, kiểm tra compile sớm.
3. Tạo fragment Candidate và CSS/JS riêng; Việt hóa riêng nhánh Candidate trong shared fragments.
4. Thêm test scope/privacy/Thymeleaf/security và fixture để kiểm tra desktop/mobile.
5. Lượt test đầu: 8/9 PASS, 1 ERROR vì Thymeleaf dành riêng tên biến `application`. Đổi biến vòng lặp thành `item`; không comment/bỏ test.
6. Lượt test chính: 23/23 PASS, gồm 4 service, 5 web, 1 DB read-only và 13 Public Jobs regression.
7. Browser round1: 51/52 PASS; ở 375px span sr-only của link interview có containing block ngoài bảng nên gây scroll ngang toàn trang.
8. Sửa root cause: đặt table wrapper Candidate `position:relative` để mô tả ẩn nằm trong vùng cuộn; không dùng overflow hidden trên body. Thu gọn spacing/nhãn header mobile riêng Candidate.
9. Chạy xác nhận 22/22 service/web/career tests sau chỉnh template/header cuối; Java/query không đổi từ lượt DB PASS. Browser round2: 52/52 PASS. Dừng sau lượt xác nhận, không thêm redesign.

## Verification

- Compile sau nhóm nối dữ liệu: PASS.
- Tổng 23 test riêng biệt: PASS. Lượt xác nhận cuối 22 test không cần chạy lại DB read-only đã PASS vì chỉ chỉnh CSS/body class.
- Spring Context ở môi trường test, Hibernate schema validate và query Candidate trên SQL Server: PASS; không khởi động HTTP server của ứng dụng trong bước này.
- Chrome fixture: PASS 52/52, 1440/1024/768/375, populated/empty, filter không dấu, keyboard details, focus/CSRF/menu/reduced motion.
- JS syntax và diff check cho các file tracked thuộc đợt này: PASS. `git diff -- database/schema` không có thay đổi.
- Live application E2E: NOT TESTED. Kết quả này không xác nhận toàn bộ bộ test project hoặc sửa các lỗi ngoài Candidate UI đã liệt kê trong audit trước.
- Không coi fixture/browser preview là E2E ứng dụng hoặc xác nhận SMTP/CV submission.

## Điểm tiếp tục

Khi triển khai mutation thật, cần xác nhận quy tắc và owner của từng feature trước; thêm DTO + validation + Service transaction + controller mỏng + GlobalExceptionHandler + CSRF. UI chỉ bật action sau khi endpoint và flow quyền sở hữu có test. Không sửa schema để lấp các gap này.

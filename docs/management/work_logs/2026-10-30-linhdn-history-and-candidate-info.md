# Tổng hợp quá trình thay đổi và bổ sung thông tin ứng viên tiềm năng

- **Ngày tổng hợp:** 30/10/2026, giờ Việt Nam.
- **Người tổng hợp / thực hiện lần này:** LinhDN / Codex, theo yêu cầu người dùng.
- **Yêu cầu:** tham khảo mockup để thêm thông tin hồ sơ vào chi tiết ứng viên tiềm năng; rà lại quá trình thay đổi trước đây và cập nhật work log.
- **Nguồn:** lịch sử Git của checkout hiện tại và các báo cáo trong `docs/members/linhdn/`, `docs/management/work_logs/`. Đây là tổng hợp phạm vi LinhDN có bằng chứng lưu trong repository, không phải danh sách mọi thay đổi của toàn nhóm hoặc mọi phiên chat.
- **Mốc Git khi bắt đầu:** `f13cea2c` (`update UI for potential candidate detail`, 30/10/2026); working tree sạch. Phần lịch sử dưới đây đã tồn tại trước phiên này, không ghi nhận là code mới của Codex.

## 1. Quá trình đã thay đổi

Ngày trong tên báo cáo là ngày công việc được tài liệu ghi nhận; ngày commit/merge có thể khác. Không cộng dồn số test hay số file giữa các đợt vì có file và ca kiểm thử lặp lại.

| Mốc | Công việc / kết quả được ghi nhận | Bằng chứng để tra lại |
| --- | --- | --- |
| 08/10 | Authentication: Login/Register/Forgot/OTP/Reset; bổ sung OTP xác minh đăng ký trước khi tạo User + Candidate; email HTML/plain text, lỗi và cooldown. | [Tổng kết 08/10](../../members/linhdn/DAILY_CHANGE_SUMMARY_2026-10-08.md) |
| 11–15/10 | Các đợt cập nhật Authentication, Dashboard Admin và quản lý tài khoản; phát triển giao diện dashboard và cách trình bày tài khoản master/detail. Đây là các đợt lịch sử, giao diện có thể đã được thay thế ở những mốc sau. | [Auth/Dashboard/Admin](../../members/linhdn/SUMMARY_AUTH_DASHBOARD_ADMIN_2026-10-12.md), [Dashboard 13/10](../../members/linhdn/ADMIN_DASHBOARD_UI_2026-10-13.md), [Dashboard 14/10](../../members/linhdn/ADMIN_DASHBOARD_CHANGES_2026-10-14.md), [Account 15/10](../../members/linhdn/ACCOUNT_MASTER_DETAIL_2026-10-15.md) |
| 14/10 | Sprint Talent Pool đầu: lưu ứng viên tiềm năng, đọc/tìm kiếm kho và các luồng liên quan. Commit hợp nhất `19544727` — `Feature/talent pool first sprint (#54)`. | [Talent Pool 14/10](../../members/linhdn/TALENT_POOL_UPDATE_2026-10-14.md) |
| 16/10 | Bookmark tiềm năng và ghi chú dùng chung HR/HM; xem CV, bỏ lưu; tinh chỉnh danh sách và chi tiết hồ sơ ứng tuyển. | [Tổng kết 16/10](../../members/linhdn/UPDATE_SUMMARY_2026-10-16.md), [Candidate pipeline](../../members/linhdn/2026-10-16-candidate-pipeline-ui-refinement.md) |
| 17/10 | Selection: chọn nhiều ứng viên/CV qua nhiều trang, giữ draft theo tài khoản và tin tuyển dụng, kiểm tra lại nguồn trước khi xác nhận, popup tổng kết và xem CV nguồn. | [Selection UI](../../members/linhdn/2026-10-17-talent-pool-selection-ui.md) |
| 17/10 | Chi tiết ứng viên: header tên/liên hệ, CV lịch sử bên trái, ghi chú bên phải, trạng thái rỗng, giữ bộ lọc và trang khi quay lại. | [Candidate detail UI](../../members/linhdn/2026-10-17-talent-pool-candidate-detail-ui.md) |
| 17/10 | Đề xuất HM dùng Application ở `HM_Suggested`; HR xác nhận ứng viên đồng ý dùng CV cũ mới chuyển `HM_Passed`. Từ chối đề xuất dùng `Rejected` cùng audit. Không tạo bảng riêng cho Talent Pool. | [Chức năng HM](../../members/linhdn/2026-10-17-hm-suggestions-functional.md) |
| 17/10 | Flow C HR → HM: gửi gợi ý, HM chấp nhận/từ chối, quản lý và xóa gợi ý trước phản hồi. HM đồng ý chưa thay cho sự đồng ý của ứng viên. Hợp nhất `37bf608d` — `Feature/potential candidate (#66)`. | [Flow C](../../members/linhdn/2026-10-17-hr-hm-recommendations-flow-c.md) |
| 17/10 | Tích hợp lưu tiềm năng vào kết quả phỏng vấn rồi thống nhất thành một nút mở popup bookmark/ghi chú dùng chung với Application; độc lập với Pass/Fail. | [Bookmark phỏng vấn](../../members/linhdn/2026-10-17-interview-potential-bookmark.md), [Popup dùng chung](../../members/linhdn/2026-10-17-interview-bookmark-toggle.md) |
| 17–22/10 | Các vòng chỉnh Authentication theo mockup: botanical, đồng bộ Login/Register và các trang OTP/Forgot/Reset. Giữ hợp đồng form/backend; các vòng sau thay hình thức trình bày của vòng trước. | [Botanical](../../members/linhdn/2026-10-17-auth-botanical-redesign.md), [21/10](../../members/linhdn/2026-10-21-login-register-sync.md), [22/10](../../members/linhdn/2026-10-22-auth-reference.md) |
| 24–26/10 | Rà Talent Pool, refactor package Candidate (`30299fec`, #98), sửa cập nhật phòng ban (`ad3d6b64`), cập nhật UI tài khoản/phòng ban (`f377727d`). Bỏ quyền System Admin khỏi workflow tuyển dụng (`d06d38a2`, #102). | Git của các commit đã nêu; cây source hiện tại trong `candidate/{domain,application,talentpool,infra}`. |
| 27/10 | Rà kiến trúc UI Admin; ghi nhận nợ phân tầng shell/CSS, stylesheet lặp, màu hardcode và kích thước nút. Hướng dẫn thiết kế hiện được lưu dưới `docs/members/linhdn/AI MD/`; root AGENTS/DESIGN đã bị gỡ trong `d627dd7b` (#110). | [Architecture review](../../members/linhdn/2026-10-27-admin-ui-architecture-review.md), [Design contract](../../members/linhdn/AI%20MD/DESIGN.md) |
| 28/10 | Tài khoản thống nhất Active/Inactive; bổ sung script chuyển dữ liệu Blocked cũ. Sửa clean build Dashboard sau refactor. | [Account status](../../members/linhdn/2026-10-28-account-status-active-inactive.md), [Compile fix](../../members/linhdn/2026-10-28-dashboard-compile-fix.md) |
| 28/10 | **Thay quyết định CV mới:** ứng viên tự nộp CV luôn về `Applied`, phát `ApplicationSubmittedEvent`, cập nhật cùng draft nếu có; tiếp tục sàng lọc thông thường. Nhánh HR xác nhận dùng CV cũ vẫn giữ `HM_Passed`. | [Luồng CV mới hiện hành](../../members/linhdn/2026-10-28-new-cv-normal-screening.md), commit `e03acee2`. |
| 28/10 | Đồng bộ list/detail/suggestions/recommendations theo workspace Admin, vẫn dùng role HR/HM. Đợt UI này có 9 file trong phạm vi báo cáo; commit `ddd73d63` còn chứa thay đổi khác nên không lấy toàn commit làm số file UI. | [Work log đồng bộ](2026-10-28-potential-candidate-admin-ui-sync.md), [Thống kê 9 file](../../members/linhdn/2026-10-28-potential-candidate-ui-change-summary.md) |
| 28/10 | Làm rõ danh sách: CV nguồn trong disclosure dưới tên, caption tin dài xuống dòng, avatar/badge/bảng và các trang gợi ý đồng bộ. Commit `695c0d3b`. | [Work log polish](2026-10-28-potential-candidate-list-polish.md) |
| 30/10, trước phiên này | `f13cea2c`: chỉnh trang chi tiết với header và contact chip, bảng CV có phòng ban/loại việc làm/ngày nộp, ghi chú và responsive; bổ sung `cvDetail` trong service. Commit còn chứa diagram/tài liệu ngoài UI, không nhận toàn bộ là thay đổi của phiên này. | `git show f13cea2c -- src/main/resources/templates/talent-pool/detail.html src/main/resources/static/css/pages/talent-pool-workspace.css src/main/java/com/group2/rms/candidate/talentpool/service/TalentPoolService.java` |
| 30/10, phiên này | Bổ sung khối **Thông tin ứng viên** theo mockup trong cột trái phía trên CV; sáu trường lấy từ Candidate hiện hữu. Tổng hợp lịch sử vào work log này. | Mục 2–5 bên dưới và working diff hiện tại. |

## 2. Thay đổi hiện tại theo mockup

- Thêm Ngày sinh, Giới tính, Địa chỉ, Nguồn ứng viên, LinkedIn và Portfolio trong cột trái, phía trên CV; ghi chú nằm ở cột phải và bắt đầu ngang hàng với khối thông tin.
- Ngày sinh hiển thị `dd/MM/yyyy`; trường null/rỗng/toàn khoảng trắng hiển thị “Chưa có thông tin”. Không điền dữ liệu minh họa vào hồ sơ thật.
- Dùng definition list `dl/dt/dd`, icon SVG cùng kiểu nét, token xanh Mộc, đường phân cách nhẹ. Bố cục 3 cột desktop, 2 cột dưới 992px, 1 cột dưới 768px; địa chỉ/URL dài được xuống dòng đầy đủ.
- Liên kết HTTP/HTTPS mở tab mới, có tên truy cập và `rel="noopener noreferrer"`; giá trị có scheme khác chỉ hiển thị dạng chữ đã escape.
- Dữ liệu được nối cho cả chi tiết tiềm năng thường và chi tiết trong ngữ cảnh recommendation. Không mở rộng DTO của danh sách ứng viên.
- CV, ghi chú, quyền truy cập, quyết định HM và điều hướng giữ hành vi hiện tại.

## 3. File thay đổi trong phiên này

| File | Nội dung |
| --- | --- |
| `src/main/java/com/group2/rms/candidate/talentpool/dto/TalentPoolDetailResponse.java` | Thêm sáu trường hồ sơ chỉ đọc vào DTO chi tiết. |
| `src/main/java/com/group2/rms/candidate/talentpool/service/TalentPoolService.java` | Map các trường Candidate trong `detail` và `recommendationDetail`. |
| `src/main/resources/templates/talent-pool/list.html` | Header bảng tích hợp tìm kiếm/bộ lọc phễu, đếm tổng ứng viên theo bộ lọc và kiểu danh sách theo mockup. |
| `src/main/resources/templates/talent-pool/detail.html` | Thêm section thông tin, xử lý thiếu dữ liệu và liên kết ngoài. |
| `src/main/resources/static/css/pages/talent-pool-workspace.css` | Class `tpw-info-*`, bố cục responsive và trạng thái focus của liên kết. |
| `src/test/java/com/group2/rms/candidate/talentpool/service/TalentPoolReadUnitTests.java` | Kiểm tra mapping các trường hồ sơ thật từ Candidate. |
| `src/test/java/com/group2/rms/candidate/application/pipeline/controller/ApplicationPipelineWebTests.java` | Cập nhật fixture, kiểm tra render hồ sơ, dữ liệu thiếu, escape và URL không an toàn. |
| `src/main/java/com/group2/rms/candidate/talentpool/controller/TalentPoolController.java` | Endpoint POST sửa/xóa ghi chú, giữ ngữ cảnh quay lại và phân trang CV. |
| `src/main/java/com/group2/rms/candidate/talentpool/dto/TalentPoolCvResponse.java` | Trạng thái/nhãn/tone CV; giữ constructor cũ tương thích. |
| `src/main/java/com/group2/rms/candidate/talentpool/dto/TalentPoolNoteResponse.java` | Role người viết và cờ sở hữu theo tài khoản đăng nhập. |
| `src/main/java/com/group2/rms/candidate/talentpool/repository/TalentPoolNoteRepository.java` | Nạp author.role cho danh sách ghi chú. |
| `src/main/resources/static/js/pages/talent-pool-detail.js` | Menu sửa, hủy, xác nhận xóa, chống gửi lặp và đếm ký tự. |
| `docs/management/WORK_LOG.md` | Thêm dòng tổng hợp và liên kết báo cáo này. |
| `docs/management/work_logs/2026-10-30-linhdn-history-and-candidate-info.md` | Tổng hợp quá trình, thay đổi hiện tại, kiểm tra và giới hạn. |

**Database:** không sửa entity, schema, migration hoặc seed. Hồ sơ đọc các cột Candidate có sẵn; endpoint sửa/xóa ghi chú dùng bảng TalentPoolNote hiện hữu. Phiên này không thao tác dữ liệu SQL thật. Không thay workflow tuyển dụng, không commit/push/stage.

## 4. Cách dùng hướng dẫn thiết kế

- Theo hợp đồng Mộc lưu tại `docs/members/linhdn/AI MD/DESIGN.md`, UI_RULES, UI_CSS_GUIDE và UI_CATALOG; tái sử dụng khung/card hiện hành.
- UI UX Pro Max: tra `responsive content reflow` (UX / Text Reflow and Spacing), áp dụng lưới linh hoạt và không cắt chữ dài.
- Impeccable: context xác nhận có visual system hiện hữu; áp dụng refinement có phạm vi và craft floor, giữ nhận diện Mộc theo mockup.
- Awesome: tham khảo [SAP Analysis / Hierarchy](../../../.agents/references/awesome-design-md/upstream/design-md/sap/DESIGN.md) về phân cấp tiêu đề/nhãn/giá trị; đây là tài liệu bên thứ ba. Chỉ dùng nguyên tắc phân cấp, giữ font/token Mộc thay vì dùng cỡ chữ và palette SAP.
- Taste không cần kích hoạt cho bổ sung thông tin có mockup và nhận diện đã chốt; không mở rộng thành redesign.

## 5. Kiểm chứng và việc còn lại

- **142/142 test Java qua**, 0 failure/error/skipped: TalentPoolReadUnitTests **56**, TalentPoolRecommendationTests **25**, ApplicationPipelineWebTests **61**. Bao gồm mapping sáu trường, render Thymeleaf, ngày sinh/URL hợp lệ, null/blank, escape HTML, chặn link javascript và các kiểm thử phân quyền/ngữ cảnh hiện hữu.
- Sau khi sửa cấu trúc `dl/dt/dd`, chạy lại riêng **4** test render chi tiết/recommendation: 0 failure/error/skipped. Không cộng các ca chạy lại vào 142.
- Lệnh dùng Maven cài sẵn ở chế độ offline; nạp Byte Buddy agent lúc khởi động JVM để tránh lỗi Mockito tự attach trong sandbox. Không sửa POM hay cài dependency. JaCoCo tắt trong lượt xác minh này, không báo độ phủ mới.

```powershell
mvn.cmd -o '-Dtest=TalentPoolReadUnitTests,TalentPoolRecommendationTests,ApplicationPipelineWebTests' '-Djacoco.skip=true' '-DargLine=-javaagent:C:/Users/Administrator/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar' test -q
```

- `git diff --check`: qua; liên kết Markdown local trong báo cáo này đã kiểm tra tồn tại.
- **Chưa có xác minh hình ảnh/computed style desktop/mobile:** Playwright không khởi chạy được Edge; thử Chrome headless dự phòng cũng không tạo ảnh. Không coi việc Chrome trả exit code 0 là bằng chứng render thành công. Script QA tạm ở thư mục artifact ngoài repository.
- MockMvc sử dụng Spring MVC/Thymeleaf thật với service mock; unit test dùng repository mock. Không chạy SQL Server/SMTP/PDF hoặc E2E với hồ sơ thật trong phiên này.
- Còn cần mở trang trên trình duyệt thực tế để nghiệm thu vị trí, spacing, URL/địa chỉ dài và bố cục 3/2/1 cột. Nợ shell/CSS được báo ngày 27/10 không được xử lý trong thay đổi này.
- Các số test trong báo cáo cũ chỉ là bằng chứng tại thời điểm của từng báo cáo, không phải kết quả chạy lại ngày 30/10.

## 6. Điều chỉnh theo phản hồi mockup tiếp theo — 30/10

- Người dùng yêu cầu khối thông tin gọn trong cột trái, không kéo ngang toàn trang.
- Gom Thông tin ứng viên và CV vào `tpw-main-stack`; ghi chú/quyết định giữ ở cột phải, bắt đầu ngang hàng với thông tin. Khoảng cách giữa hai khối trái dùng token `--s4`.
- Giữ sáu trường theo lưới 3/2/1 và cách xuống dòng hiện có; chỉ sửa bố cục template/CSS, không đổi dữ liệu hoặc nghiệp vụ.
- Kiểm tra: xác nhận cấu trúc DOM và `git diff --check`; đây là thay đổi vị trí đơn giản, không chạy lại các bộ test backend. Chưa có bằng chứng ảnh runtime cho bố cục mới.

## 7. Đối chiếu ảnh thực tế và bản tài nguyên chạy — 30/10

- Người dùng xác nhận bố cục hai cột: thông tin + CV bên trái, ghi chú bắt đầu từ đầu cột phải như mockup thứ hai.
- Kiểm tra thấy `src/main/resources/templates/talent-pool/detail.html` đã có `tpw-main-stack`, nhưng `target/classes/templates/talent-pool/detail.html` vẫn đặt thông tin trước grid. CSS trong target cũng còn margin của bố cục toàn hàng cũ.
- Đồng bộ lại resources bằng Maven và kiểm tra render chi tiết/recommendation; không thay đổi thêm nội dung hoặc nghiệp vụ để khắc phục chênh lệch này.
- Kết quả: template/CSS trong target trùng byte với source; 4/4 kiểm thử render qua, 0 lỗi; HTML Thymeleaf xuất ra có thông tin và CV trong cột trái, sidebar ghi chú ở cột phải. Chưa xác minh ảnh trình duyệt của ứng dụng đang mở.

## 8. Thu gọn khối thông tin theo ảnh cuối — 30/10

- Giữ cột trái thông tin/CV và cột phải ghi chú. Khối thông tin không grow theo chiều cao còn trống (`flex: 0 0 auto`).
- Giảm padding card còn 8px × 12px, mỗi ô 12px; tiêu đề sans 16px, nhãn 12px, giá trị 14px, icon nền 32px. Với nội dung ngắn, hai hàng gọn theo nội dung thay vì đặt chiều cao cố định.
- Vạch chia cột dùng pseudo-element ngắn, chừa 16px trên/dưới; đường ngang chia hai hàng giữ nhẹ theo token. Responsive giữ 3/2/1 cột.
- URL dài hiển thị một dòng có ellipsis để không làm hàng phình; có title chứa đầy đủ URL và link mở thật. Địa chỉ vẫn xuống dòng nếu cần để giữ thông tin.
- Đã đồng bộ template/CSS sang target/classes. Không chạy lại test backend cho thay đổi trình bày này; kiểm tra diff và đối chiếu source/resource.
- Lần này đã khởi chạy được Chrome headless qua quyền chạy trình duyệt và xem ảnh render desktop 1440×1000: khối thông tin gọn, 3 cột × 2 hàng, vạch ngăn ngắn, ghi chú nằm ngang bên phải. Dữ liệu trong ảnh là fixture MockMvc, không phải hồ sơ SQL thật. Ảnh: `C:/Users/Administrator/.codex/visualizations/2026/10/30/01a18c5b-8f9a-7663-87be-b1559676ec27/desktop.png`.
- Có capture mobile nhưng khung bị cắt bên phải nên chưa coi là nghiệm thu mobile; không tuyên bố kiểm chứng toàn bộ responsive. Giới hạn ảnh của các lượt trước vẫn là bằng chứng lịch sử, không áp dụng cho capture desktop mới này.

## 9. Toast góc phải và giao diện ghi chú — 30/10

- Thay banner thành công/lỗi toàn hàng trên trang chi tiết bằng fragment `flashToast` có sẵn; nạp CSS/JS dùng chung, không sửa component chung. Toast rộng tối đa 300px ở góc phải, tự tắt sau 3 giây, tạm dừng khi hover/focus và có nút đóng.
- Ghi chú dùng header gọn có icon/tiêu đề 16px và đường phân cách. Avatar chữ cái giữ từ tên thật; tên/timestamp cùng một hàng nếu đủ chỗ, nội dung 14px bên dưới. Các ghi chú có đường phân cách, giữ thứ tự mới nhất trước và escape nội dung.
- Form Thêm ghi chú theo ngay sau danh sách, không bị đẩy xuống đáy cột bởi khoảng trống. Panel giữ chiều cao theo nội dung; không thêm menu “…” hay nút sửa/xóa.
- Không suy đoán vai trò hay ảnh người viết khi DTO hiện tại chưa có các dữ liệu này. Giữ POST/CSRF/validation, noteDraft, phân quyền và handler lưu hiện hữu.
- Mở rộng fixture render hiện có để kiểm tra thông báo flash và header ghi chú mới.
- Kết quả cuối: 5/5 kiểm thử render/redirect ghi chú qua, 0 failure/error/skipped; kiểm tra cú pháp flash-toast.js và git diff --check qua. Fixture flash ban đầu chưa truyền successMessage đã được bổ sung trước lượt chạy đạt. Source và target/classes đồng bộ. Đã xem ảnh Chrome desktop 1440×900 có toast góc phải và ghi chú/form gọn, không có menu thao tác; dữ liệu ảnh là fixture. Ảnh: `C:/Users/Administrator/.codex/visualizations/2026/10/30/01a18c5b-8f9a-7663-87be-b1559676ec27/notes-desktop.png`.

## 10. Căn đáy khung ghi chú với cột CV — 30/10

- Theo ảnh phản hồi mới nhất, sidebar dùng `align-self: stretch`, panel ghi chú dùng `flex: 1 0 auto` để đáy ngang cột trái.
- Giữ form theo ngay sau feed; phần nền trắng dư ở cuối panel, không kéo giãn textarea hoặc khối thông tin ứng viên.
- Dưới 992px giữ rule `flex: 0 0 auto` đã có để panel trở về chiều cao nội dung khi xếp một cột.
- Đã đồng bộ CSS vào target/classes và kiểm tra diff; không đổi nghiệp vụ hay chạy lại test backend cho hai declaration trình bày.
- Đã đối chiếu ảnh Chrome với profile mới: đáy khung ghi chú ngang đáy CV; form giữ nguyên phía trên. Ảnh fixture desktop: `C:/Users/Administrator/.codex/visualizations/2026/10/30/01a18c5b-8f9a-7663-87be-b1559676ec27/notes-aligned.png`.

## 11. Menu sở hữu, role, lịch sử CV và bố cục chốt — 30/10

Phần này thay thế quyết định không có menu ở mục 9 và vị trí form trong mục 10 theo yêu cầu mới nhất.

- Nút “…” **chỉ có trên ghi chú của tài khoản đang đăng nhập**, so sánh UserId; không so tên. Menu chứa Sửa/Xóa. Sửa mở form ngay tại comment, có Hủy; xóa có bước xác nhận. Backend kiểm tra lại tác giả và CandidateId trước khi ghi/xóa, không phụ thuộc cờ frontend. ID người khác → 403, ID sai ứng viên → 404.
- Giữ tác giả/thời gian tạo khi sửa; kiểm tra nội dung 1–1.000 ký tự, giữ draft khi validation lỗi, giữ CSRF và quyền HR/HM hiện hữu. Candidate đã bỏ lưu không được sửa/xóa note qua các endpoint này. Không thêm quyền tuyển dụng cho role khác.
- Role nằm cạnh tên, lấy từ User.role hiện tại; HR hiển thị “Nhân sự”, Hiring Manager/Interviewer hiển thị role có thật. Repository nạp author.role. Avatar 40px; tên đậm 15px, role 13px, nội dung 15px, timestamp 12px.
- Lịch sử ứng tuyển & CV có sáu cột: số thứ tự, vị trí, phòng ban, ngày nộp, trạng thái, xem CV. Trạng thái lấy từ Application; trường hợp Đã rút được đối chiếu AuditLog hiện hữu, không suy từ Rejected. Giữ nhãn trạng thái chung của hệ thống, không dựng dữ liệu từ mockup.
- Phân trang 3 CV/trang bằng `cvPage`, tổng tính trên toàn bộ CV; giữ filter/job/recommendation/back context. Tổng cộng chuyển lên góc phải header. Footer chỉ còn phân trang khi có nhiều trang.
- Khung ghi chú vẫn ngang đáy cột CV. Form Thêm ghi chú **ghim xuống đáy panel** bằng flex margin tự động và padding 16px; textarea không bị kéo giãn. Feed cuộn theo giới hạn hiện hữu khi nhiều ghi chú; có bộ đếm 0/1000 theo giới hạn backend, không giảm xuống 500 chỉ vì ảnh mẫu.
- Thay đổi frontend có phạm vi dùng lại token/component Mộc. UI UX Pro Max tra keyboard/focus; Impeccable craft floor và kiểm tra ảnh; Awesome SAP Hierarchy tham khảo như mục 4. Đọc policy Taste và giữ palette/type của Mộc; mockup đã chốt nên không chạy draw để chọn hướng khác.

### Kiểm tra cuối

- **171/171 test qua**, 0 failure/error/skipped: TalentPoolReadUnitTests 62, TalentPoolRecommendationTests 25, TalentPoolBookmarkTests 7, ApplicationPipelineWebTests 64, InterviewPotentialWebTests 13.
- Bao gồm sửa/xóa chính chủ; chặn người khác dù trùng tên; sai CandidateId; blank/quá dài; bất biến tác giả/ngày tạo; ownership DTO; trạng thái rút từ audit; menu chỉ hiện chính chủ; phân trang; CSRF; redirect/draft; 403 khi backend từ chối sở hữu.
- Sau chỉnh tổng ở header/form ở đáy, chạy lại các ca render/endpoint liên quan: kết quả ghi dưới đây. CSS phóng to thêm sau đó được kiểm tra bằng Chrome.
- Kiểm tra Chrome thật trên HTML fixture Thymeleaf: 2 note nhưng chỉ 1 menu, có hai role cạnh tên, bảng 6 cột; mở sửa đúng form và focus textarea; hủy trả về nội dung; hủy xóa không gửi/không khóa nút; bộ đếm cập nhật 4/1000. Không có JS exception. Desktop 1440×1000 và viewport mobile thực 390px, document scrollWidth=390 (không tràn ngang).
- Đã xem ảnh desktop cuối: tổng CV ở header, form dưới đáy, khung hai bên ngang đáy, avatar/tên lớn hơn. Ảnh và số đo: `C:/Users/Administrator/.codex/visualizations/2026/10/30/01a18c5b-8f9a-7663-87be-b1559676ec27/actions-final-desktop.png`, `actions-final-menu.png`, `actions-final-mobile.png`, `actions-final-qa.json`.
- Đây là unit + MVC service mock và browser fixture; **chưa chạy sửa/xóa trên SQL Server thật**. Không thay cấu hình server/schema, không commit/push. Java đã biên dịch; template/CSS/JS trong target/classes được đối chiếu với source.
- Lượt kiểm tra sau cùng: 8/ 8 test render/endpoint qua; không cộng lượt chạy lại này vào 171. `node --check` và `git diff --check` qua. Tổng phạm vi cuối: 13 file (11 đã theo dõi + 2 mới), bao gồm các thay đổi của những lượt trước trong chat này.

## 12. Giãn cột lịch sử và bỏ đường kẻ comment — 30/10

- Giải thích trạng thái là của Application tương ứng với CV, không phải trạng thái file. Đổi tên cột thành “Trạng thái hồ sơ” để rõ nghĩa, giữ nguyên dữ liệu và mapping.
- Tăng khoảng đệm ô bảng từ 12px lên 16px; cấp rộng hơn cho phòng ban/ngày/trạng thái/thao tác, dùng table-layout fixed. Bảng tối thiểu 800px và cuộn trong wrapper nếu không đủ chỗ; dưới 768px giữ dạng từng hồ sơ, không tràn toàn trang.
- Bỏ đường kẻ ngang dưới header ghi chú và giữa các comment; tăng khoảng cách dọc comment lên 16px. Giữ tên/role/menu riêng chủ sở hữu và form dưới đáy.
- Đồng bộ CSS/template vào target/classes. Chỉ thay trình bày, không đổi nghiệp vụ hoặc chạy lại test backend.
- Chrome xác nhận computed border-top giữa comment và border-bottom header ghi chú đều 0px, padding ngang ô bảng 16px; menu chính chủ/sửa-hủy/xóa-hủy vẫn hoạt động. Desktop đã xem ảnh; mobile viewport và scrollWidth cùng 390px, không có JS exception. Ảnh: `C:/Users/Administrator/.codex/visualizations/2026/10/30/01a18c5b-8f9a-7663-87be-b1559676ec27/actions-spacing-desktop.png`.

## 13. Trang danh sách Talent Pool theo mockup — 30/10

- Đưa form GET tìm kiếm/bộ lọc vào header “Kho ứng viên tiềm năng” của card bảng. Search bên phải, nút phễu mở disclosure cho Người đánh dấu/Vị trí ứng tuyển; badge chỉ đếm số bộ lọc phụ đang áp dụng. Giữ input name, options, query jobPostingId, action và reset link hiện có.
- Đổi phần đếm thành **“X ứng viên”** ở mép phải header. X lấy `page.totalElements`, là tổng ứng viên thỏa keyword/markedBy/sourceJobId trên mọi trang, không lấy `page.content.size` và không hardcode số từ ảnh.
- Đối chiếu `TalentPoolCandidateRepository.POTENTIAL_FILTER`: query lấy dữ liệu và countQuery dùng cùng WHERE; không cần sửa backend để đếm.
- Tiêu đề trang sans đậm, avatar/tên căn giữa; giữ bảng ứng viên/liên hệ/vị trí/thao tác với nút xem CV, ghi chú và hồ sơ hiện hữu. Multi-CV vẫn giữ native select và mapping CV nguồn.
- Tái sử dụng management-filters.css/js: outside-click/Escape đóng popover, giữ lựa chọn khi đóng, label/focus và form GET hoạt động không cần JS. CSS mới chỉ alias `tpw-list-*`/`tpw-filter-*` trong file feature.
- Giữ class `.tp-filter` trên form mới vì handler lựa chọn đang truy vấn class này để gắn guard rời trang. Đã kiểm tra selection mode vẫn có checkbox và hidden jobPostingId.
- Không sửa shared shell, role/quyền hoặc workflow. Bảng hẹp cuộn trong wrapper; mobile xếp search xuống hàng dưới và popover một cột.
- Kiểm thử: **2/2 MVC render test** qua, trong đó fixture có 5 hàng nhưng tổng 49; sau filter tổng 27; rỗng tổng 0. Kiểm tra đúng giá trị filter truyền xuống service. **38/38 test JS Talent Pool** qua (selection, UI, workspace, recommendation, bookmark action).
- Chrome desktop/mobile: header chứa search form, GET gửi đúng keyword/markedBy/sourceJobId; Escape đóng mà không mất draft; filter badge 2; kết quả lần lượt 49/27/0 theo fixture; selection mode giữ jobPostingId=12 và checkbox hoạt động. Mobile document scrollWidth=viewport=390px, popover rộng 324px, bảng cuộn trong wrapper. Không có JS exception. Đã xem ảnh desktop thường/lọc và mobile mở bộ lọc.
- 49/27 và các ứng viên trong ảnh chỉ là fixture kiểm thử, không seed vào database hoặc hardcode vào sản phẩm. Không chạy lại SQL Server thật; đối chiếu countQuery từ source và kiểm tra hợp đồng model/MVC.
- Template/CSS target/classes trùng source; git diff --check qua. Ảnh: `C:/Users/Administrator/.codex/visualizations/2026/10/30/01a18c5b-8f9a-7663-87be-b1559676ec27/list-desktop.png`, `list-filtered-desktop.png`, `list-filter-mobile.png`; kết quả chi tiết `list-qa.json`.

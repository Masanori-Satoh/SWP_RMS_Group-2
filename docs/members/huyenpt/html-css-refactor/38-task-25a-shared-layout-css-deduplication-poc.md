# Task 25A — PoC khử trùng link CSS trong layout Thymeleaf

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](../README.md).

Nhánh `refactor/huyenpt/offer-ui-js-to-html-css`, HEAD `4ed4c25426b45a4c186415eb78340bd5c2574384`. Lỗi phát sinh trong HTML phía server. Layout Dialect 3.4.0 tự ghép head và đồng thời thay hai slot `styles` bằng fragment của trang con. Với chuỗi base → internal → trang, CSS cấp trang xuất hiện ba lần. Kiểm tra Task 25 trước đó đã xác nhận HTML và DOM có cùng thứ tự, với 15 ca Java và bốn lượt đọc HTTP/DOM trên Offer và Application.

## Bản đồ phụ thuộc

Quét 107 template HTML, 53 có head và 46 khai báo fragment `styles`. Cả 46 khai báo nằm trong head. Không tìm thấy lời gọi `th:insert`, `th:replace`, `layout:insert` hoặc `layout:replace` trỏ đích danh tới fragment này. Các template dưới đây dùng nó qua decorate. `fragments/sidebar.html` cũng decorate internal nhưng không khai báo styles. Các trang standalone và fragment/email đi theo đường render riêng.

| Chuỗi kế thừa | Template khai báo styles |
|---|---|
| base | `layout/base.html` cung cấp slot rỗng |
| base → internal | `layout/internal.html` cung cấp slot rỗng sau workspace và candidate shell |
| base → internal → Offer | `offers/{candidate-detail,candidate-list,detail,director-list,form,list}.html` |
| base → internal → Candidate/Application | `candidate/{application-detail,applications,my-application-detail,my-applications,my-profile}.html` |
| base → internal → Interview | `interview/{candidate-list,form,list,result}.html` |
| base → internal → Requisition và Job | `requisitions/{detail,form,list}.html`, `job-postings/{create,detail,list}.html` |
| base → internal → Dashboard, Notifications, Talent Pool và User | `dashboard/{admin,index}.html`, `notifications/list.html`, `talent-pool/{detail,list,recommendations,suggestions}.html`, `user/profile.html` |
| base → internal → Admin, có tham số role/menu | `admin/audit-log.html`, `admin/accounts/{candidates,form,list}.html`, `admin/api-monitoring/index.html`, `admin/departments/{form,list}.html` |
| base → public → Jobs công khai | `layout/public.html` có slot rỗng sau topbar, `candidate/{job-board,job-detail}.html` cung cấp CSS |
| base → Error | `error/{400,403,404,500}.html` |
| base → auth | `layout/auth.html` đặt `auth.css` bên trong fragment, không tìm thấy template decorate trực tiếp layout này. Login hiện tại dùng head riêng và có 5 link giống nhau trên hai server. |

Không áp dụng tìm/xóa tất cả fragment `styles` vì `auth.html` chứa link thật. `interview/result.html` nạp `applications.css` theo role. Admin account và department chọn CSS create/edit theo model. Candidate shell nằm ngoài slot internal, có điều kiện riêng. Thử nghiệm head kiểm tra cả hai nhánh của ba điều kiện cấp trang, giữ tập URL như baseline. Probe chuẩn hóa title động để chạy với model nhỏ và mô phỏng điều kiện ID null của Department qua biến boolean. Đây là kiểm tra head, không thay cho việc mở các màn Admin và Interview Result bằng dữ liệu thực.

Tài liệu liên quan: [Architecture Guide](../../../architecture/ARCHITECTURE_GUIDE.md), [UI CSS Guide](../../../architecture/UI/UI_CSS_GUIDE.md), [UI Rules](../../../architecture/UI/UI_RULES.md), [UI New Page](../../../architecture/UI/UI_NEW_PAGE.md) và [UI Migration](../../../architecture/UI/UI_MIGRATION.md). Sau khi Lead duyệt, cần cập nhật hướng dẫn về cách head merge quyết định thứ tự CSS.

## Các phương án thử

- **A** là ứng dụng local trên 8080 với hai slot gốc.
- **B** là cùng ứng dụng Spring Boot 3.5.16, Spring 6.2.19 và Layout Dialect 3.4.0 trên 18081. Resolver dùng bản sao toàn bộ cây template dưới `target/task25a-46cd8c58de664e3db7543a8bf1cf63dc/templates/`. Chỉ bỏ hai slot rỗng trong bản sao. Giữ profile local, dùng schema validate và tắt SQL init. Browser chỉ đăng nhập tài khoản fixture, GET và mở/đóng UI.
- **C** là phương án mở rộng cho nhánh public. B làm job-board/job-detail giảm từ ba xuống hai bản vì `public.html` còn slot riêng. Probe Spring Thymeleaf trên hai head public cho một bản khi bỏ thêm slot rỗng public. Sau khi kết thúc đo B, bỏ slot public trong bản sao và chạy Chrome trên `/jobs`. Tổng link A → B → C là 7 → 6 → 5. A và C khớp thuộc tính mẫu, hình học, bề rộng tài liệu, meta, CSRF và script ở ba viewport. C giữ `topbar.css` trước `job-board.css` và ghi 0 runtime exception. Job Detail mới được xác minh ở fixture head.

Ví dụ chuỗi CSS từ HTML và DOM Offer HR List. B là tokens → global → components → workspace → offer-common → offer-hr → flash-toast. A là tokens → global → components → (offer-common, offer-hr, flash-toast) → workspace → (offer-common, offer-hr, flash-toast) → (offer-common, offer-hr, flash-toast). B giữ vị trí ưu tiên của bộ link CSS trang cuối cùng trong A. Candidate thêm `candidate-shell.css` sau workspace và trước CSS trang. Application giữ thứ tự flash-toast trước applications.

## Diff đề xuất để Lead xem

Bỏ đúng các slot rỗng sau đây. Đây là đề xuất, các đường dẫn production chưa được sửa.

```diff
--- a/src/main/resources/templates/layout/base.html
+++ b/src/main/resources/templates/layout/base.html
@@ -22,4 +22,2 @@

-    <!-- Slot nạp CSS riêng của từng trang/layout con -->
-    <th:block layout:fragment="styles"></th:block>
 </head>
--- a/src/main/resources/templates/layout/internal.html
+++ b/src/main/resources/templates/layout/internal.html
@@ -10,5 +10,2 @@
     </th:block>
-    <th:block layout:fragment="styles">
-        <!-- Trang con nạp thêm style riêng nếu có -->
-    </th:block>
 </head>
```

Nếu yêu cầu bao gồm khử trùng cả Jobs công khai, thêm hunk C dưới đây. Giữ nguyên link `topbar.css` và block của `layout/auth.html`.

```diff
--- a/src/main/resources/templates/layout/public.html
+++ b/src/main/resources/templates/layout/public.html
@@ -7,5 +7,2 @@
     <link rel="stylesheet" th:href="@{/css/topbar.css}">
-    <th:block layout:fragment="styles">
-        <!-- Trang con có thể nạp thêm styles nếu cần -->
-    </th:block>
 </head>
```

Có thể review hai thay đổi riêng để giới hạn phạm vi. Rollback bằng cách khôi phục đúng các slot trong bản sửa sau khi Lead đã duyệt. Kiểm tra lại cả vai trò Admin và các trang lỗi vì chúng cùng dùng base, chạy lại các suite web liên quan và đối chiếu ảnh ở ba viewport nếu có baseline ổn định.

## Đo A/B trên ứng dụng local

So sánh 13 cặp route/role, mỗi cặp ở 1440, 768 và 390 px. HTML của cùng lượt điều hướng lấy bằng `Network.getResponseBody`, so thứ tự link với DOM. So script URL và meta theo thứ tự, bao gồm sự hiện diện của CSRF, không xuất token. Chrome tắt cache và ghi Resource Timing cho CSS. Bảng dùng số đo resource của mỗi lượt điều hướng, bao gồm CSS import. Byte là encoded body và transferred bytes.

| Vai trò và route | Link A → B | URL riêng A/B | Entry CSS A → B | Encoded A → B | Transfer A → B |
|---|---:|---:|---:|---:|---:|
| HR `/offers`, `/offers/create` | 13 → 7 | 7/7 | 10 → 9 | 154.459 → 152.196 | 157.459 → 154.896 |
| HR `/offers/8` | 13 → 7 | 7/7 | 10 → 9 | 146.598 → 144.335 | 149.598 → 147.035 |
| HR `/applications` | 10 → 6 | 6/6 | 9 → 9 | 123.257 → 123.257 | 125.957 → 125.957 |
| HR `/interviews` | 16 → 8 | 8/8 | 11 → 11 | 111.163 → 111.163 | 114.463 → 114.463 |
| HR `/requisitions` | 16 → 8 | 8/8 | 11 → 11 | 112.634 → 112.634 | 115.934 → 115.934 |
| HR `/internal/job-postings` | 16 → 8 | 8/8 | 11 → 10 | 110.408 → 108.145 | 113.708 → 111.145 |
| HR `/jobs` | 7 → 6 | 5/5 | 6 → 6 | 80.612 → 80.612 | 82.412 → 82.412 |
| Director `/offers` | 13 → 7 | 7/7 | 9 → 9 | 144.335 → 144.335 | 147.035 → 147.035 |
| Director `/offers/8` | 13 → 7 | 7/7 | 10 → 9 | 146.598 → 144.335 | 149.598 → 147.035 |
| Candidate `/portal/offers` | 11 → 7 | 7/7 | 9 → 10 | 132.074 → 134.337 | 134.774 → 137.337 |
| Candidate `/portal/applications` | 8 → 6 | 6/6 | 9 → 8 | 114.808 → 112.545 | 117.508 → 114.945 |
| Candidate `/portal/interviews` | 11 → 7 | 7/7 | 9 → 10 | 97.756 → 100.019 | 100.456 → 103.019 |

Chênh lệch resource đúng bằng một entry import `fonts.css` với 2.263 byte encoded và 2.563 byte transferred, có chiều tăng lẫn giảm giữa các route. Mỗi URL CSS cấp trang có một entry trong các mẫu đo. Không quy đổi số link bỏ đi thành số request tiết kiệm, và chưa đo CPU style recalculation hay thời gian tải đủ lần để kết luận về hiệu năng.

39 cặp snapshot sau khi chờ font và chuyển động ổn định khớp về các thuộc tính CSS mẫu, kích thước làm tròn, backdrop và bề rộng tài liệu. HR Offer có bề rộng 1425/753/375px trên cả A/B. Kiểm tra shell, navigation, heading, bảng, nút, input/select, badge, Excel modal, decision modal, decision card và toast ở nơi có mặt. Backdrop Send của HR/Director Detail đều là `rgba(37, 41, 35, 0.45)` ở ba viewport. Các lượt route ghi 0 runtime exception qua CDP. Meta, CSRF và danh sách script khớp 13/13. Lượt đo đầu có chênh lệch hình học do snapshot lấy trong lúc giao diện còn chuyển động, lượt ổn định đã loại được chênh lệch. Không có so sánh ảnh theo pixel. Các form edit có Draft, Candidate Detail có tác dụng đánh dấu thông báo, Admin thực tế và trang lỗi chưa nằm trong ma trận browser.

Fixture mới `scratch/task25a-browser.cjs` dùng lại harness CDP và fixture login đã có. Chạy `node scratch/task25a-browser.cjs routes 1440 hr 8080`, đổi role thành `director` hoặc `candidate` và port thành `18081` để so sánh. `TASK25A_ROUTE` lọc một route khi kiểm tra riêng. Khởi động B bằng classpath ứng dụng đã build, `--server.port=18081 --server.address=127.0.0.1 --spring.thymeleaf.prefix=file:///.../templates/ --server.servlet.session.persistent=false --spring.sql.init.mode=never --spring.jpa.hibernate.ddl-auto=validate` với prefix trỏ tới bản sao đã bỏ hai slot. Kết quả JSONL được lưu dưới `target/task25a-stable-*.jsonl`, inventory ở `target/task25a-inventory.json`.

## Kết quả kiểm thử và phạm vi phê duyệt

Bảy suite Offer chạy trên nguồn hiện tại với Maven offline, exit 0. Surefire ghi 223 test, 0 failure, 0 error, 0 skip. Chạy lại 15 ca `scratch/StylesMergeProbe.java` và 10 ca head điều kiện/public trong `target/Task25AHeadCases.java`, cả hai exit 0. `node --check scratch/task25a-browser.cjs` pass.

Tám suite web liên module trỏ vào bản sao PoC chạy 195 test, 4 failure và 1 error ở lượt đầu. Sáu suite pass gồm `MyApplicationWebTests`, `InterviewResultWebTests`, `InterviewScheduleFormWebTests`, `RequisitionControllerTests`, `JobPostingControllerTests` và `CareerPortalControllerTests`. Lỗi sidebar là resolver PoC thiếu template test `test/legacy-sidebar-probe.html`. Bản sao được bổ sung thư mục template chỉ dùng cho kiểm thử trước lượt đối chứng. Bốn assertion Application nằm ở màn recommendation và cũng fail trên baseline gốc.

Hai lượt đối chứng Application + Sidebar chạy 71 test mỗi lượt, cùng 4 failure, 0 error, 0 skip, Maven exit 1. Sidebar pass 4/4 ở cả hai lượt. Bốn test Application fail trùng tên trên baseline và PoC.

- `recommendationDetailShowsAllCvNotesAndSharedDecisionAction`
- `hrReusesSelectionWithItsOwnEndpointAndManagerContext`
- `hrRecommendationRosterShowsContactsStatesAndOnlyPendingWithdrawalWithoutSearch`
- `hrRecommendationHubLinksToExistingSelectionAndConfirmsWithdrawal`

| Hạng mục | Kết quả |
|---|---|
| ROOT CAUSE | VERIFIED |
| PROPOSED FIX | VALIDATED trong ma trận nội bộ A/B và `/jobs` A/C. Job Detail public có bằng chứng ở fixture head. |
| STYLESHEET COUNT | HR/Director Offer 13 → 7, Application 10 → 6, Candidate Offer 11 → 7, Jobs công khai 7 → 6 → 5 |
| CROSS-MODULE REGRESSION | PARTIAL. Browser đại diện pass, web suite còn đỏ như mô tả và chưa mở các màn ngoài ma trận. |
| VISUAL PARITY | PASS trên 39 cặp snapshot computed style/hình học A/B và 3 cặp A/C. Chưa có pixel diff. |
| MAVEN | FAIL ở tập web mở rộng. Bảy suite Offer PASS 223/223. |
| TEAM APPROVAL | REQUIRED |
| PRODUCTION FILES MODIFIED | 0 |
| GIT SAFETY | PASS. Tracked diff và staged diff rỗng. Hai hunk B và hunk C cùng vượt qua `git apply --check`. |

Hai tệp mới ngoài build output là báo cáo này và `scratch/task25a-browser.cjs`, đều chưa stage. Bản sao cuối trong `target/` có thêm hunk public C. Server thử trên cổng 18081 đã dừng.

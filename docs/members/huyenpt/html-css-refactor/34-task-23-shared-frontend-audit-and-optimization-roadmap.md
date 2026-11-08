# Task 23 — Rà soát frontend dùng chung của Offer Management

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](../README.md).

## 1. Baseline

Nhánh `refactor/huyenpt/offer-ui-js-to-html-css`, HEAD `e23da15e359f95dc713aee539ea35f30c2efea7b`. Trước audit, index và working tree của file tracked sạch. Git báo `?? scratch/` với 1.008 đường dẫn untracked được liệt kê riêng. Lịch sử gần nhất gồm `e23da15e` (tách asset theo vai trò), `10031e68` (dọn detail modal JS), `5e4e1fd8` (merge main), `2182c70b` và `cc6448fe` (Auth UI).

Đã đối chiếu [Task 22](32-task-22-final-commit-manifest-and-handoff.md), [Task 22A](33-task-22a-commit-reproducibility-verification.md) và lịch sử các báo cáo 26–31. Kết quả integration do người dùng xác nhận là điều kiện nghiệm thu đầu vào. Các kết quả Maven/CDP trong báo cáo trước thuộc lần chạy trước. Task 23 không chạy lại integration suite.

| Asset | Dòng dự kiến | Dòng đọc từ tệp | Byte trên đĩa | HTTP 200, byte body |
|---|---:|---:|---:|---:|
| `offer-common.css` | 1.879 | 1.878 | 39.499 | 39.499 |
| `offer-hr.css` | 864 | 864 | 17.687 | 17.687 |
| `offer-director.css` | 408 | 408 | 9.826 | 9.826 |
| `candidate-offers.css` | 1.736 | 1.736 | 34.604 | 34.604 |
| **CSS** | **4.887** | **4.886** | **101.616** | **101.616** |
| `offer-common.js` | 207 | 206 | 8.880 | 8.880 |
| `offer-hr.js` | 573 | 573 | 22.467 | 22.467 |
| `offer-director.js` | 480 | 479 | 20.083 | 20.083 |
| `candidate-offers.js` | 285 | 285 | 10.781 | 10.781 |
| **JS** | **1.545** | **1.543** | **62.211** | **62.211** |

Đếm bằng `File.ReadAllLines`; một phần tử rỗng do tách chuỗi ở newline cuối sẽ làm phép đếm kiểu split lớn hơn. Số dòng thực tế ở trên được lấy trực tiếp, không suy từ báo cáo cũ. Flash toast CSS/JS dùng chung trả 200, lần lượt 4.423 và 2.497 byte. Đây là số byte body từ GET trực tiếp, tách biệt với transferred bytes, compression và cache của trình duyệt.

## 2. Quy ước và đối chiếu

| Khu vực | Quy ước và cách Offer đang dùng |
|---|---|
| Token, reset và catalog | `tokens.css:7–109` import fonts và khai báo palette, typography, spacing, radius, shadow. `global.css` giữ nền và tiện ích. `components.css` cung cấp nút, badge, form control, card, bảng, dialog. `design-tokens.css` và `app-layout.css` là tài sản cũ không nằm trong chuỗi tải Offer đã đo. |
| Layout | `base.html:19–38` cung cấp CSS nền, CSRF meta, `interface.js`; `internal.html:4–48` thêm workspace, candidate shell theo vai trò, sidebar/header hoặc candidate topbar/footer. `workspace.css:1` import topbar CSS. Offer dùng `layout:decorate` và `th:replace` cho fragment nội dung và shell. |
| CSS đặc thù | Internal Offer có common, HR và Director. Candidate có `cof-` và cặp CSS/JS dành cho hai màn portal. Application dùng `pages/applications.css`; Interview có common/workspace và CSS màn cũ; Requisition dùng shared/form; Job Posting có shared/list và management-filters. Các cấu trúc cùng tồn tại theo phạm vi tái sử dụng. |
| Script nền | `interface.js` xử lý shell, `topbar-notifications.js` xử lý thông báo topbar và CSRF riêng trong IIFE. `global.js:5–70` dùng menu cũ với guard listener, `auth.js` dành cho xác thực, `status-confirmation.js` xử lý xác nhận submit form theo markup riêng. Ba script sau không có trong DOM Offer đo được. |
| Script trang | Các template Application, Interview, Requisition và Job Posting đều khai báo script theo trang với `defer`, thường có `flash-toast.js`. Offer internal khai báo common trước các script vai trò; HR dùng guard readyState; Director chủ yếu cung cấp hàm cho handler; Candidate dùng IIFE chạy khi defer. |
| Dialog | Native `dialog.confirm-dialog` cho xác nhận gửi và phản hồi ứng viên; custom overlay còn dùng ở Excel và quyết định nhanh Director. `status-confirmation.js:1–42` giữ pending form, không đảm nhiệm JSON hoặc tải tệp. |
| Toast | Flash server render qua `fragments/ui/flash-toast.html` rồi bind với `pages/flash-toast.js`. Toast động của Offer xây markup riêng cùng bộ CSS và có luồng sessionStorage qua reload. |
| Validation và CSRF | Form Offer giữ binding Thymeleaf, Bean Validation backend, ràng buộc số lương/ngày và đếm ký tự ở JS. Các thao tác AJAX nội bộ lấy CSRF meta; Candidate dùng POST form Thymeleaf. Requisition có `text-limit.js` và JS form riêng. Đồng bộ cơ chế validation chỉ có giá trị khi hợp đồng UX giống nhau. |

Tham chiếu quy chuẩn [Architecture Guide](../../../architecture/ARCHITECTURE_GUIDE.md), [UI Rules](../../../architecture/UI/UI_RULES.md), [UI CSS Guide](../../../architecture/UI/UI_CSS_GUIDE.md).

## 3. Ma trận tải asset

Ký hiệu theo thứ tự DOM. **B** = `tokens.css → global.css → components.css`; **W** = `workspace.css` (import `topbar.css`); **K** = `candidate-shell.css`; **F** = `pages/flash-toast.css`; **C/H/D** = CSS Offer common/HR/Director; **P** = `pages/candidate-offers.css`. `tokens.css` import `fonts.css`. **G** = STOMP CDN → `topbar-notifications.js` → `interface.js`. **A** = `pages/admin-workspace.js` qua fragment `admin-workspace :: scripts`; **T** = `pages/flash-toast.js`; **c/h/d/p** = JS Offer common/HR/Director/Candidate. Các JS nội bộ khai báo defer. Topbar đưa CDN và script thông báo vào trước chuỗi script chân trang.

| Template | Vai trò | CSS theo thứ tự | JS theo thứ tự | Phụ thuộc / trạng thái xác minh |
|---|---|---|---|---|
| `list.html:8–12,501–507` | HR | B → C H F → W → C H F → C H F | G → A → T → c → h → d | Live. Excel, chọn dòng, lọc, gửi. d được tải nhưng không có opener Director trong DOM HR. |
| `form.html:8–12,285–290` | HR | B → C H F → W → C H F → C H F | G → A → T → c → h | Live với Create; Edit dùng cùng template theo controller, chưa đo live. |
| `detail.html:7–11,484–489` | HR / Director | B → C D F → W → C D F → C D F | G → A → T → c → d | Live cả hai vai trò. Common phục vụ gửi; d phục vụ comment/confirm Director. |
| `director-list.html:8–12,278–283` | Director | B → C D F → W → C D F → C D F | G → A → T → c → d | Live. Có 2 opener Director trên dữ liệu đã quan sát; d cần cho bộ lọc, modal và quyết định. |
| `candidate-list.html:17–20,210–213` | Candidate | B → P F → W K → P F → P F | G → T → p | Live. Dialog và form POST riêng. Không tải common/internal Offer. |
| `candidate-detail.html:21–24,369–372` | Candidate | Nguồn khai báo B, W, K, P, F. Dự kiến cùng thứ tự Candidate List, cần xác nhận số lần lặp live. | Nguồn khai báo G → T → p | Static. Render chi tiết có thể đánh dấu thông báo đã đọc. |

Các thẻ stylesheet cấp trang xuất hiện **ba lần** trên cả năm màn đã render. Không thấy script lặp trong các DOM đó. Nguồn mỗi template chỉ khai báo một bộ link. Việc cả `base.html:24`, `internal.html:11` và trang con đều có fragment `styles` là giả thuyết cần tái hiện khi tìm nguyên nhân merge head. Xóa các bản sao ở vị trí sai có thể đổi thứ tự so với `workspace.css` và `candidate-shell.css`. Thiếu asset Offer chưa được phát hiện trên các trang đo; trạng thái CDN/transfer theo từng request chưa được xác minh.

HR list có nhánh opener Director tại `list.html:246–253`, bị loại khi role HR. `OfferController.java:80–121` trả hẳn `director-list` cho Director. Modal HTML tại `list.html:438–477` vẫn hiện trong DOM với `display:none`, giữ bốn handler gắn với d, nhưng số opener render là 0. Việc bỏ riêng script sẽ để lại handler mồ côi; checkpoint an toàn phải bỏ cả nhánh và modal khỏi template HR.

Shared detail có card và nút cuộn chỉ cho Director ở trạng thái pending (`detail.html:89–94,342–393`); dialog Director ở 424–459 được render ngoài điều kiện. HR detail pending không có card nhưng vẫn tải CSS/JS Director. Director CSS còn cần đối chiếu selector dùng chung, trạng thái có lịch sử, các dialog đóng, focus và nút gửi trước khi cân nhắc tải có điều kiện. Giữ tải hiện tại cho detail.

Candidate vẫn dùng cùng `layout/internal` nhưng nhánh shell và hành vi POST khác. Hai asset ở `pages/` là một nhóm độc lập hợp lý, không phụ thuộc các asset nội bộ. Di chuyển tệp chỉ đổi URL/import và cache key, không giảm logic.

## 4. CSS, cascade và specificity

Specificity ghi theo (ID, class/attribute/pseudo-class, type/pseudo-element); mỗi nhánh selector dấu phẩy được tính riêng. Số đo bên dưới phân biệt DOM thật với suy luận.

| ID | Nguồn và selector | Đối chiếu, cascade, hiệu quả gỡ |
|---|---|---|
| C01 | Các link trong ma trận mục 3 | **EXACT DUPLICATE** ở cấp stylesheet DOM. 3 bản cùng URL và nguyên declarations, cùng specificity. Có thể thay đổi precedence nếu chỉ giữ bản trước W/K. Chưa đo CPU, transfer hoặc so sánh toàn bộ computed style sau khi rút link. |
| C02 | `offer-common.css:22–73` `.status-badge`, `.status-pending` và các trạng thái (0,1,0) | **SEMANTIC DUPLICATE** với `components.css:113–160` `.badge`, `.badge--*` (0,1,0), trên class khác. Common tải sau catalog; badge nội bộ dùng gap 4px, padding 3px 8px, radius 4px, font 11.5px, pending có viền và palette riêng. Catalog dùng radius full, padding 6px 14px và token màu. Live pending nền `rgb(254,248,238)`, radius 4px. Candidate list dùng `.badge.cof-badge` + trạng thái từ DTO; fixture đã mở không có badge render. Đổi class/gỡ rule sẽ đổi hình thức. |
| C03 | `candidate-offers.css:11–16` `dialog::backdrop` (0,0,2), `.confirm-dialog::backdrop` (0,1,1) | **CASCADE CONFLICT** với `components.css:494–498` `dialog.confirm-dialog::backdrop` và `dialog.dialog::backdrop` (0,1,2). Cả hai khai báo background và backdrop-filter. Global `.45 / blur(2px)` thắng Candidate `.55 / blur(4px)` dù Candidate tải sau. Candidate List live xác nhận giá trị computed trên `#cof-dialog` khi đóng. Selector rộng còn có thể chạm dialog shell hoặc dialog không mang class catalog. Giữ tới khi kiểm tra tất cả dialog ở trạng thái mở và chốt thiết kế. |
| C04 | `offer-common.css:110–145,171–180` `.modal-overlay`, `.modal-box`, `.modal-overlay.active` (0,1,0 hoặc 0,2,0); `offer-director.css:141` `#directorDecisionModal .modal-box` (1,1,0) | **ROLE-SPECIFIC STYLE** dùng fixed overlay, z-index 1200, 620px và `popInModal`; Director có rule kích thước riêng. Catalog native `dialog.confirm-dialog` (0,1,1) có 480px, top layer và backdrop riêng. HR Excel và Director quick decision dùng div, HR Send và Detail Confirm dùng native. Giống mục đích, khác focus, Escape và cách hiển thị. |
| C05 | `offer-common.css:257–315` `.btn-icon`, biến thể màu, hover (0,1,0 / 0,2,0) | **ROLE-SPECIFIC STYLE** với icon thao tác trong bảng. `components.css` định kiểu nút chuẩn; cùng dùng token radius và màu ở một số chỗ nhưng khác kích thước và trạng thái. Nút chính/phụ đã đo 8px, 48px; form có nút mẫu đầu 40px. Không thay icon actions bằng nút chuẩn theo tên. |
| C06 | `offer-common.css:1861–1878` `.input-control` (0,1,0), focus (0,2,0) | **SEMANTIC DUPLICATE** với `components.css:190–214` `.form-control` và focus. Common đặt height 40px, padding ngang, ring brand qua color-mix. Controls catalog có hợp đồng focus, invalid, readonly và disabled. Input đã đo trên Create dùng catalog 48px; textarea Director detail là selector khác, 6px radius. Chưa chứng minh đổi class input-control giữ behavior trên export/modal. |
| C07 | `offer-common.css:690,758–786` `.ofw-list-surface .adw-data-table thead th` (0,2,2); `candidate-offers.css:1042,1233,1379,1489` `.cof-*.card` (0,2,0) | **INTENTIONAL OVERRIDE** theo cấu trúc DOM. Header/cell bảng Offer có nền, spacing riêng, card portal có layout thư mời riêng. Catalog `.table th` (0,1,1) và `.card` (0,1,0) chỉ định baseline. Các selector độ đặc hiệu cao hơn có thể thắng khi cùng match. Chưa đo computed card trên Candidate Detail. |
| C08 | `offer-common.css:33–73,110–145`, `candidate-offers.css:11–16`, token tại `tokens.css:10–109` | **SHARED STYLE CANDIDATE** ở mức giá trị thiết kế. Có màu literal và fallback hex, spacing 4/8px hoặc radius và shadow đã dùng token. Fallback cùng giá trị tại một theme không đảm bảo mọi trạng thái hoặc shell. Thay token hàng loạt có thể đổi palette; không có đề xuất gỡ trong Task 24. |
| C09 | `offer-common.css:489,868,923`, `offer-hr.css:830–864`, `offer-director.css:404–408`, `candidate-offers.css:945,1561–1624,1724–1736` | **ROLE-SPECIFIC STYLE**. Trùng breakpoint 768px/640px ở một số nhóm nhưng các rule nhắm filter, selection toolbar, decision hoặc thư mời. CSS theo vai trò tải sau common; thiếu cặp selector và declaration tương đương để gộp. Không thấy overflow cấp document tại các kích thước đo. |
| C10 | `candidate-offers.css:976–1031` print; `offer-common.css:171,1145` keyframes; `offer-hr.css:205`; `flash-toast.css:133,145` | **ROLE-SPECIFIC STYLE**. Print của thư mời gắn `window.print`; animation overlay, spinner, toolbar và tiến trình toast có thời điểm và mục đích khác nhau. Giữ; print preview chưa kiểm chứng live. |
| C11 | `flash-toast.css:1–158` và fragment flash trong sáu template | **EXACT DUPLICATE** chỉ ở ba link cùng file trong năm DOM; hai nguồn markup toast dùng cùng catalog CSS. Không có cơ sở xóa stylesheet hay gộp toast vào Offer. |

Các phép đo trên chỉ đọc style hiện hành, không tháo stylesheet khỏi browser. Các selector khác chưa đủ bằng chứng để gọi là dead hoặc ước tính số dòng CSS có thể xóa.

## 5. JavaScript, global và khởi tạo

| ID | Nguồn / phạm vi | Call site, side effect và đánh giá |
|---|---|---|
| J01 | `offer-common.js:98–117` `checkOfferPendingFlashToast` và `flash-toast.js:40–52` | **DUPLICATE EVENT INITIALIZATION / broken dependency**. Cùng đọc và xóa `pendingFlashToast`. Trong DOM, T chạy trước c. T xóa key trước khi kiểm tra `window.showFlashToast`; renderer chỉ xuất hiện từ c. Khi có pending key từ send (`common:177`), delete (`hr:84`), hoặc quyết định (`director:190,429`), script chung có thể làm mất toast sau điều hướng. Thứ tự đã thấy live, hậu quả suy ra trực tiếp từ code; chưa replay payload hoặc thực hiện thao tác ghi. Tạo regression bằng storage tổng hợp trong fixture riêng. |
| J02 | `offer-common.js:81–95` dismiss và listener trong `showFlashToast`; `flash-toast.js:12–38,54–55` | **SHARED HELPER CANDIDATE**. Cùng thêm `is-leaving`, transitionend với fallback 350ms, bỏ wrapper trống. Một nhánh bind toast động vừa append; nhánh shared bind toast server, có guard `_toastBound`, API `window.initFlashToasts(root)`. Có thể dùng API hiện hữu cho toast động, giữ renderer, icon, role và escape. Tránh quét ngoài wrapper hoặc bind lần hai. |
| J03 | `offer-common.js:18–38` `getCsrfInfo`, `escapeOfferHtml`; `topbar-notifications.js:7–15,44–50` | **INTENTIONAL MODULE-SPECIFIC LOGIC**. Cùng nguồn meta nhưng Offer trả token/header, topbar tạo map header dùng cục bộ trong IIFE. Escape phục vụ sink khác. Send, HR delete/export và Director fetch dùng Offer helper. Không có global collision giữa hai scope. |
| J04 | `offer-director.js:134–208,401–455` decision fetch; `offer-common.js:129–195` send; `offer-hr.js:57–99,401–515` delete/export | **SHARED HELPER CANDIDATE**, giữ hiện tại. Các nhánh khác method, Content-Type, parsing JSON/blob, redirect/reload, khóa nút, toast và nội dung lỗi. Hai đường Director có UI entry và hậu điều hướng khác nhau. Không gom wrapper chỉ vì cùng fetch; chưa có số dòng gỡ an toàn. |
| J05 | `candidate-offers.js:12–221` chip lý do, list/detail dialog, khóa submit và pageshow; `status-confirmation.js:1–42`; `auth.js:104–158` | **INTENTIONAL MODULE-SPECIFIC LOGIC**. Candidate gắn listener theo data attribute, chuyển nội dung Accept/Decline, chép lý do, mở native dialog và phục hồi nút khi Back. Shared confirmation làm việc với pending form riêng; Auth có trạng thái submit và pageshow riêng, không tải trên Offer. Candidate pageshow dùng selector rộng `button[type=submit]`, cần kiểm tra shell trước khi thu hẹp. |
| J06 | `offer-hr.js:187–330` lương/ngày; `offer-director.js:95–112,281–294,336–369` ký tự/lý do; `candidate-offers.js:226–282` countdown/print | **KEEP UNCHANGED**. HR tính 85%, ngày bắt đầu theo kỳ hạn, Director chặn từ chối thiếu lý do, Candidate cập nhật đồng hồ và in. Format thời gian thông báo ở topbar là timestamp hiển thị, không thay được countdown hoặc logic ngày form. Template format số tiền qua `#numbers`; quy đổi giá trị input không tương đương format hiển thị. |
| J07 | `offer-common.js:201–206`, `offer-hr.js:549–573`, `offer-director.js:464–479` | **GLOBAL NAME CONFLICT — NOT ENOUGH EVIDENCE** về xung đột runtime. Các script classic công khai tên phục vụ inline handlers. Tên như `showToast`, `clearSearchInput` có phạm vi rộng, nhưng chưa thấy lỗi console hoặc hai script trang khác cùng khai báo trong DOM đã đo. `global.js` là IIFE và không có trong ma trận. Không đổi namespace hay xóa export khi chưa lập danh sách caller và kiểm tra runtime. |
| J08 | `offer-director.js:19–208,464–479`; `list.html:246–253,438–477,506` | **DEAD HELPER CANDIDATE** chỉ xét đường tải HR List. Hàm vẫn cần trong Director List và Detail. Bỏ phần include cùng markup không có opener trên HR, giữ nguyên tệp và toàn bộ hàm Director. |

Các handler inline trong template là phụ thuộc thật. Cùng dùng sự kiện submit, close hoặc pageshow không đủ để kết luận trùng listener. Không có số đo listener theo node trong lượt này; console sạch trên các trang đã mở không chứng minh số lần bind hay sự vắng mặt của global conflict.

## 6. Kiểm chứng trình duyệt chỉ đọc

Dùng phiên local ở cổng 8080 và tài khoản fixture sẵn có. Đọc stylesheet/script trong DOM, computed style và error console. Mỗi hàng live được đo ở 1440, 768, 390px. Các giá trị width bên dưới là `documentElement.scrollWidth`; chênh 15px phù hợp phần scrollbar, không thấy overflow cấp document trong các mẫu. Không suy ra mọi container con đều vừa chiều ngang.

| Màn | Width document tại 1440 / 768 / 390 | Quan sát |
|---|---|---|
| HR List | 1425 / 753 / 375 | Nút mẫu 48px, radius 8px; badge 4px; input 42px; native backdrop .45/blur 2px; Director overlay đóng, không có opener. |
| HR Create | 1425 / 753 / 375 | Nút mẫu 40px, radius 8px; input 48px, radius 8px. |
| HR Edit | Chưa đo | HR List được xem không render link Edit; chưa xác minh fixture Draft có thể mở. |
| HR Detail pending | 1425 / 753 / 375 | Nút mẫu 48px/8px; badge 4px; không có decision card; backdrop .45/blur 2px. |
| Director List | 1440 / 753 / 375 | Hai opener trên hàng pending, quick modal có trong DOM; input mẫu 42px/8px; badge 4px. |
| Director Detail pending | 1425 / 753 / 375 | Có decision card, textarea radius 6px và cao khoảng 84px; confirm backdrop .45/blur 2px. |
| Candidate List | 1440 / 768 / 375 | Shell Candidate, confirm backdrop computed .45/blur 2px dù CSS trang có .55/blur 4px; mẫu tài khoản không có thẻ offer đang hiển thị. |
| Candidate Detail | Chưa mở | GET gọi `CandidateOfferServiceImpl.java:75–86` → `markOfferNotificationRead`; có khả năng ghi notification của fixture. Cần fixture cô lập hoặc xác minh notification đã đọc trước. |

Error console trả rỗng trên năm màn đã đo. HTTP GET trực tiếp xác nhận các asset cục bộ trong mục 1. Trình duyệt không cung cấp Resource Timing qua scope đọc đang dùng, nên chưa xác minh transfer/cache/CDN hoặc mọi network failure theo màn. Giá trị backdrop là computed style của pseudo-element trên dialog đóng. Chưa kiểm thử focus trap, Escape, print, listener count hoặc unexpected globals ở runtime. Không gửi POST nghiệp vụ hay ghi đè baseline ảnh.

## 7. Ưu tiên, định lượng và phạm vi

| ID | Source | Lines | Related shared source | Finding | Evidence | Recommendation | Risk | Estimated removable lines |
|---|---|---|---|---|---|---|---|---:|
| P0-01 | `offer-common.js` | 98–117 | `pages/flash-toast.js:40–52` | Pending toast có hai consumer, renderer đến muộn | Thứ tự DOM T → c và thao tác xóa key trước guard | Xác minh bằng storage tổng hợp, đưa renderer lên trước T và giữ một consumer; xác nhận toàn bộ internal template | Trung bình, thay đổi thứ tự defer | Tối đa 20 JS nếu bỏ block consumer Offer sau khi xác nhận shared xử lý đủ |
| P1-01 | `offers/list.html` | 246–253,438–477,506 | Không cần shared | Tải d và giữ modal Director trong HR List | Controller phân view; DOM HR có 0 opener, Director List có 2; selector modal trong d không có init tự mở | Bỏ cả opener branch, modal và link script ở HR List | Thấp | 49 HTML, 0 JS source |
| P1-02 | `offer-common.js` | 81–95 | `pages/flash-toast.js:12–38,54` | Dismiss và bind toast động trùng lifecycle | Cùng markup/data hooks và fallback 350ms. Shared API có guard bind. | Gọi shared binder trên wrapper sau append, giữ renderer riêng | Thấp nếu sửa sau P0-01 và kiểm tra tải | Khoảng 14 JS (15 dòng thay bằng 1) |
| P2-01 | Sáu template Offer | Fragment styles ở mục 3 | `layout/base.html:24`, `layout/internal.html:11` | 2 bản dư mỗi URL trong 5 DOM | 9 link cấp trang thay vì 3 ở màn internal; 6 thay vì 2 ở Candidate List | Tái hiện cách merge, thử giải pháp Offer-only trong checkpoint riêng; giữ bản sau W/K hoặc chứng minh cascade tương đương | Trung bình, có thể đổi precedence | 0 dòng source đã chứng minh; giảm 6/4 node runtime nếu giữ 1 bản mỗi URL |
| P3-01 | Candidate backdrop, internal badges, Director Detail bundle, modal và breakpoint | C02–C10, mục 3 | Catalog, tokens, workspace | Override hoặc scope khác | Specificity, markup và computed khác; thiếu fixture trạng thái còn lại | Giữ | Rủi ro đổi UX vượt lợi ích | 0 |

**Hai ứng viên cleanup P1** có bằng chứng ở mức nguồn và đường tải. P0-01 là lỗi thứ tự có thể sửa độc lập sau khi tái hiện. P2-01 là tối ưu runtime cần kiểm chứng, không tính trong hai cleanup an toàn. P1-01 tránh khai báo một asset 20.083 byte trên HR List, còn byte truyền thực tế tùy cache, compression và lần điều hướng.

| Phạm vi | Chủ sở hữu | Ảnh hưởng và duyệt |
|---|---|---|
| `templates/offers/list.html`, `offer-common.js`, và các block script của bốn template internal | HuyenPT, Offer | P0/P1 có thể giới hạn ở Offer. Giữ API shared hiện hữu và không sửa Application, Interview, Requisition hay Job Posting. Không cần duyệt từ người phụ trách file shared cho hướng này. |
| Sáu template Offer, block styles | HuyenPT, Offer | Thử giải pháp dedup chỉ trên Offer trước. Phải kiểm tra lại mỗi role và cascade; nếu cần đổi layout, tách sang quyết định của Lead. |
| `layout/base.html`, `layout/internal.html` | Lead Architect | **Cần duyệt Lead** nếu xử lý ở nguồn layout. Tất cả module nội bộ kế thừa, và base còn phục vụ các layout khác. |
| `components.css`, `tokens.css`, `workspace.css`, `pages/flash-toast.js` | Shared frontend / người phụ trách tài sản chung | Cần phối hợp với chủ sở hữu nếu thay component/utility hiện hữu. Không có chỉnh sửa shared trong lộ trình mặc định. |

## 8. Lộ trình Task 24 đề xuất

1. **Xác minh và sửa pending toast.** Kiểm tra key tổng hợp trước reload trong fixture, ghi nhận toast bị mất, sau đó đưa `offer-common.js` lên trước `flash-toast.js` ở `list.html`, `form.html`, `detail.html`, `director-list.html`, bỏ `checkOfferPendingFlashToast` nếu shared consumer cho đúng một toast và xóa key. Giữ các producer, kiểm tra success/error, malformed payload, reload và redirect, flash server và đóng thủ công. So ảnh/toast trên desktop và mobile. Rủi ro trung bình, tối đa 20 dòng. Rollback riêng thứ tự script và consumer. Không cần teammate duyệt nếu chỉ sửa Offer.
2. **Giảm tải Director ở HR List.** Chỉ sửa `templates/offers/list.html` tại ba vùng P1-01. Kiểm tra HR filter, export dialog, chọn dòng, dialog gửi và nút xóa khi có fixture Draft mà không submit. Kiểm tra Director List vẫn mở hai quyết định và bắt lý do, Director Detail vẫn hiển thị card. So hình HR ở 1440/768/390 và so asset DOM. Rủi ro thấp, 49 dòng HTML. Rollback commit của template. Không cần sửa shared.
3. **Dùng binder toast chung.** Sau checkpoint 1, sửa `showFlashToast` trong `offer-common.js` để bind toast động qua `window.initFlashToasts(container)`. Test nhiều toast, escape text, role/status, đóng bằng tay, animation, hover và reduced motion. Xác nhận đúng một lần bind/close; so bề ngoài toast. Rủi ro thấp, khoảng 14 dòng JS. Rollback commit hàm renderer. Không cần sửa shared.
4. **Khoanh nguyên nhân link CSS lặp.** Thử trên fixture/thay đổi có thể revert chỉ trong block styles của sáu template Offer. Điều kiện nhận là 1 link mỗi URL, cùng precedence với W/K, không đổi computed style ở ba viewport và khi mở dialog; cần có Draft, Pending Director và Candidate Detail cô lập. Rủi ro trung bình, chưa cam kết giảm source. Nếu phải đổi hai layout chung, dừng checkpoint Offer-only và chuyển Lead xem xét ảnh hưởng các module. Rollback riêng block template đã thử.

Giữ cấu trúc 4 CSS + 4 JS. Badge, backdrop, CSS role, quy tắc in, lương/ngày, countdown và luồng POST Candidate nằm trong nhóm giữ nguyên. Các thử nghiệm future cần dùng fixture cô lập cho thao tác có khả năng cập nhật dữ liệu.

## 9. Giải thích khi bảo vệ SWP391

Em tách frontend Offer theo nơi sử dụng. HR và Director có phần hiển thị chung nhưng khác hành động. Ứng viên dùng cùng hệ thống layout qua nhánh Candidate và có JS cho dialog, thời hạn phản hồi và bản in. Em kiểm tra DOM render theo từng vai trò vì nhìn tên file không cho biết selector nào thắng hoặc handler nào có đường gọi. Ví dụ, Candidate khai báo backdrop đậm hơn nhưng trình duyệt lấy rule catalog có specificity cao hơn. Danh sách HR lại tải mã Director vì còn markup modal cũ, trong khi đường mở modal chỉ có ở view Director. Phần có thể dọn được khoanh vào file Offer và có kiểm thử hồi quy riêng.

## 10. Trạng thái

- **CSS AUDIT — PARTIAL.** Có bằng chứng cascade và CSS lặp trên năm màn, còn thiếu dialog mở, toàn bộ selector computed và Candidate Detail/Edit.
- **JAVASCRIPT AUDIT — PARTIAL.** Đã lần caller, globals và thứ tự defer; chưa tái hiện toast pending qua storage tổng hợp hoặc đếm listener.
- **TEMPLATE LOADING AUDIT — PARTIAL.** Đọc đủ sáu template và hai layout; chưa kiểm tra live Edit và Candidate Detail.
- **SHARED ASSET CONFLICTS — FOUND.** CSS cấp trang lặp và backdrop bị thắng specificity đã quan sát. Xung đột consumer toast xác định từ thứ tự tải thực tế và code; biểu hiện mất toast chưa replay trong Task 23.
- **SAFE OPTIMIZATION CANDIDATES — 2.** P1-01 và P1-02.
- **TEAMMATE APPROVAL REQUIRED — NO** cho P0/P1 Offer-only. **YES** nếu P2 cần sửa `layout/base.html` hoặc `layout/internal.html`, hoặc thay tiện ích dùng chung đã nêu.
- **TASK 24 RECOMMENDATION — PROCEED** sau khi người dùng duyệt, bắt đầu bằng tái hiện P0. Task 23 chỉ tạo báo cáo.

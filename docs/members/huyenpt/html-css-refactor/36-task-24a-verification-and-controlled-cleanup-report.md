# Task 24A — Kiểm chứng và sửa có kiểm soát luồng Offer

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](../README.md).

## 1. Baseline

Đã đọc toàn bộ báo cáo [Task 23](34-task-23-shared-frontend-audit-and-optimization-roadmap.md) và [Task 24](35-task-24-toast-reliability-and-safe-cleanup-report.md), kiểm tra bốn JS, bốn CSS, sáu template, script toast dùng chung và route theo vai trò. Nhánh `refactor/huyenpt/offer-ui-js-to-html-css` ở `e23da15e359f95dc713aee539ea35f30c2efea7b`. Khi bắt đầu, index và diff tracked rỗng. Hai báo cáo trước và `scratch/` đã untracked. Các tệp cũ trong `scratch/` được giữ nguyên.

## 2. Maven — gỡ chặn mà không thay dependency

Maven offline với `-e` trong sandbox dừng ở `maven-compiler-plugin:3.14.1:testCompile`, với chuỗi nguyên nhân:

```text
Fatal error compiling: ...\org\eclipse\angus\jakarta.mail\2.0.5\jakarta.mail-2.0.5.jar
Caused by: org.codehaus.plexus.compiler.CompilerException
Caused by: java.nio.file.AccessDeniedException
  WindowsPath.toRealPath → ZipFileSystemProvider.removeFileSystem
  → ZipFileSystem.close → JavacFileManager.close
```

`org.eclipse.angus:jakarta.mail:jar:2.0.5` tồn tại, 727.296 byte, đọc được danh mục bằng `jar tf`. POM có 12.509 byte. Checksum của cả hai tệp khớp sidecar trong cache.

| Tệp | SHA-1 |
|---|---|
| JAR | `68ce3f8ddb70d375e2ae4d820ba64da0ed56a940` |
| POM | `b67b46644b41670b384d92c6b01121c47fd5eb07` |

Lỗi xảy ra lúc javac đóng archive và Windows phân giải real path trong sandbox. Không có lỗi tải từ repository từ xa, thiếu POM, JAR hỏng hay class-version mismatch trong stack trace. Lệnh đã được duyệt chạy ngoài sandbox, cùng cache và cùng dependency, vượt qua bước compile. Không thay `pom.xml`, xóa cache hoặc tải bản khác.

```powershell
mvn -o "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository" "-Dtest=OfferIntegrationTests,OfferServiceTests,CandidateOfferWebTests,CandidateOfferServiceTests,OfferExportServiceTests,OfferControllerBugTests,CandidateOfferQueryTests" test
```

## 3. J01 — tái hiện Chrome và sửa

Fixture mới `scratch/task24a-toast-browser.cjs` phục vụ trực tiếp production `flash-toast.js`, `offer-common.js` và CSS toast trên loopback. Script là classic `defer`, shared trước Offer như bốn template nội bộ. Một profile Chrome tạm, sessionStorage tổng hợp, DOM thật. Fixture không gọi API gửi/xóa/quyết định. Bản chạy đầu dùng Chrome dump-DOM để tái hiện mất toast. Bản CDP sau dùng thời gian thực để đo vòng đời animation.

Trace baseline với payload hợp lệ: tại `readyState=interactive`, shared đọc `pendingFlashToast` khi `showFlashToast` và `initFlashToasts` đều undefined, rồi xóa key. Khi Offer Common đọc, hai global đã là function nhưng key rỗng. DOM có 0 client toast. Với server và pending cùng lúc, DOM chỉ có server toast. **CONFIRMED BROWSER BUG.**

| Ca | Baseline | Sau J01/J02 |
|---|---|---|
| Không có payload | 0 | 0 |
| Success / error / warning / info | 0 cho từng loại | 1 cho từng loại, đúng text và class |
| JSON lỗi / thiếu message | 0, storage rỗng | 0, storage rỗng |
| Server toast | 1 | 1, visible |
| Server + client | 1, chỉ server | 2, mỗi nội dung một lần |
| Payload HTML | Bị mất cùng lỗi J01 | 1 error toast, text literal, không có node `img` |
| Reload | Không có toast lần đầu và lần sau | 1 lần đầu, 0 sau reload |
| Hai toast động | Cùng render | Cùng render, đóng tay còn toast thứ hai, sau animation còn 0 |
| Gọi initializer lại | Dynamic từ 1 lên 2 listener cho mỗi hook | Mỗi hook giữ 1 listener |

Sau sửa, shared đọc key chung rỗng. Offer đọc và xóa key riêng khi renderer đã có sẵn. Không có lỗi JS qua listener `error` hoặc `unhandledrejection` trong các frame chạy fixture CDP cuối. Lần thử Chrome trong sandbox dừng ở GPU; lần chạy ngoài sandbox có thể ghi cảnh báo registry của Chrome, tách khỏi lỗi JavaScript trang.

**Phương án A** đổi thứ tự defer. Cần chỉnh cả bốn template và điều phối việc khởi tạo renderer, consumer, binder shared. Đảo thứ tự một mình còn cho phép hai đường binding cùng tác động. **Phương án B** giữ thứ tự, dùng một hằng `OFFER_PENDING_TOAST_KEY` và một reader của Offer. Chọn B vì thay đúng ba JS Offer, không thay loader hay hợp đồng flash server. Cập nhật đủ bốn writer cho Send, Delete, Director List và Director Detail. Giữ nguyên payload, điều hướng, escape, trạng thái và CSRF. Payload cũ đang chờ từ tab trước khi triển khai không được di chuyển sang key mới.

## 4. J02 — dùng lại initializer

`showFlashToast` giữ renderer, icon, text escaping và `role`. Sau append gọi `window.initFlashToasts(container)`. Bốn trang nội bộ vẫn tải shared trước Offer Common. Root là wrapper chứa toast, `_toastBound` ngăn bind lại từng node. Shared xử lý close click, animationend, transitionend và fallback 350ms. CDP đo hai toast động đều visible, click đóng toast thứ nhất, toast thứ hai tự đóng và wrapper bị gỡ. Sau hai lần init lại, số listener ở close/progress vẫn là 1/1 cho mỗi toast. Server và client đồng thời có một listener cho mỗi hook. Không sửa shared script.

## 5. J08 — HR List

Trước sửa, live HR List không có opener nhưng có modal ẩn và script Director. Controller đưa Director sang `director-list.html`. Không thấy caller Director từ JS HR hoặc common. Chỉ bỏ nhánh opener dành riêng cho Director, modal ẩn, include script và comment/khoảng trắng đi kèm trong `offers/list.html`. Giảm 56 dòng HTML. `director-list.html`, `detail.html` và hàm phê duyệt/từ chối giữ nguyên. Hai thay đổi ở `offer-director.js` chỉ thuộc J01.

Sau sửa, CDP xác nhận HR List không còn modal, opener hoặc request Director JS. Excel dialog vẫn mở/đóng. Director List vẫn tải JS, opener mở decision modal. Director Detail pending vẫn render decision card. Resource Timing ở HR List trước cleanup ghi một entry Director JS với 20.091 byte encoded body và 20.391 byte transfer; sau cleanup không có entry. Đây là số đo ở lần điều hướng cụ thể, không phải mức tiết kiệm toàn hệ thống.

## 6. CSS lặp — phạm vi nhiều module, nguyên nhân vẫn PARTIAL

HR List vẫn có 13 stylesheet trong DOM, ba link cho mỗi URL CSS cấp trang, cùng thứ tự đã ghi ở Task 23. Candidate List có 11. Trang Application `/applications` cũng lặp ba link cho `applications.css` và ba cho `flash-toast.css`, xác nhận phạm vi vượt Offer. Chrome ghi một entry Resource Timing cho mỗi URL cấp trang lặp trong lần tải mẫu. Ví dụ `offer-common.css` có encoded body 39.499 và transfer 39.799 byte; `applications.css` có 25.984 và 26.284 byte. Các lần đổi viewport cùng navigation không tạo thêm lượt tải stylesheet. Chưa thực hiện phép đo cache bật/tắt có kiểm soát, nên kết quả chỉ áp dụng cho lượt tải đã quan sát.

Đã đối chiếu hai slot `styles` trong `base.html` và `internal.html`, thứ tự head merge và template lá. Fixture Java tối giản dùng dialect thực từ test classpath không tái hiện ba link với nested decorate và hai slot. Do đó hai slot là đầu mối, chưa đủ để khẳng định chính xác vì sao có ba bản. Cần tái hiện với cấu hình Spring và head gần production hơn, rồi thử một điểm ghép style có precedence sau workspace/candidate shell. Giữ nguyên các link và shared CSS trong Task 24A. Sửa `layout/base.html` hoặc `layout/internal.html` thuộc thay đổi liên phân hệ cần Lead duyệt.

## 7. Hồi quy và viewport

Maven **PASS** ở baseline, sau J01 và sau bản cuối J02/J08. Mỗi lần chạy đủ **223 test, 0 failure, 0 error, 0 skip**. Đối chiếu Surefire XML theo từng suite.

| Suite | Tests |
|---|---:|
| `CandidateOfferWebTests` | 19 |
| `OfferControllerBugTests` | 6 |
| `OfferExportServiceTests` | 10 |
| `OfferIntegrationTests` | 34 |
| `CandidateOfferQueryTests` | 5 |
| `CandidateOfferServiceTests` | 35 |
| `OfferServiceTests` | 114 |

Cả bốn Offer JS vượt qua `node -c`. CDP mở HR List, HR Create, shared Detail dưới HR, Director List, shared Detail dưới Director và Candidate List ở 1440/768/390px. Bề rộng document không vượt viewport ở các mẫu. So với baseline HR cùng mẫu, bề rộng document 1425/753/375 vẫn giữ sau khi dọn modal, Excel dialog vẫn mở. Kiểm chứng ở mức DOM, kích thước và tương tác an toàn. Chưa có screenshot pixel diff toàn màn.

Không mở Candidate Detail, vì GET có thể đánh dấu notification đã đọc. Không chạy nguyên general smoke cũ vì harness đi vào route đó. Các bài browser chưa bao phủ HR Edit trên Draft, gửi/xóa thật, xác nhận approve/reject thật, countdown hoặc mọi nhánh filter/pagination/export. Không sửa assertion cũ. Browser chỉ đăng nhập fixture, điều hướng GET và mở/đóng UI; không chạy workflow ghi Offer.

## 8. Metrics và manifest

| Production file | Dòng trước → sau | Byte trước → sau |
|---|---:|---:|
| `src/main/resources/static/js/pages/offers/offer-common.js` | 206 → 193 | 8.880 → 8.363 |
| `src/main/resources/static/js/pages/offers/offer-hr.js` | 573 → 573 | 22.467 → 22.471 |
| `src/main/resources/static/js/pages/offers/offer-director.js` | 479 → 479 | 20.083 → 20.091 |
| `src/main/resources/templates/offers/list.html` | 510 → 454 | 38.311 → 34.116 |

Tổng JS 1.543 → 1.530 dòng, 62.211 → 61.706 byte. J01 thêm ròng 2 dòng vào common, J02 giảm ròng 15 dòng. CSS giữ 4.886 dòng / 101.616 byte. Không tạo thêm asset production. Ngoài bốn tệp trên, Task 24A thêm `scratch/task24a-toast-browser.cjs` và chính báo cáo này. Probe Java dưới `target/task24a-layout` là build artifact ignored. Không stage hoặc commit.

## 9. Bảo vệ SWP391

Em xác nhận lỗi bằng script thật và DOM thật. Script flash chung lấy và xóa thông báo chờ trong lúc Offer chưa có hàm render. Em cho Offer dùng key riêng nhưng vẫn dùng binder của dự án để mỗi toast có một bộ listener. Khi dọn HR List, em kiểm tra bằng cả route trong controller, DOM theo role và request trình duyệt. CSS lặp cũng xuất hiện ở Application, nên em tách việc xử lý layout ra khỏi phạm vi Offer.

## 10. Task 24B — Nghiệm thu cuối trước khi duyệt commit

HEAD vẫn là `e23da15e359f95dc713aee539ea35f30c2efea7b`. Không phát sinh thay đổi production trong 24B. Đã rà đúng bốn diff Offer và nội dung ba báo cáo. Các đối sánh nghi giống token trong báo cáo là tên đường dẫn Markdown, không có credential trong bảy tệp dự kiến stage. `scratch/`, `target/`, profile, ảnh và cấu hình local nằm ngoài manifest.

**Hợp đồng toast.** Send reload chính trang HR List hoặc Detail, Delete reload HR List, Director List reload trang hiện tại và Director Detail chuyển về `/offers` để controller chọn Director List. Cả bốn đích tải `offer-common.js` sau `flash-toast.js`. Tất cả writer dùng `OFFER_PENDING_TOAST_KEY` với `{message, type}`; shared chỉ đọc `pendingFlashToast`. Payload cũ trong tab mở trước bản cập nhật không được chuyển sang key mới. Chạy lại fixture Chrome 24A với kỳ vọng giữ nguyên: success/error/warning/info mỗi loại 1 toast, server + pending 2 toast, input lỗi hoặc trống 0 toast, reload lần hai 0 toast, nội dung HTML được escape, mỗi hook giữ một listener, wrapper biến mất sau toast cuối. Tất cả storage sau xử lý rỗng, fixture ghi 0 lỗi JS.

**HR và Director.** Chrome chạy 1440/768/390px trên HR List/Create/Detail, Director List/Detail và Candidate List. HR List có 0 Director modal, 0 Director inline handler và 0 Resource Timing entry cho Director JS. Search và filter điều hướng tới `?search=Task24BFixture&status=Pending_Director&page=0`. Bulk select chọn 9 dòng và xóa chọn về 0. Excel modal mở/đóng, chọn 25 cột, bỏ chọn về 0 và reset về 12. Send dialog mở được qua handler với ID tổng hợp, không submit. Mẫu HR không có đủ dòng để render link pagination và không có Draft được duyệt cho kiểm tra edit. Director List vẫn tải JS và render cả Approve/Reject. Từ chối thiếu lý do hiện lỗi, nhập bốn ký tự cho counter 4, không gọi fetch. Shared Detail pending chỉ có decision card dưới Director, có CSRF token và JSON handler. Không thực hiện request ghi Offer.

**Hình học và tài nguyên.** HR List có document width 1425/753/375px, khớp số đo Task 24A. Excel modal rộng 595/595/322px, cao 778px, radius 8px. Director modal rộng 595/595/322px, cao 349/349/369px, radius 8px. Decision card Director rộng 1387/721/343px và cao 306/306/366px. Không có so sánh ảnh theo pixel. Không đo lại toàn bộ console của các route live; các tương tác được thử không ném lỗi JS trong CDP.

Nhánh Delete dùng ID tổng hợp, hiển thị yêu cầu xác nhận và được hủy tại cả ba viewport. Fetch bị chặn trong bài kiểm tra và số lần gọi là 0. Bốn lệnh `node -c` đều exit 0. Maven chạy mới đủ bảy suite theo lệnh mục 2, exit 0. Surefire ghi 223 test, 0 failure, 0 error, 0 skip.

**Phần chưa xác minh.** HR EDIT — NOT VERIFIED IN TASK 24B. CANDIDATE DETAIL — NOT VERIFIED IN TASK 24B, vì chưa chứng minh notification đã đọc. Chưa đi qua link trang thứ hai, chưa tải Excel hay thực hiện thao tác ghi Offer trên DB dùng chung.

**CSS dùng chung.** Giữ nguyên mọi layout và stylesheet. Offer và Application vẫn render ba thẻ link cho mỗi stylesheet cấp trang lặp. Task 24A đo một Resource Timing entry theo URL trên lượt quan sát, ví dụ `offer-common.css` 39.499 byte encoded body và 39.799 byte transfer. Probe Java tối giản chưa tái hiện ba bản. Tái hiện tiếp bằng Spring/Thymeleaf configuration thực, cùng hai layout, một template Offer và một template Application với dữ liệu model tối thiểu; so sánh HTML head, Resource Timing và cascade khi chỉ thay một slot trong fixture. Thay layout chung cần Lead duyệt. Đây là vấn đề riêng chưa được xử lý.

**Số đo cuối.** Bốn JS là 1.530 dòng / 61.706 byte, bốn CSS là 4.886 dòng / 101.616 byte. HR List là 454 dòng / 34.116 byte. So baseline, JS giảm ròng 13 dòng / 505 byte, HR List giảm 56 dòng / 4.195 byte. Chỉ HR List bỏ lượt tải Director JS; không suy ra tác động hiệu năng toàn hệ thống.

**Commit đề xuất.** `fix(offer): restore flash toasts and remove unused HR director assets`. Manifest gồm bốn tệp production trong mục 8 và ba báo cáo 34, 35, 36. Commit chờ người dùng duyệt.

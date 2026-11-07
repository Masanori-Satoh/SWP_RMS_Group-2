# Task 24 — Độ tin cậy của toast và dọn phụ thuộc Offer

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](../README.md).

## 1. Baseline và điều kiện Phase 0

Nhánh `refactor/huyenpt/offer-ui-js-to-html-css` ở `e23da15e359f95dc713aee539ea35f30c2efea7b`. Index và diff của file tracked sạch khi bắt đầu. Báo cáo Task 23 và `scratch/` đã có dưới dạng untracked. Kiểm tra cả mục ignored cho thấy có cấu hình local, build output, log và backup; giữ nguyên các tệp hiện có trong `scratch/`. Sáu template Offer hiện diện đầy đủ.

Đã đọc toàn bộ [báo cáo Task 23](34-task-23-shared-frontend-audit-and-optimization-roadmap.md). Báo cáo có ma trận asset, bằng chứng browser của năm màn ở 1440/768/390px, phân loại tối ưu và quyền sở hữu. Phần xác minh còn thiếu gồm HR Edit có Draft, Candidate Detail cô lập, dialog mở, network/cache, listener count và hành vi pending toast. Task 24 yêu cầu bổ sung bằng chứng trước khi sửa production. Điều kiện này chưa đạt, nên phần triển khai dừng tại gate. Integration đã được người dùng xác nhận trước đó là baseline, không tính là test mới.

## 2. J01 — Trace mã thật với storage tổng hợp

`list.html:503–506`, `form.html:287–289`, `detail.html:486–488`, `director-list.html:280–282` đều đặt `flash-toast.js` trước `offer-common.js`, cả hai là classic defer. Task 23 đã thấy thứ tự này trên DOM render. Code shared chạy IIFE, bind toast server ngay, đọc rồi xóa `pendingFlashToast`, sau đó mới kiểm tra `window.showFlashToast`. Nó công bố `window.initFlashToasts` khi sắp kết thúc. Offer Common khai báo renderer ở phạm vi global khi script bắt đầu chạy, sau đó gọi consumer ngay nếu `readyState` đã là `interactive` hoặc đăng ký DOMContentLoaded nếu vẫn `loading`.

Task 24 chạy nguyên văn cả hai file bằng Node `vm`, cùng một global scope, document tối thiểu và storage instrumented. Harness được truyền qua stdin, không ghi vào `scratch/` hoặc gọi endpoint. Mô phỏng document ở `interactive`, chạy shared rồi Offer Common, ghi lại thao tác read/delete cùng kiểu của renderer.

| Payload trước khi chạy | Shared đọc / xóa | Renderer lúc shared đọc | Offer đọc | Payload cuối | Renderer chạy |
|---|---|---|---|---|---:|
| Không có | Đọc null, không xóa | undefined | null | null | 0 |
| Success | Đọc payload và xóa | undefined | null | null | 0 |
| Error | Đọc payload và xóa | undefined | null | null | 0 |
| Warning | Đọc payload và xóa | undefined | null | null | 0 |
| Info | Đọc payload và xóa | undefined | null | null | 0 |
| JSON sai | Đọc, xóa, parse lỗi, catch xóa lại | undefined | null | null | 0 |
| JSON thiếu message | Đọc và xóa | undefined | null | null | 0 |

Sau khi shared chạy, `initFlashToasts` là function, `showFlashToast` là undefined. Sau khi Offer Common chạy, cả hai là function và storage rỗng. Kết quả tái hiện mất payload ở lớp logic khi chạy thứ tự đã quan sát. Harness không có browser DOM thực, nên chưa đo visibility, các toast song song, history/reload thực, HTML escaping ở DOM, browser console hoặc listener count. J01 vẫn **BLOCKED** theo gate browser của Task 24.

Bốn writer dùng key hiện tại tại `offer-common.js:177` (Send), `offer-hr.js:84` (Delete), `offer-director.js:190,429` (quyết định list/detail). Nếu browser xác nhận và phạm vi vẫn giới hạn theo Phase 2, phương án để kiểm thử là một key có tiền tố Offer, được dùng đồng bộ ở cả bốn writer và một reader. Shared consumer sẽ không đụng key riêng. Giữ một consumer, renderer, escape và luồng reload/redirect. Chưa chỉnh writer, reader hoặc tên key.

## 3. J02 — Tương thích dismissal helper

`flash-toast.js:12–38,54–55` nhận root có `querySelectorAll`, bind các descendant có `[data-flash-toast]`, đặt `_toastBound` để gọi lại không bind lần hai. Click close và animationend chạy cùng lifecycle, thêm `is-leaving`, đợi transitionend hoặc fallback 350ms, xóa wrapper khi hết toast. `offer-common.js:81–95` bind trực tiếp cho toast vừa append và giữ tham chiếu wrapper tại lúc render. Cùng markup/data hooks nhưng shared lấy parent tại lúc gỡ. Cần browser fixture cho server-only, dynamic-only và mixed, nhiều toast, gọi init nhiều lần, close tay và tự đóng, kèm kiểm tra wrapper sau toast cuối. Giữ nguyên renderer và dismissal vì chưa chứng minh tương đương trong những tình huống này.

## 4. J08 — Phụ thuộc Director trong HR List

`OfferController.java:80–121` chọn riêng `director-list` cho Director; HR đi đến `offers/list`. Task 23 quan sát HR DOM có modal ẩn và không có opener. Trong `list.html:246–253`, opener chỉ render khi pending và role Director. Block `438–477` có các handler đóng, nhập nhận xét và submit; script tại dòng 506 cung cấp chúng. Script Director vẫn là phụ thuộc thật của trang Director. Không thấy đường mở modal này từ lọc, chọn dòng, export, gửi hoặc xóa của HR. Chưa thực hiện cleanup vì Phase 0 vẫn chặn chỉnh sửa và chưa chạy regression của checkpoint trên Draft.

## 5. CSS lặp và ranh giới sở hữu

Task 23 quan sát HR List `B → C H F → W → C H F → C H F` và Candidate List `B → P F → W K → P F → P F`. Các link cấp trang bị lặp ba lần; bản đầu đứng trước workspace, hai bản cuối đứng sau workspace và candidate shell nếu có. Cùng một URL không chứng minh ba lượt tải mạng. Cần đo bằng browser network log cold/warm cache và so matched/computed styles trước khi bỏ bất kỳ link nào.

Nguồn trang khai báo một lần. `layout/base.html:24` và `layout/internal.html:11` đều có fragment `styles`; internal decorate base, sáu trang Offer decorate internal. Đây là giả thuyết về merge head qua hai cấp, chưa có render fixture tối giản xác nhận cơ chế. Application, Interview, Requisition và Job Posting cũng decorate internal, nên có nguy cơ chung, nhưng chưa đo số link trên các module đó trong Task 24. Cách sửa kiến trúc cần thử trên fixture là chỉ để một điểm ghép styles cho hai cấp layout và giữ thứ tự nền trước trang. Nếu tái hiện trên nhiều module, chuyển thành thay đổi riêng do Lead duyệt vì chạm `layout/base.html` hoặc `layout/internal.html`. Task 24 giữ nguyên cả hai layout, tokens, global, components, workspace và candidate shell.

## 6. Regression

- `node -c` trả exit 0 cho cả bốn file JS Offer.
- Maven được gọi với đủ bảy suite `OfferIntegrationTests,OfferServiceTests,CandidateOfferWebTests,CandidateOfferServiceTests,OfferExportServiceTests,OfferControllerBugTests,CandidateOfferQueryTests`. Build dừng tại `maven-compiler-plugin:testCompile` với lỗi trên `org.eclipse.angus:jakarta.mail:2.0.5` trong Maven local repository. Chưa có test nào chạy trong lần này. Baseline lịch sử 223 test không được dùng làm kết quả mới.
- Local `/login` trả HTTP 200. Browser smoke tổng quát hiện điều hướng thẳng đến `/portal/offers/1` ở `scratch/run_interactive_browser_smoke.js:437–438`. GET Candidate Detail gọi `markOfferNotificationRead`, có thể cập nhật notification chưa đọc. Chưa xác minh fixture này an toàn, nên không chạy nguyên suite trên DB local. Không thay assertion hay chạy lại để ghi đè baseline. Dedicated Director và countdown fixture chưa chạy lại.
- Không có visual regression mới. Số đo Task 23 của HR List/Create/Detail, Director List/Detail và Candidate List ở 1440/768/390px chỉ là bằng chứng cũ. HR Edit và Candidate Detail vẫn thiếu.

## 7. Metrics, diff và việc còn lại

| Tệp asset | Dòng đầu → cuối | Byte đầu → cuối |
|---|---:|---:|
| `offer-common.css` | 1.878 → 1.878 | 39.499 → 39.499 |
| `offer-hr.css` | 864 → 864 | 17.687 → 17.687 |
| `offer-director.css` | 408 → 408 | 9.826 → 9.826 |
| `candidate-offers.css` | 1.736 → 1.736 | 34.604 → 34.604 |
| **CSS** | **4.886 → 4.886** | **101.616 → 101.616** |
| `offer-common.js` | 206 → 206 | 8.880 → 8.880 |
| `offer-hr.js` | 573 → 573 | 22.467 → 22.467 |
| `offer-director.js` | 479 → 479 | 20.083 → 20.083 |
| `candidate-offers.js` | 285 → 285 | 10.781 → 10.781 |
| **JS** | **1.543 → 1.543** | **62.211 → 62.211** |

Diff tracked và staged rỗng. Manifest của Task 24 chỉ có tệp mới untracked `docs/members/huyenpt/html-css-refactor/35-task-24-toast-reliability-and-safe-cleanup-report.md`. Báo cáo 34 và `scratch/` là untracked có từ trước. Không có dòng HTML, CSS hay JS production bị bỏ, chuyển chỗ hoặc sửa. Không đo được thay đổi network transfer vì không có checkpoint implementation. Lỗi CSS lặp vẫn mở.

Để qua gate, cần fixture browser không ghi DB cho Draft và Candidate Detail, fixture toast dùng production scripts và ghi nhận lifecycle thực, cùng môi trường Maven compile được dependency local. Có thể dùng fixture Candidate cô lập hoặc chứng minh notification của offer đã ở trạng thái đọc trước khi mở. Không có thao tác Send, Delete, Approve, Reject, Accept hoặc Decline thật trong Task 24.

## 8. Giải thích khi bảo vệ SWP391

Em tách bằng chứng thành ba lớp. DOM cho thấy trang dùng file nào và theo thứ tự nào. Phép thử JavaScript cho thấy payload có thể mất do consumer xóa trước khi renderer sẵn sàng. Browser fixture mới cho biết người dùng có thực sự thấy đúng một toast hay không. Em giữ mã production tại baseline vì chưa đủ bằng chứng qua gate, đồng thời giữ phần layout dùng chung cho Lead xem xét nếu lỗi CSS xuất hiện ở nhiều phân hệ.

## 9. Trạng thái

- **J01 TOAST — BLOCKED.** Tái hiện mất payload trong Node VM, chưa tái hiện end-to-end trong browser.
- **J02 TOAST HELPER — KEPT UNCHANGED.**
- **J08 HR DIRECTOR DEPENDENCY — BLOCKED.** Đã lần tham chiếu và có DOM Task 23, chưa qua Phase 0.
- **DUPLICATE CSS — PARTIAL.**
- **MAVEN REGRESSION — FAIL.** Build dừng ở test compile, 0 test được thực thi trong lần này.
- **BROWSER REGRESSION — PARTIAL.** Có ma trận Task 23 và trace bổ sung, chưa có browser regression mới của checkpoint.
- **VISUAL REGRESSION — PARTIAL.** Có số đo baseline Task 23, chưa có so sánh trước/sau.
- **GIT SAFETY — PASS.** Không stage, commit, push, merge hoặc chuyển nhánh.

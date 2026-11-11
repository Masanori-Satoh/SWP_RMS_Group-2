# Tổng kết thay đổi chưa PR — Offer Management

Ngày lập: **11/11/2026** — Người phụ trách: **HuyenPT**.

## 1. Phạm vi đối chiếu và trạng thái GitHub

| Nội dung | Giá trị tại thời điểm lập báo cáo |
| --- | --- |
| Repository | `Masanori-Satoh/SWP_RMS_Group-2` |
| Nhánh | `fix/huyenpt/project-test-errors` |
| Worktree thực hiện | `scratch/project-test-errors-worktree` |
| Base `origin/main` đã fetch lại | `054c73d2c9183119393aad986883d3d52ce67b71` |
| Snapshot chức năng còn giữ sau hoàn tác seed | `44fcc15326890e9d437a02363c67c89dcfdae589` |
| HEAD nhánh trên remote | `35c16a87739c260aceb3059a64510c78af446fc1` |
| Diff chức năng tìm kiếm trước khi chuyển CSS | **10 file, +286/-44 dòng**, chưa tính chính báo cáo này; phần CSS bổ sung ở mục 2.5 |
| PR của nhánh | PR #171 đã merge; chưa tìm thấy PR mới cho phần bổ sung dưới đây |

Đã kiểm tra GitHub: [PR #171 — Fix/huyenpt/project test errors](https://github.com/Masanori-Satoh/SWP_RMS_Group-2/pull/171) đã merge vào main ngày 09/11/2026. PR đó chứa thay đổi giới hạn export chỉ cho HR (`7dcfa794`), **không phải nội dung mới cần PR lại**. Hiện `OfferController.java` không có diff so với main.

Commit `7dcfa794` vẫn xuất hiện trong lịch sử riêng của nhánh do cách merge lịch sử; chỉ nhìn danh sách commit chưa đủ để kết luận thay đổi chưa vào main. Báo cáo dùng diff thực tế và trạng thái PR để tách phạm vi. Các chức năng còn giữ tương ứng **4 commit công việc** ở mục 3; commit tài liệu và hoàn tác không phải tính năng mới.

Các commit tìm kiếm đến `35c16a87` đã có trên remote; `44fcc153` còn local tại thời điểm kiểm tra remote. Phần seed ở `7fb29794` đã được hoàn tác theo yêu cầu, không còn thuộc diff hiện tại. Chưa push, tạo PR hoặc merge trong lần cập nhật báo cáo này.

## 2. Nội dung thay đổi

### 2.1. Tìm kiếm dùng chung HR, Director và Excel

- Tách quy tắc keyword vào `OfferProposalRepository.matchingKeyword`, dùng lại trong bộ lọc HR/Excel và danh sách Director, giảm logic lặp ở service.
- Chuẩn hóa khoảng trắng; giới hạn keyword 120 ký tự; escape `%`, `_`, `[` theo tiện ích `ListFilters` hiện có.
- Tìm trên tên ứng viên, email, vị trí đề xuất và phòng ban theo OR. Dùng collation `Latin1_General_100_CI_AI` tại biểu thức query để hỗ trợ không phân biệt hoa/thường và dấu; không thay đổi collation/schema database.
- `#12` tìm chính xác OfferId 12; số thường `12` vẫn là keyword văn bản. Mã vượt giới hạn integer trả rỗng.
- Giữ nguyên phân quyền, phạm vi xem của Director, trạng thái, xóa mềm và sắp xếp.
- Hai template chỉ cập nhật placeholder và thêm `maxlength="120"`; không thêm thanh lọc, CSS hoặc bố cục mới.

### 2.2. Xuất Excel theo bộ lọc đã áp dụng

- FILTERED đọc `search`, `status`, `timeSort` từ URL hiện tại, không lấy nội dung toolbar đang gõ dở.
- Ví dụ URL đã áp dụng keyword Java nhưng người dùng mới gõ QA chưa tìm: Excel vẫn xuất theo Java, khớp danh sách đang xem.
- FILTERED không bị giới hạn bởi trang hiện tại; SELECTED/ALL giữ nguyên. Quyền export chỉ HR theo phần đã merge từ PR #171.

### 2.3. Tìm kiếm lương bằng định dạng đang hiển thị

- Nhận `36.000.000 VNĐ`, `36.000.000 VND`, `36.000.000`; cũng nhận số có đơn vị như `36000000 VNĐ`.
- So sánh BigDecimal chính xác với lương chính thức **hoặc** lương thử việc; không chuyển cột lương thành chuỗi để LIKE.
- Hỗ trợ khác biệt hoa/thường và khoảng trắng khi copy. Số trần `36000000` vẫn tìm như keyword; định dạng tiền sai hoặc câu trộn chữ dùng nhánh tìm văn bản.
- Tiền vượt 16 chữ số nguyên của decimal(18,2) trả rỗng. Không bổ sung tìm khoảng lương hoặc cú pháp điều kiện.
- Áp dụng cùng logic cho danh sách HR, Director và Excel FILTERED, không sửa UI.

### 2.4. Phần seed đã thu hồi

Theo yêu cầu ngày 11/11/2026, toàn bộ thay đổi ở `7fb29794` đã được hoàn tác: generator, SQL và hướng dẫn seed trở lại đúng nội dung trước commit; xóa tài liệu fixture demo. Không còn phần bổ sung ứng viên demo trong phạm vi PR.

Kiểm tra thực tế phát hiện dữ liệu demo đã được nạp vào database. Sau khi người dùng duyệt riêng, đã sao lưu và xóa đúng 16 tài khoản/Candidate/Application/Offer cùng dữ liệu phụ thuộc trong transaction trên `RitirementManagement2`. Còn 0 tài khoản demo; so sánh snapshot SHA-256 trên 19 bảng xác nhận mọi bản ghi ngoài phạm vi không đổi. Không reset database, sửa schema hoặc reseed. Backup và kết quả nằm local trong scratch, không đưa vào Git.

### 2.5. Đưa CSS Offer trực tiếp vào pages — 11/11/2026

- Di chuyển bằng Git 3 file `offer-common.css`, `offer-hr.css`, `offer-director.css` vào `src/main/resources/static/css/pages/`; gỡ thư mục con rỗng. SHA-256 trước/sau khớp 3/3, không sửa bất kỳ rule CSS nào.
- Đổi đúng 8 href trong 4 template `offers/list.html`, `form.html`, `detail.html`, `director-list.html`. Giữ nguyên thứ tự và cách ghép head hiện tại, không thêm styles fragment gây nạp lặp.
- Cập nhật đường dẫn trong 10 tài liệu HuyenPT có tham chiếu cũ, ghi chú đường dẫn đã chuyển; giữ các kết quả Task/PR lịch sử. Không sửa tài liệu thành viên khác.
- Bổ sung 3 tình huống HTTP: tài nguyên mới trả 200, đúng MIME và byte; URL cũ trả 404. Bốn template render 7 stylesheet duy nhất, common trước CSS vai trò, không còn đường dẫn thư mục con trong HTML.
- Maven sau thay đổi: **310/310 test Offer đạt** (OfferIntegrationTests tăng từ 65 lên 68); frontend **23/23 đạt**. Không đổi backend nghiệp vụ, JavaScript, database, exception hoặc shared layout.
- Browser xác nhận 4 trang và popup HR/Director trên HTML Thymeleaf với service mock, dùng CSS/JS thật; CSS mới được browser đọc rule thành công. Đây là fixture, không phải E2E đăng nhập/database. Bằng chứng ở `scratch/offer-search-upgrade-20261110/css-move-browser/`.
- Đã phát hiện 3 CSS build cũ trong `target/classes`; chuyển riêng các bản có hash khớp sang `scratch/offer-search-upgrade-20261110/css-move-stale-build-backup/`, không xóa nội dung hoặc clean toàn bộ target. Các artifact khác được giữ nguyên.
- Quét toàn bộ file Git quản lý bằng git grep và kiểm tra đường dẫn trong source/test/tài liệu: không còn tham chiếu thư mục CSS con cũ. Log/cache/snapshot lịch sử ngoài Git được giữ làm bằng chứng, không thuộc nội dung runtime hoặc commit.
- Manifest lần chuyển này: 3 rename CSS, 4 template, 1 file test, 10 tài liệu tham chiếu và báo cáo này. Sau bổ sung, diff branch so với base main gồm **26 file**; bảng mục 4 bên dưới chỉ là snapshot phần tìm kiếm trước lần chuyển CSS.

## 3. Các commit công việc mới

| Commit | Nội dung | Diff riêng commit |
| --- | --- | --- |
| `444979a3` | Dùng chung keyword; Excel lấy bộ lọc trên URL | 6 file, +76/-25 |
| `3fffedad` | Tìm mã, email, phòng ban và hỗ trợ không dấu | 5 file, +64/-30 |
| `35c16a87` | Kiểm tra toolbar/phân trang, báo cáo bằng chứng | 2 file, +100/-0 |
| `44fcc153` | Tìm chính xác lương theo định dạng tiền | 4 file, +62/-5 |

Các phase có sửa trùng file; không cộng số file hoặc số dòng từng commit thành diff cuối branch. Các merge commit chỉ cập nhật main, không được tính là tính năng Offer mới.

## 4. Danh sách file trong diff thực tế

Đường dẫn bên dưới tương đối từ root repository, thống kê phần còn giữ theo `git diff --numstat origin/main...44fcc153`.

| File | + | - | Nội dung |
| --- | ---: | ---: | --- |
| `src/main/java/com/group2/rms/offer/repository/OfferProposalRepository.java` | 36 | 9 | Quy tắc keyword và lương dùng chung |
| `src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java` | 2 | 22 | Director dùng lại specification, bỏ imports/logic thừa |
| `src/main/resources/static/js/pages/offers/offer-hr.js` | 5 | 4 | FILTERED export lấy bộ lọc URL |
| `src/main/resources/templates/offers/list.html` | 1 | 1 | Gợi ý ô tìm và maxlength cho HR |
| `src/main/resources/templates/offers/director-list.html` | 1 | 1 | Gợi ý ô tìm và maxlength cho Director |
| `src/test/java/com/group2/rms/offer/service/OfferServiceTests.java` | 96 | 0 | Keyword, wildcard, #ID, lương và giới hạn |
| `src/test/java/com/group2/rms/offer/OfferIntegrationTests.java` | 41 | 3 | Payload frontend, HTML theo vai trò, bộ lọc khi phân trang |
| `src/test/java/com/group2/rms/offer/OfferExportServiceTests.java` | 7 | 4 | Workbook FILTERED với keyword lương |
| `src/test/js/offer-frontend.test.cjs` | 24 | 0 | Scope export và input chưa áp dụng |
| `docs/members/huyenpt/offer-search-upgrade-20261110.md` | 73 | 0 | Các phase tìm kiếm, bằng chứng và giới hạn kiểm chứng |
| **Tổng trước báo cáo này** | **286** | **44** | **10 file** |

Snapshot trước lần chuyển CSS gồm 5 file production của ứng dụng Offer, 4 file test, 1 tài liệu; cộng báo cáo này là 11 file. Các file bổ sung của lần chuyển CSS nằm ở mục 2.5; manifest hiện tại là 26 file. Không có diff seed hoặc hướng dẫn seed so với main.

## 5. Kết quả kiểm chứng đã thực hiện

| Kiểm tra | Kết quả và giới hạn |
| --- | --- |
| Maven sau bổ sung tìm lương | **307/307 đạt**: OfferServiceTests 162, OfferIntegrationTests 65, OfferExportServiceTests 15, CandidateOfferServiceTests 46, CandidateOfferWebTests 19. Là 5 lớp Offer được chọn, không phải toàn bộ dự án. |
| JavaScript frontend | **23/23 đạt**, bao gồm các scope export và nội dung chưa áp dụng khác URL. |
| SQL chỉ đọc của đợt tìm kiếm | 9 tình huống SELECT đạt: tên/phòng ban không dấu, email, vị trí, wildcard literal, số thường, #ID, không có kết quả, chuỗi giống SQL injection. Không kiểm chứng nhánh tìm lương qua SQL thực trong đợt này. |
| Browser HR/Director | HTML từ MockMvc/Thymeleaf với service mock, CSS/JS thật; toolbar giữ bố cục, Director không có export modal; payload thực lấy bộ lọc URL dù ô nhập đang khác. Chưa là E2E trên ứng dụng thật có đăng nhập. |
| Hoàn tác seed | Ba file seed/hướng dẫn khớp snapshot trước `7fb29794`; tài liệu demo được xóa. |
| Dọn database đã được duyệt | Đã backup và xóa 16 tài khoản demo cùng dữ liệu phụ thuộc; giữ nguyên mọi bản ghi ngoài phạm vi trên 19 bảng; không đổi identity counter hoặc schema. |
| Git | `git diff --check origin/main...HEAD` đạt trước khi thêm báo cáo này; báo cáo tiếp tục được kiểm tra whitespace trước commit. |

Log local nằm trong `scratch/offer-search-upgrade-20261110`: `salary-search-final.log`, `salary-search-node.log`, `sql-read-only-results.json`, `browser-export-payload.json`, thư mục `browser/`; backup dọn demo là `removed-offer-demo-backup-20261111-020529.xml`, kết quả `removed-offer-demo-result.json`. Giữ nguyên log lịch sử trong scratch/target, không đưa vào commit. Không chạy lại Maven cho lần hoàn tác seed/tài liệu vì không đổi mã ứng dụng.

## 6. Rủi ro, giới hạn và bước trước PR

- Chưa xác minh end-to-end nhánh tìm lương bằng ứng dụng thật kết nối database; cần restart đúng worktree, thử các định dạng tiền và đối chiếu Excel FILTERED.
- Query nhiều trường có thêm công việc. Đo SQL tương đương trên 8 dòng local: median trước 1,764 ms, sau 2,198 ms; không phải SQL Hibernate đo thực tế và không đủ để kết luận hiệu năng dữ liệu lớn. Không tuyên bố tìm kiếm mới nhanh hơn.
- JaCoCo có warning execution data cũ của lớp Notification; test đạt nhưng báo cáo này không dùng coverage làm bằng chứng.
- Phần seed bổ sung đã thu hồi khỏi file và database; không chạy schema hoặc seed lại. Giữ backup ngoài Git cho khả năng khôi phục dữ liệu nếu cần.
- Chưa kiểm tra merge thử với main trong lần lập báo cáo; không suy ra không conflict chỉ từ `diff --check`. Main được fetch lại để chốt base, chưa merge thêm.
- Giữ nguyên thư mục project gốc đang có conflict tài liệu; mọi công việc ở worktree riêng nêu trên. Không reset, clean hoặc đổi credentials. Thay đổi database chỉ gồm lần dọn nhóm demo đã được duyệt riêng.
- Khi chuẩn bị PR: kiểm tra main/remote lại, push các commit local theo yêu cầu riêng, kiểm tra mergeability và CI; không đưa scratch, target, cấu hình local hoặc mật khẩu vào PR.

## 7. Nội dung PR đề xuất

**Title:** `feat(offer): unify keyword and salary search and align filtered exports`

**Mô tả ngắn:** Nâng cấp ô tìm kiếm Offer hiện có để dùng chung quy tắc cho HR, Director và Excel, hỗ trợ tên/email/vị trí/phòng ban không dấu, #ID và số tiền định dạng VNĐ/VND. Sửa export FILTERED để khớp bộ lọc đã áp dụng; giữ bố cục và phân quyền hiện tại. Đã đạt 307 test Java Offer và 23 test frontend. Không có thay đổi seed hoặc schema trong diff PR.

**Bổ sung mô tả sau chuyển CSS:** Đặt 3 stylesheet Offer trực tiếp trong css/pages, cập nhật template và tài liệu liên quan; giữ nguyên nội dung CSS, thứ tự nạp và giao diện. Kết quả kiểm thử mới nhất là 310 test Java Offer và 23 test frontend đạt.

Phạm vi PR không bao gồm thay đổi đã merge ở PR #171, không đổi schema, nghiệp vụ Application hoặc cấu trúc layout chung.

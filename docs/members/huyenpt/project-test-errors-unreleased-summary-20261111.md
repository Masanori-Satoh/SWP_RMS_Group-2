# Tổng kết thay đổi chưa PR — Offer Management

Ngày lập: **11/11/2026** — Người phụ trách: **HuyenPT**.

## 1. Phạm vi đối chiếu và trạng thái GitHub

| Nội dung | Giá trị tại thời điểm lập báo cáo |
| --- | --- |
| Repository | `Masanori-Satoh/SWP_RMS_Group-2` |
| Nhánh | `fix/huyenpt/project-test-errors` |
| Worktree thực hiện | `scratch/project-test-errors-worktree` |
| Base `origin/main` đã fetch lại | `054c73d2c9183119393aad986883d3d52ce67b71` |
| HEAD nội dung được tổng kết | `7fb29794edb22b1e857d0b18dfafb56ce69514f3` |
| HEAD nhánh trên remote | `35c16a87739c260aceb3059a64510c78af446fc1` |
| Diff nội dung so với main | **14 file, +875/-70 dòng**, chưa tính chính báo cáo này |
| PR của nhánh | PR #171 đã merge; chưa tìm thấy PR mới cho phần bổ sung dưới đây |

Đã kiểm tra GitHub: [PR #171 — Fix/huyenpt/project test errors](https://github.com/Masanori-Satoh/SWP_RMS_Group-2/pull/171) đã merge vào main ngày 09/11/2026. PR đó chứa thay đổi giới hạn export chỉ cho HR (`7dcfa794`), **không phải nội dung mới cần PR lại**. Hiện `OfferController.java` không có diff so với main.

Commit `7dcfa794` vẫn xuất hiện trong lịch sử riêng của nhánh do cách merge lịch sử; chỉ nhìn danh sách commit chưa đủ để kết luận thay đổi chưa vào main. Báo cáo dùng diff thực tế và trạng thái PR để tách phạm vi. Nhánh có 9 commit chưa là ancestor của main (gồm 3 merge và 6 commit thường), nhưng chỉ **5 commit công việc mới** liệt kê ở mục 3.

Các commit tìm kiếm đến `35c16a87` đã có trên remote; `44fcc153` và `7fb29794` còn local tại thời điểm kiểm tra. Báo cáo này được bổ sung sau snapshot trên. Chưa push, tạo PR hoặc merge trong lần lập báo cáo.

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

### 2.4. Bổ sung dữ liệu demo đủ 8 trạng thái

- Thêm 16 tài khoản Candidate Active độc lập, mỗi trạng thái 2 người, cùng hồ sơ/review/phỏng vấn Passed/lịch sử duyệt/notification/audit cần thiết.
- CandidateId **123–138**, ApplicationId **135–150**, OfferId **33–48**; username `offer_<nhóm>_01/02`.
- Nhóm `_01` dùng tin Java, lương 36.000.000/32.000.000 VNĐ; nhóm `_02` dùng tin B2B, lương 20.000.000/17.500.000 VNĐ, thuận tiện kiểm thử keyword và lương.
- Sent_Candidate còn hạn tại thời điểm nạp seed; Expired đã quá hạn. Application của Expired giữ Offered, Accepted là Hired, Declined là Withdrawn; trước gửi chưa có ResponseDeadline.
- Tổng seed: 48 thư mời, gồm 47 hiển thị và 1 Draft xóa mềm. Số hiển thị: Draft 4, Pending_Director 6, Director_Approved 3, Expired 4, Director_Rejected 4, Sent_Candidate 6, Accepted 14, Declined 6.
- Giữ nguyên ID và toàn bộ nội dung bản ghi cũ. Generator dùng lại các hàm seed hiện có và sinh thêm bảng tra cứu, 16 câu kiểm tra fixture trong SQL.
- Chỉ sinh file SQL, **chưa chạy vào database**. Không thay đổi schema, migration hoặc cấu hình kết nối.

## 3. Các commit công việc mới

| Commit | Nội dung | Diff riêng commit |
| --- | --- | --- |
| `444979a3` | Dùng chung keyword; Excel lấy bộ lọc trên URL | 6 file, +76/-25 |
| `3fffedad` | Tìm mã, email, phòng ban và hỗ trợ không dấu | 5 file, +64/-30 |
| `35c16a87` | Kiểm tra toolbar/phân trang, báo cáo bằng chứng | 2 file, +100/-0 |
| `44fcc153` | Tìm chính xác lương theo định dạng tiền | 4 file, +62/-5 |
| `7fb29794` | Thêm 16 ứng viên theo trạng thái và tài liệu seed | 4 file, +589/-26 |

Các phase có sửa trùng file; không cộng số file hoặc số dòng từng commit thành diff cuối branch. Các merge commit chỉ cập nhật main, không được tính là tính năng Offer mới.

## 4. Danh sách file trong diff thực tế

Đường dẫn bên dưới tương đối từ root repository, thống kê theo `git diff --numstat origin/main...7fb29794`.

| File | + | - | Nội dung |
| --- | ---: | ---: | --- |
| `src/main/java/com/group2/rms/offer/repository/OfferProposalRepository.java` | 36 | 9 | Quy tắc keyword và lương dùng chung |
| `src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java` | 2 | 22 | Director dùng lại specification, bỏ imports/logic thừa |
| `src/main/resources/static/js/pages/offers/offer-hr.js` | 5 | 4 | FILTERED export lấy bộ lọc URL |
| `src/main/resources/templates/offers/list.html` | 1 | 1 | Gợi ý ô tìm và maxlength cho HR |
| `src/main/resources/templates/offers/director-list.html` | 1 | 1 | Gợi ý ô tìm và maxlength cho Director |
| `database/seeds/build_seed.py` | 35 | 1 | Nhóm demo Offer và kiểm tra SQL sinh ra |
| `database/seeds/seed_data.sql` | 484 | 25 | Dữ liệu và header/count/checks được sinh lại |
| `src/test/java/com/group2/rms/offer/service/OfferServiceTests.java` | 96 | 0 | Keyword, wildcard, #ID, lương và giới hạn |
| `src/test/java/com/group2/rms/offer/OfferIntegrationTests.java` | 41 | 3 | Payload frontend, HTML theo vai trò, bộ lọc khi phân trang |
| `src/test/java/com/group2/rms/offer/OfferExportServiceTests.java` | 7 | 4 | Workbook FILTERED với keyword lương |
| `src/test/js/offer-frontend.test.cjs` | 24 | 0 | Scope export và input chưa áp dụng |
| `docs/database/data_seeding_guide.md` | 13 | 0 | Cách dùng nhóm seed mới và phạm vi nạp dữ liệu |
| `docs/members/huyenpt/offer-demo-fixtures.md` | 57 | 0 | Tài khoản/ID, trạng thái và thao tác demo |
| `docs/members/huyenpt/offer-search-upgrade-20261110.md` | 73 | 0 | Các phase tìm kiếm, bằng chứng và giới hạn kiểm chứng |
| **Tổng trước báo cáo này** | **875** | **70** | **14 file** |

Gồm 5 file production của ứng dụng Offer, 2 file seed, 4 file test, 3 tài liệu. Báo cáo tổng kết hiện tại là tài liệu thứ 4, làm manifest sau khi thêm thành **15 file**.

## 5. Kết quả kiểm chứng đã thực hiện

| Kiểm tra | Kết quả và giới hạn |
| --- | --- |
| Maven sau bổ sung tìm lương | **307/307 đạt**: OfferServiceTests 162, OfferIntegrationTests 65, OfferExportServiceTests 15, CandidateOfferServiceTests 46, CandidateOfferWebTests 19. Là 5 lớp Offer được chọn, không phải toàn bộ dự án. |
| JavaScript frontend | **23/23 đạt**, bao gồm các scope export và nội dung chưa áp dụng khác URL. |
| SQL chỉ đọc của đợt tìm kiếm | 9 tình huống SELECT đạt: tên/phòng ban không dấu, email, vị trí, wildcard literal, số thường, #ID, không có kết quả, chuỗi giống SQL injection. Không kiểm chứng nhánh tìm lương qua SQL thực trong đợt này. |
| Browser HR/Director | HTML từ MockMvc/Thymeleaf với service mock, CSS/JS thật; toolbar giữ bố cục, Director không có export modal; payload thực lấy bộ lọc URL dù ô nhập đang khác. Chưa là E2E trên ứng dụng thật có đăng nhập. |
| Dữ liệu seed | **281 kiểm tra Python đạt**, đối chiếu toàn bộ dữ liệu cũ không đổi; đầy đủ 16 fixture và trạng thái liên quan. |
| Tài liệu fixture | Đối chiếu tài khoản và ID **16/16 đạt**. |
| SQL sinh lại | Sinh lặp lại cho kết quả byte-for-byte giống nhau; chưa thực thi seed/checks trên SQL Server. |
| Git | `git diff --check origin/main...HEAD` đạt trước khi thêm báo cáo này; báo cáo tiếp tục được kiểm tra whitespace trước commit. |

Log local nằm trong `scratch/offer-search-upgrade-20261110`: `salary-search-final.log`, `salary-search-node.log`, `sql-read-only-results.json`, `browser-export-payload.json`, thư mục `browser/`, `verify_offer_seed.py`, `offer-seed-verification.json`. Scratch/target không đưa vào commit. Không chạy lại Maven cho lần sửa seed/tài liệu vì không đổi mã ứng dụng.

## 6. Rủi ro, giới hạn và bước trước PR

- Chưa xác minh end-to-end nhánh tìm lương bằng ứng dụng thật kết nối database; cần restart đúng worktree, thử các định dạng tiền và đối chiếu Excel FILTERED.
- Query nhiều trường có thêm công việc. Đo SQL tương đương trên 8 dòng local: median trước 1,764 ms, sau 2,198 ms; không phải SQL Hibernate đo thực tế và không đủ để kết luận hiệu năng dữ liệu lớn. Không tuyên bố tìm kiếm mới nhanh hơn.
- JaCoCo có warning execution data cũ của lớp Notification; test đạt nhưng báo cáo này không dùng coverage làm bằng chứng.
- Seed cần database rỗng và không tự cập nhật database hiện tại. Script schema có bước xóa database; chỉ chạy sau khi được duyệt riêng. Khi nạp, KPI/dashboard tăng theo dữ liệu mới; khi thao tác, trạng thái demo sẽ thay đổi và deadline trôi theo thời gian.
- Chưa kiểm tra merge thử với main trong lần lập báo cáo; không suy ra không conflict chỉ từ `diff --check`. Main được fetch lại để chốt base, chưa merge thêm.
- Giữ nguyên thư mục project gốc đang có conflict tài liệu; mọi công việc ở worktree riêng nêu trên. Không reset, clean, sửa dữ liệu database hay đổi credentials.
- Khi chuẩn bị PR: kiểm tra main/remote lại, push các commit local theo yêu cầu riêng, kiểm tra mergeability và CI; không đưa scratch, target, cấu hình local hoặc mật khẩu vào PR.

## 7. Nội dung PR đề xuất

**Title:** `feat(offer): unify keyword and salary search, align filtered exports and add demo fixtures`

**Mô tả ngắn:** Nâng cấp ô tìm kiếm Offer hiện có để dùng chung quy tắc cho HR, Director và Excel, hỗ trợ tên/email/vị trí/phòng ban không dấu, #ID và số tiền định dạng VNĐ/VND. Sửa export FILTERED để khớp bộ lọc đã áp dụng; giữ bố cục và phân quyền hiện tại. Bổ sung 16 ứng viên demo cho 8 trạng thái, giữ nguyên dữ liệu mẫu cũ. Đã đạt 307 test Java Offer, 23 test frontend và kiểm tra seed; chưa thực thi seed vào database.

Phạm vi PR không bao gồm thay đổi đã merge ở PR #171, không đổi schema, nghiệp vụ Application hoặc cấu trúc layout chung.

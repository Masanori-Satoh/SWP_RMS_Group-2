> Ghi chú 11/11/2026: đường dẫn CSS trong tài liệu đã được cập nhật theo vị trí hiện hành; các kết quả Task/PR lịch sử được giữ nguyên.

# Hoàn thiện giao diện, nghiệp vụ và giao dịch Offer

**Nhánh:** `feat/huyenpt/offer-workflow-completion`.
**Base:** `main`, commit `3e870c04`, đã merge PR #160, #161 và cập nhật Interview #162.

## Phạm vi gộp theo yêu cầu

Phần còn lại được chia thành đúng hai PR: PR hiện tại chứa code, test, migration và model database cần đi cùng tính năng; PR cuối chứa tài liệu audit, hướng dẫn học nghiệp vụ và báo cáo lịch sử. Tên nhánh/PR không dùng số thứ tự.

Code production và migration được đối chiếu với snapshot Offer hoàn thiện `3b677872`, không bỏ các bản sửa candidate nhận việc hoặc link notification. Các regression về actor, hồ sơ kết thúc, giới hạn phản hồi và nút thao tác đã thêm ở PR #161 được giữ lại. Không ghi đè source của Application hoặc module khác.

## Thay đổi chức năng

- Đồng bộ tên hiển thị “Thư mời nhận việc”; chuẩn hóa popup/nút/keyboard, thông báo thao tác và lỗi form. Form giữ dữ liệu đã nhập khi validation thất bại; gợi ý lương thử việc làm tròn lên đúng tỷ lệ.
- Gom request JSON, toast và fragment xác nhận; dùng form GET cho bộ lọc; tách JS form và CSS chi tiết Candidate; bỏ các phần legacy/trùng lặp.
- Lưu số ngày thử việc HR nhập xuyên suốt tạo/sửa, chi tiết, email và Excel, thay việc hiển thị giá trị cố định.
- Chống ghi đè quyết định/phát hành và phản hồi đồng thời; phản hồi candidate kiểm tra điều kiện trong UPDATE, đổi trạng thái hồ sơ cùng giao dịch và rollback nếu xung đột.
- Notification chỉ chạy sau commit trong giao dịch riêng, tải lại entity trước khi dùng quan hệ LAZY. Nhận/từ chối không bị rollback khi notification lỗi; link HR mở chi tiết Offer, link HM mở hồ sơ ứng viên.
- Email gửi sau commit và phản ánh kết quả gửi/test recipient trên UI; lỗi hệ thống trên form được che bằng thông báo chung.
- Batch dữ liệu điều kiện tạo và thông tin duyệt cho export, tái sử dụng bộ lọc; sửa payload export. Workbook ghi theo cửa sổ 100 hàng, giữ định dạng và thống kê cả Expired.
- Hỗ trợ trạng thái Expired, bộ lọc/nhãn tương ứng và query Candidate giữ thư mời hết hạn. Job tắt mặc định; không phát hành lại, không tự đóng hồ sơ hoặc tin tuyển dụng.

## Database và tương thích API

- Migration 020 thêm ProbationDays, mặc định 60 cho dữ liệu cũ, CHECK 1..365. Migration 021 mở rộng CHECK cho Expired và giữ các trạng thái cũ; không tự đổi trạng thái Offer/Application.
- Schema và model đi cùng migration. Model bổ sung mô tả ProbationDays để khớp cột đã thêm.
- Khi triển khai trên database chưa có thay đổi: kiểm tra và áp dụng 020/021 trước khi khởi động app với ddl-auto=validate. Không để Hibernate tự đổi schema.
- Chưa bật app.offer.expiration.enabled; chỉ bật sau khi hai bên xác nhận tích hợp. Không thêm trạng thái mới cho Application.
- Luồng tạo/sửa dùng form hiện hữu; các mutation JSON legacy không còn frontend gọi được retire có kiểm soát (415/405). Route view, khóa cột export và quyền của PR #161 được giữ.
- PR chứa đúng phần notification Offer đã được người dùng duyệt; không sửa nghiệp vụ notification khác, SecurityConfig hoặc shared layout.
- Link notification mới dùng trang đích theo người nhận. Script sửa hai ID 19/20 của database local không đưa vào migration dùng chung; dữ liệu thông báo cũ ở môi trường khác cần kiểm tra riêng.

## Kiểm thử trên worktree của PR

| Nhóm | Test đạt |
|---|---:|
| OfferIntegrationTests | 60 |
| CandidateOfferWebTests | 19 |
| OfferServiceTests | 141 |
| CandidateOfferServiceTests | 46 |
| OfferExportServiceTests | 12 |
| NotificationServiceImplTests | 49 |
| MyApplicationJourneyTests | 73 |
| ApplicationProgressResolverTests | 37 |
| MyApplicationServiceTests | 27 |
| OfferPersistenceRegressionTests trên SQL riêng | 46 |
| Tổng Maven | **510** |
| Node frontend | **20** |

Các lần xác minh cuối không lỗi hoặc bỏ qua. Bộ SQL bao gồm query budget, deadline boundary, quyền xem, race, rollback notification, HR khác HM và nhận/từ chối thực. Migration 020/021 chạy hai lần trên fixture riêng, giữ tám trạng thái cũ, default 60, CHECK trusted, chặn 0 và nhận 1/365 ngày.

Worktree không có cấu hình bí mật local nên lần SQL đầu bị lỗi đăng nhập; lần sau dùng lại cấu hình sẵn có chỉ trong môi trường tiến trình Maven. Không thay thông tin đăng nhập, không copy cấu hình vào Git, không chạy migration hoặc test trên database ứng dụng. Database tạm đã dọn; giữ nguyên các file scratch/target.

Git diff check và merge với origin/main được kiểm tra trước push. Không chạy browser A/B hoặc SpringBootTest phụ thuộc database ứng dụng trong lượt tách PR này. Chưa benchmark RAM theo MB; entity/DTO đầu vào và byte[] Excel vẫn ở RAM. Notification là best-effort có log, chưa có outbox/retry bền vững.

Trước push, main có thêm #162 của Interview. Đã merge sạch, xác minh code/schema Offer không đổi, rồi chạy lại 116 test web đạt: InterviewResultWebTests 25, InterviewScheduleFormWebTests 12, OfferIntegrationTests 60 và CandidateOfferWebTests 19. Không ghi đè thay đổi thành viên khác. Log: scratch/offer-workflow-completion-main-refresh.log.

Log cục bộ không commit: scratch/offer-workflow-completion-tests.log, scratch/offer-workflow-completion-sql-v2.log, scratch/offer-workflow-completion-js.log, scratch/offer-workflow-migration-proof.log.

## Manifest chi tiết

| File | Thay đổi | + / - |
|---|---|---:|
| `database/migrations/020_offer_probation_days.sql` | Thêm cột thử việc, default và CHECK; chạy lại an toàn. | 22 / 0 |
| `database/migrations/021_offer_expired_status.sql` | Mở rộng CHECK Expired, giữ trạng thái cũ. | 24 / 0 |
| `database/schema/db.sql` | Schema chuẩn có ProbationDays và Expired. | 3 / 0 |
| `docs/database/model.md` | Mô tả cột thử việc và trạng thái hết hạn. | 2 / 1 |
| `src/main/java/com/group2/rms/notification/service/NotificationServiceImpl.java` | Link phản hồi Offer theo người nhận HR/HM. | 2 / 2 |
| `src/main/java/com/group2/rms/offer/controller/OfferController.java` | Gọn xử lý form/lỗi, feedback email, retire JSON mutation legacy. | 79 / 125 |
| `src/main/java/com/group2/rms/offer/dto/CandidateOfferDetailResponse.java` | Phân biệt không thể trả lời với thực sự hết hạn. | 1 / 2 |
| `src/main/java/com/group2/rms/offer/dto/OfferDetailResponse.java` | Cờ hết hạn cho màn chi tiết nội bộ. | 1 / 0 |
| `src/main/java/com/group2/rms/offer/dto/OfferResponse.java` | Trạng thái email, thử việc và hết hạn cho UI. | 7 / 0 |
| `src/main/java/com/group2/rms/offer/dto/UpdateOfferRequest.java` | Gọn validation và gợi ý dữ liệu cập nhật. | 1 / 2 |
| `src/main/java/com/group2/rms/offer/entity/OfferProposal.java` | Persist thời gian thử việc và tính hết hạn theo Clock. | 15 / 2 |
| `src/main/java/com/group2/rms/offer/repository/CandidateOfferRepository.java` | UPDATE phản hồi nguyên tử với owner, deadline và trạng thái hồ sơ. | 17 / 11 |
| `src/main/java/com/group2/rms/offer/repository/OfferApprovalRepository.java` | Lấy quyết định mới nhất theo lô cho export. | 8 / 0 |
| `src/main/java/com/group2/rms/offer/repository/OfferProposalRepository.java` | Khóa/cập nhật có điều kiện, batch, lọc chung và chốt hết hạn. | 84 / 0 |
| `src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java` | Quyền phản hồi/nhãn chung, giao dịch nguyên tử và notification sau commit. | 101 / 70 |
| `src/main/java/com/group2/rms/offer/service/OfferExpirationJob.java` | Job hết hạn độc lập, tắt mặc định, không đổi Application. | 61 / 0 |
| `src/main/java/com/group2/rms/offer/service/OfferExportServiceImpl.java` | Giảm query, đúng payload/bộ lọc, streaming workbook và tổng Expired. | 73 / 97 |
| `src/main/java/com/group2/rms/offer/service/OfferMailSender.java` | Đúng người nhận và kết quả gửi; nội dung thử việc/email. | 19 / 12 |
| `src/main/java/com/group2/rms/offer/service/OfferService.java` | Bỏ hợp đồng legacy không còn caller. | 1 / 19 |
| `src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java` | Gọn workflow, đúng ngày/thử việc, chống race và cách ly notification. | 212 / 316 |
| `src/main/resources/static/css/pages/candidate-offer-detail.css` | CSS riêng trang chi tiết Candidate. | 897 / 0 |
| `src/main/resources/static/css/pages/candidate-offers.css` | Giữ CSS danh sách, bỏ phần chi tiết đã tách và rule dư. | 32 / 1243 |
| `src/main/resources/static/css/pages/offer-common.css` | Dùng token, thành phần chung và dọn CSS dư. | 142 / 288 |
| `src/main/resources/static/css/pages/offer-director.css` | Đồng bộ popup Director, bỏ selector không dùng. | 43 / 201 |
| `src/main/resources/static/css/pages/offer-hr.css` | Chuẩn hóa form/export và nút chọn cột, bỏ rule dư. | 17 / 94 |
| `src/main/resources/static/js/pages/candidate-offers.js` | Đồng bộ popup phản hồi và xử lý giao diện Candidate. | 16 / 13 |
| `src/main/resources/static/js/pages/offers/offer-common.js` | Helper JSON/CSRF/toast và phát hành dùng chung. | 56 / 39 |
| `src/main/resources/static/js/pages/offers/offer-director.js` | Gom xử lý quyết định, feedback và popup. | 48 / 221 |
| `src/main/resources/static/js/pages/offers/offer-form.js` | Tách logic form, giữ dữ liệu nhập và làm tròn gợi ý lương. | 174 / 0 |
| `src/main/resources/static/js/pages/offers/offer-hr.js` | Giảm handler trùng, bộ lọc GET và payload Excel đúng backend. | 37 / 265 |
| `src/main/resources/templates/offers/candidate-detail.html` | CSS riêng và popup phản hồi đồng nhất. | 3 / 2 |
| `src/main/resources/templates/offers/candidate-list.html` | Thuật ngữ/trạng thái và popup Candidate. | 4 / 4 |
| `src/main/resources/templates/offers/detail.html` | Nhãn hết hạn, popup/nút theo quyền và ngôn ngữ thống nhất. | 32 / 43 |
| `src/main/resources/templates/offers/director-list.html` | Bộ lọc GET, popup quyết định và trạng thái hết hạn. | 40 / 38 |
| `src/main/resources/templates/offers/form.html` | Ngôn ngữ và nạp script riêng, giữ binding hiện hữu. | 7 / 8 |
| `src/main/resources/templates/offers/fragments/confirmations.html` | Fragment xác nhận gửi/xóa dùng chung trong Offer. | 34 / 0 |
| `src/main/resources/templates/offers/list.html` | Bộ lọc, thuật ngữ, export, popup và trạng thái hết hạn. | 56 / 70 |
| `src/test/java/com/group2/rms/notification/service/NotificationServiceImplTests.java` | Test link nhận/từ chối theo người nhận. | 25 / 2 |
| `src/test/java/com/group2/rms/offer/OfferExportServiceTests.java` | Hợp đồng export, số liệu và workbook nhiều dòng. | 363 / 0 |
| `src/test/java/com/group2/rms/offer/OfferIntegrationTests.java` | HTTP/form/JSON, quyền và render UI; giữ regression PR #161. | 189 / 100 |
| `src/test/java/com/group2/rms/offer/controller/OfferControllerBugTests.java` | Đồng bộ fixture cho hợp đồng controller. | 1 / 1 |
| `src/test/java/com/group2/rms/offer/repository/CandidateOfferQueryTests.java` | Đồng bộ câu query và phản hồi với contract mới. | 8 / 3 |
| `src/test/java/com/group2/rms/offer/repository/OfferPersistenceRegressionTests.java` | Regression SQL riêng cho query, race, rollback, notification và hết hạn. | 601 / 0 |
| `src/test/java/com/group2/rms/offer/service/CandidateOfferServiceTests.java` | Owner, điều kiện phản hồi và notification sau commit. | 139 / 50 |
| `src/test/java/com/group2/rms/offer/service/OfferServiceTests.java` | Workflow, actor, ngày/lương/thử việc và hồi quy PR #161. | 297 / 241 |
| `src/test/js/offer-frontend.test.cjs` | 20 test runtime JS cho form, request và UI helpers. | 148 / 0 |
| `docs/members/huyenpt/pr-review/offer-workflow-completion.md` | Mô tả phạm vi, triển khai, test và manifest của PR. | Tài liệu mới |

Tổng: 46 file code/test/schema/model và một báo cáo PR. Tài liệu lịch sử/hướng dẫn cá nhân còn lại dành cho nhánh docs/huyenpt/offer-workflow-review, chỉ thực hiện sau khi PR này được người dùng kiểm tra và merge.

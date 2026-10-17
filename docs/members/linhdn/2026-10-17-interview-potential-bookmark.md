# Lưu tiềm năng và ghi chú chung trong kết quả phỏng vấn

Ngày: 17/10/2026. Phạm vi: Talent Pool — LinhDN. Không commit/push.

## Màn hình và điểm tích hợp

- Dùng trang có sẵn `/interviews/{id}/result`, truy cập từ liên kết **Kết quả** trong Lịch phỏng vấn.
- HR đã có quyền xem trang này cả khi chưa chốt kết quả; HM dùng cùng trang để xem đánh giá và kết quả cuối.
- Thêm nhóm thao tác nhỏ dưới header, gần các thao tác hiện có. Ô ghi chú mở theo yêu cầu, không dùng modal lớn và không thay bố cục phỏng vấn.
- Chỉ HR/HM nhận component; Interviewer, Director, Admin giữ giao diện trước đó.
- Bookmark độc lập với kết quả Passed/Failed. Mở trang hay chốt kết quả không tự đánh dấu Candidate.

## Phạm vi file

| File | Thay đổi / người phụ trách |
| --- | --- |
| `src/main/resources/templates/interview/result.html` | File thuộc Interview / DucNM: chỉ thêm 6 dòng để gọi fragment, tải CSS/JS theo vai trò. Không đổi form, route hay điều kiện chốt kết quả. |
| `src/main/resources/templates/talent-pool/potential-candidate-action.html` | Fragment Talent Pool mới, nhận Application ID và tên Candidate đã có trong model. |
| `src/main/resources/static/css/pages/talent-pool-potential-action.css` | CSS có tiền tố riêng, dùng token và control trong catalog hiện tại. |
| `src/main/resources/static/js/pages/talent-pool-potential-action.js` | Adapter UI cho API bookmark hiện có; không gửi yêu cầu tới Interview/Offer. |
| `src/test/java/com/group2/rms/candidate/InterviewPotentialWebTests.java` | Test render, tích hợp endpoint, CSRF, phân quyền và validation. |
| `src/test/java/com/group2/rms/candidate/InterviewPotentialIntegrationTests.java` | Test trên SQL Server local, transaction rollback, đối chiếu dữ liệu trước/sau. |
| `src/test/js/talent-pool-potential-action.test.cjs` | Test trạng thái frontend và xử lý lỗi. |

Không sửa Java production, DTO, controller/service Interview, schema, enum, dependency, layout chung, token hay CSS/JS phỏng vấn. Không chạm module Candidate List hoặc Job Management.

## Backend được tái sử dụng

- `GET /talent-pool/applications/{id}/bookmark`: trả Candidate ID, trạng thái lưu và ghi chú thực tế.
- `POST /talent-pool/applications/{id}/bookmark`: đánh dấu Candidate và ghi chú tùy chọn trong cùng transaction của `TalentPoolService.saveBookmark`.
- `Candidate.IsPotential`, `TalentPoolNote` và audit đánh dấu hiện có. Không thêm endpoint/entity.
- Server tự tìm Candidate qua Application và kiểm tra quyền. Không nhận Candidate ID, tác giả hoặc cờ IsPotential do frontend quyết định.
- Candidate đã lưu: gửi ghi chú mới qua cùng API; backend không đánh dấu/audit lặp và không ghi đè ghi chú cũ.
- Không gọi endpoint bỏ lưu. Không sao chép Interview Evaluation vào ghi chú chung.

Không tái sử dụng modal Application lớn vì script đó gắn với cấu trúc trang `.app-page`. Fragment/adapter nhỏ thuộc Talent Pool giữ nguyên API và control catalog, tránh kéo logic Application vào trang của thành viên khác.

## Hành vi frontend

1. Ban đầu hiện đang tải, chưa giả định Candidate chưa lưu. Trạng thái lấy bằng GET từ backend.
2. Chưa lưu: bookmark outline **Lưu ứng viên tiềm năng**, mở form, ghi chú không bắt buộc.
3. Đã lưu: bookmark active **Đã lưu tiềm năng**, nút **Xem / thêm ghi chú**. Ghi chú mới phải có nội dung; không bắt lưu lại flag bằng thao tác riêng.
4. Hiện tác giả, thời gian và nội dung thực; dùng `textContent` để hiển thị văn bản an toàn. Giữ giới hạn 1.000 ký tự, không thêm dòng hướng dẫn dài.
5. Đang gửi: khóa thao tác lặp, hiện **Đang lưu…**. Chỉ báo thành công khi API trả response hợp lệ và `saved=true`.
6. Lỗi: giữ bản nháp, giữ trạng thái đã xác nhận gần nhất, không báo thành công. Yêu cầu tải lại trạng thái bằng GET trước khi gửi lại, giảm nguy cơ ghi chú trùng sau timeout.
7. Có label, focus trả về nút mở khi đóng, `aria-expanded`, live status và thông báo lỗi. Không dùng localStorage/sessionStorage để lưu trạng thái bookmark.

## Bảo mật và giới hạn nghiệp vụ hiện có

Giữ nguyên Spring Security, CSRF và `ApplicationAccess`. HR thao tác trên hồ sơ được phép; HM cần Application thuộc phòng ban quản lý và đã được HR chuyển hoặc có nguồn Talent Pool. Quyền xem Interview của HM rộng hơn quyền Application: nếu xem được Interview nhưng thiếu quyền Application, API bookmark vẫn từ chối và component hiển thị lỗi, không nới quyền để vượt qua.

Không thiếu backend bắt buộc cho phạm vi này. Không thay đổi ApplicationStatus, InterviewStatus, Pass/Fail, Offer, JobPosting hoặc Requisition.

## Kiểm thử

- Hồi quy Java: **192 passed, 0 failure/error/skipped**, gồm 11 test MVC mới và các suite Interview, ApplicationAccess, TalentPoolBookmark, ApplicationPipeline hiện có.
- JavaScript toàn bộ thư mục `src/test/js`: **34 passed**, gồm 8 test mới cho component.
- Syntax check JavaScript và `git diff --check`: qua.
- SQL Server thật: **3 passed, 0 failure/error/skipped**. HM lưu sau Passed và Failed; HR lưu không ghi chú rồi thêm ghi chú, kiểm tra không đánh dấu/audit lặp, giữ lịch sử và từ chối nội dung quá dài.
- Tổng lượt kiểm chứng Java cuối cùng: **195 passed trong 11 suite**, không failure/error/skipped. Đây là bộ test tập trung cho thay đổi này và hồi quy liên quan, không phải toàn bộ test của repository.

Integration test gửi request qua MockMvc → security/controller/service/repository → SQL Server. Snapshot toàn bộ cột của Application, InterviewSchedule, InterviewEvaluation, InterviewFinalResult, OfferProposal, JobPosting và JobRequisition giữ nguyên sau bookmark/ghi chú; số Candidate/Application không tăng. HR đọc được ghi chú đúng tác giả HM, Talent Pool list phản ánh Candidate vừa lưu. Transaction rollback khi kết thúc từng test.

Lệnh kiểm thử dùng POM tạm trong `target/interview-potential-verification-pom.xml`, tách output build khỏi IDE. Mọi lượt integration đều truyền `spring.jpa.hibernate.ddl-auto=validate`; test ghi dữ liệu trong transaction rollback, không phát sinh thay đổi schema.

Lượt SQL đầu chưa tìm thấy mẫu đồng thời chưa lưu tiềm năng và đủ quyền HM. Test được điều chỉnh chuẩn bị riêng cờ IsPotential=false trong transaction rollback của fixture, không sửa quyền hoặc Interview/Application để làm test qua.

Kiểm tra trình duyệt local: HR đi từ Lịch phỏng vấn → Kết quả buổi #13, tải đúng trạng thái đã lưu, mở/đóng ghi chú; HM xem cùng trang và tải được trạng thái. Kết quả cuối/phiếu đánh giá vẫn hiện. Chưa thực hiện thao tác ghi hoặc chốt Pass/Fail bằng trình duyệt; không xem đây là nghiệm thu đầy đủ mọi luồng browser.

## Nghiệm thu thủ công bổ sung

- HM chốt Pass hoặc Fail như trước, sau đó có thể lưu tiềm năng độc lập.
- Candidate đã lưu: mở ghi chú, thêm nội dung, tải lại để kiểm tra lịch sử; không có hành động bỏ lưu tại đây.
- HR mở kết quả từ Lịch phỏng vấn, lưu với ghi chú rỗng hoặc thêm ghi chú cho Candidate đã lưu.
- Ngắt mạng/hết phiên: lỗi rõ ràng, không mất bản nháp hoặc báo thành công giả.
- Interviewer vẫn sử dụng form đánh giá cũ; không nhận UI hoặc dữ liệu ghi chú Talent Pool từ component này.

# HM đề xuất ứng viên — chức năng thực tế, giữ schema hiện tại

## Phương án đã chốt

Tái sử dụng `Application` trạng thái `HM_Suggested` làm bản nháp nội bộ. HM chọn Candidate đã được đánh dấu và một CV thật từ Application cũ, thêm vào Job Posting khác. Không tạo Candidate mới hoặc sửa Application nguồn.

HR liên hệ thủ công. Đồng ý dùng CV cũ: chuyển bản nháp sang `HM_Passed`, cập nhật thời điểm xác nhận và mở trang tạo lịch. Từ chối: chuyển bản nháp sang `Rejected`, ghi `TalentPoolCancelled` để phân biệt với đơn chính thức bị loại. Không thêm trạng thái hoặc bảng/cột/constraint.

## Rà soát ảnh hưởng trước triển khai

| Khu vực | Kết quả và cách xử lý |
| --- | --- |
| Talent Pool | Giữ bookmark/IsPotential và ghi chú chung. Preflight chỉ đọc; submit thực hiện kiểm tra lại và lưu trong transaction. |
| Application pipeline | Query danh sách và lựa chọn job trước đây chưa dùng `FORMAL_FILTER`; bổ sung để loại cả bản nháp chờ và đề xuất đã kết thúc. Số đếm giai đoạn được tính trên danh sách ID này. |
| Candidate/My Applications | Các query sở hữu đã dùng `FORMAL_FILTER`, tiếp tục ẩn đề xuất nội bộ. Candidate tự nộp CV mới vẫn đi nhánh `Applied` có sẵn, không đổi logic. |
| Dashboard | Các tổng Application đã dùng `FORMAL_FILTER`. Bổ sung cho tổng Candidate theo requisition của HM để tránh bản nháp làm tăng tổng này. |
| Quyền HM | Giữ `TalentPoolSuggested` trong audit làm bằng chứng nguồn; HM vẫn xem được đơn được HR xác nhận dù chưa có ApplicationReview HR Pass. Không thay SecurityConfig. |
| AI | Không phát sự kiện chấm AI khi đề xuất hoặc HR xác nhận dùng CV cũ. Endpoint duyệt/chấm lại vẫn kiểm tra trạng thái theo logic cũ. |
| Interview | `ApplicationStatusService.requireScheduleable` chấp nhận `HM_Passed`, kiểm tra job mở và vòng phỏng vấn. Query chọn hồ sơ cũng nhận trạng thái này. Giữ nguyên ApplicationId và code Interview; mở trang không tự tạo lịch. |
| Offer | Chỉ xử lý Application đã vào quy trình chính thức; không sửa module Offer. |
| Đóng job | Logic có sẵn kết thúc đề xuất chờ và ghi audit riêng; bộ lọc mới giữ các dòng này khỏi hồ sơ chính thức. Không đổi module Job Posting. |
| Email | Không gửi thư tự động lúc HM thêm. Luồng mời phỏng vấn hiện có không nhận `HM_Suggested`/`Rejected`; không sửa module gửi mail. |
| Admin | Thống kê hạ tầng đếm dòng lưu trữ vẫn có thể tính cả bản nháp; đây là tổng vật lý, không phải số hồ sơ chính thức. Không sửa màn quản trị. |

Không phát hiện xung đột bắt buộc đổi schema cho luồng đã chốt. Chống trùng dựa trên khóa Job Posting đang có; phạm vi bảo vệ là các thao tác qua service, không phải SQL ghi tay ngoài ứng dụng.

## Backend

- `POST /talent-pool/jobs/{id}/selection-review`: JSON `sourceApplicationIds`, trả từng lỗi theo Candidate/CV, `submissionSupported=true`; không ghi.
- `POST /talent-pool/jobs/{id}/suggest`: cùng DTO JSON, trả `ApiResponse` với `jobPostingId`, `addedCount` sau khi transaction service hoàn tất. Endpoint trước đây nhận form; toàn bộ call-site trong project đã được rà soát và caller hiện tại là popup mới.
- HM đúng phòng ban, tin Published/chưa hết hạn, Candidate còn tiềm năng, CV thuộc một đơn chính thức của Candidate, thuộc job khác, file CV hợp lệ/tồn tại theo cơ chế storage hiện có.
- Chống chọn nhiều CV của cùng Candidate và chống Candidate đã có đơn chính thức hoặc đề xuất đang chờ cho job đích. Giới hạn 20/validation dương/không rỗng do DTO.
- Giữ `findForUpdate(jobId)` trong transaction, cùng cơ chế khóa mà nhánh Candidate tự nộp đang dùng. Xác thực toàn bộ batch trước insert; lỗi lưu/audit làm transaction rollback.
- Mỗi dòng mới chỉ tham chiếu Candidate hiện có, job đích và URL CV nguồn; ghi audit `TalentPoolSuggested` kèm source Application ID. Không cập nhật nguồn.
- HR xác nhận bắt buộc consent, job mở, CV tồn tại, trạng thái còn chờ, chưa có hồ sơ chính thức khác; update có điều kiện `HM_Suggested → HM_Passed` chống xác nhận lặp.
- Lịch sử lấy sự kiện thật từ AuditLog: HM đề xuất, HR xác nhận, Candidate nộp CV mới hoặc kết thúc đề xuất. Không gán hành động nộp CV cho Candidate chỉ vì HR dùng CV cũ.
- DTO `proposalOnly` chỉ là dữ liệu render, không phải cột DB. Đề xuất kết thúc vẫn hiển thị lịch sử riêng khi mở link cũ.

## UI

- Mở modal tự kiểm tra danh sách; chỉ bật Xác nhận khi toàn bộ lựa chọn hợp lệ.
- Khi submit, dùng JSON + X-CSRF-TOKEN; chặn gửi lặp/đóng popup lúc đang lưu. Backend luôn kiểm tra lại, không tin kết quả preflight cũ.
- Chỉ báo thành công và xóa draft sau response thành công; có link tới danh sách HM đề xuất được đọc từ DB. Lỗi giữ lựa chọn và thông báo lỗi, cho kiểm tra lại.
- “Kiểm tra lại” hiện khi lỗi/không hợp lệ, bỏ thông báo chức năng chưa sẵn sàng.
- Giữ toàn bộ cơ chế lọc/phân trang, mapping Candidate–CV, preview và notes đã triển khai.

## File chính sửa/thêm

- `candidate/controller/TalentPoolController.java`: submit JSON gọi service thật.
- `candidate/dto/TalentPoolSuggestResponse.java`: kết quả lưu; `ApplicationDetailResponse.java`: phân loại render.
- `candidate/service/TalentPoolService.java`: preflight, batch lưu, xác nhận.
- `candidate/repository/ApplicationRepository.java`: query hồ sơ chính thức và lịch sử audit.
- `candidate/service/ApplicationPipelineService.java`: dựng timeline và đề xuất đã kết thúc.
- `dashboard/DashboardMetricsRepository.java`: một query tổng theo requisition.
- `templates/talent-pool/list.html`, `suggestions.html`, `candidate/application-detail.html`, `static/js/pages/talent-pool.js`, `static/css/pages/talent-pool.css`: modal, thông báo và phân loại đề xuất.
- Test service, MVC, repository, JS; thêm `TalentPoolFlowIntegrationTests` và `TalentPoolSuggestionTests`.
- `pom.xml`: Surefire đặt ddl-auto=validate cho test; các repository test liên quan cũng khai báo validate. Không thêm dependency, không đổi cấu hình runtime của ứng dụng.

## Kiểm chứng

Hai integration test đã qua trên SQL Server local với MockMvc/service/repository thật, không mock thao tác thành công:

1. HM POST lưu → có dòng Application mới đúng Candidate/CV/status; số Candidate và nguồn cũ giữ nguyên; chưa xuất hiện ở pipeline/dashboard/My Applications; HR đọc danh sách thật → consent → HM_Passed → hiện ở pipeline và đủ điều kiện Interview; HM vẫn có quyền xem. Gửi lại đề xuất bị chặn.
2. HR từ chối → Rejected + audit; dòng còn trong DB nhưng không xuất hiện ở hồ sơ chính thức/số đếm, không đủ điều kiện phỏng vấn; detail hiển thị đề xuất đã kết thúc.

Các dòng test/audit thuộc transaction rollback; không giữ lại đề xuất thử nghiệm. Giá trị identity do SQL Server cấp có thể đã được tiêu thụ trong test.

Kết quả cuối của 18 suite Java liên quan: **289 test được báo cáo, 288 passed, 1 skipped, 0 failure/error**. Test bị bỏ qua do DB local chưa có đơn đã rút để đối chiếu query. Trong đó hai integration test nêu trên đều passed, không skipped. **20 test JavaScript passed** (9 state, 8 UI, 3 preview); kiểm tra cú pháp JavaScript và `git diff --check` qua.

Lượt hồi quy chạy lại với `-Dspring.jpa.hibernate.ddl-auto=validate` hoàn tất exit code 0; log không có lệnh CREATE/ALTER/DROP. Không cộng lượt chạy bị dừng vào kết quả nghiệm thu.

Chưa có nghiệm thu thao tác trình duyệt đầy đủ; không coi test JS giả lập DOM hoặc MockMvc là bằng chứng browser E2E.

### Sự cố cấu hình test và kiểm tra schema

Lượt integration trên đặt `ddl-auto=validate` ngay từ đầu. Một lượt hồi quy mở rộng sau đó kế thừa cấu hình local sẵn có `spring.jpa.hibernate.ddl-auto=update`; Hibernate đã phát các lệnh ALTER cho 14 cột NVARCHAR(MAX). Lượt đó được dừng sau khi phát hiện, không tính là lượt nghiệm thu.

Đã đối chiếu metadata sau sự cố: 14/14 cột NVARCHAR(MAX) khai báo trong `database/schema/db.sql` hiện có kiểu tương ứng, không thiếu cột trong nhóm này. Không có snapshot metadata trước lượt chạy để khẳng định mọi thuộc tính DB trước/sau tuyệt đối giống nhau. Không tự chạy SQL để “khôi phục” khi không có baseline.

Đã đặt Surefire test-only override validate và validate riêng cho repository tests liên quan; lượt chạy lại dùng thêm JVM `-Dspring.jpa.hibernate.ddl-auto=validate`. Không có sửa đổi file schema/migration, entity bảng, FK hoặc constraint trong source. Cấu hình local runtime sẵn có vẫn giữ nguyên.

## Kiểm tra thủ công

1. HM mở job đang đăng trong phòng ban mình → Thêm ứng viên tiềm năng → chọn Candidate/CV cũ → xác nhận.
2. Xem thông báo thành công và mở danh sách HM đề xuất; thử chọn lại cùng Candidate/job để kiểm tra thông báo trùng.
3. HR mở danh sách, đọc CV/ghi chú/liên hệ; chưa xác nhận thì hồ sơ không xuất hiện trong danh sách đơn chính thức.
4. HR xác nhận consent: mở trang tạo lịch, kiểm tra Application đúng người/job; chỉ lưu lịch khi muốn tạo phỏng vấn thật.
5. Với đề xuất khác, HR từ chối: kiểm tra không biến thành hồ sơ bị loại trong pipeline chính thức.

Không commit/push.

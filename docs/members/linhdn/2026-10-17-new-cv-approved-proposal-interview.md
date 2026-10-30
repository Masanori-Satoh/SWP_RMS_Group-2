# Candidate nộp CV mới cho đề xuất đã được HM duyệt

> Lịch sử: nhánh Candidate tự nộp CV đi thẳng `HM_Passed` trong báo cáo này đã được bỏ ngày 28/10/2026. Hành vi mới là `Applied` và xét duyệt thông thường; xem [báo cáo cập nhật](2026-10-28-new-cv-normal-screening.md). Kết quả test bên dưới thuộc thời điểm 17/10.

Ngày: 17/10/2026. Đây là quyết định mới thay cho nhánh CV mới → Applied trong các báo cáo trước. Không sửa schema; không commit/push.

## Luồng đã chốt

1. HM thêm Candidate tiềm năng vào đúng Job Posting, hoặc HR gợi ý và HM chấp nhận.
2. Application đề xuất ở `HM_Suggested`, chưa là hồ sơ chính thức. HR liên hệ Candidate bên ngoài để hỏi có muốn phỏng vấn và muốn dùng CV cũ hay mới.
3. Nếu Candidate không đồng ý: HR kết thúc đề xuất như trước, giữ lịch sử và cờ tiềm năng của Candidate.
4. Nếu dùng CV cũ: HR tích xác nhận đồng ý trên trang đề xuất → cùng Application chuyển `HM_Passed` → form lên lịch phỏng vấn. Nhánh này không thay đổi.
5. Nếu dùng CV mới: Candidate đăng nhập, mở đúng tin tuyển dụng và nộp PDF qua form ứng tuyển hiện có. Server tìm đề xuất theo Candidate của tài khoản đăng nhập + JobPostingId. Nếu đề xuất đã qua HM, thay URL CV trên cùng Application và chuyển thẳng `HM_Passed`; HR có thể xếp lịch ngay.

Hành động Candidate tự nộp file là xác nhận ứng tuyển cho nhánh CV mới; không cần HR tích thêm checkbox dùng CV cũ. HR vẫn tự liên hệ bên ngoài, không có nút Candidate xác nhận đề xuất mới được bổ sung.

## Điều kiện và ranh giới

- Chỉ cờ `IsPotential=true`, hoặc từng apply một job khác, không đủ để đi tắt. Phải có đề xuất `HM_Suggested` khớp Candidate–JobPosting đang nộp.
- Gợi ý HR còn chờ HM phản hồi chưa được đi thẳng phỏng vấn: Candidate tự nộp ở thời điểm này vẫn là `Applied` và chạy sàng lọc thông thường.
- Không có đề xuất cho tin này: giữ nguyên luồng tự ứng tuyển `Applied` → AI/HR/HM.
- Không sửa Application của lần ứng tuyển nguồn, không tạo Candidate mới. Đề xuất được dùng lại, không tạo Application thứ hai cho cùng lượt nộp.
- Chặn nộp lặp, tin đóng/hết hạn, file không hợp lệ; job lock và update có điều kiện giữ nguyên. Trạng thái đích chỉ được server chọn giữa `Applied` và `HM_Passed`; query cũng chặn nâng gợi ý HR đang chờ thành `HM_Passed`.
- Chỉ nhánh `Applied` phát `ApplicationSubmittedEvent` để chạy AI. Nhánh chuyển thẳng `HM_Passed` không phát event này.
- Giữ audit `TalentPoolCandidateSubmitted`, thời gian nộp mới và cơ chế xóa file mới nếu transaction rollback. CV nguồn cũ giữ nguyên.

## File thay đổi của lượt này

| File | Nội dung |
| --- | --- |
| `src/main/java/com/group2/rms/candidate/service/ApplicationSubmissionService.java` | Xác định đề xuất của đúng Candidate/tin, phân biệt chờ HM, chọn đích và chỉ phát event AI cho luồng thông thường. |
| `src/main/java/com/group2/rms/candidate/repository/ApplicationRepository.java` | Update URL CV + trạng thái đích có điều kiện, chặn vượt bước chờ HM. |
| `src/test/java/com/group2/rms/candidate/service/ApplicationSubmissionServiceTests.java` | Cập nhật kỳ vọng nhánh HM, thêm ca chờ HM và xung đột xử lý/file rollback. |
| `src/test/java/com/group2/rms/candidate/TalentPoolFlowIntegrationTests.java` | Thêm 4 ca multipart upload qua security/controller → SQL Server/file storage thật, rollback dữ liệu test. |

Không sửa module Interview, Offer, controller/UI ứng tuyển hoặc API route trong lượt này. Các thay đổi bookmark Interview chưa commit từ lượt trước được giữ nguyên.

## Kiểm chứng

- **99/99 test đơn vị/hồi quy qua**, thuộc 7 suite: ApplicationSubmissionService, AiScreeningService, AiScreeningListener, ApplicationStatusService, TalentPoolRecommendation, ApplicationProgressResolver và InterviewSchedulingService.
- **8/8 test Flow B/C trên SQL Server thật qua** (253,1 giây), gồm 4 test cũ và 4 ca upload mới: HM thêm; HR gửi/HM chấp nhận; HR gửi/chưa được HM phản hồi; Candidate tiềm năng nhưng không có đề xuất trong tin đích.
- Ca upload dùng multipart request qua MockMvc → Spring Security/controller/service → SQL Server và file storage thật. Hai ca được HM duyệt giữ nguyên ApplicationId, chuyển `HM_Passed`, không phát event AI, được `requireScheduleable` chấp nhận và HR mở được form `/interviews/new?applicationId=...`.
- Hai ca chưa đủ điều kiện vẫn `Applied`, phát event sàng lọc và chưa được xếp lịch. Mọi ca giữ CV/trạng thái Application nguồn, không tạo Candidate, giữ số Application đúng, kiểm tra PDF mới đọc được và chặn nộp lần hai.
- Query được kiểm tra trực tiếp: không cho đổi gợi ý HR đang chờ HM sang `HM_Passed`.
- Tất cả integration test dùng `spring.jpa.hibernate.ddl-auto=validate`; không thấy câu DDL trong log. Dữ liệu test rollback, file mới dọn theo callback transaction. AI chạy sau commit nên không thực thi trong các transaction rollback; việc phát/không phát event đã được kiểm tra.
- **CareerFlowTests: 22/24 qua, 2 failure, 0 error/skipped**. Hai test `publicJobBoardRendersPublishedJobsForGuests` và `emptyJobBoardShowsRecoveryState` gọi `/` nhưng kỳ vọng giao diện job board. Controller hiện có trả `candidate/landing` cho `/`, còn job board ở `/jobs`. Đối chiếu `HEAD` và diff xác nhận controller và file test này không bị thay đổi; MVC slice còn mock ApplicationSubmissionService nên không chạy logic service vừa sửa. Không sửa homepage hoặc các test ngoài phạm vi để che lỗi này.
- Tổng của các lượt kiểm tra trong thay đổi này: **131 test, 129 passed, 2 failure ở homepage nêu trên**. Không tuyên bố toàn bộ bộ test xanh.
- `git diff --check`: qua. Không chạy thao tác ghi dữ liệu qua trình duyệt; bằng chứng chức năng là HTTP MockMvc + DB/file thật, có rollback. Không tạo lịch phỏng vấn thực tế hoặc gửi email cho Candidate.

Lệnh chạy dùng POM tạm `target/interview-potential-verification-pom.xml`, tách output khỏi IDE. Log trong `target/new-cv-hm-passed-unit-tests.log`, `target/new-cv-hm-passed-integration-tests.log`, `target/new-cv-hm-passed-web-tests.log` (đều nằm trong thư mục build được ignore).

# 02/11/2026 — Việt hóa tên vai trò trong Kho ứng viên tiềm năng

## Yêu cầu và phạm vi

- Người thực hiện: LinhDN.
- Người dùng yêu cầu sửa tên `Hiring Manager` còn hiển thị ở tiêu đề và thông báo của Kho ứng viên tiềm năng sang tiếng Việt.
- Tên hiển thị dùng `Trưởng phòng tuyển dụng`; trong câu văn dùng `trưởng phòng tuyển dụng`.

## Thay đổi

| File | Nội dung |
| --- | --- |
| `src/main/resources/templates/talent-pool/detail.html` | Đổi tiêu đề thành `Quyết định của Trưởng phòng tuyển dụng`. |
| `src/main/resources/templates/talent-pool/recommendations.html` | Việt hóa tên vai trò trong thông báo chưa có ứng viên được gợi ý. |
| `src/main/resources/templates/talent-pool/suggestions.html` | Việt hóa tên vai trò trong thông báo chưa có ứng viên được đề xuất. |
| `src/main/java/com/group2/rms/candidate/talentpool/service/TalentPoolService.java` | Việt hóa 3 thông báo về quyền truy cập, quyền tạo đề xuất và tin chưa có người phụ trách hợp lệ để nhận gợi ý. |
| `src/test/java/com/group2/rms/candidate/application/pipeline/controller/ApplicationPipelineWebTests.java` | Cập nhật tiêu đề mong đợi trong ca render chi tiết gợi ý. |

Các chuỗi `Hiring Manager` dùng làm mã vai trò, so sánh phân quyền hoặc điều kiện Thymeleaf được giữ nguyên. Không đổi quy trình gợi ý/đề xuất, trạng thái, CV hoặc ghi chú. Các chữ viết tắt HR/HM không thuộc yêu cầu đổi tên đầy đủ lần này.

## Kiểm chứng

Chạy `mvn -o clean test` với Mockito 5.17.0 được nạp bằng `-javaagent` và chỉ định 4 lớp kiểm tra liên quan:

| Lớp kiểm tra | Kết quả |
| --- | --- |
| `TalentPoolSuggestionTests` | 9/9 qua. |
| `TalentPoolRecommendationTests` | 26/26 qua. |
| `TalentPoolReadUnitTests` | 68/68 qua. |
| `ApplicationPipelineWebTests` | 67/67 qua, có render template chi tiết và các danh sách gợi ý/đề xuất bằng Spring MVC, service mock. |

- Tổng: **170 test, 0 failure, 0 error, 0 skipped — BUILD SUCCESS**.
- Lượt đầu dùng bản biên dịch sẵn có bị lỗi thiếu lớp DTO khi biên dịch test. Chạy lại từ đầu bằng `clean test` đã giải quyết lỗi, không cần thay đổi mã nguồn ngoài phạm vi Việt hóa.
- Đối chiếu service trước/sau khi loại bỏ string literal: không đổi logic; số chuỗi mã vai trò `"Hiring Manager"` giữ nguyên.
- Quét lại template: các chỗ còn `Hiring Manager` đều là điều kiện so sánh vai trò, không phải tiêu đề hoặc câu thông báo.
- `git diff --check`: không có lỗi khoảng trắng.
- Log: `target/talent-pool-vietnamese-test.log`.
- Chưa chạy trình duyệt hoặc kiểm thử với SQL Server thật.

## Database và bàn giao

- Không thay đổi schema, migration, seed hoặc dữ liệu DB.
- Chưa commit/push.

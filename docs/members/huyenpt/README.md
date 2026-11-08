# Tài liệu Offer Management — HuyenPT

## Đọc tài liệu nào trước?

Code hiện hành đã vào main qua [PR #160](https://github.com/Masanori-Satoh/SWP_RMS_Group-2/pull/160), [#161](https://github.com/Masanori-Satoh/SWP_RMS_Group-2/pull/161) và [#163](https://github.com/Masanori-Satoh/SWP_RMS_Group-2/pull/163). Tài liệu này bổ sung cách tra cứu; không thay đổi code hoặc bật tính năng.

1. [Hướng dẫn học luồng hiện hành](offer-workflow-study-guide.md): vai trò, trạng thái, hàm quan trọng và thứ tự kiểm tra nghiệp vụ.
2. [PR hoàn thiện workflow](pr-review/offer-workflow-completion.md): manifest code, migration, hợp đồng API và kết quả kiểm thử trên nền main mới.
3. [Sửa rủi ro, notification và cleanup](offer-risk-fixes-and-safe-cleanup.md): bằng chứng từng đợt, gồm lỗi candidate nhận việc bị rollback và link theo người nhận.
4. [Ngôn ngữ UI và tích hợp hết hạn](offer-completion-language-and-expiry-review.md): phạm vi đã chốt và dữ liệu kiểm thử theo thời điểm.

## Các quy tắc hiện hành cần nhớ

- HR tạo/sửa/xóa bản nháp và phát hành. Director chỉ xem, duyệt/từ chối; endpoint Excel hiện cũng cho phép HR/Director. Không mở rộng quyền chỉ để thuận tiện trên UI.
- Candidate chỉ xem thư mời của mình đã phát hành hoặc đã kết thúc phản hồi; chỉ được trả lời khi Sent_Candidate còn hạn và hồ sơ vẫn Offered.
- Offer Accepted/Declined và Application Hired/Withdrawn được ghi cùng giao dịch. Thông báo chạy sau commit trong giao dịch riêng; đọc lại entity trước khi dùng quan hệ LAZY.
- Link phản hồi gửi HR trỏ chi tiết Offer; link gửi Hiring Manager trỏ hồ sơ Application. Không cấp thêm quyền HM vào Offer.
- Expired không tự đóng hồ sơ/tin tuyển dụng và không có phát hành lại trong phạm vi đã duyệt. Job hết hạn vẫn tắt mặc định.
- Migration 020 lưu ProbationDays, default 60 cho dữ liệu cũ; migration 021 mở rộng CHECK Expired. Kiểm tra môi trường đích trước khi triển khai, không suy từ kết quả local.
- Code Application trên main đã hỗ trợ Expired và nhận biết quá hạn theo thời gian. Điều này không đồng nghĩa được tự bật job khi chưa xác nhận triển khai chung.

## Báo cáo lịch sử và cách sử dụng

| Nhóm | Mục đích | Cách đọc |
|---|---|---|
| [html-css-refactor](html-css-refactor/) | 38 báo cáo audit/thực hiện/kiểm chứng Task 01–25C | Đọc theo số task; số dòng, tên file và test là số liệu tại từng mốc |
| [Audit source cũ](offer-management-source-audit.md) | Điểm kiểm tra ban đầu của backend/frontend | Không coi các lỗi hoặc tên hàm cũ là trạng thái hiện tại |
| [Phân tích luồng cũ](offer-management-flow-analysis.md) | Phân tích chức năng tại mốc trước các lần hoàn thiện | Đối chiếu hướng dẫn hiện hành và code |
| [Kiến thức và bảo vệ](offer-management-knowledge-guide.md) | Giải thích kiến trúc và ví dụ phục vụ học tập | Ví dụ lịch sử có thể khác hợp đồng hiện tại |
| [Bài tập bảo vệ](offer-management-defense-practice.md) | Câu hỏi luyện giải thích luồng | Kiểm tra đáp án bằng code hiện hành trước khi sử dụng |
| [Bàn giao Application](offer-expired-application-handoff.md) | Lưu bốn điểm từng đề nghị Dũng xử lý | Phần code hỗ trợ đã vào main; nội dung “chưa làm” là lịch sử bàn giao |

Task 25A là PoC trên layout chung; cách áp dụng cho Offer là Task 25C. Không dùng PoC làm lý do sửa shared layout. Các đề xuất đã được giữ nguyên trong báo cáo lịch sử không tự trở thành yêu cầu nghiệp vụ đã duyệt.

Đường dẫn Windows, scratch/target, log, ảnh và commit trong báo cáo là tham chiếu theo môi trường thực hiện. Chúng không phải artifact portable được commit cùng PR này; không chạy lại lệnh thay đổi database hoặc dữ liệu cũ mà chưa kiểm tra bối cảnh.

## Nguồn để đối chiếu

- [Quy chuẩn kiến trúc](../../architecture/ARCHITECTURE_GUIDE.md) và [UI Rules](../../architecture/UI/UI_RULES.md).
- [Module Offer](../../../src/main/java/com/group2/rms/offer/) và [template Offer](../../../src/main/resources/templates/offers/).
- [Schema](../../../database/schema/db.sql), [model database](../../database/model.md) và [migration](../../../database/migrations/).
- [Báo cáo PR tài liệu](pr-review/offer-workflow-review.md).

Không dùng kết quả test lịch sử để tuyên bố đã kiểm thử lại phiên bản hiện tại. PR tài liệu cuối chỉ kiểm tra manifest, nội dung/đường dẫn hiện hành, pattern thông tin nhạy cảm, Markdown và Git; không chạy lại Maven/Node hoặc sửa production.

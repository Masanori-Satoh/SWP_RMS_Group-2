# Tài liệu thay đổi — LinhDN

Các báo cáo cập nhật Candidate/Talent Pool và Application ngày 16–17/10/2026 đã được gom về thư mục này. Mã nguồn chức năng giữ nguyên vị trí trong `src/`; không commit/push.

## Trạng thái mới nhất

HM cũng dùng một nút **Ứng viên tiềm năng** ở chi tiết tin; bên trong danh sách có **Thêm ứng viên tiềm năng** mở selection cũ. HR giữ nhãn và hành động riêng. Xem phần bổ sung trong [báo cáo Flow C](2026-10-17-hr-hm-recommendations-flow-c.md).

HR dùng một lối vào **Gợi ý đã gửi cho HM** từ chi tiết tin. Trong trang có nút mở lại selection hiện có và hành động **Xóa gợi ý** trước khi HM phản hồi: [Cập nhật Flow C](2026-10-17-hr-hm-recommendations-flow-c.md#bổ-sung-trang-quản-lý-gợi-ý-và-thu-hồi-1710). Thu hồi dùng trạng thái/audit hiện có, giữ CV, Candidate và ghi chú; không sửa schema.

Bookmark trong phỏng vấn đã gộp thành **một nút mở popup ghi chú dùng chung với Application**, theo yêu cầu cuối: [Báo cáo tái sử dụng và kiểm thử](2026-10-17-interview-bookmark-toggle.md). Popup có lịch sử, thêm ghi chú và bỏ lưu; gỡ CSS/JS riêng để tránh duy trì hai bản.

Nhánh Candidate nộp CV mới cho đề xuất đã được HM duyệt chuyển thẳng `HM_Passed` để HR xếp lịch: [Quyết định nghiệp vụ, phạm vi và kiểm chứng](2026-10-17-new-cv-approved-proposal-interview.md). HR liên hệ bên ngoài; dùng CV cũ vẫn xác nhận theo luồng hiện có. Gợi ý còn chờ HM và tự ứng tuyển thông thường vẫn qua sàng lọc.

Đã tích hợp bookmark tiềm năng và ghi chú chung vào màn hình kết quả phỏng vấn HR/HM: [Báo cáo phạm vi, phân quyền và kiểm thử](2026-10-17-interview-potential-bookmark.md). Component độc lập với Pass/Fail; module Interview chỉ thêm 6 dòng template, dùng lại API Talent Pool, không sửa backend hoặc schema.

Flow C HR → HM được bổ sung tiếp theo: [Báo cáo triển khai và kiểm thử](2026-10-17-hr-hm-recommendations-flow-c.md). Gợi ý chờ HM được tách khỏi danh sách chờ HR liên hệ; HM chấp nhận chưa có nghĩa Candidate đồng ý. Kết quả cuối Flow C/hồi quy: **316 Java passed, 1 skipped, 0 failure/error; 24 JavaScript passed**, trong đó 4 integration test SQL Server thật đã qua.

Đã nối nút xác nhận với backend lưu Application mới ở trạng thái `HM_Suggested`. Candidate và Application nguồn giữ nguyên; đề xuất chờ hoặc đã kết thúc được tách khỏi hồ sơ chính thức. Sau khi HR xác nhận ứng viên đồng ý dùng CV cũ, cùng bản ghi chuyển sang `HM_Passed`, đủ điều kiện đi tiếp bước chờ phỏng vấn. Từ chối dùng `Rejected` kèm audit phân loại đề xuất.

Không bổ sung bảng/cột/enum hoặc sửa file schema. Tuy nhiên đã có sự cố Hibernate tự phát ALTER trong một lượt test do cấu hình local `ddl-auto=update`; diễn biến, phạm vi đối chiếu và giới hạn bằng chứng được ghi rõ trong báo cáo chức năng bên dưới.

## Các báo cáo đã gom

| Tài liệu | Nội dung |
| --- | --- |
| [Popup bookmark trong phỏng vấn 17/10](2026-10-17-interview-bookmark-toggle.md) | Một nút mở popup Application cũ, giữ ghi chú/trạng thái tuyển dụng; 108 Java + 39 JavaScript test qua. |
| [CV mới cho đề xuất đã duyệt 17/10](2026-10-17-new-cv-approved-proposal-interview.md) | Candidate nộp CV mới đúng tin → cùng đề xuất chuyển `HM_Passed`; HR xếp lịch, không chấm AI lại. |
| [Bookmark trong kết quả phỏng vấn 17/10](2026-10-17-interview-potential-bookmark.md) | HR/HM lưu tiềm năng, xem/thêm ghi chú chung qua API hiện có; giới hạn tác động module Interview và kiểm chứng. |
| [Flow C HR → HM 17/10](2026-10-17-hr-hm-recommendations-flow-c.md) | Tái sử dụng Application/Review, HR gửi, HM phản hồi trên list/detail, CV lịch sử và kiểm chứng. |
| [Tổng kết cập nhật 16/10](UPDATE_SUMMARY_2026-10-16.md) | Bookmark chung, ghi chú HR/HM, bỏ lưu, preview CV, trang Application và phần admin do người dùng sửa; kế hoạch chia commit ban đầu. |
| [Tinh chỉnh Candidate pipeline 16/10](2026-10-16-candidate-pipeline-ui-refinement.md) | Bố cục bảng, icon, modal ghi chú và mở rộng CV. |
| [Chọn ứng viên từ kho tiềm năng 17/10](2026-10-17-talent-pool-selection-ui.md) | Chọn Candidate/CV ở nhiều trang, giữ draft, kiểm tra nguồn và popup tổng kết. |
| [Chi tiết ứng viên tiềm năng 17/10](2026-10-17-talent-pool-candidate-detail-ui.md) | CV lịch sử, ghi chú hai cột, điều hướng quay lại và các lượt tinh chỉnh popup. |
| [Workspace Application/đề xuất HM 17/10](2026-10-17-application-detail-workspace.md) | Header, hành trình, AI gọn, CV và sidebar liên hệ/ghi chú/hoạt động. |
| [Chức năng HM đề xuất và kiểm chứng 17/10](2026-10-17-hm-suggestions-functional.md) | Quyết định nghiệp vụ cuối, audit ảnh hưởng, lưu thật, HR xác nhận/từ chối, chống trùng, phân quyền và kết quả test. |

Các báo cáo UI lưu lịch sử từng lượt: những đoạn ghi “chưa bật xác nhận” hoặc đề nghị bảng riêng đã được thay thế bởi phương án cuối trong báo cáo chức năng. Kết quả test tại từng báo cáo là kết quả của thời điểm đó, không cộng dồn thành tổng hiện tại.

## Kiểm chứng Flow A/B trước khi bổ sung Flow C

- Java: 289 test được báo cáo trong 18 suite; 288 passed, 1 skipped vì DB chưa có mẫu đơn đã rút, 0 failure/error.
- Có 2 integration test dùng MockMvc, service, repository và SQL Server thật: thêm → HR đồng ý → đủ điều kiện Interview; thêm → HR từ chối → không vào pipeline chính thức. Dữ liệu test rollback.
- JavaScript: 20 passed; cú pháp script và `git diff --check` qua.
- Chưa xác minh thao tác đầu-cuối bằng trình duyệt. Báo cáo chức năng nêu rõ sự cố DDL và các bước kiểm tra thủ công còn cần nghiệm thu.

Các tài liệu khác, thư mục `commits/` và `tests/` hiện có của LinhDN được giữ nguyên.

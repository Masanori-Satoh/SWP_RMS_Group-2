# Work log — bộ đếm ứng viên tiềm năng trên chi tiết tin

- Ngày 08/11/2026, nhánh `feature/update-ui-authen`.
- Người phụ trách LinhDN.
- [Báo cáo phạm vi và cách đối chiếu](../../members/linhdn/2026-11-08-job-posting-suggested-candidate-count.md).

## Thực hiện

1. Dùng chung điều kiện giữa truy vấn danh sách đề xuất và truy vấn đếm `CandidateId` phân biệt theo tin. Đếm bản `HM_Suggested` do HM đề xuất trực tiếp hoặc do HR gợi ý và có lượt duyệt `HiringManager / Pass`.
2. Tải số đếm trong controller chi tiết tin khi khối Talent Pool được hiển thị. Service kiểm tra phạm vi tin như khi mở danh sách đề xuất.
3. Thêm badge `badge--neutral` vào liên kết đã có. Hiển thị `0` nếu danh sách chưa có ứng viên. Badge “Gợi ý từ HR” tiếp tục dùng số gợi ý chờ xử lý.

## Kiểm chứng

- `JobPostingControllerTests` 2/2 ca liên quan qua. Kiểm tra model và HTML render, cùng trường hợp khối Talent Pool bị ẩn.
- `TalentPoolReadUnitTests` 71/71 và `PaginationFragmentTests` 5/5 qua.
- `TalentPoolFlowIntegrationTests` 2/2 ca chọn lọc qua trên SQL Server cục bộ trong transaction rollback. Đã kiểm tra gợi ý HR chờ duyệt, HM chấp nhận, HM đề xuất trực tiếp và đề xuất chuyển thành hồ sơ chính thức. Lần chạy trong sandbox bị chặn ở kết nối JDBC. Lần chạy với quyền kết nối local đã qua.
- `git diff --check` qua. Kiểm chứng HTML bằng MockMvc, chưa thao tác trên trình duyệt.

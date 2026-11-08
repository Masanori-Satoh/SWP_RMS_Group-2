# Work log — tăng giới hạn tìm kiếm trong Kho ứng viên tiềm năng

- Ngày 08/11/2026, nhánh `feature/update-ui-authen`.
- Người phụ trách LinhDN.
- [Báo cáo và cách đối chiếu](../../members/linhdn/2026-11-08-talent-pool-search-max-length.md).

## Thay đổi và kiểm chứng

- Tăng `maxlength` của ô “Họ tên hoặc email” và giới hạn từ khóa trong `TalentPoolService.search` từ 100 lên 150 ký tự.
- Cập nhật ca kiểm tra ngưỡng để xác nhận dịch vụ giữ từ khóa 149 và 150 ký tự, cắt từ khóa 151 ký tự còn 150 và vẫn chuyển đúng bộ lọc cho repository.
- `TalentPoolReadUnitTests` qua 73/73 khi chạy Maven offline với Mockito nạp qua `-javaagent`. `git diff --check` qua. Kiểm tra thuộc tính `maxlength` trong template, chưa thao tác trên trình duyệt.

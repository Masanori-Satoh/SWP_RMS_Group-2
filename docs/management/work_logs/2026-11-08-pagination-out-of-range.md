# Work log — sửa lỗi phân trang vượt giới hạn

- Ngày 08/11/2026, nhánh `feature/update-ui-authen`.
- Người phụ trách LinhDN.
- [Báo cáo hiện tượng và cách tái hiện](../../members/linhdn/2026-11-08-pagination-out-of-range.md).

## Thay đổi

1. Rà ba fragment `paged`, `pagedWithUrl` và `pager`. Giới hạn dãy trang hiển thị quanh một chỉ số hợp lệ. Nếu trang yêu cầu nằm ngoài phạm vi, hiển thị thông báo và chỉ tạo liên kết đến trang có thật.
2. Trong `TalentPoolService.search`, giới hạn offset theo miền `int` của JPA. Nếu trang truy vấn vượt trang cuối và có kết quả, truy vấn lại trang cuối với cùng bộ lọc và kích thước trang.
3. Bổ sung ca hồi quy cho `page=9999` và `Integer.MAX_VALUE` ở cả service và bước render Thymeleaf. Kiểm tra thêm trạng thái rỗng, số nút trong trang hợp lệ và các liên kết giữ bộ lọc.

## Kiểm chứng

Maven offline chạy ba bộ test với Mockito được nạp sẵn qua `-javaagent` và tắt JaCoCo trong lần chạy này. Lần chạy mặc định bị gián đoạn khi Mockito tự nạp agent qua Windows pipe.

| Bộ test | Kết quả |
| --- | --- |
| `TalentPoolReadUnitTests` | 71/71 |
| `LegacyPaginationBridgeTests` | 9/9 |
| `PaginationFragmentTests` | 5/5 |

Tổng 85/85 test qua. `git diff --check` qua. Phạm vi kiểm chứng gồm unit test và render Thymeleaf. Chưa chạy qua trình duyệt hoặc SQL Server thật.

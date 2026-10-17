# Commit 07 — Cập nhật nhật ký và tổng hợp việc chia commit

Ngày: 14/10/2026.

## File và thay đổi

`docs/members/linhdn/TALENT_POOL_UPDATE_2026-10-14.md`: đưa vào 104 dòng nhật ký đã có trong d6e91bb, mô tả resolve conflict, file sửa, lỗi startup, kết quả test và bước kiểm tra tay. Giữ nguyên phần triển khai trước đó trong 92fb06e.

## Các commit đã tách theo thứ tự

| Thứ tự | Nội dung | Tài liệu đi cùng |
|---|---|---|
| 01 | Tích hợp main và resolve contract Pipeline/Talent Pool | [01-integrate-main.md](01-integrate-main.md) |
| 02 | Stepper xử lý POSTING_CLOSED | [02-posting-closed-stepper.md](02-posting-closed-stepper.md) |
| 03 | Mock TalentPoolService cho test sidebar | [03-sidebar-web-test-dependency.md](03-sidebar-web-test-dependency.md) |
| 04 | Fixture form lập lịch và dependency email | [04-interview-form-fixtures.md](04-interview-form-fixtures.md) |
| 05 | Phân cách JPQL FORMAL_FILTER | [05-formal-application-jpql.md](05-formal-application-jpql.md) |
| 06 | InterviewId: Java Long / SQL INT | [06-interview-id-integer-mapping.md](06-interview-id-integer-mapping.md) |
| 07 | Nhật ký conflict, startup và verification | File này và nhật ký Talent Pool |

## Phạm vi lịch sử Git

- Dựng bản tách d6e91bb trên nhánh local feature/talent-pool-first-sprint-split-d6e91bb.
- Trong lúc chuẩn bị, người dùng pull thêm main 30fd33a và có conflict database/schema/db.sql. Nhánh đang dùng, HEAD, index và MERGE_HEAD được giữ nguyên để không ảnh hưởng lần pull này.
- 92fb06e chứa phần Talent Pool chính và giữ nguyên hash.
- Commit merge đầu tiên giữ hai parent 92fb06e và d1f1144.
- Các commit main, tác giả của đồng đội, ref main và origin/main giữ nguyên.
- Commit gốc được giữ bằng nhánh backup/linhdn-d6e91bb-before-split-2026-10-14.
- d6e91bb đã được xuất bản trước đợt chia commit; origin/feature/talent-pool-first-sprint còn được cập nhật thêm trong lúc làm. Không rewrite remote hoặc đưa chuỗi tách vào phần merge đang dở.

## Bằng chứng kiểm tra

1. So sánh tree cuối với d6e91bb: tất cả file đã tồn tại phải có đúng blob/mode cũ; chỉ thêm bảy file mô tả commit trong thư mục này.
2. Mỗi commit thêm đúng một file mô tả mới; các fix chỉ chạm nhóm file ghi ở tài liệu tương ứng.
3. Kiểm tra commit main là ancestor của HEAD mới và 92fb06e không bị viết lại.
4. Xác nhận HEAD, index, MERGE_HEAD và trạng thái các file tracked của nhánh đang dùng không đổi. Bảy tài liệu mới còn untracked tại working tree hiện tại; chúng đã được lưu trong từng commit trên nhánh tách.
5. Source không đổi nên dùng bằng chứng test của snapshot gốc, không tuyên bố đã chạy lại Maven trong đợt chia commit.

Kết quả snapshot gốc: clean compile PASS; Context/JPA/query trọng điểm PASS (18 test, 0 failures, 0 errors, 1 skipped); full suite FAIL (732 test, 3 failures, 7 errors, 1 skipped). Các lỗi còn lại: sáu fixture Requisition thiếu RecruitmentRound, một lần deadlock metadata SQL Server, ba assertion Career cũ.

Không yêu cầu mọi commit trung gian khởi động được: các lỗi trước đó được xử lý theo từng commit 02–06; không thay đổi source chỉ để che lỗi này.

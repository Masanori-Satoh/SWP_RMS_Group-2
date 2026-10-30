# 30/10/2026 — Giải quyết conflict tài liệu khi merge main

**Người thực hiện:** LinhDN. **Phạm vi:** merge `origin/main` (`a5bef9cd`) vào nhánh hiện tại (`ab369832`), giữ chế độ chưa commit.

## Xung đột và kết quả

Có **3 file tài liệu, 6 đoạn conflict**; chỉ xử lý các đoạn có marker, giữ nguyên phần không xung đột của bản auto-merge.

| File | Số đoạn | Cách xử lý |
| --- | ---: | --- |
| `docs/management/WORK_LOG.md` | 1 | Giữ hai mục mới về làm sạch Markdown và giao diện Application; giữ các mục công việc/tác giả khác. Thêm liên kết báo cáo lượt resolve. |
| `docs/management/work_logs/2026-10-30-hr-recommendations-ui.md` | 2 | Giữ dòng tác giả LinhDN và dấu phẩy; dùng tên tệp QA thay đường dẫn máy cá nhân theo yêu cầu làm sạch tài liệu trước đó. |
| `docs/management/work_logs/2026-10-30-linhdn-history-and-candidate-info.md` | 3 | Giữ mô tả kiểm chứng/lịch sử, tên ảnh và archive; không đưa lại đường dẫn máy cá nhân đã được làm sạch. |

- Không chọn toàn bộ một phía cho cả repository hoặc ghi đè source.
- File `ApplicationPipelineWebTests.java` đã auto-merge, không có diff cần xử lý; không sửa thêm Java, template, CSS, JavaScript hoặc database.
- Bổ sung báo cáo này và một liên kết tại README của LinhDN theo quy ước ghi nhận mỗi lượt. Tổng phạm vi bàn giao **5 file Markdown**.

## Kiểm chứng và bàn giao

- Không còn file unmerged hoặc marker conflict trong nội dung tracked sau resolve.
- Quét toàn bộ Markdown trong `docs`, gồm vùng tạm: nội dung đã làm sạch từ lượt trước vẫn được giữ.
- Đối chiếu SHA-256 với trạng thái ngay sau khi merge bị dừng: chỉ 3 file conflict và README thay đổi; các file tracked khác giữ nguyên. Báo cáo này là file mới.
- Kiểm tra `git diff --check` và các liên kết báo cáo/ảnh hiện có.
- Đánh dấu resolved đúng 3 file conflict; đưa kèm báo cáo và liên kết README vào staging để không để work log trỏ tới một tài liệu chưa được đưa vào merge.
- Không chạy lại Maven/browser vì xung đột chỉ nằm trong tài liệu; không tạo commit hoặc push. Merge vẫn chờ người dùng review và commit.

# 30/10/2026 — Rà soát nội dung tài liệu Markdown

**Người thực hiện:** LinhDN. **Phạm vi:** tất cả file có đuôi `.md` trong `docs`, gồm vùng tạm và các file bị Git ignore.

## Yêu cầu và thay đổi

- Làm sạch toàn bộ cách viết hoa/thường của tên công cụ được chỉ định trong tài liệu.
- Quét ban đầu **187 file Markdown**; tìm thấy **60 vị trí trong 21 file**. Sau khi bổ sung báo cáo này, phạm vi kiểm tra cuối là **188 file**.
- Bỏ tên khỏi dòng người thực hiện, mô tả công cụ và hướng dẫn phối hợp; chỉnh lại câu, giữ nội dung nghiệp vụ, quy tắc bảo mật và giới hạn kiểm chứng.
- Với ảnh/log/backup cũ có tên công cụ trong đường dẫn máy cá nhân: giữ tên tệp hoặc thư mục QA cục bộ; chuyển 7 liên kết cục bộ thành mô tả tệp, không tạo URL hoặc đường dẫn mới không có thật.
- Không đổi tên, di chuyển hoặc xóa artifact trên đĩa. Ảnh desktop/mobile trong báo cáo Application tiếp tục được nhúng bằng đường dẫn trong repo.
- Bổ sung liên kết báo cáo tại README của LinhDN và work log chung.

## File thay đổi trong lượt này

Thống kê so với snapshot đầu lượt, không gộp sửa giao diện đã staged từ lượt trước. Có **21 tài liệu được làm sạch**, một README bổ sung liên kết và một báo cáo mới; tổng cộng **23 file Markdown**. Hai file trong thư mục tạm đang bị Git ignore cũng được xử lý tại máy.

<!-- file-stats:start -->
| File | Thêm | Xóa |
| --- | ---: | ---: |
| `docs/architecture/UI/USER_GUIDE.md` | +1 | -1 |
| `docs/management/WORK_LOG.md` | +3 | -2 |
| `docs/management/work_logs/2026-10-04-restore-sidebar-header-from-stash.md` | +2 | -2 |
| `docs/management/work_logs/2026-10-28-potential-candidate-admin-ui-sync.md` | +2 | -2 |
| `docs/management/work_logs/2026-10-28-potential-candidate-list-polish.md` | +2 | -2 |
| `docs/management/work_logs/2026-10-30-application-detail-ui.md` | +3 | -3 |
| `docs/management/work_logs/2026-10-30-hr-recommendations-ui.md` | +2 | -2 |
| `docs/management/work_logs/2026-10-30-linhdn-history-and-candidate-info.md` | +9 | -9 |
| `docs/members/linhdn/2026-10-20-auth-reference-alignment.md` | +6 | -6 |
| `docs/members/linhdn/2026-10-21-auth-exact-mockup.md` | +4 | -4 |
| `docs/members/linhdn/2026-10-21-login-register-sync.md` | +2 | -2 |
| `docs/members/linhdn/2026-10-22-auth-reference.md` | +2 | -2 |
| `docs/members/linhdn/2026-10-28-potential-candidate-ui-change-summary.md` | +2 | -2 |
| `docs/members/linhdn/2026-10-30-application-detail-ui.md` | +1 | -1 |
| `docs/members/linhdn/2026-10-30-cleanup-markdown-docs.md` | +53 | -0 |
| `docs/members/linhdn/ADMIN_DASHBOARD_CHANGES_2026-10-14.md` | +7 | -7 |
| `docs/members/linhdn/AI MD/AGENTS.md` | +3 | -3 |
| `docs/members/linhdn/AI MD/CLAUDE.md` | +2 | -2 |
| `docs/members/linhdn/AI MD/DESIGN.md` | +3 | -3 |
| `docs/members/linhdn/README.md` | +2 | -0 |
| `docs/members/linhdn/tests/2026-10-04-sidebar-header-recovery.md` | +2 | -2 |
| `docs/members/linhdn/tmp/testing/password-reset-test-setup.md` | +1 | -1 |
| `docs/members/linhdn/tmp/tests/2026-09-30-password-reset-local-setup.md` | +1 | -1 |
| **Tổng 23 file Markdown** | **+115** | **-59** |
<!-- file-stats:end -->

## Kiểm chứng

- Quét lại toàn bộ Markdown trong `docs` không phân biệt hoa/thường: **0 kết quả còn lại**.
- Đối chiếu SHA-256 xác nhận chỉ tài liệu Markdown trong phạm vi thay đổi; mã nguồn ứng dụng, ảnh và các file cấu hình giữ nguyên so với đầu lượt.
- Nội dung staging có sẵn giữ nguyên; không stage lại, commit hoặc push.
- `git diff --check` qua. Không chạy Maven/browser vì lượt này chỉ sửa tài liệu, không đổi giao diện hoặc hành vi ứng dụng.
- Đường dẫn hai ảnh đang nhúng trong báo cáo Application vẫn tồn tại; không phát sinh screenshot mới trong lượt chỉnh tài liệu này.

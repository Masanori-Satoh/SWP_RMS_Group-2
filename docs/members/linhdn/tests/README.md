# Hồ sơ kiểm thử theo từng đợt

Mỗi đợt thay đổi có một file Markdown riêng trong thư mục này. Tên file bắt đầu
bằng ngày và chủ đề, ví dụ `2026-09-29-auth-screens.md`. File cần ghi phạm vi,
điều kiện đầu vào, các bước thực hiện, kết quả mong đợi, kết quả thực tế,
PASS/FAIL/BLOCKED và bằng chứng. Không ghi credential hoặc token thật.

`docs/TEST_PLAN.md` là hướng dẫn kiểm thử chung. `docs/WORK_LOG.md` ghi thay đổi
và điểm tiếp tục. Test tự động, Spring Context, JPA, HTTP và thao tác trình
duyệt phải được ghi riêng; một loại bằng chứng không thay cho loại khác.

## Các đợt

- [2026-10-04 — Department CRUD, Account activation, Dashboard Admin: automated evidence và manual test steps](2026-10-04-admin-departments-activation.md)

- [2026-10-04 — Dashboard Candidate tiếng Việt: scope/privacy, bảng/filter, responsive và logout](2026-10-04-candidate-dashboard-ui.md)

- [2026-10-04 — audit project trước Candidate UI: kết quả build/startup/test và các bước retest](2026-10-04-project-audit.md)

- [2026-10-02 — tách lifecycle Internal/Candidate, scoped queries/actions, Dashboard và manual steps](2026-10-02-account-separation.md)

- [2026-09-29 — sáu màn hình và use case xác thực/tài khoản](2026-09-29-auth-screens.md)
- [2026-09-30 — chuẩn bị test local Forgot/Reset Password](2026-09-30-password-reset-local-setup.md)
- [2026-09-30 — danh mục 47 case theo từng flow](2026-09-30-flow-cases.md)
- [2026-09-30 — prototype trang tuyển dụng một công ty](2026-09-30-corporate-career-homepage.md)
- [2026-10-01 — khởi tạo ngữ cảnh sản phẩm Impeccable](2026-10-01-impeccable-init.md)
- [2026-10-01 — polish homepage tuyển dụng một công ty](2026-10-01-homepage-polish.md)
- [2026-10-01 — audit Gemini, cải thiện homepage và test từng bước](2026-10-01-homepage-gemini-audit.md)
- [2026-10-01 — tích hợp career site, English UI, saved login URL và Candidate prefill](2026-10-01-career-integration.md)
- [2026-10-01 — đồng bộ Auth/Admin UI, kiểm tra từng bước và giới hạn E2E](2026-10-01-auth-admin-ui.md)

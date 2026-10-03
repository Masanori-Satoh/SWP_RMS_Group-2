# 2026-09-29 — Tài liệu bàn giao và kịch bản test

**Yêu cầu:** Nói rõ file DB nào đã sửa; từ nay ghi tiến trình vào Markdown và
có tài liệu từng bước test.

**Thay đổi:** Tạo `docs/management/WORK_LOG.md` (file tổng) và `docs/management/TEST_PLAN.md`; cập nhật
README để người đọc đi tới schema/test/log thay vì tạo nhầm `RMS_DB` trống.
Không sửa schema, seed, Entity hoặc logic ứng dụng trong lượt này.

**Kiểm tra:** Rà các route/form/test class trước khi viết kịch bản. Bước test
tự động 28/28 và package PASS được **ghi lại từ lượt đồng bộ schema**, không
được diễn đạt như một lần chạy mới của lượt viết tài liệu. Test trình duyệt
trong `docs/management/TEST_PLAN.md` là **chưa chạy**.

**Cách nối tiếp:** Sau mỗi thay đổi tiếp theo, thêm mục mới ở cuối `WORK_LOG.md` và
cập nhật `docs/management/TEST_PLAN.md` nếu phạm vi test thay đổi. Ghi bằng chứng PASS,
FAIL hoặc BLOCKED đúng với cách đã chạy; không coi compile hay MockMvc là bằng
chứng đã test trình duyệt thật.

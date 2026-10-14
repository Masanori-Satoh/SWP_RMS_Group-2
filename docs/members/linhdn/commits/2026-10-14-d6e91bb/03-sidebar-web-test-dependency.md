# Commit 03 — Bổ sung dependency Talent Pool trong test sidebar

Ngày: 14/10/2026. Tách từ `d6e91bb`.

## File và thay đổi

`src/test/java/com/group2/rms/core/web/SidebarRoleWebTests.java`: thêm MockitoBean TalentPoolService.

## Lý do

Web slice từ main dựng ApplicationPipelineController. Controller của nhánh có thêm TalentPoolService nên test cần mock dependency này để Context khởi tạo được. Không đổi DI runtime hoặc nội dung assertion.

## Kiểm tra

1. Chạy SidebarRoleWebTests.
2. Candidate vẫn có menu /portal/offers và không có menu nội bộ /applications, /offers.
3. HR giữ sidebar recruiter.

Bằng chứng trên snapshot gốc: 3 test PASS. Không comment hoặc bỏ test.

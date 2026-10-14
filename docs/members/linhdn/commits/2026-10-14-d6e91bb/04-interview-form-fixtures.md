# Commit 04 — Đồng bộ fixture form lập lịch phỏng vấn

Ngày: 14/10/2026. Tách từ `d6e91bb`.

## File và thay đổi

`src/test/java/com/group2/rms/interview/InterviewScheduleFormWebTests.java`:

- Thêm MockitoBean RecruitmentEmailService cho dependency gửi mail sau lưu.
- Chuyển stub danh sách đơn từ findAllWithCandidateAndJobPosting sang findEligibleForInterview.

## Lý do

Test phải khớp constructor Controller và query mà form thật đang dùng. Danh sách đơn đủ điều kiện khác danh sách toàn bộ đơn; giữ nguyên assertion UI và phân quyền.

## Kiểm tra

1. Chạy InterviewScheduleFormWebTests.
2. HR mở /interviews/new và form có danh sách đơn đủ điều kiện.
3. Kiểm tra layout, nút lập lịch và dialog hủy vẫn đúng.

Bằng chứng trên snapshot gốc: 5 test PASS. Không thêm file source test mới.

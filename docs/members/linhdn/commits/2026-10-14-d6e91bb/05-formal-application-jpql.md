# Commit 05 — Sửa phân cách JPQL khi ghép FORMAL_FILTER

Ngày: 14/10/2026. Tách từ `d6e91bb`.

## File và thay đổi

`src/main/java/com/group2/rms/candidate/repository/ApplicationRepository.java`: dùng chuỗi phân cách rõ ràng `" AND "` tại:

- findOwnedByUserId.
- findOwnedById.
- existsActiveByCandidateAndJobPosting.
- findPreviousCvByCandidateId.

## Lý do

Text block Java bỏ khoảng trắng ở cuối dòng. Ghép trực tiếp and với a.applicationStatus tạo token anda, làm Spring Data không khởi tạo ApplicationRepository và kéo theo lỗi Service/Controller.

Các điều kiện ẩn HM_Suggested và TalentPoolCancelled vẫn giữ nguyên.

## Kiểm tra

1. Chạy RmsApplicationTests và các repository query test với ddl-auto=validate, sau khi commit 06 đã có mapping đúng.
2. Candidate mở My Applications; đề xuất chưa được xác nhận vẫn ẩn.
3. Kiểm tra đã nộp ở Job Posting và lấy CV cũ trong Talent Pool.
4. Log không còn BadJpqlGrammarException hoặc token anda.

Bộ Context/query trọng điểm trên snapshot hoàn chỉnh: 18 test, 0 failures, 0 errors, 1 skipped. Không dùng compile để thay bằng chứng startup.

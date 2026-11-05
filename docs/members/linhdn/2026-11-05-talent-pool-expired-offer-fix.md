# Báo cáo sửa điều kiện Offer chặn đóng tin tuyển dụng

- Ngày: 05/11/2026.
- Người thực hiện: LinhDN (hỗ trợ triển khai bằng Codex).
- Yêu cầu: Offer đã quá hạn phản hồi không tiếp tục chặn HR đóng tin tuyển dụng.

## Nguyên nhân

`TalentPoolAuditRepository.hasOpenOffers()` chỉ loại ba trạng thái `Accepted`, `Declined`, `Director_Rejected`. Trong khi đó, Offer quá hạn vẫn lưu `Sent_Candidate`; luồng Offer xác định quá hạn khi đọc và khi kiểm tra quyền phản hồi. Vì vậy `TalentPoolService.closeJob()` vẫn chặn các thư không còn được phản hồi.

## Thay đổi

- Tiêm `Clock` hiện có để tính thời điểm và ngày hiện tại từ cùng một mốc, cùng nguồn thời gian với `CandidateOfferServiceImpl`.
- Với `Sent_Candidate`, chỉ chặn khi cả hai điều kiện đều thỏa: `ResponseDeadline` trống hoặc chưa qua; `ExpectedStartDate` trống hoặc chưa trước ngày hiện tại.
- Hạn chót bằng thời điểm hiện tại vẫn còn hạn; ngày nhận việc là hôm nay vẫn chưa quá hạn.
- Thiếu hạn chót: xét ngày nhận việc. Thiếu cả hai mốc: tiếp tục chặn vì không có căn cứ xác định quá hạn.
- Ngày nhận việc đã qua vẫn làm thư hết hạn ngay cả khi hạn phản hồi còn trong tương lai.
- Bỏ qua các trạng thái kết thúc mà luồng Offer liệt kê: `Voided`, `Canceled`, `Rejected`, `Expired`, ngoài ba trạng thái đã loại trước đây. Các trạng thái nội bộ như `Draft`, `Pending_Director`, `Director_Approved` vẫn chặn.
- Không thay đổi trạng thái Offer khi truy vấn; không sửa quy trình đóng tin, xác nhận Offer hay gửi email.

## File liên quan

- `src/main/java/com/group2/rms/candidate/talentpool/repository/TalentPoolAuditRepository.java`.
- `src/test/java/com/group2/rms/candidate/talentpool/repository/TalentPoolOpenOffersQueryTests.java`.
- [Work log chi tiết](../../management/work_logs/2026-11-05-talent-pool-expired-offer-fix.md).

## Kiểm chứng và giới hạn

- `TalentPoolOpenOffersQueryTests`: **17/17 qua**, chạy truy vấn trên SQL Server local với thời gian cố định và rollback sau từng ca. Bao gồm dữ liệu cũ, mốc biên, phạm vi tin tuyển dụng, kiểm tra không đổi trạng thái và trường hợp còn Offer khác chưa hết hạn.
- `CandidateOfferServiceTests`: **35/35 qua**.
- Chạy riêng `TalentPoolDecisionUnitTests#closeJob*`: **10/10 qua**.
- `TalentPoolDecisionUnitTests`: **48/50 qua**. Hai ca lỗi do nhãn hiển thị cũ: test mong `HM đã chấp nhận` / `HM đã từ chối`, service trả `Trưởng bộ phận đã chấp nhận` / `Trưởng bộ phận đã từ chối`. Đối chiếu `git show HEAD` xác nhận sự lệch này đã có trước bản sửa; không chỉnh sửa hai file đó.
- Tổng lượt chạy ba lớp: **102 test, 100 qua, 2 lỗi assertion nhãn có sẵn, không có error hoặc skip**.
- `git diff --check`: qua.
- Lần chạy sandbox đầu tiên không kết nối được SQL Server (`Permission denied: getsockopt`); các kết quả trên là lần chạy lại có quyền truy cập local.
- Chưa kiểm thử thao tác đóng tin qua trình duyệt; truy vấn chặn được kiểm tra thực tế trên database, các nhánh xử lý đóng tin được kiểm tra bằng unit test.

Schema hiện tại chỉ cho phép bảy trạng thái Offer chính thức. Vì vậy các trạng thái phụ `Voided`, `Canceled`, `Rejected`, `Expired` được loại phòng trường hợp luồng Offer/schema mở rộng sử dụng, nhưng không ghi các trạng thái này vào database trong test. Không thay đổi schema hoặc seed.

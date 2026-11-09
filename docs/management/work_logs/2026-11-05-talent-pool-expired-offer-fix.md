# Work log — sửa Offer quá hạn chặn đóng tin

- Ngày: 05/11/2026.
- Người thực hiện: LinhDN.
- Yêu cầu: sửa điều kiện chặn đóng tin trong Talent Pool và test liên quan; tạo báo cáo và work log riêng.
- [Báo cáo](../../members/linhdn/2026-11-05-talent-pool-expired-offer-fix.md).

## Phạm vi thực hiện

1. Đối chiếu `hasOpenOffers()` với `CandidateOfferServiceImpl.requireAnswerable()` và `isExpired()`.
2. Sửa truy vấn để loại Offer `Sent_Candidate` quá hạn phản hồi hoặc quá ngày nhận việc; giữ đúng điều kiện khi thiếu ngày và các mốc bằng nhau.
3. Bỏ qua các trạng thái kết thúc được luồng Offer liệt kê; giữ chặn Offer nội bộ chưa kết thúc.
4. Thêm `TalentPoolOpenOffersQueryTests` kiểm tra truy vấn thực tế bằng thời gian cố định; fixture nằm trong transaction rollback.
5. Cập nhật báo cáo, work log này và chỉ mục `WORK_LOG.md`.

## Database

- Không migration, không sửa schema, không sửa seed.
- Test chọn Offer hiện có, thay đổi trạng thái/ngày trong transaction và rollback sau mỗi ca.
- Ca phối hợp hai Offer tạm đặt application của Offer thứ hai vào cùng tin trong transaction rollback, để kiểm tra còn một Offer mở thì vẫn chặn.
- Không gửi email hoặc gọi endpoint đóng tin trong test truy vấn.

## Kiểm thử

Lệnh: `mvn -o -Dtest=TalentPoolOpenOffersQueryTests,TalentPoolDecisionUnitTests,CandidateOfferServiceTests test`.

- Lần đầu: sandbox chặn kết nối SQL Server local với `Permission denied: getsockopt`; không phải kết quả xác nhận logic truy vấn.
- Chạy lại với quyền truy cập SQL Server local: **102 test, 100 qua, 2 assertion fail có sẵn, 0 error, 0 skip**.
- `TalentPoolOpenOffersQueryTests`: **17/17 qua** trên SQL Server local.
- `CandidateOfferServiceTests`: **35/35 qua**.
- Chạy riêng `mvn -o -Dtest=TalentPoolDecisionUnitTests#closeJob* test`: **10/10 qua**. Lần thử trong sandbox bị kẹt khi Mockito gắn agent; đã dừng và chạy lại ngoài sandbox.
- `TalentPoolDecisionUnitTests`: **48/50 qua**. Hai lỗi `recommendation_hmAccepted_mapsStateAndOrigin`, `recommendation_hmRejected_mapsStateAndOrigin` mong nhãn `HM đã ...`, trong khi service trả `Trưởng bộ phận đã ...`. Đã đối chiếu cả service và test qua `git show HEAD` để xác nhận lệch nhãn tồn tại trước bản sửa. Không sửa ngoài phạm vi.
- `git diff --check`: qua.

## Giới hạn

- Test SQL chỉ ghi các trạng thái hợp lệ theo constraint hiện tại; không mở rộng constraint để kiểm tra trạng thái phụ của Offer.
- Chưa kiểm thử thao tác đóng tin qua trình duyệt.
- Chưa commit/push.

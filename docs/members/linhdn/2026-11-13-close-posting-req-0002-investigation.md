# Kiểm tra thông báo chặn đóng tin REQ-0002

Ảnh cho thấy tin Frontend ReactJS Developer, lần đăng 1, đã tuyển 2/2 và vẫn báo còn thư mời chưa kết thúc khi đóng tin. `TalentPoolService.closeJob` kiểm tra `TalentPoolAuditRepository.hasOpenOffers` trước khi chuyển tin sang `Closed`. Số lượng đã tuyển đủ không thay thế điều kiện này.

Trong `database/seeds/seed_data.sql`, tin ID 2 có Offer #16 (hồ sơ #70) ở trạng thái `Pending_Director` và hai Offer #17, #18 ở trạng thái `Accepted`. Theo điều kiện hiện tại, #16 chặn đóng tin; #17 và #18 không chặn. Nếu dữ liệu đang chạy khớp seed, cần xử lý #16 theo quy trình duyệt. Với thư mời không tiếp tục, Giám đốc có thể từ chối đề xuất; nếu được duyệt và tiếp tục phát hành, cần đợi thư mời kết thúc trước khi đóng tin.

Truy vấn chưa lọc `IsDeleted`, nên bản nháp đã xóa mềm cũng có thể chặn đóng tin dù không còn hiện trong danh sách thư mời. Seed của REQ-0002 không cho thấy trường hợp này. Không thay đổi logic nghiệp vụ trong lần kiểm tra này.

Truy vấn chỉ đọc để đối chiếu trên DB của môi trường hiện thông báo:

```sql
SELECT p.JobPostingId, o.OfferId, o.ApplicationId, o.OfferStatus,
       o.IsDeleted, o.ResponseDeadline, o.ExpectedStartDate
FROM JobRequisition r
JOIN JobPosting p ON p.RequisitionId = r.RequisitionId
JOIN Application a ON a.JobPostingId = p.JobPostingId
JOIN OfferProposal o ON o.ApplicationId = a.ApplicationId
WHERE r.RequisitionCode = N'REQ-0002' AND p.PostingNo = 1
ORDER BY p.JobPostingId, o.OfferId;
```

Đã đối chiếu mã nguồn, schema và seed. Kết nối SQL Server local từ phiên này không thành công, nên chưa xác nhận bản ghi trên DB của ảnh.

# Bàn giao hỗ trợ Offer Expired cho Application

## Phạm vi và người phụ trách

Người dùng yêu cầu chỉ sửa Offer và gửi đề xuất Application, chưa phê duyệt sửa mã Application.

Theo `candidate/domain/package-info.java` và `candidate/application/package-info.java`, cả hai khu vực đều ghi **phụ trách: dunglt**. Git của các file resolver/journey cũng ghi tác giả gần nhất là **Dung Luu Tien**. Vì vậy đầu mối cần gửi là **Dũng LT (DungLT)**. TalentPool thuộc Linh theo package-info riêng; không nhầm người phụ trách hai phần này.

## Hợp đồng đã chuẩn bị ở Offer

- `Expired` là trạng thái mới của OfferProposal; migration 021 chỉ mở rộng CHECK hiện hữu, không thêm cột, không cập nhật trạng thái hồ sơ.
- Job chỉ chuyển Sent_Candidate chưa xóa và đã qua ResponseDeadline hoặc qua ngày ExpectedStartDate sang Expired. Bằng đúng hạn vẫn hợp lệ.
- Candidate vẫn xem được Expired tại `/portal/offers`, nhưng không được nhận/từ chối. API kiểm tra điều kiện trong UPDATE nên vẫn an toàn khi job chưa chạy.
- Offer được Accepted/Declined không bị job ghi đè. Phản hồi và cập nhật hồ sơ cùng giao dịch; hồ sơ không còn Offered thì rollback phản hồi.
- Không tự chuyển Application sang Rejected/Withdrawn/Cancelled, không tự đóng tin hoặc tự gia hạn thư mời.
- Job **tắt mặc định**. Chỉ bật `app.offer.expiration.enabled=true` sau khi phần Application tương thích được kiểm thử và duyệt.

## Bốn điểm cần Application xử lý

| File trong module candidate | Hiện tại | Thay đổi đề nghị |
|---|---|---|
| `domain/rule/ApplicationProgressStep.java` | Chưa có bước Offer hết hạn | Thêm OFFER_EXPIRED: nhãn “Thư mời đã quá hạn phản hồi”, tone neutral; vẫn ở giai đoạn OFFER, HR xử lý tiếp. Không tự coi hồ sơ đã kết thúc |
| `domain/rule/ApplicationProgressResolver.java` | Expired rơi vào default; Application Offered lại suy ra OFFER_SENT/chờ ứng viên | Ánh xạ Expired vào bước mới trước fallback Offered, không tiếp tục gán trách nhiệm phản hồi cho candidate |
| `application/portal/repository/MyApplicationRepository.java` | findMyVisibleOffers chỉ lấy Sent_Candidate/Accepted/Declined | Thêm Expired, vẫn giữ điều kiện đúng UserId và chưa xóa mềm để không làm mất thư mời trong hồ sơ |
| `application/portal/service/MyApplicationJourney.java` | offered() và timeline chưa xử lý Expired | Thêm nhãn/ghi chú quá hạn, cho xem thư mời nhưng không hiện CTA phản hồi; thêm mốc quá hạn trong timeline |

Các điều kiện “việc cần làm ngay”/“lịch sắp tới” chỉ nhận Sent_Candidate hợp lệ phải tiếp tục không nhận Expired. Không mở rộng quyền xem Draft/Pending_Director/Director_* cho ứng viên. Giữ nguyên tên model, route và binding UI.

Timeline cần quy ước thời gian: UpdatedAt của Expired là lúc job ghi, có thể muộn hơn ResponseDeadline. Không mô tả nó là thời điểm hết hạn chính xác nếu chưa đối chiếu mốc hạn/ngày bắt đầu.

## Kiểm thử đề nghị

1. Hồ sơ Offered + Offer Expired hiển thị quá hạn, không phải “chờ ứng viên trả lời”.
2. Expired vẫn có trong chi tiết/timeline của đúng chủ; người khác không xem được.
3. Không có CTA accept/decline hoặc việc cần phản hồi cho Expired; đường xem thư mời vẫn hoạt động.
4. Sent_Candidate còn hạn, Accepted, Declined, hồ sơ Hired/Withdrawn/Cancelled giữ đúng hành vi cũ.
5. Job chốt muộn, ResponseDeadline null nhưng ExpectedStartDate đã qua, và ngày giờ ở đúng ranh giới được mô tả nhất quán.
6. Sau kiểm thử mới bật job theo đợt, kiểm tra nhãn của cả nội bộ và cổng ứng viên. Không cần tạo trạng thái mới cho Application trong thay đổi này.

## Nội dung ngắn có thể gửi Dũng

> Dũng ơi, phần Offer đã chuẩn bị trạng thái Expired cho thư mời quá hạn. Candidate vẫn được xem nhưng không được phản hồi; Application giữ nguyên trạng thái. Mình đang để job tắt vì Application chưa ánh xạ Expired và query hồ sơ đang lọc mất trạng thái này. Nhờ Dũng bổ sung 4 điểm: ApplicationProgressStep, ApplicationProgressResolver, MyApplicationRepository và MyApplicationJourney theo bảng trên; giữ timeline/link xem và bỏ CTA phản hồi. Sau khi phần Application kiểm thử xong, hai bên phối hợp bật job. Mình chưa sửa file Application của bạn.

Không có tin nhắn nào được gửi tự động từ tác vụ này.

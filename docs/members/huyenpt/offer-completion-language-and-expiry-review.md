# Hoàn thiện ngôn ngữ và kiểm tra hết hạn thư mời

> **Lưu ý khi đọc:** Báo cáo theo thời điểm: số liệu môi trường và kết quả test thuộc từng phase, không khẳng định trạng thái dữ liệu local hiện tại. Xem [mục lục và hướng dẫn hiện hành](README.md).

Ngày: 07/11/2026. Nền trước thay đổi: `d6723566`, đã tích hợp main `1e3d7134`.

## Phạm vi đã chốt

- Chỉ hoàn thiện hết hạn; chưa thêm phát hành lại hoặc nhiều phiên bản thư mời.
- Không sửa Application, layout, sidebar, CSS chung hoặc database ứng dụng.
- Director chỉ duyệt/từ chối; HR giữ quyền tạo, sửa/xóa bản nháp và phát hành.
- Giữ job hết hạn tắt mặc định; chưa bật trên môi trường ứng dụng.

## Phase 1: ngôn ngữ hiển thị

- Dùng “Thư mời nhận việc” trong tiêu đề tạo/sửa/phê duyệt; dùng “thư mời” khi đã rõ ngữ cảnh.
- Đồng bộ nút, popup, tooltip, nhãn chọn hàng, bộ lọc phạm vi xuất, toast, lỗi nghiệp vụ và tiêu đề cột Excel.
- Giữ tên route, biến, trạng thái, id/name, field binding và khóa cột export. Tên tab kỹ thuật Excel `Offers`/`Summary` giữ tương thích.
- Giữ “vị trí đề xuất” và câu đánh giá “đề xuất gửi thư mời” khi mô tả đúng nội dung trường/hành động.
- Không đổi CSS, bố cục, điều kiện quyền hoặc quy tắc chuyển trạng thái.
- Ba kỳ vọng chuỗi trong test cũ được cập nhật theo câu chữ mới; kỳ vọng tiêu đề cột Excel cũng được đồng bộ.

Kiểm tra: Maven service/MockMvc Offer và Application 388/388; Node 20/20; `git diff --check` thành công. Không chạy test SpringBootTest dùng database ứng dụng trong phase này. Chưa kiểm tra trực quan bằng trình duyệt.

Log cục bộ (không đưa vào commit): `scratch/offer-completion-ui-20261107-v2.log`, `scratch/offer-completion-js-20261107.log`.

## Hợp đồng hết hạn và phần phối hợp

- Candidate nhìn thấy quá hạn theo thời gian kể cả khi trạng thái còn `Sent_Candidate`; chỉ xem, không phản hồi. Backend tiếp tục kiểm tra nguyên tử.
- Job chỉ ghi `Expired`; không đổi Application, không đóng tin. Hồ sơ giữ `Offered` khi chưa có hành động nghiệp vụ khác.
- HR không được tạo lại trên cùng hồ sơ trong phạm vi này. Không hướng dẫn xóa/sửa đè bản hết hạn hoặc đóng toàn bộ tin để lách quy tắc.
- HR theo dõi và phối hợp Dũng LT xử lý riêng hồ sơ qua nghiệp vụ Application được nhóm duyệt. Chưa triển khai thêm thao tác kết thúc hồ sơ tại Offer.
- Nhãn danh sách/chi tiết Offer đã nhận biết quá hạn theo thời gian; bước chi tiết hồ sơ HR vẫn có thể chờ ứng viên đến khi job ghi `Expired`.
- Migration 021 chỉ mở rộng CHECK trạng thái; môi trường đã áp dụng phải kiểm tra trước, không giả định môi trường khác cũng đã áp dụng.
- Chỉ bật `app.offer.expiration.enabled=true` sau khi hai bên kiểm thử tích hợp và xác nhận. Không có tin nhắn được gửi tự động.

## Phase 2: kiểm chứng tích hợp trên SQL Server riêng

- Commit phase ngôn ngữ: `27daaf4053352e3059b50b0d51245fbb06673bee`, 14 file, +122/-89 dòng.
- Chỉ bổ sung test trong `offer/repository/OfferPersistenceRegressionTests.java`; không sửa code Application hoặc production trong phase 2.
- Sáu ca tích hợp mới: trước hạn một giây, đúng hạn, sau hạn một giây, ngày bắt đầu đã qua nhưng deadline tương lai, không deadline còn ngày bắt đầu, không deadline và đã qua ngày bắt đầu.
- Mỗi ca dùng query thật `MyApplicationRepository.findMyVisibleOffers`, mapping sang `MyApplicationJourney`, kiểm tra quyền chủ hồ sơ và CTA trước/sau cập nhật trạng thái.
- Ca quá hạn kiểm tra UPDATE phản hồi bị chặn, timeline không tương lai, không gấp/không lịch phản hồi, chỉ có link xem. Ca còn hạn và đúng hạn vẫn có CTA phản hồi.
- Sau cập nhật: resolver ánh xạ `Expired` sang `OFFER_EXPIRED`, Application vẫn `Offered`; cập nhật lần hai không ảnh hưởng thêm hàng nào.
- Chạy cả bộ regression SQL hiện hữu, gồm quyền xem, bản ghi đã xóa, trạng thái hồ sơ kết thúc, kiểm tra hạn nguyên tử, rollback và các cuộc đua phản hồi/hết hạn/phát hành: **39/39 đạt**, không lỗi hoặc bỏ qua.
- Tổng xác minh lần này: **427 Maven test và 20 Node test đạt** (388 service/MockMvc + 39 SQL). Chưa chạy trình duyệt hoặc toàn bộ test các module khác.

Migration 021 được chạy hai lần trên bảng fixture có CHECK bảy trạng thái cũ trong database tạm riêng. Kết quả: giữ bảy hàng và bảy trạng thái cũ, ghi được `Expired`, CHECK vẫn enabled/trusted. Đây là kiểm tra migration riêng; schema JPA của regression được tạo lại trong cùng database tạm sau đó.

Database tạm `OfferRegression_71cbd9ba10f9478abd78ecc743ff8626` đã xóa sau test và xác minh không còn trong `sys.databases`. Giữ nguyên mọi file `scratch/` và `target/`.

Kiểm tra chỉ đọc database ứng dụng local: CHECK `CK_OfferProposal_Status` đã cho phép `Expired`, enabled/trusted. Có bảy thư mời: Accepted 3, Declined 2, Sent_Candidate 2. Không chạy lại migration, không backfill, không thay đổi thông tin đăng nhập hoặc bật job. Kết quả này chỉ áp dụng máy local, không suy ra tình trạng database của Dũng hoặc môi trường khác.

Log cục bộ: `scratch/offer-completion-expiry-sql-20261107.log`, `scratch/offer-completion-migration-proof-20261107.log`. Không stage các log, script fixture hoặc tên database tạm trong `scratch/`.

## Tin nhắn bàn giao để người dùng gửi Dũng

> Dũng ơi, mình đã tích hợp main 1e3d7134 và thống nhất tên hiển thị phía Offer thành “Thư mời nhận việc”. Mình chưa làm phát hành lại, hồ sơ hết hạn vẫn Offered. Kiểm thử 39 ca SQL trên database riêng đã đạt, gồm query Application thật, hiển thị trước/sau job cập nhật, đúng hạn, quá ngày bắt đầu và phản hồi đồng thời. Migration 021 chạy hai lần trên fixture riêng đạt, không sửa dữ liệu cũ. Database local của mình đã có CHECK cho phép Expired; mình không chạy lại migration hoặc bật job. Nhờ bạn phối hợp kiểm trên môi trường tích hợp của nhóm trước khi hai bên xác nhận bật job. Phần kết thúc riêng hồ sơ vẫn do Application phụ trách, không tự đóng tin.

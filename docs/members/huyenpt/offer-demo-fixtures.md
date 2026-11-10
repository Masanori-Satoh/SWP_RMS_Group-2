# Dữ liệu demo Offer — 11/11/2026

Nhánh `fix/huyenpt/project-test-errors`. Bổ sung 2 ứng viên mới cho mỗi trạng thái, giữ nguyên dữ liệu mẫu cũ. Nguồn: `database/seeds/build_seed.py`; SQL sinh ra: `database/seeds/seed_data.sql`. Không sửa schema hoặc source Java/HTML/CSS/JavaScript.

## Tra cứu nhanh

Mỗi tài khoản Active có một hồ sơ và một thư mời riêng. Dùng mật khẩu demo đã ghi ở đầu seed; không dùng tài khoản/mật khẩu production. Email `example.test` không phải hộp thư nhận email thực.

| Trạng thái | Ứng viên | Username | CandidateId | ApplicationId | OfferId |
| --- | --- | --- | ---: | ---: | ---: |
| Draft | Nguyễn Minh Khang | offer_draft_01 | 123 | 135 | 33 |
| Draft | Trần Ngọc Mai | offer_draft_02 | 124 | 136 | 34 |
| Pending_Director | Lê Quốc Bảo | offer_pending_01 | 125 | 137 | 35 |
| Pending_Director | Phạm Thanh Hà | offer_pending_02 | 126 | 138 | 36 |
| Director_Approved | Hoàng Đức Anh | offer_approved_01 | 127 | 139 | 37 |
| Director_Approved | Vũ Thu Trang | offer_approved_02 | 128 | 140 | 38 |
| Expired | Đặng Hải Nam | offer_expired_01 | 129 | 141 | 39 |
| Expired | Bùi Khánh Linh | offer_expired_02 | 130 | 142 | 40 |
| Director_Rejected | Đỗ Tuấn Kiệt | offer_rejected_01 | 131 | 143 | 41 |
| Director_Rejected | Hồ Minh Thư | offer_rejected_02 | 132 | 144 | 42 |
| Sent_Candidate | Ngô Gia Huy | offer_sent_01 | 133 | 145 | 43 |
| Sent_Candidate | Dương Bảo Ngọc | offer_sent_02 | 134 | 146 | 44 |
| Accepted | Đinh Quang Minh | offer_accepted_01 | 135 | 147 | 45 |
| Accepted | Mai Phương Anh | offer_accepted_02 | 136 | 148 | 46 |
| Declined | Tạ Thành Đạt | offer_declined_01 | 137 | 149 | 47 |
| Declined | Cao Thảo Vy | offer_declined_02 | 138 | 150 | 48 |

Nhóm `_01`: tin #1 Senior Java Backend Engineer, lương chính thức 36.000.000 VNĐ, thử việc 32.000.000 VNĐ. Nhóm `_02`: tin #4 Business Development Executive (B2B), lương chính thức 20.000.000 VNĐ, thử việc 17.500.000 VNĐ. Cả hai nhóm thử việc 60 ngày, phản hồi mặc định 5 ngày; riêng fixture Expired dùng 3 ngày.

## Thao tác theo trạng thái

| Trạng thái | Có thể kiểm tra |
| --- | --- |
| Draft | HR xem/sửa/xóa nháp hoặc trình duyệt; chưa có deadline hay quyết định Director. |
| Pending_Director | Director duyệt hoặc từ chối; HR không sửa/xóa/gửi; chưa có deadline. |
| Director_Approved | HR gửi thư mời; có quyết định Approved, chưa có deadline trước khi gửi. |
| Expired | HR/ứng viên xem thông tin hết hạn; ứng viên không phản hồi; Application vẫn Offered. Không có phát hành lại. |
| Director_Rejected | HR sửa và trình lại theo nhận xét; có lịch sử Rejected, chưa có deadline. |
| Sent_Candidate | Ứng viên nhận hoặc từ chối khi còn hạn; Application Offered. |
| Accepted | Xem kết quả nhận việc, Application Hired; không phản hồi lần nữa. |
| Declined | Xem lý do từ chối, Application Withdrawn; không phản hồi lần nữa. |

HR mở `/offers/{OfferId}`; ứng viên mở `/portal/offers/{OfferId}` sau khi thư mời đã gửi. Candidate không được xem bản Draft hoặc các bước duyệt nội bộ. Director giữ phạm vi chờ duyệt/lịch sử quyết định, không có quyền tạo/sửa/xóa/gửi/export.

## Tạo và sử dụng dữ liệu

Chạy `python database/seeds/build_seed.py` ở thư mục project chỉ sinh lại file SQL, không kết nối database. Generator dùng mốc `@D0` theo ngày thực thi SQL, không phải ngày sinh file. Tài khoản, ID và dữ liệu cũ giữ nguyên; thêm tổng cộng 16 User/Candidate/Application/OfferProposal cùng dữ liệu review, phỏng vấn, duyệt, notification và audit cần thiết.

**Đợt này chưa chạy SQL vào database.** Seed chung chỉ dành cho database rỗng; không chạy lại `db.sql` trên dữ liệu đang dùng vì script xóa và tạo lại database. Việc nạp dữ liệu cần duyệt riêng. Sau khi được nạp, trạng thái sẽ thay đổi khi thao tác; hạn thư mời cũng trôi theo thời gian. Đây là dữ liệu để thực hành, không tự đặt lại sau mỗi lần mở website.

## Kết quả kiểm tra và phạm vi branch

- 281 kiểm tra dữ liệu bằng Python đạt: toàn bộ bản ghi cũ ở các bảng không đổi; 16 tài khoản độc lập, 2 ứng viên/trạng thái, unique, liên kết hồ sơ, lương, thời gian, trạng thái Application, quyết định Director và recipient/event notification.
- Sinh SQL lặp lại cho cùng kết quả byte-for-byte. SQL có thêm 16 kiểm tra fixture để thực thi khi nạp sau này; chưa chạy các kiểm tra SQL trên SQL Server trong đợt này.
- Tổng 48 thư mời, 47 hiển thị; giữ nguyên một Draft xóa mềm. Không đổi Role, Department, JobRequisition, JobPosting, RequisitionApproval, SystemConfig hoặc TalentPoolNote.
- Bằng chứng local: `scratch/offer-search-upgrade-20261110/verify_offer_seed.py` và `offer-seed-verification.json` (không commit scratch). Không chạy lại Maven vì chỉ sửa generator/SQL mẫu/tài liệu; lần sửa Java trước đã đạt 307 test Offer và 23 test JavaScript.
- Branch đã có các commit tìm kiếm chung `444979a3`, mở rộng keyword `3fffedad`, kiểm chứng `35c16a87` và tìm lương `44fcc153`. Commit chứa tài liệu này bổ sung dữ liệu demo; chưa push hoặc thực thi database.

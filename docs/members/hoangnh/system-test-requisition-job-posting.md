# System test: Requisition → Job Posting → Candidate

**Phạm vi:** ứng dụng đang chạy, đăng nhập và thao tác qua giao diện/HTTP thật với SQL Server test. Các ca dưới đây là kịch bản cần thực thi, **chưa phải kết quả PASS**. Mỗi ca dùng dữ liệu riêng để có thể chạy độc lập.

## Chuẩn bị chung

- Môi trường test có dữ liệu theo `database/schema/db.sql`; không chạy trên database dùng chung nếu chưa thống nhất cách dọn dữ liệu.
- Tài khoản đang `Active`: `HM-A` thuộc phòng A; `HM-B` thuộc phòng B; `D` là Director; `H` là HR; `C` là Candidate có hồ sơ và CV hợp lệ; `SA` là System Admin. Dùng hai phòng ban đang hoạt động. Ghi username thực tế vào biên bản chạy test.
- Mỗi lần chạy đặt tên vị trí duy nhất, ví dụ `ST-REQ-<runId>-01`, để tránh quy tắc chặn hai bản nháp cùng vị trí/phòng ban. Ghi `RequisitionId`, `JobPostingId`, `PostingNo` và URL chi tiết ngay khi tạo.
- Hạn nộp của ca đăng tin thành công phải sau thời điểm test. Khi đổi tài khoản, đăng xuất hoặc dùng phiên trình duyệt riêng; lưu ảnh trang và URL trước/sau thao tác. Không sửa trạng thái trực tiếp trong DB trừ ca dữ liệu hết hạn có nêu rõ.
- Các truy vấn SQL ở cuối tài liệu chỉ dùng **đọc** để đối chiếu trạng thái lưu trữ. Chụp kết quả hoặc đính kèm log, che thông tin cá nhân của Candidate.

**Quy ước ghi kết quả:** với mỗi ca, điền `Actual`, `Status` (`NOT RUN`/`PASS`/`FAIL`/`BLOCKED`) và `Evidence` (ảnh, URL, log, mã bản ghi). Nếu FAIL, ghi mã lỗi trong bug tracker chung và liên kết tới ca này; không sửa trực tiếp dữ liệu test để biến ca thành PASS.

## ST-RJ-01 — Luồng chuẩn HM gửi yêu cầu → Director duyệt → HR đăng tin → Candidate ứng tuyển

**Vai trò:** HM-A, D, H, C. **Tiền điều kiện:** chưa có yêu cầu cùng tên trong phòng A; Candidate C có CV hợp lệ.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | HM-A vào `/requisitions/create`, nhập đủ trường bắt buộc và tiêu chí sàng lọc có tổng trọng số 100%, bấm **Gửi Giám đốc duyệt**. | Chuyển tới chi tiết; sinh mã `REQ-...`; trạng thái `Pending_Director`; HM-A thấy lịch sử gửi. |
| 2 | D mở yêu cầu từ danh sách/thông báo và bấm **Duyệt**. | Trạng thái `Approved`; có bản ghi phê duyệt và lịch sử; HM-A nhận thông báo đúng yêu cầu. |
| 3 | H mở yêu cầu Approved, chọn **Tạo tin**, kiểm tra dữ liệu nguồn chỉ đọc, nhập hạn nộp tương lai và bấm **Đăng tin**. | Tạo tin `Published`, `PostingDate` là giờ đăng; trang nội bộ hiển thị liên kết về yêu cầu nguồn; HM-A nhận thông báo phát hành. |
| 4 | Mở `/jobs`, tìm tin theo tiêu đề rồi mở `/jobs/{id}` ở phiên khách/Candidate. | Tin xuất hiện trong danh sách và trang chi tiết công khai; thông tin nội bộ (lịch sử duyệt, lý do tuyển dụng, người tạo) không lộ ra. |
| 5 | C nộp CV qua nút ứng tuyển của tin. | Có thông báo nộp thành công và hồ sơ mới gắn với đúng `JobPostingId`; số hồ sơ nội bộ cập nhật khi tải lại. |

**Actual:** …  **Status:** NOT RUN  **Evidence:** …

## ST-RJ-02 — Director từ chối, HM sửa và gửi lại

**Vai trò:** HM-A, D. **Tiền điều kiện:** một yêu cầu `Pending_Director` mới của HM-A, không dùng dữ liệu ST-RJ-01.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | D mở chi tiết, bấm **Từ chối** nhưng để trống lý do. | Hiện lỗi yêu cầu nhập lý do; trạng thái vẫn `Pending_Director`; không có quyết định từ chối mới. |
| 2 | D nhập lý do rõ ràng và xác nhận từ chối. | Trạng thái `Rejected`; lưu ý kiến/Director/thời điểm; HM-A nhận thông báo và xem được lý do. |
| 3 | H thử vào chi tiết yêu cầu này và thử mở form tạo tin từ ID yêu cầu. | HR không được xem yêu cầu chưa Approved; không tạo được Job Posting. |
| 4 | HM-A sửa nội dung yêu cầu Rejected và gửi lại; D duyệt. | Sau gửi lại là `Pending_Director`, sau duyệt là `Approved`; lịch sử giữ cả lần từ chối lẫn lần duyệt. |

**Actual:** …  **Status:** NOT RUN  **Evidence:** …

## ST-RJ-03 — Rút yêu cầu đang chờ, sửa bản nháp rồi xóa

**Vai trò:** HM-A, D. **Tiền điều kiện:** yêu cầu `Pending_Director` mới của HM-A.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | HM-A bấm **Rút lại** tại trang chi tiết. | Yêu cầu trở về `Draft`, có lịch sử rút; HM-A có quyền sửa. |
| 2 | D thử mở URL quyết định cũ hoặc gửi lại quyết định cho ID này. | Không có quyết định mới; trạng thái vẫn `Draft`. |
| 3 | HM-A sửa một trường, lưu nháp, mở lại chi tiết. | Giá trị mới được giữ; trạng thái vẫn `Draft`. |
| 4 | HM-A xóa bản nháp. | Không còn trong danh sách; mở lại URL cũ không thấy yêu cầu. |

**Actual:** …  **Status:** NOT RUN  **Evidence:** …

## ST-RJ-04 — Director tạo và phê duyệt trực tiếp

**Vai trò:** D, H. **Tiền điều kiện:** phòng A đang hoạt động, tên vị trí mới.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | D vào `/requisitions/create`, chọn phòng A, nhập đủ dữ liệu và chọn thao tác phê duyệt trực tiếp. | Yêu cầu tạo ra ở `Approved`, đúng phòng A; có lịch sử phê duyệt trực tiếp. |
| 2 | D xem chi tiết; H mở danh sách yêu cầu Approved. | D không cần duyệt lần hai; H thấy yêu cầu và có thể mở luồng tạo tin. |
| 3 | H tạo tin nháp từ yêu cầu này. | Tin `Draft` gắn đúng `RequisitionId`; chưa thấy trên `/jobs`. |

**Actual:** …  **Status:** NOT RUN  **Evidence:** …

## ST-RJ-05 — Sao chép yêu cầu thành đợt tuyển mới

**Vai trò:** HM-A, D, H. **Tiền điều kiện:** một yêu cầu `Approved` hoặc `Rejected` của HM-A; không có bản nháp cùng tên/phòng A.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | HM-A mở yêu cầu nguồn và chọn **Sao chép**. | Form mới được điền từ nguồn nhưng chưa tạo bản ghi; tiêu chí sàng lọc được sao chép. |
| 2 | Giữ tên vị trí, lưu bản nháp. | ID/mã yêu cầu mới, `RecruitmentRound` tăng một và `OriginalRequisitionId` trỏ về nguồn; nguồn không đổi. |
| 3 | Thử sao chép một yêu cầu `Draft` hoặc `Pending_Director`. | Bị chặn; không tạo đợt mới. |
| 4 | Gửi yêu cầu mới và duyệt nếu cần; H mở danh sách. | Chỉ sau khi Approved, H mới thấy nguồn mới để tạo tin. |

**Actual:** …  **Status:** NOT RUN  **Evidence:** …

## ST-RJ-06 — Kiểm tra nhập liệu và quyền Requisition qua UI/URL trực tiếp

**Vai trò:** HM-A, HM-B, D, H, SA. **Tiền điều kiện:** có yêu cầu Draft của HM-A và yêu cầu Pending/Approved trong phòng A.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | HM-A gửi form thiếu trường bắt buộc hoặc tổng trọng số tiêu chí khác 100%. | Lỗi xuất hiện cạnh trường; không tạo yêu cầu `Pending_Director`. |
| 2 | HM-A tạo hai bản nháp cùng tên vị trí trong phòng A. | Lần thứ hai bị chặn; chỉ còn một bản nháp. |
| 3 | HM-B mở URL sửa/xóa bản nháp HM-A; H mở URL tạo yêu cầu; SA mở `/requisitions`. | Không được thực hiện thao tác; dữ liệu và lịch sử không đổi. |
| 4 | H mở URL trực tiếp của yêu cầu Draft/Pending/Rejected, rồi mở Approved. | Chỉ yêu cầu Approved xem được. |
| 5 | D thử quyết định yêu cầu không ở `Pending_Director` hoặc yêu cầu do chính D tạo. | Không phát sinh quyết định mới; trạng thái không đổi. |

**Actual:** …  **Status:** NOT RUN  **Evidence:** …

## ST-RJ-07 — HR lưu nháp tin, chặn tin trùng và phát hành

**Vai trò:** H, HM-A, D, C. **Tiền điều kiện:** một yêu cầu `Approved` chưa có tin mở; hạn nộp hợp lệ.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | H mở `/internal/job-postings/create?requisitionId={id}`, kiểm tra form không có lựa chọn hẹn đăng, lưu bản nháp. | Tin có `PostingStatus=Draft`, `PostingDate=NULL`; HR xem/sửa được; HM-A và D không mở được tin nháp bằng URL trực tiếp. |
| 2 | Phiên khách/Candidate tìm tin trên `/jobs` và mở `/jobs/{id}`. | Tin nháp không xuất hiện; URL chi tiết công khai không trả nội dung tin. |
| 3 | H thử tạo tin thứ hai từ cùng yêu cầu khi tin nháp còn mở. | Bị chặn; vẫn chỉ một tin mở cho yêu cầu. |
| 4 | H sửa tin nháp, nhập hạn nộp tương lai, bấm **Đăng tin**. | Cùng `JobPostingId` đổi sang `Published`, `PostingDate` được đặt tại lúc đăng; thông báo HM-A là “đã phát hành”. |
| 5 | HM-A và D mở tin nội bộ; C mở `/jobs/{id}`. | Các vai trò xem được đúng phạm vi; Candidate thấy tin công khai và nút ứng tuyển. |

**Actual:** …  **Status:** NOT RUN  **Evidence:** …

## ST-RJ-08 — Chặn đăng tin khi dữ liệu hoặc quyền không hợp lệ

**Vai trò:** H, HM-A, D, SA. **Tiền điều kiện:** một yêu cầu Approved mới và một tin Draft hợp lệ để thử đăng.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | H tạo/đăng tin với hạn nộp đã qua hoặc thiếu nội dung bắt buộc. | Form báo lỗi; không có tin Published mới; Candidate không thấy tin. |
| 2 | H gửi lại form với `requisitionId` bị đổi sang yêu cầu chưa Approved hoặc đổi nguồn khi sửa tin. | Server từ chối, không gắn tin sang nguồn khác và không tạo tin công khai. |
| 3 | HM-A, D, SA thử truy cập URL tạo/sửa/đăng/tạm dừng/đóng tin bằng phiên riêng. | Không thực hiện được thao tác quản lý; trạng thái, `PostingDate` và lịch sử không đổi. |
| 4 | Gửi POST thao tác quản lý không có CSRF token. | Bị chặn trước khi thay đổi dữ liệu. |

**Actual:** …  **Status:** NOT RUN  **Evidence:** …

## ST-RJ-09 — Tạm dừng và mở lại tin đang đăng

**Vai trò:** H, HM-A, C. **Tiền điều kiện:** tin Published còn hạn nộp từ yêu cầu phòng A.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | H bấm **Tạm dừng**. | Tin thành `Paused`; có lịch sử; tin biến mất khỏi `/jobs`, URL `/jobs/{id}` không còn hiển thị cho Candidate. |
| 2 | HM-A mở chi tiết tin nội bộ, C thử ứng tuyển qua URL cũ. | HM-A vẫn xem được tin thuộc phòng mình; C không nộp được hồ sơ mới. |
| 3 | H bấm **Mở lại/Đăng tin**. | Tin trở lại `Published`, `PostingDate` được cập nhật; có lịch sử/thông báo phát hành. |
| 4 | C tải lại `/jobs/{id}` và ứng tuyển nếu chưa nộp tin này. | Tin hiển thị lại và nhận hồ sơ khi hạn nộp còn hiệu lực. |

**Actual:** …  **Status:** NOT RUN  **Evidence:** …

## ST-RJ-10 — Đóng tin, xử lý hồ sơ liên quan và đăng lại

**Vai trò:** H, C. **Tiền điều kiện:** tin Published/Paused có ít nhất một hồ sơ đang xử lý; chuẩn bị riêng biến thể có Offer chưa kết thúc. Luồng Offer thuộc module khác, cần phối hợp người phụ trách để tạo/hoàn tất fixture.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | H bấm **Đóng tin** khi còn Offer chưa kết thúc. | Hệ thống báo phải xử lý Offer trước; tin chưa thành `Closed`; hồ sơ chưa bị kết thúc. |
| 2 | Hoàn tất Offer theo luồng được phép, rồi H bấm **Đóng tin** lại. | Tin thành `Closed`, có lịch sử; các hồ sơ chờ không có Offer được nhận bị xử lý theo luồng đóng, lịch phỏng vấn liên quan bị hủy; hồ sơ đã có Offer được nhận không bị từ chối. |
| 3 | C mở `/jobs` và URL công khai của tin cũ; H mở chi tiết nội bộ. | Tin đóng không hiển thị công khai; nội bộ vẫn xem được lịch sử; tin cũ không sửa/đăng lại trên cùng bản ghi. |
| 4 | H chọn **Đăng lại** từ tin đã đóng, lưu bản nháp rồi phát hành. | Tạo `JobPostingId` mới, cùng `RequisitionId`, `PostingNo` tăng; bản ghi cũ vẫn `Closed`; chỉ tin mới xuất hiện công khai sau phát hành. |

**Actual:** …  **Status:** NOT RUN  **Evidence:** …

## ST-RJ-11 — Tin hết hạn và giới hạn nhận hồ sơ

**Vai trò:** H, C. **Tiền điều kiện:** tin `Published` có `ApplicationDeadline` đã qua trong database test. Tạo fixture bằng quy trình dữ liệu test được nhóm duyệt; không đổi đồng hồ hệ thống và không sửa trực tiếp database dùng chung.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | C tìm trên `/jobs`, sau đó mở URL `/jobs/{id}` đã lưu. | Tin không còn trong danh sách đang tuyển; chi tiết tin đã Published vẫn có thể xem. |
| 2 | C truy cập `/jobs/{id}/apply` và thử gửi hồ sơ. | Không tạo Application mới vì đã quá hạn. |
| 3 | H mở chi tiết nội bộ và kiểm tra trạng thái. | `PostingStatus` vẫn `Published` nếu HR chưa đóng; hết hạn không tự chuyển `Closed`. |

**Actual:** …  **Status:** NOT RUN  **Evidence:** …

## ST-RJ-12 — Xóa bản nháp Job Posting và chặn xóa tin đã phát hành

**Vai trò:** H, HM-A. **Tiền điều kiện:** một yêu cầu Approved có tin Draft chưa có hồ sơ; một tin Published khác.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | H xóa tin Draft chưa có hồ sơ. | Tin biến mất khỏi danh sách; không truy cập được chi tiết cũ; yêu cầu nguồn Approved vẫn tồn tại. |
| 2 | H tạo lại tin từ yêu cầu nguồn. | Tin mới có `JobPostingId` mới; `PostingNo = MAX(PostingNo)` của các tin **còn lưu** cùng yêu cầu + 1 (có thể lại là 1 nếu tin nháp vừa xóa là tin duy nhất). |
| 3 | H thử xóa tin Published; HM-A thử xóa tin Draft của H qua URL. | Cả hai thao tác bị chặn; dữ liệu và lịch sử của tin còn lại không đổi. |

**Actual:** …  **Status:** NOT RUN  **Evidence:** …

## Đối chiếu database sau mỗi ca (chỉ đọc)

Thay `12345` bằng `RequisitionId` đã ghi trước khi chạy trên SQL Server; chỉ thực hiện câu lệnh đọc.

```sql
DECLARE @RequisitionId INT = 12345;

SELECT RequisitionId, RequisitionCode, ApprovalStatus, RecruitmentRound,
       OriginalRequisitionId, SubmittedAt
FROM JobRequisition WHERE RequisitionId = @RequisitionId;

SELECT ApprovalId, DirectorId, [Status], Comments, ApprovalDate
FROM RequisitionApproval WHERE RequisitionId = @RequisitionId
ORDER BY ApprovalDate DESC;

SELECT JobPostingId, RequisitionId, PostingNo, PostingStatus,
       PostingDate, ApplicationDeadline, ClosedAt
FROM JobPosting WHERE RequisitionId = @RequisitionId
ORDER BY PostingNo;
```

Đối chiếu thêm số hồ sơ, lịch sử và thông báo bằng màn hình tương ứng. Nếu cần SQL sâu hơn, lấy tên cột từ `database/schema/db.sql` của đúng bản đang test.

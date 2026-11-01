# System test — phần HoangNH

**Phạm vi:** Requisition (5.1.14–5.1.16), Job Posting nội bộ (5.1.18–5.1.19), BR-CAN-01, Thông báo in-app (sự kiện + chuông WebSocket). Dashboard nội bộ làm ở đợt sau. Không gồm chatbot.

**Cách chạy:** app thật (`mvnw spring-boot:run`, cổng 8088) + SQL Server local `RitirementManagement2`, thao tác bằng trình duyệt. Khi cần hai người cùng lúc (xem chuông đổi ngay), người thứ hai thao tác bằng HTTP (curl, có đăng nhập + CSRF như trình duyệt). SQL chỉ dùng **đọc** để đối chiếu.

**Quy ước:** `Status` ∈ `NOT RUN` / `PASS` / `FAIL` / `BLOCKED`. FAIL ghi lỗi thực tế và nơi cần sửa; không sửa DB để ca thành PASS. Dữ liệu tạo trong test có tiền tố `ST-<ngày>` để dễ tìm.

## Tài khoản (seed `database/seeds`, mật khẩu chung trong `build_seed.py`)

| Ký hiệu | Username | Vai trò | Phòng ban |
|---|---|---|---|
| HM-A | `hm_tuan` | Hiring Manager | Engineering |
| HM-B | `hm_huong` | Hiring Manager | Sales & Marketing |
| D | `director` | Director | — |
| H | `hr_lan` | HR | Human Resources |
| SA | `admin` | System Admin | — |
| C | `phong.nguyen` | Candidate | — |
| Khách | (không đăng nhập) | — | — |

---

# A. Requisition

## ST-REQ-01 — HM gửi duyệt → Director duyệt (kèm thông báo)

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | HM-A `/requisitions/create`, nhập đủ trường, tiêu chí tổng 100%, bấm **Gửi duyệt**. | Về trang chi tiết, mã `REQ-…`, trạng thái **Chờ duyệt** (`Pending_Director`). |
| 2 | D mở chuông / `/notifications`. | Có thông báo yêu cầu chờ duyệt, bấm vào tới đúng `/requisitions/{id}`. |
| 3 | D bấm **Duyệt**. | Trạng thái **Đã duyệt**; lịch sử có dòng duyệt. |
| 4 | HM-A mở chuông. | Có thông báo "đã được duyệt" link đúng yêu cầu. |
| 5 | H mở `/requisitions`. | Thấy yêu cầu vừa duyệt, có nút tạo tin. |

## ST-REQ-02 — Director từ chối, HM sửa và gửi lại

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | HM-A tạo và gửi duyệt yêu cầu mới. D bấm **Từ chối** để trống lý do. | Bị chặn, báo cần lý do; vẫn Chờ duyệt. |
| 2 | D nhập lý do, từ chối. | **Bị từ chối**; HM-A nhận thông báo có lý do. |
| 3 | H mở URL chi tiết yêu cầu này. | Bị chặn (HR chỉ xem Đã duyệt). |
| 4 | HM-A sửa, gửi lại; D duyệt. | Chờ duyệt → Đã duyệt; lịch sử giữ cả lần từ chối. |

## ST-REQ-03 — Rút yêu cầu (N1), sửa nháp, xóa nháp

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | HM-A gửi duyệt yêu cầu mới rồi bấm **Rút lại**. | Về **Nháp**; D nhận thông báo `REQUISITION_WITHDRAWN`. |
| 2 | D gửi POST `/requisitions/{id}/decision` cho yêu cầu đã rút. | Không có quyết định mới, vẫn Nháp. |
| 3 | HM-A sửa một trường, lưu nháp. | Giá trị mới được giữ, vẫn Nháp. |
| 4 | HM-A xóa bản nháp. | Mất khỏi danh sách; URL cũ báo không tìm thấy. |

## ST-REQ-04 — Director tạo và duyệt trực tiếp

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | D `/requisitions/create`, chọn phòng, nhập đủ, bấm thao tác duyệt luôn. | Tạo thẳng **Đã duyệt**; không tự gửi thông báo chờ duyệt cho chính D. |
| 2 | H mở `/requisitions`. | Thấy yêu cầu, tạo được tin. |

## ST-REQ-05 — Sao chép thành đợt mới

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | HM-A mở yêu cầu Đã duyệt, bấm **Sao chép thành đợt mới**. | Form điền sẵn (cả tiêu chí), hiện **Đợt 2**; chưa tạo bản ghi. |
| 2 | Lưu nháp. | Yêu cầu mới `RecruitmentRound = 2`, `OriginalRequisitionId` trỏ về gốc; gốc không đổi. |
| 3 | Mở `/requisitions/copy/{id}` của yêu cầu Nháp / Chờ duyệt. | Bị chặn, không tạo đợt mới. |

## ST-REQ-06 — Nhập liệu và quyền qua UI / URL

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | HM-A gửi form thiếu trường bắt buộc / tiêu chí khác 100%. | Lỗi cạnh ô, không tạo bản ghi. |
| 2 | HM-A lưu 2 bản nháp cùng tên vị trí cùng phòng. | Lần 2 bị chặn. |
| 3 | HM-B mở URL sửa nháp của HM-A; H mở `/requisitions/create`; SA mở `/requisitions`. | 403 / bị chặn; dữ liệu không đổi. |
| 4 | Danh sách `/requisitions` của HM-B. | Không thấy yêu cầu phòng Engineering. |

---

# B. Job Posting nội bộ

## ST-JP-01 — HR lưu nháp tin, tin nháp không lộ ra ngoài

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | H từ yêu cầu Đã duyệt bấm **Tạo tin**, nhập hạn nộp tương lai, **Lưu nháp**. | Tin **Nháp**, `PostingDate` NULL, **Tin lần 1**. |
| 2 | Khách mở `/jobs` và `/jobs/{id}`. | Không thấy tin nháp. |
| 3 | HM-A, D mở `/internal/job-postings/{id}`. | Bị chặn (tin nháp chỉ HR mở). |
| 4 | H mở lại `/internal/job-postings/create?requisitionId=` cùng yêu cầu. | Bị chặn, yêu cầu đã có tin đang mở. |

## ST-JP-02 — Đăng tin (kèm thông báo)

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | H mở tin nháp, bấm **Đăng tin**. | **Đang đăng**, `PostingDate` = lúc đăng. |
| 2 | Khách mở `/jobs`, `/jobs/{id}`. | Tin hiện; không lộ thông tin nội bộ. |
| 3 | HM-A mở chuông. | Thông báo "tin đã phát hành", link tới tin. |

## ST-JP-03 — Chặn dữ liệu sai, quyền sai, thiếu CSRF

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | H lưu tin với hạn nộp đã qua / thiếu nội dung bắt buộc. | Lỗi cạnh ô; không có tin đăng mới. |
| 2 | H mở form tạo tin với `requisitionId` của yêu cầu chưa duyệt / id giả. | Bị chặn. |
| 3 | HM-A, D, SA POST `/internal/job-postings/{id}/pause` / `close`. | 403; trạng thái không đổi. |
| 4 | H POST `/internal/job-postings/{id}/pause` không có `_csrf`. | 403; trạng thái không đổi. |

## ST-JP-04 — Tạm dừng (N2) và mở lại

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | H bấm **Tạm dừng** tin đang đăng. | **Tạm dừng**; HM-A nhận `JOB_POSTING_PAUSED`. |
| 2 | Khách mở `/jobs/{id}`. | Không thấy tin. |
| 3 | H bấm **Mở lại**. | **Đang đăng**, `PostingDate` mới; khách thấy lại tin. |

## ST-JP-05 — Đóng tin (N2) và Đăng lại

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | H bấm **Đóng tin**. | **Đã đóng**, có `ClosedAt`; HM-A nhận `JOB_POSTING_CLOSED`; khách không thấy tin. |
| 2 | H bấm **Đăng lại (tạo tin lần 2)**, đăng. | Tin mới `JobPostingId` khác, cùng `RequisitionId`, `PostingNo = 2`; tin cũ vẫn Đã đóng. |

## ST-JP-06 — BR-CAN-01: chỉ nộp lại khi có đợt mới

**Tiền điều kiện:** yêu cầu đợt 1 có tin Lần 1 đang đăng; C chưa nộp vào yêu cầu này.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | C nộp CV PDF vào tin Lần 1. | Nộp thành công. |
| 2 | H đóng tin Lần 1, **Đăng lại** thành Lần 2 và đăng. | Lần 2 hiện công khai. |
| 3 | C mở `/jobs/{id Lần 2}`. | Hiện đã ứng tuyển, không cho nộp. |
| 4 | C POST nộp thẳng vào Lần 2. | Báo "Bạn đã ứng tuyển vị trí này trong đợt tuyển dụng hiện tại…", không tạo đơn. |
| 5 | HM-A sao chép thành **Đợt 2**, D duyệt, H tạo + đăng tin; C nộp vào tin đợt 2. | Nộp thành công. |

## ST-JP-07 — Xóa nháp, chặn xóa tin đã đăng

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | H xóa tin Nháp chưa có hồ sơ. | Mất khỏi danh sách; yêu cầu nguồn còn Đã duyệt. |
| 2 | H POST `/internal/job-postings/delete/{id}` cho tin đang đăng. | Bị chặn, tin không đổi. |

## ST-JP-08 — Danh sách tin nội bộ: tìm, lọc, phạm vi

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | H tìm theo tiêu đề `ST-`, lọc trạng thái, phân trang. | Kết quả đúng; tham số lạ (`page=-1`, `status=abc`) bị bỏ, không lỗi 500. |
| 2 | HM-B mở danh sách. | Không thấy tin phòng Engineering. |

---

# C. Thông báo

## ST-NT-01 — Chuông cập nhật ngay qua WebSocket

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | HM-A mở một trang nội bộ, ghi số trên chuông. | Số = chưa đọc trong DB. |
| 2 | Không tải lại trang; H (curl) tạm dừng một tin của yêu cầu HM-A. | Số trên chuông của HM-A tăng 1 trong vài giây. |

## ST-NT-02 — Đọc thông báo, đọc tất cả, không đọc được của người khác

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | HM-A bấm một thông báo chưa đọc. | Tới đúng link, thông báo thành đã đọc, chuông giảm 1. |
| 2 | HM-A bấm **Đánh dấu tất cả đã đọc**. | Chuông về 0. |
| 3 | HM-A POST `/api/notifications/{id của D}/read`. | Không đổi thông báo của D. |
| 4 | Khách mở `/notifications`. | Về trang đăng nhập. |

---

# D. Dashboard nội bộ (`/dashboard` → `/dashboard/internal`)

**Cách kiểm:** (1) số trên thẻ KPI phải bằng số dòng ở trang mà thẻ đó dẫn tới; (2) thao tác thật ở module khác thì dashboard đổi đúng; (3) biểu đồ vẽ được, không lỗi JS.

## ST-DB-01 — Vào đúng dashboard theo vai trò

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | HM-A, H, D, `interviewer_viet` lần lượt mở `/dashboard`. | Về `/dashboard/internal`, đúng khối của vai trò (HM / HR / Giám đốc / Người phỏng vấn). |
| 2 | SA mở `/dashboard`; SA mở `/dashboard/internal`. | SA về dashboard quản trị; trang nội bộ → 403. |
| 3 | C mở `/dashboard/internal`; khách mở `/dashboard/internal`. | C → 403; khách → `/login`. |

## ST-DB-02 — Dashboard Hiring Manager

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | HM-A mở dashboard; so thẻ "Yêu cầu chờ Giám đốc duyệt" với `/requisitions?status=Pending_Director`. | Hai số bằng nhau; dòng phạm vi "Phòng Engineering". |
| 2 | HM-A gửi duyệt một yêu cầu mới. | Thẻ chờ duyệt +1. |
| 3 | HM-A lưu một bản nháp; D từ chối một yêu cầu khác có lý do. | "Việc cần xử lý" có bản nháp (nút **Gửi duyệt**) và yêu cầu bị từ chối kèm lý do (nút **Chỉnh sửa**), bị từ chối xếp trước. |
| 4 | Biểu đồ "Tiến độ tuyển dụng". | Vẽ được (canvas hiện), có yêu cầu đã duyệt của phòng; không lỗi JS. |

## ST-DB-03 — Dashboard HR

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | H mở dashboard; so "Hồ sơ chờ sàng lọc" với số hồ sơ tab `/applications?stage=APPLIED`. | Bằng nhau. |
| 2 | Có yêu cầu đã duyệt chưa có tin. | "Việc cần xử lý" có dòng "Đã duyệt, chưa có tin tuyển dụng" nút **Tạo tin** đúng `requisitionId`. |
| 3 | H tạo + đăng tin cho yêu cầu đó. | Dòng đó biến mất; "Tin mới đăng trong ngày" +1. |
| 4 | Biểu đồ "Xu hướng hồ sơ ứng tuyển". | Vẽ được, 8 tuần; không lỗi JS. |

## ST-DB-04 — Dashboard Giám đốc

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | D mở dashboard; so "Yêu cầu tuyển dụng chờ duyệt" với `/requisitions?status=Pending_Director` của D. | Bằng nhau. |
| 2 | HM-A gửi duyệt một yêu cầu đợt 2. | Thẻ +1; danh sách chờ duyệt có dòng "Tuyển lại lần 2", link tới yêu cầu. |
| 3 | D duyệt yêu cầu đó. | Thẻ −1, dòng biến mất. |
| 4 | So "Thư mời nhận việc chờ duyệt" với DB (`OfferProposal` `Pending_Director`, chưa xóa). Biểu đồ theo phòng ban. | Bằng nhau; biểu đồ vẽ được. |

## ST-DB-05 — Dashboard Người phỏng vấn

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | `interviewer_viet` mở dashboard. | Có 3 thẻ, danh sách đánh giá chưa xong, lịch tuần; không nạp Chart.js. |
| 2 | Bấm tuần trước / tuần sau; mở `?week=abc`. | Đổi đúng tuần; tham số lạ vẫn hiện tuần hiện tại, không lỗi 500. |
| 3 | So "Đánh giá chưa hoàn thành" với số dòng danh sách đánh giá. | Bằng nhau. |

---

# Kết quả chạy — 31/10/2026

**Môi trường:** nhánh `feat/hoang-dashboard-fix` @ `2a781530`, app khởi động lại từ code này lúc 22:35, Chrome + HTTP trong phiên trình duyệt, curl cho người thứ hai ở ST-NT-01.
**Dữ liệu tạo ra (giữ lại để xem):** REQ 2789 (đợt 1), 2790, 2792, 2793 (đợt 2 của 2789); tin 1689 (Lần 1, Đã đóng), 1690 (Lần 2), 1691 (đợt 2); đơn 1520 (đã Rejected khi đóng tin 1689) và đơn mới vào tin 1691 của `phong.nguyen`. REQ 2791, 2794 và tin 1692, 1693 đã xóa trong test.

| Ca | Status | Actual / Evidence |
|---|---|---|
| ST-REQ-01 | PASS | REQ-2789 tạo qua UI → Chờ duyệt; D nhận `REQUISITION_PENDING_DIRECTOR` link `/requisitions/2789`; D bấm Phê duyệt → Đã duyệt; HM nhận `REQUISITION_APPROVED`; HR thấy yêu cầu + link tạo tin. |
| ST-REQ-02 | PASS | REQ-2790: lý do trống → "Vui lòng nhập lý do để Hiring Manager chỉnh sửa.", vẫn Chờ duyệt; có lý do → Từ chối, HM nhận thông báo kèm "Lý do: Thiếu mức lương dự kiến"; HR mở chi tiết → 403; sửa gửi lại → duyệt, lịch sử còn cả Trả lại và Phê duyệt. |
| ST-REQ-03 | PASS | REQ-2791: rút → Bản nháp, D nhận `REQUISITION_WITHDRAWN`; D quyết định trên bản nháp → 403; sửa nháp giữ giá trị mới; xóa → URL cũ 404. |
| ST-REQ-04 | PASS | D không chọn phòng → "Vui lòng chọn phòng ban đang hoạt động."; chọn Engineering → REQ-2792 Đã duyệt ngay, D không tự nhận thông báo chờ duyệt; HR thấy + link tạo tin. |
| ST-REQ-05 | PASS | UI sao chép REQ-2789 → form "Đợt 2" điền sẵn; lưu nháp → REQ-2793 `RecruitmentRound=2`, `OriginalRequisitionId=2789`, 1 tiêu chí; sao chép bản nháp → 400. |
| ST-REQ-06 | PASS | Thiếu tên/mô tả, tổng tiêu chí 60% → lỗi theo ô; nháp trùng tên (khác hoa thường) → bị chặn; HM-B sửa/xóa nháp HM-A → 403, không thấy yêu cầu Engineering; HR tạo/xem nháp → 403; SA `/requisitions` → 403; D quyết định yêu cầu đã duyệt → 403. |
| ST-JP-01 | PASS | UI tạo tin nháp 1689 (Lần 1) từ REQ-2789; khách `/jobs` không thấy, `/jobs/1689` 404; HM, D mở nháp → 403; HR tạo tin thứ hai cùng yêu cầu → "Yêu cầu này đã có tin nháp hoặc tin đang hoạt động." |
| ST-JP-02 | PASS | UI Đăng tin + hộp xác nhận → Đang đăng, `PostingDate` 22:49:51; khách thấy tin, không lộ lý do tuyển / mã REQ / tên HM; HM nhận `JOB_POSTING_PUBLISHED`. |
| ST-JP-03 | PASS | Hạn nộp đã qua, thiếu tiêu đề/mô tả → lỗi theo ô; tạo từ yêu cầu Nháp (GET và POST) → "Chỉ có thể đăng tin từ yêu cầu đã được phê duyệt."; id giả → 404; HM/D/SA pause, close → 403; HR pause thiếu CSRF → 403; DB không có tin thừa. |
| ST-JP-04 | PASS | Tạm dừng 1689 → HM nhận `JOB_POSTING_PAUSED`, xem được tin Tạm dừng; khách 404; Mở lại → Đang đăng, `PostingDate` đổi sang 22:51:19, khách thấy lại. |
| ST-JP-05 | PASS | Đóng 1689 → Closed, `ClosedAt` 22:51:39, HM nhận `JOB_POSTING_CLOSED`, khách 404; UI "Đăng lại (tạo tin lần 2)" → tin 1690 cùng REQ-2789, `PostingNo=2`, 1689 vẫn Closed. |
| ST-JP-06 | PASS | C nộp 1689 thành công; sau Đăng lại, `/jobs/1690` hiện nút "Đã ứng tuyển" bị khóa, POST nộp thẳng → "Bạn đã ứng tuyển vị trí này trong đợt tuyển dụng hiện tại. Vui lòng chờ đợt tuyển dụng mới."; đợt 2 (REQ-2793 → tin 1691) nộp thành công. |
| ST-JP-07 | PASS | HM xóa nháp của HR → 403; HR xóa nháp → biến mất (404), REQ-2792 vẫn Đã duyệt; xóa tin đang đăng 1691 → "Chỉ có thể xóa bản nháp chưa có hồ sơ ứng tuyển.", tin còn. |
| ST-JP-08 | PASS | HR tìm `ST-3110` → 3 dòng (2 Đang đăng, 1 Đã đóng); lọc Đã đóng → 1 dòng; 6 kiểu tham số lạ → 200; HM-B → 0 dòng, mở thẳng 1691 → 403. |
| ST-NT-01 | PASS | HM-A mở `/requisitions` (chuông 5); HR (curl) tạm dừng 1689 lúc 22:50:43 → chuông thành 6 cùng giây, không tải lại trang; dropdown hiện "Tin tuyển dụng đã tạm dừng… Vừa xong". |
| ST-NT-02 | PASS | Bấm thông báo → tới `/internal/job-postings/1689`, thông báo thành đã đọc; Đánh dấu tất cả → 0; HM-A đánh dấu thông báo 10480 của D → 400, thông báo của D vẫn chưa đọc; khách `/notifications` → `/login`. |
| ST-DB-01 | PASS | HM / HR / D / `interviewer_viet` → `/dashboard/internal`, đúng 3 KPI + khối từng vai trò; SA `/dashboard` → `/dashboard/admin`, `/dashboard/internal` → 403; C → 403; khách → `/login`. |
| ST-DB-02 | PASS | "Phòng Engineering"; thẻ chờ duyệt 5 = 5 dòng trang lọc; gửi duyệt REQ-2795 → 6; Việc cần xử lý: REQ-2797 bị từ chối kèm "Cần ghi rõ mức lương" + **Chỉnh sửa** đứng đầu, REQ-2796 nháp + **Gửi duyệt**; biểu đồ tiến độ vẽ bằng Chart.js, có REQ-2789. |
| ST-DB-03 | **FAIL** | "Hồ sơ chờ sàng lọc" 16 = tab Ứng tuyển 16; dòng "Yêu cầu" có **Tạo tin** đúng `requisitionId`; đăng tin REQ-2790 → "Tin mới đăng trong ngày" 2 → 3; biểu đồ 8 tuần vẽ được. **Lỗi:** bấm thẻ "Tin mới đăng trong ngày" (3) → danh sách 10 dòng vì trang tin bỏ qua `postedFrom` / `postedTo` (`JobPostingListService` để `showPostedDate = false` nên `normalizeFilter` xóa bộ lọc ngày). |
| ST-DB-04 | PASS | Chờ duyệt 7 = 7 dòng trang lọc; HM gửi đợt 2 của REQ-2792 (REQ-2798) → 8; D duyệt → 7, dòng biến mất; offer chờ duyệt 3 và trúng tuyển trong tháng 4 = DB; có dòng "Tuyển lại lần 2", "+2 yêu cầu khác", "Xem tất cả"; biểu đồ 5 phòng (Đã duyệt / Chờ duyệt). |
| ST-DB-05 | PASS | 3 KPI, không nạp Chart.js; "Đánh giá cần hoàn thành · 4" = 4 dòng = KPI 4; tuần 26/10–01/11, nút trước / sau → 19/10–25/10 và 02/11–08/11; `week=abc`, ngày sai, rỗng → tuần hiện tại, 200. |

**Dữ liệu thêm ở phần D:** REQ 2795 (Chờ duyệt), 2796 (Nháp), 2797 (Từ chối), 2798 (đợt 2 của 2792, Đã duyệt); tin 1694 (REQ-2790, Đang đăng).

## Nhận xét (không làm ca FAIL)

1. Sao chép yêu cầu chưa duyệt (`/requisitions/copy/{id}`) hiện trang lỗi 400 chung, không có thông báo dễ hiểu như các thao tác khác.
2. Trang 404 khi người dùng nội bộ mở bản ghi đã xóa dùng giao diện công khai "Mộc Careers".
3. Lịch sử yêu cầu ghi lần duyệt là "Cập nhật"; ý kiến duyệt nằm ở khung "Ý kiến của Giám đốc".
4. Đóng tin báo "hệ thống gửi mail đang gián đoạn" vì máy local không có mail server (phần gửi mail không thuộc module này).
5. HR "Việc cần xử lý" chỉ hiện 5 việc chờ lâu nhất; link "Xem tất cả" chỉ tới `/applications`, nên yêu cầu đã duyệt chưa có tin bị đẩy ra ngoài top 5 thì không có lối vào từ dashboard.
6. HR thẻ "Hồ sơ chờ lên lịch phỏng vấn" 52 (chỉ hồ sơ HR cần xếp lịch) nhưng link mở tab Phỏng vấn 61 hồ sơ; `/applications` chưa có bộ lọc "Chờ xếp lịch" (module DungLT).
7. Giám đốc: thẻ ghi "Thư mời nhận việc chờ duyệt", khối danh sách lại ghi "Đề nghị tuyển dụng chờ duyệt" — nên thống nhất một tên.

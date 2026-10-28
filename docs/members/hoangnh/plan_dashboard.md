# Kế hoạch thiết kế lại Dashboard nội bộ (HR, Hiring Manager, Director, Interviewer)

- **Người phụ trách:** Nguyễn Huy Hoàng (hoangnh)
- **Ngày lập:** 28/10/2026
- **Nhánh:** `feat/hoang-dashboard-fix`
- **Mockup (chưa được GV xác nhận, code song song theo chỉ đạo ngày 28/10):** https://claude.ai/artifact/6L9Av16WzaPSpp5JNaQEKy. Có 4 artboard: Hiring Manager, HR, Director, Interviewer. Đã sửa theo bản nhận xét UI/UX ngày 28/10.
- **Quy tắc phải theo:** `CLAUDE.md` §1 (wireframe phải được GV duyệt), §7 (hằng số), §8 (`Clock`), §11 (cờ quyền), §15.3 (N+1), §20 (Thymeleaf), §23 (test). `UI_RULES.md` §2A (layout, token chỉ Lead sửa) và §7 (các điều cấm). `ARCHITECTURE_GUIDE.md` §1 (cây module).
- **Phạm vi:** chỉ dashboard nội bộ `/dashboard/internal`. Dashboard System Admin (`/dashboard/admin`) giữ nguyên.

---

## 0. Nguyên tắc thiết kế chung

Mỗi dashboard trả lời đúng một câu hỏi: **"Hôm nay tôi cần làm gì?"**. Phần số liệu chỉ hỗ trợ cho câu hỏi đó.

| Thành phần | Quy ước |
|---|---|
| **4 thẻ KPI** | Chia 2 loại. **Dòng chảy** (hồ sơ mới, người nhận việc…) đếm trong 30 ngày gần nhất, có mũi tên % so với 30 ngày trước đó. **Tồn đọng** (chờ duyệt, chờ sàng lọc…) là số hiện tại, có dòng ngữ cảnh như "Lâu nhất: 6 ngày". Bấm thẻ mở màn danh sách đã lọc sẵn. |
| **Danh sách việc** | Đặt trên cùng, ngay dưới KPI. Việc chờ lâu nhất đứng đầu. Mỗi dòng có một nút thao tác chính. Hiện tối đa 5 dòng, phần còn lại ghi "còn N việc". |
| **Biểu đồ** | Mỗi role 0–2 biểu đồ, chỉ giữ biểu đồ dẫn tới hành động. Mọi biểu đồ vẽ bằng HTML phía server, không dùng JavaScript (D4): thanh ngang dùng fragment `barChart` sẵn có, xu hướng theo tuần là cột dọc. Nhãn tiếng Việt đọc trọn, không xoay chữ. Các bước của quy trình dùng chung một màu. Màu đỏ/cam chỉ dùng để báo quá hạn. |
| **Gọn, ít chữ, có icon** | Theo kiểu thẻ của dashboard Admin. Mỗi thẻ KPI có ô icon 44px nền xanh nhạt, nhãn ngắn, con số lớn, tối đa 1 dòng phụ. Tiêu đề mỗi khu vực có icon 20px, bỏ dòng mô tả dưới tiêu đề (trừ "Lịch tuần này" cần ghi khoảng ngày). Icon là SVG nét 1.7 cùng kiểu với sidebar/Admin, gom ở **một fragment** `dashboard/fragments :: icon(name)` để không lặp SVG. Service chỉ trả tên icon (`file`, `calendar`, `clock`…). Bỏ nút chọn kỳ 7 ngày / 30 ngày / Quý vì kỳ cố định 30 ngày (D5) |
| **Ngưỡng chờ** | Dùng hằng số có sẵn trong `DashboardLabels`: `WAIT_WARNING_DAYS = 3` tô cam, `WAIT_DANGER_DAYS = 7` tô đỏ. Mọi role dùng chung, không đặt số riêng trong template. |
| **Phạm vi số liệu** | Dòng mô tả phải ghi đúng phạm vi đang đếm: HR và Director là toàn công ty, HM là phòng ban của mình, Interviewer là lịch được phân công cho mình. |

## 1. Hiện trạng so với mockup

### 1.1 Hiring Manager (đã làm lại ở commit `037615a` → **cập nhật**)

| Mockup | Hiện tại | Việc cần làm |
|---|---|---|
| KPI: Hồ sơ mới vào phòng ban (30 ngày, có %), Đã tuyển / cần tuyển, Chờ Director duyệt (lâu nhất N ngày), Phỏng vấn đã diễn ra (30 ngày, có %) | KPI: Yêu cầu của tôi, Chờ duyệt, Hồ sơ ứng tuyển (tổng), Phỏng vấn sắp tới | Thay 3 thẻ, giữ thẻ "Chờ duyệt" (đổi dòng ngữ cảnh) |
| Cần bạn xử lý: bị từ chối, bản nháp, **chờ lâu** | Bị từ chối, bản nháp | Giữ nguyên. "Chờ lâu" đã hiện ở thẻ KPI, HM cũng không tự xử lý được (xem D9) |
| Lịch phỏng vấn sắp tới của phòng ban | Có | Giữ nguyên |
| **Tiến độ tuyển theo vị trí**: thanh đã tuyển/cần tuyển, nhãn "Thiếu ứng viên / Đang tiến triển / Đã đủ người" | Không có | **Mới**: 1 truy vấn gộp theo yêu cầu đang tuyển |
| Yêu cầu cần theo dõi (5 dòng mới cập nhật) | "Yêu cầu tuyển dụng gần đây" | Đổi tiêu đề, giữ dữ liệu |
| Không có biểu đồ trạng thái / pipeline | 2 biểu đồ: Yêu cầu theo trạng thái, Pipeline ứng viên | **Bỏ** cả 2 (theo nhận xét: trùng với danh sách và KPI) |

### 1.2 HR (đang dùng khung cũ + Chart.js → **làm mới**)

| Mockup | Hiện tại | Việc cần làm |
|---|---|---|
| KPI: Hồ sơ mới (30 ngày, %), Chờ sàng lọc (lâu nhất N ngày), Việc quá 3 ngày, Đề nghị được chấp nhận (30 ngày, %) | Tin đang tuyển, Tổng hồ sơ, Phỏng vấn sắp tới, Hồ sơ chờ HR (tiêu đề tiếng Anh trong service) | Viết lại, nhãn tiếng Việt |
| **Việc cần xử lý** (sàng lọc, tạo tin, lên lịch, lập đề nghị; xếp theo số ngày chờ) | Không có | **Mới** |
| Phỏng vấn sắp diễn ra (hôm nay + ngày mai) | Không có (chỉ có số) | **Mới**: dùng lại `departmentInterviews` nhưng không lọc theo phòng ban |
| **Hồ sơ đang chờ theo giai đoạn** (thanh ngang, phần quá 3 ngày tô đỏ) | Biểu đồ cột "Hồ sơ theo trạng thái": mất nhãn trục X, ghi "toàn hệ thống", trộn trạng thái kết thúc vào luồng | **Thay** |
| **Xu hướng hồ sơ ứng tuyển** (8 tuần) | "Hồ sơ nhận trong 14 ngày" (biểu đồ đường theo ngày) | Đổi sang theo tuần, 8 cột dọc HTML (D4) |
| Tin đang mở (ít hồ sơ trên mỗi vị trí đứng đầu) | Biểu đồ "Tin tuyển dụng theo trạng thái" | **Thay** bằng bảng |

### 1.3 Director (khung cũ → **làm mới**)

| Mockup | Hiện tại | Việc cần làm |
|---|---|---|
| KPI: Yêu cầu chờ duyệt, Đề nghị chờ duyệt, Người nhận việc (30 ngày, %), Tỷ lệ nhận đề nghị (điểm %) | Yêu cầu chờ, Đề nghị chờ, Lịch phỏng vấn của tôi, Ứng viên được phân công | Thay 2 thẻ cuối (xem D10) |
| **Yêu cầu chờ duyệt**: số ngày chờ, Tuyển mới / Tuyển thay thế, "Tuyển lại lần N" | Không có | **Mới**: dùng `SubmittedAt`, `HiringType`, `RecruitmentRound` |
| **Đề nghị chờ duyệt**: ứng viên, vị trí, mức lương, số ngày chờ | Không có | **Mới** |
| Yêu cầu đang mở theo phòng ban (thanh ngang 2 màu: chờ duyệt / đang tuyển) | Không có | **Mới** |
| Quyết định gần đây | "Hoạt động phê duyệt gần đây" | Giữ dữ liệu `recentDirectorActivity`, đổi giao diện |
| — | 3 biểu đồ Chart.js (hàng đợi, kết quả, quyết định theo ngày) | **Bỏ** (trùng với KPI và danh sách) |

> Hai danh sách chờ duyệt **không trùng nhau**: một bên là yêu cầu tuyển dụng do HM gửi, bên kia là đề nghị tuyển dụng do HR lập. Vẫn giữ cả hai.

### 1.4 Interviewer (khung cũ → **làm mới**)

| Mockup | Hiện tại | Việc cần làm |
|---|---|---|
| KPI: Phỏng vấn hôm nay (đã xong / còn lại), Chờ đánh giá (số phiếu quá 3 ngày), Sắp tới (7 ngày), Đã gửi đánh giá (tháng này) | Sắp tới, Ứng viên được phân công, Chờ đánh giá | Viết lại |
| **Buổi phỏng vấn tiếp theo** (còn N phút, nút Xem hồ sơ / Vào phòng họp) | Không có | **Mới** (xem D7) |
| **Đánh giá cần hoàn thành** (quá hạn đứng đầu, nút Gửi đánh giá) | Chỉ có số | **Mới** |
| **Lịch tuần này** (7 cột T2–CN, chuyển tuần trước / tuần sau) | 3 biểu đồ Chart.js | **Thay**, không có biểu đồ |

## 2. Quyết định cần chốt

> **Đã chốt 28/10/2026:** D1 code luôn; D2, D3, D5, D6, D8–D13 theo cột "Đề xuất"; D4 = (c); D7 = `/interviews`; D14 = `ApplicationStage`.

| # | Vấn đề | Lựa chọn | Đề xuất |
|---|---|---|---|
| **D1** | Mockup chưa được GV duyệt (§1) | Trình GV trước / code trước | **Đã chốt 28/10: code luôn**, trình GV song song; GV yêu cầu đổi thì sửa sau |
| **D2** | "Chờ bao nhiêu ngày" của hồ sơ: DB không có bảng lịch sử trạng thái | (a) dùng `Application.UpdatedAt`, nếu null thì dùng `CreatedAt`; (b) thêm bảng lịch sử (cần migration) | **(a)**, có ghi chú rằng đây là ngày cập nhật gần nhất |
| **D3** | Ngưỡng màu | Theo mockup (HR 3 ngày, Director 7 ngày) / thống nhất | **Thống nhất**: 3 ngày cam, 7 ngày đỏ (hằng số có sẵn) |
| **D4** | Vẽ biểu đồ | (a) thanh ngang bằng HTML, "Xu hướng" dùng Chart.js; (b) giữ Chart.js; (c) mọi biểu đồ bằng HTML phía server | **Đã chốt (c) ngày 28/10.** "Xu hướng" của HR vẽ thành 8 cột dọc HTML (mỗi cột 1 tuần). Dashboard nội bộ không dùng Chart.js nữa: chỉ còn một cách vẽ, ít code, người mới dễ sửa |
| **D5** | Kỳ so sánh của KPI dòng chảy | 30 ngày / tháng dương lịch | **30 ngày** gần nhất so với 30 ngày trước đó. Hai mốc thời gian lấy từ `Clock` |
| **D6** | Phạm vi HR | Toàn công ty / theo phòng ban | **Toàn công ty**, ghi rõ trong mô tả để sửa chỗ mâu thuẫn hiện tại ("theo phạm vi vai trò" và "toàn hệ thống") |
| **D7** | Nút "Vào phòng họp" / "Gửi đánh giá" của Interviewer | Hiện link họp trên dashboard / dẫn tới màn lịch | **Đã chốt: `/interviews`** (hệ thống không có trang chi tiết lịch; form đánh giá nằm ở màn danh sách). Không hiện link họp trên dashboard |
| **D8** | Director có thấy mức lương ở "Đề nghị chờ duyệt" không | Có / không | **Có**, vì Director là người duyệt lương. Định dạng `123.000` |
| **D9** | HM: "chờ lâu" có vào danh sách việc không | Có / chỉ ở KPI | **Chỉ ở KPI** (giữ rule hiện tại: HM không tự xử lý được) |
| **D10** | Director còn thấy lịch phỏng vấn của mình không (khi Director nằm trong hội đồng) | Giữ thẻ / bỏ | **Bỏ khỏi dashboard**, xem ở `/interviews`. Mockup không có |
| **D11** | Cách tính "Thiếu ứng viên" của HM | — | Còn thiếu = `NumberOfPositions` − số hồ sơ `Hired`. Gắn nhãn thiếu khi (đang phỏng vấn + đã có đề nghị) < còn thiếu |
| **D12** | Tỷ lệ nhận đề nghị | — | `Accepted / (Accepted + Declined)` của các đề nghị có `UpdatedAt` trong 30 ngày. Không có đề nghị nào thì hiện "—" |
| **D14** | Nhóm giai đoạn của "Hồ sơ đang chờ" (HR) | Theo mockup (chờ sàng lọc / lên lịch / lập đề nghị / ứng viên trả lời) / theo `ApplicationStage` | **Đã chốt: `ApplicationStage`** (Ứng tuyển, Sàng lọc CV, Phỏng vấn, Offer) để số khớp tab `/applications`, bấm mở đúng tab |
| **D13** | Tuần của lịch Interviewer | Tham số `?week=yyyy-MM-dd` / chỉ tuần hiện tại | **Tham số `week`** (ngày thứ Hai). Sai định dạng thì quay về tuần hiện tại |

## 3. Các bước thực hiện

Mỗi bước làm như sau: đọc lại rule và plan, báo xung đột, chờ đồng ý, code, test, ghi `checklist.md`.

**Bước 0. Trình GV mockup và chốt D1–D13.**

**Bước 1. Phần dùng chung**
- Fragment trong `dashboard/fragments.html`:
  - `kpis`: thêm mũi tên % và tông màu.
  - `workItems`: thêm cột số ngày chờ.
  - `barChart`: thêm biến thể 2 phần (bình thường / quá hạn).
  - Mới: `interviewList`.
- Lớp nhỏ `DashboardPeriod` tính 30 ngày gần nhất và 30 ngày trước đó từ `Clock`, kèm hàm tính %. Có unit test.
- CSS thêm vào `dashboard.css` bằng token có sẵn, không viết `style` inline, không thêm mã màu hex mới.

**Bước 2. Hiring Manager (cập nhật)**
- KPI mới. Khối "Tiến độ tuyển theo vị trí" lấy bằng 1 truy vấn gộp theo yêu cầu (không N+1).
- Bỏ 2 biểu đồ cũ, đổi tiêu đề "Yêu cầu cần theo dõi".
- Sửa `HiringManagerDashboardServiceTests` và test tích hợp.

**Bước 3. HR (mới)**
- `HrDashboardResponse` + `sections/hr.html`.
- 4 truy vấn danh sách: việc cần xử lý, phỏng vấn 2 ngày tới, hồ sơ chờ theo giai đoạn (đúng/quá hạn), tin đang mở kèm số hồ sơ.
- Xu hướng theo tuần dựa trên `recentApplicationDates`.

**Bước 4. Director (mới)**
- `DirectorDashboardResponse` + `sections/director.html`.
- Truy vấn: yêu cầu chờ duyệt (kèm ngày gửi, loại, đợt), đề nghị chờ duyệt, yêu cầu mở theo phòng ban.

**Bước 5. Interviewer (mới)**
- `InterviewerDashboardResponse` + `sections/interviewer.html`.
- Truy vấn: buổi tiếp theo, phiếu chưa gửi, lịch theo tuần.
- Controller nhận `week`.

**Bước 6. Dọn dẹp**
- Xóa:
  - `DashboardChartService`
  - `RecruitmentChartResponse`
  - `RecruitmentDashboardResponse`
  - `js/pages/recruitment-dashboard.js`
  - Khung cũ trong `index.html` (đang có các `th:if role == ...`)
  - `hr-dashboard.css` nếu không còn chỗ nào dùng
  - Dòng nạp `js/vendor/chart.umd.min.js` trong `dashboard/index.html`. Giữ file vendor vì dashboard Admin vẫn dùng
- Tách `DashboardMetricsRepository` thành phần của admin và phần nội bộ.
- **Báo Lead:** cập nhật cây thư mục `dashboard` trong `ARCHITECTURE_GUIDE.md` §1. Bảng module ở dòng 59 vẫn ghi `dashboard/candidate`.

**Bước 7. Kiểm tra**
- `mvnw clean test` cho nhóm dashboard.
- Chạy trên SQL Server thật với 4 tài khoản seed.
- Kiểm tra ở bề rộng 390px: bảng cuộn trong khung riêng, lịch tuần cuộn ngang.

## 4. Rủi ro và điểm cần báo nhóm

- **File chung:** `dashboard/index.html` và `fragments.html` dùng chung với HM. Thứ tự làm: dùng chung trước, rồi từng role.
- **Truy vấn chéo module:** Offer, Interview, Application là module của người khác. Chỉ thêm truy vấn đọc vào repository của `dashboard`, không sửa repository của module khác. Nếu cần sửa thì báo chủ module.
- **`ApplicationPipelineService.list`:** HM đang dùng để lấy số theo giai đoạn. HR nên dùng cùng nguồn đó để số trên dashboard khớp với các tab của `/applications`.
- **Cần báo nhóm — hiệu năng HR dashboard:** Dashboard đã bỏ ngưỡng cắt 10.000 hồ sơ, nhưng hiện vẫn gọi `ApplicationPipelineService.list` bốn lần để dựng bốn giai đoạn. Muốn tính trong một lượt cần API đọc chung ở module Candidate, tiếp tục dùng `ApplicationProgressResolver`; không viết lại quy tắc giai đoạn bằng SQL ở dashboard.
- **Dữ liệu seed:** cần có đủ hồ sơ quá 3 ngày, đề nghị chờ duyệt và lịch trong tuần để demo. Thiếu thì bổ sung vào `database/seeds` (báo nhóm).

## 5. Checklist hoàn thành

- [ ] GV duyệt mockup (D1 — đã chốt code song song); [x] chốt D2–D14
- [x] Bước 1: phần dùng chung + test
- [x] Bước 2: HM
- [x] Bước 3: HR
- [x] Bước 4: Director
- [x] Bước 5: Interviewer
- [x] Bước 6: dọn code cũ · [ ] báo Lead sửa AG §1
- [x] Bước 7a: `clean test` toàn bộ 1935 test, 0 lỗi (28/10/2026); 4 role render trên SQL Server qua integration test
- [ ] Bước 7b: mở trình duyệt xem thật 4 role, kiểm tra bề rộng 390px

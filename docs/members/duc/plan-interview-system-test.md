# Plan cho Gemini: System Test luồng Interview (Playwright + Excel theo mẫu Report 5.3)

> Người giao việc: Đức (ducnm), owner module Interview. Người thực hiện: Gemini Flash (high).
> Đây là bước KẾ TIẾP của Integration Test đã làm trong `integration-test/` (62 TC: 58 Passed, 1 Failed, 3 Pending). Tái dùng tối đa khung đó, nhưng System Test là loại kiểm thử KHÁC, đọc §1 trước.
> Đọc hết file này TRƯỚC khi gõ lệnh đầu tiên. Làm đúng thứ tự phase. Không chắc thì ghi vào danh sách câu hỏi cho Đức, không đoán.

## 0. Luật cứng

1. **Không commit, không push, không tạo PR, không đổi nhánh.** Làm trên nhánh đang đứng (`main`); mọi thứ nằm trong thư mục mới `system-test/` (untracked). Báo cáo xong để Đức tự quyết.
2. **Giữ `system-test/` ngoài git mà KHÔNG sửa `.gitignore`**: chạy một lần `echo "system-test/" >> .git/info/exclude` (file local, không bị commit). Kiểm bằng `git status --short` không thấy `system-test/`.
3. **Không sửa mã nguồn ứng dụng** (`src/**`), không sửa `database/**`, không sửa `integration-test/**` (chỉ đọc/import). Phát hiện lỗi ứng dụng thì GHI vào `findings.md`, không sửa giùm.
4. **Chỉ dùng DB dev local `RitirementManagement2`.** Dữ liệu tự tạo phải gắn thẻ `st.` (ví dụ tên/ghi chú bắt đầu bằng `st.`) và script dọn CHỈ xóa dữ liệu có thẻ này. Không `DELETE`/`UPDATE` không điều kiện, không `TRUNCATE`, không đụng dữ liệu của người khác.
5. **Không gửi email thật.** Cấu hình mail của app phải trỏ vào SMTP sink cục bộ (§4.3). Không sửa `application-local.properties` (file bí mật của Đức): ép bằng biến môi trường/tham số khi khởi động app.
6. **Không ghi mật khẩu, token, email thật vào bất kỳ file nào** (README, findings, Excel, log). Đọc cấu hình DB từ `application-local.properties` như `integration-test/config.py` đã làm.
7. **Không chạy cả bộ Maven test.** Plan này không cần Maven test. Nếu phải khởi động app, dùng đúng cách đã ghi trong `integration-test/output/env.txt` và `app.log`.
8. **Kỳ vọng (Expected) lấy từ SRS/SDS và quy tắc nghiệp vụ, KHÔNG lấy từ việc đọc code rồi viết cho khớp.** Code chỉ dùng để tìm selector/URL.
9. Dừng và ghi vào danh sách câu hỏi nếu: DB không kết nối được, app không lên, mẫu Excel không mở được, hoặc thấy trạng thái git lạ (merge/rebase dở).

## 1. System Test khác Integration Test ở đâu (đọc kỹ, đây là chỗ dễ làm sai)

Playwright chỉ là công cụ điều khiển trình duyệt; nó dùng được cho cả integration lẫn system test, nên hạ tầng (đăng nhập, chụp ảnh, seed, báo cáo Excel) tái dùng được khoảng 60–70%. Phần KHÓ nằm ở thiết kế ca kiểm thử, không nằm ở công cụ:

| | Integration Test (đã làm) | System Test (phải làm) |
|---|---|---|
| Phạm vi | Module Interview và chỗ nó nối với DB/module bên cạnh | Cả hệ thống chạy thật, nhìn như người dùng cuối (black-box) |
| Một TC là | Một chức năng/quy tắc nhỏ (vd: lịch trùng giờ báo lỗi) | Một **hành trình** xuyên nhiều màn hình, nhiều vai trò, nhiều module; hoặc một đặc tính phi chức năng |
| Dữ liệu đầu vào | Seed thẳng vào DB cho từng TC là chính | Đi bằng **giao diện** càng nhiều càng tốt; chỉ seed dữ liệu nền (tài khoản, ứng viên ở trạng thái xuất phát) |
| Kiểm cái gì | Đúng/sai chức năng, thông báo lỗi, quyền cơ bản | Luồng nghiệp vụ end-to-end, đồng bộ dữ liệu giữa màn hình, phân quyền toàn hệ thống, email/thông báo, responsive, trình duyệt, hiệu năng nhẹ, an toàn cơ bản |
| Nguồn Expected | SRS use case + business rule | SRS luồng nghiệp vụ tổng thể + yêu cầu phi chức năng + business rule |

Quy tắc chống trùng: **một TC system hợp lệ chỉ khi nó (a) đi xuyên ≥ 2 module HOẶC ≥ 2 vai trò, HOẶC (b) kiểm một đặc tính phi chức năng.** Kiểm validation từng ô của form, nút bấm lẻ, thông báo lỗi đơn lẻ là việc của Integration Test (IS/IE/IR đã có), KHÔNG lặp lại ở đây. Muốn tham chiếu thì ghi "đã phủ bởi IS3" trong cột Note.

## 2. Đầu vào (đọc trước khi làm)

| Thứ | Vị trí | Dùng để |
|---|---|---|
| Khung integration đã chạy | `integration-test/` (`README.md`, `config.py`, `db.py`, `runner.py`, `report_builder.py`, `verify_excel.py`, `framework/`, `seeds/`, `testcases/`) | Tái dùng; đọc `framework/auth.py`, `pages.py`, `evidence.py`, `result.py`, `seeds/fixtures.py` trước |
| Kết quả và bài học integration | `integration-test/output/findings.md`, `testcases_summary.md`, `env.txt`, `srs.txt` | `srs.txt` là SRS đã trích sẵn thành chữ. `env.txt` ghi cách dựng môi trường |
| Plan integration (bài học, bẫy UI, phân loại lỗi) | `docs/members/duc/plan-interview-integration-test.md` (đặc biệt §8 bẫy UI, §9 phân loại lỗi, §11 xung đột SRS↔code D1–D6) | Làm theo cùng quy ước |
| **Mẫu Excel System Test** | `D:\Kì FA26\SWT301\Unit Testing\ABClinic_ISP490_Report5.3_System Test.xlsx` | Khuôn báo cáo (xem §3) |
| Mẫu đã điền hoàn chỉnh (tham khảo cách viết TC) | `...\7692_APHL_ISP490_G1_Report5.2_Integration_Test_Report.xlsx` | Văn phong, định dạng |
| SRS/SDS (nguồn chân lý) | `RMS_SRS_Claude_clean_screenlist.docx`, `RMS_SDS_My_System_Claude_clean.docx` (SDS chưa cập nhật sequence/state diagram, xem Visual Paradigm nếu cần) | Expected. Tìm đường dẫn thật qua `integration-test/output/` hoặc hỏi Đức |
| Kế hoạch thay đổi nghiệp vụ liên quan | `docs/members/duc/plan-interview-ui-adw-and-director-finalize.md` §3 | Giám đốc chốt khi tuyển HM: quyết định có thể chưa có (xem SE7) |

## 3. Mẫu Excel System Test (Report 5.3): cấu trúc và lỗi của mẫu cần tránh

Workbook gồm: `Cover` (thông tin dự án, Record of change), `Test Cases` (danh sách sheet: No, Function Name, Sheet Name, Description, Pre-Condition; ô D5 mô tả môi trường), `Test Statistics` (mỗi module một dòng, Sub total, Test coverage), rồi **mỗi sheet module** theo đúng bố cục sheet integration 7692: A2:B3 Feature/Test requirement, B4 Number of TCs, bảng Round 1–4 (A5:E9), hàng tiêu đề dòng 11 (`Test Case ID | Description | Procedure | Expected Results | Pre-conditions | Evidence | Round 1 | Test date | Tester | Round 2... | Note`), dòng nhóm in nghiêng, danh sách thả xuống ở `W2:W5` (Passed/Failed/Pending/N/A).

Mẫu có lỗi, KHÔNG sao chép:
1. `Test Statistics`: nhãn `SureyFlow` gõ sai; công thức coverage `=(D16+E16)*100/(H16-G16)` chia 0 khi mọi TC là N/A → bọc `IFERROR`.
2. Bảng Round trong sheet module: dải COUNTIF không đều (`$H11:$H995`, `$N11:$N992`, `$Q12:$Q993`) và ô N/A của Round 2 tham chiếu nhầm cột `$H` thay vì `$K`. Viết lại: mỗi Round dùng dải cố định cùng kích thước từ dòng 12 đến dòng cuối thật + 200 dòng đệm, đúng cột (H, K, N, Q), đủ 4 ô Passed/Failed/Pending/N/A.
3. `Number of TCs` đếm bằng `COUNTIF(A13:A834,"*<AT*")` gắn cứng tiền tố: đặt đúng tiền tố từng sheet (`*<SE*`, `*<SA*`, ...).
4. ID dòng bằng công thức `="<PM" & ROW(A13)-11 & ">"` rất dễ vỡ khi chèn dòng → ghi **chữ cố định** (`<SE1>`, `<SE2>`...).
5. Cột lệch: có sheet mẫu (TicketFlow) để Test date vào cột Round, tên Tester vào cột Test date. Giữ đúng: H=Round 1, I=Test date, J=Tester, K..M=Round 2, N..P=Round 3, Q..S=Round 4, T=Note.
6. Chữ lỗi trong Cover: "intergration" → "system test". `Document Code` dạng `RMS_TestReport5.3_v1.0`.
7. `Test Cases` sheet liệt kê sheet Treatment... có sheet không có TC: chỉ liệt kê sheet thực sự có.

Kiểu chữ/màu giữ như mẫu 7692 mà integration đã dùng (Tahoma 10, header fill 76923C, dòng nhóm CCFFFF). Đọc `integration-test/report_builder.py` để biết nó đã xử lý ra sao; đừng viết lại từ đầu.

## 4. Môi trường và hạ tầng cần thêm

### 4.1 Cấu trúc thư mục `system-test/`

```
system-test/
  README.md  requirements.txt  config.py  runner.py  report_builder.py  verify_excel.py
  framework/   auth.py pages.py evidence.py result.py roles.py timeshift.py mailsink.py perf.py
  seeds/       fixtures.py 00_cleanup.sql
  testcases/   e2e.py access.py notification.py nonfunc.py
  evidence/  output/   (output: results.json, findings.md, testcases_summary.md, RMS_Interview_Report5.3_System_Test.xlsx)
```
Tái dùng bằng cách chép/điều chỉnh từ `integration-test/` (thư mục có dấu gạch ngang nên không `import` trực tiếp được; chèn `sys.path` hoặc chép hàm cần thiết). Nếu cùng một đoạn code xuất hiện ở cả hai nơi thì ưu tiên chép nhỏ gọn hơn là refactor `integration-test/`.

### 4.2 Khởi động app
Làm đúng như `integration-test/output/env.txt`. Cổng 8080, profile `local`, DB `RitirementManagement2`. Nếu app đã chạy sẵn thì dùng lại, đừng khởi động hai bản. Ghi log vào `system-test/output/app.log`.

### 4.3 SMTP sink (để kiểm email thật sự được gửi và nội dung đúng)
- Cài `pip install aiosmtpd` (chưa có sẵn). `framework/mailsink.py` chạy một SMTP server cục bộ tại `127.0.0.1:2525`, lưu mỗi thư (người nhận, tiêu đề, nội dung) vào danh sách để TC truy vấn theo email người nhận.
- Khởi động app với mail trỏ vào sink bằng biến môi trường (KHÔNG sửa file cấu hình): `SPRING_MAIL_HOST=127.0.0.1`, `SPRING_MAIL_PORT=2525`, và tắt xác thực/TLS của mail nếu app bật (`SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=false`, `..._STARTTLS_ENABLE=false`). Nếu app đã chạy sẵn với cấu hình mail thật thì **dừng**, hỏi Đức; không chạy các TC email.
- Nếu sink không dùng được (vd app gửi mail bất đồng bộ sau độ trễ): chờ tối đa 15 giây có thăm dò, không `sleep` cứng. Vẫn hỏng thì TC thông báo email đánh `Pending` kèm lý do.

### 4.4 Dịch chuyển thời gian (time-shift)
Nhiều bước chỉ xảy ra sau khi buổi phỏng vấn "đã diễn ra" (đánh giá, chốt kết quả, quá hạn 24h/7 ngày). Không đổi đồng hồ máy. `framework/timeshift.py` đổi `InterviewSchedule.StartTime/EndTime` (và mốc `ApprovedAt/ConsideredAt` khi cần) của **đúng lịch có thẻ `st.`** bằng `sqlcmd`, tính tương đối theo `GETDATE()`. Mọi lần dùng phải ghi vào Procedure của TC ("[Chuẩn bị bằng DB: lùi giờ buổi phỏng vấn về 2 giờ trước]") để minh bạch rằng bước đó không đi bằng UI.

### 4.5 Nhiều vai trò trong một TC
Mỗi vai trò một `browser.new_context()` riêng (cookie độc lập); `framework/roles.py` ánh xạ tên vai trò → tài khoản dev có sẵn và đăng nhập (xem `framework/auth.py`). Các vai trò dùng: System Admin, HR, Hiring Manager (đúng phòng ban), Hiring Manager (phòng khác), Director, Interviewer (được phân công), Interviewer (không phân công), Candidate (của hồ sơ), Candidate (người khác), ẩn danh. Lấy username từ DB/seed hiện có (`integration-test/seeds/fixtures.py`, `database/seeds/`); mật khẩu dev lấy từ cấu hình local, không ghi vào tài liệu.

### 4.6 Dữ liệu
- **Dữ liệu nền**: tài khoản đã có sẵn trong seed. Mỗi TC E2E cần một hồ sơ ứng viên ở trạng thái xuất phát (thường `HM_Passed`, đã qua HM duyệt, chờ lên lịch phỏng vấn) và các interviewer rảnh: `fixtures.py` tạo mới, gắn thẻ `st.`, trả về id. Mỗi TC dùng bộ dữ liệu RIÊNG, không dùng chung, để chạy lại độc lập.
- **Mọi bước nghiệp vụ còn lại đi bằng UI** (tạo lịch, đánh giá, chốt kết quả, xem phía ứng viên...). Chỉ dùng DB ở: tạo dữ liệu nền, time-shift (§4.4), và **đọc** DB để xác nhận trạng thái hồ sơ cuối luồng.
- Thời điểm tương đối `GETDATE()`, không hard-code ngày (tránh trường hợp chạy lại vào ngày khác thì hỏng).

## 5. Danh sách TC dự kiến (khung; Gemini hoàn thiện Procedure/Expected chi tiết theo §6)

Tổng khoảng 37 TC trên 4 sheet. ID là chữ cố định. `Nguồn` chỉ là gợi ý, Gemini phải đối chiếu SRS thật trong `srs.txt` và ghi chính xác UC/GBR. Chỗ nào SRS không nói thì tra §11 của plan integration; vẫn không rõ → ghi vào câu hỏi cho Đức và đánh `Pending`.

### 5.1 Sheet `InterviewE2E` (tiền tố `SE`): hành trình xuyên module và vai trò
| ID | Hành trình | Điểm kiểm (checkpoint) chính |
|---|---|---|
| SE1 | Luồng thành công: HR lên lịch → ứng viên thấy lịch → các interviewer đánh giá → HM chốt **Đạt** → HR đủ điều kiện lập Offer | trạng thái lịch Scheduled→Completed; hồ sơ giữ `Interviewing`; ứng viên có trong danh sách chờ Offer (GBR-07); dữ liệu thống nhất giữa danh sách, trang kết quả, trang ứng viên |
| SE2 | Luồng không đạt: HM chốt **Không đạt** | hồ sơ → Rejected; ứng viên KHÔNG ở danh sách chờ Offer; ứng viên thấy trạng thái cuối cùng đúng |
| SE3 | Luồng Cân nhắc rồi chốt lại | Cân nhắc giữ `Interviewing`, huy hiệu "Đang cân nhắc"; chốt lại Đạt giữ mốc `ConsideredAt`; cảnh báo quá 7 ngày (time-shift) vẫn cho chốt |
| SE4 | Đổi lịch (Reschedule) giữa chừng | trạng thái Rescheduled; ứng viên và interviewer thấy giờ mới, giờ cũ không còn; đánh giá chỉ mở theo giờ mới |
| SE5 | Hủy lịch có lý do | trạng thái Cancelled; không thể đánh giá/chốt; hồ sơ ứng viên quay về đúng trạng thái nghiệp vụ; ứng viên thấy hủy |
| SE6 | Thiếu phiếu: trong 24h không chốt được, sau 24h chốt được kèm cảnh báo | time-shift 2 mốc; thông báo chặn/cảnh báo đúng quy tắc |
| SE7 | Giám đốc chốt khi tuyển Hiring Manager | **Chỉ thiết kế đầy đủ nếu Đức đã chốt phương án** trong plan `...director-finalize.md` §3.3. Chưa chốt → viết TC, đánh `Pending`, Note "Chờ quyết định nghiệp vụ", không chạy |
| SE8 | Xung đột lịch: cùng interviewer/ứng viên bị đặt trùng giờ ở hai lịch | hệ thống chặn đúng quy tắc SRS, lịch thứ nhất không bị hỏng |
| SE9 | Nhất quán số liệu: số buổi sắp tới/cần đánh giá ở dashboard của HM/Interviewer khớp danh sách thật | so sánh con số dashboard với đếm trên `/interviews` |

### 5.2 Sheet `InterviewAccess` (tiền tố `SA`): phân quyền toàn hệ thống
Ma trận vai trò × tài nguyên. Mỗi TC một nhóm tài nguyên, kiểm TẤT CẢ vai trò trong cùng TC (Procedure ghi từng vai trò, Expected ghi kết quả mong đợi từng vai trò: vào được/403/redirect đăng nhập). Nhóm: danh sách lịch (`/interviews`), form tạo/sửa lịch, trang kết quả `/interviews/{id}/result`, gửi phiếu đánh giá (POST), chốt kết quả (POST `/interviews/{id}/final-result`), trang lịch của ứng viên, thao tác trên lịch của người khác/phòng khác (IDOR: đổi `{id}` trong URL), truy cập khi chưa đăng nhập, và truy cập sau khi phiên hết hạn. Khoảng 12 TC: `SA1`–`SA12`. Kết quả phải khớp quy tắc phân quyền trong SRS (đặc biệt GBR-05: Interviewer chỉ thấy lịch được phân công).

### 5.3 Sheet `InterviewNotification` (tiền tố `SN`): email và thông báo
Dùng SMTP sink §4.3. Khoảng 6 TC: email khi tạo lịch (gửi ứng viên + interviewer, nội dung có giờ/địa điểm/liên kết đúng), email khi đổi lịch, email khi hủy lịch, thông báo trong ứng dụng cho interviewer được phân công, "Gửi lại email" của HR chỉ hiện đúng điều kiện, không gửi email cho người không liên quan. Expected về nội dung email lấy từ SRS (mẫu MSG nếu có); chỗ SRS không nêu thì kiểm các thông tin bắt buộc (giờ, vị trí, tên) và ghi rõ là yêu cầu suy ra.

### 5.4 Sheet `InterviewNonFunc` (tiền tố `SF`): phi chức năng
Khoảng 10 TC, mỗi TC chạy trên trang Interview chính (danh sách, form, kết quả):
| ID | Nội dung | Cách đo/kiểm |
|---|---|---|
| SF1 | Responsive 375px | không cuộn ngang, nút/hành động vẫn bấm được, chụp ảnh |
| SF2 | Responsive 768px | bố cục không vỡ |
| SF3 | Responsive 1440px | bố cục đúng, không khoảng trống bất thường |
| SF4 | Trình duyệt Firefox | luồng chính (xem danh sách → mở kết quả) chạy được; ghi ảnh |
| SF5 | Trình duyệt WebKit (Safari engine) | như SF4 |
| SF6 | Hiệu năng nhẹ: tải `/interviews` với ≥ 100 lịch có thẻ `st.` | đo thời gian tải 10 lần, báo trung vị và p95; ngưỡng mặc định ≤ 3 giây (xin Đức xác nhận ngưỡng, nếu SRS có yêu cầu hiệu năng thì dùng số đó) |
| SF7 | Bàn phím và hộp thoại | mở hộp thoại Đánh giá/Chốt kết quả bằng phím, Tab giữ trong hộp thoại, Esc đóng, focus trả về nút mở |
| SF8 | Chống XSS ở ô nhận xét | nhập `<script>`/`<img onerror>` vào nhận xét, xem lại ở trang kết quả: phải hiển thị như chữ, không chạy mã |
| SF9 | Bấm đúp nút gửi | chỉ tạo MỘT phiếu đánh giá/MỘT kết quả chốt/MỘT lịch (đếm trong DB) |
| SF10 | Hết phiên khi đang thao tác | thao tác sau khi mất phiên → về trang đăng nhập, không mất dữ liệu của người khác, không lỗi 500 |

Không thêm kiểm thử tải nặng, quét lỗ hổng, hay dùng công cụ tấn công; chỉ làm đúng danh sách trên.

## 6. Cách viết mỗi TC (giữ quy ước integration)

- `Description` bắt đầu bằng "Verify that ...". `Procedure` và `Expected` là danh sách đánh số 1., 2., ... khớp từng bước/checkpoint; mỗi bước nói rõ **vai trò** đang thao tác.
- `Pre-conditions`: nêu dữ liệu nền và tài khoản (không nêu mật khẩu).
- Một TC E2E là MỘT hàm chạy tuần tự nhiều bước. Khi một bước hỏng: dừng TC, đánh `Failed`, `Note` ghi rõ "Hỏng ở bước N: <hiện tượng>", chụp ảnh tại bước hỏng, lưu trace (`context.tracing`) vào `output/traces/<ID>.zip`.
- Evidence: chụp ảnh tại mỗi checkpoint chính: `evidence/<ID>_01.png`, `<ID>_02.png`...; ảnh cuối (hoặc ảnh bước hỏng) được đưa vào cột Evidence theo cách `integration-test/report_builder.py` đang làm.
- Chờ bằng điều kiện (`wait_for`, `expect`), không dùng `sleep` cố định.
- Mỗi TC tự dựng và tự dọn dữ liệu `st.` của nó; chạy lại được nhiều lần cho cùng kết quả.
- Dùng selector ổn định: `id`, `data-*`, vai trò ARIA. Giữ nguyên `id` có sẵn như `btn-create-interview`. Bẫy UI của trang Interview xem plan integration §8.
- Kết quả hợp lệ: `Passed`, `Failed`, `Pending` (chưa chạy được vì thiếu điều kiện/quyết định), `N/A` (không áp dụng). Mọi TC `Pending`/`N/A` phải có lý do ở `Note`.

## 7. Phân loại lỗi và xung đột (giữ như plan integration §9)

Khi một TC không đạt, phân loại TRƯỚC khi ghi `Failed`:
- **A. Lỗi của TC/khung test** (selector sai, thiếu chờ, dữ liệu nền sai): sửa test, chạy lại, đừng ghi là lỗi ứng dụng.
- **B. Lỗi ứng dụng** (khác SRS): ghi `Failed`, thêm mục vào `findings.md` (mã TC, mức độ, màn hình, tham chiếu SRS, hiện trạng, mong đợi, khuyến nghị).
- **C/D. SRS thiếu/mơ hồ hoặc code đã đi trước SRS**: ghi vào `findings.md` mục đề xuất làm rõ SRS; TC đánh `Pending` hoặc dùng kỳ vọng đã thống nhất, nói rõ trong Note.
Khi nghi ngờ: chạy lại TC 3 lần. Kết quả không ổn định → là loại A (khung test), phải sửa chờ/đồng bộ, không được chốt "Passed" do chạy may.
Đã biết từ integration, cần xem lại trong bối cảnh E2E: `findings.md` DEFECT-01 (Interviewer thiếu đường xem CV) và DEFECT-02 (Giám đốc chưa chốt được khi tuyển HM); xung đột D1–D6 ở plan integration §11.

## 8. Các phase làm việc

| Phase | Việc | Hoàn thành khi |
|---|---|---|
| P0 | Chuẩn bị + câu hỏi cho Đức (§9). `echo "system-test/" >> .git/info/exclude`. Kiểm DB và app | `git status --short` không thấy `system-test/`; app chạy; kết nối DB OK |
| P1 | Đọc khung integration và mẫu Report 5.3 (§2, §3). Ghi nhận xét ngắn vào `system-test/output/notes.md` | liệt kê được hàm tái dùng và lỗi mẫu cần tránh |
| P2 | Dựng khung `system-test/`: config, auth nhiều vai trò, evidence, result, runner (`--list`, `--only`, `--sheet`, `--headed`). Thêm `mailsink`, `timeshift`, `perf` | `runner.py --list` in được danh sách TC giả; mailsink nhận được 1 thư thử; timeshift đổi và hoàn nguyên được một lịch `st.` |
| P3 | Hoàn thiện danh sách TC §5 thành bản chi tiết (Procedure/Expected/Pre-conditions/Nguồn) vào `output/testcases_summary.md`, đối chiếu SRS | mỗi TC có nguồn SRS hoặc ghi rõ "suy ra" |
| P4 | Cài đặt TC theo thứ tự: `InterviewE2E` → `InterviewAccess` → `InterviewNotification` → `InterviewNonFunc`. Sau mỗi sheet chạy riêng và xem ảnh evidence | từng sheet chạy xong, kết quả ổn định 3 lần liên tiếp |
| P5 | Chạy toàn bộ, phân loại lỗi (§7), viết `findings.md` | mọi `Failed` có phân loại; không còn `Failed` loại A |
| P6 | Dựng Excel Report 5.3 (`report_builder.py`) từ mẫu, theo §3; viết `verify_excel.py` kiểm: số TC khớp `results.json`, công thức Round đúng cột, không ô lỗi `#DIV/0!`/`#REF!`, ID không trùng, ảnh Evidence có thật | `verify_excel.py` in "OK"; mở được bằng Excel/LibreOffice |
| P7 | Dọn dữ liệu `st.`, kiểm `git status` sạch (không thấy `system-test/`), viết báo cáo cuối (§10) | báo cáo gửi Đức |

## 9. Câu hỏi phải hỏi Đức ở P0 (không tự bịa; nếu không có câu trả lời thì dùng mặc định và ghi rõ trong báo cáo)

1. Tên tester / người tạo (mặc định `ducnm`), người duyệt (Reviewer/Approver) trong `Test Statistics` (mặc định để trống), ngày phát hành báo cáo (mặc định ngày chạy).
2. Giám đốc chốt khi tuyển Hiring Manager: đã có quyết định phương án chưa? Chưa → SE7 `Pending`.
3. Bản ứng dụng cần test: `main` mới nhất tại thời điểm chạy (mặc định). Nếu Đức đang chờ nhánh khác merge thì chờ.
4. Ngưỡng hiệu năng SF6 (mặc định 3 giây trung vị/p95 với ≥ 100 lịch).
5. Có cho phép cài thêm thư viện Python (`aiosmtpd`) và trình duyệt Playwright Firefox/WebKit (`python -m playwright install firefox webkit`) không.

## 10. Báo cáo cuối (ngắn, gửi Đức)

1. Số TC theo sheet và theo kết quả (Passed/Failed/Pending/N/A), đường dẫn Excel và `findings.md`.
2. Danh sách `Failed` kèm phân loại B/C/D; mục nào cần Đức xử lý.
3. Danh sách `Pending` và lý do (đặc biệt SE7, email nếu sink hỏng).
4. Các TC dùng DB cho bước chuẩn bị (time-shift) và vì sao.
5. Câu hỏi §9 đã hỏi, câu trả lời, giá trị mặc định đã dùng.
6. Xác nhận: không sửa `src/**`, không commit, `system-test/` ngoài git, DB đã dọn dữ liệu `st.`.

## 11. Tiêu chí hoàn thành (checklist)

- [ ] `system-test/` có README chạy được theo từng lệnh; `runner.py --list` in đủ TC.
- [ ] ≥ 35 TC, mỗi TC hợp lệ theo quy tắc chống trùng ở §1; không lặp lại TC integration.
- [ ] Mọi bước nghiệp vụ trong E2E đi qua giao diện (trừ chuẩn bị nền và time-shift có ghi chú).
- [ ] Mỗi TC có Expected lấy từ SRS (có nguồn) hoặc ghi rõ "suy ra"; không có Expected viết theo code.
- [ ] Chạy 3 lần liên tiếp cho kết quả giống nhau trên toàn bộ TC không `Pending`.
- [ ] Excel Report 5.3 mở được, không lỗi công thức, thống kê khớp `results.json`, không dính các lỗi mẫu ở §3.
- [ ] `findings.md` đủ phân loại; không bịa lỗi ứng dụng.
- [ ] Không có file nào ngoài `system-test/` bị thay đổi; không commit; không mật khẩu trong tài liệu/log.

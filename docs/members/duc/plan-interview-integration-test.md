# Kế hoạch: Integration Test module Interview bằng Playwright → báo cáo Excel theo mẫu 7692_APHL

> Người giao việc: ducnm · Lập ngày 29/10/2026 · Người thực hiện: Gemini (chạy trong repo này)
> Trạng thái: **CHƯA LÀM**. File này là kế hoạch, chưa có code test nào được viết.
> Ngôn ngữ làm việc với người dùng: tiếng Việt. Tên file, class, selector, câu lệnh giữ nguyên tiếng Anh.

---

## 0. Mục tiêu và đầu ra

Chạy kiểm thử tích hợp (Integration Test) cho **module Interview** của hệ thống RMS bằng **Playwright (Python)** trên trình duyệt thật, rồi xuất kết quả thành **một file Excel đúng bố cục** của mẫu:

`D:\Kì FA26\SWT301\Unit Testing\7692_APHL_ISP490_G1_Report5.2_Integration_Test_Report.xlsx`

Đầu ra bắt buộc (4 thứ):

| # | Đầu ra | Vị trí |
|---|---|---|
| 1 | Danh sách test case (nguồn dữ liệu duy nhất) | `integration-test/testcases/*.py` |
| 2 | Bộ chạy Playwright + seed SQL + dọn dữ liệu | `integration-test/` |
| 3 | Ảnh bằng chứng (evidence) từng test case | `integration-test/evidence/<TC_ID>.png` |
| 4 | File báo cáo Excel mới | `integration-test/output/RMS_Interview_Report5.2_Integration_Test.xlsx` |

Ngoài ra Gemini phải tạo `integration-test/output/findings.md` (danh sách lỗi và chênh lệch SRS↔code tìm được, xem mục 9).

**Không** sửa file mẫu 7692 gốc. **Không** sửa code ứng dụng (`src/`). **Không** commit, **không** push.

---

## 1. Quy tắc bất di bất dịch (đọc trước khi làm bất cứ việc gì)

1. **Expected Results lấy từ SRS, KHÔNG lấy từ code.** Viết cột Expected Results xong rồi mới chạy test. Không bao giờ sửa Expected cho khớp kết quả chạy.
2. **Không bịa dữ liệu.** Cột Round 1 / Test date / Tester chỉ điền từ lần chạy thật. Round 2, 3, 4 để trống. Ngày test = ngày chạy thật (định dạng `yyyy-mm-dd`). Tester = giá trị `TESTER_NAME` trong `integration-test/config.py` (xem mục 4.5, phải hỏi người dùng).
3. **Test hỏng ≠ ứng dụng hỏng.** Trước khi ghi `Failed`, phải phân loại theo mục 9. Lỗi do script của Gemini thì sửa script, không ghi `Failed`.
4. **Không động vào thứ không phải của mình.** Không kill process không do Gemini khởi động. Không xóa dữ liệu không mang tag `E2E_` (xem mục 6). Không xóa file ngoài thư mục dự án.
5. **Bí mật.** `application-local.properties` chứa mật khẩu mail và DB. Không in ra màn hình, không ghi vào log, không ghi vào Excel, không đưa vào file mới. Đọc cấu hình từ file đó khi cần, đừng chép giá trị.
6. **Không gửi email thật.** Tạo lịch phỏng vấn sẽ gọi `RecruitmentEmailService`. Xem mục 4.4 để tắt đường gửi mail thật khi chạy test.
7. **Dừng và hỏi người dùng** tại các cổng đánh dấu `🛑 HỎI NGƯỜI DÙNG` (mục 10). Không tự quyết ở những chỗ đó.
8. **Không dùng `python -I`** (chế độ cô lập làm mất `openpyxl` và `playwright`). Dùng `python` thường.
9. **Làm từng bước nhỏ, mỗi bước có tiêu chí kiểm tra (gate).** Chưa qua gate thì không sang bước sau.

---

## 2. Đầu vào (đọc thật, không đoán)

| Nguồn | Đường dẫn | Dùng để làm gì |
|---|---|---|
| SRS (bản chuẩn) | `D:\Kì FA26\SWP391\RMS_SRS_Claude_clean_screenlist.docx` | Nguồn **Expected Results**. Chỉ đọc. |
| SDS | `D:\Kì FA26\SWP391\RMS_SDS_My_System_Claude_clean.docx` | Tham khảo thiết kế. SDS **chưa cập nhật** sequence/state machine. |
| State machine | `D:\Kì FA26\SWP391\RMS_Interview_State_Machine_Diagram.vpp` | Trạng thái lịch / phiếu / kết quả. File `.vpp` là zip. |
| Sequence | `D:\Kì FA26\SWP391\SWP_sequence_diagram\RMS_Interview_Sequence_Diagram.vpp` | 8 luồng tương tác. |
| Mẫu báo cáo | `D:\Kì FA26\SWT301\Unit Testing\7692_APHL_ISP490_G1_Report5.2_Integration_Test_Report.xlsx` | Bố cục Excel. Chỉ đọc. |
| Code đã chú thích để tra selector | `D:\Kì FA26\SWP391\SWP_learn\Interview_source_main\` | Mọi comment `[HỌC]` giải thích id, name, luồng. Chỉ để tra cứu. |
| Kế hoạch tính năng | `docs/members/duc/plan-interview-evaluation-consider.md` | Hạn 24h / 2 ngày / 7 ngày, kết quả "Cân nhắc". |
| Hướng dẫn demo | `docs/members/duc/interview-demo-guide.md` | Danh sách tài khoản và kịch bản. **File seed nó nhắc tới (`demo_interview_flow.sql`) KHÔNG tồn tại**, phải tự viết. |
| Schema DB | `database/schema/db.sql`, `database/migrations/010_interview_consider.sql` | Cột bắt buộc khi seed. |
| Seed gốc | `database/seeds/seed_data.sql` | Đã có user `director`, `hr_lan`, `hm_tuan`, `interviewer_nam`... |

Cách đọc `.docx`: dùng `zipfile` + regex trên `word/document.xml` (tự viết script ngắn: mở zip, đọc `word/document.xml`, thay `</w:tc>` bằng ` ; ` và `</w:tr>` bằng xuống dòng, rồi gom text của các thẻ `<w:t>`; lưu bản text ra `integration-test/output/srs.txt` để tra cứu). Cách đọc `.vpp`: giải nén zip, tìm file XML/`.vpp` chứa tên `Interview`.

### 2.1. Điều đã xác minh trong SRS bản chuẩn (số thứ tự có thể khác bản cũ, hãy tự đọc lại để chắc)

Use case (cột "UC" theo bản SRS đã đọc ngày 29/10/2026):

| UC | Tên | Actor |
|---|---|---|
| 54 | View Interview Result | Hiring Manager |
| 55 | Finalize Interview Result | Hiring Manager |
| 60 | View Schedule Interview List | HR, Director |
| 61 | View Schedule Details | HR, Director |
| 62 | Create Schedule | HR, Director |
| 63 | Update Schedule | HR, Director |
| 64 | Cancel Schedule | HR, Director |
| 65 | View Interview Schedule | User (Candidate, Interviewer) |
| 66 | View Candidate CV | Interviewer |
| 67 | Submit Interview Evaluation | Interviewer |

Quy tắc và thông báo cần dùng: `BR-INT-01`, `GBR-01`, `GBR-02`, `GBR-04`, `GBR-05`, `GBR-06`, `OR-01`, `OR-06`, `OR-07`, `OR-10`, `MSG24`–`MSG29`, `MSG12`, `MSG26`.
Màn hình: Screen Inventory mục 37–43 (Record Final Result, Interview Schedule List, Create/Update Schedule, Cancel Confirmation, My Interview Schedule, Submit Evaluation Form).

**Gemini phải mở lại SRS và chép đúng số UC, GBR, MSG vào cột Description/Expected của từng test case.** Nếu số khác bảng trên thì theo SRS.

---

## 3. Cấu trúc của mẫu 7692 (phải bắt chước đúng)

Mẫu có các sheet: `Cover`, `Test Cases`, `Test Statistics`, rồi mỗi module một sheet. Mẫu đã dump được như sau (Gemini nên tự dump lại bằng `openpyxl` để xác nhận, đặc biệt các cột R, S và AE–AF):

### 3.1. Sheet `Cover`
- `B2` (gộp `B2:F2`): `TEST REPORT DOCUMENT`.
- `A4 Project Name` / `B4` (gộp `B4:D4`) / `E4 Creator` / `F4`.
- `A5 Project Code` / `B5` / `E5 Issue Date` / `F5`.
- `A6 Document Code` / `B6` = `=B5&"_"&"TestReport5.2"&"_"&"v1.0"` / `E6 Version` / `F6`.
- `A9 Record of change`; hàng 10 là tiêu đề: `Effective Date | Version | Change Item | *A,D,M | Change description | Reference`; từ hàng 11 là các dòng thay đổi (`A` = thêm, `M` = sửa).

### 3.2. Sheet `Test Cases`
- `D1`: `TEST CASE LIST`. `B3 Project Name`, `B4 Project Code`, `B5 Test Environment Setup Description` (giá trị ở `D3`, `D4`, `D5`).
- Bảng bắt đầu hàng 8: `No | Function Name | Sheet Name | Description | Pre-Condition` (cột `B` đến `F`). Mỗi sheet module một dòng.

### 3.3. Sheet `Test Statistics`
- Hàng 9: `No | Module code | Passed | Failed | Pending | N/A | Number of test cases`.
- Mỗi module một dòng, công thức tham chiếu sheet module, ví dụ `=Authentication!B9` (hàng "Round 4" của sheet module). **Mẫu tham chiếu Round 4; báo cáo của mình chỉ có Round 1 nên phải trỏ vào hàng Round 1 (`B6:E6`).** Để tham số `STAT_ROUND_ROW = 6`.
- `Sub total` = `SUM` từng cột. `Test coverage` = `(Passed+Failed)*100/(Total-N/A)`. `Test successful coverage` = `Passed*100/(Total-N/A)`.
- **Lỗi công thức trong mẫu, KHÔNG được chép theo:** `E18 =SUM(E8:E14)` và `G18 =SUM(G8:G14)` (thiếu dòng), `D12 =ProfileManagement!B8` (sai hàng), `E7 =COUNTIF($H11:$H1002,E5)` ở sheet module (đang đếm cột Round 1 thay vì Round 2), `B9 =COUNTIF($Q12:$Q1000,...)` (range lệch). Viết lại cho nhất quán, cùng một khoảng hàng.

### 3.4. Sheet module (ví dụ `Authentication`)
- `A2 Feature` / `B2` (gộp `B2:E2`): tên chức năng.
- `A3 Test requirement` / `B3` (gộp `B3:E3`): mô tả yêu cầu, liệt kê dạng gạch đầu dòng.
- `A4 Number of TCs` / `B4` = `=COUNTIF(A13:A841,"*<AT*")` (đếm số ô ID chứa tiền tố mã module).
- `A5 Testing Round` / `B5:E5` = `Passed | Failed | Pending | N/A`.
- Hàng 6–9 = `Round 1`…`Round 4`: `=COUNTIF($H$13:$H$1002,B$5)` cho Round 1, cột `K` cho Round 2, `N` cho Round 3, `Q` cho Round 4.
- Hàng 11 = tiêu đề bảng: `A Test Case ID | B Test Case Description | C Test Case Procedure | D Expected Results | E Pre-conditions | F Evidence (gộp F:G) | H Round 1 | I Test date | J Tester | K Round 2 | L Test date | M Tester | N Round 3 | O | P | Q Round 4 | R | S`.
- Hàng 12 = tên nhóm chức năng (ví dụ `Login`), tô nền `CCFFFF`, chữ đậm, chỉ có ô `A`.
- Từ hàng 13 = test case. Cột `A` = `<AT1>`, `<AT2>`... (tiền tố riêng từng module). Mỗi dòng test case nằm xen với dòng nhóm.
- Cột `F:G` gộp từng dòng test case, chứa **ảnh bằng chứng** (đặt ảnh neo tại `F<hàng>`).
- Dropdown (data validation) cho các cột trạng thái `H, K, N, Q`: danh sách lấy từ `$W$2:$W$5` = `Passed, Failed, Pending, N/A`. Nên đặt danh sách này ở cột `W` của mỗi sheet module, rồi gán validation cho đúng các ô test case.
- Định dạng: font **Tahoma 10**, tiêu đề bảng nền `76923C` chữ đậm, canh giữa; ô test case `wrap_text=True`, canh trên (`vertical=top`); chiều cao dòng test case lớn (mẫu 216 pt).
- Quy ước ID trong mẫu dùng công thức `="<AT" & ROW(..)-11 & ">"` nhưng công thức này **sai số khi có dòng nhóm xen vào**. Mình dùng **chuỗi cố định** `<IS1>`, `<IS2>`... để ID không đổi.

### 3.5. Cách tạo file Excel (quyết định kỹ thuật)
Tạo workbook **mới** bằng `openpyxl` và tự áp style như trên (không sao chép file mẫu 18 MB). Phải mở được trong Excel mà không báo lỗi. Nhờ vậy file nhẹ và không mang rác của module khác.

---

## 4. Môi trường và kiểm tra tiền đề (Phase 0)

### 4.1. Phần mềm
Chạy các lệnh kiểm tra và ghi kết quả vào `integration-test/output/env.txt`:

```
python --version
python -c "import openpyxl, playwright; print(openpyxl.__version__)"
python -m playwright --version
sqlcmd -?          # cần có sqlcmd; KHÔNG dùng pyodbc (máy chưa cài)
java -version      # 21
```
Nếu thiếu `playwright` Python: `python -m pip install playwright` rồi `python -m playwright install chromium`. Nếu thiếu `openpyxl`: `python -m pip install openpyxl`. Không cài thêm gói nào khác nếu không cần.

### 4.2. Cơ sở dữ liệu
- SQL Server `localhost:1433`, database `RitirementManagement2`, user và mật khẩu lấy từ `src/main/resources/application-local.properties` (đừng in ra).
- Chạy thử: `sqlcmd -S localhost -U sa -P <đọc từ file> -C -d RitirementManagement2 -Q "SELECT COUNT(*) FROM [User]"`.
- Xác nhận migration `010` đã áp dụng: có cột `InterviewFinalResult.ConsideredAt` và ràng buộc `UQ_InterviewEvaluation_Interview_Interviewer`. Nếu chưa, **dừng và hỏi người dùng** (không tự chạy migration).

### 4.3. Khởi động ứng dụng
- Cổng thật của app là **8080** (không phải 8082 như `playwright.config.js` của repo JS). `BASE_URL` mặc định cho bộ test mới: `http://localhost:8080`.
- Kiểm tra app đã chạy chưa: `GET http://localhost:8080/login` trả 200. Nếu chạy rồi thì **dùng luôn**, không tắt.
- Nếu chưa chạy: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` ở chế độ nền, ghi log ra `integration-test/output/app.log`, chờ tối đa 180 giây tới khi `/login` trả 200. Tự tắt process này khi xong (chỉ process do mình bật).
- Ứng dụng phải chạy trên branch/commit nào thì ghi vào Cover: `git rev-parse --short HEAD` và `git branch --show-current`.

### 4.4. Chặn email thật
Khi tự khởi động app, thêm tham số để mail trỏ vào cổng không có ai nghe, ví dụ:
`-Dspring-boot.run.arguments="--spring.mail.host=127.0.0.1 --spring.mail.port=2525"`.
Hệ quả mong đợi: tạo lịch vẫn thành công, nhưng hiện cảnh báo gửi mail thất bại (SRS `OR-10`: lỗi mail không được làm hỏng giao dịch chính). **Đó chính là một test case** (`IS18`, mục 12). Nếu app đã được người dùng bật sẵn với mail thật thì **dừng và hỏi** trước khi tạo lịch, và chỉ dùng email ứng viên dạng `*@example.invalid`.

### 4.5. 🛑 Câu hỏi bắt buộc hỏi người dùng ở Phase 0 (hỏi một lần, gom lại)
1. `TESTER_NAME` điền gì vào cột Tester? (gợi ý: `ducnm`).
2. `Creator` và `Issue Date` ở Cover điền gì?
3. Chạy trên DB local hiện tại có sao không (dữ liệu test mang tag `E2E_` sẽ được tạo và xóa)?
4. Các test case bị mâu thuẫn SRS↔code (mục 11) xử lý thế nào? (đề xuất mặc định ở mục 11.)

---

## 5. Kiến trúc thư mục (Phase 1)

Tạo mới, không sửa `e2e/` hay `playwright.config.js` của bộ JS cũ:

```
integration-test/
├─ README.md                  # cách chạy, 20 dòng
├─ config.py                  # BASE_URL, DB, TESTER_NAME, đường dẫn, STAT_ROUND_ROW
├─ requirements.txt           # playwright, openpyxl
├─ db.py                      # chạy sqlcmd qua subprocess, trả list dòng
├─ seeds/
│  ├─ 00_cleanup.sql          # xóa dữ liệu tag E2E_
│  └─ fixtures.py             # hàm tạo từng fixture (mục 7) bằng SQL sinh động
├─ framework/
│  ├─ auth.py                 # đăng nhập từng role, lưu storage_state
│  ├─ pages.py                # Page Object: ListPage, FormPage, ResultPage
│  ├─ evidence.py             # chụp ảnh, thu nhỏ, đặt tên <TC_ID>.png
│  └─ result.py               # dataclass TcResult(status, note, started_at, screenshot)
├─ testcases/
│  ├─ schedule.py             # mã <IS..>
│  ├─ evaluation.py           # mã <IE..>
│  └─ result.py               # mã <IR..>
├─ runner.py                  # chạy tất cả hoặc một TC: python runner.py --only IS3
├─ report_builder.py          # đọc kết quả JSON + testcases -> Excel
├─ evidence/                  # ảnh (không commit)
└─ output/                    # xlsx, findings.md, results.json, app.log (không commit)
```
Thêm `integration-test/evidence/`, `integration-test/output/` và `__pycache__/` vào `.gitignore` của repo (chỉ thêm 3 dòng, không sửa gì khác).

**Gate P1:** `python integration-test/runner.py --list` chạy không lỗi (in danh sách rỗng cũng được) và `db.py` đếm được số dòng bảng `[User]`.

---

## 6. Dữ liệu test và dọn dẹp (Phase 2)

Nguyên tắc: **mỗi test case tự dựng dữ liệu riêng bằng fixture, test xong dọn ngay.** Không phụ thuộc test khác chạy trước (tránh dây chuyền lỗi).

- Mọi dòng seed mang dấu hiệu nhận biết:
  - Ứng viên: `Username` bắt đầu `e2e.`, email `@example.invalid`.
  - Hồ sơ/ lịch gắn vào các ứng viên đó. Vì bảng `InterviewSchedule` không có cột ghi chú, **nhận diện bằng ApplicationId của ứng viên `e2e.*`**.
- `00_cleanup.sql` xóa theo thứ tự khóa ngoại: `InterviewFinalResult` → `InterviewEvaluation` → `InterviewPanel` → `InterviewSchedule` → `Application` → `Candidate` → `[User]` (chỉ `e2e.*`). Chạy trước toàn bộ và sau toàn bộ, và `try/finally` sau từng TC.
- **Thời gian trong seed tính bằng `DATEADD(...)` từ `GETDATE()`**, không dùng ngày cố định, để các trạng thái "đã diễn ra / sắp diễn ra / quá hạn" luôn đúng khi chạy vào bất kỳ ngày nào. Ghi mốc đã dùng vào ghi chú Excel (ví dụ "lịch kết thúc 25 giờ trước lúc chạy").
- Tài khoản nội bộ **dùng lại** từ `seed_data.sql` (mật khẩu `12345678`, hash BCrypt có sẵn): `director`, `hr_lan`, `hm_tuan` (Hiring Manager, phòng Engineering), `interviewer_nam`, `hm_huong` (phòng Sales). Các tài khoản `hr_quang`, `interviewer_viet`, `interviewer_thao` có trong demo guide nhưng **chưa chắc có trong DB**: truy vấn `SELECT Username, RoleId, DepartmentId, AccountStatus FROM [User]` rồi tạo bổ sung (mang tag `e2e.` nếu cần, dùng cùng `PasswordHash` của `hr_lan`).
- Cột bắt buộc (NOT NULL) của `Candidate`, `Application`, `InterviewSchedule`, `InterviewPanel`, `InterviewEvaluation`, `InterviewFinalResult`: đọc `database/schema/db.sql`, không đoán. `InterviewId` là `INT` IDENTITY; lấy id bằng `SCOPE_IDENTITY()` hoặc `OUTPUT`.
- Tin tuyển dụng: dùng tin đã có "Senior Java Backend Engineer" (REQ-0001, phòng Engineering, quản lý bởi `hm_tuan`). Xác nhận bằng SQL trước khi dùng.
- Hồ sơ phải ở trạng thái đủ điều kiện xếp lịch (`HM_Passed`, tin đang mở). Để biết điều kiện chính xác, đọc `ApplicationRepository.findEligibleForInterview` (file nằm ngoài module Interview, tra trong repo).

**Gate P2:** chạy `fixtures.py` một fixture mẫu → SQL trả đúng số dòng → chạy cleanup → số dòng `e2e.*` về 0.

---

## 7. Danh sách fixture (mỗi TC khai báo fixture nó cần)

| Mã | Mô tả | Ứng viên | Lịch | Hội đồng | Phiếu | Kết quả |
|---|---|---|---|---|---|---|
| `F_NO_SCHEDULE` | Hồ sơ đủ điều kiện, chưa có lịch | e2e.a | không | | | |
| `F_UPCOMING` | Lịch ngày mai, Scheduled | e2e.b | +1 ngày, 1 giờ | nam, tuan (HM), quang (HR) | 0 | |
| `F_STARTED` | Buổi đang diễn ra (bắt đầu 2 giờ trước, chưa kết thúc), vẫn Scheduled | e2e.c | start now-2h, end now+1h | nam, tuan, quang | 0 | |
| `F_ENDED_PARTIAL` | Đã kết thúc 2 giờ trước, Completed, 1/3 phiếu | e2e.d | | nam, tuan, quang | 1 (nam) | |
| `F_ENDED_FULL` | Đã kết thúc, đủ phiếu | e2e.e | | | đủ | |
| `F_AFTER_24H` | Đã kết thúc 25 giờ trước, 0 phiếu | e2e.f | | | 0 | |
| `F_OVERDUE_DECISION` | Đã kết thúc 80 giờ trước, chưa chốt | e2e.g | | | đủ | |
| `F_PASSED` | Đã chốt Passed | e2e.h | | | đủ | Passed |
| `F_FAILED` | Đã chốt Failed | e2e.i | | | đủ | Failed |
| `F_CONSIDERING` | Đang Cân nhắc (3 ngày) | e2e.j | | | đủ | Considering |
| `F_CONSIDER_OVERDUE` | Cân nhắc quá 7 ngày (8 ngày) | e2e.k | | | đủ | Considering |
| `F_CANCELLED` | Lịch đã hủy | e2e.l | | | | |
| `F_OVERLAP` | Hai lịch giao nhau (cùng người phỏng vấn) | e2e.m, e2e.n | | | | |

Ghi chú `F_STARTED`: `StartTime = now-2h`, `EndTime = now+1h` để buổi đang diễn ra (đã tới giờ nên chấm điểm được). `F_ENDED_*`: `EndTime = now-2h`. Một CHECK constraint ép `EndTime > StartTime`.

---

## 8. Cách viết một test case (Phase 3)

Mỗi test case là một đối tượng Python có các trường sau (tất cả bắt buộc):

```python
TestCase(
    id="IS3",                      # khớp cột A trong Excel, ví dụ <IS3>
    group="View Schedule List",    # tên nhóm (dòng nền CCFFFF)
    description="Verify that ...", # tiếng Anh, bắt đầu bằng "Verify that" như mẫu
    procedure=["1. Login as hr_lan", "2. Go to /interviews", "3. Click 'Tất cả'"],
    expected=["1. List shows card of e2e.b"],   # LẤY TỪ SRS
    preconditions=["1. hr_lan is Active", "2. Fixture F_UPCOMING exists"],
    source="SRS UC60; GBR-05",     # chỗ trích dẫn; hoặc "PLAN Q2" nếu SRS im lặng
    fixture="F_UPCOMING",
    role="hr_lan",
    kind="auto",                   # auto | manual
    tier=1,                        # 1 = làm trước, 2 = làm sau
    run=fn,                        # hàm Playwright thực hiện và kiểm tra
)
```

Quy tắc viết:
- `procedure` dùng đúng thao tác thật, kèm dữ liệu đầu vào cụ thể (như mẫu: `Input email:'...'`). Mốc thời gian ghi dạng tuyệt đối khi biết ngày chạy, hoặc "now + 25h" kèm ghi chú. **Không ghi "hôm nay/hôm qua".**
- `expected` chỉ nêu điều SRS yêu cầu. Thông báo SRS bằng tiếng Anh (`MSG24`...) còn giao diện thật bằng tiếng Việt: ghi cả hai, ví dụ `Display message per MSG24 (UI: "Tạo lịch phỏng vấn thành công!")`. Khi so sánh chữ, **so khớp ý chính hoặc phần không đổi**, không so khớp toàn câu để tránh test mong manh.
- Kiểm tra **hiệu ứng trong DB** khi SRS yêu cầu (ví dụ trạng thái lịch đổi thành `Cancelled`) bằng một truy vấn SQL chỉ đọc, không chỉ nhìn giao diện.
- Mỗi test case chụp **một ảnh sau bước kiểm tra cuối** (toàn trang, rộng 1280). Ảnh thu nhỏ còn tối đa rộng 960 px để Excel không quá nặng.
- Mỗi test case có timeout 60 giây. Hết giờ → ghi `Pending` + ghi chú "timeout", không ghi `Failed`.
- Gộp các lỗi cùng một loại (cùng một thông báo) thành **một** test case, đừng liệt kê mọi tổ hợp (bài học của giảng viên: tránh exhaustive testing).

### 8.1. Gotcha khi thao tác giao diện (đã đọc từ code, phải tuân theo)

| Chỗ | Điều phải làm |
|---|---|
| Trang `/interviews` | Mặc định bộ lọc thời gian là **"Hôm nay"** nên hầu hết thẻ bị ẩn. Luôn bấm `#btn-filter-all` ("Tất cả") rồi mới kiểm tra thẻ. Thẻ là `article.il-card` có thuộc tính `data-id`, `data-status`, `data-result`. |
| Tạo lịch `/interviews/new` | Ô giờ là `input[type=datetime-local]`: `fill("2026-10-30T09:00")`. Chọn hội đồng bằng `select#interviewerIds` (multiple): `select_option([...])`. Nút gửi chữ "Lên lịch phỏng vấn". |
| Mở từ `?applicationId=` | Form **khóa hồ sơ** (không có `#applicationId`, có input hidden). Không khóa thì chọn qua `select#applicationId`. |
| Hủy lịch | Bấm nút `[data-cancel-url]` trên thẻ → hộp thoại `#il-cancel-dialog` mở → bấm nút `Hủy lịch` **trong dialog** (submit form `#il-cancel-form`). Nút "Không hủy" có `[data-cancel-dismiss]`. |
| Trang kết quả `/interviews/{id}/result` | Nút `[data-dialog-open="iev-dialog"]` (Đánh giá) và `[data-dialog-open="ifr-dialog"]` (Chốt kết quả). Hộp thoại là thẻ `<dialog>`: chờ `dialog[open]`. |
| Gửi phiếu / chốt kết quả | Là **AJAX**, thành công thì trang **tải lại** với `?evaluated=1` hoặc `?finalized=1`. Chờ `page.wait_for_url("**evaluated=1**")`, rồi mới kiểm tra. Lỗi hiện ở `[data-error-for="<tên ô>"]` và khung `[data-form-alert]`. |
| Phiếu đánh giá | Radio `technicalScore`, `softSkillScore`, `culturalFitScore` (giá trị `""` = Bỏ qua, `1`–`5`), `select#iev-recommendation` (`Hire`/`Consider`/`No_Hire`), textarea `strengths`, `weaknesses`, `comments`. |
| Chốt kết quả | Radio `finalDecision` (`Passed`/`Failed`/`Considering`), `input#ifr-salary`, `textarea#ifr-summary`. |
| CSRF | Form SSR và AJAX của app tự gửi token. Test thao tác qua giao diện nên không cần xử lý. Chỉ test gọi thẳng `page.request.post` mới cần lấy token từ `<meta name="_csrf">`. |
| Đăng nhập | Mỗi role một `browser.new_context()` riêng, đăng nhập qua `/login`, lưu `storage_state` để dùng lại. **Không** đăng nhập hai role trong cùng một context. |
| 403 | Spring Security trả trang lỗi 403. Kiểm tra bằng `response.status == 403` của lần `goto`. |
| Tài khoản bị chặn phiên | Bộ lọc `AccountSessionGuardFilter` đá user không tồn tại trong DB về `/login`. Tài khoản test phải thật sự có trong DB. |
| Giờ máy | App và DB cùng chạy trên máy này nên `now` khớp. Không cần xử lý múi giờ. |

**Selector ưu tiên:** `get_by_role` / `get_by_label` khi có nhãn; nếu không thì id/name ở bảng trên. Không dùng XPath dài, không dựa vào thứ tự phần tử.

---

## 9. Phân loại kết quả khi test không đạt (bắt buộc làm đúng)

Khi một TC không đạt, làm tuần tự và ghi quyết định vào `findings.md`:

1. **Chạy lại 1 lần** (có thể do chờ chưa đủ). Vẫn hỏng → tiếp bước 2.
2. Mở ảnh chụp + `app.log` đoạn thời điểm đó. Tự hỏi: selector đúng chưa? chờ đủ chưa? fixture đúng chưa? Dữ liệu seed đã tồn tại đúng trạng thái chưa (truy vấn SQL)?
3. Phân loại:

| Loại | Dấu hiệu | Ghi trong Excel | Việc làm |
|---|---|---|---|
| A. Lỗi script/fixture | Selector sai, seed sai, chờ thiếu | không ghi, sửa rồi chạy lại | Sửa script |
| B. Lỗi ứng dụng thật | Hành vi khác SRS, dữ liệu sai, 500 | `Failed` + Note ghi lý do ngắn | Ghi `findings.md`: bước tái hiện, kỳ vọng (trích SRS), thực tế, file/dòng code nghi ngờ |
| C. SRS im lặng, code theo kế hoạch `plan-interview-evaluation-consider.md` | Hành vi khớp plan | `Passed` (so với plan) + Note "Source: PLAN, SRS chưa nêu" | Ghi vào danh sách "SRS cần bổ sung" |
| D. SRS mâu thuẫn plan/code | Xem mục 11 | Theo quyết định của người dùng | Chờ quyết định |
| E. Không thể tự động hóa | Cần email thật, đánh giá thẩm mỹ | `Pending` + Note lý do | Liệt kê thủ công |

**Cấm:** đổi Expected để thành `Passed`; ghi `Passed` khi chưa thật sự kiểm tra; tắt assert; xóa TC khó.

---

## 10. Các giai đoạn thực hiện (làm tuần tự, mỗi giai đoạn có cổng)

| Phase | Việc | Cổng (gate) để qua |
|---|---|---|
| P0 | Mục 4: kiểm tra môi trường, DB, app; 🛑 hỏi 4 câu ở mục 4.5 | Có `env.txt`; `/login` = 200; người dùng đã trả lời |
| P1 | Dựng khung thư mục và `config.py`, `db.py` (mục 5) | `runner.py --list` chạy được; `db.py` đếm được `[User]` |
| P2 | Fixture + cleanup (mục 6, 7) | Mỗi fixture dựng và dọn sạch thành công (đếm SQL) |
| P3 | Đọc SRS và 2 file `.vpp`, viết `testcases/*.py` **chỉ phần metadata** (id, description, procedure, expected, preconditions, source), chưa viết hàm chạy | Có đủ TC theo mục 12; mỗi `expected` có `source`; 🛑 gửi người dùng bảng tóm tắt (ID, mô tả, expected, source) để duyệt |
| P4 | Viết hàm chạy cho **Tier 1** (khoảng 30 TC) | Mỗi TC chạy riêng được: `runner.py --only <ID>` |
| P5 | Chạy thử 5 TC đại diện: `IS1`, `IS9`, `IE3`, `IR2`, `IS20` | 5 TC cho kết quả có ảnh; không còn dữ liệu `e2e.*` sau khi chạy |
| P6 | Viết nốt Tier 2, chạy toàn bộ, phân loại lỗi (mục 9) | `results.json` đủ TC; `findings.md` đầy đủ |
| P7 | `report_builder.py`: sinh Excel (mục 3), chèn ảnh | File mở được; công thức tính ra số; số TC từng sheet khớp |
| P8 | Tự kiểm tra Excel (mục 13) | Tất cả mục trong checklist đạt |
| P9 | Báo cáo cuối cho người dùng, dọn process, **không commit** | Mục 14 |

Nếu bị kẹt một TC quá 3 lần thử khác nhau: đặt `Pending` + Note, ghi `findings.md`, **đi tiếp**, không dừng cả dự án.

---

## 11. Mâu thuẫn SRS ↔ code đã biết (🛑 người dùng quyết định, mặc định ghi sau)

SRS bản chuẩn chưa được cập nhật theo các thay đổi của đợt "hạn chấm điểm + Cân nhắc" (ducnm, 23/10/2026). Những chỗ dưới đây Gemini **không tự quyết**. Chờ người dùng chọn; nếu người dùng không trả lời thì dùng mặc định.

| # | SRS nói | Code/Plan làm | TC ảnh hưởng | Mặc định đề xuất |
|---|---|---|---|---|
| D1 | `FinalDecision` chỉ `Passed`/`Failed`; "a saved result cannot be changed" | Có thêm `Considering`, chốt lại được từ Cân nhắc | IR10, IR11, IR12 | Ghi `Pending` + Note "SRS chưa có trạng thái Considering, chờ cập nhật SRS" |
| D2 | `BR-INT-01`: chỉ nộp được "khi điền đủ mọi điểm" | Cho bỏ trống từng điểm, chỉ cần ≥1 điểm (plan) | IE4 | Như D1 |
| D3 | `MSG25`: `Interview schedule {status} successfully` | UI: "Cập nhật lịch phỏng vấn thành công!" / "Hủy lịch phỏng vấn thành công!" | IS21, IS25 | Chỉ kiểm có toast thành công, ghi Note khác câu chữ, **không** ghi Failed |
| D4 | UC66: Interviewer xem CV ứng viên được phân công | Nhiều khả năng chưa làm | IS29 | Kiểm tra thực tế; chưa có thì `Failed` + Note (đây là lỗi thật theo SRS) |
| D5 | Không nêu hạn 24h/2 ngày/7 ngày, không nêu "Nộp trễ" | Có (plan Q1–Q7) | IE15, IR8, IR9, IR12, IR14 | Loại C: `Passed` so với plan, Note "SRS chưa nêu" |
| D6 | `GBR-06` (bản chuẩn): Interviewer chỉ thấy phiếu của mình tới khi HM chốt | Code khớp | IE12, IE13 | Không mâu thuẫn, kiểm bình thường |

Gemini phải **đọc lại SRS** để xác nhận từng dòng D1–D6 còn đúng không, rồi báo cáo nếu có thêm mâu thuẫn mới.

---

## 12. Danh sách test case (bản nháp để Gemini bám theo)

Quy ước cột: `Tier` (1 làm trước), `Auto` (A tự động / M thủ công), `Fixture`, `Role`. Số thứ tự và câu chữ cuối cùng phải khớp SRS sau khi Gemini đọc. Tổng 62 TC (29 + 16 + 17), 3 sheet.

### Sheet `InterviewSchedule` (tiền tố `<IS`, Feature: "Interview Scheduling")

| ID | Nhóm | Mô tả ngắn (Verify that...) | Source | Fixture | Role | Tier |
|---|---|---|---|---|---|---|
| IS1 | View List | HR sees all schedules (card per schedule, status badge) | UC60 | F_UPCOMING, F_ENDED_FULL | hr_lan | 1 |
| IS2 | View List | Director sees the same full list | UC60 | F_UPCOMING | director | 1 |
| IS3 | View List | Interviewer sees only assigned schedules | UC65, GBR-05 | F_UPCOMING + lịch của người khác | interviewer_nam | 1 |
| IS4 | View List | Hiring Manager sees only own department's schedules | UC65 | lịch phòng Engineering và Sales | hm_tuan | 2 |
| IS5 | View List | Candidate sees own interviews at `/portal/interviews` without internal data (no panel, no scores) | UC65, GBR-02 | F_UPCOMING | e2e.b | 1 |
| IS6 | View List | Candidate cannot open `/interviews` (403) | OR-01 | | e2e.b | 1 |
| IS7 | View List | Client-side status filter hides non-matching cards, empty state when none match | UC60, MSG01 | F_UPCOMING | hr_lan | 2 |
| IS8 | View Details | Result/detail page shows candidate, time, format, link/location, panel | UC61 | F_UPCOMING | hr_lan | 1 |
| IS9 | Create | HR creates a valid online schedule: redirected to list, success message (MSG24), status Scheduled, panel saved in DB | UC62, MSG24 | F_NO_SCHEDULE | hr_lan | 1 |
| IS10 | Create | HR creates an offline schedule with a room location | UC62 | F_NO_SCHEDULE | hr_lan | 2 |
| IS11 | Create | Error when end time is not later than start time | MSG26 | F_NO_SCHEDULE | hr_lan | 1 |
| IS12 | Create | Error when Scheduled start time is in the past | PLAN (code rule) | F_NO_SCHEDULE | hr_lan | 2 |
| IS13 | Create | Error when no panel member is selected | UC62 | F_NO_SCHEDULE | hr_lan | 1 |
| IS14 | Create | HR cannot select self as panel member (own name absent from list) | UC62 | F_NO_SCHEDULE | hr_lan | 1 |
| IS15 | Create | Director creates a schedule and includes self in panel | UC62 | F_NO_SCHEDULE | director | 1 |
| IS16 | Create | Error when an interviewer already has an overlapping schedule | PLAN (code rule) | F_OVERLAP | hr_lan | 2 |
| IS17 | Create | Opening form with `?applicationId=` locks the application; ineligible application redirects back with message | UC62 | F_NO_SCHEDULE | hr_lan | 2 |
| IS18 | Create | Schedule is still created when the email gateway fails; warning shown, no rollback | OR-10 | F_NO_SCHEDULE | hr_lan | 1 |
| IS19 | Create | Application moves to Interviewing after a schedule is created | OR-06 | F_NO_SCHEDULE | hr_lan | 2 |
| IS20 | Create | Hiring Manager and Interviewer cannot open the create form (403) | OR-01, UC62 | | hm_tuan, interviewer_nam | 1 |
| IS21 | Update | HR changes time and location, saved, success toast | UC63, MSG25 | F_UPCOMING | hr_lan | 1 |
| IS22 | Update | Cannot move status backward (Completed → Scheduled) | GBR-01, OR-06 | F_ENDED_FULL | hr_lan | 1 |
| IS23 | Update | Reschedule requires a future start time | PLAN (code rule) | F_UPCOMING | hr_lan | 2 |
| IS24 | Update | HR already in panel (added by another HR) keeps own seat when editing | UC62 (note) | F_UPCOMING | hr_quang | 2 |
| IS25 | Cancel | HR cancels a schedule through the confirmation dialog: status Cancelled in DB, success toast | UC64, MSG12, MSG25 | F_UPCOMING | hr_lan | 1 |
| IS26 | Cancel | Choosing "Không hủy" leaves status unchanged | MSG12 | F_UPCOMING | hr_lan | 2 |
| IS27 | Cancel | Cancel button not offered for Completed or Cancelled schedules | GBR-01 | F_ENDED_FULL, F_CANCELLED | hr_lan | 2 |
| IS28 | Cancel | Interviewer cannot cancel (403) | OR-01 | F_UPCOMING | interviewer_nam | 1 |
| IS29 | CV | Interviewer opens CV of an assigned candidate | UC66 | F_UPCOMING | interviewer_nam | 1 |

### Sheet `InterviewEvaluation` (tiền tố `<IE`, Feature: "Interview Evaluation")

| ID | Nhóm | Mô tả ngắn | Source | Fixture | Role | Tier |
|---|---|---|---|---|---|---|
| IE1 | Open Form | Panel member sees "Đánh giá" button once the interview has started | UC67 | F_STARTED | interviewer_nam | 1 |
| IE2 | Open Form | No evaluate button before the interview starts; a reason is shown | PLAN (code rule) | F_UPCOMING | interviewer_nam | 1 |
| IE3 | Submit | Valid full evaluation: success (MSG27), page reloads, member shows "Đã nộp", schedule becomes Completed | UC67, MSG27, GBR-01 | F_STARTED | interviewer_nam | 1 |
| IE4 | Submit | HR panel member submits scores for soft skill and culture only | UC67, BR-INT-01 (D2) | F_STARTED | hr_quang | 1 |
| IE5 | Submit | Error when no score is given at all | UC67 | F_STARTED | interviewer_nam | 1 |
| IE6 | Submit | Error when no recommendation is chosen | UC67 | F_STARTED | interviewer_nam | 1 |
| IE7 | Submit | Second submission by the same evaluator is rejected (MSG28), even by replaying the request | MSG28, BR-INT-01 | F_STARTED (đã có phiếu của nam) | interviewer_nam | 1 |
| IE8 | Submit | HM and Director who are panel members can submit | MSG27 | F_STARTED | hm_tuan, director | 2 |
| IE9 | Submit | Cannot evaluate a cancelled schedule | UC67 (code) | F_CANCELLED | interviewer_nam | 2 |
| IE10 | Submit | Evaluation after 24h is accepted and flagged "Nộp trễ" | PLAN Q1 | F_AFTER_24H | interviewer_nam | 2 |
| IE11 | Access | Interviewer outside the panel cannot open the result page (403) | GBR-05 | F_STARTED | interviewer_thao (tag e2e) | 1 |
| IE12 | Access | Candidate cannot open the result page or see scores | GBR-02 | F_ENDED_FULL | e2e.e | 1 |
| IE13 | Blind Review | Before final result, interviewer sees only own evaluation, no average, no others' scores | GBR-06 | F_ENDED_PARTIAL | interviewer_nam | 1 |
| IE14 | Blind Review | After final result, interviewer sees all evaluations | GBR-06 | F_PASSED | interviewer_nam | 1 |
| IE15 | Dashboard | Interviewer's list card shows "Chờ đánh giá · còn Nh" when evaluation is pending | PLAN Q1 | F_STARTED | interviewer_nam | 2 |
| IE16 | Dashboard | Overdue badge after 24h without evaluation | PLAN Q1 | F_AFTER_24H | interviewer_nam | 2 |

### Sheet `InterviewResult` (tiền tố `<IR`, Feature: "Interview Result & Final Decision")

| ID | Nhóm | Mô tả ngắn | Source | Fixture | Role | Tier |
|---|---|---|---|---|---|---|
| IR1 | View Result | HM sees evaluations, averages and recommendation distribution | UC54 | F_ENDED_FULL | hm_tuan | 1 |
| IR2 | Finalize | HM of the owning department records Passed with salary: success (MSG29), result saved, application stays Interviewing | UC55, MSG29 | F_ENDED_FULL | hm_tuan | 1 |
| IR3 | Finalize | HM records Failed: application becomes Rejected | UC55 | F_ENDED_FULL | hm_tuan | 1 |
| IR4 | Finalize | Cannot finalize before the interview has started | UC55 / Screen 37 | F_UPCOMING | hm_tuan | 1 |
| IR5 | Finalize | A saved Passed/Failed result cannot be changed or re-submitted | UC55 / Screen 37 | F_PASSED | hm_tuan | 1 |
| IR6 | Finalize | HM of another department cannot finalize | Screen 37 | F_ENDED_FULL | hm_huong | 1 |
| IR7 | Finalize | HR, Director, Interviewer get 403 when posting a final result | OR-01 | F_ENDED_FULL | hr_lan, director, interviewer_nam | 1 |
| IR8 | Finalize | Within 24h and missing evaluations: blocked with reason "n/m" | PLAN Q2 | F_ENDED_PARTIAL | hm_tuan | 1 |
| IR9 | Finalize | After 24h HM can finalize despite missing evaluations, with a warning | PLAN Q2 | F_AFTER_24H | hm_tuan | 2 |
| IR10 | Considering | HM chooses "Cân nhắc": badge "Đang cân nhắc", application stays Interviewing | PLAN Q6 (D1) | F_ENDED_FULL | hm_tuan | 1 |
| IR11 | Considering | HM turns Considering into Passed; ConsideredAt unchanged, ApprovedAt updated | PLAN Q6 (D1) | F_CONSIDERING | hm_tuan | 2 |
| IR12 | Considering | Warning when Considering lasts over 7 days, still finalizable | PLAN Q7 (D1) | F_CONSIDER_OVERDUE | hm_tuan | 2 |
| IR13 | Validation | Negative recommended salary is rejected with field error | Screen 37 | F_ENDED_FULL | hm_tuan | 2 |
| IR14 | List labels | List card shows "Chờ chốt" / "Quá hạn chốt" and the "Kết quả" filter works | PLAN Q3 | F_ENDED_FULL, F_OVERDUE_DECISION | hr_lan | 2 |
| IR15 | Integration | Passed candidate appears in HR's offer-creation list | UC68 Create Offer | F_PASSED | hr_lan | 1 |
| IR16 | Integration | Failed result: candidate sees application as rejected in `/portal/applications` | UC55 | F_FAILED | e2e.i | 2 |
| IR17 | Privacy | Candidate never sees final result or scores on `/portal/interviews` | GBR-02 | F_PASSED | e2e.h | 1 |

Tier 1 = ưu tiên làm trước. Test case thủ công (`kind = manual`, ghi `Pending` kèm lý do) chỉ dành cho việc không tự động được: kiểm tra email thật đã tới hộp thư và đánh giá bố cục/thẩm mỹ. Bản nháp trên chưa có TC thủ công; nếu Gemini thêm thì phải đánh dấu rõ.

---

## 13. Kiểm tra Excel đầu ra (Phase 8, checklist)

Gemini tự kiểm bằng `openpyxl` rồi ghi kết quả vào `integration-test/output/excel-check.txt`:

- [ ] Có đủ sheet: `Cover`, `Test Cases`, `Test Statistics`, `InterviewSchedule`, `InterviewEvaluation`, `InterviewResult`.
- [ ] Mỗi sheet module có ô `A2..A5`, hàng 6–9 `Round 1..4`, hàng 11 tiêu đề, hàng nhóm nền `CCFFFF`.
- [ ] ID dạng `<IS1>`… không trùng, đúng tiền tố; `B4 = COUNTIF(...)` đếm ra đúng số TC.
- [ ] Cột `H` chỉ có `Passed/Failed/Pending/N/A`; có dropdown ở `H,K,N,Q`.
- [ ] Cột `I` là ngày chạy thật; `J` là `TESTER_NAME`; Round 2–4 để trống.
- [ ] Mỗi dòng TC đã tự động có ảnh ở cột F; ảnh không phủ chồng dòng khác.
- [ ] `Test Statistics` trỏ vào hàng Round 1; tổng `Sub total` khớp tay; `Test coverage` không chia cho 0.
- [ ] Mở file bằng Excel (hoặc LibreOffice nếu có) không báo lỗi. Nếu không mở được GUI, tối thiểu `openpyxl.load_workbook` đọc lại không lỗi.
- [ ] Dung lượng < 25 MB.
- [ ] Không có mật khẩu hay thông tin SMTP trong file (quét không phân biệt hoa thường các từ `password`, `smtp`, `secret`, và đối chiếu với giá trị trong `application-local.properties`).

---

## 14. Báo cáo cuối Gemini phải đưa cho người dùng

1. Bảng tổng: số TC theo sheet, Passed / Failed / Pending / N/A, coverage.
2. Danh sách `Failed` với một dòng lý do mỗi cái (đã phân loại loại B).
3. Danh sách "SRS cần cập nhật" (loại C và D).
4. Danh sách TC `Pending` kèm lý do.
5. Đường dẫn đầy đủ: file Excel, `findings.md`, thư mục evidence.
6. Xác nhận: DB đã dọn sạch dữ liệu `e2e.*`; app do mình bật đã tắt; không file ngoài dự án bị sửa; không commit.
7. Ghi rõ **những gì chưa kiểm** và **giả định đã dùng** (ví dụ giờ máy, tài khoản bổ sung đã tạo).

---

## 15. Đoạn lệnh gợi ý để người dùng dán cho Gemini

> Đọc kỹ `docs/members/duc/plan-interview-integration-test.md` rồi thực hiện từng Phase P0 → P9, **dừng và hỏi tôi** ở mọi chỗ có biểu tượng 🛑. Không sửa code trong `src/`, không commit, không in mật khẩu. Expected Results phải lấy từ SRS, không lấy từ code. Sau mỗi Phase, báo ngắn gọn: đã làm gì, cổng đã qua chưa, vấn đề còn lại. Bắt đầu với P0.

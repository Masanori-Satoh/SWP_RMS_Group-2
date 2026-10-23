# Kế hoạch: hạn chấm điểm phỏng vấn, kết quả "Cân nhắc", khóa ứng viên khi đặt lịch

> Người làm: ducnm · Lập ngày 23/10/2026 · Trạng thái: **đã làm xong phần code + DB (23/10/2026), chưa mở PR**
>
> Ghi chú khi thực hiện:
> - Migration `010_interview_consider.sql` đã chạy trên DB local; có thêm UNIQUE `(InterviewId, InterviewerId)` cho `InterviewEvaluation`.
> - Chưa làm (tùy chọn): seed dòng `Considering` cho demo, cập nhật SRS (file .docx ngoài repo), `WORK_LOG.md`.
> - `InterviewScheduleIntegrationTests` ở mục 5 không còn trong repo nên không chạy lại.
> Branch đề xuất: `feat/ducnm/interview-evaluation-deadline-consider` (tách từ `main` mới nhất)

## 1. Quyết định đã chốt

| # | Chủ đề | Quyết định |
|---|---|---|
| Q1 | Hạn nộp phiếu đánh giá | **24h kể từ `EndTime`** của buổi phỏng vấn. Quá hạn **vẫn cho nộp**, phiếu được đánh dấu **"Nộp trễ"**. |
| Q2 | Điều kiện HM chốt kết quả | Trong 24h sau `EndTime`: **phải đủ phiếu** của cả hội đồng mới được chốt. Sau 24h: chốt tự do dù thiếu phiếu. |
| Q3 | Hạn chốt kết quả lần đầu | **2 ngày kể từ lúc hết hạn chấm điểm**, tức `EndTime + 24h + 48h` (= `EndTime + 72h`). Áp dụng cho lần chốt đầu tiên: Cân nhắc / Đạt / Không đạt. |
| Q4 | Quá hạn thì sao | **Chỉ cảnh báo** (nhãn "Quá hạn" trên giao diện). Hệ thống **không tự quyết** thay con người. |
| Q5 | Có khóa sau hạn không | **Không khóa.** HM vẫn chốt được sau mọi hạn, chỉ bị gắn nhãn quá hạn. |
| Q6 | Lưu "Cân nhắc" ở đâu | **Phương án 1: lưu trong DB.** `FinalDecision` có thêm giá trị `Considering`, kèm cột `ConsideredAt`. |
| Q7 | Thời gian tối đa ở "Cân nhắc" | **7 ngày kể từ `ConsideredAt`**. Quá 7 ngày chỉ cảnh báo (theo Q4, Q5). |

## 2. Các mốc thời gian (một nguồn duy nhất)

Gom vào **một class hằng số + hàm tính**, ví dụ `interview/service/InterviewDeadlines.java`, để service, trang kết quả, danh sách lịch và test cùng dùng:

```text
evaluationDeadline = EndTime + 24h          (Q1, Q2)
decisionDeadline   = EndTime + 72h          (Q3: hết hạn chấm + 2 ngày)
considerDeadline   = ConsideredAt + 7 ngày  (Q7)
```

- Lịch `Cancelled`: không áp dụng hạn nào.
- Lịch đổi giờ (`Rescheduled`): luôn tính theo `EndTime` hiện tại.
- Dùng một `Clock` để test được. Commit `30b8c35` đã thêm `core/config/ClockConfig.java`, nên kiểm tra và tái dùng bean này.

## 3. Thay đổi Database (phần của mình)

### 3.1 `database/schema/db.sql` (bảng 3.17 `InterviewFinalResult`)

```sql
ConsideredAt DATETIME2 NULL,   -- lúc HM chọn "Cân nhắc" lần đầu; NULL nếu chốt thẳng Passed/Failed

CONSTRAINT CK_InterviewFinalResult_FinalDecision
    CHECK (FinalDecision IN (N'Passed', N'Failed', N'Considering')),

-- Considering thì phải có ConsideredAt
CONSTRAINT CK_InterviewFinalResult_ConsideredAt
    CHECK (FinalDecision <> N'Considering' OR ConsideredAt IS NOT NULL)
```

- `ApprovedAt` giữ nghĩa "thời điểm ra quyết định hiện tại". Khi đổi `Considering → Passed/Failed` thì cập nhật lại `ApprovedAt`, còn `ConsideredAt` **giữ nguyên** để còn biết đã cân nhắc bao lâu.
- (Tùy chọn, nên làm cùng lúc) thêm vào bảng 3.16 `InterviewEvaluation`:
  `CONSTRAINT UQ_InterviewEvaluation_Interview_Interviewer UNIQUE (InterviewId, InterviewerId)`.
  Service đã chặn nộp trùng nhưng DB chưa có ràng buộc này (điểm D11 hôm trước).
- **"Nộp trễ" không cần cột mới**: tính được từ `EvaluatedAt > EndTime + 24h`.

### 3.2 Migration cho DB đang chạy: `database/migrations/010_interview_consider.sql`

Theo đúng kiểu các file 001–009:

- `ALTER TABLE InterviewFinalResult ADD ConsideredAt DATETIME2 NULL`
- `DROP` rồi tạo lại `CK_InterviewFinalResult_FinalDecision` với 3 giá trị
- thêm `CK_InterviewFinalResult_ConsideredAt`
- (tùy chọn) thêm UNIQUE cho `InterviewEvaluation`. Phải kiểm tra trùng dữ liệu seed trước khi thêm (`GROUP BY InterviewId, InterviewerId HAVING COUNT(*) > 1`).

### 3.3 Seed

Không bắt buộc sửa. Có thể thêm 1–2 dòng `Considering` vào `seed_extra_jobs_and_pipeline.sql` để demo màn hình "Đang cân nhắc". Script phải chạy lại nhiều lần vẫn an toàn.

## 4. Thay đổi code

### 4.1 Hạn và trạng thái chấm điểm (Q1, Q2)

| File | Việc |
|---|---|
| `interview/service/InterviewDeadlines.java` (mới) | Hằng số và hàm tính 3 mốc ở mục 2; enum trạng thái chấm điểm của từng người: `NotStarted` (chưa tới giờ), `Pending` (còn X giờ), `Submitted`, `SubmittedLate`, `Overdue` (quá 24h chưa nộp) |
| `InterviewEvaluationService.blockedReason` | **Giữ nguyên ý nghĩa**: sau 24h vẫn cho nộp (Q1). Không thêm nhánh chặn. |
| `InterviewEvaluationServiceImpl.submit` | Không đổi luồng; phiếu nộp sau `evaluationDeadline` được coi là trễ (suy ra, không lưu) |
| `InterviewResultServiceImpl.getResult` + `InterviewResultDetailResponse` | Thêm vào mỗi `PanelMemberStatus`: trạng thái chấm điểm và cờ nộp trễ. Thêm `evaluationDeadline` và `decisionDeadline` vào response |
| `interview/result.html` | Hiện trạng thái từng thành viên (badge), hạn chấm điểm, nhãn "Nộp trễ" trên phiếu |
| Danh sách lịch của Interviewer (`interview/list.html`) | Badge "Chờ đánh giá (còn Xh)" / "Quá hạn đánh giá" cho lịch mình chưa nộp |

### 4.2 Điều kiện chốt và kết quả "Cân nhắc" (Q2–Q7)

| File | Việc |
|---|---|
| `InterviewFinalResult.java` | Thêm `consideredAt`; cập nhật Javadoc: `Passed / Failed / Considering` |
| `SubmitFinalResultRequest.java` | `@Pattern` cho phép `Passed\|Failed\|Considering` |
| `InterviewFinalResultService.blockedReason` | Thêm điều kiện Q2: nếu `now < evaluationDeadline` **và** chưa đủ phiếu thì trả lý do *"Chưa đủ phiếu đánh giá (x/y). Có thể chốt khi đủ phiếu hoặc sau HH:mm dd/MM."* Hàm này cần thêm số phiếu đã nộp và số người trong hội đồng, nên đổi chữ ký (hoặc truyền vào một đối tượng `facts`). |
| `InterviewFinalResultServiceImpl.decide` | Chưa có kết quả: lưu như cũ, nếu là `Considering` thì đặt `consideredAt = now`. Đã có kết quả `Considering`: **cho cập nhật** sang `Passed`/`Failed` (hoặc giữ `Considering` để sửa nhận xét/lương, tùy chọn), cập nhật `approvedAt`, giữ `consideredAt`. Đã có `Passed`/`Failed`: chặn như cũ. Chỉ gọi `applicationStatusService.onInterviewDecided` khi kết quả là `Passed`/`Failed`. `Considering` giữ hồ sơ ở `Interviewing`. |
| `InterviewResultServiceImpl` | `canFinalize` = chưa có kết quả **hoặc** đang `Considering`. Thêm các cờ cảnh báo: `decisionOverdue` (chưa chốt, quá `decisionDeadline`) và `considerOverdue` (đang `Considering`, quá `considerDeadline`) |
| `interview/final-result-dialog.html` + `js/pages/interview-result.js` | Thêm lựa chọn "Cân nhắc". Khi mở lại từ trạng thái Cân nhắc thì điền sẵn nhận xét và lương đã nhập |
| `interview/result.html` | Thẻ kết quả: badge "Đang cân nhắc", "Cân nhắc từ dd/MM", cảnh báo "Quá hạn chốt" / "Cân nhắc quá 7 ngày". **Không khóa nút** (Q5) |
| `ApplicationStatusLabels` | Nhãn "Đang cân nhắc", màu `warning` |
| `ApplicationProgressResolver` | `Considering` hiện cho ứng viên là `AWAITING_DECISION` ("Chờ chốt kết quả phỏng vấn"), **không lộ chữ "cân nhắc"** (GBR-02) |
| Danh sách lịch của HM (`InterviewSchedulingController` + `interview/list.html`) | Bộ lọc nhanh: **"Đang cân nhắc"** và **"Quá hạn chốt"**, để cuối đợt tuyển HM xem lại và chốt hàng loạt từng người |

**Không cần sửa:**
- Danh sách "ứng viên đạt" để HR lập offer (`InterviewFinalResultRepository` lọc `= 'passed'`): `Considering` tự bị loại.
- `OfferServiceImpl` chỉ hiển thị `finalDecision`. Cần kiểm tra lại trang `offers/detail.html` có hiển thị đúng nhãn không.
- Dashboard (`dashboard/fragments.html`) có đọc `FinalDecision`: kiểm tra số liệu đếm Passed/Failed không bị lệch.

### 4.3 Khóa ứng viên khi đặt lịch từ hồ sơ

Nguồn mở form: [`candidate/applications.html:138`](../../../src/main/resources/templates/candidate/applications.html) và [`candidate/application-detail.html:172`](../../../src/main/resources/templates/candidate/application-detail.html) → `/interviews/new?applicationId=X`.

| File | Việc |
|---|---|
| `InterviewSchedulingController.showCreateForm` | Có `applicationId` và hồ sơ đủ điều kiện xếp lịch thì đặt `lockedApplication` (thông tin hiển thị) vào model. Không đủ điều kiện (đã có lịch, sai trạng thái, tin đã đóng) thì redirect về trang hồ sơ kèm `errorMessage` thay vì mở form trống |
| `InterviewSchedulingController.createSchedule` | Form gửi kèm ô ẩn `lockApplication=true`. Khi trả lại form do lỗi, **vẫn giữ trạng thái khóa**. Server không cần tin cờ này để phân quyền, vì `requireScheduleable` vẫn kiểm tra như cũ |
| `interview/form.html` | Khi khóa: **thay** ô tìm kiếm + dropdown bằng thẻ chỉ đọc (Hồ sơ #, tên, email, vị trí) và `<input type="hidden" name="applicationId">`; thêm link "Đổi hồ sơ" về `/interviews/new` (không khóa). Khi không khóa: giữ nguyên như hiện tại |
| `js/pages/...` (script tìm kiếm của form) | Không chạy phần tìm kiếm hồ sơ khi dropdown không tồn tại (tránh lỗi JS) |

## 5. Test

| Test | Ca cần thêm / sửa |
|---|---|
| `InterviewEvaluationServiceTests` | Nộp sau 24h vẫn thành công (không bị chặn) |
| `InterviewFinalResultServiceTests` | Trong 24h + thiếu phiếu thì chặn. Trong 24h + đủ phiếu thì cho. Sau 24h + thiếu phiếu thì cho. Lưu `Considering` có `consideredAt`. `Considering → Passed` cập nhật `approvedAt`, giữ `consideredAt`, hồ sơ giữ Interviewing cho tới khi Passed. `Considering → Failed` thì hồ sơ thành Rejected. `Passed` không sửa được |
| `InterviewResultServiceTests` | Trạng thái chấm điểm từng người (Pending / Submitted / SubmittedLate / Overdue). `canFinalize` khi đang Considering. Cờ `decisionOverdue`, `considerOverdue` (dùng `Clock` cố định) |
| `InterviewScheduleFormWebTests` | Có `applicationId` hợp lệ thì form hiện thẻ khóa, không có dropdown. Không có `applicationId` thì dropdown như cũ. Lỗi validate vẫn giữ khóa. Hồ sơ không đủ điều kiện thì redirect kèm lỗi |
| `InterviewScheduleIntegrationTests` | Chạy lại toàn bộ (10 ca) để chắc không vỡ luồng cũ |
| `ApplicationProgressResolverTests` | `Considering` hiện `AWAITING_DECISION` |

Lệnh: `mvn -Dtest="Interview*Tests,ApplicationProgressResolverTests" test`, sau đó `mvn test` toàn bộ trước khi mở PR.

## 6. Tài liệu cần cập nhật

- `docs/database/model.md`: bảng `InterviewFinalResult` (giá trị mới, `ConsideredAt`), và UNIQUE của `InterviewEvaluation` nếu thêm.
- SRS:
  - Entity `InterviewFinalResult`: `Status / lifecycle` = Considering → Passed / Failed.
  - `BR-INT-01`: thêm hạn 24h và cờ nộp trễ.
  - Rule mới, đề xuất **BR-INT-02 Decision Deadlines**: HM chỉ chốt khi đủ phiếu hoặc đã qua 24h; nên chốt trong 2 ngày sau hạn chấm điểm; tối đa 7 ngày ở Cân nhắc; quá hạn chỉ cảnh báo.
- UC "Finalize Interview Result": thêm lựa chọn Cân nhắc và việc chốt lại sau.

## 7. Thứ tự làm (ước lượng)

1. Tạo branch, pull `main` mới nhất. *(5 phút)*
2. DB: sửa `db.sql`, viết migration `010`, chạy lên DB local, cập nhật `model.md`. *(30 phút)*
3. `InterviewDeadlines` + trạng thái chấm điểm + test service. *(45 phút)*
4. Điều kiện chốt (Q2) + kết quả Considering + test. *(1–1,5 giờ)*
5. Giao diện trang kết quả, dialog, bộ lọc HM. *(1 giờ)*
6. Khóa ứng viên ở form tạo lịch + web test. *(45 phút)*
7. Chạy `mvn test`, test tay theo `interview-demo-guide.md`, cập nhật SRS. *(30–45 phút)*

## 8. Checklist trước khi mở PR

- [ ] `db.sql` và migration `010` khớp nhau; app khởi động được với `ddl-auto=validate`
- [ ] Không có chỗ nào còn hard-code chỉ `Passed|Failed` (tìm `"Passed"`, `'Passed'`, `Passed|Failed`)
- [ ] Ứng viên không bao giờ thấy chữ "Cân nhắc" (GBR-02)
- [ ] Các hạn chỉ cảnh báo, không chặn thao tác (Q4, Q5)
- [ ] Form tạo lịch mở từ hồ sơ không đổi được ứng viên, kể cả sau khi lỗi validate
- [ ] `mvn test` xanh toàn bộ
- [ ] Cập nhật `docs/management/WORK_LOG.md` theo quy trình chống conflict trong `docs/README.md`

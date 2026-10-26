# 2026-10-23 — Hạn chấm điểm phỏng vấn, kết quả "Cân nhắc", khóa hồ sơ khi đặt lịch

## 1. Người thực hiện

Đức (ducnm) · Branch `feat/ducnm/interview-evaluation-deadline-consider`
Kế hoạch gốc: [`docs/members/duc/plan-interview-evaluation-consider.md`](../../members/duc/plan-interview-evaluation-consider.md)

## 2. Yêu cầu / Quyết định

| Chủ đề | Quyết định |
|---|---|
| Hạn nộp phiếu đánh giá | 24h kể từ giờ kết thúc buổi phỏng vấn. Quá hạn **vẫn nộp được**, phiếu được đánh dấu "Nộp trễ". |
| Điều kiện HM chốt | Trong 24h đó phải **đủ phiếu** của hội đồng; sau 24h chốt tự do. |
| Hạn chốt lần đầu | 2 ngày sau hạn chấm điểm (giờ kết thúc + 72h). |
| Kết quả "Cân nhắc" | Lưu trong DB (`FinalDecision = 'Considering'`). HM chốt lại Đạt/Không đạt sau; Đạt/Không đạt vẫn là kết quả cuối. Nên chốt trong 7 ngày. |
| Khi quá hạn | Chỉ cảnh báo, **không khóa** thao tác (quyết định cuối thuộc về con người). |
| Đặt lịch từ hồ sơ | Mở `/interviews/new?applicationId=X` thì cố định hồ sơ, không cho đổi ứng viên. |

## 3. Database

- `database/schema/db.sql`:
  - `InterviewFinalResult`: CHECK `FinalDecision IN ('Passed','Failed','Considering')`, cột mới `ConsideredAt DATETIME2 NULL`, CHECK `CK_InterviewFinalResult_ConsideredAt` (đang cân nhắc thì phải có `ConsideredAt`).
  - `InterviewEvaluation`: `UQ_InterviewEvaluation_Interview_Interviewer UNIQUE (InterviewId, InterviewerId)` (BR-INT-01).
- Migration mới `database/migrations/010_interview_consider.sql` cho DB đang chạy (chạy lại nhiều lần vẫn an toàn; báo lỗi rõ nếu có phiếu trùng trước khi thêm UNIQUE).
- **Mọi máy cần chạy migration `010`**, nếu không app sẽ không khởi động (`ddl-auto=validate`, entity có cột `ConsideredAt`).
- Seed: không đổi.

## 4. Mã nguồn / Tài liệu

**Mới**
- `interview/service/InterviewDeadlines.java`: một nguồn duy nhất cho các mốc (hạn chấm điểm, hạn chốt, hạn cân nhắc) và trạng thái chấm điểm từng người (`Chưa đến giờ / Chờ đánh giá / Đã nộp / Nộp trễ / Quá hạn đánh giá`). "Nộp trễ" suy ra từ `EvaluatedAt`, không lưu DB.
- `interview/dto/InterviewCardState.java`: nhãn phụ trên thẻ lịch.

**Sửa**
- `InterviewFinalResult` (+`consideredAt`, hằng số `PASSED / FAILED / CONSIDERING`), `SubmitFinalResultRequest` (nhận `Considering`).
- `InterviewFinalResultService(Impl)`:
  - Chặn chốt trong 24h nếu chưa đủ phiếu.
  - Cho chốt lại khi đang `Considering` (giữ `ConsideredAt`, cập nhật `ApprovedAt`).
  - `Considering` không đổi hồ sơ sang Rejected/Offer.
  - Dùng `Clock` để test được.
- `InterviewResultService(Impl)`:
  - Trạng thái chấm điểm và cờ nộp trễ trên trang kết quả; các mốc hạn; `decisionOverdue`, `considerOverdue`.
  - `canFinalize` bật cả khi đang cân nhắc.
  - Thêm `decisionStates` và `evaluationStates` cho danh sách lịch.
- Repository: truy vấn theo lô `findByInterviewSchedule_InterviewIdIn`, `findByInterviewer_UserIdAndInterviewSchedule_InterviewIdIn`.
- `InterviewSchedulingController`:
  - Danh sách lịch có thêm nhãn kết quả và trạng thái chấm điểm.
  - Form tạo lịch khóa hồ sơ khi có `applicationId` hợp lệ; giữ khóa khi trả lại form do lỗi; hồ sơ không đủ điều kiện thì quay về `/applications/{id}` kèm lý do.
- Giao diện:
  - `interview/result.html`: hạn chấm điểm và hạn chốt, badge trạng thái từng người, "Nộp trễ", cảnh báo quá hạn, thông tin cân nhắc.
  - `interview/final-result-dialog.html`: lựa chọn "Cân nhắc", điền sẵn khi chốt lại.
  - `interview/list.html` + `js/interview-list.js`: bộ lọc "Kết quả".
  - `interview/form.html`: thẻ hồ sơ cố định.
  - CSS `interview-list.css`, `interview-form.css`.
- Phía ứng viên: `ApplicationProgressResolver` hiện `Considering` là "Chờ chốt kết quả phỏng vấn" (GBR-02, không lộ chữ "cân nhắc"). `ApplicationStatusLabels` thêm nhãn nội bộ "Đang cân nhắc".
- Tài liệu: `docs/database/model.md` (bảng 3.16, 3.17).

## 5. Kiểm tra (Testing)

- Test mới:
  - `InterviewFinalResultServiceTests`: 7 ca (đủ/thiếu phiếu trong và sau 24h, lưu `Considering`, chốt lại sang Đạt/Không đạt, cân nhắc quá 7 ngày vẫn chốt được, Đạt không mở lại được).
  - `InterviewResultServiceTests`: 7 ca (nộp trễ, quá hạn đánh giá, chặn chốt thiếu phiếu, cảnh báo quá hạn chốt, cân nhắc quá 7 ngày, nhãn danh sách).
  - `InterviewScheduleFormWebTests`: 3 ca (khóa hồ sơ, hồ sơ không đủ điều kiện, giữ khóa sau lỗi validate).
  - `ApplicationProgressResolverTests`: 1 ca (ứng viên thấy "Chờ chốt kết quả").
- Sửa test cũ theo constructor và record mới. `applicationIdFromApplicationPageIsPreselected` đổi thành `applicationIdFromApplicationPageLocksTheApplication`.
- `mvn -Dtest="Interview*Tests,ApplicationProgressResolverTests,SubmitEvaluationRequestTests" test`: **tất cả đạt**.
- `mvn test` toàn bộ: 1772 test, **6 fail có sẵn từ trước** (đã chạy lại trên `main` gốc, fail y hệt): `CareerFlowTests` ×2, `CareerReadOnlyTests`, `SidebarMenuTests`, `SidebarRoleWebTests`, `RequisitionControllerTests`.
- Chưa test tay trên trình duyệt.

## 6. Vấn đề tồn đọng

- Test tay theo `docs/members/duc/interview-demo-guide.md` (chốt "Cân nhắc", chốt lại, cảnh báo quá hạn, khóa hồ sơ).
- Seed vài dòng `Considering` để demo bộ lọc "Đang cân nhắc" (tùy chọn).
- Cập nhật SRS: entity `InterviewFinalResult`, BR-INT-01 (hạn 24h, nộp trễ), rule mới BR-INT-02 (điều kiện và các hạn chốt).
- 6 test fail có sẵn ở Career/Sidebar/Requisition: báo người phụ trách các module đó.

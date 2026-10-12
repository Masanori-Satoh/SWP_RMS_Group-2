# Bàn giao: đọc và đồng bộ kết quả phỏng vấn (module `interview`)

> **Người viết:** DucNM (P4) · **Ngày:** 2026-10-11 · **Nhánh:** `feature/ducnm/iter2-interview_evaluation`
> **Dành cho:** người phụ trách `candidate` (DungLT), `offer` (HuyenPT), `dashboard` (LinhDN), `notification`.
> Module `interview` đã ghi đủ phiếu đánh giá và kết quả cuối. Các module khác **chưa hiển thị hoặc chưa đồng bộ** kết quả này. File này mô tả dữ liệu có sẵn và việc từng module nên làm.

---

## 1. Ví dụ thực tế: kết quả "Đạt" không hiện ở đâu cả

Ứng viên Đinh Văn Hiếu, hồ sơ #23, buổi phỏng vấn #5. HM Vũ Anh Tuấn chốt **Passed** lúc 2026-10-11 16:35.

| Màn hình | Đang hiện | Đúng ra |
|---|---|---|
| `/interviews/5/result` | Card "Kết quả cuối: **Đạt**" | ✅ |
| `/offers` → tạo offer | Hiếu có trong dropdown "ứng viên đạt" | ✅ |
| `/interviews` (thẻ lịch) | "Đã hoàn thành" | Nên có badge Đạt (việc của Đức, mục 4.5) |
| `/applications`, `/applications/23` | **"Bị loại"** (`Rejected`, dữ liệu seed) | Hồ sơ còn đi tiếp, có nút "Tạo offer" |
| Timeline hồ sơ #23 | "Lịch phỏng vấn: Completed" | Có thêm mốc "Kết quả phỏng vấn: Đạt" |

---

## 2. Dữ liệu module `interview` cung cấp

### 2.1. Bảng và quy tắc ghi

| Bảng | Ghi khi nào | Ai ghi | Quy tắc |
|---|---|---|---|
| `InterviewSchedule` | HR tạo/sửa/hủy lịch | `InterviewSchedulingServiceImpl` | `InterviewStatus` ∈ `Scheduled`, `Rescheduled`, `Completed`, `Cancelled`; chỉ đi tiến (GBR-01). **Phiếu đánh giá đầu tiên hoặc lúc HM chốt kết quả sẽ tự chuyển lịch sang `Completed`** |
| `InterviewPanel` | Cùng lúc tạo/sửa lịch | như trên | `RoleInPanel` ∈ `HR`, `HM`, `Interviewer`, `Director` (suy từ role tài khoản). Dữ liệu tạo trước 2026-10-11 có thể bị lưu nhầm toàn bộ là `HM` |
| `InterviewEvaluation` | Thành viên hội đồng nộp phiếu: `POST /interviews/{id}/evaluations` | `InterviewEvaluationServiceImpl` | Mỗi người 1 phiếu/lịch, không sửa. 3 điểm 1–5, **mỗi điểm có thể NULL** (ít nhất 1 điểm có giá trị). `Recommendation` ∈ `Hire`, `Consider`, `No_Hire`. Vẫn nhận phiếu nộp muộn sau khi đã chốt |
| `InterviewFinalResult` | HM chốt: `POST /interviews/{id}/final-result` | `InterviewFinalResultServiceImpl` (**nơi duy nhất ghi bảng này**) | **1 kết quả/lịch** (UNIQUE `InterviewId`), không sửa sau khi chốt. `FinalDecision` ∈ `Passed`, `Failed` (đúng hoa thường). `RecommendedSalary`, `FinalSummaryComments` có thể NULL. Chỉ HM quản lý phòng ban của yêu cầu tuyển dụng mới chốt được |

**Một hồ sơ có thể có nhiều buổi phỏng vấn** (nhiều vòng, hoặc buổi bị hủy rồi lên lịch lại), nên có thể có **nhiều** `InterviewFinalResult`. Kết quả có hiệu lực là **bản mới nhất theo `ApprovedAt`**.

**Module `interview` không đổi `Application.ApplicationStatus`** (quyết định D9: trạng thái hồ sơ thuộc module `candidate`). Xem mục 3.1.

### 2.2. Cách đọc (dùng lại, đừng viết query trùng)

| Cần gì | Dùng | Ghi chú |
|---|---|---|
| Kết quả cuối mới nhất của một hồ sơ | `InterviewFinalResultRepository.findByApplicationIdOrderByApprovedAtDesc(Integer applicationId)` → lấy phần tử đầu | `offer` đang dùng |
| Kết quả của một buổi | `InterviewFinalResultRepository.findByScheduleId(Long interviewId)` | **Không** dùng `findByInterviewSchedule_InterviewId(Integer)`: sai kiểu (id là `Long`), gọi sẽ lỗi |
| Mọi kết quả `Passed` kèm ứng viên, tin, phòng ban | `findAllPassedWithDetails()` | Xem lưu ý ở mục 3.2 |
| Toàn bộ trang kết quả đã tính sẵn (điểm trung bình, phân bố đề xuất, quyền xem) | `InterviewResultService.getResult(Long interviewId, User viewer)` | Trả DTO, đã kiểm quyền theo D5/GBR-05 |
| Link sang trang kết quả | `/interviews/{interviewId}/result` | HR, HM, Director, System Admin xem được mọi buổi. Interviewer chỉ xem buổi mình được xếp. Candidate **không** xem được |

Cột `InterviewSchedule.InterviewId` là `INT` trong DB nhưng entity là `Long`. Khi viết SQL thuần (`nativeQuery`) như module `candidate` đang làm thì không bị ảnh hưởng.

---

## 3. Việc nên làm theo từng module

### 3.1. `candidate` (DungLT): trạng thái hồ sơ và timeline. **Ưu tiên cao**

**a) Đồng bộ trạng thái hồ sơ khi có lịch / có kết quả.** Hiện tại hồ sơ đứng yên ở `HM_Passed` (hoặc giá trị seed) dù đã phỏng vấn xong. Đề xuất:

| Sự kiện bên `interview` | `ApplicationStatus` nên thành |
|---|---|
| HR tạo lịch phỏng vấn đầu tiên | `HM_Passed` → `Interviewing` |
| HM chốt `Passed` | giữ `Interviewing` (đã nằm trong `ApplicationAccess.NEXT_STEP_STATUSES`, nên nút "Tạo offer" hiện) |
| HM chốt `Failed` | `Rejected` |

Cách nối **không tạo phụ thuộc vòng**. `interview` đã phụ thuộc `candidate`, nên `candidate` **không được** nghe event hay import service của `interview`.
1. DungLT thêm một method trong `candidate`, ví dụ:
   ```java
   // candidate/service/ApplicationStatusService (hoặc đặt trong service sẵn có)
   @Transactional
   public void onInterviewScheduled(Integer applicationId);            // HM_Passed -> Interviewing
   @Transactional
   public void onInterviewDecided(Integer applicationId, boolean passed); // Failed -> Rejected
   ```
   Method tự kiểm tra trạng thái hiện tại (không lùi `Offered`/`Hired`) và tự ghi timeline nếu cần.
2. Đức thêm **1 dòng gọi** ở `InterviewSchedulingServiceImpl.createSchedule` và ở `InterviewFinalResultServiceImpl.decide`, ngay sau khi lưu, chung transaction.
3. Báo nhau tên method trước khi code để tránh conflict.

**b) Timeline hồ sơ thêm mốc kết quả.** `ApplicationRepository` hiện chỉ đọc `InterviewSchedule`. Thêm một query cùng kiểu `findInterviewEvents`:
```java
@Query(value = "SELECT r.ApprovedAt AS at, r.FinalDecision AS status FROM InterviewFinalResult r "
        + "JOIN InterviewSchedule s ON s.InterviewId = r.InterviewId "
        + "WHERE s.ApplicationId = :id", nativeQuery = true)
List<TimelineEventRow> findInterviewResultEvents(@Param("id") Integer applicationId);
```
Trong `ApplicationPipelineService` (cạnh dòng dựng mốc "Lịch phỏng vấn"):
```java
applications.findInterviewResultEvents(id).forEach(event -> items.add(new TimelineItem(event.getAt(),
        "Kết quả phỏng vấn: " + ("Passed".equals(event.getStatus()) ? "Đạt" : "Không đạt"),
        null, null, "Passed".equals(event.getStatus()) ? "success" : "danger")));
```
Nếu muốn hiện điểm trung bình phiếu đánh giá trên trang hồ sơ, hãy **link** sang `/interviews/{id}/result` thay vì tự tính lại.

**c) Dữ liệu seed.** Hồ sơ #23 (Đinh Văn Hiếu) đang `Rejected` dù HR/HM đều `Pass` và đã có lịch phỏng vấn. Nên rà lại `database/seeds` để trạng thái hồ sơ khớp với review và lịch.

### 3.2. `offer` (HuyenPT): chọn đúng ứng viên đạt

Đã đọc kết quả qua `findAllPassedWithDetails()` (danh sách tạo offer) và `findByApplicationIdOrderByApprovedAtDesc()` (chi tiết offer). Cần lưu ý:
1. **Nhiều vòng phỏng vấn:** `findAllPassedWithDetails()` trả mọi bản `Passed`, kể cả khi vòng sau của cùng hồ sơ là `Failed`. Query không có `ORDER BY`, nên việc lọc trùng bằng `processedAppIds` giữ bản nào là ngẫu nhiên. Đề xuất: với mỗi `applicationId`, chỉ nhận khi **bản mới nhất** (`findByApplicationIdOrderByApprovedAtDesc(...).get(0)`) là `Passed`.
2. **Trạng thái hồ sơ:** nên bỏ qua hồ sơ `Rejected` / `Hired` (ví dụ hồ sơ #23 hiện vẫn lọt vào danh sách dù đang `Rejected`). Sau khi mục 3.1a xong thì tự đúng.
3. Trang chi tiết offer có thể thêm link "Xem kết quả phỏng vấn" → `/interviews/{id}/result`.
4. Không ghi `InterviewFinalResult`. `RecommendedSalary` của HM chỉ là gợi ý, lương offer vẫn do HR nhập.

### 3.3. `dashboard` (LinhDN): số liệu đã có ý nghĩa

- `pendingInterviewEvaluations` (thành viên hội đồng có lịch `Completed` mà chưa nộp phiếu) giờ **có số thật**, vì phiếu đầu tiên hoặc lúc chốt kết quả đã chuyển lịch sang `Completed`.
- Có thể thêm cho Hiring Manager một chỉ số "**Chờ chốt kết quả**": lịch `Completed`, thuộc phòng ban mình quản lý, chưa có `InterviewFinalResult`.
  ```sql
  SELECT COUNT(*) FROM InterviewSchedule s
  JOIN Application a ON a.ApplicationId = s.ApplicationId
  JOIN JobPosting jp ON jp.JobPostingId = a.JobPostingId
  JOIN JobRequisition r ON r.RequisitionId = jp.RequisitionId
  WHERE s.InterviewStatus = 'Completed'
    AND r.DepartmentId = :departmentId
    AND NOT EXISTS (SELECT 1 FROM InterviewFinalResult f WHERE f.InterviewId = s.InterviewId)
  ```
  Quy tắc "HM quản lý phòng ban" nên giống `RequisitionAccess.inManagedDepartment`.
- Dashboard ứng viên **không** hiện điểm hay phiếu đánh giá (dữ liệu nội bộ). Nếu cần báo kết quả cho ứng viên thì đi qua trạng thái hồ sơ (mục 3.1) hoặc offer.

### 3.4. `notification`: thông báo khi có phiếu hoặc có kết quả

`NotificationService` chưa có method chung (chỉ có `notifyRequisitionRejected`; nhánh của HoangNH thêm `notifyJobPostingPublished`). Đề xuất người phụ trách thêm **một method chung**, ví dụ:
```java
void notifyUser(User recipient, String eventType, String title, String content, String linkUrl);
```
Đức sẽ gọi ở 2 chỗ:

| Sự kiện | Gửi tới | `linkUrl` |
|---|---|---|
| Có phiếu đánh giá mới | HM phụ trách phòng ban | `/interviews/{id}/result` |
| HM chốt kết quả | HR đã tạo lịch (`InterviewSchedule.CreatedBy`) | `/interviews/{id}/result` |

---

## 4. Việc còn lại của chính module `interview` (Đức)

1. Gọi `candidate` khi tạo lịch và khi chốt kết quả (mục 3.1a), sau khi DungLT có method.
2. Gọi `notification` (mục 3.4), sau khi có method chung.
3. `InterviewSchedule.interviewId` là `Long` nhưng DB là `INT`, làm các test `ddl-auto=validate` của nhóm hỏng. Cần bàn chung trước khi đổi (ảnh hưởng `offer`, `dashboard`).
4. Dữ liệu `InterviewPanel` cũ bị lưu `HM` cho mọi người: cân nhắc script sửa theo role tài khoản.
5. Thẻ lịch ở `/interviews` hiện badge **Đạt / Không đạt** khi đã có kết quả cuối (thêm trường vào `InterviewScheduleResponse` + 1 query).

---

## 5. Không nên làm

- Ghi trực tiếp vào `InterviewEvaluation` / `InterviewFinalResult` từ module khác (bỏ qua kiểm quyền và GBR-01).
- Đổi tên field/kiểu của entity `InterviewFinalResult`, `InterviewEvaluation` (`offer`, `dashboard` và test đang dùng builder và tên field).
- Coi `InterviewStatus = Completed` nghĩa là "đạt". `Completed` chỉ có nghĩa buổi phỏng vấn đã diễn ra; kết quả nằm ở `InterviewFinalResult`.
- So sánh `FinalDecision` / `Recommendation` không đúng hoa thường hoặc bằng giá trị khác (`PASSED`, `NO_HIRE`...): DB có CHECK constraint.
- Để `candidate` import service/event của `interview` (tạo phụ thuộc vòng). Chiều gọi đúng: `interview` → `candidate`.

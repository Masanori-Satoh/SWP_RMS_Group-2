# Đối chiếu Application State Machine với code hiện tại

**Ngày:** 06/11/2026.  
**Đầu vào:** Ảnh Application state machine người dùng gửi trong chat.  
**Checkout:** `feature/update-ui-authen`.  
**Phương pháp:** Đọc code thực thi của Application, Talent Pool, Interview, Offer và đóng tin; không lấy SRS làm chuẩn thay cho code. Đây là review tĩnh, chưa chạy ứng dụng hoặc test. Không chỉnh code, database hay ảnh diagram.

## Kết luận

Diagram **khớp các trạng thái và luồng chính**, nhưng **chưa bao quát đầy đủ transition/guard đang có trong code**. Mười tên trạng thái trong hình khớp danh sách `ApplicationStatusLabels.STATUSES`: Applied, HR_Passed, HM_Suggested, HM_Passed, Interviewing, Offered, Hired, Rejected, Withdrawn, Cancelled. Không cần thêm AI_Screened theo code hiện tại.

Đáng chú ý: **Candidate Declines Offer → Withdrawn là đúng code**, dù một số mô tả SRS/comment cũ có thể ghi Rejected. Căn cứ là lời gọi thực thi `onOfferAnswered(..., false)`, không phải comment trong interface Offer.

## 1. Các nhánh trong hình

Đường dẫn trong bảng tính từ `src/main/java/com/group2/rms/`; số là dòng tại thời điểm review.

| Transition trong hình | Kết luận theo code | Bằng chứng |
| --- | --- | --- |
| Initial → Applied / Candidate submits CV | Khớp; tạo Application mới với Applied khi không có draft | `candidate/application/submission/service/ApplicationSubmissionService.java:31,42–76` |
| Initial → HM_Suggested / Talent Pool Draft | Khớp; draft có thể do HM suggest hoặc HR recommend | `candidate/talentpool/service/TalentPoolService.java:385–451` |
| HM_Suggested → Applied / Candidate submits new CV | Khớp; cập nhật cùng draft, không nhảy thẳng HM_Passed | `ApplicationSubmissionService.java:64–72`; `candidate/domain/repository/ApplicationRepository.java:199–211` |
| HM_Suggested → HM_Passed / HR confirms old CV | Khớp; cần consent, tin mở, CV hợp lệ, chưa có application chính thức và không còn chờ HM phản hồi recommendation | `TalentPoolService.java:625–642,746–755`; `ApplicationRepository.java:189–197` |
| Applied → HR_Passed / HR pass | Khớp | `candidate/application/pipeline/service/ApplicationReviewService.java:54–78`; `candidate/domain/rule/ApplicationAccess.java:25,70–78` |
| Applied → Rejected / HR fail | Khớp | `ApplicationReviewService.java:67–78` |
| HR_Passed → HM_Passed / HM or Director pass | Khớp; HM phải thuộc phạm vi phòng ban, Director có thể duyệt thay | `ApplicationAccess.java:70–78`; `ApplicationReviewService.java:54–78` |
| HR_Passed → Rejected / HM or Director fail | Khớp | Cùng các nhánh review trên |
| HM_Passed → Interviewing / Interview scheduled | Khớp; thêm lịch phỏng vấn gọi cập nhật Application | `interview/service/InterviewSchedulingServiceImpl.java:143–145`; `candidate/domain/rule/ApplicationStatusService.java:69–71` |
| Interviewing → Rejected / Final result fail | Khớp | `interview/service/InterviewFinalResultServiceImpl.java:111–118`; `ApplicationStatusService.java:79–84` |
| Interviewing → Offered / Offer sent | Khớp luồng thông thường; xem lưu ý guard ở mục 4 | `offer/service/OfferServiceImpl.java:616–667` |
| Offered → Hired / Candidate accepts | Khớp, khi Offer còn được phép trả lời | `offer/service/CandidateOfferServiceImpl.java:108–125,141–154`; `ApplicationStatusService.java:87–89` |
| Offered → Withdrawn / Candidate declines offer | Khớp, khi Offer còn được phép trả lời | Cùng các đoạn trả lời Offer trên |
| Applied → Withdrawn / Candidate withdraws | Khớp; chỉ đơn của chính ứng viên và chỉ Applied | `candidate/application/portal/service/MyApplicationService.java:43–44,152–167` |
| In Progress → Cancelled / HR closes job | Khớp cho application chính thức còn hoạt động, nhưng cần guard và ngoại lệ ở mục 2 | `TalentPoolService.java:658–685`; `candidate/talentpool/repository/TalentPoolApplicationRepository.java:56–70` |
| Hired / Rejected / Withdrawn / Cancelled → final | Khớp khái niệm bốn trạng thái kết thúc | `ApplicationStatusService.java:43–50` |

## 2. Cần thêm hoặc chỉnh để diagram đầy đủ hơn

### A. Thiếu HM_Suggested → Rejected

Code có các trường hợp sau cùng chuyển draft từ HM_Suggested sang Rejected:

- HM từ chối recommendation của HR: `TalentPoolService.java:578–599`.
- HR rút recommendation chưa được HM trả lời: dòng 608–621.
- HR ghi nhận Candidate từ chối tham gia: dòng 645–654.
- HR đóng tin khi draft còn HM_Suggested: dòng 670–673.

**Nên thêm mũi tên:** `HM_Suggested → Rejected`, ghi các trigger trên trong note hoặc tách thành các cạnh.

**Không đổi nhánh này sang Withdrawn/Cancelled chỉ để đồng nhất tên:** code hiện lưu Rejected và audit `TalentPoolCancelled`; `ApplicationRepository.FORMAL_FILTER` dòng 30–35 loại các draft này khỏi hồ sơ chính thức. Đây là kết thúc đề xuất nội bộ, không có nghĩa Candidate đã có application chính thức rồi bị công ty loại.

### B. Bổ sung điều kiện cho HR Confirms Old CV

Nhãn hiện tại quá ngắn nếu dùng diagram làm đặc tả đầy đủ. Đề xuất:

`HR confirms candidate consent to use old CV [job open, CV valid, no formal application, not awaiting HM decision]`.

Nếu là HR recommendation, HM phải chấp nhận trước; khi HM chấp nhận thì **Application vẫn HM_Suggested**, chỉ lưu review và audit. Chưa chuyển HM_Passed đến khi HR xác nhận consent. Nếu muốn thể hiện toàn bộ thao tác, thêm self-loop `HM accepts HR recommendation` tại HM_Suggested.

### C. Giới hạn câu “from any in-progress status when HR closes the job”

Code đóng tin có điều kiện và hai cách xử lý:

1. Tin phải Published hoặc Paused; còn Offer mở theo `hasOpenOffers()` thì chặn đóng tin.
2. Draft HM_Suggested → Rejected + audit TalentPoolCancelled.
3. Application chính thức chưa kết thúc → Cancelled; bỏ qua application có Offer Accepted.
4. Hired, Rejected, Withdrawn, Cancelled giữ nguyên.

**Nên sửa note:** `When HR successfully closes the job: active formal applications → Cancelled; talent-pool drafts → Rejected. Closing is blocked while open offers remain.`

`Offered → Cancelled` có thể xảy ra khi Offer gửi cho Candidate đã hết hạn và không còn Offer mở khác chặn đóng tin. Offer đang chờ phản hồi còn hiệu lực chặn thao tác này. `TalentPoolAuditRepository.java:36–53` kiểm tra trạng thái Offer, hạn phản hồi và ngày bắt đầu dự kiến.

### D. Thiếu các thao tác giữ nguyên ApplicationStatus

Nếu hình chỉ mô tả **các lần đổi trạng thái**, có thể đặt chúng trong note thay vì thêm self-loop:

- HR Hold tại Applied: giữ Applied.
- HM/Director Hold tại HR_Passed: giữ HR_Passed.
- HM chấp nhận recommendation của HR: giữ HM_Suggested, chờ HR liên hệ/xác nhận.
- Kết quả phỏng vấn Passed hoặc Considering: thông thường giữ Interviewing. Considering thuộc InterviewFinalResult, không phải một ApplicationStatus mới.
- Tạo, gửi duyệt hoặc Director duyệt/từ chối Offer chưa chuyển Application thành Offered; chỉ hành động gửi Offer cho Candidate mới đổi.

Nguồn: `ApplicationReviewService.java:67–78`; `TalentPoolService.java:588–599`; `InterviewFinalResultServiceImpl.java:111–118`; `OfferServiceImpl.java:643–666`.

## 3. Guard nên ghi thêm, không cần đổi cấu trúc luồng chính

- `Candidate withdraws [own application, status = Applied]`: code không cho rút từ HR_Passed trở đi.
- `Candidate accepts / declines [OfferStatus = Sent_Candidate, response deadline not passed, expected start date not passed]`.
- `Interview scheduled [posting Published, application eligible for interview]`: `ApplicationStatusService.requireScheduleable()` còn kiểm tra giai đoạn suy ra từ interview/offer, không chỉ chuỗi ApplicationStatus.
- `HR pass/fail`, `HM/Director pass/fail` phải được xử lý đúng bước và đúng quyền; không phải mọi role đều đổi được trạng thái.
- Label khởi tạo draft nên viết `HM suggests / HR recommends candidate from Talent Pool` để không khiến người đọc hiểu HM_Suggested chỉ do HM tạo.

## 4. Giới hạn nếu muốn diagram mô tả chính xác mọi đường code

### Nhánh tương thích dữ liệu cũ ở HM_Passed

`ApplicationStatusService.onInterviewDecided()` cho Failed chuyển cả **HM_Passed hoặc Interviewing → Rejected**. Với Passed, HM_Passed có thể được đồng bộ sang Interviewing. Với Considering, lời gọi `onInterviewScheduled()` cũng có thể đồng bộ HM_Passed → Interviewing. Đây là nhánh tương thích cho Application chưa được đồng bộ khi tạo lịch, không phải bằng chứng UI cho chốt kết quả khi chưa có lịch phỏng vấn.

Nếu diagram dành cho luồng nghiệp vụ bình thường, ghi note là đủ. Nếu muốn bao quát implementation, thêm nhánh đồng bộ/fallback này.

### Gửi Offer chưa guard trạng thái nguồn Application tại service

`OfferServiceImpl.sendOfferToCandidate()` kiểm tra Offer đã Approved/Director_Approved và các điều kiện thời hạn; khi gửi thì trực tiếp `application.setApplicationStatus("Offered")`. Phương thức này **không kiểm tra ApplicationStatus hiện tại phải Interviewing**.

Do đó cạnh Interviewing → Offered đúng với luồng UI thông thường, nhưng chưa chứng minh code chỉ cho duy nhất transition này. Không nên dùng riêng diagram để khẳng định mọi chuyển trạng thái trái luồng đã được chặn ở service. Lượt này chỉ ghi nhận giới hạn kiểm tra tĩnh, chưa tái hiện đường gọi làm sai trạng thái và chưa sửa code.

## Đề xuất bản diagram tiếp theo

Giữ mười trạng thái và toàn bộ các cạnh chính hiện có. Bổ sung HM_Suggested → Rejected; sửa note đóng tin; thêm guard consent/Offer; ghi note các thao tác giữ nguyên trạng thái. Nếu tài liệu muốn khớp mọi nhánh implementation, ghi thêm fallback HM_Passed khi chốt phỏng vấn và lưu ý guard gửi Offer.

**Không chạy test/runtime; không chỉnh code hoặc ảnh; không commit/push.**

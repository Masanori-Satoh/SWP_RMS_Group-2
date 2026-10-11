# 2026-10-11 — Kết quả phỏng vấn & Phiếu đánh giá (Iter 2/3, module `interview`)

1. **Người thực hiện:** Đức (DucNM, P4) — nhánh `feature/ducnm/iter2-interview_evaluation`.
2. **Mục tiêu:** Hai màn hình SRS 5.1.28 *Interview Result Details* và 5.1.29 *Submit Evaluation Form (pop-up)*.
3. **Nghiệp vụ chính:**
   - Thành viên hội đồng (Interviewer / Hiring Manager / Director) nộp **một** phiếu cho mỗi buổi: 3 điểm 1–5, đề xuất `Hire` / `Consider` / `No_Hire`, điểm mạnh, điểm yếu, nhận xét. Chỉ nộp được khi đã tới giờ bắt đầu và lịch chưa bị hủy.
   - **Phiếu đầu tiên chuyển lịch sang `Completed`** (GBR-01, chỉ đi tiến). Nhờ vậy chỉ số "chờ đánh giá" trên dashboard đếm đúng những thành viên còn lại.
   - HR, Hiring Manager, Director, System Admin xem mọi phiếu, điểm trung bình và phân bố đề xuất. Interviewer chỉ mở được lịch mình được gán và chỉ thấy phiếu của mình cho tới khi có kết quả cuối (GBR-05).
4. **Mã nguồn:**
   - Mới: `InterviewResultController` (`GET /interviews/{id}/result`), `InterviewEvaluationController` (`POST /interviews/{id}/evaluations`, JSON + CSRF), `InterviewResultService`, `InterviewEvaluationService`, `InterviewEvaluationRepository`, DTO + exception, `templates/interview/result.html`, `templates/interview/evaluation-dialog.html`, `css/pages/interview-result.css`, `js/pages/interview-evaluation.js`.
   - Sửa (chỉ thêm): `SecurityConfig` (+1 rule POST cho Interviewer/HM/Director), `InterviewFinalResultRepository` (+1 query), `interview/list.html` (+link "Kết quả").
   - Không đổi schema, không sửa `GlobalExceptionHandler`.
5. **Kiểm tra:** 30 test mới pass (service, validation, web + render template). Toàn bộ suite: số test hỏng giữ nguyên như `main` (13 failure + 9 error có sẵn, liên quan DB thật và chuỗi tiếng Việt). Chưa kiểm tra trên trình duyệt.
6. **Bổ sung cùng ngày (sửa lỗi khi chạy thử):**
   - HR: form "Lên lịch phỏng vấn mới" mở từ trang hồ sơ giờ chọn sẵn đúng hồ sơ (`?applicationId=`).
   - Hiring Manager phụ trách phòng ban có nút **Chốt kết quả** (Đạt/Không đạt, lương đề xuất, nhận xét) trên trang kết quả, `POST /interviews/{id}/final-result`. Chốt được khi chưa đủ phiếu, chốt xong không sửa. Kết quả Đạt đi thẳng vào danh sách tạo offer của HR.
   - Candidate: menu "Lịch phỏng vấn của tôi" (`/portal/interviews`) trước đây 404 vì thiếu controller; nay có trang riêng chỉ hiện lịch của chính ứng viên.
7. **Tồn đọng:** Notifications 5.1.39 chưa làm (thiếu API chung trong `notification`); các nút cũ trên `interview/list.html` chưa có style (trang chưa migrate layout); `InterviewSchedule.interviewId` là `Long` nhưng DB là `INT`. Chi tiết: [`docs/members/duc/iter2-3-worklog.md`](../../members/duc/iter2-3-worklog.md).

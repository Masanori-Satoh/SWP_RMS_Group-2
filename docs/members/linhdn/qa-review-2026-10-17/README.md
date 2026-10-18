# Test Plan và danh sách kiểm thử RMS của Linh

Ngày lập: 17/10/2026. Trạng thái: bản thiết kế để người kiểm thử tự thực hiện.

Chưa chạy test trong lượt rà soát này. Actual Result để trống, Status là Pending.
SRS hiện tại đã được chủ dự án xác nhận chưa cập nhật; Expected Result sẽ truy vết đến code hiện tại và quyết định nghiệp vụ mới nhất trong tài liệu dự án.

## Mở tài liệu

1. [64 Integration Test case chi tiết](INTEGRATION_TEST_CASES.md) — danh sách chính để Linh xem xét: đầy đủ ID, loại test, tình huống, điều kiện, dữ liệu, bước làm, expected, actual trống, Pending, ưu tiên và truy vết.
2. [Test Plan](TEST_PLAN.md) — hiện trạng pom/config/test, phạm vi Linh và điểm nối team, GAP SRS cũ, phân tầng unit/integration/system, lớp/method cần test và ưu tiên rủi ro.
3. [8 System Test thủ công](SYSTEM_TEST_MANUAL.md) — thao tác website theo role bằng các session riêng; có thể dùng trực tiếp khi tự nghiệm thu.
4. [Ghi chú nghiên cứu nguồn](RESEARCH_SOURCES.md) — Spring Petclinic, Spring Guide Testing Web, Testcontainers Java và ISTQB CTFL v4.0.1; chỉ áp dụng cấu trúc phù hợp Boot 3.5.16 hiện tại.
5. [Catalog dữ liệu](integration-cases.json) — dữ liệu thiết kế có cấu trúc để chuyển sang mẫu báo cáo khác sau này; kết quả thực thi quản lý ở bản test case, chưa có dữ liệu Passed/Failed.

## Những luồng cần tự test

| Nhóm | ID | Số ca | Trọng tâm |
|---|---|---:|---|
| Authentication | IT-A01–A12 | 12 | Login username/email, đăng ký OTP, chống trùng, hạn/sai OTP, reset, snapshot, logout |
| Authorization/session | IT-S01–S06 | 6 | Ma trận role, scope phòng ban, session sau khóa/đổi role, CSRF, không lộ note/CV |
| Candidate Account | IT-C01–C05 | 5 | Tìm/lọc, activate/deactivate, Blocked, tách internal/Candidate, transaction User/profile |
| Application | IT-P01–P09 | 9 | Upload PDF, hạn job, duplicate/race, file rollback, HR/HM review, AI lỗi, rút đơn |
| Talent Pool và HM Suggested | IT-T01–T17 | 17 | Bookmark/note/CV, selection, HM → HR consent, HR → HM decision, CV mới, thu hồi, batch/race, Interview bookmark |
| Closure và email | IT-E01–E05 | 5 | Offer chặn close, audit dừng đợt, bảo vệ Accepted offer, SMTP sau commit, invite/resend |
| Requisition Approval liên phân hệ | IT-R01–R06 | 6 | Submit/approve/reject, self-approval, withdraw, concurrency, Approved → Job Posting |
| Admin và hiển thị số liệu | IT-M01–M04 | 4 | Internal account, Department, dashboard theo role, pipeline query/detail/CV |
| **Tổng Integration Test** | | **64** | **Chưa thực hiện; mỗi biến thể cần kết quả riêng** |

Ưu tiên trước: quyền/session/reset, proposal → formal application, chống trùng và transaction, closure/offer, phê duyệt đúng actor. Các ca race/after-commit không thể chứng minh chỉ bằng click website.

## Giới hạn của lần lập kế hoạch

Chỉ đọc code, test source, config, SRS và tài liệu; tra cứu nguồn chính thức; tạo bộ tài liệu trong thư mục này. Không chạy Maven/Node test, không start app, không truy vấn DB, không sửa source/schema/pom/config/SRS. Kết quả test cũ trong worklog không được dùng để đánh dấu kết quả mới.

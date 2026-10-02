# 2026-09-29 — Đồng bộ project theo database mới

**Yêu cầu và quyết định:** `database/schema/db.sql` là source of truth. Tạo DB
mới, không chuyển dữ liệu cũ. Một Application được phép có nhiều lần chấm AI
để lưu lịch sử. Không tự sửa schema khi nghiệp vụ chưa rõ.

**Database đã đọc:** `database/schema/db.sql` có 20 bảng SQL Server.
`database/seeds/seed_data.sql` và `database/seeds/build_seed.py` đã được rà tên
bảng/cột. Tại thời điểm kiểm tra, `RitirementManagement2` đã tồn tại với 20
bảng và khoảng 1.846 dòng seed. Không chạy script xóa/tạo DB và không chạy lại
seed. Cấu hình local bị Git ignore trỏ tới DB này với `ddl-auto=validate`.

| File database | Trong lượt đồng bộ schema đã làm gì? |
| --- | --- |
| `database/schema/db.sql` | Chỉ đọc; **không sửa, không chạy lại**. Đầu file có lệnh xóa DB đích. |
| `database/seeds/seed_data.sql` | Chỉ đọc/đối chiếu tĩnh; **không sửa, không chạy lại**. |
| `database/seeds/build_seed.py` | Chỉ đọc; **không sửa, không chạy lại**. |
| `database/migrations/001_candidate_account_link.sql` | Thêm chú thích đầu file: migration này chỉ dành cho schema snake_case cũ; **không chạy** trên DB mới. File đã tồn tại từ mốc trước ở trạng thái Git chưa theo dõi. |

**Mã đã đổi:** Các Entity khác schema được chỉnh tối thiểu; trọng tâm là
`Candidate.UserId` bắt buộc/duy nhất, bỏ cột liên hệ trùng, `ApplicationReview`
đúng bảng review, `AIScreeningResult` nhiều–một với Application, ngày đăng tin
dùng `LocalDateTime`, trường Offer/trạng thái/độ dài cột. Thêm
`SchemaNamingConfig` để Hibernate giữ tên PascalCase và quote bảng `User`.
Account form/service và Dashboard query được chỉnh theo các mapping mới.
Chi tiết từng file, dependency và NEED CONFIRMATION nằm trong
`docs/architecture/schema-migration-impact.md`.

**Đã xác minh:** `mvn test` có 28 tests PASS, 0 failure/error/skip;
`mvn -DskipTests package` PASS và tạo JAR. `RmsApplicationTests` khởi động
Spring Context; Hibernate `validate` qua toàn bộ mapping với DB mới. Test DB
đã ghi hai kết quả AI cho một Application trong transaction rồi rollback.
Tìm tên field cũ trong `src/main` và `src/test` không còn kết quả. Chưa xác nhận
HTTP/browser end-to-end trên server thật; xem `docs/management/TEST_PLAN.md` để chạy tay.

**Còn chờ nghiệp vụ:** `InterviewEvaluation` không có unique theo
`(InterviewId, InterviewerId)` và không ràng buộc thành viên `InterviewPanel`.
Cần xác nhận có cho phép nhiều phiếu hoặc người ngoài panel không trước khi
thay đổi schema/luồng nộp phiếu. Cũng cần quyết định quy tắc khi Admin đổi role
của User đã có Candidate profile; hiện code giữ profile. AI Configuration và
health check AI/email chưa có contract được duyệt, không tự tạo form/probe.

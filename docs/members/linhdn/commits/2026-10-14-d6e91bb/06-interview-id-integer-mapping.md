# Commit 06 — Ánh xạ InterviewId đúng SQL INT

Ngày: 14/10/2026. Tách từ `d6e91bb`.

## File và thay đổi

- `src/main/java/com/group2/rms/interview/entity/InterviewSchedule.java`: thêm JdbcTypeCode(SqlTypes.INTEGER) trên interviewId.
- `src/main/java/com/group2/rms/interview/entity/InterviewPanelId.java`: thêm cùng mapping cho phần interviewId của khóa ghép.

## Lý do

database/schema/db.sql quy định InterviewId là INT. Giữ Java Long để các API của module Interview không bị thay đổi; Hibernate đọc/bind khóa này bằng JDBC INTEGER. Các FK kế thừa kiểu của khóa được tham chiếu.

Hai file nằm cùng commit để khóa chính và khóa ghép nhất quán. Không sửa database, schema, FK, constraint hoặc entity relationship.

## Kiểm tra

1. Khởi động Context với spring.jpa.hibernate.ddl-auto=validate.
2. Xác nhận EntityManagerFactory dựng thành công.
3. Không còn lỗi SQL INT/JPA BIGINT hoặc Hibernate yêu cầu ALTER InterviewId BIGINT.
4. Chạy query Pipeline, My Applications và Candidate Offer.

Bằng chứng trên snapshot hoàn chỉnh: Context/JPA/query PASS; toàn bộ test vẫn có lỗi khác được ghi trong tài liệu 07.

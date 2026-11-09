# Giới hạn tìm kiếm 150 ký tự trong Kho ứng viên tiềm năng

Ô “Họ tên hoặc email” trên `/talent-pool` chấp nhận tối đa 150 ký tự, tương ứng với độ dài email trong dữ liệu người dùng. Dịch vụ tìm kiếm giữ đủ từ khóa 150 ký tự khi gửi truy vấn. Từ khóa dài hơn được cắt còn 150 ký tự nếu truyền trực tiếp qua URL.

Để đối chiếu, dán chuỗi 151 ký tự vào ô tìm kiếm và kiểm tra trình duyệt giữ tối đa 150 ký tự. Gửi tìm kiếm với email dài 150 ký tự có trong kho để đối chiếu kết quả theo email đầy đủ.

[Nhật ký triển khai và kiểm chứng](../../management/work_logs/2026-11-08-talent-pool-search-max-length.md)

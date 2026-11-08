# Số ứng viên tiềm năng trên chi tiết tin tuyển dụng

Trang chi tiết tin hiển thị số ứng viên ở cạnh nút mở danh sách đề xuất, kể cả khi số lượng là `0`. Bộ đếm tính mỗi ứng viên một lần trong tin đang xem.

| Trường hợp | Tính vào số ứng viên tiềm năng |
| --- | --- |
| Trưởng bộ phận đề xuất trực tiếp và bản đề xuất còn mở | Có |
| Nhân sự gửi gợi ý, Trưởng bộ phận đã duyệt và bản đề xuất còn mở | Có |
| Gợi ý của Nhân sự đang chờ Trưởng bộ phận duyệt | Đếm ở mục “Gợi ý từ HR” |
| Đề xuất đã bị từ chối, rút hoặc chuyển thành hồ sơ chính thức | Không |

Để đối chiếu, mở chi tiết một tin với vai trò Trưởng bộ phận. Sau khi duyệt một gợi ý từ HR, quay lại chi tiết tin. Ứng viên xuất hiện trong danh sách đề xuất và được tính vào badge của mục “Ứng viên tiềm năng”.

[Nhật ký triển khai và kiểm chứng](../../management/work_logs/2026-11-08-job-posting-suggested-candidate-count.md)

# Nhật ký đối chiếu kiến trúc hệ thống RMS

- **Ngày thực hiện** 08/11/2026.
- **Phạm vi** So sánh sơ đồ *Software Architecture* trong ảnh với nhánh `feature/update-ui-authen` tại `ca8f1779`. Kiểm tra phiên bản, các tầng web và dữ liệu, lưu tệp, gửi mail, job định kỳ và kênh thông báo.
- **Kết quả** Khung ba tầng khớp. Đề xuất sửa phiên bản Spring Boot, thay Cloudinary bằng lưu tệp cục bộ và bỏ Compliance Scheduler. Ghi rõ job Offer là tùy chọn. Bổ sung JSON, WebSocket/STOMP và JDBC để phản ánh các luồng có trong mã nguồn.
- **Kiểm chứng** Đọc `pom.xml`, cấu hình ứng dụng và các lớp MVC, bảo mật, dữ liệu, lưu tệp, gửi thư, thông báo và job Offer. Phạm vi kiểm chứng là đối chiếu tĩnh.
- **Tài liệu** [Báo cáo đối chiếu và sơ đồ đề xuất](../../members/linhdn/2026-11-08-system-architecture-code-review.md).
- **Đối chiếu bản ảnh cập nhật** Xác nhận ba thay đổi về phiên bản, Cloudinary và Compliance Scheduler. Ghi nhận hai điểm còn lệch là vị trí Local File Storage trong External Services và vị trí nhãn Spring Data JPA / Hibernate ở Data Tier. Kiểm tra lại trên cùng commit `ca8f1779`.

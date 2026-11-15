# Sơ đồ kiến trúc tổng thể RMS

[Sơ đồ chỉnh sửa được trong draw.io](../../architecture/RMS_High_Level_Design.drawio) dùng bố cục dọc của mẫu, với trình duyệt và người dùng phía trên, ứng dụng Spring Boot ở giữa, SQL Server phía dưới và SMTP ở bên phải.

- Spring Security tiếp nhận yêu cầu HTTP và áp dụng phiên đăng nhập, CSRF và phân quyền. Spring MVC điều phối nghiệp vụ, dùng Thymeleaf để render trang HTML và trả JSON ở các endpoint phù hợp.
- Kênh STOMP qua WebSocket tại `/ws` phục vụ thông báo theo người dùng.
- Các dịch vụ truy cập SQL Server qua Spring Data JPA/Hibernate hoặc JDBC. CV, ảnh đại diện và PDF chính sách được lưu trên hệ thống tệp của máy chủ ứng dụng. Email được gửi qua `JavaMailSender` tới máy chủ SMTP.

Đối chiếu với `pom.xml`, `SecurityConfig`, `WebSocketConfig`, `NotificationPushListener`, các lớp lưu tệp, repository và cấu hình tại commit `bafdf65d`. XML đã được đọc lại và kiểm tra ID, liên kết và hình học. Chưa kiểm tra hiển thị trong draw.io.

Bản cập nhật dùng nền trắng, chữ và đường nét đen. Chỉ khối Web Browser dùng màu xanh. Đã đọc lại XML và kiểm tra màu của 32 phần tử.

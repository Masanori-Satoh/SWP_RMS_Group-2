# Recruitment Management System (RMS) - Group 2 (SE2064)

Hệ thống Quản lý Tuyển dụng (RMS) được xây dựng trên nền tảng **Spring Boot 3.5.16**, **Thymeleaf**, **Spring Security**, và **Microsoft SQL Server**.

* **Database hiện hành (Source of Truth):** [`database/schema/db.sql`](database/schema/db.sql)
* **Nhật ký tiến độ dự án:** [docs/management/WORK_LOG.md](docs/management/WORK_LOG.md)
* **Quy chuẩn kiến trúc & Lập trình:** [docs/architecture/ARCHITECTURE_GUIDE.md](docs/architecture/ARCHITECTURE_GUIDE.md)

---

## 📚 TRUNG TÂM TÀI LIỆU DỰ ÁN (DOCS HUB)

Vui lòng tham khảo các tài liệu chuyên đề trước khi bắt tay vào code hoặc kiểm thử:
* 🏗️ **[Quy chuẩn Kiến trúc (Architecture Guide)](docs/architecture/ARCHITECTURE_GUIDE.md):** Cấu trúc Package-by-Feature, quy chuẩn đặt tên DTO, xử lý ngoại lệ toàn cục `GlobalExceptionHandler`, kiến trúc giao diện (Layout + Sidebar theo role + File nội dung, fragment dùng chung, bản đồ CSS/JS), CSRF, Unicode.
* 🎨 **[Bộ tài liệu Giao diện (UI)](docs/architecture/UI/README.md):** Lộ trình làm giao diện, luật & vùng không được chạm, tra cứu class/token/fragment, hướng dẫn tạo trang mới, migrate trang cũ, tổ chức CSS và dọn file cũ.
* 🕒 **[Nhật ký công việc (Work Log)](docs/management/WORK_LOG.md):** Lịch sử thay đổi từng ngày của team, theo dõi module nào vừa được cập nhật qua các PR.
* 🗄️ **[Thiết kế Cơ sở Dữ liệu](docs/database/model.md):** Sơ đồ quan hệ thực thể (ERD), bảng danh mục và ý nghĩa các trường.
* 🌱 **[Hướng dẫn Seeding Dữ liệu](docs/database/data_seeding_guide.md):** Hướng dẫn nạp dữ liệu mẫu ban đầu để kiểm thử hệ thống.
* ⚠️ **[Đánh giá Tác động Schema](docs/architecture/schema-migration-impact.md):** Báo cáo ảnh hưởng khi thay đổi cấu trúc bảng.
* 📂 **[Hướng dẫn Tổ chức Thư mục Docs](docs/README.md):** Quy định lưu trữ tài liệu chung và tài liệu cá nhân của từng thành viên.

---

## 🚀 1. YÊU CẦU MÔI TRƯỜNG

Các phần mềm **BẮT BUỘC** cài đặt trên máy lập trình viên:

1. **JDK 21 LTS** ([Tải Oracle JDK 21](https://download.oracle.com/java/21/archive/jdk-21.0.12_windows-x64_bin.exe))
   * Nhớ chọn "Add to PATH" và đặt biến môi trường `JAVA_HOME`.
2. **Apache Maven 3.9+** ([Tải Apache Maven](https://dlcdn.apache.org/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.zip))
   * Thêm thư mục `bin` của Maven vào biến môi trường `PATH`.
3. **Microsoft SQL Server & SSMS**
   * Bật xác thực "Mixed Mode Authentication" (SQL Server and Windows Authentication).
   * Đặt mật khẩu cho tài khoản quản trị `sa`.
4. **IDE khuyến nghị:** IntelliJ IDEA (hoặc VS Code với Extension Pack for Java).

---

## 🛠️ 2. HƯỚNG DẪN KHỞI TẠO DỰ ÁN (SETUP)

Thực hiện lần lượt các bước sau khi `git clone`:

### 2.1 Cấu hình IDE (VS Code nếu có dùng)
* Trong thư mục `.vscode/`, sao chép file `settings.example.json` thành `settings.json`.
* Kiểm tra đường dẫn JDK 21 và Maven trỏ đúng thư mục cài đặt trên máy.

### 2.2 Tạo Database
1. Mở file [`database/schema/db.sql`](database/schema/db.sql) bằng SSMS.
2. Thực thi toàn bộ script để tạo database `RitirementManagement2`.
   > ⚠️ **Lưu ý:** Script này sẽ xóa và tạo mới database. Không chạy lại nếu DB đang có dữ liệu cần giữ.
3. Nếu cần dữ liệu mẫu thử nghiệm, mở và chạy file [`database/seeds/seed_data.sql`](database/seeds/seed_data.sql).

### 2.3 Cấu hình Môi trường Local (`application-local.properties`)
Dự án áp dụng cơ chế cấu hình tách biệt để **không bao giờ lộ mật khẩu cá nhân lên Git**:
1. Trong thư mục `src/main/resources/`, sao chép file:
   `application-local.properties.example` ➡️ thành ➡️ `application-local.properties`
2. Mở file `application-local.properties` và điền thông tin tài khoản SQL Server cá nhân:
   ```properties
   spring.datasource.username=sa
   spring.datasource.password=mat_khau_cua_ban
   server.port=8082
   ```
   *(File `application-local.properties` đã được cấu hình trong `.gitignore`, tuyệt đối an toàn).*

### 2.4 Đồng bộ Dependencies (Maven Sync)
* Mở dự án bằng IDE, chọn **Reload / Sync Maven Project**.
* Chờ 1–3 phút để tải toàn bộ thư viện cần thiết. Khi hoàn tất, các thông báo lỗi đỏ sẽ biến mất.

---

## ▶️ 3. KHỞI CHẠY DỰ ÁN

Mở Terminal tại thư mục gốc của dự án và chọn một trong hai cách:

### Cách 1: Sử dụng Maven cục bộ (Khuyên dùng)
```bash
mvn clean compile spring-boot:run
```

### Cách 2: Sử dụng Maven Wrapper
```powershell
.\mvnw.cmd spring-boot:run
```

Khi ứng dụng khởi động thành công, truy cập trình duyệt tại:
* Trang giới thiệu tuyển dụng (Landing): `http://localhost:8082/`
* Danh sách vị trí đang tuyển (Jobs Board): `http://localhost:8082/jobs`
* Đăng nhập (ứng viên & nội bộ): `http://localhost:8082/login`

---

## 🔍 4. KIỂM THỬ HỆ THỐNG

Chạy toàn bộ bộ kiểm thử tự động của dự án:
```bash
mvn test
```
Đảm bảo tất cả các test suite của các module (`CareerFlowTests`, `SecurityFlowTests`, `AccountManagementServiceTests`, `InterviewSchedulingServiceTests`, `OfferServiceTests`,...) đều đạt kết quả PASS (xanh).

*Mọi thắc mắc kỹ thuật hoặc xung đột mã nguồn, vui lòng trao đổi trực tiếp với Core Team / Leader!*

# Recruitment Management System (RMS) - Group 2 (SE2064)

Dự án Hệ thống Quản lý Tuyển dụng (RMS) sử dụng **Spring Boot 3.5.16**, **Thymeleaf**, và **MS SQL Server**.

Database hiện hành: [`database/schema/db.sql`](database/schema/db.sql).
Xem [nhật ký công việc](docs/management/WORK_LOG.md) để tiếp tục dự án và
[kịch bản kiểm thử từng bước](docs/management/TEST_PLAN.md) trước khi xác nhận tính năng.
Kết quả theo từng đợt được ghi tại [thư mục tests](docs/tests/README.md).

**📚 TÀI LIỆU DỰ ÁN (DOCS HUB):**
- 🕒 **[Nhật ký công việc (Work Log)](docs/management/WORK_LOG.md):** Xem lịch sử thay đổi mỗi ngày, hôm nay ai làm gì, module nào được cập nhật.
- 🏗️ **[Quy chuẩn Kiến trúc](docs/architecture/ARCHITECTURE_GUIDE.md):** Hướng dẫn cấu trúc code (Package-by-Feature), Naming Convention (thể hiện trạng thái hệ thống hiện tại).
- 🗄️ **[Tài liệu Database](docs/database/model.md):** Cấu trúc DB và hướng dẫn [Seeding dữ liệu](docs/database/data_seeding_guide.md).
- 🧪 **[Kế hoạch Kiểm thử](docs/management/TEST_PLAN.md):** Các bước test hệ thống.
- ⚠️ **[Tác động Schema](docs/architecture/schema-migration-impact.md):** Ghi chú các ảnh hưởng khi cập nhật DB.

---

## 🚀 1. YÊU CẦU CÀI ĐẶT

Các phần mềm **BẮT BUỘC** cài đặt:

1. **JDK 21 LTS** ([Tải tại đây](https://download.oracle.com/java/21/archive/jdk-21.0.12_windows-x64_bin.exe))
   - Khuyên dùng: Oracle JDK 21. Nhớ tick "Add to PATH" và "Set JAVA_HOME".
2. **Apache Maven 3.9+** ([Tải tại đây](https://dlcdn.apache.org/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.zip))
   - Giải nén và thêm thư mục `bin` vào biến môi trường `PATH` thành "MAVEN_HOME".
3. **Microsoft SQL Server & SSMS**
   - Bật "Mixed Mode Authentication" và đặt mật khẩu tài khoản `sa`.
4. **IDE**: IntelliJ IDEA hoặc Visual Studio Code (kèm Extension Pack for Java).

---

## 🛠️ 2. HƯỚNG DẪN SETUP

Thực hiện lần lượt các bước sau khi `git clone`:

### 2.1 Cấu hình VS Code (Nếu dùng)
1. Trong `.vscode/`, đổi tên `settings.example.json` thành `settings.json`.
2. Cập nhật đường dẫn **JDK 21** và **Maven** của máy theo mẫu trong file settings.json (lưu ý dùng dùng `\\` trên Windows vì // gây lỗi).

### 2.2 Tạo Database & Cấu hình
**`database/schema/db.sql` là source of truth. Hibernate chỉ kiểm tra schema,
không tự tạo bảng.**

1. Nếu cần tạo DB mới từ đầu, đọc toàn bộ `database/schema/db.sql` trước khi
   chạy bằng SSMS. Script này **xóa rồi tạo lại** `RitirementManagement2`, nên
   tuyệt đối không chạy trên DB có dữ liệu cần giữ. Không chuyển dữ liệu từ DB cũ.
2. Nếu cần dữ liệu mẫu, xem `database/seeds/seed_data.sql` và
   `docs/database/data_seeding_guide.md`. Không chạy seed lặp trên DB đã có seed.
3. Tạo/cập nhật `src/main/resources/application.properties` trên máy theo cấu
   hình nội bộ của nhóm. JDBC URL phải trỏ `RitirementManagement2` và
   `spring.jpa.hibernate.ddl-auto=validate`; giữ credential ngoài Git. Chọn
   `server.port` phù hợp (cấu hình local đã kiểm tra dùng `8082`).

### 2.3 Cấu hình API Key (Có thể bỏ qua)
- Ở thư mục gốc, đổi tên `.env.example` thành `.env`. Điền API Key nếu có.

### 2.4 Load Maven (Quan trọng)
- Khi mở dự án, IDE sẽ hỏi Load/Sync Maven. Chọn **Yes / Import / Load**.
- Đợi 2-5 phút tải thư viện. Khi xong, lỗi đỏ ở các file Java sẽ tự biến mất.

*(Lưu ý: Các file `settings.json`, `application.properties`, `.env` đã được ignore nên không lo commit nhầm).*

---

## ▶️ 3. CÁCH CHẠY DỰ ÁN

Mở Terminal ở thư mục gốc và chọn 1 trong 2 cách:

### Cách 1: Dùng Maven đã cài (Khuyên dùng)
```bash
mvn clean compile spring-boot:run
```
👉 *Giúp IDE gợi ý code nhanh, chuẩn xác hơn.*

### Cách 2: Dùng Maven Wrapper (Nhanh gọn)
```bash
.\mvnw.cmd spring-boot:run
```
👉 *Không cần cài Maven máy tính, script tự tải bản ảo.*

---

## 🔍 4. KIỂM TRA

Chạy `mvn test`, sau đó làm theo [TEST_PLAN.md](docs/management/TEST_PLAN.md) để kiểm tra
login, Dashboard theo role, Account Management và API Monitoring. Mở trang chủ
theo cổng `server.port` đã cấu hình để bắt đầu luồng đăng nhập. Các route demo
`/test-web` và `/test-db` không thay thế kiểm thử tính năng hoặc JPA mapping.

*Vướng mắc gì liên hệ Leader nhé!*


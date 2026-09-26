# Recruitment Management System (RMS) - Group 2 (SE2064)

Dự án Hệ thống Quản lý Tuyển dụng (RMS) sử dụng **Spring Boot 3.5.16**, **Thymeleaf**, và **MS SQL Server**.

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
**⚠️ BẮT BUỘC: Phải tạo Database trống trước, nếu không app sẽ lỗi khi khởi động!**

1. Mở SSMS, tạo Database mới tên: **`RMS_DB`**. (Bảng sẽ do code tự sinh).
2. Trong `src/main/resources/`, đổi tên `application.properties.example` thành `application.properties`.
3. Mở `application.properties`:
   - Sửa `spring.datasource.password=` thành mật khẩu SQL Server của bạn.
   - Đổi `server.port=8080` thành cổng khác (vd `8081`) nếu bị lỗi trùng port.

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

Vào các link sau (nhớ đổi `8080` nếu bạn đã sửa port):
- **Giao diện:** http://localhost:8080/test-web
- **Test Database:** http://localhost:8080/test-db
- **Test Login:** http://localhost:8080/login

*Vướng mắc gì liên hệ Leader nhé!*

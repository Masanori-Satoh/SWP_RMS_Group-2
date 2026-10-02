# Khởi động ứng dụng và kiểm tra mapping theo schema mới

**Phạm vi:** Flow kỹ thuật của migration: schema `database/schema/db.sql` đã tồn tại; ứng dụng chỉ `validate`, không tự tạo/sửa DB. File SQL có lệnh drop/create database nên **không chạy script chỉ để test flow này**. `@SpringBootTest` và JPA validation PASS trong lượt này; HTTP server startup FAIL trên cổng 8082 vì đang được dùng, trên 18082 vì `Unable to establish loopback connection` / `Invalid argument: connect`. Maven plugin trả exit 0 ở lần lỗi 18082; phải đọc log, không chỉ exit code.

## End-to-End Execution Flow

```text
mvn spring-boot:run / java -jar → [PROJECT CODE] RmsApplication.main()
 → [SPRING FRAMEWORK] SpringApplication.run()
 → đọc application.properties và bean SchemaNamingConfig.schemaPhysicalNamingStrategy()
 → DataSource SQL Server database RitirementManagement2
 → JPA/Hibernate đọc Entity mapping + ddl-auto=validate
 → so metadata bảng/cột/FK mapping với schema hiện hữu
 → nếu hợp lệ: ApplicationContext và web server khởi động
 → nếu lệch: startup fail, log SchemaManagementException / BeanCreationException
```

Các method nội bộ Hibernate của schema validation và SQL metadata queries chính xác: **Not verified at source-code level**. Cấu hình được đối chiếu từ file project; không suy diễn startup PASS từ việc code compile.

## Detailed Execution Trace

| Step | Loại; class.method; package/file | Input → output → next |
|---|---|---|
| 1 | [PROJECT CODE] `RmsApplication.main(String[])`; `com.group2.rms/RmsApplication.java` | Lệnh chạy → `SpringApplication.run(RmsApplication.class,args)`. |
| 2 | [SPRING FRAMEWORK] Spring Boot config loading từ `src/main/resources/application.properties` | URL SQL Server, username/password runtime, `ddl-auto=validate`, dialect/nationalized strings/port → DataSource và JPA setup. Không ghi credential vào docs/log. |
| 3 | [PROJECT CODE] `SchemaNamingConfig.schemaPhysicalNamingStrategy()`; `com.group2.rms.config` | `HibernatePropertiesCustomizer` đặt `PhysicalNamingStrategyStandardImpl` để giữ PascalCase; `User` entity quote reserved table name. |
| 4 | [SPRING FRAMEWORK] Hibernate mapping/validation | Entities `com.group2.rms.entity` được đối chiếu metadata DB hiện hữu; match → context tiếp tục, mismatch → fail startup. Method nội bộ và truy vấn metadata **Not verified at source-code level**. |
| 5 | [PROJECT CODE] `RmsApplicationTests.contextLoads()`; `com.group2.rms` (test) | Khi chạy test có DB: kiểm tra context khởi động. Test rỗng không tự chứng minh HTTP/browser. |

## Failure / Alternative Flows

- DB chưa tạo/không kết nối được: DataSource/JPA startup fail; không tự tạo bảng.
- Entity sai tên/kiểu cột so với schema: Hibernate validate fail, app không sẵn sàng nhận HTTP. Error thực tế phải lấy từ log lần chạy tương ứng.
- `mvn compile` PASS chỉ xác nhận Java biên dịch, không xác nhận mapping hay server nhận HTTP.

## Database Interaction

| Order | Repository Method | Entity | Table | Operation | Purpose |
|---|---|---|---|---|---|
| 1 | Không có repository của project | Tất cả mapped entities | 20 bảng theo schema | Metadata/read-only từ Hibernate | Validate mappings. |

Không INSERT/UPDATE/DELETE. `database/schema/db.sql` là nguồn chuẩn, không được ứng dụng chạy tự động trong flow này.

## Data Transformation

Entity annotations + naming strategy + properties → Hibernate mapping metadata → đối chiếu SQL Server schema metadata → Spring Context/web startup hoặc exception. Không có HTTP DTO/controller/template ở startup flow này.

## External Test Cases

| ID | Scenario | Preconditions | Action | Input | Expected HTTP | Expected Result | DB Change |
|---|---|---|---|---|---|---|---|
| SCHEMA-01 | Schema đúng | DB test theo `db.sql` | Start app | Runtime properties | GET `/login` 200 sau startup | Context/JPA/web hoạt động | Không |
| SCHEMA-02 | DB unavailable | DB test dừng hoặc URL sai trong config test | Start app | Config test | Không có HTTP | Log connection/startup fail thật | Không |
| SCHEMA-03 | Mapping regression | Chỉ test branch/fixture schema sai | Chạy context test | Entity/schema mismatch | Không có HTTP | Hibernate validate fail | Không |

## Test Procedure

SCHEMA-01: đảm bảo DB test đã tạo từ schema mới (không chạy lại script drop/create), chạy `mvn.cmd test` và `mvn.cmd spring-boot:run`, đợi log startup thành công, dùng browser mở `/login`, xác nhận HTTP 200. SCHEMA-02: chỉ trong môi trường test, ngắt DB hoặc đổi URL test, chạy lại app, ghi lỗi thật rồi khôi phục cấu hình. SCHEMA-03: dùng DB fixture cô lập không đúng schema hoặc test mapping, không sửa DB chung. Ghi build/context/HTTP thành ba kết quả riêng.

## Test Data

SQL Server test có database `RitirementManagement2` đúng 20 bảng. Credentials chỉ đặt ngoài tài liệu. Không cần User record để GET `/login` nhưng cần seeder/fixtures riêng cho test auth.

## Debugging Points

| Order | Class | Method | Why |
|---|---|---|---|
| 1 | `RmsApplication` [PROJECT CODE] | `main` | Entry startup. |
| 2 | `SchemaNamingConfig` [PROJECT CODE] | `schemaPhysicalNamingStrategy` | Naming PascalCase. |
| 3 | Hibernate schema validation [SPRING FRAMEWORK] | **Not verified at source-code level** | Tên bảng/cột sai. |
| 4 | `RmsApplicationTests` [PROJECT CODE] | `contextLoads` | Context test. |

## Code References

`database/schema/db.sql`; `src/main/java/com/group2/rms/RmsApplication.java`; `src/main/java/com/group2/rms/config/SchemaNamingConfig.java`; `src/main/java/com/group2/rms/entity/`; `src/main/resources/application.properties`; `src/test/java/com/group2/rms/RmsApplicationTests.java`; `pom.xml`.

## Flow Completion Checklist

- [x] Entry point identified
- [x] Request URL identified (N/A: entry là lệnh khởi động; GET /login chỉ kiểm tra HTTP readiness)
- [x] Security behavior documented
- [x] Controller identified (N/A: startup không qua Controller)
- [x] Service identified (N/A: startup do Spring/Hibernate quản lý)
- [x] Repository identified (N/A: Hibernate đọc metadata, không qua Repository)
- [x] Database interaction identified
- [ ] Spring internal components identified at source-code level (các method nội bộ chưa được xác minh)
- [x] Data transformation documented
- [x] Success path documented
- [x] Failure paths documented
- [x] External test cases created
- [x] Manual test procedure created
- [x] Debugging breakpoints documented
- [x] Code references verified
- [ ] Flow verified against current implementation bằng HTTP/browser/SMTP/DB ngoài test suite

**FLOW VERIFICATION: PARTIAL.**

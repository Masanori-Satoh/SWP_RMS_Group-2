# API Monitoring: xem trạng thái và gửi probe nội bộ

**Nguồn:** controller/service/transport hiện tại. Chỉ System Admin. Probe gửi GET thật tới loopback của chính app và `SELECT 1` thật; AI/email hiện UNCONFIGURED, không gửi probe. Chưa chạy HTTP/SQL Server thật trong lượt tài liệu.

## End-to-End Execution Flow

```text
Admin GET /admin/api-monitoring
 → Security ROLE_SYSTEM_ADMIN + AccountSessionGuardFilter
 → ApiMonitoringController.index() → ApiMonitoringService.rows()
 → admin/api-monitoring/index.html
Admin bấm Gửi GET → POST /admin/api-monitoring/probe/internal + CSRF
 → ApiMonitoringController.probeInternal(request)
 → ApiMonitoringService.probeInternal(request)
 → HttpProbeTransport.get(127.0.0.1:<localPort>/admin/api-monitoring/internal/health, JSESSIONID)
 → GET nội bộ qua SecurityFilterChain → InternalMonitoringHealthController.health()
 → JdbcTemplate.queryForObject("SELECT 1") → 200 UP hoặc 503 DOWN
 → HTTP status/elapsed → deque tối đa 100 mẫu RAM → flash message
 → 302 /admin/api-monitoring → rows() render trạng thái mới
```

## Detailed Execution Trace

| Step | Loại; class.method; package/file | Input → output → next |
|---|---|---|
| 1 | [PROJECT CODE] `SecurityConfig.filterChain(...)`; `com.group2.rms.config`; [SPRING FRAMEWORK] authorization/CSRF | GET/POST `/admin/api-monitoring/**` chỉ `ROLE_SYSTEM_ADMIN`; guard recheck User Active/role. |
| 2 | [PROJECT CODE] `ApiMonitoringController.index(Model)`; `com.group2.rms.controller` | GET → `ApiMonitoringService.rows()` → `endpoints` model → `admin/api-monitoring/index.html`. |
| 3 | [PROJECT CODE] `ApiMonitoringService.rows()`; `com.group2.rms.service` | Snapshot deque RAM; nếu chưa probe: NOT_CHECKED; có mẫu: latest status, average ms, error rate/error count; thêm AI/email UNCONFIGURED, `canProbe=false`. |
| 4 | [PROJECT CODE] template form `admin/api-monitoring/index.html` | Chỉ hàng internal có form POST **Gửi GET**; Thymeleaf/Spring thêm CSRF; AI/email không có nút probe. |
| 5 | [PROJECT CODE] `ApiMonitoringController.probeInternal(HttpServletRequest,RedirectAttributes)` | POST hợp lệ → Service, nhận `ProbeOutcome`, flash success/failure với detail + elapsed → redirect. |
| 6 | [PROJECT CODE] `ApiMonitoringService.probeInternal(HttpServletRequest)` | Lấy session hiện tại (`getSession(false)`), tạo fixed URI loopback với `getLocalPort`/context path, đo `System.nanoTime()` → `transport.get(uri,sessionId)`. |
| 7 | [PROJECT CODE] `LoopbackHttpProbeTransport.get(URI,String)`; `com.group2.rms.service` | Allowlist scheme `http`, host `127.0.0.1`, suffix path; `HttpURLConnection` GET, cookie `JSESSIONID`, no proxy/redirect, timeout 2s/5s → HTTP status. |
| 8 | [PROJECT CODE] `InternalMonitoringHealthController.health()`; `com.group2.rms.controller` | GET nội bộ qua cùng admin security → `JdbcTemplate.queryForObject("SELECT 1",Integer.class)` → 200 `{"status":"UP"}` hoặc 503 DOWN, no-store. |
| 9 | [PROJECT CODE] `ApiMonitoringService.probeInternal()` + `ApiMonitoringController.probeInternal()` | 2xx=success; khác/IOException/InterruptedException=failure; lưu tối đa 100 mẫu trong RAM, redirect list. Không lưu probe vào DB. |

## Failure / Alternative Flows

- Guest → login, HR/Candidate → 403; POST thiếu CSRF → 403.
- DB down: health trả 503 DOWN nếu JdbcTemplate ném `DataAccessException`; probe ghi failure. Kết nối loopback lỗi/timeout: Service ghi “Không nhận được phản hồi HTTP”.
- Không có session: `IllegalStateException` từ Service; không có xử lý riêng. URI vi phạm allowlist: `IllegalArgumentException` không bị Service catch; không giả định thông báo UI.
- Restart app xóa deque lịch sử; chưa probe → NOT_CHECKED. AI/email không được gọi HTTP/SMTP từ trang này.

## Database Interaction

| Order | Repository Method | Entity | Table | Operation | Purpose |
|---|---|---|---|---|---|
| 1 | `UserRepository.findByUsernameIgnoreCase` qua guard | User | `User` | SELECT | Xác nhận Admin session. |
| 2 | `JdbcTemplate.queryForObject("SELECT 1")` (không phải Repository) | Không entity | Không bảng | SQL scalar SELECT | Kiểm tra kết nối SQL Server. |

Không có bảng thống kê probe; các mẫu trong `ArrayDeque` của process. GET view `rows()` tự nó không query DB, ngoài session guard.

## Data Transformation

Admin form → POST + cookie/CSRF → fixed URI + session ID → HTTP response code → `Probe(success,status,elapsed,time,detail)` → `MonitorRow` tổng hợp → Thymeleaf table/flash message. Không gửi target do người dùng nhập; không hiển thị session ID.

## External Test Cases

| ID | Scenario | Preconditions | Action | Input | Expected HTTP | Expected Result | DB Change |
|---|---|---|---|---|---|---|---|
| MON-01 | Chưa probe | Admin, app mới chạy | Mở page | GET | 200 | Internal Chưa kiểm tra; AI/email Chưa cấu hình | Không |
| MON-02 | Probe DB khỏe | Admin, DB kết nối | Bấm Gửi GET | POST + CSRF | 302 rồi 200 | HTTP 200/Hoạt động/elapsed thật | Không |
| MON-03 | DB lỗi/không phản hồi | Môi trường test riêng | Bấm Gửi GET | POST + CSRF | 302 rồi 200 hoặc lỗi app nếu config khác | Hiển thị thất bại thực, không 200 giả | Không |
| MON-04 | Sai quyền/CSRF | HR hoặc Admin | Mở/POST | Có/không token | 403 | Không probe | Không |

## Test Procedure

MON-01: khởi động app test, login Admin, vào **API Monitoring**, kiểm tra 3 hàng và chỉ internal có nút. MON-02: bấm **Gửi GET**, xem Network POST 302, GET 200; trang có `HTTP 200`, elapsed, số mẫu 1; gọi trực tiếp health endpoint với cùng session để đối chiếu `UP`. MON-03: chỉ trong môi trường test cô lập, tạm ngắt kết nối DB sau khi app lên hoặc chặn loopback, thử probe, ghi HTTP/detail thực tế rồi khôi phục; nếu app dừng hẳn thì ghi BLOCKED, không coi là test pass. MON-04: login HR mở route (403); Admin POST thiếu `_csrf` (403). Không ghi cookie/session ID vào báo cáo.

## Test Data

Một Admin Active và một HR Active; SQL Server test kết nối cho MON-02. Không cần bản ghi nghiệp vụ vì health dùng `SELECT 1`.

## Debugging Points

| Order | Class | Method | Why |
|---|---|---|---|
| 1 | `ApiMonitoringController` [PROJECT CODE] | `index`, `probeInternal` | Model, flash/redirect. |
| 2 | `ApiMonitoringService` [PROJECT CODE] | `rows`, `probeInternal` | URI, outcome, deque. |
| 3 | `LoopbackHttpProbeTransport` [PROJECT CODE] | `get` | Allowlist/cookie/HTTP status. |
| 4 | `InternalMonitoringHealthController` [PROJECT CODE] | `health` | `SELECT 1` và 200/503. |
| 5 | `SecurityConfig` [PROJECT CODE] | `filterChain` | Role/CSRF cả outer/inner request. |

## Code References

`src/main/java/com/group2/rms/config/SecurityConfig.java`; `src/main/java/com/group2/rms/controller/ApiMonitoringController.java`; `src/main/java/com/group2/rms/controller/InternalMonitoringHealthController.java`; `src/main/java/com/group2/rms/service/ApiMonitoringService.java`; `src/main/java/com/group2/rms/service/HttpProbeTransport.java`; `src/main/java/com/group2/rms/service/LoopbackHttpProbeTransport.java`; `src/main/java/com/group2/rms/security/AccountSessionGuardFilter.java`; `src/main/java/com/group2/rms/repository/UserRepository.java`; `src/main/resources/templates/admin/api-monitoring/index.html`; `src/main/resources/static/js/api-monitoring.js`; `src/test/java/com/group2/rms/SecurityFlowTests.java`.

## Flow Completion Checklist

- [x] Entry point identified
- [x] Request URL identified
- [x] Security behavior documented
- [x] Controller identified
- [x] Service identified
- [x] Repository identified
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

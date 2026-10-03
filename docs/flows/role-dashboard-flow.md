# Dashboard theo vai trò

> **Cập nhật 02/10/2026:** System Admin dùng `internalAccountStatuses()/candidateAccountStatuses()` → hai `AccountSummary`; không gọi all-user counts trong Admin dashboard nữa. Groups theo current role, health riêng. Các role khác giữ scope cũ. Chi tiết: [account-lifecycle-flow.md](account-lifecycle-flow.md).

**Nguồn:** `DashboardAccessController`, `DashboardService`, `DashboardMetricsRepository`, `SecurityConfig`, `database/schema/db.sql`. Số liệu là query JPA thật, không có widget giả. Browser/DB thực tế chưa chạy trong lượt này.

## End-to-End Execution Flow

```text
Browser GET / hoặc /dashboard + JSESSIONID
 → [SPRING FRAMEWORK] SecurityFilterChain: authenticated()
 → [PROJECT CODE] AccountSessionGuardFilter.doFilterInternal(): kiểm tra status/role hiện tại
 → [PROJECT CODE] DashboardAccessController.root() → 302 /dashboard (nếu GET /)
 → DashboardAccessController.dashboard(Authentication,Model)
 → DashboardService.forUsername(auth.getName())
 → UserRepository.findByUsernameIgnoreCase()
 → switch RoleName → DashboardMetricsRepository JPQL / ApiMonitoringService.rows()
 → DashboardView → dashboard/index.html → HTTP 200
```

## Detailed Execution Trace

| Step | Loại; class.method; package/file | Input → output → next |
|---|---|---|
| 1 | [PROJECT CODE] `SecurityConfig.filterChain(...)`; `com.group2.rms.config` + [SPRING FRAMEWORK] authorization | Guest GET `/` hoặc `/dashboard` → login; authenticated → guard. `/dashboard/**` dùng `.authenticated()`; `/` thuộc `.anyRequest().authenticated()`. |
| 2 | [PROJECT CODE] `AccountSessionGuardFilter.doFilterInternal(...)`; `com.group2.rms.security` | Authentication từ `SecurityContextHolder` → `UserRepository.findByUsernameIgnoreCase`, status Active và authority hợp role; sai → logout + 302 `/login?session-expired`; đúng → chain. |
| 3 | [PROJECT CODE] `DashboardAccessController.root()` / `dashboard(Authentication,Model)`; `com.group2.rms.controller` | `/` → redirect `/dashboard`; `/dashboard` → username từ Authentication → Service → model `dashboard`, view `dashboard/index`. |
| 4 | [PROJECT CODE] `DashboardService.forUsername(String)`; `com.group2.rms.service` | Load User Active và role; `LocalDateTime.now()`, switch 6 role → role-specific `DashboardView`. |
| 5 | [PROJECT CODE] `DashboardMetricsRepository` methods; `com.group2.rms.repository` | JPQL `count/statuses/recentDirectorActivity` qua `EntityManager` → SQL Server, số liệu theo role; SQL thực tế Hibernate sinh động chưa capture. |
| 6 | [PROJECT CODE] `ApiMonitoringService.rows()`; `com.group2.rms.service` | Chỉ Admin: snapshot probe nội bộ trong bộ nhớ và AI/email UNCONFIGURED → breakdown; không truy vấn DB tại method này. |
| 7 | [SPRING FRAMEWORK] Thymeleaf render `dashboard/index.html` | `DashboardView` → 200 HTML; exact renderer method **Not verified at source-code level**. |

Scope query: Admin đếm `User` và trạng thái; HR đếm tin `Published` chưa quá hạn, application/stage, interview, hồ sơ chưa HR review; Hiring Manager lọc `userId`/`departmentId`; Director hàng chờ toàn hệ thống và activity theo DirectorId; Interviewer theo `InterviewPanel.InterviewerId`; Candidate theo `Candidate.UserId` qua `Candidate.account.userId`. Candidate notification và AI config shortcut ghi chưa có datasource/trang; không tạo số giả.

## Failure / Alternative Flows

- Guest: 302 login. Account Inactive/Blocked hoặc role đổi sau login: guard 302 `/login?session-expired`.
- User không tồn tại/role không hỗ trợ: Service ném exception, không có xử lý riêng tại Controller; không khẳng định HTTP status cụ thể.
- Query DB lỗi: không có fallback fake metric; request lỗi. Cần kiểm tra log/DB.

## Database Interaction

| Order | Repository Method | Entity | Table | Operation | Purpose |
|---|---|---|---|---|---|
| 1 | `UserRepository.findByUsernameIgnoreCase` (guard và Service) | User/Role | `User`, `Role` | SELECT | Session và role. |
| 2 | `DashboardMetricsRepository.accounts/accountsWithStatus` | User | `User` | SELECT aggregate | Admin. |
| 3 | `activeJobPostings/applications/applicationStages/upcomingInterviews/newApplicationsAwaitingHrReview` | JobPosting/Application/InterviewSchedule/ApplicationReview | Bảng cùng tên | SELECT aggregate | HR. |
| 4 | `ownRequisitions/departmentRequisitions/ownPendingRequisitions/candidatesForOwnRequisitions/upcomingInterviewsForHiringManager/offersNeedingHiringManagerAction` | JobRequisition/Application/InterviewSchedule/OfferProposal | Bảng cùng tên | SELECT aggregate | Hiring Manager. |
| 5 | `requisitionsAwaitingDirector/offersAwaitingDirector/recentDirectorActivity` | JobRequisition/OfferProposal/RequisitionApproval/OfferApproval | Bảng cùng tên | SELECT aggregate | Director. |
| 6 | `upcomingAssignedInterviews/assignedCandidatesForInterviewer/pendingInterviewEvaluations` | InterviewPanel/InterviewEvaluation | Bảng cùng tên | SELECT aggregate | Interviewer. |
| 7 | `candidateApplications/candidateApplicationStages/candidateUpcomingInterviews/candidateOfferStatuses` | Candidate/Application/InterviewSchedule/OfferProposal | Bảng cùng tên | SELECT aggregate | Candidate, lọc UserId. |

## Data Transformation

HTTP session Authentication → username → `User(role,userId,departmentId)` → parameter JPQL → `long`/`StatusCount`/`ApprovalActivity` → `DashboardView` → Thymeleaf. `DashboardService.view(...)` tạo DTO; không cập nhật DB.

## External Test Cases

| ID | Scenario | Preconditions | Action | Input | Expected HTTP | Expected Result | DB Change |
|---|---|---|---|---|---|---|---|
| DASH-01 | Guest | Chưa login | Mở Dashboard | GET `/dashboard` | 302 | Đến login | Không |
| DASH-02 | Sáu vai trò | 6 User Active | Login từng role | GET `/dashboard` | 200 | Widget đúng role | Không |
| DASH-03 | Candidate scope | 2 Candidate có dữ liệu khác | So sánh Dashboard | Hai session | 200 | Chỉ thấy số liệu của mình | Không |
| DASH-04 | Role/status đổi sau login | User test có session | Admin đổi role/status | Refresh Dashboard | 302 | `/login?session-expired` | UPDATE User do thao tác chuẩn bị |

## Test Procedure

DASH-01: cửa sổ ẩn danh mở Dashboard, xem redirect. DASH-02: lần lượt login bằng System Admin, HR, Hiring Manager, Director, Interviewer, Candidate test; chụp tên widget và đối chiếu số liệu bằng SELECT COUNT read-only trên bảng tương ứng. DASH-03: tạo 2 Candidate test với application riêng, đăng nhập từng account và so sánh số đơn/offer; không dùng dữ liệu account khác. DASH-04: sau khi User test login, Admin đổi status Inactive hoặc role ở tab khác; refresh Dashboard, xác nhận session hết hiệu lực. Không chỉnh account production.

## Test Data

6 `User` Active với role đúng và department cho internal role; 2 Candidate liên kết `Candidate.UserId` riêng. HR/HM/Director/Interviewer cần fixture application/requisition/interview/offer cụ thể để thấy số >0; nếu không có, xác nhận 0 theo DB và ghi thiếu fixture thay vì bịa dữ liệu.

## Debugging Points

| Order | Class | Method | Why |
|---|---|---|---|
| 1 | `AccountSessionGuardFilter` [PROJECT CODE] | `doFilterInternal` | Session/status/role. |
| 2 | `DashboardAccessController` [PROJECT CODE] | `dashboard` | Principal và view. |
| 3 | `DashboardService` [PROJECT CODE] | `forUsername` + nhánh role | Scope userId/departmentId. |
| 4 | `DashboardMetricsRepository` [PROJECT CODE] | Method widget liên quan | JPQL/parameter/count. |

## Code References

`src/main/java/com/group2/rms/config/SecurityConfig.java`; `src/main/java/com/group2/rms/security/AccountSessionGuardFilter.java`; `src/main/java/com/group2/rms/controller/DashboardAccessController.java`; `src/main/java/com/group2/rms/service/DashboardService.java`; `src/main/java/com/group2/rms/service/DashboardView.java`; `src/main/java/com/group2/rms/service/ApiMonitoringService.java`; `src/main/java/com/group2/rms/repository/UserRepository.java`; `src/main/java/com/group2/rms/repository/DashboardMetricsRepository.java`; `src/main/resources/templates/dashboard/index.html`; `database/schema/db.sql`.

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

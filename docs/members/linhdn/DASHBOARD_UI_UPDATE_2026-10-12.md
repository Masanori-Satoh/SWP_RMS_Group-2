# Dashboard theo DTO mới — quá trình sửa, luồng và hướng dẫn test

Ngày: 12/10/2026. Phạm vi: giao diện `/dashboard` của System Admin, HR, Hiring Manager, Director, Interviewer và Candidate.

Đây là tài liệu tổng hợp duy nhất của đợt này. Các đường dẫn bên dưới tính từ root `SWP_RMS_Group-2`.

## 1. Yêu cầu và quyết định đã áp dụng

- Đọc quy định trong `docs/architecture/UI/` và implementation homepage `templates/candidate/landing.html` trước khi sửa.
- Dùng Impeccable theo quy trình polish cho trang hiện có; UI UX Pro Max để đối chiếu trạng thái rỗng, focus, bảng responsive; marketing copy-editing để làm rõ nhãn và hành động. Không làm lại design system.
- Giữ hướng Mộc: nền sáng ấm, xanh rừng, tiêu đề serif, chữ nội dung theo font chung, đường viền mảnh, trang tổng quan thực dụng.
- Giao diện Dashboard dùng tiếng Việt. Nội dung nghiệp vụ lấy từ DB vẫn hiển thị nguyên văn bằng `th:text`; không dịch seed hoặc cập nhật DB.
- Giữ ba DTO đã có: `AdminDashboardResponse`, `CandidateDashboardResponse`, `RecruitmentDashboardResponse`, cùng interface `DashboardResponse`.
- Không sửa entity, repository, service nghiệp vụ, authentication, SecurityConfig, schema hoặc dữ liệu.
- Người dùng chọn **giữ nguyên khung chung, ghi nhận lỗi mobile**. Không sửa topbar, sidebar, layout, `workspace.css` hoặc dùng CSS Dashboard để che lỗi khung chung.
- Người dùng yêu cầu không sửa file trong `docs/architecture/UI/`. Phần cập nhật trạng thái từng thêm vào `UI_MIGRATION.md` đã hoàn lại; các thay đổi có sẵn của đồng đội trong Git index vẫn được giữ.

## 2. Vấn đề trước khi sửa và hướng xử lý

| Vấn đề | Cách xử lý |
|---|---|
| `dashboard/index.html` còn đọc model gộp cũ, trong khi Controller trả DTO riêng theo role | Chỉ dùng trường của `RecruitmentDashboardResponse` trong index; Admin và Candidate có trang riêng |
| `dashboard/admin.html` chưa có nội dung | Dựng tổng quan tài khoản nội bộ/ứng viên, phòng ban và trạng thái API từ DTO hiện tại |
| Candidate view còn cấu trúc fragment/model `dashboard.candidate` | Chuyển thành trang hoàn chỉnh, đọc trực tiếp `dashboard.recentApplications`, `offers`, `profile`... |
| Khung Dashboard tự dựng và CSS cũ trùng với hệ thống UI mới | Kế thừa `layout/internal`; tái sử dụng card, statCard, button, badge, table, form từ catalog |
| Một số ghi chú Candidate nói profile/thông báo chưa tồn tại | Trỏ tới `/profile`, `/notifications`, các portal đã có implementation trong code hiện tại |
| Có hành động rút đơn hoặc logout riêng chưa khớp luồng mới | Rút đơn qua trang `/portal/applications`; dùng dialog logout của topbar chung |
| Nhãn tiếng Anh/chi tiết kỹ thuật lọt vào số liệu | Việt hóa nhãn hiển thị trong fragment; giữ nguyên key DTO và dữ liệu DB |
| Hiring Manager có thống kê offer nhưng không được mở `/offers` theo SecurityConfig hiện tại | Giữ số liệu; không thêm link bị chặn; giải thích nơi xử lý hiện tại |
| Director có hai phạm vi dữ liệu khác nhau | Nêu rõ hàng đợi phê duyệt toàn hệ thống, số liệu phỏng vấn chỉ tính phân công cá nhân |

## 3. Danh sách file sửa / tạo

| File | Sửa gì | Lý do |
|---|---|---|
| `src/main/java/com/group2/rms/dashboard/DashboardController.java` | Thêm model `activeMenu="dashboard"`, `pageLang="vi"` | Chọn đúng menu và ngôn ngữ của layout chung; không đổi route hoặc điều kiện phân quyền |
| `src/main/resources/templates/dashboard/index.html` | Trang cho HR/HM/Director/Interviewer; tiêu đề, CTA, số liệu, hành động theo role, hoạt động phê duyệt | Tương thích DTO Recruitment đã tách; không truy cập trường Admin/Candidate |
| `src/main/resources/templates/dashboard/admin.html` | Tổng quan hai nhóm tài khoản, phòng ban, API và công cụ quản trị | Tương thích `AccountSummary`, `DepartmentSummary(totalCount, activeCount, inactiveCount)` và breakdown thực tế |
| `src/main/resources/templates/dashboard/candidate.html` | Trang Candidate độc lập; hồ sơ gần đây, lịch, offer, thông báo, profile | Đọc DTO phẳng; mở đúng luồng cá nhân đang có, không đưa dữ liệu đánh giá nội bộ ra UI |
| `src/main/resources/templates/dashboard/fragments.html` **(mới)** | Fragment feature `metrics`, `statuses`, `statusRows`; metric dùng `fragments/ui/cards :: statCard` | Tái sử dụng phần hiển thị trong Dashboard; không sửa fragment dùng chung của đồng đội |
| `src/main/resources/static/css/dashboard.css` | Thay CSS Dashboard cũ bằng layout có tiền tố `dash-`, dùng token hiện tại | Tránh ghi đè `.btn`, `.card`, `.workspace-*`; responsive cho nội dung trang |
| `src/main/resources/static/js/candidate-dashboard.js` | Giữ bộ lọc không dấu; bỏ block logout riêng đã lỗi thời | Logout được khung chung quản lý; tránh hai dialog/handler cho cùng hành động |
| `docs/members/linhdn/DASHBOARD_UI_UPDATE_2026-10-12.md` **(mới)** | Gộp quá trình sửa, luồng, kết quả và các bước test | Có thể đọc tiếp ở đợt sau; không tạo tài liệu ở thư mục UI |

Không xóa source file. Không tạo source test trong `src/test`. Bộ kiểm tra render tạm được dùng trong thư mục thành viên rồi dọn sau khi kiểm tra.

## 4. Luồng kỹ thuật — truy cập Dashboard

| Bước | Package / class / hàm | Xử lý |
|---|---|---|
| 1 | Trình duyệt → `GET /dashboard` | Session cookie được gửi như các trang SSR khác |
| 2 | `com.group2.rms.core.config.SecurityConfig.filterChain(...)` | Spring Security yêu cầu authentication cho `/dashboard`; Guest được đưa vào luồng login |
| 3 | `com.group2.rms.core.security.AccountSessionGuardFilter.doFilterInternal(...)` | Guard hiện có kiểm tra tài khoản còn được phép dùng session trước authorization; đợt UI này không thay đổi guard |
| 4 | `com.group2.rms.core.web.TopbarUserAdvice.topbarUser(Authentication)` | Nạp `topbarUser` qua `UserRepository.findByUsernameIgnoreCase(...)` cho topbar dùng chung |
| 5 | `com.group2.rms.dashboard.DashboardController.dashboard(Authentication, Model)` | Gọi service bằng `authentication.getName()` |
| 6 | `com.group2.rms.dashboard.DashboardService.forUsername(String)` | Tìm User, kiểm tra `accountStatus == Active`, lấy role và thời điểm hiện tại |
| 7 | `DashboardService.admin/hr/hiringManager/director/interviewer/candidate` | Switch role hiện có chọn hàm và DTO tương ứng; Director vẫn gọi `director(user, now)` |
| 8 | `com.group2.rms.dashboard.DashboardMetricsRepository` và `com.group2.rms.admin.service.ApiMonitoringService.rows()` | Nạp các số liệu theo scope hiện có; không dùng số liệu hardcode trong UI |
| 9 | `DashboardController.dashboard(...)` | Đưa DTO vào model `dashboard`, thêm `activeMenu`, `pageLang`, trả view theo role |
| 10 | Thymeleaf + Layout Dialect | Ghép view với `layout/internal`, topbar/sidebar theo role, catalog CSS và fragment Dashboard |
| 11 | `static/js/candidate-dashboard.js` | Chỉ Candidate: lọc tập dữ liệu đang hiển thị trên trình duyệt; không gọi API hoặc cập nhật DB |

`DashboardController.root()` xử lý `/dashboard/` bằng redirect về `/dashboard`.

### View và nguồn dữ liệu từng role

| Role | Hàm service → DTO → view | Nguồn và phạm vi |
|---|---|---|
| System Admin | `admin(user)` → `AdminDashboardResponse` → `dashboard/admin` | `internalAccountStatuses()`, `candidateAccountStatuses()`, `departmentStatuses()`, `monitoring.rows()`; toàn hệ thống |
| HR | `hr(user, now)` → `RecruitmentDashboardResponse` → `dashboard/index` | `activeJobPostings()`, `applications()`, `upcomingInterviews()`, `newApplicationsAwaitingHrReview()`, `applicationStages()` |
| Hiring Manager | `hiringManager(user, now)` → Recruitment → index | `ownRequisitions()`, `departmentRequisitions()`, `ownPendingRequisitions()`, `candidatesForOwnRequisitions()`, `upcomingInterviewsForHiringManager()`, `offersNeedingHiringManagerAction()`; scope của User/phòng ban |
| Director | `director(user, now)` → Recruitment → index | `requisitionsAwaitingDirector()`, `offersAwaitingDirector()` toàn hệ thống; `recentDirectorActivity(userId)`, `upcomingAssignedInterviews(userId, now)`, `assignedCandidatesForInterviewer(userId)` theo cá nhân |
| Interviewer | `interviewer(user, now)` → Recruitment → index | `upcomingAssignedInterviews()`, `assignedCandidatesForInterviewer()`, `pendingInterviewEvaluations()` theo phân công |
| Candidate | `candidate(user, now)` → `CandidateDashboardResponse` → `dashboard/candidate` | Các method `candidate*` đều dùng UserId của tài khoản đăng nhập qua `Application.candidate.account.userId`; chỉ dữ liệu của ứng viên đó |

Nếu Hiring Manager chưa có Department, vẫn hiển thị giá trị 0 theo backend và ghi rõ **Tài khoản chưa được gán phòng ban**; không mô tả 0 là một phòng ban có thật.

### Candidate: dữ liệu và tương tác

1. `candidateApplications`, `candidateUpcomingInterviews`, `candidateOffersAwaitingResponse` trả số tổng; không phụ thuộc bộ lọc cục bộ.
2. `candidateRecentApplications(userId, 20)` lấy tối đa 20 hồ sơ gần đây. UI hiển thị vị trí, ngày nộp và nhãn trạng thái từ service. Không tự tạo stage độc lập vì DTO hiện tại không cung cấp stage riêng.
3. `candidateNextInterviews(userId, now, 10)` lấy tối đa 10 lịch sắp tới ở trạng thái Scheduled/Rescheduled. `safeMeetingUrl(...)` hiện có kiểm tra link; view chỉ render link đã được service trả về, kèm `noopener noreferrer`.
4. `candidateOffers(userId, 20)` chỉ lấy trạng thái offer mà backend cho Candidate xem; hiển thị lương chính thức/thử việc, ngày bắt đầu, nơi làm việc, quyền lợi. Nội dung thiếu hiện thông báo cụ thể, không thay bằng giá trị giả.
5. JS khởi tạo từng `[data-candidate-filter]`, `normalize(...)` bỏ dấu tiếng Việt và đ/Đ; `update()` áp keyword + status lên `[data-filter-row]`, cập nhật `aria-live` và trạng thái rỗng.
6. Xóa lọc giữ focus ở nút trong section hiện tại đến khi người dùng chuyển đi; không cuộn về đầu trang hoặc chuyển focus sang section khác.
7. `<details>/<summary>` mở chi tiết đãi ngộ bằng hành vi HTML native, không gọi backend.
8. Xem toàn bộ/rút đơn: `/portal/applications` → `com.group2.rms.candidate.controller.MyApplicationController.list()` → `MyApplicationService.list()`. Rút đơn tiếp tục ở `MyApplicationController.withdraw(...)`, không xử lý trong Dashboard.
9. Xem lịch: `/portal/interviews` → `com.group2.rms.interview.controller.CandidateInterviewController.myInterviews()` → `CurrentUserService.requireUser()` → `InterviewSchedulingService.getSchedulesForCandidate(candidateUserId)`.
10. Thông báo: `/notifications` → `com.group2.rms.notification.NotificationController.list(...)`. Profile: `/profile` → `com.group2.rms.user.controller.UserProfileController.viewProfile(...)` → `UserProfileService.getProfile(...)`; giữ luồng edit/change-password hiện tại.
11. Logout dùng dialog/form từ topbar chung; xác nhận sẽ POST `/logout` với CSRF để Spring Security xử lý. Không có Controller logout mới trong Dashboard.

## 5. Kết quả kiểm tra đã chạy

| Kiểm tra | Kết quả | Phạm vi / giới hạn |
|---|---|---|
| Maven offline compile | **PASS**, BUILD SUCCESS lúc 21:12 ngày 12/10/2026 | Compile main source và copy resources; không phải Maven test |
| `node --check .../candidate-dashboard.js` | **PASS** | Cú pháp JavaScript |
| MVC/Thymeleaf render bằng harness tạm | **PASS: 84 kiểm tra** | Controller thật + template/layout thật; service fixture, CSRF token kiểm thử, không kết nối DB |
| Các trạng thái render | **PASS** | Sáu role, Candidate rỗng, HM chưa có Department; tổng 8 trạng thái |
| Browser ở 1440 / 1024 / 768px | **PASS: 21 tổ hợp** | 7 trạng thái: 6 role + Candidate rỗng; CSS thật, HTML render từ DTO fixture |
| Browser ở 375px | **FAIL: 7 tổ hợp** | Lỗi topbar/sidebar chung được giữ lại theo quyết định người dùng, xem mục 6 |
| Xác nhận sau polish cuối ở 1440px | **PASS: 7 trạng thái** | Một main, heading theo role, không tràn ngang; HM không có link `/offers`, Candidate không có link quản trị |
| Browser lọc hồ sơ / offer | **PASS** | Keyword không dấu, status, clear, không có kết quả, khôi phục danh sách |
| Keyboard / dialog | **PASS** | Tab từ search đến select, focus có outline; mở logout, Escape đóng và trả focus; CSRF có trong form |
| Offer detail | **PASS** | Native details mở, giữ lương thử việc và quyền lợi; dữ liệu thiếu không làm render lỗi |
| `git diff --check` | **PASS** | Không phát hiện whitespace error trong diff của đợt này |
| Toàn bộ `mvnw test` | **NOT RUN** | Không kết luận test suite xanh; đợt này không sửa/khôi phục test source cũ |
| Full Spring Boot startup + SQL Server / JPQL execution | **NOT TESTED** | Context MVC dùng cho render đã chạy; không thay thế kiểm tra Application Context đầy đủ |
| Login thực, scope DB và click xuyên các module | **NOT TESTED end-to-end** | Đã đối chiếu href với Controller/SecurityConfig; server preview chỉ phục vụ Dashboard/homepage và asset |

Lệnh compile đã dùng tại root project:

```powershell
.\mvnw.cmd '-Dmaven.repo.local=C:\Users\Administrator\.m2\repository' '-Dstyle.color=never' '-o' compile
node --check src/main/resources/static/js/candidate-dashboard.js
git diff --check
```

Render kiểm tra: lang vi, menu Dashboard active, asset catalog, một main, Thymeleaf đã xử lý hết, dialog logout dùng chung, token CSRF trong form, không truy cập trường DTO cũ, Candidate không có URL quản trị/offer nội bộ, HM không có link bị chặn. Kiểm tra Director dùng đúng type `Job Requisition` mà repository trả về, đã dịch nhãn giao diện.

Browser cũng kiểm tra reference ARIA, duplicate ID, label control, kích thước control và focus. Đây là kiểm tra cơ bản; chưa phải audit bằng screen reader hoặc chứng nhận WCAG.

## 6. Lỗi mobile đã ghi nhận — giữ nguyên theo yêu cầu

Tại 375px, topbar của khung chung bị tràn bởi nhóm tên/email/role. Đồng thời sidebar vẫn hiển thị dù đã có thuộc tính `hidden`, do CSS shared đặt display flex. Lỗi ảnh hưởng tất cả role, không phải dữ liệu Candidate riêng.

| Trạng thái | Độ rộng trang đo được ở viewport 375px trong batch đầu |
|---|---:|
| Admin | 559px |
| HR | 490px |
| Hiring Manager | 597px |
| Director | 522px |
| Interviewer | 559px |
| Candidate | 507px |
| Candidate rỗng | 494px |

Sau polish, Candidate đo được khoảng 508px do khung chung. Nội dung `main` khoảng 314px, không có phần tử nội dung tràn ngoài main (ngoại trừ table nằm trong wrapper cuộn ngang); hai wrapper rộng khoảng 281px, `overflow-x:auto`.

Không sửa `workspace.css`, `topbar.css`, shared HTML hoặc core/web. Không đánh dấu mobile toàn trang PASS. Việc sửa khung chung cần được Lead Architect/người dùng cho phép ở đợt khác.

## 7. Các bước test thủ công với ứng dụng và DB thật

Chuẩn bị: chạy project bằng cấu hình localhost hiện có, SQL Server đúng schema, có tài khoản Active của sáu role và dữ liệu demo phù hợp. Không nhập mật khẩu hoặc secret vào tài liệu. Không sửa seed/DB chỉ để làm test pass.

### T01 — Guest và phiên đăng nhập

1. Mở cửa sổ riêng tư, vào `/dashboard` khi chưa login.
2. Mong đợi trang login; không có dữ liệu Dashboard.
3. Login bằng từng tài khoản Active, mở `/dashboard`.
4. Kiểm tra tên/role ở topbar, menu Dashboard active, đúng heading trong bảng role ở trên.
5. Mở `/dashboard/`; mong đợi redirect về `/dashboard`, không render hai main.

### T02 — System Admin

1. Kiểm tra hai nhóm tài khoản được tách nội bộ/ứng viên, tổng và Active/Inactive/Blocked khớp dữ liệu hiện có.
2. Bấm Quản lý từng nhóm; URL lần lượt `/admin/accounts`, `/admin/candidate-accounts`.
3. Kiểm tra tổng phòng ban và trạng thái; bấm Quản lý phòng ban, mở `/admin/departments`.
4. Bấm Mở giám sát, kiểm tra đi đúng `/admin/api-monitoring`; trạng thái chưa kiểm tra không được trình bày là đang hoạt động.
5. Nội dung Dashboard không có khối yêu cầu tuyển dụng. Sidebar chung vẫn giữ menu hiện có của đồng đội.
6. Cấu hình AI hiện ghi chưa khả dụng, không có form hoặc action giả.

### T03 — HR

1. Đối chiếu số tin đang tuyển còn hạn, tổng hồ sơ, lịch sắp tới, hồ sơ Applied chưa duyệt với dữ liệu hiện có.
2. Kiểm tra breakdown tiến trình và trạng thái rỗng nếu không có hồ sơ.
3. Bấm lần lượt Tin tuyển dụng, Hồ sơ ứng tuyển, Lịch phỏng vấn, Đề nghị tuyển dụng, Thông báo; đúng route và không gặp 403 với quyền hiện tại.

### T04 — Hiring Manager

1. Chọn tài khoản có requisition riêng và Department; đối chiếu số liệu theo UserId/DepartmentId.
2. Kiểm tra các link requisitions, applications, interviews và notifications.
3. Không có nút mở `/offers`; thống kê offer và ghi chú vẫn hiện.
4. Nếu có tài khoản chưa gán Department, kiểm tra số phòng ban là 0 và ghi chú Tài khoản chưa được gán phòng ban.

### T05 — Director

1. Đối chiếu hai hàng đợi phê duyệt toàn hệ thống.
2. Đối chiếu lịch/ứng viên được phân công riêng; không nhầm số này với toàn bộ lịch được phép xem ở module Interview.
3. Kiểm tra lịch sử do Director đó thực hiện; loại Job Requisition hiển thị Yêu cầu tuyển dụng; status Approved/Rejected có nhãn tiếng Việt.
4. Trường hợp chưa có quyết định hiển thị empty state; bấm links đến requisitions, offers, interviews hoạt động theo quyền hiện có.

### T06 — Interviewer

1. So sánh lịch sắp tới, ứng viên phân công, đánh giá còn chờ với InterviewPanel/evaluation hiện có.
2. Chỉ có CTA liên quan lịch phỏng vấn và thông báo trong nội dung Dashboard; không có link accounts hoặc requisitions.
3. Bấm xem lịch; đối chiếu data scope của tài khoản đó ở module hiện có.

### T07 — Candidate: hồ sơ và dữ liệu riêng tư

1. Đăng nhập Candidate A có đơn; kiểm tra tên công việc tiếng Việt từ DB vẫn giữ nguyên, ngày nộp và trạng thái đúng.
2. Tìm `ky su` với vị trí Kỹ sư: mong đợi vẫn khớp; chọn status để kết hợp lọc.
3. Nhập từ khóa không tồn tại: đếm 0, bảng ẩn, thông báo không có kết quả hiện.
4. Bấm Xóa bộ lọc: trả danh sách, không nhảy về Hero hoặc section khác; dùng Tab rời nút clear.
5. Bấm Xem toàn bộ → `/portal/applications`; rút đơn trong luồng của module đó, reload Dashboard để kiểm tra trạng thái mới.
6. Logout rồi đăng nhập Candidate B: không thấy đơn, lịch, offer hoặc profile của A.
7. Kiểm tra HTML không chứa điểm AI, nhận xét nội bộ, đánh giá phỏng vấn, ghi chú đàm phán nội bộ.
8. Dùng Candidate chưa có dữ liệu: empty state có hướng dẫn và link công việc; không có số liệu giả.

### T08 — Candidate: lịch và thư mời

1. Kiểm tra lịch sắp tới có thời gian, hình thức, link họp hoặc địa điểm; lịch cũ/Cancelled không nằm trong bảng sắp tới.
2. Bấm Xem lịch của tôi → `/portal/interviews`; đối chiếu chỉ lịch cá nhân.
3. Kiểm tra offer chính thức có lương/ngày bắt đầu; mở Xem chi tiết đãi ngộ bằng click và Enter.
4. Kiểm tra lương thử việc, quyền lợi và nơi làm việc giữ nguyên nội dung DB; trường NULL có thông báo phù hợp.
5. Thử keyword không dấu và status offer, rồi clear; không đổi số tổng trên đầu Dashboard.
6. Không có nút Accept/Decline/Negotiate giả. Các nghiệp vụ này chưa được triển khai trong đợt giao diện này.

### T09 — Profile, thông báo, logout

1. Candidate bấm Mở thông báo → `/notifications`, Chỉnh sửa hồ sơ → `/profile`; kiểm tra dữ liệu riêng của User.
2. Bấm Đăng xuất trên topbar: dialog chung xuất hiện, focus nằm trong dialog.
3. Bấm Ở lại hoặc Escape: dialog đóng, focus trả về nút Đăng xuất, session còn dùng được.
4. Bấm Đăng xuất → xác nhận: Network có POST `/logout` kèm CSRF, chuyển `/login?logout`; truy cập lại Dashboard phải login.
5. Bước POST/logout thật chưa chạy trong preview UI đợt này; cần kiểm tra trên app thật.

### T10 — Responsive và bàn phím

1. Kiểm tra từng role tại 1440, 1024, 768, 375px với dữ liệu thực và tên/email dài.
2. Ở ba mức lớn, không có scroll ngang cả trang; grid đổi cột phù hợp, link/bộ lọc đủ chỗ.
3. Ở mobile, bảng cuộn trong wrapper; bộ lọc và offer/profile xuống cột đơn.
4. Ghi nhận lỗi khung chung ở 375px theo mục 6, không kết luận toàn trang đạt.
5. Dùng Tab qua skip link, menu, CTA, input/select, bảng, summary và logout; focus nhìn thấy, thứ tự hợp lý.
6. Đổi cài đặt giảm chuyển động của hệ điều hành; xác nhận kế thừa hành vi `prefers-reduced-motion` chung.

## 8. Giới hạn và việc nối tiếp

- Dashboard mới tương thích cấu trúc DTO hiện tại; không thay đổi architecture backend hoặc data scope.
- Bộ lọc Candidate chỉ lọc tập 20 bản ghi đã nạp; không tìm toàn bộ DB. Link Xem toàn bộ mở module riêng.
- Candidate DTO chưa có stage tách biệt; view không dựng stage giả.
- Offer chỉ xem thông tin, chưa có thao tác phản hồi/thương lượng từ Dashboard.
- Notification chỉ là đường dẫn vào module thật; Dashboard DTO chưa có danh sách/số unread riêng. Topbar dùng cơ chế thông báo chung hiện có.
- Quyền/scoping và các thao tác sửa dữ liệu cần chạy T01–T09 với app + DB thật. Không suy ra chúng PASS từ render fixture.
- Lỗi mobile khung chung để nguyên; tài liệu này là nơi ghi nhận, không sửa docs kiến trúc UI.
- Không commit, merge hoặc push trong đợt này. Không khôi phục đè lên thay đổi staged của đồng đội.

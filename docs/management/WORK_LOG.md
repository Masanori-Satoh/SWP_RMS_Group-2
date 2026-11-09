# Nhật ký Công việc (Work Log)

Tài liệu này là lịch sử tóm tắt các thay đổi quan trọng và bàn giao của project. Sau **mỗi lần làm việc**, hãy thêm một mục mới vào bảng dưới đây và tạo một file chi tiết trong thư mục `work_logs/` để tham chiếu.

## Lịch sử Thay đổi

| Ngày | Tên công việc / Yêu cầu chính | Người thực hiện | Chi tiết thay đổi |
| --- | --- | --- | --- |
| **2026-11-09** | Khôi phục toast khi mở trang ngoài quyền sau merge | LinhDN | [Work log](work_logs/2026-11-09-forbidden-page-toast-regression.md) · [Báo cáo](../members/linhdn/2026-11-09-forbidden-page-toast-regression.md) |
| **2026-11-08** | Hiện toast khi người đã đăng nhập mở trang ngoài quyền | LinhDN | [Work log](work_logs/2026-11-08-forbidden-page-toast.md) · [Báo cáo](../members/linhdn/2026-11-08-forbidden-page-toast.md) |
| **2026-11-08** | Cho ứng viên tự đổi email trên Hồ sơ cá nhân, giữ tên đăng nhập chỉ đọc và đối chiếu quyền của các vai trò nội bộ | LinhDN | [Work log](work_logs/2026-11-08-candidate-profile-email.md) · [Báo cáo](../members/linhdn/2026-11-08-candidate-profile-email.md) |
| **2026-11-08** | Đối chiếu hai bản sơ đồ kiến trúc hệ thống RMS với mã nguồn, xác nhận luồng chính và xác định vị trí lưu tệp, lớp truy cập dữ liệu | LinhDN | [Work log](work_logs/2026-11-08-system-architecture-code-review.md) · [Báo cáo](../members/linhdn/2026-11-08-system-architecture-code-review.md) |
| **2026-11-08** | Nâng giới hạn ô tìm họ tên hoặc email trong Kho ứng viên tiềm năng lên 150 ký tự | LinhDN | [Work log](work_logs/2026-11-08-talent-pool-search-max-length.md) · [Báo cáo](../members/linhdn/2026-11-08-talent-pool-search-max-length.md) |
| **2026-11-08** | Hiện số ứng viên do HM đề xuất trực tiếp hoặc đã duyệt từ HR trên chi tiết từng tin tuyển dụng | LinhDN | [Work log](work_logs/2026-11-08-job-posting-suggested-candidate-count.md) · [Báo cáo](../members/linhdn/2026-11-08-job-posting-suggested-candidate-count.md) |
| **2026-11-08** | Sửa lỗi tạo hàng loạt nút khi yêu cầu vượt trang cuối và tải trang cuối có dữ liệu trong Talent Pool | LinhDN | [Work log](work_logs/2026-11-08-pagination-out-of-range.md) · [Báo cáo](../members/linhdn/2026-11-08-pagination-out-of-range.md) |
| **2026-11-07** | Lịch phỏng vấn: gửi email ngầm, tự chuyển "Đã hoàn thành" khi hết giờ, tạo lịch mới thì thay thế lịch chưa bắt đầu của hồ sơ; sửa banner "không có quyền" hiện cả với HR/Director | Đức | [Quyết định, thay đổi và kiểm chứng](work_logs/2026-11-07-interview-async-email-auto-complete-replace.md) |
| **2026-11-06** | Đối chiếu Application state diagram với code hiện tại: luồng chính khớp; bổ sung nhánh draft bị kết thúc, guard đóng tin/consent/Offer và ngoại lệ kỹ thuật | LinhDN | [Work log](work_logs/2026-11-06-application-state-diagram-code-review.md) · [Báo cáo đối chiếu](../members/linhdn/2026-11-06-application-state-diagram-code-review.md) |
| **2026-11-06** | Rà soát mâu thuẫn nội bộ SRS trong Authentication, Account/Department, Talent Pool, Admin và Core/Audit Log; gồm diagram, màn hình và message; chưa so với code | LinhDN | [Work log](work_logs/2026-11-06-srs-internal-consistency-review.md) · [12 nhóm lỗi và 14 điểm cần làm rõ](../members/linhdn/2026-11-06-srs-internal-consistency-review.md) |
| **2026-11-06** | Business rule, Notification, Validate & Phân quyền UI của Lịch phỏng vấn: tự suy trạng thái (bỏ ô chọn), chặn sửa lịch Hoàn thành/Hủy, thời lượng 15p–4h, trùng lịch theo ứng viên, email đổi/hủy lịch, hủy chỉ báo ứng viên, banner "không có quyền" thay 403 toàn trang | Đức | [Yêu cầu, thay đổi và kiểm chứng](work_logs/2026-11-06-interview-business-rules-notification-permission-ui.md) |
| **2026-11-06** | Sửa lỗi Auth, Tài khoản, Phòng ban, Kho ứng viên tiềm năng: 1 trưởng phòng Active/phòng, đăng xuất phiên cũ khi đổi mật khẩu, ẩn ứng viên bị khóa, lọc người đánh dấu, SĐT 10 số, audit đổi mật khẩu/hồ sơ | LinhDN | [Lỗi đã sửa, cách tái hiện và kiểm chứng](work_logs/2026-11-06-auth-account-talent-pool-bugfix.md) |
| **2026-11-05** | Hiện thông báo lỗi thao tác trong Auth, Account, Department, Talent Pool và Audit Log; giữ đường lỗi hệ thống qua Global | LinhDN | [Phạm vi, kiến trúc và kiểm chứng](work_logs/2026-11-05-module-error-messages.md) |
| **2026-11-05** | Đồng bộ validation họ tên/SĐT của Profile với Admin, Đăng ký và SRS | LinhDN | [Quy tắc, phạm vi và kiểm chứng](work_logs/2026-11-05-account-contact-validation-sync.md) |
| **2026-11-05** | Làm sạch thông tin công cụ hỗ trợ trong docs, metadata sơ đồ và file kiểm tra tạm | LinhDN | [Work log](work_logs/2026-11-05-docs-cleanup.md) · [Báo cáo](../members/linhdn/2026-11-05-docs-cleanup.md) |
| **2026-11-05** | Sửa Offer quá hạn phản hồi chặn đóng tin Talent Pool; đồng bộ quy tắc ngày và bổ sung test truy vấn | LinhDN | [Work log và kiểm chứng](work_logs/2026-11-05-talent-pool-expired-offer-fix.md) · [Báo cáo](../members/linhdn/2026-11-05-talent-pool-expired-offer-fix.md) |
| **2026-11-04** | Gỡ conflict merge Work Log và email mời phỏng vấn | LinhDN | [Phạm vi và kiểm chứng](work_logs/2026-11-04-merge-conflicts.md) |
| **2026-11-04** | Bảo vệ pháp lý ứng viên & Quản trị chính sách hệ thống (Candidate Policy Consent & PDF Management) | HoangNH | Đảm bảo tính tuân thủ bảo vệ dữ liệu (GDPR/Chính sách tuyển dụng): tạo popup cam kết điều khoản bắt buộc trước khi nộp CV, module quản trị file PDF `/terms` và `/privacy-policy` cho Admin. [Kế hoạch](../members/hoangnh/candidate-policy-notice-plan.md) · [Checklist](../members/hoangnh/checklist.md) |
| **2026-11-04** | Hiện toast “Bạn đã đăng xuất” sau logout, dùng đích đăng nhập hiện tại | LinhDN | [Thay đổi và kiểm chứng trình duyệt](work_logs/2026-11-04-logout-toast.md) |
| **2026-11-04** | Đổi hướng đăng xuất thành công về trang chủ, kiểm tra hợp đồng CSRF của logout | LinhDN | [Phạm vi và giới hạn kiểm chứng](work_logs/2026-11-04-logout-home.md) |
| **2026-11-02** | Xây dựng cơ chế phòng vệ lỗi & Tránh Crash hệ thống (Fault Tolerance & Error Handling UX) | HoangNH | Xử lý triệt để lỗi crash 500 khi comment dài quá 1000 ký tự (hàm `shortComment`), chặn URL sai/sai trạng thái bằng thông báo Toast thân thiện thay vì bung trang lỗi Tomcat 403/500; chuẩn hóa trường Thời gian làm việc. [Review Guide](../members/hoangnh/requisition-review-guide.md) |
| **2026-11-02** | Khắc phục lỗi import/package hàng loạt trong IDE bằng cách nạp lại dự án Java | LinhDN | [Chẩn đoán và xác nhận 0 lỗi đỏ](work_logs/2026-11-02-ide-java-project-reload.md) |
| **2026-11-02** | Việt hóa tên Hiring Manager trong tiêu đề và thông báo Kho ứng viên tiềm năng | LinhDN | [Phạm vi và kiểm chứng](work_logs/2026-11-02-talent-pool-role-vietnamese.md) |
| **2026-11-02** | Việt hóa thông báo lỗi tạo/sửa tài khoản nội bộ, gồm trùng tên đăng nhập/email và kiểm tra vai trò/phòng ban | LinhDN | [Phạm vi và kiểm chứng](work_logs/2026-11-02-account-validation-vietnamese.md) |
| **2026-11-01** | Dọn fixture/comment AI screening trong Admin/Admin Dashboard; giữ nguyên 9 file module khác theo xác nhận của người dùng | LinhDN | [4 file thay đổi, 19 test qua và giới hạn SQL Server](work_logs/2026-11-01-admin-ai-screening-cleanup.md) |
| **2026-10-31** | Cài đặt quy tắc nghiệp vụ BR-CAN-01 & Bộ 16 ca System Test nghiệm thu thực tế | HoangNH | Chặn ứng viên spam/nộp trùng hồ sơ vào các tin đăng lại của cùng một đợt tuyển dụng (`existsActiveByCandidateInSameRequisition`); hoàn thành trọn bộ 16 ca System Test nghiệm thu luồng thực tế trên SQL Server thật. [System Test](../members/hoangnh/system-test.md) |
| **2026-10-31** | Chuẩn hóa giao diện cổng Offer ứng viên (Candidate Offer Portal) theo UI_RULES & ARCHITECTURE_GUIDE | HuyenPT | [File change và kiểm thử](work_logs/2026-10-31-candidate-offer-ui-sync.md) |
| **2026-10-31** | Đồng bộ giao diện Danh sách Offer của Giám đốc (Director List) theo layout Admin, giữ 100% nghiệp vụ | HuyenPT | [File change và kiểm thử](work_logs/2026-10-31-director-offer-ui-sync.md) |
| **2026-10-31** | Giải quyết conflict Work Log khi khôi phục stash sau merge, giữ cả nhật ký Offer và giao diện tạo mới | LinhDN | [Bổ sung xử lý stash](work_logs/2026-10-31-merge-work-log-conflict.md#bổ-sung-conflict-khi-khôi-phục-stash) |
| **2026-10-31** | Giải quyết conflict Work Log khi merge, giữ lịch sử hai bên và các file Offer đã tự merge | LinhDN | [Phạm vi và kiểm chứng](work_logs/2026-10-31-merge-work-log-conflict.md) |
| **2026-10-31** | Đồng bộ giao diện các màn hình Offer của HR (Danh sách & Đề xuất) theo layout Admin, giữ 100% nghiệp vụ | HuyenPT | [File change và kiểm thử](work_logs/2026-10-31-hr-offer-proposal-admin-ui-sync.md) |
| **2026-10-31** | Áp dụng mockup Tạo tài khoản/Tạo phòng ban, giữ hai màn sửa và khung điều hướng | LinhDN | [Thống kê, kiểm chứng và ảnh giao diện](../members/linhdn/2026-10-31-admin-create-forms-ui.md) |
| **2026-10-31** | Chỉnh nội dung Sửa tài khoản nội bộ theo mockup, giữ sidebar/topbar và nghiệp vụ | LinhDN | [Thống kê, kiểm chứng và ảnh giao diện](../members/linhdn/2026-10-31-admin-account-edit-ui.md) |
| **2026-10-30** | Xây dựng Hạ tầng Thông báo thời gian thực (Real-time STOMP WebSocket) & Đẩy tin sau Commit | HoangNH | Xây dựng hạ tầng WebSocket dùng chung toàn hệ thống (`/ws`, `/user/queue/notifications`), cơ chế `@TransactionalEventListener(phase = AFTER_COMMIT)` chống báo ảo khi rollback; liên kết 7 luồng thông báo tự động (N1–N7) giữa các module. [Kế hoạch](../members/hoangnh/plan_dashboard.md) |
| **2026-10-30** | Giải quyết 3 conflict tài liệu khi merge origin/main; giữ lịch sử và nội dung đã làm sạch | LinhDN | [Phạm vi và kiểm chứng](../members/linhdn/2026-10-30-merge-main-docs-conflicts.md) |
| **2026-10-30** | Rà soát toàn bộ Markdown trong docs, làm sạch tên công cụ và tham chiếu đường dẫn cục bộ | LinhDN | [Phạm vi và kiểm chứng](../members/linhdn/2026-10-30-cleanup-markdown-docs.md) |
| **2026-10-30** | Đồng bộ chi tiết Application với workspace Talent Pool; bố cục hồ sơ/CV và cột thông tin/thao tác theo mockup | LinhDN | [File change, số dòng và kiểm chứng](work_logs/2026-10-30-application-detail-ui.md) |
| **2026-10-30** | Cập nhật bảng HR Recommend theo mockup; bỏ tìm kiếm/bộ lọc, chuyển liên kết HM lên đầu trang | LinhDN | [File change, số dòng và kiểm chứng](work_logs/2026-10-30-hr-recommendations-ui.md) |
| **2026-10-31** | Đồng bộ giao diện Offer (HR) theo Admin Workspace & khắc phục vi phạm kiến trúc | HuyenPT | [Xem chi tiết](work_logs/2026-10-31-hr-offer-proposal-admin-ui-sync.md) |
| **2026-10-30** | Tổng hợp lịch sử thay đổi LinhDN; thêm thông tin hồ sơ vào chi tiết ứng viên tiềm năng theo mockup | LinhDN | [Lịch sử, file change và kiểm chứng](work_logs/2026-10-30-linhdn-history-and-candidate-info.md) |
| **2026-10-28** | Tái cấu trúc kiến trúc Dashboard đa phân hệ (Role-Based Dashboard Architecture) | HoangNH | Giải quyết nợ kỹ thuật phình to mã nguồn: chia tách 3 package `shared/`, `admin/`, `internal/` tối ưu hiệu năng theo 4 vai trò (HR, HM, Director, Interviewer); vẽ biểu đồ Chart.js trực quan và khử hoàn toàn code rác. [Kế hoạch](../members/hoangnh/plan_dashboard.md) |
| **2026-10-28** | Làm nổi bật danh sách Potential Candidate theo Quản lý tài khoản Admin; gọn CV và tên tin dài | LinhDN  | [File change và kiểm chứng](work_logs/2026-10-28-potential-candidate-list-polish.md) |
| **2026-10-28** | Đồng bộ toàn bộ layout workspace Admin cho Potential Candidate, giữ nghiệp vụ HR/HM | LinhDN  | [File change và kiểm thử](work_logs/2026-10-28-potential-candidate-admin-ui-sync.md) |
| **2026-10-26** | Tái thiết lập ranh giới bảo mật: Cách ly System Admin khỏi nghiệp vụ tuyển dụng | HoangNH | Vá lỗ hổng bảo mật nghiêm trọng: tước quyền can thiệp trái phép của System Admin vào duyệt Offer, duyệt Yêu cầu và Phỏng vấn; đảm bảo tính toàn vẹn và phân quyền độc lập của quy trình nhân sự. [CLAUDE.md](../members/hoangnh/CLAUDE.md) |
| **2026-10-23** | Hạn chấm điểm phỏng vấn 24h, kết quả "Cân nhắc" (migration 010) & khóa hồ sơ khi đặt lịch từ hồ sơ | Đức | [Xem chi tiết](work_logs/2026-10-23-interview-evaluation-deadline-consider.md) |
| **2026-10-18** | "Đại phẫu" làm sạch Clean Code & Giải quyết triệt để vấn nạn N+1 Query toàn hệ thống | HoangNH | Tối ưu hóa hiệu năng tải danh sách từ hàng chục câu query xuống đúng 1 query (`@EntityGraph`, SQL aggregate); biên soạn bộ luật chuẩn [`CLAUDE.md`](../members/hoangnh/CLAUDE.md) (767 dòng); đưa bean `Clock` vào toàn bộ ứng dụng để hỗ trợ kiểm thử thời gian. [CLAUDE.md](../members/hoangnh/CLAUDE.md) |
| **2026-10-17** | Chuẩn hóa quy trình tạo tin từ Yêu cầu đã duyệt & Định dạng tiền tệ Real-time | HoangNH | Chốt 11 quyết định kỹ thuật Job Posting (`plan_posting.md`), xử lý format tiền tệ phân tách hàng nghìn `123.000` trên JS không làm loạn con trỏ; cơ chế Giám đốc phê duyệt trực tiếp giúp rút ngắn 50% thời gian tạo tin. [Kế hoạch](../members/hoangnh/plan_posting.md) · [Validation](../members/hoangnh/validation.md) |
| **2026-10-17** | Hạn phản hồi Offer, tái thiết kế chi tiết duyệt Director & chuẩn hóa Flash Toast 2s | HuyenPT | [Xem chi tiết](../members/huyenpt/pr-description.md) |
| **2026-10-17** | Hoàn thiện đề xuất HM bằng Application/HM_Suggested, UI và kiểm thử tích hợp | LinhDN | [Tổng hợp tài liệu](../members/linhdn/README.md) · [Chức năng và kiểm chứng](../members/linhdn/2026-10-17-hm-suggestions-functional.md) |
| **2026-10-16** | Tinh chỉnh Giao diện Quản lý Hồ sơ Ứng tuyển & Tuân thủ Kiến trúc Tuyệt đối | Nhóm 2 | [Xem chi tiết](../members/linhdn/2026-10-16-candidate-pipeline-ui-refinement.md) |
| **2026-10-13** | Chuyển đổi Tạo mới & Xem chi tiết Offer sang Màn hình chuyên biệt | HuyenPT | [Xem chi tiết](work_logs/2026-10-13-convert-popup-to-screen.md) |
| **2026-10-11** | Kết quả phỏng vấn (5.1.28) & Pop-up phiếu đánh giá (5.1.29) | Đức | [Xem chi tiết](work_logs/2026-10-11-interview-result-evaluation.md) |
| **2026-10-08** | Khởi tạo phân hệ Quản lý Tin tuyển dụng nội bộ & Quy chuẩn cô lập UI/UX Mộc RMS | HoangNH | Xây dựng phân hệ Job Posting nội bộ (`/internal/job-postings`); phân định rạch ròi Section A (dữ liệu nguồn Read-only) và Section B (nội dung Editable); ban hành quy tắc cô lập CSS/JS bảo vệ giao diện của các thành viên khác. [Kế hoạch UI/UX](../members/hoangnh/HOANGNH_UI_UX_PLAN.md) |
| **2026-10-05** | Hoàn thiện Ràng buộc Lương, Single Active Offer (GBR-07) & Bộ Test Offer | HuyenPT | [Xem chi tiết](work_logs/2026-10-05-offer-flow-testing-and-bugfix.md) |
| **2026-10-04** | Thiết lập nền tảng In-App Notification & Siết chặt phân quyền xem Requisition của HR | HoangNH | Khởi tạo module `com.group2.rms.notification` phẳng dùng chung, cơ chế Idempotent chống tạo trùng thông báo; khóa chặt bảo mật HR chỉ được xem đơn Approved (chặn truy cập trái phép qua URL). [Báo cáo](../members/hoangnh/requisition-summary.md) |
| **2026-10-03** | Di chuyển Requisition sang Package-by-Feature & Xây dựng bộ khung kiểm thử 4 Stage | HoangNH | Tái cấu trúc module theo chuẩn `ARCHITECTURE_GUIDE.md`; khắc phục lỗi nuốt mã HTTP 403 của Global Exception; thiết lập khung kiểm thử 4 tầng (Validator, Service Unit Test, WebMvc Test, SQL Server Integration Test). [Báo cáo Merge](../members/hoangnh/2026-10-03-requisition-architecture-merge.md) |
| **2026-10-03** | Khắc phục Xung đột PR #7, #8, #10 & Nâng cấp Core | dunglt | [Xem chi tiết](work_logs/2026-10-03-dunglt-pr-unification-and-core-update.md) |
| **2026-10-03** | Đồng bộ Main (PR7 & PR8) & Giải quyết Xung đột Kiến trúc | Đức | [Xem chi tiết](work_logs/2026-10-03-merge-main-resolve-conflicts.md) |
| **2026-10-03** | Sửa lỗi Droplist Profile & Đăng xuất trên Topbar `/interviews` | Đức | [Xem chi tiết](work_logs/2026-10-03-fix-interview-topbar-dropdown.md) |
| **2026-10-03** | Đồng bộ Module Lịch phỏng vấn với Phân quyền & Bảo mật của Linh | Đức | [Xem chi tiết](work_logs/2026-10-03-interview-sync-linhdn-security.md) |
| **2026-10-03** | Tái cấu trúc Module Lịch phỏng vấn & Sửa lỗi Frontend/Backend | Đức | [Xem chi tiết](work_logs/2026-10-03-interview-refactor-and-bugfix.md) |
| **2026-10-02** | Cải tiến Giao diện & Tách CSS/JS Lịch phỏng vấn | Đức | [Xem chi tiết](work_logs/2026-10-02-interview-schedule-ui-refactor.md) |
| **2026-10-02** | Xây dựng Public Job Board & Details chuẩn kiến trúc | dunglt | [Xem chi tiết](work_logs/2026-10-02-plan-job-board.md) |
| **2026-10-01** | Đại phẫu Kiến trúc & Database | Nhóm 2 | [Xem chi tiết](work_logs/2026-10-01-architecture-refactor.md) |
| **2026-09-30** | Khởi tạo kiến trúc nền tảng phân hệ Yêu cầu tuyển dụng (Job Requisition Core) | HoangNH | Xây dựng thành công phân hệ cốt lõi Requisition: nghiệp vụ lưu nháp (Draft) linh hoạt song song với kiểm tra nghiêm ngặt khi nộp duyệt (Submit), đồng bộ khung Sidebar chuẩn cho toàn bộ ứng dụng. [Worklog](../members/hoangnh/hoangnh-requisition-worklog.md) |
| **2026-09-29** | Sáu màn hình xác thực/tài khoản | Nhóm 2 | [Xem chi tiết](work_logs/2026-09-29-auth-screens.md) |
| **2026-09-29** | Tài liệu bàn giao và kịch bản test | Nhóm 2 | [Xem chi tiết](work_logs/2026-09-29-documentation-update.md) |
| **2026-09-29** | Đồng bộ project theo database mới | Nhóm 2 | [Xem chi tiết](work_logs/2026-09-29-database-sync.md) |

---

### Quy trình cập nhật

Mỗi khi có một cụm thay đổi lớn, cần tạo mới một file Markdown trong thư mục `work_logs/` theo định dạng `YYYY-MM-DD-ten-cong-viec.md`. File này phải ghi rõ:
1. **Người thực hiện:** Ai là người code hoặc tổng hợp tài liệu.
2. **Yêu cầu/Quyết định:** Lý do tại sao thực hiện.
3. **Database:** Có thay đổi gì tới cấu trúc DB, script seed không.
4. **Mã nguồn/Tài liệu:** Đã tạo, xóa hay sửa những module nào.
5. **Kiểm tra (Testing):** Cách kiểm tra (Run tay, Unit Test) và kết quả.
6. **Vấn đề tồn đọng:** Việc cần làm tiếp theo.

Sau đó, thêm 1 dòng vào bảng trên với liên kết tới file bạn vừa tạo.

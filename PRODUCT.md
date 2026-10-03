# Recruitment Management System (RMS)

<!-- impeccable:product-schema 1 -->

Ngữ cảnh sản phẩm cho toàn bộ RMS, được người dùng xác nhận ngày 01/10/2026 khi chạy `/impeccable init`. Phạm vi gồm cổng ứng viên và các màn hình vận hành nội bộ của một doanh nghiệp. Phân biệt yêu cầu sản phẩm với chức năng đã triển khai ở bên dưới.

## Platform

web

## Users

- **Candidate:** ứng viên bên ngoài doanh nghiệp; tìm hiểu công ty, tìm vị trí phù hợp, đăng ký/đăng nhập và sử dụng các chức năng bảo vệ cho hồ sơ của mình.
- **System Admin:** quản lý mọi tài khoản, bao gồm nhân viên và Candidate; theo dõi API/tích hợp và quản trị cấu hình khi yêu cầu cấu hình đã được xác nhận.
- **HR:** theo dõi tin tuyển dụng, hồ sơ ứng tuyển, giai đoạn tuyển dụng, phỏng vấn và các việc HR cần xử lý.
- **Hiring Manager:** theo dõi yêu cầu tuyển dụng của mình/phòng ban, ứng viên liên quan, phỏng vấn và các việc về offer.
- **Director:** theo dõi hàng chờ phê duyệt requisition/offer và hoạt động phê duyệt của mình.
- **Interviewer:** theo dõi buổi phỏng vấn được phân công, ứng viên liên quan và đánh giá còn thiếu.
- **Guest:** được dùng các trang công khai được xác nhận; không được vào Dashboard hoặc chức năng bảo vệ.

## Product Purpose

Hỗ trợ một doanh nghiệp quản lý tuyển dụng và giúp ứng viên tiếp cận vị trí của chính doanh nghiệp đó. Người dùng cần biết mình có thể làm gì, dữ liệu nào thuộc phạm vi của mình và bước tiếp theo trong quy trình.

Thành công được đánh giá bằng luồng thao tác hoàn chỉnh, dữ liệu đúng nguồn và quyền truy cập đúng vai trò. Hiện diện của một table, entity hoặc card chưa chứng minh một feature đã hoàn thành.

## Positioning

RMS phục vụ duy nhất một tổ chức tuyển dụng, với người dùng nội bộ và Candidate cùng có danh tính đăng nhập. Trang career site giới thiệu tổ chức này và các vị trí của tổ chức; không có nhà tuyển dụng thứ ba, directory nhiều công ty, gói giá nhà tuyển dụng hoặc thao tác đăng tin dành cho bên ngoài.

Chưa có tuyên bố marketing, số liệu hiệu quả hoặc lợi thế cạnh tranh được xác nhận.

## Operating Context

- Ứng dụng hiện có dùng Java 21, Spring Boot 3.5.16, Spring MVC, Thymeleaf, JPA/Hibernate, Spring Security và SQL Server; xem [pom.xml](pom.xml) và [README.md](README.md).
- Xác thực bằng form login và session; dự án không dùng JWT. Các form POST phải giữ CSRF.
- Tài liệu kỹ thuật của từng flow ở [docs/flows/](docs/flows/README.md), kịch bản kiểm thử ở [docs/tests/](docs/tests/README.md), quá trình làm và điểm nối tiếp ở [docs/WORK_LOG.md](docs/WORK_LOG.md).
- Các lần thay đổi phải cập nhật nhật ký và tài liệu test từng bước. Phân biệt kiểm tra code/build/context với browser, HTTP, SMTP hoặc E2E thật.
- Chạy Spring Boot cần cấu hình DB và các secret ngoài Git. Những lượt kiểm tra trước có lỗi Tomcat loopback tại máy local; xem nhật ký. Không suy ra HTTP startup đã thành công từ việc JPA khởi tạo hoặc Maven trả exit code 0.
- Homepage hiện do Spring Boot/Thymeleaf phục vụ ở `/` và `/jobs`, dữ liệu từ JobPosting thật. [prototype/index.html](prototype/index.html) là entrypoint tới ứng dụng, không duy trì bộ jobs tĩnh thứ hai. [prototype/README.md](prototype/README.md) ghi nguồn UI và cách kiểm tra.

## Capabilities and Constraints

### Yêu cầu đã chốt

- `database/schema/` là source of truth cho cấu trúc DB. Không tự thêm table/column/relationship, sửa schema để code cũ chạy hoặc thay business rule. Gặp mâu thuẫn schema/nghiệp vụ phải hỏi người dùng trước.
- Database mới được tạo riêng, không chuyển dữ liệu DB cũ. Không tự chạy script drop/create hoặc seed trên dữ liệu cần giữ.
- `User` là danh tính xác thực; `Candidate` là hồ sơ nghiệp vụ riêng, liên kết tới `User`. Schema hiện tại quy định `Candidate.UserId` NOT NULL và UNIQUE. Email Candidate đồng bộ theo `User.Email` khi cập nhật tài khoản.
- System Admin quản lý hai nhóm riêng: Internal Accounts (năm role nội bộ hiện được hỗ trợ) và Candidate Accounts (role Candidate). Phân nhóm theo role hiện tại, không theo hồ sơ Candidate lịch sử. Hai loại có lifecycle riêng; không đổi Candidate thành internal hoặc ngược lại. Khi hired, tạo User nội bộ mới với Department, giữ/deactivate Candidate cũ theo thao tác đã có. Create/Edit Internal chỉ nhận internal role; Candidate tự đăng ký qua Careers. Username/email duy nhất toàn hệ thống, kể cả account Inactive; User mới cần định danh riêng. Username cố định, Create mặc định Active và hash BCrypt; Update giữ nguyên hash.
- Trạng thái tài khoản: Active, Inactive, Blocked. Inactive/Blocked không được xác thực hoặc tiếp tục dùng tài nguyên bảo vệ. UI dùng `Deactivate`, có xác nhận và đặt Inactive, giữ record và lịch sử; không hard delete, không hiện action với account đã Inactive. Candidate oversight hiện hỗ trợ list/search/status filter/deactivate, không role conversion/Department/profile editing.
- Dashboard mở cho cả sáu vai trò đã đăng nhập, với dữ liệu theo vai trò/tài khoản/phòng ban phù hợp. Widget phải dùng nguồn thật; nguồn chưa có phải báo chưa khả dụng, không tạo metric giả.
- Forgot/Reset Password dùng link có hạn 15 phút và SMTP cấu hình ngoài Git. Người dùng đã rút phê duyệt thêm bảng reset token; thiết kế hiện tại dùng link HMAC, không thêm bảng và không dùng JWT. Không ghi secret/token/password/hash vào tài liệu hoặc log.
- Một Application được phép có nhiều AIScreeningResult để lưu lịch sử chấm lại.
- AI Configuration còn thiếu catalogue key/type/range/secret được xác nhận. Chỉ audit và trình catalogue trước khi tạo form. Không tự đặt model, endpoint, prompt hoặc threshold.
- API Monitoring hướng tới API nội bộ và dịch vụ ngoài, dựa trên yêu cầu/phản hồi thật; không mô phỏng thành công khi chưa có cấu hình.

### Chức năng có trong code tại lần init này

- GET/POST đăng ký Candidate; form login/logout; yêu cầu email reset và xử lý link reset.
- Dashboard theo sáu role; danh sách, tạo, sửa và deactivate tài khoản.
- API Monitoring có probe HTTP nội bộ và lịch sử mẫu trong bộ nhớ. Các dòng AI/email hiện chưa có probe ngoài khả dụng. Có code gửi mail reset qua SMTP, nhưng điều đó chưa làm chức năng monitoring email được triển khai.
- CareerController/CareerService phục vụ danh sách và chi tiết job công khai, query Published với deadline chưa hết theo rule Dashboard hiện có. Guest Apply lưu URL qua session rồi login trở lại đúng role. Candidate xem được dữ liệu tài khoản/profile của mình; internal accounts bị từ chối. CV upload và submission chưa có backend nên bị vô hiệu hóa và báo rõ trước login. Xem [career flow](docs/flows/career-homepage-apply-flow.md).
- Code Job Requisition của teammate có controller/service/templates trong checkout hiện tại; không coi các luồng application/interview/offer đã hoàn tất chỉ vì có entity/query. Đợt career integration không kiểm chứng đầy đủ nghiệp vụ requisition.
- Chưa có form/controller AI Configuration. Candidate notifications chưa có datasource; Dashboard hiện ghi chưa khả dụng.
- Career UI dùng cards từ `th:each` làm nguồn duy nhất cho search/filter không dấu, department counts và location. Chi tiết `/jobs/{id}` giữ salary/date/JD/requirements/benefits gốc. Nội dung branding và company-wide policy Mộc là fiction được duyệt trước; không còn JD/lương tĩnh, hash dialog hoặc `.example` contact CTA. Talent Pool hiện chỉ dẫn đến đăng ký tài khoản, không có enrollment/upload giả.

### Auth / workspace visual system — 01/10/2026

Auth và các màn Dashboard/Accounts/API hiện dùng chung wordmark, local Lora/Source Sans 3,
palette/tokens với Careers. Shared shell/form/feedback là Thymeleaf, native forms và
vanilla JS enhancement; session/CSRF/RBAC giữ nguyên, không JWT. Account Management
bao gồm Candidate; Delete xác nhận Inactive. Candidate nav không mời thao tác staff,
nhưng UI không thay quyền route Requisition của teammate. Legacy Requisition/Hello
chưa được đồng bộ visual trong đợt này. Xem [UI report](docs/design/2026-10-01-auth-admin-report.md)
và [test từng bước](docs/tests/2026-10-01-auth-admin-ui.md); static preview không phải auth E2E.

### Các quyết định còn mở

- Tên, logo, nội dung thương hiệu và thông tin liên hệ chính thức của doanh nghiệp.
- Nội dung vị trí, lương, benefits, culture và legal đã được doanh nghiệp duyệt; dữ liệu Mộc hiện tại chỉ minh họa.
- Endpoint/cấu hình thực cho AI và probe dịch vụ ngoài; catalogue AI Config; datasource notifications.
- Chính sách thu hồi mọi session sau đổi mật khẩu và các yêu cầu thu hồi link reset riêng/lưu rate limit qua nhiều node, nếu nghiệp vụ cần.
- CV storage/upload, application submit transaction, duplicate-application rule và correction route cho Candidate profile cần backend/yêu cầu được xác nhận. Browser E2E đang bị chặn bởi lỗi HTTP Tomcat loopback; không coi static preview là đăng nhập hoặc nộp hồ sơ thật.

## Brand Commitments

Tên sản phẩm trong repo là Recruitment Management System (RMS). “Mộc” chỉ là doanh nghiệp hư cấu dùng cho prototype, chưa phải tên công ty chính thức. Không chuyển placeholder thành thông tin thật.

Brief homepage đã chốt hướng thực tế, chuyên nghiệp, đáng tin, tiết chế và có yếu tố con người theo ảnh tham chiếu. Không dùng giao diện marketplace nhiều công ty, metric/testimonial/logo đối tác giả, gradient tím/xanh, glassmorphism, bố cục SaaS bento hoặc hero chữ quá lớn. Các ràng buộc homepage không tự động cho phép redesign mọi màn hình nội bộ.

Người dùng trao đổi bằng tiếng Việt. Theo xác nhận mới nhất, mọi literal giao diện/thông báo/validation phải dùng tiếng Anh; nội dung lấy từ DB (tên người, phòng ban, postingTitle, JD, requirements, benefits, salary, location…) hiển thị nguyên văn, kể cả tiếng Việt. Không dịch seed hoặc cập nhật DB để đổi ngôn ngữ.

## Evidence on Hand

- [database/schema/db.sql](database/schema/db.sql): nguồn cấu trúc DB; [database/seeds/seed_data.sql](database/seeds/seed_data.sql): dữ liệu mẫu, không phải dữ liệu vận hành.
- [SecurityConfig.java](src/main/java/com/group2/rms/config/SecurityConfig.java), controller/service/repository và template: bằng chứng chức năng hiện có và phạm vi quyền.
- [docs/flows/README.md](docs/flows/README.md): danh mục các flow đang có endpoint và giới hạn xác minh.
- [docs/recruitment-homepage-design-system.md](docs/recruitment-homepage-design-system.md) và [templates/careers/index.html](src/main/resources/templates/careers/index.html): direction và homepage hiện tại. Ảnh tham chiếu do người dùng cung cấp là art direction, không chứng minh sở hữu ảnh hay thương hiệu thực.
- [docs/tests/2026-09-30-corporate-career-homepage.md](docs/tests/2026-09-30-corporate-career-homepage.md): kết quả prototype trước init và các bước còn chưa test. Không dùng kết quả này để khẳng định E2E RMS.
- Chưa có testimonial, số liệu vận hành, company profile hoặc legal copy chính thức được duyệt để công bố.

## Product Principles

1. Mọi hành động và dữ liệu phải thuộc đúng phạm vi một doanh nghiệp và quyền của người đăng nhập.
2. Giao diện phải thể hiện đúng khả năng hệ thống hiện có, với bước tiếp theo rõ ràng và trạng thái chưa khả dụng trung thực.
3. Giữ dữ liệu nghiệp vụ và lịch sử; ưu tiên sửa nhỏ đúng root cause, tận dụng phần code còn hợp lệ.
4. Dữ liệu thật, nguồn schema và quyết định nghiệp vụ được xác nhận có quyền ưu tiên hơn giả định thiết kế.
5. Feature chỉ được ghi hoàn tất cùng tài liệu luồng, test từng bước và bằng chứng kiểm chứng phù hợp.

## Accessibility & Inclusion

Homepage phải dùng semantic HTML, nhãn form thật, focus rõ, thao tác bàn phím được, dialog đóng được, tôn trọng reduced motion và không cuộn ngang ở 375/768/1024/1440 px. Chữ thường cần tương phản tối thiểu 4.5:1. Đây là ràng buộc thiết kế/kiểm thử, chưa phải chứng nhận WCAG cho toàn RMS.

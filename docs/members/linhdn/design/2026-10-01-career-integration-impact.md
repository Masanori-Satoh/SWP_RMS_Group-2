# Career homepage — impact analysis trước implementation

## Nguồn đã kiểm tra

`database/schema/db.sql`, User/Candidate/JobPosting/JobRequisition/Application/Department,
repository, controller, SecurityConfig/UserDetails/SessionGuard, auth templates, pom,
SQL seed (read-only), prototype/index.html và các tài liệu luồng hiện có.

| Hiện trạng | Code affected | Quyết định |
|---|---|---|
| Homepage sáu cards + `const jobs` hư cấu | prototype/index.html | MODIFY: chuyển trang thật sang Thymeleaf; prototype chỉ là entry point, không giữ nguồn jobs thứ hai. |
| JobPosting repository chưa có public query | JobPostingRepository | MODIFY: đọc Published/chưa quá deadline, cùng định nghĩa active của Dashboard; fetch requisition.department. |
| Chưa có career controller/service | controller/service/templates | CREATE: GET /, /jobs, /jobs/{id}, /jobs/{id}/apply. Tái dùng entity, không tạo bảng. |
| `/` redirect Dashboard | DashboardAccessController | MODIFY: bỏ root mapping sau khi search toàn repo, Dashboard giữ nguyên. |
| GET /jobs/** permitAll | SecurityConfig | MODIFY: apply matcher trước public matcher, chỉ Candidate; / public, form login session/CSRF giữ nguyên. |
| Login luôn về Dashboard | defaultSuccessUrl(..., true) | MODIFY: dùng saved request; Dashboard vẫn là fallback khi login trực tiếp. |
| Candidate.UserId NOT NULL UNIQUE | CandidateRepository/registration | KEEP: lookup profile theo UserId; không tự tạo profile khi apply, không ghép theo email. |
| Application.AppliedCvUrl NOT NULL, chưa có upload/storage/submit service | Application/form | NEED CONFIRMATION cho backend sau: form prefill an toàn, CV và Submit disabled, chưa có POST handler/save. Không mailto, không fake success. |
| User chứa contact, Candidate chứa LinkedIn/portfolio/address | application form | CREATE: đọc đúng nguồn, không lưu snapshot/contact vào Application hoặc tự sửa User. |
| Không có Responsibilities/PreferredQualifications riêng | job detail | MODIFY: Overview & Responsibilities dùng JobDescription; Requirements dùng JobRequirements; bỏ preferred chưa có nguồn. |
| Role benefits có JobPosting.Benefits | detail/homepage | MODIFY: role benefits từ DB, company benefits viết một lần trong homepage. |
| Không có profile/My Applications route | header | KEEP: không dựng link chết; dùng Dashboard route đang có. |
| Nội dung seed/DB tiếng Việt | UI language | User xác nhận: **giữ nguyên DB, không sửa gì**. Nhãn mới English, nội dung động nguyên văn. Không dịch runtime hoặc seed. |
| Thiếu company image/contact thực | Hero/footer | CREATE: visual document/workspace minh họa bằng HTML/CSS; footer chỉ link nội bộ thật, không .example. |

## Preserve

Lora + Source Sans 3 (self-host), warm off-white/forest green, hairline borders,
compact type/spacing, semantic card links, filters gần results, search không dấu,
skip link/focus/reduced-motion/live count. Không marketplace, SaaS, gradient,
glass, người SVG, số liệu giả hoặc corporate facts mới.

## Gaps chưa tự quyết

- CV storage: vị trí lưu, loại/kích thước, scan, quyền tải xuống, retention, cleanup.
- Application submit/duplicate policy: chưa có workflow code; không tự đặt UNIQUE.
- Candidate profile edit: chưa có route, không tự mutate account trong form ứng tuyển.
- Company-wide policies vẫn là copy prototype đã được duyệt ở lượt trước; chưa là policy chính thức.

**Không sửa SQL, seed, entity mapping, thêm schema hoặc chạy drop/create.**

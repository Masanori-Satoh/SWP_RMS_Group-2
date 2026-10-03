# Audit trước đồng bộ Auth/Admin — 01/10/2026

## Phạm vi và nguồn

Đọc attachment fa512cc3-e140-42a6-8e60-0d16979d8725, homepage/style/fonts hiện tại, 4 Auth templates, Dashboard, Account List/Create/Edit, API Monitoring, fragments/head/sidebar, CSS/JS, controller mappings/forms/validation/security và tests. Không có Change Password, Account Detail, Role Management hoặc AI Configuration form/controller. Không tạo màn hình/route giả.

Hai khuyến nghị trong brief được áp dụng theo quyết định nghiệp vụ đã chốt: Account List gồm Candidate cùng internal roles; Delete đã tồn tại và là deactivation có confirmation, không hard-delete. Schema/status vẫn Active/Inactive/Blocked, không thêm enabled/locked/createdAt columns.

## Phát hiện trước chỉnh sửa

- **Confirmed high:** Auth generic centered white card/blue button khác homepage; admin CDN Inter/icons và blue/purple app shell; repeated head/topbar; dead-end nav cho profile/notifications/interviews/offers; all role labels thành badges; metrics từng card lớn/shadow; inline button màu riêng.
- **Confirmed accessibility:** inline errors chưa gắn aria-describedby/invalid; labels thiếu required hint ở Auth; sidebar mobile chiếm nhiều viewport; bảng rộng cần region có tên/keyboard/scroll cue; dialog cần Cancel focus; pagination còn “Trang/Sau”.
- **Preserve:** action/method/object/field/errors/maxlength/minlength/required; actual source lists/widgets and role scope; readonly username/no password in update; Candidate registration fields; form login/session/saved URL/BCrypt; POST logout/CSRF; Delete modal→Inactive; filters/pagination parameters; API probe target/feedback.
- Không dùng ảnh stock/blur giả workplace. Branded forest surface là lựa chọn đủ nguồn; không tạo ảnh người hoặc décor blobs.

## Impact / dependency order

| Classification | Files/component | Required change / boundary |
|---|---|---|
| KEEP | Java controllers/services/DTOs/security/entities/repos, database/schema/seeds/config | Không thay architecture/business rule/DB. Regression bằng explicit tests đã xác định không ghi DB thật. |
| MODIFY | careers.css | Extract font-face và đúng token values vào shared design-tokens; layout homepage giữ nguyên. |
| CREATE | interface.css, workspace.css, interface.js | Shared native controls/messages/focus/toggle/error summary; compact operational shell, responsive nav; không framework mới. |
| MODIFY | fragments/head.html | Thêm interfaceHead riêng; legacy head dùng bởi hello giữ nguyên contract. |
| CREATE | fragments/brand.html, auth-layout.html, workspace-header.html, messages.html | Reuse wordmark giữa public/auth/admin; Auth brand panel4pages; header/logout4operational pages; validation summary4form types. |
| MODIFY | fragments/sidebar.html | Thêm workspace fragment riêng; legacy sidebar của Requisition giữ nguyên để không ghi đè UI teammate. Chỉ route thực, Admin links theo role đã được SecurityConfig xác nhận. |
| MODIFY | Auth4pages | Same two-zone layout/types/buttons/fields, preserve no-referrer reset; visible errors and association, required marks, safe optional password toggle. |
| MODIFY | Dashboard/accounts/API templates + page CSS | Shared head/header/sidebar; thin operational hierarchy, tables/forms/real metrics; không thêm datasource/columns/actions. |
| MODIFY | account-list.js | Confirmation giữ action/data/CSRF, Cancel focus và restore trigger; không auto submit hoặc fake success. |
| MODIFY | SecurityFlowTests | Regression rendered HTML/bindings/error states + opt-in fictional visual export; actual service calls vẫn mock, không test ghi DB. |
| CREATE | Browser harness/test/flow/report docs | Four viewports, controls/error/toggle/nav/table/modal checks; chỉ fixture visual nếu real HTTP vẫn bị chặn. |

## Backend boundaries và inconsistencies

Accounts/API controller không truyền current-user display name; header chỉ dùng role System Admin được route protection xác nhận, không thêm model/backend để trang trí. Dashboard có fullName/roleName thật trong DashboardView và có thể dùng trực tiếp. Không có remember-me config; không thêm checkbox. Change/Reset Password là khác nhau, hiện chỉ Reset có route.

Requisition route hiện dùng authenticated fallback và có business code teammate; đợt này không thiết kế lại authorization của nó. New workspace giữ link route thật, không thêm quyền mới. Sidebar visibility không phải security gate. Backend/SystemConfig/notifications/CV chưa có thì không tạo links hoặc fake KPIs.

Git index SecurityConfig đang UU trước đợt này, working file compile được và không markers; không stage/finish/abort merge. Actual Tomcat loopback trước đó FAIL; cần ghi riêng Context/MockMvc/static visual và real HTTP E2E. Không chạy wildcard hoặc Database/Requisition probe tests có ghi thử, kể cả rollback.

## Skill recommendations dùng có chọn lọc

UI UX Pro Max: explicit ux queries “error summary validation”, “table mobile horizontal scroll”. Focusable linked summary đi cùng inline errors; table là ngoại lệ dùng named local scroll region, không ẩn page overflow để che lỗi. Native semantics/44px controls/input16px, font/tracking/contrast theo homepage. Không tạo design system mới thay incumbent hoặc áp SaaS/card-heavy pattern.

Impeccable: Operate cho admin, Auth là thao tác form; refinement theo homepage identity, bounded build→batched inspection→one repair batch→one confirmation. Hai review độc lập theo critique skill/quyền user đã duyệt, không coi clean detector là quality proof. Giữ giới hạn HTTP/backend rõ.

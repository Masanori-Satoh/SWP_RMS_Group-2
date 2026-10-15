# Account Management — Master–Detail

Ngày thực hiện: 15/10/2026. Phạm vi: Internal Accounts và Candidate Accounts.

## Định hướng và đối chiếu ảnh tham khảo

Ảnh tham khảo có bảng gọn, phân cấp tên/username rõ và các nhóm tài khoản dễ nhận biết. RMS tiếp tục tách hai nhóm bằng liên kết điều hướng thật, không gộp vòng đời Internal/Candidate như ảnh. Giữ toàn bộ 8 cột Internal và 7 cột Candidate, các bộ lọc hiện có, breadcrumb, H1, CTA và palette Mộc. Không thêm avatar hay thống kê minh họa.

Đã áp dụng Impeccable (Operate: ưu tiên thao tác và khả năng đọc) và UI UX Pro Max (focus rõ, focus không bị che, điều hướng bằng bàn phím). Không thêm framework/thư viện.

## Bố cục và tương tác

- Vùng rộng đủ chỗ: bảng/panel chiếm 75%/25%, gap 24px theo token. Kiểm tra cả chiều rộng tối thiểu và overflow thực tế của bảng trước khi dock.
- Vùng hẹp: bảng giữ toàn bộ chiều rộng, inspector chuyển vào native dialog dạng drawer. Giữ lựa chọn khi resize; đóng bằng nút, Escape hoặc backdrop; trả focus về nút chọn và giữ Tab/Shift+Tab trong drawer.
- Chọn tên bằng nút thật hoặc click ô dữ liệu để xem inspector. Các nút/link CRUD trong bảng không bị biến thành thao tác chọn hàng.
- Dòng được chọn có nền xanh nhẹ và trạng thái `aria-pressed`; thông báo chọn dùng live region.
- Bỏ chọn trả inspector về tổng quan. GET tìm kiếm/lọc/sort/phân trang dựng lại trang và xóa lựa chọn cũ.
- Thống kê mặc định: tổng tài khoản **khớp bộ lọc**, số đang hiển thị, trang hiện tại và số Active/Inactive/Blocked **trên trang đang xem**. Không trình bày số của một trang thành tổng hệ thống.
- Inspector dài cuộn riêng; không ép bảng nhỏ hơn mức cần thiết chỉ để giữ panel.
- Khi script inspector không tải/không có API hỗ trợ, tên trở lại văn bản; bảng, bộ lọc, link Sửa và các form trạng thái gốc vẫn tồn tại.

## Nguồn dữ liệu đã xác minh

| Dữ liệu | Nguồn |
|---|---|
| ID, tên, username, email, điện thoại, trạng thái | `[User]`, qua truy vấn phân trang có phân biệt nhóm hiện tại |
| Vai trò, phòng ban Internal | Quan hệ `User.Role`/`User.Department`, giữ tên phòng ban gốc |
| Ngày tạo/cập nhật tài khoản | `User.CreatedAt`/`UpdatedAt`, kế thừa `BaseEntity` |
| Hồ sơ Candidate | `Candidate.UserId` FK → `[User].UserId`, unique, ánh xạ `@OneToOne` |
| ID hồ sơ, ngày sinh, giới tính, địa chỉ, nguồn, ngày tạo | Candidate đã được nối đúng theo UserId bằng truy vấn batch hiện có |

Một User có thể chưa có Candidate Profile; khi thiếu, inspector nói rõ chưa có hồ sơ liên kết. Không khôi phục cột Hồ sơ và không tạo link tới màn hình profile chưa được xác nhận quyền/route.

Timestamp là `LocalDateTime`/SQL Server `DATETIME2` theo backend hiện có; frontend không đổi sang UTC hoặc tự suy diễn timezone. Thiếu ngày cập nhật được ghi rõ chưa có dữ liệu.

`AccountListService` chỉ thêm ánh xạ DTO đọc. Candidate sử dụng lại truy vấn batch đã có; Internal không truy vấn Candidate. Không thêm query/endpoint, không đổi điều kiện lọc, số dòng/trang, business logic, schema, entity, authentication hoặc authorization. Không xuất password hash hay entity ra inspector.

## Giữ hợp đồng thao tác

- Sửa trong inspector dùng đúng href của link Sửa trên dòng được chọn. Candidate không xuất hiện nút Sửa hoặc Tạo mới.
- Nút đổi trạng thái trong inspector liên kết bằng thuộc tính HTML `form` tới **form gốc** của dòng. Giữ CSRF, method, action, nội dung xác nhận và listener `status-confirmation.js` hiện tại; không clone form hoặc tự viết luồng POST.
- Nhánh Inactive dùng Activate; Active/Blocked giữ Deactivate theo nghiệp vụ sẵn có.
- Các endpoint admin vẫn được bảo vệ bởi `RoleAuthorities.SYSTEM_ADMIN`; không sửa SecurityConfig hoặc cơ chế kiểm tra nhóm tại service.

## File của thay đổi này

1. `src/main/java/com/group2/rms/user/service/AccountListService.java`: DTO inspector và ánh xạ đọc, giữ constructor DTO cũ để tương thích.
2. `src/main/resources/templates/admin/accounts/list.html`: ghép master/inspector, nút chọn tên và ID form.
3. `src/main/resources/templates/admin/accounts/candidates.html`: cùng cơ chế, giữ riêng nhóm và 7 cột.
4. `src/main/resources/templates/admin/accounts/inspector.html`: fragment riêng trong Account Management cho tổng quan, chi tiết và profile liên kết.
5. `src/main/resources/static/css/pages/account-inspector.css`: layout, trạng thái chọn, panel/drawer và style có tiền tố `aim-`.
6. `src/main/resources/static/js/pages/account-inspector.js`: progressive enhancement, chọn hàng, native form association, resize và focus.
7. `src/test/java/com/group2/rms/service/AccountInspectorMappingTests.java`: 3 test ánh xạ, profile thiếu và trang rỗng.
8. Báo cáo này và ảnh trong `account-master-detail/`.

Các thay đổi Department đã có trước lượt này được giữ nguyên; không sửa thêm module đó hoặc file shell/component/token dùng chung.

## Kết quả kiểm tra

- Maven build thành công. 16 test liên quan hiện có đạt: AccountListService (2), AccountManagementService (7), AccountActivationDepartment (5), AdminDashboardIntegration (2). Ba test inspector mới đạt. Tổng: **19 test Java đạt**.
- **2 test JavaScript hiện có đạt**; `node --check` cho script mới đạt.
- Chrome với SQL Server local: đối chiếu đầy đủ dòng bảng, cột, query field/option, href Sửa, POST/form metadata và CSRF với bản trước. Dữ liệu không thay đổi: 12 Internal / 192 Candidate ở thời điểm kiểm tra.
- Ma trận 1280/1366/1440/1920px × sidebar mở/thu gọn × 2 nhóm: 16 trường hợp desktop đạt. 1920px/1080px dùng panel 75/25; các viewport nhỏ hơn trong ma trận dùng drawer. Kiểm tra thêm 768px và 390px.
- Đạt: chọn tên/ô dữ liệu, chọn dòng khác, Bỏ chọn, tổng quan, đổi nhóm, tìm kiếm, bốn sort, năm role thật, phòng ban thật, giữ filter khi phân trang, empty results, resize khi đang chọn, Escape, trả focus, vòng Tab/Shift+Tab.
- Đạt: confirmation/cancel trong drawer, focus giữa hai dialog, POST form gốc có CSRF đúng một lần. **POST bị chặn bằng browser interception**, không kích hoạt/vô hiệu hóa tài khoản thật.
- Ba nhánh Active/Inactive/Blocked được kiểm tra thêm bằng HTML fixture do test MockMvc hiện có render; chỉ dùng trong trình duyệt kiểm thử, không đưa dữ liệu giả vào ứng dụng/database.
- Script inspector bị chặn: bảng, link Sửa và form gốc vẫn truy cập được. Department không nạp CSS/JS inspector. Không có JavaScript error trong các lượt nghiệm thu đã hoàn tất.

### Giới hạn kiểm chứng

- Không ghi dữ liệu CRUD thật và không thử đăng nhập lại sau thay đổi trạng thái thật.
- Dữ liệu thật hiện toàn Active; case Inactive/Blocked dùng fixture. Thiếu Candidate Profile được kiểm tra bằng unit test, không tạo bản ghi thiếu liên kết trong database để thử.
- Chưa kiểm tra thủ công bằng trình đọc màn hình, Firefox hoặc Safari.
- JaCoCo có cảnh báo execution data cũ không khớp một số class; không dùng báo cáo đó để khẳng định coverage. Các test nêu trên đều pass.
- Runtime kiểm tra tạm dừng một lần giữa lượt bổ sung; đã khởi động lại và hoàn tất các kiểm tra còn lại.

Không commit, push, merge hoặc deploy.

## Ảnh before/after

| Nhóm | Before | Tổng quan | Inspector | Drawer |
|---|---|---|---|---|
| Internal | [Before](account-master-detail/internal-before.png) | [Summary](account-master-detail/internal-summary.png) | [After](account-master-detail/internal-after.png) | [1440px](account-master-detail/internal-drawer.png) |
| Candidate | [Before](account-master-detail/candidate-before.png) | [Summary](account-master-detail/candidate-summary.png) | [After](account-master-detail/candidate-after.png) | [1440px](account-master-detail/candidate-drawer.png) |

Before/panel chụp 1920×1080, sidebar mở. Drawer chụp đúng viewport 1440×900 để thể hiện overlay thực tế.

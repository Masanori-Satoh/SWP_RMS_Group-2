# Tổng kết cập nhật RMS — 16/10/2026

## 1. Phạm vi

Cập nhật chức năng lưu ứng viên tiềm năng, ghi chú chung, xem nhanh CV và bố cục hồ sơ ứng tuyển. Ghi nhận riêng các thay đổi admin do người dùng thực hiện.

- Nền tảng hiện tại: Spring Boot, Thymeleaf, HTML/CSS/JavaScript.
- Giữ phong cách giao diện mộc. RMS, sử dụng token và component sẵn có.
- Áp dụng Impeccable và UI UX Pro Max cho phần giao diện ứng viên.
- Tài liệu bao gồm đợt triển khai ban đầu và lượt tinh gọn giao diện/bổ sung bỏ lưu theo phản hồi. Kế hoạch 11 commit ở cuối tài liệu thuộc đợt ban đầu; trạng thái commit thực tế cần xem bằng `git status`/`git log`.
- Không thay đổi schema database, không chạy SQL cập nhật dữ liệu và không commit/push trong quá trình thực hiện.

## 2. Danh sách hồ sơ ứng tuyển

Trang `/applications`, bao gồm danh sách được lọc theo Job Posting, được bổ sung:

| Thành phần | Nội dung cập nhật |
| --- | --- |
| Icon mắt | Mở modal xem nhanh CV ngay trên danh sách. |
| Icon bookmark | Lưu ứng viên vào kho tiềm năng chung hoặc mở ghi chú nếu đã lưu. |
| Icon hồ sơ | Thay nút chữ “Xem chi tiết”; tooltip và nhãn truy cập vẫn mô tả rõ hành động. |
| Badge Tiềm năng | Tái sử dụng trạng thái `Candidate.IsPotential`; không tạo trạng thái bookmark riêng. |
| Thao tác khác | Bỏ menu ba chấm. Phỏng vấn/offer là icon trực tiếp theo quyền; mở CV trong tab mới nằm trong modal xem CV. |
| Bảng dữ liệu | Giữ tên, email, vị trí, ngày nộp, điểm AI, trạng thái và người đang xử lý. |
| Điều hướng | Giữ header, sidebar, bộ lọc, thanh giai đoạn và phân trang. |

### Trước và sau khi lưu

1. **Chưa lưu:** bookmark dạng viền, tooltip “Lưu vào kho ứng viên tiềm năng”.
2. **Mở modal:** tải trạng thái mới nhất từ server; có thể nhập ghi chú tối đa 1.000 ký tự, không bắt buộc khi lưu lần đầu.
3. **Lưu thành công:** bookmark được tô màu, badge Tiềm năng xuất hiện; trạng thái được lưu bằng backend hiện có.
4. **Đã lưu:** tooltip “Đã lưu vào kho tiềm năng”; mở lại để xem các ghi chú và bổ sung ghi chú mới.
5. **Cùng ứng viên xuất hiện nhiều dòng trên trang:** các bookmark tương ứng được cập nhật sau thao tác lưu thành công.
6. **Bỏ lưu:** bấm “Bỏ lưu tiềm năng” rồi xác nhận trong modal. Bookmark trở về viền, badge được ẩn; ứng viên ra khỏi kho chung nhưng hồ sơ/CV/ghi chú được giữ lại. Có thể lưu lại sau đó.

Trạng thái lưu thuộc kho tiềm năng toàn hệ thống, không phải danh sách riêng của từng HM.

## 3. Xem nhanh CV

- Modal hiển thị tên ứng viên và vị trí ứng tuyển.
- Modal gần toàn màn hình, iframe chiếm phần chiều cao còn lại; bỏ dòng hướng dẫn dài dưới CV. Gửi gợi ý vừa chiều ngang/ẩn khung điều hướng cho trình đọc PDF; việc hỗ trợ phụ thuộc trình duyệt.
- Tải CV bằng endpoint hiện có: `GET /applications/{id}/cv`.
- Kiểm tra phản hồi và định dạng PDF trước khi hiển thị trong iframe.
- Có trạng thái đang tải, thông báo lỗi và hướng dẫn khi không hỗ trợ xem trước.
- Có nút đóng, mở CV trong tab mới và xem hồ sơ chi tiết.
- Không thay URL danh sách khi mở modal; các bộ lọc đang dùng được giữ nguyên.
- Không tạo CV mẫu, dữ liệu giả hoặc dùng localStorage để mô phỏng lưu thành công.
- Hủy yêu cầu tải và giải phóng URL tạm khi đóng cửa sổ xem CV.

## 4. Ghi chú chung

Modal chia hai cột: lịch sử ghi chú bên trái, ô bổ sung ghi chú bên phải. Trên màn hình nhỏ hai phần xếp dọc. Chỉ giữ tên ứng viên, tác giả, thời gian, nội dung và các thao tác cần thiết; bỏ các đoạn giải thích phạm vi, trạng thái lưu và dòng “Tối đa 1.000 ký tự”. Giới hạn nhập/validation 1.000 ký tự vẫn được giữ ở HTML và backend.

- Tái sử dụng bảng/entity và chức năng ghi chú tiềm năng hiện có.
- Hiển thị nội dung, tác giả và thời gian của từng ghi chú.
- Ghi chú mới được thêm riêng, không ghi đè hoặc xóa ghi chú của người khác.
- Khi ứng viên đã được lưu, thao tác “Thêm ghi chú” yêu cầu nội dung không trống.
- Ghi chú do người dùng nhập được đưa vào DOM bằng `textContent`; template dùng `th:text`.
- Có xử lý lỗi tải/lưu, nút tải lại ghi chú và trạng thái chờ lưu để hạn chế gửi lặp.
- Candidate không được truy cập endpoint ghi chú chung mới; HM/HR phải có quyền xem hồ sơ liên quan.

## 5. Bố cục trang chi tiết hồ sơ

Áp dụng cho `/applications/{id}`, bao gồm `/applications/218`.

| Trước | Sau |
| --- | --- |
| Hành trình nằm dưới khung CV dài. | Hành trình nằm trong cột bên cạnh CV trên desktop. |
| Ghi chú tiềm năng trình bày bằng danh sách đơn giản. | Có khu vực Ghi chú chung, tác giả/thời gian rõ ràng và nút bổ sung ghi chú. |
| Khối thao tác duyệt chiếm toàn chiều ngang trước CV. | Khối Đánh giá hồ sơ nằm cùng cột hành trình và ghi chú. |
| Liên hệ và AI nằm trong cột bên cạnh CV. | Liên hệ và AI chuyển xuống dưới khu vực CV. |
| Màn hình nhỏ có thể phải cuộn qua PDF mới thấy hành trình. | Hành trình, ghi chú và đánh giá xuất hiện trước CV theo thứ tự DOM. |

Giữ các form và điều kiện quyền hiện có cho duyệt CV, chấm AI, phỏng vấn, offer, xác nhận đề xuất và gửi lại email dừng tuyển. CSS chỉ nằm trong file của trang; không sửa khung layout hoặc token dùng chung.

## 6. Tích hợp backend tối thiểu

Mở rộng `TalentPoolController` và `TalentPoolService` hiện có; không tạo controller/service/repository riêng cho một subsystem mới.

| Endpoint | Mục đích |
| --- | --- |
| `GET /talent-pool/applications/{id}/bookmark` | Trả về candidateId, trạng thái đã lưu và danh sách ghi chú được phép xem. |
| `POST /talent-pool/applications/{id}/bookmark` | Lưu cờ tiềm năng và ghi chú tùy chọn trong cùng transaction. |
| `POST /talent-pool/applications/{id}/bookmark/remove` | Bỏ cờ tiềm năng chung, giữ nguyên hồ sơ, CV và lịch sử ghi chú. |

- Request sử dụng DTO `TalentPoolBookmarkRequest`; giới hạn nội dung 1.000 ký tự.
- Response sử dụng DTO `TalentPoolBookmarkResponse`.
- POST giữ bảo vệ CSRF hiện có.
- Service kiểm tra vai trò HR/Hiring Manager và quyền truy cập Application.
- Tái sử dụng `markPotential`, `addNote` và `notesFor`.
- Audit đánh dấu tiềm năng chỉ được tạo khi cờ chuyển từ chưa lưu sang đã lưu.

**Lưu ứng viên chỉ là lưu để tham khảo. Thao tác này không:**

- Duyệt CV hoặc thay đổi `ApplicationStatus`.
- Chuyển hồ sơ sang phỏng vấn.
- Tạo Application mới.
- Đề xuất ứng viên vào một Job Posting.
- Gửi yêu cầu HR liên hệ hoặc gửi email.

Cờ tiềm năng không bị thay đổi bởi thao tác bổ sung ghi chú, kể cả khi hồ sơ đã bị từ chối. Chức năng bỏ lưu có kiểm tra vai trò, quyền xem Application và CSRF; ghi audit `TalentPoolUnmarked` khi cờ thực sự chuyển về false. Gọi bỏ lưu lại không tạo thêm audit nếu cờ đã false. Lịch sử ghi chú vẫn có thể đọc từ hồ sơ bởi HR/HM có quyền sau khi bỏ lưu.

## 7. Phần admin do người dùng cập nhật

### Sidebar admin

Trong `templates/fragments/layout/sidebars/admin.html`, đã bỏ các shortcut tuyển dụng không cần thiết:

- Yêu cầu tuyển dụng.
- Tin tuyển dụng.
- Hồ sơ ứng tuyển.
- Lịch phỏng vấn.
- Đề nghị tuyển dụng / Offer.

Giữ Bảng điều khiển, Tài khoản nội bộ, Tài khoản ứng viên, Quản lý phòng ban và Giám sát API. Thay đổi này là điều chỉnh menu, không phải thay đổi quyền truy cập backend.

### Định dạng code quản lý tài khoản

`AccountController.java` và `AccountListService.java` có thay đổi cách xuống dòng, comment và sử dụng import kiểu thời gian. Diff đã kiểm tra không thể hiện thay đổi logic nghiệp vụ; phần này được tách riêng khỏi commit xóa shortcut.

## 8. Danh sách file thay đổi

Các đường dẫn dưới đây tính từ thư mục project `SWP_RMS_Group-2`.

| File | Loại | Vai trò |
| --- | --- | --- |
| `src/main/java/com/group2/rms/candidate/dto/TalentPoolBookmarkRequest.java` | Thêm | DTO ghi chú tùy chọn và validation. |
| `src/main/java/com/group2/rms/candidate/dto/TalentPoolBookmarkResponse.java` | Thêm | DTO trạng thái lưu và danh sách ghi chú. |
| `src/main/java/com/group2/rms/candidate/service/TalentPoolService.java` | Sửa | Đọc/lưu bookmark, tái sử dụng nghiệp vụ hiện có. |
| `src/main/java/com/group2/rms/candidate/controller/TalentPoolController.java` | Sửa | Hai endpoint JSON phục vụ modal. |
| `src/main/java/com/group2/rms/candidate/controller/ApplicationPipelineController.java` | Sửa bổ sung | Giữ lịch sử ghi chú trên trang hồ sơ sau khi bỏ lưu. |
| `src/main/resources/templates/candidate/application-dialogs.html` | Thêm | Fragment modal CV và ghi chú. |
| `src/main/resources/static/js/pages/applications.js` | Thêm | Tải CV, xử lý modal, lưu và hiển thị ghi chú. |
| `src/main/resources/static/css/pages/applications.css` | Sửa | Layout, trạng thái bookmark và responsive. |
| `src/main/resources/templates/candidate/applications.html` | Sửa | Nút xem nhanh/lưu trên bảng hồ sơ. |
| `src/main/resources/templates/candidate/application-detail.html` | Sửa | Hành trình và ghi chú cạnh CV. |
| `src/test/java/com/group2/rms/candidate/service/TalentPoolBookmarkTests.java` | Thêm | Kiểm tra quyền, lưu cờ, ghi chú và giữ workflow. |
| `src/test/java/com/group2/rms/candidate/ApplicationPipelineWebTests.java` | Sửa | Kiểm tra MVC, CSRF, endpoint và UI. |
| `src/main/resources/templates/fragments/layout/sidebars/admin.html` | Người dùng sửa | Bỏ shortcut admin không cần thiết. |
| `src/main/java/com/group2/rms/user/controller/AccountController.java` | Người dùng sửa | Format code. |
| `src/main/java/com/group2/rms/user/service/AccountListService.java` | Người dùng sửa | Format, comment và import. |

## 9. Kiểm tra đã thực hiện và giới hạn

Nhóm test cô lập đã chạy thành công ở lượt triển khai:

| Nhóm test | Số test | Kết quả |
| --- | ---: | --- |
| `ApplicationPipelineWebTests` | 30 | Passed |
| `ApplicationAccessTests` | 56 | Passed |
| `ApplicationStatusServiceTests` (gồm các nhóm lồng) | 11 | Passed |
| `TalentPoolBookmarkTests` | 5 | Passed |
| **Tổng** | **102** | **0 failure, 0 error** |

Lệnh chạy lại:

```powershell
mvn.cmd -q "-Dtest=TalentPoolBookmarkTests,ApplicationPipelineWebTests,ApplicationStatusServiceTests,ApplicationAccessTests" test
node --check src/main/resources/static/js/pages/applications.js
git diff --check
```

Ngoài ra đã kiểm tra cấu trúc template, ID tĩnh không trùng, không có inline style và các CSS token đều tồn tại.

**Giới hạn bằng chứng:**

- Đây là test MVC/service dùng mock, không phải kiểm thử lưu dữ liệu trên SQL Server thật.
- Chưa xác minh tương tác và bố cục desktop/mobile trực tiếp bằng trình duyệt: lượt đầu bị xét duyệt tự động chặn do hạn mức, lượt thử lại gặp `ERR_BLOCKED_BY_CLIENT` khi mở localhost.
- Chưa kiểm thử riêng luồng admin bằng trình duyệt.
- Nhóm test được chạy lại khi bổ sung bỏ lưu và tinh gọn giao diện; kết quả cập nhật trong bảng trên.

## 10. Hướng dẫn kiểm tra thủ công

1. Khởi động lại ứng dụng để nạp thay đổi Java; tải lại trang bằng Ctrl+F5.
2. Đăng nhập HM có quyền, mở Job Posting 8 và chọn “Xem hồ sơ ứng tuyển”.
3. Bấm icon mắt: kiểm tra tên/vị trí, PDF, nút đóng, mở tab mới và link hồ sơ chi tiết. Đóng modal phải giữ nguyên danh sách/bộ lọc.
4. Chọn một ứng viên chưa lưu, bấm bookmark và lưu không có ghi chú. Kiểm tra icon/badge cập nhật và còn giữ sau khi tải lại trang.
5. Mở lại bookmark, bổ sung ghi chú; kiểm tra tác giả, thời gian và các ghi chú cũ vẫn còn. Dùng HR hoặc HM có quyền để kiểm tra khả năng xem chung.
6. Đối chiếu trạng thái hồ sơ trước/sau lưu: không được tự đổi giai đoạn, tạo đề xuất hoặc gửi email.
7. Mở `/applications/218`: kiểm tra hành trình/ghi chú cạnh CV trên desktop và trước CV trên màn hình nhỏ; các nút duyệt vẫn hoạt động theo quyền.
8. Thử lỗi mạng, CV không tồn tại hoặc tài nguyên không phải PDF; kiểm tra thông báo và đường mở CV/hồ sơ thay thế.
9. Kiểm tra đóng modal bằng Escape, điều hướng Tab và focus quay lại nút mở. Trong lúc đang lưu, modal chặn đóng để tránh thao tác mơ hồ.
10. Đăng nhập admin và kiểm tra sidebar chỉ còn các mục đã giữ ở phần 7.
11. Bấm bookmark của ứng viên đã lưu → “Bỏ lưu tiềm năng” → xác nhận. Tải lại danh sách/kho tiềm năng, kiểm tra ứng viên đã ra khỏi kho và trạng thái hồ sơ không đổi. Lưu lại và kiểm tra lịch sử ghi chú vẫn còn.

Các bước lưu ghi chú hoặc duyệt hồ sơ thực sự có thể ghi dữ liệu; nên thực hiện trên dữ liệu thử nghiệm phù hợp.

## 11. Phương án chia commit đã đề xuất

Đây là kế hoạch chia commit, chưa phải lịch sử commit đã thực hiện.

| Thứ tự | Commit message | Phạm vi |
| ---: | --- | --- |
| 1 | `feat(candidate): add bookmark request and response DTOs` | Hai DTO mới. |
| 2 | `feat(candidate): save shared bookmarks without changing application workflow` | Service và unit test bookmark. |
| 3 | `feat(candidate): expose bookmark and shared note endpoints` | Controller. |
| 4 | `feat(candidate): add CV preview and shared note dialogs` | Fragment modal. |
| 5 | `feat(candidate): connect CV preview and bookmark dialog interactions` | JavaScript. |
| 6 | `style(candidate): refine application layout and dialog responsiveness` | CSS. |
| 7 | `feat(candidate): add CV preview and bookmark actions to application list` | Template danh sách. |
| 8 | `refactor(candidate): place timeline and shared notes beside CV` | Template chi tiết. |
| 9 | `test(candidate): cover bookmark endpoints and application UI` | Test MVC. |
| 10 | `refactor(admin): remove unnecessary recruitment sidebar shortcuts` | Sidebar admin. |
| 11 | `style(admin): tidy account controller and service formatting` | Hai file Java quản lý tài khoản. |

Tài liệu tổng kết này có thể được đưa vào một commit tài liệu riêng sau các commit trên.

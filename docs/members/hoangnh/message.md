# Danh mục System Message – RMS

- **Người tổng hợp:** Nguyễn Huy Hoàng (hoangnh)
- **Phạm vi:**
  - Mục 1–6: 3 phân hệ của hoangnh (Yêu cầu tuyển dụng, Tin tuyển dụng nội bộ, Thông báo in-app). Rà soát và sửa đầy đủ.
  - Mục 7–8: các phân hệ còn lại (Auth, User/Admin, Dashboard, Core, Career, Candidate/Talent Pool, Interview, Offer).
    **Chỉ đánh giá, chưa sửa code**, để người phụ trách từng phân hệ quyết định.
- **Cập nhật:** 19/10/2026

> Câu thông báo chép **nguyên văn từ code hiện tại**. `{...}` là phần được điền lúc chạy.
> Cột **Nguồn** chỉ nơi định nghĩa câu, để sửa đúng một chỗ khi cần đổi nội dung.

---

## 1. Loại thông báo (Message Type)

| Message Type | Hiển thị ở đâu | Cách sinh ra |
| :--- | :--- | :--- |
| Inline | Ngay trên màn (bảng rỗng, khối chưa có dữ liệu) | Template Thymeleaf |
| Inline validation | Dưới ô nhập trên form | Server: `{X}Validator` / `@Size`, `@Min`... / `messages.properties`. JS: báo sớm, cùng câu với server |
| Flash (thành công / lỗi) | Khung `alert alert--success` / `alert alert--danger` ở đầu trang, hiện một lần sau khi redirect | `addFlashAttribute("successMessage" / "errorMessage", ...)` trong controller |
| Confirmation dialog | Hộp thoại trước thao tác quan trọng | Template (`data-heading`, `data-message`, `<dialog>`) |
| Error page | Trang lỗi 403 / 404 | `AccessDeniedException`, `ResourceNotFoundException` → `GlobalExceptionHandler` |
| In-app notification | Chuông thông báo + trang `/notifications` | `NotificationServiceImpl` |

---

## 2. System Messages dùng chung

| # | Message code | Message Type | Context | Content | Nguồn |
| ---: | :--- | :--- | :--- | :--- | :--- |
| 1 | MSG01 | Inline validation | Ô nhập vượt quá độ dài cho phép. | Nội dung quá {max} ký tự | `RequisitionValidator`, `JobPostingValidator`, 2 file JS form |
| 2 | MSG02 | Inline validation | Ô bắt buộc để trống (ô văn bản dài của form yêu cầu khi gửi duyệt). | Vui lòng nhập trường này. | `RequisitionValidator.validateTextField`, `requisition-form.js` |
| 3 | MSG03 | Inline validation | Giá trị chọn không nằm trong danh sách cho phép (hình thức, mô hình, loại tiêu chí...). | Vui lòng chọn một tùy chọn hợp lệ. | `RequisitionValidator.validateChoiceField` |
| 4 | MSG04 | Inline validation | Dữ liệu gửi lên sai kiểu (chung). | Dữ liệu nhập vào không đúng định dạng. | `messages.properties` (`typeMismatch`) |
| 5 | MSG05 | Inline validation | Ô ngày sai định dạng. | Ngày tháng không đúng định dạng hoặc không hợp lệ. | `messages.properties` |
| 6 | MSG06 | Inline validation | Ô số thập phân (lương) sai định dạng. | Vui lòng nhập giá trị số hợp lệ. | `messages.properties` |
| 7 | MSG07 | Inline validation | Ô số nguyên (số lượng, ngày thử việc) sai định dạng. | Vui lòng nhập số nguyên hợp lệ. | `messages.properties` |
| 8 | MSG08 | Error page | Chưa đăng nhập mà mở màn Yêu cầu / Tin tuyển dụng / Thông báo. | Vui lòng đăng nhập để tiếp tục. | `RequisitionAccess.actor`, `NotificationApiController` |
| 9 | MSG09 | Error page | Tài khoản đăng nhập không còn `Active` (màn Yêu cầu / Tin tuyển dụng / API thông báo). | Tài khoản không hoạt động hoặc không tồn tại. | `RequisitionAccess.actor`, `NotificationApiController` |
| 10 | MSG10 | — | *(Đã gộp vào MSG09 ngày 19/10/2026)* | — | — |

---

## 3. Job Requisition (Yêu cầu tuyển dụng)

### 3.1 Màn danh sách / chi tiết

| # | Message code | Message Type | Context | Content | Nguồn |
| ---: | :--- | :--- | :--- | :--- | :--- |
| 11 | MSG11 | Inline | Tìm kiếm / lọc danh sách yêu cầu không có kết quả. | Không tìm thấy yêu cầu tuyển dụng nào. Hãy thử thay đổi hoặc đặt lại bộ lọc. | `requisitions/list.html` |
| 12 | MSG12 | Inline | Chi tiết: yêu cầu chưa có tiêu chí sàng lọc. | Chưa có tiêu chí sàng lọc nào. | `requisitions/detail.html` |
| 13 | MSG13 | Inline | Chi tiết: ô Lý do / Mô tả / Yêu cầu ứng viên để trống (bản nháp). | Chưa nhập. | `requisitions/detail.html` |
| 14 | MSG14 | Inline | Chi tiết: yêu cầu đã duyệt nhưng chưa có tin tuyển dụng. | Chưa có tin nào cho yêu cầu này. | `requisitions/detail.html` |
| 15 | MSG15 | Inline | Chi tiết: chưa có lịch sử hoạt động. | Chưa có hoạt động nào. | `requisitions/detail.html` |
| 16 | MSG16 | Error page (403) | Vai trò không thuộc nhóm được dùng màn Yêu cầu. | Vai trò hiện tại không có quyền truy cập Yêu cầu tuyển dụng. | `RequisitionAccess.actor` |
| 17 | MSG17 | — | *(Đã gộp vào MSG18 ngày 19/10/2026)* | — | — |
| 18 | MSG18 | Error page (404) | Mở / sửa / xóa / duyệt / rút lại yêu cầu không tồn tại. | Không tìm thấy yêu cầu tuyển dụng. | `RequisitionServiceImpl`, `RequisitionAccess.requireView` |
| 19 | MSG19 | Error page (403) | HR mở yêu cầu chưa được duyệt. | HR chỉ có quyền xem các yêu cầu tuyển dụng đã được phê duyệt. | `RequisitionAccess.requireView` |
| 20 | MSG20 | Error page (403) | Người dùng mở yêu cầu không thuộc phạm vi được xem. | Bạn không có quyền truy cập yêu cầu tuyển dụng này. | `RequisitionAccess.requireView` |

### 3.2 Form tạo / sửa / sao chép

| # | Message code | Message Type | Context | Content | Nguồn |
| ---: | :--- | :--- | :--- | :--- | :--- |
| 21 | MSG21 | Error page (403) | Vai trò không được tạo yêu cầu (HR, Candidate...). | Chỉ Hiring Manager, Giám đốc và Quản trị viên mới có quyền tạo yêu cầu tuyển dụng. | `RequisitionAccess.requireCreate` |
| 22 | MSG22 | Error page (403) | Sửa / xóa yêu cầu không ở trạng thái Bản nháp hoặc Từ chối. | Chỉ có thể chỉnh sửa hoặc xóa yêu cầu ở trạng thái Bản nháp hoặc Từ chối. | `RequisitionAccess.requireEdit` |
| 23 | MSG23 | Inline | Mở form từ nút "Sao chép thành đợt mới". | Đã sao chép vào biểu mẫu mới. Biểu mẫu chưa được lưu cho đến khi bạn chọn Lưu bản nháp hoặc {Gửi Giám đốc duyệt. \| Phê duyệt trực tiếp.} | `RequisitionController.copy` |
| 24 | MSG24 | Inline validation | Gửi form không qua 2 nút Lưu bản nháp / Gửi duyệt. | Vui lòng chọn Lưu bản nháp hoặc Gửi Giám đốc duyệt. | `RequisitionValidator` |
| 25 | MSG25 | Inline validation | Tên vị trí để trống. | Vui lòng nhập tên vị trí. | `RequisitionValidator`, `requisition-form.js` |
| 26 | MSG26 | Inline validation | Gửi duyệt khi tên vị trí vẫn là tên tạm. | Vui lòng nhập chức danh công việc cụ thể trước khi gửi duyệt. | `RequisitionValidator` |
| 27 | MSG27 | Inline validation | Địa điểm làm việc để trống. | Vui lòng nhập địa điểm làm việc. | `RequisitionValidator`, `requisition-form.js` |
| 28 | MSG28 | Inline validation | Số lượng cần tuyển để trống. | Vui lòng nhập số lượng cần tuyển. | `RequisitionValidator`, `requisition-form.js` |
| 29 | MSG29 | Inline validation | Số lượng cần tuyển ngoài khoảng cho phép. | Số lượng cần tuyển phải từ {min} đến {max} người. | `RequisitionValidator.POSITIONS_RANGE_MESSAGE`, `requisition-form.js` |
| 30 | MSG30 | Inline validation | Thời gian thử việc ngoài khoảng cho phép. | Thời gian thử việc phải từ {min} đến {max} ngày. | `RequisitionValidator.PROBATION_RANGE_MESSAGE`, `requisition-form.js` |
| 31 | MSG31 | Inline validation | Lương ≤ 0 hoặc quá 2 chữ số thập phân. | Mức lương phải lớn hơn 0 với tối đa 2 chữ số thập phân. | `RequisitionValidator.SALARY_FORMAT_MESSAGE`, `requisition-form.js` |
| 32 | MSG32 | Inline validation | Lương vượt trần. | Mức lương không được vượt quá 1.000.000.000 VND/tháng. | `RequisitionValidator.SALARY_MAX_MESSAGE`, `requisition-form.js` |
| 33 | MSG33 | Inline validation | Lương tối đa nhỏ hơn lương tối thiểu. | Mức lương tối đa phải lớn hơn hoặc bằng mức lương tối thiểu. | `RequisitionValidator`, `requisition-form.js` |
| 34 | MSG34 | Inline validation | Gửi duyệt với ngày bắt đầu trong quá khứ. | Ngày bắt đầu dự kiến phải từ hôm nay trở đi. | `RequisitionValidator`, `requisition-form.js` |
| 35 | MSG35 | Inline validation | Ngày bắt đầu quá xa. | Ngày bắt đầu dự kiến không được quá {n} năm kể từ hôm nay. | `RequisitionValidator`, `requisition-form.js` |
| 36 | MSG36 | Inline validation | Giám đốc tạo yêu cầu mà chưa chọn phòng ban / chọn phòng ban ngừng hoạt động. | Vui lòng chọn phòng ban đang hoạt động. | `RequisitionServiceImpl`, `requisition-form.js` |
| 37 | MSG37 | Inline validation | Người tạo (không phải Giám đốc) chưa được gán phòng ban đang hoạt động. | Tài khoản của bạn chưa được gán phòng ban đang hoạt động. Vui lòng liên hệ quản trị viên. | `RequisitionServiceImpl` |
| 38 | MSG38 | Inline validation | Mã phòng ban gửi lên không tồn tại. | Phòng ban không tồn tại. | `RequisitionServiceImpl` |
| 39 | MSG39 | Inline validation | Tạo mới khi đã có bản nháp cùng vị trí trong phòng ban. | Bạn đang có một bản nháp cho vị trí này trong phòng ban. Vui lòng hoàn thiện bản nháp hiện có. | `RequisitionServiceImpl` |
| 40 | MSG40 | Inline validation | Sửa bản nháp thành trùng vị trí với một bản nháp khác. | Bạn đang có một bản nháp khác cho vị trí này trong phòng ban. | `RequisitionServiceImpl` |
| 41 | MSG41 | Inline validation | Thêm quá số tiêu chí tối đa. | Chỉ được thêm tối đa {max} tiêu chí. | `RequisitionValidator`, `requisition-form.js` |
| 42 | MSG42 | Inline validation | Dòng tiêu chí gửi lên rỗng (dữ liệu lỗi). | Tiêu chí không hợp lệ. | `RequisitionValidator` |
| 43 | MSG43 | Inline validation | Dòng tiêu chí mới nhập dở (JS kiểm tra cả dòng). | Mỗi tiêu chí đã nhập cần đủ tên, loại, giá trị yêu cầu và trọng số. | `requisition-form.js` |
| 44 | MSG44 | Inline validation | Hai tiêu chí trùng tên. | Tên các tiêu chí không được trùng nhau. | `RequisitionValidator`, `requisition-form.js` |
| 45 | MSG45 | Inline validation | Một tiêu chí cũ bị gửi lên hai lần (dữ liệu lỗi). | Tiêu chí đã được gửi hai lần. | `RequisitionValidator` |
| 46 | MSG46 | Inline validation | Trọng số để trống. | Vui lòng nhập trọng số. | `RequisitionValidator` |
| 47 | MSG47 | Inline validation | Trọng số ngoài 0.01–100 hoặc quá 2 chữ số thập phân. | Trọng số phải từ 0.01 đến 100 với tối đa 2 chữ số thập phân. | `RequisitionValidator.WEIGHT_MESSAGE` |
| 48 | MSG48 | Inline validation | Gửi duyệt khi chưa có tiêu chí hoặc tổng trọng số ≠ 100%. | Vui lòng thêm đầy đủ tiêu chí với tổng trọng số đúng bằng 100%. | `RequisitionValidator`, `requisition-form.js` |
| 49 | MSG49 | Inline validation | Tiêu chí mới mang mã của tiêu chí có sẵn (dữ liệu lỗi). | Tiêu chí mới không được tham chiếu bản ghi có sẵn. | `RequisitionServiceImpl` |
| 50 | MSG50 | Inline validation | Sửa: mã tiêu chí không thuộc yêu cầu đang sửa (dữ liệu lỗi). | Một tiêu chí không thuộc về yêu cầu này. | `RequisitionServiceImpl` |
| 51 | MSG51 | Inline validation | Sao chép từ yêu cầu không còn tồn tại. | Yêu cầu nguồn để sao chép không tồn tại. | `RequisitionServiceImpl` |
| 52 | MSG52 | Inline validation (đầu form) | DB từ chối lưu (trùng khóa, sai ràng buộc). | Không thể lưu dữ liệu. Vui lòng kiểm tra lại tính trùng lặp tiêu chí và giá trị các trường. | `RequisitionController.STORAGE_ERROR` |
| 53 | MSG53 | Confirmation dialog | HM / Admin bấm Gửi duyệt. | **Gửi cho Giám đốc duyệt?** Sau khi gửi, bạn không sửa được nữa trừ khi rút lại yêu cầu hoặc bị Giám đốc trả lại. | `requisitions/form.html` |
| 54 | MSG54 | Confirmation dialog | Giám đốc bấm Phê duyệt trực tiếp. | **Phê duyệt yêu cầu này?** HR có thể tạo tin tuyển dụng ngay và yêu cầu không sửa được nữa. | `requisitions/form.html` |
| 55 | MSG55 | Flash (thành công) | Lưu bản nháp. | Đã lưu bản nháp thành công. | `RequisitionController.savedMessage` |
| 56 | MSG56 | Flash (thành công) | HM / Admin gửi duyệt. | Yêu cầu đã được gửi lên Giám đốc phê duyệt. | `RequisitionController.savedMessage` |
| 57 | MSG57 | Flash (thành công) | Giám đốc phê duyệt trực tiếp. | Yêu cầu đã được phê duyệt. HR có thể tạo tin tuyển dụng ngay. | `RequisitionController.savedMessage` |

### 3.3 Xóa / Duyệt / Trả lại / Rút lại / Sao chép

| # | Message code | Message Type | Context | Content | Nguồn |
| ---: | :--- | :--- | :--- | :--- | :--- |
| 58 | MSG58 | Confirmation dialog | Bấm Xóa yêu cầu (danh sách hoặc chi tiết). | **Xóa yêu cầu tuyển dụng** Bạn có chắc chắn muốn xóa "{tên}" cùng toàn bộ tiêu chí và lịch sử phê duyệt liên quan? Hành động này không thể hoàn tác. Lịch sử nhật ký hệ thống vẫn sẽ được lưu trữ. | `requisitions/list.html`, `detail.html` |
| 59 | MSG59 | Flash (thành công) | Xóa thành công. | Đã xóa yêu cầu tuyển dụng thành công. | `RequisitionController.delete` |
| 60 | MSG60 | Flash (lỗi) | Xóa yêu cầu đã có tin tuyển dụng. | Yêu cầu này đã được liên kết với tin đăng tuyển và không thể xóa. | `RequisitionServiceImpl` |
| 61 | MSG61 | Flash (lỗi) | Xóa yêu cầu đã được sao chép sang đợt sau. | Yêu cầu này đã có đợt tuyển sau được sao chép từ nó và không thể xóa. | `RequisitionServiceImpl` |
| 62 | MSG62 | Flash (lỗi) | DB chặn xóa do còn dữ liệu liên kết khác. | Yêu cầu này đã được liên kết với dữ liệu khác và không thể xóa. | `RequisitionController.delete` |
| 63 | MSG63 | Flash (lỗi) | Gửi quyết định không phải approve / reject. | Vui lòng chọn quyết định phê duyệt. | `RequisitionController.decide` |
| 64 | MSG64 | Error page (403) | Người không phải Giám đốc (hoặc chính người tạo) gửi quyết định. | Chỉ Giám đốc mới có quyền quyết định phê duyệt yêu cầu. | `RequisitionServiceImpl.decide` |
| 65 | MSG65 | — | *(Đã gộp vào MSG66 ngày 19/10/2026)* | — | — |
| 66 | MSG66 | Inline validation (JS) / Flash (lỗi) | Giám đốc bấm Trả lại mà chưa nhập lý do (JS chặn trước, server kiểm tra lại). | Vui lòng nhập lý do để Hiring Manager chỉnh sửa. | `static/js/requisitions.js`, `RequisitionServiceImpl.decide` |
| 67 | MSG67 | Flash (lỗi) | Ý kiến / lý do vượt {max} ký tự. | Nội dung quá {max} ký tự | `RequisitionServiceImpl.decide` |
| 68 | MSG68 | Flash (thành công) | Giám đốc duyệt hoặc từ chối thành công. | Đã lưu quyết định thành công. | `RequisitionController.decide` |
| 69 | MSG69 | Error page (403) | Người không phải người tạo rút lại yêu cầu. | Chỉ người tạo yêu cầu mới có quyền rút lại yêu cầu đang chờ duyệt. | `RequisitionServiceImpl.withdraw` |
| 70 | MSG70 | Flash (thành công) | Rút lại yêu cầu đang chờ duyệt. | Đã rút lại yêu cầu thành công. Bạn có thể chỉnh sửa lại bản nháp. | `RequisitionController.withdraw` |
| 71 | MSG71 | Inline validation | Sao chép yêu cầu chưa Đã duyệt / Từ chối. | Chỉ có thể sao chép yêu cầu đã hoàn tất phê duyệt hoặc bị từ chối. | `RequisitionServiceImpl` |

---

## 4. Job Posting (Tin tuyển dụng nội bộ)

### 4.1 Màn danh sách / chi tiết / dashboard

| # | Message code | Message Type | Context | Content | Nguồn |
| ---: | :--- | :--- | :--- | :--- | :--- |
| 72 | MSG72 | Inline | Tìm kiếm / lọc danh sách tin không có kết quả. | Không tìm thấy tin tuyển dụng nào. Hãy thử thay đổi hoặc đặt lại bộ lọc. ("đặt lại bộ lọc" là link) | `job-postings/list.html` |
| 73 | MSG73 | Inline | Chi tiết: tin chưa đăng nên chưa có hồ sơ. | Chưa có hồ sơ vì tin chưa được đăng. | `job-postings/detail.html` |
| 74 | MSG74 | Inline | Chi tiết: tin đã đăng nhưng chưa có hồ sơ. | Chưa có hồ sơ nào. | `job-postings/detail.html` |
| 75 | MSG75 | Inline | Chi tiết: chưa có lịch sử. | Chưa có lịch sử. | `job-postings/detail.html` |
| 76 | MSG76 | Inline | Dashboard HR: mọi yêu cầu đã duyệt đều đã có tin. | 🎉 Tuyệt vời! Tất cả các yêu cầu đã duyệt đều đã được đăng tin. | `job-postings/dashboard.html` |
| 77 | MSG77 | Inline | Dashboard HR: chưa có tin nào. | Chưa có tin tuyển dụng nào được tạo. | `job-postings/dashboard.html` |
| 78 | MSG78 | Inline | Dashboard HR: chưa có số liệu theo phòng ban. | Chưa có dữ liệu phòng ban cho các tin tuyển dụng. | `job-postings/dashboard.html` |
| 79 | MSG79 | Error page (403) | Vai trò không phải HR / Admin vào chức năng quản lý tin. | Chỉ HR hoặc Quản trị viên hệ thống mới có quyền quản lý tin tuyển dụng. | `JobPostingAccess` |
| 80 | MSG80 | Error page (403) | Vai trò không được xem danh sách tin. | Bạn không có quyền xem tin tuyển dụng. | `JobPostingAccess` |
| 81 | MSG81 | Error page (403) | Mở một tin không thuộc phạm vi được xem. | Bạn không có quyền xem tin tuyển dụng này. | `JobPostingAccess` |
| 82 | MSG82 | Flash (lỗi) | Mã tin gửi lên rỗng. | Mã tin tuyển dụng không hợp lệ. | `JobPostingServiceImpl` |
| 83 | MSG83 | Error page (404) | Tin không tồn tại (xem / sửa / đổi trạng thái). | Không tìm thấy tin tuyển dụng. | `JobPostingServiceImpl.findById`, `findForUpdate` |
| 84 | MSG84 | — | *(Đã gộp vào MSG83 ngày 19/10/2026)* | — | — |

### 4.2 Form tạo / sửa / đăng lại tin

| # | Message code | Message Type | Context | Content | Nguồn |
| ---: | :--- | :--- | :--- | :--- | :--- |
| 85 | MSG85 | Flash (lỗi) | Mở form tạo tin không kèm mã yêu cầu. | Mã yêu cầu tuyển dụng không hợp lệ. | `JobPostingServiceImpl.prepareCreateForm` |
| 86 | MSG86 | — | *(Đã gộp vào MSG18 ngày 19/10/2026: mã yêu cầu nguồn không tồn tại)* | — | — |
| 87 | MSG87 | Error page (404) | Lưu tin mà yêu cầu nguồn đã bị xóa. | Không tìm thấy yêu cầu tuyển dụng nguồn. | `JobPostingServiceImpl` |
| 88 | MSG88 | Flash (lỗi) / Inline validation | Tạo tin từ yêu cầu chưa được duyệt. | Chỉ có thể đăng tin từ yêu cầu đã được phê duyệt. | `JobPostingValidator.requireApproved` |
| 89 | MSG89 | Flash (lỗi) / Inline validation | Yêu cầu đã có tin nháp hoặc tin đang mở. | Yêu cầu này đã có tin nháp hoặc tin đang hoạt động. | `JobPostingServiceImpl` |
| 90 | MSG90 | Flash (lỗi) | "Đăng lại" một tin chưa đóng. | Chỉ tin đã đóng mới đăng lại được. | `JobPostingServiceImpl.prepareRepostForm` |
| 91 | MSG91 | Inline validation (đầu form) | Form tạo gửi kèm mã tin / thiếu mã yêu cầu (dữ liệu lỗi). | Yêu cầu tạo tin không hợp lệ. | `JobPostingServiceImpl.create` |
| 92 | MSG92 | Inline validation (đầu form) | Sửa tin mà đổi mã tin / mã yêu cầu nguồn (dữ liệu lỗi). | Không thể đổi tin hoặc yêu cầu tuyển dụng nguồn. | `JobPostingServiceImpl` |
| 93 | MSG93 | Inline validation (đầu form) | Sửa tin đã đóng. | Tin đã đóng không thể chỉnh sửa. | `JobPostingValidator` |
| 94 | MSG94 | Inline validation (đầu form) | Gửi form không qua nút lưu / đăng. | Thao tác lưu tin không hợp lệ. | `JobPostingValidator` |
| 95 | MSG95 | Inline validation | Tiêu đề tin để trống. | Vui lòng nhập tiêu đề tin. | `JobPostingValidator`, `job-posting-form.js` |
| 96 | MSG96 | Inline validation | Mô tả công việc để trống. | Vui lòng nhập mô tả công việc. | `JobPostingValidator`, `job-posting-form.js` |
| 97 | MSG97 | Inline validation | Yêu cầu ứng viên để trống. | Vui lòng nhập yêu cầu ứng viên. | `JobPostingValidator`, `job-posting-form.js` |
| 98 | MSG98 | Inline validation | Chưa chọn cách hiển thị lương. | Vui lòng chọn cách hiển thị mức lương. | `JobPostingValidator` |
| 99 | MSG99 | Inline validation | Chọn "Tự nhập" lương nhưng để trống. | Nhập mức lương hiển thị hoặc chọn một lựa chọn khác. | `JobPostingValidator`, `job-posting-form.js` |
| 100 | MSG100 | Inline validation | Bật "Hiển thị số lượng tuyển" nhưng để trống. | Nhập số lượng hiển thị hoặc tắt hiển thị số lượng. | `JobPostingValidator`, `job-posting-form.js` |
| 101 | MSG101 | Inline validation | Đăng tin mà chưa chọn ngày đăng. | Vui lòng chọn ngày đăng. | `JobPostingValidator` |
| 102 | MSG102 | Inline validation | Chọn "Hẹn ngày đăng" cho tin đã lên trang tuyển dụng. | Tin đã đăng nên không hẹn lại ngày đăng được. | `JobPostingValidator` |
| 103 | MSG103 | Inline validation | Chọn "Hẹn ngày đăng" nhưng chưa chọn thời điểm. | Vui lòng chọn thời điểm đăng. | `JobPostingValidator`, `job-posting-form.js` |
| 104 | MSG104 | Inline validation | Thời điểm hẹn đăng đã qua. | Thời điểm hẹn đăng phải sau thời điểm hiện tại. | `JobPostingValidator`, `job-posting-form.js` |
| 105 | MSG105 | Inline validation | Hẹn đăng quá xa. | Chỉ hẹn đăng trong vòng {n} ngày tới. | `JobPostingValidator` |
| 106 | MSG106 | Inline validation | Đăng tin mà chưa có hạn nộp. | Vui lòng chọn hạn nộp hồ sơ. | `JobPostingValidator`, `job-posting-form.js` |
| 107 | MSG107 | Inline validation | Hạn nộp trong quá khứ. | Hạn nộp hồ sơ phải từ hôm nay trở đi. | `JobPostingValidator`, `job-posting-form.js` |
| 108 | MSG108 | Inline validation | Hạn nộp trước ngày đăng / ngày hẹn đăng. | Hạn nộp hồ sơ phải sau ngày đăng. | `JobPostingValidator`, `job-posting-form.js` |
| 109 | MSG109 | Inline (gợi ý) | Đã chọn hạn nộp hợp lệ. | Nhận hồ sơ trong {n} ngày, đến hết {ngày}. | `job-posting-form.js` |
| 110 | MSG110 | Inline (cảnh báo) | Hạn nộp sau ngày bắt đầu dự kiến của yêu cầu. | Hạn nộp sau ngày bắt đầu dự kiến của yêu cầu ({ngày}). Có thể không kịp tuyển. | `job-posting-form.js` |
| 111 | MSG111 | Flash (thành công) | Tạo tin và lưu nháp. | Đã lưu bản nháp tin tuyển dụng. | `JobPostingController.savedMessage` |
| 112 | MSG112 | Flash (thành công) | Sửa tin và lưu thay đổi. | Đã lưu thay đổi. | `JobPostingController.savedMessage` |
| 113 | MSG113 | Flash (thành công) | Lưu với "Hẹn ngày đăng". | Đã lên lịch đăng tin lúc {HH:mm 'ngày' dd/MM/yyyy}. | `JobPostingController.savedMessage` |
| 114 | MSG114 | Flash (thành công) | Lưu với "Đăng ngay", hoặc bấm Đăng ngay ở tin đang hẹn. | Đã đăng tin. Ứng viên có thể nộp hồ sơ ngay. | `JobPostingController.PUBLISHED_NOW_MESSAGE` |

### 4.3 Đổi trạng thái tin (chi tiết / menu ⋯ trên danh sách)

| # | Message code | Message Type | Context | Content | Nguồn |
| ---: | :--- | :--- | :--- | :--- | :--- |
| 115 | MSG115 | Confirmation dialog | Đăng tin nháp. | **Đăng tin tuyển dụng?** Tin sẽ hiện trên trang tuyển dụng và ứng viên có thể nộp hồ sơ ngay. | `job-postings/detail.html`, `list.html` |
| 116 | MSG116 | Confirmation dialog | Đăng ngay tin đang hẹn. | **Đăng tin ngay?** Tin sẽ hiện trên trang tuyển dụng ngay bây giờ thay vì đợi tới giờ hẹn. | `job-postings/detail.html` |
| 117 | MSG117 | Confirmation dialog | Mở lại tin đang tạm dừng. | **Mở lại nhận hồ sơ?** Tin sẽ hiện lại trên trang tuyển dụng. | `job-postings/detail.html`, `list.html` |
| 118 | MSG118 | Confirmation dialog | Tạm dừng tin đang đăng. | **Tạm dừng nhận hồ sơ?** Tin bị ẩn khỏi trang tuyển dụng. Hồ sơ đã nộp vẫn được xử lý bình thường. | `job-postings/detail.html`, `list.html` |
| 119 | MSG119 | Confirmation dialog | Hủy lịch đăng. | **Hủy lịch đăng?** Tin trở về bản nháp, ứng viên sẽ không thấy tin. | `job-postings/detail.html` |
| 120 | MSG120 | Confirmation dialog | Đóng tin (chi tiết). | **Đóng tin tuyển dụng?** Hồ sơ đang xử lý sẽ dừng, lịch chưa diễn ra được hủy. Hồ sơ đã nhận việc và Offer đã chấp nhận được giữ lại. Hệ thống chặn đóng nếu còn Offer chưa kết thúc. | `job-postings/detail.html` |
| 121 | MSG121 | Confirmation dialog | Đóng tin (menu ⋯ danh sách). | **Đóng tin tuyển dụng?** Hồ sơ đang xử lý sẽ dừng, lịch chưa diễn ra được hủy. Hệ thống chặn đóng nếu còn Offer chưa kết thúc. | `job-postings/list.html` |
| 122 | MSG122 | Confirmation dialog | Xóa tin nháp. | **Xóa bản nháp?** Bản nháp sẽ bị xóa và không thể hoàn tác. | `job-postings/detail.html`, `list.html` |
| 123 | MSG123 | Flash (thành công) | Đăng tin nháp / mở lại tin tạm dừng. | Đã đăng tin tuyển dụng. | `JobPostingController.publishJobPosting` |
| 124 | MSG124 | Flash (thành công) | Hủy lịch đăng. | Đã hủy lịch đăng. Tin trở về bản nháp. | `JobPostingController.unschedule` |
| 125 | MSG125 | Flash (thành công) | Tạm dừng tin. | Đã tạm dừng tin tuyển dụng. | `JobPostingController.pauseJobPosting` |
| 126 | MSG126 | Flash (thành công) | Đóng tin. | Đã đóng đợt tuyển và xử lý các hồ sơ còn lại. | `JobPostingController.closeJobPosting` |
| 127 | MSG127 | Flash (thành công) | Xóa tin nháp. | Đã xóa tin tuyển dụng thành công. | `JobPostingController.deleteJobPosting` |
| 128 | MSG128 | Flash (lỗi) | Đăng tin không ở trạng thái Nháp / Tạm dừng. | Chỉ tin nháp hoặc đang tạm dừng mới được đăng. | `JobPostingServiceImpl` |
| 129 | MSG129 | Flash (lỗi) | Đăng tin thiếu nội dung bắt buộc. | Tin thiếu tiêu đề, mô tả hoặc yêu cầu ứng viên. | `JobPostingValidator` |
| 130 | MSG130 | Flash (lỗi) | Đăng tin chưa có hạn nộp. | Tin chưa có hạn nộp hồ sơ. Hãy chỉnh sửa tin trước khi đăng. | `JobPostingValidator` |
| 131 | MSG131 | Flash (lỗi) | Đăng / mở lại tin đã quá hạn nộp. | Hạn nộp đã qua. Hãy cập nhật tin trước khi đăng. | `JobPostingValidator` |
| 132 | MSG132 | Flash (lỗi) | Đăng ngay tin không ở trạng thái Hẹn đăng. | Chỉ tin đang hẹn đăng mới đăng ngay được. | `JobPostingServiceImpl.publishNow` |
| 133 | MSG133 | Flash (lỗi) | Hủy lịch tin không ở trạng thái Hẹn đăng. | Chỉ tin đang hẹn đăng mới hủy lịch được. | `JobPostingServiceImpl.unschedule` |
| 134 | MSG134 | Flash (lỗi) | Tạm dừng tin không ở trạng thái Đang đăng. | Chỉ tin đang đăng mới được tạm dừng. | `JobPostingServiceImpl.pauseJobPosting` |
| 135 | MSG135 | Error page (403) | Vai trò không phải HR đóng tin. | Chỉ HR được đóng đợt tuyển dụng. | `JobPostingAccess` |
| 136 | MSG136 | Flash (lỗi) | Xóa tin không phải nháp hoặc đã có hồ sơ. | Chỉ có thể xóa bản nháp chưa có hồ sơ ứng tuyển. | `JobPostingServiceImpl.deleteJobPosting` |

> Lỗi khi đóng tin do module Talent Pool / email kết quả trả về (`TalentPoolException`, `RecruitmentEmailException`)
> được hiện nguyên câu của module đó, không liệt kê ở đây.

---

## 5. Notification (Thông báo in-app)

| # | Message code | Message Type | Context | Content | Nguồn |
| ---: | :--- | :--- | :--- | :--- | :--- |
| 137 | MSG137 | In-app notification | Giám đốc từ chối yêu cầu → gửi HM tạo yêu cầu. | **Tiêu đề:** Yêu cầu tuyển dụng đã bị từ chối: {tên vị trí}<br>**Nội dung:** Yêu cầu tuyển dụng "{tên}" (Mã: {mã}) đã bị Giám đốc {tên GĐ} từ chối vào lúc {dd/MM/yyyy HH:mm}. Lý do: {lý do \| Không có lý do cụ thể}. Vui lòng chỉnh sửa lại thông tin và nộp duyệt lại. | `notifyRequisitionRejected` |
| 138 | MSG138 | In-app notification | Giám đốc duyệt yêu cầu → gửi HM tạo yêu cầu. | **Tiêu đề:** Yêu cầu tuyển dụng đã được phê duyệt: {tên vị trí}<br>**Nội dung:** Yêu cầu tuyển dụng "{tên}" (Mã: {mã}) đã được Giám đốc {tên GĐ} phê duyệt vào lúc {dd/MM/yyyy HH:mm}. [Ý kiến: {ý kiến}.] HR có thể tạo tin tuyển dụng cho yêu cầu này. | `notifyRequisitionApproved` |
| 139 | MSG139 | In-app notification | HR đăng tin (đăng ngay / mở lại) → gửi HM của yêu cầu nguồn. | **Tiêu đề:** Tin tuyển dụng đã phát hành: {tiêu đề tin}<br>**Nội dung:** Tin tuyển dụng "{tiêu đề}" (thuộc yêu cầu {mã}) đã được nhân sự {tên HR} phát hành lên cổng việc làm công khai vào lúc {dd/MM/yyyy HH:mm}. Hạn nộp hồ sơ: {dd/MM/yyyy}. Ứng viên hiện đã có thể xem chi tiết và nộp hồ sơ trực tuyến. | `notifyJobPostingPublished` |
| 140 | MSG140 | In-app notification | HR hẹn đăng tin → gửi HM của yêu cầu nguồn. | **Tiêu đề:** Tin tuyển dụng hẹn đăng: {tiêu đề tin}<br>**Nội dung:** Tin tuyển dụng "{tiêu đề}" (thuộc yêu cầu {mã}) đã được nhân sự {tên HR} hẹn đăng lên cổng việc làm công khai vào lúc {dd/MM/yyyy HH:mm}. Hạn nộp hồ sơ: {dd/MM/yyyy}. Trước thời điểm đó ứng viên chưa thấy tin. | `notifyJobPostingPublished` |
| 141 | MSG141 | Inline | Trang `/notifications` không có thông báo nào. | **Không có thông báo nào** Bạn đã xem hết toàn bộ các cập nhật hệ thống. | `notifications/list.html` |
| 142 | MSG142 | Flash (thành công) | Đánh dấu một thông báo đã đọc. | Đã đánh dấu thông báo là đã đọc. | `NotificationController` |
| 143 | MSG143 | Flash (thành công) | Đánh dấu tất cả đã đọc. | Đã đánh dấu tất cả thông báo là đã đọc. | `NotificationController` |
| 144 | MSG144 | Error page (404) | Mở / đánh dấu thông báo không tồn tại hoặc của người khác. | Thông báo không tồn tại hoặc không thuộc về bạn. | `NotificationServiceImpl.markAsRead` |

> Thông báo Offer (`notifyOfferLetterSent`, `notifyOfferResponseFromCandidate`) và email `OfferMailSender`
> nằm trong package notification nhưng thuộc phân hệ Offer, không liệt kê ở đây (xem mục 6.2).

---

## 6. Kết quả rà soát đồng bộ tiếng Việt

Tất cả câu **người dùng nhìn thấy** trong 3 phân hệ đều đã là tiếng Việt. Log (`log.warn`, `log.info`) vẫn là
tiếng Anh, nhưng log chỉ dành cho lập trình viên, không hiện lên giao diện. Còn các điểm chưa đồng bộ sau:

### 6.1 Flash message của Yêu cầu và Tin tuyển dụng (đã sửa ngày 19/10/2026)

Sau khi bỏ flash toast (18/10), 4 trang `requisitions/list`, `requisitions/detail`, `job-postings/list`,
`job-postings/detail` không còn in flash nên người dùng không thấy cả thông báo lỗi. Đã sửa:

- Mỗi trang có 2 khung alert tĩnh (UI_CATALOG §8) ngay dưới tiêu đề trang: `successMessage` màu xanh, `errorMessage` màu đỏ.
  Không thêm CSS / JS mới, khung không tự tắt.
- `RequisitionController` đổi `failureMessage` → `errorMessage`, giờ cùng tên với Job Posting và phần lớn hệ thống.

### 6.2 Câu còn trộn tiếng Anh

Đã Việt hóa ngày 19/10/2026:

| Code | Câu trước | Câu hiện tại |
| :--- | :--- | :--- |
| MSG16 | Vai trò hiện tại không có quyền truy cập **Requisition**. | Vai trò hiện tại không có quyền truy cập Yêu cầu tuyển dụng. |
| MSG21 | Chỉ Hiring Manager, **Director** và Quản trị viên mới có quyền tạo yêu cầu tuyển dụng. | Chỉ Hiring Manager, Giám đốc và Quản trị viên mới có quyền tạo yêu cầu tuyển dụng. |

Còn lại (không thuộc phạm vi hoangnh): thông báo / email của Offer vẫn có "offer", "(Hired)", "(Rejected)",
"Job Offer Letter", "Đề xuất Offer". Cần báo người phụ trách Offer quyết định có Việt hóa không.
"Hiring Manager" giữ nguyên vì là tên vai trò dùng thống nhất trong toàn hệ thống.

### 6.3 Cùng một ý nhưng câu khác nhau (đã thống nhất ngày 19/10/2026)

| Chỗ lệch | Câu trước | Câu thống nhất |
| :--- | :--- | :--- |
| Lý do khi trả lại: JS và server khác câu | "Vui lòng nêu rõ lý do từ chối để người tạo yêu cầu chỉnh sửa." | MSG66 "Vui lòng nhập lý do để Hiring Manager chỉnh sửa." |
| Không tìm thấy yêu cầu | "Yêu cầu tuyển dụng không tồn tại." / "Không tìm thấy yêu cầu tuyển dụng với mã: {id}" | MSG18 "Không tìm thấy yêu cầu tuyển dụng." |
| Không tìm thấy tin | "Không tìm thấy tin tuyển dụng với mã: {id}" | MSG83 "Không tìm thấy tin tuyển dụng." |
| Tài khoản không hợp lệ | "Tài khoản không hợp lệ hoặc đã bị khóa." | MSG09 "Tài khoản không hoạt động hoặc không tồn tại." |
| Danh sách rỗng | "Không có tin nào khớp bộ lọc. Đặt lại mặc định" | MSG72 cùng mẫu với MSG11 |
| Ô bắt buộc | "Tên vị trí / Địa điểm làm việc không được để trống." | "Vui lòng nhập {tên ô}." (MSG25, MSG27) |
| Chữ số thập phân | "tối đa hai chữ số thập phân" | MSG47 "tối đa 2 chữ số thập phân" |
| Lương ở JS | "Mức lương phải lớn hơn 0." | MSG31 (giống server) |

**Quy ước giữ nguyên (không sửa):**

- **"Trả lại" và "Từ chối":** "Trả lại" là tên **hành động** của Giám đốc (nút, lịch sử phê duyệt, banner).
  "Từ chối" là tên **trạng thái** (`Rejected`) và dùng trong thông báo in-app.
- **"Nội dung quá {max} ký tự"** (MSG01, MSG67) không có dấu chấm cuối câu. Câu này dùng chung ở 6 file, kể cả `text-limit.js`.
- **"Vui lòng nhập trường này."** (MSG02) vẫn dùng cho các ô văn bản dài, vì validator kiểm tra chung nhiều ô.

### 6.4 Câu có trong code nhưng không bao giờ hiện ra

- `RequisitionController` (ô lương): "Mức lương không hợp lệ." Khi ô lương nhập sai, Spring bỏ câu này và hiện
  MSG06 "Vui lòng nhập giá trị số hợp lệ." từ `messages.properties`. Có thể xóa câu này, hoặc thêm
  `typeMismatch.minSalary` / `typeMismatch.maxSalary` để dùng câu riêng cho ô lương.
- **403 dùng một câu chung:** `GlobalExceptionHandler` luôn hiện "Bạn không có quyền truy cập vào chức năng hoặc tài nguyên này."
  cho mọi `AccessDeniedException` (xem mục 7.2). Vì vậy câu riêng của MSG16, 19–22, 64, 69, 79–81, 135 chỉ ghi vào log,
  người dùng không thấy.
- **Sai tên class ở form Yêu cầu (đã sửa 19/10/2026):** khung tổng hợp lỗi của `requisitions/form.html` (dòng 51) dùng
  `alert alert-danger` (một gạch) nên không có màu đỏ; đã đổi thành `alert alert--danger` theo UI_CATALOG.

---

## 7. Đánh giá system message của các phân hệ khác

> Rà soát ngày 19/10/2026, **chỉ đọc code, chưa sửa gì**. Mục này để nhóm và người phụ trách từng phân hệ quyết định sửa.
> Danh mục đầy đủ từng câu nằm ở mục 8.

### 7.1 Tổng quan theo phân hệ

| Phân hệ | Số câu (Java) | Còn tiếng Anh, người dùng **thấy** | Cách hiện flash | Đánh giá |
| :--- | ---: | ---: | :--- | :--- |
| Auth | 18 | 1 | Riêng của trang auth | Java viết tiếng Anh, **template dịch sang tiếng Việt** bằng bảng trong `auth/fragments.html`. Câu không có trong bảng sẽ hiện câu chung "Không thể hoàn tất yêu cầu...". Hiện người dùng thấy tiếng Việt, nhưng thêm câu mới phải nhớ thêm vào bảng dịch. |
| User & Admin (Tài khoản, Phòng ban, Hồ sơ) | 37 | **30** | `alert alert--*` (trang hồ sơ dùng sai `alert-success`) | **Yếu nhất.** Phòng ban đã tiếng Việt. Form tài khoản, đổi mật khẩu và trang hồ sơ in thẳng câu tiếng Anh: "Enter a full name.", "Account created successfully.", "Profile updated successfully."... |
| Admin – API Monitoring | 3 | 3 | `alert alert--*`, flash lỗi tên `failureMessage` | Câu kỹ thuật tiếng Anh, chỉ Admin thấy. |
| Dashboard | 3 | 3 (nếu lỗi) | — | Câu lỗi tiếng Anh. Nếu là 403 thì người dùng chỉ thấy câu chung (7.2). |
| Core | 5 | 0 | — | Tiếng Anh nhưng toàn là lỗi 403 hoặc đăng nhập, người dùng không thấy nguyên văn. |
| Career Portal | 5 | 0 | — | Tốt. 5 template cũ `careers/*.html` còn rất nhiều chữ tiếng Anh nhưng **không controller nào dùng** (xem 7.4). |
| Candidate / Application / Talent Pool | 79 | 0 | `alert alert--*` | Tốt, câu ngắn, rõ, có hướng dẫn ("Vui lòng tải lại trang"). |
| Interview | 61 | 1 | `alert alert--*` | Tiếng Việt nhưng **lộ thuật ngữ kỹ thuật** (7.3). |
| Offer | 65 | 0 | Khung riêng `page-flash-*` + script (nội bộ), `alert alert--*` (ứng viên) | Tiếng Việt, nhưng trộn chữ Anh và mã nội bộ (7.3). Hai kiểu khung flash khác nhau trong cùng phân hệ. |
| *(hoangnh: Requisition, Job Posting, Notification)* | *144* | *0* | `alert alert--*` | *Xem mục 1–6.* |

### 7.2 Vấn đề chung toàn hệ thống

1. **403 luôn hiện một câu chung.** `GlobalExceptionHandler.handleAccessDeniedException` bỏ qua câu trong exception và
   luôn hiện "Bạn không có quyền truy cập vào chức năng hoặc tài nguyên này." Có 14 câu ở mục 8 đánh dấu
   **Error page 403 (\*)**, cộng các câu 403 của hoangnh, đều không đến được người dùng. Hai cách sửa (cần Lead quyết định vì đây là file core):
   - (a) In `ex.getMessage()` nếu câu đó là tiếng Việt, giống cách đang làm với 404.
   - (b) Giữ câu chung và coi câu trong exception chỉ để ghi log. Khi đó nên ghi rõ quy ước này trong ARCHITECTURE_GUIDE.
2. **Có 3 cách làm tiếng Việt.** (1) Viết thẳng tiếng Việt trong Java: Candidate, Interview, Offer, Requisition, Job Posting.
   (2) Viết tiếng Anh rồi dịch ở template: Auth. (3) Viết tiếng Anh và không dịch: User, Admin, Dashboard.
   Nên thống nhất cách (1), vì `messages.properties` đã có sẵn câu mặc định tiếng Việt cho `@NotBlank`, `@Size`, `typeMismatch`.
3. **4 kiểu khung flash:** `alert alert--*` (đa số, đúng UI_CATALOG §8), `page-flash-*` + script tự tắt (Offer nội bộ),
   `alert alert-success` / `alert-danger` sai tên class (trang hồ sơ), và khung riêng của trang auth.
   Tên flash lỗi cũng chưa thống nhất: `errorMessage` (đa số) và `failureMessage` (API Monitoring).
4. **Câu tiếng Anh trong JS** (trang hồ sơ, `profile.js`): "Full name is required.", "Passwords do not match.",
   "Password changed successfully.", "Unable to update password."... Trang hồ sơ cũng có hộp thoại tiếng Anh
   "Confirm Profile Update / Are you sure you want to save the changes made to your profile? / Yes, Save Changes".
5. **Tiêu đề khung lỗi chung.** `fragments/messages.html :: validationSummary` hiện "Check your details" nếu
   controller không đặt `vietnameseUi = true`. Hiện chỉ Account và Department đặt cờ này; trang hồ sơ thì không.

### 7.3 Câu tiếng Việt nhưng chưa thân thiện với người dùng

| Phân hệ | Ví dụ | Vấn đề | Gợi ý |
| :--- | :--- | :--- | :--- |
| Interview | "Business Rule GBR-01: Không thể chuyển trạng thái từ [...] sang [...]" | Lộ mã luật nội bộ (GBR-01, GBR-05) | "Lịch phỏng vấn đã ở trạng thái X nên không thể chuyển về Y." |
| Interview | "Lỗi Rule MSG26: Thời gian kết thúc (...) phải lớn hơn..." | Lộ mã message trong câu | Bỏ "Lỗi Rule MSG26:" |
| Interview | "Application ID không được để trống.", "Interviewer ID...", "Department ID...", "Candidate User ID..." | Tên trường kỹ thuật tiếng Anh | "Vui lòng chọn hồ sơ ứng viên.", "Vui lòng chọn người phỏng vấn." |
| Interview | "Hình thức phỏng vấn không được để trống (Online_GoogleMeet hoặc Offline_Office)." | Lộ giá trị enum | "Vui lòng chọn hình thức phỏng vấn." |
| Interview | "Chỉ có nhân sự (HR) và Giám đốc (Director)...", "Tạo lịch phỏng vấn thành công!" | Song ngữ trong ngoặc; dấu "!" khác các phân hệ khác | "Chỉ HR và Giám đốc...", "Đã tạo lịch phỏng vấn." |
| Offer | "Theo quy tắc GBR-07, chỉ được phép cập nhật Offer khi ở trạng thái thuộc Nhóm A (Draft)." | Mã luật + tên trạng thái tiếng Anh | "Chỉ sửa được Offer ở trạng thái Bản thảo." |
| Offer | "Không tìm thấy OfferProposal với ID: ...", "Không tìm thấy Application với ID: ..." | Tên entity trong câu | "Không tìm thấy đề xuất Offer.", "Không tìm thấy hồ sơ ứng tuyển." |
| Offer | "...ở trạng thái chờ duyệt (Pending_Director)", "Lưu bản thảo Offer (Draft) thành công!" | Mã trạng thái trong ngoặc | Chỉ dùng nhãn tiếng Việt |
| Offer | "Phản hồi của Director không được vượt quá 2000 ký tự." | "Director" | "Giám đốc" |
| Candidate | "Job không thuộc phạm vi phòng ban của bạn." | "Job" | "Tin tuyển dụng" |

**Cách viết chưa thống nhất giữa các phân hệ** (nên chọn một mẫu cho toàn hệ thống):

| Ý | Các cách đang dùng | Đề xuất |
| :--- | :--- | :--- |
| Thành công | "Đã ... ." (Candidate, Requisition, Job Posting) / "... thành công!" (Interview, Offer) | "Đã + động từ + đối tượng." |
| Bắt buộc nhập | "Vui lòng nhập ..." / "... không được để trống." / "Trường này không được để trống." | "Vui lòng nhập {tên ô}." (chọn thì "Vui lòng chọn {tên ô}.") |
| Độ dài | "Nội dung quá N ký tự" / "... tối đa N ký tự." / "... không được vượt quá N ký tự." / "Ghi chú tối đa 1.000 ký tự." (có dấu chấm ngăn cách) | "{Tên ô} tối đa N ký tự." |
| Dữ liệu cũ | "Vui lòng tải lại trang." (Candidate, Offer) | Tốt, nên dùng ở mọi chỗ bắt lỗi đồng thời |
| Không tìm thấy | "Không tìm thấy X." / "Không tìm thấy X với ID: n" / "Không tìm thấy tin tuyển dụng" (thiếu dấu chấm) | "Không tìm thấy X." |

### 7.4 Mã không còn dùng

- `templates/careers/index.html`, `detail.html`, `apply.html`, `unavailable.html`, `fragments.html` và `static/js/careers.js`:
  không controller nào trả về các view này. `CareerPortalController` đã dùng `candidate/landing`, `candidate/job-board`, `candidate/job-detail`.
  Các file này chứa khoảng 45 câu tiếng Anh ("How We Work", "View Open Positions", "No open positions found"...).
  Nên xóa để tránh nhầm khi rà soát.
- `core/.../FormErrorViewHelper`: 5 hàm chỉ ném "Unimplemented method ...".

### 7.5 Đề xuất thứ tự sửa (để nhóm phân công)

| Ưu tiên | Việc | Phân hệ / người phụ trách |
| :---: | :--- | :--- |
| 1 | Việt hóa form tài khoản, đổi mật khẩu, trang hồ sơ (Java + `profile.js` + `user/profile.html`) | User / Admin |
| 2 | Quyết định cách hiện câu 403 (7.2 mục 1) | Lead (`core/exception`) |
| 3 | Bỏ mã GBR / MSG, tên entity, ID kỹ thuật khỏi câu lỗi | Interview, Offer |
| 4 | Thống nhất khung flash `alert alert--*` + tên `errorMessage`; sửa `alert-success` / `alert-danger` | Offer, User, API Monitoring |
| 5 | Xóa template `careers/*` không dùng | Career |
| 6 | Chốt mẫu câu chung (bảng cuối 7.3) và ghi vào UI_RULES | Cả nhóm |

---

## 8. Phụ lục: danh mục message các phân hệ khác (sinh từ code)

> Danh sách trích tự động từ các câu trong `throw new ...Exception(...)`, `addFlashAttribute`, `errors.put` / `rejectValue`
> và `@NotBlank(message = ...)`... ngày 19/10/2026. Cột **Context** để trống: người phụ trách từng phân hệ bổ sung khi rà soát.
> Câu ghép từ nhiều phần (`"..." + biến + "..."`) có thể chỉ hiện một phần.
>
> - ⚠️ EN: người dùng thấy nguyên văn tiếng Anh.
> - 🔁 EN: Java viết tiếng Anh nhưng template đã dịch sang tiếng Việt.
> - **Error page 403 (\*)**: câu này không hiện ra; người dùng chỉ thấy câu 403 chung (7.2).
### Auth (Đăng nhập / Đăng ký / Quên mật khẩu)

| # | Message code | Message Type | Content | Nguồn |
| ---: | :--- | :--- | :--- | :--- |
| 145 | MSG145 | Inline validation | Enter an email address. 🔁 EN (dịch ở `auth/fragments.html`) | `ForgotPasswordRequest:12` |
| 146 | MSG146 | Inline validation | Enter a valid email address. 🔁 EN (dịch ở `auth/fragments.html`) | `ForgotPasswordRequest:13` |
| 147 | MSG147 | Inline validation | Email must be at most 150 characters. 🔁 EN (dịch ở `auth/fragments.html`) | `ForgotPasswordRequest:14` |
| 148 | MSG148 | Inline validation | Passwords do not match. 🔁 EN (dịch ở `auth/fragments.html`) | `RegisterAccountRequest:12` |
| 149 | MSG149 | Inline validation | Enter a full name. 🔁 EN (dịch ở `auth/fragments.html`) | `RegisterAccountRequest:14` |
| 150 | MSG150 | Inline validation | Full name must be at most 100 characters. 🔁 EN (dịch ở `auth/fragments.html`) | `RegisterAccountRequest:15` |
| 151 | MSG151 | Inline validation | Enter a username. 🔁 EN (dịch ở `auth/fragments.html`) | `RegisterAccountRequest:18` |
| 152 | MSG152 | Inline validation | Username must be at most 50 characters. 🔁 EN (dịch ở `auth/fragments.html`) | `RegisterAccountRequest:19` |
| 153 | MSG153 | Inline validation | Enter a password. 🔁 EN (dịch ở `auth/fragments.html`) | `RegisterAccountRequest:27` |
| 154 | MSG154 | Inline validation | Password must contain 8–32 characters. 🔁 EN (dịch ở `auth/fragments.html`) | `RegisterAccountRequest:28` |
| 155 | MSG155 | Inline validation | Confirm your password. 🔁 EN (dịch ở `auth/fragments.html`) | `RegisterAccountRequest:31` |
| 156 | MSG156 | Inline validation | Enter a new password. 🔁 EN (dịch ở `auth/fragments.html`) | `ResetPasswordRequest:14` |
| 157 | MSG157 | Inline validation | Enter the verification code. 🔁 EN (dịch ở `auth/fragments.html`) | `VerifyPasswordResetOtpRequest:13` |
| 158 | MSG158 | Lỗi nghiệp vụ | Password recovery email delivery failed. ⚠️ EN | `PasswordResetDeliveryException:8` |
| 159 | MSG159 | Lỗi nghiệp vụ | Please wait before requesting another code. 🔁 EN (dịch ở `auth/fragments.html`) | `PasswordRecoveryService:68` |
| 160 | MSG160 | Lỗi nghiệp vụ | Your reset request is invalid or expired. Request a new code. 🔁 EN (dịch ở `auth/fragments.html`) | `PasswordRecoveryService:207` |
| 161 | MSG161 | Lỗi nghiệp vụ | Unable to send the verification email. Please try again later. 🔁 EN (dịch ở `auth/fragments.html`) | `RegistrationEmailSender:60` |
| 162 | MSG162 | Lỗi nghiệp vụ | Vui lòng đăng ký trước khi xác minh email. | `RegistrationVerificationService:164` |

### User & Admin (Tài khoản, Phòng ban, Hồ sơ cá nhân)

| # | Message code | Message Type | Content | Nguồn |
| ---: | :--- | :--- | :--- | :--- |
| 163 | MSG163 | Inline validation | Current password is required. ⚠️ EN | `ChangePasswordRequest:12` |
| 164 | MSG164 | Inline validation | New password is required. ⚠️ EN | `ChangePasswordRequest:15` |
| 165 | MSG165 | Inline validation | New password must be between 8 and 32 characters. ⚠️ EN | `ChangePasswordRequest:16` |
| 166 | MSG166 | Inline validation | Confirm password is required. ⚠️ EN | `ChangePasswordRequest:19` |
| 167 | MSG167 | Inline validation | Passwords do not match. ⚠️ EN | `CreateAccountRequest:14` |
| 168 | MSG168 | Inline validation | Enter a full name. ⚠️ EN | `CreateAccountRequest:17` |
| 169 | MSG169 | Inline validation | Full name must be at most 100 characters. ⚠️ EN | `CreateAccountRequest:18` |
| 170 | MSG170 | Inline validation | Enter a username. ⚠️ EN | `CreateAccountRequest:21` |
| 171 | MSG171 | Inline validation | Username must be at most 50 characters. ⚠️ EN | `CreateAccountRequest:22` |
| 172 | MSG172 | Inline validation | Enter an email address. ⚠️ EN | `CreateAccountRequest:25` |
| 173 | MSG173 | Inline validation | Enter a valid email address. ⚠️ EN | `CreateAccountRequest:26` |
| 174 | MSG174 | Inline validation | Email must be at most 150 characters. ⚠️ EN | `CreateAccountRequest:27` |
| 175 | MSG175 | Inline validation | Phone number must be at most 20 characters. ⚠️ EN | `CreateAccountRequest:30` |
| 176 | MSG176 | Inline validation | Select a role. ⚠️ EN | `CreateAccountRequest:33` |
| 177 | MSG177 | Inline validation | Enter a password. ⚠️ EN | `CreateAccountRequest:38` |
| 178 | MSG178 | Inline validation | Password must contain 8–32 characters. ⚠️ EN | `CreateAccountRequest:39` |
| 179 | MSG179 | Inline validation | Confirm your password. ⚠️ EN | `CreateAccountRequest:42` |
| 180 | MSG180 | Inline validation | Vui lòng nhập tên phòng ban. | `DepartmentRequest:8` |
| 181 | MSG181 | Inline validation | Tên phòng ban không được quá 100 ký tự. | `DepartmentRequest:9` |
| 182 | MSG182 | Inline validation | Trưởng phòng không hợp lệ. | `DepartmentRequest:10` |
| 183 | MSG183 | Inline validation | Select a status. ⚠️ EN | `UpdateAccountRequest:32` |
| 184 | MSG184 | Inline validation | Full name is required. ⚠️ EN | `UpdateProfileRequest:12` |
| 185 | MSG185 | Inline validation | Full name must be between 2 and 100 characters. ⚠️ EN | `UpdateProfileRequest:13` |
| 186 | MSG186 | Inline validation | Full name can only contain letters, spaces, hyphens, and apostrophes, and must start with a letter. ⚠️ EN | `UpdateProfileRequest:16` |
| 187 | MSG187 | Inline validation | Phone number cannot exceed 15 characters. ⚠️ EN | `UpdateProfileRequest:20` |
| 188 | MSG188 | Inline validation | Avatar URL cannot exceed 500 characters. ⚠️ EN | `UpdateProfileRequest:24` |
| 189 | MSG189 | Error page | Không tìm thấy tài khoản. | `AccountListService:153` |
| 190 | MSG190 | Error page | Tài khoản không thuộc nhóm này. | `AccountListService:156` |
| 191 | MSG191 | Error page | Không thể kích hoạt tài khoản đang bị khóa bằng thao tác này. | `AccountListService:160` |
| 192 | MSG192 | Error page | Account not found ⚠️ EN | `AccountListService:170` |
| 193 | MSG193 | Error page | Account not found in this account group ⚠️ EN | `AccountListService:173` |
| 194 | MSG194 | Error page | Internal account not found ⚠️ EN | `AccountManagementService:68` |
| 195 | MSG195 | Lỗi nghiệp vụ | Select an internal role. Candidate accounts have a separate lifecycle. ⚠️ EN | `AccountManagementService:75` |
| 196 | MSG196 | Lỗi nghiệp vụ | Candidate role is not configured ⚠️ EN | `AccountManagementService:108` |
| 197 | MSG197 | Lỗi nghiệp vụ | Account types cannot be changed. Create a new account for the other lifecycle. ⚠️ EN | `AccountManagementService:140` |
| 198 | MSG198 | Lỗi nghiệp vụ | Candidate accounts do not belong to internal departments. ⚠️ EN | `AccountManagementService:184` |
| 199 | MSG199 | Error page 404 | Không tìm thấy phòng ban. | `DepartmentService:126` |

### Admin (API Monitoring)

| # | Message code | Message Type | Content | Nguồn |
| ---: | :--- | :--- | :--- | :--- |
| 200 | MSG200 | Lỗi nghiệp vụ | Database health check failed. ⚠️ EN | `DatabaseHealthException:6` |
| 201 | MSG201 | Lỗi nghiệp vụ | Authenticated session is required. ⚠️ EN | `MonitoringSessionRequiredException:7` |
| 202 | MSG202 | Lỗi nghiệp vụ | Monitoring target is not allowlisted ⚠️ EN | `LoopbackHttpProbeTransport:19` |

### Dashboard

| # | Message code | Message Type | Content | Nguồn |
| ---: | :--- | :--- | :--- | :--- |
| 203 | MSG203 | Lỗi nghiệp vụ | Unsupported dashboard role ⚠️ EN | `DashboardController:36` |
| 204 | MSG204 | Error page 404 | Account not found ⚠️ EN | `DashboardService:54` |
| 205 | MSG205 | Error page 403 (*) | Inactive account cannot access dashboard ⚠️ EN | `DashboardService:57` |

### Core (dùng chung)

| # | Message code | Message Type | Content | Nguồn |
| ---: | :--- | :--- | :--- | :--- |
| 206 | MSG206 | Error page 403 (*) | Please log in. ⚠️ EN | `CurrentUserService:20` |
| 207 | MSG207 | Error page 403 (*) | An active account is required. ⚠️ EN | `CurrentUserService:24` |
| 208 | MSG208 | Đăng nhập thất bại (không hiện câu này) | Invalid credentials ⚠️ EN | `DatabaseUserDetailsService:29` |
| 209 | MSG209 | Lỗi nghiệp vụ | Role is missing ⚠️ EN | `RoleAuthorities:17` |
| 210 | MSG210 | Lỗi nghiệp vụ | Unsupported role ⚠️ EN | `RoleAuthorities:27` |

### Career Portal (trang việc làm công khai)

| # | Message code | Message Type | Content | Nguồn |
| ---: | :--- | :--- | :--- | :--- |
| 211 | MSG211 | Error page 404 | Không tìm thấy tin tuyển dụng | `CareerPortalService:78` |
| 212 | MSG212 | Error page 404 | Tin tuyển dụng này không còn khả dụng. | `CareerPortalService:82` |
| 213 | MSG213 | Error page 404 | Tin tuyển dụng này không còn khả dụng hoặc đã ngừng nhận hồ sơ. | `CareerPortalService:117` |
| 214 | MSG214 | Lỗi nghiệp vụ | Vui lòng đăng nhập để tiếp tục. | `CareerPortalService:137` |
| 215 | MSG215 | Lỗi nghiệp vụ | Bạn không có quyền truy cập trang này. Vui lòng đăng nhập với tài khoản Ứng viên. | `CareerPortalService:143` |

### Candidate / Application / Talent Pool

| # | Message code | Message Type | Content | Nguồn |
| ---: | :--- | :--- | :--- | :--- |
| 216 | MSG216 | Flash | Đã chấm lại AI: %.2f điểm. | `ApplicationPipelineController:135` |
| 217 | MSG217 | Flash | successMessageĐã rút hồ sơ ứng tuyển vị trí . | `MyApplicationController:37` |
| 218 | MSG218 | Inline / JSON | Đã kiểm tra danh sách. | `TalentPoolController:101` |
| 219 | MSG219 | Flash | Đã chấp nhận gợi ý. Chờ HR liên hệ và xác nhận ứng viên đồng ý. | `TalentPoolController:181` |
| 220 | MSG220 | Inline / JSON | Đã gửi gợi ý cho HM. | `TalentPoolController:196` |
| 221 | MSG221 | Inline / JSON | ứng viên vào danh sách HM đề xuất. HR sẽ liên hệ xác nhận. | `TalentPoolController:249` |
| 222 | MSG222 | Flash | Đã xác nhận ứng tuyển bằng CV cũ. Có thể lên lịch phỏng vấn. | `TalentPoolController:266` |
| 223 | MSG223 | Inline validation | Vui lòng chọn quyết định. | `ApplicationReviewRequest:13` |
| 224 | MSG224 | Inline validation | Nhận xét tối đa 1000 ký tự. | `ApplicationReviewRequest:17` |
| 225 | MSG225 | Inline validation | Ghi chú tối đa 1000 ký tự. | `TalentPoolBookmarkRequest:7` |
| 226 | MSG226 | Inline validation | Cần xác nhận ứng viên đã đồng ý ứng tuyển bằng CV cũ. | `TalentPoolConfirmRequest:8` |
| 227 | MSG227 | Inline validation | Vui lòng nhập ghi chú. | `TalentPoolNoteRequest:6` |
| 228 | MSG228 | Inline validation | Ghi chú tối đa 1.000 ký tự. | `TalentPoolNoteRequest:7` |
| 229 | MSG229 | Inline validation | Chọn ít nhất một CV của ứng viên. | `TalentPoolSuggestRequest:8` |
| 230 | MSG230 | Lỗi nghiệp vụ | Đã lưu dữ liệu nhưng hệ thống gửi mail đang gián đoạn. HR có thể bấm Gửi lại Email. | `RecruitmentEmailException:5` |
| 231 | MSG231 | Error page 404 | Không tìm thấy đơn ứng tuyển. | `AiScreeningService:35` |
| 232 | MSG232 | Error page 403 (*) | Bạn không có quyền xem hồ sơ này. | `ApplicationAccess:64` |
| 233 | MSG233 | Error page 404 | Không tìm thấy hồ sơ ứng tuyển. | `ApplicationPipelineService:115` |
| 234 | MSG234 | Error page 404 | Không tìm thấy hồ sơ. | `ApplicationPipelineService:158` |
| 235 | MSG235 | Error page 404 | Không tìm thấy CV của hồ sơ này. | `ApplicationPipelineService:191` |
| 236 | MSG236 | Error page 403 (*) | Chỉ HR và trưởng bộ phận được duyệt hồ sơ. | `ApplicationReviewService:45` |
| 237 | MSG237 | Lỗi nghiệp vụ | Hồ sơ đang ở bước " | `ApplicationReviewService:48` |
| 238 | MSG238 | Lỗi nghiệp vụ | Hồ sơ vừa được người khác xử lý. Vui lòng tải lại trang. | `ApplicationReviewService:72` |
| 239 | MSG239 | Error page 403 (*) | Chỉ HR được chấm lại AI. | `ApplicationReviewService:89` |
| 240 | MSG240 | Lỗi nghiệp vụ | Chỉ chấm lại AI khi hồ sơ chưa qua vòng nhân sự. | `ApplicationReviewService:92` |
| 241 | MSG241 | Lỗi nghiệp vụ | Hồ sơ chưa đủ điều kiện hoặc đợt tuyển đã đóng. | `ApplicationStatusService:53` |
| 242 | MSG242 | Lỗi nghiệp vụ | Hồ sơ đã chuyển sang vòng Offer hoặc đã kết thúc. | `ApplicationStatusService:57` |
| 243 | MSG243 | Lỗi nghiệp vụ | Không phải bước dừng: | `ApplicationStepper:56` |
| 244 | MSG244 | Lỗi nghiệp vụ | Tài khoản chưa có hồ sơ ứng viên. Vui lòng liên hệ quản trị viên. | `ApplicationSubmissionService:43` |
| 245 | MSG245 | Error page 404 | Tin tuyển dụng này không còn khả dụng. | `ApplicationSubmissionService:47` |
| 246 | MSG246 | Lỗi nghiệp vụ | Tin tuyển dụng đã hết hạn nhận hồ sơ. | `ApplicationSubmissionService:50` |
| 247 | MSG247 | Lỗi nghiệp vụ | Bạn đã ứng tuyển vị trí này. | `ApplicationSubmissionService:54` |
| 248 | MSG248 | Lỗi nghiệp vụ | Đề xuất vừa được xử lý. Vui lòng tải lại trang. | `ApplicationSubmissionService:70` |
| 249 | MSG249 | Error page 404 | Không tìm thấy file CV. | `LocalCvStorage:63` |
| 250 | MSG250 | Lỗi nghiệp vụ | Hồ sơ đã qua vòng sơ tuyển nên không thể rút. Vui lòng liên hệ bộ phận tuyển dụng. | `MyApplicationService:55` |
| 251 | MSG251 | Lỗi nghiệp vụ | Hồ sơ vừa được cập nhật. Vui lòng tải lại trang. | `MyApplicationService:58` |
| 252 | MSG252 | Lỗi nghiệp vụ | Đơn này không bị dừng do đóng đợt tuyển. | `RecruitmentEmailService:51` |
| 253 | MSG253 | Lỗi nghiệp vụ | Không tìm thấy đơn để gửi email. | `RecruitmentEmailService:62` |
| 254 | MSG254 | Lỗi nghiệp vụ | Lịch này không còn đủ điều kiện gửi thư mời. | `RecruitmentEmailService:79` |
| 255 | MSG255 | Lỗi nghiệp vụ | SMTP chưa cấu hình. | `RecruitmentEmailService:86` |
| 256 | MSG256 | Error page 403 (*) | Chỉ HR được gửi email tuyển dụng. | `RecruitmentEmailService:101` |
| 257 | MSG257 | Error page 403 (*) | Chỉ HR và Hiring Manager được truy cập kho tiềm năng. | `TalentPoolService:45` |
| 258 | MSG258 | Error page 404 | Ứng viên chưa nằm trong kho tiềm năng. | `TalentPoolService:78` |
| 259 | MSG259 | Lỗi nghiệp vụ | Hãy đánh dấu ứng viên tiềm năng trước khi thêm ghi chú. | `TalentPoolService:151` |
| 260 | MSG260 | Lỗi nghiệp vụ | Ứng viên không còn trong kho tiềm năng. | `TalentPoolService:191` |
| 261 | MSG261 | Lỗi nghiệp vụ | Ứng viên được chọn nhiều lần. | `TalentPoolService:192` |
| 262 | MSG262 | Lỗi nghiệp vụ | Chọn CV từ một tin tuyển dụng khác. | `TalentPoolService:194` |
| 263 | MSG263 | Lỗi nghiệp vụ | CV này không còn đủ điều kiện làm hồ sơ nguồn. | `TalentPoolService:197` |
| 264 | MSG264 | Lỗi nghiệp vụ | Ứng viên đã có hồ sơ hoặc đề xuất cho tin này. | `TalentPoolService:201` |
| 265 | MSG265 | Lỗi nghiệp vụ | Tin chưa có Hiring Manager phụ trách hợp lệ để nhận gợi ý. | `TalentPoolService:229` |
| 266 | MSG266 | Lỗi nghiệp vụ | Ứng viên phải được đánh dấu tiềm năng. | `TalentPoolService:235` |
| 267 | MSG267 | Lỗi nghiệp vụ | Chỉ chọn một CV cho mỗi ứng viên. | `TalentPoolService:236` |
| 268 | MSG268 | Lỗi nghiệp vụ | CV nguồn phải thuộc một đợt tuyển khác. | `TalentPoolService:238` |
| 269 | MSG269 | Lỗi nghiệp vụ | CV nguồn phải thuộc đơn ứng tuyển chính thức. | `TalentPoolService:241` |
| 270 | MSG270 | Lỗi nghiệp vụ | Ứng viên đã có đơn hoặc đề xuất cho vị trí này. | `TalentPoolService:246` |
| 271 | MSG271 | Error page 404 | Không tìm thấy gợi ý cho ứng viên này. | `TalentPoolService:304` |
| 272 | MSG272 | Lỗi nghiệp vụ | Gợi ý thiếu thông tin người gửi. Cần kiểm tra dữ liệu. | `TalentPoolService:312` |
| 273 | MSG273 | Lỗi nghiệp vụ | Gợi ý đã được xử lý. Vui lòng tải lại trang. | `TalentPoolService:342` |
| 274 | MSG274 | Lỗi nghiệp vụ | Ứng viên đã có hồ sơ chính thức cho tin này. | `TalentPoolService:345` |
| 275 | MSG275 | Lỗi nghiệp vụ | Gợi ý đã được xử lý. | `TalentPoolService:348` |
| 276 | MSG276 | Error page 404 | Không tìm thấy gợi ý gửi cho HM trong tin này. | `TalentPoolService:364` |
| 277 | MSG277 | Lỗi nghiệp vụ | Gợi ý đã được xử lý, không thể xóa. Vui lòng tải lại trang. | `TalentPoolService:367` |
| 278 | MSG278 | Lỗi nghiệp vụ | Cần xác nhận ứng viên đồng ý dùng CV cũ. | `TalentPoolService:375` |
| 279 | MSG279 | Lỗi nghiệp vụ | Đề xuất đã được xử lý. Vui lòng tải lại trang. | `TalentPoolService:380` |
| 280 | MSG280 | Lỗi nghiệp vụ | Đề xuất đã được xử lý. | `TalentPoolService:397` |
| 281 | MSG281 | Lỗi nghiệp vụ | Chỉ tin đã đăng hoặc đang tạm dừng mới được đóng đợt tuyển. | `TalentPoolService:407` |
| 282 | MSG282 | Lỗi nghiệp vụ | Còn Offer chưa kết thúc. Hãy xử lý Offer trước khi đóng đợt tuyển. | `TalentPoolService:408` |
| 283 | MSG283 | Error page 403 (*) | CV không thuộc ứng viên tiềm năng này. | `TalentPoolService:435` |
| 284 | MSG284 | Lỗi nghiệp vụ | Hồ sơ nguồn không có CV. | `TalentPoolService:442` |
| 285 | MSG285 | Lỗi nghiệp vụ | File CV cũ không còn tồn tại. Ứng viên cần nộp CV mới. | `TalentPoolService:447` |
| 286 | MSG286 | Lỗi nghiệp vụ | Đường dẫn CV không hợp lệ. | `TalentPoolService:449` |
| 287 | MSG287 | Error page 403 (*) | Job không thuộc phạm vi phòng ban của bạn. | `TalentPoolService:454` |
| 288 | MSG288 | Lỗi nghiệp vụ | Gợi ý đang chờ HM phản hồi, chưa thể xác nhận hoặc kết thúc liên hệ. | `TalentPoolService:458` |
| 289 | MSG289 | Lỗi nghiệp vụ | Tin tuyển dụng không còn nhận hồ sơ. | `TalentPoolService:463` |
| 290 | MSG290 | Error page 404 | Không tìm thấy ứng viên. | `TalentPoolService:466` |
| 291 | MSG291 | Error page 404 | Không tìm thấy tin tuyển dụng. | `TalentPoolService:473` |
| 292 | MSG292 | Lỗi nghiệp vụ | Vui lòng chọn file CV. | `CvFileValidator:28` |
| 293 | MSG293 | Lỗi nghiệp vụ | File CV tối đa 5 MB. | `CvFileValidator:31` |
| 294 | MSG294 | Lỗi nghiệp vụ | Chỉ nhận file PDF. | `CvFileValidator:34` |

### Interview

| # | Message code | Message Type | Content | Nguồn |
| ---: | :--- | :--- | :--- | :--- |
| 295 | MSG295 | Inline / JSON | Vui lòng kiểm tra lại các ô được đánh dấu. | `InterviewEvaluationController:50` |
| 296 | MSG296 | Inline / JSON | Đã gửi phiếu đánh giá. | `InterviewEvaluationController:60` |
| 297 | MSG297 | Inline / JSON | Đã chốt kết quả phỏng vấn. | `InterviewFinalResultController:58` |
| 298 | MSG298 | Flash | errorMessageKhông tìm thấy lịch phỏng vấn với mã: | `InterviewSchedulingController:203` |
| 299 | MSG299 | Inline validation | Application ID không được để trống. | `InterviewScheduleRequest:28` |
| 300 | MSG300 | Inline validation | Hình thức phỏng vấn không được để trống (Online_GoogleMeet hoặc Offline_Office). | `InterviewScheduleRequest:31` |
| 301 | MSG301 | Inline validation | Thời gian bắt đầu phỏng vấn không được để trống. | `InterviewScheduleRequest:34` |
| 302 | MSG302 | Inline validation | Thời gian kết thúc phỏng vấn không được để trống. | `InterviewScheduleRequest:38` |
| 303 | MSG303 | Inline validation | Địa điểm hoặc liên kết phòng họp tối đa 500 ký tự. | `InterviewScheduleRequest:42` |
| 304 | MSG304 | Inline validation | Hội đồng phỏng vấn phải có ít nhất 1 người phỏng vấn. | `InterviewScheduleRequest:49` |
| 305 | MSG305 | Inline validation | Thời gian kết thúc (endTime) phải lớn hơn thời gian bắt đầu (startTime). | `InterviewScheduleRequest:96` |
| 306 | MSG306 | Inline validation | Interviewer ID không được để trống. | `PanelMemberRequest:11` |
| 307 | MSG307 | Inline validation | Điểm kỹ thuật phải từ 1 đến 5. | `SubmitEvaluationRequest:18` |
| 308 | MSG308 | Inline validation | Điểm kỹ năng mềm phải từ 1 đến 5. | `SubmitEvaluationRequest:22` |
| 309 | MSG309 | Inline validation | Điểm phù hợp văn hóa phải từ 1 đến 5. | `SubmitEvaluationRequest:26` |
| 310 | MSG310 | Inline validation | Vui lòng chọn đề xuất. | `SubmitEvaluationRequest:30` |
| 311 | MSG311 | Inline validation | Điểm mạnh tối đa 2000 ký tự. | `SubmitEvaluationRequest:34` |
| 312 | MSG312 | Inline validation | Điểm yếu tối đa 2000 ký tự. | `SubmitEvaluationRequest:37` |
| 313 | MSG313 | Inline validation | Nhận xét tối đa 2000 ký tự. | `SubmitEvaluationRequest:40` |
| 314 | MSG314 | Inline validation | Vui lòng chấm ít nhất một mục điểm. | `SubmitEvaluationRequest:45` |
| 315 | MSG315 | Inline validation | Vui lòng chọn kết quả. | `SubmitFinalResultRequest:16` |
| 316 | MSG316 | Inline validation | Mức lương đề xuất không hợp lệ. | `SubmitFinalResultRequest:21` |
| 317 | MSG317 | Inline validation | Nhận xét tổng hợp tối đa 4000 ký tự. | `SubmitFinalResultRequest:24` |
| 318 | MSG318 | Lỗi nghiệp vụ | Business Rule Violation: EndTime ( ⚠️ EN | `InterviewSchedule:133` |
| 319 | MSG319 | Lỗi nghiệp vụ | Business Rule GBR-01 Violation: Không thể chuyển trạng thái từ [ | `InterviewSchedule:147` |
| 320 | MSG320 | Lỗi nghiệp vụ | Interviewer không được null và phải có UserId hợp lệ. | `InterviewSchedule:158` |
| 321 | MSG321 | Error page 404 | Không tìm thấy lịch phỏng vấn với mã: | `InterviewEvaluationServiceImpl:39` |
| 322 | MSG322 | Error page 403 (*) | Bạn không thuộc hội đồng của buổi phỏng vấn này nên không thể đánh giá. | `InterviewEvaluationServiceImpl:48` |
| 323 | MSG323 | Lỗi nghiệp vụ | Bạn đã nộp phiếu đánh giá cho buổi phỏng vấn này. | `InterviewEvaluationServiceImpl:56` |
| 324 | MSG324 | Lỗi nghiệp vụ | Business Rule GBR-01: Không thể chuyển lịch phỏng vấn sang trạng thái Đã hoàn thành. | `InterviewEvaluationServiceImpl:77` |
| 325 | MSG325 | Error page 403 (*) | Chỉ Hiring Manager phụ trách phòng ban của vị trí này mới được chốt kết quả. | `InterviewFinalResultServiceImpl:51` |
| 326 | MSG326 | Lỗi nghiệp vụ | Buổi phỏng vấn này đã được chốt kết quả, không thể chốt lại. | `InterviewFinalResultServiceImpl:60` |
| 327 | MSG327 | Error page 403 (*) | Bạn không được phân công vào buổi phỏng vấn này. | `InterviewResultServiceImpl:63` |
| 328 | MSG328 | Error page 404 | Không tìm thấy tài khoản người dùng với ID: | `InterviewSchedulingServiceImpl:59` |
| 329 | MSG329 | Error page 404 | Không tìm thấy hồ sơ ứng tuyển với ID: | `InterviewSchedulingServiceImpl:66` |
| 330 | MSG330 | Lỗi nghiệp vụ | Hội đồng phỏng vấn phải có ít nhất một người phỏng vấn. | `InterviewSchedulingServiceImpl:75` |
| 331 | MSG331 | Lỗi nghiệp vụ | Interviewer ID trong hội đồng không được để trống. | `InterviewSchedulingServiceImpl:85` |
| 332 | MSG332 | Lỗi nghiệp vụ | Trùng lặp người phỏng vấn trong hội đồng: ID | `InterviewSchedulingServiceImpl:89` |
| 333 | MSG333 | Lỗi nghiệp vụ | Quy tắc nghiệp vụ: HR tạo lịch không được phép tự gán mình vào Hội đồng phỏng vấn. | `InterviewSchedulingServiceImpl:95` |
| 334 | MSG334 | Error page 404 | Không tìm thấy người phỏng vấn với ID: | `InterviewSchedulingServiceImpl:99` |
| 335 | MSG335 | Lỗi nghiệp vụ | Người dùng | `InterviewSchedulingServiceImpl:104` |
| 336 | MSG336 | Lỗi nghiệp vụ | không được phép tham gia Hội đồng phỏng vấn. | `InterviewSchedulingServiceImpl:105` |
| 337 | MSG337 | Lỗi nghiệp vụ | Tài khoản người phỏng vấn  không ở trạng thái Active. | `InterviewSchedulingServiceImpl:110` |
| 338 | MSG338 | Error page 404 | Không tìm thấy lịch phỏng vấn với ID: | `InterviewSchedulingServiceImpl:154` |
| 339 | MSG339 | Lỗi nghiệp vụ | Quy tắc nghiệp vụ: HR không được phép tự gán mình vào Hội đồng phỏng vấn. | `InterviewSchedulingServiceImpl:206` |
| 340 | MSG340 | Lỗi nghiệp vụ | Business Rule GBR-01: Lịch phỏng vấn đang ở trạng thái [ | `InterviewSchedulingServiceImpl:274` |
| 341 | MSG341 | Lỗi nghiệp vụ | Interview ID và Interviewer ID không được để trống. | `InterviewSchedulingServiceImpl:336` |
| 342 | MSG342 | Lỗi nghiệp vụ | Business Rule GBR-05: Bạn không có quyền truy cập hoặc không được phân công vào lịch phỏng vấn này. | `InterviewSchedulingServiceImpl:342` |
| 343 | MSG343 | Lỗi nghiệp vụ | Candidate User ID không được để trống. | `InterviewSchedulingServiceImpl:350` |
| 344 | MSG344 | Lỗi nghiệp vụ | Department ID không được để trống. | `InterviewSchedulingServiceImpl:360` |
| 345 | MSG345 | Lỗi nghiệp vụ | Dữ liệu yêu cầu lịch phỏng vấn không được để trống. | `InterviewTimeValidationService:37` |
| 346 | MSG346 | Lỗi nghiệp vụ | Thời gian bắt đầu và kết thúc phỏng vấn không được để trống. | `InterviewTimeValidationService:44` |
| 347 | MSG347 | Lỗi nghiệp vụ | Lỗi Rule MSG26: Thời gian kết thúc ( | `InterviewTimeValidationService:49` |
| 348 | MSG348 | Lỗi nghiệp vụ | Quy tắc nghiệp vụ: Không thể tạo mới một lịch phỏng vấn với trạng thái ' | `InterviewTimeValidationService:61` |
| 349 | MSG349 | Lỗi nghiệp vụ | Lịch phỏng vấn mới ở trạng thái 'Đã lên lịch' phải có thời gian bắt đầu ở trong tương lai. | `InterviewTimeValidationService:68` |
| 350 | MSG350 | Lỗi nghiệp vụ | Hồ sơ ứng viên này đã có một lịch phỏng vấn khác đang hoạt động trùng vào khoảng thời gian đã chọn. | `InterviewTimeValidationService:77` |
| 351 | MSG351 | Lỗi nghiệp vụ | Thành viên hội đồng (ID: | `InterviewTimeValidationService:90` |
| 352 | MSG352 | Lỗi nghiệp vụ | Dữ liệu cập nhật hoặc lịch phỏng vấn hiện tại không hợp lệ. | `InterviewTimeValidationService:103` |
| 353 | MSG353 | Lỗi nghiệp vụ | Business Rule GBR-01: Không thể chuyển trạng thái từ [ | `InterviewTimeValidationService:119` |
| 354 | MSG354 | Lỗi nghiệp vụ | Khi đổi lịch phỏng vấn (Đã đổi lịch), thời gian bắt đầu mới phải ở trong tương lai. | `InterviewTimeValidationService:126` |
| 355 | MSG355 | Lỗi nghiệp vụ | Hồ sơ ứng viên này đã có một lịch phỏng vấn khác đang hoạt động trùng vào khoảng thời gian cập nhật. | `InterviewTimeValidationService:135` |

### Offer

| # | Message code | Message Type | Content | Nguồn |
| ---: | :--- | :--- | :--- | :--- |
| 356 | MSG356 | Flash | successMessageBạn đã nhận offer vị trí . | `CandidateOfferController:66` |
| 357 | MSG357 | Flash | successMessageBạn đã từ chối offer vị trí . | `CandidateOfferController:85` |
| 358 | MSG358 | Error page 403 (*) | Bạn không có quyền truy cập vào phân hệ Quản lý Đề xuất Offer. | `OfferController:58` |
| 359 | MSG359 | Flash | Lưu bản thảo Offer (Draft) thành công! | `OfferController:207` |
| 360 | MSG360 | Inline / JSON | Lấy chi tiết Offer thành công. | `OfferController:226` |
| 361 | MSG361 | Flash | Cập nhật bản thảo Offer thành công! | `OfferController:309` |
| 362 | MSG362 | Inline / JSON | Đã xóa bản thảo Offer thành công. | `OfferController:330` |
| 363 | MSG363 | Inline / JSON | Đã phát hành và gửi thư mời nhận việc (Offer Letter) tới ứng viên thành công! | `OfferController:360` |
| 364 | MSG364 | Inline / JSON | Đã phê duyệt đề xuất Offer thành công! | `OfferController:392` |
| 365 | MSG365 | Inline / JSON | Đã từ chối đề xuất Offer thành công! | `OfferController:408` |
| 366 | MSG366 | Inline validation | Vui lòng chọn ứng viên đỗ phỏng vấn. | `CreateOfferRequest:30` |
| 367 | MSG367 | Inline validation | Vị trí chức danh đề xuất không được để trống. | `CreateOfferRequest:33` |
| 368 | MSG368 | Inline validation | Vị trí chức danh đề xuất không được vượt quá 150 ký tự. | `CreateOfferRequest:34` |
| 369 | MSG369 | Inline validation | Mức lương chính thức không được để trống. | `CreateOfferRequest:37` |
| 370 | MSG370 | Inline validation | Mức lương chính thức phải lớn hơn 0. | `CreateOfferRequest:38` |
| 371 | MSG371 | Inline validation | Mức lương thử việc không được để trống. | `CreateOfferRequest:41` |
| 372 | MSG372 | Inline validation | Mức lương thử việc phải lớn hơn 0. | `CreateOfferRequest:42` |
| 373 | MSG373 | Inline validation | Ngày bắt đầu dự kiến không được để trống. | `CreateOfferRequest:45` |
| 374 | MSG374 | Inline validation | Ngày bắt đầu dự kiến phải lớn hơn ngày hiện tại. | `CreateOfferRequest:46` |
| 375 | MSG375 | Inline validation | Địa điểm làm việc không được để trống. | `CreateOfferRequest:50` |
| 376 | MSG376 | Inline validation | Địa điểm làm việc không được vượt quá 255 ký tự. | `CreateOfferRequest:51` |
| 377 | MSG377 | Inline validation | Gói phúc lợi không được vượt quá 2000 ký tự. | `CreateOfferRequest:54` |
| 378 | MSG378 | Inline validation | Thời gian thử việc không được để trống. | `CreateOfferRequest:59` |
| 379 | MSG379 | Inline validation | Thời gian thử việc phải lớn hơn 0. | `CreateOfferRequest:60` |
| 380 | MSG380 | Inline validation | Thời gian thử việc không được vượt quá 365 ngày. | `CreateOfferRequest:61` |
| 381 | MSG381 | Inline validation | Thời gian phản hồi không được để trống. | `CreateOfferRequest:64` |
| 382 | MSG382 | Inline validation | Thời gian phản hồi phải lớn hơn 0. | `CreateOfferRequest:65` |
| 383 | MSG383 | Inline validation | Thời gian phản hồi phải tối thiểu 1 ngày. | `CreateOfferRequest:66` |
| 384 | MSG384 | Inline validation | Thời gian phản hồi không được vượt quá 60 ngày. | `CreateOfferRequest:67` |
| 385 | MSG385 | Inline validation | Lương thử việc phải đạt tối thiểu 85% lương chính thức. | `CreateOfferRequest:98` |
| 386 | MSG386 | Inline validation | Lương thử việc không được vượt quá lương chính thức. | `CreateOfferRequest:113` |
| 387 | MSG387 | Inline validation | Năm bắt đầu dự kiến không hợp lệ (không quá 2 năm tới). | `CreateOfferRequest:124` |
| 388 | MSG388 | Inline validation | Phản hồi của Director không được vượt quá 2000 ký tự. | `DirectorDecisionRequest:14` |
| 389 | MSG389 | Inline validation | Phạm vi xuất dữ liệu không được để trống | `OfferExportRequest:12` |
| 390 | MSG390 | Inline validation | Vui lòng chọn ít nhất một cột dữ liệu để xuất | `OfferExportRequest:19` |
| 391 | MSG391 | Error page 404 | Không tìm thấy thư mời nhận việc. | `CandidateOfferServiceImpl:77` |
| 392 | MSG392 | Lỗi nghiệp vụ | Thư mời vừa được cập nhật. Vui lòng tải lại trang. | `CandidateOfferServiceImpl:112` |
| 393 | MSG393 | Lỗi nghiệp vụ | Thư mời này đã được trả lời nên không thể thay đổi. | `CandidateOfferServiceImpl:132` |
| 394 | MSG394 | Lỗi nghiệp vụ | Thư mời này đã quá thời hạn phản hồi ( | `CandidateOfferServiceImpl:138` |
| 395 | MSG395 | Lỗi nghiệp vụ | Thư mời này đã quá ngày bắt đầu nhận việc dự kiến ( | `CandidateOfferServiceImpl:143` |
| 396 | MSG396 | Lỗi nghiệp vụ | Không có dữ liệu đề xuất Offer phù hợp để xuất Excel. | `OfferExportServiceImpl:88` |
| 397 | MSG397 | Lỗi nghiệp vụ | Đã xảy ra lỗi khi tạo tệp Excel. Vui lòng thử lại sau. | `OfferExportServiceImpl:111` |
| 398 | MSG398 | Lỗi nghiệp vụ | Phạm vi xuất dữ liệu không được để trống. | `OfferExportServiceImpl:136` |
| 399 | MSG399 | Lỗi nghiệp vụ | Vui lòng chọn ít nhất một đề xuất để xuất dữ liệu. | `OfferExportServiceImpl:141` |
| 400 | MSG400 | Lỗi nghiệp vụ | Vui lòng chọn ít nhất một cột dữ liệu để xuất. | `OfferExportServiceImpl:146` |
| 401 | MSG401 | Lỗi nghiệp vụ | Cột dữ liệu yêu cầu không hợp lệ hoặc không được phép xuất: INVALID_EXPORT_COLUMN | `OfferExportServiceImpl:152` |
| 402 | MSG402 | Error page 404 | Không tìm thấy Application với ID: | `OfferServiceImpl:102` |
| 403 | MSG403 | Error page 404 | Không tìm thấy OfferProposal với ID: | `OfferServiceImpl:132` |
| 404 | MSG404 | Error page 404 | Không tìm thấy OfferProposal cho Application ID: | `OfferServiceImpl:141` |
| 405 | MSG405 | Error page 404 | Không tìm thấy đơn ứng tuyển với ID: | `OfferServiceImpl:229` |
| 406 | MSG406 | Lỗi nghiệp vụ | Theo quy tắc GBR-07, không thể tạo mới Offer Proposal vì đơn ứng tuyển đang có gói Offer ở trạng thái [ | `OfferServiceImpl:245` |
| 407 | MSG407 | Lỗi nghiệp vụ | Theo quy tắc GBR-07, chỉ được phép cập nhật Offer khi ở trạng thái thuộc Nhóm A (Draft). Trạng thái hiện tại: | `OfferServiceImpl:471` |
| 408 | MSG408 | Lỗi nghiệp vụ | Theo quy tắc GBR-07, chỉ được phép chỉnh sửa Offer khi ở trạng thái Bản thảo (Draft). Trạng thái hiện tại: | `OfferServiceImpl:524` |
| 409 | MSG409 | Lỗi nghiệp vụ | Chỉ được phép xóa bản thảo Offer (Draft). Trạng thái hiện tại: | `OfferServiceImpl:549` |
| 410 | MSG410 | Lỗi nghiệp vụ | Chỉ được gửi thư mời nhận việc khi Offer đã được Director phê duyệt. Trạng thái hiện tại: | `OfferServiceImpl:570` |
| 411 | MSG411 | Lỗi nghiệp vụ | Offer chưa có thời gian phản hồi hợp lệ. | `OfferServiceImpl:576` |
| 412 | MSG412 | Lỗi nghiệp vụ | Thời hạn phản hồi phải trước ngày bắt đầu dự kiến. | `OfferServiceImpl:583` |
| 413 | MSG413 | Lỗi nghiệp vụ | Mức lương đề xuất và lương thử việc không được để trống. | `OfferServiceImpl:617` |
| 414 | MSG414 | Lỗi nghiệp vụ | Mức lương đề xuất và lương thử việc phải lớn hơn 0. | `OfferServiceImpl:621` |
| 415 | MSG415 | Lỗi nghiệp vụ | Lương thử việc ( VND) phải đạt tối thiểu 85% lương chính thức ( | `OfferServiceImpl:628` |
| 416 | MSG416 | Lỗi nghiệp vụ | Thời gian phản hồi phải lớn hơn 0 ngày. | `OfferServiceImpl:677` |
| 417 | MSG417 | Lỗi nghiệp vụ | Chỉ được phê duyệt đề xuất Offer khi ở trạng thái chờ duyệt (Pending_Director). Trạng thái hiện tại: | `OfferServiceImpl:762` |
| 418 | MSG418 | Lỗi nghiệp vụ | Không xác định được danh tính Director để thực hiện phê duyệt. | `OfferServiceImpl:769` |
| 419 | MSG419 | Lỗi nghiệp vụ | Chỉ được từ chối đề xuất Offer khi ở trạng thái chờ duyệt (Pending_Director). Trạng thái hiện tại: | `OfferServiceImpl:801` |
| 420 | MSG420 | Lỗi nghiệp vụ | Không xác định được danh tính Director để thực hiện từ chối. | `OfferServiceImpl:808` |

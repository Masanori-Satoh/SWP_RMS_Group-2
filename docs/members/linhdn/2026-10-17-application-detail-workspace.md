# Application / HM Proposal Detail — bố cục workspace

> Chức năng lưu đề xuất thực tế được triển khai tiếp trong `2026-10-17-hm-suggestions-functional.md` theo quyết định giữ schema và dùng HM_Suggested. Trạng thái khóa nút trong báo cáo này là lịch sử trước quyết định đó.

## Phạm vi và file sửa

Chỉnh UI theo brief `Refine Application Detail / HM Proposal Detail UI`, giữ quy tắc `docs/architecture/UI/`, catalog và token mộc. RMS; dùng hướng dẫn Impeccable và UI UX Pro Max. Dùng PDF skill để đọc file mẫu phục vụ kiểm tra nguồn.

| File từ project root | Thay đổi trong lượt này |
| --- | --- |
| `src/main/resources/templates/candidate/application-detail.html` | Tổ chức header, hành trình/AI, CV trái và sidebar phải; giữ các form/action hiện có. |
| `src/main/resources/static/css/pages/applications.css` | Bố cục riêng `app-detail-page`, header, tiến trình đề xuất, AI gọn và contact facts; responsive. |
| `src/test/java/com/group2/rms/candidate/ApplicationPipelineWebTests.java` | Kiểm tra thứ tự khu vực, URL CV và contact links, hai nhánh AI/Proposal/Application. |

Các chỉnh sửa đang có từ lượt trước trong working tree được giữ nguyên. Không sửa schema, trạng thái, CV nguồn, SecurityConfig, AI service, shared layout, token, bookmark service hoặc workflow HM/HR. Không commit/push.

## Bố cục

- Header dạng card gọn: breadcrumb, tên, tin tuyển dụng, phòng ban, thời gian và badge; giữ lineage tới requisition/job/đề xuất hoặc hồ sơ.
- Hành trình ở ngay dưới header/cảnh báo, trước CV. Điểm AI, ngày chấm và form chấm lại theo quyền nằm cùng khu vực phía phải tiêu đề. Bỏ card AI riêng dưới CV.
- Main/side dùng grid 60/40, gap 24px; nội dung rộng tối đa 1360px. Trên màn nhỏ co về một cột.
- CV nằm đầu DOM và ở cột trái, tiêu đề “CV ứng viên”, link mở tab mới và iframe 72vh (65vh trên điện thoại). Giữ endpoint `/applications/{id}/cv`; thêm gợi ý PDF `FitH`/ẩn pane thumbnail. Viewer có thể bỏ qua gợi ý này; chức năng zoom/scroll vẫn do viewer cung cấp.
- Sidebar: **Thông tin liên hệ → Ghi chú chung → Hành trình hoạt động**, tiếp theo là các card thao tác/xác nhận theo quyền hiện có. Card không bị kéo theo chiều cao toàn bộ tài liệu.
- Chỉ hiện trường contact có dữ liệu. Email dùng mailto, điện thoại dùng tel. LinkedIn/Portfolio tách riêng, URL http(s) mở tab mới với `noopener noreferrer`; URL không an toàn chỉ hiện text. Text dài có wrap.
- Giữ ID/data attribute bookmark, ghi chú và các endpoint form hiện có; không xây hệ thống ghi chú mới.

## Phân biệt Application và đề xuất HM

Trang dùng chung DTO `ApplicationDetailResponse` và template; nhánh được xác định bởi `applicationStatus == HM_Suggested`.

- Application chính thức: dùng nguyên stepper từ service và fragment catalog, không tự tính hoàn thành giai đoạn trong HTML.
- Đề xuất chờ HR: giữ nguyên badge “HM đề xuất — chờ HR liên hệ” và cảnh báo yêu cầu ứng viên đồng ý. Chỉ hiện hai thông tin được trạng thái xác nhận: HM đã đề xuất và đang chờ HR liên hệ. Không vẽ các giai đoạn sàng lọc/phỏng vấn của đơn chính thức, không tạo enum hay audit mới.
- Lịch sử hoạt động vẫn đọc `detail.timeline`; đây là sự kiện đã xảy ra, tách với tiến trình phía trên.
- Điểm AI lấy từ `detail.aiMatchScore`; service đọc kết quả theo Application ID hiện tại, không lấy điểm từ hồ sơ nguồn. Không có kết quả: Application hiển thị “Chưa có điểm AI”, đề xuất hiển thị “Chưa sàng lọc AI”.
- Bản ghi đề xuất legacy vẫn lưu trong Application và bước hoàn tất popup mới vẫn bị khóa như báo cáo trước. Đây là sửa UI, chưa tách mô hình dữ liệu hoặc số đếm pipeline.

## Kiểm tra CV của Phan Minh Hải

Đã chạy SELECT chỉ đọc trên database local cấu hình của project, không thực hiện ghi dữ liệu:

| Dữ kiện | Kết quả |
| --- | --- |
| Đề xuất | Application 219, Candidate 191, Phan Minh Hải, Job Posting 8, HM_Suggested |
| Audit `TalentPoolSuggested` của 219 | OldValue = 191, tức source Application 191 |
| Hồ sơ nguồn | Application 191, Candidate 191, Phan Minh Hải, Job Posting 3 |
| AppliedCvUrl của cả hai | `/samples/cv/sample_cv_qa_automation.pdf` |
| Endpoint chi tiết | Đọc AppliedCvUrl của Application được mở sau kiểm tra quyền; URL mẫu được redirect tới file static tương ứng |

Đã đọc file PDF thật bằng pypdf: tên trong file là **Lê Quốc Bảo**. Cuối file ghi **“CV mẫu – nhân vật và dữ liệu giả lập, chỉ dùng cho môi trường phát triển RMS.”**

Kết luận: Candidate ID và nguồn audit khớp nhau; PDF khác tên do bản ghi hiện tại dùng CV mẫu. Không có bằng chứng endpoint đang lấy nhầm source ID cho trường hợp này. Dữ liệu mẫu không nhất quán với tên ứng viên; giữ nguyên và báo riêng, không sửa tên PDF hoặc thay CV nguồn để che lệch dữ liệu.

## “Liên kết hồ sơ” trong mẫu

Mục nằm dưới nhãn LinkedIn/Portfolio là URL hồ sơ nghề nghiệp hoặc trang portfolio của Candidate. Đây là trường thông tin liên hệ của Candidate, không thể dùng để suy ra ứng viên đã đồng ý hay đã nộp đơn cho job hiện tại. Giao diện triển khai dùng tên **LinkedIn** và **Portfolio** cụ thể thay cho một nhãn “Xem liên kết hồ sơ” mơ hồ.

## Kiểm thử và giới hạn

- **80 test Java qua:** 42 MVC, 24 pipeline service, 7 bookmark, 7 selection review; 0 failure/error.
- **17 test JavaScript qua:** state lựa chọn, UI Talent Pool và preview động; 0 failure.
- Kiểm tra ID template không trùng, CSS dùng token tồn tại, không thêm inline style/handler, `git diff --check` qua.
- Test render cả nhánh Application có AI, không có AI, Proposal chờ HR; bảo toàn quyền/form/CSRF và link contact/CV.
- Có kiểm chứng DB local và nội dung file PDF chỉ đọc. Chưa thao tác lưu ghi chú hoặc chuyển trạng thái trên DB thật.
- Trình duyệt kiểm tra vào được `/applications/219` và được chuyển tới trang đăng nhập; tab sau đó không còn truy cập được bởi phiên điều khiển. Chưa có screenshot nghiệm thu layout mới hoặc bằng chứng end-to-end sau đăng nhập; cần kiểm tra trực quan trên ứng dụng.

## Kiểm tra thủ công

1. HM mở `/applications/219`: badge/cảnh báo đúng, hành trình chỉ hiển thị đề xuất/chờ HR; AI chưa có điểm, contact đứng đầu sidebar.
2. Mở một Application chính thức có kết quả AI: stepper/trạng thái đúng, điểm/ngày chấm ở đầu, các nút duyệt/phỏng vấn/offer đúng quyền.
3. Mở đơn chưa có AI: thấy trạng thái trống; không xuất hiện điểm của CV cũ.
4. Xem và mở CV tab mới: cùng file nguồn, zoom/cuộn được. Với #219 tên PDF mẫu Lê Quốc Bảo là lệch dữ liệu demo đã báo ở trên.
5. Kiểm tra mailto/tel, LinkedIn/Portfolio, ghi chú/bookmark và link Talent Pool; không bấm xác nhận ứng tuyển khi chỉ kiểm tra layout.
6. Cuộn desktop và viewport nhỏ: CV không kéo giãn sidebar, contact/notes/activity đúng thứ tự, không tràn ngang toàn trang.

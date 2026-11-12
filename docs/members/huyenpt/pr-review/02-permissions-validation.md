> Ghi chú 11/11/2026: đường dẫn và package test đã được cập nhật theo tầng kiểm thử; kết quả Task/PR lịch sử giữ nguyên.

# PR 02/08 — Phân quyền và validation nghiệp vụ Offer

**Base:** `main`, commit `586d6c44` đã chứa PR #160.
**Nhánh:** `fix/huyenpt/offer-02-permissions-validation`.

## Vấn đề và hành vi sau thay đổi

Các URL quản lý Offer trước đây còn dùng quyền chung HR/Director; một số nút trên chi tiết cũng hiện cho Director. Service còn có fallback chọn người dùng đầu tiên hoặc nhận người tạo từ request, và chưa kiểm tra đầy đủ các đường truy cập thư mời đã xóa, điều kiện phỏng vấn và lý do từ chối khi gọi Java trực tiếp.

Sau thay đổi:

- HR tạo, sửa/xóa nháp và phát hành; Director chỉ được xem, duyệt hoặc từ chối. Các matcher cụ thể đứng trước matcher Offer tổng quát.
- Tạo qua luồng HR phải có kết quả phỏng vấn Passed; request không thể chọn thay người tạo bằng proposedById.
- Người tạo, người quyết định và người xem thống kê/lịch sử Director lấy từ CurrentUserService; không fallback sang tài khoản đầu tiên. Tài khoản phải tồn tại, đang Active và có phiên đăng nhập hợp lệ.
- Đường đọc/ghi theo OfferId dùng helper loại bản ghi xóa mềm. Không tạo hoặc tiếp tục sửa/phát hành/quyết định khi Application đã Hired, Rejected, Withdrawn hoặc Cancelled; không đưa các hồ sơ này vào dropdown tạo.
- Service bắt buộc lý do từ chối và giới hạn phản hồi Director 2.000 ký tự. Duyệt vẫn cho phép ghi chú tùy chọn.

## File thay đổi

| File | Nội dung |
|---|---|
| `src/main/java/com/group2/rms/core/config/SecurityConfig.java` | Thêm đúng chín dòng matcher Offer để giới hạn GET tạo/sửa và POST/PUT/DELETE quản lý cho HR; giữ quyền duyệt/từ chối của Director và các matcher module khác |
| `src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java` | Gom tìm Offer chưa xóa; kiểm tra Passed và hồ sơ chưa kết thúc; dùng CurrentUserService thay tra UserRepository/fallback; bỏ tin cậy proposedById; kiểm tra lý do và độ dài phản hồi tại service |
| `src/main/resources/templates/offers/detail.html` | Nút xóa nháp, phát hành và dialog phát hành chỉ hiện cho HR khi trạng thái phù hợp; không đổi CSS/layout |
| `src/test/java/com/group2/rms/offer/controller/OfferIntegrationTests.java` | Chặn Director ở tám URL/method với cả JSON và form; kiểm tra bốn trường hợp hiển thị nút theo HR/Director và trạng thái |
| `src/test/java/com/group2/rms/offer/service/OfferServiceTests.java` | Hồi quy Passed/xóa mềm/người tạo; thêm actor anonymous/missing/inactive, giới hạn phản hồi, hồ sơ kết thúc; cập nhật fixture Active và sửa các test cũ từng cho từ chối không lý do |
| `docs/architecture/ARCHITECTURE_GUIDE.md` | Bổ sung hai dòng bảng quyền URL Offer tương ứng với SecurityConfig; không đổi quy tắc kiến trúc khác |
| `docs/members/huyenpt/pr-review/02-permissions-validation.md` | Báo cáo phạm vi, file thay đổi và bằng chứng kiểm thử của PR |

## Kiểm thử

| Nhóm | Kết quả |
|---|---:|
| OfferServiceTests | 125 đạt |
| OfferIntegrationTests | 46 đạt |
| CandidateOfferServiceTests | 35 đạt |
| CandidateOfferWebTests | 19 đạt |
| Tổng | **225 đạt; không lỗi hoặc bỏ qua** |

Test chạy trực tiếp trong worktree của PR 2. Những test cũ cho từ chối thiếu lý do đã đổi sang kiểm tra bị từ chối trước khi lưu. Fixtures dùng account Active và dọn security context sau mỗi test. Lần chạy đầu phát hiện ba kỳ vọng nghiệp vụ cũ và một lỗi setup mock; đã sửa test đúng hợp đồng rồi chạy lại toàn bộ bốn nhóm đạt.

Git diff check và mô phỏng merge với origin/main được kiểm tra trước push. Chưa kiểm thử trực quan bằng browser. Không chạy SpringBootTest dùng database ứng dụng hoặc migration trong lượt này.

## Phạm vi và phụ thuộc

- Dựa trên main đã merge PR 1, không mang lặp 16 file frontend của PR trước.
- Không sửa schema, migration, nguồn Application, notification chung, CSS hoặc JavaScript.
- Không mở rộng quyền Director, không tự chuyển trạng thái Application và không tạo tài khoản thay thế.
- Bảo vệ đồng thời/cập nhật trạng thái có điều kiện, notification sau commit và chỉnh mail/export thuộc PR tiếp theo. Kiểm tra hồ sơ trong PR này là validation ở service, chưa phải bảo đảm xử lý đồng thời của PR 6.
- Các API/service legacy chưa được gỡ ở đây; việc loại đường không còn dùng dành cho PR 5. Không bao gồm phát hành lại thư mời.

Chỉ PR 2 được chuẩn bị và push trong lượt này. Đợi người dùng tạo PR thành công rồi mới tiếp tục PR 3.

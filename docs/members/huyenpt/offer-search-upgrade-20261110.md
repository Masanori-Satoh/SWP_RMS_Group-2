# Offer keyword search upgrade — 10/11/2026

## Phạm vi và Git

- Nhánh: `fix/huyenpt/project-test-errors`, tại worktree `scratch/project-test-errors-worktree`.
- Đã fetch và merge `origin/main` tại `054c73d2`, merge commit `d8959d51`, không conflict trong worktree này.
- Working tree gốc có sẵn conflict tài liệu nên được giữ nguyên; không reset, restore, clean hoặc xử lý các conflict đó.
- Chỉ sửa production thuộc Offer. Không đổi layout/core dùng chung, database, cấu hình hay thông tin đăng nhập. Không push.
- Tuân thủ `ARCHITECTURE_GUIDE.md` và `UI_RULES.md`: tái sử dụng `ListFilters`, giữ toolbar và phân quyền hiện tại.

## Các phase

| Phase | Kết quả | Commit | Diff của phase | Kiểm thử |
| --- | --- | --- | --- | --- |
| 1 | Dùng chung điều kiện keyword HR/Director; Excel FILTERED đọc bộ lọc đã áp dụng trên URL | `444979a3` | 6 file, +76/-25 | 219 Java, 23 JavaScript đạt |
| 2 | Mở rộng trường tìm kiếm và tìm chính xác mã thư mời; giữ UI hiện có | `3fffedad` | 5 file, +64/-30 | 288 Java, 23 JavaScript đạt |
| 3 | Bổ sung kiểm tra HTML/phân trang theo vai trò và tổng hợp bằng chứng | Commit chứa báo cáo này | Xem Git stat của commit | 290 Java đạt; chạy lại riêng integration 65/65 đạt sau bổ sung assertion phân trang |

Số file mỗi phase có phần trùng nhau; không cộng các số đó thành số file riêng biệt.

## Hành vi sau thay đổi

- Một ô tìm kiếm cho tên ứng viên, email, vị trí đề xuất và phòng ban; các trường được kết hợp OR.
- `#12` tìm đúng OfferId 12; `12` là keyword văn bản. Mã vượt phạm vi integer trả danh sách rỗng.
- Chuẩn hóa khoảng trắng, giới hạn 120 ký tự, escape ký tự wildcard. Dùng collation `Latin1_General_100_CI_AI` tại biểu thức tìm kiếm để hỗ trợ không phân biệt hoa/thường và dấu mà không đổi schema.
- Tái sử dụng `ListFilters.cleanKeyword`, `containsPattern` và constant collation hiện có. Không sửa source của thành viên khác.
- Giữ điều kiện soft delete, phạm vi Director, status, sort và phân trang hiện tại.
- Excel FILTERED dùng `search`, `status`, `timeSort` trên URL; nội dung đang gõ nhưng chưa áp dụng không làm thay đổi export. SELECTED/ALL giữ nguyên.
- Hai template chỉ đổi placeholder và thêm `maxlength="120"` cho input hiện có; không thêm control hay CSS.

## Bằng chứng và giới hạn

### Kiểm thử tự động

- Maven chọn 5 lớp Offer: `OfferServiceTests` (148), `OfferIntegrationTests` (65), `OfferExportServiceTests` (12), `CandidateOfferServiceTests` (46), `CandidateOfferWebTests` (19): **290/290 đạt**, không failure/error/skipped.
- Chạy lại `OfferIntegrationTests` sau thay đổi fixture query string: **65/65 đạt**. Assertion kiểm tra link trang tiếp theo giữ keyword/status/sort cho HR và Director.
- Node frontend: **23/23 đạt**, bao gồm FILTERED/SELECTED/ALL và input chưa áp dụng khác URL.
- Đây là các lớp Offer được chọn, không phải kết luận toàn bộ test của dự án đều đạt. JaCoCo có warning execution data cũ của lớp Notification; không sử dụng số coverage để đánh giá đợt này.

### SQL chỉ đọc

- 9 tình huống SELECT fixture đạt: không dấu, email hoa/thường, phòng ban không dấu, vị trí, wildcard literal `%_[`, số thường, `#ID`, mã không tồn tại và chuỗi giống SQL injection được truyền như tham số.
- Không INSERT/UPDATE/DELETE/DDL. Checksum OfferProposal trước/sau phép đo không đổi.
- Trên 8 thư mời local, 20 lần đo sau warm-up: median SQL tương đương trước 1,764 ms, sau 2,198 ms; p95 lần lượt 2,406/2,728 ms. Cả hai trả 3 bản ghi với keyword Java.
- Đây là SQL tương đương, chưa phải đo SQL Hibernate sinh thực tế. Dataset nhỏ, không chứng minh tối ưu tốc độ trên dữ liệu lớn; tìm nhiều trường làm tăng công việc truy vấn.

### Browser

- Mở HTML do MockMvc/Thymeleaf render cho HR và Director, dùng CSS/JS thật của project trên fixture server local. Toolbar vẫn một ô tìm và hai select; Director không có export modal.
- Gõ `Tester chưa áp dụng` trong ô HR khi URL vẫn là `search=Java&status=Draft&timeSort=LATEST`, mở export: payload thực tế vẫn `Java` / `Draft` / `LATEST`.
- Endpoint export fixture chỉ ghi payload rồi trả lỗi có chủ đích; không tạo Excel, gửi email hoặc gọi database. Kiểm tra browser này không thay thế end-to-end trên ứng dụng thật có đăng nhập.
- Bằng chứng local giữ tại `scratch/offer-search-upgrade-20261110`: log Maven, `sql-read-only-results.json`, `browser-export-payload.json`, HTML và ảnh `browser/hr-toolbar.png`, `browser/director-toolbar.png`. Các file scratch không đưa vào commit.

## File thay đổi trong đợt này

Production: `OfferProposalRepository.java`, `OfferServiceImpl.java`, `offer-hr.js`, `offers/list.html`, `offers/director-list.html`.

Tests: `OfferServiceTests.java`, `OfferIntegrationTests.java`, `offer-frontend.test.cjs`. Documentation: báo cáo này.

## Kiểm tra tiếp trên ứng dụng thật

Sau restart, HR/Director tìm thử tên không dấu, email, phòng ban, `#ID`; đổi trang và xác nhận URL giữ bộ lọc. HR gõ keyword khác nhưng chưa áp dụng rồi export FILTERED, so sánh dữ liệu với danh sách đang áp dụng. Nếu cần đánh giá dữ liệu lớn, đo execution plan và SQL Hibernate trên database kiểm thử riêng trước khi đề xuất index hoặc cơ chế tìm kiếm khác.

## Bổ sung tìm lương — 11/11/2026

- Nhận `36.000.000 VNĐ`, `36.000.000 VND`, `36.000.000` và số có đơn vị như `36000000 VNĐ`; không phân biệt hoa/thường, hỗ trợ khoảng trắng khi copy từ bảng.
- So sánh BigDecimal chính xác với `proposedSalary` HOẶC `probationSalary`, không chuyển cột tiền thành chuỗi để LIKE. Dùng nhánh chung cho HR, Director và Excel FILTERED; giữ các điều kiện phân quyền/trạng thái/xóa mềm hiện có.
- Số trần `36000000` vẫn là keyword; định dạng sai hoặc câu trộn chữ vẫn tìm văn bản. Giá trị tiền vượt decimal(18,2) trả rỗng trước khi tạo điều kiện SQL.
- Production chỉ sửa `OfferProposalRepository.java` (+9 dòng), không thêm file production, không đổi HTML/CSS/JS, schema hoặc cấu hình kết nối.
- Bổ sung 14 tình huống kiểm thử keyword/lương trong `OfferServiceTests.java`; mở rộng kiểm thử workbook FILTERED sang ba định dạng tiền trong `OfferExportServiceTests.java`.
- Kết quả: 307/307 test Java thuộc 5 lớp Offer đã nêu ở trên và 23/23 test frontend đạt; `git diff --check` đạt. Log lưu tại `scratch/offer-search-upgrade-20261110/salary-search-final.log` và `salary-search-node.log`, không stage scratch.
- Giới hạn: đợt bổ sung này kiểm thử bằng mock Criteria và workbook thực trong bộ test; chưa xác minh end-to-end tìm lương trên ứng dụng đang đăng nhập hoặc benchmark SQL thực. Không kết nối/ghi database khi chạy bộ test này.
- Kiểm tra thủ công sau restart: tìm `36.000.000 VNĐ`, đối chiếu cả hai cột lương; thử `17.000.000` để khớp lương thử việc; giữ status/sort rồi export FILTERED và so sánh danh sách.

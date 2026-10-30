# 2026-10-30 — Đồng bộ giao diện chi tiết Application

- Người thực hiện: LinhDN / Codex.
- Yêu cầu: dùng 4 skill FE đã cài, áp dụng bố cục ảnh mẫu và giữ cùng giao diện với Talent Pool / HR Recommend.
- Phạm vi: `/applications/{id}`; không thay đổi trang danh sách Application hay luồng của module khác.
- Database: không sửa schema, migration, seed hoặc dữ liệu.

## Kết quả

- Dùng lại `admin-workspace :: scene/scripts`, `adw-surface` và nút/badge/dialog catalog đang dùng ở Talent Pool. Giữ màu xanh, font và nền văn phòng hiện có.
- Chuyển thông tin nhận diện ứng viên, cảnh báo đồng ý, hành trình và CV vào cột trái khoảng 64%; cột phải khoảng 36% chứa thông tin ứng viên, ghi chú chung, hoạt động và thao tác theo quyền.
- Thêm avatar chữ cái đầu từ tên thật; tên, vị trí, phòng ban, nguồn gốc và trạng thái giữ nguyên binding backend.
- Thông tin ứng viên thành grid 2 cột gọn. Trường thiếu tiếp tục ẩn; email/điện thoại có liên kết; URL chỉ thành liên kết web khi có HTTP/HTTPS.
- Đề xuất chờ HR có 4 mốc: HM đề xuất đã hoàn tất, Chờ HR liên hệ đang xử lý, Ứng viên đồng ý và Lên lịch / xử lý tiếp chưa tới. Hai mốc sau không được đánh dấu hoàn tất giả.
- Hồ sơ ứng tuyển chính thức vẫn dùng stepper chung và điểm AI thật; nút chấm AI chỉ hiện theo `actions.rescreen`.
- Giữ trình xem PDF thật và liên kết mở CV trong tab mới. Không dựng toolbar hoặc nội dung CV giả trong template.
- Giữ form duyệt, nhận xét, xác nhận loại, lên lịch, xem kết quả và tạo offer đúng điều kiện hiện có.
- Checkbox consent vẫn bắt buộc; nút xác nhận liên kết với đúng form POST/CSRF. HM và Director không được hiện thao tác xác nhận thay HR.
- Hộp kết thúc đề xuất dùng cấu trúc dialog chuẩn, có tiêu đề, mô tả, Hủy và xác nhận; giữ nguyên URL và phương thức gửi.
- Dưới 1100px chuyển thành một cột; ở 540px trở xuống hành trình chuyển dọc, các thẻ thu gọn padding và nút mở CV chiếm đủ hàng.
- CSS mới chỉ nạp trên trang chi tiết và có tiền tố `apd-`. Không sửa stylesheet danh sách, layout, sidebar, topbar, tokens, fragment hoặc JavaScript dùng chung.

## Skill và quyết định thiết kế

- Đọc hợp đồng Mộc tại `docs/members/linhdn/AI MD/DESIGN.md`, UI Rules/Catalog/CSS Guide trước khi sửa.
- UI UX Pro Max: tra cứu responsive và feedback; ưu tiên không tràn ngang, thông báo thao tác rõ và giữ focus/validation.
- Impeccable: dùng operate/craft floor để tinh chỉnh UI đang có, giữ thương hiệu và luồng; kiểm tra cả trạng thái dialog.
- Taste: app detail, `accent-ladder`, seed **887247549**, giữ palette/type của Mộc. Chọn kiểu này để thống nhất với workspace hiện có; không áp shell hoặc palette mới từ sheet.
- Awesome DESIGN reference: tham khảo mục Hierarchy của bản SAP cục bộ `.agents/references/awesome-design-md/upstream/design-md/sap/DESIGN.md`; chỉ lấy cách phân tầng nội dung, không sao chép nhận diện hoặc cỡ chữ.
- Câu hỏi chính: hồ sơ/đề xuất này đang ở bước nào, có bằng chứng CV gì và người đang xem được xử lý bước nào tiếp theo?
- Dấu hiệu riêng: hành trình đồng ý của ứng viên đặt ngay trước CV; mặt nhận diện là hệ màu xanh và workspace Mộc.
- Dữ liệu: một hồ sơ trong một tin, ngày giờ backend, điểm AI 0–100 hoặc chưa có, các trường cá nhân có thể thiếu, ghi chú và hoạt động theo dữ liệu thật. Người dùng là HR/HM/Director; tác vụ chính là đọc CV rồi quyết định hoặc xác nhận sau liên hệ. Không thêm số liệu minh họa vào sản phẩm.

## Báo cáo có ảnh trong thư mục LinhDN

[Báo cáo thay đổi kèm ảnh desktop/mobile](../../members/linhdn/2026-10-30-application-detail-ui.md). Ảnh được lưu trong repo, không phụ thuộc thư mục tạm của Codex.

## File change

Số dòng dưới đây tính so với snapshot ngay trước tác vụ này, không gộp các lượt Talent Pool / HR Recommend trước.

<!-- file-stats:start -->
| File | Thêm | Xóa |
| --- | ---: | ---: |
| `docs/management/WORK_LOG.md` | +1 | -0 |
| `docs/members/linhdn/README.md` | +6 | -0 |
| `src/main/resources/templates/candidate/application-detail.html` | +91 | -68 |
| `src/test/java/com/group2/rms/candidate/application/pipeline/controller/ApplicationPipelineWebTests.java` | +50 | -2 |
| `src/main/resources/static/css/pages/application-detail.css` | +84 | -0 |
| `docs/management/work_logs/2026-10-30-application-detail-ui.md` | +85 | -0 |
| `docs/members/linhdn/2026-10-30-application-detail-ui.md` | +43 | -0 |
| `docs/members/linhdn/assets/application-detail-ui-2026-10-30/desktop.png` | Ảnh mới (464,965 byte) | — |
| `docs/members/linhdn/assets/application-detail-ui-2026-10-30/mobile.png` | Ảnh mới (190,902 byte) | — |
| **Tổng 9 file (7 text, 2 ảnh)** | **+360 dòng** | **-70 dòng** |

Đối chiếu SHA-256: **953 file tracked ngoài phạm vi giữ nguyên**; 4 file tracked của tác vụ thay đổi (gồm README được bổ sung theo yêu cầu mới), thêm 5 file mới. Ngoài bảng còn một ngoại lệ rule/file trong cấu hình hook đang Git ignore, mô tả ở cuối báo cáo.
<!-- file-stats:end -->

## Kiểm chứng

- Maven offline `ApplicationPipelineWebTests`: **67/67 qua**, không failure/error/skip, có render Thymeleaf qua MockMvc.
- Sau chỉnh cấu trúc dialog: chạy lại riêng kịch bản render/phân quyền HR đề xuất, HM đề xuất, HR duyệt và Director duyệt.
- Node `application-preview.test.cjs`: **3/3 qua**; không sửa JS hoặc API preview/bookmark.
- Taste source lint: **0 FAIL, 0 WARN** cho template và CSS riêng.
- Taste inspector: **0 FAIL, 4 WARN** (các WARN có giải thích bên dưới), allowance `placeholder-data` vì HTML QA dùng email `example.com` trong fixture hiện có, không phải dữ liệu gán vào sản phẩm. Không sửa fixture email hoặc dữ liệu thật để che cảnh báo.
- Các WARN được giữ có lý do: `palette-cream` là token nền catalog Mộc; `font-fallback` là `--sans` chung khai báo Inter rồi dùng Source Sans 3 đã tải (không đổi font toàn hệ thống); `ragged-row` nhận nhầm hai stack nội dung độc lập của mockup thành hai thẻ ngang hàng; `no-data-hue` không tính màu xanh của các mốc pseudo-element dù màu `--brand` hiển thị đúng. Không sửa file dùng chung để làm vừa quy tắc generic của skill.
- Browser Chrome: kiểm tra viewport 1440px, 768px và 390px từ HTML render thật của MVC; các mốc đề xuất có trạng thái đúng, quyền HR/HM/Director, checkbox required, CSRF và nút submit gắn đúng form.
- Browser: mở/Hủy kết thúc đề xuất, mở dialog lưu ứng viên, mở/Hủy loại hồ sơ; không gửi mutation nghiệp vụ trong fixture QA.
- Kiểm tra tên/vị trí/email dài ở mobile, không tràn ngang toàn trang; không có JavaScript exception.
- Artifact nằm ngoài source tại `C:/Users/Administrator/.codex/visualizations/2026/10/30/01a18c5b-8f9a-7663-87be-b1559676ec27/`: `application-detail-*.png`, `application-detail-qa.json`, log Maven và báo cáo Taste inspector.
- Ảnh dùng dữ liệu fixture của MVC và PDF mẫu có sẵn `samples/cv/sample_cv_frontend_react.pdf`; không seed hoặc thay CV người dùng. Chưa kiểm chứng live SQL Server/SMTP/CV của tài khoản thật trong lượt này.
- Đối chiếu hash file ngoài phạm vi và `git diff --check` trước bàn giao. Không commit/push.

## Nhận xét sau render

- CV là phần chính có bề ngang lớn hơn cột phụ; các đầu thẻ của hai cột cùng thẳng hàng, khoảng cách đều 16px.
- Thẻ thông tin không kéo ngang cả trang; nhãn và giá trị cá nhân phân nhóm, URL dài được xuống dòng.
- Hoạt động/ghi chú dài cuộn trong vùng riêng, không ép thẻ khác phình ra. Mobile vẫn đọc đủ dữ liệu và dùng được các nút.
- Giữ nền/đổ bóng của catalog Mộc dù Taste ưu tiên bề mặt phẳng: đây là yêu cầu đồng bộ giao diện hiện có, không phải thay thương hiệu.

## Bổ sung theo yêu cầu tài liệu và hook cuối lượt

- Thêm báo cáo có ảnh trong thư mục LinhDN; ghi quy ước báo cáo cho mỗi lượt và liên kết mới tại README của LinhDN.
- `flat-type-hierarchy`: detector phân tích HTML không nạp CSS ngoài nên nhận body/h1/h2 đều 16px; Chrome đo h1 26px desktop / 23px mobile, tiêu đề thẻ 17px, body 13,5–15px. Đây là false positive; giữ UI, không sửa font/khung dùng chung.
- Đã dùng `impeccable hooks ignore-value flat-type-hierarchy "*" --file src/main/resources/templates/candidate/application-detail.html --reason ...`; chạy `detect` lại không còn finding. Ngoại lệ chỉ một rule/một file; giữ ngoại lệ có sẵn của suggestions. `.impeccable/config.json` là cấu hình đang bị Git ignore tại máy, không thuộc số file tracked trong bảng.
